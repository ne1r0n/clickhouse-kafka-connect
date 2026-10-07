# Phase 7: Type Mapping, Writer, Helper Client & Config - Pattern Map

**Mapped:** 2026-09-19
**Files analyzed:** 15 (7 `src/main`, 8 `src/test`)
**Analogs found:** 15 / 15

## How analogs work in this phase (read first)

This is **not** a greenfield phase. Branch `sync/v1.6.0` is a mechanical merge of upstream tag
`v1.6.0` into fork branch `custom_build`; `src/main` for every file this phase touches is
byte-identical to `v1.6.0` right now (confirmed via `git diff v1.6.0 HEAD -- <path>` = 0 lines
for all seven `src/main` files, and independently re-confirmed below via `git ls-files`, both
tracked source, not a mirror). There is **no textual merge conflict** anywhere in this phase's
scope — only additive re-expression of fork behaviour onto upstream's new shape.

Consequently the single best analog for every production file in this phase is **the same file's
own pre-merge fork implementation**, readable via `git show pre-v1.6.0-sync:<path>`, read
**alongside** the current working-tree file (pure upstream `v1.6.0` today). Every excerpt below
pairs both sides: "Current state" (what's in the tree now, upstream shape, to attach to) and
"Fork's pre-merge target" (the behaviour to port forward, adjusted for the new shape per
CONTEXT.md's locked decisions D-01..D-19). All paths below were verified tracked via
`git ls-files` this session — none are `.gsd`/plugin-install mirrors.

For the four rewritten fork test files (`ClickHouseHelperClientForkUnitTest`,
`RowBinaryUnsupportedTypeGateTest`, `MetadataFreeJsonInsertUnitTest`,
`ClickHouseHelperClientForkLiveTest`), the analog is themselves, in their current
(non-compiling-against-new-`Table`) form — they need mechanical call-site rewrites, not new
tests.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/.../sink/db/mapping/Type.java` | model (enum) | transform | `git show pre-v1.6.0-sync:` same path | exact (self, pre-merge) |
| `src/main/java/.../sink/db/mapping/Column.java` | model | transform | `git show pre-v1.6.0-sync:` same path | exact (self, pre-merge) |
| `src/main/java/.../sink/db/ClickHouseWriter.java` | service (writer) | streaming / batch | `git show pre-v1.6.0-sync:` same path | exact (self, pre-merge) |
| `src/main/java/.../sink/db/helper/ClickHouseHelperClient.java` | service (HTTP client) | request-response / batch | `git show pre-v1.6.0-sync:` same path | exact (self, pre-merge) |
| `src/main/java/.../sink/ClickHouseSinkConfig.java` | config | CRUD (parse+expose) | `git show pre-v1.6.0-sync:` same path | exact (self, pre-merge) |
| `src/main/java/.../sink/ClickHouseSinkTask.java:149` | controller (Connect task) | event-driven | itself (delete one annotation) | exact — trivial |
| `src/test/java/.../sink/ClickHouseBase.java` | test (base fixture) | — | `git show pre-v1.6.0-sync:` same path | exact (self, pre-merge), sliver only |
| `src/test/java/.../sink/db/helper/ClickHouseHelperClientForkUnitTest.java` | test | — | itself, current form | exact — mechanical rewrite |
| `src/test/java/.../sink/db/RowBinaryUnsupportedTypeGateTest.java` | test | — | itself, current form | exact — mechanical rewrite |
| `src/test/java/.../sink/db/MetadataFreeJsonInsertUnitTest.java` | test | — | itself, current form | exact — mechanical rewrite + `@Disabled` |
| `src/test/java/.../sink/db/helper/ClickHouseHelperClientForkLiveTest.java` | test (integration-shaped, `@Tag("clickhouse")`) | — | itself, current form | exact — verify-compiles only |
| `src/test/java/.../sink/JsonColumnTest.java` | test | — | itself, current form | exact — one import line |
| `src/test/java/.../sink/ClickHouseSinkTaskJsonInsertFormatTest.java`, `ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java` | test | — | itself, current form | exact — add `@Disabled` only |
| CFG-03 defaults test (new file) | test (parametrized config pin) | — | `src/test/java/.../sink/AsyncInsertSettingsUnitTest.java` | role-match (same package, same fork-only Docker-free config-pin shape) |

## Pattern Assignments

### `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java` (model, transform)

**Analog:** self, `git show pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java`

**Current state — full file, 35 lines** (`Read` verified this session, unchanged from RESEARCH.md):
```java
    Enum8,
    Enum16,
    JSON
}
```
No `DYNAMIC` constant.

**Fork's pre-merge target:** enum ends `..., JSON, DYNAMIC }`.

**Change:** add `DYNAMIC` as the new last constant, immediately after `JSON`:
```java
    Enum8,
    Enum16,
    JSON,
    DYNAMIC
}
```
No import changes (file has no imports at all — single-package enum).

---

### `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (model, transform)

