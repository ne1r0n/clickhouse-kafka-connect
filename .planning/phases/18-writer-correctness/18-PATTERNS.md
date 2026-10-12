# Phase 18: Writer Correctness - Pattern Map

**Mapped:** 2026-10-12
**Files analyzed:** 17 (6 modified source/doc, 11 new or extended test/doc files)
**Analogs found:** 17 / 17 (one partial: LogCapture, no in-repo analog for the appender)

All analog paths below were checked as git-tracked. Line numbers are current as read on 2026-10-12 and shift commit to commit, so re-read before editing. `W` = `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`. `T` = `src/test/java/com/clickhouse/kafka/connect/sink`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `W` (`writeRecordsJson` ~L1353, `writeRecordsRowBinary` ~L1274): shared skip helper | writer (serialization loop) | batch / transform | existing inline skip in `W` L1365-1376 | exact (move) |
| `W` `doWriteCol` else branch ~L1091-1095 (WRT-10) | writer | transform | `buildNestedNullMessage` / `NESTED_NULL_MESSAGE_FORMAT` L554-566 | exact |
| `W` `doInsertRawBinary` catch ~L1175-1191 (WRT-11) | writer error path | request-response | same catch block (self) | exact |
| `W` `doWriteDates` ~L461, L515, L523-526 (WRT-12) | writer | transform | ClassCastException catch in `doWriteColValue` ~L843-853 (data-free message) | role-match |
| `W` `doWriteColValue` generic catch ~L854-857 (D-09) | writer | transform | same ClassCastException catch | role-match |
| `src/main/java/.../util/Utils.java` L121-127 (D-05) | utility | request-response | `W` WRT-11 guard (same fix family) | role-match |
| `docs/FORK-DELTA.md` | doc | n/a | existing rows and Known costs (L20-60, L122-147) | exact |
| `.planning/scripts/fork-anchors.txt` | config | n/a | existing rows L12-23 | exact |
| `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` (new) | doc | n/a | `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` | exact |
| New Docker-free test: empty-CDC byte-identical / defaults-only (reflective `writeRecordsJson`) | test | batch | `T/db/MetadataFreeJsonInsertUnitTest.java` | exact |
| New Docker-free test: exactly-once state with empty-CDC batch | test | event-driven | `T/processing/ProcessingTest.java` (`InMemoryDBWriter`) | exact |
| New live test: per-route empty-CDC (3 routes) | test | CRUD | `T/ExplicitJsonInsertPathLiveTest.java#metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes` L378-441 | exact |
| New Docker-free test: WRT-10 DataException (flat + `Array(Tuple)`) | test | transform | `T/db/NestedNullableCompositeWriteTest.java` (`assertNestedNullThrows` L153) | exact |
| New Docker-free test: WRT-11 null-message `doInsertRawBinary` | test | request-response | `T/db/MetadataFreeJsonInsertUnitTest.java` (reflection seam) + `T/db/ClickHouseWriterTest.java` ~L694 (`chc` injection) | role-match |
| Extended `T/util/UtilsTest.java` (D-05 null-message IOException) | test | request-response | same file L51-57 | exact |
| New `LogCapture` helper (e.g. `T/util/LogCapture.java`) + WRT-12/D-09 tests | test support | event-driven | none in repo; `src/test/resources/log4j.properties` for backend | partial |
| `T/dlq/InMemoryDLQ.java` (additive getter) and `put()`-level pins | test support / test | pub-sub | `T/dlq/InMemoryDLQ.java` itself; `T/ClickHouseSinkTaskTest.java` for task harness | role-match |

## Pattern Assignments

### Shared empty-CDC skip helper (`W`, writer, batch)

**Analog:** the inline block in `writeRecordsJson` (W L1353-1376). Move it, do not copy it.

