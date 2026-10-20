# Phase 23: Declared-Nullability Hardening & Review Closure - Pattern Map

**Mapped:** 2026-10-20
**Files analyzed:** 17 (1 src/main, 8 test, 8 planning or bookkeeping)
**Analogs found:** 17 / 17 (every path below was confirmed with `git ls-files`; `23-*` files and the new fixture class are new files and keep their intended paths)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java` (modify) | utility (discovery pass) | transform | itself (current file, 168 lines) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` (modify) | test (Docker-free) | request-response (log capture) | itself | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` (modify) | test (Docker-free) | transform | itself (`discover(...)`, D-11 flips already in file) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` (modify) | test (live) | CRUD round trip | itself (multi-line premise ~L715-750) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java` (modify) | test (live) | CRUD | itself (L52, L119) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java` (modify) | test helper | utility | itself (L195-197) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/<new public fixture class>` (create) | test fixture (constants) | none | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnNullabilityPaths.java` | role-match |
| `.planning/phases/23-.../23-BEHAVIOUR-LEDGER.md` (create) | planning doc | none | `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md` | exact |
| `.planning/phases/23-.../23-EVIDENCE.md` (extend) | planning doc | none | `.planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md` | exact |
| `docs/FORK-DELTA.md` COLS-10 row (modify, L51) | docs | none | the same row | exact |
| `.planning/scripts/fork-anchors.txt` rows 71-75 (modify) | config | none | rows 71-75 themselves | exact |
| `.planning/milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/17-REVIEW-DISPOSITION.md` (modify) | planning doc | none | `19-REVIEW-DISPOSITION.md` rows | exact |
| `.planning/milestones/v1.7.0_gg2-hardening-phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md` (modify) | planning doc | none | WR-03 and CR-01 rows (`fixed` + SHA) | exact |
| `.planning/ROADMAP.md` `## Backlog` 999.4 and 999.5 (append) | planning doc | none | `### Phase 999.3` entry (L303-309) | exact |

## Pattern Assignments

### `DeclaredNullability.java` (utility, transform)

**Analog:** the current file. Keep its shape: private constructor, `LOGGER`, private `record Assignment`, all-or-none (collect first, apply after).

**Constants and WARN format** (lines 30-40), copy the style for the new unpaired constant:
```java
private static final Logger LOGGER = LoggerFactory.getLogger(DeclaredNullability.class);
/** The two characters backslash and n that {@code fromJsonRow} leaves for a server-escaped newline. */
static final String ESCAPED_NEWLINE = "\\n";          // D-14: becomes `public`
static final String FALLBACK_WARNING = "Could not take element nullability for column {} of table {}.{} from its "
        + "declared type {} ({}); keeping the nullability DESCRIBE reported for it";
private static final String SPACE = " ";
private static final String NAME_SEPARATOR = ".";
private static final String STRUCTURE_MISMATCH = "structure mismatch";
```
The new constant must keep a delimited `"column {} of table"` segment (Pitfall 8) and must not contain "keeping the nullability DESCRIBE reported" or the sentinel text.

**`apply` fail-open core** (lines 80-102): the part to restructure for D-06/D-01/D-07.
```java
public static boolean apply(Column root, String declaredType, String database, String table) {
    if (declaredType == null) { return false; }          // add `root == null ||` (D-06), then compute columnName before try
    try {
        ClickHouseColumn parsed = ClickHouseColumn.of(root.getName(), normalise(declaredType));
        List<Assignment> assignments = new ArrayList<>();
        if (!collect(root, parsed, assignments)) {
            LOGGER.warn(FALLBACK_WARNING, root.getName(), database, table, declaredType, STRUCTURE_MISMATCH);
            return false;
        }
        for (Assignment assignment : assignments) { assignment.column().setNullable(assignment.nullable()); }
        return true;                                      // D-01: log UNPAIRED warn here, still return true
    } catch (RuntimeException e) {
        // The exception text can echo the type, so only its class name is logged. ...
        LOGGER.warn(FALLBACK_WARNING, root.getName(), database, table, declaredType, e.getClass().getSimpleName());
        return false;
    }
}
```

**`containsTuple`** (lines 56-70): add `|| column.getType() == null` to the null check (D-05); `switch (column.getType())` is what NPEs today.

