# Phase 19: Nullable Enum & Tuple Mapping - Research

**Researched:** 2026-10-12
**Domain:** `Column.extractColumn` nullability for `Enum8`/`Enum16`/`Tuple`; RowBinary wire shape for `Nullable(Enum)`/`Nullable(Tuple)`; DESCRIBE sub-column reporting; WARN/anchor/FORK-DELTA bookkeeping
**Confidence:** HIGH (wire shape, DESCRIBE behaviour and the end-to-end byte proof were all run live on ClickHouse 26.9.16.11 this session); MEDIUM on the exact shape of the final Tuple patch, which the spike gate decides (D-06, Discretion)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Carried forward (locked earlier; not re-opened)
- Element nullability comes from the declared type string, never from DESCRIBE's all-`Nullable` sub-columns; `Table.java` untouched; parse the type string once per `Column`, never per record (ROADMAP research flag).
- Never edit upstream-identical tests (`ColumnTest`, `TableTest`); add fork-only classes beside them (`ColumnArrayRecursionTest` precedent).
- Run every Tuple matrix cell against a twin with a `DEFAULT` column (`RowBinaryWithDefaults`) and one without; do not set `enable_nullable_tuple_type` in test DDL; record `SELECT version()` at each gate; get a seen-red mutation.
- Phase 18 rules hold: exception/log texts carry column metadata only, never record values; the flipped pins rely on Phase 18's column-naming `DataException` (WRT-10).
- Typed SQL read-back, never `countRows`; cache-busted two-run suite gate on `latest` x V2; `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; `spotlessApply` on touched files only.
- Per-fix commits; each fork patch in upstream regions gets its FORK-DELTA row and fork-anchors row in the same commit (D-13 pattern).
- Operator-visible changes are recorded in a behaviour ledger as they land (`19-BEHAVIOUR-LEDGER.md`, Phase 13/18 format) for Phase 20's CHANGELOG/ROLLOUT. Two Enum-fix rows ROADMAP already names: stored values change for `Nullable(Enum)` slots, and a record omitting a `Nullable(Enum)` field now passes validation and writes NULL.
- Every approval gate needs the user's own explicit answer; executors refuse relayed approvals, so human gates run orchestrator-inline (milestone rule, Phase 16 precedent).

#### Go/no-go gate shape
- **D-01:** The Tuple go/no-go is a **blocking-human gate**: the spike presents its matrix results plus a one-line recommendation and the user answers go or no-go directly. No auto-go, no criteria-driven delegation.
- **D-02:** The decision is **binary only**. Go means ROADMAP criterion 3 in full: `Nullable(Tuple(...))` round-trips AND `Array(Nullable(Tuple(...)))` still round-trips. Anything less is a no-go with a Known cost. No partial go (e.g. top-level fixed, array shape still warned).
- **D-03:** The gate sits **between plans**: the spike is its own plan that ends by writing the matrix results and recommendation; the orchestrator asks the user at the plan boundary, then dispatches either the Tuple-fix plan or the Known-cost plan. Plan the Tuple work as two alternative plans (or one plan written after the gate), never as a mid-plan checkpoint.
- **D-04:** The gate bundle is **matrix + approach + cost**: per-cell results on both twin tables, `SELECT version()`, the proposed type-string parsing approach, the estimated `ClickHouseWriter`/`Column` diff shape, and a one-line recommendation. Enough to judge risk without reading spike code.

#### Spike artifact & reuse (COLS-09)
- **D-05:** The spike is a **committed live JUnit test**, not a throwaway script. Before the fix it documents current (broken) behaviour per cell - which is the seen-red requirement, satisfied structurally. On go, the same class flips to the fixed expectations and becomes the permanent COLS-10 coverage. On no-go, it stays as a pinned record of exactly what fails. - **Reversibility:** costly - the spike test's assertions become the COLS-10 acceptance surface; replacing it later means rebuilding the matrix evidence.
- **D-06:** Each matrix cell proves **both sides**: (a) the current DESCRIBE-derived behaviour (broken), and (b) the same write with a hand-built `Column` carrying correct element nullability, proving the writer produces round-trippable bytes once nullability is right. (b) is the real go evidence - L-11 already shows the writer honours `isNullable(true)` on a Tuple column.
- **D-07:** Matrix scope: the **ROADMAP cells decide** go/no-go - `Nullable(Tuple(...))` with nullable and non-nullable elements, and `Array(Nullable(Tuple(...)))`, each against both twin tables. Added as **informational, non-deciding extras**: single-field vs multi-field tuple (the L-11 coincidence), `Map(K, Nullable(Tuple))`, and a nested Tuple-in-Tuple. Extras cannot veto a go; they feed the FORK-DELTA/rollout wording so the fix's edges are stated, not guessed.
- **D-08:** The verdict is recorded in **`.planning/phases/19-nullable-enum-tuple-mapping/19-SPIKE.md`**: matrix tables per twin table, `SELECT version()`, the parsing approach, the estimated diff shape, the recommendation, and the user's recorded go/no-go answer - the Phase 16/17 measured-approval-bundle shape.

#### WARN lifecycle
- **D-09:** The discovery WARN **narrows to Tuple-only in the Enum-fix commit itself**: predicate retargeted, WARN text, `fork-anchors.txt` row and FORK-DELTA row rewritten together, so the WARN is never stale at any commit (COLS-11's same-commit rule). The Tuple outcome later either deletes it (go) or leaves the narrowed state as final (no-go).
- **D-10:** On go, the **whole WARN machinery is deleted** in the Tuple-fix commit: `hasNullableEnumOrTuple` (or its narrowed successor), the `WARNED_NULLABLE_ENUM_OR_TUPLE` dedup set, the message constants, and both describe-loop hooks, with the anchors row and FORK-DELTA row removed in the same commit. Shrinks the fork delta per the prefer-upstream policy; IN-03/IN-05 die with it. - **Reversibility:** reversible - git history keeps the machinery if a new broken shape ever needs it.
- **D-11:** The WARN pin tests (`ClickHouseHelperClientForkUnitTest#nullableEnumOrTupleTypesAreFlagged`, `#warnOnceDedupesPerColumnAndRawType`, `ClickHouseHelperClientForkLiveTest#describeTableWarnsOnceForNullableEnumOrTupleColumns`) are **flipped to negative pins, never deleted**: each asserts the new truth - discovery of a `Nullable(Enum)`/`Nullable(Tuple)` table logs no such WARN (use Phase 18's log-capture helper) and the resulting `Column` carries correct nullability. Each flip carries a written rationale (ROADMAP criterion 2 applies the same rule to the D-16/L-13 behaviour pins).

#### IN-01..IN-05 disposition (DEBT-09)
- **D-12:** Appetite is **fix what the phase already touches**; no work outside lines the phase edits.
  - **On go:** all five close as moot/superseded - the WARN machinery is deleted (IN-01, IN-03, IN-05), the negative pins supersede the text-pin ask (IN-04), and the hooks IN-02 worried about are gone.
  - **On no-go:** the narrowed Tuple WARN rewrite fixes **IN-01** (text names the RowBinary insert route as the only affected route) and **IN-04** (the new narrowed text is pinned with the Phase 18 log-capture helper); **IN-03**'s delimiter ambiguity is fixed while in those lines (unambiguous key separator); **IN-02** closes with the written reason that the suite target is V2-only (project constraint) and **IN-05** closes with the rebase-churn reason.
- **D-13:** Dispositions are written into the archived `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md`, each row citing the fixing commit or the updated reason - the Phase 16 precedent for IN-07..IN-10.