Current code to move (L1357-1376):
```java
for (Record record : records) {
    if (record.getSinkRecord().value() != null) {
        LOGGER.trace("Record: {}", record.getTopicAndPartition());
        ...
        } else if (metadataFree && record.getSchemaType() == SchemaType.DEBEZIUM_CDC) {
            // op=t and a null before/after struct convert to an empty map, which would
            // otherwise land as an all-defaults row.
            if (record.getJsonMap() == null || record.getJsonMap().isEmpty()) {
                LOGGER.debug("Skipping empty CDC record (no values) topic [{}] partition [{}] offset [{}]",
                        record.getTopic(),
                        record.getRecordOffsetContainer().getPartition(),
                        record.getRecordOffsetContainer().getOffset());
                continue;
            }
            beforeSerialize = System.currentTimeMillis();
            bytes = MetadataFreeJsonWriter.cdcRow(record.getJsonMap());
```
Target shape (from RESEARCH Pattern 1): `private static boolean skipEmptyCdcRecord(Record)`, schema type check first, no `Optional`, no lambda, same log text, constant for the format. Call it right after `if (record.getSinkRecord().value() != null) {` in both `writeRecordsJson` (L1357, hoisted above the three-way branch) and `writeRecordsRowBinary` (L1277, before the column loop):
```java
private long writeRecordsRowBinary(List<Record> records, Table table, OutputStream stream, boolean supportDefaults) throws IOException {
    long pushStreamTime = 0;
    for (Record record : records) {
        if (record.getSinkRecord().value() != null) {
            for (Column col : table.getRootColumnsList()) {
```
Keep the `metadataFree && DEBEZIUM_CDC` branch (it still builds `cdcRow`) so `metadataFree` stays used.

**Trap:** `fork-anchors.txt` pins the literal `shouldSkipTableMetadata()` at exactly 8 lines in `W`. Never write that string in the helper's comments or Javadoc; say "the metadata-free predicate". Run `grep -cF "shouldSkipTableMetadata()" W` (must be 8) before each commit. `buildRowBinaryUnsupportedTypeMessage` is pinned at 5 and `if (value.getObject() == null && col.isNullable()) {` at 1.

---

### WRT-10 throw site (`W` `doWriteCol` ~L1091-1095, writer, transform)

**Analog:** `NESTED_NULL_MESSAGE_FORMAT` and `buildNestedNullMessage` (W L554-566).
```java
private static final String NESTED_NULL_MESSAGE_FORMAT = "column '%s' (%s) %s: null in non-nullable slot";
...
return String.format(NESTED_NULL_MESSAGE_FORMAT, container.getName(), renderColumnType(container), slotKind);
```
Code to replace (L1091-1095):
```java
} else {
    // no filled and not nullable
    LOGGER.error("Column {} is not nullable and no value is provided", name);
    throw new RuntimeException();
}
```
Replace with a `DataException` built from a new constant beside `NESTED_NULL_MESSAGE_FORMAT` (text locked by D-10) and drop the `LOGGER.error` line (D-11):
```java
private static final String MISSING_NON_NULLABLE_MESSAGE_FORMAT =
        "column '%s' (%s): no value provided for a non-nullable column without a default; "
                + "add the field, make the column Nullable, or give it a DEFAULT";
throw new DataException(String.format(MISSING_NON_NULLABLE_MESSAGE_FORMAT, name, renderColumnType(col)));
```
`renderColumnType` renders the connector `Type` enum name (`INT32`, `Array(INT32)`), not the ClickHouse string. Do not touch the sibling `RuntimeException("An attempt to write null into not nullable column '%s'")` branches (L-12).

---

### WRT-11 (`W` `doInsertRawBinary` catch ~L1175-1191, writer error path)

Current code (L1176-1178), note the NPE is at the `filter` lambda and the later `!= null` check comes too late:
```java
} catch (Exception e) {
    // Note: this part will be removed once V1 is deprecated
    Optional<String> updateTableException = UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE.keySet().stream().filter(code -> e.getMessage().contains(code)).findFirst();
    if (e.getMessage() != null && updateTableException.isPresent() && retry) {
```
Fix: read `String failureMessage = e.getMessage();` once; null yields `Optional.empty()`; `failureMessage::contains` otherwise. The retry body (`pinReplica` / `urgentTableUpdate` / recursive call with `false` / `unpinReplica`) and the `else { LOGGER.error("Error inserting records", e); throw e; }` stay unchanged. Note the first `catch` above it (L1168-1172) has the same `LOGGER.error` + `throw e` shape; leave it.

### `Utils.handleException` (D-05, `Utils.java` L121-127, utility)