**`collectTuple` pairing** (lines 142-167): add null checks for tuple name, null field, null field name (D-07, Pitfall 4); change `HashMap` at L149 to `LinkedHashMap` and track paired keys so the unpaired set is `byKey.keySet() - paired` (stable WARN text).
```java
Map<String, ClickHouseColumn> byKey = new HashMap<>();
for (int i = 0; i < elements.size(); i++) {
    String name = elements.get(i).getColumnName();
    byKey.put(name == null || name.isEmpty() ? String.valueOf(i + 1) : name, elements.get(i));
}
String prefix = tuple.getName() + NAME_SEPARATOR;
```

**`normalise`** (lines 47-53): replace `declaredType.replace(ESCAPED_NEWLINE, SPACE)` with the quote-aware single pass; the working prototype is in `23-RESEARCH.md` "Code Examples" (backtick and single-quote state, backslash-run parity, line break only when the run is exactly 1 outside quotes). Keep `public static String normalise(String)`.

**Anchor caution:** `unwrapSimpleAggregate(` matches 2 lines (definition L109, call L119). Do not add a Javadoc or comment line containing that token (Pitfall 7).

---

### `DeclaredNullabilityTest.java` (test, Docker-free, request-response via LogCapture)

**Analog:** itself. Helpers (lines 20-60): `row(name, type)`, `describe(rows...)`, `root(table)`, `nullabilityByPath`. Add a `root(table, name)` variant and a sentinel constant (D-16, e.g. `ROOT_SENTINEL = "nullability_root"`) in the first commit that needs it (Pitfall 3, step 2). Remove `SERVER_NEWLINE` (L33, used L140, 142, 348) and use `DeclaredNullability.ESCAPED_NEWLINE` (D-14).

**WARN-assertion pattern to copy and strengthen** (lines 201-224, `unparsableDeclaredTypeFailsOpenWithOneMetadataWarning`); the weak part is `message.contains(ROOT)` with `ROOT = "c"`:
```java
try (LogCapture log = LogCapture.attach(DeclaredNullability.class)) {
    assertFalse(DeclaredNullability.apply(column, type, DATABASE, TABLE));
    List<LogCapture.CapturedEvent> warnings = log.events("WARN");
    assertEquals(1, warnings.size());
    String message = warnings.get(0).message();
    assertTrue(message.contains(ROOT), message);            // -> assertTrue(message.contains("column " + SENTINEL + " of table"), message)
    assertTrue(message.contains(type), message);
    assertTrue(message.contains(QUALIFIED_TABLE), message);
    assertFalse(warnings.get(0).hasThrowable(), "the exception text can echo the type and is never logged");
    assertEquals(1, log.events().size());
}
assertEquals(before, nullabilityByPath(column));
```
Same pattern at `structureMismatchFailsOpenWithOneWarning` (L227) and `unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning` (L268, the D-08 flip).

**D-08 flip target** (lines 266-286): keep `Column.builder().name(ROOT).build()` input; replace `message.contains(NullPointerException.class.getSimpleName())` with an assertion of the `structure mismatch` reason in its delimiter `"(structure mismatch)"`; add the rationale comment (see the flip format below).

**Pinned test to keep byte-identical** (lines 190-199), `missingDeclaredElementDoesNotShiftPairing`; add the D-01 unpaired-WARN test as a sibling, built on the same input:
```java
String type = "Nullable(Tuple(a Int32, b Nullable(String)))";
Column column = root(describe(row("c", type), row("c.b", "Nullable(String)")));
assertTrue(DeclaredNullability.apply(column, type, DATABASE, TABLE));
assertEquals(Map.of("c", true, "c.b", true), nullabilityByPath(column));
```

**Null-safety pins:** the existing `containsTupleIsFalseWithoutATupleAnywhere` (L289) already passes `containsTuple(null)`; add the null-root test (`apply(null,...)` false, `log.events().size() == 0`), null type at root/Array element/Map value, null `tupleFields` entry, null field name, null tuple name, each asserting one WARN with `(structure mismatch)` and the root untouched. D-09 catch-path pin: call `ClickHouseColumn.of` first with `assertThrows(RuntimeException.class, ...)`, read `getClass().getSimpleName()`, assert `"(" + simpleName + ")"` (see RESEARCH Job 5 for safe inputs; reuse `"Nullable(Tuple(SENTINEL_ELEMENT_NAME NoSuchType))"`).

---

### Phase 19 D-11 test-flip format (D-08 and D-19 use it)

