# Pitfalls Research — Merging Upstream v1.6.0 into `custom_build`

**Domain:** Big-bang `git merge` of 581 upstream commits (`v1.2.9` → `v1.6.0`) into a production CDC (Debezium → ClickHouse) fork, `custom_build` @ `v1.2.9_gg4`
**Researched:** 2026-09-18
**Confidence:** HIGH — every finding below is grounded in `git show`/`git diff main..custom_build`/`git diff main..v1.6.0`/`git merge-tree custom_build v1.6.0` output read directly from this repository (merge-base confirmed `bec167d` = `main` = upstream `v1.2.9`), plus upstream's own `CHANGELOG.md` at `v1.6.0`. No upstream source-code claim below is inferred from memory.

**Method note:** `git merge-tree custom_build v1.6.0` (git 2.50, two-ref form, real three-way merge, no working-tree mutation) was run to get the *authoritative* conflict list — 39 files, enumerated below — rather than guessing from diff stats. This matters: it separates the merge's real dangers (clean, unflagged auto-merges) from its loud-but-laborious ones (real conflicts that still require careful, feature-by-feature manual reconstruction).

## The Authoritative Conflict List

`git merge-tree custom_build v1.6.0` reports **39 conflicting paths**. Every file PROJECT.md's collision table names is in it, plus several PROJECT.md doesn't mention:

```
.github/workflows/tests.yaml, .gitignore, VERSION, build.gradle.kts, gradle/wrapper/gradle-wrapper.properties
src/main/java/.../sink/ClickHouseSinkConfig.java
src/main/java/.../sink/ClickHouseSinkTask.java
src/main/java/.../sink/ProxySinkTask.java
src/main/java/.../sink/db/ClickHouseWriter.java
src/main/java/.../sink/db/helper/ClickHouseHelperClient.java
src/main/java/.../sink/db/mapping/{Column,Table,Type}.java
src/main/java/.../sink/processing/Processing.java
src/main/java/.../transforms/KeyToValue.java
src/test/java/.../sink/ClickHouseBase.java
src/test/java/.../sink/{ClickHouseSinkTaskSchemalessProxyTest,ClickHouseSinkTaskSchemalessTest,
  ClickHouseSinkTaskStringTest,ClickHouseSinkTaskTest,ClickHouseSinkTaskWithSchemaProxyTest,
  ClickHouseSinkTaskWithSchemaTest}.java
src/test/java/.../sink/db/ClickHouseWriterTest.java
src/test/java/.../sink/db/helper/ClickHouseHelperClientTest.java
src/test/java/.../sink/db/mapping/{ColumnTest,TableTest}.java
src/test/java/.../sink/kafa/RangeContainerTest.java
src/test/java/.../sink/processing/ProcessingTest.java
src/test/java/.../sink/util/UtilsTest.java
src/testFixtures/java/.../helper/{SchemaTestData,SchemalessTestData}.java   (rename-detected: was src/test/.../helper/*)
```

Plus 5 **modify/delete** conflicts (fork modified a file, upstream deleted/moved it — git could not rename-detect these, so the fork's version is kept verbatim and orphaned unless someone manually reconciles it against wherever upstream's replacement logic now lives):

```
src/integrationTest/.../helper/ClickHouseAPI.java
src/integrationTest/.../helper/SchemaTestData.java
src/integrationTest/.../helper/SchemalessTestData.java
src/test/java/.../sink/helper/ClickHouseTestHelpers.java
src/test/java/transforms/KeyToValueTest.java
```

