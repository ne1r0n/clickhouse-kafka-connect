# Phase 19 Tuple Spike: go/no-go bundle

This bundle records what the live Tuple spike measured for COLS-09, so the Tuple go/no-go can be decided without opening the spike code. The measurements come from `NullableTupleMatrixLiveTest` (commits `fc44b1f6` for the first cell and the harness, `5d4424c7` for the rest of the matrix), run against ClickHouse `latest` with client V2. Every cell runs on two twin tables and on two sides. Side (a) puts the cell's records through the real `ClickHouseSinkTask.put` against the table as DESCRIBE describes it today. Side (b) serializes the same records with the unchanged `ClickHouseWriter` over a hand-built table whose columns carry the nullability the DDL declares, inserts the captured bytes, and reads the row back with typed SQL. The answer is taken from the user directly at the 19-03 gate (D-01); nothing in this file decides it.

## Server

**Server version:** 26.9.17.8

Run date (UTC): 2026-10-12. Planning saw 26.9.16.11, so the floating `latest` image moved between planning and execution; the matrix below is from 26.9.17.8. No server setting was set in any DDL or query: `Nullable(Tuple(...))` was accepted as it stands.

## Matrix: plain twin (RowBinary)

The plain twin has no DEFAULT column, so DESCRIBE reports no default and the writer takes RowBinary. A failed put leaves no row behind in every "fails" entry.

| Cell | Shape | Row | Side (a): DESCRIBE-derived today | Side (b): declared nullability |
|------|-------|-----|----------------------------------|--------------------------------|
| C1 | Nullable(Tuple(a Int32, b String)) | id 1 {a, b} | fails: server CANNOT_READ_ALL_DATA (Bytes read 2, expected 4) | round-trips |
| C1 | Nullable(Tuple(a Int32, b String)) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips |
| C2 | Nullable(Tuple(a Nullable(Int32), b String)) | id 1 {a, b} | stores wrong: tuple not NULL, a read back as NULL, b as the empty string, plus one phantom row under an id no record carries; no failure | round-trips |
| C2 | Nullable(Tuple(a Nullable(Int32), b String)) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips |
| C2 | Nullable(Tuple(a Nullable(Int32), b String)) | id 3 {a NULL, b} | fails: server CANNOT_READ_ALL_DATA (Bytes read 3, expected 4) | round-trips |
| C3 | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | id 1 [tuple, NULL, tuple with b NULL] | fails in the writer: column 'c' (Array(Tuple(...))) element: null in non-nullable slot | round-trips, neighbours of the NULL element intact |
| C3 | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | id 2 empty array | round-trips | round-trips |
| C3 | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | id 3 [NULL] | fails in the writer: column 'c' (Array(Tuple(...))) element: null in non-nullable slot | round-trips |
| X1 (informational) | Nullable(Tuple(a Int32)) | id 1 {a} | round-trips (single-field coincidence) | round-trips |
| X1 (informational) | Nullable(Tuple(a Int32)) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips |
| X1 (informational) | Nullable(Tuple(a Int32)) | id 3 {a NULL} | stores wrong: a NULL tuple is stored silently | refused by the declared writer before any insert (DataException naming tuple field 'a', declared Int32) |
| X2 (informational) | Map(String, Nullable(Tuple(a Int32, b String))) | id 1 {k1: tuple, k2: NULL} | fails in the writer: column 'c' (Map(STRING, Tuple(...))) map value: null in non-nullable slot | round-trips |
| X2 (informational) | Map(String, Nullable(Tuple(a Int32, b String))) | id 2 empty map | round-trips | round-trips |
| X3 (informational) | Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))) | id 1 {a, n: {d, e NULL}} | fails: server CANNOT_READ_ALL_DATA (Bytes read 2, expected 4) | round-trips |
| X3 (informational) | Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips |

## Matrix: DEFAULT twin (RowBinaryWithDefaults)

The DEFAULT twin adds an unrelated column `d UInt32 DEFAULT 42`, so DESCRIBE reports a default and the writer takes RowBinaryWithDefaults. Every round-trip on this twin also reads `d` back as 42.

