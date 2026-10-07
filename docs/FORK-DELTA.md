# Introduction

This document records what this fork of `ClickHouse/clickhouse-kafka-connect` still patches on top of upstream, and what it gave up in favour of upstream's own implementation when `v1.6.0` was merged in. It is written for whoever runs the next upstream sync: it says which behaviours exist only here, where they live, why upstream does not supply them, and which test or requirement pins each one, so that a merge which silently drops one of them is noticed.

Every row below was checked against the merged tree (`sync/v1.6.0`, commit `09cae191`) by searching the code for the construct the row describes. A row that claims a patch survives was only recorded after the patch was found in the source; a row that claims a patch was retired was only recorded after the fork's version was found to be absent. The rows describe what the code contains, not what a plan intended.

## Contents

- [What this fork still patches](#what-this-fork-still-patches)
  - [Connector source](#connector-source)
  - [Build, dependencies and CI](#build-dependencies-and-ci)
  - [Test harness](#test-harness)
- [What the fork retired in favour of upstream](#what-the-fork-retired-in-favour-of-upstream)
  - [Retired in favour of an upstream implementation](#retired-in-favour-of-an-upstream-implementation)
  - [Adopted wholesale](#adopted-wholesale)
- [The raw numbers](#the-raw-numbers)

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
| Six fork build additions, re-applied onto upstream's version-catalog `build.gradle.kts`: JaCoCo (`jacocoTestReport` plus the 15-class `jacocoForkReport`, report-only, no threshold); the `testUnit` task, which excludes the `clickhouse` tag, disables JaCoCo and is kept out of `check`; a JDK 17 `javaLauncher` for the test JVM together with `file.encoding=windows-1252`; the `org.lz4:lz4-java` capability resolution to `at.yawk.lz4:lz4-java:1.10.2`; the `com.github.ben-manes.versions` plugin with its `dependencyUpdates` configuration; and the Spotless `importOrder()` plus `removeUnusedImports()` block with its `removeUnusedImports` task | `build.gradle.kts` | None of these exist upstream. The Spotless block is planner decision PD-01, kept because the repository's own conventions document both the block and the task | BLD-03, BLD-04, BLD-05, BLD-06; Recorded Facts 2 and 7 |
| `org.slf4j:slf4j-reload4j:2.0.17` is declared as a literal coordinate | `build.gradle.kts` | Upstream's version catalog has no entry for it | BLD-08; Recorded Fact 2 |
| Fork dependency pins: ClickHouse client `0.9.6` (upstream `0.9.5`) and Kafka Connect API `4.1.1` (upstream `2.7.0`), the latter declared `api(...)` | `gradle/libs.versions.toml`, `build.gradle.kts` | Production runs the shaded jar built here, so these are the production versions; the LZ4 capability resolution above travels with the Connect API pin | BLD-08, SYNC-04; Recorded Facts 1 and 2 |

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

### Adopted wholesale

| Adopted | Measured state | Pinned by |
|---|---|---|
| `Table.java`, `Processing.java` and `KeyToValue.java` | Byte-identical to upstream `v1.6.0`. `Table` is upstream's immutable form, with the fork's call sites rewritten against it; `Processing` takes upstream's five-argument constructor; `KeyToValue` is upstream's rewrite with its stale-schema-cache fix | MAP-03, ORCH-01, ORCH-04 |
| Upstream's `src/main` tree | 38 of the 63 files present at `v1.6.0` are byte-identical in this tree. Of the 25 that differ, 16 differ only in import order (see [the raw numbers](#the-raw-numbers)) and 9 carry fork logic, each listed in the first section | SYNC-03; `.planning/MERGE-CHECKLIST.md` Section 1 |
| Upstream's test tree | Between `v1.2.9` and `v1.6.0` upstream added or changed 145 test-tree files that still exist at `v1.6.0`. 129 of them are byte-identical in this tree. The other 16 differ by fork adaptation: `ClickHouseBase` and `ClickHouseTestHelpers` carry the harness contract above, seven tests (`ProcessingTest`, `FailureTest`, `ClickHouseSinkTaskTest`, `AvroDecimalLogicalTest`, `AvroNullableUnionVariantTest`, `ClickHouseWriterColumnContextTest`, `ClickHouseWriterTest`) pass an explicit `null` connector name to the two-argument `SinkTaskStatistics` constructor, the two proxy test classes add the `usesSharedContainer()` opt-out override, `ClickHouseSinkTaskMappingTest` adds a `tableFilterTest`, `TableTest` carries one corrected assertion, and the remaining three differ in import order only | SUITE-01; ORCH-04 |

## The raw numbers

The table gives files changed, insertions and deletions for the same two scopes at both endpoints. The wider scope is `src build.gradle.kts gradle .github`; the narrower scope is `src/main` alone. "Before" compares the fork's base `v1.2.9` with the pre-sync tip `pre-v1.6.0-sync`; "after" compares upstream `v1.6.0` with the merged `sync/v1.6.0`. Figures were measured at commit `09cae191` and can be refreshed by running the four commands shown beneath the table.

| Scope | Endpoint | Files | Insertions | Deletions |
|---|---|---|---|---|
| `src build.gradle.kts gradle .github` | before: `v1.2.9` to `pre-v1.6.0-sync` | 93 | 8401 | 423 |
| `src build.gradle.kts gradle .github` | after: `v1.6.0` to `sync/v1.6.0` | 82 | 9255 | 450 |
| `src/main` | before: `v1.2.9` to `pre-v1.6.0-sync` | 32 | 1308 | 139 |
| `src/main` | after: `v1.6.0` to `sync/v1.6.0` | 30 | 1358 | 97 |

```text
git diff --shortstat v1.2.9 pre-v1.6.0-sync -- src build.gradle.kts gradle .github
git diff --shortstat v1.6.0 sync/v1.6.0 -- src build.gradle.kts gradle .github
git diff --shortstat v1.2.9 pre-v1.6.0-sync -- src/main
git diff --shortstat v1.6.0 sync/v1.6.0 -- src/main
```

Insertions grew at both scopes, from 8401 to 9255 at the wider scope and from 1308 to 1358 in `src/main`, and deletions at the wider scope also rose, from 423 to 450. The reason is that the fork-only tests, plus this milestone's canaries, are the previous milestone's deliverable, so line growth in the test tree is the product rather than the debt. Deletions in `src/main` fell, from 139 to 97, and the file counts fell at both scopes, but neither of those says how much fork logic each file carries, so neither is offered as the headline. The smaller-delta claim rests on the behaviour ledger above, not on any single number here.

One number does bear on how the delta reads: of the 30 `src/main` files that differ from upstream `v1.6.0`, 16 differ only in import order, because the fork keeps Spotless `importOrder()` (planner decision PD-01, above) while upstream applies Spotless with no configuration block. The other 14 carry fork logic: nine modified upstream files, including the service file, and five new transform classes.

This ledger was derived from `.planning/MERGE-CHECKLIST.md`, whose Section 1 classifies every conflicted path as survives-as-is, re-express or redundant and whose numbered Recorded Facts carry the reasoning behind each disposition. Read it for the per-path index.
