# Phase 13: Fork Hardening on the Merged Tree - Context

**Gathered:** 2026-10-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Close the fork's open correctness follow-ups on top of the `v1.7.0` merge (Phase 12). There are 15 requirements: JSONP-05..08, WRT-07/08, DESC-05, TYPE-08, COLS-07, CFG-04 and DEBT-01..05.

- **R-01 fixed by routing:** `Struct` records (and Debezium-envelope records) in metadata-free `insertFormat=json` mode reach the JSON writer and land with correct typed values.
- **Writer null-correctness:** a null inside `Map(K, Nullable(V))` round-trips as `NULL`. A nested null no longer desyncs RowBinary.
- **Metadata/config fixes:** DESCRIBE skip at any depth, the `Int256`/`UInt256` fail-fast, the `extractColumn` argument order, the `USERNAME` ConfigDef default, and the `cleanupExtraFields` operand order.
- **Test debt:** HeaderToValue pin, `JsonColumnTest` read-back, JSON-format `try/finally`, `tearDown()` drop failures, stale Javadocs.

Not in this phase: dependency bumps (Phase 14), writing CHANGELOG/FORK-DELTA prose (Phase 15, though this phase must record each behaviour change as it lands), and publication.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
- R-01 is fixed by **routing**, not by a throw. Wrong logical-type values on the route are **fixed**, not documented (REQUIREMENTS locked decisions).
- The routing predicate is `shouldSkipTableMetadata()`, never the raw format string. The metadata-free mode, its guard sites and the synthetic `new Table(database, name, false, List.of(), 0)` stay as they are (Phase 9 D-01/D-03/D-04/D-05).
- A top-level null in a non-nullable Map/Array keeps upstream #820's coercion to empty. The MAP null guard is scoped to `col.isNullable()` (Phase 12 WRT-06).
- CR-02 pins may be re-pinned to the correct bytes under the user-approved tightening of 2026-10-07. Re-pinning is recorded as a tightening, never a weakening.
- Typed SQL read-back, never `countRows`. Existing tests are never deleted or weakened. Bug fixes ship with the test that exposes them.
- Requirement wording is corrected by **annotating** with a dated note, never by rewriting the text (Phase 9 D-07 convention).

### JSON-route logical-type encoding (JSONP-06)
- **D-01:** **The reference for "correct" is RowBinary parity.** A cell passes when the value ClickHouse stores via the JSON route equals what the same `Struct` stores via RowBinary into the same column. The matrix may write both routes into one table and compare them, so no cell needs a hand-written expected value.
- **D-02:** **No forced settings.** The encoding must parse correctly with the server's and operator's settings unchanged. The JSON route must not add `date_time_input_format=best_effort` or any other insert setting, which matches the project's "don't force `async_insert`" stance. In practice `Timestamp`/`Date` need a **time-zone-independent numeric (epoch) encoding**. A string such as `"2024-01-01 00:00:00"` is read in the server's or column's time zone, while RowBinary is not. The pre-planning spike picks the exact numeric form that reaches parity in both `DateTime` and `DateTime64(p)`, including precisions other than 3.
- **D-03:** **Kafka `bytes` → `String`: raw-byte parity, otherwise fail.** Write the raw bytes with the minimal JSON escaping that the spike proves ClickHouse's JSONEachRow parser stores verbatim, including non-UTF-8 bytes. If the spike shows that raw bytes cannot survive, a `bytes` field on this route throws a `DataException` naming the field. It must not store base64 or any other different bytes.
  - **Reversibility:** costly. If it throws, the change is visible to any operator whose Structs carry `bytes` on this route.
- **D-04:** **The matrix covers only the pairs RowBinary supports.** Those pairs are:
  - `Timestamp`→`DateTime`/`DateTime64`
  - `Date`→`Date`/`Date32`
  - `Time`→`Int32` (milliseconds since midnight)
  - `Decimal`→`Decimal`
  - `bytes`→`String`
  - nulls (JSONP-07) and a nested `Struct`

  Pairs that RowBinary rejects are out of scope, and the JSON route's behaviour on them is not pinned.

