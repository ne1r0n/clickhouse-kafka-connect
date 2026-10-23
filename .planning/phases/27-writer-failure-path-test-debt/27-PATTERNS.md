# Phase 27: Writer Failure Path & Test Debt - Pattern Map

**Mapped:** 2026-10-22
**Files analyzed:** 14 new/modified
**Analogs found:** 14 / 14 (all tracked; verified with `git ls-files`)

Note: `WriterFailureClassificationUnitTest` lives in `src/test/java/com/clickhouse/kafka/connect/sink/` (not `sink/db/`). Line numbers below are from this session; re-grep before editing.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` | service (writer) | request-response / transform | itself (`passesThroughUnchanged`, generic catch, `buildValueConversionFailureMessage`) | exact (in-place) |
| `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` | model | transform | itself (`convertEnumValues`) | exact (in-place) |
| comment-only scrub: `WithheldMessageException`, `ParentShape`, `DeclaredNullability`, `DescribeTableBuilder`, `ClickHouseHelperClient`, `SinkTaskStatistics`, `HeaderToValue` | utility/model | n/a | n/a | in-place |
| `src/test/java/.../sink/WriterFailureClassificationUnitTest.java` (WRT-16 rows) | test | request-response | its own enum-kind table (`IO_PLAIN`, `SOCKET_TIMEOUT`) | exact |
| `src/test/java/.../sink/db/WriterValueFreeFailureUnitTest.java` (Jackson, enum, ClassCastException rows) | test | request-response | `streamIoExceptionPassesThrough`, `unsupportedJsonFieldTypePassesThrough` | exact |
| new `Column` enum unit row (location: near `ClickHouseWriterColumnContextTest`) | test | transform | `ClickHouseWriterColumnContextTest` | role-match |
| `src/test/java/.../sink/JsonAsStringKeyResidualLiveTest.java` (flip + scrub) | test (live) | request-response | itself | exact |
| `src/test/java/.../sink/ClickHouseSinkTaskWithSchemaTest.java` (`schemaWithFixedStringMismatchTest`) | test | request-response | itself | exact |
| `src/test/java/.../sink/db/CountingHelperClient.java` (remove counter) | test fixture | n/a | itself | exact |
| `src/test/java/.../sink/NullableTupleMatrixLiveTest.java` (premise loops) | test (live) | CRUD | itself (`:568`, `:743`, `:885` non-vacuous loops) | exact |
| new planning-ID guard in `src/test/java/com/clickhouse/kafka/connect/ci/` | test (source-tree guard) | file-I/O | `ci/TestsWorkflowPinGuardUnitTest.java` | exact |
| `.planning/scripts/fork-anchors.txt` | config | n/a | existing rows 75-83 (Phase 22 block) | exact |
| `docs/FORK-DELTA.md`, `CHANGELOG.md` | docs | n/a | existing rows (42, 53, 82, line 217, Known costs item 6) | exact |

## Pattern Assignments

### `ClickHouseWriter.java` (WRT-17, WRT-19)

**Analog:** itself. Current code (lines ~636-660, ~890-908):

```java
private static boolean passesThroughUnchanged(Exception e) {
    if (e instanceof org.apache.kafka.connect.errors.ConnectException || e.getClass() == RuntimeException.class) {
        return true;
    }
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
        if (t instanceof IOException) {
            return true;
        }
    }
    return false;
}

private static String buildValueConversionFailureMessage(Column col, Type columnType, Data value, Exception cause) {
    String sourceType = value == null ? "null" : String.valueOf(value.getFieldType());
    String valueClass = (value == null || value.getObject() == null) ? "null" : value.getObject().getClass().getName();
    return String.format(WRITE_VALUE_CONVERSION_FAILURE_FORMAT, col.getName(), sourceType, valueClass, columnType,
            cause.getClass().getName());
}
// ClassCastException branch duplicates sourceType/valueClass, then:
String message = String.format(
        "Failed to write column `%s`: cannot convert Kafka value of type %s (value class: %s) to ClickHouse type %s",
        col.getName(), sourceType, valueClass, columnType);
