# Phase 9: Metadata-free JSON Path — Keep or Drop - Pattern Map

**Mapped:** 2026-09-20
**Files analyzed:** 8 (all modifications to existing tracked files — this phase creates no new files)
**Analogs found:** 8 / 8

**Note on this phase's shape:** every file CONTEXT.md/RESEARCH.md names is an edit to an existing, git-tracked file, not a new file. The strongest "analog" for most edits is therefore the surrounding code already in the same file (a sibling guard, a sibling test method, a sibling checklist row) rather than a different file. Where a genuinely separate file is the better analog (the Docker-free test class shape, the immutable `Table` constructor call shape, the bookkeeping-note shape), it is named explicitly below. All paths verified git-tracked via `git ls-files` this session — none are gitignored mirrors.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — 4 guard sites (`start`, `updateMapping`, `getTable`, `cleanupExtraFields`) | service (db writer / control-flow guard) | request-response (skip-vs-run branch, no new I/O) | Same file's existing `cleanupExtraFields`'s `csc.isBypassFieldCleanup()` guard (already an early-return keyed on a `ClickHouseSinkConfig` boolean) | exact — same method, same guard shape, same file |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — synthetic `Table` construction inside `getTable()` | model-construction (value object) | transform | `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:533,602` — the two existing `new Table(database, tableName, hasDefaults, columns, numColumns)` call sites | exact — identical 5-arg constructor, only the argument values differ |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` — `doInsertJsonV2` overload swap (`jsonInsertColumns`) | service (db writer / batch insert) | request-response / batch | Same file's current 4-arg `client.insert(table.getName(), dataWriter, JSONEachRow, insertSettings)` call at `:1379` (own prior code, same method) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` — delete `@Disabled`; add 2 Docker-free pins | test (Docker-free unit) | request-response (unit-level guard assertions) | `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` (Docker-free class shape: extends nothing, stays in `testUnit`) + this file's own existing reflective `csc` accessor | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java` — delete `@Disabled` only | test (live, `ClickHouseBase`) | request-response (feature test against real ClickHouse) | `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java` and `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` (identical one-line `@Disabled` removal, same annotation text, same `extends ClickHouseBase`) | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/JsonInsertColumnsLiveTest.java` — delete `@Disabled` only | test (live) | request-response | `ExplicitJsonInsertPathLiveTest.java` | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskJsonInsertFormatTest.java` — delete `@Disabled` only | test (live) | request-response | `ExplicitJsonInsertPathLiveTest.java` | exact |
| `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/TableTest.java` — `extractNullables()` assertion `3`→`2` | test (live, `ClickHouseBase`) | transform (metadata-shape assertion) | This file's own `extractMapOfPrimitives()` (same class, same `assertEquals(int, table.get...List().size())` idiom) | exact |
| `.planning/REQUIREMENTS.md` — JSONP-03 correction note | doc/bookkeeping | transform (annotate, don't rewrite) | This file's own "Count correction" blockquote at line 237 | exact |
| `.planning/MERGE-CHECKLIST.md` — row 26 annotation, Recorded Fact 13 resolution, KEEP disposition | doc/bookkeeping | transform (annotate, don't flip) | This file's own Recorded Fact 11/12 `repo-derived` notes and row 8's "Phase 7 note (D-02/D-03)" inline-annotation pattern | exact |

## Pattern Assignments

### `ClickHouseWriter.java` — the four guard sites (D-03/D-05)

**Analog:** the file's own existing `cleanupExtraFields` guard shape, generalized into a shared predicate.

**Existing sibling guard to copy the shape from** (`ClickHouseWriter.java:1387-1399`, quoted in full — this is the pattern the four new guards must match):
```java
    protected Map<String, Object> cleanupExtraFields(Map<String, Object> m, Table t) {
        if (csc.isBypassFieldCleanup()) {
            return m;
        }

        Map<String, Object> cleaned = new HashMap<>();
        for (Column c : t.getRootColumnsList()) {
            if (m.containsKey(c.getName())) {
                cleaned.put(c.getName(), m.get(c.getName()));
            }
        }
        return cleaned;
    }
```
D-03's guard for this method becomes exactly this shape with one more disjunct: `if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) { return m; }` — same early-return-then-untouched-body idiom already present.

**Shared predicate (D-05):** a single private method, matching the pre-merge shape and the codebase's `is`-prefixed boolean-method convention (`isSslEnabled()`, `isCloud()`, per `AGENTS.md` Naming Patterns):
```java
private boolean shouldSkipTableMetadata() {
    return csc != null && csc.isJsonInsertFormatExplicit();
}
```
Call this — never `csc.getInsertFormat() == InsertFormats.JSON` (Pitfall 2: the latter also matches the upstream unset-`insertFormat` default-branch typo and would widen the bypass to every connector).

**Insertion points, each verbatim from the current tree** (all four `[VERIFIED]` in RESEARCH.md Pattern 1 this session):

1. `start()`, between `ClickHouseWriter.java:124-125`:
```java
        LOGGER.debug("Ping was successful.");
        this.updateMapping(csc.getDatabase());
```
Guard goes after line 124, before line 125: `if (shouldSkipTableMetadata()) { LOGGER.info(...); return true; }`

2. `updateMapping(String database)`, at `:142-149` — guard returns early before the `compareAndSet`:
```java
    public boolean updateMapping(String database) {
        // Do not start a new update cycle if one is already in progress
        // Atomically compare and set isUpdateMappingRunning
        if (!this.isUpdateMappingRunning.compareAndSet(false, true)) {
            return false; // in progress
        }

        LOGGER.debug("Update table mapping.");
```

3. `getTable(String database, String topic)`, full method at `:1592-1615`:
```java
    protected Table getTable(String database, String topic) {
        String tableName = Utils.getTableName(database, topic, csc.getTopicToTableMap());
        Table table = this.mapping.get(tableName);
        if (table == null) {
            if (this.updateMapping(database)) {
                table = this.mapping.get(tableName);//If null, update then do it again to be sure
            } else {
                String cleanTopicName = Utils.getMappedOrTopicTableName(topic, csc.getTopicToTableMap());
                table = chc.describeTable(database, cleanTopicName.replace("`", "").trim());
            }
        }
        ...
        return table;//It'll only be null if we suppressed the error
    }