### Claude's Discretion
- The spike test's class name and placement, and how cells are parameterised.
- The narrowed predicate/constant naming on the Tuple-only state (e.g. `hasNullableTuple`), as long as the anchors row moves in the same commit.
- The exact parsing approach for element nullability from the declared type string (proposed at the gate, decided by the spike evidence).
- Plan split beyond the fixed phase order (e.g. whether the Enum fix is its own plan or shares the spike's wave; the gate must still fall between plans).
- Exact wording of the narrowed WARN and of flipped-pin rationales, within the metadata-only content rules.

### Deferred Ideas (OUT OF SCOPE)
- **IN-02 (V1 describe-hook coverage):** stays closed-with-reason; a V1-path test run belongs to a milestone that targets client V1, which none does.
- **Repairing rows already stored wrongly by 999.1:** operator data - Phase 20's rollout note documents the query, the connector never rewrites it (REQUIREMENTS Out of Scope).
- **`Map(K, Nullable(Tuple))` and Tuple-in-Tuple fixes beyond what criterion 3 promises:** the spike measures them informationally; fixing shapes beyond the ROADMAP cells is a new capability for a future phase if the evidence warrants it.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| COLS-08 | `Nullable(Enum8)`/`Nullable(Enum16)` round-trip NULL and non-NULL, top level and nested in `Array`/`Map`, typed read-back; D-16/L-13 pins flipped, not deleted | Enum fix is one builder call (`.isNullable(isNull)`) in `Column.extractColumn`'s Enum branch; wire shape and a live byte proof below; pin inventory with exact fate per outcome |
| COLS-09 | Live twin-table Tuple spike with go/no-go and parsing approach before any Tuple production code | Matrix design, seam to reuse (`CapturingClickHouseWriter` + client `insert`), verified DESCRIBE behaviour, and an offline end-to-end emulation that already supports a go |
| COLS-10 | On go: `Nullable(Tuple)` + `Array(Nullable(Tuple))` round-trip with element nullability from the declared type string, `Table.java` untouched; on no-go: WARN narrows to Tuple, FORK-DELTA Known cost, 999.1 updated | Recommended parsing approach (`ClickHouseColumn.of`, verified), literal-`\n` normalisation pitfall, both-outcome file lists |
| COLS-11 | WARN, `fork-anchors.txt` row and FORK-DELTA change in the same commit as the code they describe; merge-invariant script green | Exact anchor row text and counts, the script's cost (about 3 min 12 s) and per-commit semantics, per-commit file checklists |
| DEBT-09 | 15-REVIEW IN-01..IN-05 fixed or closed with written reason, consistently with the Tuple outcome | Finding texts, disposition-file vocabulary and row format, per-outcome disposition table |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Sources: `./CLAUDE.md` -> `AGENTS.md`, and `.claude/CLAUDE.md`.
- **High-risk files:** `ClickHouseWriter.java` (type-mapping errors corrupt data) and `ClickHouseSinkConfig.java` need extra care and thorough tests. This phase's recommended design edits `ClickHouseWriter.java` only for one stale comment.
- **Tests:** always read data back and `assertEquals` against the original; never delete or weaken existing tests; DRY; read `ClickHouseTestHelpers.java` before writing tests; descriptive test names; negative tests.
- **Style:** Java 17, Google Java Style via Spotless (`spotlessApply` on touched files only; `ClickHouseWriter.java` stays IS DIRTY under the IDE hook per STATE, so do not reformat it); Lombok for boilerplate; SLF4J (TRACE per record, DEBUG per batch, INFO lifecycle); static string constants extracted; comments only for non-obvious invariants.
- **Security:** never log secrets or record data in plaintext; do not upgrade dependencies without asking.
- **Observability:** error messages clear and actionable, with context.
- **Hot paths:** avoid per-record allocation/parsing in `Processing`, `ProxySinkTask`, `ClickHouseWriter` (maps to the "parse once per `Column`" flag).
- **Project:** ClickHouse `latest` + client V2 only; behaviour preservation (code changes limited to genuine bug fixes); additive changes for rebasability; commits only when the user asks (GSD workflows commit per the phase plan); do not edit upstream-identical tests.
- **GSD enforcement:** repo edits go through a GSD command.

## Summary

The defect is exactly where CONTEXT says: `Column.extractColumn` builds the Enum branch and the Tuple branch without `.isNullable(isNull)`. For the Enum half the fix is one builder call and the writer already does the right thing for a nullable Enum (marker byte, then value; `doWritePrimitive` writes the null marker for a null). For the Tuple half the root cause is deeper than a missing builder call: ClickHouse's `DESCRIBE ... describe_include_subcolumns=1` reports every sub-column of a `Nullable(Tuple(...))` (and of `Array(Nullable(Tuple))`, `Map(K, Nullable(Tuple))`) as `Nullable(...)`, even for elements declared non-nullable, and `Table.java` (byte-identical to upstream, pinned) copies those sub-columns into `tupleFields` as they come. Setting the tuple's own `isNullable` alone therefore double-flags (verified: `ATTEMPT_TO_READ_AFTER_EOF` even for the single-field case). The declared type string is the only correct source of element nullability.

**I ran the whole Tuple go case offline against a live server and it works.** A scratch probe (not committed) built the real `Column`/`Table` objects from live DESCRIBE output with the repo's own classes, paired each root `Column` tree with `com.clickhouse.data.ClickHouseColumn.of(rawType)` (already on the classpath through `api(libs.clickhouse.data)`) to overwrite `isNullable` on tuple roots and tuple fields, then serialized three records with the unchanged `ClickHouseWriter.doWriteCol`. Inserted with `RowBinary` into a table without a DEFAULT column and with `RowBinaryWithDefaults` into a twin with one, every cell read back correctly on ClickHouse 26.9.16.11: `Nullable(Tuple)` with nullable and non-nullable elements, `Array(Nullable(Tuple))` with a NULL element, `Map(String, Nullable(Tuple))` with a NULL value, a nested `Tuple` inside `Nullable(Tuple)`, `Nullable(Enum8)`, `Array(Nullable(Enum16))`, `Map(String, Nullable(Enum8))`, and omitted fields stored as NULL. The same probe against the unmodified code throws on the first null tuple. So the recommendation going into the gate is **GO**, with the approach below; the committed spike must still produce the evidence (D-05, D-06).

Two traps would have broken a straightforward implementation, both verified: (1) `ClickHouseFieldDescriptor.fromJsonRow` turns the server's escaped newlines in pretty-printed non-nullable `Tuple(...)` type strings into a literal backslash plus `n` (two characters), which makes the type unparseable unless normalised; (2) enum labels and tuple element names can contain commas, parentheses and backticks, so a hand-rolled splitter is wrong in edge cases while `ClickHouseColumn.of` handles all of them.

**Primary recommendation:** Ship the Enum fix (`.isNullable(isNull)` in the Enum branch plus Tuple-only WARN narrowing, anchors and FORK-DELTA in one commit), run the spike first as a committed live test with both-sides cells on both twin tables, then on go add `.isNullable(isNull)` to the Tuple branch and a once-per-root-column declared-nullability pass that pairs the `Column` tree with `ClickHouseColumn.of(normalise(rawType))` and overwrites `isNullable`; delete the WARN machinery in the same commit.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Type-string -> `Column` mapping (nullability) | Mapping layer (`Column.extractColumn`) | Discovery (`ClickHouseHelperClient` describe loops) | `extractColumn` is the defect site; the declared raw string only exists in the DESCRIBE row, which only the describe loops see |
| Attaching DESCRIBE sub-columns to Tuple/Map/Array parents | Mapping layer (`Table`, upstream-identical, must stay untouched) | - | `Table.updateParent` copies sub-columns verbatim; the fix therefore runs after `new Table(...)`, not inside it |
| Null-marker emission | Writer (`ClickHouseWriter.doWriteCol`/`doWriteColValue`) | - | Already correct when `Column.isNullable` is correct (proved by the probe); no writer logic change |
| Wire contract (flag + raw elements) | ClickHouse server RowBinary reader | - | Fixed by the server; verified below |
| Operator warning | Discovery (`ClickHouseHelperClient` WARN) | `docs/FORK-DELTA.md`, `fork-anchors.txt` | Pure static predicate over the raw DESCRIBE string; must move with the code (COLS-11) |
| Fork bookkeeping | `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`, `19-BEHAVIOUR-LEDGER.md` | - | Same-commit rule |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:clickhouse-data` (`ClickHouseColumn.of`) | 0.9.6 | Parse a declared ClickHouse type string into a nested column tree with per-node `isNullable()` | Already an `api` dependency of the connector; handled every shape I fed it (below); no hand-rolled tokenizer needed. `[VERIFIED: build.gradle.kts:92]` declares `api(libs.clickhouse.data)`; `[VERIFIED: probe run against clickhouse-data-0.9.6.jar this session]` |
| JUnit 5 + Testcontainers (`ClickHouseBase`) | 5.11.3 / 1.21.4 per project | Live twin-table tests | Existing harness; `NestedNullRowBinaryLiveTest` is the model |
| `CapturingClickHouseWriter` | in-repo (`src/test/.../sink/db/`) | Real writer with the network hooks replaced by byte capture; takes any hand-built `Table` | Gives D-06(b): serialize a hand-built `Table` with the real serialization loops, then insert the captured body with client V2 (`client.insert(table, new ByteArrayInputStream(body), ClickHouseFormat.RowBinary, ...)`, as `Lz4RoundTripLiveTest` does) |
| `LogCapture` | in-repo (`src/test/.../sink/util/LogCapture.java`) | reload4j appender with guaranteed detach (Phase 18) | Negative WARN pins and the no-go IN-04 text pin |
| `RowSnapshots.select` | in-repo (testFixtures) | Typed SQL read-back into `Map<String,String>` | The mandated read-back helper; use SQL expressions (`isNull(...)`, `tupleElement(...)`, `toString(...)`, `CAST(... AS Nullable(Int8))`) as in WRT-07's `Cell` pattern |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `ClickHouseTestHelpers.newDescriptor` | in-repo | Build a `ClickHouseFieldDescriptor` for Docker-free `Column`/`Table` tests | Unit tests built from the recorded DESCRIBE fixtures below |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `ClickHouseColumn.of` | Extend `Column.splitUnlessInBrackets` / `Utils.splitIgnoringQuotes` | `splitUnlessInBrackets` counts only parentheses; it is not quote-aware (enum labels like `'x)y'`, backtick names like `` `c,d` ``), and `Utils.splitIgnoringQuotes` is not parenthesis-aware. A correct tokenizer is real code to own. Fall back to this only if the gate rejects the `clickhouse-data` coupling |
| Pair-and-overwrite pass after `Table` (approach C, recommended) | Per-`Tuple` `Column` stores its own declared map and a custom `tupleFields` list stamps nullability in `add` (approach A) | A touches only `Column.java` but relies on `Table.updateParent` calling `parent.getTupleFields().add(child)` (it does, `Table.java:194`) and on every other mutator staying unused; C depends only on public getters |

**Installation:** none. No new dependency.

**Version verification:** `[VERIFIED: ran this session]` `clickhouse-data-0.9.6.jar` is the resolved jar (`~/.gradle/caches/modules-2/files-2.1/com.clickhouse/clickhouse-data/0.9.6/...`); `ClickHouseColumn` is not yet imported anywhere in `src/` (grep), so this is its first use. The "client 0.10.0" upgrade is explicitly not approved (REQUIREMENTS Out of Scope), so the coupling is to 0.9.6 only.

## Package Legitimacy Audit

No external packages are installed or added by this phase. `clickhouse-data` is an existing project dependency.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none added) | - | - | - | - | n/a | Not applicable |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 ClickHouse server
   DESCRIBE TABLE ... describe_include_subcolumns=1   (JSONEachRow)
        |  one row per column AND per sub-column (is_subcolumn=1)
        v
 ClickHouseHelperClient.describeTableV1 / describeTableV2   (the two describe loops)
   |-- skip rules (alias/materialized/ephemeral, shouldSkipDescribeRow)
   |-- [today] warnOnceIfNullableEnumOrTuple(raw type)  --> WARN                 <- deleted on go (D-10)
   |-- ClickHouseFieldDescriptor.fromJsonRow(line)      (turns "\n" into backslash+n!)
   |-- Column.extractColumn(descriptor)                  <- Enum fix here; Tuple root flag here (go)
   |-- remember raw type per ROOT row                    <- new (approach C), go only
   v
 new Table(...)  [upstream-identical]  attaches sub-column Columns into tupleFields / arrayType / mapValueType
        v
 [go only] declared-nullability pass (once per root column per DESCRIBE):
        pair  Column tree  <->  ClickHouseColumn.of(normalise(rawType)).getNestedColumns()
        overwrite isNullable on tuple roots, tuple fields, and nested tuples
        v
 TableMappingRefresher / ClickHouseWriter.mapping  (published only after the pass)
        v
 ProxySinkTask -> Processing -> ClickHouseWriter.doInsertRawBinary
        -> doWriteCol (flag logic)  -> doWriteColValue ARRAY / MAP / TUPLE / Enum  (unchanged)
        v
 RowBinary / RowBinaryWithDefaults  (choice: table.hasDefaults())
        v
 ClickHouse INSERT  ->  typed SQL read-back in tests
```

### Recommended Project Structure
```
src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/
├── Column.java                      # Enum branch (+ Tuple branch on go): .isNullable(isNull); a package-visible setter for isNullable
└── DeclaredNullability.java         # NEW, fork-only, go only: static apply(Column root, String rawType) -- the pairing walk
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/
└── ClickHouseHelperClient.java      # Enum commit: narrow WARN to Tuple. Go: delete WARN machinery, add one post-Table call in each describe loop
src/test/java/com/clickhouse/kafka/connect/sink/
├── NullableTupleSpikeLiveTest.java        # name at the planner's discretion; D-05: pre-fix pins, flips on go
└── NullableEnumRoundTripLiveTest.java     # COLS-08 live (twin tables, top-level + Array + Map)
src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/
└── ColumnNullableEnumTupleTest.java       # Docker-free; beside, never inside, ColumnTest/TableTest (ColumnArrayRecursionTest precedent)
.planning/phases/19-nullable-enum-tuple-mapping/
├── 19-SPIKE.md   19-BEHAVIOUR-LEDGER.md   # D-08 / D-13 of Phase 18 format (IDs continue at L-31)
```

### Pattern 1: Enum fix (COLS-08, unconditional)
**What:** the Enum branch must carry `isNull`, exactly like the primitive tail already does.
**Current code** `[VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:202-211]`:
```java
        if (valueType.startsWith("Enum")) {
            Type type;
            if (valueType.startsWith("Enum16")) {
                type = Type.Enum16;
            } else {
                type = Type.Enum8;
            }
            return builder.type(type)
                    .enumValues(extractEnumValues(valueType))
                    .build();
```
**Fix:** add `.isNullable(isNull)` to that builder chain (the primitive tail at `Column.java:288-293` already does `.isNullable(isNull)`; `grep -cF ".isNullable(isNull)"` is 1 today, so 2 after the Enum fix and 3 after the Tuple fix).
**Why this is enough:** the `Nullable(...)` branch recurses with `isNull=true` (`Column.java:274-275`), the `Array` branch recurses into `Nullable(Enum...)` (`Column.java:213`), and DESCRIBE reports `Map(K, Nullable(Enum))` as `m.values Array(Nullable(Enum8(...)))`, so top level, `Array` and `Map` all reach the Enum branch with `isNull=true`. The probe's corrected tree showed `ne Enum8 nullable=true`, `ae` element `Enum16 nullable=true`, `me.values Enum8 nullable=true`.
**Writer side (no change needed)** `[VERIFIED: ClickHouseWriter.java:1011-1016]` `case Enum8: BinaryStreamUtils.writeEnum8(stream, col.convertEnumValues((String) value).byteValue());` and `doWritePrimitive` starts with `if (value == null) { BinaryStreamUtils.writeNull(stream); return; }` (`:901-904`). ARRAY writes `writeNonNull` before a non-null element of a nullable element column (`:737-739`), MAP likewise (`:709-711`), `doWriteCol` handles top level (`:1051-1100`).

### Pattern 2: Declared-nullability pass (COLS-10 on go, recommended approach C)
**What:** once per root column per DESCRIBE, after `new Table(...)`, walk the `Column` tree and `ClickHouseColumn.of(normalise(rawType))` in parallel and overwrite `isNullable`.
**Pairing rules** (all exercised by the probe, zero unmatched elements):
- `TUPLE`: `column.isNullable := declared.isNullable()`; for each `tupleFields` child, element name = `child.getName().substring(tuple.getName().length() + 1)`; match it to `declared.getNestedColumns()` by `getColumnName()`, or by 1-based position when the declared name is empty (unnamed tuple: DESCRIBE names are `p5.1`, `p5.2`); recurse.
- `ARRAY`: pair `getArrayType()` with `declared.getNestedColumns().get(0)`.
- `MAP`: pair `getMapValueType()` with `declared.getNestedColumns().get(1)` (value is index 1; key is index 0).
- Everything else (scalar, Enum, ...): `isNullable := declared.isNullable()`.
**Sketch** (probe prototype; not committed code):
```java
// Source: scratch probe /tmp/probe19/src/Emu.java, run against ClickHouse 26.9.16.11
static void pair(Column c, ClickHouseColumn d) {
    switch (c.getType()) {
        case TUPLE:
            c.setNullable(d.isNullable());
            List<ClickHouseColumn> decl = d.getNestedColumns();
            for (Column child : c.getTupleFields()) {
                String element = child.getName().substring(c.getName().length() + 1);
                // match decl by getColumnName(), or by 1-based index when the name is empty
                pair(child, matchingElement(decl, element));
            }
            break;
        case ARRAY: pair(c.getArrayType(), d.getNestedColumns().get(0)); break;
        case MAP:   if (c.getMapValueType() != null) pair(c.getMapValueType(), d.getNestedColumns().get(1)); break;
        default:    c.setNullable(d.isNullable());
    }
}
```
**Where it runs:** after `new Table(...)` in `describeTableV1` and `describeTableV2` (`ClickHouseHelperClient.java:595` and `:666` today), over `table.getRootColumnsList()`, with a `rootName -> rawType` map filled for non-sub-column rows in the same loop. `ClickHouseColumn.of` is called once per root column per DESCRIBE, never per record. A shared private helper that builds the `Table` and runs the pass keeps it to one place.
**Failure policy:** `ClickHouseColumn.of` throws `IllegalArgumentException` for types it does not know (probe: `Interval` -> `Unknown data type: Interval`). Fail open: keep DESCRIBE's nullability for that root column and log one metadata-only line (column name and raw type, no values). That reproduces today's behaviour for exotic types instead of making the table unusable. (Open question 2.)
**Alternative (approach A):** each Tuple `Column` parses its own raw string in `extractColumn` and stores declared element nullability; a `tupleFields` list subclass stamps children in `add`. Same result, no helper-client hook, but magic and tied to `Table`'s `add` call. Choose at the gate.

### Pattern 3: Both-sides spike cell (D-06) with the existing seams
(a) current behaviour: real `ClickHouseSinkTask.put` against the DESCRIBE-derived `Table`, asserting the observed failure or wrong read-back, and that no row is written where it throws (as `putExpectingFailure` does in `NestedNullRowBinaryLiveTest`).
(b) fixed bytes: `new CapturingClickHouseWriter(handBuiltTable, csc)` -> `doInsert(records, queryId)` -> `captures().get(0).body()` -> `client.insert(table, new ByteArrayInputStream(body), ClickHouseFormat.RowBinary | RowBinaryWithDefaults, new InsertSettings())` -> typed read-back. `Capture.supportDefaults` proves which format the twin took. The hand-built `Table` uses `Column.builder()...isNullable(true).tupleFields(List.of(...non-null/nullable children...))` as in `NestedNullableCompositeWriteTest#tupleColumn`.
Twin tables: the same DDL once with an extra unrelated `d UInt32 DEFAULT 42` column and once without. Assert `chc.describeTable(...).hasDefaults()` as a precondition, because that boolean (set from any described row with `default_type == DEFAULT`) selects the format (`ClickHouseWriter.java:241`, `:1264-1266`).

### Anti-Patterns to Avoid
- **Setting only the tuple's `isNullable` from DESCRIBE.** The children stay all-`Nullable`, so the writer emits tuple flag plus element markers. Verified: `Nullable(Tuple(a Int32))` with `[0][0][7 0 0 0]` -> `ATTEMPT_TO_READ_AFTER_EOF`.
- **Trusting sub-column nullability under a Nullable parent.** `nt.a` is `Nullable(Int32)` although declared `Int32`.
- **Parsing per record or per `Column.extractColumn` call for every sub-column row.** Parse per root column per DESCRIBE.
- **Editing `Table.java`, `ColumnTest`, `TableTest`.** `Table.java` is byte-identical to upstream v1.7.0 `[VERIFIED: git diff --stat v1.7.0 HEAD for Column.java and Table.java shows only Column.java differs]`; the "Adopted wholesale" row in FORK-DELTA pins it.
- **Putting Enum-in-Tuple cells in the spike matrix.** See Pitfall 7.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Declared type-string parsing (element names, per-node `Nullable`, nesting) | A character tokenizer or regex over `Tuple(...)` | `ClickHouseColumn.of(name, normalisedRawType)` | Handles backtick names with spaces/commas/doubled backticks, enum labels containing `,` and `)`, `LowCardinality(Nullable(String))`, `Decimal(10, 2)`, `DateTime64(3, 'UTC')`, `JSON(max_dynamic_paths = 10)`, `Variant(...)`, `Object('json')`, unnamed tuples, Unicode names `[VERIFIED: probe this session, 20+ shapes]` |
| Log capture in tests | A new appender | `LogCapture.attach(ClickHouseHelperClient.class)` | Phase 18 helper detaches and restores the level |
| Serialized-byte inspection | A new byte-array writer | `CapturingClickHouseWriter` (`Capture.body()`, `.supportDefaults()`) | Real routing and serialization loops, no mocks (AGENTS.md forbids mocking the writer in state tests) |
| Typed read-back | `countRows` | `RowSnapshots.select` with SQL expressions | Mandated by AGENTS.md and STATE |

**Key insight:** the writer already implements the wire contract correctly for every nullable shape in scope; the whole phase is about handing it correct `Column.isNullable` values. Any change in the write branches is a smell.

## Runtime State Inventory

Not a rename or migration phase, but the Enum fix changes stored values, so the five categories were answered for completeness:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | Rows already written through the broken path into `Nullable(Enum8/16)` and `Nullable(Tuple)` slots (silent NULLs or shifted values) | None in code. Out of Scope (REQUIREMENTS): the connector never repairs them; Phase 20's rollout note carries the explanation. Record in the ledger |
| Live service config | Kafka Connect connectors keep `exactlyOnce` state in KeeperMap by offset ranges, not by column types | None - verified: no type-dependent state in `Processing`/`KeeperStateProvider` (not touched) |
| OS-registered state | None | None - verified: phase touches only Java sources, tests and docs |
| Secrets/env vars | None | None |
| Build artifacts | `build/libs/...-v1.7.0_gg1.jar` is stale relative to the source tree (my probe used `build/classes/java/main`) | Always run `./gradlew` tasks; never run ad-hoc code against the jar |

## Common Pitfalls

### Pitfall 1: `fromJsonRow` turns the server's `\n` into backslash + `n`
**What goes wrong:** non-nullable tuples are pretty-printed by the server: `p1` is `Tuple(\n    a Int32,\n    b String)` in JSONEachRow, and `Array(Tuple(\n    a Int32,\n    b String))` for arrays and map values, while `Nullable(Tuple(a Int32, b String))` and `Array(Nullable(Tuple(...)))` come single-line. `ClickHouseFieldDescriptor.fromJsonRow` does `json.replace("\\", "\\\\").replace("\n", "\\n")` before Jackson, so the descriptor's type string contains a literal backslash and `n` (printed as `Tuple(<BS>n    a Int32,<BS>n    b String)`).
**Why it matters:** `ClickHouseColumn.of` on that literal string throws `Unknown data type: a Int32` `[VERIFIED: probe]`. A parser would silently fail for every non-nullable multi-line tuple (harmless where DESCRIBE is already right, harmful anywhere server formatting differs by version).
**How to avoid:** normalise before parsing: replace the two-character sequence backslash-`n` with a space (outside quotes if you want to be exact; labels containing a backslash are already mangled by `fromJsonRow` today, so a plain replace is acceptable). Pin it with a unit test whose input is exactly what `fromJsonRow` delivers for a multi-line tuple. `[VERIFIED: D.java probe printed "Tuple(<BS>n    a Int32,<BS>n    b String)"]`
**Warning signs:** pairing falls back (fail-open log) for plain `Tuple(...)` columns.

### Pitfall 2: Single-field `Nullable(Tuple(a Int32))` works today by coincidence
**What goes wrong:** today the Tuple is non-nullable and the only child is `Nullable(Int32)`, so the bytes are `[0][int]`, which the server reads as tuple flag 0 plus the raw Int32. `[VERIFIED: live]` inserted and read back `(7)`. After the fix, an element that is `null` in the record is a `DataException` (declared non-nullable `a`), where today the child's marker `1` is read as "tuple is NULL" and the row silently stores a NULL tuple.
**How to avoid:** include single-field cells in the spike extras (D-07) and record the behaviour change in the ledger.

### Pitfall 3: DESCRIBE reports Array and Map elements, and non-`Nullable`-able types, differently
Under a `Nullable(Tuple)`, `Array(...)`/`Map(...)` elements are never wrapped (`p11.d Array(Int32)`, `p11.e Map(String, Int32)`), `LowCardinality(Nullable(String))` stays as declared, and plain scalars get wrapped (`Nullable(Decimal(10, 2))`, `Nullable(DateTime64(3, 'UTC'))`). Under `Array(Nullable(Tuple))` the sub-columns are `Array(Nullable(Int32))`. A rule such as "unwrap Nullable from DESCRIBE" would be wrong for some elements; only the declared tree is right. `[VERIFIED: live DESCRIBE]`

### Pitfall 4: Nested Tuple under a Nullable parent gets a wrapper it was not declared with
`tt Nullable(Tuple(a Int32, c Tuple(d String, e Nullable(Int32))))` yields a sub-column row `tt.c` typed `Nullable(Tuple(d String, e Nullable(Int32)))` (wrapper added, contents accurate). `Column.extractColumn` on that row makes `tt.c` nullable on the Tuple fix, which is wrong (declared non-nullable). The pairing pass must overwrite the nested tuple's own flag too (the probe's final tree shows `tt.c TUPLE nullable=false`). `[VERIFIED: live]`