**Concrete committed example:** commit `2a94e045` (`fix(19-04) ...`), in `NestedNullRowBinaryLiveTest#nullElementInArrayOfNullableTupleReadsBackAsSqlNull` and `NestedNullableCompositeWriteTest#nullElementInArrayOfNullableTupleWritesNullMarker` (both tracked under `src/test/java/com/clickhouse/kafka/connect/sink/`; view with `git show 2a94e045 -- src/test`). The in-tree, still-current instances are in `ClickHouseHelperClientForkUnitTest.java` (lines ~340-440). Format: a `// COLS-10 flip (D-11): <what it asserted before>. <why the premise changed>. The input is kept/unchanged ...` comment above `@Test`, a `@DisplayName("COLS-10 (D-11 flip): ...")`, the method renamed only when the old name states a stale premise, and the commit message listing "Red before the fix ... flipped expectations on the unchanged code" and "Renamed test methods (old -> new)".

From the nested-composite example:
```java
// COLS-10 (D-16 flip): before COLS-10 Column.extractColumn dropped Tuple nullability and this case
// threw a DataException for the null element. The premise is inverted now that element
// nullability comes from the declared type; the input is unchanged and the null element writes
// the null marker.
@Test
@DisplayName("COLS-10 (D-16 flip): a null element in Array(Nullable(Tuple(a Int32))) writes the null marker once element nullability comes from the declared type")
void nullElementInArrayOfNullableTupleWritesNullMarker() throws Exception {
```
In-tree D-11 flip with the method kept (`ClickHouseHelperClientForkUnitTest`, ~L349-352 and L360):
```java
// COLS-10 flip (D-11): this case asserted that these ten raw types were not flagged. With the
// WARN gone the inputs are kept and assert that a root row alone discovers silently, with the
// root nullability exactly as Column.extractColumn gives it.
@ParameterizedTest(name = "COLS-10 (D-11 flip): {0} discovers without a warning")
```
For D-08 the method keeps its name (CONTEXT: "keeps its method"); for D-19 the three tests keep their methods, gain element rows, and the root-only input moves to a new sibling asserting exactly one unpaired WARN. Note the D-19 flip is not a pure input-kept flip: element rows are added, so the comment must say so.

---

### `ClickHouseHelperClientForkUnitTest.java` (test, Docker-free)

**Analog:** itself.

**`discover(...)` helper** (lines 276-300), which D-04 extends (new helper or parameter) with the real skip, and D-15 re-points at constants:
```java
private static Table discover(Map<String, String> declaredTypes, String... nameAndTypePairs) {
    List<Column> columns = new ArrayList<>();
    int numColumns = 0;
    for (int i = 0; i < nameAndTypePairs.length; i += 2) {
        ClickHouseFieldDescriptor descriptor = newDescriptor(nameAndTypePairs[i], nameAndTypePairs[i + 1]);
        columns.add(Column.extractColumn(descriptor));
        if (!descriptor.isSubcolumn()) { numColumns++; }
    }
    return ClickHouseHelperClient.buildDescribedTable("db", "t", false, columns, numColumns, declaredTypes);
}
```
**Skip-aware variant** (D-04): the loop `describeTableV2` runs, already excerpted in `23-RESEARCH.md` "D-04 skip-aware discovery" (`shouldSkipDescribeRow(d, columnsByName)` then `declaredTypes.put` for non-subcolumn rows). Existing direct calls to the skip are at ~L113-153 and 250-268: `ClickHouseHelperClient.shouldSkipDescribeRow(newDescriptor(rowName, "String"), columns)` with `columnsSoFar(...)`.

**D-15 target:** `WARN_LEVEL` (L255) and `DISCOVERED_TABLE = "db.t"` (L403) are declared mid-class; the class has no constants block. Create one at the top (`DATABASE = "db"`, `TABLE = "t"`, `DISCOVERED_TABLE = DATABASE + "." + TABLE`) and use them in `discover(...)`.

**D-16 target** (`fallbackWarningNamesDatabaseAndTable`, lines 405-420): weak assertion `message.contains("c")` ("Could" contains c); switch to the sentinel root and `"column " + SENTINEL + " of table"`.

**D-19 collisions** (turn red under D-01): `otherTypesDiscoverWithoutWarningAndUnchangedRootNullability` values `Tuple(a Nullable(Int32))` and `Tuple(a Nullable(Enum8('q' = 1)))` (`discover("c", rawType)` with a root row only), and `repeatedDiscoveryNeverWarns` (L428). Add the matching `c.a` element rows, keep `warnCount(...) == 0` and root-nullability assertions identical.