Current:
```java
} else if (rootCause instanceof IOException) {
    final String msg = rootCause.getMessage();
    if (msg.indexOf(CLICKHOUSE_CLIENT_ERROR_READ_TIMEOUT_MSG) == 0 || msg.indexOf(CLICKHOUSE_CLIENT_ERROR_WRITE_TIMEOUT_MSG) == 0) {
        LOGGER.warn("IOException thrown, wrapping exception: {}", e.getLocalizedMessage());
        throw new RetriableException(e);
    }
}
```
Fix: `if (msg != null && (msg.indexOf(...) == 0 || msg.indexOf(...) == 0)) {`. A `SocketTimeoutException` is already caught earlier at L112, so the null-message case is retriable there already (pin, not red-green). Needs an anchor row and a FORK-DELTA row (line is upstream-identical; `Utils.java` has no anchor rows yet).

---

### WRT-12 and D-09 log masking (`W` `doWriteDates` L461, L515, L523-526; `doWriteColValue` ~L854)

**Analog for data-free message plus rethrow:** the `ClassCastException` catch at W L843-853 (stays as is):
```java
} catch (ClassCastException e) {
    String sourceType = value == null ? "null" : String.valueOf(value.getFieldType());
    String valueClass = (value == null || value.getObject() == null) ? "null" : value.getObject().getClass().getName();
    String message = String.format(
            "Failed to write column `%s`: cannot convert Kafka value of type %s (value class: %s) to ClickHouse type %s",
            col.getName(), sourceType, valueClass, columnType);
    LOGGER.error(message, e);
    throw new DataException(message, e);
```
Code to change:
```java
// L461 (DateTime)
LOGGER.error("Error parsing date time string: {}", value.getObject());
// L515 (DateTime64)
LOGGER.error("Error parsing date time string: {}, exception: {}", value.getObject(), e.getMessage());
// L523-526
String msg = String.format("(Potentially) Not implemented conversion from %s to %s", value.getFieldType(), type);
// ~L854 generic catch
LOGGER.error("Error writing value of " + (value == null ? "<value null>" : value.getFieldType() ) + " to the column `" + col.getName() + "` of type " + columnType, e);
throw e;
```
Target: one private helper for the two DateTime logs (column name, ClickHouse type, Kafka field type, `e.getClass().getName()`, `String.valueOf(value.getObject()).length()`), format kept in a static constant; never pass `e` or `e.getMessage()`. The `:854` log drops the throwable argument but keeps `throw e;`. Keep the "(Potentially) Not implemented conversion from" prefix and append column name and type from a constant. See RESEARCH "WRT-12 / D-09" snippet and Pitfalls 8, 9, 10, 11.

---

### Docker-free tests