### Pitfall 5: Element names that collide with structural sub-columns
`shouldSkipDescribeRow` drops rows ending in `.null`, `.size0` or `.size` (`ClickHouseHelperClient.java:477-488`), so a tuple element literally named `size`, `size0` or `null` never becomes a field. Pre-existing and out of scope; pairing by name (not by position) means a missing field does not shift the others. Pairing by position would. `[VERIFIED: code reading]`

### Pitfall 6: `RowSnapshots.select` uses `getString`
A NULL tuple or NULL element must be read back with SQL (`isNull(nt)`, `tupleElement(nt, 'a')`, `toString(nt)`), not with the raw column. For Enums assert the numeric code as well as the label: `CAST(ne AS Nullable(Int8))` and `arrayMap(x -> CAST(x AS Nullable(Int16)), ae)` work on 26.9.16.11 (`toInt8OrNull(enum)` does not). The test JVM runs `file.encoding=windows-1252`: use ASCII values.

### Pitfall 7: Spike ordering versus the Enum fix
The spike plan runs first and its "current behaviour" pins are written against the pre-Enum-fix tree. The Enum commit changes how Enum children inside a Tuple are mapped: `Nullable(Tuple(a Enum8(...)))` becomes working-by-coincidence like the Int32 single-field case. Keep Enum-in-Tuple cells out of the deciding matrix; if wanted, add them as informational extras after the Enum plan.