**Analog:** self, `git show pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`

**Current state** (`Column.java:163-178`, verbatim):
```java
            default:
                if (valueType.startsWith("DateTime64")) {
                    // Need to understand why DateTime64(3)
                    type = Type.DateTime64;
                } else if (valueType.startsWith("Decimal")) {
                    // FIXME: Map keys doesn't support Decimal:
                    //        Type of Map key must be a type, that can be represented by integer or String or FixedString (possibly LowCardinality) or UUID or IPv6, but Decimal(5, 0) given.
                    type = Type.Decimal;
                } else if (valueType.startsWith("FixedString")) {
                    type = Type.FIXED_STRING;
                } else if (valueType.startsWith("JSON")) {
                    // There are some parameters that we ignore because server will handle them for us
                    type = Type.JSON;
                }

                break;
```

**Fork's pre-merge target** (`git show pre-v1.6.0-sync`, lines 148-156):
```java
                    type = Type.Decimal;
                } else if (valueType.startsWith("FixedString")) {
                    type = Type.FIXED_STRING;
                } else if (valueType.startsWith("JSON")) {
                    type = Type.JSON;
                } else if (valueType.startsWith("Dynamic")) {
                    type = Type.DYNAMIC;
                }

                break;
```

**Core pattern (the only edit):** add exactly one `else if` branch after the existing
`startsWith("JSON")` branch:
```java
                } else if (valueType.startsWith("JSON")) {
                    // There are some parameters that we ignore because server will handle them for us
                    type = Type.JSON;
                } else if (valueType.startsWith("Dynamic")) {
                    type = Type.DYNAMIC;
                }
```
Everything else in `Column.java` (upstream's `SimpleAggregateFunction` unwrapping,
`SIMPLE_AGGREGATE_FUNCTION_TYPE_PATTERN`, Avro/Protobuf union handling via
`AVRO_UNION_SCHEMA_NAME`/`PROTOBUF_UNION_SCHEMA_PREFIX`, lines 38-48) is adopted **untouched** —
do not port the fork's version of those sections.

**Do not copy:** any other part of the fork's pre-merge `Column.java` diff — CONTEXT.md's
discretion note confirms the rest was pure Spotless import reordering.

---

### `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java` — NO EDIT

**Analog:** N/A — this file is not touched. MAP-03 is satisfied entirely by rewriting *call
sites* elsewhere (see the four test-file sections below and `ClickHouseWriter`/
`ClickHouseHelperClient` sections). The constructor to target everywhere:
```java
public Table(String database, String name, boolean hasDefaults, List<Column> columns, int numColumns)
```
`addColumn` is private; never re-open it. If a call site needs to build a `Table` for a test, it
must build a `List<Column>` first, then pass it to this 5-arg constructor in one shot.

---

### `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (service/writer, streaming+batch)

**Analog:** self, `git show pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`

This file gets four independent re-expressions (WRT-01..05) plus one D-16 log tweak. Treat each
as its own attachment point; do not merge them into one giant diff (D-19: one behaviour per
commit — WRT-01/02/03 together as "RowBinary fail-fast", WRT-05 as "targetTableFilter", the
log line with whichever commit touches its neighbourhood).

**1. Message-builder helper (new private method) — core pattern:**
```java
    /**
     * Builds the actionable message shared by the {@code doWriteColValue} default branch and the
     * {@code validateDataSchema} DYNAMIC/UNKNOWN case, so the two gates cannot drift apart in
     * wording. Names only the column, its type, and the two escape hatches - never any part of
     * the record payload.
     */
    private String buildRowBinaryUnsupportedTypeMessage(String columnName, Type columnType) {
        return String.format(
                "Column [%s] has unsupported type [%s] for RowBinary serialization. "
                        + "Use insertFormat=json or bypassRowBinary=true instead.",
                columnName, columnType);
    }
```

**2. `validateDataSchema`'s gate — current state** (`:291-300`, verbatim):
```java
                    switch (colTypeName) {
                        case "Date":
                        case "Date32":
                        case "DateTime":
                        case "DateTime64":
                        case "UUID":
                        case "FIXED_STRING":
                        case "Enum8":
                        case "Enum16":
                            break;//I notice we just break here, rather than actually validate the type
```
No `case "DYNAMIC":`/`case "UNKNOWN":` today; both fall to `default:` (line 318) which sets
`validSchema = false` with only `LOGGER.error` — no throw. **WRT-02 discretion: do NOT add
`case "JSON":`** — upstream's own `if (type == Type.JSON) { ... continue; }` branch a few lines
below already owns JSON in this same switch's `default:` region; re-adding it would make
RowBinary-JSON fail-fast again, contradicting the locked "RowBinary + JSON" decision.