### Conversion scope (JSONP-05/06)
- **D-05:** **The conversion applies only on the metadata-free route,** gated on `shouldSkipTableMetadata()`. Upstream's `bypassRowBinary=true` JSON output stays byte-identical to upstream (prefer-upstream, rebasability). Its Jackson-default limitation is recorded in FORK-DELTA, not fixed. The combination `bypassRowBinary=true` + metadata-free also passes the gate and so is converted.
- **D-06:** **Route and convert `SCHEMA` and `DEBEZIUM_CDC` alike.** Both share the switch case in `doInsertBatch`, and both silently lose rows today in metadata-free mode. Apply the same conversion to `DEBEZIUM_CDC` values on this route. The matrix gets at least one CDC row, for example a `Timestamp` and a `Decimal` field in the `after` struct.
- **D-07:** **Recurse at every depth, and pin a small set of nested cells.** Logical types inside containers (`Array`, nested `Struct`/`Tuple`, `Map` values) are converted too, so parity holds wherever the value sits. Pin a handful of nested cells, for example `Array(DateTime64)`, a `Decimal` inside a nested `Struct`, and `Map(String, Date)`, not the full cross-product.
- **D-08:** **The routing change is unconditional, with no config flag.** Operator fact 2 says no live pipeline takes this route. Record it for CHANGELOG, FORK-DELTA and the Phase 15 rollout checklist as it lands. Include the KeeperMap backfill note: offset ranges already marked `AFTER_PROCESSING` under R-01 are not replayed without a consumer offset reset **and** clearing the KeeperMap state (research PITFALLS §5).
  - **Reversibility:** one-way. Once published, pipelines on this route start writing rows, and some may start failing on type mismatches. Reverting would bring back silent data loss for them.

### Nested-null containers (WRT-07/08)
- **D-09:** **A nested null *container* (Array/Map) in a non-nullable slot is always coerced to empty,** whatever `table.hasDefaults()` says. This mirrors #820's top-level rule. Today the bytes come out as an empty value only by accident when the table has DEFAULT columns, and desync the stream when it has none. WRT-08's `DataException` therefore covers **nested non-nullable scalar slots and Tuples only**. Add a dated annotation to WRT-08 in `REQUIREMENTS.md` recording the narrowing for containers, without rewriting the text.
  - **Reversibility:** costly. It defines operator-visible semantics for nested nulls, and FORK-DELTA must record the split so the next sync sees the reasoning.
- **D-10:** **The CR-02 TUPLE early-null branch gets the same scoping as MAP.** When `col.isNullable()`, write the null marker. Otherwise throw a `DataException` naming the column. If the spike confirms `Nullable(Tuple)` is illegal on `latest`, the nullable half is dead code but harmless. Any CR-02 Tuple pin that asserts `0x01` for a non-nullable slot is re-pinned under the 2026-10-07 approval, with a rationale comment next to the assertion. The VARIANT branch (discriminator 255) is correct and stays.
- **D-11:** **Leave upstream's top-level `RuntimeException` alone.** It covers a null in a non-nullable scalar or Tuple column at the top level. Only the new nested-slot failures throw `DataException`. Record the difference in FORK-DELTA. Existing top-level DLQ and tolerance behaviour is unchanged.
- **D-12:** **The `DataException` message names the column, its type and the slot kind/path, with no data values.** Example: `column 'tags' (Array(String)) element: null in non-nullable slot`. Never include array indices' values, map keys or field values. Map keys are record data, and these messages reach logs and DLQ headers (AGENTS.md: no record data in logs).
- **Note (not a choice):** the throw happens mid-stream, so the whole batch fails with no rows written. With `errors.tolerance=all`, the whole batch goes to the DLQ through the existing `Utils.handleException` path. Per-record isolation would change batch composition, which the exactly-once invariant forbids. It is out of scope.

### Small-fix depth
- **D-13 (JSONP-08 / P9-IN-01):** **Swap the operands and correct the docs.**
  - Change the guard to `shouldSkipTableMetadata() || csc.isBypassFieldCleanup()`.
  - Pin the order with a Docker-free Mockito test: in metadata-free mode, `isBypassFieldCleanup()` is never called.
  - Correct the comment/Javadoc so it stops claiming null-safety. A null `csc` still throws an NPE, because `shouldSkipTableMetadata()` returns `false` for null.
  - Behaviour is identical for every non-null `csc`.
  - Add a dated correction note to JSONP-08 explaining that "null-safety" is not what the swap delivers. Do not add a `csc == null` short-circuit.