### Pitfall 8: The WARN dedup set is process-wide
`WARNED_NULLABLE_ENUM_OR_TUPLE` is a static `ConcurrentHashMap.newKeySet()` (`ClickHouseHelperClient.java:498`). Any WARN-text test (the no-go IN-04 pin, or a positive test during the Enum-to-Tuple window) must use a unique table name (the existing tests use `"d02_" + UUID.randomUUID()` and `createTopicName(...)`), or it sees a suppressed WARN depending on test order.

### Pitfall 9: Pre-existing edges not to widen
- An unknown enum label: `Column.convertEnumValues` returns `enumValues.get(value)` (a null `Integer`), and `.byteValue()` on it is an NPE `[VERIFIED: ClickHouseWriter.java:1012, Column.java:521-526]`. Same for non-nullable Enums today. Do not change in this phase.
- `extractEnumValues` cuts at the first `)` (`Column.java:370`), so an enum label containing `)` is mis-parsed. Keep test labels free of parentheses.
- Behaviour preservation: only the mapping changes; error types for the cases above stay as they are.

### Pitfall 10: Do not set `enable_nullable_tuple_type`
On the pinned target the setting already defaults on: `SELECT name, value, changed FROM system.settings WHERE name LIKE '%nullable_tuple%'` returned `enable_nullable_tuple_type 1 0`, `allow_experimental_nullable_tuple_type 1 0`, `allow_nullable_tuple_in_extracted_subcolumns 1 0` (value, then changed=0) on 26.9.16.11 `[VERIFIED: live]`. Test DDL must not set it (carried decision).

