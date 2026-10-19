# Phase 22: Writer Record-Data Exposure - Pattern Map

**Mapped:** 2026-10-19
**Files analyzed:** 17 (new or modified)
**Analogs found:** 16 / 17

All analog paths below were checked with `git ls-files` and are tracked source. None is a gitignored mirror.
Line numbers are from HEAD of `sync/v1.7.0`. Do not re-derive them from memory: re-grep before editing.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (generic catch ~L870, new constants near L564-570, private helpers) | service (serializer) | transform / request-response | itself: CCE branch ~L858-869 + `NESTED_NULL_MESSAGE_FORMAT` / `WRITE_VALUE_FAILURE_LOG_FORMAT` constants | exact |
| `src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java` (NEW, name is discretion) | utility (exception) | transform | no sanitized-throwable analog; closest style: other `util` classes (`Utils`, `QueryIdentifier`) | partial |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (D-14, `convertEnumValues` ~L524-529) | model | transform | itself (one throw-site text edit) | exact |
| `src/test/.../sink/db/ClickHouseWriterNullMessageUnitTest.java` (HARN-08, edit) | test | request-response (hooks) | itself + `CapturingClickHouseWriter` (no-Mockito subclass seam) | exact |
| `src/test/.../sink/db/CountingHelperClient` or nested class (NEW, counting `ClickHouseHelperClient` subclass) | test fixture | event-driven (call counting) | `CapturingClickHouseWriter` (no-Mockito, reflection on private field) | role-match |
| `src/test/.../sink/WriterFailureClassificationUnitTest.java` (NEW, WRT-14 pin; package `...connect.sink` because `ChunkFlusher` ctor is package-private) | test | request-response | `UtilsNullMessageClassificationUnitTest` (outcome style) + `WriterErrorPathLiveTest` (reflection swap of `Processing.dbWriter`) | role-match |
| `src/test/.../sink/db/<ScriptedWriteFailureWriter>` (NEW, writer subclass that calls the REAL `doWriteColValue`) | test fixture | transform | `ClickHouseWriterNullMessageUnitTest.ScriptedInsertWriter` + `CapturingClickHouseWriter` | exact |
| `src/test/.../sink/db/WriterLogMaskingUnitTest.java` (flip `uuidWriteFailureRethrowsOriginalWithValueInMessage` ~L251; new sentinel tests) | test | transform | itself (SENTINEL, `capture`, `assertNoSentinel`, `writeColValue`) | exact |
| new Docker-free WRT-13 sentinel test class (writer-level + `doInsertRawBinary` V1/V2 + D-03/D-04/D-14 cases) | test | transform | `WriterLogMaskingUnitTest` + `ClickHouseWriterNullMessageUnitTest` | exact |
| `src/test/.../sink/WriterErrorPathLiveTest.java` (add live put() cases, D-12) | test (live) | request-response | itself (`putBatch`, `FailingInsertWriter`, `causeChain`) | exact |
| `src/test/.../sink/dlq/InMemoryDLQ.java` (add `exceptions()` accessor) | test fixture | event-driven | itself | exact |
| `src/test/.../sink/ClickHouseSinkTaskWithSchemaTest.java` (flip `schemaWithFixedStringMismatchTest` ~L887-912, F2) | test (live) | request-response | itself; baseline-red spotless file | exact |
| `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md` (NEW) | doc | n/a | `.planning/milestones/v1.7.0_gg2-hardening-phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` and `.../19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md` | exact |
| `docs/FORK-DELTA.md` (item 6, WRT-12 row ~L41, new row) | doc | n/a | the WRT-12 row at L40-41 | exact |
| `.planning/scripts/fork-anchors.txt` (new rows) | config | n/a | rows 20-69 (WRT-10 / WRT-12 rows) | exact |
| `.planning/ROADMAP.md` (`### Phase 999.3`, ~L287) | doc | n/a | sibling "Delivered" lines (see Pitfall 5 in RESEARCH) | role-match |
| 22 evidence file (D-13 trace, D-03 result, red/green runs) | doc | n/a | Phase 17/20/21 evidence files | role-match |

## Pattern Assignments

### `ClickHouseWriter.java` generic catch (service, transform)

**Analog:** the same method's CCE branch and the existing constants.