// generic catch:
if (passesThroughUnchanged(e)) { throw e; }
throw new DataException(buildValueConversionFailureMessage(col, columnType, value, e), WithheldMessageException.of(e));
```

**WRT-17 edit:** change the loop condition to `t instanceof IOException && !(t instanceof JacksonException)`; import `ConnectException` and `com.fasterxml.jackson.core.JacksonException` (D-04). Reword the Javadoc ("an IOException that is not a Jackson exception").

**WRT-19 edit:** add `sourceTypeName(Data)` / `valueClassName(Data)` private statics; move the ClassCastException format into a constant built as `WRITE_FAILURE_COLUMN_PREFIX + "%s`: cannot convert ..."`, copying the concatenation style of `WRITE_VALUE_CONVERSION_FAILURE_FORMAT` (~line 571). Pin the exact text green first: ``Failed to write column `effective_at`: cannot convert Kafka value of type STRING (value class: java.lang.String) to ClickHouse type UINT64``.

### `Column.java` (WRT-18)

**Analog:** itself, `convertEnumValues` (~line 526):

```java
public Integer convertEnumValues(String value) {
    if ( this.enumValues != null ) {
        return enumValues.get(value);
    }
    // The label is record data and this exact RuntimeException reaches Connect status unchanged, so the text is fixed (D-14).
    throw new RuntimeException("No content in enum");
}
```

Target: on null `get`, throw `IllegalArgumentException` (message names `name`, never the label); the no-content branch stays exact-class `RuntimeException` with `No content in enum column <name>`. Use String constants (AGENTS.md). Existing anchor `fork-anchors.txt:82` (`throw new RuntimeException("No content in enum");`, 1) must be replaced in the same commit; keep negative row `No content in enum %s` (0). Avoid `Map.of` fixtures when probing null labels (production map is a `HashMap`).

### `WriterFailureClassificationUnitTest.java` (WRT-16)

**Analog:** enum kinds, lines ~108-126:

```java
IO_PLAIN(RetryClass.NON_RETRIABLE, () -> uuidStream(new IOException("stream closed by test"))),
IO_READ_TIMEOUT_PREFIX(RetryClass.RETRIABLE, () -> uuidStream(new IOException("Read timed out after 30000 ms"))),
SOCKET_TIMEOUT(RetryClass.RETRIABLE, () -> uuidStream(new SocketTimeoutException("timed out"))),
```

New kinds (insert mid-list, e.g. after `SOCKET_TIMEOUT`, so the diff is `+` lines only; avoid touching the last constant's `;`):

```java
JACKSON_ALONE(RetryClass.NON_RETRIABLE, () -> uuidStream(new JsonMappingException("scripted jackson failure"))),
JACKSON_OVER_SOCKET_TIMEOUT(RetryClass.RETRIABLE,
        () -> uuidStream(new JsonMappingException("scripted jackson failure", new SocketTimeoutException("timed out")))),
JACKSON_OVER_TIMEOUT_TEXT(RetryClass.RETRIABLE,
        () -> uuidStream(new JsonMappingException("scripted jackson failure", new IOException("Read timed out after 30000 ms")))),
```
Plain IOException control already exists (`IO_PLAIN`); add a named control or a plain IOException over a Jackson cause. `JacksonException` ctors are protected; use `JsonMappingException`. Expected count 184 + 8 per kind.

### `WriterValueFreeFailureUnitTest.java` (new rows)

**Analog:** lines ~405-450:

```java
@Test
@DisplayName("... pass-through: an IOException from the stream is rethrown as the same instance")
void streamIoExceptionPassesThrough() throws Exception {
    IOException failure = new IOException("stream closed by test");
    ClickHouseWriter writer = newWriter(config());
    Column uuid = column(UUID_COLUMN, Type.UUID);
    Captured captured = capture(() -> writer.doWriteColValue(
            uuid, new ScriptedWriteFailureWriter.FailingOutputStream(failure, 0), stringData(VALID_UUID), false));
    assertSame(failure, captured.thrown);
}

Map<String, String> props = minimalProps();
props.put(ClickHouseSinkConfig.CLICKHOUSE_SETTINGS, JSON_AS_STRING_SETTING);
Captured captured = writeColValue(new ClickHouseSinkConfig(props), column(JSON_COLUMN, Type.JSON), new Data(Schema.INT32_SCHEMA, 1));
assertEquals(RuntimeException.class, captured.thrown.getClass());
```
Copy for: Jackson-key Struct row (sentinel key -> `new Object()`; red on unchanged writer), Jackson-over-timeout same-instance (`assertSame`), unknown-enum row (NPE then IAE), updated `NO_ENUM_CONTENT_MESSAGE`. Helpers: `LogCapture.attach(ClickHouseWriter.class)`, `ThrowableTexts.printedStackTrace`.

### `JsonAsStringKeyResidualLiveTest.java` (flip + D-11 scrub)

**Analog:** itself, `assertJacksonRouteReached` (~line 187):

```java
ThrowableTexts.causeChain(thrown).stream()
        .anyMatch(t -> JACKSON_FAILURE_CLASS.equals(t.getClass().getName()))