## Code Examples

### Wire shapes (bytes), verified by direct insertion on ClickHouse 26.9.16.11
```
Nullable(Tuple(a Int32, b String)), non-null  : 00 | 07 00 00 00 | 01 78          (flag, Int32, String "x"; no markers on elements)
Nullable(Tuple(a Nullable(Int32), b String))  : 00 | 00 05 00 00 00 | 01 79       (flag, element marker + Int32, String)
Nullable(Tuple(...)), NULL                    : 01                                  (flag only, no element bytes)
Nullable(Enum8('a'=1,'b'=2,'z'=0)), 'b'       : 00 02 ;  NULL : 01
Nullable(Enum16('a'=1,'b'=1000)), 'b'         : 00 E8 03 ;  NULL : 01
```
`RowBinaryWithDefaults` prefixes each top-level column with `00` (value follows) or `01` (use default); verified `00 00 07..` for a non-null Nullable(Tuple), `00 01` for NULL, and a lone `01` for the default (stored NULL for a Nullable column, `42` for the `DEFAULT 42` column) `[VERIFIED: live]`. The connector writes the prefix only at the top level; tuple fields call `doWriteCol(..., false)` (`ClickHouseWriter.java:784`).

### What the current code derives from real DESCRIBE rows (repo classes + live rows)
```
nt  TUPLE nullable=false   children nt.a INT32 nullable=true, nt.b STRING nullable=true       (declared Int32, String)
aat ARRAY -> element TUPLE nullable=false, aat.a nullable=true, aat.b nullable=true            (declared Array(Nullable(Tuple(a Int32, b Nullable(String)))))
mt  MAP   -> value   TUPLE nullable=false (name mt.values), mt.values.a/.b nullable=true
tt  TUPLE nullable=false   tt.a nullable=true, tt.c TUPLE nullable=false, tt.c.d/.e nullable=true
ne  Enum8 nullable=false ; ae element Enum16 nullable=false ; me value Enum8 nullable=false
```
After the Enum fix `ne`, `ae` element, `me` value are `nullable=true`. After the Tuple pass (declared): `nt` true with `a,b` false; `nt2` true with `a` true, `b` false; `aat` element true with `a` false, `b` true; `mt` value true with both false; `tt` true, `tt.a` false, `tt.c` false, `tt.c.d` false, `tt.c.e` true `[VERIFIED: probe output]`.

