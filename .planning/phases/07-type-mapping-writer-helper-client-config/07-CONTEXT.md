# Phase 7: Type Mapping, Writer, Helper Client & Config - Context

**Gathered:** 2026-09-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Re-express the fork behaviours that have no upstream anchor against upstream `v1.6.0`'s rewritten mapping, writer, helper-client and config layer, and get `src/main` compiling again.

Delivers: `Type.DYNAMIC` alongside upstream's native `Type.JSON`; upstream's rewritten `Column` adopted with the `Dynamic` dispatch ported in; upstream's immutable `Table` adopted as-is with every fork call site rewritten to the 5-arg constructor; the RowBinary fail-fast for `DYNAMIC`/`UNKNOWN` (with `JSON` handed to upstream's flag-gated `case JSON:`); `targetTableFilter`'s server-side `match(table, '<regex>')` re-expressed over `List<Table.TableDesc>`; `shouldSkipDescribeRow` re-expressed over upstream's local-`List<Column>` describe loop; UTF-8 on every `InputStreamReader` in the resolved helper client; `targetTableFilter` / `jsonInsertColumns` config keys with their parsing contracts; `async_insert` un-forced; the thirteen new upstream options confirmed at defaults; and FREE-02 / FREE-03 recorded.

Gate: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava` succeeds tree-wide, and `./gradlew testUnit` (Docker-free) is green.

Out of this phase: the JMX `connector=<name>` re-plumb and everything else in `ProxySinkTask` / `SinkTaskStatistics` (Phase 8); the metadata-free `insertFormat=json` keep/drop decision and `jsonInsertColumns`' new consumer (Phase 9); every harness judgement call — MBean sweep extension, shared-container strategy, Testcontainers `2.0.5` verification, the six conflicted sink-task test files (Phase 10); any live ClickHouse-backed verification (Phase 11).

</domain>

<decisions>
## Implementation Decisions

### The Compile Blocker

- **D-01:** `src/main` currently fails `compileJava` on **exactly one error**, and it is not in any Phase 7 file: `ClickHouseSinkTask.java:149` carries `@Override` on `onPartitionsRevoked(Collection<TopicPartition>)`, which **connect-api `4.1.1` does not declare** (removed from `SinkTask` in Kafka 3.0; verified by `javap` against the resolved jar). Upstream declares the method because its catalog pins connect-api `2.7.0`; the pre-merge fork never had it (upstream added it in `c36ab418`). **This is a new fork/upstream incompatibility created by the locked Kafka `4.1.1` override, not merge fallout.**

- **D-02:** The fix is to **delete the `@Override` annotation only** — one line — keeping upstream's method and body. Rationale: `connect-api` is declared `api(...)` and Connect workers serve those classes parent-first, so the bridge still fires if the connector is ever loaded by a pre-3.0 worker, and it costs nothing under `4.1.1`. Add a comment naming connect-api `4.1.1` as the reason. Do **not** delete the method and do **not** add a fork-only shim interface.
  - **Reversibility:** reversible — one annotation, one file.

- **D-03:** Phase 7 lands D-02 as a **named, separately-committed change**, but `MERGE-CHECKLIST.md`'s `ClickHouseSinkTask.java` row **stays `pending` and owned by Phase 8**. Phase 7 appends a note to the row recording that the one-line compile fix landed early and in which commit. Phase 11's zero-pending assertion is unaffected.

- **D-04:** The API gap is also recorded as a **named entry in `MERGE-CHECKLIST.md` § Recorded Facts** — connect-api `4.1.1` removed `SinkTask.onPartitionsRevoked`, upstream still declares it under its `2.7.0` pin, the fork carries a deliberate one-line divergence. Feeds Phase 11's SUITE-03 dropped-changes record and warns the next sync.

### The Test Gate and the Phase 10 Boundary

- **D-05:** ROADMAP criterion #2's `./gradlew testUnit` gate is **kept**, reached by a **mechanical carve-out** from Phase 10 — not by downgrading the gate and not by fixing the whole test tree. Measured blast radius that forces this:
  - `uniqueConnectorName(TestInfo)` — used by **9** fork test files, **absent** from the merged `ClickHouseBase`
  - `@Tag("clickhouse")` — absent, so `testUnit` would start Docker
  - `FromVersionConditionExtension` moved to `com.clickhouse.kafka.connect.test.junit.extension` under `src/testFixtures` — **1** stale import (`JsonColumnTest.java`)
  - dead `Table` API — **4** fork test files (`ClickHouseHelperClientForkUnitTest` ×11 sites, `RowBinaryUnsupportedTypeGateTest`, `MetadataFreeJsonInsertUnitTest`, `ClickHouseHelperClientForkLiveTest`)

- **D-06:** From the fork's `ClickHouseBase`, Phase 7 restores **`@Tag("clickhouse")` and `uniqueConnectorName(TestInfo)` + its counter, and nothing else.** The `@AfterEach cleanupLeakedMBeans` sweep, the shared-container-per-JVM-fork strategy, `newTestDatabaseName`, and the helper methods all stay Phase 10. The fork's base is `static`-based while upstream's is `@TestInstance(PER_CLASS)` with instance methods, so a wholesale restore is not available anyway — but both restored members are shape-independent.

- **D-07:** `MERGE-CHECKLIST.md`'s `ClickHouseBase.java` row **stays `pending` and owned by Phase 10**; Phase 7 appends a note recording that the tag + connector-name sliver landed early and in which commit. Same bookkeeping pattern as D-03. Do not split the row and do not flip it.

- **D-08:** `shouldSkipDescribeRow` is re-expressed as **`shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)`**. Upstream's describe loop builds a local `List<Column>` and constructs the immutable `Table` only at the end, so the fork's `table.getAllColumnsMap()` lookup is unavailable; the loop maintains an incremental name→`Column` map alongside `columns`. The helper stays a pure static function with O(1) parent lookup, and `ClickHouseHelperClientForkUnitTest`'s 11 cases build the map directly instead of a `Table`. Rejected: an accumulating `List<Column>` with a linear scan per DESCRIBE row, and constructing an interim `Table` on every iteration.

- **D-09:** **HARN-04 is already satisfied by the mechanical merge** — exactly one `ClickHouseTestHelpers.java` exists, under `src/testFixtures`, in package `com.clickhouse.kafka.connect.sink.helper` (the fork's import path is unchanged). Phase 7 does not touch it; Phase 10 confirms by grep as its requirement states.

### The JSON-Path Default Phase 9 Inherits

- **D-10:** Phase 7 **re-applies nothing of the metadata-free `insertFormat=json` bypass** — not `shouldSkipTableMetadata`, not the synthetic `Table` in `getTable`, not the `start()` / `updateMapping()` short-circuits, not the `cleanupExtraFields` bypass. The mechanical merge already removed all of it; `src/main` is pure upstream there today. **Phase 9's blocking JSONP-01 checkpoint therefore inherits "dropped" as its default**, and "keep" is the option that costs work. This matches the milestone's conflict policy (prefer upstream unless the fork's tests fail) and means Phase 7 never writes code Phase 9 might delete.
  - **Reversibility:** reversible within the milestone — the fork's version is recoverable from `pre-v1.6.0-sync` at any time; Phase 9 owns the call.

- **D-11:** The fork's JSON-path tests — `MetadataFreeJsonInsertUnitTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, and R-01's labelled loss test — are made to **compile against the new `Table`, then `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")`**. Nothing is deleted and no assertion is weakened; Phase 9 either removes the annotation or deletes the test outright. Phase 11's skip accounting sees a named reason. Rejected: tagging them `clickhouse` to dodge `testUnit` (hides a red test behind a tag that means something else) and leaving them failing (gives up a binary gate for Phases 7–8).