| Cell | Shape | Row | Side (a): DESCRIBE-derived today | Side (b): declared nullability |
|------|-------|-----|----------------------------------|--------------------------------|
| C1 | Nullable(Tuple(a Int32, b String)) | id 1 {a, b} | fails: server CANNOT_READ_ALL_DATA (Bytes read 0, expected 1) | round-trips (d = 42) |
| C1 | Nullable(Tuple(a Int32, b String)) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips (d = 42) |
| C2 | Nullable(Tuple(a Nullable(Int32), b String)) | id 1 {a, b} | fails: server CANNOT_READ_ALL_DATA (Bytes read 0, expected 1) | round-trips (d = 42) |
| C2 | Nullable(Tuple(a Nullable(Int32), b String)) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips (d = 42) |
| C2 | Nullable(Tuple(a Nullable(Int32), b String)) | id 3 {a NULL, b} | fails: server CANNOT_READ_ALL_DATA (Bytes read 3, expected 4) | round-trips (d = 42) |
| C3 | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | id 1 [tuple, NULL, tuple with b NULL] | fails in the writer: column 'c' (Array(Tuple(...))) element: null in non-nullable slot | round-trips (d = 42), neighbours of the NULL element intact |
| C3 | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | id 2 empty array | round-trips (d = 42) | round-trips (d = 42) |
| C3 | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | id 3 [NULL] | fails in the writer: column 'c' (Array(Tuple(...))) element: null in non-nullable slot | round-trips (d = 42) |
| X1 (informational) | Nullable(Tuple(a Int32)) | id 1 {a} | round-trips (single-field coincidence) | round-trips (d = 42) |
| X1 (informational) | Nullable(Tuple(a Int32)) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips (d = 42) |
| X1 (informational) | Nullable(Tuple(a Int32)) | id 3 {a NULL} | stores wrong: a NULL tuple is stored silently | refused by the declared writer before any insert (DataException naming tuple field 'a', declared Int32) |
| X2 (informational) | Map(String, Nullable(Tuple(a Int32, b String))) | id 1 {k1: tuple, k2: NULL} | fails in the writer: column 'c' (Map(STRING, Tuple(...))) map value: null in non-nullable slot | round-trips (d = 42) |
| X2 (informational) | Map(String, Nullable(Tuple(a Int32, b String))) | id 2 empty map | round-trips | round-trips (d = 42) |
| X3 (informational) | Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))) | id 1 {a, n: {d, e NULL}} | fails: server CANNOT_READ_ALL_DATA (Bytes read 0, expected 1) | round-trips (d = 42) |
| X3 (informational) | Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))) | id 2 NULL tuple | fails in the writer: An attempt to write null into not nullable column 'c' | round-trips (d = 42) |

How to read the two sides: side (a) is the connector as shipped, so it shows the defect. The one dangerous entry is C2 id 1 on the plain twin, which does not fail at all: the bytes misalign, the row is stored wrong, and a second phantom row appears. Side (b) uses the same writer code with only the element nullability corrected, so a column of `round-trips` says the fix is a nullability fix and the write branches do not need to change.

## Control

Side (b) has teeth: C1 serialized with both tuple fields marked nullable (the double flag DESCRIBE produces today, because it reports every tuple sub-column as `Nullable(...)`) is rejected by the server on both twins and leaves no row. Plain twin: insert failed as expected, no row. DEFAULT twin: insert failed as expected, no row.

## DESCRIBE rows (metadata only)

Raw DESCRIBE rows (name, raw type, subcolumn flag) as logged by the class for both twins of each cell. The sub-column types under a Nullable parent (`c.a`, `c.b`) are `Nullable(...)` although the DDL declares them plain, which is the double flag side (b) corrects. No record values appear here.

C1:

```text
NULLABLE_TUPLE_MATRIX describe c1/PLAIN id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c1/PLAIN c | Nullable(Tuple(a Int32, b String)) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c1/PLAIN c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/PLAIN c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/PLAIN c.b | Nullable(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/PLAIN c.b.size | Nullable(UInt64) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT c | Nullable(Tuple(a Int32, b String)) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT d | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT c.b | Nullable(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c1/DEFAULT c.b.size | Nullable(UInt64) | subcolumn=true
```

C2:

```text
NULLABLE_TUPLE_MATRIX describe c2/PLAIN id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c2/PLAIN c | Nullable(Tuple(a Nullable(Int32), b String)) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c2/PLAIN c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/PLAIN c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/PLAIN c.a.null | Nullable(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/PLAIN c.b | Nullable(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/PLAIN c.b.size | Nullable(UInt64) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT c | Nullable(Tuple(a Nullable(Int32), b String)) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT d | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT c.a.null | Nullable(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT c.b | Nullable(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c2/DEFAULT c.b.size | Nullable(UInt64) | subcolumn=true
```