**`Utils.java` and (surprisingly) `Processing.java` are NOT in the conflict list**, despite both sides touching them (PROJECT.md's own collision table lists `Utils.java: fork +11`). This is the single most important fact for Section 1 below — it means these two files will merge **cleanly, silently, with no review checkpoint forced by git**.

---

## 1. Silent Semantic Merges — Where Git Says Clean But Is Wrong (or Silently Right)

These are the two files git will **not** flag, plus the specific spots inside the 39 conflicted files where the *textual* conflict is loud but the *correct resolution* is easy to get wrong because the surrounding code has been fully rewritten.

### Pitfall 1: `Utils.java` merges silently — and it's actually good news, if you notice it

**What goes wrong:** `git diff main..custom_build -- Utils.java` shows the fork's *only* change is Spotless import reordering (`-6/+6`, zero semantic diff). Upstream's `Utils.java` is `+123/−12` and rewrites `handleException`/`getRootCause` to recognize `com.clickhouse.client.api.ServerException` (client V2's real exception type) alongside the old `ClickHouseException` (client V1), via a new `isServerError()` helper. Since the fork never touched the method bodies, this file has **zero overlapping hunks** and merges with no conflict marker — upstream's version wins outright.

**Why it happens:** The fork's only edit to this file predates the actual bug discovery; the file was reformatted by Spotless in an earlier fork commit but never patched for the V2 exception-classification gap.

**Prevention:** None needed for the code itself — but the merge must not be treated as "nothing to check here" just because there's no conflict marker. `UtilsTest.java` **does** conflict (it's in the 39), because the fork's test file predates `ServerException`-aware assertions; upstream's `UtilsTest.java` will have its own new cases for `ServerException` error-code classification. Reconcile by taking upstream's test wholesale and re-adding only fork-specific cases (if any exist beyond import order).

**Detection signal:** This is a **positive** signal, not a defect — this clean merge closes the fork's own tracked Active follow-up, verbatim: *"Error classification is dead code on the V2 path... `Utils.handleException`'s `instanceof ClickHouseException` switch never matches under client V2."* Upstream's fix (confirmed in `CHANGELOG.md` under `1.6.0`: *"With client V2, server errors arrive as `com.clickhouse.client.api.ServerException` rather than the V1 `ClickHouseException`... Both exception types now share the same list"* — upstream issue #818) is the exact fix this follow-up wanted. **Action:** close that Active item at milestone completion, citing the clean merge as the mechanism, not a new fix.

**Owning phase:** Upstream-inventory phase (verify and record the closure); merge-resolution phase (confirm `UtilsTest.java` reconciliation, since that file *does* conflict even though `Utils.java` doesn't).

---

### Pitfall 2: `Processing.java` also merges silently — but its constructor arity changed upstream, so `ProcessingTest.java` (which DOES conflict) is where the real work is

**What goes wrong:** Same shape as Pitfall 1 — the fork's `Processing.java` diff is pure import reordering (`-4/+4`), so it carries no overlapping hunks with upstream's `+57/−30` rewrite and merges silently, taking upstream's version. But upstream's `ProxySinkTask` now constructs `Processing` with **five** arguments (`new Processing(stateProvider, dbWriter, errorReporter, clickHouseSinkConfig, statistics)`) instead of the fork's four (`Processing(stateProvider, dbWriter, errorReporter, clickHouseSinkConfig)`) — `SinkTaskStatistics` is now threaded all the way into `Processing` for per-insert timing. `ProcessingTest.java` **is** in the conflict list, because the fork's test constructs the 4-arg form.

**Why it happens:** The constructor signature only shows up as a caller-side (`ProxySinkTask.java`, also conflicted) and test-side (`ProcessingTest.java`) concern — `Processing.java` itself doesn't need to change shape enough to conflict on its own.

**Prevention:** When resolving `ProxySinkTask.java`'s conflict, use upstream's 5-arg `Processing` construction (it's required — `Processing.java` already merged to that shape silently). When resolving `ProcessingTest.java`, take upstream's test file wholesale (it already expects 5 args and a statistics mock) rather than trying to patch the fork's 4-arg test forward.

**Detection signal:** A stale 4-arg `new Processing(...)` call anywhere (most likely surviving in `ProxySinkTask.java`'s conflict resolution if resolved carelessly) is a straight compile error — loud, cheap to catch, but only if the merge-resolution phase actually attempts a compile before moving to the next file.

**Owning phase:** Merge-resolution phase (`ProxySinkTask.java` + `Processing.java` + `ProcessingTest.java` should be resolved as one linked group, not independently).

---

### Pitfall 3: JMX MBean naming — `connector=` is a fork-only production feature that upstream's rewrite structurally cannot preserve by accident

**What goes wrong:** Fork's `ProxySinkTask` owns MBean registration: a private `getMBeanName()` calls `buildMBeanName(connectorName, id, version)`, which renders `com.clickhouse:type=ClickHouseKafkaConnector,connector=<name>,name=SinkTask<id>,version=<v>` (colon-sanitized, `null`→`unknown`). `ClickHouseSinkTask.start()` passes `props.get("name")` into the `ProxySinkTask` constructor for exactly this purpose (JMX-01..04, D-02).

Upstream **moved MBean ownership entirely into `SinkTaskStatistics`**: `ProxySinkTask` no longer imports `MBeanServerUtils` at all; `statistics.registerMBean()`/`statistics.unregisterMBean()` are called from `start()`/`stop()`, and the name comes from a new static `SinkTaskStatistics.getMBeanName(int taskId)`:
```java
return String.format("com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask%d,version=%s", taskId, Version.ARTIFACT_VERSION);
```
**No `connector=` segment, no colon-sanitization, no null-version fallback** (`Version.ARTIFACT_VERSION` is a compile-time constant, never null — the whole reason those two branches exist in the fork's code, `buildMBeanName`'s explicit extraction rationale, evaporates upstream). Upstream also added **per-topic MBeans** (`getTopicMBeanName(taskId, topic)`, tracked in a `Deque<String> topicMBeans`, unregistered alongside the task MBean).

Since `ProxySinkTask.java` is fully conflicted (the whole constructor, `start()`, `stop()` region overlaps), this is a **loud** conflict — git forces a decision. The danger is not silence; it's that **"prefer upstream wherever it passes the fork's tests" is the wrong default policy here**, because upstream's version does not carry `connector=` at all — it will *fail* JMX-01/JMX-02 (which assert the segment's presence/absence), not pass them. A resolver working fast under that stated policy could see the conflict, reach for upstream's clean rewrite (it's objectively better-organized — self-registering, per-topic metrics), and simply not notice that a load-bearing production feature (distinguishing multiple connectors' task MBeans in one Connect worker — exactly the CDC-multi-pipeline case this fork exists for) has no equivalent to fall back on.

**Why it happens:** Upstream had no reason to invent per-connector MBean naming; the fork's whole reason for the feature (multiple Debezium connectors in one JVM) is a fork-specific operational need upstream doesn't share.

**Prevention:** Treat this as a **redesign task, not a merge task**: re-add a `connector=<name>` segment to `SinkTaskStatistics.getMBeanName(taskId)` (or thread `connectorName` into `SinkTaskStatistics`'s constructor the way `ProxySinkTask` did before), and re-apply the colon/null handling if `Version.ARTIFACT_VERSION` can ever be null-like in practice (check its actual definition — if it's a hardcoded string it can't be, which is fine, but the *rendering* logic for whatever version string is used must not silently regress if that assumption ever breaks). Do this **before** running the fork's JMX tests, and treat their zero-green-with-no-changes outcome as the actual gate — "prefer upstream" only applies where upstream's version *already* satisfies the fork's test, which it will not here without this addition.

**Detection signal:** `MBeanNameUnitTest`/`MBeanRegistrationLiveTest` fail with an assertion on the literal string `connector=` missing from the rendered ObjectName. This is unambiguous and specific — not a vague "test failed," a literal substring check. **Any dashboard or alert already querying JMX by `connector=<name>` in production would silently stop matching if this is missed and shipped** — this is the one production-visible regression in this list most likely to be *invisible in CI* (JMX-01..04 catch it) but *catastrophic in prod* if the pinning tests are ever skipped, disabled, or their assertions loosened under merge pressure.

**Owning phase:** Dedicated phase — "JMX naming re-integration onto upstream's `SinkTaskStatistics` self-registration model" — should not be folded into generic `ProxySinkTask.java` conflict resolution; it needs its own plan with JMX-01..04 as the acceptance gate, not just "compiles."

---

### Pitfall 4: The metadata-free JSON insert path (and R-01) has **zero upstream equivalent** — `getTable()`, `cleanupExtraFields()`, `doInsertJsonV2()` are all rewritten around real metadata

**What goes wrong:** The fork's entire "skip table discovery, validation, and field cleanup when `insertFormat=json` is explicit" feature is four cooperating pieces, none of which exist upstream:
- `ClickHouseSinkConfig.isJsonInsertFormatExplicit` — fork-only, no upstream field.
- `ClickHouseWriter.shouldSkipTableMetadata()` — fork-only method.
- `getTable()`'s early return of a synthetic, columnless `new Table(database, tableName)` when metadata-free — this specific branch is the root cause of R-01 (zero rows, no throw, for a `Struct` record).
- `cleanupExtraFields()`'s `|| shouldSkipTableMetadata()` bypass.

Upstream's `getTable()` (confirmed by direct read) has **no such branch at all**: it always calls `updateMapping`/`describeTable`, and if the table still isn't found, **throws** `RuntimeException("Table %s does not exist")` unless `isSuppressTableExistenceException()` is set — the opposite failure mode from the fork's silent zero-row write. Upstream's `writeRecordsJson()` (the JSON-format serializer, used by both V1 and V2 `doInsertJson*`) **already handles `Struct` records correctly** — it builds a real `Map<String,Object>` field-by-field and writes it, *given real table metadata from `cleanupExtraFields`'s `getRootColumnsList()`*. Upstream's `doInsertJsonV2` also has a completely different client-call shape: `client.insert(table.getName(), dataWriter, ClickHouseFormat.JSONEachRow, insertSettings)` using a `DataStreamWriter` **lambda**, not the fork's `client.insert(table.getName(), jsonInsertColumns, data, format, settings)` four-plus-column-list overload. `jsonInsertColumns` (the fork's other WIP feature — explicit column subset for JSON inserts) has **no upstream config key, no upstream call site to attach to** — it must be redesigned as a filter step inside `writeRecordsJson`'s field-selection logic, not passed to a client API that no longer takes a raw column list at that call site.

**Why it happens:** These four fork pieces were built to route around upstream's original (v1.2.9-era) metadata-fetch-or-fail behavior for a specific reason (presumably: some CDC target tables aren't always DESCRIBE-able the way the fork needs, or the team wanted to avoid the "one bad subcolumn drops the whole table" failure mode from the previous milestone's own PITFALLS.md). Upstream's 581 commits solved the *symptoms* differently (real Struct-to-JSON serialization, clearer existence errors) without ever inventing the fork's specific bypass mechanism, because upstream never had the fork's underlying operational reason to build it.

**Prevention:** This is not a merge-and-move-on item. Before writing any conflict-resolution code, get an explicit decision: **does the metadata-free path still need to exist**, given that upstream's rewritten `writeRecordsJson`/`getTable` may have already fixed the original reasons the fork built the bypass (e.g., if upstream's per-column-diff-based mapping refresh, `Utils.computeColumnDiff`, is more resilient than the fork remembers v1.2.9 being)? Whichever way it goes:
- **If kept:** re-implement `shouldSkipTableMetadata()`/the synthetic-`Table` `getTable()` branch/the `cleanupExtraFields` bypass on top of upstream's rewritten methods, and re-verify R-01 still reproduces exactly as pinned (it should, since a columnless synthetic `Table` still makes `writeRecordsJson`'s `cleanupExtraFields` call produce an empty map for `Struct` records the same way it did before — but this must be *proven* with the live test, not assumed).
- **If dropped:** run `ExplicitJsonInsertPathLiveTest`/`MetadataFreeJsonInsertUnitTest`/`R-01`'s labelled test and expect them to fail — that failure is the **conscious, wanted** signal that the loss is gone (Struct records now actually insert via upstream's `writeRecordsJson`, because real table metadata is always fetched). This must be a recorded decision, not a silent side effect discovered by a red test nobody expected.
- **`jsonInsertColumns`:** must be redesigned regardless — the API surface it attached to (`client.insert(table, columns, data, ...)`) no longer exists at that call site in upstream code.

**Detection signal:** `MetadataFreeJsonInsertUnitTest`, `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsUnitTest`, `JsonInsertColumnsLiveTest` — a 100% failure rate on these four files immediately post-merge (before any manual re-implementation) is the **expected, correct** state; it is not evidence the merge went wrong, it's evidence these fork-only features need conscious redesign work. Danger is the opposite: any of these staying green with zero changes right after the mechanical merge would mean git accidentally resolved the conflict by preserving fork logic verbatim against a table shape upstream no longer produces the same way — verify by reading the resolved diff, not just by running tests.

**Owning phase:** A dedicated "metadata-free JSON path redesign" phase, gated on the R-01 keep/drop decision being made explicitly (ideally at the same blocking-human checkpoint style used successfully in the previous milestone).

---

### Pitfall 5: RowBinary fail-fast (TYPE-06) is lost by default — upstream's `default:` branch in the RowBinary switch is still silent

**What goes wrong:** Fork's `doWriteColValue` default branch (originally `LOGGER.error("Cannot serialize unsupported type {}", columnType);` with no throw) was changed in Phase 3 to `throw new DataException(buildRowBinaryUnsupportedTypeMessage(...))`, and the same message is shared with `validateDataSchema`'s `case JSON: case DYNAMIC: case UNKNOWN:` fail-fast gate. Read directly from `v1.6.0`: the `default:` case of the (otherwise heavily rewritten) RowBinary switch is **still**:
```java
default:
    LOGGER.error("Cannot serialize unsupported type {}", columnType);
```
No throw. Upstream *did* add a real `case JSON:` (see Pitfall 6) but never touched the fallback default. Since `ClickHouseWriter.java` is fully conflicted, this single-case region will be inside a giant, mostly-upstream-authored switch statement by the time it's resolved — the fork's one-line `throw` addition is trivially easy to lose while reconstructing hundreds of lines of surrounding, unfamiliar upstream code.

**Why it happens:** The fix is a tiny diff (one `throw` statement) buried in a switch that both sides rewrote at very different scale (fork: +4 lines in this exact spot; upstream: the entire ~1700-line method restructured around `DataStreamWriter`).

**Prevention:** Treat `buildRowBinaryUnsupportedTypeMessage`/the `default:` throw as a named, individually-verified checklist item during `ClickHouseWriter.java` conflict resolution — not something that "comes along for free" because the surrounding validateDataSchema JSON/DYNAMIC/UNKNOWN gate (which is more self-contained and thus easier to spot) got reapplied.

**Detection signal:** `RowBinaryFailFastLiveTest`/`RowBinaryUnsupportedTypeGateTest` targeting `Type.DYNAMIC` specifically (upstream still has no `Type.DYNAMIC` case at all — confirmed, `Column.java`'s upstream diff adds `JSON` handling only) fail with "no exception thrown" instead of the expected `DataException`. This is unambiguous, but only if it's actually run — since `Type.DYNAMIC` columns are rare in ad hoc manual testing, this can pass unnoticed in a manual smoke test and only surface via the specific pinning tests.

**Owning phase:** Same `ClickHouseWriter.java` conflict-resolution phase as Pitfall 4 — these two should be resolved together since they're both inside the same method family.

---

### Pitfall 6: Upstream's own `Type.JSON` RowBinary support is real but narrower than it looks — reconcile, don't just delete the fork's `TYPE-06` JSON case

**What goes wrong:** Upstream independently added `Type.JSON` to the `Type` enum (fork also added `Type.JSON`, same name, **different purpose** — collision risk at the semantic level, not just textual). Upstream's RowBinary switch has a real `case JSON:` block, gated behind a **new** config flag `csc.isBinaryFormatWrtiteJsonAsString()` (note: upstream's own method name has a typo, `Wrtite` — worth grepping for exactly that spelling when searching, not "Write"): if the flag is on and the value is a `Struct`, it's serialized via Jackson (`OBJECT_MAPPER.writeValueAsBytes`) and written as a RowBinary string; if the flag is off, it **throws** `RuntimeException("Writing JSON in binary is not supported yet. Use \`input_format_binary_read_json_as_string=1\`...")`. So upstream's default (flag off) *also* fails fast for `JSON` columns via RowBinary — just with a different exception type (`RuntimeException`, not `DataException`) and a different message than the fork's `TYPE-06` wording.

**Why it happens:** Both sides converged on "fail loudly for RowBinary+JSON" independently, but via different mechanisms and different exception types — the fork's fix predates knowledge of upstream's parallel work, and upstream's flag-gated string-serialization path is a genuinely new capability the fork's tests were never written against.

**Prevention:** Don't treat `Type.JSON`'s RowBinary handling as "already fixed, nothing to do." Decide explicitly whether to adopt upstream's `binaryFormatWrtiteJsonAsString` flag-gated write-as-string capability (new, real value — actual JSON-via-RowBinary support, unlike the fork's fail-fast-only stance) or keep the fork's simpler explicit-error contract. Whichever is chosen, `TYPE-06`'s JSON-specific assertions need to be rewritten to match — they currently assert a `DataException` with the fork's specific wording, which upstream's code will not produce (wrong exception type, wrong message, and only reachable at all when the flag is off).