### Recorded DESCRIBE rows to use as Docker-free fixtures (26.9.16.11, `describe_include_subcolumns=1`, JSONEachRow)
```
p1   Tuple(\n    a Int32,\n    b String)                       -> p1.a Int32 ; p1.b String          (non-nullable: DESCRIBE is right)
p6   Nullable(Tuple(Int32, Nullable(String)))                   -> p6.null UInt8 ; p6.1 Nullable(Int32) ; p6.2 Nullable(String) ; p6.2.null ; p6.2.size
p8   Nullable(Tuple(`a b` Int32, `c,d` Nullable(String)))       -> p8.a b Nullable(Int32) ; p8.c,d Nullable(String) ; ...
p9   Tuple(\n    a Int32,\n    c Tuple(\n        d String,\n        e Nullable(Int32)))  -> p9.c Tuple(\n    d String,\n    e Nullable(Int32)) ; p9.c.d String ; p9.c.e Nullable(Int32)
aat  Array(Nullable(Tuple(a Int32, b Nullable(String))))        -> aat.size0 UInt64 ; aat.null Array(UInt8) ; aat.a Array(Nullable(Int32)) ; aat.b Array(Nullable(String)) ; ...
mt   Map(String, Nullable(Tuple(a Int32, b String)))            -> mt.values Array(Nullable(Tuple(a Int32, b String))) ; mt.values.a Array(Nullable(Int32)) ; mt.values.b Array(Nullable(String)) ; ...
```
(`\n` above is the two-character form `fromJsonRow` delivers; build fixtures by feeding the raw JSON line through `fromJsonRow`, not by writing the string by hand.)

### Negative WARN pin (D-11)
```java
// Source: LogCapture (src/test/.../sink/util/LogCapture.java) + ClickHouseHelperClientForkLiveTest pattern
try (LogCapture capture = LogCapture.attach(ClickHouseHelperClient.class)) {
    Table table = chc.describeTable(chc.getDatabase(), uniqueTopic);   // table with Nullable(Enum8) / Nullable(Tuple) columns
    assertTrue(capture.events("WARN").stream().noneMatch(e -> e.message().contains("Nullable(")));
    assertTrue(table.getRootColumnsMap().get("e").isNullable());        // the Column carries correct nullability
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Nullable(Tuple)` experimental setting `allow_experimental_nullable_tuple_type` | `enable_nullable_tuple_type`, default 1 on the pinned target; sub-column extraction governed by `allow_nullable_tuple_in_extracted_subcolumns` (default 1) | Present on 26.9.16.11 `[VERIFIED: live system.settings]`; earlier history `[ASSUMED]` | The declared-string approach does not depend on any of these settings, which is a robustness argument for it |
| Non-nullable tuple type names compact | Non-nullable `Tuple(...)` pretty-printed with newlines in DESCRIBE | Observed on 26.9.16.11; when it started `[ASSUMED]` | Parser must accept both forms (Pitfall 1) |

**Deprecated/outdated:** nothing in scope. Upstream `v1.7.0` is still the latest tag (`git ls-remote upstream`), and its `Column.extractColumn` has the same Enum/Tuple branches (`git diff v1.7.0 HEAD` shows Column.java differs only by the `Dynamic` dispatch and the Array-recursion argument fix), so this remains a fork-side patch in upstream-identical lines and needs its FORK-DELTA and anchors rows.

## Pin Inventory and Fate (D-11, ROADMAP criterion 2)

| Pin | What it asserts | Enum commit | Go | No-go |
|-----|-----------------|-------------|----|-------|
| `NestedNullableCompositeWriteTest#nullTupleFollowsColumnNullability` | Hand-built Columns: a non-nullable TUPLE throws `DataException`, a nullable one writes `0x01` | Unchanged | Unchanged (writer contract still true) | Unchanged |
| `NestedNullableCompositeWriteTest#nullElementInArrayOfNullableTupleThrowsBecauseColumnDropsTupleNullability` (asserts `assertFalse(... isNullable(), "premise: extractColumn drops Tuple nullability")`) | Old behaviour: DataException | Unchanged | **Flip** with written rationale: premise becomes `assertTrue`, null element writes the marker | Unchanged |
| `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleFailsWithDataException` | Old behaviour on a live table | Unchanged | **Flip**: the null element reads back as SQL NULL, neighbour intact | Unchanged |
| L-13 Enum null entries | No dedicated test exists (ledger: "no dedicated test; follows from the loop checks pinned in L-10") | Positive coverage is the new `NullableEnumRoundTripLiveTest`; the "flip" is the ledger row + FORK-DELTA text | - | - |
| `ClickHouseHelperClientForkUnitTest#nullableEnumOrTupleTypesAreFlagged` (six flagged strings incl. `Array(Nullable(Enum8('x' = 1)))`, `Map(String, Nullable(Enum16('y' = 1)))`, `Nullable(Enum8('z' = 1))`, `Tuple(a Nullable(Enum8('q' = 1)))`) | Predicate flags them | The Enum and Tuple-element-Enum strings **move to the negative list with rationale**; the `Nullable(Tuple(...))`/`Array(Nullable(Tuple(...)))` strings stay flagged | All become negative pins (machinery deleted) | Tuple strings stay flagged |
| `#otherTypesAreNotFlagged`, `#nullRawTypeIsNotFlagged` | Negative predicate cases | Unchanged | Rewritten against the deleted/negative form | Unchanged |
| `#warnOnceDedupesPerColumnAndRawType` (uses `Nullable(Enum8('a' = 1))` / `Nullable(Enum16('a' = 1))`) | Once-per-key dedup | Retarget its inputs to Tuple types (same assertions) | Flip to a negative pin | Retarget (keep) ; extend with the unambiguous-key case (IN-03) |
| `#subColumnsAndUnflaggedTypesNeverWarn` | Sub-column and unflagged rows never warn | Unchanged | Flip/absorb | Unchanged |
| `ClickHouseHelperClientForkLiveTest#describeTableWarnsOnceForNullableEnumOrTupleColumns` (columns `e`, `m`, `t`) | Live registration of the WARN for all three | `e` and `m` become negative, `t` stays positive | Negative for all three (`LogCapture` + `isNullable` assertions) | `t` positive; assert the new text (IN-04) |