**Add**, as new case labels feeding the shared throw (mirroring the fork's pre-merge shape at
its pre-merge lines 267-282, JSON label dropped per discretion):
```java
                        case "DYNAMIC":
                        case "UNKNOWN":
                            String unsupportedTypeMsg = buildRowBinaryUnsupportedTypeMessage(colName, type);
                            LOGGER.error(unsupportedTypeMsg);
                            throw new DataException(unsupportedTypeMsg);
```
**Verification grep after edit:** `"JSON"` must not appear as a case label anywhere in this
switch.

**3. `doWriteColValue`'s `default:` — current state** (`:669-673`, verbatim, the exact silent-
corruption path):
```java
                default:
                    // If you wonder, how NESTED works in JDBC:
                    // https://github.com/ClickHouse/clickhouse-java/blob/6cbbd8fe3f86ac26d12a95e0c2b964f3a3755fc9/clickhouse-data/src/main/java/com/clickhouse/data/format/ClickHouseRowBinaryProcessor.java#L159
                    LOGGER.error("Cannot serialize unsupported type {}", columnType);
```
**Error handling pattern — change to** (fork's pre-merge shape, `git show pre-v1.6.0-sync`
lines 562-568, re-attached immediately after upstream's `case JSON:` block which ends at `:668`
— do not attach after `case ARRAY:`, upstream inserted `case JSON:` between them):
```java
                default:
                    // If you wonder, how NESTED works in JDBC:
                    // https://github.com/ClickHouse/clickhouse-java/blob/6cbbd8fe3f86ac26d12a95e0c2b964f3a3755fc9/clickhouse-data/src/main/java/com/clickhouse/data/format/ClickHouseRowBinaryProcessor.java#L159
                    String msg = buildRowBinaryUnsupportedTypeMessage(col.getName(), columnType);
                    LOGGER.error(msg);
                    throw new DataException(msg);
```
**Pitfall (documented, HIGH risk):** it is easy to re-type the comment + `LOGGER.error` from
memory and drop the `throw` — reproducing the exact bug this phase exists to fix. Verify via
`RowBinaryUnsupportedTypeGateTest`'s `Type.DYNAMIC`/`Type.UNKNOWN` parametrized cases
specifically report `DataException`, not "no exception thrown".

**4. Upstream's `case JSON:` (`:651-668`) — adopt as-is, WRT-03, no edit:**
```java
                case JSON:
                    if (value.getObject() == null) {
                        BinaryStreamUtils.writeNull(stream);
                        break;
                    }
                    if (csc.isBinaryFormatWrtiteJsonAsString()) {
                        if (value.getFieldType() == Schema.Type.STRUCT) {
                            byte[] jsonBytes = OBJECT_MAPPER.writeValueAsBytes(value);
                            BinaryStreamUtils.writeString(stream, jsonBytes);
                        } else if (value.getFieldType() == Schema.Type.STRING) {
                            BinaryStreamUtils.writeString(stream, ((String) value.getObject()).getBytes(StandardCharsets.UTF_8));
                        } else {
                            throw new RuntimeException("Unsupported field type: " + value.getFieldType() + " for column type [JSON]");
                        }
                        break;
                    } else {
                        throw new RuntimeException("Writing JSON in binary is not supported yet. Use `input_format_binary_read_json_as_string=1` in clickhouse settings to allow writing as string");
                    }
```
Note the upstream method-name typo `isBinaryFormatWrtiteJsonAsString()` is real — grep for that
exact misspelling if you need to find call sites later, not "Write".

**5. `start()`'s builder chain — WRT-05, `targetTableFilter` wiring:**
Current state ends (`:84-96`) `.enableReplicaPinning(csc.isEnableReplicaPinning()).build();` with
no `.setTargetTableFilter(...)` call. **Add**, reading CFG-01's new getter:
```java
                .setTargetTableFilter(csc.getTargetTableFilter())
```
into that same fluent chain, alongside the adjacent `.enableReplicaPinning(...)` call.

**6. D-16 log tweak (non-behavioural, still locked, land with whichever commit touches this
region):** `:220` currently: `"Trying to insert [{}] records to table name [{}] (QueryId: [{}])"`.
Add `schema type [{}]`, sourced from `first.getSchemaType()` (already in scope at `:219`).

**7. FREE-02 — verify only, no edit:** `:562` already reads (verbatim, confirmed):
```java
if (col.getMapValueType() != null && col.getMapValueType().isNullable() && mapValue != null) {
    BinaryStreamUtils.writeNonNull(stream);
}
```
This has the extra `!= null` guard the fork's own patch lacked. **Do not touch this line** —
record the fact in `MERGE-CHECKLIST.md § Recorded Facts` per D-18 instead.