- **D-12:** Phase 7's SUMMARY carries a short **evidence brief for Phase 9's checkpoint**: that upstream's `writeRecordsJson` inserts via `client.insert(tableName, dataWriter, format, settings)` at `ClickHouseWriter.java:1307` (the `(table, columns, data, …)` overload `jsonInsertColumns` attached to is gone from that call site, confirming JSONP-03's premise), that `jsonInsertColumns` must be redesigned as a field filter inside `writeRecordsJson`, and which fork tests are `@Disabled` pending the call. JSONP-01 opens on evidence rather than a fresh investigation.

### Configuration

- **D-13:** Both config keys and all their parsing land in Phase 7, per CFG-01: `TARGET_TABLE_FILTER` and `JSON_INSERT_COLUMNS` `configDef.define` entries, and the `targetTableFilter` / `jsonInsertColumns` / `jsonInsertFormatExplicit` fields with their parsing. `jsonInsertColumns` and `jsonInsertFormatExplicit` are briefly read by nothing (their consumer returns in Phase 9 if JSONP-01 says keep) — that is a one-phase gap, not a defect. `JsonInsertColumnsUnitTest` and `AsyncInsertSettingsUnitTest` pass here.

- **D-14:** CFG-02: `this.addClickHouseSetting("async_insert", "0", false)` is back at `ClickHouseSinkConfig.java:240` after the merge and must be **commented out again**, exactly as the fork had it.

