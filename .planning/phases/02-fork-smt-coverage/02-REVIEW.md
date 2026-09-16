---
phase: 02-fork-smt-coverage
reviewed: 2026-09-16T00:00:00Z
depth: standard
files_reviewed: 11
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java
  - src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java
  - src/test/java/com/clickhouse/kafka/connect/sink/SmtSinkLandingTest.java
  - src/test/java/transforms/ConvertToMapOrJsonTest.java
  - src/test/java/transforms/HeaderToValueTest.java
  - src/test/java/transforms/HeadersToValueTest.java
  - src/test/java/transforms/TableOperationFilterTest.java
  - src/test/java/transforms/TransformTestHelpers.java
findings:
  critical: 0
  warning: 3
  info: 2
  total: 5
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-09-16T00:00:00Z
**Depth:** standard
**Files Reviewed:** 11
**Status:** issues_found

## Summary

This phase adds unit/integration test coverage for four fork-only SMTs (`TableOperationFilter`,
`HeaderToValue`, `HeadersToValue`, `ConvertToMapOrJson`) plus one shared-path production fix in
`ClickHouseWriter.doWriteColValue()`'s `MAP` branch (writing the RowBinary null-indicator byte for
`Nullable`-typed map values, mirroring the sibling `ARRAY` branch). The transform-level fixes
(D-01 nested-collection recursion, D-02 stale-schema-cache guards, D-03 `configure()` state reset)
are each correctly implemented and are backed by solid, well-targeted unit tests.

The `ClickHouseWriter` fix itself is a faithful, minimal mirror of the existing `ARRAY` branch and
is correct for the case it was written to fix (a non-null value written into a `Nullable` map
value column). Digging into the branch's full behavior surfaces two related, unaddressed gaps that
are worth tracking even though they are not regressions introduced by this diff: (1) the branch
still has no defense against a null value landing in a *non-nullable* map value slot — it silently
desyncs the RowBinary stream rather than failing loudly the way the top-level `doWriteCol()` gate
does; and (2) the specific "null entry inside a `Nullable` map value" case that the fix explicitly
claims to leave "deliberately unhandled" for the *whole map* is, for individual *entries*, actually
handled correctly by the new code — but that correctness is asserted only by reasoning during this
review, not exercised by any test in the suite (the one integration test that lands data into a
`Map(String, Nullable(String))` column deliberately avoids a null-valued header for exactly this
reason). A `HeaderToValue` robustness gap (crashing on legitimate `fields` configuration reuse
against existing non-string columns) and a minor `ConvertToMapOrJson` recursion gap (map keys are
not recursively converted, only values) round out the findings.

## Warnings

### WR-01: `ClickHouseWriter` MAP branch has no test — unit or integration — for a null entry inside a `Nullable` map value

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:443-453`
**Issue:** The fix under review adds:
```java
if (col.getMapValueType().isNullable() && mapValue != null) {
    BinaryStreamUtils.writeNonNull(stream);
}
doWriteColValue(col.getMapValueType(), stream, new Data(value.getNestedValueSchema(), mapValue), defaultsSupport);
```
This is the highest-risk change in the phase (shared serialization path for every `Map` column).
Tracing it: when `mapValue == null` and the value type is `Nullable`, no marker byte is written
here, and the recursive `doWriteColValue()` call lands on a primitive write helper (e.g.
`doWritePrimitive`, `doWriteDates`, `doWriteFixedString`, the `Decimal` branch) that itself detects
`null` and emits exactly one `writeNull()` byte — which, by inspection, is the correct RowBinary
encoding for a null `Nullable` scalar. That reasoning is sound, but it is *only* reasoning: no test
in this phase (or pre-existing) writes an actual null-valued entry through this branch and reads it
back from a real ClickHouse server.
`SmtSinkLandingTest.hdrs09HeadersLandInMapColumn` creates a `Map(String, Nullable(String))` column
and explicitly comments that it avoids a null-valued header "because ... the null-map-entry
contract is already pinned at unit level by HDRS-03" — but HDRS-03
(`HeadersToValueTest.testNullValuedHeaderStoredAsNullEntrySchemaPath`) only asserts that the SMT
*builds* a `Map` with a null entry in Java; it never touches `ClickHouseWriter` or RowBinary
encoding. `ClickHouseWriterTest` (not in this phase's scope, but the natural home for this) also
has no `MAP`-branch coverage at all (`grep` for `MAP`/`doWriteColValue` in that file only turns up
the unrelated `doWriteColValue_Tuples` test). The one code path this fix was written to unblock —
`Map(String, Nullable(*))` — is therefore verified end-to-end only for non-null entries.
**Fix:** Add either (a) a `ClickHouseWriterTest` unit test that calls `doWriteColValue()` directly
for a `MAP` column whose value type `isNullable()` with a `null` entry and asserts the exact byte
sequence written (one `writeNull()` byte, no leading `writeNonNull()`), or (b) extend
`SmtSinkLandingTest.hdrs09HeadersLandInMapColumn` to include a header with a `null` value and
assert the row reads back with that map key present and null in ClickHouse — closing the gap the
test's own comment identifies.

### WR-02: MAP branch has no guard against a null value landing in a non-nullable map value slot

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:446-449`
**Issue:** The new guard only special-cases the *nullable* value-type case
(`col.getMapValueType().isNullable() && mapValue != null`). If `col.getMapValueType().isNullable()`
is `false` but the actual `mapValue` is `null` (a data/schema mismatch — e.g. a Java `Map` entry
put with a `null` value where the ClickHouse column is `Map(String, String)`, not
`Map(String, Nullable(String))`), no marker byte is written, and the recursive
`doWriteColValue()` call still hits the same primitive-write null-check and emits a single
`writeNull()` byte. For a non-nullable value type, the server does **not** expect a leading
null-indicator byte at all — it expects the raw value's own encoding. Writing a stray byte here
desyncs the RowBinary stream for the remainder of the row (and, since encoding is a byte-oriented
protocol, potentially the whole batch), which the server will report as a generic decode failure
(e.g. `CANNOT_READ_ALL_DATA`, or worse — a misleading parse of subsequent columns) rather than the
clear, actionable `RuntimeException("An attempt to write null into not nullable column ...")` that
`doWriteCol()` throws for exactly this situation at the *top* level (`ClickHouseWriter.java:686`).
This gap already existed for the sibling `ARRAY` branch (`col.getArrayType().isNullable() && v !=
null`, line 467) before this diff and is not newly introduced by this fix — but since this
phase's diff explicitly extends the same unguarded pattern to `MAP`, and the phase context calls
out that "actual-null handling was deliberately left unimplemented" for the *whole* map object
only, it's worth flagging that the *per-entry* case has the identical unguarded-corruption
property, just one level deeper.
**Fix:** Either accept this as a known, pre-existing limitation shared with `ARRAY` (document it
explicitly next to the `MAP`/`ARRAY` cases) or add the same fail-loud guard used by `doWriteCol()`
before falling through to `doWriteColValue()`:
```java
if (!col.getMapValueType().isNullable() && mapValue == null) {
    throw new RuntimeException(String.format(
            "An attempt to write null into not nullable map value column '%s'", col.getName()));
}
```

