# Feature Research

**Domain:** Discovery (DESCRIBE TABLE) correctness, writer error hygiene and operator rollout for a production ClickHouse Kafka Connect sink fork (milestone `v1.7.0_gg4-hardening`)
**Researched:** 2026-10-21
**Confidence:** HIGH for what ClickHouse `latest` emits and for the pairing/escaping behaviour (measured live in this research); MEDIUM for upstream's direction (read from GitHub pages) and for the rollout consequences (inferred by analogy with measured gg3 behaviour); LOW for the Kafka 4.4.0 release content (release-note titles only).

Scope: only the NEW behaviours of this milestone. Existing features (SMTs, `targetTableFilter`, `Type.DYNAMIC`, metadata-free JSON route, JMX naming, exactly-once state machine, `DeclaredNullability`, COLS-14 WARN, `WithheldMessageException`) are dependencies, not subjects.

## Evidence legend

Every factual claim below carries one tag.

| Tag | Meaning |
|-----|---------|
| **[M]** | Measured in this research on 2026-10-21. Server: `clickhouse/clickhouse-server:latest`, image `bdb5c911a23d` (the same image as the gg3 evidence), version `26.9.19.9`, run with `clickhouse local` in a throwaway `docker run --rm` container (nothing written to the repo). Client-side parsing: `clickhouse-data-0.9.6` `ClickHouseColumn.of`, Jackson `2.22.3`, and the repo's compiled `build/classes` (`DeclaredNullability.normalise`, `Column.extractColumn`, built after the last source change). Harness code lives in `/tmp/gsd24-research`, outside the repo. |
| **[E]** | Archived gg3 evidence (`23-EVIDENCE.md` R-01/R-02, `24-COMMAND-EVIDENCE.md` E-15/E-16, `ROLLOUT-v1.7.0_gg3.md`), measured through the real sink task. |
| **[D]** | ClickHouse documentation page (URL in Sources), cross-checked against [M] where possible. |
| **[U]** | Upstream `ClickHouse/clickhouse-kafka-connect` GitHub page, read 2026-10-21. |
| **[I]** | Inference from code reading or analogy. Not measured. Needs a live pin before it is stated to operators as fact. |

---

## 1. Reference facts the requirements depend on

### 1.1 What DESCRIBE gives a rule to work with

`DESCRIBE TABLE ... SETTINGS describe_include_subcolumns=1 FORMAT JSONEachRow` returns exactly eight keys per row: `name`, `type`, `default_type`, `default_expression`, `comment`, `codec_expression`, `ttl_expression`, `is_subcolumn` **[M]**, **[D]** (the DESCRIBE page lists the same eight; `is_subcolumn` is "a flag that is 1 for internal subcolumns" and exists only with the setting on). There is **no field that says whether a sub-column is structural or a declared Tuple element**. `is_subcolumn=1` is true for both. A rule can only use: the row's `name`, the row's `type`, and the type of the parent row (which always precedes its children in the output **[M]**).

Parent rows precede children in every table measured (several dozen tables across all the wrapper combinations of 1.2) **[M]**. Row order among siblings follows the declaration, with structural offsets rows (`size0`, `sizeN`) first for an `Array` parent **[M]**; do not build a rule on that order, it is not documented.

### 1.2 Which structural rows `latest` emits **[M]**

Row names below are the part after the parent path. `K` is the 0-based array dimension. All types are the `type` field of the row.

| Parent type (declared) | Structural rows emitted | Type of the structural row |
|------------------------|-------------------------|-----------------------------|
| `Nullable(T)` (scalar T) | `.null` | `UInt8` |
| `Array(T)` | `.size0` | `UInt64` |
| `Array(Array(T))` | `.size0`, `.size1` | `UInt64`, `Array(UInt64)` (rule: `.sizeK` has type `Array^K(UInt64)`) |
| `Array(Nullable(T))` | `.size0`, `.null` | `UInt64`, `Array(UInt8)` |
| `String` | `.size` | `UInt64` |
| `Nullable(String)` | `.null`, `.size` | `UInt8`, `Nullable(UInt64)` |
| `Array(String)` | `.size0`, `.size` | `UInt64`, `Array(UInt64)` |
| `Array(Array(String))` | `.size0`, `.size1`, `.size` | `UInt64`, `Array(UInt64)`, `Array(Array(UInt64))` |
| `Array(Nullable(String))` | `.size0`, `.null`, `.size` | `UInt64`, `Array(UInt8)`, `Array(Nullable(UInt64))` |
| `Map(K, V)` | `.size0`, `.keys`, `.keys.size` (for String keys), `.values` | `UInt64`, `Array(K)`, `Array(UInt64)`, `Array(V)` |
| `Map(K, Array(V))` | as Map, plus `.values.size1` | `Array(UInt64)` |
| `Map(K, Tuple(...))` | as Map; `.values` is `Array(Tuple(...))` and its elements follow as `.values.<element>` | |
| `Tuple(...)` (named or not) | no structural row of its own. Elements: `.<name>` or `.1`, `.2`. Each element then gets its own structural rows by its own type | |
| `Nullable(Tuple(...))` | `.null` only; every element row is reported `Nullable(...)` even when declared plain (this is why `DeclaredNullability` exists) | `UInt8` |
| `Array(Tuple(...))` | `.size0` (offsets); elements `.<name>` typed `Array^D(elementType)` | `UInt64` |
| `Variant(T1, T2)` | `.<TypeName>` rows (`v.String`, `v.UInt64`) plus the structural rows of each member (`v.String.null`, `v.String.size`, `v.Array(UInt8).size0`) | |
| `LowCardinality(T)`, `LowCardinality(Nullable(T))`, `FixedString(N)`, `Enum`, `DateTime64` | **no** sub-columns at all | |
| `JSON`, `JSON(a Tuple(...), b UInt8)` | typed paths appear as `j.a`, `j.a.size`, `j.a.v`, `j.a.v.size`, `j.b`; an untyped `JSON` and an empty `Dynamic` show none | |
| `Nested(x T, size U)` (default `flatten_nested=1`) | no `Nested` row. The members are **root rows** (`is_subcolumn=0`) named `n.x`, `n.size`, each `Array(...)`, with their own structural rows (`n.size.size0`) | |

Two facts matter for the fix:

1. **Bare `.size` is the String-length sub-column, not an Array size.** It is `UInt64` wrapped in the same `Nullable`/`Array` layers as its parent. It is **not documented**: the ClickHouse `String`, `Array`, `Nullable` and `Map` pages describe `size0`/`sizeN`, `null`, `keys`, `values` only **[D]**; the `.size` row was seen live **[M]** and in gg3 R-01 **[E]**. A rule must therefore tolerate structural rows it has never heard of (never require a structural row to exist) and must not enumerate structural names as a closed set.
2. **`.null` is always structural.** The server refuses a Tuple element named `null` in every wrapper (see 1.4), so no declared element can collide with it **[M]**, **[E]**.

### 1.3 The collision that decides the rule **[M]**

A declared Tuple element named `size0`/`sizeN` under an `Array` produces **two DESCRIBE rows with the same `name`**: the structural offsets row first, the declared element second. Types differ.

| Declared | Rows (name : type) |
|----------|---------------------|
| `e Array(Tuple(size0 UInt8, v String))` | `e.size0 : UInt64` (structural), `e.size0 : Array(UInt8)` (declared), `e.v`, `e.v.size : Array(UInt64)` |
| `i Array(Array(Tuple(size1 UInt8, size0 UInt8)))` | `i.size0 : UInt64`, `i.size1 : Array(UInt64)` (both structural), then `i.size1 : Array(Array(UInt8))`, `i.size0 : Array(Array(UInt8))` (both declared) |
| `c Array(Array(Tuple(size0 UInt64, size1 Array(UInt64), sz String)))` | structural `c.size0`, `c.size1`; declared `c.size0 : Array(Array(UInt64))`, `c.size1 : Array(Array(Array(UInt64)))`; and `c.size1.size2 : Array(Array(UInt64))`, a structural row that hangs off a declared element |
| `e Array(Tuple(size0 String, size String))` | `e.size0 : UInt64` (structural), `e.size0 : Array(String)`, `e.size0.size : Array(UInt64)` (String length of the element), `e.size : Array(String)`, `e.size.size : Array(UInt64)` |
| `c Array(Map(String, Tuple(size UInt8, size0 UInt8)))` | `c.values.size : Array(Array(UInt8))`, `c.values.size0 : Array(Array(UInt8))` (declared; no structural twin because Map values carry no offsets row of their own) |

So **name alone cannot decide, and for bare `.size` the row's own type cannot decide either** (`Array(Tuple(size UInt64, ...))` yields `e.size : Array(UInt64)`, byte-identical to the structural row of an `Array(String)` named `e`). What does decide, in every case measured:

- **bare `size`:** the parent. A parent whose innermost non-wrapper type is a Tuple holds a declared element; any other parent (String, Array of scalars, Map, Variant member) holds the String-length row.
- **`size0`/`sizeN` under an Array-of-Tuple parent of array depth `D`:** the row is structural exactly when `N < D` and its `type` equals `Array^N(UInt64)`. A declared element of dimension depth `D` always has type `Array^D(T)`, and `Array^D(T) = Array^N(UInt64)` with `D > N` is impossible, so the two never coincide.
- **`size0`/`sizeN` under a non-Tuple parent** (scalar array, Map, Variant member): structural.

**Prototype result [M]:** this rule (parent inner type + parent array depth + own `type`), written outside the repo over type strings, classified all **75** `size`/`sizeN`/`null`-suffixed sub-column rows of 21 tables correctly against hand-labelled DDL ground truth. The tables were the 13 affected and the look-alike shapes of gg3 E-15, plus `Array(Array(Tuple(size0, size1)))`, `Tuple(size String, size0 Nullable(String))`, `Array(Tuple(size0 String, size String))`, `Array(Map(String, Tuple(size, size0)))` and Nested. The prototype was not the production code, and an emulation of today's rule is not a measurement (for Map values it overstates what is dropped, which `24-COMMAND-EVIDENCE.md` E-15 shows is not dropped), so no "before" count is claimed here.

### 1.4 Does the server accept the element names? **[M]** (and [E] for `null`)

| Element name | `Tuple(x)` | `Nullable(Tuple(x))` | `Array(Tuple(x))` | `Map(String, Tuple(x))` |
|--------------|-----------|----------------------|-------------------|-------------------------|
| `size`, `size0`, `size1`, `size12` | accepted | accepted | accepted | accepted |
| `keys`, `values` | accepted | accepted | accepted | accepted |
| `null` | **rejected, code 36** `BAD_ARGUMENTS`: "Tuple element name 'null' is reserved because it would conflict with the subcolumn name used for Nullable null maps if such a tuple is wrapped in Nullable" | rejected | rejected | rejected |
| `NULL`, `Null` | accepted (the check is case-sensitive) | | | |
| two elements of the same name | rejected, code 15 `DUPLICATE_COLUMN` | | | |
| `` `a.b` `` (dot in a name) | accepted. Out of scope but adjacent: `Table` and the writer split names on the last dot | | | |

The message text is not documented on the Tuple page **[D]**; it was measured here and in gg3 R-01 **[E]**. Consequence for the fix: only `size`, `size0` and the `sizeN` family can collide with structural rows; `null`, `keys` and `values` need no special handling beyond keeping them un-dropped. `keys`/`values` as Tuple elements are not touched by the structural rule (they are not in its family) but belong in the live matrix as look-alikes, because `Table.updateParent` routes `.keys`/`.values` specially for `Map` parents **[I]**.

### 1.5 The two escaping layers and what the doubling does **[M]**

`name` is the raw identifier with one layer of JSON escaping. The `type` string carries **two** layers for any quoted element name: the SQL-identifier layer ClickHouse writes into the type text, then JSON on top. JSON output escapes `\`, `"`, control characters (`\n`, `\t`, `\u0001`) and, by default, `/` as `\/` (`output_format_json_escape_forward_slashes = 1`, "Don't confuse with backslashes that are always escaped") **[M]**.

`fromJsonRow` doubles every backslash before Jackson parses, which leaves the JSON escapes as literal text in `name` and `type`. Measured per character class, for a Tuple element of that name (the server name is shown in words):