**Writer reflection seam** (`T/db/MetadataFreeJsonInsertUnitTest.java` L50-72): reuse for `csc` injection, and for `getDeclaredMethod("writeRecordsJson"/"writeRecordsRowBinary", ...).setAccessible(true)`.
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
private static Map<String, String> minimalProps() { ... HOSTNAME/PORT/DATABASE/USERNAME/PASSWORD ... }
```
The class deliberately extends nothing (not `ClickHouseBase`, whose `@Tag("clickhouse")` drops it from `testUnit`). Same for all Docker-free classes. `chc` is injected the same way (`WRITER_HELPER_CLIENT_FIELD = "chc"`, `T/db/ClickHouseWriterTest.java` ~L694) with a Mockito mock of `ClickHouseHelperClient`.

**WRT-10 test** (`T/db/NestedNullableCompositeWriteTest.java` L135-155): copy the helpers.
```java
private static ClickHouseWriter newWriter() {
    return new ClickHouseWriter(new SinkTaskStatistics(null, 0));
}
private static byte[] writeColumnValue(Column col, Data data, boolean defaultsSupport) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    newWriter().doWriteColValue(col, out, data, defaultsSupport);
    return out.toByteArray();
}
private static DataException assertNestedNullThrows(Column col, Data data, boolean defaultsSupport) {
    return assertThrows(DataException.class, () -> writeColumnValue(col, data, defaultsSupport));
}
```
and the assertion style (L194-197): `assertEquals("column 'tags' (Array(STRING)) element: " + NESTED_NULL_TEXT, ex.getMessage()); assertFalse(ex.getMessage().contains(SECRET_ELEMENT));`. For WRT-10 the missing value goes through `doWriteCol` (not `doWriteColValue`), so call `doWriteCol(null, false, col, out, defaultsSupport)` for the flat case and build an `Array(Tuple(...))` column from `Column.extractColumn(newDescriptor(...))` for the nested case (child name is the dotted path, e.g. `at.a`). Do not touch the L-12 `RuntimeException` test at ~L375.

**WRT-11 test:** subclass `ClickHouseWriter`, override protected `doInsertRawBinaryV1/V2` (and `updateMapping`/`getTable` for the retry twin), inject `chc` and `csc` reflectively; assert `assertSame(boom, thrown)` for `new RuntimeException()` and exactly two hook calls for a message containing `"ClickHouseException: Code: 33"`. Walk cause chains in any `put()`-level assertion, never `instanceof` on the top exception.

**D-05 unit cases** (`T/util/UtilsTest.java` L51-57 style):
```java
assertThrows(RetriableException.class, () -> {
    Exception timeout = new IOException("Write timed out after 30000 ms");
    Utils.handleException(timeout, false, false, new ArrayList<>());
});
```
Add a null-message `new IOException()` (not retriable, no NPE, falls to tolerance handling) and a null-message `new SocketTimeoutException()` (retriable, a pin).

**Exactly-once state test:** `T/processing/ProcessingTest.java` (uses `new InMemoryDBWriter()` at L80 and following, `InMemoryState`); do not mock `ClickHouseWriter`.

---

### Live per-route empty-CDC tests (`T/ExplicitJsonInsertPathLiveTest.java` L378-441)

**Analog:** `metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes`. Copy its structure for the three routes.
```java
Map<String, String> props = getBaseProps();
String topic = createTopicName("jsonp05_cdc_route");
props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
props.put(ClickHouseSinkConfig.DEBEZIUM_CDC_ENABLED, "true");
props.put("name", uniqueConnectorName(testInfo));
ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
ClickHouseTestHelpers.dropTable(chc, topic);
createTable(chc, topic, "CREATE TABLE %s (id Int32, ts DateTime64(3, 'UTC'), amount Decimal(18,4), name String, "
        + "_version UInt64, is_deleted UInt8) ENGINE = MergeTree ORDER BY id");
