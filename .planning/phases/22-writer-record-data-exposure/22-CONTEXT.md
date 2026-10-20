# Phase 22: Writer Record-Data Exposure - Context

**Gathered:** 2026-10-19
**Status:** Ready for planning

<domain>
## Phase Boundary

`ClickHouseWriter.doWriteColValue`'s generic `catch (Exception e)` (~`ClickHouseWriter.java:870`) stops rethrowing value-carrying exceptions as they are. A value-conversion failure (an invalid UUID string, an unparseable number) instead surfaces as a value-free `DataException`. It names the column, the ClickHouse type and the exception class in three places: the Connect task status, the DLQ exception headers (message and stack trace), and `doInsertRawBinary`'s `Error inserting records` ERROR logs. Two readers downstream must behave exactly as before: `Utils.handleException`'s retry classification and `doInsertRawBinary`'s `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE` match.

Requirements: HARN-08, WRT-13, WRT-14, WRT-15. This closes backlog 999.3, FORK-DELTA Known costs item 6 and the T-18-07 accepted threat.

Out of this phase:
- Any other writer branch's message, except as D-03 lets it pass through unchanged.
- The `ClassCastException` branch (~L858). It stays exactly as it is.
- The DateTime logs from Phase 18 WRT-12.
- Other Mockito usage in the suite.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
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
  - `spotlessApply` runs on touched files only. Every touched file must be spotless-clean on its own (Phase 21 D-14: no tree-wide reformat, baseline-equal gate). For files already in the spotless baseline-red set, this is read as D-16.
  - Phase gate: cache-busted `testUnit` (`--rerun`), a full suite on `latest` × V2, and the anchors invariant.

### What gets replaced (WRT-13)
- **D-01:** The generic catch **rethrows the original unchanged** in either of these cases:
  - **any** throwable in its cause chain is an `IOException`, so retry classification still sees `SocketTimeoutException`, `SocketException`, `UnknownHostException` and the read/write-timeout `IOException` messages as before. This includes the `new RuntimeException(ioException)` wrappers that the Map/Tuple lambdas build at ~L713, ~L742 and ~L786.
  - it is a `ConnectException` (including `DataException`). These are the fork's value-free texts: WRT-08 nested null, WRT-10 missing non-nullable, unsupported RowBinary type, and an inner column's own D-02 replacement.

  Everything else is **replaced** (D-02/D-05). — **Reversibility:** costly — the thrown type and text are operator-visible once Phase 24 publishes, and operators may build greps or alerts on them.
- **D-02:** The replacement is an `org.apache.kafka.connect.errors.DataException`. This matches the `ClassCastException` branch above it. Because a `DataException` passes through outer catches (D-01), a failure inside a Tuple/Map/Array child keeps the **innermost** column's name. The WRT-14 pin (D-11) must prove the outcome is unchanged.
- **D-03:** An exception whose class is **exactly** `RuntimeException` (`e.getClass() == RuntimeException.class`) is also rethrown unchanged. Those are the connector's own throw sites, with safe and useful text:
  - the upstream JSON hint `Writing JSON in binary is not supported yet. Use input_format_binary_read_json_as_string=1 …` (~L849);
  - `Unsupported field type … for column type [JSON]` (~L845);
  - the nested L-12 `An attempt to write null into not nullable column '%s'` (~L1073/L1088, reached through Tuple/Map children).

  No throw sites are edited, with the single exception of D-14. JDK and client conversion failures are typed subclasses (`IllegalArgumentException`, `NumberFormatException`, `ArithmeticException`, `DateTimeException`, `NullPointerException`, …) and are replaced.
  - **Research must verify** that no client-library (`clickhouse-java` `BinaryStreamUtils`, `ClickHouseValues`, …) or JDK code reachable from `doWriteColValue` throws an exact `RuntimeException` whose message carries a value. **If it finds one, stop and ask the user** before choosing another rule.
- **D-04:** Today, a record value that itself contains `ClickHouseException: Code: 33` (or `131`) makes the rethrown message match `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE`, which triggers one table-mapping refresh and retry. After the change it can't. **This loss is accepted and intended**, because it is record content steering control flow, which is the leak itself.
  - Read the roadmap's "must neither start nor stop matching" rule as follows. Metadata-built messages never start matching. Real server and client failures (which pass through per D-01/D-03, or never reach this catch) never stop matching.
  - Record the loss in the ledger and FORK-DELTA, and pin it with one Docker-free test.

### Cause chain & frames
- **D-05:** The replacement `DataException` carries a **sanitized copy** of the original as its cause. That is a small fork class (a `RuntimeException` subclass) that:
  - keeps no original message;
  - copies the original's stack frames (`setStackTrace(original.getStackTrace())`);
  - sanitizes the original's own causes recursively in the same way, with a cycle/depth guard;
  - drops suppressed exceptions.

  Connect status, DLQ stack traces and the `Error inserting records` log then still show the frames (e.g. `at java.util.UUID.fromString …` down to the writer line) with no message text. Under classification, `getRootCause` lands on the sanitized object, which takes the same non-retriable path as the original `IllegalArgumentException`. D-11 proves that.