Never delete: moving a case between lists with a rationale comment counts as a flip.

## Plan-Shape Guidance (for the planner)

Fixed order: spike -> Enum -> gate -> Tuple (go or no-go) -> DEBT-09. Suggested plans:
1. **19-01 Spike (COLS-09).** Committed live class with both-sides cells x two twin tables; extras per D-07 (single vs multi-field, `Map(K, Nullable(Tuple))`, Tuple-in-Tuple); records `SELECT version()` (the test logs it, as `Lz4RoundTripLiveTest` does); writes `19-SPIKE.md` (D-04 bundle). Ends with the matrix and a one-line recommendation; no production code. My recommendation going in: GO.
2. **19-02 Enum fix (COLS-08, part of COLS-11).** Single commit family: `Column.java` Enum branch, WARN narrowed to Tuple-only (rename the predicate, constants and `warnOnce...` so the name is honest; the discretion note allows it), flipped WARN pins, new `NullableEnumRoundTripLiveTest` and a Docker-free `ColumnNullableEnumTupleTest`, `fork-anchors.txt` (row 57 pattern and a new `Column.java` row, see below), `docs/FORK-DELTA.md` (WARN row, Known costs item 1, a new row for the Enum patch), ledger rows. Seen-red: remove `.isNullable(isNull)` from the Enum branch and watch the new live and unit tests fail, then restore.
3. **Gate (D-01/D-03)** orchestrator-inline, the user's own go/no-go answer recorded in `19-SPIKE.md`.
4. **19-03 (one of two, written after the gate or drafted as alternatives).**
   - **Go:** Tuple branch `.isNullable(isNull)`, the pairing pass, delete the WARN machinery (D-10), flip the pins above, flip the spike class to fixed expectations, stale-comment update at `ClickHouseWriter.java:749-750`, anchors/FORK-DELTA removals in the same commit, ledger rows. Seen-red: skip the pass (or revert the Tuple builder call) and watch the flipped spike cells fail.
   - **No-go:** keep the narrowed Tuple WARN, rewrite it (IN-01), pin it with `LogCapture` (IN-04), unambiguous key (IN-03), FORK-DELTA Known cost, ROADMAP 999.1 entry updated, spike class stays as the record.
5. **19-04 DEBT-09.** Edit the archived disposition file only (see below). Last, per ROADMAP.

**Per-commit COLS-11 checklist:** code + its test flips + `fork-anchors.txt` + `docs/FORK-DELTA.md` + ledger row, in one commit. `verify-merge-invariants.sh 54e63648 HEAD` evaluates `fork-anchors.txt` at every commit in the range (its header says so), so one run at the end of each plan proves every intermediate commit. Baseline today: `invariants: provenance_paths=21 anchor_commits=255 anchor_rows=55 marker_commits=302 result=PASS`, wall time 3 min 12 s `[VERIFIED: ran this session on a76d093d]`; run it in the background.

**Anchors rows to move** (format: four TAB-separated fields, fixed-string pattern, expected `grep -cF` line count, backs-ID):
- Row 57 today `[VERIFIED: .planning/scripts/fork-anchors.txt:57]`: `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	static boolean hasNullableEnumOrTuple(	1	PUB-02`. Enum commit: pattern becomes the renamed predicate (still count 1; the backs-ID can move to COLS-11). Go commit: delete the row.
- New `Column.java` rows: pattern `.isNullable(isNull)`, count 2 in the Enum commit, 3 in the Tuple commit (today 1). Backs-ID COLS-08 / COLS-10. Add a second guard row for the pass call site on go.
- Do not repeat an anchored string in a comment (the 15-REVIEW notes this gotcha).

**Behaviour ledger rows** (Phase 18 ended at L-30 `[VERIFIED: 18-BEHAVIOUR-LEDGER.md:22 "| L-30 |"]`, so Phase 19 starts at L-31; the format is the Phase 18 table: ID, source plan, change before -> after, decisions, pinned by, operator-visible, how an operator notices, Phase 20 destination):
- Enum: stored values for `Nullable(Enum8/16)` slots (before: silent NULL or shifted rows, or `CANNOT_READ_ALL_DATA`; after: correct); a record omitting a `Nullable(Enum)` field now passes `validateDataSchema` (the `!isNullable && !hasDefault` gate at `ClickHouseWriter.java:293`) and writes NULL; null entries in `Array(Nullable(Enum))`/`Map(K, Nullable(Enum))` now write NULL instead of throwing the L-13 `DataException`.
- Go only: `Nullable(Tuple)` round-trips (before: `ATTEMPT_TO_READ_AFTER_EOF` for multi-field); the same omitted-field-passes-validation effect for `Nullable(Tuple)`; null element in `Array(Nullable(Tuple))` writes NULL instead of the L-11 `DataException`; single-field `Nullable(Tuple(a T))` with a null `a`: before a silently stored NULL tuple, after `DataException` (Pitfall 2); `Nullable(Tuple(a Enum8))` single-field becomes working.

