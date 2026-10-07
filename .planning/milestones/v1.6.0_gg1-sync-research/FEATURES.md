# Feature Research — Fork vs. Upstream v1.6.0 Disposition

**Domain:** ClickHouse Kafka Connect sink — fork (`custom_build`, `v1.2.9_gg4`) vs. upstream `v1.6.0`
**Researched:** 2026-09-18
**Confidence:** HIGH — every claim below is backed by `git show`/`git diff` output against `v1.6.0` (`789a57c`) and `main` (`bec167d` = upstream `v1.2.9`), quoted inline. Two items are marked explicitly as requiring the fork's live test suite to confirm (noted per-item).

**Method used:** `git show v1.6.0:<path>`, `git diff main...v1.6.0 -- <path>`, `git diff main...custom_build -- <path>`, `git show v1.6.0:CHANGELOG.md`, plus one direct jar inspection (`unzip -l` on the resolved `kafka-clients-4.1.1.jar` in the local Gradle cache) to settle a compile-safety question. No memory-based claims.

---

## Disposition Table (the eight fork features)

| # | Fork feature | Upstream v1.6.0 coverage | Disposition | Fork test that adjudicates |
|---|---|---|---|---|
| 1 | `TableOperationFilter` SMT (drop `c`/`u`/`d`/`t` per `database.table`) | **Not at all.** No file of this name exists at `v1.6.0` (`git ls-tree -r v1.6.0` — no match). Upstream's new `DebeziumRecordConvertor` is a different job (see below) — it doesn't drop ops, it *converts* the whole envelope into a flattened, versioned row. | **Keep fork wholesale.** Carry the file forward unchanged; only mechanical import/package adjustments possible. | `TableOperationFilterTest` (26 unit) + 1 sink-level test — must stay green byte-for-byte, no upstream code path touches it |
| 2 | `HeadersToValue` SMT (all headers → one map field) | **Not at all.** No such file upstream. | **Keep fork wholesale.** | `HeadersToValueTest` (25 unit) + 1 sink-level |
| 3 | `HeaderToValue` SMT + `HeaderToValueConfig` (selected headers → value fields) | **Not at all** for the transform itself. But upstream **independently fixed the identical bug class** the fork found in this transform family — see "KeyToValue" below: upstream's 1.3.8 changelog entry reads *"Fixed `com.clickhouse.kafka.connect.transforms.KeyToValue` transformation to handle different schemas. Before this fix, the value schema was cached and not updated, making the transformation incompatible with evolving schemas."* That is the same stale-`valueSchema`-cache defect the fork fixed in `HeaderToValue`/`HeadersToValue` (D-02) — upstream just never touched `HeaderToValue` itself, so the fork's fix there still stands and is still needed. | **Keep fork wholesale**, including the D-02 stale-schema-cache fix — upstream never fixed this particular class. | `HeaderToValueTest` (24 unit) + 1 sink-level |
| 4 | `ConvertToMapOrJson` SMT (Struct → Map/JSON, Jackson/Gson, bytes→base64) | **Not at all.** No such file upstream. | **Keep fork wholesale.** | `ConvertToMapOrJsonTest` (37 unit) + perf + 1 sink-level |
| 5 | `targetTableFilter` config (regex, server-side `match(table, '<regex>')`) | **Not at all.** `git show v1.6.0:.../ClickHouseHelperClient.java` has no `match(` clause and no filter-related config; `showTables`/`showTablesV1`/`showTablesV2` build an unconditional `WHERE database = '%s'` with no filter argument. | **Keep fork's feature, re-applied by intent.** The carrier class changed shape substantially (see below) so this is a manual re-application, not a patch apply. | `ClickHouseSinkTaskMappingTest.tableFilterTest`, FILT-01..04 |
| 6a | `JSON(...)` parameterized column recognition in `Column`/`Type` | **Covered natively, same approach.** `Type.JSON` now exists upstream (confirmed: `git show v1.6.0:.../Type.java` lists `JSON` as the last enum constant, `main`'s does not). `Column.dispatchPrimitive` has `else if (valueType.startsWith("JSON")) { // There are some parameters that we ignore because server will handle them for us; type = Type.JSON; }` — functionally identical posture to the fork's own `valueType.startsWith("JSON")` branch. | **Adopt upstream's `Column.java`/`Type.java` for the `JSON` half.** It brings extra value: `SimpleAggregateFunction` unwrapping, Confluent Avro/Protobuf union-schema detection, and `connectTypeToClickHouseType`/`resolveBaseType` for `auto.evolve` (new upstream feature, see below) — none of which the fork has. | TYPE-01..05 (JSON portion) |
| 6b | `Dynamic` column recognition in `Column`/`Type` | **Not at all.** `git show v1.6.0:.../Type.java` has no `DYNAMIC` constant (confirmed directly — milestone's stated lead holds). A raw `Dynamic` type string falls through `dispatchPrimitive`'s `default:` with no match, returning `Type.UNKNOWN` (never throws, never returns null) — so a `Dynamic` column silently becomes an `UNKNOWN`-typed `Column` upstream, which later fails validation with the old bare "Data schema validation failed" message (the same class of gap the fork's own P3-WR-03 follow-up already documents for `INT128`/`INT256`). | **Keep fork's `Type.DYNAMIC` + its `Column.dispatchPrimitive` branch, ported onto upstream's rewritten `Column.java`.** This is exactly the "candidate for removal… still stays" case the milestone context already predicted; now code-confirmed. | TYPE-01..05 (Dynamic portion) |
| 6c | `describeTable` sub-column skip (`.null`, `.size0`, `.size`, children of `JSON`/`Dynamic`) | **Partially covered, by a different mechanism — needs live verification.** Upstream's `Table.handleNonRoot` only special-cases `.null`/`.size[0-9]+` inside the `ARRAY` branch (`SIZE_FIELD_MATCHER = Pattern.compile(".+\\.size[0-9]+$")` — note: requires a trailing **digit**, so a bare `.size` suffix with no digit, which the fork explicitly matches, would NOT match this regex). For any parent type the `switch` doesn't recognize (which now includes `JSON`, since upstream has no `case JSON:` there either), it falls to `default:`, which since 1.3.5 logs at `DEBUG` (`"Ignoring complex parent type…"`) instead of erroring, and the sub-column is still added to `allColumnsMap`/`allColumnsList` (via `registerValidColumn`, called unconditionally in `addColumn`) but never wired into any parent's structure and never added to `rootColumnsList`/`rootColumnsMap`. Net effect on insert correctness looks equivalent (no orphaned root column, no thrown error) but is unproven for real ClickHouse `JSON`/`Dynamic` sub-column shapes, and the exact-match-vs-suffix-regex gap (`.size` vs `.size0`) is a real, code-confirmed divergence. | **Keep the fork's explicit `shouldSkipDescribeRow` structural rule**, ported onto upstream's rewritten `Table`/`ClickHouseHelperClient` describe path — don't rely on upstream's implicit swallow, since it's a different code path with an unverified edge (bare `.size`) and doesn't skip the row before `Column.extractColumn` is even called (upstream still constructs and stores the sub-column; the fork's version never does). | DESC-01..04 — **this is the test suite that would settle whether upstream's implicit handling alone would actually pass**; it wasn't run against upstream's `Table.java` as part of this research (static reading only) |
| 7 | Metadata-free JSON insert path (`insertFormat=json` skips discovery/validation/cleanup) + `jsonInsertColumns` | **Not at all.** Upstream's `doInsertJsonV1`/`doInsertJsonV2` still take a `Table table` parameter and still call `cleanupExtraFields(data, table)` iterating `t.getRootColumnsList()` (`git show v1.6.0:.../ClickHouseWriter.java` lines ~1229–1310) — meaning upstream's JSON insert path *still requires* a normally-described `Table`. There is no `shouldSkipTableMetadata()`-equivalent anywhere upstream (confirmed: `grep -n` for that identifier and for `jsonInsertColumns` across `ClickHouseWriter.java`/`ClickHouseSinkConfig.java`/`ClickHouseHelperClient.java` at `v1.6.0` returns nothing). | **Keep fork wholesale, re-applied by intent** onto the rewritten `ClickHouseWriter`/`ClickHouseSinkConfig` — the `shouldSkipTableMetadata()` gate in `start()`/`updateMapping()`/`getTable()`, and `jsonInsertColumns` parsing, have no upstream equivalent to merge against. | JSON-01..04, JSON-05, COLS-01..06 |
| 8 | JMX `connector=<name>` naming; `async_insert` not forced | **`connector=<name>`: not at all, and the carrier method is gone.** Upstream moved MBean-name construction entirely out of `ProxySinkTask` (whose `getMBeanNAme()`/`buildMBeanName` no longer exist there) into a new `SinkTaskStatistics.getMBeanName(int taskId)` / `getTopicMBeanName(int taskId, String topic)` (new: per-topic MBeans, see Statistics section below). Both format as `"com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask%d,version=%s"` — no `connector=` segment anywhere, confirmed by direct read of `SinkTaskStatistics.java` at `v1.6.0`. Version source also changed: `Version.ARTIFACT_VERSION`, a build-time-generated constant from the connector's own `VERSION` file (via a new `generateVersionFile` Gradle task), **not** the ClickHouse client jar's `getImplementationVersion()` the fork's tests target. **`async_insert` not forced: unaffected — no upstream diff touches it** (`git diff main...v1.6.0 -- ClickHouseWriter.java \| grep async_insert` is empty). | **`connector=<name>`: keep fork's intent, but it must be reimplemented in the new location** (`SinkTaskStatistics`), not merged as a patch — the carrier method it lived in no longer exists. Also decide whether to extend it to the new `getTopicMBeanName()` for consistency. The fork's `:` → `_` / `null` → `unknown` sanitization is now largely moot for the *null* case (`Version.ARTIFACT_VERSION` is a compile-time non-null constant) but should be kept for safety since `connectorName` itself can still be null/contain `:`. **`async_insert`: keep fork wholesale, no conflict.** | JMX-01..04 (needs re-target once `SinkTaskStatistics` owns naming) · ASYNC-01..03 (unaffected, should stay green as-is) |

---

## Detail: the "known leads" questions, answered directly

### Does upstream's `JSON` support parse `JSON(...)` parameters, skip sub-columns, and serialize through RowBinary?

- **Parses `JSON(...)`:** yes, same way the fork does — `startsWith("JSON")` matches both bare `JSON` and parameterized `JSON(max_dynamic_paths=…)`, with parameters deliberately ignored ("the server will handle them for us" — same comment text, same posture, in `Column.java` at `v1.6.0`).
- **Skips sub-columns in `describeTable`:** partially, implicitly, by a different mechanism than the fork's — see row 6c above. Not proven equivalent; DESC-01..04 is the test that settles it.
- **Serializes JSON through RowBinary:** **yes, conditionally** — this is new capability the fork doesn't have at all. `ClickHouseWriter.doWriteColValue`'s `case JSON:` (new in `v1.6.0`, confirmed present at line 651):
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
      } else {
          throw new RuntimeException("Writing JSON in binary is not supported yet. Use `input_format_binary_read_json_as_string=1` in clickhouse settings to allow writing as string");
      }
  ```
  `isBinaryFormatWrtiteJsonAsString()` is derived from `clickhouseSettings.get("input_format_binary_read_json_as_string")` (a passthrough setting key, not a first-class connector config) — `ClickHouseSinkConfig.java` line 326-327. Default off → **default behavior is a bare `RuntimeException`, not the fork's actionable `DataException` naming `insertFormat=json`/`bypassRowBinary=true`.** This is genuinely new capability (JSON-as-string via RowBinary when the setting is opted in) that the fork's fail-fast approach (TYPE-06) doesn't offer. `Dynamic` has no equivalent `case DYNAMIC:` upstream at all (falls to `default:` which now *throws* a `DataException` in the fork but upstream's default just logs an error and silently drops the byte — a real behavioral gap the fork's TYPE-06 fix specifically closed and upstream hasn't).

  **Recommendation:** adopt upstream's `case JSON:` capability (it's strictly more than the fork has for the JSON-as-string case) but retain the fork's TYPE-06 fail-fast wording/behavior for the *un-opted-in* path and for `Dynamic`, since upstream's default-path exception (bare `RuntimeException`) and default `default:` case (silent log, no throw) are both worse than what TYPE-06 already pins.

### `DebeziumRecordConvertor.java` (+289, new file) — what does it do, does it overlap fork features, new config?

Genuinely new, opt-in upstream capability — not a replacement for anything the fork has. Summary (from `git show v1.6.0:.../DebeziumRecordConvertor.java`):

- **Activation:** gated by *both* the new `debeziumCDCEnabled` config (`ClickHouseSinkConfig.java` line 313, default `false`) *and* the incoming schema name ending in `.Envelope` — wired in `Record.java`: `if (debeziumCDCEnabled && schema.name() != null && schema.name().endsWith(".Envelope"))`. **Opt-in, safe by default** — a fork production connector that doesn't set `debeziumCDCEnabled=true` is unaffected even if its records happen to have `.Envelope`-named schemas.
- **Behavior:** flattens the Debezium envelope into a `ReplacingMergeTree(_version, is_deleted)`-shaped row: `op=c/r/u` → `after` struct with `is_deleted=0`; `op=d` → `before` struct with `is_deleted=1`; `op=t` → record skipped entirely. Injects synthetic `_version` (from `source.lsn`/`gtid`/`pos`, PostgreSQL/MySQL/SQL Server aware) and `is_deleted` columns. Also does Debezium-logical-type-aware value conversion (`MicroTimestamp`, `Date`, `Decimal`, hex-encoded raw bytes).
- **Overlap with fork features:** none structurally. `TableOperationFilter` *drops* selected op types per table (a filter); `DebeziumRecordConvertor` *converts every op* (except `t`) into an upsert/tombstone row for a specific ClickHouse table engine pattern (ReplacingMergeTree) — a different CDC strategy entirely, and the two could in principle be used together (`TableOperationFilter` upstream of `DebeziumRecordConvertor`'s auto-detection, since SMTs run before the sink task's `Record.convert`). `ConvertToMapOrJson` (Struct→Map/JSON formatting) doesn't overlap either — it's schema-agnostic, not Debezium-envelope-aware.
- **New config surface just for this:** `debeziumCDCEnabled` (boolean, default `false`).
- **Recommendation:** must-absorb capability to know about (it's a real upstream feature the fork's CDC users may want), but **not required** for this milestone's scope (default off, no interaction with the fork's existing eight features). Record it as a "new capability" in Requirements, not a disposition item.

### `KeyToValue.java` — upstream rewrote (+90), fork touched (+3) — does the fork's change survive?

**No survives-as-meaningful-change — the fork's diff is a no-op.** `git diff main...custom_build -- .../KeyToValue.java` shows the entire fork delta is an import-block reorder (`java.util.Map` moved above the `org.apache.kafka...` imports — a Spotless artifact), zero functional change.

Upstream's rewrite is a real bug fix, independently confirmed by the `1.3.8` `CHANGELOG.md` entry quoted in row 3 above: it replaces the single cached `valueSchema` field (which went stale on schema evolution — the exact same defect class Phase 2 of this project fixed in `HeaderToValue`/`HeadersToValue`, D-02) with a size-bounded `LRUCache<CacheKey, Schema>` keyed on `(valueSchema, keySchema)`, adds a new `shema_cache_max_size` config (range `[16, 1000]`, default `32`), and switches `builder.name/version/doc(...)` to null-guarded calls.

**Recommendation:** **adopt upstream's `KeyToValue.java` wholesale** — the fork carries no functional divergence to preserve, and upstream's version fixes a real bug for free.

**Compile-safety check performed:** upstream's rewrite imports `org.apache.kafka.common.cache.{Cache,LRUCache,SynchronizedCache}`. Verified directly against the fork's actual resolved dependency (`kafka-clients-4.1.1.jar` in the local Gradle module cache) via `unzip -l`: `org/apache/kafka/common/cache/{Cache,LRUCache,LRUCache$1,SynchronizedCache}.class` are all present. **No compile risk from the Kafka Connect API version difference** (upstream's own `libs.versions.toml` pins `kafka = "2.7.0"`; the fork stays on 4.1.1 — this class package survives across that range).

### `Utils.handleException` (+135) — does it now handle `com.clickhouse.client.api.ServerException`? Client-V2 dead-error-classification question.

**Yes — plainly, confirmed by direct read of `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/util/Utils.java`:**

```java
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.api.ServerException;
...
private static boolean isServerError(Throwable t) {
    return t instanceof ClickHouseException || t instanceof ServerException;
}
...
public static void handleException(Exception e, boolean errorsTolerance, boolean retryOnSocketException, Collection<SinkRecord> records) {
    Exception rootCause = Utils.getRootCause(e, true);
    if (isServerError(rootCause)) {
        int errorCode = rootCause instanceof ServerException
                ? ((ServerException) rootCause).getCode()
                : ((ClickHouseException) rootCause).getErrorCode();
        LOGGER.warn("ClickHouse server error code: {}", errorCode);
        switch (errorCode) {
            case 3: case 107: case 159: case 164: case 202: case 203: case 209:
            case 210: case 241: case 242: case 252: case 285: case 319: case 425: case 999:
                throw new RetriableException(e);
            ...
```

This is exactly the fix the fork's Active follow-up describes needing: `getRootCause`'s drill-down (`isServerError`) and the retriable-code switch both now match client V2's `ServerException` as well as V1's `ClickHouseException`, via `getCode()` vs `getErrorCode()`. Upstream's own `CHANGELOG.md` documents it explicitly under `1.6.0` Bug Fixes: *"With client V2, server errors arrive as `com.clickhouse.client.api.ServerException` rather than the V1 `ClickHouseException`, so the retriable error-code list in `Utils.handleException` never matched and tasks failed instead of retrying. Both exception types now share the same list."* (issue #818).

The fork's own `Utils.java` diff over `main` is, like `KeyToValue.java`, a pure import-reorder no-op (`git diff main...custom_build -- .../Utils.java` — 6 lines moved, zero logic changed).

**Recommendation:** **adopt upstream's `Utils.java` wholesale.** This closes the fork's Active follow-up "Error classification is dead code on the V2 path" for free, via absorbing upstream's fix rather than writing one — record this fact per the milestone's out-of-scope instruction; do not additionally "fix" it.

**Signature change to note:** `handleException` gained a `retryOnSocketException` parameter (new config, see below) and a new `SocketException`-retry branch gated by it — every call site (`ChunkFlusher.putDirect`, `ProxySinkTask.put`) must pass the new arg; this is a mechanical merge-time change, not a disposition question.

### `DeliveryStrategy`/`DirectDeliveryStrategy`/`AtLeastOnceBufferStrategy`/`ExactlyOnceBufferStrategy`/`ChunkFlusher` — do they change where `insertFormat` / the metadata-free JSON path plug in?

**No structural conflict.** All four strategies funnel through `ChunkFlusher.putDirect()`, whose entire body is:

```java
void putDirect(Collection<SinkRecord> records) {
    try {
        ...
        this.proxySinkTask.put(records);
        ...
    } catch (Exception e) {
        ...
        Utils.handleException(e, errorTolerance, retryOnSocketException, records);
        ...
    }
}
```

— i.e. it still calls `ProxySinkTask.put(records)`, the exact same entry point that exists pre-refactor, which is where `insertFormat`/the metadata-free path lives (inside `ClickHouseWriter`/`Processing`, several layers below `ProxySinkTask`). Selection of which strategy to use is gated by the new `bufferCount` config (`ClickHouseSinkTask.createDeliveryStrategy`: `bufferCount == 0` → `DirectDeliveryStrategy`, matching pre-refactor behavior exactly; `bufferCount > 0` → buffered strategies, chunking records before calling the same `put()`). Default `bufferCount=0`, so **out of the box, behavior for the fork's JSON insert path is unchanged.** If a fork user opts into `bufferCount>0`, `insertFormat=json` and `jsonInsertColumns` still apply per-chunk with no code interaction — worth a smoke test post-merge but not a disposition-changing finding.

### `ClickHouseSinkConfig.java` (+227) — every new config option, collisions checked

| New config key | Type / default | Purpose | Collides with fork's `targetTableFilter`/`jsonInsertColumns`? |
|---|---|---|---|
| `retryOnSocketException` | boolean, `false` | Retry the batch on `java.net.SocketException` (broken pipe/reset) instead of failing the task — matters more under client V2's persistent connections | No |
| `bufferCount` | int ≥0, `0` | Records to buffer before flush (0 = no buffering / original behavior) | No |
| `bufferFlushTime` | long ≥0 ms, `0` | Time-based flush bound, only effective when `bufferCount>0` | No |
| `clickhouseClientInsertTimeoutMs` | long ≥100 ms | Bounds a single insert's wait so a hung insert can't outlive `consumer.override.max.poll.interval.ms` | No |
| `reportInsertedOffsets` | boolean, `false` | `preCommit()` reports only actually-inserted offsets instead of echoing `currentOffsets` | No |
| `debeziumCDCEnabled` | boolean, `false` | Enables `DebeziumRecordConvertor` routing (see above) | No |
| `auto.evolve` | boolean, `false` | Auto `ALTER TABLE … ADD COLUMN IF NOT EXISTS` for new fields | No |
| `auto.evolve.ddl.refresh.retries` | int ≥0, `3` | Retries waiting for DDL propagation after evolution | No |
| `auto.evolve.struct.to.json` | boolean, `false` | Maps Connect `STRUCT` → ClickHouse `JSON` during auto-evolution | No |
| `errors.retry.timeout` | long ms, `60000` | Connect framework retry-timeout override, validated with a warning threshold | No |
| `clusterName` | string, `""` | Adds `ON CLUSTER '<name>'` to distributed DDL (auto-evolve's `ALTER TABLE`) | No |
| `enableReplicaPinning` | boolean, `false` | Pins retries to one ClickHouse replica via `X-ClickHouse-Replica-Tag` | No |
| `ssl_socket_sni` | string, `""` | Overrides TLS SNI hostname (proxy routing) | No |

**No name or intent collision with `targetTableFilter` or `jsonInsertColumns`** — confirmed by `grep -n -i "targetTableFilter\|jsonInsertColumns"` returning nothing in `ClickHouseSinkConfig.java`, `ClickHouseWriter.java`, or `ClickHouseHelperClient.java` at `v1.6.0`.

### `SinkTaskStatisticsMBean`, `TopicStatistics`, `TopicStatisticsMBean`, `SimpleMovingAverage`, `SinkTaskStatistics` (+163) — interaction with `connector=<name>`

Covered in row 8 above. To restate the concrete porting requirement: the fork's `connector=<name>` addition currently lives in `ProxySinkTask.buildMBeanName(String connectorName, int id, String version)` — a method that **no longer exists** at `v1.6.0` (the whole `getMBeanNAme()`/name-building responsibility moved into `SinkTaskStatistics.registerMBean()`/`getMBeanName(int taskId)`/`getTopicMBeanName(int taskId, String topic)`, which are new, per-topic-MBean-aware, and sourced from `Version.ARTIFACT_VERSION` — the connector's own build-time version, not the ClickHouse client jar's). Porting the fork's intent means:

1. Thread `connectorName` (already extracted as `props.get("name")` in the fork's `ClickHouseSinkTask.start()`) through to `new SinkTaskStatistics(id)`, which needs a new constructor parameter.
2. Rebuild `getMBeanName`/`getTopicMBeanName` to include `connector=%s` the way the fork's `buildMBeanName` does.
3. Re-decide the sanitization: `:` → `_` still matters if `connectorName` can contain a colon; `null` → `unknown` for version is now largely moot (`Version.ARTIFACT_VERSION` is a non-null compile-time constant) but may still be worth keeping defensively.
4. `MBeanNameUnitTest`/`MBeanRegistrationLiveTest`/`ProxySinkTask.buildMBeanName`-targeting tests all need re-targeting to the new home — this is a genuine "carry by intent, not by diff" case, same class of risk the milestone context already flags for `ClickHouseWriter.java`.

### `ClickHouseHelperClient.java` (+359) — table-filtering or describe-streaming overlap?

**None.** `showTables`/`showTablesV1`/`showTablesV2` build an unconditional `WHERE database = '%s' GROUP BY database, table` with no filter parameter anywhere (confirmed: no `match(`, no filter-clause building function in the file at `v1.6.0`). `describeTableV1`/`describeTableV2` only skip `isAlias()`/`isMaterialized()`/`isEphemeral()` columns and their sub-columns — no `.size0`/`.null`/JSON-Dynamic-structural skip (see row 6c). The `+359` lines are almost entirely: the `Table`/`Table.TableDesc` split (immutable `Table` built from a fully-populated column list rather than mutated in place), `alterTableAddColumns` (new, for `auto.evolve`), `retryOnSocketException`/`enableReplicaPinning`/`clusterName`/`ssl_socket_sni` plumbing, and `extractTablesMapping`'s new cache-freshness comparison against `Table.TableDesc.getNumColumns()`. None of it does what `targetTableFilter` does.

**Merge-risk note:** `Table`'s constructor signature changed completely (`Table(String database, String name, boolean hasDefaults, List<Column> columns, int numColumns)`, columns supplied up front and `addColumn` made `private`) — the fork's `targetTableFilter`/`shouldSkipDescribeRow` code, which calls `table.addColumn(column)` in a streaming loop and sets `table.hasDefaults(true)` via a fluent setter, cannot be patched onto this file; it must be rewritten against the new immutable-construction shape.

---

## New upstream capability at v1.6.0 — categorized for the fork's production CDC pipelines

**Must-absorb** (the fork loses correctness or a documented bug otherwise):
- `Utils.handleException` client-V2 `ServerException` fix — closes the fork's own "dead error classification on V2" Active follow-up for free (adopt wholesale, don't re-fix).
- `KeyToValue` stale-schema-cache fix — same bug class Phase 2 already fixed in the fork's own SMTs; adopting upstream's rewrite is strictly better and has zero fork-side cost (fork's own diff there was a no-op).
- Map-`Nullable`-value RowBinary null-marker fix, upstream `1.3.8` (issue/PR #687): *"Fixed RowBinary serialization for Map columns with Nullable value types. The nullable marker byte was missing when writing map values, causing CANNOT_READ_ALL_DATA errors for `Map(K, Nullable(V))` columns."* — **this is the exact same bug the fork's own Phase 2 (commit `3929ce6`) fixed independently**, confirmed by reading the matching code in `git show v1.6.0:.../ClickHouseWriter.java` (`if (col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null) { BinaryStreamUtils.writeNonNull(stream); }` — upstream additionally null-guards `col.getMapValueType() != null`, a small defensive improvement the fork's version lacks). **Action: drop the fork's duplicate patch during the merge and take upstream's version** — record this as a "closed for free" item per the milestone's out-of-scope instruction (do not re-verify the WR-01 null-entry follow-up's status without running the suite; it's out of this milestone's scope either way).
- `enableDbTopicSplit` multi-database fixes (`DESCRIBE TABLE` built from the wrong database, records silently dropped/misrouted across databases, connector refusing to start on an empty split database) — relevant if any fork production pipeline uses `enableDbTopicSplit=true`.

**Neutral** (opt-in, zero effect unless explicitly configured):
- `debeziumCDCEnabled` / `DebeziumRecordConvertor` — off by default, gated on schema name `.Envelope` AND the flag; no fork pipeline is silently rerouted through it.
- `auto.evolve` / `auto.evolve.ddl.refresh.retries` / `auto.evolve.struct.to.json` / `clusterName` — all off/empty by default.
- `bufferCount`/`bufferFlushTime` buffered delivery strategies — `bufferCount=0` by default, `DirectDeliveryStrategy` preserves pre-refactor behavior exactly.
- `retryOnSocketException`, `enableReplicaPinning`, `ssl_socket_sni`, `reportInsertedOffsets`, `clickhouseClientInsertTimeoutMs`, `errors.retry.timeout` — all opt-in or conservative defaults.
- JSON-as-string RowBinary writing (`input_format_binary_read_json_as_string`) — off by default; when off, upstream's default-path exception differs from the fork's TYPE-06 message (see above) but neither path executes unless a JSON column with a non-null value actually reaches RowBinary, which the fork's own JSON columns route through `insertFormat=json`/`bypassRowBinary` already.
- Per-topic JMX MBeans (`TopicStatistics`/`TopicStatisticsMBean`) — additive; doesn't remove or rename the existing per-task MBean, just adds new ones. Should get the `connector=<name>` treatment for consistency once ported (see above), but isn't required for correctness.

**Potentially breaking for the fork's production CDC pipelines — flag for the merge plan:**
- **JMX MBean name format changes even without `connector=<name>` re-application**: `version=%s` now sources from `Version.ARTIFACT_VERSION` (the connector's own build version, e.g. what the fork's `VERSION` file says) instead of the ClickHouse client jar's implementation version (`0.9.6`-style). Any external JMX scraping/dashboards keyed on the old version string format will see a different value after the merge — independent of whether `connector=<name>` gets re-applied.
- **`Table`'s immutable-construction API break**: any fork code outside `ClickHouseHelperClient`/`Table` itself that called the old `new Table(database, name)` / `table.addColumn(column)` / `table.hasDefaults(true)` fluent setters (the fork's `targetTableFilter`/`shouldSkipDescribeRow` describe-loop does exactly this) will not compile against `v1.6.0`'s `Table` — this is a required, non-optional rewrite, not a nice-to-have.
- **Batch grouping key changed** when `enableDbTopicSplit=true`: upstream's `1.4.0` changelog documents that batches are now correctly keyed per-database (previously records for `dbA.t`/`dbB.t` could collapse into one batch and all land in the first database) — a correctness fix, but a behavior change for any fork pipeline currently relying on (or unknowingly working around) the old collapsing behavior. Also documented: *"a batch left in the `BEFORE` state by an earlier version is not deduplicated on the first insert after upgrading"* for `exactlyOnce=true` users with `enableDbTopicSplit=true` — an operational note for the eventual production rollout, not this milestone, but worth carrying into that follow-up's context.
- **`ClickHouseSinkTask.start()`/`ProxySinkTask` constructor signature changes** (new `errorReporter`-carrying field, `statistics.registerMBean()` moved out of the constructor into a new `start()` method, `dbWriter.stop()` added to `stop()`) — mechanical but must be reconciled with the fork's `connectorName`-carrying constructor change; not a behavior risk once correctly merged, but a guaranteed merge-conflict site.

---

## Summary for the requirements step

- **Zero of the fork's four SMTs (`TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`) exist upstream in any form** — no naming collision, no native coverage, no upstream Debezium/auto-evolve feature does their job. Carry all four forward unchanged.
- **`targetTableFilter` and the JSON/Dynamic sub-column skip are fork-only, need re-application (not patching)** onto a substantially reshaped `Table`/`ClickHouseHelperClient`.
- **The metadata-free JSON insert path + `jsonInsertColumns` are entirely fork-only** — upstream's JSON insert still requires full table description; must be re-applied by intent onto the rewritten `ClickHouseWriter`.
- **`Type.JSON` mapping is now upstream-native and should be adopted from upstream** (brings extras); **`Type.DYNAMIC` has zero upstream coverage and must stay fork-only**, confirmed directly in `Type.java`.
- **`connector=<name>` JMX naming is fork-only and the code it lived in was deleted** — must be reimplemented against the new `SinkTaskStatistics`-owned naming, alongside a version-source semantic change (client-jar version → connector build version) that's worth flagging regardless of what happens to `connector=<name>`.
- **`async_insert` non-forcing needs zero action** — untouched upstream.
- **Two of the fork's own bug fixes (Map-`Nullable` null marker, and by extension the dead-error-classification issue) are already fixed upstream** — the merge should take upstream's versions and drop the fork's duplicate patches, closing two items from the milestone's Active follow-ups list "for free" (record the fact per the milestone instruction; do not additionally fix anything here).
- **`KeyToValue`'s rewrite is a pure win** — the fork's own diff there is a no-op, upstream's version fixes the same bug class the fork independently found elsewhere, and it's confirmed compile-safe against the fork's actual Kafka Connect API 4.1.1 dependency.
- **Zero new upstream config option collides by name or intent with `targetTableFilter`/`jsonInsertColumns`.**
- **`debeziumCDCEnabled`/`DebeziumRecordConvertor` is a large, genuinely new, safe-by-default capability** worth a line in Requirements as "available, not adopted this milestone" — it does not conflict with or replace any of the eight fork features.

## Sources

- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java` / `git show custom_build:...Type.java`
- `git diff main...v1.6.0 -- src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` and `git show v1.6.0:...Column.java`
- `git diff main...v1.6.0 -- src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` and `git show custom_build:src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` and `git diff main...custom_build -- .../ClickHouseWriter.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/data/convert/DebeziumRecordConvertor.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java`
- `git diff main...v1.6.0 -- src/main/java/com/clickhouse/kafka/connect/transforms/KeyToValue.java` and `git diff main...custom_build -- .../KeyToValue.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/util/Utils.java` and `git diff main...custom_build -- .../Utils.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/{DeliveryStrategy,DirectDeliveryStrategy,ChunkFlusher}.java`
- `git diff main...v1.6.0 -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` and `git show v1.6.0:...ClickHouseSinkConfig.java`
- `git diff main...v1.6.0 -- src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` and `git diff main...custom_build -- .../ProxySinkTask.java`
- `git show v1.6.0:src/main/java/com/clickhouse/kafka/connect/util/jmx/{SinkTaskStatistics,MBeanServerUtils}.java`
- `git show v1.6.0:build.gradle.kts` (Version.java generation, `generateVersionFile` task) and `git show v1.6.0:gradle/libs.versions.toml` (`kafka = "2.7.0"`)
- `git show v1.6.0:CHANGELOG.md` (release notes 1.3.0 through 1.6.0, quoted directly for the KeyToValue, Map-Nullable, handleException, and enableDbTopicSplit fixes)
- Direct jar inspection: `unzip -l ~/.gradle/caches/modules-2/files-2.1/org.apache.kafka/kafka-clients/4.1.1/.../kafka-clients-4.1.1.jar | grep -i common/cache` — confirms `org.apache.kafka.common.cache.{Cache,LRUCache,SynchronizedCache}` present in the fork's actual resolved dependency

---
*Feature research for: ClickHouse Kafka Connect fork vs. upstream v1.6.0 sync*
*Researched: 2026-09-18*