- **D-06:** The sanitized cause overrides `toString()` so that its stack-trace line reads `<original class name> (message withheld: may contain record data)`, e.g. `Caused by: java.lang.IllegalArgumentException (message withheld: may contain record data)`. The note is a fixed static constant. `getMessage()` returns no original text: it is null or the same fixed note. The research/planner picks which, consistent with how `Throwable.printStackTrace` and Connect's DLQ reporter render it.

### Message wording
- **D-07:** The message follows the `ClassCastException` precedent and shares its `Failed to write column ` prefix, kept in a static constant. Target shape (wording may be polished within these fields):
  `Failed to write column `<name>`: Kafka value of type <Kafka field type> (value class: <value class name>) could not be written as ClickHouse type <Type enum>: <original exception class name>`
  Use `"null"` for a null `Data` or a null object, as the CCE branch does. — **Reversibility:** costly — operator-visible grep target once published.
- **D-08:** **No value length.** This message travels further than a log line (Connect status, and a DLQ topic other consumers read), so it carries metadata only. The Phase 18 DateTime logs keep their `value length` as they are.
- **D-09:** Render the ClickHouse type as the **`Type` enum** token (`UUID`, `INT32`, `DECIMAL`), the same token the CCE branch and the `Error writing value of` log print. Don't use `renderColumnType`.

### Test evidence
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

### Post-research decisions (user, 2026-10-19, answering 22-RESEARCH F1/F3/F6)
- **D-14 (F1, the D-03 stop-and-ask):** `Column.convertEnumValues` (`Column.java` ~L524-529) throws an exact `RuntimeException("No content in enum <value>")` that carries the record value, so D-03's pass-through would leak it. Production-built columns can't reach it (`Column.extractColumn` always sets `enumValues`); a hand-built `Column` can. **Edit that one throw site** so its message is the fixed text `No content in enum` with no value. This is the only throw-site edit in the phase, and D-03 stays as written for everything else.
  - It lands in the WRT-13 commit, with a ledger row and FORK-DELTA coverage (a fork patch in `Column.java`, an upstream-region file).
  - A Docker-free test with a hand-built Enum8 column and a sentinel value asserts that the sentinel is absent and the fixed text is present.
  - The existing `Column.java` anchors keep passing. — **Reversibility:** cheap. The message is unreachable in production.
- **D-15 (F3, the SC2 DLQ "message" header):** read SC2/WRT-13's "DLQ exception headers (message and stack trace)" as follows:
  - the column name, the `Type` token and the original exception class name appear in the `__connect.errors.exception.stacktrace` header;
  - the `__connect.errors.exception.message` header stays `Processing.doInsert`'s queryId wrapper text, as before this phase;
  - the sentinel is absent from **both** headers.

  `Processing` is not touched. The D-12 DLQ test pins all three facts, so the limitation is pinned rather than hidden. The ledger, the verification and the SC2 evidence state this reading explicitly.
- **D-16 (F6, spotless on baseline-red files):** for touched files already in the 46-file spotless baseline-red set (`ClickHouseWriter.java`, `ClickHouseSinkTaskWithSchemaTest.java`, `Column.java`), "spotless-clean on its own" means **no new violations**:
  - don't reorder existing imports;
  - put any new import where `spotlessApply` would sort it;
  - each such file's post-edit `spotlessApply` diff has the same +/- line counts as its baseline (e.g. 19 for `ClickHouseWriter.java`, 55 for `ClickHouseSinkTaskWithSchemaTest.java`), measured on a throwaway `git archive` copy.

  Every other touched or new file must have an empty `spotlessApply` diff.