```
Guard goes before line 1593's `mapping.get` lookup and returns `new Table(database, Utils.getMappedOrTopicTableName(topic, csc.getTopicToTableMap()), false, List.of(), 0)` — reuse `Utils.getMappedOrTopicTableName`, already used two lines below in the `describeTable` fallback, do not re-derive the `getOrDefault(topic, topic)` lookup by hand:
```java
    public static String getMappedOrTopicTableName(String topicName, Map<String, String> topicToTableMap) {
        String tableName = topicToTableMap.get(topicName);
        LOGGER.debug("Topic name: {}, Table Name: {}", topicName, tableName);
        if (tableName == null) {
            tableName = topicName;
        }
        return tableName;
    }
```
`[VERIFIED: src/main/java/com/clickhouse/kafka/connect/util/Utils.java:187-194]`

4. `cleanupExtraFields` — see the analog block above; final shape: `if (csc.isBypassFieldCleanup() || shouldSkipTableMetadata()) { return m; }`.

**Error handling pattern:** none of the four guards introduce new exceptions — they early-return before the existing exception-throwing code (`getTable`'s `RuntimeException("Table %s does not exist")` at `:1601`) is reached. Do not add a try/catch around any guard.

---

### `ClickHouseWriter.java` — synthetic `Table` construction (D-04)

**Analog:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:533` and `:602` — both existing, `DESCRIBE`-backed call sites of the same constructor:
```java
return new Table(database, tableName, hasDefaults, columns, numColumns);
```
**Constructor signature** (`Table.java:48`, `[VERIFIED]`):
```java
public Table(String database, String name, boolean hasDefaults, List<Column> columns, int numColumns) {
```
**The one difference from the analog:** this phase's call site is the *third* `new Table(...)` call in `src/main` and the only one not backed by a real `DESCRIBE` response — argument values are `(database, tableName, false, List.of(), 0)` rather than real describe-derived values. `hasDefaults()` has its own hand-written getter (`Table.java:62-64`, excluded from Lombok's `@Getter`), so `false` here is guaranteed to flow through to `doInsertBatch:228`'s `table.hasDefaults()` read unchanged.

---