---

### `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` (service/HTTP client, request-response+batch)

**Analog:** self, `git show pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`

Four independent re-expressions (HLP-01..04) plus two D-16 log tweaks.

**1. `buildTableFilterClause` (HLP-01) — transplants verbatim, pure static function, no upstream
touch needed:**
```java
    /**
     * Builds the {@code match(table, '...')} clause appended to the {@code system.columns} query
     * that backs {@link #showTables(String)}. Returns the empty string when {@code targetTableFilter}
     * is exactly the empty string (the connector default); otherwise doubles any single quote in the
     * filter before embedding it in the SQL literal. No backslash handling, no trim.
     */
    static String buildTableFilterClause(String targetTableFilter) {
        return targetTableFilter.isEmpty()
                ? ""
                : String.format(" AND match(table, '%s')", targetTableFilter.replace("'", "''"));
    }
```

**Current state — `showTables`/`showTablesV1`/`showTablesV2`** (`:333-377`, verbatim):
```java
    public List<Table.TableDesc> showTables(String database) {
        if (useClientV2) {
            return showTablesV2(database);
        } else {
            return showTablesV1(database);
        }
    }

    public List<Table.TableDesc> showTablesV1(String database) {
        List<Table.TableDesc> tables = new ArrayList<>();
        try (ClickHouseClient client = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build();
             ClickHouseResponse response = client.read(server)
                     .query(String.format("select database, table, count(*) as col_count from system.columns where database = '%s' group by database, table", database))
                     .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                     .executeAndWait()) {
            ...
        }
        return tables;
    }

    public List<Table.TableDesc> showTablesV2(String database) {
        List<Table.TableDesc> tablesList = new ArrayList<>();
        try (Records records = queryV2(String.format("select database, table, count(*) as col_count from system.columns where database = '%s' group by database, table", database))) {
            ...
        }
        return tablesList;
    }
```
No `targetTableFilter` field on the class today, and both methods build their SQL inline with no
filter clause.

**IMPORTANT — do not restore the fork's pre-merge shape here.** The fork's pre-merge
`showTables`/`showTablesV1`/`showTablesV2` took a pre-built **query string** parameter
(`showTables(String query)`). CONTEXT.md/RESEARCH.md explicitly recommend the *opposite*: inject
`+ buildTableFilterClause(targetTableFilter)` **inline** into each method's own
`String.format(...)` call, keeping the `(String database)` signature upstream now uses. Example
for `showTablesV1` (same shape for V2):
```java
                     .query(String.format("select database, table, count(*) as col_count from system.columns where database = '%s' group by database, table", database)
                             + buildTableFilterClause(targetTableFilter))
```