| Character in the name | Column name today (kept JSON-escape text) | Pairs with the declared type today? | Column name if decoded normally | Pairs if decoded and nothing else changes? |
|-----------------------|--------------------------------------------|--------------------------------------|----------------------------------|---------------------------------------------|
| backslash | two backslashes | yes | one backslash | yes |
| backslash then `n` (two characters) | two backslashes + `n` | yes | one backslash + `n` | yes |
| line feed | backslash + `n` | yes (gg3 COLS-13) | real line feed | **no**: `ClickHouseColumn.of` turns SQL-escaped `\n` into plain `n` (`lfnb`) |
| tab | backslash + `t` | yes | real tab | **no** (`tabtb`) |
| carriage return | backslash + `r` | yes | real CR | **no** |
| double quote | **JSON parse error: the whole table is unmapped** | n/a | `"` | yes |
| slash | backslash + `/` | **no**: the client parser reads `\/` as `/` | `/` | yes |
| control byte `0x01` | six characters `\u0001` | **no** | the control byte | yes |
| backtick | `` ` `` | **no**: `ClickHouseColumn.of` throws `IllegalArgumentException` | `` ` `` | yes |
| trailing backslash, space, single quote, non-ASCII | unchanged / doubled backslash | yes | natural | yes |

The "today" column reproduces the gg3 rollout table (item 3) character for character, which validates the harness **[E]**. The last column is the new finding: **decoding names without touching `DeclaredNullability` fixes `/`, `"`, backtick and control bytes but regresses line feed, tab and carriage return in `Nullable(Tuple)` element names.** gg3's COLS-13 live twins pin exactly that line-feed case, so a naive decode turns them red.

Enum label keys are a second consumer of the `type` text. `Column.extractEnumValues` uses the label text between the quotes as the lookup key. Measured keys for a label (raw label first, then key today, then key if decoded) **[M]**:

| Raw label | Key today | Key if decoded |
|-----------|-----------|----------------|
| `a\b` (one backslash) | four backslashes | two backslashes (the DDL literal) |
| line feed | backslash-backslash-`n`, i.e. two backslashes + `n` | one backslash + `n` (the DDL literal) |
| `sl/ash` | `sl\/ash` | `sl/ash` (the raw label) |
| `q'uote` | two backslashes + quote | one backslash + quote (the DDL literal) |
| `dq"x` | JSON parse error, table unmapped | `dq"x` |

Neither form equals the raw label for backslash, quote or line feed, so those labels are effectively unusable today and stay awkward after the decode; only `/` and `"` become natural. Treat enum-label unescaping as a separate decision (see 2.2 anti-features).

### 1.6 Why the doubling exists, and what upstream is doing **[U]**

- PR #529 "Adjust JSON to escape backslash" (merged 2025-05-14, closes #518) added the doubling. #518's reproduction is `COMMENT '\\'` failing in `describeTableV1` with `JsonParseException`. Per #838, the V1 path reads the response through `response.records()`, which applies TSV unescaping, so the JSON line arrives already unescaped and is invalid JSON. The doubling repaired the V1 line by breaking the V2 line, which is raw JSON and needs no help **[U]**.
- Issue #838 (open, filed 2026-09-23): "Column names containing / cause silent field loss with both client versions". Root cause as stated there: V2 `fromJsonRow` doubles backslashes so `a\/b` becomes a name with a literal backslash, and `cleanupExtraFields` then drops the `a/b` field. Reproduced on ClickHouse 26.8.6.5 and Cloud 26.4. No maintainer reply on the page **[U]**.
- PR #839 (open, ready for review, rebased 2026-09-30) is the fix: `fromJsonRow` becomes `OBJECT_MAPPER.readValue(json.replace("\n", "\\n"), ...)` (the doubling removed, nothing else in that file), `describeTableV1` reads raw lines from `response.getInputStream()` instead of `records()`, both readers are pinned to UTF-8, with `FieldDescriptorEscapeTest` and `SlashColumnRoundTripTest` added. It keeps the #529 comment regression green. Its CI was not fully green when read (a 25.2 cluster job failed an existing assertion) **[U]**. It does not touch anything `DeclaredNullability`-shaped; upstream has no such class.

The fork's `fromJsonRow` is byte-identical to upstream's today (`git show upstream/main:...ClickHouseFieldDescriptor.java`), including the `\n` replacement order that the fork already corrected in review (`9cb2d382`).

### 1.7 Jackson 2.22 exception hierarchy (for 22-REVIEW WR-01) **[M]** (javap on 2.22.3 jars) and **[U]**

```
java.io.IOException
 └─ JacksonException                       (jackson-core, since 2.12: "Base class for all Jackson-produced checked exceptions")
     └─ JsonProcessingException
         ├─ StreamReadException (abstract)  ─ JsonParseException, InputCoercionException
         ├─ StreamWriteException (abstract) ─ JsonGenerationException
         └─ DatabindException (abstract)
             └─ JsonMappingException       ─ MismatchedInputException ─ InvalidFormatException; InvalidDefinitionException
```

So every Jackson-produced checked exception, not only `JsonProcessingException`, is an `IOException`. Retriable I/O does **not** arrive as a Jackson exception: `StdSerializer.wrapAndThrow` rethrows an `IOException` that is not a `JacksonException` unchanged and only wraps `JacksonException`s and runtime failures into `JsonMappingException` with the "through reference chain" path **[U]**. A socket or timeout failure while writing to the `OutputStream` therefore reaches the writer as a plain `java.io.IOException` (or `SocketTimeoutException`, `ConnectException`...), which the fix must keep passing through. No Jackson 2.22 type signals a retriable transport failure; `JacksonIOException` (an unchecked wrapper) exists only in Jackson 3 (`tools.jackson`), not in the 2.22.3 on the classpath **[I]**.

---

## 2. Feature Landscape

Four feature groups. "Complexity" is for the implementing phase, not the research.

### 2.1 Backlog 999.4 — keep a declared Tuple element named `size`/`size0`/`sizeN`, still drop structural rows

#### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| A classifier that separates declared elements from structural rows using **parent inner type, parent array depth and the row's own `type`** (section 1.3), not names alone | Only a rule of this shape is correct on all 75 measured rows. The backlog sketch ("parent is a Tuple") is insufficient: it already says so for `Array(Tuple)`. | MEDIUM | Lives in `shouldSkipDescribeRow`, which already receives `columnsSoFar` (the kept `Column`s by name) and the `ClickHouseFieldDescriptor` (so the row's `type`). Parent array depth is countable from the kept parent `Column` (`ARRAY` → `getArrayType()` chain) and `Nullable` is a flag, not a wrapper. |
| **Both drop sites change**: `ClickHouseHelperClient.shouldSkipDescribeRow` (bare `.size`, `.size0`, `.null`) **and** `Table.updateParent`'s `ARRAY` branch (`SIZE_FIELD_MATCHER` = `.+\.size[0-9]+$`, which drops declared `size1`, `size12`... under an `Array`) | The gg3 note and FORK-DELTA item 10 already say `size<digits>` under an Array is affected, and three of the four shapes that were inserted and failed are `Array(Tuple(size ...))`/`Array(Tuple(size1 ...))`/`Array(Tuple(v String, size1 ...))` **[E]**. The ROADMAP 999.4 goal text names only `size`/`size0` and the first site. Fixing only that leaves `Array(Tuple(size1 ...))` broken. | MEDIUM | `Table.java` is upstream-region: FORK-DELTA row, anchors row, and a decision on where the single classifier lives. Cleanest: `shouldSkipDescribeRow` becomes the only place that decides, and drops every structural `.sizeN` before `Table` sees it; `Table`'s matcher then narrows to "child is not a declared element" or the structural signature (`UInt64` for N=0, `Array^N(UInt64)` for N>0), so hand-built `TableTest` columns keep working. |
| **Do not add a structural row's name to `skippedCols` when a declared element of the same name can follow** | Measured: `Array(Tuple(size0 Tuple(a UInt8), ...))` emits `e.size0 : UInt64`, then `e.size0 : Array(Tuple(a UInt8))`, then `e.size0.a`. The loop's parent check (`skippedCols.contains(parentName)`) would drop `e.size0.a` because the structural twin put `e.size0` in the set, reproducing the short-tuple defect one level down. | LOW | Today a dropped `c.size` also cascades and drops `c.size.a`, `c.size.b` (a nested Tuple named `size`) **[I]**; keeping the parent fixes that, but only if the structural twin is not remembered by name. |
| Keep `.null` always dropped | The server forbids a Tuple element named `null` in every wrapper (code 36) **[M]**, **[E]**, so there is no collision to resolve. | LOW | One-line comment citing the code-36 message so a later reader does not "fix" it. |
| Unknown parent falls back to today's behaviour (drop) | Rows under `JSON`/`Dynamic` (DESC-05), and any row whose parent was itself skipped, have no kept parent. Dropping them is the current, tested behaviour. | LOW | Check the ancestor-prefix rule (DESC-05) **first**, so a typed JSON path such as `jt.a.size` stays skipped exactly as now **[M]** (`jt.a` is a Tuple under a `JSON` ancestor). |
| Live round-trip pin: insert a record with the `size` element and read it back with `assertEquals`, for the shapes of gg3 E-15 (13 affected, 19 look-alikes, `s4_o01`), on ClickHouse `latest` × client V2 | Core Value of the project, and the only proof that the writer writes the full tuple. Expected behaviour: discovery keeps the element, `tupleFields` has every declared element in DESCRIBE order, the writer emits the whole tuple, the row reads back equal **[I]**. | MEDIUM | The 13 affected fixtures exist in `24-COMMAND-EVIDENCE.md` E-15 as DDL text; reuse them. Add the new shapes found here: `Array(Array(Tuple(size1, size0)))`, `Array(Tuple(size0 Tuple(a ...)))`, `Array(Tuple(size0 String, size String))`, `Array(Map(String, Tuple(size, size0)))`, `Tuple(keys, values)`. |
| DESC-05 regression check and the existing look-alike set stay green | The classifier must not start keeping `sizes`, `size_x`, `Size`, a plain column `size`, a `Nested` member named `size`, `JSON` sub-paths | LOW | `Nested(size ...)` members are root rows (`is_subcolumn=0`) and never reach the rule **[M]**. |
| Flip the D-04 pin `COLS-14 unpaired-element WARN (D-04, known defect 999.4)` (3 parameterised cases in `ClickHouseHelperClientForkUnitTest`) | It asserts the defect. After the fix the element pairs and no WARN fires. | LOW | The COLS-14 WARN code stays (generic safety net for any future drop). |
| 23-REVIEW WR-02: the pin must exercise the **production** loop, not `discoverAsDescribeTableV2`'s copy | Otherwise the flipped pin proves nothing about the shipped loop. | MEDIUM | Extract the per-row decision (or the whole loop over `Iterable<String>`) as a package-private method used by `describeTableV1`, `describeTableV2` and the test. This also removes the V1/V2 duplication, but enlarges the diff in upstream-region code; if kept small, state the duplication in the helper's Javadoc and add the live pin instead (the review's own alternative). |
| FORK-DELTA row, `fork-anchors.txt` row, CHANGELOG line, retire Known-cost item 10 | The repo's rebasability invariant (`verify-merge-invariants.sh`). | LOW | Anchors currently list `shouldSkipDescribeRow` (HLP-02). |

#### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| One shared structural-row classifier, unit-tested against a **table of real DESCRIBE rows** captured from 26.9 (section 1.2/1.3 fixtures as `.jsonl` text) | Pins the server's actual output, not our reading of it. A future ClickHouse release that adds a structural family shows up as a failing fixture refresh, not a production surprise. | MEDIUM | Docker-free, so it also runs in `testUnit`. Capture command is the `clickhouse local` one used in this research. |
| A WARN-free "unknown structural-looking row kept as an element" log at DEBUG | Makes a mis-classification visible without an operator-facing change. | LOW | Optional. |
| Make COLS-14's remaining role explicit in its Javadoc | The WARN now marks only a *future* drop, not 999.4. | LOW | |

#### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Decide by occurrence order ("first of two same-named rows is structural") | Matches every measured table | Server ordering of sibling rows is undocumented | Use the `type` signature plus parent array depth |
| Drop a row only if its name is in a closed list of structural names | Simple | `.size` was undocumented and found live; the list rots silently | Classify by parent type and `type` shape |
| "Keep every row whose parent is a Tuple" | The backlog sketch | Wrong for `Array(Tuple)`: `e.size0` (offsets) has a Tuple-typed element parent. Wrong for `Map(K, Tuple)` too. | Section 1.3 rule |
| Rename or work around the element at table level in the operator note | Cheap | The fork "has not tested one" (gg3 item 2), and a schema change on a production table is the operator's call | Fix discovery |
| Handle dotted element names (`` `a.b` ``) in this phase | Same family of "names the splitter cannot take" | A different defect (parent routing and the writer's `split("\\.")`); the server accepts them **[M]** | Log as a new backlog item |
| Reject the table when an element is unpaired | "Fail loud" | Falling back cannot repair missing bytes; gg3 D-01 already decided to log and still apply | Keep COLS-14 as is |

### 2.2 Backlog 999.5 — canonical name form for backslashes and escapes in names

**Recommended canonical form: the raw identifier as written in `CREATE TABLE`** (what Jackson returns when it parses the server's JSON as is). Reasons, strongest first:

1. It is the only form an operator or a Debezium source can know. The gg3 note calls the doubled form "a known limitation", not a feature.
2. Upstream reached the same conclusion independently and has the fix in review (#838/#839, section 1.6). Taking the same one-line change keeps the next sync conflict-free.
3. The metadata-free JSON route sends Kafka field names to the server untouched, so it already uses raw names; the RowBinary route currently needs the escaped form. Decoding removes a route-dependent naming rule **[I]**.
4. It repairs the silent loss of slash names (#838), the unmapped table for a name containing `"`, and the `Nullable(Tuple)` pairing failures for slash, backtick and control bytes (section 1.5).

#### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| `fromJsonRow` parses the server's JSON line without doubling backslashes (the #839 one-liner, kept byte-identical to upstream's) | The intended name form | LOW | Keep the existing `\n` replacement for a raw line feed in the line (V1 text path). |
| **V1 keeps working**: port #839's `describeTableV1` change (read raw lines from `response.getInputStream()` in UTF-8) | `TableTest.extractCommentV1` (upstream's #518 regression, `CLIENT_VERSION=V1`, a comment holding a backslash) runs in the default suite. Removing the doubling alone makes V1's already-unescaped line unparseable again **[U]**. | MEDIUM | Touches upstream-region `ClickHouseHelperClient` in a second place. Verify V1 on ClickHouse `latest` with a live test; the project tests V2 only elsewhere. |
| **`DeclaredNullability.normalise` gets a proper SQL-identifier unescape for quoted names** (line feed, tab, carriage return, and the other single-letter escapes the server writes inside a backtick name) so the element names the parse yields equal the decoded sub-column names | Without it a decoded line feed, tab or CR in a `Nullable(Tuple)` element name no longer pairs: the fallback WARN fires and the double nullability flag is back (code 33) **[M]**. gg3 COLS-13 fixed exactly this and its live twins would go red. | HIGH | The scan already tracks quotes and backslash runs. After the decode the type text holds one SQL-escape layer instead of two, so the "half the run" arithmetic and the lone-backslash-`n` line-break rule change: the server's own line break between elements becomes a real line feed (parses as is, measured), and inside a quoted name `\n` is the SQL escape for a line feed, which should be rewritten to the real character before `ClickHouseColumn.of`. `DeclaredNullabilityTest` (62 cases) and the `ESCAPED_NEWLINE` constant are rewritten, not just extended. |
| Live pins for **both forms** on ClickHouse `latest` × client V2: the raw name stores the value, the old doubled name no longer matches (root column: stored as `DEFAULT`; Tuple element: the record fails), for backslash, backslash+`n`, line feed, tab, slash, double quote | The roadmap line "pin both forms live" and the Core Value | MEDIUM | Reuse the gg3 fixtures `e5_e01`..`e5_e11` (E-16) and the look-alikes. Add `/` in `Nullable(Tuple)` (now stores), backtick and `"` (table now mapped). |
| 23-REVIEW WR-01: pin the escaped-quote branch of `normalise` | Carried review advisory; lands with the rewrite | LOW | The branch is rewritten by the work above, so the pin targets the new code. |
| Rollout item (section 4) | The change is observable: a Kafka field named with the doubled form stops matching | MEDIUM | |

#### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| One metadata-only WARN at discovery for a table whose names contain a character that changed form (backslash, line feed, tab, carriage return, slash, control byte), naming the column/element and the table, never a value, in the COLS-14 style (no throttling) | The silent case is the dangerous one: a root column whose Kafka field still uses the doubled form is ignored and the column's `DEFAULT` is stored with no error **[E]**. A WARN gives the operator a grep, like gg3 item 2 gave for `size`. | LOW | Fires at every describe for the life of the table, as COLS-14 does; say so in the note. Do not log the record's keys. |
| Decode `Enum` label keys with a SQL unescape so a raw label (`a\b`, `q'uote`, a label containing a line feed) matches a Kafka string value | Measured: neither today's nor the decoded key equals the raw label for those characters (section 1.5), so such an enum column cannot be written correctly | MEDIUM | Separate from the name decision. Only `/` and `"` become natural by the decode alone. Defer unless the phase has room; at minimum state in the note that labels with backslash, quote or line feed change their lookup key. |
| `Utils.escapeName` backtick-escapes backslash for `evolveTableSchema`'s `ALTER ... ADD COLUMN` | With the raw form, `getMissingColumns` compares raw Kafka field names with raw column names, so an existing `a\b` column is found. A genuinely missing field whose name has a backslash is created with a wrong identifier today (`escapeName` only strips backticks) **[I]** | LOW | Only when `auto.evolve` is on. Adjacent, optional. |

#### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Accept both the raw and the doubled form on every field lookup, permanently | "Zero-breakage upgrade" | Two names can collide (`a\\b` raw vs the doubled form of `a\b`); an extra lookup on the `ClickHouseWriter` hot path; diverges from upstream's writer; hides the problem the WARN is meant to surface | One canonical form, a WARN, and the rollout note. If the user insists, a one-release transitional alias built once at discovery (never per record) with a WARN on use |
| Decode `name` only, leave `type` as is | Smaller diff | The Tuple element names the type parse yields then differ from the sub-column names for every backslash name (`bs\b` vs `bs\\b`), pairing fails and the double flag returns **[M]** | Decode both, adapt `normalise` |
| Fix the Enum label key and the name form in one commit | Same function | Different consumers, different rollout statements, harder to bisect | Separate commits, one rollout line each |
| Silently accept `"` in names as "now works" without a writer pin | The table is now mapped | A newly mapped table reaches code that never saw such a name (JSON insert route, error messages that name columns) | Live round-trip pin for `"` and backtick before the note says they work |
| Rewrite `Table`'s `lastIndexOf('.')` routing to support dotted names in this change | Same area | Separate defect | New backlog item |

### 2.3 22-REVIEW WR-01 — keep Jackson exceptions out of the retry pass-through

#### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| In `passesThroughUnchanged`, a chain link counts as the retry-relevant `IOException` only if it is **not** a Jackson exception | On the opt-in `input_format_binary_read_json_as_string` route a `JsonMappingException` carries "(through reference chain: ...)" text that can name a Map key; the pass-through delivers it to task status, the DLQ header and the logs unsanitized (22-REVIEW WR-01; the residual is already measured by `JsonAsStringKeyResidualLiveTest`) | LOW | Use `!(t instanceof com.fasterxml.jackson.core.JacksonException)` rather than `JsonProcessingException`: `JacksonException` is the 2.12+ base of all Jackson checked exceptions, so a future 2.x subtype is covered. Both are correct for the 2.22 hierarchy (section 1.7). |
| Retry classification unchanged, pinned both ways | gg3 pinned it; the review says no retry shape depends on Jackson | LOW | Keep the existing pass-through tests for a plain `IOException`, a `SocketTimeoutException` and a wrapped `UncheckedIOException`-style chain. |
| Sentinel Map-key test: serialise a `Struct` whose Map value cannot be serialized, with a sentinel as the Map key, and assert the thrown chain, the DLQ headers and the logs carry no sentinel; and flip `JsonAsStringKeyResidualLiveTest`'s residual assertion | The review's own acceptance test; closes the "one measured exception" in the gg3 note | MEDIUM | `ThrowableTexts` (renders message, localized message, `toString`, suppressed, stack trace) already exists for this. |
| Update FORK-DELTA item 6 and the rollout sentence "on the opt-in JSON-as-string route, a Jackson reference chain can name a Map key" | The documented exception disappears | LOW | |

#### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Walk the chain once and decide per link, so a Jackson link *wrapping* a genuine transport `IOException` still passes the real cause | Preserves retry even in the unlikely wrapped case | LOW | Jackson 2 does not wrap a raw `IOException` (`wrapAndThrow`, section 1.7), so this is belt and braces. If the genuine cause is found deeper, the pass-through rethrows the *outer* Jackson exception and its path text; rethrow a value-free exception built from the cause instead. |

#### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Catch `JsonProcessingException` separately at the JSON case only | Narrow change | The generic catch is where the leak happens; a second catch drifts | Fix `passesThroughUnchanged` |
| Sanitise the Jackson message instead of excluding the type | Keeps the type visible | A message filter on free text is the pattern 999.3 retired | Exclude the type, name the class in `WithheldMessageException` |

### 2.4 Operator-facing rollout expectations

What each change means to an operator, and what the note must carry. Statements tagged [I] must be pinned (live, as gg3 did with `DedupTokenRetryLiveTest`) before the note states them as fact.

| Change | Observable effect | Detection | Clean stop? | Field renames? |
|--------|-------------------|-----------|-------------|----------------|
| **999.4 fix** | Tables with a Tuple element `size`/`size0`/`sizeN` start inserting; the COLS-14 `have no matching DESCRIBE sub-column` WARN disappears for them (it still fires for any future drop) | Pre-upgrade: the gg3 item 2 suspects query and `grep -F 'have no matching DESCRIBE sub-column'`. Post-upgrade: the grep returns nothing for those tables and a `count()` on the table rises | **Upgrade: no.** Inserts into these tables failed on the measured route (code 32/33, 0 rows) **[E]**, so a range left in `BEFORE_PROCESSING` holds no rows and the retry lands once **[I]**. **Rollback: yes, stop cleanly first.** Same mechanism as gg3 item 4: the old release rebuilds the short bytes, the server rejects them before it looks at the deduplication token, so a range that the new release already stored fails again **[I]**, by analogy with the measured gg3 case. | No. The Kafka field is already named `size` |
| 999.4 data tail | Rows stored wrongly by earlier releases (the "bytes happen to parse" case, gg3 R-01) are **not repaired** and cannot be found by the grep | None reliable. State it. | n/a | n/a |
| **999.5 fix** | A column or element whose server name holds a backslash, line feed, tab, carriage return, slash or control byte is now matched by its **natural** name; the doubled/escaped form stops matching. Slash names (`a/b`) that silently lost their field now store it. Tables with `"` or a backtick in a name become mapped | The gg3 item 3 limitation query already lists these tables (exactly the ten fixture tables, no look-alike) **[E]**; reuse it unchanged. The new discovery WARN, if built, is the log-side check | **Upgrade: no, if** the field names are changed together with the connector. A range left in flight by the old release was written with the old form; the retry by the new release has different bytes under the same `insert_deduplication_token`, so on deduplicating engines it is dropped if the earlier attempt landed (the gg2/gg3 note's statement) **[I]**. **Rollback: yes**, same reasoning, and the field names must be switched back. | **Yes, for names with a backslash, line feed, tab, CR or control byte**: producers/SMT outputs that used the doubled form must send the natural name. A root column keeps working *silently wrong* (the `DEFAULT` is stored) if you forget; an element inside a Tuple fails loudly. Slash names need **no** rename: the natural name is the new match. Untested aid: a `ReplaceField$Value` `renames` entry per field. |
| Enum labels containing backslash, quote, line feed or `/` | The lookup key changes (section 1.5). Plain labels: no change | Query `system.columns` for `Enum` types whose label text holds `\` or `/` | Same as above | Only if a producer sent the old key (nobody can have, for the backslash cases) |
| **`connect-api` 4.1.1 → 4.4.0** | Dependabot labels it `version-update:semver-minor` (PR #9), not a major. Maven Central lists `4.4.0` with metadata last updated 2026-10-20, a day before this research. The Apache release notes list only three Connect items (KAFKA-20430 new internal-topic config, KAFKA-19404 `plugin.path` docs, KAFKA-20881 a system-test classpath fix) and the 4.2/4.3 notes list no removal or deprecation of `SinkTask`, `SinkConnector`, `Transformation`, `Converter`, `ConfigDef`, `Header` or `Schema` **[U]**, titles only, **LOW** until the upgrade notes are read | **The shipped artifact does not bundle `connect-api`.** `confluentJar` is `from(clickhouseDependencies, sourceSets.main.output)` and `clickhouseDependencies` holds only httpclient5, the ClickHouse client artifacts, lz4, gson and the three Jackson artifacts (`build.gradle.kts`). `connect-api` is `api(...)`, a compile-time and consumer-classpath dependency. The Connect worker supplies the API at run time. FORK-DELTA's sentence "Production runs the shaded jar built here, so `4.1.1` is the production Connect API version" should be re-checked against a built `confluentJar` (`unzip -l ... | grep org/apache/kafka/connect`); it is read from the build file here, not from an artifact **[I]** | **No clean stop, no rename**, provided the compiled bytecode references no Connect API member newer than the oldest worker in production. That is checkable: compare the connector's `org/apache/kafka/connect/**` member references against the `4.1.1` jar | n/a |

What the rollout note should therefore say, as concrete obligations:

1. **Item for 999.4:** what changes, the two checks (suspects query before, WARN grep after), upgrade needs no clean stop, rollback does, the data-tail caveat, and that Known-cost item 10 is retired.
2. **Item for 999.5:** the new canonical form with the character table (reuse gg3 item 3's table with a new "natural name" column), the limitation query unchanged, an explicit "root column: silent `DEFAULT`; element: loud failure" sentence, the enum-label sentence, the rename instruction, and a rollback paragraph that mirrors item 4.
3. **Item for WR-01:** the Map-key exception sentence from the gg3 note is removed; no operator action.
4. **Item for `connect-api` 4.4.0:** semver-minor, not bundled in the artifact, worker version governs, no operator action, plus the bytecode member check as the evidence. Do not claim anything about 4.4.0's behaviour beyond what a live suite run showed.
5. The independent `gsd-code-reviewer` pass on the operator documents before the publication gate (memory: recurred in Phase 24).

---

## Feature Dependencies

```
999.4 classifier (shouldSkipDescribeRow + Table.SIZE_FIELD_MATCHER)
    └──requires──> ClickHouseFieldDescriptor.type (own row type) + kept parent Column (innermost type, array depth)
    └──requires──> skippedCols not remembering ambiguous structural names
    └──requires──> DESC-05 ancestor rule evaluated first (JSON/Dynamic descendants stay skipped)
    └──flips────> COLS-14 D-04 pin (3 cases) ; keeps COLS-14 WARN code
    └──requires──> WR-02 production-loop pin (extract per-row decision) or a live pin

DeclaredNullability pairing ──benefits──> 999.4 (the kept element pairs; Nullable(Tuple(size0 ...)) element is corrected from DESCRIBE's Nullable to the declared plain type)

999.5 decode (fromJsonRow, #839 one-liner)
    └──requires──> describeTableV1 raw-line read (keeps TableTest.extractCommentV1 green)
    └──requires──> DeclaredNullability.normalise rewrite (SQL-identifier unescape; COLS-13 live twins are the oracle)
    └──requires──> 23-REVIEW WR-01 pin lands on the rewritten branch
    └──touches───> Column.extractEnumValues keys (decision: unescape or document)
    └──produces──> rollout item (rename of doubled-form Kafka fields)

WR-01 (JacksonException excluded) ── independent of both; needs ThrowableTexts sentinel test

connect-api 4.4.0 ── independent of all; gates on the suite and on a member-reference check
```

### Dependency Notes

- **999.4 before 999.5.** Both edit the `describeTableV2` loop and `fromJsonRow` tests use `row(name, type)` fixtures whose JSON embeds raw escape text. 999.5 changes what that text means, so 999.4's new pins should use names free of backslashes and be written first; they then survive 999.5 unchanged. The reverse order forces 999.4's fixtures to be rewritten.
- **999.4 conflicts with nothing in COLS-14 except the D-04 pin.** The pin is the only test that asserts the defect.
- **999.5 requires the `normalise` rewrite, not just extends it.** The Phase 23 premise "the server's line break is the only lone backslash-`n` outside quotes, and every backslash run inside quotes is even" (23-EVIDENCE R-02, D-13) holds for the *kept* form. After the decode there is one layer fewer, so the arithmetic is different; the live twins from 23-04 are the oracle for the rewrite.
- **999.5 is where the fork re-converges with upstream.** If PR #839 merges before the phase starts, the `fromJsonRow` and V1 hunks become identical to upstream's and drop out of the fork delta.
- **WR-01 and the 999.x items are independent**, so they can share a phase only for review economy.

---

## MVP Definition

### Launch With (this milestone)

- [ ] **999.4 classifier at both drop sites** with the unit table of real DESCRIBE rows, the live round-trip matrix, the DESC-05 check, the D-04 pin flipped, WR-02 pin on the production loop — required by ROADMAP and measured as high severity (every insert into an affected table fails).
- [ ] **999.5 decode + V1 raw-line read + `normalise` rewrite + both-form live pins + rollout item** — required by ROADMAP; the `normalise` rewrite is what makes it safe.
- [ ] **WR-01 `JacksonException` exclusion + sentinel test** — carried advisory, small, removes the one documented exception from the gg3 note.
- [ ] **Rollout note** covering the three items above and `connect-api` (semver-minor, not bundled), reviewed by an independent reviewer.

### Add After Validation (this milestone, only if time allows)

- [ ] Discovery WARN for names that changed form (trigger: the user wants a log-side check for the silent root-column case).
- [ ] Enum label key unescape (trigger: an operator reports a backslash/quote enum label).
- [ ] `Utils.escapeName` backslash handling for `auto.evolve` (trigger: `auto.evolve` on a table with such a name).

### Future Consideration

- [ ] Dotted Tuple element names (`` `a.b` ``): new backlog item.
- [ ] Permanent dual-form field lookup: not recommended (anti-feature).
- [ ] Drop the fork's V1 describe path once client V1 is retired.

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| 999.4 classifier, both drop sites, live matrix | HIGH (affected tables never insert) | MEDIUM | P1 |
| 999.4 `skippedCols` cascade guard | HIGH (same defect one level down) | LOW | P1 |
| 999.4 D-04 flip + WR-02 production-loop pin | MEDIUM (test truth) | MEDIUM | P1 |
| 999.5 `fromJsonRow` decode + V1 raw read | HIGH for slash users (silent loss), MEDIUM otherwise | LOW | P1 |
| 999.5 `normalise` rewrite + COLS-13 oracle | HIGH (prevents a regression) | HIGH | P1 |
| 999.5 both-form live pins + rollout item | HIGH | MEDIUM | P1 |
| WR-01 exclude Jackson from pass-through + sentinel test | MEDIUM | LOW | P1 |
| Rollout: `connect-api` statement + member-reference check | MEDIUM | LOW | P1 |
| Discovery WARN for changed-form names | MEDIUM | LOW | P2 |
| Enum label key unescape | LOW to MEDIUM | MEDIUM | P3 |
| `escapeName` backslash for auto.evolve | LOW | LOW | P3 |

**Priority key:** P1 must have for the milestone, P2 should have, P3 future.

---

## Upstream comparison (the "competitor" here is `ClickHouse/clickhouse-kafka-connect`)

| Feature | Upstream today | Upstream direction | Our approach |
|---------|----------------|--------------------|--------------|
| Structural vs declared `size`/`size0` | Upstream keeps `Table.SIZE_FIELD_MATCHER`; `shouldSkipDescribeRow` is a fork addition. No upstream issue or PR about a Tuple element named `size` was found, but only a few searches were run (the repository issue search for "backslash" and one web search), so absence is not established | none known | Fork-only fix in the fork's own method plus a narrow edit to upstream-region `Table`. Offer upstream a PR only after it is green here |
| `fromJsonRow` backslash handling | Doubles every backslash (identical to the fork) | PR #839 (open) removes it, fixes V1 to read raw lines, UTF-8 pinned | Take #839's two hunks verbatim; the fork-only part is `DeclaredNullability` |
| `Nullable(Tuple)` declared-type pairing | does not exist | n/a | Fork-only; adapt to the decode |
| Jackson pass-through | n/a (fork-only value-free errors) | n/a | Fork-only |

## Open questions needing a user decision

1. **999.5 canonical form.** Recommendation above is the raw identifier. If the user prefers "keep the escaped form and document it", the whole group collapses to: pins + note, and #838's slash loss stays open on the fork.
2. **Transitional alias.** Whether to build a one-release dual-form lookup with a WARN. Recommended: no; WARN plus rename instruction.
3. **Where the single structural classifier lives** (`ClickHouseHelperClient` only, with `Table`'s matcher narrowed, or a shared helper in `sink.db.mapping`). It decides how many upstream-region lines change.
4. **Enum label keys**: fix now or document.
5. **Re-check FORK-DELTA's "4.1.1 is the production Connect API" sentence** against a built `confluentJar` before the rollout note repeats it.

## Sources

Measured in this research (HIGH): ClickHouse `26.9.19.9` (`clickhouse/clickhouse-server:latest`, image `bdb5c911a23d`) via `docker run --rm --entrypoint clickhouse ... local --multiquery`; `ClickHouseColumn.of` from `clickhouse-data-0.9.6`; Jackson `2.22.3` jars (`javap`); `build/classes/java/main` (`DeclaredNullability`, `Column`); `git show upstream/main:...ClickHouseFieldDescriptor.java`; `repo1.maven.org/maven2/org/apache/kafka/connect-api/maven-metadata.xml`.

Repository: `src/main/.../ClickHouseHelperClient.java` (`shouldSkipDescribeRow`, `describeTableV1/V2`), `ClickHouseFieldDescriptor.java`, `mapping/Table.java` (`SIZE_FIELD_MATCHER`, `updateParent`), `mapping/DeclaredNullability.java`, `mapping/Column.java` (`extractEnumValues`), `ClickHouseWriter.java` (`passesThroughUnchanged`), `build.gradle.kts` (`confluentJar`, `clickhouseDependencies`), `docs/FORK-DELTA.md` items 6, 10, 11, `docs/ROLLOUT-v1.7.0_gg3.md` items 2-4, `.planning/milestones/v1.7.0_gg3-hardening-phases/23-*/23-EVIDENCE.md` (R-01, R-02), `24-*/24-COMMAND-EVIDENCE.md` (E-15, E-16), `22-*/22-REVIEW.md` (WR-01), `23-*/23-REVIEW.md` (WR-01, WR-02).

ClickHouse documentation (MEDIUM, official, cross-checked by [M]):
- https://clickhouse.com/docs/sql-reference/statements/describe-table (columns, `describe_include_subcolumns`, `is_subcolumn`)
- https://clickhouse.com/docs/sql-reference/data-types/array (`size0`, `sizeN-1` for multi-dimensional arrays; Array-of-Tuple sub-columns)
- https://clickhouse.com/docs/sql-reference/data-types/nullable (`null` sub-column)
- https://clickhouse.com/docs/sql-reference/data-types/map (`keys`, `values`; no `size` mentioned)
- https://clickhouse.com/docs/sql-reference/data-types/tuple (element reference only; no reserved-name rule documented)
- https://clickhouse.com/docs/sql-reference/data-types/string (no `.size` sub-column documented)
- `system.settings` on 26.9.19.9: `output_format_json_escape_forward_slashes`, `describe_include_subcolumns` (measured)

Upstream (MEDIUM, read from GitHub 2026-10-21, cross-checked against the fork's own history):
- https://github.com/ClickHouse/clickhouse-kafka-connect/issues/838
- https://github.com/ClickHouse/clickhouse-kafka-connect/pull/839 (and its diff at patch-diff.githubusercontent.com)
- https://github.com/ClickHouse/clickhouse-kafka-connect/pull/529
- https://github.com/ClickHouse/clickhouse-kafka-connect/issues/518
- Jackson: https://raw.githubusercontent.com/FasterXML/jackson-core/2.x/src/main/java/com/fasterxml/jackson/core/JacksonException.java, https://raw.githubusercontent.com/FasterXML/jackson-databind/2.x/src/main/java/com/fasterxml/jackson/databind/ser/std/StdSerializer.java

Kafka (LOW, release-note titles only): https://downloads.apache.org/kafka/4.4.0/RELEASE_NOTES.html, https://archive.apache.org/dist/kafka/4.3.0/RELEASE_NOTES.html, https://archive.apache.org/dist/kafka/4.2.0/RELEASE_NOTES.html. The 4.4.0 upgrade notes were not found on kafka.apache.org at research time.

Confidence by seam: the web providers used here (`webfetch`, `websearch`) classify as LOW unverified and MEDIUM only when `websearch` is cross-verified; every web claim above that carries MEDIUM was cross-checked against the fork's code or a live measurement, and the Kafka 4.4.0 content stays LOW.

---
*Feature research for: v1.7.0_gg4-hardening discovery defects, writer error hygiene and operator rollout*
*Researched: 2026-10-21*