### WR-03: `HeaderToValue` crashes when `fields` names a pre-existing non-STRING column

**File:** `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java:151-168, 188-220`
**Issue:** `buildSchema()` copies a pre-existing field's original schema verbatim
(`builder.field(field.name(), field.schema())`, line 207) even when that field also appears in the
configured `fields` list (i.e., it will be overwritten by a header value). `applyWithSchema()` then
unconditionally does `newValue.put(fieldName, header.value().toString())` (line 159) for any field
with a matching `headerIdx`, regardless of the field's declared schema type. If a user configures
`fields=off16` (or any existing non-STRING column) to be overwritten from a header — which is
exactly the documented use case, per the config doc string "List of field names to **create or
overwrite** with header values" — every record throws `DataException` from
`Struct.put()`/`Struct.validateType()` ("Invalid Java object for schema type INT16: class
java.lang.String" or similar), wrapped by `apply()`'s catch block into `DataException("Failed to
transform record", ...)`. This is a hard task failure, not a config-time validation error, so it
surfaces only once records are flowing. Notably, `HeadersToValue` (the sibling SMT in this same
file set) has an explicit, tested contract for its analogous collision (`HDRS-05:
testSchemaPathFieldCollisionThrows`), but `HeaderToValue`'s far more common collision case (any
`fields` entry naming a pre-existing non-string column) has neither a validation guard nor a test.
**Fix:** Either coerce/validate at `configure()` time (reject `fields` entries whose type, if
already present in a known/sample schema, isn't STRING-compatible — not generally possible without
a schema, so more practically: document the STRING-only overwrite restriction clearly in the config
doc string) or add the same explicit, well-messaged validation `HeadersToValue` uses for its
collision case, and pin the behavior (whichever is chosen) with a test parallel to HDRS-05.

## Info

### IN-01: `ConvertToMapOrJson.convertFieldValue()` does not recursively convert Map keys

**File:** `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java:253-259`
**Issue:**
```java
if (value instanceof Map<?, ?> map) {
    Map<Object, Object> convertedMap = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
        convertedMap.put(entry.getKey(), convertFieldValue(entry.getValue()));
    }
    return convertedMap;
}
```
Only `entry.getValue()` is passed through `convertFieldValue()`; `entry.getKey()` is copied as-is.
Kafka Connect's `Map` schema type permits an arbitrary key schema (including `Struct`), so a
`Map<Struct, X>` field would leave raw `Struct` objects as map keys in the output, which neither
Jackson nor Gson can serialize meaningfully as a JSON object key, and which downstream `Map`-mode
consumers (e.g. `ClickHouseWriter`) do not expect either. In practice, ClickHouse `Map` keys are
always primitive/String types, so this is unlikely to be hit in production, but the asymmetry
(values fully recursed, keys never touched) is worth a one-line fix or a doc comment noting the
assumption.
**Fix:** `convertedMap.put(convertFieldValue(entry.getKey()), convertFieldValue(entry.getValue()));`
or add a comment documenting that map keys are assumed to already be JSON/ClickHouse-safe
primitives.

### IN-02: `HeaderToValue.applySchemaless()` is missing the `@SuppressWarnings("unchecked")` its sibling uses

**File:** `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java:112`
**Issue:** `final Map<String, Object> value = new HashMap<>((Map<String, Object>) record.value());`
performs the same unchecked generic cast as `HeadersToValue.applySchemaless()`
(`HeadersToValue.java:90`), but `HeadersToValue` annotates the local with
`@SuppressWarnings("unchecked")` while `HeaderToValue` does not. This is a cosmetic inconsistency
between the two sibling transforms, but it means `HeaderToValue.java` emits an "unchecked cast"
compiler warning that its sibling suppresses, which is inconsistent with the project's enforced
`-Xlint:unchecked` build convention (`build.gradle.kts:168`).
**Fix:** Add `@SuppressWarnings("unchecked")` to the `value` declaration at line 112 to match
`HeadersToValue`'s convention.

---

_Reviewed: 2026-09-16T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
