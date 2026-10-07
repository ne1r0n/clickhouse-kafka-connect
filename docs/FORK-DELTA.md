# Introduction

This document records what this fork of `ClickHouse/clickhouse-kafka-connect` still patches on top of upstream, and what it gave up in favour of upstream's own implementation when `v1.6.0` was merged in. It is written for whoever runs the next upstream sync: it says which behaviours exist only here, where they live, why upstream does not supply them, and which test or requirement pins each one, so that a merge which silently drops one of them is noticed.

Every row below was checked against the merged tree (`sync/v1.6.0`, commit `09cae191`; the `network_compression_method` row was added at its fix commit `87fa1e78`) by searching the code for the construct the row describes. A row that claims a patch survives was only recorded after the patch was found in the source; a row that claims a patch was retired was only recorded after the fork's version was found to be absent. The rows describe what the code contains, not what a plan intended.

## Contents

- [What this fork still patches](#what-this-fork-still-patches)
  - [Connector source](#connector-source)
  - [Build, dependencies and CI](#build-dependencies-and-ci)
  - [Test harness](#test-harness)
- [What the fork retired in favour of upstream](#what-the-fork-retired-in-favour-of-upstream)
  - [Retired in favour of an upstream implementation](#retired-in-favour-of-an-upstream-implementation)
  - [Adopted wholesale](#adopted-wholesale)
- [The raw numbers](#the-raw-numbers)
- [Known costs and open items](#known-costs-and-open-items)
- [Named skip inventory](#named-skip-inventory)
- [For the next syncer](#for-the-next-syncer)

## What this fork still patches

Rows are grouped by area and, inside each group, ordered by file path and then by position in the file. The last column names the requirement ID from the sync milestone, or the numbered Recorded Fact in `.planning/MERGE-CHECKLIST.md`, that pins the behaviour, together with the test classes that exercise it.

### Connector source

| Behaviour | Where it lives | Why upstream does not provide it | Pinned by |
|---|---|---|---|
| `targetTableFilter` and `jsonInsertColumns` are connector config keys, parsed and trimmed, alongside a `jsonInsertFormatExplicit` flag that is true only when `insertFormat` was set to `json` explicitly | `sink/ClickHouseSinkConfig.java` | Upstream has no table-name filter and no JSON column-list option. The explicit flag exists because upstream's unset-`insertFormat` default branch also resolves to the JSON enum constant, so keying on the enum value would switch the bypass on for every default-configured connector | CFG-01; `ClickHouseSinkTaskMappingTest`, `JsonInsertColumnsUnitTest`, `ClickHouseHelperClientForkLiveTest` |
| No `async_insert` setting is sent by default: the `addClickHouseSetting("async_insert", "0", false)` call is commented out | `sink/ClickHouseSinkConfig.java` | Upstream forces `async_insert=0` on every insert. This fork sends no such setting, so the server's own `async_insert` setting stays in charge | CFG-02; `AsyncInsertSettingsUnitTest`, `AsyncInsertLiveTest` |
| The connector name is read from `props.get("name")` in `start()` and threaded through `ProxySinkTask` into `SinkTaskStatistics`, so the JMX `ObjectName` carries a `connector=<name>` segment, at task level and for upstream's per-topic MBean | `sink/ClickHouseSinkTask.java`, `sink/ProxySinkTask.java`, `util/jmx/SinkTaskStatistics.java` | Upstream's MBean name has no connector segment, so two connectors in one worker are indistinguishable in a JMX scraper. Rendered names before and after are in Recorded Fact 11. The `version=` segment now comes from the build-generated `Version.ARTIFACT_VERSION` (currently `v1.6.0_gg1`), so it changes on every `VERSION` bump | ORCH-02, ORCH-03; Recorded Facts 4 and 11; `MBeanNameUnitTest`, `MBeanRegistrationLiveTest` |
| `onPartitionsRevoked(Collection<TopicPartition>)` is kept without its `@Override` annotation | `sink/ClickHouseSinkTask.java` | The fork pins Kafka Connect API `4.1.1`, which removed this method from `SinkTask` in Kafka 3.0, while upstream still declares it under its own `2.7.0` pin. Keeping the annotation would not compile. The method still binds if a pre-3.0 worker loads the connector. Re-verify this if the Connect API pin ever changes | Recorded Fact 8 |
| The metadata-free `insertFormat=json` path: when `insertFormat` is explicitly `json`, the writer skips `DESCRIBE`/`SHOW`, builds a synthetic column-less `Table`, and posts the records as JSON. One predicate, `shouldSkipTableMetadata()`, is read at five guard sites: `start()`, `updateMapping()`, the `auto.evolve` branch of `doInsert`, `cleanupExtraFields`, and `getTable` | `sink/db/ClickHouseWriter.java` | Upstream always resolves table metadata first. The bypass is the only way to run against a user that has no `DESCRIBE`/`SHOW` grant, which none of `suppressTableExistenceException`, `enableDbTopicSplit` or `bypassFieldCleanup` reaches. It carries a known silent-data-loss route, listed first under [Known costs and open items](#known-costs-and-open-items) | JSONP-01 to JSONP-04; Recorded Fact 14; `MetadataFreeJsonInsertUnitTest`, `ExplicitJsonInsertPathLiveTest`, `ClickHouseSinkTaskJsonInsertFormatTest` |
| `jsonInsertColumns` reaches the insert: when the list is non-empty, `doInsertJson` calls the `client.insert(table, columns, writer, format, settings)` overload instead of the column-less one | `sink/db/ClickHouseWriter.java` | Upstream never sets an explicit column list on a JSON insert | JSONP-03; `JsonInsertColumnsUnitTest`, `JsonInsertColumnsLiveTest` |
| RowBinary fails fast for `DYNAMIC` and `UNKNOWN` columns: `validateDataSchema` throws a `DataException` naming the column, its type and the two escape hatches (`insertFormat=json`, `bypassRowBinary=true`), and the `default:` branch of `doWriteColValue` throws the same message instead of upstream's logged error | `sink/db/ClickHouseWriter.java` | Upstream logs and continues, which can desynchronise the RowBinary stream. `JSON` is deliberately not in this gate: upstream's flag-gated JSON-as-string write handles it | WRT-01, WRT-02; `RowBinaryUnsupportedTypeGateTest`, `RowBinaryFailFastLiveTest` |
| `doWriteColValue` writes a null marker instead of dereferencing a genuine null for a `Nullable` child of an Array or Map: the `MAP`, `TUPLE` and `VARIANT` cases each carry an early null guard (CR-02). Since the v1.7.0 sync the `MAP` guard fires only for `Nullable` columns (`value.getObject() == null && col.isNullable()`), so a null optional Map into a non-nullable Map column writes upstream #820's empty map (`00`, or `00 00` with defaults) | `sink/db/ClickHouseWriter.java` | Upstream v1.7.0 handles a null only for the `ARRAY` case and for a non-nullable `MAP` (#820). Its `TUPLE` case calls `getClass()` on `value.getObject()` and its `VARIANT` case calls `values()` on it, each with no null check, so a null `Nullable(Tuple(...))` or `Nullable(Variant(...))` entry reached as a child throws a `NullPointerException`. The fork's `MAP` guard was unconditional until the sync, and it shadowed #820's block: a non-nullable null Map wrote its size byte and then a second, stray marker byte, which desynchronises the RowBinary stream | WRT-06, WRT-09; `NestedNullableCompositeWriteTest`; upstream's `nullMapTypeTest`; Recorded Facts 22 and 23 |
| The server-side table filter: `buildTableFilterClause` appends `AND match(table, '<regex>')` to both table-discovery queries, with backslash-run and quote escaping, and `ClickHouseClientBuilder.setTargetTableFilter` carries the setting into the helper client | `sink/db/helper/ClickHouseHelperClient.java` | Upstream lists every table in the database; the fork restricts discovery to matching tables on the server. Re-expressed over upstream's `List<Table.TableDesc>` signatures | HLP-01, WRT-05; `ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest` |
| `shouldSkipDescribeRow` drops structural `DESCRIBE` rows (`.null`, `.size0`, bare `.size`, and children of `JSON`/`Dynamic` columns) before they reach `Table`, at both describe call sites; `numColumns` is still incremented before the skip decision | `sink/db/helper/ClickHouseHelperClient.java` | Upstream's `Table` filters only `.null` and `.size[0-9]+` children, which misses the bare `.size` case, and it has no rule for `JSON`/`Dynamic` children | HLP-02, HLP-03; Recorded Fact 13; `ClickHouseHelperClientForkUnitTest`, `TableTest` |
| The describe-table reader decodes the response with an explicit `StandardCharsets.UTF_8` | `sink/db/helper/ClickHouseHelperClient.java` | Decoding must not depend on the JVM's default charset, which the test suite deliberately sets to `windows-1252`. There is exactly one `InputStreamReader` in `src/main`, and it is explicit | HLP-04; `ClickHouseHelperClientForkLiveTest`, `JsonDynamicRoundTripTest` |
| `Type.DYNAMIC` exists beside upstream's native `Type.JSON`, and `Column.extractColumn` dispatches type strings starting with `Dynamic` to it | `sink/db/mapping/Column.java`, `sink/db/mapping/Type.java` | Upstream maps `Dynamic` to nothing, so such a column would silently become `UNKNOWN` | MAP-01; `JsonDynamicTypeMappingTest` |
| Four Single Message Transforms, registered in the `Transformation` service file: `ConvertToMapOrJson`, `HeaderToValue` (with `HeaderToValueConfig`), `HeadersToValue` and `TableOperationFilter` | `transforms/ConvertToMapOrJson.java`, `transforms/HeaderToValue.java`, `transforms/HeaderToValueConfig.java`, `transforms/HeadersToValue.java`, `transforms/TableOperationFilter.java`, `resources/META-INF/services/org.apache.kafka.connect.transforms.Transformation` | They exist only in this fork; upstream's `transforms` package holds `ExtractTopic` and `KeyToValue`. The fork uses them in its CDC (Debezium) pipelines into ClickHouse | SUITE-02; `SmtSinkLandingTest`, `ConvertToMapOrJsonTest`, `HeaderToValueTest`, `HeadersToValueTest`, `TableOperationFilterTest` |

### Build, dependencies and CI

| Behaviour | Where it lives | Why upstream does not provide it | Pinned by |
|---|---|---|---|
| A single-job CI workflow: ClickHouse `latest` with client V2 only, no job-level version pin, `timeout-minutes: 45`, a JaCoCo summary step and artifact upload | `.github/workflows/tests.yaml` | Upstream runs a client-by-server-version matrix plus cloud and cluster jobs. This fork has no cloud credentials or cluster, and production uses client V2 | CI-05; Recorded Fact 15 |
| `VERSION` reads `v1.6.0_gg1` | `VERSION` | The fork carries its own release suffix; `generateVersionFile` turns it into `Version.ARTIFACT_VERSION`, which feeds the JMX `version=` segment | BLD-07; Recorded Fact 4 |
| Five fork build additions, re-applied onto upstream's version-catalog `build.gradle.kts`: JaCoCo (`jacocoTestReport` plus the 15-class `jacocoForkReport`, report-only, no threshold); the `testUnit` task, which excludes the `clickhouse` tag, disables JaCoCo and is kept out of `check`; a JDK 17 `javaLauncher` for the test JVM together with `file.encoding=windows-1252`; the `com.github.ben-manes.versions` plugin with its `dependencyUpdates` configuration; and the Spotless `importOrder()` plus `removeUnusedImports()` block with its `removeUnusedImports` task | `build.gradle.kts` | None of these exist upstream. The Spotless block is planner decision PD-01, kept because the repository's own conventions document both the block and the task | BLD-03, BLD-04, BLD-05, BLD-06; Recorded Facts 2 and 7 |
| `org.slf4j:slf4j-reload4j:2.0.17` is declared as a literal coordinate | `build.gradle.kts` | Upstream's version catalog has no entry for it | BLD-08; Recorded Fact 2 |
| Fork dependency pin: Kafka Connect API `4.1.1` (upstream `2.7.0`), declared `api(...)`. ClickHouse client `0.9.6` is upstream's own value since v1.7.0 and is no longer a fork override | `gradle/libs.versions.toml`, `build.gradle.kts` | Production runs the shaded jar built here, so `4.1.1` is the production Connect API version. It is also why `onPartitionsRevoked` has no `@Override` (see the row above) | BLD-08, SYNC-04, BLD-13; Recorded Facts 1, 2 and 20 |

### Test harness

| Behaviour | Where it lives | Why upstream does not provide it | Pinned by |
|---|---|---|---|
| The `ClickHouseBase` contract: `@Tag("clickhouse")`, one lazily started ClickHouse container per test JVM fork behind a counted start site (`sharedContainerStarts()`), a per-class opt-out through `usesSharedContainer()`, the `@AfterEach` MBean leak sweep, and `uniqueConnectorName(TestInfo)` | `src/test/.../sink/ClickHouseBase.java` | Upstream starts one container per test class and has no tag, sweep or unique-name helper. Sharing makes the per-class database drop at `tearDown()` the only isolation between classes, which is an accepted cost recorded under [Known costs and open items](#known-costs-and-open-items) | HARN-01, HARN-02, HARN-03; Recorded Fact 16; `ClickHouseTestHarnessTest`, `ClickHouseHarnessOptOutContainerTest` |
| `CLICKHOUSE_VERSION_DEFAULT` is the floating `latest` tag (named `CLICKHOUSE_VERSION_LATEST`), with an always-pull policy applied to floating tags only, and a Docker-free canary asserts the default value | `src/testFixtures/.../helper/ClickHouseTestHelpers.java`, `src/test/.../sink/ClickHouseVersionDefaultUnitTest.java` | The merge replaced the default with a pinned server version and raised no conflict and no compile error, so the suite ran against a server nothing named. The canary, not the constant, is the thing that must survive a sync | Recorded Fact 15; `ClickHouseVersionDefaultUnitTest` |
| The describe and query helpers read responses with an explicit `StandardCharsets.UTF_8` (three sites) | `src/testFixtures/.../helper/ClickHouseTestHelpers.java` | Same encoding hazard as the connector reader above | HLP-04 |

## What the fork retired in favour of upstream

This is the claim the sync milestone exists to support: that the fork carries less of its own code than before. Each row names the retired patch, what upstream supplies in its place, and the mechanism. The fork's version was confirmed absent from the merged tree for every row in the first group.

### Retired in favour of an upstream implementation

| Retired patch | What upstream supplies now | Mechanism | Pinned by |
|---|---|---|---|
| Duplicate `Map(K, Nullable(V))` null-marker write in `ClickHouseWriter` | One guarded write: `col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null`. The construct occurs exactly once in the file. The fork's own patch lacked the `getMapValueType() != null` guard | The mechanical merge dropped the fork's copy. The fork's pinning test stays and passes against upstream's guard. An actual `null` map entry is still not covered end to end, see WR-01 (map) below | WRT-04, FREE-02; Recorded Fact 9; `ClickHouseWriterTest` |
| Client V2 error-classification workaround | `Utils.isServerError` accepts both `ClickHouseException` (client V1) and `ServerException` (client V2), and `handleException` reads the error code from whichever it is. `Utils.java` differs from upstream only in import order | Upstream issue #818. No fork patch exists in `Utils.java` | FREE-01; Recorded Fact 12; `UtilsTest` |
| The `DESCRIBE TABLE` database fix in `describeTableV2` | Both `describeTableV1` and `describeTableV2` build the query from the method's own `database` parameter. No describe query uses the instance field | Nothing needed fixing: upstream already builds from the parameter, which closes the fork's dormant follow-up | FREE-03; Recorded Fact 10 |
| `ProxySinkTask.buildMBeanName` as the owner of MBean naming | `SinkTaskStatistics` owns naming. The method is absent from `ProxySinkTask`; `SinkTaskStatistics.buildMBeanName` is its byte-for-byte restoration with the connector segment | The carrier method was deleted upstream and re-plumbed. The `connector=` segment itself is still a fork patch, listed above | ORCH-01, ORCH-02; Recorded Fact 11 |
| The fork's own JSON type dispatch in `Column` | `Column` dispatches `JSON` type strings to upstream's `Type.JSON`, which exists in upstream's enum. The only fork line in `Column` is the `Dynamic` dispatch | Upstream already had the JSON dispatch | MAP-01, MAP-02 |
| The vestigial direct pin `org.junit.jupiter:junit-jupiter-api:5.11.3` | JUnit Jupiter `5.14.4` through the version catalog's BOM. The `5.11.3` string appears nowhere in `build.gradle.kts` or `gradle/` | Deleted rather than ported | BLD-08 |
| The `signing` plugin and the `com.github.gmazzo.buildconfig` plugin | Neither appears in `build.gradle.kts` or `gradle/libs.versions.toml`. `generateVersionFile` supplies the version constant | Dropped with upstream's build file | BLD-01 |
| The fork's orphaned `ClickHouseAPI.java`, the integration-test copies of `SchemaTestData` and `SchemalessTestData`, the `src/test` copy of `ClickHouseTestHelpers`, and the fork's `src/test/java/transforms/KeyToValueTest.java` | `git ls-files` finds no `ClickHouseAPI.java`; exactly one `SchemaTestData`, `SchemalessTestData` and `ClickHouseTestHelpers`, all under `src/testFixtures`; and upstream's repackaged `KeyToValueTest` under `com/clickhouse/kafka/connect/transforms` | Upstream deleted or moved these; the fork's copies were removed rather than re-expressed | HARN-04, HARN-05 |
| D-17's unconditional `network_compression_method=lz4` setting on every client (V2 `serverSetting`, V1 `CUSTOM_SETTINGS`), with its two package-private constants | `setCompressionMethod` (default `true`, alias `set_compression_method`) sets the lz4 setting on the V1 options, the V1 node and the V2 server settings, and `compress=false` is set unconditionally on all three (V2 through `compressServerResponse(false)`), on every client the connector builds. Against ClickHouse 26.9.12.8 with client `0.9.6`, `compress=false` alone is sufficient on both clients (with the lz4 setting absent the server reports `network_compression_method=ZSTD` and the response still decodes), the lz4 setting alone is sufficient on V2 when an operator re-enables response compression, and removing both on V2 with `compress=true` fails with `Invalid LZ4 magic byte: '-112'`, so on V2 at least one of the two controls is required, while on V1 an operator's `?compress=true` did not reach the node options and the V1 stripped-both case was not observed (Recorded Fact 26). A `compress=true` in `jdbcConnectionProperties` re-enables response compression on V2 and wins over the connector's `compress=false`, pinned by `operatorCompressTrueOverridesV2ResponseCompression`. | Merge hunks resolved to upstream in 12-01; the dead constants were deleted in 12-03 | CMP-01, CMP-02, CMP-03; `NetworkCompressionPinUnitTest`, `NetworkCompressionPinLiveTest`; Recorded Facts 25 and 26 |
| The fork's own `org.lz4:lz4-java` capability rule in `build.gradle.kts`, which resolved the capability to a fixed older `at.yawk.lz4` release | Upstream's group-based capability rule plus the catalog `lz4Java` entry. The fork's rule was redundant, not masking: both resolved the capability to the same version before it was deleted | The mechanical merge brought upstream's rule in beside the fork's; the fork's block was deleted in 12-02. `dependencyInsight` shows exactly one `at.yawk.lz4:lz4-java` headline on `runtimeClasspath` and on `clickhouseDependencies`, before and after | BLD-09; Recorded Facts 19 and 24 |

### Adopted wholesale

| Adopted | Measured state | Pinned by |
|---|---|---|
| `Table.java`, `Processing.java` and `KeyToValue.java` | Byte-identical to upstream `v1.6.0`. `Table` is upstream's immutable form, with the fork's call sites rewritten against it; `Processing` takes upstream's five-argument constructor; `KeyToValue` is upstream's rewrite with its stale-schema-cache fix | MAP-03, ORCH-01, ORCH-04 |
| Upstream's `src/main` tree | 38 of the 63 files present at `v1.6.0` are byte-identical in this tree. Of the 25 that differ, 16 differ only in import order (see [the raw numbers](#the-raw-numbers)) and 9 carry fork logic, each listed in the first section | SYNC-03; `.planning/MERGE-CHECKLIST.md` Section 1 |
| Upstream's test tree | Between `v1.2.9` and `v1.6.0` upstream added or changed 145 test-tree files that still exist at `v1.6.0`. 129 of them are byte-identical in this tree. The other 16 differ by fork adaptation: `ClickHouseBase` and `ClickHouseTestHelpers` carry the harness contract above, seven tests (`ProcessingTest`, `FailureTest`, `ClickHouseSinkTaskTest`, `AvroDecimalLogicalTest`, `AvroNullableUnionVariantTest`, `ClickHouseWriterColumnContextTest`, `ClickHouseWriterTest`) pass an explicit `null` connector name to the two-argument `SinkTaskStatistics` constructor, the two proxy test classes add the `usesSharedContainer()` opt-out override, `ClickHouseSinkTaskMappingTest` adds a `tableFilterTest`, `TableTest` carries one corrected assertion, and the remaining three differ in import order only | SUITE-01; ORCH-04 |

## The raw numbers

The table gives files changed, insertions and deletions for the same two scopes at both endpoints. The wider scope is `src build.gradle.kts gradle .github`; the narrower scope is `src/main` alone. "Before" compares the fork's base `v1.2.9` with the pre-sync tip `pre-v1.6.0-sync`; "after" compares upstream `v1.6.0` with the merged `sync/v1.6.0`. Figures were measured on the tree as published, including the `network_compression_method` fix in `ClickHouseHelperClient.java` and its two test classes. They can be refreshed by running the four commands shown beneath the table.

| Scope | Endpoint | Files | Insertions | Deletions |
|---|---|---|---|---|
| `src build.gradle.kts gradle .github` | before: `v1.2.9` to `pre-v1.6.0-sync` | 93 | 8401 | 423 |
| `src build.gradle.kts gradle .github` | after: `v1.6.0` to `sync/v1.6.0` | 84 | 9476 | 451 |
| `src/main` | before: `v1.2.9` to `pre-v1.6.0-sync` | 32 | 1308 | 139 |
| `src/main` | after: `v1.6.0` to `sync/v1.6.0` | 30 | 1368 | 98 |

```text
git diff --shortstat v1.2.9 pre-v1.6.0-sync -- src build.gradle.kts gradle .github
git diff --shortstat v1.6.0 sync/v1.6.0 -- src build.gradle.kts gradle .github
git diff --shortstat v1.2.9 pre-v1.6.0-sync -- src/main
git diff --shortstat v1.6.0 sync/v1.6.0 -- src/main
```

Insertions grew at both scopes, from 8401 to 9476 at the wider scope and from 1308 to 1368 in `src/main`, and deletions at the wider scope also rose, from 423 to 451. The reason is that the fork-only tests, plus this milestone's canaries, are the previous milestone's deliverable, so line growth in the test tree is the product rather than the debt. Deletions in `src/main` fell, from 139 to 98, and the file counts fell at both scopes, but neither of those says how much fork logic each file carries, so neither is offered as the headline. The smaller-delta claim rests on the behaviour ledger above, not on any single number here.

One number does bear on how the delta reads: of the 30 `src/main` files that differ from upstream `v1.6.0`, 16 differ only in import order, because the fork keeps Spotless `importOrder()` (planner decision PD-01, above) while upstream applies Spotless with no configuration block. The other 14 carry fork logic: nine modified upstream files, including the service file, and five new transform classes.

This ledger was derived from `.planning/MERGE-CHECKLIST.md`, whose Section 1 classifies every conflicted path as survives-as-is, re-express or redundant and whose numbered Recorded Facts carry the reasoning behind each disposition. Read it for the per-path index.

## Known costs and open items

Nothing in this section has been fixed by the sync. Each item is published knowingly open.

**1. R-01: silent data loss with `insertFormat=json` and `Struct` records.** With `insertFormat` set explicitly to `json`, a record whose value is a Kafka Connect `Struct` (a schema'd record, as produced by the Avro, Protobuf and schema-enabled JSON converters), and `bypassRowBinary` left at its default of `false`, **every row is silently discarded**. No exception is thrown, `put()` returns normally.

The mechanism: in metadata-free mode the writer builds a synthetic `Table` with no columns. `doInsertBatch` sends schema'd records to the RowBinary writer unless `bypassRowBinary` is true, and that writer's per-column loop iterates the table's root columns, which for the synthetic table is an empty list. Each record therefore serialises to zero bytes and the client reports zero rows written.

The loss is pinned by a test rather than fixed: `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch`, whose Javadoc is headed "R-01 PIN - NOT AN ENDORSEMENT". A fail-fast repair was left out on purpose, because it would turn a pipeline that is losing data quietly today into one that fails loudly, and it needs a migration story for existing `Struct`-converter pipelines.

The loss was accepted when the metadata-free path was kept (Recorded Fact 14), and the acceptance rests on an operator fact: no live pipeline in the deployment this fork serves takes the `Struct` plus explicit-JSON route today. **That fact is true of that deployment and cannot be verified by anyone else building from this fork.** If you build from this fork, check your own connector configuration: do not combine an explicit `insertFormat=json` with `Struct` values unless you have confirmed rows arrive. The only route pinned by a passing test in metadata-free mode is schemaless `Map` values. Setting `bypassRowBinary=true` sends schema'd records through the JSON writer according to `doInsertBatch`, but no test pins that combination in metadata-free mode, so verify it before relying on it. Status: recorded as the Future Requirement "R-01 fail-fast" in `.planning/REQUIREMENTS.md`; not fixed.

**2. WR-01 (test harness): `ClickHouseBase.tearDown()` swallows a database-drop failure.** It logs the failure and carries on. Under the shared container, that per-class database drop is the only isolation between classes in one JVM fork, so a transient failure can strand a database and make a different, innocent later class fail its isolation assertion with no attributable cause. The only detection mechanism today is a clean full-suite run. Status: accepted as a cost in Recorded Fact 16 of `.planning/MERGE-CHECKLIST.md` and carried as an open item in `.planning/STATE.md`; not scheduled, not fixed. This is a different item from the Future Requirement also named WR-01, which is item 4 below.

**3. Four open code-review findings**, published with the findings open. Each is recorded in the review that found it (`07-REVIEW.md` and `09-REVIEW.md` under `.planning/phases/`) and carried as an open item in `.planning/STATE.md`; none is scheduled, and none is fixed.

| Finding | What is wrong | Why it is not urgent |
|---|---|---|
| `Column.extractColumn`, `Array` recursion | The recursive call passes `isSubColumn` and `hasDefaultValue` in swapped positions | Dead code today; a landmine for a future change |
| `ClickHouseSinkConfig`, `ConfigDef` default for `USERNAME` | The definition reads `passwordDefault` (`""`) instead of `usernameDefault` (`"default"`) | It misreports the default in Kafka Connect's config UI; the runtime value comes from a separate `getOrDefault` that uses the right default, so there is no runtime effect |
| `JsonColumnTest` | Asserts row counts only and never reads the JSON payload back | Coverage gap, not a defect in the connector |
| `ClickHouseWriter.cleanupExtraFields` (finding P9-IN-01) | The guard `csc.isBypassFieldCleanup() \|\| shouldSkipTableMetadata()` dereferences `csc` before the predicate whose point is a `csc != null` check | Unreachable today: the method is only called through a started writer |

**4. Other tracked follow-ups.** The Future Requirements list in `.planning/REQUIREMENTS.md` remains open and is not contradicted by anything here. It includes WR-01 (map): end-to-end coverage for an actual `null` entry inside a `Map(K, Nullable(V))` column, which upstream's guarded write does not by itself close (Recorded Fact 9).

**5. Upstream capability that the merge makes available and this milestone deliberately did not adopt:** `DebeziumRecordConvertor` (`debeziumCDCEnabled`), the `auto.evolve` family, buffered delivery (`bufferCount` and `bufferFlushTime`) and `retryOnSocketException` are all present in the tree and deliberately not adopted by this milestone.

## Named skip inventory

The current suite skips 18 tests. The structural basis is measured by command rather than copied from a report:

```text
grep -rc '@Disabled' src/test/java                -> 7 annotation sites, 7 test methods
grep -rc 'Assumptions.assumeTrue' src/test/java   -> 1 site, in a parameterized test with 11 cases
```

Seven disabled methods plus the eleven parameterized cases behind the single standalone-mode assumption make 18.

| Class | Test | Reason |
|---|---|---|
| `ClickHouseSinkTaskTest` | `clientNameTest()` | `@Disabled`: a pre-existing flaky-test disable |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithNestedTupleMapArrayAndVariant()` | `@Disabled`: requires a flag on the instance |
| `ClickHouseSinkTaskWithSchemaTest` | `schemaWithTupleOfMapsWithVariantTest()` | `@Disabled`: requires a flag on the instance |
| `ClickHouseSinkTaskWithSchemaTest` | `exactlyOnceStateMismatchTest`, cases `[1]` to `[11]` | `Assumptions.assumeTrue(isCluster \|\| isCloud)`: skipped in a standalone run |
| `InsertFormatPerformanceTest` | `compareInsertPerformance()` | `@Disabled`: not suitable for unit tests |
| `JsonColumnTest` | `compareAllConfigurations()` | `@Disabled`: not suitable for unit tests |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testWideStructPerformance()` | `@Disabled`: not suitable for unit tests |
| `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testDeepNestedStructPerformance()` | `@Disabled`: not suitable for unit tests |

None of the 18 belongs to the fork's own coverage. The SMT sink-landing tests, the JSON and Dynamic round-trip, RowBinary fail-fast, JMX registration and insert-columns tests all run.

Upstream v1.7.0's #800 Avro record-union and cluster `buildFromClause` tests live only in the `integrationTest` source set (`ClickHouseSinkConnectorIntegrationTest`, `clickhouse_sink_avro_json.json`, and the testFixtures `union_two_records.json` schemas). That source set needs a Confluent Platform stack and is not run by `./gradlew test`. They are accepted byte-unmodified and are proven by `compileIntegrationTestJava` only (SYNC-10, Recorded Fact 30): a named, compile-only skip that is not counted in the 18 above. The 18 skips are unchanged by the v1.7.0 merge.

## For the next syncer

Read this file first, then `.planning/MERGE-CHECKLIST-v1.7.0.md`, then `.planning/MERGE-CHECKLIST.md`. Each checklist's Section 1 gives a per-path resolution for every conflicted file, and its numbered Recorded Facts carry the reasoning behind each disposition in the ledger above. Recorded Facts 1-16 live in the v1.6.0 checklist and 17 onward in the v1.7.0 one, so a citation "Recorded Fact N" is never ambiguous.

The most expensive fact of the `v1.6.0` sync is that seven fork behaviours vanished from the merge with no conflict marker and no compile error. The one found last, the test harness's default ClickHouse image reverting from `latest` to a pinned version, was found only after two phases had closed green against the wrong server. That is why every one of them now has a test pinning it, and why a green build after a merge is not evidence that the fork's behaviour survived: check each row of [What this fork still patches](#what-this-fork-still-patches) against the new tree.

An eighth behaviour was easy to lose the same way, because it was one line in a helper with no failing test on any server older than ClickHouse 26.9: the forced `network_compression_method=lz4`. Since the v1.7.0 sync it is retired onto upstream's `setCompressionMethod` flag (see the row under [Retired in favour of an upstream implementation](#retired-in-favour-of-an-upstream-implementation)), so an upstream sync no longer has to carry it. The hazard that remains is an operator one: `compress=true` in `jdbcConnectionProperties` re-enables response compression on client V2, and combined with `setCompressionMethod=false` against a 26.9+ server it fails reads with the LZ4 magic-byte error.

The mechanical checks are `.planning/scripts/verify-merge-invariants.sh` with its anchor inventory `.planning/scripts/fork-anchors.txt`, run at the end of every phase from 12 to 15. An anchor that moves must move in the commit that moves the code, so a merge that silently drops a fork behaviour fails the script instead of passing a green build.