ClickHouseSinkTask sinkTask = new ClickHouseSinkTask();
try {
    sinkTask.start(props);
    Struct[] envelopes = { cdcEnvelope("c", null, cdcRow(1, "created"), 100L),
        cdcEnvelope("t", null, null, 110L), cdcEnvelope("u", null, null, 120L),
        cdcEnvelope("d", cdcRow(2, "deleted"), null, 101L) };
    for (...) records.add(new SinkRecord(topic, 0, null, null, CDC_ENVELOPE_SCHEMA, envelopes[i], i + 1));
    assertDoesNotThrow(() -> sinkTask.put(records));
    List<Map<String, String>> rows = allRows(chc, String.format("SELECT id, ... FROM `%s` ORDER BY id", topic), "id", ...);
    assertEquals(2, rows.size());
} finally { sinkTask.stop(); ClickHouseTestHelpers.dropTable(chc, topic); }
```
`CDC_ENVELOPE_SCHEMA` (L350), `cdcEnvelope` (L365), `cdcRow` and `allRows` are private to that class: either add the new routes as extra methods in it, or extract the fixtures (DRY). Route variations: `bypassRowBinary=true` with no `insertFormat` (do not assert `ts`, known L-03); RowBinary with `DEBEZIUM_CDC_ENABLED=true` on a table with DEFAULT columns (on a plain non-null table the pre-fix failure is a failed batch, not a phantom row). Add an all-empty batch case per route (`assertDoesNotThrow`, 0 rows). Client-v2 `GenericRecord` is a reused view: `allRows` snapshots inside the iteration, so keep using it.

---

### `LogCapture` helper (D-12, no in-repo analog)

Backend: reload4j 1.2.22 via `slf4j-reload4j`, `src/test/resources/log4j.properties` sets `com.clickhouse.kafka=DEBUG`. Use an `AppenderSkeleton` subclass (see the RESEARCH "Log-capture helper" shape), `AutoCloseable` with detach plus level restore in `close()`, attached to `Logger.getLogger(ClickHouseWriter.class)`. Expose rendered message, `getThrowableStrRep()` lines and every `getMessage()` down the cause chain; `getThrowableInformation()` may be null. Tests need a column-name positive assertion, a sentinel absence assertion, and a self-check that the raw JDK exception message actually embeds the sentinel (Pitfalls 8, 9). DateTime64 branch reads `csc.getDateTimeFormats()` inside the `try`, so inject `csc`.

### `InMemoryDLQ` (`T/dlq/InMemoryDLQ.java`)

Currently only `report()` and `size()`; the stored `Throwable` is private. Add an additive getter for the `put()`-level pins (test-only file). To inject the null-message failure into `put()` see RESEARCH Open Question 1 (reflective swap of `ProxySinkTask.processing`, else drive `Processing.doLogic` with a throwing `DBWriter` and pass the exception to `Utils.handleException`).

---

### Docs and scripts (same commit as each code fix)

- `.planning/scripts/fork-anchors.txt`: 4 TAB-separated fields `path<TAB>fixed-string<TAB>count<TAB>backs-ID`, e.g.
  `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java	buildRowBinaryUnsupportedTypeMessage	5	WRT-01`.
  The gate evaluates each commit against its own copy of the file, so code, anchor row and FORK-DELTA row must share a commit. Measure counts with `grep -cF -e <pattern> <file>` in that commit.
- `docs/FORK-DELTA.md`: new rows for the `writeRecordsRowBinary` skip, `doWriteDates` logs and exception, the `:854` log, the `Utils` IOException guard. Close Known costs item 2, item 3 (WR-03), the T-13-19 half of item 6 and the WR-04 clause of item 5 (RESEARCH Pitfall 12); add the `:854` rethrow residual (name the L1172/L1188 `LOGGER.error("Error inserting records", e)` lines too).
- `18-BEHAVIOUR-LEDGER.md`: table header `| ID | Source plan | Behaviour change | Decisions | Pinned by | Operator-visible | Phase 20 destination |` (adapted from Phase 13's `Phase 15 destination`), plus a Purpose paragraph and a "Destination key". Add one row per change in the fix's commit; the empty-CDC row should state token-based dedup (Pitfall 3) and the JMX counting gap (Pitfall 14).

## Shared Patterns

### Data-free messages (all new exception and log text)
**Source:** W L554-566 (`NESTED_NULL_MESSAGE_FORMAT`, `buildNestedNullMessage`) and W L843-853.
**Apply to:** WRT-10, WRT-12, D-09. Static format constants, built from `Column` metadata only (name, rendered type, Kafka field type, exception class, value length). No record value, no throwable message as an argument.

### Writer-error classification
**Source:** `Utils.handleException` (`Utils.java` L108-130). Classifies by exception type, error code and root cause, never by message. `Processing.doInsert` wraps failures in `RuntimeException(queryId, e)`, and client V2 wraps in `ClickHouseException: Insert failed`, so tests must search the cause chain.

### Docker-free vs live test split
**Source:** `MetadataFreeJsonInsertUnitTest` (extends nothing; runs under `testUnit`) versus `ExplicitJsonInsertPathLiveTest` (extends `ClickHouseBase`, run with `./gradlew test`). Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Read `ClickHouseTestHelpers.java` before writing live tests.

### Hot-path rule
**Apply to:** the skip helper. No `Optional`, lambda, boxing, or `String.format` in the per-record loop; the DEBUG call allocates only when a record is skipped.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `LogCapture` test helper | test support | event-driven | No log-capture helper exists; build from reload4j `AppenderSkeleton` per RESEARCH |
| `put()`-level null-message injection | test | request-response | `ProxySinkTask` is final and builds its own writer and `Processing`; reflective swap is untested (RESEARCH A4) |

## Metadata

**Analog search scope:** `src/main/java/.../sink/db`, `.../util`, `src/test/java/.../sink` (db, util, dlq, processing), `.planning/scripts`, `docs`, Phase 13 milestone folder
**Files scanned:** about 20 read or grepped
**Pattern extraction date:** 2026-10-12
