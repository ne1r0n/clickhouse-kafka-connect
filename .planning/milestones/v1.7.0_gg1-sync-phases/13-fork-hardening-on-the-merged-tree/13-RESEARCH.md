# Phase 13: Fork Hardening on the Merged Tree - Research

**Researched:** 2026-10-08
**Domain:** Kafka Connect sink to ClickHouse - JSON-route type encoding, RowBinary null correctness, DESCRIBE/config metadata fixes, test debt
**Confidence:** HIGH (every behavioural claim below was observed live this session against ClickHouse `26.9.12.8` x client V2 on branch `sync/v1.7.0`; probe files were deleted afterwards and `git status` is clean)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

### Deferred Ideas (OUT OF SCOPE)
- **Fixing logical types on upstream's `bypassRowBinary=true` route.** It shares the Jackson-default limitation. Recorded in FORK-DELTA as a known difference, not fixed (D-05). It could go upstream as a PR later.
- **Unifying top-level null failures on `DataException`.** Upstream's `RuntimeException` stays (D-11). This is an upstream change if it is ever wanted.
- **Per-record isolation of nested-null failures,** so that only the bad record goes to the DLQ. It would change batch composition, which the exactly-once invariant forbids.
- **A HeaderToValue fix for a non-STRING target field,** for example skipping the header or coercing the value. Pinned as-is in this phase (D-15). A fix would be its own behaviour change.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| JSONP-05 | `insertFormat=json` + `Struct` + default `bypassRowBinary` lands rows through the JSON writer; R-01 pin flips to typed read-back | Routing condition site and unit-test seam (Integration Points); R-01 flip mechanics; CDC shares the same `case` |
| JSONP-06 | Logical types land with correct values in `DateTime`/`DateTime64`/`Date`/`Date32`/`Decimal`/`String` | Live spike matrices: Jackson-today vs RowBinary diff, accepted encodings (decimal seconds, integer days, raw bytes via `writeUTF8String`), Decimal already at parity |
| JSONP-07 | Null semantics, `jsonInsertColumns` and `bypassFieldCleanup` interactions pinned | Live null-semantics table (omitted vs explicit null, Nullable vs DEFAULT) |
| JSONP-08 | `cleanupExtraFields` guard evaluates `shouldSkipTableMetadata()` first | Exact line, Mockito seam, honest framing (no null-safety delivered) |
| WRT-07 | `null` in `Map(K, Nullable(V))` round-trips as `NULL` | Already green on this tree for 13 value types (matrix below); pin only, no RED possible; Enum gap is pre-existing |
| WRT-08 | Nested non-nullable null throws `DataException` naming the column | Current-behaviour table, discriminator design, TUPLE/Nullable(Tuple) finding, exception-surfacing finding |
| DESC-05 | DESCRIBE rows at any depth below JSON/Dynamic are skipped | Live DESCRIBE shape for dotted JSON paths; ancestor-walk fix; non-widening cases |
| TYPE-08 | `Int256`/`UInt256` fail fast via `validateDataSchema`; `Int128`/`UInt128` still write | Gate location and its precondition (`!isNullable && !hasDefault`); existing Int128 round-trip pins |
| COLS-07 | `Column.extractColumn` Array recursion argument order | Swapped-argument line cited; observable effect and a Docker-free test shape |
| CFG-04 | `ConfigDef` reports `"default"` as USERNAME default | Line cited, constants verified, test shape |
| DEBT-01 | HeaderToValue non-STRING target pinned | Source read; exact exception chain |
| DEBT-02 | `JsonColumnTest` asserts read-back values | Observed read-back for all six configs |
| DEBT-03 | `ClickHouseSinkTaskJsonInsertFormatTest` try/finally | Review text and call sites |
| DEBT-04 | `tearDown()` drop failure attributed | Exact lines; subclasses that call `super.tearDown()` |
| DEBT-05 | Stale Javadocs corrected | Review findings text for each; D-17 wording drift |
</phase_requirements>

## Summary

The research flag's two spikes are done. **Spike (a), JSON-route type fidelity**: Jackson's defaults store wrong values for `Timestamp` and `Date`, and for `bytes`, and are already at RowBinary parity for `Decimal` and `Time`. A `Timestamp` becomes an unquoted epoch-millisecond integer, which ClickHouse reads as *seconds*: `DateTime` silently clamps to `4294967295`, `DateTime64(p)` either overflows or stores a year ~55 000 value. A `Date` becomes epoch millis, which wraps in `Date` and `Date32`. `bytes` becomes base64. One encoding per logical type reaches parity with no forced setting and without knowing the column type (the metadata-free route has no column metadata): **`Timestamp` -> unquoted decimal seconds with three fractional digits (`BigDecimal.valueOf(ms, 3)`), `Date` -> unquoted integer epoch days, `bytes` -> raw bytes written with Jackson's `JsonGenerator.writeUTF8String(byte[],int,int)`**. The raw-byte path survives all 256 single bytes, invalid UTF-8 sequences and a 300 000-byte payload, so D-03's "otherwise fail" branch is not needed. `Decimal` and `Time` need no conversion.

**Spike (b), WR-02**: the discriminator question dissolves if the null check lives in the *container's loop* (ARRAY, MAP, TUPLE branches of `doWriteColValue`) rather than in the recursed call, because the loop already knows it is nested. Top-level `doWriteCol` and the 4-arg `doWriteColValue` signature (which the existing CR-02 unit tests call directly) stay untouched. Two premises in CONTEXT need the user's attention before planning: **(1) `Nullable(Tuple(...))` is legal on `latest` 26.9.12.8** (D-10 assumed it might be dead code), and a single-field `Array(Nullable(Tuple(a Int32)))` with a null element round-trips as `NULL` today by coincidence, which a literal D-10 would turn into a `DataException`; **(2) WRT-07 is already green** on this tree, so there is nothing to make RED. A third correction: a nested `DataException` does not reach `put()` callers as a bare `DataException`; `Processing.doInsert` wraps every exception in `RuntimeException(queryId, e)`, so tests must walk the cause chain.