- **D-14 (DEBT-04):** **`tearDown()` rethrows and the class goes red.** Wrap a database-drop failure on the shared container with the test class's simple name and the database name, then rethrow from `@AfterAll` so JUnit reports it against the class that leaked. Server log copy still happens first. A flaky drop is allowed to fail an otherwise-green class, which is preferable to silent cross-class pollution.
- **D-15 (DEBT-01):** **Pin HeaderToValue's current behaviour, with no main-code change.**
  - Schema path: when `fields` names an existing non-STRING field such as `INT32`, the old field schema is kept, `Struct.put` of the header's String fails validation, and the SMT throws `DataException("Failed to transform record")`. Assert the exception and its cause.
  - Schemaless path: the header String silently replaces the value. Pin that too.

### Post-research decisions (2026-10-08, answers to 13-RESEARCH.md Open Questions)
- **D-16 (D-10 confirmed despite false premise):** `Nullable(Tuple)` is legal on `latest`, and `Column.extractColumn` drops Tuple nullability. Follow D-10 **literally anyway**: a null in a Tuple slot that `Column` reports as non-nullable throws the nested `DataException`. Do **not** change `extractColumn` nullability. The single-field `Array(Nullable(Tuple(a Int32)))` null element moves from round-tripping as `NULL` (by coincidence) to a `DataException`. Pin that change with a live test and record it in the behaviour ledger for FORK-DELTA.
- **D-17 (Date into a DateTime column on the JSON route):** encode Kafka `Date` as **integer days** everywhere on the metadata-free route. Pin `Date`→`Date`/`Date32` parity. Record `Date`→`DateTime`/`DateTime64` as a known gap with no parity in the behaviour ledger for FORK-DELTA, and leave it out of the parity matrix.
- **D-18 (CDC conversion scope and schema access):** add a small additive `Data.getSchema()` getter so the writer can dispatch on `schema.name()` for CDC values. Convert only the types `DebeziumRecordConvertor` already emits: Connect `Timestamp`, `java.sql.Date` and `MicroTimestamp`. Pass `io.debezium.time.Timestamp` longs and `ZonedTimestamp` strings through unchanged, and record in the ledger that raw `io.debezium.time.Timestamp` millisecond longs are read as seconds.
- **D-19 (empty CDC records on the JSON route):** on the metadata-free JSON route, skip a `DEBEZIUM_CDC` record whose converted map is empty, so no `{}` row of defaults is written. This covers `op=t` (truncate) and a null `before`/`after` struct. Pin it with a test.
- **Defaults taken without asking** (research recommendations):
  - WRT-07 is already green, so it lands as a pin with no RED. An optional throwaway mutation shows the pin has teeth and is not committed.
  - The nested `DataException` reaches `put()` callers as the root cause of a `RuntimeException`, because `Processing.doInsert` wraps it. Tests walk the cause chain, and the plan and ledger state this correctly.
  - Pre-existing `Nullable(Enum8/16)` corruption is logged to the backlog, not fixed here.
  - D-12's message type text comes from a small renderer over the `Column` tree, or from `Type` names. Tests assert substrings: column name, slot kind, and the absence of values.

### Claude's Discretion
- **Spike shape.** The research flag requires a spike before planning on (a) what each logical type stores through Jackson today versus RowBinary, and (b) WR-02's nested-versus-top-level discriminator in `doWriteColValue`, for example an extra parameter versus `col` parent info. Choosing the exact numeric `Timestamp` encoding (D-02), proving raw-bytes survival (D-03), and checking `Nullable(Tuple)` legality (D-10) are the spike's outputs. The planner decides whether it runs as a research task or as the first plan's RED step.
- **Where the conversion lives.** Inside `writeRecordsJson`, through a route-scoped Jackson module/serializer, or as a pre-pass over the `data` map. It must not change `DataJson.OBJECT_MAPPER`'s output for the non-metadata-free routes (D-05).
- **Decimal encoding,** such as avoiding scientific notation and preserving scale, and `DateTime64` precisions other than 3. Both are covered by D-01 parity; the planner picks the mechanism.
- **JSONP-07 null semantics** follow from `NON_NULL` inclusion plus ClickHouse's omitted-field defaults. The planner pins them as they are (Nullable → `NULL`, non-nullable → column `DEFAULT`), and pins the interactions with `jsonInsertColumns` (server-side 5-arg insert, Phase 9 D-06) and with `bypassFieldCleanup` (moot in metadata-free mode).
- **R-01 test flip.** Rename `ExplicitJsonInsertPathLiveTest.explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch` to a typed read-back test that keeps an origin note. Remove the "R-01 PIN - NOT AN ENDORSEMENT" Javadoc deliberately, citing the milestone approval in the commit message. Add a Docker-free routing unit test next to `MetadataFreeJsonInsertUnitTest`, overriding the `protected` `doInsertJson`/`doInsertRawBinary`.
- **Plan and wave breakdown.** R-01 routing/conversion, WR-02/WRT-07, P9-IN-01 and TYPE-08 serialise on `ClickHouseWriter.java`. Research suggests routing first, so the JSON baseline is settled before WR-02. DESC-05, COLS-07, CFG-04 and DEBT-01..05 touch other files and can be parallel plans. Commits follow one behaviour per commit, and `spotlessApply` runs only on the touched files (Phase 9 D-13).
- **Where behaviour-change notes accumulate** until Phase 15 writes CHANGELOG/FORK-DELTA prose, for example a section in the phase SUMMARY or a ledger file. Both R-01 (D-08) and the nested-null semantics (D-09..D-11) must be recorded.
- **TYPE-08 / DESC-05 / COLS-07 / CFG-04 test shapes.**
  - TYPE-08: the gate covers `INT256`/`UINT256` only, and `Int128`/`UInt128` round-trip live.
  - DESC-05: must not widen what gets dropped. Pin it with `Nullable`/`Array`/`Map`/`Tuple`/`JSON`/`Dynamic` nesting.
  - COLS-07: needs a unit test that actually reaches the dead Array recursion.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and locked decisions