**Field + builder setter (fork's pre-merge, port verbatim):**
```java
    private final String targetTableFilter;         // instance field, set from builder.targetTableFilter
    // ...
        private String targetTableFilter = "";        // ClickHouseClientBuilder field, default ""
    // ...
        public ClickHouseClientBuilder setTargetTableFilter(String targetTableFilter) {
            this.targetTableFilter = targetTableFilter;
            return this;
        }
```
Add both following the exact pattern of the adjacent `setSslSocketSni`/`setClusterClause`
methods already present (`:732-745`).

**2. `shouldSkipDescribeRow` (HLP-02, D-08) — re-signed, NOT ported verbatim:**

**Structural fact driving the re-signature:** there is no `Table` object during either describe
loop — `columns` is a bare `List<Column>` (V1 line 406, V2 line 464), and
`new Table(database, tableName, hasDefaults, columns, numColumns)` is constructed only once,
*after* the loop (V1 line 443, V2 line 503). `Table.getAllColumnsMap()` is not available mid-loop.

**Fork's pre-merge method (verbatim, for behaviour only — signature must change):**
```java
    static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Table table) {
        if (!fieldDescriptor.isSubcolumn()) {
            return false;
        }
        String name = fieldDescriptor.getName();
        if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
            return true;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return false;
        }
        String parentName = name.substring(0, lastDot);
        Column parent = table.getAllColumnsMap().get(parentName);
        return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
    }
```
**Re-express as** (D-08 locked signature):
```java
    static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar) {
        if (!fieldDescriptor.isSubcolumn()) {
            return false;
        }
        String name = fieldDescriptor.getName();
        if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
            return true;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return false;
        }
        String parentName = name.substring(0, lastDot);
        Column parent = columnsSoFar.get(parentName);
        return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
    }
```
Only two tokens change: `Table table` → `Map<String, Column> columnsSoFar`, and
`table.getAllColumnsMap().get(parentName)` → `columnsSoFar.get(parentName)`. Do not construct a
throwaway `Table` per row inside the loop (rejected — quadratic, rebuilds via
`addColumn`'s dispatch on every row).

**Insertion point in both loops:** immediately after the existing alias/materialized/ephemeral
skip `continue` block (V1 ends `:428`, V2 ends `:488`) and before
`Column column = Column.extractColumn(fieldDescriptor);` (V1 `:434`, V2 `:494`). Both loops
already maintain `List<Column> columns` — add a sibling `Map<String, Column> columnsByName` and
put into it whenever a column is added to `columns`.

**3. `numColumns` ordering (HLP-03) — already correct, no code change, insertion-point discipline
only.** Current state, V1 (`:411-414`) and V2 (`:471-474`, identical shape):
```java
                // Count what system.columns counts: top-level columns, including the ones skipped below
                if (!fieldDescriptor.isSubcolumn()) {
                    numColumns++;
                }

                if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) {
```
The increment already precedes the alias/materialized/ephemeral skip for every top-level row.
HLP-03 is satisfied automatically as long as the new `shouldSkipDescribeRow` call is inserted
*after* this point (structurally required anyway).

**4. UTF-8 reader (HLP-04) — current state** (`:465-466`, verbatim):
```java
            try (QueryResponse queryResponse = client.query(describeQuery, settings).get();
                 BufferedReader br = new BufferedReader(new InputStreamReader(queryResponse.getInputStream()))) {
```
**Change to:**
```java
                 BufferedReader br = new BufferedReader(new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8))) {
```
Add `import java.nio.charset.StandardCharsets;` (not currently imported; existing `import
java.io.BufferedReader; import java.io.InputStreamReader;` block is at `:34-35`).
**Acceptance check (exact wording of HLP-04):** `grep -n "InputStreamReader(" src/main/.../ClickHouseHelperClient.java` returns exactly one match, now with `StandardCharsets.UTF_8`. `describeTableV1` never constructs a raw `InputStreamReader` — no V1 change needed or possible.

**5. D-16 log tweaks (non-behavioural, locked):**
- `:315` `System.out.println("query " + query + " retry …")` → `LOGGER.debug(...)`.
- `:437` and `:497` `LOGGER.warn("Unable to handle column")` → `LOGGER.error(...)`.

**6. FREE-03 — verify only, no edit.** `describeTableV2`'s query already builds from the method's
own `database` parameter (`:453`, verbatim: `String.format("DESCRIBE TABLE \`%s\`.\`%s\`", database, tableName)`), not `this.database`. Record in `MERGE-CHECKLIST.md § Recorded Facts`, do not
edit.

---

### `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (config, CRUD-parse)

**Analog:** self, `git show pre-v1.6.0-sync:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`

**1. `async_insert` un-forcing (CFG-02) — current state** (`:238-240`, verbatim):
```java
        this.addClickHouseSetting("input_format_skip_unknown_fields", "1", false);
        this.addClickHouseSetting("wait_end_of_query", "1", false);
        this.addClickHouseSetting("async_insert", "0", false);
```
**Change:** comment out line 240 exactly, matching the fork's pre-merge form verbatim:
```java
        //this.addClickHouseSetting("async_insert", "0", false);
```

**2. `TARGET_TABLE_FILTER` / `JSON_INSERT_COLUMNS` (CFG-01) — confirmed absent today** (zero grep
hits for `TARGET_TABLE_FILTER|JSON_INSERT_COLUMNS|targetTableFilter|jsonInsertColumns|jsonInsertFormatExplicit`). **Core pattern — port verbatim from fork's pre-merge:**
```java
    public static final String JSON_INSERT_COLUMNS = "jsonInsertColumns";
    // ...
    public static final String TARGET_TABLE_FILTER = "targetTableFilter";
    // field declarations
    private final String targetTableFilter;
    private final boolean jsonInsertFormatExplicit;
    private final List<String> jsonInsertColumns;
    // parsing, inside the constructor
    this.jsonInsertFormatExplicit = insertFormatRaw != null && insertFormatRaw.trim().equalsIgnoreCase("json");
    String jsonInsertColumnsRaw = props.getOrDefault(JSON_INSERT_COLUMNS, "").trim();
    List<String> jsonInsertColumnsTmp = new ArrayList<>();
    if (!jsonInsertColumnsRaw.isBlank()) {
        String[] stringSplit = jsonInsertColumnsRaw.split(",");
        for (String s : stringSplit) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                jsonInsertColumnsTmp.add(trimmed);
            }
        }
    }
    this.jsonInsertColumns = jsonInsertColumnsTmp;
    // ...
    this.targetTableFilter = props.getOrDefault(TARGET_TABLE_FILTER, "").trim();
    // ConfigDef.define blocks
    configDef.define(JSON_INSERT_COLUMNS,
            ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
            "Comma-separated list of column names to use for JSON inserts. Whitespace is ignored.",
            group, ++orderInGroup, ConfigDef.Width.LONG, "JSON insert columns."
    );
    configDef.define(TARGET_TABLE_FILTER,
            ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
            "Optional filter for destination table names using a regex pattern.",
            group, ++orderInGroup, ConfigDef.Width.MEDIUM, "Target Table Filter (regex pattern).");
```
Class carries a class-level `@Getter` (`:20`) — Lombok auto-generates `getTargetTableFilter()`,
`isJsonInsertFormatExplicit()`, `getJsonInsertColumns()` from these `private final` fields; no
manual getter needed.

**3. CFG-03 — thirteen defaults, verify only, no production edit.** All thirteen constant names
already present (byte-identical to `v1.6.0`): `AUTO_EVOLVE` (`:63`), `AUTO_EVOLVE_DDL_REFRESH_RETRIES` (`:64`), `AUTO_EVOLVE_STRUCT_TO_JSON` (`:65`), `BUFFER_COUNT` (`:56`), `BUFFER_FLUSH_TIME` (`:57`), `CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS` (`:58`), `CLUSTER_NAME` (`:67`), `DEBEZIUM_CDC_ENABLED` (`:60`), `ENABLE_REPLICA_PINNING` (`:68`), `CONNECTOR_RETRY_TIMEOUT` (`:66`), `REPORT_INSERTED_OFFSETS` (`:59`), `RETRY_ON_SOCKET_EXCEPTION` (`:39`), `SSL_SOCKET_SNI` (`:71`).

---

### `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` (controller, event-driven) — D-02 unblock-compile

**Analog:** itself — one-line change, no external analog needed.

**Current state** (`:148-151`, verbatim):
```java
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        close(partitions); // call newer method in case we are running in older runtime
    }
```
**Change:** delete the `@Override` annotation only (connect-api `4.1.1` does not declare this
method on `SinkTask`), keep everything else, add a comment naming the reason:
```java
    // No @Override: connect-api 4.1.1 removed SinkTask.onPartitionsRevoked (added by upstream
    // under its 2.7.0 pin); connect-api is declared api(...) so this still overrides the method
    // when loaded by a pre-3.0 worker, and costs nothing under 4.1.1.
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        close(partitions); // call newer method in case we are running in older runtime
    }
```
Do not delete the method; do not add a fork-only shim interface. This is the *only* edit to this
file in Phase 7 — everything else in it belongs to Phase 8 (D-03).

---

### `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` (test base fixture) — D-06 sliver

**Analog:** self, `git show pre-v1.6.0-sync:src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`

Restore **only** two members from the fork's pre-merge base class: `@Tag("clickhouse")` on the
class, and `uniqueConnectorName(TestInfo)` + its backing counter. Do **not** restore
`@AfterEach cleanupLeakedMBeans`, the shared-container strategy, or `newTestDatabaseName` — those
stay Phase 10. The fork's base is `static`-based; upstream's merged base is
`@TestInstance(PER_CLASS)` with instance methods — both restored members are shape-independent
so this is safe despite the base-class shape mismatch.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` (test) — D-08 map rewrite, 11 sites

**Analog:** itself, current form (`Read`/`Grep` verified this session — 11 confirmed sites).

**Pattern — every site currently does:**
```java
Table table = new Table("db", "t1");
table.addColumn(Column.extractColumn(newDescriptor("nullable_col", "Nullable(String)")));

boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
        newDescriptor("nullable_col.null", "UInt8"), table);
```
**Rewrite to** (build the map directly, matching the new signature):
```java
Map<String, Column> columnsSoFar = new HashMap<>();
columnsSoFar.put("nullable_col", Column.extractColumn(newDescriptor("nullable_col", "Nullable(String)")));

boolean skipped = ClickHouseHelperClient.shouldSkipDescribeRow(
        newDescriptor("nullable_col.null", "UInt8"), columnsSoFar);
```
The "parent intentionally never added" case (line 114, `// parent "json_col" intentionally never
added`) becomes simply an empty `Map<String, Column>` — do not put a placeholder entry. All 11
sites follow this identical shape (confirmed via grep, no divergent site found); mechanical, low
risk.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java` (test) — Table ctor + JSON assertion rewrite

**Analog:** itself, current form.

**Site needing the 5-arg constructor** (`:88-91`, verbatim):
```java
        Table table = new Table("test_db", "test_table");
        Column column =
                Column.builder().name("unsupported_col").type(type).isNullable(false).hasDefault(false).build();
        table.addColumn(column);
```
**Rewrite** (build the column first, pass a singleton list, `addColumn` is now private):
```java
        Column column =
                Column.builder().name("unsupported_col").type(type).isNullable(false).hasDefault(false).build();
        Table table = new Table("test_db", "test_table", false, List.of(column), 1);
```

**WRT-02 discretion — the JSON assertion at lines 40-57 must be rewritten.** Current test expects
a `DataException` with the fork's message for a `Type.JSON` column
(`doWriteColValueThrowsActionableExceptionForJsonColumnAndWritesNothing`). Since `"JSON"` is
dropped from the gate/message helper and upstream's own `case JSON:` now owns that path
exclusively, rewrite this one test's expectation to upstream's `RuntimeException` and its exact
message ("Writing JSON in binary is not supported yet. Use `input_format_binary_read_json_as_string=1`..." — the flag-off default path). The two `@EnumSource`/`@ParameterizedTest` methods that
iterate `{"JSON", "DYNAMIC", "UNKNOWN"}` must drop `"JSON"` from their `@EnumSource(names = ...)`
list (it no longer shares the `DataException` behaviour) — keep only `{"DYNAMIC", "UNKNOWN"}`
there, and cover `JSON`'s upstream `RuntimeException` behaviour separately in the now-rewritten
single-case test.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java` (test) — Table ctor rewrite + `@Disabled`

**Analog:** itself, current form.

**Site needing the 5-arg constructor** (`:75-76`, verbatim):
```java
        Table table = new Table("test_db", "test_table");
        table.addColumn(Column.builder().name("known_col").type(Type.STRING).build());
```
**Rewrite:**
```java
        Column knownCol = Column.builder().name("known_col").type(Type.STRING).build();
        Table table = new Table("test_db", "test_table", false, List.of(knownCol), 1);
```
**Then (D-11), add class- or method-level annotation:**
```java
@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")
```
Do not delete the test, do not weaken any assertion — only make it compile and disable it.
Same `@Disabled` treatment applies to `ClickHouseSinkTaskJsonInsertFormatTest.java`,
`ExplicitJsonInsertPathLiveTest.java`, `JsonInsertColumnsLiveTest.java`, and the R-01 labelled
loss test inside `ExplicitJsonInsertPathLiveTest.java` (no `Table`-constructor rewrite needed in
those three unless `Grep` at implementation time finds one — verify per file before assuming
"annotation only").

---

### `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java` (test, `@Tag("clickhouse")`)

**Analog:** itself, current form. This file does **not** construct `Table` via the 2-arg
constructor anywhere (confirmed via grep — its only `Table` usages are
`Table table = chc.describeTable(chc.getDatabase(), topic);`, which already returns a properly-
built `Table` from the real (already-upstream) `describeTable` implementation). Treat this file
as "verify it still compiles against `compileTestJava`" rather than "rewrite it" — CONTEXT.md
lists it among files needing rewrite but the mechanical audit found no non-compiling call site
today; re-verify at implementation time via `compileTestJava` and only touch what fails.

---

### `src/test/java/com/clickhouse/kafka/connect/sink/JsonColumnTest.java` (test) — stale import

**Analog:** itself, current form.

**Current import** (`:7`, verbatim):
```java
import com.clickhouse.kafka.connect.sink.junit.extension.FromVersionConditionExtension;
```
**Correct location, confirmed this session:**
```
src/testFixtures/java/com/clickhouse/kafka/connect/test/junit/extension/FromVersionConditionExtension.java
```
**Change the import to:**
```java
import com.clickhouse.kafka.connect.test.junit.extension.FromVersionConditionExtension;
```
(Leave `SinceClickHouseVersion`'s import at `:8` alone unless the same move applies to it — verify
via the same `find` at implementation time.)

---

### CFG-03 defaults test (new file) — service/config, CRUD

**Analog:** `src/test/java/com/clickhouse/kafka/connect/sink/AsyncInsertSettingsUnitTest.java` (full file read this session — best structural match: fork-only, Docker-free, extends nothing, package `com.clickhouse.kafka.connect.sink`, builds a minimal-props `ClickHouseSinkConfig` and asserts getters).

**Imports pattern** (from analog, lines 1-10):
```java
package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
```

**Core pattern (from analog's `minimalProps()`, reuse verbatim shape):**
```java
    private static Map<String, String> minimalProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        return props;
    }
