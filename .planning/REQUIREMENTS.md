# Requirements: v1.6.0_gg1-sync — Upstream v1.6.0 Sync

**Defined:** 2026-09-18
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

**Milestone goal:** Merge upstream `v1.6.0` (581 commits past the fork's `v1.2.9` base) into `custom_build`, preserving every fork feature or consciously retiring it in favour of an upstream equivalent, with the fork's test suite as the acceptance gate.

**Locked decisions this milestone is built on** (settled during questioning — plans do not re-open these):

| Decision | Choice |
|---|---|
| Sync strategy | `git merge v1.6.0` into `custom_build` — history preserved, no rebase, no replay |
| Steps | Big-bang straight to `v1.6.0`, not stepwise through `v1.3`/`v1.4`/`v1.5` |
| Conflict policy | Prefer upstream **wherever its implementation passes the fork's tests**; shrinking the fork delta is an explicit goal. Where upstream's version cannot pass (JMX naming), the policy does not apply — keep the fork's |
| `build.gradle.kts` | Take upstream's wholesale (version catalog + `testFixtures`), re-apply the fork's additions on top |
| Upstream tests | Accept all 56 files; run only ClickHouse `latest` × client V2; cloud-dependent ones stay skipped |
| Done | `./gradlew test` green locally **+** `tests.yaml` green on the pushed SHA **+** `custom_build` pushed to the public fork **+** `VERSION` = `v1.6.0_gg1` |
| Kafka Connect API | Keep the fork's `4.1.1` (upstream's catalog is still `2.7.0`) — carries the LZ4 `resolutionStrategy` with it |
| Testcontainers | Accept upstream's `2.0.5` (major bump from `1.21.4`), with its own verification checkpoint |
| JUnit Jupiter | Take upstream's `5.14.4`; delete the fork's vestigial `5.11.3` direct pin rather than porting it |
| RowBinary + JSON | Adopt upstream's flag-gated write-as-string for `JSON`; keep the fork's fail-fast throw for `DYNAMIC`/`UNKNOWN` |
| R-01 | Decided at a blocking-human checkpoint inside the JSON-path phase, on evidence — not pre-decided here |
| The nine deferred follow-ups | Out of scope. Two close for free; record the fact, do not fix |
| Production rollout | Out of scope |

---

## v1 Requirements

### Inventory & Rollback (SYNC)

- [ ] **SYNC-01**: Upstream `ClickHouse/clickhouse-kafka-connect` is configured as a named git remote in this repo, so future syncs do not depend on ad-hoc `git fetch <url>` invocations
- [ ] **SYNC-02**: The pre-merge tip of `custom_build` is tagged (e.g. `pre-v1.6.0-sync`) as an unambiguous rollback point, and the merge is performed on a scratch branch so the already-published `custom_build` never requires a force-push
- [ ] **SYNC-03**: The `git merge-tree --write-tree custom_build v1.6.0` conflict inventory is captured as a checked-in, per-file resolution checklist — 40 conflict messages (35 `content` + 5 `modify/delete`) across 39 unique paths, each marked with its resolution shape: survives as-is / re-express / redundant
- [ ] **SYNC-04**: The resolved ClickHouse Java client version from upstream's `gradle/libs.versions.toml` is recorded explicitly and compared against what the fork runs in production, so a wholesale build adoption cannot silently bump it
- [ ] **SYNC-05**: A production-config audit is recorded answering whether any live connector combines `enableDbTopicSplit=true`+`exactlyOnce=true` (upstream documents a one-time dedup gap on upgrade) or `bufferCount>0`+`exactlyOnce=true` (now rejected at startup unless `bufferFlushTime=0` and `ignorePartitionsWhenBatching=false`)
- [ ] **SYNC-06**: The merge lands as a single identifiable merge commit with no `.planning/` churn interleaved into conflict resolution — no committed revision in the merge range contains a literal `<<<<<<<` conflict marker in tracked source

### Build Migration (BLD)