**Detection signal:** `JsonDynamicRoundTripTest`'s JSON-column RowBinary assertions fail with a `RuntimeException` (upstream's) where a `DataException` (fork's) was expected — this is a **conscious behavior change to adjudicate**, not a bug: upstream's version is a genuine capability upgrade for `Type.JSON` specifically, while `Type.DYNAMIC` still has no equivalent (Pitfall 5 still applies there in full).

**Owning phase:** Same `ClickHouseWriter.java` conflict-resolution phase; explicitly scope it to decide "JSON: adopt upstream's flag-gated write-as-string, DYNAMIC: keep the fork's fail-fast throw" (or whatever the team decides) rather than resolving JSON and DYNAMIC as one undifferentiated case.

---

### Pitfall 7: `targetTableFilter` and `shouldSkipDescribeRow` are 100% fork-only, and upstream's `describeTable` rewrite doesn't even have the fork's subcolumn-skip TODO filled in yet

**What goes wrong:** `ClickHouseSinkConfig` grep against `v1.6.0` confirms **no `targetTableFilter`, no `TARGET_TABLE_FILTER` key exists upstream** — the whole regex-based table-discovery filter (`buildTableFilterClause`, `showTables`'s `database` param becoming a `query` param, server-side `match(table, '...')`) is fork-only and must be re-threaded onto upstream's rewritten `showTables`/`describeTableV1`/`describeTableV2` (`+359` total). Likewise, direct read of upstream's `describeTableV1`/`V2` shows subcolumn handling is still just `isAlias()/isMaterialized()/isEphemeral()` plus a literal code comment `// TODO: add subcolumn filter here?` at the V1 read path — **upstream has not solved the "one unrecognized subcolumn silently drops the whole table" problem the previous milestone's own PITFALLS.md flagged** (Pitfall 2 there). The fork's `shouldSkipDescribeRow` (`.null`/`.size0`/`.size` suffix skip, plus the structural "child of a `JSON`/`DYNAMIC`-typed parent" rule replacing the old `.object_shared_data.` literal) has no upstream counterpart at all.

**Why it happens:** `targetTableFilter` and structural subcolumn-skip are both fork-specific operational needs (restricting metadata discovery to CDC target tables; coping with ClickHouse's evolving `JSON`/`Dynamic` internal representation) that upstream never had a reason to build.

**Prevention:** Both features need to be manually re-applied onto upstream's rewritten `ClickHouseHelperClient`, not merged — treat `showTables`'s signature (upstream may have changed it differently than the fork's `database`→`query` param swap) as the actual current shape to build on top of, not the fork's version. Re-run `FILT-01..04` and `DESC-01..04` as the acceptance gate for this specific file's conflict resolution, expecting 100% failure until the features are reattached.

**Detection signal:** `ClickHouseHelperClientForkUnitTest`/`ForkLiveTest`, `JsonDynamicRoundTripTest`'s subcolumn assertions — failing hard (compile error if `buildTableFilterClause`/`shouldSkipDescribeRow` are missing entirely, assertion failure if present but wired to the wrong query shape).

**Owning phase:** `ClickHouseHelperClient.java` conflict-resolution phase, same phase as the UTF-8 describe-reader re-application below (Pitfall 8) since they're in the same methods.

---

### Pitfall 8: The UTF-8 describe-reader fix (D-15/TYPE-07) is **not present upstream at v1.6.0** — confirmed by direct read

**What goes wrong:** Direct read of `v1.6.0`'s `ClickHouseHelperClient.java` shows the V2 `describeTable` path still constructs `new InputStreamReader(queryResponse.getInputStream())` **with no explicit charset** (line ~466 of that file) — the exact platform-default-charset bug the fork's Phase 3 fixed (`StandardCharsets.UTF_8`) after it was reproduced live and shown to mangle non-ASCII column names and `Enum` members under the test JVM's deliberate `windows-1252` setting. 581 upstream commits did not touch this.

**Why it happens:** This is a latent upstream bug the fork happened to find and fix independently; upstream's own test suite apparently never runs under a non-UTF-8 default charset, so it was never surfaced there.

**Prevention:** Re-apply the explicit `StandardCharsets.UTF_8` construction to whichever `InputStreamReader`/`BufferedReader` call sites survive in upstream's rewritten `describeTableV1`/`V2` — there may now be more than the two the fork originally patched, since upstream restructured the surrounding methods. Grep the resolved file for every `InputStreamReader(` call, not just the ones the fork's diff originally touched.

**Detection signal:** `TYPE-07`'s non-ASCII round-trip assertions (Cyrillic/CJK/non-BMP column names/`Enum` members) fail specifically when run via `./gradlew test` (which sets `file.encoding=windows-1252`) but would pass if run from an IDE with a UTF-8 default — the previous milestone's own documented false-positive trap. This is a **bug**, not a conscious change — upstream never decided anything about this, it simply never wrote the fix.

**Owning phase:** Same `ClickHouseHelperClient.java` conflict-resolution phase as Pitfall 7.

---

### Pitfall 9: `async_insert=0` is still force-set upstream — a one-line loss buried in a large, otherwise-conflicting file

**What goes wrong:** Direct read of `v1.6.0`'s `ClickHouseSinkConfig.java` confirms line 240 is still `this.addClickHouseSetting("async_insert", "0", false);` — unchanged, still force-disabling async insert by default. The fork's only change to this exact region is commenting that single line out (`//this.addClickHouseSetting("async_insert", "0", false);`), a deliberate, tested decision (ASYNC-01..03) that async insert should be left to the server profile / `clickhouseSettings`, not forced off by the connector. Because `ClickHouseSinkConfig.java` is fully conflicted (`+227` upstream vs `+59` fork, overlapping the constructor body extensively), this one-line difference will be surrounded by dozens of other new upstream config fields (`bufferCount`, `clusterName`, `enableReplicaPinning`, etc.) during resolution — easy to silently let the upstream line win by taking "upstream's constructor body" wholesale and forgetting this one commented-out line was intentional.

**Why it happens:** It's a single line in the middle of a ~250-line rewritten constructor; upstream's policy on this setting is simply unrelated to (not superseded by) the fork's — this is a genuine, orthogonal disagreement that 581 commits did not resolve either way.

**Prevention:** Treat "keep `async_insert` un-forced" as an explicit, named checklist item for `ClickHouseSinkConfig.java` conflict resolution, verified by re-running `AsyncInsertSettingsUnitTest`/`AsyncInsertLiveTest` immediately after resolving this file, before moving to the next one.

**Detection signal:** `AsyncInsertSettingsUnitTest`'s "default config sends no `async_insert` setting" assertion fails, finding `async_insert=0` present again. This is a clean, specific, binary signal — but it is **entirely a config-map assertion**; it will not surface via any compile error or via manual smoke testing unless someone specifically inspects `clickhouseSettings`. **This is a production-visible regression that is easy to ship silently**: any connector currently relying on server-profile-controlled async insert would start getting synchronous inserts again post-merge, with no error, just a latency/throughput change that looks like "the merge made things slower" rather than "a one-line config default flipped back."

**Owning phase:** `ClickHouseSinkConfig.java` conflict-resolution phase; call this out by name in that phase's plan, not left to general code review to notice.

---

### Pitfall 10: The `Map(K, Nullable(V))` RowBinary null-marker fix was independently discovered and fixed on **both** sides — near-identical, not identical

**What goes wrong:** Fork's fix (Phase 2, commit `3929ce6`, "approved out-of-scope fix"): `if (col.getMapValueType().isNullable() && mapValue != null) { BinaryStreamUtils.writeNonNull(stream); }`. Upstream's fix (confirmed in `CHANGELOG.md` under an earlier version, PR #687: *"Fixed RowBinary serialization for Map columns with Nullable value types... nullable marker byte was missing"*), read directly from `v1.6.0`'s `ClickHouseWriter.java`: `if (col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null) { BinaryStreamUtils.writeNonNull(stream); }` — the same fix, plus an extra `col.getMapValueType() != null` null-safety guard the fork's version lacks.

**Why it happens:** Both teams found the same real bug independently; upstream's fix happened to ship in a version between `v1.2.9` and `v1.6.0` that this fork branched before seeing.

**Prevention:** Do **not** try to layer the fork's patch onto upstream's already-fixed code (this would either double-write the null marker, corrupting the RowBinary stream, or be a silent no-op if the second write is idempotent-looking but isn't validated). Since `ClickHouseWriter.java` fully conflicts here anyway, take upstream's (more defensive) version verbatim and drop the fork's redundant patch entirely — do not attempt a hand-merge of "both" fixes.

**Detection signal:** The fork's own `WR-01` Active follow-up (*"End-to-end coverage for an actual `null` entry inside a `Map(K, Nullable(V))` column... the null path is code-read, never exercised through RowBinary"*) can likely be **closed for free** here too — verify upstream's version against that exact scenario and, if it holds, record the closure rather than re-deriving the fix.

**Owning phase:** `ClickHouseWriter.java` conflict-resolution phase; explicitly check whether this closes WR-01.

---

## 2. Test Triage: Which of the 371 Fork Tests Fail for the Right Reason vs the Wrong Reason

Legend: **BUG** = failure means a fork feature was lost during merge and must be re-applied. **CONSCIOUS** = failure is the expected, wanted signal that upstream's equivalent (or lack of the underlying problem) makes the old pinned behavior obsolete — needs a human decision to confirm, not a fix. **RECONCILE** = test itself needs updating to match a legitimately different (often better) upstream shape; the underlying feature is fine.

| Test file(s) | What it pins | If it fails post-merge | Why |
|---|---|---|---|
| `MBeanNameUnitTest`, `MBeanRegistrationLiveTest` (JMX-01..04) | `connector=<name>` in ObjectName; colon sanitization; null-version→`unknown` | **BUG** | Upstream's rewrite has no `connector=` segment at all (Pitfall 3) — this is a production feature loss unless manually re-added |
| `AsyncInsertSettingsUnitTest`, `AsyncInsertLiveTest` (ASYNC-01..03) | No forced `async_insert` setting by default | **BUG** | Upstream still forces `async_insert=0` (Pitfall 9) — orthogonal policy, not obsoleted |
| `MetadataFreeJsonInsertUnitTest`, `ExplicitJsonInsertPathLiveTest` (JSON-01..05), the R-01 labelled test | Metadata-free JSON insert path; R-01's zero-row/no-throw silent loss | **CONSCIOUS** — pending an explicit keep/drop decision (Pitfall 4) | If kept and correctly re-applied: still **BUG** if it fails (means re-application was incomplete). If deliberately dropped in favor of upstream's real `Struct`→JSON serialization: R-01's labelled test failing is the *wanted* signal the loss is gone |
| `JsonInsertColumnsUnitTest`, `JsonInsertColumnsLiveTest` (COLS-01..06) | `jsonInsertColumns` explicit column subset | **BUG** (after the redesign in Pitfall 4 is done) | Zero upstream equivalent; call-site API shape changed (`DataStreamWriter` lambda, not `List<String>` overload) — needs redesign, not a straight port |
| `RowBinaryFailFastLiveTest`, `RowBinaryUnsupportedTypeGateTest` — `Type.DYNAMIC` cases | Fail-fast `DataException` for unsupported RowBinary types | **BUG** if failing on `Type.DYNAMIC` | Upstream's `default:` branch is still silent (Pitfall 5); `Type.DYNAMIC` has no upstream equivalent at all |
| `RowBinaryFailFastLiveTest`, `RowBinaryUnsupportedTypeGateTest`, `JsonDynamicRoundTripTest` — `Type.JSON` cases | Same fail-fast contract, but for `Type.JSON` | **RECONCILE** | Upstream has real (flag-gated) `Type.JSON` RowBinary support with a different exception type/message (Pitfall 6) — update the test's expectations, the underlying capability improved |
| `ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest` (FILT-01..04) | `targetTableFilter` regex clause construction, quote/backslash escaping | **BUG** | Zero upstream equivalent (Pitfall 7) |
| `JsonDynamicRoundTripTest` — subcolumn-skip assertions (DESC-01..04) | `.null`/`.size0`/`.size` suffix skip + structural JSON/Dynamic-parent rule | **BUG** | Upstream's `describeTable` still has an open TODO for subcolumn filtering (Pitfall 7); no structural rule exists there |
| `JsonDynamicRoundTripTest` — non-ASCII/Cyrillic/CJK content (TYPE-07) | UTF-8-explicit describe readers | **BUG** | Upstream's `InputStreamReader` still uses the platform default charset (Pitfall 8) |
| `TableOperationFilterTest`, `HeaderToValueTest`, `HeadersToValueTest`, `ConvertToMapOrJsonTest`, and their sink-landing tests | The four fork SMTs and the stale-schema-cache/recursion/union fixes from Phase 2 | Should be **unaffected** — these live entirely in `transforms/`, a package upstream barely touches (only `KeyToValue.java`/`KeyToValueTest.java` conflict; `ExtractTopic*`/the fork's four other SMT files are untouched by upstream). A failure here is a genuine **BUG** in a totally unexpected place — investigate immediately, it should not happen |
| `ProcessingTest`, `RangeContainerTest`, `ColumnTest`, `TableTest`, `UtilsTest`, `ClickHouseWriterTest`, `ClickHouseHelperClientTest`, `ClickHouseSinkTask*Test` (the six conflicted sink-task test files) | Various — mostly construction/wiring assertions against classes whose *production* code moved out from under them | **RECONCILE** | These conflict because the classes they test were rewritten upstream (5-arg `Processing`, `SinkTaskStatistics`-owned MBeans, `DeliveryStrategy`-based `ClickHouseSinkTask`). Take upstream's versions of these test files as the base and port forward only fork-specific assertions (SMT interactions, JMX `connector=` checks, etc.) — don't try to patch the fork's old test bodies onto new constructors |
| `KeyToValueTest.java` (modify/delete conflict) | `KeyToValue`'s stale-schema-cache fix, pinned by the fork | **RECONCILE**, verify not lost | Upstream independently fixed the *identical* stale-`valueSchema`-cache bug in `KeyToValue` (issue #718, "value schema was cached and not updated") and apparently restructured/renamed the test — confirm upstream's replacement test still covers the scenario before deleting the fork's version outright |
| `describeTableV2` using `this.database` instead of the passed-in parameter (P3-WR-02 follow-up) | N/A — untested dormant bug | **Investigate** | Upstream's changelog (issue #580, "`DESCRIBE TABLE` was built from the configured database rather than the database requested by the caller") describes what sounds like the *same class* of bug, fixed for `enableDbTopicSplit` scenarios. Check whether upstream's fix subsumes P3-WR-02 before re-deriving it |
| `MetadataFreeJsonInsertUnitTest`/R-01, cross-checked against `enableDbTopicSplit`/exactly-once batching change | N/A | **New risk to add to the triage, not previously tracked** | Upstream's `1.6.0` changelog explicitly warns: batching now happens per-database when `enableDbTopicSplit=true`, and *"a batch left in the `BEFORE` state by an earlier version is not deduplicated on the first insert after upgrading"* — if any fork production connector uses `enableDbTopicSplit=true` with `exactlyOnce=true`, this is a real upgrade-time dedup gap upstream itself documents. Confirm whether any production config uses this combination before treating it as N/A |

---

## 3. Test-Infrastructure Breakage

### Pitfall 11: `ClickHouseBase.java` — upstream reverted the exact performance/isolation work the previous milestone shipped, and dropped the MBean/tag/uniqueName contract entirely

**What goes wrong:** Direct read of `v1.6.0`'s `ClickHouseBase.java` shows a full rewrite:
- **`@TestInstance(TestInstance.Lifecycle.PER_CLASS)`** with `@BeforeAll setup()` starting a **fresh container per class** (`setupContainer` called from `setup()`, container stopped in `@AfterAll tearDown()`) — this is precisely the "one container per class" shape the fork's Phase 1 harness work moved *away from* (Key Decision: "One shared ClickHouse container per test JVM fork... 0 flakes observed at 6 forks").
- Adds `ClickHouseCluster`/`isCluster`/`isCloud` support (multi-node cluster tests, `withCreateContainerCmdModifier` for a 2GB memory cap, a `clickhouse_keeper_map.xml` config copy for embedded-keeper exactly-once tests), log-file extraction on teardown, and a `com.google.crypto.tink.internal.Random`-based database-name suffix.
- **No `@Tag("clickhouse")`, no `@AfterEach` MBean leak sweep, no `uniqueConnectorName(TestInfo)` helper** are present in this version of the file.

Since `ClickHouseBase.java` is one of the 39 conflicted files, resolving it is unavoidable — but the size and unfamiliarity of upstream's rewrite (cluster/cloud support the fork doesn't use and explicitly excludes from scope) makes it easy to take upstream's version "because it's clearly the more complete implementation" and lose three load-bearing fork contracts silently within that same file-level decision.

**Symptom if the fork's contract is lost:**
- No `@Tag("clickhouse")` → `./gradlew testUnit` (`excludeTags("clickhouse")`, documented as the Docker-free unit loop, deliberately excluded from `check`) starts trying to run **every** ClickHouse-backed test. In an environment without Docker running (or with the `TESTCONTAINERS_HOST_OVERRIDE=null` shell hazard active — see Pitfall 16), this doesn't fail cleanly; it hangs or fails with confusing Testcontainers/Ryuk errors that look like environment problems, not a missing tag.
- No `@AfterEach` MBean sweep → reintroduces exactly the suite-order-dependent `InstanceAlreadyExistsException` flakiness the previous milestone's own PITFALLS.md (Pitfall 5 there) already diagnosed and fixed — except now there's *more* MBean surface to leak, since upstream's `SinkTaskStatistics` also registers per-topic MBeans (`topicMBeans` deque) that need their own unregistration on every code path, not just the happy one.
- No `uniqueConnectorName(TestInfo)` → any fork test that opted into it for MBean-collision avoidance silently reverts to relying on config defaults, reintroducing cross-test name collisions.
- Reverting to per-class containers (from per-fork-shared) is a pure performance regression — CI wall-clock for the combined 371 fork + upstream's 56 test files grows roughly linearly with test-class count instead of staying flat.

**Why it happens:** Upstream's harness evolved independently to support Cloud/Cluster test matrices the fork has explicitly excluded from scope (constraint: "ClickHouse Cloud test matrix — fork has no cloud credentials and does not use Cloud"); the container-sharing optimization and MBean-safety net were fork-only hardening with no upstream counterpart to preserve them.

**Prevention:** Resolve `ClickHouseBase.java` as a deliberate re-composition, not a pick-one-side decision: keep upstream's Cloud/Cluster scaffolding present but unused (matches the constraint — don't delete code just because it's unused, but don't wire CI to exercise it either), re-add `@Tag("clickhouse")`, the `@AfterEach` MBean sweep (extended to also sweep any registered `topicMBeans`), `uniqueConnectorName(TestInfo)`, and evaluate whether to keep the per-fork-shared-container optimization on top of upstream's per-class default (the two are compatible in principle — sharing can be layered over `@BeforeAll`/`@AfterAll` semantics — but must be explicitly re-implemented, not assumed to survive).

**Detection signal:** `./gradlew testUnit` hanging or failing to complete without Docker is the loudest, fastest signal that `@Tag("clickhouse")` was lost — run it as the very first smoke test after resolving this file, before running the full `./gradlew test` suite. A full-suite (not per-class) run producing `InstanceAlreadyExistsException` failures that don't reproduce when tests are run individually is the MBean-sweep-loss signal — exactly the flakiness pattern the previous milestone already catalogued and fixed once.

**Owning phase:** Dedicated test-harness reconciliation phase, run *before* any other test-file conflict resolution — `ClickHouseSinkTaskTest.java` and its five siblings (also conflicted) all extend `ClickHouseBase`, so getting this file right first avoids re-doing downstream work.

---

### Pitfall 12: `SchemaTestData`/`SchemalessTestData` moved to `src/testFixtures` — rename-detected and mergeable, but `ClickHouseTestHelpers.java` (a `modify/delete`) was not

**What goes wrong:** Upstream restructured shared test data/helpers into a new `src/testFixtures` source set (`jvm-test-suite` plugin, confirmed present in `v1.6.0`'s `build.gradle.kts`, plus new `testFixturesApi`/`testFixturesImplementation` dependency blocks). Git's rename detection correctly matched the fork's modified `src/test/.../helper/SchemaTestData.java`/`SchemalessTestData.java` against upstream's near-identical-content files at the new `src/testFixtures/.../helper/` path — these show up as ordinary content conflicts at the *new* location, which is the best-case outcome (loud, resolvable, no content silently orphaned).

`ClickHouseTestHelpers.java`, by contrast, is a **modify/delete** conflict: upstream deleted it (its responsibilities were apparently split across the new `ClickHouseCluster.java`, `CreateTableStatement.java`, and a much-expanded `testFixtures` version of the same filename — `src/testFixtures/.../helper/ClickHouseTestHelpers.java`, `+458` lines, confirmed present in the upstream tree), but git's rename heuristic did **not** match it against any single upstream file closely enough to auto-resolve. Git's fallback for a modify/delete conflict is to **keep the fork's modified version, orphaned, at its old path** — meaning post-merge, the repository could contain *both* the fork's old `src/test/.../helper/ClickHouseTestHelpers.java` (with the fork's `CLICKHOUSE_VERSION` default, UTF-8 helpers, and the `windows-1252`-safe test utilities) *and* upstream's new, much larger `src/testFixtures/.../helper/ClickHouseTestHelpers.java`, with nothing wiring them together and every fork test still importing the old path.

**Why it happens:** Git's rename detection is similarity-based; `ClickHouseTestHelpers.java` changed enough between the two sides (upstream: `+458` insertions, essentially a rewrite; fork: `+20/−0` targeted additions) that the heuristic didn't confidently pair them, unlike `SchemaTestData`/`SchemalessTestData` where the overlap was apparently closer.

**Prevention:** Do not trust git's modify/delete resolution here. After merge-conflict resolution, explicitly `git log --follow`/read-diff both `src/test/.../helper/ClickHouseTestHelpers.java` (fork's, kept) and `src/testFixtures/.../helper/ClickHouseTestHelpers.java` (upstream's, new) side by side, decide which one is now canonical (almost certainly upstream's `testFixtures` version, since that's the direction the whole build restructure is heading, per PROJECT.md's "build.gradle.kts taken from upstream wholesale"), and manually port forward every fork-specific helper method (`CLICKHOUSE_VERSION` defaulting to `latest`, the `countRowsWithEmojis`-style non-ASCII helpers, `uniqueConnectorName`) into the new canonical file. Delete the orphaned old-path file once done, and fix every fork test's import statement.

**Detection signal:** A mass **compile error** across every fork test file that imports `com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers` if the class ends up duplicated in two packages with ambiguous resolution, or (worse, quieter) tests compiling fine against the *old, orphaned* copy while the rest of the suite uses the new `testFixtures` one — producing two divergent notions of e.g. "what's the default `CLICKHOUSE_VERSION`" living in the same build. Grep for exactly two `ClickHouseTestHelpers.java` files post-merge as an explicit checklist item — its presence is itself the bug signal, independent of any test result.

**Owning phase:** Same test-harness reconciliation phase as Pitfall 11 — do this immediately after `ClickHouseBase.java`, since `ClickHouseBase` is the file that calls into `ClickHouseTestHelpers` the most.

---

### Pitfall 13: `src/integrationTest`'s three modify/delete conflicts mirror the same testFixtures-migration hazard, at lower priority

**What goes wrong:** `ClickHouseAPI.java`, `SchemaTestData.java`, `SchemalessTestData.java` under `src/integrationTest/.../helper/` are all modify/delete conflicts, same shape as Pitfall 12, but in the source set explicitly marked "only `workflow_dispatch` in CI" and out of this milestone's direct scope (`integrationTest` is Confluent-platform/Cloud-only per Context).

**Why it happens:** Same testFixtures restructuring, applied to the integration-test source set too.

**Prevention:** Lower priority than Pitfall 12 (this source set doesn't run in the milestone's CI job), but still worth a pass: confirm `./gradlew integrationTest` at least **compiles** post-merge (it doesn't need to pass, since it's out of scope and largely cloud-dependent), so it doesn't silently rot into a permanently broken source set that blocks a future `check`-all invocation.

**Detection signal:** `./gradlew compileIntegrationTestJava` (or equivalent) failing.

**Owning phase:** Test-harness reconciliation phase, deprioritized to "verify compiles" rather than "verify passes."

---

## 4. Config/Behaviour Regressions Only Visible in Production

These are distinct from Section 1/2's items in that they are **not** about losing a fork feature during merge — they are upstream's own new defaults and semantics landing on top of whatever the fork's production connectors are already configured with.

| Change | What it does | Risk to existing fork connectors | Source |
|---|---|---|---|
| **`clickhouseClientInsertTimeoutMs`** new setting, default `240000` (4 min) | Inserts now time out instead of blocking forever on a network partition/server hang/long GC pause; on timeout, throws a **retriable** exception instead of blocking past `consumer.override.max.poll.interval.ms` | Net positive (previously-hanging inserts now retry), but any currently-slow legitimate insert that takes >4 minutes (e.g., a very large batch under load) would newly retry/fail where it previously just blocked — worth a one-time check against production batch sizes/latency | `CHANGELOG.md` 1.3.10, PR #756 |
| **`retryOnSocketException`**, default `false` | New opt-in: broken-pipe/connection-reset `SocketException` from client V2's persistent connections is retried like existing timeout cases, only if enabled | No risk at default (`false`) — but worth deliberately turning on given the fork already runs client V2 in production, since the whole point is a V2-specific failure mode (persistent connections) the fork is exposed to today with no mitigation | `CHANGELOG.md` 1.6.0, PR #817 |
| **`bufferCount`/`bufferFlushTime`/exactly-once "strict-chunking" validator** | If `bufferCount > 0` **and** `exactlyOnce=true`, connector now **requires** `bufferFlushTime=0` and `ignorePartitionsWhenBatching=false`, else throws `ConnectException` at startup | Dormant at defaults (`bufferCount=0`) — but if any production connector config sets buffering **and** exactly-once together without matching this exact combination, the connector will now **refuse to start** post-merge where it previously started (buffering + exactly-once wasn't safely supported before at all) | `CHANGELOG.md` 1.6.0 "New Features"; `ClickHouseSinkTask.java` validator, confirmed by direct read |
| **`enableDbTopicSplit=true` batching-per-database fix** | Fixes a real bug (records for `dbA.t`/`dbB.t` sharing a batch key), but upstream explicitly warns: *"a batch left in the `BEFORE` state by an earlier version is not deduplicated on the first insert after upgrading"* | Only relevant if any production connector uses `enableDbTopicSplit=true` **and** `exactlyOnce=true` — if so, the very first insert after the upgrade may double-insert a batch that was mid-flight at merge/deploy time | `CHANGELOG.md` 1.6.0, issue #580 |
| **`async_insert=0` still forced by default** | See Pitfall 9 | Real regression if the fork's own un-forcing patch is lost during merge — not an upstream *change*, an upstream *non-change* colliding with a fork decision | Confirmed by direct read |
| **JMX `connector=` naming lost** | See Pitfall 3 | Real regression for any dashboard/alert keyed on the `connector=` ObjectName segment, if not manually re-added | Confirmed by direct read |
| **`Version.ARTIFACT_VERSION`** replaces `ClickHouseClientOption.class.getPackage().getImplementationVersion()` for the JMX `version=` segment | Different mechanism for the same rendered field | Low risk on its own (both should render the connector's own version string) but verify the new `Version` class's actual value matches what dashboards currently expect, since it's now sourced from a different place (likely a generated `BuildConfig`-style constant vs. jar manifest metadata) | Confirmed by direct read of `SinkTaskStatistics.getMBeanName` |
| **New `SinkTaskStatisticsMBean`/`TopicStatisticsMBean` attribute set** | Existing attribute names (`getReceivedRecords`, `getRecordProcessingTime`, `getTaskProcessingTime`, `getInsertedRecords`, etc.) are preserved verbatim in the new interface (confirmed by direct read) — this is **not** a breaking rename | Low risk for attribute-level dashboards; only the ObjectName's `connector=` segment (ancestor path, not attribute name) is the actual breaking change | Confirmed by direct read |

**Owning phase:** A "production-config compatibility review" step belongs in the upstream-inventory phase (before the merge, so the team knows what to check) and again as a milestone-close checklist item (after, to confirm nothing above silently shipped wrong) — not a single point-in-time check, since some of these (JMX naming, async_insert) are merge-execution risks while others (bufferCount+exactlyOnce validator, insert timeout) are upstream's own new opt-in/dormant behavior that only needs a one-time production-config audit, not code changes.

---

## 5. ClickHouse Server / Client Compatibility

**No evidence of a hard minimum-ClickHouse-version bump.** `manifest.json` shows no diff between `main` (`v1.2.9`) and `v1.6.0` — the declared `ClickHouse v22.5+` requirement is unchanged. This milestone's constraint (test against `latest` only) means the fork was never exercising the low end of that range anyway.

**JSON/Dynamic behavior against `latest`:** upstream's `1.6.0`-era `Type.JSON` RowBinary handling (Pitfall 6) is new capability, not a compatibility requirement — it's gated behind a connector-level setting (`binaryFormatWrtiteJsonAsString`) plus a ClickHouse-side setting (`input_format_binary_read_json_as_string=1`), both opt-in. `Type.DYNAMIC` remains entirely unaddressed upstream (same gap the fork already knew about and worked around with its own fail-fast fix — Pitfall 5). No new ClickHouse server-side setting is *required* by anything in the 581-commit range that this research surfaced; the closest is the exactly-once "strict-chunking" feature's `bufferFlushTime=0` requirement, which is a connector-config constraint, not a server one.

**Client dependency:** `build.gradle.kts`'s move to a Gradle version catalog (`gradle/libs.versions.toml`, confirmed present at `v1.6.0`) means the ClickHouse Java client version is now catalog-pinned rather than declared inline — when re-applying the fork's build customizations on top of upstream's `build.gradle.kts` (per the already-locked decision), explicitly diff the catalog's `clickHouseDriver`/`clickhouse-client`-family entries against whatever version the fork currently runs in production, since a silent client-library bump is exactly the kind of thing a wholesale `build.gradle.kts` adoption could introduce without anyone noticing it wasn't a deliberate upgrade decision.

**Owning phase:** Upstream-inventory phase (record the catalog's resolved client version explicitly as a named fact, not just "we took upstream's build file").

---

## 6. The `.planning/` Tracked-Docs Hazard

**Concrete failure mode:** `.planning/` does not exist upstream at all, so `git merge-tree` correctly shows **zero conflicts** anywhere under `.planning/` — every one of the 80+ tracked planning files is "added only in `custom_build`" from the merge's perspective and is kept automatically. This is not where the risk lives.

The actual risk is **process, not merge-mechanics**: this is a multi-day, 39-file, feature-by-feature manual reconstruction (per Sections 1–3 above), executed inside a GSD-managed repo where `commit_docs: true` means every phase-plan step normally produces its own commit touching `.planning/state.json` and other tracked docs. Two concrete problems follow:

1. **A `git merge` in progress leaves `MERGE_HEAD` set and a dirty, partially-resolved working tree for as long as conflict resolution takes.** If GSD's normal per-task auto-commit cadence (used by `/gsd-execute-phase` for ordinary phases) is applied naively to this merge — i.e., treating "resolve `ClickHouseWriter.java`" as one task-commit, "resolve `ClickHouseHelperClient.java`" as the next — each of those intermediate commits is **not** a real git merge commit; the actual `git merge --continue`/`git commit` that finalizes the merge only happens once, at the end, after every conflicted file (all 39) is resolved. Mixing GSD's usual "small, frequent, individually-revertable commits" pattern with git's single-merge-commit model means either: (a) the whole 39-file resolution has to happen as one giant working-tree edit before any commit exists at all (long stretch with no checkpoint, no partial rollback), or (b) the plan needs an explicit convention for how "one commit per resolved file, all still under the same in-progress merge" interacts with GSD's own commit-per-task assumption. This needs to be decided by the phase plan, not discovered mid-merge.
2. **80+ tracked planning files bloat the final merge-commit diff**, making it harder for a later reviewer (or a future `git log`/`git blame` pass) to separate "this changed because of the upstream sync" from "this changed because GSD tooling updated `state.json` mid-session." Cheapest mitigation: keep the merge-resolution commit(s) free of incidental `.planning/` churn — don't let routine GSD state updates interleave with in-progress merge-conflict-resolution commits; batch planning-doc updates to before the merge starts or after it's fully resolved and green, not during.

**Prevention:** Do the merge on a disposable scratch branch (e.g., `sync/v1.6.0`) rather than directly on `custom_build` (this also solves the force-push risk in Section 7); treat "the merge is fully resolved and compiling" as one atomic checkpoint before any `.planning/` document updates resume; only fast-forward or merge the scratch branch into `custom_build` once done.

**Detection signal:** A `git log` on the eventual merge showing planning-doc commits interleaved with unresolved-conflict-marker commits (i.e., commits made while `<<<<<<< HEAD` markers still existed anywhere in tracked source files) — grep the merge range for literal conflict markers in any *committed* revision as a sanity check, not just the final state.

**Owning phase:** Process decision belongs in whichever phase plans the merge-execution mechanics (likely the "big-bang merge" phase itself) — needs to be decided before conflict resolution starts, not improvised.

---

## 7. Process Pitfalls

### Pitfall 14: No bisect path through a single 581-commit merge commit

**What goes wrong:** A single `git merge` commit is one atomic unit from `git bisect`'s perspective — if a regression is found weeks later and traced to "somewhere in the v1.6.0 sync," there's no way to bisect *within* the merge to find which of the 581 upstream commits caused it, only whether the whole merge is at fault.

**Prevention:** The already-planned "scratch refs, map of every file both sides touched" mitigates discovery-time investigation (this research itself relied on exactly that kind of direct comparison), but doesn't solve post-merge bisection. Two cheap additions: (1) tag the pre-merge commit explicitly (e.g., `pre-v1.6.0-sync`) as a clean, fast, unambiguous rollback point; (2) keep the eventual merge as a single, clearly-labeled commit (not squashed further, not split across unrelated work) so `git revert -m 1 <merge-sha>` remains a viable full-rollback path if needed shortly after landing, before other work builds on top of it.

**Detection signal:** N/A (this is a preventive structural choice, not something that fails loudly).

**Owning phase:** Upstream-inventory/rollback-point phase.

### Pitfall 15: Force-push risk on an already-published branch

**What goes wrong:** `custom_build` is already pushed to the public fork. If the merge resolution goes wrong mid-way and the instinct is to `git reset --hard`/redo, doing that **on `custom_build` itself** after any intermediate push would require a force-push to the public branch to fix — explicitly flagged as something to warn about, not do casually, per this agent's own operating constraints.

**Prevention:** Do the entire merge on a scratch branch (same recommendation as Pitfall 13's mitigation) that is never itself required to be pushed publicly mid-work; only bring `custom_build` forward via a normal (non-force) merge/fast-forward once the scratch branch is fully green. This makes "redo the whole thing" always safe — reset the scratch branch, no public history is touched.

**Detection signal:** N/A — preventive.

**Owning phase:** Merge-execution phase (mechanics decision, made up front).

### Pitfall 16: A long stretch of non-compiling working tree during resolution

**What goes wrong:** With `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `ClickHouseSinkConfig.java`, `ProxySinkTask.java`, `ClickHouseSinkTask.java`, `Processing.java`, `Column.java`, `Table.java`, `Type.java`, `KeyToValue.java`, and `ClickHouseBase.java` (plus its six dependent test files) all needing coordinated resolution, the working tree is very likely to be non-compiling for a meaningful stretch — any one of the interdependent files being resolved before its counterpart (e.g., `Processing.java`'s constructor arity change, Pitfall 2) breaks the build until the whole group lands together.

**Prevention:** Resolve files in dependency order, not alphabetical/arbitrary order: config → writer/helper-client/mapping (Column/Table/Type) → processing → proxy-sink-task/sink-task → test harness (`ClickHouseBase` and its dependents) → remaining tests. Compile after each logical group, not after each individual file, to avoid false "still broken" signals from expected in-progress states.

**Detection signal:** `./gradlew compileJava compileTestJava` (skipping the slower `test` task) run frequently as a fast checkpoint during resolution — this is unaffected by the `TESTCONTAINERS_HOST_OVERRIDE="null"` shell hazard carried forward from the previous milestone (still a live environment fact: must run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, already codified in `config.json`'s `build_command`/`test_command`).

**Owning phase:** Merge-resolution phase (sequencing decision).

### Pitfall 17: `VERSION` / release-metadata conflict is trivial but must not be forgotten

**What goes wrong:** `VERSION` is a guaranteed one-line conflict (`main`: `v1.2.9`, `custom_build`: `v1.2.9_gg4`, `v1.6.0`: `v1.6.0`) — already anticipated in PROJECT.md's target features (`VERSION` → `v1.6.0_gg1`). Low risk on its own, but worth naming explicitly since it's easy to resolve on "autopilot" (take whichever side looks newer) and land the wrong string.

**Prevention:** Resolve explicitly to `v1.6.0_gg1` as already planned; verify it round-trips through `ProxySinkTask`'s JMX `version=` segment correctly post-merge (tying back to Pitfall 3's naming work) if the JMX version source is ever re-pointed at this file rather than `Version.ARTIFACT_VERSION`/the jar manifest.

**Detection signal:** Trivial to verify by inspection; include in the same checklist as the JMX naming re-verification.

**Owning phase:** CI/publication phase (already the phase named in PROJECT.md's target features for this).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|---|---|---|---|
| "Prefer upstream wherever it passes the fork's tests" applied literally, file-by-file, without checking whether upstream's version *can* pass those tests structurally (e.g., JMX naming, Section 1 Pitfall 3) | Faster resolution, less manual re-implementation | Silently drops production-load-bearing fork features whose absence doesn't cause a compile error, only a specific assertion failure a reviewer might not run before merging | Only ever acceptable *after* running the specific fork tests for that feature and confirming they pass — never as a default when a test hasn't been run yet |
| Resolving `ClickHouseBase.java`'s conflict by "take the more complete-looking upstream version" | Gets Cloud/Cluster support, which is objectively more feature-rich | Silently drops `@Tag("clickhouse")`, MBean sweep, `uniqueConnectorName` — all load-bearing for the fork's own 371-test suite's stability | Never — always re-compose explicitly (Pitfall 11) |
| Treating a "clean, unconflicted merge" (`Utils.java`, `Processing.java`) as "nothing to review" | Saves review time on two files | Misses both a free bug-fix closure (Utils.java) and a load-bearing constructor-arity change that surfaces only via a *different*, conflicted file (Processing.java + ProcessingTest.java) | Never — a merge summary should explicitly list clean-merged files that both sides touched and spot-check each one, not just review the conflict list |
| Patching a fork fix onto code upstream already fixed independently (Map-nullable RowBinary marker, Pitfall 10) | Feels like "preserving fork work" | Risk of double-applying or subtly diverging from a more defensive upstream version | Never — always check whether upstream already solved it before re-deriving a patch during conflict resolution |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|---|---|---|
| Client V2 JSON insert (`client.insert(...)`) | Assuming the fork's `List<String> jsonInsertColumns, InputStream data` overload still exists at upstream's `doInsertJsonV2` call site | Upstream now uses a `DataStreamWriter` lambda overload (`client.insert(table, dataWriter, format, settings)`) — `jsonInsertColumns` must be redesigned as a field-filter inside `writeRecordsJson`, not passed to the client call (Pitfall 4) |
| ClickHouse `match()`/`targetTableFilter` (carried forward from previous milestone's PITFALLS.md, still live) | Testing the regex as Java `Pattern` instead of live `match()` — still applies, unchanged, since the feature is being re-threaded onto a rewritten `showTables` | Test against the real container's `match()` after re-implementation, same as before |
| `windows-1252` test JVM encoding (carried forward, still live) | New/reconciled test helpers under `src/testFixtures` not specifying `StandardCharsets.UTF_8` explicitly | Explicit UTF-8 on every stream reader, verified specifically for whatever file becomes canonical after the `ClickHouseTestHelpers.java` modify/delete reconciliation (Pitfall 12) |
| Testcontainers + colima (carried forward, still live) | Assuming the shell environment hasn't regressed | `TESTCONTAINERS_HOST_OVERRIDE="null"` is still a known live hazard in this shell — always run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, already the configured `build_command`/`test_command` |
| GenericRecord snapshot pattern (carried forward, still live) | Collecting client-v2 `GenericRecord` references across a `Records` iteration | Still a mutable, reused row view post-merge (nothing in the diff touches this) — keep snapshotting into a `Map` during iteration |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|---|---|---|---|
| Reverting to a container-per-test-class (Pitfall 11) | CI wall-clock grows roughly linearly with the combined 371 fork + 56 upstream test-class count, instead of staying flat under the fork's per-JVM-fork container sharing | Re-apply (or re-derive) the container-sharing optimization on top of upstream's `@BeforeAll`/`@TestInstance(PER_CLASS)` shape | Immediately visible once the full merged suite (400+ tests) runs — likely to roughly double or worse CI time if unaddressed |
| JaCoCo instrumentation stacked on a now-larger Testcontainers-heavy suite (carried forward, amplified) | Slower runs, especially once upstream's own 56 test files (many container-backed) are added to the same instrumented `test` task | Keep JaCoCo scoped to `test` only, not `integrationTest`; re-verify after the merge that it's still report-only with no build-failing threshold | Most visible in CI resource-constrained runners once the merged suite size roughly triples |

## "Looks Done But Isn't" Checklist

- [ ] **JMX `connector=` naming:** Often assumed preserved because "the MBean tests pass" — verify specifically against `MBeanNameUnitTest`'s literal `connector=` substring assertion, not just "an MBean got registered"
- [ ] **`async_insert` un-forcing:** Often assumed preserved because the config compiles — verify `AsyncInsertSettingsUnitTest`'s default-config assertion specifically, since the setting's *absence* is what's being tested, easy to miss in a config map dump
- [ ] **R-01's disposition:** Often left as "whatever the merge produces" — verify there is an explicit, recorded keep/drop decision, not a silently-changed test result nobody looked at
- [ ] **`ClickHouseTestHelpers.java` duplication:** Often missed because both copies compile fine independently — explicitly grep for exactly one canonical file post-merge, not two
- [ ] **`@Tag("clickhouse")` on `ClickHouseBase`:** Often assumed present because `ClickHouseBase` compiles — verify `./gradlew testUnit` actually completes without Docker, as a first-class smoke test
- [ ] **`targetTableFilter`/`shouldSkipDescribeRow`:** Often assumed carried over because `ClickHouseHelperClient.java`'s conflict "looks resolved" (i.e., compiles) — verify `FILT-01..04`/`DESC-01..04` specifically, since these features have zero upstream anchor to fall back on if missed
- [ ] **UTF-8 describe readers:** Often assumed fixed upstream because the file was rewritten — verify by grep for every `InputStreamReader(` construction in the resolved `ClickHouseHelperClient.java` and confirm each specifies `StandardCharsets.UTF_8`
- [ ] **Production config audit (Section 4 table):** Often deferred as "not code, so not this milestone's problem" — verify at least a documented pass over whether any production connector combines `enableDbTopicSplit=true` + `exactlyOnce=true`, or `bufferCount>0` + `exactlyOnce=true`, before declaring the merge production-ready

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---|---|---|
| JMX `connector=` naming lost and shipped (Pitfall 3) | MEDIUM | Re-add the segment to `SinkTaskStatistics.getMBeanName`, ship a patch release, note in release notes that dashboards need no change (name format restored) — but any metrics gap during the window it was missing is unrecoverable |
| `async_insert` forcing reintroduced (Pitfall 9) | LOW | Re-comment the line, release a patch; no data loss, only a latency/throughput regression while live |
| R-01 disposition made by accident rather than decision (Pitfall 4) | LOW (if caught before release) / HIGH (if shipped and later reversed, since reversing now breaks pipelines relying on the *new* behavior) | Catch via the labelled test's result immediately post-merge, before any release — this is exactly why the test is labelled and pinned |
| `ClickHouseTestHelpers.java` duplicated (Pitfall 12) | LOW | Delete the orphaned copy, fix imports, recompile — mechanical once noticed |
| Non-compiling working tree mid-resolution (Pitfall 16) | LOW | Resolve remaining files in dependency order; this is expected mid-process state, not a regression, as long as it resolves before the final merge commit |
| Metadata-free JSON path re-application incomplete (Pitfall 4) | MEDIUM–HIGH | Re-derive against upstream's current `getTable`/`writeRecordsJson`/`cleanupExtraFields` shapes; requires understanding upstream's rewrite, not just re-applying an old diff |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---|---|---|
| Silent clean merge, `Utils.java`/`Processing.java` (P1, P2) | Upstream-inventory phase (record); merge-resolution phase (`ProcessingTest.java`/`ProxySinkTask.java` linked group) | `UtilsTest.java` reconciled; no stale 4-arg `Processing` constructor call anywhere |
| JMX `connector=` naming loss (P3) | Dedicated JMX-reintegration phase | `MBeanNameUnitTest`/`MBeanRegistrationLiveTest` green with `connector=` present |
| Metadata-free JSON path / R-01 disposition (P4) | Dedicated redesign phase, gated on an explicit keep/drop decision | `MetadataFreeJsonInsertUnitTest`, `ExplicitJsonInsertPathLiveTest`, R-01's labelled test, `JsonInsertColumnsUnitTest`/`LiveTest` all reflect the decision made, not an accident |
| RowBinary fail-fast for `Type.DYNAMIC` lost (P5) | `ClickHouseWriter.java` conflict-resolution phase | `RowBinaryFailFastLiveTest`/`RowBinaryUnsupportedTypeGateTest` on `Type.DYNAMIC` throw `DataException` |
| `Type.JSON` RowBinary reconciliation (P6) | Same phase as P5 | `JsonDynamicRoundTripTest`'s JSON assertions updated to match the adopted behavior, and pass |
| `targetTableFilter`/subcolumn-skip re-application (P7) | `ClickHouseHelperClient.java` conflict-resolution phase | `FILT-01..04`, `DESC-01..04` green |
| UTF-8 describe-reader re-application (P8) | Same phase as P7 | `TYPE-07` non-ASCII round-trip green under `./gradlew test`'s `windows-1252` setting |
| `async_insert` un-forcing lost (P9) | `ClickHouseSinkConfig.java` conflict-resolution phase | `AsyncInsertSettingsUnitTest` default-config assertion green |
| Map-nullable double-fix risk (P10) | `ClickHouseWriter.java` conflict-resolution phase | Take upstream's version verbatim; confirm WR-01 closure |
| `ClickHouseBase.java` contract loss (P11) | Test-harness reconciliation phase, first | `./gradlew testUnit` completes without Docker; full-suite run has zero `InstanceAlreadyExistsException` |
| `ClickHouseTestHelpers.java` duplication (P12) | Same phase as P11 | Exactly one canonical file post-merge |
| `integrationTest` compile rot (P13) | Same phase as P11, deprioritized | `compileIntegrationTestJava` succeeds |
| Production config regressions (Section 4) | Upstream-inventory phase (pre-check) + milestone-close checklist (post-check) | Documented pass over `enableDbTopicSplit`+`exactlyOnce`, `bufferCount`+`exactlyOnce` combinations in production configs |
| ClickHouse/client compatibility (Section 5) | Upstream-inventory phase | Resolved `libs.versions.toml` client version recorded and compared against production |
| `.planning/` process hazard (Section 6) | Merge-execution mechanics phase | No planning-doc commits interleaved with unresolved-conflict-marker commits in the merge range |
| No bisect path (P14) | Upstream-inventory/rollback-point phase | Pre-merge commit tagged; merge lands as one clean, identifiable commit |
| Force-push risk (P15) | Merge-execution phase | Entire merge done on a scratch branch, never force-pushed to `custom_build` |
| Non-compiling stretch (P16) | Merge-execution phase | Files resolved in dependency order; `compileJava`/`compileTestJava` checkpoints between groups |
| `VERSION` conflict (P17) | CI/publication phase | `VERSION` = `v1.6.0_gg1`, confirmed round-tripping through any JMX version rendering |

## Sources

- Direct repository reads (HIGH confidence, primary grounding for every finding above): `git show custom_build:<path>`, `git show v1.6.0:<path>`, `git diff main..custom_build -- <path>`, `git diff main..v1.6.0 --stat -- <path>` across `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `ProxySinkTask.java`, `ClickHouseSinkTask.java`, `ClickHouseSinkConfig.java`, `Processing.java`, `Utils.java`, `Column.java`, `Type.java`, `SinkTaskStatistics.java`, `SinkTaskStatisticsMBean.java`, `DeliveryStrategy.java`, `ChunkFlusher.java`, `DataJson.java`, `ClickHouseBase.java`, `build.gradle.kts`, `VERSION`, `.gitignore`, `manifest.json`
- `git merge-tree custom_build v1.6.0` (git 2.50.1, two-ref form) — authoritative 39-file conflict list plus 5 modify/delete conflicts, confirmed merge-base `bec167d` = `main` = upstream `v1.2.9`
- `CHANGELOG.md` at `v1.6.0` (HIGH — upstream's own primary source, read in full from `1.6.0` back through `1.2.9`) — cited by version and PR/issue number throughout
- `.planning/PROJECT.md` (Current Milestone collision table, Context, Constraints, Key Decisions, Active follow-ups) — cross-checked against direct source reads, several PROJECT.md predictions confirmed and sharpened (e.g., the collision table's file list matches the merge-tree conflict list for `src/main`)
- `.planning/milestones/v1.2.9_gg4-tests-research/PITFALLS.md` — live pitfalls carried forward explicitly where still applicable (RE2 vs. Java regex, `windows-1252` charset, Testcontainers/colima, `GenericRecord` snapshot pattern, container-per-class performance); pitfalls specific to *building* the original test suite (JaCoCo/Lombok exclusion, CI Actions-enabled check, `CLICKHOUSE_VERSION` default) not carried forward as they describe a resolved past state, not a live merge risk
- `.planning/RETROSPECTIVE.md` — `TESTCONTAINERS_HOST_OVERRIDE` environment hazard and the `.planning/` tracked-docs caveat restated per the milestone context's explicit instruction, not rediscovered

---
*Pitfalls research for: ClickHouse Kafka Connect fork — upstream v1.6.0 merge*
*Researched: 2026-09-18*