**Constants pattern** (L564-570), put the new ones directly beside these; keep one constant per line so anchor counts stay line-based:
```java
    // Slot kinds name the position of a nested null in the schema; none of them carries record data.
    private static final String NESTED_NULL_MESSAGE_FORMAT = "column '%s' (%s) %s: null in non-nullable slot";
    private static final String WRITE_VALUE_FAILURE_LOG_FORMAT = "Error writing value of {} to the column `{}` of type {}: exception {}";
    private static final String MISSING_NON_NULLABLE_MESSAGE_FORMAT = "column '%s' (%s): no value provided for a non-nullable column without a default; "
            + "add the field, make the column Nullable, or give it a DEFAULT";
```

**Core pattern to mirror** (L858-874, the CCE branch is the D-07 message precedent and the catch to edit):
```java
        } catch (ClassCastException e) {
            String sourceType = value == null ? "null" : String.valueOf(value.getFieldType());
            String valueClass = (value == null || value.getObject() == null) ? "null" : value.getObject().getClass().getName();
            String message = String.format(
                    "Failed to write column `%s`: cannot convert Kafka value of type %s (value class: %s) to ClickHouse type %s",
                    col.getName(), sourceType, valueClass, columnType);
            LOGGER.error(message, e);
            throw new DataException(message, e);
        } catch (Exception e) {
            LOGGER.error(WRITE_VALUE_FAILURE_LOG_FORMAT, value == null ? "<value null>" : value.getFieldType(), col.getName(), columnType,
                    e.getClass().getName());
            throw e;
        }
```
Rules for the edit:
- Leave the CCE branch byte-identical (Pitfall 6; `ClickHouseWriterColumnContextTest` pins its text). Add a new prefix constant and use it only in the new message.
- Keep the `LOGGER.error(WRITE_VALUE_FAILURE_LOG_FORMAT, ...)` statement exactly; the anchor `WRITE_VALUE_FAILURE_LOG_FORMAT 2 WRT-12` counts the definition line plus this use line.
- Replace only `throw e;` by `if (<passThrough>) throw e; throw new DataException(msg, WithheldMessageException.of(e));`. Do not reassign `e` (precise rethrow depends on it). Put the predicate and message builder in private helpers; the null-safe expressions are the same as the CCE branch (Pitfall 7).
- Pass-through set (D-01, D-03): any `IOException` in the cause chain (identity-guarded loop), any `ConnectException`, or `e.getClass() == RuntimeException.class`.

**Message builder** (RESEARCH "Code Examples", D-07, D-08 no length, D-09 enum token, expected text for UUID):
```java
    private static String buildWriteFailureMessage(Column col, Type columnType, Data value, Exception cause) {
        String sourceType = value == null ? "null" : String.valueOf(value.getFieldType());
        String valueClass = (value == null || value.getObject() == null) ? "null" : value.getObject().getClass().getName();
        return String.format(WRITE_VALUE_CONVERSION_FAILURE_FORMAT, col.getName(), sourceType, valueClass, columnType, cause.getClass().getName());
    }
```
**Import placement (D-16):** the file is spotless-baseline-red (19 lines). Do not reorder existing imports. `org.apache.kafka.connect.errors.ConnectException` sorts immediately before the existing `org.apache.kafka.connect.errors.DataException` import; the new fork class goes in its sorted spot among `com.clickhouse.kafka.connect.util.*` imports.

**Downstream readers that must not change** (do not edit): `doInsertRawBinary` generic catch reading `String failureMessage = e.getMessage();` (anchor, count 1) and `Utils.handleException`.

---

### `WithheldMessageException.java` (utility, transform) - NEW

**Analog:** no existing sanitized-throwable class. Follow RESEARCH Pattern 2 verbatim for structure; copy only project style from a `util` class (PascalCase, static constants, `final`, Javadoc on non-obvious invariants).

**Core pattern** (RESEARCH Pattern 2; D-05/D-06):
```java
public final class WithheldMessageException extends RuntimeException {
    private static final String WITHHELD_NOTE = "(message withheld: may contain record data)";
    private static final int MAX_CAUSE_DEPTH = 32;
    private final String originalClassName;

    private WithheldMessageException(String originalClassName, Throwable sanitizedCause) {
        super(WITHHELD_NOTE, sanitizedCause, /* enableSuppression */ false, /* writableStackTrace */ true);
        this.originalClassName = originalClassName;
    }
    public static WithheldMessageException of(Throwable original) { /* setStackTrace(t.getStackTrace()) per node; identity + depth guard */ }
    public String getOriginalClassName() { return originalClassName; }
    @Override public String toString() { return originalClassName + " " + WITHHELD_NOTE; }
}
```
`getMessage()` returns the fixed note (non-null), per RESEARCH. No Lombok needed beyond optional `@Getter` on `originalClassName`. Add a fork-anchors row for the class declaration line (count 1).