C3:

```text
NULLABLE_TUPLE_MATRIX describe c3/PLAIN id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c.size0 | UInt64 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c.null | Array(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c.a | Array(Nullable(Int32)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c.b | Array(Nullable(String)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c.b.null | Array(Nullable(UInt8)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/PLAIN c.b.size | Array(Nullable(UInt64)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c | Array(Nullable(Tuple(a Int32, b Nullable(String)))) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT d | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c.size0 | UInt64 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c.null | Array(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c.a | Array(Nullable(Int32)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c.b | Array(Nullable(String)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c.b.null | Array(Nullable(UInt8)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe c3/DEFAULT c.b.size | Array(Nullable(UInt64)) | subcolumn=true
```

X1 (informational):

```text
NULLABLE_TUPLE_MATRIX describe x1/PLAIN id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x1/PLAIN c | Nullable(Tuple(a Int32)) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x1/PLAIN c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x1/PLAIN c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x1/DEFAULT id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x1/DEFAULT c | Nullable(Tuple(a Int32)) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x1/DEFAULT d | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x1/DEFAULT c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x1/DEFAULT c.a | Nullable(Int32) | subcolumn=true
```

X2 (informational):

```text
NULLABLE_TUPLE_MATRIX describe x2/PLAIN id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c | Map(String, Nullable(Tuple(a Int32, b String))) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.size0 | UInt64 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.keys | Array(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.keys.size | Array(UInt64) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.values | Array(Nullable(Tuple(a Int32, b String))) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.values.null | Array(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.values.a | Array(Nullable(Int32)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.values.b | Array(Nullable(String)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/PLAIN c.values.b.size | Array(Nullable(UInt64)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c | Map(String, Nullable(Tuple(a Int32, b String))) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT d | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.size0 | UInt64 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.keys | Array(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.keys.size | Array(UInt64) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.values | Array(Nullable(Tuple(a Int32, b String))) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.values.null | Array(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.values.a | Array(Nullable(Int32)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.values.b | Array(Nullable(String)) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x2/DEFAULT c.values.b.size | Array(Nullable(UInt64)) | subcolumn=true
```

X3 (informational):

```text
NULLABLE_TUPLE_MATRIX describe x3/PLAIN id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c | Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.n | Nullable(Tuple(d String, e Nullable(Int32))) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.n.d | Nullable(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.n.d.size | Nullable(UInt64) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.n.e | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/PLAIN c.n.e.null | Nullable(UInt8) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT id | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c | Nullable(Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))) | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT d | UInt32 | subcolumn=false
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.null | UInt8 | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.a | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.n | Nullable(Tuple(d String, e Nullable(Int32))) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.n.d | Nullable(String) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.n.d.size | Nullable(UInt64) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.n.e | Nullable(Int32) | subcolumn=true
NULLABLE_TUPLE_MATRIX describe x3/DEFAULT c.n.e.null | Nullable(UInt8) | subcolumn=true
```

## Parsing approach (proposed)

Approach C. Once per root column per DESCRIBE, after `new Table(...)` has built the column tree (`Table.java` stays untouched), and only for root columns whose `Column` tree contains a TUPLE: parse the root's raw type with `ClickHouseColumn.of` after replacing the two-character backslash-n sequence with a space, pair the `Column` tree with the parsed tree, and overwrite `isNullable` on tuple roots, tuple fields and nested tuples. Pairing rules: TUPLE fields match by element name, or by 1-based position for unnamed tuples; an ARRAY element pairs with nested column 0; a MAP value pairs with nested column 1. It runs per root column per DESCRIBE, never per record, so the write path is not touched.