**DEBT-09 file format** `[VERIFIED: 15-REVIEW-DISPOSITION.md:81]`: "Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`." The file has YAML frontmatter per finding (`id`, `severity`, `disposition`, `title`; counters `open: 0`, `total: 13`) and a table `| Finding | Severity | Disposition | Source |` where the Source cell carries the commit or the reason (current IN-01..IN-05 rows read `deferred | deferred in 15-02: ...`). Phase 16 precedent: `fixed | <sha> (docs only, Phase 16)`. Update both the frontmatter and the table row for each of IN-01..IN-05, and the file is under `.planning/milestones/...`, so commit it with the phase docs.
- Go: all five `fixed` with the deleting commit and "superseded: machinery deleted" (IN-01/03/05 by deletion, IN-04 superseded by the negative pins, IN-02's hook is gone).
- No-go: IN-01, IN-03, IN-04 `fixed` with SHAs; IN-02 `skipped` with "suite target is V2-only (project constraint)"; IN-05 `skipped` with "rebase churn".

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | All live tests | Yes | `colima list`: `vz` Running aarch64, `DOCKER_HOST` set | - |
| ClickHouse server image | Live tests, spike | Yes | `clickhouse/clickhouse-server:latest` = 26.9.16.11 (local image; `ClickHouseBase` re-pulls floating tags, so the gate version can differ - record it) | Pin with `CLICKHOUSE_VERSION` for diagnosis only; target stays `latest` |
| JDK | Gradle | Yes | local `java` 21.0.12; build uses the Gradle toolchain (a `./gradlew --offline` classpath task ran) | - |
| Gradle wrapper | Build/test | Yes | works offline | - |
| `TESTCONTAINERS_HOST_OVERRIDE=null` in this shell | Testcontainers | Breaks Ryuk | - | Run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (STATE blocker) |
| `timeout` command | scripts | No (`env: timeout: No such file`) | - | Use Bash tool timeouts or background runs |
| `yq`, `gh` | Not needed | gh deliberately absent | - | - |

**Missing dependencies with no fallback:** none.
**Test commands:** targeted live run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests "*NullableEnumRoundTripLiveTest"`; Docker-free `./gradlew testUnit` (excludes tag `clickhouse`); two-run phase gate needs `--rerun` on the second run (STATE); `spotlessApply` on touched files only.

*Validation Architecture section omitted: `workflow.nyquist_validation` is `false` in `.planning/config.json`.*

## Security Domain

`security_enforcement` is on (ASVS level 1, block on high).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication / V3 Session | no | Unchanged; no credential handling |
| V4 Access Control | no | - |
| V5 Input Validation | yes | The only new external input is the server's DESCRIBE type string; parse it with `ClickHouseColumn.of` inside a try/catch that fails open; never evaluate or execute it |
| V6 Cryptography | no | - |
| V7 Error handling / logging | yes | WARN, parse-failure and exception texts carry column name, database, table and raw type only; never record values, element values or map keys (Phase 18 rules, WRT-10/WRT-12 precedent); assert with a sentinel-absence check over `LogCapture.allTexts()` |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Record data leaking via `DataException` messages from the newly reachable tuple-field null path | Information disclosure | Reuse `buildNestedNullMessage` (column metadata only); pin with a sentinel-value test as `NestedNullRowBinaryLiveTest` does |
| A malformed or exotic type string making table discovery throw and stalling the task | Denial of service | Fail-open per root column (keep DESCRIBE nullability), log metadata only; test with `Interval`-style input |
| Silent data corruption from a wrong marker (the defect itself) | Tampering | Live typed read-back on both twin tables and a seen-red mutation |
| Batch bytes differing between retries (exactly-once dedup) | Tampering | The pass is deterministic and runs before the `Table` is published; no per-record state; nothing in `Processing`/`KeeperStateProvider` is touched |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Server DESCRIBE formatting of non-nullable tuples (pretty-printed) and the all-`Nullable` sub-column reporting are stable on future `latest` images | Pitfalls 1, 3, 4 | Low: the declared-string approach and whitespace normalisation do not depend on the formatting, and the gate re-runs the spike on the `latest` of that day |
| A2 | The history of `allow_experimental_nullable_tuple_type` -> `enable_nullable_tuple_type` (State of the Art) | State of the Art | None for the plan; only the target version matters |
| A3 | Lombok will generate `setNullable(boolean)` for `@Setter` on the field `isNullable`; the probe used reflection | Pattern 2 | Low: the executor can write an explicit package-private setter instead; a compile error would show immediately |
| A4 | One `ClickHouseColumn.of` parse per root column per DESCRIBE has negligible cost (descriptions refresh only when a table's column count changes) | Pattern 2 | Low; it is off the record hot path |
| A5 | Fail-open (keep DESCRIBE nullability, log once) is the right policy for an unparseable declared string, versus failing the table | Pattern 2 / Open Question 2 | Medium: user may prefer fail-closed; surface at the gate |
| A6 | `skipped` vs `fixed` is the right disposition word for "moot, machinery deleted" | Plan-Shape Guidance | Low: wording of a bookkeeping file; vocabulary itself is verified |

## Open Questions

1. **Approach C (post-`Table` pairing pass) or approach A (per-`Column` declared map plus stamping list)?**
   - What we know: both give identical results in analysis; C was proven end to end by the probe; C needs two call sites in the describe loops, A stays inside `Column.java` but depends on `Table`'s `add` call.
   - What's unclear: reviewer taste about a helper-client hook versus a `List` subclass.
   - Recommendation: C; present both in the gate bundle (D-04) with the estimated diff (C: about 1 new class of roughly 50 lines, 2 changed lines in `ClickHouseHelperClient`, 1 setter and 2 builder calls in `Column.java`, 0 lines in `ClickHouseWriter` except one comment).
2. **Parse-failure policy.** Fail open with one metadata-only log line (recommended) or fail closed (table unusable)?
3. **Rename vs keep the predicate name in the Enum commit.** Recommended rename (`hasNullableTuple`, `warnOnceIfNullableTuple`, constants) so the name matches behaviour; it touches the flipped tests anyway, and the anchors row moves in the same commit.
4. **Plan 19-03 shape.** Draft both alternatives now, or write the chosen one after the gate (CONTEXT allows either)? Writing after the gate avoids dead plans.
5. **Spike extras with Enum inside Tuple.** Add after the Enum plan or skip (Pitfall 7)?

## Sources

### Primary (HIGH confidence)
- Live ClickHouse `clickhouse/clickhouse-server:latest` = 26.9.16.11, run via local Docker this session: `DESCRIBE TABLE ... describe_include_subcolumns=1` for 12 tables, `system.settings` for the nullable-tuple settings, and direct RowBinary / RowBinaryWithDefaults inserts of hand-built bytes (wire shapes, current-code failure modes, round-trip of the probe output).
- `clickhouse-data-0.9.6.jar` `ClickHouseColumn.of`: run against about 20 type strings, including the literal-backslash-`n` form (failure).
- Repo sources read this session: `Column.java` (full), `Table.java` (full), `ClickHouseWriter.java` (285-402, 540-1110, 1290-1345, 1770-1800), `ClickHouseHelperClient.java` (440-670), `ClickHouseFieldDescriptor.java`, `Data.java`, `Utils.java:270-300`, `ClickHouseBase.java`, `CapturingClickHouseWriter.java`, `LogCapture.java`, `RowSnapshots.java`, `NestedNullableCompositeWriteTest.java`, `NestedNullRowBinaryLiveTest.java` (1-470), `ClickHouseHelperClientForkUnitTest.java` (255-324), `ClickHouseHelperClientForkLiveTest.java` (340-440), `fork-anchors.txt`, `verify-merge-invariants.sh` (head), `docs/FORK-DELTA.md` (rows 38-51, 128-150), 13-BEHAVIOUR-LEDGER (L-09..L-13, L-21), 18-BEHAVIOUR-LEDGER, 15-REVIEW (IN-01..IN-05), 15-REVIEW-DISPOSITION, ROADMAP (Phase 19, backlog 999.1), `.planning/config.json`.
- Scratch probe (not committed; files in `/tmp/probe19`, deleted with the OS tmp): `Emu.java` reproduced the connector's describe loop with the repo's `Column`/`Table`/`ClickHouseWriter` classes against live DESCRIBE rows, applied the pairing prototype, serialized three records for the twin tables and inserted them.

### Secondary (MEDIUM confidence)
- Web search for an upstream fix of the Enum/Tuple nullability: nothing found; upstream `v1.7.0` is still the latest tag via `git ls-remote upstream`.

### Tertiary (LOW confidence)
- None used for recommendations.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - `ClickHouseColumn.of` run against the shapes DESCRIBE produces; no new dependency
- Architecture: HIGH for "writer unchanged, fix `isNullable`"; MEDIUM for the choice between approaches C and A (gate decides)
- Pitfalls: HIGH - literal-`\n`, single-field coincidence, all-`Nullable` sub-columns and nested-tuple wrapper were each reproduced live

**Research date:** 2026-10-12
**Valid until:** 2026-10-19 for the live-server facts (floating `latest`; record `SELECT version()` at each gate); the code-reading facts hold until the next change to `Column.java`, `ClickHouseHelperClient.java` or `ClickHouseWriter.java`.