---

### `Column.java` `convertEnumValues` (model, transform) - D-14

**Analog:** itself. Current text (L524-530):
```java
    public Integer convertEnumValues(String value) {
        if ( this.enumValues != null ) {
            return enumValues.get(value);
        }
        throw new RuntimeException(String.format("No content in enum %s", value));
    }
```
Edit only the throw: drop `String.format(..., value)` and use the fixed text `No content in enum`. Keep the exact `RuntimeException` type (D-03 relies on it). Existing `Column.java` anchors (`Dynamic` 1, `.isNullable(isNull)` 3, `@Setter(AccessLevel.PACKAGE) private boolean isNullable;` 1) do not touch this line. File is spotless-baseline-red: no import changes. Test: hand-built `Column.builder().name("c").type(Type.Enum8).build()` with an 8-char sentinel such as `Zq7x41kV` through `doWriteColValue`; assert sentinel absent and fixed text present.

---

### `ClickHouseWriterNullMessageUnitTest.java` (HARN-08, test)

**Analog:** itself, replacing the Mockito lines. Current Mockito seam to remove (L116-124, plus imports L6-10):
```java
    private static ClickHouseHelperClient installHelperClient(ClickHouseWriter writer, boolean useClientV2)
            throws Exception {
        ClickHouseHelperClient chc = mock(ClickHouseHelperClient.class);
        when(chc.isUseClientV2()).thenReturn(useClientV2);
        Field field = ClickHouseWriter.class.getDeclaredField(WRITER_HELPER_CLIENT_FIELD);
        field.setAccessible(true);
        field.set(writer, chc);
        return chc;
    }
```
Mapping of assertions (keep every `@DisplayName`, test name, `assertSame`, `hookCalls`):
- `verify(chc, never()).pinReplica()` -> `assertEquals(0, chc.pinCalls)` (L139, L154, L185)
- `verify(chc, times(1)).pinReplica()` / `.unpinReplica()` -> `assertEquals(1, chc.pinCalls)` / `assertEquals(1, chc.unpinCalls)` (L169-170)

**Fake pattern:** a small `ClickHouseHelperClient` subclass calling `super(new ClickHouseHelperClient.ClickHouseClientBuilder("127.0.0.1", <port>, null, null, -1))`-style dummy builder (research: constructor opens no connection and owns no threads), overriding `isUseClientV2()`, `pinReplica()`, `unpinReplica()` (public, non-final; `isUseClientV2` is Lombok-generated). Close it after each test (try-with-resources or `@AfterEach`). Inject by the same reflection (`chc` field). Builder construction example already in the tree: grep `new ClickHouseHelperClient.ClickHouseClientBuilder(` in `ClickHouseWriter.java` (anchor count 2: do not add occurrences there; test files are not counted).

**No-Mockito precedent** (`CapturingClickHouseWriter` class Javadoc, L20-29): "It deliberately uses no Mockito: inline-mocking `ClickHouseHelperClient` crashed the Semeru/OpenJ9 test JVM natively inside `retransformClasses`". Reuse this comment style on the new fake.

**Test must stay Docker-free:** extends nothing (L30-36 Javadoc: `ClickHouseBase` carries `@Tag("clickhouse")`).

---

### `<ScriptedWriteFailureWriter>` and new WRT-13 unit tests (test fixtures)

**Analog:** `ClickHouseWriterNullMessageUnitTest.ScriptedInsertWriter` (L51-98) for hook overrides; `CapturingClickHouseWriter` for a no-Mockito writer subclass reaching private state through reflection.

**Hook-driven scripted writer** (copy; change `failFromHook` to call the REAL `doWriteColValue`, per RESEARCH hook-level D-04 example):
```java
        @Override
        protected void doInsertRawBinaryV1(
                List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults)
                throws IOException {
            failFromHook();   // here: doWriteColValue(UUID_COLUMN, new ByteArrayOutputStream(), new Data(Schema.STRING_SCHEMA, VALUE), false);
        }
        @Override public boolean updateMapping(String database) { return true; }
        @Override protected Table getTable(String database, String topic) { return stubTable; }
```
Parameterise over V1/V2 with the existing `@ParameterizedTest(name = "...clientV2={0}") @ValueSource(booleans = {false, true})` pattern (L126-129). The D-04 pin asserts `hookCalls == 1` and `pinCalls == 0` (the "before" on the unchanged writer is `hookCalls == 2`, `pinCalls == 1`).