The approach test ran on the DESCRIBE form exactly as `describeTableV2` reads it (`fromJsonRow`, which turns the server's escaped newline into a literal backslash plus n inside pretty-printed non-nullable tuples). Every root parsed after normalisation and every node's `isNullable()` equalled the DDL: the six matrix roots C1, C2, C3, X1, X2, X3; the plain multi-line tuple `Tuple(a Int32, b String)`; a nested multi-line `Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))`; the unnamed `Nullable(Tuple(Int32, Nullable(String)))` (elements matched by position 1 and 2); and the backtick-named `Nullable(Tuple(`a b` Int32, `c,d` Nullable(String)))` (a name holding a space and a name holding a comma). The two non-nullable roots reached the parser in the escaped multi-line form, which `ClickHouseColumn.of` cannot parse before normalisation.

**Approach test:** pass

Alternative, approach A: each Tuple `Column` parses its own raw string inside `extractColumn`, stores the declared element nullability, and a `tupleFields` list subclass stamps the children when `Table` calls `add`. It reaches the same result and stays inside `Column.java`, with no hook in the helper client. Its trade-off is that it relies on `Table.updateParent` calling `parent.getTupleFields().add(child)` and on every other mutator of that list staying unused, which is hidden coupling to an upstream-identical file; approach C depends only on public getters and one explicit call after the table is built.

## Failure policy (proposed)

Fail open per root column: when the declared type does not parse, or the two trees do not pair, keep DESCRIBE's nullability for that root column and log one WARN naming the column and its raw type (metadata only, no values). The consequence is that an exotic type the parser does not know behaves exactly as it does today and the table stays usable. Fail closed (the table becomes unusable) is the alternative; its consequence is that one exotic column would stop every topic mapped to that table, in exchange for never running with a nullability the pass could not confirm.

## Estimated diff shape

- `Column.java`: a package-level setter on `isNullable` plus `.isNullable(isNull)` on the Tuple branch builder call.
- New fork-only `DeclaredNullability.java` (the pairing walk), roughly 80-120 lines.
- `ClickHouseHelperClient.java`: the nullable Enum/Tuple WARN block replaced by one Table-building helper, with one line swapped in each describe loop.
- `ClickHouseWriter.java`: one stale comment only; no write branch changes (side (b) shows none are needed).
- `Table.java`: untouched.
- Test flips as listed in RESEARCH "Pin Inventory and Fate": the Array-of-Nullable-Tuple pins and the live null-element pin flip to round-trips with a written rationale, the WARN pins move to negative forms, and this class's side (a) expectations flip to round-trips and become the COLS-10 coverage.

## Recommendation

**Recommendation:** GO

Every deciding cell (C1, C2, C3) round-trips on side (b) on both twins and the approach test passed; side (a) shows the current mapping fails or silently corrupts on every non-trivial row, including a plain-twin C2 row that stores wrong data and a phantom row without any error.

Extras never veto (D-07), and none failed on side (b). Edges FORK-DELTA must state once the fix lands: X1 `Nullable(Tuple(a Int32))` with a NULL `a` is silently stored as a NULL tuple today and becomes a writer `DataException` after the fix (the declared element is not nullable, so the refusal is the correct behaviour, but it is a visible change); and a record that omits a `Nullable(Tuple)` field will pass schema validation and write NULL, as noted in RESEARCH.

## Gate refresh

Re-run for the 19-03 gate, after the Enum fix (19-02: commits `598f2caf`, `a8501693`, `108f8a95`), as the D-04 rule requires ("record `SELECT version()` at each gate").

**Gate server version:** 26.9.17.8

Run date (UTC): 2026-10-12, 21:00. Image `clickhouse/clickhouse-server:latest`, digest `sha256:387ebbcca253026def04dc79e83d2323ac7d97bf309ddd090a8a3c702573e01a`; `docker pull` immediately after the run reported the local image up to date, so this is the registry's `latest` on the gate day. The version is the one `NULLABLE_TUPLE_MATRIX server version=` logged in the run itself. It matches the 19-01 measurement (26.9.17.8), so the floating `latest` did not move between 19-01 and the gate.

Class result: `NullableTupleMatrixLiveTest` 28 tests, 0 failures, 0 errors, 0 skipped (client V2). Every side (a) pin held as written, so no matrix row changed and no pinned expectation was edited. Production state at the gate: `git diff --name-only ede0a45d HEAD -- src/main` names only `Column.java` and `ClickHouseHelperClient.java` (the Enum fix), and the Tuple branch of `Column.extractColumn` still builds without the nullability call.

| Cell | Role | Plain twin, side (a) | Plain twin, side (b) | DEFAULT twin, side (a) | DEFAULT twin, side (b) |
|------|------|----------------------|----------------------|------------------------|------------------------|
| C1 | deciding | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 |
| C2 | deciding | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 |
| C3 | deciding | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 |
| X1 | extra (informational) | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 |
| X2 | extra (informational) | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 |
| X3 | extra (informational) | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 | unchanged since 19-01 |

The double-flag control and the approach test (`declaredTupleTypesParseWithDdlNullability`) also passed in this run, so `**Approach test:**` stays `pass`. Every deciding side (b) cell still round-trips on both twins, so the recommendation below is unchanged: GO.

## Decision

Pending: the user answers at the 19-03 gate.