**Primary recommendation:** Plan the writer chain as three serial plans on `ClickHouseWriter.java` (routing + route-scoped schema-aware JSON writer; nested-null loop checks; operand swap + Int256 gate), run DESC-05/COLS-07/CFG-04 and the five DEBT items as parallel plans, and put the open questions below to the user before the writer plans are written.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Route Struct records to JSON writer in metadata-free mode | Connector writer (`ClickHouseWriter.doInsertBatch`) | - | Single routing site shared by `SCHEMA` and `DEBEZIUM_CDC` |
| Logical-type encoding (Timestamp/Date/bytes) | Connector writer (new route-scoped JSON writer) | ClickHouse JSONEachRow parser | Connector must pick a column-type-independent numeric form; server parses it |
| Null semantics for omitted/explicit nulls | ClickHouse server (`input_format_null_as_default`, DEFAULT) | Connector (NON_NULL omission at top level) | Connector only decides omit vs explicit `null` |
| Nested null detection in RowBinary | Connector writer (`doWriteColValue` container loops) | - | RowBinary has no recovery; the stream must be correct byte for byte |
| DESCRIBE structural-row skipping | Helper client (`ClickHouseHelperClient.shouldSkipDescribeRow`) | `Table` parent routing | Pure function over `columnsSoFar` |
| Int256/UInt256 fail-fast | Connector writer (`validateDataSchema`) | `doWriteColValue` default branch | Pre-stream gate, plus existing mid-stream backstop |
| Config default reporting | `ClickHouseSinkConfig` ConfigDef | - | Display metadata only; runtime default already `usernameDefault` |
| Test-debt fixes | Test sources only | - | No main-code change except where a requirement says so |

## Standard Stack

No new libraries. Everything needed is already on the classpath. [VERIFIED: `.claude/CLAUDE.md` stack section; probes compiled and ran against the current Gradle classpath]

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `jackson-core` / `jackson-databind` | 2.21.0 now, 2.22.3 after Phase 14 | JSON serialisation (`ObjectMapper`, `JsonGenerator.writeUTF8String`) | Already used by `DataJson.OBJECT_MAPPER`; Phase 14 BLD-11 bump must re-run the raw-bytes test |
| Kafka Connect API | 4.1.1 | `Schema`, `Struct`, logical types `Timestamp`/`Date`/`Time`/`Decimal` | Source of the logical-type LOGICAL_NAME constants |
| JUnit 5 + Mockito | 5.11.3 / 5.21.0 (5.24.0 after Phase 14) | Tests; Mockito for the `isBypassFieldCleanup()` never-called pin | Existing conventions |
| Testcontainers ClickHouse | 1.21.4 | Live read-back tests via `ClickHouseBase` | Existing conventions |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Route-scoped schema-aware writer | Pre-pass converting values into Jackson-natural objects | A pre-pass cannot keep nested `Struct` null semantics (explicit `null`) because converted nested structs must become `Map`s, which `NON_NULL` strips. Recommend the writer approach |
| `writeUTF8String` for bytes | Hand-written escape-then-write | Both verified to store identical bytes; Jackson's is shorter and well-tested. Hand-written escaper (`"`, `\`, and `< 0x20` as `\u00xx`) is the fallback |

**Installation:** none. **Package Legitimacy Audit:** not applicable - this phase installs no external packages. [VERIFIED: no `build.gradle.kts`/catalog edit is required by any requirement]

## Architecture Patterns

### System Architecture Diagram

```
SinkRecord(Struct, schema)
   |
   v
Record.convert --> SCHEMA (StructToJsonMap) | DEBEZIUM_CDC (DebeziumRecordConvertor, op=c/r/u/d)
   |
   v
Processing.doLogic --> dbWriter.doInsert --> ClickHouseWriter.doInsert
   |   getTable(): shouldSkipTableMetadata()? -> synthetic Table(db, name, false, List.of(), 0)
   v
doInsertBatch  (ClickHouseWriter.java:232)
   | case SCHEMA / DEBEZIUM_CDC:
   |    if (csc.isBypassRowBinary() || shouldSkipTableMetadata())   <-- Phase 13 change (was: isBypassRowBinary only)
   |        --> doInsertJson --> V2: client.insert(table[, jsonInsertColumns], writer, JSONEachRow)
   |                              writeRecordsJson (line 1279)
   |                                 data = per-field map  --> [NEW, metadata-free only] schema-aware writer:
   |                                                             Timestamp -> decimal seconds, Date -> int days,
   |                                                             bytes -> raw via writeUTF8String, else Jackson default
   |                                 cleanupExtraFields (returns m untouched in metadata-free mode)
   |                                 OBJECT_MAPPER.writeValueAsBytes (unchanged for every other route, D-05)
   |    else --> doInsertRawBinary --> writeRecordsRowBinary --> doWriteCol --> doWriteColValue
   |                                     ARRAY / MAP / TUPLE loops <-- Phase 13 nested-null checks live here
   v
ClickHouse (JSONEachRow parser: unquoted number = seconds for DateTime/DateTime64, days for Date/Date32)
```

### Recommended Project Structure
```
src/main/java/com/clickhouse/kafka/connect/
├── sink/db/ClickHouseWriter.java        # routing, nested-null checks, operand swap, Int256 gate (serial)
├── sink/db/MetadataFreeJsonWriter.java  # NEW (name is the planner's): route-scoped schema-aware JSON writer
├── sink/data/Data.java                  # optional additive getter for the field Schema (see Open Questions 4)
├── sink/db/helper/ClickHouseHelperClient.java  # DESC-05 ancestor walk
├── sink/db/mapping/Column.java          # COLS-07 argument order
└── sink/ClickHouseSinkConfig.java       # CFG-04
src/test/java/...                        # new Docker-free classes next to existing ones; extend, never weaken
```

### Pattern 1: Container-loop null checks (WR-02 discriminator)
**What:** The check happens where the container iterates its children, so "nested" is known by construction. `doWriteCol` (top level) and the 4-arg `doWriteColValue` are unchanged.
**When to use:** ARRAY loop (`ClickHouseWriter.java:648`), MAP `forEach` (`:613`), TUPLE field loop (`:670`).
**Example (design sketch - [ASSUMED], not compiled):**
```java
// ARRAY loop: v is the element, elem = col.getArrayType()
if (v == null && !elem.isNullable()) {
    switch (elem.getType()) {
        case ARRAY: case MAP: BinaryStreamUtils.writeVarInt(stream, 0); return;   // D-09: always coerce to empty
        case VARIANT: break;                                                      // existing 255 discriminator path
        default: throw new DataException(nestedNullMessage(col, "element"));      // D-12: no data values
    }
}
```
A nested null container currently relies on `defaultsSupport` (nested ARRAY null writes `0x00` only when `defaultsSupport` is true; nested MAP null writes `varint 0` only when true), which is why the no-defaults table desyncs (table below).