```
Must also accept `t instanceof WithheldMessageException w && JACKSON_FAILURE_CLASS.equals(w.getOriginalClassName())` (still requires class identity). Flip key assertions to absent (task status, DLQ stack-trace header, `Error inserting records` throwable); keep the no-row check (`countRows == 0`). Rename methods that describe the old leak. Scrub Javadoc IDs and `@DisplayName` prefixes. Land together with the writer fix to avoid an intermediate red commit.

### `ClickHouseSinkTaskWithSchemaTest.java` / `CountingHelperClient.java` / `NullableTupleMatrixLiveTest.java` (DEBT-16)

- `schemaWithFixedStringMismatchTest` (~906-917): `RuntimeException e = assertThrows(RuntimeException.class, () -> chst.put(sr));` keep the two `Utils.getRootCause(e)` assertions; move `chst.stop()` into `finally`. Red proof: widen FixedString so `put` does not throw.
- `CountingHelperClient`: remove `useClientV2Calls` field (line 21), increment (32), getter (46-47), and the class Javadoc mention; edit `docs/FORK-DELTA.md` row ~82 in the same commit.
- `NullableTupleMatrixLiveTest` sites ~810-814 (premise loop), ~820-823 (`columns.get(...)`), ~896 (`getAllColumnsMap().get(...)`): collect the matching row into a variable and `assertNotNull(cellRow, PREMISE_MESSAGE + ": DESCRIBE returned no row named " + CELL_COLUMN)` before comparing; wrap lookups in `assertNotNull(..., key)`. Loops at ~568, 743, 885 need no change. Red proof by mutated premise key.

### New planning-ID guard (D-14)

**Analog:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java`. Conventions: class extends nothing and carries no tag (stays in `testUnit`), relative `Path.of`, UTF-8 read explicitly, static `Pattern` constants, assertion messages naming file and line, non-vacuity floor, `assertAll`.

```java
private static final Path WORKFLOW = Path.of(".github", "workflows", "tests.yaml");
// read as UTF-8 explicitly: the test JVM runs with file.encoding=windows-1252
```
Patterns (see RESEARCH Pattern 4): prefixed IDs plus `Phase \d+`, `Pitfall \d+`, `RESEARCH`, `.planning/`; include a pattern self-test (positives `(D-01)`, `Phase 28's`; negatives `UTF-8`, `SHA-256`).

### `.planning/scripts/fork-anchors.txt`

Row format (tab-separated: path, fixed string, expected line count, tag), e.g.
```
src/main/java/.../Column.java	throw new RuntimeException("No content in enum");	1	WRT-13
src/main/java/.../Column.java	No content in enum %s	0	WRT-13
```
Counts are `grep -cF` line counts. Do not reflow `WithheldMessageException` Javadoc lines 23-24. Baseline `anchor_rows=79`; restate expected total per commit.

## Shared Patterns

### Value-free failure text
**Source:** `ClickHouseWriter.WRITE_VALUE_CONVERSION_FAILURE_FORMAT`, `WithheldMessageException.of(e)`. **Apply to:** every new message (writer and Column): column metadata, class names, Kafka types only; never a value, key, element or length.

### Red-first, anchors-per-commit
Changed pins are seen red on the unchanged tree first; anchor rows and FORK-DELTA rows move in the same commit as the code; comment-only scrub commits are separate and run the anchors gate and a compile.

### Test harness
Use `CountingHelperClient` (no Mockito inline mock of `ClickHouseHelperClient`), `LogCapture`, `ThrowableTexts`, `ScriptedWriteFailureWriter.FailingOutputStream`. All Gradle calls as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`.

## No Analog Found

None. Closest to new: the Docker-free Jackson-key row, which is a composition of `unsupportedJsonFieldTypePassesThrough` (config) and the `JsonAsStringKeyResidualLiveTest` Struct+Map builder (value).

## Metadata

**Analog search scope:** `src/main/.../sink/db`, `src/test/.../sink`, `.../ci`, `.planning/scripts`
**Pattern extraction date:** 2026-10-22