```
**New test body — per D-15, a parametrized (or plain, thirteen-assertion) test** building one
default `ClickHouseSinkConfig` and asserting each Lombok-generated getter against upstream's
default, e.g.:
```java
    @Test
    @DisplayName("CFG-03: all thirteen new upstream config options reach their getters at upstream defaults")
    void thirteenNewUpstreamOptionsReachDefaults() {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(minimalProps());
        assertFalse(csc.isAutoEvolve());
        // ... one assertion per getter listed in ClickHouseSinkConfig section above (CFG-03),
        //     naming the constant, default, and consumer in the SUMMARY per D-15 — a getter
        //     assertion alone can't show the consumer, that's the SUMMARY's job, not the test's.
    }
```
Class doc-comment should follow the analog's style: explain which requirement IDs it pins
(CFG-03), why it's fork-only/Docker-free (extends nothing, stays in `testUnit`).

**Class extending nothing, not `ClickHouseBase`** — this is the load-bearing reason
`AsyncInsertSettingsUnitTest`/`JsonInsertColumnsUnitTest`/`MetadataFreeJsonInsertUnitTest`/
`RowBinaryUnsupportedTypeGateTest` all stay in `testUnit`: `ClickHouseBase`'s `@Tag("clickhouse")`
(once D-06 restores it) would pull any subclass out of the Docker-free gate.

## Shared Patterns

### Fork-vs-upstream re-expression discipline
**Source:** D-19 (CONTEXT.md), confirmed via `git diff v1.6.0 HEAD` = 0 lines for all seven
`src/main` files this phase touches.
**Apply to:** every production file above.
Every edit in this phase is an isolated diff against a known-good upstream baseline — never a
merge-conflict resolution. Commit shape: one fork behaviour per commit (unblock-compile →
`Type.DYNAMIC` + `Column` → RowBinary fail-fast → `targetTableFilter` → describe skip + UTF-8 →
config), each compiling after landing, each independently `git show pre-v1.6.0-sync`-checkable.

### DataException over silent LOGGER.error for unsupported RowBinary types
**Source:** `ClickHouseWriter.java` `buildRowBinaryUnsupportedTypeMessage` (new), applied at both
`validateDataSchema`'s gate and `doWriteColValue`'s `default:`.
**Apply to:** `ClickHouseWriter.java` only (two call sites) — do not create a second message
format anywhere else; both gates must throw byte-identical wording so they cannot drift.

### Map/List-based lookup during construction of an otherwise-immutable model object
**Source:** `shouldSkipDescribeRow(ClickHouseFieldDescriptor, Map<String, Column>)` (D-08),
`ClickHouseHelperClient`'s two describe loops.
**Apply to:** any future helper needing mid-construction lookups against `Table`/`Column` now
that `Table` is immutable (5-arg constructor, `addColumn` private) — build the loop-local
`Map<String, X>` (or `List<X>`) incrementally, construct the immutable object once at the end.
Never construct a throwaway immutable object per iteration just to get lookup access.

### UTF-8 on every stream reader touching ClickHouse HTTP responses
**Source:** `ClickHouseHelperClient.java:466`, `java.nio.charset.StandardCharsets.UTF_8`.
**Apply to:** `describeTableV2`'s `InputStreamReader` only in this phase (the sole occurrence,
confirmed via grep) — but any future `InputStreamReader`/`Reader` construction against a
ClickHouse HTTP response in this file must carry an explicit `StandardCharsets.UTF_8` argument;
the project's JDK-17 test-JVM is pinned to `windows-1252`, so platform-default charset silently
mangles non-ASCII identifiers under `./gradlew test`.

### Config keys: constant + field + parsing + `configDef.define`, four-part unit
**Source:** `ClickHouseSinkConfig.java`'s `TARGET_TABLE_FILTER`/`JSON_INSERT_COLUMNS` (CFG-01).
**Apply to:** any new connector setting — always land all four parts together (public static
final `String` key constant, `private final` field, parsing inside the constructor reading
`props.getOrDefault(KEY, default).trim()`, and a `configDef.define(...)` block with
Importance/group/order/Width/display-name) in the same commit; Lombok's class-level `@Getter`
auto-generates the accessor, no manual getter needed.

## No Analog Found

None. Every file this phase touches (or creates) has a same-file pre-merge analog via
`git show pre-v1.6.0-sync:<path>`, or — for the one new test file (CFG-03 defaults) — a strong
role-match analog already in the tree (`AsyncInsertSettingsUnitTest.java`).

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/sink/{db/mapping,db,db/helper,}`, `src/test/java/com/clickhouse/kafka/connect/sink/{,db,db/helper,db/mapping}`, plus `pre-v1.6.0-sync` tag (whole-tree `git show`) and current working tree (`sync/v1.6.0`, byte-identical to `v1.6.0` in this phase's scope).
**Files scanned:** 7 `src/main` files (all `git ls-files`-tracked, confirmed non-mirror), 11 `src/test` files, 1 new test file planned.
**Pattern extraction date:** 2026-09-19
**Primary source:** `.planning/phases/07-type-mapping-writer-helper-client-config/07-RESEARCH.md` Patterns 1-6 (all excerpts independently `Read`/`git show`-verified this session, cross-checked against this session's own `Read`/`Bash` calls for line-number drift — none found), `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` D-01..D-19.