**V2 shape:** wrap the writer exception in `new com.clickhouse.client.api.ClickHouseException("Insert failed ...", cause)` to model the V2 client wrap (RESEARCH D-13 corollary). `Utils.isServerError` does not match it.

---

### `WriterLogMaskingUnitTest.java` (flip + new sentinel cases, test)

**Analog:** itself.

**Reusable helpers** (copy by reference; do not re-implement): `SENTINEL = "SENTINELq7Zx41kV"` (L45), `capture(Executable)` (L81-91), `newWriter()` (L104-110, sets `csc` by reflection), `writeColValue(Column, String)` (L227-230), `column(name, type)` (L232-234), `assertNoSentinel(Captured)` (L125-129), `assertRawJdkExceptionEmbedsSentinel()` (L131-134).

**Flip target** (L251-264). Keep the test, change the assertion. Current body to flip:
```java
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> writer.doWriteColValue(uuid, new ByteArrayOutputStream(), stringData(SENTINEL), false));
        assertTrue(ex.getMessage().contains(SENTINEL));
```
New form: `assertThrows(DataException.class, ...)`; walk message, `toString()`, and `getSuppressed()` of every node down the cause chain with an identity-guarded loop and `assertFalse(contains(SENTINEL))`; assert presence of column name `UUID_COLUMN`, `Type.UUID` (built from the enum value via `String.format`, never hand-typed; F8: `Type.Decimal` prints `Decimal`), and `IllegalArgumentException.class.getName()`. Keep a written rationale comment replacing "A later fix (backlog 999.3) must flip this". Keep the raw-JDK self-check (`UUID.fromString(SENTINEL)` message embeds the sentinel, L247-248).

**Existing test that interacts:** `dateTimeFailureThroughWriteColValueLogsNoValueAndNoThrowable` (L266-278) expects `WRITE_FAILURE_LINE_FORMAT` with `DataException` class name for the nested path. It must stay green: DataException passes through (D-01). Do not weaken.

**Numeric sentinel caution (F4):** `new BigInteger(...)` truncates the value in its `NumberFormatException`. For the unparseable-number case use an INT128/UINT128 column and a sentinel of at most 9 characters (e.g. `Zq7x41kV`), with its own self-check. Do not reuse the 16-char `SENTINEL` there.

---

### `WriterFailureClassificationUnitTest.java` (WRT-14, test) - NEW

**Analogs:** `UtilsNullMessageClassificationUnitTest` for outcome style; `WriterErrorPathLiveTest` for swapping `Processing.dbWriter` by reflection.

**Outcome-assertion style** (UtilsNullMessageClassificationUnitTest L27-54):
```java
    private static void handle(Exception e, boolean errorsTolerance) {
        Utils.handleException(e, errorsTolerance, NO_SOCKET_RETRY, new ArrayList<>());
    }
    ...
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handle(failure, TOLERANCE_NONE));
        assertFalse(thrown instanceof RetriableException, "a message-less IOException is not a timeout");
        assertDoesNotThrow(() -> handle(new IOException(), TOLERANCE_ALL));
```
Here, instead of calling `handle` with hand-built exceptions, drive the real path (D-11): real `ProxySinkTask` (ctor opens no connection; never call `start()`), real `Processing`, real `ChunkFlusher.putDirect`, real `Utils.handleException`. Wrapping shapes: A = `ProxySinkTask.put` catch (L96-L101) receiving `RuntimeException(queryId.toString(), e)` from `Processing.doInsert`; B = `ChunkFlusher.putDirect` catch (L72) receiving what `ProxySinkTask.put` threw. Real wrap code to rely on (Processing L70-72):
```java
        } catch (Exception e) {
            statistics.recordTopicStatsOnFailure(records.size(), queryId.getTopic(), eventReceiveLag);
            throw new RuntimeException(queryId.toString(), e);//This way the queryId will propagate
        }
```
**Reflection swap** (WriterErrorPathLiveTest L137-147, L209-213):
```java
    private static Object readField(Object target, String fieldName) throws Exception { ... getDeclaredField ... setAccessible(true) ... }
    private static void writeField(Object target, String fieldName, Object value) throws Exception { ... }
    ...
    Object processing = readField(proxySinkTask, "processing");
    writeField(processing, "dbWriter", writer);
```
Assert outcome only: `RetriableException`, `DataException`, `RuntimeException` wrap, or returns normally; never messages or root-cause classes. Use the outcome matrix in RESEARCH "WRT-14" (tolerance x failure kind; `retryOnSocketException` where it matters). Place the class in package `com.clickhouse.kafka.connect.sink` (package-private `ChunkFlusher`), extend nothing. Failure kinds: real for UUID, number, Decimal out of range, FixedString too long, unknown Enum label (NPE), stream IO via a throwing `OutputStream`; scripted for `ArithmeticException` (override `doWritePrimitive`) and nested exact `RuntimeException` L-12 (override protected `doWriteCol`, F5).