### Claude's Discretion
- The name and package of the sanitized-cause class and of the helper-client fake (test-side: beside `CapturingClickHouseWriter` or nested in the test).
- Whether the replacement logic lives in a small private helper in `ClickHouseWriter` (recommended, to keep the catch body short) and how the cause-chain IO scan is written. The catch runs only on failure, not on the per-record hot path, so either a plain loop or a stream is acceptable; the scan needs a cycle guard.
- The exact wording polish of D-07 within its fields, and whether `getMessage()` of the sanitized cause is null or the fixed note (D-06).
- The plan split, within the fixed order HARN-08 → WRT-14 pin → WRT-13 + WRT-15.
- The evidence file layout. Precedent: the Phase 17/20/21 evidence files with the branch trace (D-13), the D-03 research result, the red-before/green-after runs and the gate output.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` §"Phase 22: Writer Record-Data Exposure" — goal, success criteria, the ordering note and the design tension (D-09 rethrow vs value-free chain; the two downstream readers).
- `.planning/ROADMAP.md` §"Phase 999.3" — the backlog entry to mark delivered (WRT-15).
- `.planning/REQUIREMENTS.md` — HARN-08, WRT-13, WRT-14, WRT-15 (exact wording).

### Prior decisions this phase builds on
- `.planning/milestones/v1.7.0_gg2-hardening-phases/18-writer-correctness/18-CONTEXT.md` — D-07 (`put()`-level pin style, cause-chain walking), D-09 (why the rethrow was kept), D-12 (sentinel + positive-presence test pattern), D-13 (behaviour ledger), and the content rule.
- `.planning/milestones/v1.7.0_gg2-hardening-phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` — the ledger format to copy.
- `.planning/milestones/v1.7.0_gg2-hardening-phases/18-writer-correctness/18-SECURITY.md` — T-18-07 / R-18-02, the accepted threat this phase closes.
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-CONTEXT.md` — D-01/D-02 (anchors gate and exemption list, unchanged invocation), D-14 (spotless baseline-equal rule).

### Fork bookkeeping
- `docs/FORK-DELTA.md` — Known costs item 6 (~L158) and the WRT-12 patch-table row (~L41), both edited in the WRT-13 commit.
- `.planning/scripts/fork-anchors.txt` — the existing `WRITE_VALUE_FAILURE_LOG_FORMAT 2 WRT-12` row must keep passing; add a row for the new constant(s).
- `.planning/scripts/verify-merge-invariants.sh` and `.planning/scripts/fork-anchor-exemptions.txt` — the gate run at every commit.
- `AGENTS.md` — `ClickHouseWriter.java` is high-risk; testing discipline; no deleting or weakening tests.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java`: the Phase 18 reload4j appender helper (captured message plus throwable chain texts, guaranteed detach). Use it for every log assertion.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java`: `SENTINEL`, `assertNoSentinel`, `assertRawJdkExceptionEmbedsSentinel`, and the UUID-column writer setup. The flip target is `uuidWriteFailureRethrowsOriginalWithValueInMessage` (~L251).
- `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java`: `ScriptedInsertWriter` (overrides `doInsertRawBinaryV1/V2`, `updateMapping`, `getTable`) is the hook pattern for driving `doInsertRawBinary` Docker-free. HARN-08 edits this file.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/CapturingClickHouseWriter.java`: the no-Mockito writer-subclass pattern, with reflection on `csc` and serializer access via `writeRecordsRowBinary`.
- `src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java`: the live `put()` pins (tolerance none/all, `InMemoryDLQ`, retriable `SocketTimeoutException`) to extend for D-12.
- `src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java`: the `handleException` outcome-assertion style for D-11.
- `src/test/java/com/clickhouse/kafka/connect/sink/dlq/InMemoryDLQ.java`: captures the reported exception for DLQ assertions.

### Established Patterns
- The `ClassCastException` branch (`ClickHouseWriter.java` ~L858-869): a value-free `DataException` naming the column, the Kafka type, the value class and the `Type`. This is the D-07 message precedent.
- `Utils.getRootCause(e, true)`: walks to the first server exception, otherwise the deepest cause. `handleException` checks `instanceof DataException` on the **top-level** `e` only, and that `e` is normally `Processing.doInsert`'s `RuntimeException(queryId, …)` wrapper.
- `doInsertRawBinary` (~L1169-1210): the `ServerException` branch matches by code, and the generic branch matches the message against `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE` (read once into a local, null-safe since WRT-11). Both log `Error inserting records` **with** the throwable, so a value-free chain makes both logs value-free automatically.
- Map/Tuple lambdas wrap a nested `IOException` as `new RuntimeException(e)` (~L713, ~L742, ~L786). That's why D-01 scans the whole chain.

### Integration Points
- `ClickHouseWriter.doWriteColValue` generic `catch (Exception e)` (~L870-874): the single production change site, plus new static constants near L564-570.
- `Processing.doInsert` (~L70-72, ~L94-96) and the buffered delivery strategies wrap writer failures before `Utils.handleException`. D-11 pins each wrapping shape.
- Connect's DLQ reporter (Connect API/runtime 4.1.1) builds the `exception.message`/`exception.stacktrace` headers from the reported exception. D-12 renders it the same way.

</code_context>

<specifics>
## Specific Ideas

- The withheld-message line should read like `Caused by: java.lang.IllegalArgumentException (message withheld: may contain record data)`. Operators see the real class first and know the missing text is deliberate.
- Operators grep `Failed to write column ` for value failures (both the CCE branch and the new branch), and `Error writing value of` for the catch's log line. The ledger states this for ROLL-03.

</specifics>

<deferred>
## Deferred Ideas

None. The discussion stayed within the phase scope.

</deferred>

---

*Phase: 22-writer-record-data-exposure*
*Context gathered: 2026-10-19*