- [ ] **BLD-01**: `build.gradle.kts` and `gradle/libs.versions.toml` come from upstream `v1.6.0` (version catalog, `jvm-test-suite`, `java-test-fixtures`, `protobuf`, `com.gradleup.shadow`), with the dead `signing` and `buildconfig` plugins dropped
- [ ] **BLD-02**: The Gradle wrapper is at `9.6.1`, matching the version upstream authored and tested the new build file against
- [ ] **BLD-03**: JaCoCo is re-applied — `jacocoTestReport` plus the 15-class `jacocoForkReport`, report-only with no failing threshold, and `.github/scripts/jacoco_summary.py` still renders both XML reports without edits
- [ ] **BLD-04**: `testUnit` is re-applied — `excludeTags("clickhouse")`, JaCoCo disabled on it, and kept out of `check` via the `if (name != "testUnit")` guard
- [ ] **BLD-05**: The test JVM is pinned to JDK 17 by an explicit `javaLauncher`/toolchain mechanism — not merely by `options.release` — and runs with `file.encoding=windows-1252`, so a machine invoking Gradle on a different JDK cannot silently change what the suite exercises
- [ ] **BLD-06**: The fork's two undocumented build additions survive: the LZ4 `withCapability("org.lz4:lz4-java")` resolution and the `com.github.ben-manes.versions` plugin with its `dependencyUpdates` configuration
- [ ] **BLD-07**: `VERSION` reads `v1.6.0_gg1`
- [ ] **BLD-08**: Dependency versions are reconciled deliberately — upstream's for Jackson, HttpClient5, Lombok, Mockito, OkHttp, `org.json`, slf4j and JUnit Jupiter; the fork's for the ClickHouse client (`0.9.6`) and Kafka Connect API (`4.1.1`); `slf4j-reload4j` kept as a literal coordinate since upstream's catalog has no entry for it

### Type & Mapping Layer (MAP)

- [ ] **MAP-01**: `Type.DYNAMIC` exists alongside upstream's native `Type.JSON`, and `Column` dispatches `Dynamic` type strings to it — a `Dynamic` column must not silently become `Type.UNKNOWN`
- [ ] **MAP-02**: Upstream's rewritten `Column.java` is adopted for everything else, including its `SimpleAggregateFunction` unwrapping and Avro/Protobuf union handling, with `TYPE-01..05`'s JSON assertions passing against it
- [ ] **MAP-03**: Upstream's immutable `Table` is adopted as-is (`addColumn()` private, columns supplied to the constructor), with every fork call site rewritten against the new shape rather than the constructor being re-opened

### Writer (WRT)

- [ ] **WRT-01**: `doWriteColValue`'s `default:` branch throws the fork's actionable `DataException` instead of upstream's silent `LOGGER.error`, so an unsupported RowBinary type cannot desync the stream
- [ ] **WRT-02**: `validateDataSchema`'s fail-fast gate covers `DYNAMIC` and `UNKNOWN`; `JSON` is removed from the gate in favour of upstream's native `case JSON:` path
- [ ] **WRT-03**: Upstream's flag-gated JSON-as-string RowBinary write is available, and `TYPE-06`'s JSON assertions are rewritten to match its exception type and message rather than the fork's
- [ ] **WRT-04**: The fork's duplicate `Map(K, Nullable(V))` null-marker patch is dropped in favour of upstream's version, which carries an extra `getMapValueType() != null` guard — the fork's pinning test stays and still passes
- [ ] **WRT-05**: `setTargetTableFilter(...)` still reaches the helper-client builder from `ClickHouseWriter.start()`

### Helper Client (HLP)

- [ ] **HLP-01**: `targetTableFilter` still restricts table discovery server-side via `match(table, '<regex>')`, re-expressed against upstream's `showTables*` methods that return `List<Table.TableDesc>` — `FILT-01..04` green
- [ ] **HLP-02**: `shouldSkipDescribeRow`'s structural rule (`.null`, `.size0`, `.size`, and children of `JSON`/`Dynamic` columns) is re-expressed against upstream's local-`List<Column>` describe loop — `DESC-01..04` green, and the bare `.size` case that upstream's `.size[0-9]+` regex misses is covered
- [ ] **HLP-03**: `numColumns` is incremented before any skip decision, matching upstream's treatment of alias/materialized/ephemeral columns, so `extractTablesMapping`'s cache-staleness comparison stays correct
- [ ] **HLP-04**: Every `InputStreamReader` in the resolved `ClickHouseHelperClient` specifies `StandardCharsets.UTF_8` — verified by grepping the whole resolved file, not just the two call sites the fork originally patched — with `TYPE-07` green under the suite's `windows-1252` JVM

### Configuration (CFG)

- [ ] **CFG-01**: `targetTableFilter` and `jsonInsertColumns` survive as config keys with their parsing contracts intact
- [ ] **CFG-02**: The connector sends no `async_insert` setting by default — upstream's `addClickHouseSetting("async_insert", "0", false)` stays commented out, `ASYNC-01..03` green
- [ ] **CFG-03**: All thirteen new upstream config options are present and reach their consumers at their upstream defaults