### Pattern 2: Route-scoped schema-aware JSON writer
**What:** A new class holding a copy of the mapper (`DataJson.OBJECT_MAPPER.copy()`, so the shared instance is never mutated) plus a writer that walks `Schema` and `value` together and dispatches on `schema.name()`. Only `writeRecordsJson` calls it, and only when `shouldSkipTableMetadata()`.
**Why not a global serializer:** `java.util.Date` is the runtime class of `Timestamp`, `Date` and `Time` alike; only the schema distinguishes them.
**Keep:** top-level null fields omitted (today's `HashMap` + `NON_NULL` behaviour), nested `Struct` null fields written as explicit `null` (today's `JacksonStructSerializer`), so JSONP-07 pins today's semantics.
**Must stay byte-based:** `writeUTF8String` is only exact on a UTF-8 byte generator. `writeValueAsBytes` creates one; do not route this through a `Writer`/`String`.

### Anti-Patterns to Avoid
- **Quoted or ISO timestamps:** a quoted decimal (`"1704067200.123"`) fails for `DateTime` and for DateTime64 values under 6 integer digits; `"2024-01-01 00:00:00.123"` is time-zone dependent (wrong by 7 h on `DateTime64(3,'America/New_York')`, 9 h on `DateTime('Asia/Tokyo')`).
- **Changing `DataJson.OBJECT_MAPPER`:** shared with the `bypassRowBinary=true`, schemaless and `ConvertToMapOrJson` paths (D-05).
- **Keying anything on `csc.getInsertFormat()`:** the unset-`insertFormat` typo makes it match every default connector (Javadoc at `ClickHouseWriter.java:1624-1631`).
- **Adding a `nested` parameter to `doWriteCol`/`doWriteColValue`:** breaks the direct calls in `NestedNullableCompositeWriteTest` and every future upstream hunk; not needed with Pattern 1.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Raw-byte JSON string escaping | A custom escaper | `JsonGenerator.writeUTF8String(byte[], 0, len)` | Verified: escapes `"`, `\`, control bytes; passes bytes >= 0x80 through; 264 single/multi-byte samples + 300 000 bytes all stored identically |
| Decimal text for timestamps | String formatting of seconds | `BigDecimal.valueOf(date.getTime(), 3)` -> `gen.writeNumber(BigDecimal)` | Exact, scale fixed at 3, never scientific (adjusted exponent >= -3), handles negatives |
| Days since epoch | `ms / 86_400_000L` | `Math.floorDiv(ms, 86_400_000L)` for `java.util.Date`; `((java.sql.Date) d).toLocalDate().toEpochDay()` for `java.sql.Date` | Floor semantics; `java.sql.Date` is local-midnight and TZ dependent |
| Parity comparison | Hand-authored expected values | Same `Struct` through two sink tasks into twin tables, then identical `SELECT` per column | D-01; both routes already run in one JVM |
| DataException type assertion through `put()` | `assertThrows(DataException.class, ...)` on `put()` | Walk `getCause()` chain, assert `instanceof DataException` plus message substrings | `Processing.doInsert` wraps (below) |

**Key insight:** every conversion above is a pure function of the Kafka value and its schema, so retries produce byte-identical payloads, which the exactly-once invariant requires.

## Spike Results (live, ClickHouse 26.9.12.8, client V2, UTC server)

### Spike (a1): what Jackson stores today vs RowBinary (same Struct, same column)

Route "today" = `insertFormat=json` + `bypassRowBinary=true` (the only JSON route reachable for a Struct before this phase). [VERIFIED: live probe, `ClickHouseSinkTask.put` then typed `SELECT`]

| Cell | RowBinary stores | Jackson default stores | Verdict |
|------|------------------|------------------------|---------|
| Timestamp -> `DateTime` | `1704067200` | `4294967295` (silent clamp) | WRONG |
| Timestamp -> `DateTime64(0/3/6)` | correct | server-side read overflow (`DECIMAL_OVERFLOW` on `toUnixTimestamp64Nano`; value ~year 55 000) | WRONG |
| Timestamp -> `DateTime64(9)` | correct | insert fails `Numeric value is out of range for DateTime64` | WRONG (loud) |
| Date -> `Date` | `19723` | `62464` (millis wrapped) | WRONG |
| Date -> `Date32` | `19723` | `-1034816512` | WRONG |
| Time -> `Int32` / `Int64` | `45296789` | `45296789` | parity |
| Decimal (6 shapes incl. scale 0, scale 7 `1E-7`, 38,10 big, negative, zero, value scale > column scale) | identical | identical | parity, no conversion needed |
| bytes -> `String` | raw (`616263`) | base64 text (`59574A6A` = "YWJj") | WRONG |
| `ByteBuffer` bytes | `616263` (StructToJsonMap calls `struct.getBytes`) | base64 text; the JSON route sees the raw `ByteBuffer` from `struct.get(field)` | WRONG; serializer must handle `ByteBuffer` too |
| `Array(DateTime64(3))` of Timestamps | correct | wrong (millis read as seconds) | WRONG |
| `Map(String, Date)` | correct | wrong (`2141-01-08`) | WRONG |
| Nested Struct -> Tuple with Timestamp + Decimal | correct | Timestamp clamped to `9999-12-31 23:59:59`, Decimal correct | WRONG (Timestamp only) |

Also: `DateTime('Asia/Tokyo')` is **not RowBinary-supported** (`Column [c] has unsupported type [UNKNOWN] ...`), so it is outside D-04's matrix even though the JSON encoding below handles it. [VERIFIED: live probe]

### Spike (a2): candidate encodings, direct JSONEachRow inserts (5 timestamps x 8 column types x 8 candidates)

Timestamps tested: `1704067200123`, `645453296789` (9-digit seconds), `1704067200000`, `86400123` (5-digit seconds), `4102444800123` (year 2100). Columns: `DateTime`, `DateTime('Asia/Tokyo')`, `DateTime64(0,1,3,6,9)`, `DateTime64(3,'America/New_York')`. Expected = RowBinary's value (`getEpochSecond` floor for `DateTime`; `ms`/`BASES[p]` truncation for `DateTime64(p)`). [VERIFIED: live probe]

| Candidate | Parity cells | Failure mode |
|-----------|--------------|--------------|
| **Unquoted decimal seconds `1704067200.123`** (also scientific `1.704067200123E+9`) | **40/40, all columns and precisions** | none |
| Unquoted epoch millis (Jackson today) | 0/40 | clamp / overflow |
| Quoted decimal seconds | `DateTime`: 0/5 (`expected '"' before: '.123'`); DateTime64: fails when whole part < 6 digits | parser |
| Unquoted integer seconds | `DateTime`, `DateTime64(0)` only; drops fraction for p >= 1 | lossy |
| Quoted ISO `2024-01-01T00:00:00.123Z` | 40/40 on 26.9 | would need `best_effort` on older servers [ASSUMED for older versions] |
| Quoted local `2024-01-01 00:00:00.123` | fails with tz'd columns | time-zone dependent (D-02 rejects) |

Negative epochs via decimal seconds also match RowBinary on `DateTime64(0/3/9)` (`-1.500` -> `-1 s` on p=0, `-1500 ms` on p=3 and 9, `-0.001` -> `0` on p=0). `DateTime` is unsigned; negatives clamp in both routes and are out of scope. [VERIFIED: live probe]

**Date candidates** (`Date`, `Date32`): unquoted integer days = parity (`19723`, `7470`, `0`, `40000`). Negative days: `Date32` fine, `Date` errors loudly (RowBinary wraps via `writeUnsignedInt16`, so neither is meaningful). Quoted `"2024-01-01"` also works but silently clamps negatives to `0` in `Date`. **Recommend unquoted integer days: it fails loudly out of range.** Epoch millis wraps silently. [VERIFIED: live probe]

**Gap to record (not fixable without column metadata):** a `Date` logical value written to a **`DateTime`/`DateTime64`** column is supported by RowBinary (midnight UTC seconds) but cannot reach parity on this route, because the same literal must also serve `Date`. Integer days land as *seconds* in a `DateTime` column (silent 1970 timestamps). D-04 lists only `Date`->`Date`/`Date32`, so this is outside the matrix; FORK-DELTA should state it.

**bytes (D-03 resolved):** raw bytes with only `"`, `\\` and `< 0x20` (as `\u00xx`) escaped store verbatim: all 256 single bytes, `C3 28`, `ED A0 80` (surrogate), `C0 80` (overlong), a 4-byte emoji, an empty array, embedded NUL, a literal backslash-u text, `FF FE " \ /`. `JsonGenerator.writeUTF8String(bytes, 0, len)` on the byte generator produced the same stored bytes for all 256 single bytes (each followed by `0x41`), for those multi-byte sequences, for a 300 000-byte random array and a 100 000-byte array of `"`/`0x1F`/`0xC3`. `writeRawUTF8String` is **wrong** (does not escape: `"` breaks the document, `\` is eaten). `writeBinary` is base64. So the "throw a `DataException`" branch of D-03 is not needed. [VERIFIED: live probe]

### Spike (a3): null semantics on the JSON route (JSONP-07)

Table: `n Nullable(Int32)`, `nd Nullable(Int32) DEFAULT 5`, `nn Int32`, `ndf Int32 DEFAULT 7`, `s String`, `a Array(Int32)`, `m Map(String,String)`, `dt Nullable(DateTime64(3))`. [VERIFIED: live probe]

| Input | `n` | `nd` | `nn` | `ndf` | `s` | `a` | `m` | `dt` |
|-------|-----|------|------|-------|-----|-----|-----|------|
| field omitted | `NULL` | `5` | `0` | `7` | `''` | `[]` | `{}` | `NULL` |
| explicit `null` | `NULL` | **`NULL`** | `0` | `7` | `''` | `[]` | `{}` | `NULL` |
| `a:[1,null]` | | | | | | `[1,0]` | | |
| `m:{"k":null}` | | | | | | | `{'k':''}` | |

So "Nullable -> `NULL`, non-nullable -> column `DEFAULT`" holds for both omission and explicit null. The single difference is `Nullable` + explicit non-NULL `DEFAULT` (`nd`): omitted gives the default, explicit `null` gives `NULL`. Nulls *inside* arrays/maps are silently coerced to the element default on this route (RowBinary would fail), which belongs in FORK-DELTA.

What the existing writer emits today: top-level `HashMap` + `NON_NULL` **omits** null fields; a nested `Struct` writes **explicit** `null`; `Map` entries with null values are **dropped**; `List` elements keep `null`. [VERIFIED: live probe of `DataJson.OBJECT_MAPPER`]

### Spike (a4): other Jackson facts the writer must respect
- `java.sql.Date` (what `DebeziumRecordConvertor` emits for `io.debezium.time.Date`) serialises as the **local-midnight** epoch millis (`1704060000000` on this Riga-timezone JVM), so any conversion that divides by 86 400 000 is off by one day east of UTC. Use `toLocalDate().toEpochDay()`. [VERIFIED: live probe]
- RowBinary's `doWriteDates` has the same off-by-one for those values in non-UTC JVMs (existing upstream behaviour). A parity test including a Debezium `Date` would therefore fail on this developer machine and pass on UTC CI; keep it out of the parity matrix or assert a literal. [ASSUMED: derived by reading `TimeUnit.MILLISECONDS.toDays(date.getTime())` at `ClickHouseWriter.java` `case Date:`; not run]
- `BigDecimal.valueOf(1704067200123L, 3)` -> `1704067200.123`; `valueOf(-1500, 3)` -> `-1.500`; `valueOf(1, 3)` -> `0.001`. `BigDecimal("1E+3")` serialises as `1E+3` and ClickHouse parses it (`Decimal(18,0)` -> `1000`). [VERIFIED: live probe]

### Spike (b): current nested-null behaviour through RowBinary (single-row batches)

`NO_DEFAULTS` = table without DEFAULT columns (`RowBinary`); `DEFAULTS` = with a `d Int32 DEFAULT 1` column (`RowBinaryWithDefaults`). [VERIFIED: live probe]

| Slot | NO_DEFAULTS | DEFAULTS |
|------|-------------|----------|
| `Map(String, Nullable(Int32))` null value | `{'a':1,'b':NULL}` OK | OK |
| `Array(Nullable(String))` null element | OK | OK |
| `Array(String)` null element | server error `CANNOT_READ_ALL_DATA` | same |
| `Map(String, String)` null value | server error | same |
| `Array(Array(Int32))` null inner | `ATTEMPT_TO_READ_AFTER_EOF` | `[[1],[]]` (accidental) |
| `Map(String, Array(Int32))` null value | `ATTEMPT_TO_READ_AFTER_EOF` | `{'k':[]}` (accidental) |
| `Array(Map(String,Int32))` null element | `ATTEMPT_TO_READ_AFTER_EOF` | `[{'a':1},{}]` (accidental) |
| `Tuple(a Int32, b String)` `b` null | `RuntimeException: An attempt to write null into not nullable column 'c.b'` | same |
| `Tuple(a Nullable(Int32), b Nullable(String))` null field | `(1,NULL)` OK | OK |
| `Array(Tuple(a Int32))` null element | `CANNOT_READ_ALL_DATA` | same |

Reading: server errors above are the *lucky* outcomes. A scalar `writeNull` byte (`0x01`) written into a non-nullable `Int32` slot is one byte instead of four, so with a following row it can be consumed as data and store garbage rather than fail. The batch is still all-or-nothing per insert. The tuple-field failure is a bare `RuntimeException` from `doWriteCol` (thrown at `ClickHouseWriter.java:982`/`:997`), not a `DataException`, and it names `c.b` (the sub-column path), so WRT-08 needs a new check in the TUPLE loop to produce the required `DataException`.

### Spike (b2): Map(K, Nullable(V)) matrix for WRT-07 (13 value types, MAP and ARRAY)

A `null` entry reads back as `NULL` for `Int32`, `String`, `Date`, `Date32`, `DateTime`, `DateTime64(3)`, `Decimal(18,4)`, `UUID`, `FixedString(3)`, `Bool`, `Float64`, `LowCardinality(Nullable(String))`, in both `Map(String, Nullable(V))` and `Array(Nullable(V))`. [VERIFIED: live probe] **WRT-07 is therefore already satisfied on this tree**; the deliverable is a pin test (the earlier WR-01 defect predates the Phase 12 merge). The one failing value type is `Nullable(Enum8)`, a **pre-existing, out-of-scope** defect: `Column.extractColumn`'s Enum branch drops `isNull`, so `{'a':NULL,'b':NULL}` comes back for `{a:'a', b:null}`, and a top-level `Nullable(Enum8)` stores `NULL` for a real value. Keep Enum out of the WRT-07 matrix and record it as a finding (see Open Questions 6).

### Spike (b3): TUPLE nullability (the D-10 premise)

- `CREATE TABLE ... (c Nullable(Tuple(a Int32)))` **succeeds** on 26.9.12.8; so do `Array(Nullable(Tuple(...)))` and `Map(String, Nullable(Tuple(...)))`. `Nullable(Array)`, `Nullable(Map)` and `Nullable(Variant)` are still illegal (`Nested type ... cannot be inside Nullable type`). [VERIFIED: live probe]
- `Column.extractColumn`'s TUPLE branch (`Column.java:232-236`) does not apply `isNull`, so a DESCRIBE-derived `Nullable(Tuple)` column has `isNullable() == false`. Same for the Enum, Array, Map and Variant branches. [VERIFIED: read of `Column.java:202-236`]
- Today a single-field `Array(Nullable(Tuple(a Int32)))` with elements `[(5), null]` round-trips as `[(5),NULL]` because the CR-02 branch writes `0x01` unconditionally, and the non-null element works only because the tuple's one `Nullable(Int32)` sub-field marker doubles as the tuple null flag. With two fields, `Nullable(Tuple(a Int32, b String))` fails (`ATTEMPT_TO_READ_AFTER_EOF`), so multi-field Nullable tuples are already broken. A top-level null in a `Nullable(Tuple)` column throws the bare `RuntimeException` since `isNullable()` is false. [VERIFIED: live probe]
- Consequence: D-10 as written (`col.isNullable()` -> marker, else `DataException`) would turn the single-field `Array(Nullable(Tuple))` null element from `NULL` into a `DataException`. Open Question 1.
- None of the existing CR-02 TUPLE pins builds a non-nullable Tuple (`NestedNullableCompositeWriteTest` builds `isNullable(true)`), so D-10 has **no existing pin to re-pin**; it needs new pins.

### DESCRIBE shape for DESC-05 (live, `describe_include_subcolumns=1`)

For `j JSON(a Int32, \`b.c\` String, d Tuple(x Int32, y String), e Array(Int32))` the rows are `j.a`, `j.b.c`, `j.b.c.size`, `j.d`, `j.d.x`, `j.d.y`, `j.d.y.size`, `j.e`, `j.e.size0`. [VERIFIED: live probe]

- `j.d.x`, `j.d.y`: already skipped inside `describeTableV1/V2` because `j.d` was itself skipped and the loop's `skippedCols.contains(parentName)` chain catches it.
- **`j.b.c` is the real hole**: no `j.b` row exists, so neither `columnsSoFar` nor `skippedCols` knows the immediate parent, and the row becomes a bogus `Column`. `shouldSkipDescribeRow` itself (the unit under test) returns `false` for every depth >= 2 row, including `j.d.x`.
- Must not widen: `nt.a.b` (Tuple inside Tuple) must stay un-skipped; `aj.k` (Array(JSON)) stays un-skipped; `tj.jj.k` (JSON as a Tuple field) is skipped because `tj.jj` is JSON.

### JsonColumnTest read-back (DEBT-02), observed for all six transform configs
The sample is `new Random(42).nextBytes` of 128 bytes in the test; the probe used 8. Observed on `json_data JSON`: `nested_str` = `"test"+i`, `nested_int` = `i` (`Int64`) in all six. `nested_bytes` = **base64 text** (String) for every Jackson config and the no-JSON `Map` configs, and **an `Array(Int64)` of signed byte values** (`[53,-99,65,...]`) for both Gson configs, because `bytes.to.base64` only affects `Struct` input, which these tests do not send. [VERIFIED: live probe]

## Common Pitfalls

### Pitfall 1: A "nested DataException" test asserts the wrong type through `put()`
**What goes wrong:** `assertThrows(DataException.class, () -> sinkTask.put(...))` fails.
**Why:** `Processing.doInsert` wraps everything in `new RuntimeException(queryId.toString(), e)` (`Processing.java:70-72` and `:94-96`), so `Utils.handleException`'s `e instanceof DataException` branch is never taken from this path. CONTEXT's note "a DataException with errors.tolerance=none is thrown directly" does not hold through `put()`.
**How to avoid:** Unit-level: call `doWriteColValue` directly and `assertThrows(DataException.class, ...)`. Live-level: `assertThrows(RuntimeException.class, ...)`, walk `getCause()` for an `instanceof DataException`, assert message substrings (column name, slot kind) and that no value appears, and assert zero rows by typed `SELECT` (the pattern in `RowBinaryFailFastLiveTest`).

### Pitfall 2: The Phase 13 JSON writer must be byte-based
`writeUTF8String` is exact only on a UTF-8 byte generator. A mapper writing to a `String`/`Writer` decodes the bytes and corrupts anything not valid UTF-8. Keep `writeValueAsBytes`, and re-run the raw-bytes test after the Jackson bump (BLD-11, Phase 14).

### Pitfall 3: Parity test that depends on the JVM time zone
Debezium `io.debezium.time.Date` becomes a local-midnight `java.sql.Date` (see Spike a4). Keep CDC dates out of the RowBinary-vs-JSON equality, or compare against a literal and run under an explicit `-Duser.timezone`. Kafka `Date`/`Timestamp` logical values are time-zone free.

### Pitfall 4: CDC records that are skipped produce an empty-object row
`DebeziumRecordConvertor` returns an empty `jsonMap` for `op=t` and for a null `before`/`after` struct. On the JSON route that becomes `{}`, a row of column defaults. Decide explicitly (skip such a record in the CDC branch, or pin the behaviour); RowBinary either throws `RuntimeException` for a non-nullable no-default column or writes defaults.

### Pitfall 5: COLS-07 changes `hasDefault` for Array element columns that `doWriteCol` reads
The Array recursion passes `(hasDefaultValue, isSubColumn)` swapped (`Column.java:213`: `extractColumn(name, ..., false, isSubColumn, hasDefaultValue, arrayDepth + 1)` against the signature `(name, valueType, isNull, hasDefaultValue, isSubColumn, arrayDepth)`). Tuple fields of `Array(Tuple(...))` are routed through `doWriteCol`, whose `!fieldExists` branch checks `col.hasDefault()`. After the fix a missing tuple field with no default takes the "not nullable and no value" `RuntimeException` instead of writing `0x01`. Run the full suite, not only the new unit test.

### Pitfall 6: `validateDataSchema` only inspects columns that are `!isNullable && !hasDefault`
The type `switch` is nested inside `if (!isNullable && !hasDefault && type != Type.VARIANT)` (`ClickHouseWriter.java:~292`). A new `INT256`/`UINT256` case fires only for those columns; nullable or defaulted ones, and every run with `bypassSchemaValidation=true`, still reach the `doWriteColValue` `default:` backstop, which already throws the same `DataException` mid-stream. The Int256 test must use a non-nullable, no-default column to reach the new gate. Today the failure is the generic `RuntimeException("Data schema validation failed.")`.

### Pitfall 7: Kafka `STRING` fields cannot be written to `Int128`/`UInt128` through validation
`validateDataSchema` accepts `Int128` only for numeric Kafka types (`INT8..INT64`); the existing round-trip pin (`ClickHouseSinkTaskWithSchemaTest.testInt32ToInt64ConversionWithConnectSchema`, targets `Int64` and `Int128`) uses `INT64` fields. Add `UInt128` using the same shape; do not use a `STRING` field.

### Pitfall 8: `Nullable(Enum8/16)` is silently broken (pre-existing)
Do not include Enum in the WRT-07 matrix, and do not let a "fix" for it ride in this phase (outside declared scope, the fork runs in production).

### Pitfall 9: Mid-stream failure leaves a deterministic poison batch
A nested-null `DataException` fails the same batch on every retry (`errors.tolerance=none` fails the task; `all` DLQs the whole batch). This is by design (D-12 note); FORK-DELTA/rollout wording should say operators will see the failure on the *first* offending batch, not a hang.

## Code Examples

Verified patterns (observed to produce correct stored values in the live probes):

```java
// Timestamp (INT64, java.util.Date) -> unquoted decimal seconds; valid for DateTime and every DateTime64(p).
gen.writeNumber(BigDecimal.valueOf(((java.util.Date) v).getTime(), 3));        // 1704067200.123

// Date (INT32, java.util.Date at UTC midnight) -> unquoted integer days.
gen.writeNumber((int) Math.floorDiv(((java.util.Date) v).getTime(), 86_400_000L));   // 19723

// bytes: byte[] or ByteBuffer, honour position/limit, never mutate the buffer.
gen.writeUTF8String(bytes, offset, length);   // on a UTF-8 byte generator only

// Time (INT32 java.util.Date at epoch day 0): Jackson default already equals RowBinary; explicit form:
gen.writeNumber((int) ((java.util.Date) v).getTime());                         // 45296789
```

DESC-05 shape (sketch, [ASSUMED] until compiled): walk every ancestor prefix of `name`, return `true` if any prefix is in `columnsSoFar` as `Type.JSON` or `Type.DYNAMIC`; keep the existing early `return false` for `!isSubcolumn()` and the existing `.null`/`.size0`/`.size` suffix rule.

Routing (the whole change at `ClickHouseWriter.java:238`):
```java
if (csc.isBypassRowBinary() || shouldSkipTableMetadata()) {
```

Operand swap at `ClickHouseWriter.java:1414`: `if (shouldSkipTableMetadata() || csc.isBypassFieldCleanup()) {`

### Verbatim anchors (read this session)
- `ClickHouseWriter.java:238`: `if (csc.isBypassRowBinary()) {`
- `ClickHouseWriter.java:1414`: `if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) {`
- `ClickHouseWriter.java:1632-1633`: `private boolean shouldSkipTableMetadata() {` / `return csc != null && csc.isJsonInsertFormatExplicit();`
- `ClickHouseWriter.java:1291`: `data.put(field.name(), struct.get(field));//Doesn't handle multi-level object depth`
- `ClickHouseWriter.java:982`, `:997`: `throw new RuntimeException(String.format("An attempt to write null into not nullable column '%s'", name));`
- `Column.java:213`: `Column arrayType = extractColumn(name, valueType.substring("Array".length() + 1, valueType.length() - 1), false, isSubColumn, hasDefaultValue, arrayDepth + 1);`
- `Column.java:193`: `public static Column extractColumn(String name, String valueType, boolean isNull, boolean hasDefaultValue, boolean isSubColumn, int arrayDepth) {`
- `ClickHouseSinkConfig.java:440-443`: `configDef.define(USERNAME,` / `ConfigDef.Type.STRING,` / `passwordDefault,`; constants `:83-84`: `public static final String usernameDefault = "default";` / `public static final String passwordDefault = "";`
- `ClickHouseHelperClient.java:471,483`: `static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar) {` / `String parentName = name.substring(0, lastDot);`
- `ClickHouseBase.java:144-146`: `} catch (Exception e) {` / `LOGGER.error("Error dropping database", e);`
- `Processing.java:72`: `throw new RuntimeException(queryId.toString(), e);//This way the queryId will propagate`

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ISO-8601 needs `date_time_input_format=best_effort` | Basic parser accepts `YYYY-MM-DDTHH:MM:SS.sssZ` | observed on 26.9.12.8 | Not relied on: D-02 and older LTS servers make numeric the safe choice [ASSUMED for older versions] |
| `Nullable(Tuple)` illegal | Legal | observed on 26.9.12.8 | D-10 premise (Open Question 1) |
| Connector forced `lz4` | `setCompressionMethod` + `compress=false` | Phase 12 | No impact on this phase |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | ISO-8601 quoted timestamps may need `best_effort` on older ClickHouse versions | Spike a2 / State of the Art | None for the plan: the recommended numeric form is verified on `latest`; project test target is `latest` only |
| A2 | RowBinary's `Date` handling is off by one day for Debezium `java.sql.Date` in non-UTC JVMs | Spike a4 | A parity test could pass on CI and fail locally; mitigated by keeping CDC dates out of the equality |
| A3 | Container-loop checks (Pattern 1) are sufficient and need no signature change | Pattern 1 | Planner may find a call path that reaches a container column without a loop; mitigated by also making the TUPLE early-null branch throw per D-10 |
| A4 | `writeUTF8String` behaviour is unchanged by the Jackson 2.22.3 bump | Pitfall 2 | Raw-bytes test would go red in Phase 14; that is its purpose |
| A5 | The DESC-05 ancestor walk and the DESCRIBE row shapes hold for ClickHouse versions other than 26.9.12.8 | DESCRIBE shape | Project test target is `latest` only |

## Open Questions

1. **D-10's premise is false: `Nullable(Tuple)` is legal on `latest` 26.9.12.8.**
   - What we know: the nullable half of the TUPLE branch is *not* dead code. `Column.extractColumn`'s TUPLE branch drops `isNull`, so a DESCRIBE-derived `Nullable(Tuple)` has `isNullable() == false`. A single-field `Array(Nullable(Tuple(a Int32)))` null element works today (`[(5),NULL]`) only through the unconditional `writeNull`, and multi-field Nullable tuples are already broken.
   - What's unclear: whether the user accepts that D-10 turns that single-field case into a `DataException`.
   - Recommendation: follow D-10 literally (no change to `extractColumn` nullability, which is outside scope and risks the top-level double-marker), add a live pin documenting the single-field behaviour change, and record it in the behaviour ledger / FORK-DELTA. Needs one explicit user ack because it narrows a case that works today.
2. **WRT-07 has no RED.** The behaviour is already correct. Recommendation: land it as a pin (13-type matrix), say so in the plan, and optionally demonstrate the pin's teeth by a throwaway local mutation (not committed), per the project's "tests seen to fail" habit (EOS-01 precedent).
3. **Date -> DateTime/DateTime64 on the JSON route cannot reach parity** (needs column metadata). Recommendation: integer days, pin `Date`->`Date`/`Date32`, state the gap in FORK-DELTA.
4. **CDC-specific logical types and how the writer finds the schema.** `Data` has no schema getter (only `getFieldType()` and nested-schema getters), so the writer cannot see `schema.name()` for CDC values. Options: add a tiny additive `Data.getSchema()`, or look the schema up via `record.getFields()` (the CDC fields list carries the original field schemas plus two synthetic ones). Scope question: convert only what `DebeziumRecordConvertor` already emits (`java.util.Date` for Timestamp, `java.sql.Date` via `toLocalDate()`, `MicroTimestamp` as `BigDecimal.valueOf(micros, 6)` seconds) and pass everything else (`io.debezium.time.Timestamp` ms, `ZonedTimestamp` strings) through, documenting that raw `io.debezium.time.Timestamp` longs will be read as seconds. Recommendation: that scope; confirm with the user.
5. **D-12's "thrown directly" note** (Pitfall 1) needs a corrected statement in the plan and in FORK-DELTA: the `DataException` is the root cause of a `RuntimeException`.
6. **Pre-existing `Nullable(Enum8/16)` corruption** (`isNull` dropped in the Enum branch) should be logged to the backlog, not fixed here.
7. **D-12's example message prints the declared type (`Array(String)`)**, but `Column` keeps no original type string. The planner needs either a small renderer over the `Column` tree or a message built from `Type` names; tests should assert substrings (column name, slot kind, absence of values), consistent with D-17's substring rule.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker engine (colima `vz` profile) | all live tests | yes | `colima list`: `vz` Running; socket `~/.colima/vz/docker.sock` | none needed |
| `docker` CLI | not used by Testcontainers | no | - | Testcontainers talks to the socket; not required |
| JDK | Gradle toolchain 17 | yes | system `openjdk 21.0.12.1`, toolchain resolved by Gradle | - |
| ClickHouse `latest` image | live tests | yes | `26.9.12.8` (from server error strings) | - |
| Gradle | build | yes | wrapper (`--offline` works) | - |

Run commands that worked this session: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "*ClassName" --offline` (single live class 15-30 s). `testUnit` is Docker-free. The shell has `TESTCONTAINERS_HOST_OVERRIDE=null`, so the `env -u` prefix is mandatory. The default `colima` profile is stopped; leave it, the `vz` profile is the one in use.

## Validation Architecture

Omitted: `workflow.nyquist_validation` is `false` in `.planning/config.json`. Test guidance that the planner needs is folded into the sections above (test shapes, Docker-free vs live, cause-chain assertions, pin-only items).

## Security Domain

`security_enforcement` is enabled (`.planning/config.json`, `security_asvs_level: 1`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | - |
| V3 Session Management | no | - |
| V4 Access Control | no | - |
| V5 Input Validation | yes | Raw-byte JSON string must escape `"`, `\`, control bytes (use `writeUTF8String`); never construct JSON by string concatenation of record data |
| V6 Cryptography | no | - |
| V7 Error Handling and Logging | yes | `DataException` messages and logs carry column name, type and slot kind only; no record values, no map keys (D-12, AGENTS.md) |
| V12 Files and Resources | partial | Avoid copying large `ByteBuffer`s needlessly (array-backed fast path); bounded by the Kafka record size |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| JSON injection through a `bytes` field containing `"` or `\` | Tampering | `writeUTF8String` escapes both (verified for all 256 bytes); `writeRawUTF8String` does **not** and must not be used |
| Record data leaking into exception text, logs or DLQ headers | Information disclosure | Fixed-format messages with identifiers only; assert `assertFalse(message.contains(secret))` as `RowBinaryUnsupportedTypeGateTest` already does |
| SQL interpolation of names in new tests | Tampering | Use test-generated table names via `createTopicName`; no user input reaches DDL |
| Poison batch retried forever | Denial of service | Existing behaviour: `errors.tolerance=none` fails the task, `all` routes the batch to the DLQ; no new retry loop is added |

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` (-> `AGENTS.md`) and `./.claude/CLAUDE.md`; the planner must verify compliance:
- Java 17, Gradle Kotlin DSL, Lombok where it fits, SLF4J (TRACE per-record, DEBUG batch, INFO lifecycle), JUnit 5 + Mockito.
- High-risk files need extra care and thorough tests: `Processing.java`, `KeeperStateProvider.java`, `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `RangeContainer.java`. This phase edits two of them (`ClickHouseWriter`, `ClickHouseSinkConfig`).
- Exactly-once invariant: never change batch composition after `BEFORE` is set; retried batches must be byte-identical. New JSON encoding must be a deterministic pure function of value + schema.
- Never log secrets or record data in plaintext.
- Hot paths (`ClickHouseWriter`, `ProxySinkTask`, `Processing`): avoid per-record allocations, copies and autoboxing; keep streaming. The current `writeRecordsJson` allocates a `HashMap` per record; the new writer should not add more (write straight to the generator).
- Never delete or weaken existing tests; add tests; descriptive test names; read `ClickHouseTestHelpers.java` first; negative tests required.
- Typed SQL read-back, never `countRows`; UTF-8 explicit for any new reader of ClickHouse output (test JVM runs `file.encoding=windows-1252`); tables created before `start()`; snapshot `GenericRecord` rows into a `Map` during iteration; `--rerun` for any second acceptance run.
- Static string constants extracted; DRY; Google Java Style via Spotless; run `spotlessApply` on touched files only (Phase 9 D-13). No dependency upgrades without asking (Phase 14 owns them).
- `VERSION`/`CHANGELOG.md` bumps are Phase 15; this phase only records behaviour changes in a ledger.
- Do not commit or change PR text without being asked; GSD workflow entry points must be used for edits.
- Test target is ClickHouse `latest` x client V2 only; additive changes to ease future rebases.

## Sources

### Primary (HIGH confidence)
- Live probes this session against ClickHouse `26.9.12.8` x client V2 via Testcontainers (colima `vz`), on branch `sync/v1.7.0`: Jackson-vs-RowBinary diff over 47 cells, timestamp/date/decimal/bytes encoding candidates (about 400 inserts), null semantics, negative epochs, nested-null behaviour, Map/Array nullable matrix, `Nullable(Tuple)` legality, DESCRIBE row shapes, `JsonColumnTest` read-back. Probe classes were deleted; `git status` is clean; raw outputs were in `/tmp/phase13-spike-*.txt` (not committed).
- Repository reads: `ClickHouseWriter.java` (232-249, 568-760, 952-1010, 1279-1311, 1413-1430, 1632-1640), `Column.java` (185-300), `Table.java` (60-200), `ClickHouseHelperClient.java` (471-495, 518-560), `ClickHouseSinkConfig.java` (80-90, 430-455), `DataJson.java`, `StructToJsonMap.java`, `DebeziumRecordConvertor.java`, `Record.java`, `Processing.java`, `Utils.java`, `HeaderToValue.java`, `ConvertToMapOrJson.java`, and the test classes named in CONTEXT.
- `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `13-CONTEXT.md`, `.planning/research/PITFALLS.md` (Pitfalls 5, 10, 11, 14, 15), and the archived review findings for P4-IN-01, WR-01 (Phase 10), WR-03 (Phase 2), IN-01 (Phase 11) and P9-IN-01.

### Secondary (MEDIUM confidence)
- None used. No external documentation lookup was needed; the research-plan/Context7 seam was not invoked because every question was answerable from the repository or a live probe.

### Tertiary (LOW confidence)
- Behaviour on ClickHouse versions older than `latest` (A1, A5) is unverified, and out of the project's declared test matrix.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new dependency; all APIs exercised in probes
- Architecture (routing, encodings, container-loop checks): HIGH for encodings and routing, MEDIUM for the exact shape of the writer class (planner's discretion)
- Pitfalls: HIGH - each one reproduced or read at a cited line

**Research date:** 2026-10-08
**Valid until:** 2026-11-07 (30 days), but `latest` floats: re-run the encoding probes if the server moves past 26.9 before the writer plans execute