- `.planning/ROADMAP.md` § Phase 13: goal, success criteria, research flag, ordering note
- `.planning/REQUIREMENTS.md`: the locked decisions table (top), plus JSONP-05..08, WRT-07/08, DESC-05, TYPE-08, COLS-07, CFG-04, DEBT-01..05
- `.planning/STATE.md` § Accumulated Context: carried-forward rules (typed read-back, UTF-8 readers, `GenericRecord` snapshotting, tables before `start()`, `--rerun` gates, `env -u TESTCONTAINERS_HOST_OVERRIDE`)

### Research (milestone-level)
- `.planning/research/ARCHITECTURE.md` §(c) R-01 integration point, plus the follow-up collision table (WR-01/02, P9-IN-01, P3-WR-03, P3-WR-01) and the Phase 13 ordering
- `.planning/research/PITFALLS.md`: Pitfall 5 (R-01 consequences: exactly-once, KeeperMap backfill, type fidelity), Pitfall 10 (WR-02 vs #820), Pitfall 11 (P3-WR-03 narrowed), and the P9-IN-01 / USERNAME / extractColumn / shouldSkipDescribeRow notes
- `.planning/research/FEATURES.md`: findings 2-4, 7-8 (P3-WR-03 scope, MAP/TUPLE guard collision, P9-IN-01 cosmetic, `StructToJsonMap` dependency, nested-container asymmetry)

### Prior phase decisions
- `.planning/milestones/v1.6.0_gg1-sync-phases/09-metadata-free-json-path-keep-or-drop/09-CONTEXT.md`: metadata-free KEEP, guard sites, synthetic `Table`, `jsonInsertColumns` server-side, annotate-don't-flip
- `.planning/phases/12-upstream-v1-7-0-merge-reconcile/12-CONTEXT.md`: merge shape, WRT-06 scoped MAP guard, CR-02 re-pin approach

### Fork documentation
- `docs/FORK-DELTA.md`: the metadata-free row, the R-01 "Known costs" entry, and where the D-05/D-09/D-11 differences get recorded (Phase 15 writes the prose)
- `.planning/MERGE-CHECKLIST-v1.7.0.md`: the v1.7.0 Recorded Facts convention for spike results
- `AGENTS.md`: exactly-once invariants (no batch-composition change), no record data in logs, never weaken tests, Google Java Style / spotless

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ClickHouseWriter.shouldSkipTableMetadata()` (`ClickHouseWriter.java:1632`): the single route predicate for D-05/D-06.
- `ClickHouseWriter.writeRecordsJson` `case SCHEMA` / `case DEBEZIUM_CDC` (`ClickHouseWriter.java:~1287-1304`): where the Struct→Map and serialisation happen. `struct.get(field)` passes raw `java.util.Date`, `BigDecimal` and `byte[]` objects to Jackson.
- `util/DataJson.OBJECT_MAPPER`: `NON_NULL` inclusion plus `JacksonStructSerializer`. It is shared with the upstream routes, so it must not change globally (D-05).
- `sink/data/StructToJsonMap`: identifies `Decimal`/`Date`/`Time`/`Timestamp` by `LOGICAL_NAME` but keeps raw objects. Useful for its detection pattern, but it does not convert.
- RowBinary reference behaviour for D-01 parity: `doWritePrimitive` (`Date`→`Int32` millis for `Time`; `DateTime64`/`Int64` from `date.getTime()`), `doWriteDates`, `Decimal` via `writeDecimal(precision, scale)`, `byte[]`→`writeString` raw.
- Tests:
  - `MetadataFreeJsonInsertUnitTest` (reflective `csc` accessor, Docker-free)
  - `ExplicitJsonInsertPathLiveTest` (R-01 pin to flip)
  - `NestedNullableCompositeWriteTest` (CR-02 pins)
  - `RowBinaryUnsupportedTypeGateTest` (TYPE-08)
  - `JsonSerializationEquivalenceTest`, `AvroDecimalLogicalTest`, `StructToJsonMapTest`
  - `transforms/HeaderToValueTest`, `JsonColumnTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `ClickHouseVersionDefaultUnitTest`, `AsyncInsertLiveTest`, `db/helper/NetworkCompressionPin{Unit,Live}Test`, `db/mapping/TableTest`

### Established Patterns
- `doWriteColValue` (`ClickHouseWriter.java:568`) recurses into ARRAY/MAP children with the **same** `defaultsSupport` flag. That is why a nested null container writes `0x00` (accidentally empty) with defaults and nothing without. D-09's always-coerce must emit the empty-size byte independently of `defaultsSupport` in nested context.
- `doWriteCol` (`ClickHouseWriter.java:~950`) is the top-level path. Null in a non-nullable Array/Map gets coerced; null in a non-nullable scalar or Tuple throws `RuntimeException("An attempt to write null into not nullable column ...")`, which is untouched (D-11).
- The MAP branch's WRT-06 guard (`value.getObject() == null && col.isNullable()`) is the template for D-10's TUPLE scoping.
- `Utils.handleException`: a `DataException` with `errors.tolerance=none` is thrown directly. With tolerance on, the batch goes to the DLQ.
- `ClickHouseBase.tearDown()` (`src/test/java/.../sink/ClickHouseBase.java:132-153`): the shared-container drop is caught and logged at `:144-146`. D-14 changes it to rethrow.
- `HeaderToValue.applyWithSchema` keeps an existing field's schema (`buildSchema` copies old fields first) and `put`s `header.value().toString()`. `applySchemaless` overwrites with a String (D-15).

### Integration Points
- `ClickHouseWriter.doInsertBatch` (`:231-249`): the routing change. The condition becomes `csc.isBypassRowBinary() || shouldSkipTableMetadata()` for both `SCHEMA` and `DEBEZIUM_CDC`.
- `ClickHouseWriter.cleanupExtraFields` (`:1413-1415`): the P9-IN-01 swap, directly above upstream's #820 Map-coercion loop.
- `ClickHouseHelperClient.shouldSkipDescribeRow` (`:471`): DESC-05.
- `Column.extractColumn` Array recursion: COLS-07. `ClickHouseSinkConfig` USERNAME `define`: CFG-04.
- `ClickHouseWriter.validateDataSchema`: TYPE-08 gate for `INT256`/`UINT256`.

</code_context>

<specifics>
## Specific Ideas

- The parity matrix can insert the same records through both routes into twin tables (or one table with a route discriminator column) and compare `SELECT` output column by column. With D-01 as the standard, no expected value is hand-authored.
- Example message shape for D-12: `column 'tags' (Array(String)) element: null in non-nullable slot`.
- At least one Debezium-envelope row in the matrix (D-06), and a handful of nested logical-type cells (D-07).
- The `tearDown()` rethrow message carries the class simple name and the database name (D-14).

</specifics>

<deferred>
## Deferred Ideas

- **Fixing logical types on upstream's `bypassRowBinary=true` route.** It shares the Jackson-default limitation. Recorded in FORK-DELTA as a known difference, not fixed (D-05). It could go upstream as a PR later.
- **Unifying top-level null failures on `DataException`.** Upstream's `RuntimeException` stays (D-11). This is an upstream change if it is ever wanted.
- **Per-record isolation of nested-null failures,** so that only the bad record goes to the DLQ. It would change batch composition, which the exactly-once invariant forbids.
- **A HeaderToValue fix for a non-STRING target field,** for example skipping the header or coercing the value. Pinned as-is in this phase (D-15). A fix would be its own behaviour change.

</deferred>

---

*Phase: 13-fork-hardening-on-the-merged-tree*
*Context gathered: 2026-10-08*
