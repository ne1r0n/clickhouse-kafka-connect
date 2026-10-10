# Pitfalls Research

**Domain:** Upstream sync (`v1.6.0` -> `v1.7.0`) plus follow-up hardening on a brownfield, production-running fork of the ClickHouse Kafka Connect sink
**Milestone:** v1.7.0_gg1-sync
**Researched:** 2026-10-07
**Confidence:** HIGH for the merge-derived and code-read findings (evidence cited); MEDIUM for compression wire semantics (client defaults confirmed from the 0.9.6 jars, server behaviour inferred from upstream's changelog and the fork's own live-test Javadoc); LOW for what mockito 5.24 / okhttp 5.5 / jackson 2.22.3 / org.json 20260814 contain (web search returned no release notes; the dependabot branches are one-line version bumps)

## How this was researched

All read-only: `git merge-tree <base> custom_build v1.7.0` (legacy trivial mode, writes nothing, base = `v1.6.0` = `789a57c2`), `git diff v1.6.0 v1.7.0` and `git diff v1.6.0 custom_build`, `javap` against the cached `clickhouse-client-0.9.6.jar` and `client-v2-0.9.6.jar`, and file reads of the current tree. Nothing was checked out or merged.

**The merge shape, so phases can plan against it.** Files changed on both sides: `VERSION`, `build.gradle.kts`, `gradle/libs.versions.toml`, `ClickHouseSinkConfig.java`, `StructToJsonMap.java`, `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `ClickHouseTestHelpers.java`. `merge-tree` reports 13 conflict regions in 6 of them (VERSION, libs.versions.toml, ClickHouseSinkConfig x4, ClickHouseWriter x1, ClickHouseHelperClient x7). Everything else in the delta is a **clean auto-merge with no marker**, and several of those clean hunks are semantically wrong (Pitfalls 1 and 2). `KeeperStateProvider.java` and `AtLeastOnceBufferStrategy.java` merge clean because the fork never edited them. `.github/` is untouched upstream (`git diff v1.6.0 v1.7.0 -- .github` is empty), so `tests.yaml` has no merge risk this time.

---

## Critical Pitfalls

### Pitfall 1: Upstream #820's null-Map block lands as dead code behind the fork's own MAP-null branch, and the RowBinary stream desyncs

**What goes wrong:** The fork's `doWriteColValue` `case MAP:` already begins with `if (value.getObject() == null) { BinaryStreamUtils.writeNull(stream); break; }` (`ClickHouseWriter.java:612-621`, written for a *Nullable child* of an Array/Map). Upstream #820 inserts its own block immediately after `Map<?, ?> mapTmp = ...` (`if (mapTmp == null) { if (defaultsSupport) writeVarInt(stream, 0); break; }`). `merge-tree` reports this hunk as clean (context lines are the fork's `break; }`), so after the merge upstream's block is unreachable and compiles without a warning. Meanwhile the other #820 hunks do apply: `doWriteCol` now writes the NonNull marker for `ARRAY || MAP` (both the `defaultsSupport` and plain branches), `StructToJsonMap` now yields `Data(schema, null)` for a null optional Map, and `cleanupExtraFields` substitutes an empty map. The result for a top-level non-nullable `Map` column holding `null`: `doWriteCol` writes `0x00`, then the fork's branch writes `0x01`. ClickHouse reads a map of size 0 and then treats the stray `0x01` as the first byte of the next column. That is corruption (wrong values or "Cannot read all data"), not an exception the connector sees.

**Why it happens:** Two independent fixes for the same switch case, each correct alone, adjacent enough to auto-merge. The fork's branch cannot tell "nullable child" from "top-level non-nullable column".

**How to avoid:**
- Resolve by intent, not by textual side: keep the fork's `writeNull` only when `col.isNullable()` (the nested-Nullable case it was written for); otherwise fall through to upstream's behaviour (`defaultsSupport ? writeVarInt(0) : write nothing more`, because `doWriteCol` already wrote the `0x00` that doubles as the empty-map size).
- Take upstream's `nullMapTypeTest` and `nullMapTypeJsonInsertTest` (`ClickHouseSinkTaskWithSchemaTest`, +68 lines) and `StructToJsonMapTest` **unmodified** as the acceptance gate. They assert `count of empty maps == ceil(n/10)`. Do not make them green by editing them.
- Add a Docker-free byte-level test for the writer's MAP branch (null top-level, null nested value, both with and without `defaultsSupport`) so the invariant does not depend on a server.

**Warning signs:** `nullMapTypeTest` red or, worse, row count right but `countEmptyMaps` wrong; any diff in which the fork's `if (value.getObject() == null)` is still the first statement under `case MAP:` and upstream's `mapTmp == null` block is below it.

**Phase to address:** Re-apply-by-intent phase (WR-01/WR-02/#820 reconciliation), immediately after the mechanical merge commit.

---

### Pitfall 2: The fork's "Temporary fix" LZ4 capability rule survives next to upstream's new one and neutralises the lz4-java 1.10.4 bump

**What goes wrong:** `build.gradle.kts:134-140` is a fork-authored block (`git blame`: Alexander Chashnikov, 2026-09-18) that does `select("at.yawk.lz4:lz4-java:1.10.2")` for capability `org.lz4:lz4-java`. Upstream `v1.7.0` removes nothing from `v1.6.0` here, because `v1.6.0` never had it; it adds a *different* candidates-based rule at the top of the file plus `implementation(libs.lz4.java)` (`lz4Java = "1.10.4"`). The two blocks sit in different hunks, so they auto-merge: two rules for one capability, one hard-selecting 1.10.2. Depending on Gradle's rule ordering the outcome is either the build failing with a capability-selection error (loud) or lz4-java quietly resolving to 1.10.2, so the user-approved 1.10.4 bump (a CVE-driven bump) never takes effect while `libs.versions.toml` says it did (silent).

**Why it happens:** The fork's workaround and upstream's proper fix address the same problem; nothing flags a duplicate rule. FORK-DELTA row 51 even says "the LZ4 capability resolution above travels with the Connect API pin", which encourages keeping it.

**How to avoid:** Delete the fork's block when merging; upstream's rule is a superset (it selects the `at.yawk.lz4` candidate whatever its version). Prove it with `./gradlew dependencyInsight --dependency lz4-java --configuration runtimeClasspath` (and `testRuntimeClasspath`, `clickhouseDependencies`): expect `at.yawk.lz4:lz4-java:1.10.4`, no `org.lz4:lz4-java`. Record the resolved version as a Recorded Fact the way the previous sync did (MERGE-CHECKLIST Facts 1-2). Check the shaded jar contents too (`shadowJar` output must contain one `net/jpountz/lz4` set).

**Warning signs:** Two `withCapability("org.lz4:lz4-java")` occurrences in `build.gradle.kts`; `dependencyInsight` showing 1.10.2.

**Phase to address:** Mechanical merge / build re-apply phase; verified again in the dependency-bump phase.

---

### Pitfall 3: Retiring D-17 leaves a pin that passes while protecting nothing; the load-bearing control is `compress=false`, not `lz4`

**What goes wrong:** Evidence from the 0.9.6 jars: V1 `ClickHouseClientOption.COMPRESS` = key `compress`, default `true`, "Whether the server will compress response it sends to client"; V2 `ClientConfigProperties.COMPRESS_SERVER_RESPONSE` = key `compress`, default `true`; request compression (`DECOMPRESS` / `COMPRESS_CLIENT_REQUEST` = `decompress`) defaults to `false`. The D-17 failure was `compress=1` responses arriving as ZSTD native blocks (`Invalid LZ4 magic byte: '-112'`). Upstream #841 therefore does two things: sets `network_compression_method=lz4` when `setCompressionMethod` is true, **and unconditionally** sets `COMPRESS=false` on V1 (`getDefaultClientOptions()` and the node options) and `compressServerResponse(false)` on V2. With no compressed responses requested, `network_compression_method` has no HTTP consumer left in the connector. Consequences:
- `NetworkCompressionPinLiveTest` ("ping succeeds and `getSetting('network_compression_method') = 'lz4'`") will stay green even if `compress=false` is deleted, because lz4 still decodes; and it will stay green if the lz4 pin is deleted, because nothing is compressed. It no longer pins the ZSTD protection it was written for.
- `NetworkCompressionPinUnitTest` asserts the pin (`clickhouse_setting_network_compression_method=lz4`, `CUSTOM_SETTINGS`) but not `compress`. Upstream's `ClickHouseHelperClientTest.testSetCompressionMethod_*` likewise pin only the lz4 side.
- `setCompressionMethod=false` is a safe opt-out only while `compress` stays false. An operator who has `compress=true` in `jdbcConnectionProperties` (applied *after* `compressServerResponse(false)` on V2, so it wins) and sets `setCompressionMethod=false` re-creates the ZSTD break on 26.9+.

**Behaviour/performance versus the fork's pin:** v1.6.0_gg1 received server-compressed (LZ4 native) responses for describe/state/ping/version queries; v1.7.0 receives them uncompressed. Those responses are small (metadata, one state row), so the cost is bytes on the wire for `DESCRIBE` of very wide tables or thousands of tables (the fork's `targetTableFilter` exists precisely because discovery can be large). Inserts are unaffected: request compression was already off and insert responses are tiny. Expect no measurable throughput change; do not spend a phase benchmarking it, but do record "responses uncompressed" in the rollout checklist.

**Why it happens:** The upstream flag is named after the lz4 setting, so it reads as "the" fix; the `COMPRESS=false` lines are not behind the flag and are easy to overlook in the diff.

**How to avoid:**
- Add Docker-free pins for the load-bearing half: V1 `getDefaultClientOptions().get(COMPRESS)` is `false`, V1 node options contain `compress=false`, V2 `getClient().getConfiguration().get("compress")` is `"false"`. Keep the existing lz4 pins (they now duplicate upstream's, and that is fine; they are cheap).
- Add a live test with `setCompressionMethod=false` on `latest` (26.9+, ZSTD default): ping, a typed query and a describe must all succeed. That is the test that fails if anyone ever removes `compress=false`.
- Optionally keep one informational negative control (client forced to `compress=true` with no lz4 fails on 26.9) under `Assumptions` so server drift does not turn it into a red gate.
- Rewrite the FORK-DELTA row: D-17's fork patch is **retired**, the removal condition ("once the client decodes ZSTD") is superseded, and the new standing fact is "responses are uncompressed by upstream design; lz4 is defence in depth".
- Decide explicitly whether `jdbcConnectionProperties` carrying `compress=true` is supported; document it in the rollout checklist.

**Warning signs:** The live pin test passes after you comment out `compress=false`; a rollout where `setCompressionMethod=false` is recommended without the `compress` caveat.

**Phase to address:** Re-apply-by-intent phase (D-17 retirement); rollout checklist phase for the operator text.

---

### Pitfall 4: `setCompressionMethod` is not applied on every client the fork creates, and omission fails silently

**What goes wrong:** `ClickHouseClientBuilder.setCompressionMethod` defaults to `ClickHouseSinkConfig.setCompressionMethodDefault` (= `true`), so a builder site that forgets `.setCompressionMethod(csc.isSetCompressionMethod())` quietly ignores an operator's `false`. `src/main` has exactly three builder sites: `ClickHouseWriter.start()` (line 84), the temp client in `ClickHouseWriter` (line 1575), and `KeeperStateProvider` (line 48); `ClickHouseTestHelpers.createClient` (testFixtures:434) is a fourth outside `src/main`. In the merge, `ClickHouseWriter.start()` is one of the **conflicted** hunks (fork's `.setTargetTableFilter(...)` vs upstream's `.setCompressionMethod(...)` on adjacent lines); choosing one side by habit drops the other. Both V1 and V2 sub-clients are built in the helper's constructor, so one missed builder site misses both. Every V1 request path in `src/main` goes through `getDefaultClientOptions()` (6 sites in the helper plus `KeeperStateProvider:99` and `ClickHouseWriter:1547`), which is good; a new direct `ClickHouseClient.builder()` without it would bypass `COMPRESS=false`.

**How to avoid:** Resolve the `start()` hunk to contain **both** lines. Add an invariant (Docker-free, reusing the style of `.planning/scripts/verify-merge-invariants.sh` or a source-scanning JUnit test): number of `new ClickHouseHelperClient.ClickHouseClientBuilder(` in `src/main` equals the number of `.setCompressionMethod(csc.isSetCompressionMethod())` in `src/main`, and every `ClickHouseClient.builder()` in `src/main` is followed by `.options(...getDefaultClientOptions())`. Extend `NewUpstreamConfigDefaultsUnitTest` (it pins "all thirteen" upstream v1.6.0 options) with the fourteenth: `setCompressionMethod` default `true`, canonical key and the `set_compression_method` snake-case alias.

**Warning signs:** `git grep -c setCompressionMethod src/main` is not 3 builder sites plus the config; a live run with `setCompressionMethod=false` where `getSetting('network_compression_method')` still reads `lz4` from the writer's client.

**Phase to address:** Mechanical merge (conflict resolution) and re-apply phase (invariant).

---

### Pitfall 5: The R-01 routing fix changes where the Struct bytes go, not whether exactly-once survives, but three consequences are easy to miss

**What goes wrong:** R-01 lives in `doInsertBatch` (`ClickHouseWriter.java:234-246`): `SCHEMA` and `DEBEZIUM_CDC` records go to `doInsertRawBinary` unless `csc.isBypassRowBinary()`, and under metadata-free mode the synthetic `Table` has zero columns, so the RowBinary loop writes zero bytes and ClickHouse accepts a zero-row insert. The fix is a one-condition change (`csc.isBypassRowBinary() || shouldSkipTableMetadata()`), but:

1. **Exactly-once and existing state.** Deduplication is token-based (`QueryIdentifier.getDeduplicationToken()` = `topic-partition-min-max`, applied to the insert at `ClickHouseWriter.java:1155/1377`), not content-hash based, and a zero-row insert never records a token. So changing the write path does not break dedup for a retry that straddles the upgrade: the earlier attempt inserted nothing. The AGENTS.md "byte-identical retry" rule is about not changing batch composition inside one code version; it is not violated by routing. **But** every offset range already marked `AFTER_PROCESSING` in KeeperMap under R-01 stays "done": the state machine will drop a replay of those ranges (`SAME`/`PREVIOUS` + `AFTER`). Backfilling lost data in exactly-once mode needs both a consumer-group offset reset **and** clearing/advancing the KeeperMap state rows for those topic-partitions. Rollout checklist item, not a code item.
2. **Type fidelity of the JSON writer is now on the hook.** `writeRecordsJson` `case SCHEMA` serialises `struct.get(field)` with Jackson (`DataJson.OBJECT_MAPPER`, `NON_NULL` inclusion plus `JacksonStructSerializer`). Nested `Struct` is handled; logical types are not converted: `Timestamp`/`Date` leave as `java.util.Date` (epoch millis by Jackson's default), `Decimal` as `BigDecimal`, bytes as base64. Those are exactly the cases where a `DateTime`/`DateTime64`/`Decimal`/`String` target column can parse differently than RowBinary would. This is the pre-existing `bypassRowBinary=true` behaviour, now reachable by a new population. The flipped R-01 test must not be a 2-column `Int32`/`String` happy path.
3. **Null handling differs from RowBinary.** `NON_NULL` drops null top-level fields, so nulls become ClickHouse defaults rather than `NULL`, and `input_format_skip_unknown_fields=1` (set by default at `ClickHouseSinkConfig.java:244`) silently ignores payload fields with no column.

**How to avoid:**
- Put the routing condition in the shared `SCHEMA`/`DEBEZIUM_CDC` branch (one predicate, `shouldSkipTableMetadata()`, already the single source of truth). Do not key it on `csc.getInsertFormat()` (the upstream `getOrDefault(INSERT_FORMAT, "node")` typo makes that true for default-configured connectors; see the Javadoc at `ClickHouseWriter.java:1606-1615`).
- RED first, same commit as the fix: flip `ExplicitJsonInsertPathLiveTest`'s R-01 pin (`explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch`) to typed read-back, and add a typed Struct matrix on `latest` × V2 (and V1 JSON writer if the suite still builds it): Int/String/Nullable, `Timestamp`, `Date`, `Decimal`, bytes, nested Struct, Array, Map (including null optional Map, which exercises #820's `StructToJsonMap` change on this path), a Struct with extra fields, a tombstone in the middle of a batch, and `jsonInsertColumns` set to a strict subset.
- AGENTS.md says "never delete/weaken existing tests". Flipping a pin is the opposite of weakening, but record the approval (the milestone goal is the approval) in the commit message and FORK-DELTA so the R-01 Javadoc ("PIN - NOT AN ENDORSEMENT") is removed deliberately, not by a refactor.
- Interactions to state explicitly in the docs: `bypassFieldCleanup` is moot in metadata-free mode (`cleanupExtraFields` returns the payload untouched because of `shouldSkipTableMetadata()`); `jsonInsertColumns` is applied server-side via the 5-arg `client.insert(table, columns, ...)` and therefore works for Struct exactly as for Map; `auto.evolve` stays skipped (CR-01 guard).

**Warning signs:** The flipped test only asserts `countRows`; the routing change is keyed on anything other than `shouldSkipTableMetadata()`; no test with a `Timestamp` or `Decimal` Struct field.

**Phase to address:** R-01 phase. Rollout checklist phase for the backfill/KeeperMap note.

---

### Pitfall 6: Operators whose pipelines "succeed" with 0 rows today will see data appear, or a new class of errors, on upgrade

**What goes wrong:** PROJECT.md Key Decisions and MERGE-CHECKLIST Fact 14 rest on "operator fact 2": no live pipeline takes the Struct + explicit-JSON route (the explicit-JSON connectors emit schemaless `Map` values). That fact is the *only* reason this is low-risk, and it was true on 2026-09-18. Upgrading changes behaviour for any connector with `insertFormat=json` and a schema-carrying converter (Avro/Protobuf/JSON-with-schema): from silent zero rows to real rows landing from the next offset (old offsets are not replayed), and any JSON parse or type error that RowBinary would never have raised now surfaces as a non-retriable server error, which with `errors.tolerance=none` fails the task and with `all` fills the DLQ.

**How to avoid:** The rollout checklist must make the operator re-confirm fact 2 per connector before deploy (`value.converter`, `insertFormat`, whether the source table has rows after a test window), list the new failure modes, and say that `errors.tolerance` determines whether the first symptom is a stopped task or a DLQ burst. CHANGELOG and FORK-DELTA must carry the behaviour change (the milestone already requires both). Keep a canary connector first.

**Warning signs:** A connector config with `insertFormat=json` and `value.converter` not `JsonConverter` with `schemas.enable=false`; table row counts that are flat for a connector whose lag is moving.

**Phase to address:** Rollout checklist phase.

---

### Pitfall 7: Keeper state read is exactly-once-critical, the change is coupled to `COMPRESS=false`, and standalone CI coverage is thinner than it looks

**What goes wrong:** Upstream changed `KeeperStateProvider.getStateRecord` from `response.getSummary().getResultRows()` (plus `firstRecord()`) to materialising `response.stream()` and counting rows (`KeeperStateProvider.java:106-119` in `v1.7.0`). The commit message ("Fixed issued with state record and disabled server compression as not needed", `a7cc5ec9`) ties the two together. The summary header's `result_rows` is a response-header value; once response compression is off, whether it is populated at all is a server streaming detail. The failure mode of getting this wrong is the worst one in the connector: a `0` count returns `State.NONE`, so a restarted task treats every partition as new data and relies solely on ClickHouse's dedup window. Taking #841's `COMPRESS=false` without this change (or this change reverted by a "keep ours" on a file the fork never edited) is the dangerous combination; they must travel together.

Coverage reality: `exactlyOnceStateMismatchTest` (the only test named for exactly-once state) is `Assumptions.assumeTrue(isCluster || isCloud)` and is one of the 18 named skips on standalone `latest`. What does exercise the state read on standalone is `ClickHouseSinkTaskBufferTest.strictChunkingDedupesReplayedFlush[withRestart=true]` and the other `strictChunking*` replay tests (the standalone container mounts `clickhouse_keeper_map.xml`). Those pass if either the state read works or the dedup token alone suppresses the duplicate, so a broken state read can hide behind dedup.

**How to avoid:**
- Add a direct `KeeperStateProvider` live test: write `BEFORE`, then `AFTER` state, construct a **new** provider, assert `getStateRecord` returns the written `(min, max, state)` and not `NONE`; and a no-row case returns `NONE`. Run it with the default flag and with `setCompressionMethod=false`.
- Do a one-time mutation check recorded in the plan summary: temporarily restore `getSummary().getResultRows()` and show the new test (or a `strictChunking` replay) goes red under `COMPRESS=false`. A guard that has never been seen red is not a guard.
- Verify the executed (not skipped) set for `strictChunking*` in the run output, using `--rerun` (Gradle echoes cached results otherwise).
- Minor: the new code collects all rows then reads `records.get(0)`. V1 record objects may be reused views; with one row per key this is harmless, but do not "improve" the multi-row warn branch without checking.
- Treat the file as high-risk in review (AGENTS.md Key Files table) even though the diff is 6 lines.

**Warning signs:** `exactlyOnce=true` task restart logs `Read state record ... with NONE state` for partitions that have prior state; duplicate rows after a worker restart on a table with no dedup window.

**Phase to address:** Re-apply/merge phase (carry the change with #841) and full-suite gate phase (mutation check, executed-set check).

---

### Pitfall 8: Merge-time decisions that "take upstream wholesale" silently revert fork build pins

**What goes wrong:** `gradle/libs.versions.toml` conflicts on the first two lines: fork `clickHouseDriver = "0.9.6"  # FORK OVERRIDE (upstream 0.9.5)` and `kafka = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)` versus upstream `clickHouseDriver = "0.9.6"` and `kafka = "2.7.0"`. The mechanical-merge invariant (byte-identical to upstream for conflicted paths) resolves to `kafka = "2.7.0"`, which must be re-applied or `connect-api` drops to 2.7.0 and `onPartitionsRevoked` (kept without `@Override`, FORK-DELTA row 32) plus the LZ4 coupling change meaning. Separately, `0.9.6` stops being a fork override and becomes upstream's own value, so the `# FORK OVERRIDE (upstream 0.9.5)` comment, FORK-DELTA row 51, the PROJECT.md "Stack" line and MERGE-CHECKLIST Fact 1 all become stale. `clickhouse-jdbc` disappears from the catalog and from `integrationTest` dependencies (clean).

**How to avoid:** Re-apply `kafka = "4.1.1"` in the by-intent commit and prove the four resolved-version facts again with `./gradlew dependencies` (client modules `0.9.6`, `connect-api` `4.1.1`, `slf4j-reload4j` `2.0.17`, lz4 `at.yawk` `1.10.4`). Update the stale documentation in the same plan. Extend `.planning/scripts/verify-merge-invariants.sh` with the new expectations rather than writing a new script.

**Warning signs:** `kafka` resolving to 2.7.0; compile error on `onPartitionsRevoked`; docs still claiming a 0.9.6 fork override.

**Phase to address:** Mechanical merge phase (invariants) and by-intent re-apply phase.

---

### Pitfall 9: A green build after the merge is not evidence the fork survived (the previous milestone's lesson, applied to this diff)

**What goes wrong:** Seven fork behaviours vanished from the v1.6.0 merge with no marker and no compile error, and `CLICKHOUSE_VERSION_DEFAULT` reverted to a pinned LTS unseen for four phases. This delta is smaller, but the same classes are present: auto-merged hunks inside conflicted files (Pitfall 1), duplicate build rules (Pitfall 2), a builder default that hides a missing call (Pitfall 4), and test helpers (`ClickHouseTestHelpers` is "changed in both"; `merge-tree` says its one hunk merges cleanly, but the previous merge dropped helper methods the same way).

**How to avoid (a checklist the merge phase can execute):**
1. Before merging, list every fork-touched file the upstream delta touches: `comm -12 <(git diff --name-only v1.6.0 custom_build | sort) <(git diff --name-only v1.6.0 v1.7.0 | sort)`. Review each *non-conflicting* hunk in those files, not just the markers.
2. After the mechanical commit: `ClickHouseVersionDefaultUnitTest`, `NewUpstreamConfigDefaultsUnitTest`, `NetworkCompressionPinUnitTest`, `MBeanNameUnitTest`, `MetadataFreeJsonInsertUnitTest` and the rest of `testUnit` must be green with Docker stopped; `compileTestJava` and `compileIntegrationTestJava` must both compile.
3. Compare the symbol set of the fork's test helpers before and after (`git grep -h "public static" -- src/testFixtures` on `custom_build` vs the merge result); a method that disappeared is the failure.
4. Full-suite gate: two consecutive runs, the second with `--rerun`, `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, expected count = 663 + upstream's new tests (roughly +10: `nullMapTypeTest`, `nullMapTypeJsonInsertTest`, `StructToJsonMapTest` x2, three `ClickHouseHelperClientTest` cases, buffer and schemaless additions, plus the Avro-union JSON fixture), skips identical by (class, method) to the 18 named in FORK-DELTA "Named skip inventory", and `ClickHouseTestHarnessTest` confirming the server image is `latest`.
5. The new upstream tests that touch JSON (`union_two_records` now targets a `JSON` column) are the most likely to interact with the fork's `shouldSkipDescribeRow` JSON/Dynamic child skip and RowBinary fail-fast gate. Run them first and read the failures before triaging anything else.

**Phase to address:** Mechanical merge phase (steps 1-3) and full-suite gate phase (steps 4-5).

---

## Moderate Pitfalls

### Pitfall 10: WR-02 "fail loudly on null" collides with upstream #820 if scoped to top-level columns

**What goes wrong:** WR-02 says MAP/ARRAY branches should throw on a `null` landing in a non-nullable *slot*. Upstream #820 deliberately defines the opposite for a top-level non-nullable `Map` column: null becomes an empty map (writer, `StructToJsonMap`, `cleanupExtraFields`), asserted by upstream tests. Implementing WR-02 on `doWriteCol`'s top-level path breaks upstream's tests and diverges from upstream permanently, which the "prefer upstream" policy exists to avoid. (The `ARRAY` null branch already behaves this way: `doWriteCol` writes NonNull and `ARRAY` writes an empty array.)

**How to avoid:** Scope WR-02 to *nested* slots only: a `null` map **value** whose `col.getMapValueType()` is non-nullable, or a `null` array **element** whose `col.getArrayType()` is non-nullable, throws a `DataException` naming the column and path. Leave top-level null Map/Array as upstream defines it. WR-01 (a real `null` inside `Map(K, Nullable(V))`) is exactly the fork's `writeNull` branch and is compatible with upstream once Pitfall 1 is resolved; its end-to-end test (typed read-back that the `NULL` arrives as `NULL`, not default) belongs with it. Write down the split in FORK-DELTA so the next sync sees the reasoning.

**Phase to address:** Re-apply-by-intent phase, same plan as Pitfall 1.

---

### Pitfall 11: P3-WR-03's premise is stale; implementing it as written would reject types the writer now supports

**What goes wrong:** The follow-up says `INT128`/`INT256`/`UINT128`/`UINT256` all surface a bare "Data schema validation failed" message. `ClickHouseWriter` in the merged v1.6.0 tree writes `INT128` and `UINT128` (`doWriteColValue` cases at lines 576/581 and 916/921, with a bit-width table entry at 267-268). Only `INT256`/`UINT256` have no write path. A fail-fast gate over all four would turn working `Int128`/`UInt128` columns into hard errors.

**How to avoid:** Re-derive the unsupported set from the code before writing the plan (grep the `switch` cases), gate only `INT256`/`UINT256`, and add live round-trips proving `Int128`/`UInt128` still land. Correct the PROJECT.md Active text when closing it.

**Phase to address:** Metadata/config follow-up phase.

---

### Pitfall 12: The MBean name changes again on this release, not only once (D-07)

**What goes wrong:** The rendered name is `com.clickhouse:type=ClickHouseKafkaConnector,connector=<name>,name=SinkTask<id>,version=<Version.ARTIFACT_VERSION>[,topic=<t>]` (`SinkTaskStatistics`, MERGE-CHECKLIST Fact 11). `version=` comes from the `VERSION` file, so `v1.6.0_gg1` -> `v1.7.0_gg1` changes every ObjectName again. Anyone upgrading from a pre-v1.6.0 build sees two changes (new `connector=` segment, new `version=` source: the jar's `0.9.6` before the Phase 6 merge), anyone on `v1.6.0_gg1` sees one. The unit tests (`MBeanNameUnitTest`) pin the formatter with literal `version=0.9.6`, deliberately not the build constant, so nothing will go red when `VERSION` moves; D-07 chose to record rather than pin. JMX scrapers with exact-name dashboards or alerts go blank with no error.

**How to avoid:** The rollout checklist should render the exact before and after strings for both task and topic MBeans for the version the operator is coming from, and recommend wildcard/pattern ObjectName queries (`com.clickhouse:type=ClickHouseKafkaConnector,*` filtered on `connector`) so the next bump does not repeat the incident. Also note the client user-agent (`CONNECT_CLIENT_NAME`) carries the same version string.

**Phase to address:** Rollout checklist phase.

---

### Pitfall 13: Dependency bumps are verified by different tasks than the one CI runs

**What goes wrong:** The four dependabot branches (`upstream/dependabot/gradle/{jackson-2.22.3, com.squareup.okhttp3-okhttp-5.5.0, org.json-json-20260814, org.mockito-mockito-junit-jupiter-5.24.0}`) are each a one-line `libs.versions.toml` change and mutually independent. But: `okhttp` is declared only in the `integrationTest` suite (`build.gradle.kts:158`), which `./gradlew test` and CI never compile, so a break there is invisible until someone runs `integrationTest`; `org.json` is a `testFixtures`/test dependency; `mockito` only affects tests; `jackson` is the one that reaches production and the shaded jar (`OBJECT_MAPPER` with `NON_NULL`, `JacksonStructSerializer`, `ConvertToMapOrJson`). `jacksonAnnotations = "2.22"` is pinned separately from `jackson`. I could not obtain release notes for any of the four new versions, so the risk assessment is "unknown, verify", not "safe". Mockito 5 uses the inline mock maker by default (self-attaching ByteBuddy agent); on JDK 17 that is quiet, but on a JDK 21+ launcher you get dynamic-agent warnings and JDK 24+ makes it stricter. CI uses JDK 17 temurin and the Gradle toolchain is 17, so the exposure is local-shell JDK drift (the dev box has JDK 21 on PATH).

**How to avoid:** Apply the bumps **after** the sync merge and **after** R-01/#820 work, one commit per bump, so any red run is attributable. Gate each with: `compileIntegrationTestJava` (covers okhttp), `testUnit` with Docker stopped (covers mockito/org.json without a server), then the full suite. For jackson, run the writer JSON-serialisation tests (`ConvertToMapOrJsonTest`, the new Struct-to-JSON matrix from Pitfall 5, `Dynamic`/`JSON` round trips) and diff `dependencyInsight --dependency jackson` to confirm no second Jackson version enters the shaded jar via the ClickHouse client. Keep the scope to the five approved bumps; do not opportunistically take `clickhouse-client 0.10.0` (explicitly out of scope).

**Phase to address:** Dependency-bump phase.

---

### Pitfall 14: Publication mechanics replay the v1.6.0 CI cancellation and risk publishing more than intended

**What goes wrong:**
- `tests.yaml` triggers `on: [push]` with `concurrency: group: ${{ github.workflow }}-${{ ...github.sha }}`, `cancel-in-progress: true`. Pushing the branch and the tag at one SHA produces two runs in one group and cancels the first; CI-06's "exactly one run" locator had to be narrowed last time. Fixing the group alone (`github.ref`) removes the cancellation but yields **two** green runs for one SHA, which still breaks "exactly one run per published SHA".
- The fix needs to be on the published tree *before* the tag is pushed, i.e. it ships in the same push as the first run it affects.
- Local-only refs: tag `v1.6.0_gg1-sync` (`a0f9026f`, a planning-only SHA) exists locally but not on `origin`; `git push --tags` or `--follow-tags` would publish it and start a run on a docs-only commit. `.planning/.gsd-ws-arg` is untracked and must not be added.
- `custom_build` is protected: no force pushes, ever. `sync/v1.6.0` is 15 planning-only commits ahead of `custom_build` and is **not** a descendant of `c61d90ce` (the publish was a merge made on `custom_build`; `git merge-base --is-ancestor custom_build sync/v1.6.0` is false, though `git diff custom_build sync/v1.6.0 -- . ':!.planning'` is empty). Publishing the planning history therefore happens by merging the branch that contains it, not by fast-forward, and it publishes everything in `.planning/` for the first time including the milestone archives.
- The v1.7.0 `VERSION` conflict resolves mechanically to upstream's `v1.7.0`; if a rehearsal CI run or any artifact is produced before the by-intent commit sets `v1.7.0_gg1`, it carries the wrong version in `Version.ARTIFACT_VERSION`, the MBean name and the client user-agent.
- `upstream/main` is ambiguous (`refs/upstream/main` is a stale ref pointing at `789a57c2` while `refs/remotes/upstream/main` is `379b8d01`); `git merge upstream/main` would resolve to the wrong one with only a warning.

**How to avoid:**
- Make the workflow trigger tag-free (`on: push: branches: ['**']`, no `tags`) *and* keep the `github.sha` group, so a tag push cannot start a run at all; or, if tag CI is wanted, restrict by `if:`. Either way, push the branch, wait for the single green run, then push the tag alone, and re-run the CI-06 locator unchanged (exactly one match for `head_sha` + workflow path, no `conclusion != cancelled` exception).
- Merge by tag name: `git merge --no-ff v1.7.0` on the scratch branch (create a rollback tag first, `pre-v1.7.0-sync`, on `custom_build` and push it before anything else).
- Push refs explicitly by name (`git push origin custom_build`, `git push origin v1.7.0_gg1`); never `--tags`, `--all`, `--mirror` or `--force*`.
- Run the same credential scan used at Phase 5 (D-09) over the actual planning delta to be published, and record the human approval; one-way publication was gated by a blocking-human checkpoint in both previous milestones and that pattern was right each time.
- Rehearse on a scratch branch for CI (as run `37617508949` did), but remember each push to any branch runs the workflow.
- Do not read a green run on an earlier SHA as evidence for the published one; the published SHA is the `--no-ff` merge commit.

**Warning signs:** Two Actions runs for one `head_sha`; a `cancelled` conclusion anywhere in the publication window; `git push` output listing a tag you did not name.

**Phase to address:** CI hygiene phase (workflow change, before publication) and publication phase.

---

### Pitfall 15: Environment and harness traps from the previous milestone still apply

**What goes wrong:** Each of these has already cost time once.
- `TESTCONTAINERS_HOST_OVERRIDE="null"` in the Claude Code shell breaks Ryuk; always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (as `config.json`'s `build_command` does).
- A second acceptance run without `--rerun` echoes the first run's cached results.
- The test JVM runs `file.encoding=windows-1252` on purpose; any new reader of ClickHouse output (new tests in this milestone included) must specify UTF-8.
- `latest` floats: ClickHouse 26.9 -> a later release can change another default mid-milestone. Budget a gap-closure slot in the final phase; keep the server version printed in the run log.
- Docker (colima) must be up for every `ClickHouseBase` test; `testUnit` must be proven with it stopped.
- New sink-task tests create their tables before `start()`; client-v2 `GenericRecord` iteration yields a reused view (snapshot it).
- Verification reports go stale in sequential phases; expect another `override_closeout` or re-verify at close.

**Phase to address:** Every phase's plan template; full-suite gate phase for `--rerun` and the executed-set comparison.

---

## Minor Pitfalls

### Pitfall 16: `spotlessApply` churn on upstream-owned files

AGENTS.md says to run `spotlessApply`. Applied to files merged from upstream it reorders imports and creates gratuitous fork delta (the fork already carries import-reorder-only diffs in `StructToJsonMap`, `ProxySinkTask`, `Record`, `Data` and others, recorded in MERGE-CHECKLIST). `KeeperStateProvider` and `ClickHouseWriter` are touched upstream in this delta; reformatting them inflates the next sync's conflict surface. Run Spotless on fork-authored files only, or revert import-only hunks in upstream-owned files before committing.

### Pitfall 17: Test-debt items that change tests need the same "typed read-back" discipline

`JsonColumnTest` (row counts only), `HeaderToValue` collision contract (WR-03), `ClickHouseBase.tearDown()` drop-failure handling (a swallowed drop is the only isolation between classes in a fork), and `ClickHouseSinkTaskJsonInsertFormatTest` try/finally are small, but a `tearDown()` that starts failing loudly could turn a transient server hiccup into an unattributable red in an unrelated class. Decide the policy (fail the class vs log) explicitly and keep the MBean leak sweep untouched.

### Pitfall 18: Small config/metadata fixes with outsized blast radius

`USERNAME` ConfigDef default (`passwordDefault` -> `usernameDefault`) changes what Connect's REST `/config` validation reports; runtime unaffected, but anything that diffs validated config output will see it. `Column.extractColumn` argument swap is dead code today, so it needs a unit test that reaches it or it stays unproven. `shouldSkipDescribeRow` at any depth must not widen what it drops: pin with `Nullable`, `Array`, `Map`, `Tuple`, `JSON`, `Dynamic` nesting. `cleanupExtraFields` operand order (P9-IN-01): swap to `shouldSkipTableMetadata() || csc.isBypassFieldCleanup()`; behaviour-neutral by design, so a regression test would be a null-`csc` unit probe.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Keep both LZ4 capability rules "because the build passes" | No build-file edit | Silent 1.10.2 pin defeats the CVE bump; two sources of truth | Never |
| Keep the fork's `NETWORK_COMPRESSION_METHOD_*` constants alongside upstream's | Smaller diff | Two names for one setting; the next reader assumes the fork's pin still exists | Never; take upstream's constants |
| Edit upstream's `nullMapTypeTest` to pass | Green sooner | Permanent silent divergence from upstream on RowBinary null maps | Never |
| Flip the R-01 test but assert only `countRows` | Fast | Type-fidelity regressions in the JSON writer reach production under a new population | Never (project rule: typed read-back) |
| Close P3-WR-03 as written | Matches the backlog text | Rejects working `Int128`/`UInt128` | Never; re-derive from code |
| Bump all four dependencies in one commit | One CI run | A red run is not attributable; okhttp break hidden until `integrationTest` | Only if all four are individually proven green first |
| Tag push alongside branch push | One command | Cancelled run, ambiguous CI evidence | Never once the workflow fix lands |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| ClickHouse 26.9+ HTTP | Treating `network_compression_method=lz4` as the protection | `compress=false` on V1 and V2 is the protection; lz4 is defence in depth |
| ClickHouse client V1 | Setting the option on one object | Node options, `getDefaultClientOptions()` and every `ClickHouseClient.builder()` must agree |
| ClickHouse client V2 | Assuming `serverSetting` order does not matter | The pin must be applied before the `jdbcConnectionProperties` loop so operator values win (kept in upstream's version) |
| ClickHouse Keeper (`KeeperMap`) | Reading state via the HTTP summary header | Count streamed rows (upstream #841 follow-up); never mix with the old code path |
| Kafka Connect API 4.1.1 vs upstream's 2.7.0 | Taking upstream's catalog line | Re-apply the fork pin and re-prove resolved versions |
| GitHub Actions | Keying concurrency on `github.sha` while triggering on tags | No tag trigger, or key on ref and still require exactly one run per SHA |
| Git remotes | `git merge upstream/main` | `git merge --no-ff v1.7.0` (ambiguous ref) |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Uncompressed responses from `COMPRESS=false` | Larger `DESCRIBE`/`SHOW`/state responses | Keep `targetTableFilter` selective; accept as a one-time, small cost | Thousands of tables or very wide tables across a slow link; not on the insert path |
| `KeeperStateProvider` materialising all rows | Extra heap on a state read | One row per key by construction; leave as is | Only if the `KeeperMap` key space is corrupted with duplicates |
| JSON writer for Struct (post-R-01) | Higher CPU than RowBinary, per-record `HashMap` + Jackson | Documented as the cost of metadata-free mode; operators wanting RowBinary performance should grant `DESCRIBE`/`SHOW` and drop `insertFormat=json` | Large batches of wide Structs |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Publishing `.planning/` history without a scan | Credentials or hostnames in 15+ never-published commits and milestone archives | Repeat the Phase 5 D-09 credential scan on the exact delta before the push; record approval |
| Pushing local-only tags/branches | Unintended refs become public and start CI | Push named refs only |
| Log lines for the new JSON path | Payload leakage at TRACE (existing `writeRecordsJson` trace prints payload) | Do not add logging of records; AGENTS.md rule stands |
| lz4-java stale | Known CVE exposure if 1.10.2 survives (Pitfall 2) | `dependencyInsight` proof, recorded |

## UX Pitfalls (operators are the users)

| Pitfall | Operator Impact | Better Approach |
|---------|-----------------|-----------------|
| Rollout notes list "MBean rename" without strings | Dashboards blank, no one knows what to search | Print exact old/new ObjectNames per source version |
| `setCompressionMethod` documented as "the compression switch" | Operator disables it on a server profile that locks the setting, and also sets `compress=true` | Explain that responses are uncompressed regardless, and that the flag only controls the lz4 setting |
| R-01 described as "fixed" | Operators assume lost history is recovered | State that old offsets are not replayed and that exactly-once state must be cleared to backfill |
| A server profile that makes `network_compression_method` read-only | Every query fails with a readonly-setting error right after upgrade | Rollout step: test with `setCompressionMethod=false` first on such clusters (T-11-38, previously accepted risk) |

## "Looks Done But Isn't" Checklist

- [ ] **Merge:** `case MAP:` in `doWriteColValue` has exactly one reachable null branch per case (nullable child vs top-level), and upstream's `mapTmp == null` block is not dead.
- [ ] **Build:** one `org.lz4:lz4-java` capability rule; `dependencyInsight` shows `at.yawk.lz4:lz4-java:1.10.4`; `connect-api` is `4.1.1`; client modules `0.9.6`.
- [ ] **D-17 retirement:** Docker-free pins for `compress=false` (V1 options, V1 node options, V2 config) exist; a live test with `setCompressionMethod=false` is green on `latest`; FORK-DELTA row rewritten, not just deleted.
- [ ] **Coverage of the flag:** three `src/main` builder sites each pass `csc.isSetCompressionMethod()`; `NewUpstreamConfigDefaultsUnitTest` pins the 14th option.
- [ ] **Keeper:** a direct restart-read test exists and was seen red against the old summary-count code under `COMPRESS=false`.
- [ ] **R-01:** the routing predicate is `shouldSkipTableMetadata()`; the flipped test reads back typed values including `Timestamp`/`Decimal`/nested Struct/null Map; FORK-DELTA "Known costs" no longer leads with R-01 as an open loss, and CHANGELOG records the behaviour change.
- [ ] **WR-02:** throws only for nested non-nullable slots; upstream's two null-map tests unmodified and green.
- [ ] **P3-WR-03:** gate covers `INT256`/`UINT256` only; `Int128`/`UInt128` round-trip live.
- [ ] **Counts:** two consecutive full runs, second with `--rerun`, expected test count accounted for, skips identical to the 18 named, server reported as `latest`.
- [ ] **Docker-free:** `testUnit` green with the daemon stopped; `compileIntegrationTestJava` green.
- [ ] **Publication:** workflow no longer runs on tag pushes (or otherwise yields one run per SHA); `pre-v1.7.0-sync` pushed first; branch pushed, one green run located by `head_sha` + path, then tag pushed alone; no `--tags`; credential scan recorded; `VERSION` is `v1.7.0_gg1` in the published SHA.
- [ ] **Rollout checklist:** MBean old/new strings for both source versions, lz4/compress semantics and the `compress=true` caveat, R-01 behaviour change plus KeeperMap backfill note, per-connector re-confirmation of operator fact 2, read-only-setting profile check.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Corrupted rows from the MAP desync reach production | HIGH | Roll back with `git revert -m 1` of the publication merge (the same single-commit revert path recorded for v1.6.0); redeploy `v1.6.0_gg1` (a null in a non-nullable Map column threw "An attempt to write null into not nullable column" there; it did not corrupt); identify affected tables by `empty(map_col)` anomalies and replay from offsets with exactly-once state cleared |
| lz4 1.10.2 shipped by mistake | LOW | Fix the build rule, re-tag as `v1.7.0_gg2`-style follow-up (never move a published tag); no data impact |
| Keeper state read returns `NONE` after restart | HIGH | Stop tasks; verify dedup window covers the replay span; restore the previous build; inspect `KeeperMap` rows; duplicates inside the dedup window are suppressed, beyond it need manual dedup |
| Tag push cancelled the branch run again | LOW | Do not re-run to launder; record the cancelled run, rely on the locator rule only if the workflow fix is absent, and fix the workflow in a follow-up |
| Planning history/credential leak after push | HIGH | Cannot be un-published on a public fork; rotate any exposed secret first, then history rewrite on a non-protected path only with explicit user approval |
| R-01 JSON errors flood DLQ on first deploy | MEDIUM | Set `errors.tolerance=all` temporarily or roll back; fix column types; replay from offsets with exactly-once state cleared |

## Pitfall-to-Phase Mapping

Phase names are working labels; the roadmapper should map them onto the real phase list.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 Dead #820 block / MAP desync | Re-apply by intent (right after mechanical merge) | Upstream `nullMapTypeTest` + `nullMapTypeJsonInsertTest` green unmodified; Docker-free byte-level MAP-null test |
| 2 Duplicate LZ4 rules | Mechanical merge / build re-apply | `dependencyInsight` output recorded as a Fact |
| 3 D-17 pin meaning | Re-apply by intent | `compress=false` unit pins; live `setCompressionMethod=false` test; FORK-DELTA row rewritten |
| 4 Flag missed on a builder site | Mechanical merge + re-apply | Builder-site invariant; 14th-option default pin |
| 5 R-01 routing correctness | R-01 phase | Typed read-back matrix; flipped pin; routing predicate review |
| 6 Operator behaviour change | Rollout checklist | Checklist step with per-connector re-confirmation |
| 7 Keeper state read | Re-apply + full-suite gate | New restart-read test; mutation red/green recorded; executed-set check |
| 8 Build pins reverted | Mechanical merge + re-apply | Four resolved-version facts re-proved; extended invariant script |
| 9 Green build is not evidence | Mechanical merge + full-suite gate | Checklist steps 1-5 |
| 10 WR-02 vs #820 | Re-apply by intent | Nested-slot throw tests plus upstream tests unmodified |
| 11 P3-WR-03 stale premise | Metadata/config follow-ups | Live `Int128`/`UInt128` round-trip; gate on 256-bit only |
| 12 MBean rename | Rollout checklist | Strings in checklist match a live `MBeanRegistrationLiveTest` render |
| 13 Dependency bumps | Dependency-bump phase | Per-bump commits; `compileIntegrationTestJava`, `testUnit` (Docker down), full suite |
| 14 Publication mechanics | CI hygiene, then publication | One run per published SHA via locator; no force; tags pushed by name; scan recorded |
| 15 Environment traps | All phases | `--rerun`, `env -u`, UTF-8, skip inventory |
| 16-18 Minor | Follow-up phases | Per-item tests |

**Research flags for phases:** the compression retirement (Pitfall 3) merits a short live experiment during planning (build a client with `compress=true` and no lz4 against `latest` to confirm the ZSTD failure still reproduces, then with `compress=false` and no lz4 to confirm it does not) because the "lz4 is vestigial" conclusion rests on jar defaults plus the existing live-test Javadoc rather than a direct run. The dependency-bump phase needs a quick release-note read at planning time (not obtainable by search here). Everything else follows patterns already used in the v1.6.0 sync.

## Sources

- `git merge-tree 789a57c2 custom_build v1.7.0` and `git diff v1.6.0 v1.7.0 -- src/main build.gradle.kts gradle .github src/testFixtures` (HIGH; local, read-only)
- `ClickHouseWriter.java:84-96, 233-246, 267, 576-581, 612-621, 916-921, 942-1005, 1269-1311, 1403-1421, 1546, 1575-1590, 1606-1617`; `ClickHouseHelperClient.java:105-120, 180-190`; `build.gradle.kts:134-140`; `docs/FORK-DELTA.md` rows 31-36, 51, 137-153; `.planning/MERGE-CHECKLIST.md` Facts 1, 2, 4, 11, 14; `.planning/PROJECT.md`; `.planning/RETROSPECTIVE.md`; `.planning/STATE.md` (HIGH)
- Upstream commits `a7cc5ec9`, `cf81cd8c`, `e0722b6f`, `5c5ad683`, `f3de9ce2`, `02dd6d71` (CHANGELOG 1.7.0: "default `network_compression_method` value was switched from LZ4 to ZSTD ... client do not detect it") (HIGH)
- `javap` of `clickhouse-client-0.9.6.jar` (`ClickHouseClientOption.COMPRESS` default true, `DECOMPRESS` default false) and `client-v2-0.9.6.jar` (`ClientConfigProperties.COMPRESS_SERVER_RESPONSE` = `compress` default `true`, `COMPRESS_CLIENT_REQUEST` = `decompress` default `false`) (HIGH for defaults, MEDIUM for the conclusion that no HTTP consumer of `network_compression_method` remains)
- Upstream dependabot branches `upstream/dependabot/gradle/*` (one-line version bumps, HIGH); release contents of mockito 5.24.0, okhttp 5.5.0, jackson 2.22.3, org.json 20260814 not retrievable via web search (LOW; verify at planning time)

---
*Pitfalls research for: v1.7.0_gg1-sync (upstream v1.7.0 sync and follow-up hardening on the ClickHouse Kafka Connect fork)*
*Researched: 2026-10-07*