---

### `WriterErrorPathLiveTest.java` (live put() cases, test)

**Analog:** itself. Add D-12 live cases reusing:
- `putBatch(...)` (L182-231) and `putWithInjectedFailure` (L171-174) with `FailingInsertWriter` (L66-99; here prefer a real conversion failure, i.e. `failure == null` and a batch with an invalid UUID/number string).
- `causeChain(Throwable)` identity-guarded walk (L149-155).
- `InMemoryDLQ` + `task.setErrorReporter(reporter)` (L201-203).
- Render DLQ headers as Connect 4.1.1 does (RESEARCH "Rendering what Connect renders"): `error.printStackTrace(new PrintStream(bos, true, StandardCharsets.UTF_8))` for the stacktrace header, `getMessage()` for the message header (equals queryId text, F3/D-15). Pass `UTF_8` explicitly (`file.encoding` is `windows-1252` for tests). `connect-runtime` is not on the test classpath.
- Tripwire: assert no `ServerException` in the thrown chain (D-13).
- `LogCapture.attach(ClickHouseWriter.class)` for the two `Error inserting records` logs.

Extends `ClickHouseBase` (live, Docker). Keep `SENTINEL` (L58) usage and the style of the WRT-10 assertion (L309-310): `chain.forEach(t -> assertFalse(String.valueOf(t.getMessage()).contains(SENTINEL), ...))` and extend it to `toString()` and suppressed entries.

---

### `InMemoryDLQ.java` (test fixture)

**Analog:** itself. Current (L10-33) stores `DLQRecord(record, t)` privately with only `size()`. Add an additive accessor returning the reported throwables (`List<Throwable> exceptions()`); do not alter existing members. File is spotless-clean: empty diff required.

---

### `ClickHouseSinkTaskWithSchemaTest.java` `schemaWithFixedStringMismatchTest` (F2, test)

**Analog:** itself (L887-912). Current assertion to flip (equal strength, per RESEARCH F2):
```java
        try {
            chst.put(sr);
        } catch (RuntimeException e) {
            assertInstanceOf(IllegalArgumentException.class, Utils.getRootCause(e), "Could not detect size mismatch for FixedString");
        }
```
New: root cause is `WithheldMessageException` and `getOriginalClassName()` equals `IllegalArgumentException.class.getName()`. File is spotless-baseline-red (55 lines): add only the one new import in its sorted position, do not reorder others (D-16). Record the flip in the ledger.

---

### `22-BEHAVIOUR-LEDGER.md` (doc) - NEW

**Analog:** `.planning/milestones/v1.7.0_gg2-hardening-phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` (header block at L1-12, table header at L14-16) and the `19-` ledger (last ID is L-41; Phase 22 rows start at L-42 per RESEARCH). Copy the structure:
```markdown
# Phase 22 Behaviour Ledger
**Purpose:** ...
**Source plans:** 22-0x (L-42..)
**How to read it:** IDs continue after the Phase 19 ledger's L-41. ... The ledger holds test names, decision IDs and message formats only, never record data.
| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 24 destination |
```
Rows (D-ledger): root-cause type change to `DataException`; withheld-note cause line; D-04 Code-33/131 loss on V1/RowBinary only (F9); grep target `Failed to write column `; D-14 enum text; F2 test flip; F3/D-15 DLQ message-header fact; `Decimal` token casing. Rename the last column to "Phase 24 destination". One row per change, same commit as the change. Do not write `VERSION` or `CHANGELOG.md`.

---

### `docs/FORK-DELTA.md` and `.planning/scripts/fork-anchors.txt` (docs / config)

**FORK-DELTA analog:** the WRT-12 generic-catch row at L41 (table columns: change | file | rationale | pinned by). Edit sites in the WRT-13 commit: row L41 (drop "rethrows the original exception unchanged ... its message can still carry the value"), Known costs item 6 at L158 (remove the "The rethrown exception of `doWriteColValue`'s generic catch keeps its message." sentence, state T-18-07 closed), add a new "Connector source" table row (`Pinned by` WRT-13, WRT-14, WRT-15 plus test classes), and a Column.java row for D-14.

