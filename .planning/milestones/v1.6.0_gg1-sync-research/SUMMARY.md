# Project Research Summary

**Project:** ClickHouse Kafka Connect — `custom_build` fork, milestone `v1.6.0_gg1-sync`
**Domain:** Big-bang `git merge` of 581 upstream commits (`v1.2.9` → `v1.6.0`) into a production CDC fork
**Researched:** 2026-09-18
**Confidence:** HIGH — every claim traces to a `git show`/`git diff`/`git merge-tree` read in this repo or to upstream's own `CHANGELOG.md`. No claim is recalled.

> **Synthesis note (#222):** the `gsd-research-synthesizer` subagent twice reported success while writing nothing to disk, fabricating a write restriction. The orchestrator wrote this file from the four source reports directly and re-ran `git merge-tree` itself to settle a numeric discrepancy between two of them. Every number below is first-hand.

## Executive Summary

This is not a large merge; it is an **architecturally discontinuous** one. Upstream did four things across those 581 commits that a textual three-way merge cannot see: it inserted a new orchestration layer (`DeliveryStrategy` / `ChunkFlusher` / buffer strategies) above `ProxySinkTask`; it moved JMX MBean-name construction out of `ProxySinkTask` into `util.jmx.SinkTaskStatistics` (different package, static method, build-time `Version.ARTIFACT_VERSION`); it made `Table` immutable (`addColumn()` now private, columns passed to the constructor); and it changed `showTables()`'s return type from `List<Table>` to `List<Table.TableDesc>`. The fork's edits sit on top of APIs whose *shape* changed, not just whose bodies changed — so several conflicts must be **re-expressed by intent**, and at least one fork feature vanishes with no conflict marker and no compile error at all.

The conflict surface is smaller and better-shaped than the diff stats implied. `git merge-tree --write-tree custom_build v1.6.0` (re-run by the orchestrator, git 2.50.1, merge-base `bec167d` = upstream `v1.2.9`) reports **40 conflict messages — 35 `content` + 5 `modify/delete` — across 39 unique paths** (one path carries two messages). Of the twelve `src/main` files the milestone's collision table named, **nine carry zero functional fork change** — `Record.java`, `Data.java`, `StructToJsonMap.java`, the record convertors, `Processing.java`, `Utils.java`, `KeyToValue.java`, `Table.java`, and every `state/provider` file were only touched by Spotless import reordering. Those are taken from upstream wholesale. The real work concentrates in five files: `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `ClickHouseSinkConfig.java`, `ProxySinkTask.java`, and `ClickHouseBase.java`.

Two of the fork's nine Active follow-ups close for free and must be recorded, not worked: upstream's rewritten `Utils.handleException` now branches on `com.clickhouse.client.api.ServerException` as well as `ClickHouseException` (upstream issue #818), closing the fork's "error classification is dead code on client V2" item; and upstream independently shipped the same `Map(K, Nullable(V))` RowBinary null-marker fix the fork made in Phase 2, with one extra null guard the fork's version lacks. The dominant risk is the mirror image of those free wins — **silent feature loss**: the JMX `connector=<name>` segment, the un-forcing of `async_insert=0`, the UTF-8 `describeTable` readers, `targetTableFilter` / the sub-column-skip rule, and `ClickHouseBase`'s `@Tag("clickhouse")` / shared-container / MBean-sweep contract all disappear unless deliberately re-applied, and only two of those five would surface as a compile error.

## Key Findings

### Build Stack

Upstream's `build.gradle.kts` moved to a Gradle **version catalog** (`gradle/libs.versions.toml`, new), adopted `jvm-test-suite` and `java-test-fixtures`, added the `protobuf` plugin, renamed the shadow plugin's group ID (`com.github.johnrengelman.shadow` → `com.gradleup.shadow`), and **dropped `signing` and `com.github.gmazzo.buildconfig`** — both confirmed dead in the fork too (zero references anywhere in `src/`). The wrapper goes `8.14` → `9.6.1`.

**The fork has five build additions to re-apply, not three.** `PROJECT.md`'s framing named JaCoCo, `testUnit` and JDK 17; research found two more that would otherwise be silently dropped:

- **JaCoCo** (`jacoco` plugin, `toolVersion 0.8.15`, the 15-class `forkChangedClasses` list, `jacocoTestReport` + `jacocoForkReport`) — copies over verbatim; upstream has no JaCoCo to conflict with, and `tasks.named<Test>("test")` still resolves under `jvm-test-suite`.
- **`testUnit`** (`excludeTags("clickhouse")`, JaCoCo disabled on it, kept out of `check` by the `if (name != "testUnit")` guard) — copies over; the guard must be re-inserted because upstream's `tasks.withType<Test>` has no `check.dependsOn` line at all.
- **JDK 17 + `file.encoding=windows-1252`** — **the hard one.** Upstream deleted the whole mechanism, not just the number: `java { toolchain { ... } }`, `javaLauncher.set(javaToolchains.launcherFor { ... })`, the `jdkHome` override and the custom `addTestListener` summary are all gone, replaced by `options.release.set(11)`. The `javaLauncher` line is what actually pins the *test* JVM to 17 independent of the invoking JDK — re-inserting it is not optional.
- **LZ4 capability resolution** (`withCapability("org.lz4:lz4-java") { select("at.yawk.lz4:lz4-java:1.10.2") }`) — fork-only, absent from both `main` and `v1.6.0`, coupled to the fork's Kafka bump.
- **`com.github.ben-manes.versions` plugin** + its `dependencyUpdates` config — the config block references a plugin-supplied Kotlin type, so dropping the plugin ID breaks the *build script compile*, not just behaviour.

**The `jacocoForkReport` 15-class list needs zero edits.** All ten upstream-shared classes still exist at their exact paths at `v1.6.0` (`MBeanServerUtils.java` is byte-identical to `main`); the five "missing" entries are fork-only transform classes that never existed upstream at any version and return with the merge itself. `.github/scripts/jacoco_summary.py` needs no edits either — its two hardcoded XML paths match what the re-applied tasks produce.

**Dependency deltas.** Take upstream's for Jackson (`2.21.0` → `2.22.2`), HttpClient5 (`5.6` → `5.6.2`), Lombok (`1.18.42` → `1.18.46`), Mockito (`5.21.0` → `5.23.0`), OkHttp (`5.3.2` → `5.4.0`), `org.json`, slf4j (`2.0.17` → `2.0.18`). Keep the fork's ClickHouse client `0.9.6` (one patch *ahead* of upstream's `0.9.5`). `slf4j-reload4j` has **no catalog entry upstream** and must survive as a literal coordinate. Four version choices need a decision — see Open Decisions.

**CI survives essentially untouched.** The fork's single-job `tests.yaml` only shells out to `./gradlew test`, so it has no dependency on the build file's internals; its action pins (`checkout@v4`, `setup-java@v6`, `setup-gradle@v6`) are already *ahead* of upstream's `v1.6.0` workflow in every case. One number to watch: the job's `timeout-minutes: 45` against a suite that grows by upstream's 56 test files plus a possible container-per-class regression.

### Feature Disposition

| # | Fork feature | Upstream v1.6.0 | Action | Adjudicating test |
|---|---|---|---|---|
| 1 | `TableOperationFilter` SMT | none | **Keep wholesale** | `TableOperationFilterTest` (26) + sink-level |
| 2 | `HeadersToValue` SMT | none | **Keep wholesale** | `HeadersToValueTest` (25) + sink-level |
| 3 | `HeaderToValue` SMT + config | none (upstream fixed the same stale-schema-cache bug class in `KeyToValue` only) | **Keep wholesale**, incl. the D-02 guard | `HeaderToValueTest` (24) + sink-level |
| 4 | `ConvertToMapOrJson` SMT | none | **Keep wholesale** | `ConvertToMapOrJsonTest` (37) + perf + sink-level |
| 5 | `targetTableFilter` | none — `showTables*` build an unconditional `WHERE database = '%s'`, no `match(` anywhere | **Re-express** onto the new `List<Table.TableDesc>` signatures | `FILT-01..04`, `ClickHouseSinkTaskMappingTest.tableFilterTest` |
| 6a | `JSON(...)` in `Column`/`Type` | **native** — `Type.JSON` exists, `Column` uses the same `startsWith("JSON")` posture and the same "server handles the params" comment | **Adopt upstream** — also brings `SimpleAggregateFunction` unwrapping and Avro/Protobuf union handling | `TYPE-01..05` (JSON half) |
| 6b | `Dynamic` in `Column`/`Type` | **none** — no `DYNAMIC` constant; a `Dynamic` column silently becomes `Type.UNKNOWN` | **Keep fork**, ported onto upstream's rewritten `Column.java` | `TYPE-01..05` (Dynamic half) |
| 6c | `describeTable` sub-column skip | **partial, by a different mechanism** — `Table.handleNonRoot` special-cases `.null`/`.size[0-9]+` in the ARRAY branch only; a bare `.size` (no digit) does **not** match its regex, and JSON/Dynamic children fall to `default:` which logs at DEBUG and still registers the sub-column | **Keep the fork's explicit `shouldSkipDescribeRow`**, re-expressed against the new local-`List<Column>` loop | `DESC-01..04` — the suite that actually settles it |
| 7 | Metadata-free JSON path + `jsonInsertColumns` | **none** — `doInsertJsonV1/V2` still require a described `Table`; no `shouldSkipTableMetadata()` equivalent; `doInsertJsonV2` now takes a `DataStreamWriter` lambda, not a `List<String> columns` overload | **Redesign, not merge** — gated on an explicit R-01 keep/drop decision | `JSON-01..05`, `COLS-01..06`, R-01's labelled test |
| 8a | JMX `connector=<name>` | **none, and the carrier method was deleted** — `ProxySinkTask.buildMBeanName` is gone; naming moved to `SinkTaskStatistics.getMBeanName(int)` / `getTopicMBeanName(int, String)` with no `connector=` segment, sourced from `Version.ARTIFACT_VERSION` | **Re-plumb into the new location** — a design task, not a patch re-apply. Also decide whether the new per-topic MBean gets the same treatment | `JMX-01..04` (need re-targeting) |
| 8b | `async_insert` not forced | **unchanged upstream** — `addClickHouseSetting("async_insert", "0", false)` is still there, same spot | **Keep fork's one-line comment-out** | `ASYNC-01..03` |

**Thirteen new upstream config options**, none colliding by name or intent with `targetTableFilter` / `jsonInsertColumns`: `retryOnSocketException`, `bufferCount`, `bufferFlushTime`, `clickhouseClientInsertTimeoutMs`, `reportInsertedOffsets`, `debeziumCDCEnabled`, `auto.evolve`, `auto.evolve.ddl.refresh.retries`, `auto.evolve.struct.to.json`, `errors.retry.timeout`, `clusterName`, `enableReplicaPinning`, `ssl_socket_sni`. All default to off/empty — `bufferCount=0` in particular means `DirectDeliveryStrategy`, i.e. pre-refactor behaviour exactly.

**`DebeziumRecordConvertor` (+289) does not overlap the fork's SMTs.** It flattens a Debezium envelope into a `ReplacingMergeTree(_version, is_deleted)` row, gated on `debeziumCDCEnabled=true` **and** a schema name ending `.Envelope`. `TableOperationFilter` *drops* ops; this *converts* them. They could compose. Record it as available, not adopted.

**Upstream's `KeyToValue` rewrite is a pure win** — the fork's diff there is a no-op import reorder, and upstream's version fixes the same stale-`valueSchema`-cache bug the fork found elsewhere, using an `LRUCache` plus a new `shema_cache_max_size` config. Compile-safety confirmed by inspecting the fork's actual resolved `kafka-clients-4.1.1.jar`: `org.apache.kafka.common.cache.{Cache,LRUCache,SynchronizedCache}` are present.

### Architecture and Merge Sequence

At `v1.6.0` the path is `ClickHouseSinkTask.put()` → `DeliveryStrategy.put()` (one of `Direct` / `AtLeastOnceBuffer` / `ExactlyOnceBuffer`, chosen from `bufferCount`/`exactlyOnce`) → `ChunkFlusher.putDirect()/flush()` → `ProxySinkTask.put()` → `Processing.doLogic()` → `ClickHouseWriter.doInsert()`. The new layer sits entirely **above** `ProxySinkTask`, so `insertFormat` and the fork's JSON path plug in at an unchanged point — but `Processing`'s constructor went from four args to five (`SinkTaskStatistics` threaded in for per-insert timing), which surfaces only in `ProxySinkTask.java` and `ProcessingTest.java`.

Resolution order is dependency-driven, not size-driven: `ClickHouseWriter.getTable()` cannot compile until `Table`'s new constructor exists; `shouldSkipDescribeRow` cannot reference `Type.DYNAMIC` until the enum has it; `ProxySinkTask`'s JMX work cannot finish until the writer and helper client compile, because it constructs both.

### Critical Pitfalls

1. **JMX `connector=<name>` silently vanishes.** Upstream's `SinkTaskStatistics.getMBeanName` renders `com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask%d,version=%s` — no `connector=`. The conflict in `ProxySinkTask.java` is loud, but the trap is that "prefer upstream where it passes the fork's tests" **does not apply here**: upstream's version *fails* JMX-01/02. Any dashboard keyed on `connector=` stops matching. Re-plumb deliberately; gate on `MBeanNameUnitTest`'s literal substring assertion.
2. **`async_insert=0` re-forced.** One commented-out line, buried in a ~250-line rewritten constructor surrounded by a dozen new upstream config fields. No compile error, no test error outside `AsyncInsertSettingsUnitTest`'s "no setting present" assertion. Production symptom is a silent latency/throughput regression that reads as "the merge made things slower."
3. **`ClickHouseBase` reverts the previous milestone's harness work.** Upstream's version is `@TestInstance(PER_CLASS)` with a fresh container per class, and carries **no `@Tag("clickhouse")`, no `@AfterEach` MBean sweep, no `uniqueConnectorName(TestInfo)`**. Losing the tag breaks `./gradlew testUnit`'s Docker-free guarantee — and it fails as a Testcontainers/Ryuk hang that reads like an environment problem. Losing the sweep reintroduces the `InstanceAlreadyExistsException` order-dependence the last milestone already fixed, now with more surface since upstream registers per-topic MBeans too. Resolve as a deliberate re-composition, first, before any dependent test file.
4. **`ClickHouseTestHelpers.java` is a modify/delete that git resolves by orphaning the fork's copy.** Upstream moved it to `src/testFixtures` (+458 lines); git's rename heuristic did not pair them. Post-merge the tree can hold *both* files, each compiling fine, with every fork test importing the stale one — two divergent notions of e.g. the default `CLICKHOUSE_VERSION` in one build. Grep for exactly one canonical file as a checklist item.
5. **`Utils.java` and `Processing.java` merge with no conflict marker at all** despite both sides touching them. That is correct and good (it is how the free `ServerException` fix arrives) — but it means a reviewer who only reads the conflict list never looks at them, and misses that `ProcessingTest.java` and `UtilsTest.java` *do* conflict and need upstream's versions taken wholesale.
6. **The UTF-8 `describeTable` reader fix is not upstream.** `describeTableV2` at `v1.6.0` still does `new InputStreamReader(queryResponse.getInputStream())` with no charset. 581 commits did not touch it. Re-apply — and grep every `InputStreamReader(` in the resolved file, since upstream restructured the surrounding methods and there may be more call sites than the two the fork originally patched.
7. **RowBinary fail-fast is half-lost.** Upstream's `default:` branch is still `LOGGER.error(...)` with no throw, so the fork's TYPE-06 throw must be re-applied for `DYNAMIC`/`UNKNOWN`. For `JSON`, upstream added a real flag-gated write-as-string path (`isBinaryFormatWrtiteJsonAsString()` — note upstream's own typo) that throws a bare `RuntimeException` when the flag is off. So TYPE-06's JSON assertions need *reconciling* (upstream's capability is strictly more), while its DYNAMIC assertions need *preserving*.

## Implications for Roadmap

### Suggested phase structure

Nine stages emerged from the research; they group naturally into **six phases**, with the two hardest files split out rather than bundled.

#### Phase A: Upstream inventory, rollback point, and open decisions
**Rationale:** the merge is one atomic commit with no bisect path through 581 upstream commits. The rollback point and the six open decisions must exist before any conflict is touched.
**Delivers:** upstream wired as a permanent remote; a `pre-v1.6.0-sync` tag on the pre-merge commit; a scratch branch (`sync/v1.6.0`) so `custom_build` — already public — never needs a force-push; the re-run `git merge-tree` inventory committed as a checklist; the six open decisions answered; a recorded production-config audit for `enableDbTopicSplit=true`+`exactlyOnce=true` and `bufferCount>0`+`exactlyOnce=true`; the catalog's resolved ClickHouse client version recorded as a named fact.
**Gate:** decisions recorded; tag and scratch branch exist; merge-tree output committed.
**Avoids:** Pitfalls 14 (no bisect path), 15 (force-push), and the Section-4 production-config regressions.

#### Phase B: Build plumbing
**Rationale:** nothing compiles until the version catalog, `testFixtures` source set, and wrapper bump land. Everything downstream needs a working build.
**Delivers:** upstream's `build.gradle.kts` + `gradle/libs.versions.toml` adopted; wrapper → `9.6.1`; all **five** fork additions re-applied; `VERSION` → `v1.6.0_gg1`.
**Gate:** `./gradlew help` and `./gradlew dependencies` succeed — no compilation needed yet, this only proves the build script parses and resolves.
**Avoids:** silently dropping LZ4 resolution / ben-manes; silently un-pinning the test JVM from JDK 17.

#### Phase C: Type/mapping layer, then writer + helper + config
**Rationale:** `Column`/`Type`/`Table` are the leaf layer both hard files depend on. `Table`'s immutable constructor and `Type.DYNAMIC` must exist before `ClickHouseWriter.getTable()` or `shouldSkipDescribeRow` can even compile.
**Delivers:** `Type.DYNAMIC` added next to upstream's `JSON`; upstream's `Column.java` adopted with the Dynamic branch ported in; `ClickHouseWriter` re-expressed (JSON dropped from the `validateDataSchema` gate in favour of upstream's native `case JSON:`, `DYNAMIC`/`UNKNOWN` kept, the `default:` throw re-applied, the fork's redundant MAP null-marker patch dropped in favour of upstream's, the synthetic `Table` rebuilt as `new Table(db, name, false, emptyList(), 0)`); `ClickHouseHelperClient` re-expressed (`buildTableFilterClause` injected into the two inline `String.format` calls, `shouldSkipDescribeRow` re-pointed at the local `List<Column>`, `numColumns` incremented *before* any skip so the cache-staleness check stays correct, UTF-8 on every `InputStreamReader`); `ClickHouseSinkConfig`'s new keys added additively with `async_insert` left un-forced.
**Gate:** `./gradlew compileJava`, then `./gradlew testUnit` — the Docker-free gate is usable here, before the harness is rebuilt, because `ColumnTest`/`TableTest`/config-parsing tests do not touch `ClickHouseBase`.
**Avoids:** Pitfalls 5, 6, 7, 8, 9, 10.

#### Phase D: Orchestration and JMX re-integration
**Rationale:** the JMX feature needs design time, not a patch re-apply, and it is the one production feature that vanishes with neither a compile error nor a merge conflict on the file that matters.
**Delivers:** `ProxySinkTask` / `ClickHouseSinkTask` resolved with upstream's 5-arg `Processing` construction; `connectorName` threaded from `ClickHouseSinkTask.start()` into `SinkTaskStatistics`; `connector=<name>` restored in both `getMBeanName` overloads with the colon sanitization kept; `Processing.java` / `Utils.java` confirmed as clean merges and explicitly reviewed rather than skipped.
**Gate:** `./gradlew testUnit` plus `MBeanNameUnitTest` specifically — its literal `connector=` assertion is the canary.
**Avoids:** Pitfalls 1, 2, 3, 5 (silent merges).

#### Phase E: Metadata-free JSON path — keep or drop
**Rationale:** this is the only genuinely open design question in the milestone, and it is entangled with R-01. Upstream's `writeRecordsJson` already serializes `Struct` records correctly *given real table metadata*; the fork's bypass exists precisely to avoid fetching that metadata. `jsonInsertColumns` must be redesigned regardless, because the client overload it attached to no longer exists at that call site.
**Delivers:** an explicit, recorded keep/drop decision at a blocking-human checkpoint (the same pattern that worked for R-01 last milestone); then either the re-implementation on top of upstream's rewritten methods with R-01's loss re-verified as still pinned, or the deliberate drop with the labelled test's failure recorded as the wanted signal; `jsonInsertColumns` re-expressed as a field filter inside `writeRecordsJson`.
**Gate:** `JSON-01..05`, `COLS-01..06` and R-01's labelled test all reflecting the decision that was made — not a test result nobody looked at.
**Avoids:** Pitfall 4 — the single highest-ambiguity item in the merge.

#### Phase F: Test harness, full suite, CI and publication
**Rationale:** the harness is the widest fan-in — six conflicted sink-task test files extend `ClickHouseBase`, so getting it right first avoids redoing downstream work. Everything else is the acceptance gate.
**Delivers:** `ClickHouseBase` re-composed (upstream's Cloud/Cluster scaffolding present but unwired, plus `@Tag("clickhouse")`, the MBean sweep extended to upstream's per-topic MBeans, `uniqueConnectorName`, and the per-JVM-fork shared container re-derived over upstream's `PER_CLASS` shape); `ClickHouseTestHelpers` consolidated to exactly one canonical file under `src/testFixtures` with the fork's helpers ported and every import fixed; `SchemaTestData`/`SchemalessTestData` reconciled and the `integrationTest` duplicates deleted; `FromVersionConditionExtension`'s package move absorbed; the fork's 371 tests plus upstream's 56 files green; `tests.yaml` green on the pushed SHA; `custom_build` published.
**Gate:** `./gradlew testUnit` completes **without Docker** (the loudest, fastest signal that `@Tag` survived); then `./gradlew compileIntegrationTestJava` succeeds; then full `./gradlew test` green on ClickHouse `latest` × client V2; then CI green located by `head_sha` + workflow `path`, asserting exactly one match.
**Avoids:** Pitfalls 11, 12, 13, and the container-per-class performance trap.

### Phase ordering rationale

- **Dependency, not size.** `ClickHouseWriter.java` is the biggest file but cannot be resolved before `Table.java`'s constructor exists. Ordering by size would produce a working tree that cannot compile for the longest possible stretch.
- **`testUnit` is available from Phase C onward.** Because Phases B–D are `src/main` plus Docker-free-eligible tests, four of six phases get a fast, no-Docker feedback loop before the container-dependent harness is touched at all.
- **JMX and the JSON path each get their own phase** because both are redesigns with no upstream anchor, and both have a specific named test as their gate rather than "compiles."
- **The harness precedes the test suite** because six conflicted test files extend `ClickHouseBase`.

### Research flags

Needs deeper research during phase planning:
- **Phase C** — whether upstream's implicit JSON/Dynamic sub-column swallow in `Table.handleNonRoot`'s `default:` actually passes `DESC-01..04` unmodified. Static reading found one confirmed gap (bare `.size` vs the `.size[0-9]+` regex) but could not settle the rest without running the suite.
- **Phase E** — `jsonInsertColumns` against the new `DataStreamWriter` API shape. Needs a design spike before planning tasks.
- **Phase F** — Testcontainers `1.21.4` → `2.0.5` is a major bump with renamed module coordinates, underpinning every ClickHouse-backed test. Verify `ClickHouseBase` compiles and starts a container under `2.0.5` **before** touching any of the 371 fork tests.
- **Phase F** — whether the per-JVM-fork container sharing can be layered over upstream's `@TestInstance(PER_CLASS)` shape, and what it costs if not.

Standard patterns, no research phase needed: **A** (git mechanics), **B** (build refactor with a written recipe), **D** (mechanical once the design decision is made).

### Must-not-silently-lose list

Five fork behaviours disappear with **no conflict marker and no compile error** unless deliberately re-applied. Only the `Table` constructor break and the `Processing` arity change fail loudly.

| Behaviour | How it vanishes | Detection |
|---|---|---|
| JMX `connector=<name>` | upstream's `SinkTaskStatistics` builds a connector-less name in a different class | `MBeanNameUnitTest` literal `connector=` assertion |
| `async_insert` not forced | one commented-out line lost inside a rewritten constructor | `AsyncInsertSettingsUnitTest` "no setting present" |
| UTF-8 `describeTable` readers | upstream never fixed it; the rewrite looks like it might have | `TYPE-07` under `./gradlew test`'s `windows-1252` — passes in an IDE, fails in the suite |
| `targetTableFilter` + sub-column skip | zero upstream anchor; `ClickHouseHelperClient` "compiles" without them | `FILT-01..04`, `DESC-01..04` |
| `ClickHouseBase`: `@Tag("clickhouse")`, shared container per JVM fork, MBean sweep, `uniqueConnectorName` | upstream's version looks more complete (Cloud/Cluster) and is easy to take wholesale | `./gradlew testUnit` hangs without Docker; `InstanceAlreadyExistsException` only in full-suite runs |

### Open decisions

| # | Decision | Options | Recommendation |
|---|---|---|---|
| 1 | **Kafka Connect API** — fork `4.1.1` vs upstream catalog `2.7.0` | keep the fork's bump (override one catalog key) / adopt upstream's | **Keep `4.1.1`.** Upstream simply has not moved this in 581 commits; the fork's bump was production-driven (newer brokers), and downgrading risks both compile breakage in fork-added transforms and runtime broker incompatibility. The "prefer upstream" policy is about implementation conflicts, not about downgrading a fork-only dependency upstream never reached. |
| 2 | **Testcontainers** `1.21.4` → `2.0.5` | accept upstream's / pin back to 1.x | **Accept `2.0.5`** — already implied by taking upstream's build wholesale, but size it explicitly. Highest-uncertainty bump in the migration; give it its own verification checkpoint in Phase F. |
| 3 | **JUnit Jupiter** — fork `6.0.2` vs catalog `5.14.4` | either | **Take upstream's `5.14.4`** — smaller diff, matches the conflict policy. Delete the fork's vestigial `5.11.3` direct pin rather than porting it; the BOM already wins over it today. |
| 4 | **LZ4 `resolutionStrategy`** | keep / re-derive | Downstream of #1. If Kafka stays at `4.1.1`, keep it. If not, re-run `./gradlew dependencies` and only keep it if the capability conflict still reproduces. |
| 5 | **R-01 / metadata-free JSON path** | keep the bypass and re-verify the loss / drop it and let upstream's `writeRecordsJson` handle `Struct` records with real metadata | **No default.** This is the milestone's one genuine design question and belongs at a blocking-human checkpoint in Phase E. Dropping it would fix a silent-data-loss path for free but changes behaviour for any pipeline running explicit JSON behind a Struct converter. |
| 6 | **Upstream `Type.JSON` RowBinary write-as-string** (`input_format_binary_read_json_as_string`) | adopt upstream's flag-gated capability / keep the fork's fail-fast-only contract | **Adopt upstream's for JSON, keep the fork's throw for DYNAMIC/UNKNOWN.** Upstream's is strictly more capable for JSON; TYPE-06's JSON assertions get rewritten (different exception type and message), its DYNAMIC assertions stay. |

### Free wins — verify, do not plan

Both are **out of scope** as work items. Record the closure, cite the mechanism, do not write a fix.

- **Client-V2 error classification.** Upstream's `Utils.isServerError()` now matches `ServerException` as well as `ClickHouseException`, and `handleException` reads `getCode()` vs `getErrorCode()` accordingly (upstream issue #818, documented in `CHANGELOG.md` under 1.6.0). This is exactly the fork's Active follow-up. `Utils.java` merges with no conflict at all, so upstream's version simply wins. `UtilsTest.java` *does* conflict — take upstream's wholesale.
- **`Map(K, Nullable(V))` RowBinary null marker.** Upstream shipped the identical fix in 1.3.8 (PR #687) with an extra `col.getMapValueType() != null` guard the fork's version lacks. Take upstream's verbatim; do **not** layer the fork's patch on top. Keep the fork's pinning test — it is the suite's contract that this survives the sync — and check whether it also closes WR-01.

Note also: upstream issue #580 ("`DESCRIBE TABLE` was built from the configured database rather than the database requested by the caller") sounds like the same class of bug as the fork's dormant P3-WR-02 follow-up. Check whether upstream's fix subsumes it before re-deriving.

### Hard numbers for sizing

| Measure | Value |
|---|---|
| Upstream commits to absorb | 581 (`bec167d` → `789a57c`) |
| Conflict messages (`git merge-tree`, re-verified) | **40** — 35 `content` + 5 `modify/delete`, across **39 unique paths** (one path carries two messages) |
| Files merging cleanly but carrying fork logic worth a manual read | 7 |
| Named `src/main` collision files that are **phantom** (Spotless import reorder only) | 9 of 12 — `Record`, `Data`, `StructToJsonMap`, the convertors, `Processing`, `Utils`, `KeyToValue`, `Table`, all `state/provider` |
| Files where the real work concentrates | 5 — `ClickHouseWriter`, `ClickHouseHelperClient`, `ClickHouseSinkConfig`, `ProxySinkTask`, `ClickHouseBase` |
| Upstream `src/main` + build delta | 35 files, +3,603 / −865 |
| Upstream test delta | 56 files, +9,726 / −4,609 |
| Fork delta over `v1.2.9` (`src/main` + build + CI) | 36 files, +1,643 / −203 |
| Fork tests in play | 371 (0 failures today), 15,179 LOC across `src/test` + `src/integrationTest` |
| Fork build additions to re-apply | **5**, not 3 |
| New upstream config options | 13, zero collisions |
| Fork Active follow-ups closed for free | 2 (a third, P3-WR-02, to be checked) |

## Confidence Assessment

| Area | Confidence | Notes |
|---|---|---|
| Build structure and dependency deltas | HIGH | Read from `libs.versions.toml` and full `build.gradle.kts` at all three refs; every JaCoCo class path checked with `git cat-file -e` |
| Feature dispositions | HIGH | Each verdict cites the upstream code that justifies it; `KeyToValue`/`Utils` no-op claims verified by direct fork diff |
| Conflict inventory | HIGH | `git merge-tree` re-run by the orchestrator; the two reports' differing counts reconciled to 40 messages / 39 paths |
| Merge order and per-file resolution shapes | HIGH | Justified by concrete compile-time dependencies (constructor signatures, enum values) traced in the diffs |
| Production-visible regressions (JMX, `async_insert`) | HIGH | Direct code reads on both sides |
| Sub-column skip equivalence (`DESC-01..04`) | MEDIUM | Static reading only; one confirmed regex gap (`.size` vs `.size[0-9]+`), the rest needs the suite to settle |
| `bufferCount`+`exactlyOnce` / `enableDbTopicSplit` upgrade hazards | MEDIUM | Upstream's own changelog claim, not independently re-derived from code |
| Gradle 9.6 / JaCoCo 0.8.15 compatibility | MEDIUM | Corroborated by issue trackers and community sources, not vendor docs |

**Overall confidence:** HIGH.

### Gaps to address

- **Production config audit is not derivable from the repo.** Whether any live connector combines `enableDbTopicSplit=true`+`exactlyOnce=true` or `bufferCount>0`+`exactlyOnce=true` needs an operator-side check. The first combination has an upstream-documented one-time dedup gap on upgrade; the second now throws `ConnectException` at startup unless `bufferFlushTime=0` and `ignorePartitionsWhenBatching=false`. Handle in Phase A.
- **`Version.ARTIFACT_VERSION`'s actual value** versus what any existing JMX dashboard expects — the `version=` segment changes source from the client jar's manifest to the connector's own build constant, independent of the `connector=` question. Check in Phase D.
- **Whether `KeeperStateProvider` tests need a live ZK ensemble**, and whether upstream's new embedded-Keeper `clickhouse_keeper_map.xml` mount in `ClickHouseBase` would let them run without one. Check in Phase F.
- **`src/integrationTest/.../helper/ClickHouseAPI.java`** — a modify/delete where upstream deleted the file. Out of the `./gradlew test` gate, so low priority: confirm it still compiles or drop it.
- **CI wall-clock against `timeout-minutes: 45`** with a roughly tripled suite. Watch on the first green run.

### Process constraints carried into every phase

- Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` — this shell sets it to the literal string `"null"`, breaking Ryuk. Already baked into `config.json`'s `build_command`/`test_command`. Docker is colima profile `vz`.
- Do the merge on a scratch branch, never directly on the already-published `custom_build`. Tag the pre-merge commit. Keep the merge as one identifiable commit so `git revert -m 1` stays viable.
- Do not interleave routine `.planning/` commits with in-progress conflict resolution — a commit made while `<<<<<<<` markers exist anywhere in tracked source is the failure signal. Batch planning-doc updates before the merge starts or after it is resolved and green.

## Sources

### Primary (HIGH confidence)
- [`STACK.md`](STACK.md) — build/dependency migration: structural diff at all three refs, catalog-by-catalog version comparison, the five fork build additions with re-application recipes, wrapper decision, CI survival analysis, 15-class JaCoCo audit
- [`FEATURES.md`](FEATURES.md) — per-feature disposition for all eight fork features, the 13 new config options with collision checks, `DebeziumRecordConvertor` / `KeyToValue` / `Utils.handleException` deep dives, CDC breaking-change categorisation
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — v1.6.0 data-flow trace with line citations, per-collision-file resolution shapes, test-infrastructure verdicts, the dependency-ordered stage sequence, structural blockers, the `git merge-tree` inventory
- [`PITFALLS.md`](PITFALLS.md) — 17 pitfalls each with symptom → cause → prevention → detection → owning phase, the per-test BUG/CONSCIOUS/RECONCILE triage, production-visible regression table, the "looks done but isn't" checklist
- `git merge-tree --write-tree custom_build v1.6.0` (git 2.50.1), re-run by the orchestrator to settle the count discrepancy between ARCHITECTURE.md and PITFALLS.md
- Upstream `CHANGELOG.md` at `v1.6.0`, read from 1.2.9 forward — cited by version and PR/issue number throughout

### Carried forward
- [`../milestones/v1.2.9_gg4-tests-research/PITFALLS.md`](../milestones/v1.2.9_gg4-tests-research/PITFALLS.md) — live pitfalls carried into this milestone (RE2 vs Java regex, `windows-1252`, Testcontainers/colima, the `GenericRecord` snapshot rule, container-per-class performance); pitfalls describing a resolved past state were dropped
- [`../RETROSPECTIVE.md`](../RETROSPECTIVE.md) — `TESTCONTAINERS_HOST_OVERRIDE` hazard and the tracked-`.planning/` caveat

---
*Research completed: 2026-09-18*
*Ready for roadmap: yes*