---

### `NullableTupleMatrixLiveTest.java` (test, live, CRUD round trip)

**Analog:** itself. Remove the private copy at L85 (`ESCAPED_NEWLINE = "\\n"`), import `com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability`, and use `DeclaredNullability.ESCAPED_NEWLINE` at L745.

**D-15 call site** (~L730), replace `NullableRoundTrip.normaliseDeclaredType(row.getType())` with `DeclaredNullability.normalise(row.getType())`:
```java
for (ClickHouseFieldDescriptor row : NullableRoundTrip.describeRows(chc, tableName)) {
    ...
    String normalised = NullableRoundTrip.normaliseDeclaredType(row.getType());
    rawTypes.put(row.getName(), row.getType());
```
**D-12 live premise:** place beside `omittedNullableTupleFieldStoresNull` and the multi-line premise (L745-750: `rawTypes.get(multiLine).contains(ESCAPED_NEWLINE)`). Copy the `@ParameterizedTest @EnumSource(Twin.class)` twin convention, `NullableRoundTrip.createTable(chc, tableName, twin.ddl(...))`, `NullableRoundTrip.describeRows`, a `LogCapture` on `DeclaredNullability` around the describe, `NullableRoundTrip.putAndStop` and `selectAndAssert`. See the sketch in `23-RESEARCH.md` "D-12 live premise sketch" (the Kafka Struct field name needs two backslashes, per R-02).

---

### `SimpleAggregateTupleRoundTripLiveTest.java` (test, live)

Remove the private `ESCAPED_NEWLINE` at L52 (use at L119); the class already imports `DeclaredNullability` (L15), so replace with `DeclaredNullability.ESCAPED_NEWLINE`. Its `LogCapture` zero-WARN assertion (~L125) is one of the cases COLS-14 must keep green.

### `NullableRoundTrip.java` (test helper)

Delete lines 195-197 (`static String normaliseDeclaredType(String rawType) { return DeclaredNullability.normalise(rawType); }`) and the Javadoc above it; remove the then-unused import at L16.

---

### New public fixture class for the shared captured type strings (Pitfall 6)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnNullabilityPaths.java` (tracked; `public final class`, private constructor, `public static final String` constants, Javadoc "Test helper: ...", package `com.clickhouse.kafka.connect.sink.db.mapping`). Intended new path: `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/CapturedDeclaredTypes.java` (name is the planner's choice).
```java
package com.clickhouse.kafka.connect.sink.db.mapping;

/** Test helper: flattens a Column tree into {@code path -> isNullable}. */
public final class ColumnNullabilityPaths {

    public static final String PATH_SEPARATOR = ".";
    public static final String ARRAY_ELEMENT_PATH = "[]";
    public static final String MAP_VALUE_PATH = "{}";

    private ColumnNullabilityPaths() {}
```
Constants to hold (strings from `23-EVIDENCE.md` R-02, as Java literals in `23-RESEARCH.md` Job 2): the literal backslash-n name type, the enum-label type, the `Nullable(Tuple(...))` live table type, and the sub-column row name. Both `DeclaredNullabilityTest` and `NullableTupleMatrixLiveTest` import it, so they cannot drift. It must not hold a copy of `ESCAPED_NEWLINE`.

---

### `23-BEHAVIOUR-LEDGER.md` (create)

**Analog:** `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md` (also `18-` and `19-`). Header block plus an 8-column table; IDs continue from L-48, so Phase 23 starts at **L-49**.
```markdown
# Phase 22 Behaviour Ledger

**Purpose:** one place that lists every operator-visible behaviour change made in Phase 22 (...), so that Phase 24 can write the `1.7.0_gg3` CHANGELOG section, complete FORK-DELTA and write `docs/ROLLOUT-v1.7.0_gg3.md` (ROLL-03). ...
**Source plans:** 22-03 (L-42..L-47), 22-04 (L-48; ...)
**How to read it:** IDs continue after the Phase 19 ledger's L-41. ... The ledger holds test names, decision IDs and message formats only, never record data.
**Destination key:** CHANGELOG = ...; FORK-DELTA = a row or Known costs item; ROLL-03 = an item in `docs/ROLLOUT-v1.7.0_gg3.md`.

## Ledger
| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 24 destination |
```
Rows expected: L-49 unpaired-element WARN (D-01, D-02; element named `size`/`size0` triggers it, `null` does not on `latest`, per R-01); L-50 backslash-n in a name or label now pairs (D-10; also a real newline inside a name); optionally a no-visible-change row for the null-totality hardening (D-05..D-07). Same-commit rule from CONTEXT.