### `ClickHouseWriter.java` — `doInsertJsonV2` overload swap (D-06/D-08)

**Analog:** the method's own current call, `ClickHouseWriter.java:1379`:
```java
client.insert(table.getName(), dataWriter, ClickHouseFormat.JSONEachRow, insertSettings)
```
**Target shape (D-08 — branch on non-empty):**
```java
if (csc.getJsonInsertColumns() != null && !csc.getJsonInsertColumns().isEmpty()) {
    client.insert(table.getName(), csc.getJsonInsertColumns(), dataWriter, ClickHouseFormat.JSONEachRow, insertSettings);
} else {
    client.insert(table.getName(), dataWriter, ClickHouseFormat.JSONEachRow, insertSettings);
}
```
Both overloads confirmed present side-by-side in `client-v2-0.9.6.jar` via `javap` this session (RESEARCH.md Pattern 4) — no version bump, no new import beyond what the file already has (`client.insert` is already called at this exact line).

**Error handling:** unchanged — COLS-05's contract (a nonexistent column name produces ClickHouse's own loud `NO_SUCH_COLUMN_IN_TABLE`) comes from the server, not from connector code; do not add connector-side validation of `csc.getJsonInsertColumns()` (Anti-Pattern, `JsonInsertColumnsUnitTest.java:87-90` explicitly locks this out of scope).

---

### `MetadataFreeJsonInsertUnitTest.java` — re-enable + 2 pins (D-10/D-11)