- **D-15:** CFG-03 is proved by **a Docker-free parametrized defaults test plus a written inventory**. The test builds a default `ClickHouseSinkConfig` and asserts each of the thirteen getters returns upstream's default (catching a silently-dropped `configDef.define` on the next sync); the SUMMARY table names each option, its default, and its consumer (which a getter assertion cannot show). The thirteen, enumerated from the merged file: `auto.evolve`, `auto.evolve.ddl.refresh.retries`, `auto.evolve.struct.to.json`, `bufferCount`, `bufferFlushTime`, `clickhouseClientInsertTimeoutMs`, `clusterName`, `debeziumCDCEnabled`, `enableReplicaPinning`, `errors.retry.timeout`, `reportInsertedOffsets`, `retryOnSocketException`, `ssl_socket_sni`.

### Fork Delta: What Comes Back

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

### Commit Shape

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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone contract
- `.planning/ROADMAP.md` § "Phase 7: Type Mapping, Writer, Helper Client & Config" — the five success criteria this phase is judged against
- `.planning/ROADMAP.md` § "Merge Mechanics (applies to every phase)" — scratch-branch rule, commit cadence, the `env -u TESTCONTAINERS_HOST_OVERRIDE` Gradle invocation, `testUnit` as the cheap gate
- `.planning/REQUIREMENTS.md` § "Locked decisions this milestone is built on" — the 14 decisions plans must not re-open, in particular "Conflict policy", "RowBinary + JSON", "Kafka Connect API 4.1.1" and "R-01 decided at a blocking-human checkpoint inside the JSON-path phase"
- `.planning/REQUIREMENTS.md` § "Type & Mapping Layer (MAP)", § "Writer (WRT)", § "Helper Client (HLP)", § "Configuration (CFG)", § "Free Wins — Verify and Record (FREE)" — this phase's 17 requirements
- `.planning/REQUIREMENTS.md` § "Out of Scope" — what must not be attempted
- `.planning/MERGE-CHECKLIST.md` § "Section 1 — Conflicted" — the 12 rows owned by Phase 7, and the two Phase-8/Phase-10 rows Phase 7 annotates but does not flip (D-03, D-07)
- `.planning/MERGE-CHECKLIST.md` § "Recorded Facts" — where D-04, FREE-02 and FREE-03 are written down

### Prior phase context
- `.planning/phases/06-merge-groundwork-build-migration/06-CONTEXT.md` § "Merge Commit Shape" — D-01/D-02, the byte-identity invariant that makes every Phase 7 re-application a reviewable diff against upstream
- `.planning/phases/06-merge-groundwork-build-migration/06-CONTEXT.md` § "Build-File Fidelity" — the JDK-17 test-JVM pin and `windows-1252` encoding that make HLP-04 load-bearing

### Re-expression sources (per-file)
- `.planning/research/ARCHITECTURE.md` — per-collision-file resolution shapes for `Column` / `Table` / `ClickHouseWriter` / `ClickHouseHelperClient` / `ClickHouseSinkConfig`
- `.planning/research/FEATURES.md` — per-feature disposition for all eight fork features
- `.planning/research/PITFALLS.md` — 17 pitfalls with symptom → cause → prevention → detection → owning phase
- `.planning/research/SUMMARY.md` § "Must-not-silently-lose list" — the five fork behaviours that vanish with no conflict marker and no compile error, each with its canary test