### `23-EVIDENCE.md` (extend, executor sections)

**Analog:** `22-EVIDENCE.md` (and `21-EVIDENCE.md`). The file already has the header, Index table and R-01..R-04; the Index has a pending `E-01..` row to replace with real rows. Entry format: `### E-NN: {title} (plan)`, a "Recorded <date>. Scratch output is in `/tmp/gsd23/exec/` (...)" line, then `key: value` lines:
```
spotless_base_rev: fefd8648524f8b906a22f47540391f170e2357af
spotless_base_files: 46
spotless_base_md5: f2e32c7bc069ecd155830f82d26285d6
spotless_after_22_01: SPOTLESS-BASELINE-EQUAL
anchors_mirror_22_01: rows=59 bad=0
testunit_22_01: Tests summary: 577 tests, 575 succeeded, 0 failed, 2 skipped
harn08_commit: 84e773be9d20bbe67a8850c05904a099636239e1
```
Entries needed: red-before/green-after for each pin (D-07 style), the D-17 mutation 1 and mutation 2 runs for each WARN-content assertion with failing test names, the SC3 reading (log and still apply, per Open Question 3), the phase gate (`testUnit --rerun`, full suite, `verify-merge-invariants.sh 54e63648 HEAD`). Current baseline: anchors `rows=65 bad=0`, spotless numstat 46 files md5 `f2e32c7bc069ecd155830f82d26285d6`.

---

### `docs/FORK-DELTA.md` COLS-10 row (L51)

