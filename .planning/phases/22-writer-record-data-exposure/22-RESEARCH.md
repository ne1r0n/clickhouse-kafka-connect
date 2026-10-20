# Phase 22: Writer Record-Data Exposure - Research

**Researched:** 2026-10-19
**Domain:** Java exception hygiene in a Kafka Connect sink (ClickHouse RowBinary writer): value-free exceptions, retry-classification invariance, no-Mockito test seams
**Confidence:** HIGH (every behavioural claim below was run against the real writer, real client V1/V2 and a real ClickHouse `latest` 26.9.19.9 this session)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Carried forward (locked earlier; not re-opened)
- **Content rule (Phase 18):** every exception or log text this phase adds is built from `Column` metadata and exception class names only. It never contains a record value, including through a cause message, `toString()` or a suppressed exception. Formats live in static constants beside `WRITE_VALUE_FAILURE_LOG_FORMAT` / `NESTED_NULL_MESSAGE_FORMAT`.
- **The catch's own log line stays.** `Error writing value of {} to the column `{}` of type {}: exception {}` is unchanged: no throwable, no stack. The prefix is kept for greps, and the `fork-anchors.txt` row `WRITE_VALUE_FAILURE_LOG_FORMAT 2 WRT-12` must keep passing. Stack frames reach operators through the `Error inserting records` log instead (D-05).
- **Order inside the phase (roadmap):** each step lands as its own commit(s), with `testUnit` green at every commit.
  1. HARN-08.
  2. The WRT-14 pin, committed and green on the **unchanged** writer.
  3. WRT-13, with the WRT-15 doc edits in the **same commit**.
- **WRT-15 same-commit set:**
  - FORK-DELTA Known costs item 6 (remove the residual sentence; T-18-07 is closed);
  - the WRT-12 row of the patch table (the "rethrows the original exception unchanged … its message can still carry the value" text);
  - a new FORK-DELTA row and a new `fork-anchors.txt` row for the new fork patch in the upstream region;
  - the ROADMAP backlog 999.3 entry marked delivered.

  The anchors invariant `verify-merge-invariants.sh 54e63648 HEAD` passes at every commit (Phase 21 D-01: the gate stays as invoked, with the SHA-pinned exemption list).
- **Flip, don't delete:** `WriterLogMaskingUnitTest.uuidWriteFailureRethrowsOriginalWithValueInMessage` pins the residual on purpose; its own comment says "backlog 999.3 must flip this". In the WRT-13 commit, flip it to assert the new behaviour and keep a written rationale in the test. `assertRawJdkExceptionEmbedsSentinel` stays: it proves the JDK message really embeds the sentinel, so the negative assertions are meaningful.
- **Behaviour ledger (Phase 18 D-13 pattern):** create `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md` in the format of `18-BEHAVIOUR-LEDGER.md`. Add one row per operator-visible change in the same commit as the change, for Phase 24's ROLL-03. Expected rows:
  - the root-cause type behind `put()` changes from `IllegalArgumentException`/`NumberFormatException`/… to `DataException` with the new text;
  - the stack trace shows the withheld-message cause line (D-06);
  - a record value containing a Code 33/131 text no longer triggers a refresh (D-04);
  - what to grep for instead (`Failed to write column `).

  `VERSION` and `CHANGELOG.md` stay untouched; Phase 24 writes them.
- **Process rules:**
  - Existing tests are never deleted or weakened.
  - Gradle always runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`.
  - `spotlessApply` runs on touched files only. Every touched file must be spotless-clean on its own (Phase 21 D-14: no tree-wide reformat, baseline-equal gate).
  - Phase gate: cache-busted `testUnit` (`--rerun`), a full suite on `latest` × V2, and the anchors invariant.

#### What gets replaced (WRT-13)
- **D-01:** The generic catch **rethrows the original unchanged** in either of these cases:
  - **any** throwable in its cause chain is an `IOException`, so retry classification still sees `SocketTimeoutException`, `SocketException`, `UnknownHostException` and the read/write-timeout `IOException` messages as before. This includes the `new RuntimeException(ioException)` wrappers that the Map/Tuple lambdas build at ~L713, ~L742 and ~L786.
  - it is a `ConnectException` (including `DataException`). These are the fork's value-free texts: WRT-08 nested null, WRT-10 missing non-nullable, unsupported RowBinary type, and an inner column's own D-02 replacement.

  Everything else is **replaced** (D-02/D-05). — **Reversibility:** costly — the thrown type and text are operator-visible once Phase 24 publishes, and operators may build greps or alerts on them.
- **D-02:** The replacement is an `org.apache.kafka.connect.errors.DataException`. This matches the `ClassCastException` branch above it. Because a `DataException` passes through outer catches (D-01), a failure inside a Tuple/Map/Array child keeps the **innermost** column's name. The WRT-14 pin (D-11) must prove the outcome is unchanged.
- **D-03:** An exception whose class is **exactly** `RuntimeException` (`e.getClass() == RuntimeException.class`) is also rethrown unchanged. Those are the connector's own throw sites, with safe and useful text:
  - the upstream JSON hint `Writing JSON in binary is not supported yet. Use input_format_binary_read_json_as_string=1 …` (~L849);
  - `Unsupported field type … for column type [JSON]` (~L845);
  - the nested L-12 `An attempt to write null into not nullable column '%s'` (~L1073/L1088, reached through Tuple/Map children).

  No throw sites are edited. JDK and client conversion failures are typed subclasses (`IllegalArgumentException`, `NumberFormatException`, `ArithmeticException`, `DateTimeException`, `NullPointerException`, …) and are replaced.
  - **Research must verify** that no client-library (`clickhouse-java` `BinaryStreamUtils`, `ClickHouseValues`, …) or JDK code reachable from `doWriteColValue` throws an exact `RuntimeException` whose message carries a value. **If it finds one, stop and ask the user** before choosing another rule.
- **D-04:** Today, a record value that itself contains `ClickHouseException: Code: 33` (or `131`) makes the rethrown message match `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE`, which triggers one table-mapping refresh and retry. After the change it can't. **This loss is accepted and intended**, because it is record content steering control flow, which is the leak itself.
  - Read the roadmap's "must neither start nor stop matching" rule as follows. Metadata-built messages never start matching. Real server and client failures (which pass through per D-01/D-03, or never reach this catch) never stop matching.
  - Record the loss in the ledger and FORK-DELTA, and pin it with one Docker-free test.

#### Cause chain & frames
- **D-05:** The replacement `DataException` carries a **sanitized copy** of the original as its cause. That is a small fork class (a `RuntimeException` subclass) that:
  - keeps no original message;
  - copies the original's stack frames (`setStackTrace(original.getStackTrace())`);
  - sanitizes the original's own causes recursively in the same way, with a cycle/depth guard;
  - drops suppressed exceptions.

  Connect status, DLQ stack traces and the `Error inserting records` log then still show the frames (e.g. `at java.util.UUID.fromString …` down to the writer line) with no message text. Under classification, `getRootCause` lands on the sanitized object, which takes the same non-retriable path as the original `IllegalArgumentException`. D-11 proves that.
- **D-06:** The sanitized cause overrides `toString()` so that its stack-trace line reads `<original class name> (message withheld: may contain record data)`, e.g. `Caused by: java.lang.IllegalArgumentException (message withheld: may contain record data)`. The note is a fixed static constant. `getMessage()` returns no original text: it is null or the same fixed note. The research/planner picks which, consistent with how `Throwable.printStackTrace` and Connect's DLQ reporter render it.

#### Message wording
- **D-07:** The message follows the `ClassCastException` precedent and shares its `Failed to write column ` prefix, kept in a static constant. Target shape (wording may be polished within these fields):
  `Failed to write column `<name>`: Kafka value of type <Kafka field type> (value class: <value class name>) could not be written as ClickHouse type <Type enum>: <original exception class name>`
  Use `"null"` for a null `Data` or a null object, as the CCE branch does. — **Reversibility:** costly — operator-visible grep target once published.
- **D-08:** **No value length.** This message travels further than a log line (Connect status, and a DLQ topic other consumers read), so it carries metadata only. The Phase 18 DateTime logs keep their `value length` as they are.
- **D-09:** Render the ClickHouse type as the **`Type` enum** token (`UUID`, `INT32`, `DECIMAL`), the same token the CCE branch and the `Error writing value of` log print. Don't use `renderColumnType`.

#### Test evidence
- **D-10 (HARN-08):** `ClickHouseWriterNullMessageUnitTest` replaces `mock(ClickHouseHelperClient.class)` with a **hand-written test-only `ClickHouseHelperClient` subclass**:
  - it overrides `isUseClientV2()`, `pinReplica()` and `unpinReplica()`, counting calls;
  - it is injected into the writer's `chc` field by reflection, as today;
  - `ScriptedInsertWriter` keeps overriding `doInsertRawBinaryV1/V2`, `updateMapping` and `getTable`;
  - every `verify(chc, times(n)/never())` becomes an exact count assertion. Every `assertSame`, every `hookCalls` assertion, every `@DisplayName` and every test name is kept;
  - there is no production change and no Mockito in the class.
  - **Research must confirm** that the real constructor (`createClientV2()` / `createClientV1()`) opens no connection and starts no threads with a dummy builder. The fake is closed after each test.
  - This closes the Phase 18 deferred item: the OpenJ9 `retransformClasses` crash risk.
  - Lands first, so the new pins follow the same no-Mockito pattern.
- **D-11 (WRT-14):** A Docker-free classification pin in `testUnit` that passes on the unchanged writer and then, **unmodified**, after WRT-13.
  - **Drive through the real catch.** Provoke each failure kind inside `doWriteColValue`, take whatever the writer throws, wrap it exactly as each real caller does (at least `Processing.doInsert`'s `new RuntimeException(queryId.toString(), e)`), and pass it to `Utils.handleException`. Research enumerates every path from the writer to `handleException`, including the buffered delivery strategies, and pins each distinct wrapping shape. Feeding hand-built exceptions straight to `handleException` does not count.
  - **Assert the full outcome** for each failure kind × `errorsTolerance` ∈ {false, true} (× `retryOnSocketException` where it matters). The outcome is one of: `RetriableException` thrown, `DataException` rethrown, `RuntimeException` wrap thrown, or returns normally (the DLQ path).
  - **Never assert messages or root-cause classes**, or the pin can't stay unchanged across the fix.
  - **Failure kinds (minimum; research completes the list):**
    - invalid UUID string (`IllegalArgumentException`);
    - unparseable number (`NumberFormatException`);
    - out-of-range or `ArithmeticException` (Decimal);
    - a `NullPointerException` inside a conversion;
    - a stream `IOException` with an unrelated message, and one with the read/write-timeout prefix;
    - `SocketException` (with `retryOnSocketException` true and false);
    - `SocketTimeoutException`;
    - an `IOException` inside a Map value and inside a Tuple field (the lambda `RuntimeException` wrapper);
    - a nested `DataException` (WRT-08);
    - a nested exact `RuntimeException` (L-12).
- **D-12 (WRT-13 proof):** sentinel tests use a unique sentinel string as the field value. Each test asserts **both**:
  - the sentinel is absent from every message, `toString()` and suppressed entry down the cause chain, and from every captured log event (message plus rendered throwable chain);
  - the column name, the `Type` token and the original exception class name are **present**, so a negative-only test can't pass just because a line or a message disappeared (Phase 18 D-12).

  Cases: an invalid UUID string and an unparseable number. Two layers:
  - **Docker-free (`testUnit`):**
    - writer-level tests on `doWriteColValue` (flat columns, plus one nested child showing that the innermost column is named);
    - `doInsertRawBinary` driven through the insert hooks, parameterised over V1/V2, with `LogCapture` on the writer logger covering the `Error inserting records` log;
    - D-03 pass-through cases (an exact `RuntimeException` JSON hint text arrives unchanged);
    - the D-04 Code-33-in-value pin.
  - **Live on `latest` × V2** (`WriterErrorPathLiveTest` style), through `ClickHouseSinkTask.put()`:
    - with `errors.tolerance=none`, walk the thrown cause chain and its rendered stack trace (what the task status shows);
    - with `errors.tolerance=all`, render the `InMemoryDLQ`-captured exception the way Connect 4.1.1's DLQ reporter builds its `exception.message` and `exception.stacktrace` headers;
    - `LogCapture` covers both `Error inserting records` logs.

  There is no live V1 run, by the project constraint.
- **D-13 (the "both logs" in SC2):** research traces, for V1 and V2, which `doInsertRawBinary` branch an invalid UUID and an unparseable number actually reach.
  - If the `ServerException` branch is unreachable for a client-side conversion failure, which is expected because the writer fails before the server answers, SC2 is met by the generic-branch log plus a written trace in the phase evidence explaining why.
  - **If a conversion failure can surface as a `ServerException` on either client** (e.g. the server rejects a truncated body first), **stop and ask the user** before touching that branch (Phase 18 D-01 style).

### Claude's Discretion
- The name and package of the sanitized-cause class and of the helper-client fake (test-side: beside `CapturingClickHouseWriter` or nested in the test).
- Whether the replacement logic lives in a small private helper in `ClickHouseWriter` (recommended, to keep the catch body short) and how the cause-chain IO scan is written. The catch runs only on failure, not on the per-record hot path, so either a plain loop or a stream is acceptable; the scan needs a cycle guard.
- The exact wording polish of D-07 within its fields, and whether `getMessage()` of the sanitized cause is null or the fixed note (D-06).
- The plan split, within the fixed order HARN-08 → WRT-14 pin → WRT-13 + WRT-15.
- The evidence file layout. Precedent: the Phase 17/20/21 evidence files with the branch trace (D-13), the D-03 research result, the red-before/green-after runs and the gate output.

### Deferred Ideas (OUT OF SCOPE)
None. The discussion stayed within the phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| HARN-08 | `ClickHouseWriterNullMessageUnitTest` no longer inline-mocks `ClickHouseHelperClient`; every WRT-11 assertion is kept; closes the Phase 18 OpenJ9 deferred item | D-10 is feasible without any production change: the real `ClickHouseHelperClient` constructor opens no connection (measured, section "HARN-08 facts"). Lombok-generated `isUseClientV2()` and the public `pinReplica()`/`unpinReplica()` are overridable. One wording caveat: Finding F7 |
| WRT-13 | Generic catch of `doWriteColValue` reaches Connect status, DLQ headers and both `Error inserting records` logs value-free, naming column, type, exception class | Failure-kind survey, replacement algorithm, sanitized-cause class, D-13 branch trace (ServerException branch unreachable, measured on real V1/V2 against a real server), sentinel pitfalls (F4), existing-test casualty (F2), DLQ-header reality (F3), open D-03 trigger (F1) |
| WRT-14 | `Utils.handleException` classification unchanged for every failure kind reaching the catch, pinned Docker-free before and after | Complete list of wrapping shapes (A: `ProxySinkTask.put`, B: `ChunkFlusher.putDirect`, both reachable Docker-free), outcome matrix, failure-kind construction recipes (which kinds are real vs scripted), L-12 reachability (F5) |
| WRT-15 | FORK-DELTA item 6 + WRT-12 row, ROADMAP 999.3 "Delivered", in the same commit as the code | Exact edit sites, anchor-row mechanics (counts are line counts; a row and its code must land together), cheap local anchor mirror (rows=59 bad=0 at HEAD), spotless baseline caveat (F6) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` (-> `AGENTS.md`) and `./.claude/CLAUDE.md`. Treat as locked, same authority as CONTEXT.md.