### Orchestration & JMX (ORCH)

- [ ] **ORCH-01**: `ProxySinkTask` and `ClickHouseSinkTask` are resolved against upstream's new layering — `DeliveryStrategy`/`ChunkFlusher` above, the 5-arg `Processing` construction below — with no stale 4-arg call site anywhere
- [ ] **ORCH-02**: The JMX MBean ObjectName carries `connector=<name>` again, re-plumbed from `ClickHouseSinkTask.start()` into `SinkTaskStatistics` where upstream now owns naming, with colon sanitization retained — `JMX-01..04` green against the new home
- [ ] **ORCH-03**: Upstream's new per-topic MBean gets the same `connector=<name>` treatment, so per-topic metrics are attributable to a connector in a multi-connector worker
- [ ] **ORCH-04**: `Utils.java` and `Processing.java` — which merge with no conflict marker despite both sides touching them — are explicitly reviewed rather than skipped, and their conflicting test files (`UtilsTest`, `ProcessingTest`) take upstream's versions

### Metadata-free JSON Path (JSONP)

- [ ] **JSONP-01**: The keep-or-drop decision for the metadata-free `insertFormat=json` path is made at a blocking-human checkpoint, on evidence from the merged code, and recorded with its rationale — not left as whatever the merge happened to produce
- [ ] **JSONP-02**: `JSON-01..05` and R-01's labelled test reflect that decision: if kept, the bypass works and R-01's loss is still pinned; if dropped, R-01's test failing is recorded as the wanted signal that the loss is gone
- [ ] **JSONP-03**: `jsonInsertColumns` works again, redesigned as a field filter inside upstream's `writeRecordsJson` — the `client.insert(table, columns, data, ...)` overload it used no longer exists at that call site — with `COLS-01..06` green
- [ ] **JSONP-04**: No fork-only JSON-path code survives against a `Table` shape upstream no longer produces — verified by reading the resolved diff, not only by a green test

### Test Harness (HARN)

- [ ] **HARN-01**: `ClickHouseBase` carries `@Tag("clickhouse")` again, proven by `./gradlew testUnit` completing with Docker stopped
- [ ] **HARN-02**: `ClickHouseBase`'s `@AfterEach` MBean leak sweep is restored and extended to cover upstream's per-topic MBeans, with a full-suite run showing zero `InstanceAlreadyExistsException`
- [ ] **HARN-03**: `uniqueConnectorName(TestInfo)` is available again, and the per-JVM-fork shared-container strategy is re-derived over upstream's `@TestInstance(PER_CLASS)` shape — or, if that proves incompatible, the cost is measured and the fallback recorded
- [ ] **HARN-04**: Exactly one `ClickHouseTestHelpers.java` exists in the tree, under `src/testFixtures`, carrying the fork's helpers, with every fork test import updated — verified by grep, not by compilation
- [ ] **HARN-05**: `SchemaTestData` / `SchemalessTestData` exist only under `src/testFixtures` (the `src/integrationTest` duplicates deleted), and `FromVersionConditionExtension`'s package move is absorbed
- [ ] **HARN-06**: `./gradlew compileIntegrationTestJava` succeeds, so the out-of-scope integration source set does not rot into a permanently broken state
- [ ] **HARN-07**: Testcontainers `2.0.5` is verified to start a ClickHouse container through `ClickHouseBase` before any of the 371 fork tests are adapted

### Suite & CI (SUITE)

- [ ] **SUITE-01**: `./gradlew test` is green against ClickHouse `latest` × client V2 — the fork's tests plus upstream's 56 test files — with every skip accounted for and explained
- [ ] **SUITE-02**: The four fork SMTs (`TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`) and their sink-landing tests pass untouched; any failure there is investigated as a genuine defect, since upstream touches nothing in that package
- [ ] **SUITE-03**: A written record lists every fork change dropped in favour of upstream and why, and shows the fork's delta over upstream is smaller after this milestone than before it
- [ ] **CI-05**: `tests.yaml` runs the merged suite on push and publishes coverage, with its runtime measured against the job's `timeout-minutes: 45`
- [ ] **CI-06**: A `tests.yaml` run is machine-verified green on the pushed SHA, located by `head_sha` plus workflow `path` and asserting exactly one match
- [ ] **CI-07**: `custom_build` is pushed to the public fork after human approval, by a non-force push from the scratch branch