**Analog for the Docker-free class shape:** `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java:1-20`:
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
...
import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pins for {@link SinkTaskStatistics#buildMBeanName(String, int, String)} - JMX-01,
 * JMX-02 and JMX-03. New fork-only class (D-16): it deliberately extends nothing, so it stays in
 * {@code testUnit} - extending {@code ClickHouseBase} would inherit its {@code @Tag("clickhouse")}
 * and drop this class out of the Docker-free loop (Phase 1 D-12).
 */
class MBeanNameUnitTest {
```
`MetadataFreeJsonInsertUnitTest` already follows this exact shape (package `com.clickhouse.kafka.connect.sink.db`, extends nothing, same `testUnit`-visibility javadoc convention) — no restructuring needed, only: delete line 36's `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` and update the class javadoc's second paragraph (currently describes the disabled state) to describe the enabled state instead.

**Existing reflective accessor to reuse for both new pins** (`MetadataFreeJsonInsertUnitTest.java:39-49`, already in the file, full excerpt):
```java
    private static final String WRITER_SINK_CONFIG_FIELD = "csc";

    private static Field writerSinkConfigField() throws NoSuchFieldException {
        Field field = ClickHouseWriter.class.getDeclaredField(WRITER_SINK_CONFIG_FIELD);
        field.setAccessible(true);
        return field;
    }

    private static void setSinkConfig(ClickHouseWriter writer, ClickHouseSinkConfig csc) throws Exception {
        writerSinkConfigField().set(writer, csc);
    }
```
**Core pattern to copy for D-11's two new pins** — this file's own `cleanupExtraFieldsBypassDiffersByInsertFormat()` (`:93-118`, full excerpt) is the template: construct a `ClickHouseWriter`, `setSinkConfig(writer, ...)` via the reflective accessor, call the guarded method directly (no Docker, no `ClickHouseHelperClient`), assert on the return value:
```java
    @Test
    @DisplayName("JSON-03: cleanupExtraFields returns the payload untouched under insertFormat=json, and strips unknown keys under default config, without mutating the input map")
    void cleanupExtraFieldsBypassDiffersByInsertFormat() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column knownCol = Column.builder().name("known_col").type(Type.STRING).build();
        Table table = new Table("test_db", "test_table", false, List.of(knownCol), 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("known_col", "value");
        payload.put("unknown_col", "distinctive_unknown_value");

        Map<String, String> jsonProps = minimalProps();
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        setSinkConfig(writer, new ClickHouseSinkConfig(jsonProps));
        Map<String, Object> cleanedUnderJson = writer.cleanupExtraFields(payload, table);
        assertEquals(payload, cleanedUnderJson);
        ...
    }
```
D-11's two new pins (`getTable()` returns a synthetic `Table` resolved from `topic2TableMap` without touching the helper client; `updateMapping()` short-circuits) follow this identical shape: `new ClickHouseWriter(...)`, `setSinkConfig(writer, csc)`, call `getTable(...)`/`updateMapping(...)` directly, assert on the returned `Table`'s `getRootColumnsList().isEmpty()`/`getName()` or on `updateMapping`'s boolean return — no live ClickHouse connection, matching `minimalProps()`'s existing `localhost:8123` placeholder config (never actually dialed under the bypass since the guard returns before any network call).

**`start()` stays live-only** — per D-11, it pings a server before reaching its guard, so no third Docker-free pin for `start()` belongs in this file.

---

### `ExplicitJsonInsertPathLiveTest.java` / `JsonInsertColumnsLiveTest.java` / `ClickHouseSinkTaskJsonInsertFormatTest.java` — delete `@Disabled` only (D-10)

**Analog:** each of the three is the analog for the other two — identical one-line change, identical annotation text, identical `extends ClickHouseBase`:
```java
@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")
public class ExplicitJsonInsertPathLiveTest extends ClickHouseBase {
```
```java
@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")
public class JsonInsertColumnsLiveTest extends ClickHouseBase {
```
```java
@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")
public class ClickHouseSinkTaskJsonInsertFormatTest extends ClickHouseBase {
```
`[VERIFIED: grep this session — exact lines 44/45, 41/42, 31/32 respectively]`. **D-10's instruction is literal: delete only the `@Disabled(...)` line.** Do not touch the class javadoc, imports, or any test body — Phase 7's compile fixes (reflective `setSinkConfig`, 5-arg `Table` construction, local `createTable` helpers already inside these classes) stay exactly as they are. `ExplicitJsonInsertPathLiveTest`'s R-01 pin (`explicitJsonStructRecordWithDefaultBypassRowBinarySilentlyDropsBatch`, its `PIN - NOT AN ENDORSEMENT` Javadoc) must remain byte-identical per D-10.

**Core pattern already present (do not re-derive):** each class extends `ClickHouseBase` (live-ClickHouse feature test base) and uses `ClickHouseTestHelpers` + `ClickHouseHelperClient` for setup/teardown — this is the codebase's standard live-test shape (`AGENTS.md` § Writing Tests: "read back the data and `assertEquals` against the original records").

---

### `TableTest.java` — `extractNullables()` assertion fix (D-14)

**Analog:** the same class's own `extractMapOfPrimitives()` (`TableTest.java:19-33`) — same `assertEquals(int, table.get...List().size())` idiom, same class, same file:
```java
    @Test
    public void extractMapOfPrimitives() {
        ...
        Table table = new Table("default", "t", false, List.of(map, mapValues), 1);
        assertEquals(1, table.getRootColumnsList().size());
        ...
    }
```
**Current (stale) code to fix**, `TableTest.java:35-53`, full method:
```java
    @Test
    public void extractNullables() {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);

        String tableName = createTopicName("extract-table-test");
        ClickHouseTestHelpers.dropTable(chc, tableName);
        new CreateTableStatement()
                .tableName(tableName)
                .column("off16", "Int16")
                .column("date_number", "Nullable(Date)")
                .engine("MergeTree").orderByColumn("off16").execute(chc);

        Table table = chc.describeTable(chc.getDatabase(), tableName);
        assertNotNull(table);
        assertEquals(2, table.getRootColumnsList().size());
        assertEquals(3, table.getAllColumnsList().size());
        ClickHouseTestHelpers.dropTable(chc, tableName);
    }
```
**Change:** line 51, `3` → `2`, with a comment citing `DESC-01`/`HLP-02` per D-14 (e.g. `// 2, not 3: shouldSkipDescribeRow (HLP-02/DESC-01) drops the structural date_number.null subcolumn before it reaches Table's constructor — see MERGE-CHECKLIST.md Recorded Fact 13`). Do not touch `assertEquals(2, table.getRootColumnsList().size())` on the line above — that assertion is already correct and already passing.

---

### `.planning/REQUIREMENTS.md` — JSONP-03 correction note (D-07)

**Analog:** this file's own existing "annotate, don't rewrite" precedent, the Count correction blockquote at line 237:
```markdown
> **Count correction.** This block previously read "46 total". The document has always listed **53** v1 requirements (`grep -c '^- \[ \] \*\*'` = 53); the earlier summary figure was a miscount, not a lost set of requirements. No requirement was added or removed at roadmap creation.
```
**Current JSONP-03 text to append after, unchanged** (line 88):
```markdown
- [ ] **JSONP-03**: `jsonInsertColumns` works again, redesigned as a field filter inside upstream's `writeRecordsJson` — the `client.insert(table, columns, data, ...)` overload it used no longer exists at that call site — with `COLS-01..06` green
```
**Append a dated blockquote in the same style**, e.g.:
```markdown
> **Correction (Phase 9, 2026-09-20).** `javap` on `client-v2-0.9.6.jar`'s `com.clickhouse.client.api.Client` confirms `insert(String, List<String>, DataStreamWriter, ClickHouseFormat, InsertSettings)` still exists — the overload was never removed from the API, only unused at the old call site since the merge. This requirement's stated *rationale* was wrong; its *goal* ("`jsonInsertColumns` works again ... with `COLS-01..06` green") is unchanged and is satisfied by re-attaching the surviving overload server-side (D-06), not by a client-side field filter. The requirement text above is left as originally written.
```
Do not rewrite line 88's checkbox text itself — D-07 is explicit that only a correction note is appended.

---

### `.planning/MERGE-CHECKLIST.md` — row 26 annotation, Recorded Fact 13 resolution, KEEP disposition (D-01 bullet 4, D-14)

**Analog for inline row annotation:** row 8's existing "Phase 7 note (D-02/D-03)" pattern (same table, `ClickHouseSinkTask.java` row):
```markdown
| ... | **Phase 7 note (D-02/D-03):** Phase 7 landed the one-line `@Override` removal on `onPartitionsRevoked` early (commit `b419b778`), the compile-unblock only, to unblock `compileJava` tree-wide — the rest of this file's re-express work (the JMX segment above) is untouched and stays Phase 8's. | ORCH-02 | done |
```
**Row 26 to annotate** (`ClickHouseWriter.java` row, do not flip `status`/`owning phase`):
```markdown
| src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java | content | re-express | 7 | RowBinary fail-fast for `DYNAMIC`/`UNKNOWN`, `targetTableFilter` builder setter, `jsonInsertColumns` passthrough, the synthetic `Table` constructor call backing the metadata-free JSON path | WRT-01, WRT-02, WRT-03, WRT-05 | done |
```
Append (in the "fork behaviour at risk" cell, same style as row 8's inline note): a **Phase 9 note** stating the JSON-path portion of this row's description ("`jsonInsertColumns` passthrough, the synthetic `Table` constructor call") was closed in Phase 9, not Phase 7 — Phase 7 only made it compile-safe against upstream's immutable `Table`, per RESEARCH.md Pitfall 3.

**Analog for Recorded Facts entries:** Recorded Fact 11/12's `repo-derived` prefix and quoted-evidence shape (`.planning/MERGE-CHECKLIST.md:105,115`):
```markdown
11. **The operator-visible MBean ObjectName change (D-07) — `repo-derived`.** ...
12. **FREE-01 closed (D-11) — `repo-derived`.** ...
```
**Recorded Fact 13 to resolve** (currently ends "**Owning phase: none.**" — flip per D-14, same numbered-list position, append rather than rewrite the existing prose):
```markdown
13. **`TableTest.extractNullables()` triage, carried from plan 08-02 (D-13) — `repo-derived`.** ... **Owning phase: none.** ...
```
Append: `**Resolved in Phase 9 (commit <SHA>).** \`TableTest.java:51\`'s assertion changed 3 → 2, matching the already-correct \`getRootColumnsList().size()==2\` assertion on the preceding line; \`shouldSkipDescribeRow\`/\`Table.java\` are unchanged.`

**New Recorded Fact for the KEEP disposition** — add a fourteenth numbered entry in the same `repo-derived`/operator-confirmed style, stating D-01's KEEP call, the three operator facts from `09-CONTEXT.md`, and that this is a second deliberate departure from the conflict policy (after the JMX carve-out, Recorded Fact 11).

## Shared Patterns

### Guard predicate naming (`is`-prefixed boolean methods)
**Source:** `ClickHouseSinkConfig.java` (`isSslEnabled()`, `isCloud()`, `isErrorsTolerance()`, `isUseClientV2()`, `isBypassFieldCleanup()`, `isJsonInsertFormatExplicit()`) — `AGENTS.md` § Naming Patterns confirms this is the established convention.
**Apply to:** `shouldSkipTableMetadata()` in `ClickHouseWriter.java` (D-05) — matches the family's shape even though it is `should`- not `is`-prefixed, following the pre-merge fork's own naming for this exact method.

### Reflective private-field access for Docker-free writer tests
**Source:** `MetadataFreeJsonInsertUnitTest.java:39-49` (`WRITER_SINK_CONFIG_FIELD`, `writerSinkConfigField()`, `setSinkConfig()`).
**Apply to:** D-11's two new pins in the same class — reuse the existing accessor, do not add a new one or a setter to `ClickHouseWriter` itself (which would grow the fork's diff against a file every phase has kept minimal).

### `@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")` → delete-only re-enablement
**Source:** identical annotation text across all four parked classes (`MetadataFreeJsonInsertUnitTest.java:36`, `ExplicitJsonInsertPathLiveTest.java:44`, `JsonInsertColumnsLiveTest.java:41`, `ClickHouseSinkTaskJsonInsertFormatTest.java:31`).
**Apply to:** all four files — delete exactly this one line each, nothing else (D-10). `MetadataFreeJsonInsertUnitTest.java` additionally gets D-11's two new methods, but the `@Disabled` deletion itself is identical across all four.

### Annotate-don't-flip bookkeeping
**Source:** `REQUIREMENTS.md:237` (Count correction blockquote), `MERGE-CHECKLIST.md:105,115` (Recorded Fact `repo-derived` notes), `MERGE-CHECKLIST.md:24` (row 8's inline "Phase 7 note").
**Apply to:** `REQUIREMENTS.md` § JSONP-03 (D-07), `MERGE-CHECKLIST.md` row 26 and Recorded Fact 13 (D-14), and the new KEEP-disposition Recorded Fact (D-01). Always append; never edit or delete existing prose, never flip a `status`/`owning phase` column you do not own in this phase.

### Immutable `Table` 5-arg construction
**Source:** `ClickHouseHelperClient.java:533,602` — `new Table(database, tableName, hasDefaults, columns, numColumns)`.
**Apply to:** `ClickHouseWriter.getTable()`'s synthetic `Table` (D-04) and `MetadataFreeJsonInsertUnitTest`'s test-fixture `Table` construction (already present at `:98`). Never construct a `Table` any other way — the 2-arg pre-merge constructor no longer exists.

## No Analog Found

None. Every file this phase touches is an edit to an existing, git-tracked file with either a sibling pattern in the same file or a near-identical sibling file already in the codebase.

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/sink/db/`, `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/`, `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/`, `src/main/java/com/clickhouse/kafka/connect/util/`, `src/test/java/com/clickhouse/kafka/connect/sink/`, `src/test/java/com/clickhouse/kafka/connect/sink/db/`, `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/`, `.planning/REQUIREMENTS.md`, `.planning/MERGE-CHECKLIST.md`
**Files scanned:** `ClickHouseWriter.java` (full, via RESEARCH.md's session reads), `Table.java` (full), `ClickHouseHelperClient.java` (grep + 2 call sites), `Utils.java` (`getMappedOrTopicTableName`), `MetadataFreeJsonInsertUnitTest.java` (full, 119 lines), `MBeanNameUnitTest.java` (imports + class header + 4 methods), `ExplicitJsonInsertPathLiveTest.java` / `JsonInsertColumnsLiveTest.java` / `ClickHouseSinkTaskJsonInsertFormatTest.java` (imports + class declarations, grep), `TableTest.java` (first 60 lines), `REQUIREMENTS.md` (JSONP-03 line + Count correction precedent), `MERGE-CHECKLIST.md` (row 26, Recorded Facts 11-13)
**Pattern extraction date:** 2026-09-20
**Git-tracked verification:** `git ls-files` run against all 8 analog/target paths this session — all tracked, none gitignored mirrors (confirmed: this is the primary repo checkout, not a `.gsd/capabilities/` sync).