**Analog:** the row itself. One very long table row with four cells: what the fork does | files | why | `COLS-10; <pinned-by test classes>`. Add the D-01 (unpaired element WARN, still applies), D-07 (null tree is a structure mismatch) and D-10 (quote-aware `normalise`) clauses to the first cell. Existing wording to amend: "after turning the backslash-n that `ClickHouseFieldDescriptor.fromJsonRow` leaves in multi-line types into a space" and "a declared type that does not parse, a tree that does not pair, or any unexpected exception in the pass keeps the nullability `DESCRIBE` reported and logs one metadata-only WARN". Append any new pinning class (the new fixture class is not a test, so likely only the new live/unit tests' classes) to the pinned-by list. Same commit as each behaviour change.

### `.planning/scripts/fork-anchors.txt` rows 71-75

**Analog:** the rows. TAB-separated `path <TAB> fixed-string <TAB> count <TAB> backs-ID`:
```
.../mapping/Column.java	.isNullable(isNull)	3	COLS-10
.../mapping/Column.java	@Setter(AccessLevel.PACKAGE) private boolean isNullable;	1	COLS-10
.../helper/ClickHouseHelperClient.java	DeclaredNullability.apply(	1	COLS-10
.../helper/ClickHouseHelperClient.java	buildDescribedTable(	3	COLS-10
.../mapping/DeclaredNullability.java	unwrapSimpleAggregate(	2	COLS-10
```
(full paths begin `src/main/java/com/clickhouse/kafka/connect/sink/db/`). New rows go after line 75 under a `# Phase 23 (v1.7.0_gg3): ...` comment in the style of the Phase 19 (line 70) and Phase 22 comments; measure with `grep -cF -e "<pattern>" <path>` after the edit and rewrite the file in the same commit. Candidate anchors: the new unpaired-WARN constant name, a token of the quote-aware scan. Counts are LINES, not occurrences.

---

### `17-REVIEW-DISPOSITION.md` and `19-REVIEW-DISPOSITION.md` (modify)

**Analog:** the 19 file's `fixed` rows. Each finding exists twice: frontmatter list entry and table row; update both plus `open:`.
```yaml
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "FORK-DELTA states per-twin server error codes that the test does not pin"
...
open: 0
total: 8
```
```markdown
| WR-03 | warning | fixed | 3e39867d (docs only, Phase 20: FORK-DELTA states a read-past-end code) |
| WR-01 | warning | deferred | carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md): no code churn between Phase 19's green gate and the release (D-08); ... |
```
Pattern for the new Source cell: `<disposition> | <short SHA> (<test or evidence name>)`, reason in the Source cell, "previously deferred (20-MILESTONE-CLOSE-RECORD.md)" kept as a trailing note if desired. 17 file: frontmatter `open: 4` goes to `open: 0` (IN-03 is `skipped`), rows currently `| WR-01 | warning | open | - |`. Use the full SHAs from RESEARCH Job 4 (`fee898c6...` CI-18/CI-19/D-09 guard, `5d616806...` OPS-02, UAT commit `5010093f...`). 19 file: `open: 0` stays. Do not edit `20-MILESTONE-CLOSE-RECORD.md`.

### `.planning/ROADMAP.md` Backlog 999.4 and 999.5 (append)

**Analog:** `### Phase 999.3` (lines 303-309); append after it, before `## Progress`. Slice to the Backlog section before any scripted edit (Pitfall 10).
```markdown
### Phase 999.3: Rethrown write-failure messages can carry record values (BACKLOG — scheduled into Phase 22)

**Goal:** <defect, evidence, fix sketch>. Found 2026-10-12 in the Phase 18 discussion (D-09)
**Scheduled:** ...
**Requirements:** mapped under Phase 22
**Plans:** planned under Phase 22
**Delivered:** ...
```
999.4 (title, e.g. "shouldSkipDescribeRow drops Tuple elements named size/size0"): Goal quotes R-01 (code 33 `CANNOT_READ_ALL_DATA`, no row stored; silent wrong value reachable; severity high), notes `null` is rejected by the server, includes the fix sketch (the skip must keep `c.size` but still drop `e.size0` and `c.name.size`, so "parent is a Tuple" alone is not enough). 999.5: backslash in a name comes out of `fromJsonRow` doubled (R-02). Both are un-scheduled backlog entries, so omit the scheduled/delivered fields or mark them "not scheduled"; the 999.1-999.3 entries only show the scheduled form, so there is no un-scheduled example in the file.

## Shared Patterns

### Metadata-only WARN with a sentinel root (Phase 18 content rule, D-16, D-17)
**Source:** `DeclaredNullabilityTest.java` L201-224 plus `LogCapture.java`
**Apply to:** every new or changed WARN assertion in `DeclaredNullabilityTest` and `ClickHouseHelperClientForkUnitTest`. Always `LogCapture.attach(DeclaredNullability.class)` in try-with-resources, `events("WARN")`, assert size 1, `hasThrowable()` false, `log.events().size()` 1, and the delimited segment `"column " + SENTINEL + " of table"` (not a bare `contains`). Run each red against both mutations (argument dropped; placeholder dropped) and log them in `23-EVIDENCE.md`.

### Phase 19 D-11 flip format
**Source:** commit `2a94e045`, `ClickHouseHelperClientForkUnitTest.java` L349-352 and L360
**Apply to:** D-08 flip, three D-19 flips. A `// COLS-NN flip (D-11): ...` rationale, `@DisplayName("... (D-11 flip): ...")`, method kept, no deletions or weakened assertions, flipped names listed in the commit message.

### Fork bookkeeping in the same commit (Phase 19 D-13)
**Source:** `docs/FORK-DELTA.md:51`, `.planning/scripts/fork-anchors.txt`, `23-BEHAVIOUR-LEDGER.md`
**Apply to:** every commit that changes behaviour. Gate per commit: anchors mirror loop (`rows=65 bad=0` today), `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun`, and the spotless numstat staying at the 46-file md5. Full `verify-merge-invariants.sh 54e63648 HEAD` (about 6 minutes) at the phase gate.

### Process rules
`spotlessApply` on touched files only; none of this phase's files are in the 46-file baseline-red set, so each must have an empty diff. Never edit `ColumnTest` or `TableTest`. `ClickHouseHelperClient.java` stays untouched (D-05).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| Quote-aware scanner inside `normalise` | utility | transform | No existing quote-aware scanner in the repo; use the tested prototype in `23-RESEARCH.md` "Code Examples" |
| Un-scheduled backlog entries 999.4 / 999.5 | planning doc | none | 999.1-999.3 are all scheduled entries; adapt the 999.3 shape |

## Metadata

**Analog search scope:** `src/main/.../sink/db/mapping`, `src/test/.../sink`, `src/test/.../sink/db/{helper,mapping}`, `.planning/phases/21-23`, `.planning/milestones/v1.7.0_gg2-hardening-phases`, `.planning/scripts`, `docs`
**Files scanned:** about 25 read or grepped; all paths verified with `git ls-files`
**Pattern extraction date:** 2026-10-20
