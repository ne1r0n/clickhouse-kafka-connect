# Phase 19: Nullable Enum & Tuple Mapping - Pattern Map

**Mapped:** 2026-10-12
**Files analyzed:** 16 (new and modified)
**Analogs found:** 15 / 16 (all analog paths verified git-tracked)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/.../sink/db/mapping/Column.java` (Enum branch L202-211, Tuple branch L232-236; mod) | mapping | transform | itself: primitive tail `.isNullable(isNull)` (L288-293) | exact |
| `src/main/java/.../sink/db/mapping/DeclaredNullability.java` (new, go only) | utility | transform | `Column.extractColumn` recursion (Array branch L213) | role-match |
| `src/main/java/.../sink/db/helper/ClickHouseHelperClient.java` (mod: narrow, then delete WARN; add post-Table pass, go) | service (discovery) | request-response | itself L470-530, hooks L579/L650, `new Table(` L595/L666 | exact |
| `src/main/java/.../sink/db/ClickHouseWriter.java` (stale comment ~L749-750 only, go) | writer | streaming | itself | exact (comment only; file is IS DIRTY under IDE hook, do not reformat) |
| `src/test/.../sink/NullableTupleSpikeLiveTest.java` (new, name discretionary) | test (live) | request-response | `NestedNullRowBinaryLiveTest` + `CapturingClickHouseWriter` + `Lz4RoundTripLiveTest` | exact |
| `src/test/.../sink/NullableEnumRoundTripLiveTest.java` (new) | test (live) | CRUD | `NestedNullRowBinaryLiveTest` (WRT-07 Cell pattern) | exact |
| `src/test/.../sink/db/mapping/ColumnNullableEnumTupleTest.java` (new) | test (unit) | transform | `ColumnArrayRecursionTest` | exact |
| `NestedNullableCompositeWriteTest` (flip pin on go) | test (unit) | transform | itself L263+ | exact |
| `NestedNullRowBinaryLiveTest` (flip L418 pin on go) | test (live) | request-response | itself | exact |
| `ClickHouseHelperClientForkUnitTest` (L264-324, flip/retarget) | test (unit) | transform | itself | exact |
| `ClickHouseHelperClientForkLiveTest` (L395-434, flip/retarget) | test (live) | request-response | itself + `LogCapture` | exact |
| `.planning/scripts/fork-anchors.txt` (row 57 + new `Column.java` rows) | config | batch | existing rows 50-60 | exact |
| `docs/FORK-DELTA.md` (WARN row ~L48, Known costs ~L136, new Enum row) | docs | - | existing rows | exact |
| `19-BEHAVIOUR-LEDGER.md` (new, IDs from L-31) | docs | - | `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` | exact |
| `19-SPIKE.md` (new) | docs | - | Phase 16/17 measured-approval bundles (not read; see No Analog) | partial |
| `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md` (mod) | docs | - | its own IN-07..IN-10 rows (Phase 16) | exact |

## Pattern Assignments

### `Column.java` (mapping, transform)

**Analog:** same file. Current Enum branch (L202-211) lacks the builder call the primitive tail has:
```java
            return builder.type(type)
                    .enumValues(extractEnumValues(valueType))
                    .build();          // fix: add .isNullable(isNull) before build()
```
Tuple branch (L232-236): add `.isNullable(isNull)` the same way (go only):
```java
            return builder.type(Type.TUPLE)
                    .tupleFields(new ArrayList<>())
                    .build();
```
Anchor counts: `grep -cF ".isNullable(isNull)"` is 1 today, 2 after Enum, 3 after Tuple. Do not repeat that string in a comment. Tuple pass needs a package-visible `isNullable` setter (Lombok `@Setter` or explicit).

### `DeclaredNullability.java` (utility, transform, go only)

Fork-only class beside `Column`. Walk `Column` tree and `ClickHouseColumn.of(normalise(rawType))` in parallel (RESEARCH Pattern 2 sketch: TUPLE pairs by name via `child.getName().substring(tuple.getName().length()+1)` or 1-based index when unnamed; ARRAY pairs `getArrayType()` with nested[0]; MAP pairs `getMapValueType()` with nested[1]; default sets `isNullable`). Normalise first: replace the two-char sequence backslash-`n` with a space (`fromJsonRow` mangling). Fail open on `IllegalArgumentException` with one metadata-only log line. Run once per root column per DESCRIBE, never per record.

### `ClickHouseHelperClient.java` (discovery, request-response)

**Analog:** itself, L470-527 (the WARN machinery to narrow then delete):
```java
private static final Pattern NULLABLE_ENUM_OR_TUPLE = Pattern.compile("Nullable\\((?:Enum8|Enum16|Tuple)\\(");
private static final Set<String> WARNED_NULLABLE_ENUM_OR_TUPLE = ConcurrentHashMap.newKeySet();
static boolean hasNullableEnumOrTuple(String rawType) { return rawType != null && NULLABLE_ENUM_OR_TUPLE.matcher(rawType).find(); }
static boolean warnOnceIfNullableEnumOrTuple(String database, String tableName, ClickHouseFieldDescriptor fieldDescriptor) {
    ...
    if (!WARNED_NULLABLE_ENUM_OR_TUPLE.add(database + "." + tableName + "." + name + ":" + rawType)) return false;
    LOGGER.warn(NULLABLE_ENUM_OR_TUPLE_WARNING, database, tableName, name, rawType);
```
Hooks: `warnOnceIfNullableEnumOrTuple(...)` at L579 (V1) and L650 (V2); `return new Table(...)` at L595 and L666. Enum commit: retarget regex to `Nullable\(Tuple\(` only, rename predicate/constants/method, rewrite text (IN-01: name the RowBinary insert route; IN-03: unambiguous key separator). Go commit: delete all of it plus both hooks and add the post-`Table` pass call in both loops (shared private helper). Keep WARN text metadata-only (catalog names and types, no values).

### `NullableTupleSpikeLiveTest` and `NullableEnumRoundTripLiveTest` (live tests)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/NestedNullRowBinaryLiveTest.java`

Structure: `public class X extends ClickHouseBase`, `createTable` helper (L123-129), `putAndStop` (L132-147), `Cell` record + `selectAndAssert` (L214-232):
```java
private static void createTable(ClickHouseHelperClient chc, String tableName, String createTableQuery) {
    try (Records ignored = chc.queryV2(String.format(createTableQuery, tableName))) { } catch (Exception e) { throw new RuntimeException(e); }
}
private record Cell(String alias, String sqlExpression, String expected) {}
// builds SELECT <expr> AS <alias> ... WHERE id = N ; RowSnapshots.select(chc, sql, aliases...) ; assertEquals(expected, rows.get(0))
```
Failure-side cells copy `putExpectingFailure` (L313-338): drop/create table, `assertThrows(RuntimeException.class, ...)`, assert no row left via `RowSnapshots.select(chc, "SELECT id FROM %s", "id")`, drop in `finally`. Table names via `createTopicName(...)` (unique, matters for process-wide dedup set, Pitfall 8). Read back with SQL (`isNull(x)`, `tupleElement`, `CAST(ne AS Nullable(Int8))`), ASCII values only.

Spike side (b), fixed bytes: `new CapturingClickHouseWriter(handBuiltTable, csc)` (`src/test/.../sink/db/CapturingClickHouseWriter.java:46`, `Capture(route, body, deduplicationToken, supportDefaults)`), then insert body like `Lz4RoundTripLiveTest` L146-150:
```java
try (InsertResponse ignored = client.insert(table, new ByteArrayInputStream(body),
        ClickHouseFormat.RowBinary, new InsertSettings().setQueryId(queryId)).get()) { }
```
Use `RowBinaryWithDefaults` for the DEFAULT twin; assert `chc.describeTable(...).hasDefaults()` as precondition. Hand-built Column pattern, `NestedNullableCompositeWriteTest.tupleColumn` L178-187:
```java
Column.builder().name("c").type(Type.TUPLE).isNullable(nullable)
    .tupleFields(List.of(int32Column("c.a"), Column.builder().name("c.b").type(Type.STRING).build())).build();
```
Log `SELECT version()` (as `Lz4RoundTripLiveTest`). Do not set `enable_nullable_tuple_type`.

### `ColumnNullableEnumTupleTest` (unit)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnArrayRecursionTest.java` (L1-50): package-private class extending nothing (stays in `testUnit`), `@ParameterizedTest` + `@MethodSource`, `@DisplayName("COLS-xx: ...")`, calls `Column.extractColumn(name, type, false, hasDefault, isSubColumn)` or `Column.extractColumn(newDescriptor(name, type))`. Fixtures for DESCRIBE rows: feed raw JSON through `fromJsonRow` (RESEARCH recorded rows), via `ClickHouseTestHelpers.newDescriptor`. Never touch `ColumnTest`/`TableTest`.

### Pin flips (never delete; add written rationale in `@DisplayName` or comment)

- `NestedNullableCompositeWriteTest#nullElementInArrayOfNullableTupleThrowsBecauseColumnDropsTupleNullability` (L263-): premise `assertFalse(... isNullable())` becomes `assertTrue`; null element writes marker (go only). `#nullTupleFollowsColumnNullability` (L251-261) unchanged.
- `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleFailsWithDataException` (L418): on go uses read-back of SQL NULL via `selectAndAssert` instead of `putExpectingFailure`.
- `ClickHouseHelperClientForkUnitTest` L264-324 and `ClickHouseHelperClientForkLiveTest#describeTableWarnsOnceForNullableEnumOrTupleColumns` (L395-434; columns `e`, `m`, `t`): Enum commit makes `e`, `m` negative, `t` stays positive. Negative form uses `LogCapture`:
```java
try (LogCapture capture = LogCapture.attach(ClickHouseHelperClient.class)) {   // LogCapture.java:87
    Table table = chc.describeTable(chc.getDatabase(), uniqueTopic);
    assertTrue(capture.events("WARN").stream().noneMatch(e -> e.message().contains("Nullable(")));
    assertTrue(table.getRootColumnsMap().get("e").isNullable());
}
```

### `fork-anchors.txt`

Four TAB-separated fields: path, fixed pattern, expected `grep -cF` count, backs-ID. Existing row 57:
```
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	static boolean hasNullableEnumOrTuple(	1	PUB-02
```
Enum commit: pattern becomes renamed predicate (count 1). Add `Column.java	.isNullable(isNull)	2	COLS-08`. Go: delete row 57, bump to count 3 and add pass call-site row. Phase 18 comment-header style (`# Phase 18 (v1.7.0_gg2): ...`) precedes each phase block; add a `# Phase 19` block. Gate: `verify-merge-invariants.sh 54e63648 HEAD` (about 3 min 12 s, run in background).

### `19-BEHAVIOUR-LEDGER.md`

**Analog:** `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md`. Header (Purpose, Source plans, How to read, Destination key) then table columns: `ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 20 destination`. IDs start at L-31. Test names, decision IDs and message formats only, never record data.

### `15-REVIEW-DISPOSITION.md`

Update both YAML frontmatter per finding and the `| Finding | Severity | Disposition | Source |` row for IN-01..IN-05. Vocabulary: `open`, `fixed`, `skipped`, `deferred`. Phase 16 precedent: `fixed | <sha> (docs only, Phase 16)`.

## Shared Patterns

### Typed read-back
**Source:** `RowSnapshots.select` (`src/testFixtures/.../helper/RowSnapshots.java`) with SQL expressions. Apply to all live tests; never `countRows`.

### Metadata-only messages (Phase 18)
Exception and log texts carry column name and type only, never record values. Applies to the narrowed WARN, the fail-open log line, and any new `DataException`.

### Fork-only test classes
New classes beside, never inside, upstream-identical `ColumnTest`/`TableTest`; unit classes extend nothing; live classes extend `ClickHouseBase`.

### Same-commit bookkeeping (COLS-11)
Code + pin flips + `fork-anchors.txt` + `docs/FORK-DELTA.md` + ledger row in one commit.

## No Analog Found

| File | Role | Reason |
|---|---|---|
| `19-SPIKE.md` | docs | Phase 16/17 approval-bundle files were not read here; planner should open the Phase 16/17 measured-approval bundles for table format. |
| `DeclaredNullability.java` | utility | No existing code uses `ClickHouseColumn.of` (first use of `clickhouse-data` parsing); follow RESEARCH Pattern 2 sketch and verified probe. |

## Metadata

**Analog search scope:** `src/main/java/.../sink/db/**`, `src/test/java/.../sink/**`, `src/testFixtures`, `.planning/scripts`, `.planning/phases/18-writer-correctness`
**Files scanned:** about 14 read or grepped; tracked status confirmed with `git ls-files` for the test files and the Phase 18 ledger
**Pattern extraction date:** 2026-10-12