### Environment and process
- `.planning/STATE.md` § "Blockers/Concerns" — the `TESTCONTAINERS_HOST_OVERRIDE="null"` hazard, colima profile `vz`, the five silently-vanishing fork behaviours, and CR-01 (the fail-open invariant script, to be fixed before Phase 11)
- `.planning/config.json` — `build_command` / `test_command` carry the `env -u` prefix; `granularity: coarse`, `mode: yolo`
- `AGENTS.md` § "Coding Conventions", § "Correctness & Safety First", § "Security" — Google Java Style via Spotless, no secrets or record data logged in plaintext
- `.planning/codebase/CONVENTIONS.md` — SLF4J level conventions (TRACE per-record, DEBUG batch-level, INFO lifecycle), which D-16 rests on

### Files this phase touches directly
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java` — add `DYNAMIC` after upstream's `JSON` (MAP-01)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:173` — `startsWith("JSON")` already present upstream; add the `startsWith("Dynamic")` branch after it (MAP-01/MAP-02)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java` — **no edit**; call sites are rewritten instead (MAP-03)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — `:220` log line, `:562` (confirm FREE-02, no edit), `:651` upstream `case JSON:`, `:672` fail-fast throw, `:1307` (read-only evidence for D-12), `:1355` payload logging, plus `setTargetTableFilter` in `start()` (WRT-01..05)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` — `:315` stdout, `:333-371` `showTables*` over `List<Table.TableDesc>`, `:392-503` both describe loops, `:437`/`:497` log level, `:466` UTF-8, plus `buildTableFilterClause` / `shouldSkipDescribeRow` / the builder's `setTargetTableFilter` (HLP-01..04)
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:240` — comment out `async_insert`; plus both config keys and their parsing (CFG-01/CFG-02)
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:149` — delete the `@Override` only (D-02); the rest of the file is Phase 8's
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — `@Tag("clickhouse")` + `uniqueConnectorName(TestInfo)` only (D-06)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` — 11 sites rewritten to the map-based `shouldSkipDescribeRow` (D-08)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java`, `.../db/MetadataFreeJsonInsertUnitTest.java`, `.../db/helper/ClickHouseHelperClientForkLiveTest.java` — rewritten to the 5-arg `Table` / `List<Table.TableDesc>`
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` — stale `FromVersionConditionExtension` import updated to `com.clickhouse.kafka.connect.test.junit.extension`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **`ClickHouseTestHelpers` already landed correctly.** Exactly one copy exists, under `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/`, in the same package the fork's tests import — so no fork test import needs changing for it. The orphaned `src/test` copy was removed by the mechanical merge as a `modify/delete` resolution, which is what HARN-04 asks for.
- **`java-test-fixtures` is already wired** (`build.gradle.kts:31`), and `testFixturesApi`/`testFixturesImplementation` blocks are in place, so `src/testFixtures` classes are on the test compile classpath without build work.
- **The fork's canary tests survived the merge untouched** because they are fork-only files, not conflicted paths: `JsonDynamicTypeMappingTest` (MAP-01), `RowBinaryUnsupportedTypeGateTest` (WRT-01..03), `ClickHouseHelperClientForkUnitTest` / `ForkLiveTest` (HLP-01..04), `JsonInsertColumnsUnitTest` (CFG-01), `AsyncInsertSettingsUnitTest` (CFG-02). They need API re-expression, not rewriting.
- **`buildTableFilterClause` transplants verbatim.** It is already a pure static function of a `String` and touches nothing upstream changed.

### Established Patterns

- **Upstream's describe loops already count `numColumns` before skipping.** Both `describeTableV1` (`:405-413`) and `describeTableV2` (`:463-473`) increment for non-subcolumn rows ahead of the alias/materialized/ephemeral skip. HLP-03 is satisfied by inserting the fork's subcolumn skip after that increment.
- **Upstream's `Table` is immutable by construction:** `Table(String database, String name, boolean hasDefaults, List<Column> columns, int numColumns)` with `addColumn` private and `registerValidColumn` internal. Every mutation-style call site must become list-building.
- **Upstream already owns `Type.JSON` and a `case JSON:` write path** (`ClickHouseWriter.java:651`), so the fork's JSON half of both gates is redundant; only `DYNAMIC`/`UNKNOWN` remain fork behaviour.
- **`Table.SIZE_FIELD_MATCHER` upstream is `.+\.size[0-9]+$`** — it does not match a bare `.size` suffix, which is precisely the case HLP-02 calls out as needing the fork's rule.

### Integration Points

- **`ClickHouseWriter.start()` → `ClickHouseClientBuilder.setTargetTableFilter(csc.getTargetTableFilter())` → `ClickHouseHelperClient.showTables()`** is the single chain WRT-05 + HLP-01 + CFG-01 form; breaking any link silently disables server-side table filtering with no compile error. This is one of the five "vanish with no conflict marker" behaviours.
- **`ClickHouseHelperClient.extractTablesMapping` (`:554`) iterates `List<Table.TableDesc>`** and compares `numColumns` for cache staleness — the reason HLP-03's increment ordering matters beyond bookkeeping.
- **Phase 8 depends on Phase 7 compiling**, because `ProxySinkTask` constructs both the writer and the helper client. D-02's one-line fix in `ClickHouseSinkTask.java` is the first link in that chain.

</code_context>

<specifics>
## Specific Ideas

- **"`src/main` is pure upstream right now" is the organising fact of this phase.** Phase 6's mechanical merge means every fork behaviour re-applied here shows up as an isolated diff against `v1.6.0`. D-19's one-commit-per-behaviour shape exists to preserve that property all the way to Phase 11's SUITE-03 record — not for tidiness.
- **The compile surface was measured, not assumed.** `./gradlew compileJava` fails on exactly **one** error, and it is in a Phase 8 file for a reason that has nothing to do with the merge (Kafka `4.1.1` vs upstream's `2.7.0` pin). Two of the phase's named risks — "upstream effectively rewrote `ClickHouseWriter.java`" and the `Table` constructor change — produce **zero** compile errors today precisely because nothing fork-specific is left in `src/main` to break. The work is additive re-expression, not conflict repair.
- **Phase 9's default was chosen deliberately, not inherited by accident.** The user was shown that leaving the bypass out makes "keep" the expensive option at JSONP-01's checkpoint, and chose it anyway on the milestone's prefer-upstream policy. That framing must survive into Phase 9 — see D-12's evidence brief.
- **Both free wins landed better than the fork's own versions.** Upstream's `Map(K, Nullable(V))` fix carries a `getMapValueType() != null` guard the fork's patch lacked, and upstream's `describeTableV2` fixed the `database`-parameter bug the fork had merely documented as dormant. Record them as wins, do not re-fix.

</specifics>

<deferred>
## Deferred Ideas

- **A standing charset-regression test** that fails when any `InputStreamReader` in `ClickHouseHelperClient` lacks an explicit charset. Considered under D-17 and not adopted — the grep proves HLP-04 as worded. Worth revisiting if a future sync reintroduces an unguarded reader.
- **Re-expressing the fork's `describeTableV1` UTF-8 `BufferedReader` rewrite.** Dropped under D-17 because V1 is outside both the test matrix and production. If client V1 ever re-enters the matrix, this returns with it.
- **Splitting `MERGE-CHECKLIST.md`'s `ClickHouseBase.java` row** into a Phase 7 half and a Phase 10 half. Rejected under D-07 in favour of a note, to avoid editing the generated Section 1 structure.
- **Sweeping `src/test` / `src/testFixtures` for further Kafka `2.7.0`-vs-`4.1.1` API gaps** beyond `onPartitionsRevoked`. Not pursued as its own investigation; any such gap surfaces as a compile error under D-05's `compileTestJava` requirement and gets fixed there.
- **Adopting `retryOnSocketException`** — `REQUIREMENTS.md` § Future Requirements calls it "worth deliberately enabling" against a client-V2 persistent-connection failure mode the fork is exposed to today. CFG-03 keeps it at its upstream default; enabling it is feature work, out of scope.
- **Fixing CR-01** (`.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep` error as "no match", so Invariant 2 can report clean on a broken pathspec). Carried from Phase 6's security audit; must be fixed before Phase 11, which gates publication on that script running unattended. Not Phase 7 work.

</deferred>

---

*Phase: 7-Type Mapping, Writer, Helper Client & Config*
*Context gathered: 2026-09-18*