- `ClickHouseWriter.java` is a **high-risk file**; serialization-hot-path changes need extensive tests. The catch fires on failure only, so no per-record cost may be added on the success path.
- **Existing tests are only added to, never deleted or weakened.**
- Feature tests: do **not** mock `ClickHouseWriter` in state-machine tests; when writing to ClickHouse read the data back and `assertEquals`; DRY; read `ClickHouseTestHelpers.java` before writing tests; descriptive test names.
- No secrets or **record data in logs** (the whole point of this phase).
- Error messages must be clear, actionable, include context.
- Use try-with-resources for every `AutoCloseable` (the fake helper client is `AutoCloseable`).
- Lombok allowed (`@Getter` etc.); SLF4J levels: TRACE per-record, DEBUG batch, INFO lifecycle. Static string constants extracted to top-level static strings. Code comments only for non-obvious invariants.
- Do **not** upgrade dependencies (none are needed here). Do **not** commit unless asked (inside GSD execution the plan's commits are the sanctioned path).
- `VERSION`/`CHANGELOG.md` are **not** touched (Phase 24).
- Behaviour preservation: tests pin current fork behaviour; code changes limited to genuine fixes; additive changes to keep upstream rebases cheap.
- Test target is ClickHouse `latest` + client V2 only. Docker is colima profile `vz` (`colima status -p vz`).
- GSD workflow enforcement: edits happen through a GSD command (the executor), not ad hoc.
- Stale in `.claude/CLAUDE.md`: it says Gradle 7.4.2. The real wrapper is Gradle 9.6.1 (confirmed by this session's build output). Do not treat that file as a version source.

## Summary

The change site is small (`ClickHouseWriter.doWriteColValue`, generic `catch (Exception e)`, L870-874) but its blast radius is wider than CONTEXT assumes. I ran the real writer, the real clients V1 and V2, and a real ClickHouse (`latest`, 26.9.19.9) this session. Confirmed: (1) on V1 the raw conversion exception propagates out of `doInsertRawBinary` unchanged; on V2 it propagates synchronously out of `Client.insert(...)` wrapped as `com.clickhouse.client.api.ClickHouseException("Insert failed (Attempt: 1/4 - Duration: ...)")` with the original as cause. On both clients it lands in the **generic** (second) catch of `doInsertRawBinary`; the `ServerException` branch is **unreachable** for client-side conversion failures (D-13 satisfied, no user stop needed). (2) The replacement design in D-01..D-06 keeps `Utils.handleException`'s outcome identical for every failure kind, because the root cause stays a non-IO, non-server `RuntimeException`, and every IO-bearing chain is passed through untouched.

Nine findings (F1-F9) need attention before planning (details in "Findings that change the plan"). The three that matter most: F1 is a **D-03 stop-and-ask trigger**: `Column.convertEnumValues` throws an exact `RuntimeException("No content in enum <VALUE>")` carrying the record value. It is unreachable from production-built columns (only `Column.extractColumn` builds Enum columns and it always sets `enumValues`) but reachable from a hand-built `Column`, so the "exact RuntimeException is safe" premise of D-03 is false in one place and the user must choose. F2 is an **existing upstream live test that WRT-13 will break**: `ClickHouseSinkTaskWithSchemaTest.schemaWithFixedStringMismatchTest` asserts the root cause is `IllegalArgumentException`. F3 is a spec gap: the DLQ `exception.message` header is the `queryId` string (unchanged, by `Processing.doInsert`), so the column/type/class are visible in the DLQ **stack trace header**, not the message header.

**Primary recommendation:** Implement the catch as `if (passThrough(e)) throw e; throw valueFreeWriteFailure(col, columnType, value, e);` with a final fork class `WithheldMessageException` (a `RuntimeException` built with the 4-arg `(message, cause, enableSuppression=false, writableStackTrace=true)` constructor, `toString()` overridden, `getMessage()` returning the fixed note). Land it in the fixed order HARN-08 -> WRT-14 pin -> WRT-13+WRT-15, using real `ProxySinkTask`/`ChunkFlusher`/`Utils.handleException` for the Docker-free classification pin, with a scripted writer subclass that calls the **real** `doWriteColValue`.

## Findings that change the plan

These are ordered by how much they can hurt. Each is verified; none is in CONTEXT.md.

### F1 (USER DECISION REQUIRED - D-03 trigger): an exact `RuntimeException` that carries the record value exists
`Column.convertEnumValues` [VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java:524-529]:
```java
    public Integer convertEnumValues(String value) {
        if ( this.enumValues != null ) {
            return enumValues.get(value);
        }
        throw new RuntimeException(String.format("No content in enum %s", value));
    }
```
It is called from `doWritePrimitive` for `Enum8`/`Enum16` and is reachable from `doWriteColValue`. Run on the unchanged writer with a hand-built `Column.builder().name("c").type(Type.Enum8).build()` and the value `Zq7x41kV`, the writer throws `java.lang.RuntimeException: No content in enum Zq7x41kV` [VERIFIED: scratch run, this session]. Under D-03 (exact `RuntimeException` passes through unchanged) this leaks the value.

Reachability: the only code that builds an `Enum8`/`Enum16` column is `Column.extractColumn` [VERIFIED: grep `Enum8|Enum16` in src/main/java/.../db/mapping and .../db/helper shows only Column.java:205-208], and it always passes `.enumValues(extractEnumValues(valueType))` (never null) [VERIFIED: Column.java:211]. So production-built columns cannot reach the leak; a hand-built `Column` (tests, a future code path) can.

Options for the user (D-03 says stop and ask; do not pick for them):
- **A. Accept and document.** Leave D-03 as is, add one Docker-free test that pins the hand-built-column behaviour as a known unreachable-in-production exception, and record it in FORK-DELTA/ledger. Weakens the "T-18-07 closed" claim to "closed for production-built columns".
- **B. (research recommendation) Edit the one throw site's text** to drop the value: `No content in enum` plus the column is not available there (method takes only `value`), so the message becomes a fixed text such as `No content in enum`. Zero operator impact (unreachable in production), makes D-03's premise true everywhere, one line in `Column.java` (a fork-modified file; anchors on `Column.java` are `Dynamic` 1 and `.isNullable(isNull)` 3, `@Setter(AccessLevel.PACKAGE) private boolean isNullable;` 1 - none touches this line). It contradicts D-03's "No throw sites are edited", hence the user's call.
- **C. Narrow the pass-through rule** (for example pass through exact `RuntimeException` only when its stack's top frame is not `Column.convertEnumValues`). Fragile, not recommended.

### F2: WRT-13 breaks an existing upstream live test, `schemaWithFixedStringMismatchTest`
[VERIFIED: src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java:887-912]
```java
        try {
            chst.put(sr);
        } catch (RuntimeException e) {
            assertInstanceOf(IllegalArgumentException.class, Utils.getRootCause(e), "Could not detect size mismatch for FixedString");
        }
```
A too-long FixedString value makes `ClickHouseChecker.notLongerThan` throw `IllegalArgumentException("length of byte array value is 8, which should NOT longer than 2")` [VERIFIED: scratch run; bytecode `ClickHouseChecker.newException` builds `java/lang/IllegalArgumentException`], which reaches the generic catch. Today `Utils.getRootCause(e)` (deepest cause) is that IAE. After D-05 the deepest cause is the sanitized copy (not an `IllegalArgumentException`), so this live test goes red in the full suite. This is the only such assertion: the other two `getRootCause` assertions in the live tests (`ClickHouseSinkTaskWithSchemaTest.java:501`, `ClickHouseSinkTaskWithSchemaProxyTest.java:313`) expect `DataException`, which D-01 passes through unchanged [VERIFIED: grep `getRootCause` in src/test].

Plan consequence: the WRT-13 commit must update this assertion. "Never weakened" is satisfied by a **flip to an equally strong assertion**: assert the root cause is the sanitized class and that its recorded original class name is `java.lang.IllegalArgumentException` (the sanitized class needs a public `getOriginalClassName()` accessor). The file is upstream-identical and in the spotless baseline-red set (F6), so keep the edit minimal. Record it in the ledger.

### F3: the DLQ `exception.message` header is the queryId, not the column text
Kafka 4.1.1 `DeadLetterQueueReporter.populateContextHeaders` writes `context.error().getClass().getName()` to `__connect.errors.exception.class.name`, `context.error().getMessage()` to `__connect.errors.exception.message`, and `error.printStackTrace(...)` bytes to `__connect.errors.exception.stacktrace` [CITED: https://raw.githubusercontent.com/apache/kafka/4.1.1/connect/runtime/src/main/java/org/apache/kafka/connect/runtime/errors/DeadLetterQueueReporter.java]. The exception handed to the reporter is the one `ProxySinkTask` keeps in `FailedRecords`, i.e. the `RuntimeException(queryId.toString(), e)` that `Processing.doInsert` builds [VERIFIED: Processing.java:72, 96; ProxySinkTask.java:104-106]. So the **message header always carries the queryId text** (`Topic: [..], Partition: [..], MinOffset: [..], MaxOffset: [..], (QueryId: [..])` [VERIFIED: QueryIdentifier.java toString]) both before and after this phase. The Phase 18 ledger already states this for L-25/L-28.

SC2 / WRT-13 wording says the DLQ "exception headers (message and stack trace) name the column, the ClickHouse type and the exception class". That is literally true only for the **stacktrace** header (the `Caused by: ...DataException: Failed to write column ...` line). Changing `Processing.doInsert`'s wrapper to surface the cause message is out of scope (hot path, not in CONTEXT). Planning consequence: D-12's DLQ test must assert (a) sentinel absent from **both** headers, (b) column name, `Type` token and original class name present in the **stacktrace** header, and (c) the message header equals the queryId text, so the limitation is pinned rather than hidden. Record it in the ledger "How an operator notices" column. Raise it with the user at plan review only if they read SC2 as requiring the message header.

### F4: a numeric sentinel can be vacuous - the JDK truncates the value in `NumberFormatException`
`new BigInteger("SENTINELq7Zx41kV")` (the INT128/UINT128 string path, `doWritePrimitive`) throws `NumberFormatException: For input string: "SENTINE"` - only the first 7 characters (the first digit group is `length % 9`) [VERIFIED: scratch run: V1 and V2 `int128` runs]. A full-sentinel absence check passes **on the unchanged writer**, so the test would prove nothing. With an 8-character value (`Zq7x41kV`) the whole value is embedded (`For input string: "Zq7x41kV"`) [VERIFIED: Kinds run]. Rule for the numeric case: use a sentinel of **at most 9 characters**, and keep the `assertRawJdkExceptionEmbedsSentinel`-style self-check (`new BigInteger(sentinel)` message contains the sentinel). `BigDecimal` (Decimal column, invalid string) leaks one character only (`Character Z is neither a decimal digit number, ...`), so use INT128/UINT128 for "unparseable number". A Decimal **out of range** case is useful for D-11: `BigDecimal(12345678901234512.00) should be between -1000000000 and 1000000000 exclusive of both values` embeds the value [VERIFIED: Kinds run].

### F5: the nested L-12 `RuntimeException` is unreachable through real columns inside `doWriteColValue`
L-12 is thrown only by `doWriteCol` [VERIFIED: ClickHouseWriter.java:1073 and 1088, `throw new RuntimeException(String.format("An attempt to write null into not nullable column '%s'", name));`]. Inside `doWriteColValue`, `doWriteCol` is called only from the TUPLE lambda with `defaultsSupport=false` [VERIFIED: ClickHouseWriter.java:782-790], and the guard immediately above it throws the WRT-08 `DataException` for exactly the same condition (`containsKey && null/empty && !nullable && !coercedToEmpty && != VARIANT`). Run: a Tuple child with a null non-nullable value gives the WRT-08 `DataException`, a nullable child gives no exception [VERIFIED: Kinds run]. So CONTEXT's "reached through Tuple/Map children" is not reachable with real data; D-11's "nested exact RuntimeException" kind must be **provoked by a test subclass overriding `doWriteCol`** (protected) to throw the L-12 text. The real exact-`RuntimeException` pass-through that is reachable is the JSON hint (JSON column, `binaryFormatWrtiteJsonAsString` off).

### F6: two files this phase must edit are in the spotless baseline-red set
The 46-file baseline (`spotlessApply` diff, import-order lines only) includes `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (19 changed lines), `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java` (55 changed lines), `db/mapping/Column.java` (if option F1-B), `ClickHouseHelperClient.java`, `UtilsTest.java`, `ClickHouseHelperClientTest.java`, `ClickHouseHelperClientForkLiveTest.java` [VERIFIED: `spotlessApply` run on a `git archive HEAD` copy, 46 files; the sorted list is not retained in the repo]. The files `WriterLogMaskingUnitTest`, `ClickHouseWriterNullMessageUnitTest`, `InMemoryDLQ`, `LogCapture`, `CapturingClickHouseWriter`, `Utils.java`, `UtilsNullMessageClassificationUnitTest`, `WriterErrorPathLiveTest` are clean. Spotless here is `importOrder()` + `removeUnusedImports()` only (`googleJavaFormat()` is commented out) [VERIFIED: build.gradle.kts:495-501]. Making `ClickHouseWriter.java` "spotless-clean on its own" (CONTEXT process rule) would reorder ~19 import lines of an upstream-synced file, which conflicts with the "additive, rebasable" constraint and with Phase 21 D-14's "no tree-wide reformat".

Recommendation (planner to confirm in the plan): for baseline-red files, **do not reorder existing imports**; insert any new import in the position `spotlessApply` would choose (`org.apache.kafka.connect.errors.ConnectException` sorts immediately before the existing `DataException` import; the new fork class goes in its sorted spot among the `com.clickhouse.kafka.connect.util.*` imports), and gate with "no new violations": the post-edit per-file `spotlessApply` diff has the **same hunks' +/- line counts** as the baseline (19 and 55), and every other touched or new file has an empty diff. Use the throwaway-copy recipe (not a git worktree if avoidable: `git archive HEAD | tar -x -C <empty dir>`, `git init`, `gradlew spotlessApply --offline`), as `git stash` is shared across worktrees (21-RESEARCH Pitfall 3). The whole-tree md5 `3393873370f9b48d2239131eedba8ff8` will legitimately change because hunk headers carry line numbers; compare file lists and per-file counts instead.

### F7: HARN-08 wording vs D-10
REQUIREMENTS HARN-08 and ROADMAP SC3 say the test "overrides one level up instead, in the `CapturingClickHouseWriter` pattern". D-10 (locked) instead uses a hand-written `ClickHouseHelperClient` **subclass** injected into `chc`. D-10 is the only option with **no production change**, because `doInsertRawBinary` calls `chc.isUseClientV2()`, `chc.pinReplica()` and `chc.unpinReplica()` directly [VERIFIED: ClickHouseWriter.java:1170-1203] and the method under test is `doInsertRawBinary` itself. The criterion's operative clause is "no longer inline-mocks ... with Mockito" and "closes the OpenJ9 `retransformClasses` risk"; both are met. Have the plan's must-haves and the verifier state this reading explicitly so SC3 is not judged on the phrase "one level up".

### F8: the `Type` token for Decimal is `Decimal`, not `DECIMAL`
`Type` is [VERIFIED: src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Type.java:3-34]:
```java
public enum Type {
    UNKNOWN,
    INT8,
    INT16,
    INT32,
    INT64,
    INT128,
    INT256,
    STRING,
    FLOAT32,
    FLOAT64,
    BOOLEAN,
    ARRAY,
    MAP,
    TUPLE,
    VARIANT,
    Date,
    Date32,
    DateTime,
    DateTime64,
    UUID,
    UINT8,
    UINT16,
    UINT32,
    UINT64,
    UINT128,
    UINT256,
    Decimal,
    FIXED_STRING,
    Enum8,
    Enum16,
    JSON,
    DYNAMIC
}
```
D-09's example `DECIMAL` is wrong for the enum: `String.format("%s", Type.Decimal)` prints `Decimal`. Tests must build expected text from the enum value (`String.format(..., Type.UUID)`) as `WriterLogMaskingUnitTest` already does, never from a hand-typed token.

### F9: D-04's Code-33-in-value effect exists on V1 and at the hook level only
The "second catch" matches `e.getMessage()` of whatever `doInsertRawBinaryV1/V2` throws. On V1 that is the raw writer exception, so a value containing `ClickHouseException: Code: 33` triggers the refresh today. On V2 the thrown message is the client wrapper text `Insert failed (Attempt: 1/4 - Duration: ...)` [VERIFIED: Real run], which never contains record content, so V2 never had the effect. The D-04 pin therefore belongs at the hook level (a scripted `doInsertRawBinaryV1` that runs the real `doWriteColValue`), and the ledger row should say "client V1 / RowBinary route".

## Architectural Responsibility Map

This is a single-process Kafka Connect sink; the "tiers" are code layers. Browser/CDN/DB tiers do not apply.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Detect and describe a value-conversion failure | `ClickHouseWriter.doWriteColValue` (serialization layer) | `Column`/`Data` metadata | Only here are the column, `Type` and `Data` known together; the message must be built from metadata |
| Value-free cause chain (sanitized copy) | New fork class in `util` (or `sink.db`) | `ClickHouseWriter` private helper | Pure Throwable manipulation; keeps the catch body short |
| Retry classification | `Utils.handleException` (unchanged) | `Processing.doInsert` wrapper, `ProxySinkTask`/`ChunkFlusher` callers | Must be invariant; the phase proves it, never edits it |
| Table-mapping refresh on schema drift | `ClickHouseWriter.doInsertRawBinary` generic catch (unchanged) | - | Reads `e.getMessage()`; must not be steered by record text |
| DLQ / Connect status rendering | Kafka Connect runtime (`printStackTrace`, DLQ headers) | `ErrorReporter` | Out of our control; we shape what it renders via `toString()` and the cause chain |
| Evidence | Docker-free unit tests (`testUnit`) + live `put()` tests on `latest` x V2 | `LogCapture`, `InMemoryDLQ` | Two layers per D-12 |

## Standard Stack

No new libraries. Everything used is already on the classpath.

### Core (existing, versions read from the tree)
| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| Kafka Connect API | 4.1.1 | `DataException`, `ConnectException`, `RetriableException` | The `ClassCastException` branch already throws `DataException(String, Throwable)` [VERIFIED: javap of connect-api-4.1.1.jar: `DataException(java.lang.String, java.lang.Throwable)`; `DataException extends ConnectException`] |
| ClickHouse client-v2 | 0.9.6 | Wraps writer exceptions on V2 | `HttpAPIClientHelper.wrapException` maps IOException -> `DataTransferException`, others -> `com.clickhouse.client.api.ClickHouseException` [VERIFIED: bytecode] |
| clickhouse-data | 0.9.6 | `BinaryStreamUtils`, `ClickHouseChecker` | All write-side throw sites are `IllegalArgumentException` (see survey) |
| JUnit Jupiter | 5.11.3 | Tests | Existing |
| reload4j via slf4j-reload4j | 2.0.20 | `LogCapture` appender | Existing test helper |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Fork `WithheldMessageException` class | Reuse `DataException`/`RuntimeException` with message override | A plain subclass cannot keep the original class name in `printStackTrace` output without overriding `toString()`, and cannot drop suppressed exceptions without the 4-arg constructor. A dedicated tiny class is the minimal solution |
| Mockito for the helper client | Hand-written subclass (D-10) | Locked; avoids OpenJ9 `retransformClasses` |

**Installation:** none. No `npm`/`pip`/`cargo`/Gradle dependency is added.

## Package Legitimacy Audit

No external packages are installed or added by this phase. Audit not applicable. Packages removed due to `[SLOP]`: none. Packages flagged `[SUS]`: none.

## Architecture Patterns

### System Architecture Diagram (failure path, measured)

```
Kafka SinkRecord (value carries sentinel)
   |
   v
ClickHouseSinkTask.put -> DeliveryStrategy (Direct | AtLeastOnceBuffer | ExactlyOnceBuffer)
   |                       all three end in ChunkFlusher.putDirect
   v
ChunkFlusher.putDirect --------------------------------------------------+  (handleException pass #2, shape B)
   |                                                                      |
   v                                                                      |
ProxySinkTask.put -> Processing.doLogic -> Processing.doInsert            |
   |                         catch(Exception e) -> new RuntimeException(queryId.toString(), e)
   |                                                                      |
   |   Utils.handleException (pass #1, shape A) <-------------------------+
   |        rootCause = getRootCause(e, true)   [deepest cause, or first ServerException/V1 ClickHouseException]
   |        retriable: server code in list | SocketTimeout | UnknownHost | (retryOnSocket && SocketException)
   |                   | IOException whose message starts "Read/Write timed out after"
   |        else: tolerance=all -> returns (ProxySinkTask sends FailedRecords to DLQ)
   |              tolerance=none -> throw RuntimeException("Number of records: n", e)
   v
ClickHouseWriter.doInsert -> doInsertBatch -> doInsertRawBinary(retry=true)
   |   try { V2: client.insert(..., DataStreamWriter) | V1: writeRecordsRowBinary on caller thread }
   |   catch ServerException  -> [UNREACHABLE for conversion failures: measured]
   |   catch Exception        -> reads e.getMessage() vs "ClickHouseException: Code: 33|131"
   |                              LOGGER.error("Error inserting records", e)  <- stack trace with Caused by
   v
writeRecordsRowBinary -> doWriteCol -> doWriteColValue(col, stream, Data, defaults)
   switch(Type) ... doWritePrimitive / doWriteDates / Decimal / Map / Array / Tuple / ...
   catch ClassCastException -> DataException("Failed to write column `x`: cannot convert ...", e)   [UNCHANGED]
   catch Exception e        -> LOGGER.error(WRITE_VALUE_FAILURE_LOG_FORMAT, ...)                    [UNCHANGED]
                               NEW: if passThrough(e) throw e;
                                    throw DataException(metadata-only message, WithheldMessageException.of(e))
```

### Recommended Project Structure
```
src/main/java/com/clickhouse/kafka/connect/
├── sink/db/ClickHouseWriter.java              # catch body + 2-3 private helpers + new constants near L564-570
└── util/WithheldMessageException.java         # NEW (name is discretion): sanitized-copy Throwable
src/test/java/com/clickhouse/kafka/connect/sink/
├── db/ClickHouseWriterNullMessageUnitTest.java        # HARN-08 (edit; keep every name/DisplayName)
├── db/<ScriptedWriteFailureWriter or similar>         # test subclass that calls the real doWriteColValue
├── WriterFailureClassificationUnitTest.java           # WRT-14 pin; MUST be in package ...connect.sink (ChunkFlusher is package-private)
├── db/WriterLogMaskingUnitTest.java                   # flip uuidWriteFailureRethrows... (WRT-13 commit)
├── WriterErrorPathLiveTest.java                       # live put() cases (D-12)
└── dlq/InMemoryDLQ.java                               # add an exceptions() accessor (additive)
```

### Pattern 1: pass-through-or-replace catch (the production change)
**What:** keep the log line, then rethrow unchanged for the three pass-through families, otherwise throw a metadata-only `DataException` whose cause is a sanitized copy.
**When:** only in the generic catch of `doWriteColValue`; failure path only (no hot-path cost).
**Example** (illustrative skeleton; names are discretion, the structure is what matters):
```java
        } catch (Exception e) {
            LOGGER.error(WRITE_VALUE_FAILURE_LOG_FORMAT, value == null ? "<value null>" : value.getFieldType(), col.getName(), columnType,
                    e.getClass().getName());
            if (isSafeToRethrowUnchanged(e)) {
                throw e; // precise rethrow keeps the method's `throws IOException` contract
            }
            throw new DataException(buildWriteFailureMessage(col, columnType, value, e), WithheldMessageException.of(e));
        }

    private static boolean isSafeToRethrowUnchanged(Exception e) {
        if (e instanceof ConnectException || e.getClass() == RuntimeException.class) {
            return true;
        }
        // identity-guarded walk: a cause cycle must not loop forever
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof IOException) {
                return true;
            }
        }
        return false;
    }
```
Source: behaviour measured this session; `DataException(String, Throwable)` [VERIFIED: javap]. Keep `WRITE_VALUE_FAILURE_LOG_FORMAT` exactly as is: anchor row `WRITE_VALUE_FAILURE_LOG_FORMAT 2` counts the constant line and this use.

Two details the skeleton hides:
- The pass-through predicate covers the checked `IOException` only because the method is declared `throws IOException`; `throw e` inside a `catch (Exception e)` compiles via Java's precise rethrow only while `e` is effectively final and the `try` body throws nothing wider than `IOException`+unchecked. Do not reassign `e`.
- The pass-through order matters for one subtle case: a lambda wrapper `new RuntimeException(ioException)` is an **exact** `RuntimeException`, so it passes via D-03 and via the IO scan; either rule alone suffices, and the test for it should assert pass-through identity (`assertSame`).

### Pattern 2: the sanitized cause (D-05/D-06)
**What:** a final `RuntimeException` subclass created with the protected 4-arg constructor so suppressed exceptions are structurally dropped.
```java
public final class WithheldMessageException extends RuntimeException {
    private static final String WITHHELD_NOTE = "(message withheld: may contain record data)";
    private static final int MAX_CAUSE_DEPTH = 32;
    private final String originalClassName;

    private WithheldMessageException(String originalClassName, Throwable sanitizedCause) {
        super(WITHHELD_NOTE, sanitizedCause, /* enableSuppression */ false, /* writableStackTrace */ true);
        this.originalClassName = originalClassName;
    }

    // builds the chain iteratively from the deepest recorded cause up; stops at MAX_CAUSE_DEPTH or on an identity repeat
    public static WithheldMessageException of(Throwable original) { /* ... setStackTrace(t.getStackTrace()) per node */ }

    public String getOriginalClassName() { return originalClassName; }

    @Override
    public String toString() { return originalClassName + " " + WITHHELD_NOTE; }
}
```
**`getMessage()` choice (D-06 leaves it to research): return the fixed note, not null.** Reason: `printStackTrace` (Connect task status, DLQ stacktrace header, reload4j) uses `toString()`, so the override gives the exact `Caused by: java.lang.IllegalArgumentException (message withheld: may contain record data)` line. Renderers that format as `getClass().getName() + ": " + getMessage()` (log4j2/Logback-style throwable proxies, which operators' Connect workers commonly use; Kafka 4.0 moved to Log4j2 for the broker/tools side [CITED: https://kafka-options-explorer.conduktor.io/kip/653/], Connect-specific rendering `[ASSUMED]`) never call the `toString()` override; with a null message they would print the bare fork class name and tell the operator nothing, with the fixed note they print `...WithheldMessageException: (message withheld: may contain record data)`. The original class name is also in the `DataException` message (D-07) and in the catch's own log line, so no information is lost on those renderers. `getMessage()` never contains original text either way.

Stack frames: `setStackTrace(original.getStackTrace())` on every node. Measured: Throwable's "... n more" elision still works because it compares frame arrays.

### Pattern 3: Docker-free test seam for the real catch (no Mockito)
A `ClickHouseWriter` subclass that overrides `doInsert(List<Record>, QueryIdentifier, ErrorReporter)` to call the **real** `doWriteColValue(col, stream, data, false)` for a scripted column/value/stream, then lets the real `Processing`, `ProxySinkTask`, `ChunkFlusher` and `Utils` do their work. `ProxySinkTask`'s constructor opens no connection (it builds `InMemoryState`, `SinkTaskStatistics`, `new ClickHouseWriter(...)`, `Processing`) [VERIFIED: ProxySinkTask.java:41-58]; only `start()` pings. Swap `Processing.dbWriter` by reflection exactly like `WriterErrorPathLiveTest.writeField` does, and never call `start()`. `ChunkFlusher`'s constructor is package-private [VERIFIED: ChunkFlusher.java `ChunkFlusher(ProxySinkTask, ClickHouseSinkConfig, ErrorReporter)`], so the classification test lives in package `com.clickhouse.kafka.connect.sink` and extends nothing (stays in `testUnit`; `ClickHouseBase` carries `@Tag("clickhouse")`).

### Anti-Patterns to Avoid
- **Asserting messages or root-cause classes in the WRT-14 pin** - it must pass unmodified before and after; outcome only (D-11).
- **Feeding hand-built exceptions to `handleException`** - does not exercise the catch (D-11).
- **A full-sentinel check on a numeric failure with a >9-character sentinel** - vacuous (F4).
- **Reordering imports in baseline-red files** (F6).
- **Putting `Mockito` anywhere new** - the whole point of HARN-08.
- **Changing `Processing.doInsert`'s wrapper** to surface the cause message in the DLQ message header - out of scope, hot path (F3).

## Failure-kind survey (what actually reaches the generic catch)

Every row was executed against the unchanged writer through `doWriteColValue` (and, where marked, through real `doInsertRawBinary` on V1/V2). "Replace?" is the D-01/D-03 verdict. Messages shortened. [VERIFIED: scratch runs, this session]

| Kind | How provoked (real data unless noted) | Thrown (type: message) | Replace? |
|------|----------------------------------------|------------------------|----------|
| invalid UUID | UUID column, STRING `SENTINELq7Zx41kV` | `IllegalArgumentException: Invalid UUID string: <value>` | yes |
| unparseable integer | INT128/UINT128 column, STRING | `NumberFormatException: For input string: "<first group of value>"` (F4) | yes |
| integer too wide | INT128, 60 nines | `IllegalArgumentException: Expected 16 bytes but got 26 from: <full value>` | yes |
| Decimal out of range | `Decimal(9,2)`, "123456789012345.12" | `IllegalArgumentException: BigDecimal(<value>) should be between ... exclusive of both values` | yes |
| Decimal unparseable | Decimal, STRING | `NumberFormatException: Character Z is neither a decimal digit number, ...` (1 char leaks) | yes |
| Decimal too many fractional digits | "1.23456789" | no exception (rounded) | n/a (so `ArithmeticException` must be scripted) |
| unknown enum label | Enum8 with `enumValues` set | `NullPointerException: Cannot invoke "java.lang.Integer.byteValue()" because the return value of ...convertEnumValues(String) is null` | yes |
| enum without `enumValues` | hand-built Column | **exact `RuntimeException: No content in enum <value>`** | **F1** |
| FixedString too long | FIXED_STRING precision 2 | `IllegalArgumentException: length of byte array value is 8, which should NOT longer than 2` | yes (F2 casualty) |
| wrong Java type | INT32 column, STRING value | `ClassCastException` -> CCE branch `DataException` | n/a (existing branch) |
| stream IO plain / timeout prefix / `SocketException` / `SocketTimeoutException` | `OutputStream` that throws on write | the same `IOException` instance | no (pass through) |
| IO inside Tuple field | `OutputStream` failing after N bytes | `RuntimeException("java.io.IOException: boom")` cause `IOException` | no (exact RuntimeException and IO chain) |
| IO inside Map value | stream failing after the size varint | same wrapper shape (Map lambda L713) | no |
| nested null (WRT-08) | Tuple child null, non-nullable | `DataException: column 't' (Tuple(...)) tuple field 'a': null in non-nullable slot` | no (ConnectException) |
| JSON column, flag off | JSON column | exact `RuntimeException: Writing JSON in binary is not supported yet. ...` | no (D-03) |
| nested L-12 | **not reachable with real columns (F5)**; script via `doWriteCol` override | exact `RuntimeException: An attempt to write null into not nullable column '<name>'` | no (D-03) |
| `ArithmeticException` | not producible by a real conversion here; script via `doWritePrimitive` override | - | yes |

Unchecked sites inside `BinaryStreamUtils`/`ClickHouseChecker` (clickhouse-data 0.9.6): `javap -c` shows `BinaryStreamUtils` contains **no** `java/lang/RuntimeException` reference, and `ClickHouseChecker.newException` returns `IllegalArgumentException` for every check [VERIFIED: javap output]. The only exact `RuntimeException` throw sites reachable from `doWriteColValue` in `src/main` are the JSON pair (L845, L849), the L-12 pair (L1073, L1088) via `doWriteCol`, the Map/Tuple/Array lambda wrappers (L714, L742, L786), and `Column.convertEnumValues` (F1) [VERIFIED: grep `new RuntimeException` over src/main]. JSON serialization (`OBJECT_MAPPER.writeValueAsBytes`, L840) throws `JsonProcessingException`, an `IOException` subclass, so it passes through under D-01; Jackson mapping failures can in principle embed map keys in a "reference chain", which would be an unreplaced exposure `[ASSUMED]` (see Assumptions A2).

## D-13 trace: which `doInsertRawBinary` branch a conversion failure reaches

Measured with the real `ClickHouseWriter.doInsertRawBinary`, a real `ClickHouseHelperClient`, and (a) a local `com.sun.net.httpserver` stand-in, then (b) a real ClickHouse `clickhouse/clickhouse-server:latest` (26.9.19.9), three records, the bad value in record 0 and in record 2. [VERIFIED: scratch runs `Scratch`, `Real`, this session]

| Client | What `doInsertRawBinary` sees | Branch | Rows landed |
|--------|-------------------------------|--------|-------------|
| V2 | thrown **synchronously** from `Client.insert(Client.java:1518)` via `runAsyncOperation` (async is off): `com.clickhouse.client.api.ClickHouseException: Insert failed (Attempt: 1/4 - Duration: PT0.02s)`, cause = the original exception, 0 suppressed | generic (second) catch; the `ServerException` branch is not entered | 0 |
| V1 | the raw exception from `writeRecordsRowBinary` on the caller thread (`doInsertRawBinaryV1:1371`) | generic (second) catch | 0 |

Logged: `Error writing value of STRING to the column `c1` of type UUID: exception java.lang.IllegalArgumentException` (no throwable) then `Error inserting records` **with** the throwable (V2: `ClickHouseException`; V1: the raw exception). So after the change, both logs are value-free by construction once the chain is. A `ServerException` never appears for a client-side conversion failure on either client (the writer throws mid-stream; HttpClient aborts and never reads a response; on V1 the exception is thrown before the response future is read). SC2's "both logs" is therefore met by the generic-branch log plus this written trace; **no user stop is needed** (D-13 condition not met). A tripwire is cheap: the live D-12 test should assert that no `ServerException` is in the thrown chain.

Corollary for D-12's Docker-free layer: model the V2 shape by wrapping the writer exception in `new com.clickhouse.client.api.ClickHouseException("Insert failed ...", cause)` (public constructor, `RuntimeException` subclass [VERIFIED: javap]); it is **not** matched by `Utils.isServerError` (which tests the V1 class `com.clickhouse.client.ClickHouseException` and `ServerException`) [VERIFIED: Utils.java imports and `isServerError`].

## WRT-14: wrapping shapes and the outcome matrix

Every writer failure reaches `Utils.handleException` through one of two shapes (all three delivery strategies - Direct, AtLeastOnceBuffer, ExactlyOnceBuffer - call `ChunkFlusher.putDirect`; the buffer strategies only clear their buffer and rethrow the same exception) [VERIFIED: DirectDeliveryStrategy.java, AtLeastOnceBufferStrategy.java:51-62, ExactlyOnceBufferStrategy.java:57, ChunkFlusher.java:68-77]:

| Shape | Caller | `e` handed to `handleException` | Records arg |
|-------|--------|----------------------------------|-------------|
| A | `ProxySinkTask.put` catch (L96-L101) | `RuntimeException(queryId.toString(), writerThrowable)` from `Processing.doInsert` | the put batch |
| B | `ChunkFlusher.putDirect` catch (L72) | whatever `ProxySinkTask.put` threw: `RetriableException(wrapper)` or `RuntimeException("Number of records: n", wrapper)` | the put batch |

Under `errors.tolerance=all` shape A returns normally and `ProxySinkTask` sends `FailedRecords` to the DLQ, so shape B never sees a writer failure then. `e instanceof DataException && !errorsTolerance` is dead for writer failures in both shapes (the top-level `e` is always a `RuntimeException`), which is why making the replacement a `DataException` cannot change an outcome.

Expected outcomes for the pin (derived from `Utils.handleException` [VERIFIED: Utils.java:71-142]; to be re-confirmed by the test itself on the unchanged writer):

| Failure kind | tolerance=false | tolerance=true |
|--------------|-----------------|----------------|
| UUID / number / Decimal / FixedString / NPE / scripted `ArithmeticException` | `RuntimeException("Number of records: n")` thrown, not `RetriableException` | returns normally |
| `IOException` unrelated message | same non-retriable wrap | returns normally |
| `IOException("Write timed out after ...")` / `("Read timed out after ...")` | `RetriableException` | `RetriableException` (retriable branch precedes the tolerance branch) |
| `SocketTimeoutException` | `RetriableException` | `RetriableException` |
| `SocketException`, `retryOnSocketException=true` | `RetriableException` | `RetriableException` |
| `SocketException`, `retryOnSocketException=false` | non-retriable wrap | returns normally |
| `UnknownHostException` | `RetriableException` | `RetriableException` |
| IO inside Tuple/Map lambda (wrapper) | follows the inner IO row | follows the inner IO row |
| nested `DataException`, nested exact `RuntimeException` | non-retriable wrap | returns normally |

In shape B, a first-pass `RetriableException` is re-handled and comes out as a `RetriableException` again; a first-pass `RuntimeException("Number of records")` comes out as a `RuntimeException` again. The pin should cover A for the full matrix and B for at least one retriable and one non-retriable kind per `errorsTolerance`.

**Why the outcome cannot change:** `getRootCause(e, true)` walks to the deepest cause unless it meets a V1 `ClickHouseException`/`ServerException`. After the change, an IO-free, server-free chain ends at `WithheldMessageException` (a `RuntimeException`), which matches none of `SocketTimeoutException`, `UnknownHostException`, `SocketException`, `IOException`, so control reaches the tolerance branch exactly as before. Any chain containing an `IOException` is passed through untouched. The writer path never raises a server exception (no network call inside `doWriteColValue`), so the sanitized copy's inability to preserve `ServerException`/`ClickHouseException` identity cannot matter; if the planner wants belt and braces, add both types to the pass-through scan at zero cost.

## HARN-08 facts (D-10 research obligation)

- **Real constructor, dummy builder: no connection, no owned threads.** `new ClickHouseHelperClient.ClickHouseClientBuilder("127.0.0.1", <closed port>, null, null, -1).build()` against a `ServerSocket` acceptor: 0 connections accepted in 1.5 s; the only new thread was `ForkJoinPool.commonPool-worker-1` (the JDK's shared pool, still alive after `close()`, not owned by the client); first construction took ~850 ms (class loading), later ones are fast [VERIFIED: scratch `threads` run]. The constructor builds the V2 `Client` and a V1 `ClickHouseNode` only (`createClientV2()`, `createClientV1()`) [VERIFIED: ClickHouseHelperClient.java:97-117].
- **Overridable surface:** `isUseClientV2()` is Lombok-generated (`@Getter private boolean useClientV2`), non-final; `pinReplica()` and `unpinReplica()` are public and non-final [VERIFIED: ClickHouseHelperClient.java:751, 807]. The public constructor is `ClickHouseHelperClient(ClickHouseClientBuilder)`, so a subclass calls `super(builder)`; the builder's `useClientV2(boolean)` also sets the flag, but D-10 overrides `isUseClientV2()` to be explicit and countable.
- **`close()`** closes the V2 `Client` [VERIFIED: ClickHouseHelperClient.java:812-816]; use try-with-resources or `@AfterEach`.
- **Exact counts the Mockito `verify` calls map to** (from the current test): `never().pinReplica()` -> `pinCalls == 0`; `times(1).pinReplica()` and `times(1).unpinReplica()` -> both `== 1`. `unpinReplica` was only verified in the retry test; assert `== 0` in the no-retry tests only if it was implicitly guaranteed (it is: `unpinReplica` is in the `finally` of the retry block). Do not add assertions that were not there unless they are exact consequences.
- **Baseline passes:** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --offline --tests "*WriterLogMaskingUnitTest" --tests "*ClickHouseWriterNullMessageUnitTest" --tests "*UtilsNullMessageClassificationUnitTest"` -> 21 tests, 21 succeeded, BUILD SUCCESSFUL in 13 s [VERIFIED: command output, this session].

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Dropping suppressed exceptions | A custom `addSuppressed` override | `RuntimeException(String, Throwable, false, true)` | `enableSuppression=false` makes `addSuppressed` a no-op and `getSuppressed()` empty, by contract |
| Copying frames | Manual frame formatting | `setStackTrace(original.getStackTrace())` | `printStackTrace` then renders and elides "... n more" correctly |
| Rendering the Connect DLQ headers in the live test | A new serializer | `error.printStackTrace(new PrintStream(bos, true, UTF_8))` for the stacktrace header and `getMessage()` for the message header | That is exactly what Kafka 4.1.1 does [CITED: DeadLetterQueueReporter.java, see F3]. `connect-runtime` is **not** on the test classpath (only `connect-api` 4.1.1), so the algorithm must be re-implemented in the test, with a comment pointing to the source |
| Log capture | A new appender | `LogCapture` (`allTexts()` already includes the message, the rendered throwable lines, and every cause's `getMessage()`) | Existing, detaches on close |
| Cause-chain walking in tests | Recursion without a guard | an identity-checked loop (as `WriterErrorPathLiveTest.causeChain` does) | cycles |
| Helper-client counting | Mockito `spy`/`mock` | a 15-line subclass | OpenJ9 `retransformClasses` risk (Phase 18 deferred item) |

**Key insight:** the leak is a rendering problem, not a classification problem. Everything that the Connect runtime or the log layer prints comes from `getMessage()`, `toString()` and the cause chain of exceptions we construct; controlling those three on one small class is enough, and it never touches `Utils`.

## Common Pitfalls

### Pitfall 1: the sentinel is detected on the old code but not the new one for the wrong reason (or vice versa)
**What goes wrong:** a negative assertion passes because the JDK never embedded the value (F4), or a "present" assertion is tautological.
**How to avoid:** every case asserts both directions (D-12) and keeps a raw-JDK self-check; numeric sentinel <= 9 chars; build expected strings from enum values and constants.
**Warning signs:** the new tests are green on the unchanged writer.

### Pitfall 2: redundant per-level logging after the change
**What goes wrong:** a failure nested in Array/Map/Tuple logs `Error writing value of ...` once per nesting level (the inner level names the child column, outer levels name the parent and class `DataException`/`RuntimeException`). This exists today and is unchanged; do not "fix" it and do not assert a single line for nested cases. The pass-through of the inner `DataException` is what keeps the innermost column in the final message (D-02).

### Pitfall 3: `toString()` is not honoured by every renderer
See Pattern 2. Pin the `printStackTrace` rendering in tests (that is what Connect status, the DLQ stacktrace header and `LogCapture` use), and make `getMessage()` the fixed note so other renderers stay informative.

### Pitfall 4: anchors are line counts and are evaluated at every commit
`fork-anchors.txt` rows are `path <TAB> fixed-string <TAB> expected count <TAB> id`, count = matching **lines** (`grep -cF`), evaluated at every commit in the range against that commit's own copy of the file [VERIFIED: fork-anchors.txt header; verify-merge-invariants.sh]. So: a new row and the code it counts must land in the same commit; new code must not add a second line containing an anchored string (watch `String failureMessage = e.getMessage();` 1, `WRITE_VALUE_FAILURE_LOG_FORMAT` 2, `buildRowBinaryUnsupportedTypeMessage` 5, `shouldSkipTableMetadata()` 8, `DATE_CONVERSION_UNSUPPORTED_MESSAGE_FORMAT` 2, `MISSING_NON_NULLABLE_MESSAGE_FORMAT` 2, `of type " + columnType, e);` 0, `skipEmptyCdcRecord(` 3, `logDateTimeParseFailure(` 3, `new ClickHouseHelperClient.ClickHouseClientBuilder(` 2). A cheap local mirror of invariant 3, run now on the clean tree: `rows=59 bad=0`:
```bash
bad=0; rows=0; while IFS=$'\t' read -r path pat cnt id; do case "$path" in \#*|"") continue;; esac; rows=$((rows+1)); got=$(grep -cF -e "$pat" "$path" 2>/dev/null); [ "$got" = "$cnt" ] || { echo "MISMATCH $path '$pat' want $cnt got $got ($id)"; bad=$((bad+1)); }; done < .planning/scripts/fork-anchors.txt; echo "rows=$rows bad=$bad"
```
Use it per commit; run the real gate (`bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`, minutes; run in background and redirect output, per 21-RESEARCH Pitfall 4) at the phase gate and after the WRT-13 commit. Candidate new rows (counts to be measured by the executor on the final text): `WithheldMessageException` class declaration line (1), the new message-format constant name in `ClickHouseWriter.java` (2: definition + use), backs-ID `WRT-13`.

### Pitfall 5: the ROADMAP edit
The roadmap is edited by script elsewhere; an unanchored replace once truncated it (`c1480f8f`) [VERIFIED: ROADMAP.md "Editing this file by script"]. Edit only the `### Phase 999.3` section by line-addressed `Edit`. The delivered line format of the siblings is `**Delivered:** Phase 19 (go), shipped in `v1.7.0_gg2` on 2026-10-14 - ...`; for 999.3 the release is not published yet, so write `**Delivered:** Phase 22 (WRT-13..WRT-15), lands in the next release (Phase 24 publishes it)` and let Phase 24 rewrite it to "shipped in `v1.7.0_gg3`".

### Pitfall 6: the CCE branch must stay exactly as it is
It is "out of this phase". Sharing its `Failed to write column ` prefix means extracting a constant; if the executor refactors the CCE format to use that constant, the rendered text must be byte-identical (pinned by `ClickHouseWriterColumnContextTest`). Safer: add the constant and use it only in the new message; leave L858-869 untouched.

### Pitfall 7: Kafka field type / value class null-safety
`value` can be null and `value.getObject()` can be null; `value.getFieldType()` dereferences the schema. Mirror the CCE branch's expressions exactly (`value == null ? "null" : String.valueOf(value.getFieldType())`, and the null-safe class name) inside the message builder, because it runs while an exception is already in flight and must not throw its own.

### Pitfall 8: Connect wrapper mis-assumptions
The task-status trace is what Connect prints for the exception that escapes `put()`; the live test can only approximate it with `printStackTrace` of the thrown exception (Connect adds its own outer `ConnectException("Exiting WorkerSinkTask due to unrecoverable exception.")` which has no record data). State that approximation in the test.

## Code Examples

### Message builder (D-07; null-safe, metadata only)
```java
    private static final String WRITE_FAILURE_COLUMN_PREFIX = "Failed to write column `";
    private static final String WRITE_VALUE_CONVERSION_FAILURE_FORMAT =
            WRITE_FAILURE_COLUMN_PREFIX + "%s`: Kafka value of type %s (value class: %s) could not be written as ClickHouse type %s: %s";

    private static String buildWriteFailureMessage(Column col, Type columnType, Data value, Exception cause) {
        String sourceType = value == null ? "null" : String.valueOf(value.getFieldType());
        String valueClass = (value == null || value.getObject() == null) ? "null" : value.getObject().getClass().getName();
        return String.format(WRITE_VALUE_CONVERSION_FAILURE_FORMAT, col.getName(), sourceType, valueClass, columnType, cause.getClass().getName());
    }
```
Expected rendering for the UUID case with a column named `order_uuid` (the existing test constant) and a STRING field: `Failed to write column `order_uuid`: Kafka value of type STRING (value class: java.lang.String) could not be written as ClickHouse type UUID: java.lang.IllegalArgumentException`. It contains no `ClickHouseException: Code: 33|131` (the keys need the `: Code: ` infix), so the refresh match stays off for metadata-built text.

### Hook-level D-04 pin (V1 shape; shows the technique, not final code)
```java
    // ScriptedInsertWriter variant: the V1 hook runs the REAL conversion, so what reaches the catch is what V1 produces
    @Override
    protected void doInsertRawBinaryV1(List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults) throws IOException {
        hookCalls++;
        doWriteColValue(UUID_COLUMN, new ByteArrayOutputStream(), new Data(Schema.STRING_SCHEMA, CODE_33_IN_VALUE), false);
    }
    // assert: hookCalls == 1, fake helper client pinCalls == 0, thrown instanceof DataException
    // (on the unchanged writer the same script gives hookCalls == 2 and pinCalls == 1: that is the D-04 "before")
```

### Rendering what Connect renders (live DLQ layer)
```java
    private static String stacktraceHeader(Throwable error) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(bos, true, StandardCharsets.UTF_8)) {
            error.printStackTrace(stream);
        }
        return bos.toString(StandardCharsets.UTF_8);
    }
    // messageHeader = error.getMessage()   // == the queryId text (F3)
```
Note `file.encoding` is `windows-1252` for tests [VERIFIED: build.gradle.kts `systemProperty("file.encoding", "windows-1252")`], so always pass `UTF_8` explicitly, as above.

## State of the Art

| Old approach | Current approach | Impact |
|--------------|------------------|--------|
| Rethrow original, accept value in message (Phase 18 D-09) | Replace with metadata-only `DataException` + sanitized cause | closes T-18-07; root cause class behind `put()` changes (ledger row) |
| Inline Mockito mock of `ClickHouseHelperClient` | Hand-written subclass | removes OpenJ9 `retransformClasses` crash risk |

**Deprecated/outdated:** `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE` is `@Deprecated` upstream ("this part will be removed once V1 is deprecated") and stays as is.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Operators' Connect workers render throwables with a log4j2/Logback-style proxy that uses `getClass().getName() + ": " + getMessage()` and ignores `toString()` | Pattern 2 (`getMessage()` choice) | Low: choosing the non-null note is harmless if all renderers use `printStackTrace` |
| A2 | Jackson `JsonProcessingException` from `OBJECT_MAPPER.writeValueAsBytes(value)` (JSON column, `binaryFormatWrtiteJsonAsString`, STRUCT value, L840) does not embed record values; it passes through under D-01 because it is an `IOException` | Failure-kind survey | Medium-low: an unreplaced exposure for that one route; fix would be to exclude `JsonProcessingException` from the IO pass-through and keep the IO check for stream errors only (classification stays safe: Jackson errors are not retriable today either) |
| A3 | Connect task status shows `printStackTrace` of the exception escaping `put()` plus a value-free outer `ConnectException` | Pitfall 8 | Low |
| A4 | Kafka Connect runtime 4.1.1 (not on this classpath) populates the DLQ headers via `DeadLetterQueueReporter.populateContextHeaders` from `WorkerErrantRecordReporter` unchanged for sink `ErrantRecordReporter.report(record, error)` | F3 | Low: confirmed from the 4.1.1 source for the header algorithm; the path from `ErrantRecordReporter` to it was not re-read |
| A5 | `enumValues` is never null for any Enum column built outside tests (only `Column.extractColumn` builds them, grep over `src/main`) | F1 | Medium: if a path exists that this grep missed, F1 becomes reachable in production |
| A6 | The ClickHouse server version `latest` image is 26.9.19.9 today; behaviour measured on it | D-13 trace | Low (ROADMAP: check the `latest` image date first if the suite goes red unexpectedly) |

## Open Questions

1. **F1 / D-03: leak in `Column.convertEnumValues`.**
   - Known: exact `RuntimeException` with the value; unreachable from production-built columns; reachable from hand-built ones.
   - Unclear: whether the user wants A (accept + pin), B (edit the throw site) or C.
   - Recommendation: **B**. Ask before planning. If no answer is available, plan A with the pin and flag it in the ledger and FORK-DELTA; do not silently choose B.
2. **F3: does the user read SC2 as requiring the column text in the DLQ `exception.message` header?**
   - Known: that header is the queryId text before and after; changing it means changing `Processing.doInsert`.
   - Recommendation: keep as is; pin it; mention in the ledger. Raise only if the user objects.
3. **F6: spotless handling for the two baseline-red files.**
   - Recommendation: the "no new violations, per-file +/- counts equal baseline" gate described above. Confirm at plan review since it reads CONTEXT's "spotless-clean on its own" as "adds no new violation".
4. **F2: how to flip `schemaWithFixedStringMismatchTest`** (accessor name on the new class; keep it a live test).
   - Recommendation: assert `getRootCause(e)` is a `WithheldMessageException` whose `getOriginalClassName()` equals `IllegalArgumentException.class.getName()`.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | live tests, full suite | yes | `colima [profile=vz]` running (verified with `colima status -p vz`) | - |
| `clickhouse/clickhouse-server:latest` image | live tests | yes | pulled; server reports 26.9.19.9 | - |
| Gradle wrapper | all test tasks | yes | 9.6.1 (build output) | - |
| JDK for scratch experiments | research only | yes | OpenJDK 21 (Homebrew); the Gradle toolchain runs its own JDK 17 | - |
| `yq` | matrix lookups in AGENTS.md | not probed | - | not needed: this phase tests `latest` x V2 only |

Missing dependencies with no fallback: none. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; `timeout` is not available on this macOS shell (do not use it in recipes).

## Security Domain

`security_enforcement` is enabled (`.planning/config.json`: `"security_enforcement": true`, `"security_asvs_level": 1`, `"security_block_on": "high"`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | - |
| V3 Session Management | no | - |
| V4 Access Control | no | - |
| V5 Input Validation | partly | record values are untrusted input; the writer must not let them steer control flow (D-04) or leave through diagnostics |
| V6 Cryptography | no | - |
| V7 Error Handling and Logging | **yes** | error messages and logs carry no record data (the phase's whole purpose); stack frames kept, messages withheld |
| V8 Data Protection | yes | record values must not reach Connect status (REST-readable), DLQ topics (other consumers) or log aggregation |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| T-18-07: record value in a rethrown exception message reaching Connect status, DLQ headers and ERROR logs | Information Disclosure | metadata-only `DataException` + `WithheldMessageException` cause (D-05..D-08); sentinel tests in both directions (D-12) |
| Record content steering control flow (a value containing `ClickHouseException: Code: 33` triggers a mapping refresh) | Tampering | message no longer derived from the value; D-04 pin |
| Value leak via **suppressed** exceptions or **cause messages** | Information Disclosure | `enableSuppression=false`; sanitized recursive causes; test walks suppressed entries |
| Value leak via an exact `RuntimeException` pass-through (F1) | Information Disclosure | user decision (A/B/C) |
| Value leak via IO pass-through of `JsonProcessingException` (A2) | Information Disclosure | accepted residual unless the planner narrows the IO rule |
| Client-library debug logging of the exception | Information Disclosure | out of our control; V2 client logs `Retrying.` with the throwable only when it retries (it did not in any run here) |

## Validation Commands (config: `workflow.nyquist_validation` is `false`, so no formal Validation Architecture section)

| Purpose | Command |
|---------|---------|
| Cheap gate, per commit | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --offline` |
| Narrow loop | `... ./gradlew testUnit --offline --tests "*WriterLogMaskingUnitTest" --tests "*ClickHouseWriterNullMessageUnitTest" --tests "*<new classification test>"` |
| Phase gate, cache-busted | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` |
| Full suite on `latest` x V2 | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` (CLICKHOUSE_VERSION unset = latest, CLIENT_VERSION unset = V2). Exit 255 can be the intermittent OpenJ9 GPF: delete the ~8 GB core dump, re-run once |
| Anchors, quick | the `grep -cF` mirror in Pitfall 4 (seconds) |
| Anchors, real gate | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (minutes, run in background) |
| Spotless, per file | throwaway copy recipe (F6), never tree-wide `spotlessApply` |

Wave 0 gaps (additive test infrastructure, all green on the unchanged writer): `InMemoryDLQ.exceptions()` accessor; a root-logger `LogCapture` overload (optional, only if the live test wants client-library log lines in scope); the helper-client fake; the scripted-write-failure writer subclass.

## Recommended plan split and commit order

1. **22-01 HARN-08** (test-only, 1 commit): replace the mock by the counting `ClickHouseHelperClient` subclass; keep all names/DisplayNames/assertions; remove the Mockito imports from this class; `testUnit` green.
2. **22-02 WRT-14 pin + Wave 0 helpers** (test-only, 1-2 commits): the classification test in package `...connect.sink` driving real `ProxySinkTask`/`ChunkFlusher`/`Utils`; `InMemoryDLQ.exceptions()`; the scripted writer. **Run it on the unchanged writer, record the green output as evidence.** Red-before/green-after for the later WRT-13 tests is evidenced by running the new sentinel tests against the unchanged writer first (expected red), then after.
3. **22-03 WRT-13 + WRT-15** (1 commit, or a test-first commit pair if the first only adds tests that are expected red - not allowed by "testUnit green at every commit", so keep one commit): production change + constants + sanitized class + flipped `WriterLogMaskingUnitTest` case + flipped `schemaWithFixedStringMismatchTest` + new Docker-free sentinel tests + new live cases + FORK-DELTA (item 6, WRT-12 row, new row) + `fork-anchors.txt` rows + ROADMAP 999.3 "Delivered" + `22-BEHAVIOUR-LEDGER.md` (rows L-42.. continuing after `19-BEHAVIOUR-LEDGER.md`'s L-41, covering: root-cause type change; withheld-note cause line; D-04 loss on V1; grep target `Failed to write column `; the F2 test flip; the DLQ message-header fact F3; the `Decimal` token casing) + the evidence file (D-13 trace table, D-03 research result incl. F1 outcome, red/green runs, gate outputs).

FORK-DELTA edit sites [VERIFIED: docs/FORK-DELTA.md]: the WRT-12 generic-catch row (the row beginning "`doWriteColValue`'s generic catch logs the column, the ClickHouse type, the Kafka type and the exception class, with no throwable and no stack trace, and rethrows the original exception unchanged"), Known costs item 6 (the sentence beginning "The rethrown exception of `doWriteColValue`'s generic catch keeps its message."), and a new row in the "Connector source" table for the new patch, with `Pinned by` = `WRT-13, WRT-14, WRT-15` and the test classes.

## Sources

### Primary (HIGH confidence)
- Source files read this session: `ClickHouseWriter.java` (L60-70, L196-262, L555-600, L600-900, L896-1180, L1180-1420, L1715-1780), `Utils.java`, `Processing.java` (L40-140), `ProxySinkTask.java`, `ChunkFlusher.java`, `ClickHouseSinkTask.java`, `ClickHouseHelperClient.java`, `Column.java`, `Type.java`, `QueryIdentifier.java`, `Data.java`, `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `verify-merge-invariants.sh` (head), `docs/FORK-DELTA.md`, `.planning/ROADMAP.md`, `REQUIREMENTS.md`, `18-BEHAVIOUR-LEDGER.md`, Phase 18 `deferred-items.md`, Phase 21 `21-CONTEXT.md` D-14 and `21-RESEARCH.md` Pitfall 3, and the test files `WriterLogMaskingUnitTest`, `ClickHouseWriterNullMessageUnitTest`, `CapturingClickHouseWriter`, `LogCapture`, `InMemoryDLQ`, `UtilsNullMessageClassificationUnitTest`, `WriterErrorPathLiveTest`, `ClickHouseSinkTaskWithSchemaTest` (L885-912).
- `javap -c` of `com.clickhouse.data.format.BinaryStreamUtils`, `com.clickhouse.data.ClickHouseChecker` (clickhouse-data 0.9.6), `com.clickhouse.client.api.Client` / `internal.HttpAPIClientHelper` / `ClickHouseException` / `ClientException` (client-v2 0.9.6), `org.apache.kafka.connect.errors.DataException`/`RetriableException` (connect-api 4.1.1).
- Executed experiments (scratch programs in `/tmp/r22-scratch`, not in the repo; the Docker container used was removed): thread/connection probe of the real `ClickHouseHelperClient`; V1/V2 `doInsertRawBinary` against a local HTTP stand-in and against `clickhouse/clickhouse-server:latest` 26.9.19.9; 23-kind survey through `doWriteColValue`; baseline `testUnit` run (21 passed); `spotlessApply` baseline in a throwaway copy (46 files); anchors mirror (`rows=59 bad=0`).
- Apache Kafka 4.1.1 source: https://raw.githubusercontent.com/apache/kafka/4.1.1/connect/runtime/src/main/java/org/apache/kafka/connect/runtime/errors/DeadLetterQueueReporter.java (header construction).

### Secondary (MEDIUM confidence)
- KIP-653 (Log4j2 migration of Kafka's logging): https://kafka-options-explorer.conduktor.io/kip/653/ - broker/tools side; Connect-specific rendering not verified.

### Tertiary (LOW confidence)
- None used for recommendations.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - nothing new; all existing and read from the tree.
- Architecture / design of the catch and the sanitized class: HIGH - every behavioural claim executed; the single design choice with a trade-off (`getMessage()` non-null) is explained.
- Pitfalls: HIGH for F1-F9 (reproduced); MEDIUM for A1/A2/A4 (assumed, listed).

**Research date:** 2026-10-19
**Valid until:** 2026-11-18 for code facts (stable until `ClickHouseWriter.java`, the client 0.9.6 pin or Connect 4.1.1 change); re-check the `latest` image date before trusting a red full-suite run.
