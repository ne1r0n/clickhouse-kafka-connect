# Feature Research: Test Coverage for `custom_build` Fork Additions

**Domain:** Kafka Connect sink connector (ClickHouse), fork test-coverage milestone
**Researched:** 2026-09-15
**Confidence:** HIGH (all findings read directly from fork source `src/main/java` and existing tests in `src/test/java`; upstream conventions verified against live Apache Kafka source)

This file re-purposes the "feature landscape" template for a **test-coverage** milestone. "Table stakes" = test cases that must exist for a fork addition to be called "covered". "Differentiators" = tests that buy real extra confidence without being required for the milestone's Definition of Done. "Anti-features" = tests deliberately *not* written, because they'd be flaky, would re-test a library/upstream behavior, or would validate behavior the codebase doesn't actually have.

Every claim below is grounded in a specific source file and line, or an existing/missing test file, read on 2026-09-15 at fork commit `22f055c` (branch `custom_build`, 34 commits ahead of local `main`).

## Feature Landscape

### 1. `TableOperationFilter` SMT

Source: `src/main/java/.../transforms/TableOperationFilter.java`. Existing tests: `TableOperationFilterTest.java` (9 unit tests, no `t`/truncate case, no invalid-config case).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| `op` in `{c,u,d}` for a listed table drops the record (`apply()` returns `null`) | Table stakes | UNIT | Covered today (`testRecordWithOperationInSkippedOperationsAndTableInTables`). |
| `op=t` (truncate) for a listed table is filtered | Table stakes | UNIT | **Gap** — `"cudt".contains(op)` accepts `t` at config-parse time (line 75) but no test exercises it. Debezium truncate events carry no `source.table`/`before`/`after` payload shape guarantees the same as DML ops; must assert filter still keys off `op`+`source.db`/`source.table` only. |
| Multiple tables in `tables` config, only listed ones filtered | Table stakes | UNIT | **Gap** — only single-table config tested; must assert unlisted table in a multi-table CSV list passes through. |
| `op=r` (snapshot read) is never filterable | Table stakes | UNIT | `SKIPPED_OPERATIONS_DEFAULT="d"`; parser only accepts single chars in `"cudt"` (line 75) — `r` can never enter `skippedOperations`, so even `skipped.operations=r` silently logs a warning and filters nothing. Assert config with `r` produces a no-op filter (record with `op=r` always passes) — this is existing (intentional-by-omission) behavior, not a bug to fix. |
| Invalid entries in `skipped.operations` (e.g. `x`, `cc`, empty string) are dropped with a warning, not thrown | Table stakes | UNIT | **Gap** (PROJECT.md flags this). `TableOperationFilter` has no `ConfigDef` validator for this field — it's free-text `STRING`, split/validated manually in `configure()`. Assert malformed tokens are silently ignored (do **not** assert on log content — see anti-features). |
| Whitespace-only or empty `tables`/`skipped.operations` config | Table stakes | UNIT | **Gap.** Empty string ⇒ `TABLES_DEFAULT`/`SKIPPED_OPERATIONS_DEFAULT` path is already partly covered by defaults, but " , ,  " (whitespace-only tokens between commas) is not; `.trim()` per-token means a token that trims to `""` becomes an empty string added to `targetTables`/checked against `"cudt".contains(op)` (empty won't match, so it's implicitly safe) — worth one assertion that it doesn't crash or produce a spurious empty-string table match. |
| `record.value() == null` (tombstone) passes through unchanged | Table stakes | UNIT | Covered (`testNullValueRecord`). |
| Non-`Struct` value (e.g. schemaless `Map`, since Debezium+`JsonConverter` schemaless mode is a valid pipeline) passes through unchanged | Table stakes | UNIT | Covered (`testNonStructValueRecord`) — but confirm it covers a `Map` value specifically (Debezium's raw JSON envelope), not just an arbitrary non-Struct object. |
| Missing `op` field on the Struct passes through | Table stakes | UNIT | Covered (`testRecordWithoutOpField`). |
| Missing `source` field, or `source` missing `db`/`table`, passes through | Table stakes | UNIT | Covered (`testRecordWithoutSourceField`, `testRecordWithoutDbOrTableInSource`). |
| `close()` is idempotent / callable multiple times without throwing | Differentiator | UNIT | No internal state to null out (`targetTables`/`skippedOperations` are `final`-shaped but not nulled in `close()`), so this is a cheap regression guard, not urgent. |
| `config()` returns the same `ConfigDef` instance/definition used by `configure()` | Differentiator | UNIT | Matches Kafka Connect's `Transformation` contract; trivial to assert (`assertSame` or field equality). |
| Sink-level: a Debezium-shaped record with `op=d` for a filtered table never reaches ClickHouse | Differentiator | ClickHouse-backed | PROJECT.md explicitly asks for "at least one sink-level test per fork SMT" — chain `TableOperationFilter` in front of a `ClickHouseSinkTask.put()` call and assert `countRows == 0` for filtered records, `> 0` for non-filtered. |
| Log message content/format when an invalid operation code is supplied | Anti-feature | — | Asserting on `LOGGER.warn` text ties the test to log wording, not behavior; don't do it. Assert the *behavior* (record not filtered) instead. |
| Malformed Debezium envelopes with `op` present but wrong Java type (e.g. `op` as `INT32` instead of `STRING`) | Anti-feature | — | `value.getString("op")` would throw a `DataException`/`ClassCastException` from Kafka Connect's own `Struct` API, not fork logic — that's testing the Kafka Connect API, not this SMT. |

### 2. `HeadersToValue` SMT

Source: `src/main/java/.../transforms/HeadersToValue.java`. Existing tests: `HeadersToValueTest.java` (2 unit tests: schemaless + with-schema happy path).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| `record.value() == null` (tombstone) passes through unchanged | Table stakes | UNIT | **Gap** — `apply()` line 63 checks this, no test exercises it. |
| No headers present (`record.headers()` empty `ConnectHeaders`) → field is an empty map, not `null`/missing | Table stakes | UNIT | **Gap.** `extractHeaders()` (line 144) always returns a `Map`, even if empty; assert the produced field is `{}` not absent. |
| Header with a `null` value → map entry is `key -> null` | Table stakes | UNIT | **Gap.** Line 149: `value == null ? null : value.toString()` — explicit null-preservation branch, currently untested. |
| Missing/blank `field` config throws `DataException` at `configure()` | Table stakes | UNIT | **Gap** (PROJECT.md flags this). Line 49-50 explicitly guards `null`/blank; both `null` and `"   "` (whitespace-only, trimmed) should be tested — two distinct branches of `isEmpty()` after trim vs. the raw null check. |
| Field name collision: `field` config names an already-existing value field | Table stakes | UNIT | **Gap** (PROJECT.md flags this) — and it's the most important gap. In `applyWithSchema` (line 109-125), `buildSchema()` only adds the headers-map field `if (oldSchema.field(fieldName) == null)` (line 178) — i.e. if the name collides, the *original* field's schema/type is kept, but the value loop still does `newValue.put(name, extractHeaders(record))` (a `Map`) for that field. If the original field's schema was e.g. `STRING`, `Struct.put()` performs Kafka Connect's own runtime type check and throws `DataException: Invalid value: ... for field ... Expected Schema.Type.STRING, received: java.util.Map`. **Expected behavior to assert:** collision against a non-map-typed field throws `DataException`; collision against a field whose existing schema already is `Schema.OPTIONAL_STRING_SCHEMA`-keyed map (unlikely in practice) succeeds and overwrites. Pin whichever the code actually does — do not "fix" this unless it's clearly wrong (it likely is the intended fail-fast contract, but must be pinned). |
| Schemaless path with existing key in the value map named the same as `field` | Table stakes | UNIT | For schemaless (line 83-101), `value.put(fieldName, extractHeaders(record))` unconditionally overwrites — no type-check exists (raw `Map<String,Object>`), so schemaless collision silently overwrites while schema-based collision throws. This asymmetry is worth pinning explicitly with two tests (schemaless overwrites silently vs. schema-based throws) so future rebases don't "fix" one without the other. |
| `newRecord` preserves topic/partition/key/timestamp; headers on the output record are the *original* headers object (fork doesn't call the `newRecord(..., headers)` overload) | Table stakes | UNIT | **Gap.** Both `applySchemaless`/`applyWithSchema` call the 7-arg `newRecord(topic, partition, keySchema, key, valueSchema, value, timestamp)` (no headers argument) — per Kafka Connect's `ConnectRecord.newRecord` contract, the record's original `headers` are retained by the default overload. Assert output record headers still equal input headers (the SMT copies FROM headers, it doesn't consume/clear them). |
| Repeated header keys (multi-valued headers, e.g. two headers both named `"trace"`) | Differentiator | UNIT | `extractHeaders()` iterates all headers into a single-valued map (line 147-151); Kafka `Headers` can carry duplicate keys. Last-one-wins is the resulting behavior since it's a `Map.put` in iteration order — worth pinning explicitly since Kafka's own `Headers.lastWithName()` (used by `HeaderToValue`, see below) suggests "last wins" is the house convention, but `HeadersToValue` doesn't call `lastWithName` — it just puts in header-iteration order, which for `ConnectHeaders` is *insertion* order, i.e. **last-added-with-same-key wins** in practice too, but for a different reason. Test to lock in observed behavior. |
| Non-string header value (e.g. header value is `Integer`, `Struct`, or `byte[]`) | Differentiator | UNIT | Line 149 calls `.toString()` unconditionally — a `byte[]` header value becomes something like `[B@1a2b3c4` (identity hash), not a useful string. Worth one test documenting this as current (possibly surprising) behavior; do not silently assume it round-trips. |
| `close()` nulls `valueSchema`/`headersSchema` so a re-`configure()` call rebuilds schema | Differentiator | UNIT | Cheap regression guard for the schema-caching optimization (line 112: `if (valueSchema == null)` lazy-build). |
| Sink-level: headers land as a ClickHouse `Map(String, String)` (or `Map(String, Nullable(String))`) column | Differentiator | ClickHouse-backed | One record-count-plus-column-value assertion through `ClickHouseSinkTask`. |
| Testing Kafka Connect's own `ConnectHeaders`/`Header` equality or iteration semantics | Anti-feature | — | That's Kafka Connect API behavior, not fork logic. |

### 3. `HeaderToValue` SMT (+ `HeaderToValueConfig`)

Source: `src/main/java/.../transforms/HeaderToValue.java` + `HeaderToValueConfig.java`. Existing tests: `HeaderToValueTest.java` (2 unit tests, happy path only; note `HeaderToValue.java` itself defines its own inline `ConfigDef` — `HeaderToValueConfig.java` is a **separate, apparently unused** class with an identical but independently-defined `ConfigDef`; confirm at test-writing time whether `HeaderToValueConfig` is dead code or used elsewhere before writing tests against it in isolation).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| `fields.size() != headers.size()` throws `DataException` at `configure()` | Table stakes | UNIT | **Gap** (PROJECT.md flags "config parsing edge cases"). Line 66-69 explicit check; both directions (more fields than headers, more headers than fields) should be separate test cases. |
| Multi-valued headers: `record.headers().lastWithName(...)` semantics | Table stakes | UNIT | **Gap** (explicitly called out in PROJECT.md and in milestone question). The transform uses `lastWithName` (line 114, 154) — Kafka Connect's documented "last wins" convention for duplicate header keys. Build a record with two headers of the same name, assert the *last* value is copied. |
| Header listed in config but missing from the actual record | Table stakes | UNIT | **Gap.** Both schemaless (line 115: `if (header != null && header.value() != null)`) and schema paths (line 155) skip silently when the header is absent — assert the target field is left at its prior value (schema path: whatever `oldValue` had, or absent if the field is new) rather than set to `null`/empty string. |
| Header present but with a `null` value | Table stakes | UNIT | **Gap.** Same `header.value() != null` guard — a header key that exists with a `null` value is treated identically to "header absent" (field untouched), which is a meaningfully different case from `HeadersToValue` (which explicitly preserves `null`). Worth a dedicated test contrasting the two SMTs' null-handling. |
| Schema-based: field named in `fields` doesn't exist in the original schema (new field to add) | Table stakes | UNIT | `buildSchema()` (line 185-217) adds unmatched `fields` as `Schema.OPTIONAL_STRING_SCHEMA`; existing tests likely only exercise overwriting existing fields — add a "new field created" case. |
| `newRecord` preserves topic/partition/key/timestamp/headers (same overload pattern as `HeadersToValue`) | Table stakes | UNIT | **Gap**, same rationale as SMT #2. |
| Non-string header value converted via `.toString()` | Differentiator | UNIT | Same caveat as `HeadersToValue`; document current behavior. |
| `HeaderToValueConfig` class independently exercised (if it turns out to be live code, not dead) | Differentiator | UNIT | Confirm usage first (`grep -rn HeaderToValueConfig src/main`) — if genuinely unused by `HeaderToValue.java`'s own `configure()`, testing it in isolation is low value; note as a likely dead-code cleanup candidate for a future phase, not this milestone's scope. |
| Sink-level: header-derived field lands correctly in a ClickHouse column | Differentiator | ClickHouse-backed | One test. |

### 4. `ConvertToMapOrJson` SMT

Source: `src/main/java/.../transforms/ConvertToMapOrJson.java`. Existing tests: `ConvertToMapOrJsonTest.java` (13 unit tests — strong coverage of base64/JSON-library/date-serialization paths) + `ConvertToMapOrJsonPerformanceTestJson.java` (perf, not correctness).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| `preserve.null.values=false` omits `null` fields from the output map and from JSON (both Jackson and Gson) | Table stakes | UNIT | **Gap** — confirmed absent by grep; `PRESERVE_NULL_VALUES_CONFIG` (default `true`, line 49-50) is entirely untested. Jackson path uses `mapper.setSerializationInclusion(NON_NULL)` (line 145); Gson path relies on *not* calling `builder.serializeNulls()` (line 152-153, Gson's default already drops nulls) — these are two different mechanisms reaching the same contract; test both. |
| Nested `Struct` containing a nested `Struct` inside a `Map`/`Array`-typed field is recursively converted | Table stakes | UNIT | PROJECT.md explicitly calls out "nested arrays/maps" as a gap. `convertFieldValue()` (line 238-254) only recurses into `Struct` — a `List<Struct>` (Kafka Connect `ARRAY` of `STRUCT`) or `Map<String,Struct>` field is passed through **unconverted**, i.e. array/map elements stay as raw `Struct` objects, not maps. **Expected behavior to assert:** confirm current (likely-a-gap) behavior — this could be a genuine bug (nested structs inside arrays/maps not flattening) that the "fix bugs found by tests" rule in PROJECT.md would require addressing. Write the test first to establish ground truth. |
| Non-`Struct`, non-`Map`, non-`String` value with `outputJson=true` (e.g. record value is `null`, a primitive, or the value is a `Map` but `outputJson=false`) | Table stakes | UNIT | Line 166-186: value flows through 4 branches; the `else { return record; }` fallthrough (untransformed passthrough) needs at least one explicit test — e.g. a bare `Long` or already-`String` value with JSON disabled. |
| `newRecord` output passes `record.headers()` through explicitly (8-arg overload, unlike the other 3 SMTs) | Table stakes | UNIT | Line 187-196 uses the `newRecord(..., timestamp, headers)` overload explicitly — confirm this is intentional and test that headers survive; also confirm `keySchema`/`key` still pass through unchanged. |
| Invalid `output.json.library` value rejected by `ConfigDef.ValidString.in("jackson","gson")` at `configure()` | Table stakes | UNIT | **Gap.** `ConfigDef` validation (line 67) should throw `ConfigException` for e.g. `"xml"` before `AbstractConfig` construction even completes — this is a pure `ConfigDef` contract test (see upstream convention below), currently absent. |
| `ByteBuffer` conversion via `encodeByteBuffer()` for both the array-backed and non-array-backed (`duplicate()`) code paths | Differentiator | UNIT | Line 262-275 branches on `buffer.hasArray() && arrayOffset()==0 && limit()==array().length` — likely only one of the two branches is exercised by existing `testStructToMapByteBufferToBase64`; a `ByteBuffer.allocateDirect(...)` or a sliced buffer would hit the fallback path. |
| `Date`/`Timestamp` custom Gson serializers emit epoch millis, matching Jackson's default | Differentiator | UNIT | Already partially covered (`testDateTypesJsonJacksonVsGson`) — good existing test, no gap. |
| `close()` clears the `ThreadLocal` `Base64.Encoder` | Anti-feature | — | Implementation detail of the `ThreadLocal.remove()` call; no observable behavior difference from the caller's perspective across a single test JVM — not worth a dedicated assertion. |
| Sink-level: JSON-string output value lands in a `String`/`JSON` ClickHouse column | Differentiator | ClickHouse-backed | PROJECT.md's "at least one sink-level test per fork SMT" — `JsonColumnTest.java` already exercises this at a basic level (row-count only, see gap in section 6 below). |

### 5. `targetTableFilter` (server-side `match()` regex)

Source: `ClickHouseHelperClient.showTables()` line 296-307 (builds `AND match(table, '<regex>')`, quotes escaped via `.replace("'", "''")` line 297). Existing tests: `ClickHouseSinkTaskMappingTest.tableFilterTest` (1 ClickHouse-backed) + `ClickHouseSinkTaskJsonInsertFormatTest.explicitJsonInsertIgnoresTargetTableFilter` (1, proves the filter is *bypassed* in the JSON-insert path).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| Regex matches a subset of tables; only matching tables are discoverable/insertable | Table stakes | ClickHouse-backed | Covered (`tableFilterTest`). |
| Empty `targetTableFilter` (default `""`) ⇒ no `AND match(...)` clause, all tables visible | Table stakes | ClickHouse-backed or unit | **Gap.** Line 296: `targetTableFilter.isEmpty() ? "" : ...` — currently only the non-empty branch is exercised end-to-end. A unit-level test of `showTables()`'s generated SQL string (no ClickHouse needed) would be cheapest; a full-container test is also valid since it's cheap once a container is already running for other mapping tests. |
| Regex containing a single quote (`'`) is escaped so `match()` doesn't break the SQL string | Table stakes | Unit (string-building) | **Gap** — this is a pure string-construction concern (`.replace("'", "''")`), fully unit-testable without a running ClickHouse: assert the generated query string for an input like `it's_a_table` produces `match(table, 'it''s_a_table')`. Additionally worth one ClickHouse-backed smoke test that a filter containing an escaped quote actually round-trips through ClickHouse's `match()` (regex containing a literal `'` character is unusual but must not break the query). |
| Regex with no matches at all (filters out every table) | Table stakes | ClickHouse-backed | Distinguish from "empty filter" — assert `showTables()` returns `[]`, and (paired with `suppressTableExistenceException`) sink startup/put behaves per that existing config rather than crashing unexpectedly. |
| `targetTableFilter` combined with `topicToTableMap` where the mapped table name does **not** match the filter | Table stakes | ClickHouse-backed | Already effectively covered by `tableFilterTest`'s two-topic setup (filtered vs. unfiltered mapped names) — confirm this remains true after other changes; count as covered. |
| Regex containing ClickHouse `match()`/RE2-specific syntax (e.g. anchors, character classes) behaves per RE2 semantics, not Java regex semantics | Differentiator | ClickHouse-backed | `match()` uses RE2 (via `re2_syntax` server-side), which differs subtly from `java.util.regex` (e.g. no backreferences). One test using a RE2-valid-but-Java-invalid-or-different-semantics pattern would catch a config author's mental-model mismatch — nice-to-have, not required. |
| Validating ClickHouse's own `match()`/RE2 engine correctness | Anti-feature | — | Re2/regex engine correctness is ClickHouse server behavior, not fork logic — don't test it beyond "the filter clause reaches the server and changes results". |

### 6. `JSON(...)` and `Dynamic` column mapping

Source: `Column.java` lines 152-156 (`valueType.startsWith("JSON")` → `Type.JSON`; `valueType.startsWith("Dynamic")` → `Type.DYNAMIC`). Existing tests: `JsonColumnTest.java` (7 enabled + 1 `@Disabled` perf-comparison test) — but **all existing `JsonColumnTest` assertions are `countRows(...)` only; none assert the actual JSON column content/subcolumn structure**. **No `Dynamic` column test exists at all.**

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| A column declared `JSON` (bare, no params) is mapped to `Type.JSON` from `DESCRIBE TABLE` output | Table stakes | Unit | `Column.extractColumn()` is directly unit-testable by feeding a synthetic `valueType="JSON"` string — no ClickHouse container needed for the mapping logic itself. |
| A column declared `JSON(max_dynamic_paths=100)` or similar parameterized form is still mapped to `Type.JSON` (the `startsWith("JSON")` check, not exact match) | Table stakes | Unit | **Gap** — this is the actual "JSON(...) with parameters" requirement from PROJECT.md; a bare `"JSON"` string test doesn't prove the parameterized form works, since `DESCRIBE TABLE` on a real `JSON(...)` column returns the type string with parameters inline (e.g. `JSON(max_dynamic_paths = 100)`). |
| A `Dynamic` column is mapped to `Type.DYNAMIC` from `DESCRIBE TABLE` output | Table stakes | Unit + ClickHouse-backed | **Gap, currently zero coverage.** Both the pure string-mapping (`Column.extractColumn`) and, separately, an end-to-end insert through a real `Dynamic` column. |
| Real insert into a `JSON(...)` column via the schemaless/JSON path (`doInsertJson`), and the inserted value is retrievable with correct nested paths (e.g. via `SELECT json_data.nested_str`) | Table stakes | ClickHouse-backed | **Gap** — current `JsonColumnTest` only asserts `countRows`, never queries back column content. Use `ClickHouseTestHelpers.getAllRowsAsJson()` (already exists, used elsewhere in the suite) or a targeted `SELECT ... FROM table` to assert actual values, not just row counts. |
| Real insert into a `Dynamic` column via the JSON path, retrievable with `dynamicType(col)`/correct value | Table stakes | ClickHouse-backed | **Gap**, zero coverage today. |
| `Dynamic`/`JSON` column via the **RowBinary** path (schema records where `csc.isBypassRowBinary()==false`) | Table stakes (as a documented pin, not a "works" assertion) | ClickHouse-backed | **Known risk, confirmed in code**: `ClickHouseWriter` has **no `case Type.JSON` / `case Type.DYNAMIC`** branch anywhere in its RowBinary serialization switch (`grep` for `Type.JSON`/`Type.DYNAMIC`/`case JSON`/`case DYNAMIC` in `ClickHouseWriter.java` returns nothing). A schema (`Struct`) record targeting a `JSON`/`Dynamic` column via the default RowBinary path will either silently mis-serialize or throw. Per PROJECT.md's "fix bugs found immediately" rule: write the test first to establish what actually happens (exception type, or silent corruption), then either (a) fix by routing `JSON`/`Dynamic` columns through `doInsertJson` regardless of `bypassRowBinary`, or (b) turn it into an explicit, clear error — do not leave it as an undocumented crash. |
| Multiple `JSON`/`Dynamic` columns in the same table, mixed with primitive columns | Differentiator | ClickHouse-backed | Confirms the mapping doesn't only work in isolation. |
| Full round-trip through `TableOperationFilter`/`HeadersToValue`/`ConvertToMapOrJson` into a `JSON`/`Dynamic` column | Differentiator | ClickHouse-backed | Cross-cutting confidence; not required for "covered". |
| ClickHouse's own `JSON`/`Dynamic` type storage/merge/compression internals | Anti-feature | — | That's ClickHouse server behavior; the fork's job is correct type mapping + correct insert format, nothing more. |

### 7. `describeTable` sub-column skipping

Source: `ClickHouseHelperClient.describeTableV1`/`describeTableV2`, lines 373-382 / 420-429 (identical logic duplicated in both). Existing tests: **none** (confirmed — `describeTable` has zero dedicated tests; only exercised incidentally through other mapping tests using simple primitive-typed tables that never produce alias/materialized/subcolumn rows).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| `Nullable(T)` column produces a `.null` UInt8 subcolumn in `DESCRIBE ... describe_include_subcolumns=1` output, and that subcolumn is skipped (not added to `Table`) | Table stakes | ClickHouse-backed | **Gap.** Create a table with a `Nullable` column, call `describeTable`, assert the resulting `Table`'s column list has exactly the logical columns (no `.null` entries) and `Table.getRootColumnsList()`/similar excludes them. |
| `Array(T)` column produces a `.size0` (and deeper `.sizeN` for nested arrays) subcolumn, skipped | Table stakes | ClickHouse-backed | **Gap.** Same pattern for arrays. |
| `JSON` column produces `.object_shared_data.` subcolumns (shared-data storage internals), skipped | Table stakes | ClickHouse-backed | **Gap**, and this exercises a real logic bug worth confirming: the condition is `fieldDescriptor.isSubcolumn() && (endsWith(".null") \|\| endsWith(".size0")) \|\| fieldDescriptor.getName().contains(".object_shared_data.")` (line 379/426) — due to Java operator precedence, the `.object_shared_data.` check is **not** gated by `isSubcolumn()` at all (it's `(A && B) || C`, not `A && (B || C)`). In practice this likely doesn't matter because a field literally named with `.object_shared_data.` in it can only be a subcolumn, but it's worth a precedence-aware test (assert a column whose *logical* name happens to contain the literal substring `.object_shared_data.` — contrived but proves the current boolean logic, not assumed intent). |
| Alias / `MATERIALIZED` / `EPHEMERAL` columns are skipped (pre-existing upstream behavior, not fork-added, but on the same code path fork changes touch) | Differentiator | ClickHouse-backed | Not a fork addition per PROJECT.md's inventory, but cheap to add alongside the subcolumn tests since the table fixture is shared; low priority. |
| `describeTableV1` and `describeTableV2` produce identical `Table` results for the same schema | Differentiator | ClickHouse-backed | Only relevant if the milestone ever runs both client versions; PROJECT.md scopes this milestone to client V2 only (Constraints section), so this is explicitly **out of scope for now** — note as a gap for a future client-V1 regression pass, not this milestone. |
| Column that "can't be handled" (`Column.extractColumn` returns `null`) causes `describeTable` to return `null` for the whole table (line 390-393 / 437-440) with a warning | Differentiator | ClickHouse-backed or unit | Existing behavior (not fork-specific to subcolumns, but adjacent) — worth one test since it's a real failure mode (table silently excluded from mapping) that an operator could hit. |
| Testing ClickHouse's own `describe_include_subcolumns` setting semantics beyond what the fork consumes | Anti-feature | — | The fork's contract is "given whatever subcolumns ClickHouse returns, skip the ones matching these name patterns" — don't test ClickHouse's subcolumn-generation rules themselves. |

### 8. Explicit `insertFormat=json` metadata-free path

Source: `ClickHouseWriter.shouldSkipTableMetadata()` (line 1235-1237, `csc.isJsonInsertFormatExplicit()`), used at `start()` (line 136-139), `updateMapping()` (155-158), `getTable()` (1208-1212), `cleanupExtraFields()` (1006-1009). Existing tests: `ClickHouseSinkTaskJsonInsertFormatTest.java` (2 ClickHouse-backed: startup-skip, target-table-filter-ignored).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| Sink starts successfully with **no target table present at all** (not just skipped validation, but literally zero tables in the database) | Table stakes | ClickHouse-backed | Covered by `explicitJsonInsertSkipsTableCheckOnStart` (drops table before start, creates it only after `start()` returns) — good existing test. |
| `getTable()` bypass uses `topicToTableMap.getOrDefault(topic, topic)` directly, without any `mapping` cache lookup | Table stakes | ClickHouse-backed or unit | **Gap.** Current tests prove records land in the right table, but don't isolate/assert the *bypass mechanism itself* — e.g. a unit test could call `getTable()` reflectively/via a package-visible seam and assert it never touches `this.mapping`. If that's awkward, a ClickHouse-backed test with topic-to-table mapping configured (topic name ≠ table name) and the table genuinely absent from `mapping` (never having been fetched) is an adequate proxy — this exists already via `explicitJsonInsertIgnoresTargetTableFilter`'s `TABLE_MAPPING` usage; confirm it actually proves the bypass (it currently only proves the *filter* is ignored, not that the mapping-cache lookup is skipped). |
| `cleanupExtraFields()` bypass: a value with fields **not present in the target table's schema** is inserted as-is (unknown fields sent to ClickHouse, relying on `input_format_skip_unknown_fields=1` server-side) | Table stakes | ClickHouse-backed | **Gap.** `shouldSkipTableMetadata()` short-circuits `cleanupExtraFields` (line 1007) — since table metadata was never fetched (`Table` object in the bypass path has no columns, see `getTable()` line 1211: `new Table(database, tableName)` with **no columns added**), `cleanupExtraFields` would have stripped *everything* if it ran; the explicit-JSON path must be proven to send the raw record unmodified. Test: insert a record with an extra field not in the real ClickHouse table's DDL, assert the insert still succeeds (ClickHouse silently drops the unknown field per `input_format_skip_unknown_fields`) and known fields still land correctly. |
| `targetTableFilter` is ignored (server-side `match()` clause never applied since `showTables()`/`extractTablesMapping` are never called) | Table stakes | ClickHouse-backed | Covered (`explicitJsonInsertIgnoresTargetTableFilter`). |
| `insertFormat=json` set via mixed case (`"Json"`, `"JSON"`) still triggers `jsonInsertFormatExplicit=true` | Differentiator | Unit | `ClickHouseSinkConfig` line 240: `insertFormatRaw.trim().equalsIgnoreCase("json")` — case-insensitivity is a real contract worth one unit test at the config level (no ClickHouse needed). |
| `insertFormat` unset (default) does **not** set `jsonInsertFormatExplicit=true`, even though the *default* `InsertFormats` enum value is also `JSON` (line 237) | Table stakes | Unit | **Important, easy-to-miss gap.** `this.insertFormat = InsertFormats.JSON` is the fallthrough default (unset config), but `jsonInsertFormatExplicit` is only `true` when the raw prop was literally present and equal to `"json"` (line 239-240: `insertFormatRaw != null && ...`). This is the crux of "explicit" in the feature name — a config-level unit test proving *default* (unset) `insertFormat` behaves like the normal schema/mapping-driven path (not the metadata-free bypass) is table stakes and currently untested at the config-object level (only indirectly implied by the fact that all *other* sink tests, which don't set `insertFormat`, pass). |
| Records with `null` value (tombstones) in the explicit-JSON path | Differentiator | ClickHouse-backed | Existing `doInsertString`-adjacent code (`record.getSinkRecord().value() != null` guards in `doInsertJson`) logs+skips; worth a test, but low risk since the guard is generic (not JSON-path-specific). |
| Interaction with `KeyToValue`/`ExtractTopic` upstream SMTs feeding the JSON path | Anti-feature | — | Those are upstream, not fork additions; combinatorial SMT-chaining tests belong to whichever SMT introduces the interaction risk, not to every consumer. |

### 9. `jsonInsertColumns`

Source: `ClickHouseSinkConfig` lines 241-252 (parsing) + `ClickHouseWriter.doInsertJson` line 998-999 (`client.insert(table.getName(), jsonInsertColumns, data, ClickHouseFormat.JSONEachRow, insertSettings)`). Existing tests: **none** (confirmed by PROJECT.md inventory and by direct grep — zero references to `JSON_INSERT_COLUMNS`/`jsonInsertColumns` outside `main/`).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| Empty `jsonInsertColumns` config (default `""`) ⇒ `getJsonInsertColumns()` returns an empty `List`, and the client-V2 `insert(...)` overload behaves as if no explicit column list were given | Table stakes | Unit + ClickHouse-backed | **Gap.** Unit-level: assert `ClickHouseSinkConfig` parses `""` to `List.of()`. ClickHouse-backed: assert inserts still work identically to the pre-existing (no `jsonInsertColumns`) explicit-JSON tests. |
| Whitespace-only entries between commas (e.g. `"col1, , col2"`) are dropped, not added as empty-string column names | Table stakes | Unit | **Gap.** Line 246-249: per-token `.trim()` then `if (!trimmed.isEmpty())` guard — directly unit-testable without ClickHouse. |
| Comma-separated explicit column list is passed to the insert and **only those columns** are populated from the JSON payload; columns not in the list use their table `DEFAULT`/materialize normally | Table stakes | ClickHouse-backed | **Gap, core behavior, zero coverage.** Create a table with 3+ columns including one with a `DEFAULT` expression or nullable-with-default; set `jsonInsertColumns` to a subset; send records containing all fields (or only the subset — see next row) in the JSON payload; assert the omitted/non-listed column keeps its default rather than being overwritten by same-named JSON fields (this is the entire point of explicit-column JSON inserts: it changes what ClickHouse treats as "the schema of this insert" independent of what keys exist in the JSON body). |
| JSON payload contains keys **not** in `jsonInsertColumns` — are they ignored by the client-side `insert(table, columns, ...)` call, or does ClickHouse reject/error? | Table stakes | ClickHouse-backed | **Gap.** This is the behavior PROJECT.md explicitly calls "insert of a column subset into a table with extra/default columns" — establish ground truth (likely: extra JSON keys are ignored since the explicit column list constrains what's inserted, consistent with `JSONEachRow` + an explicit column list at the protocol level) and pin it. |
| `jsonInsertColumns` naming a column that doesn't exist in the real table | Differentiator | ClickHouse-backed | Since this path skips table-metadata validation entirely when combined with `insertFormat=json` (the two features are designed to be used together), a nonexistent column name would surface only as a ClickHouse-side error at insert time — worth one test to confirm the failure mode is a clear exception, not a silent partial insert. Given PROJECT.md's "test WIP as-is" instruction, pin whatever currently happens rather than adding new validation. |
| `jsonInsertColumns` used **without** `insertFormat=json` (i.e. combined with the normal metadata-driven path) | Differentiator | ClickHouse-backed | The config is independent of `insertFormat`/`isJsonInsertFormatExplicit` in the code (`doInsertJson` always reads `csc.getJsonInsertColumns()` regardless of path) — worth confirming it also affects the normal (non-bypass) JSON/schemaless insert path, since `doInsertJson` is shared code (line 998 is inside the single `doInsertJson` method used by both `SCHEMA_LESS` and `bypassRowBinary` schema records, per `doInsert()` dispatch at lines 208-218). |
| Column ordering sensitivity (does `jsonInsertColumns` order matter, or is it order-independent since payload is JSON keyed by name?) | Anti-feature | — | `JSONEachRow` is a keyed format; ordering of an explicit column list for a keyed format is very unlikely to be insertion-order-sensitive in ClickHouse's insert path — not worth a dedicated test unless ground-truth testing above reveals otherwise. |

### 10. MBean `connector=` naming

Source: `ProxySinkTask.getMBeanName()` (line 71-79). Existing tests: **none** (confirmed — `SinkTaskStatistics`/`MBeanServerUtils` have no dedicated JMX-naming test in the inventory).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| `connectorName` present (non-null, e.g. `"my-connector"`) produces MBean name containing `connector=my-connector` | Table stakes | Unit | **Gap.** `getMBeanName()` is a private method — either make it package-visible for direct testing, or (preferred, lower-risk to rebasability) assert indirectly by registering the MBean (`ProxySinkTask` constructor calls `MBeanServerUtils.registerMBean(..., getMBeanName())` at line 68) and querying `MBeanServer.queryNames(...)` for an `ObjectName` matching the expected pattern. |
| `connectorName == null` (props without a `"name"` key — legitimate in unit tests that build `ProxySinkTask` directly, and technically possible if Connect ever invoked `start()` without a `name` prop, though Connect always supplies one in practice) produces `connector=null` (literal string, since `String.format("%s", null)` prints `"null"`, no `NullPointerException`) | Table stakes | Unit | **Gap.** PROJECT.md explicitly calls out "present/absent"; pin the literal-`"null"`-string behavior rather than assuming a crash or a blank segment — confirmed by reading `String.format`'s null-handling contract, no fork-specific null-guard exists for `connectorName` (only `version` has one, line 72-76). |
| Implementation version is `null` (e.g. running from an IDE / non-jar classpath where `Package.getImplementationVersion()` returns `null`) ⇒ `version=unknown` | Table stakes | Unit | **Gap.** Line 73-76 explicit guard; testable by asserting behavior in the existing dev/test classpath (where `getImplementationVersion()` typically *is* null, since it's `-jar`-only), OR by isolating the version-formatting logic into a small pure function invocation if `getMBeanName()` is refactored to be testable — prefer the non-invasive registration-based approach used above. |
| Version string containing `:` is sanitized to `_` (line 74) | Table stakes | Unit | **Gap.** This only fires when running from a built jar with a `Implementation-Version` manifest entry containing a colon (plausible for `git describe`-style versions like `v1.2.9:gg4` or SNAPSHOT builds) — if the local/CI classpath never has a colon-bearing version, this branch may be practically untestable end-to-end; consider extracting the sanitization into a small testable pure function (`sanitizeVersion(String)`) as a minimal, additive, rebase-safe refactor, OR accept it as a documented gap if extraction is judged too invasive for a coverage-only milestone. |
| Multiple `ProxySinkTask` instances (multiple tasks under one connector, or multiple connectors) register **distinct** MBean names (`SinkTask%d` via `NEXT_ID.getAndAdd(1)`, plus distinct `connector=` per connector name) | Differentiator | Unit | Confirms no collision/overwrite in `MBeanServerUtils` when >1 task is active — relevant for multi-task connectors, a real production shape. |
| `stop()` unregisters exactly the MBean that `getMBeanName()` currently computes (i.e. name is stable between construction and `stop()` — no mutable state affecting the name in between) | Differentiator | Unit | Cheap regression guard against a future change making `getMBeanName()` non-deterministic across calls. |
| JMX MBean attribute *values* (`receivedRecords`, `taskProcessingTime`, etc. — `SinkTaskStatistics` itself) | Anti-feature | — | Out of scope: MBean *naming* is the fork addition; the statistics-collection logic itself (`SinkTaskStatistics`) is pre-existing upstream code, not part of this milestone's fork-addition inventory. |

### 11. `async_insert` not forced

Source: `ClickHouseSinkConfig.java` line 209 — `//this.addClickHouseSetting("async_insert", "0", false);` is **commented out** (the intentional fork change: upstream forces `async_insert=0`, the fork no longer does). Existing tests: **none** (confirmed — no test asserts on the *absence* of a setting).

| Test case | Category | Complexity | Notes |
|---|---|---|---|
| Default config (no `clickhouseSettings` prop set) never injects `async_insert` into `csc.getClickhouseSettings()` | Table stakes | Unit | **Gap.** This is a "negative" assertion — a config-level unit test asserting `!csc.getClickhouseSettings().containsKey("async_insert")` after building `ClickHouseSinkConfig` with default props is the cheapest, most direct proof, and doesn't need a ClickHouse container at all. |
| User-supplied `clickhouseSettings=async_insert=1,wait_for_async_insert=0` is honored (passed through to `insertSettings.serverSetting(...)` / `request.set(...)` unmodified) | Table stakes | Unit or ClickHouse-backed | **Gap.** `clickhouseSettings` parsing (line 195-206) is generic key=value CSV parsing shared by *all* settings, not `async_insert`-specific — a unit test asserting the parsed map contains the user's `async_insert` value (not overridden) is sufficient and cheap; a ClickHouse-backed variant (assert the insert actually completes successfully with `async_insert=1` set, e.g. via `system.query_log`'s `Settings` map or simply via successful row count) is a stronger, still-reasonable-cost differentiator given a container is already running for other tests. |
| The two settings the fork **does** still force unconditionally (`input_format_skip_unknown_fields=1`, `wait_end_of_query=1`, `send_progress_in_http_headers=1`, lines 207-211) are **not** accidentally required to also cover `async_insert` non-forcing | Anti-feature (as a separate test) / fold into table stakes above | — | Don't write a redundant test enumerating every *other* forced setting — one well-named test (`asyncInsertNotForcedByDefault`) plus one for override-honored is sufficient; don't inflate the count. |
| Verifying ClickHouse server-side `async_insert` queuing/flush *behavior* itself (latency, batching internals) | Anti-feature | — | That's ClickHouse server behavior, not connector behavior — the fork's contract ends at "we don't send this setting unless the user asked us to." |

## Feature Dependencies

```
JaCoCo Gradle plugin added to build.gradle.kts
    └──required-before──> CI publishes coverage report (PROJECT.md Quality Gates item)

ClickHouseBase (Testcontainers ClickHouse, per-class @BeforeAll) running locally/CI
    └──required-before──> every "ClickHouse-backed" test cell in every table above
    └──required-before──> targetTableFilter, describeTable sub-column, JSON(...)/Dynamic,
                           jsonInsertColumns, explicit insertFormat=json, async_insert-honored
                           (server-side) tests

ClickHouse `latest` image pinned as the test target (PROJECT.md Constraint)
    └──required-for──> JSON(...)/Dynamic column tests specifically
                        (older ClickHouse versions lack these types entirely —
                         this is why the milestone scopes out 23.7/24.3)

TableOperationFilter / HeadersToValue / HeaderToValue / ConvertToMapOrJson unit tests
    └──independent-of──> ClickHouse container (pure SMT unit tests, fastest feedback loop)
    └──feeds-into──> "sink-level" differentiator test per SMT (needs ClickHouseBase)

describeTable sub-column skipping tests
    └──shares-fixture-with──> JSON(...)/Dynamic column mapping tests
                               (both need a table with Nullable/Array/JSON columns —
                                write the DESCRIBE-based tests and the insert-based
                                tests against the same table schema to avoid duplicated
                                container setup cost)

Explicit insertFormat=json path tests
    └──shares-fixture-with──> jsonInsertColumns tests
                               (jsonInsertColumns is consumed by the same doInsertJson
                                method used by the explicit-JSON bypass path; a single
                                ClickHouseSinkTaskJsonInsertFormatTest-style test class
                                can cover both without duplicating the "skip metadata"
                                setup)

Bug found by any test above (e.g. Dynamic/JSON via RowBinary, nested Struct-in-Array/Map
in ConvertToMapOrJson)
    └──triggers──> "fix in the same phase" rule (PROJECT.md Key Decision) — the fix itself
                   is a small, targeted, additive change per the Constraints section
                   (no ClickHouseWriter/Processing/ClickHouseSinkConfig refactor)
```

### Dependency Notes

- **JaCoCo requires no other feature but blocks CI coverage publishing**: it's a build-tooling addition (new Gradle plugin), independent of any test content, but the milestone's Definition of Done requires a coverage report to exist before CI can publish it — sequence it early (e.g. first phase) so every subsequent phase's tests are visible in coverage from day one, not retrofitted.
- **ClickHouseBase (Docker/Testcontainers) is the single hard environmental dependency** for roughly two-thirds of the table-stakes cells above. PROJECT.md's Context section already flags that Docker/colima was not running at initialization — establishing "does `./gradlew test` pass on `latest`/V2 today" (also a PROJECT.md open item) is the correct first gate before writing any ClickHouse-backed test.
- **`describeTable` sub-column skipping and `JSON(...)`/`Dynamic` mapping are best planned together**: both need the same kind of fixture table (columns of type `Nullable(...)`, `Array(...)`, `JSON`, `Dynamic`) and the same `describe_include_subcolumns=1` DESCRIBE-based introspection. Splitting them into unrelated phases would duplicate container/table setup for no benefit.
- **Explicit `insertFormat=json` and `jsonInsertColumns` share `doInsertJson`**: `jsonInsertColumns` is not gated by `isJsonInsertFormatExplicit()` in the code — it's read unconditionally inside `doInsertJson` (used by both the `SCHEMA_LESS` case and the `bypassRowBinary` schema case, see `doInsert()` dispatch). Test-planning these as one connected group (rather than two unrelated features) avoids missing the "used without `insertFormat=json`" interaction case.
- **Bug-fix rule creates a soft dependency from "write the test" to "fix or explicitly document the failure"**: for `Type.DYNAMIC`/`Type.JSON` via RowBinary and for `ConvertToMapOrJson`'s nested-Struct-in-Array/Map gap, the *test* must be written before it's known whether a fix is even needed — plan these as "establish ground truth first" tasks rather than assuming pass/fail up front.

## MVP Definition

Re-interpreted for a test-coverage milestone: "launch" = the milestone's Definition of Done (PROJECT.md Active requirements + Quality Gates).

### Launch With (v1) — Required for "Covered"

Every "Table stakes" row across all 11 sections above. In priority-of-risk order (highest-value gaps first, per this research):

- [ ] `jsonInsertColumns` — column-subset insert behavior (zero coverage today, core WIP feature)
- [ ] `JSON(...)`/`Dynamic` real-insert content assertions (today only row-counts are asserted; also the RowBinary-path known-risk must be resolved or documented)
- [ ] `HeadersToValue` field-name-collision behavior (explicitly flagged in PROJECT.md, has a real divergent-behavior finding above)
- [ ] `describeTable` sub-column skipping for `Nullable`/`Array`/`JSON` (zero coverage today)
- [ ] `ConvertToMapOrJson` nested Struct-in-Array/Map behavior (possible real bug, PROJECT.md-flagged)
- [ ] `TableOperationFilter` truncate (`t`), multi-table, invalid-operation-token cases
- [ ] `HeaderToValue` multi-valued/missing-header/config-mismatch cases
- [ ] MBean `connector=` naming (present/absent/null-version) — no coverage today
- [ ] `async_insert` not-forced negative assertion + user-override-honored assertion
- [ ] `targetTableFilter` empty-filter and quote-escaping cases (string-level, cheap)
- [ ] `ConvertToMapOrJson` `preserve.null.values=false` and invalid-library `ConfigException`
- [ ] Config-validation (`ConfigDef`/`DataException`) tests for all 4 SMTs — **currently zero exist across the entire SMT test suite**, confirmed by direct grep; this is the single biggest structural gap versus upstream Kafka Connect SMT testing conventions (see Sources)

### Add After Validation (v1.x) — Differentiators

- [ ] One sink-level (ClickHouse-backed) test per SMT proving transformed records land correctly — PROJECT.md already scopes this in but it's inherently lower-urgency than the unit-level gaps since it re-proves plumbing more than new logic
- [ ] RE2-specific regex semantics for `targetTableFilter`
- [ ] JMX multi-task/multi-connector name-collision-avoidance
- [ ] `describeTableV1`/`V2` parity (deferred — milestone is V2-only per Constraints)

### Future Consideration (v2+) — Explicitly Out of Scope This Milestone

- [ ] `HeaderToValueConfig` dead-code investigation/cleanup (not a test-coverage task; flag for a future phase if confirmed unused)
- [ ] MBean version-sanitization (`:` → `_`) testable only via a built-jar classpath with a colon-bearing version — consider a small extraction refactor in a future milestone if truly needed, not this one (Constraints section forbids non-targeted refactors)
- [ ] ClickHouse 23.7/24.3 and client V1 regression matrix (explicitly Out of Scope in PROJECT.md)

## Feature Prioritization Matrix

| Fork addition | Test Value | Coverage Cost | Priority |
|---|---|---|---|
| `jsonInsertColumns` | HIGH (zero coverage, core WIP feature, production-used) | MEDIUM (ClickHouse-backed, needs default-column fixture) | P1 |
| `JSON(...)`/`Dynamic` real content + RowBinary risk | HIGH (zero content coverage, known bug risk) | MEDIUM-HIGH (needs `latest`, careful fixture, possible bug fix) | P1 |
| `describeTable` sub-column skipping | HIGH (zero coverage, core to type mapping correctness) | MEDIUM (shared fixture with JSON/Dynamic) | P1 |
| `HeadersToValue` collision + null/no-headers | HIGH (explicitly flagged, real divergent-behavior finding) | LOW (pure unit) | P1 |
| `ConvertToMapOrJson` nested array/map + preserve-null | MEDIUM-HIGH (possible bug, PROJECT.md-flagged) | LOW-MEDIUM (pure unit) | P1 |
| SMT `ConfigDef`/`DataException` validation (all 4) | HIGH (structural gap vs. upstream convention, cheap) | LOW (pure unit, no container) | P1 |
| `TableOperationFilter` truncate/multi-table/invalid-op | MEDIUM (explicitly flagged, moderate risk) | LOW (pure unit) | P1 |
| `HeaderToValue` multi-value/missing/config-mismatch | MEDIUM (explicitly flagged) | LOW (pure unit) | P1 |
| MBean `connector=` naming | MEDIUM (zero coverage, low production risk) | LOW (unit, JMX registry query) | P1 |
| `async_insert` not forced | MEDIUM (intentional behavior change, easy to regress silently on rebase) | LOW (pure unit) | P1 |
| `targetTableFilter` empty/quote-escaping | MEDIUM (edge cases, core behavior already covered) | LOW (string-level unit) | P1 |
| Sink-level per-SMT tests | MEDIUM (re-proves plumbing) | MEDIUM (ClickHouse-backed) | P2 |
| RE2 regex semantics for `targetTableFilter` | LOW-MEDIUM (nice-to-have confidence) | LOW | P2 |
| JMX multi-task naming | LOW (edge case, low production impact) | LOW | P2 |
| `describeTableV1`/V2 parity | LOW (out of milestone scope, V2-only) | MEDIUM | P3 |
| `HeaderToValueConfig` dead-code check | LOW (cleanup, not coverage) | LOW | P3 |

**Priority key:** P1 = must-have for "covered" per PROJECT.md Active requirements; P2 = should-have, add once P1 gaps are closed; P3 = defer past this milestone.

## Reference Test Suites Consulted (in place of "Competitor Analysis")

This is a fork of an open-source connector, so the useful comparison isn't competing products — it's the **testing conventions of the two upstream projects this fork sits on**: Apache Kafka Connect (the `Transformation<R>` SPI these SMTs implement) and `ClickHouse/clickhouse-kafka-connect` (the connector being forked).

| Convention | Apache Kafka Connect SMTs (`HeaderFrom`, `InsertHeader`) | Fork's current SMT tests | Fork's target state |
|---|---|---|---|
| Schema vs. schemaless coverage | `HeaderFromTest` runs its core assertions through **both** `schemaless()` and `withSchema()` parameterized methods for every scenario — every behavior is proven in both modes, not just one | All 4 fork SMTs have separate `applySchemalessTest`/`applyWithSchemaTest` methods, but only for the happy path; edge cases (nulls, collisions, config errors) are untested in **either** mode | Every table-stakes edge case above should get both a schemaless and a with-schema variant where the code path actually differs (it does for `HeadersToValue`/`HeaderToValue`/`ConvertToMapOrJson`) |
| Config validation (`ConfigDef`/`ConfigException`) | `InsertHeaderTest` has dedicated `configRejectsNullHeaderKey`/`configRejectsNullHeaderValue`; `HeaderFromTest` has `invalidConfigExtraHeaderConfig`/`invalidConfigExtraFieldConfig`/`invalidConfigEmptyHeadersAndFieldsConfig` — config-error paths are first-class, separately-named test methods | **Zero** config-validation tests exist across all 4 fork SMTs (confirmed by direct grep for `assertThrows`/`ConfigException`/`DataException` in `src/test/java/transforms/`) despite 3 of the 4 SMTs having explicit `configure()`-time guards (`HeadersToValue` blank-field check, `HeaderToValue` size-mismatch check, `ConvertToMapOrJson` invalid-library check) | Add one dedicated, clearly-named test method per existing `configure()`-time guard — matches upstream naming convention (`configRejects...`/`invalidConfig...`) |
| Header mutation semantics (last-wins, existing-header handling) | `InsertHeaderTest.insertionWithExistingSameHeader`/`insertionWithExistingOtherHeader` explicitly test collision with pre-existing headers of the same/different name | Fork's `HeaderToValue` already uses `lastWithName()` (the Kafka-convention "last wins" for duplicate headers) but has no test proving it; `HeadersToValue` doesn't use `lastWithName` and its duplicate-key behavior is implicit | Both SMTs need an explicit duplicate-header test; the two should be contrasted since they behave differently (see section 2/3 above) |
| `newRecord`/immutability contract | Not separately named in upstream tests but implicit — Kafka Connect's own `ConnectRecord.newRecord` overload contracts (topic/partition/timestamp/headers preserved unless explicitly changed) are exercised via the same before/after struct assertions in every SMT test | No fork SMT test currently asserts topic/partition/offset/timestamp/headers are preserved on the *output* record — all assertions inspect `result.value()` only | Add one `newRecord`-preservation assertion per SMT (cheap, catches accidental overload misuse — relevant here because `ConvertToMapOrJson` uses a different `newRecord` overload than the other 3) |
| Debezium envelope / CDC-shaped records | Not applicable — upstream Kafka Connect SMTs are envelope-agnostic | `TableOperationFilter` is the fork's only Debezium-aware SMT; existing tests cover missing-field defensive branches well but not the `t` (truncate) operation or `r` (snapshot read, never-filterable) operation | Add the `t` and `r` cases per section 1 above |
| ClickHouse-backed content assertions vs. row-count-only assertions | N/A (upstream Kafka Connect SMTs don't sink anywhere) | `JsonColumnTest` (the fork's most relevant precedent for `JSON`/`Dynamic` coverage) asserts **only** `countRows` — never queries back actual column content/paths | Use the existing `ClickHouseTestHelpers.getAllRowsAsJson()` helper (already present in the test helper class, used elsewhere in the suite) to assert real values for `jsonInsertColumns` and `JSON(...)`/`Dynamic` tests, not just counts |

## Sources

- Fork source (read directly, commit `22f055c`, branch `custom_build`): `src/main/java/com/clickhouse/kafka/connect/transforms/{TableOperationFilter,HeadersToValue,HeaderToValue,HeaderToValueConfig,ConvertToMapOrJson}.java`; `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`; `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`; `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`; `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`; `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java`; `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`
- Fork tests (read directly): `src/test/java/transforms/{TableOperationFilterTest,HeadersToValueTest,HeaderToValueTest,ConvertToMapOrJsonTest,ConvertToMapOrJsonPerformanceTestJson,KeyToValueTest}.java`; `src/test/java/com/clickhouse/kafka/connect/sink/{ClickHouseSinkTaskJsonInsertFormatTest,ClickHouseSinkTaskMappingTest,JsonColumnTest}.java`; `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`
- `.planning/PROJECT.md` and `.planning/codebase/TESTING.md` (required reading, milestone scope and existing test inventory)
- [Apache Kafka `HeaderFrom` PR #9549 (KIP-145)](https://github.com/apache/kafka/pull/9549) — upstream SMT design/test-convention reference
- Apache Kafka `HeaderFromTest.java` and `InsertHeaderTest.java` (`connect/transforms/src/test/java/org/apache/kafka/connect/transforms/`, `trunk` branch, fetched 2026-09-15) — parameterized schemaless/withSchema pattern, `configRejects*`/`invalidConfig*` naming convention
- [ClickHouse Kafka Connect discussion #182 — Debezium tombstone/null handling](https://github.com/ClickHouse/clickhouse-kafka-connect/discussions/182) — context on why `TableOperationFilter`-style Debezium-envelope filtering exists in CDC pipelines
- [ClickHouse/clickhouse-kafka-connect (upstream repo)](https://github.com/ClickHouse/clickhouse-kafka-connect) — parent project this fork diverges from

---
*Feature research for: ClickHouse Kafka Connect fork test coverage*
*Researched: 2026-09-15*