**fork-anchors.txt analog:** rows L58-69, TAB-separated `path<TAB>fixed string<TAB>count<TAB>id`, count = matching lines:
```
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java	WRITE_VALUE_FAILURE_LOG_FORMAT	2	WRT-12
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java	MISSING_NON_NULLABLE_MESSAGE_FORMAT	2	WRT-10
```
Add rows for the new message-format constant (definition + use = 2) and the `WithheldMessageException` class declaration line (1), id `WRT-13`; measure counts on the final text. A row and its code land in the same commit. Quick mirror: the `grep -cF` loop from RESEARCH Pitfall 4 (`rows=59 bad=0` at HEAD). Real gate: `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (run in background).

**ROADMAP:** edit only the `### Phase 999.3` section (L287-292) by line-addressed Edit (Pitfall 5); add `**Delivered:** Phase 22 (WRT-13..WRT-15), lands in the next release (Phase 24 publishes it)`.

## Shared Patterns

### Content rule: metadata only, never the value
**Source:** CCE branch in `ClickHouseWriter.java` L858-869 and `NESTED_NULL_MESSAGE_FORMAT` (L564).
**Apply to:** the new message, the sanitized cause, the D-14 text, every new log line.
Build text from `col.getName()`, `columnType` (the `Type` enum), `value.getFieldType()`, value class name, exception class name. Never from `getMessage()`, `toString()` or suppressed of the original.

### Sentinel test, both directions (Phase 18 D-12)
**Source:** `WriterLogMaskingUnitTest` L45, L125-134, L237-249.
**Apply to:** every new WRT-13 test (unit and live).
Assert sentinel absent from all messages, `toString()`, suppressed entries and `LogCapture.allTexts()`, AND assert column name, `Type` token and original class name present, AND keep a raw-JDK self-check that the unmodified JDK message embeds the sentinel.

### LogCapture around the writer logger
**Source:** `src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java` (`attach(Class)`, `events()`, `allTexts()`, auto-detach on close).
```java
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            ... action ...
            return new Captured(thrown, log.events(), log.allTexts());
        }
```

### No-Mockito test seams
**Source:** `CapturingClickHouseWriter.java` (hand-written writer subclass, reflection `setField("csc", csc)` L66-70) and `ScriptedInsertWriter`.
**Apply to:** the helper-client fake, the scripted writer, the classification test. No new Mockito imports anywhere.

### Docker-free tests stay in `testUnit`
Extend nothing (`ClickHouseBase` has `@Tag("clickhouse")`). Live tests extend `ClickHouseBase`. Gradle always as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`.

### Spotless (D-16)
Spotless is `importOrder()` + `removeUnusedImports()` only. Baseline-red touched files: `ClickHouseWriter.java` (19), `ClickHouseSinkTaskWithSchemaTest.java` (55), `Column.java`. For these: no reordering, new imports in sorted position, same +/- counts as baseline (throwaway `git archive` copy). All other touched or new files: empty diff.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `WithheldMessageException.java` | utility (sanitized Throwable copy) | transform | No existing class copies a throwable's frames while withholding its message; use RESEARCH Pattern 2. Only the repo-level style conventions apply |

## Metadata

**Analog search scope:** `src/main/java/com/clickhouse/kafka/connect/{sink,sink/db,sink/db/mapping,sink/processing,util}`, `src/test/java/com/clickhouse/kafka/connect/sink/{,db,dlq,util}`, `.planning/milestones/v1.7.0_gg2-hardening-phases/{18,19}-*`, `docs/FORK-DELTA.md`, `.planning/scripts/`
**Files read:** CONTEXT, RESEARCH, `ClickHouseWriter.java` (targeted ranges), `ClickHouseWriterNullMessageUnitTest`, `WriterLogMaskingUnitTest`, `LogCapture`, `InMemoryDLQ`, `UtilsNullMessageClassificationUnitTest`, `WriterErrorPathLiveTest`, `CapturingClickHouseWriter`, plus ranges of `ProxySinkTask`, `ChunkFlusher`, `Processing`, `Column`, `FORK-DELTA`, `ROADMAP`, `fork-anchors.txt`, the 18 ledger
**Tracked-source gate:** all analogs verified with `git ls-files`
**Pattern extraction date:** 2026-10-19