### Free Wins — Verify and Record (FREE)

<!-- These are OUT of scope as work. The requirement is to confirm and record, never to fix. -->

- [ ] **FREE-01**: Upstream's `Utils.handleException` is confirmed to classify `com.clickhouse.client.api.ServerException` alongside `ClickHouseException`, and the fork's "error classification is dead code on client V2" follow-up is closed citing upstream issue #818 as the mechanism
- [ ] **FREE-02**: Upstream's `Map(K, Nullable(V))` RowBinary fix is confirmed in the merged code, the fork's duplicate patch is confirmed dropped, and whether it also closes WR-01 is recorded either way
- [ ] **FREE-03**: Whether upstream issue #580's `DESCRIBE TABLE`-database fix subsumes the fork's dormant P3-WR-02 follow-up is checked and recorded

---

## Future Requirements

Deferred — tracked, not in this roadmap.

### Follow-ups carried from v1.2.9_gg4-tests

- **R-01 fail-fast** — a real fix for the silent-data-loss path, if JSONP-01 decides to keep the bypass. Needs a migration story for existing Struct-converter pipelines
- **WR-01** — end-to-end coverage for an actual `null` entry inside a `Map(K, Nullable(V))` column, unless FREE-02 shows upstream closed it
- **WR-02** — `ClickHouseWriter` MAP/ARRAY branches failing loudly when a `null` lands in a non-nullable element slot
- **WR-03** — pin `HeaderToValue`'s behavior when `fields` names a pre-existing non-STRING column
- **P3-WR-01** — `shouldSkipDescribeRow` walking more than one level up for deeply nested `JSON`/`Dynamic` children
- **P3-WR-03** — `validateDataSchema`'s gate covering `INT128`/`INT256`/`UINT128`/`UINT256`
- **P3-WR-02** — `describeTableV2` using the passed-in `database` parameter, unless FREE-03 shows upstream closed it
- **P4-WR-01** — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally` task-lifecycle pattern
- **P4-IN-01** — `AsyncInsertLiveTest`'s misleading class Javadoc

### New upstream capability, available but not adopted

- **`debeziumCDCEnabled` / `DebeziumRecordConvertor`** — flattens a Debezium envelope into a `ReplacingMergeTree(_version, is_deleted)` row. Off by default, gated on a `.Envelope` schema name. Does not overlap the fork's SMTs; potentially valuable to the fork's CDC pipelines, but adopting it is a separate decision
- **`auto.evolve`** family — automatic `ALTER TABLE … ADD COLUMN IF NOT EXISTS`, with `clusterName` for distributed DDL
- **`bufferCount` / `bufferFlushTime`** buffered delivery strategies
- **`retryOnSocketException`** — worth deliberately enabling, since it targets a client-V2 persistent-connection failure mode the fork is exposed to today with no mitigation

---

## Out of Scope

| Item | Reason |
|---|---|
| Fixing any of the nine deferred follow-ups | Mixing 581 upstream commits with production bug fixes makes it impossible to tell which one broke what. Two close for free; record, do not fix |
| Production rollout of the v1.6.0-based connector | The milestone ends at a green suite, green CI, and a pushed branch |
| Stepwise sync through `v1.3.x` / `v1.4.0` / `v1.5.0` | User chose big-bang. The tests shipped by the previous milestone are the safety net that makes it affordable |
| Rebasing or replaying fork history onto `v1.6.0` | `custom_build` is already published; merge preserves history without a force-push |
| Test matrix beyond ClickHouse `latest` × client V2 | Unchanged constraint — production uses V2, and older ClickHouse lacks JSON/Dynamic |
| Upstream's cloud and cluster test jobs, and the `use-clickhouse-java` composite action | No cloud credentials, no cluster deployment; the fork's single-job CI stands |
| Making `src/integrationTest` pass | Confluent/Cloud-dependent and outside `./gradlew test`. Required only to compile (HARN-06) |
| Adopting `DebeziumRecordConvertor`, `auto.evolve`, or buffered delivery | Real new upstream capability, but adopting it is feature work, not sync work |
| Replacing `slf4j-reload4j` | Pre-existing upstream-hardening item, untouched by this sync |

---

## Traceability

Populated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| *(pending roadmap)* | | |

**Coverage:**
- v1 requirements: 46 total
- Mapped to phases: 0
- Unmapped: 46 ⚠️

---
*Requirements defined: 2026-09-18*
*Last updated: 2026-09-18 after initial definition*
