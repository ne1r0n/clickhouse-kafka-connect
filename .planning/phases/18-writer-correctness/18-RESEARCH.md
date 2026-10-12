# Phase 18: Writer Correctness - Research

**Researched:** 2026-10-12
**Domain:** Kafka Connect sink writer (`ClickHouseWriter`, `Utils`) - empty-CDC skip, data-free exception and log text, null-message error path
**Confidence:** HIGH (every code claim was read in this session; the empty-body spike and the baseline were run live against ClickHouse 26.9.16.11 on both clients)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Carried forward (locked earlier; not re-opened)**
- The empty-CDC skip covers **all three routes**, RowBinary included (user scope decision 2026-10-10, REQUIREMENTS.md).
- The skip lives in **one shared private helper** inside `ClickHouseWriter`, called from `writeRecordsJson` (hoisted above the three-way branch, right after the tombstone check) and from `writeRecordsRowBinary`.
  - The helper must **never call `shouldSkipTableMetadata()`**. `fork-anchors.txt` pins that string at exactly 8 occurrences.
  - **Never filter records in `ProxySinkTask` or `Processing`.** That would change offset ranges, `BEFORE`/`AFTER` state, the dedup token and `reportInsertedOffsets`.
- The skip predicate is `SchemaType.DEBEZIUM_CDC` plus a null or empty `getJsonMap()`, evaluated **before** `cleanupExtraFields`. A schemaless record whose fields are all unknown still lands a defaults-only row.
  - On the metadata-free route the behaviour must stay byte-identical: same skip, same log line.
- The skip is a pure function of the record, so a retried batch is byte-identical.
- Upstream-identical lines are fixed in the fork. Each such patch gets a `docs/FORK-DELTA.md` row and a `.planning/scripts/fork-anchors.txt` row **in the same commit**.
- Every exception or log text that this phase adds or changes is built from `Column` metadata only: name, rendered type and Kafka field type. It never contains a record value, including through a cause or throwable message.
  - Follow `buildNestedNullMessage` / `NESTED_NULL_MESSAGE_FORMAT` and keep formats in static constants.
- The "null in non-nullable column" `RuntimeException` (value present but null, L-12/D-11) is a different branch. Leave it as is and do not unify it with WRT-10.
- Commit and plan rules:
  - Each fix lands as its own commit.
  - WRT-10 and WRT-11 share one plan.
  - Each skip or message change is recorded as it lands, for Phase 20.
- Process rules:
  - Existing tests are never deleted or weakened.
  - Gradle always runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`.
  - `spotlessApply` runs on touched files only.
  - Phase gate: a cache-busted full suite on `latest` x V2, plus `verify-merge-invariants.sh 54e63648 HEAD`.

**Empty-CDC batches (JSONP-09)**
- **D-01:** If every record in a batch is skipped, keep today's flow. The insert is opened and receives an **empty body**.
  - The research spike must prove that client V1 and V2 both accept a zero-row JSON insert and a zero-row RowBinary insert. A live test pins it.
  - **If the spike shows a client rejects an empty body, stop and ask the user** before adding any short-circuit in `doInsertJsonV1/V2` or `doInsertRawBinaryV1/V2`. Do not fold a short-circuit in silently.
- **D-02:** Log each skipped record at DEBUG. Move the existing metadata-free line into the shared helper, so all three routes log identically and the metadata-free text stays unchanged: `Skipping empty CDC record (no values) topic [..] partition [..] offset [..]`.
  - Add no batch-level count. Stay allocation-free in the per-record loop: no `Optional` and no lambda.
- **D-03:** Tests:
  - **Live, one test per route** (explicit `insertFormat=json`, `bypassRowBinary=true`, RowBinary with `debeziumCDCEnabled=true`):
    - a mixed batch (real, `op=t`, null-after, real) lands exactly the real rows, checked by read-back with `assertEquals`;
    - an all-empty batch completes with 0 rows and no exception.
  - **Docker-free:**
    - the same batch serialized twice gives byte-identical bodies, asserted on the bytes rather than row counts;
    - a schemaless record whose fields are all unknown still serializes a defaults-only row on the bypass route;
    - with an all-empty or mixed batch, exactly-once state ends `AFTER_PROCESSING` with the full offset range. Use the `ProcessingTest` / `InMemoryDBWriter` style, and don't mock `ClickHouseWriter`.
- **D-04:** Add **no JMX metric** for skipped records. The DEBUG log is enough.

**Null-message handling (WRT-11 + sibling)**
- **D-05:** Also fix the same NPE in `Utils.handleException`'s `IOException` branch (`rootCause.getMessage().indexOf(...)` at `Utils.java:122`) in a **separate commit**, in the same plan as WRT-10/11.
  - Treat a null message as "not a timeout", so the error falls through to the normal tolerance handling: DLQ when `errors.tolerance=all`, task failure when it is `none`.
  - `Utils.java` is already fork-modified. Add a FORK-DELTA row and an anchor row if the line is otherwise upstream-identical.
- **D-06:** In `ClickHouseWriter.doInsertRawBinary`'s `catch (Exception e)`, read `e.getMessage()` once into a local. A null message yields no match. Behaviour for a non-null message containing a known code with `retry==true` is unchanged: one table-mapping refresh and one retry.
- **D-07:** Tests:
  - **Docker-free:**
    - A writer subclass whose protected `doInsertRawBinaryV1`/`V2` hook throws `new RuntimeException()` propagates **that same instance**, with no NPE.
    - The positive twin: a message containing `"ClickHouseException: Code: 33"` triggers exactly one retry.
    - Unit cases for `Utils.handleException` with a null-message `IOException` and a null-message `SocketTimeoutException`, which is now retriable.
  - **`put()`-level pins** through `ClickHouseSinkTask.put()` that walk the cause chain (never assert `instanceof` on the top-level exception, because `Processing.doInsert` wraps it):
    - `errors.tolerance=none` stops the task with the original error visible in the chain;
    - `errors.tolerance=all` routes the batch to the DLQ (`InMemoryDLQ`);
    - a null-message `SocketTimeoutException` is classified retriable.

**Missing non-nullable value (WRT-10)**
- **D-10:** The message format, kept in a static constant beside `NESTED_NULL_MESSAGE_FORMAT`, is:
  `column '<name>' (<rendered type>): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT`
  - Render the type with the existing `renderColumnType`.
  - For a tuple child, `<name>` is the dotted path (e.g. `at.a`).
  - Throw `org.apache.kafka.connect.errors.DataException`.
- **D-11:** **Drop** the `LOGGER.error("Column {} is not nullable and no value is provided", name)` line before the throw. The exception carries the information, and callers already log it: the generic catch for tuple children, and `Utils.handleException`.
- Tests assert `DataException`, the column name, the rendered type, and that no record value appears in the message. Cover a flat column and a field inside `Array(Tuple(...))` (pattern: `NestedNullableCompositeWriteTest.assertNestedNullThrows`).

**Log masking (WRT-12 + `:854`)**
- **D-08:** Mask both DateTime ERROR logs in `doWriteDates`: L461 (DateTime) and L515 (DateTime64, which leaks twice through `e.getMessage()`).
  - Each log carries: column name, ClickHouse type, Kafka field type, exception **class** and value **length**.
  - It never carries the value, a prefix or suffix of it, or the exception's message.
  - Topic/partition/offset are **not** added: `doWriteDates` doesn't receive the record, and adding it would change an upstream signature.
  - Also add the column name and ClickHouse type (no value) to the `DataException` "(Potentially) Not implemented conversion from STRING to DateTime" (~L525). This is a data-free message change on the same upstream lines.
- **D-09:** Also fix the generic `catch (Exception e)` log in `doWriteColValue` (~`ClickHouseWriter.java:854`).
  - Log the column, ClickHouse type, Kafka type and exception class.
  - **Pass no throwable to the logger.** Its rendered message, e.g. `Invalid UUID string: <value>` or `NumberFormatException: For input string: ...`, would leak the value.
  - **Keep `throw e` unchanged**, so `Utils.handleException` sees the same root cause and retriable classification can't change.
  - The rethrown exception's message can still carry the value into Connect status and the DLQ. Record that as a **known residual** in FORK-DELTA Known costs and as a new backlog entry (999.x) in ROADMAP.md.
  - The `ClassCastException` catch at ~L842-853 stays as it is (its message is data-free).
- **D-12:** Build a **reusable log-capture test helper** in `src/test`. It attaches a reload4j appender to a named logger and exposes the captured events (rendered message and throwable chain messages), with a guaranteed detach.
  - Tests use a unique sentinel string as the field value.
  - They assert the sentinel is absent from every captured event: message, throwable message and cause messages.
  - They also assert the column name **is** present, so a negative-only test can't pass just because the log line was deleted.
  - Cover the DateTime log, the DateTime64 log and the `:854` catch, using a UUID column with an invalid string.

**Change record for Phase 20 (all items)**
- **D-13:** Create `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` in the format of Phase 13's ledger. Add one row per operator-visible change **in the same commit as the fix**: what changed, route or setting, old vs new behaviour, and how an operator notices or finds it (e.g. a query for key-0 phantom rows, a grep for an old log text).
  - Leave `VERSION` and `CHANGELOG.md` untouched; Phase 20 writes CHANGELOG and ROLLOUT from the ledger.
  - Expected rows:
    - phantom rows stop on the bypass and RowBinary routes;
    - the root-cause type behind `put()` changes from `RuntimeException` to `DataException` for a missing non-nullable value, with the new text;
    - null-message exceptions now surface as themselves, and a null-message timeout becomes retriable;
    - a null-message `IOException` in `Utils` follows tolerance handling instead of an NPE;
    - the DateTime and DateTime64 log texts change;
    - the DateTime `DataException` text changes;
    - the `:854` log text changes and no longer has a stack trace;
    - the ERROR line "Column X is not nullable and no value is provided" disappears (log-based alerting).
- FORK-DELTA updates:
  - close Known costs item 2 (empty-CDC skip only on metadata-free) and update the metadata-free row's "(L-06)" wording;
  - remove the T-13-19 half of Known costs item 6;
  - add rows for each new fork patch in upstream regions (`writeRecordsRowBinary` skip, `doWriteDates` logs and exception, `:854` log, `Utils` IOException guard);
  - add the `:854` rethrow residual.

### Claude's Discretion
- Name and location of the shared empty-CDC helper and of the log-capture helper class.
- Exact wording of the masked log lines and of the DateTime `DataException`, within D-08/D-09 content rules.
- How the RowBinary-route live test builds `DEBEZIUM_CDC` records (connector config vs direct writer).
- The plan split for JSONP-09 and WRT-12, and the plan order. WRT-10/11 (+ the D-05 Utils commit) stay one plan. Research suggests WRT-10/11 -> WRT-12 -> JSONP-09, or JSONP-09 first; the planner picks.
- Whether `:854` logs a sanitized stack, i.e. stack frames without the message. Only allowed if no message text can be rendered; the default is no stack.

### Deferred Ideas (OUT OF SCOPE)
- **The `:854` rethrown exception message can carry the value** into Connect status and the DLQ (e.g. `Invalid UUID string: <value>`). Fixing it means changing the rethrown type or message without changing classification. This phase adds a backlog entry (999.x) and a FORK-DELTA Known cost; the fix itself is not in this phase.
- Short-circuiting the insert for an all-skipped batch: only if the D-01 spike shows an empty body is rejected, and only with the user's approval.
- A JMX counter for skipped empty CDC records: rejected for now (D-04).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| JSONP-09 | An empty Debezium record (`op=t`, or null `before` and `after`) inserts no row on all three routes; offsets and exactly-once state still advance; the skip is decided inside the writer so a retried batch is byte-identical; one live test per route | Spike proves both clients accept a zero-row body on all four write shapes (Spike section). Baseline proves phantom rows today on the bypass route and on RowBinary with a DEFAULT table (Baseline section). Helper design, hoist points and the anchor-count trap are in Architecture Patterns and Pitfalls 1-6. |
| WRT-10 | A missing value for a non-nullable column (incl. inside `Array(Tuple(...))`) raises a `DataException` naming the column and its type | Throw site is `doWriteCol` else branch (L1091-1095). Reachable for flat columns on the CDC route and with `bypassSchemaValidation=true`, and for tuple children always. Skeleton in Code Examples; tests pattern in Validation notes. |
| WRT-11 | `ClickHouseWriter`'s error path handles a null-message exception without an NPE, classification unchanged; Docker-free test | NPE site is L1177 (`e.getMessage().contains(code)` runs before the null check). Reproduced live on client V1 (baseline). The V2 client wraps the failure, which is why V2 never showed it. Sibling NPE at `Utils.java:123`. |
| WRT-12 | A DateTime parse failure logs column, ClickHouse and Kafka types, exception class and value length, never the raw value; log-capture test; FORK-DELTA row | Two leaking ERROR logs at L461 and L515, plus the value-bearing generic catch at L855. Verified that `ZonedDateTime.parse` and `UUID.fromString` messages embed the value. reload4j 1.2.22 is on the test compile classpath, so the capture helper needs no new dependency. |
</phase_requirements>

## Summary

All four defects are small, local edits to `ClickHouseWriter.java` plus one line in `Utils.java`, and every one of them is in a high-risk file. None of them needs a new dependency. The two risks that deserve planner attention are not the edits themselves. They are (a) the bookkeeping that the merge-invariant gate enforces per commit (anchor counts, in particular the `shouldSkipTableMetadata()` count of 8, which a stray comment can break) and (b) test design: three of the specified tests cannot be written the obvious way (see Pitfalls 4, 5, 8, 9 and Open Questions 1-2).

The research flag is closed: **an insert whose rows are all skipped (zero-byte body) is accepted by both clients on all four write shapes** (explicit JSON, bypass JSON, RowBinary, RowBinaryWithDefaults), each reporting `Written Bytes: [0], Written Rows: [0]` on ClickHouse 26.9.16.11. D-01 needs no stop-and-ask and no short-circuit. A live baseline run also confirmed the defect shape on each route today, including two facts the CONTEXT does not state: on RowBinary the phantom row appears only when the table has DEFAULT/Nullable columns (a plain non-null table fails the whole batch instead), and on client V1 that failure surfaces as the WRT-11 NPE.

**Primary recommendation:** Land three plans in the order 18-01 WRT-11 -> D-05 -> WRT-10 (one plan, three commits, WRT-11 first so the null-message path is fixed before WRT-10 changes what reaches it), 18-02 WRT-12 + D-09 + the log-capture helper, 18-03 JSONP-09 with the live route tests and the doc closures. In every commit, put the code, its tests, the `fork-anchors.txt` row, the FORK-DELTA row and the behaviour-ledger row together, and never write the literal `shouldSkipTableMetadata()` anywhere in `ClickHouseWriter.java` except where it already is.

## Spike result (research flag: empty-body insert)

Method [VERIFIED: live run this session, scratch test deleted afterwards, `git status` shows only the two pre-existing entries]: a throwaway `ClickHouseBase` live test started a real `ClickHouseWriter` against a `latest` container and called `doInsert` with two `Record`s of `SchemaType.DEBEZIUM_CDC` whose `SinkRecord.value()` is `null`. Both `writeRecordsJson` and `writeRecordsRowBinary` skip such records at their `record.getSinkRecord().value() != null` check, so the stream gets zero bytes, exactly what an all-skipped batch will produce. Run with `CLIENT_VERSION=V2` and `CLIENT_VERSION=V1`.

| Route | Table | V2 | V1 |
|-------|-------|----|----|
| explicit `insertFormat=json` | plain | accepted, `Written Rows: [0]` | accepted, `Written Rows: [0]` |
| `bypassRowBinary=true` JSON | plain | accepted, `Written Rows: [0]` | accepted, `Written Rows: [0]` |
| RowBinary | plain | accepted, `Written Rows: [0]` | accepted, `Written Rows: [0]` |
| RowBinaryWithDefaults | table with a `DEFAULT` column | accepted, `Written Rows: [0]` | accepted, `Written Rows: [0]` |

Each case also read back `count() = 0` and `put`-level `doInsert` returned normally. Server: `Connected to ClickHouse version: 26.9.16.11`. The client in use was told apart by the writer's own log text (V1 RowBinary prints `topic :` with a space, V2 prints `topic:`).

Limit of the evidence [ASSUMED] A3: null-valued records stand in for "skipped rows". They produce the same empty stream, but the real skip path is only exercised by the tests this phase adds (D-03), which therefore must include the all-empty batch per route on both clients.

## Baseline (what each route does today with an `op=t` / null-after batch)

Batch: `c` (id 1), `t` (null/null), `u` (null after), `d` (id 2, before). Table columns `id, ts, amount, name, _version, is_deleted`. [VERIFIED: live run this session, scratch test deleted]

| Route | Table | Result today (V2 and V1 alike unless stated) |
|-------|-------|-----------------------------------------------|
| explicit `insertFormat=json` | plain | `put` OK, 2 rows (ids 1, 2): the existing skip works |
| `bypassRowBinary=true` | plain | `put` OK, **4 rows: two phantom `id=0 / ts=1970-01-01 / _version=0` rows** plus the 2 real rows. The real rows' `ts` reads `9999-12-31 23:59:59` (known L-03: epoch milliseconds read as seconds) |
| RowBinary | all columns `DEFAULT` | `put` OK, **4 rows: two phantom rows** plus the 2 real rows |
| RowBinary | plain non-null, no defaults | **`put` throws, 0 rows.** V2 chain ends `ClickHouseException: Insert failed (Attempt: 1/4 ...)` caused by `RuntimeException: <null>`. V1 chain ends `NullPointerException: Cannot invoke "String.contains(CharSequence)" because the return value ...` (the WRT-11 NPE replaced the real error) |

Consequences for the plan:
- A RowBinary live test that shows "no phantom row" needs a table whose columns are `DEFAULT`/`Nullable`. On a plain table the pre-fix failure is a failed batch, not a phantom row; that table shape is a useful second case (mixed batch lands the real rows instead of failing).
- Bypass-route read-back must not assert `ts` (L-03). Assert `id`, `name`, `amount`, `_version`, `is_deleted`, or use a table without a `DateTime64` column.
- WRT-11 is observable end-to-end only on client V1, and only until WRT-10 lands, because after WRT-10 the same failure has a message.

## Architectural Responsibility Map

This is a connector, not a web stack, so the tiers are the connector's own layers (see `.claude/CLAUDE.md` Architecture).

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Empty-CDC skip (JSONP-09) | DB writer (`ClickHouseWriter` serialization loops) | Record conversion (`DebeziumRecordConvertor` already yields the empty map) | The skip must be a pure function of the record, inside the writer, so offsets, state and the dedup token (`topic-partition-minOffset-maxOffset`) are computed from the unfiltered batch |
| Offsets and exactly-once state | Processing + StateProvider | - | Untouched. They see every record because the skip happens after them |
| Missing non-nullable value message (WRT-10) | DB writer (`doWriteCol`) | Exception classification (`Utils.handleException`) | Message built from `Column` metadata where the failure is detected; classification is by type and root cause, not by message |
| Null-message guard (WRT-11) | DB writer error path (`doInsertRawBinary` catch) | `Utils.handleException` (D-05) | Two independent message dereferences in the same failure family |
| DateTime log masking (WRT-12, D-09) | DB writer (`doWriteDates`, `doWriteColValue`) | Test support (`LogCapture`) | Logs are emitted where the parse fails |
| Accepting a zero-row insert | ClickHouse server + both Java clients | - | Proven by the spike; the writer needs no change |
| Change record / drift guard | Docs and scripts (`FORK-DELTA.md`, `fork-anchors.txt`, ledger) | `verify-merge-invariants.sh` | Per-commit invariant, see Pitfall 2 |

## Standard Stack

No new libraries. Everything needed is already on the classpath.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit Jupiter | 5.11.3 [CITED: .claude/CLAUDE.md] | Unit and live tests | Existing convention |
| Mockito | 5.21.0 [CITED: .claude/CLAUDE.md] | Mock `ClickHouseHelperClient` to inject `chc` in Docker-free writer tests | `ClickHouseWriterTest` already injects `chc` reflectively (`WRITER_HELPER_CLIENT_FIELD = "chc"`, `ClickHouseWriterTest.java:694`) |
| reload4j (via `slf4j-reload4j:2.0.17`) | 1.2.22 [VERIFIED: `./gradlew dependencies --configuration testCompileClasspath` this session shows `org.slf4j:slf4j-reload4j:2.0.17 -> ch.qos.reload4j:reload4j:1.2.22`] | `AppenderSkeleton` for the log-capture helper (D-12) | Already the test logging backend; `src/test/resources/log4j.properties` sets `log4j.logger.com.clickhouse.kafka=DEBUG` |
| Testcontainers | 1.21.4 [CITED: .claude/CLAUDE.md] | Live ClickHouse via `ClickHouseBase` | Existing harness; shared container per JVM fork |

Observed tool versions: Gradle 9.6.1 (from Gradle's own output; `.claude/CLAUDE.md` still says 7.4.2), ClickHouse server 26.9.16.11 for `latest` [VERIFIED: spike log]. Test JVM runs with `file.encoding=windows-1252` on purpose; any new reader of ClickHouse output must specify UTF-8 [CITED: STATE.md].

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Reflection to reach private `writeRecordsJson` / `writeRecordsRowBinary` in Docker-free byte tests | Widen the methods to package-private | Widening edits an upstream-identical line (`writeRecordsRowBinary` is upstream's), needing an anchor and FORK-DELTA row for no behaviour. Prefer reflection (pattern: `MetadataFreeJsonInsertUnitTest` reads `csc` reflectively) |

**Installation:** none.

## Package Legitimacy Audit

No external package is added or changed in this phase (verified by the dependency listing above: the only library touched by the new test helper, reload4j, is already a transitive compile dependency). The `package-legitimacy` seam was not run because there is nothing to install.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Kafka SinkRecord batch (one topic-partition)
        |
        v
Record.convert()  --- op=t / op=null / null before|after --> Record(DEBEZIUM_CDC, jsonMap = {})   (DebeziumRecordConvertor L79-96)
        |
        v
Processing.doLogic  (UNCHANGED: range, BEFORE/AFTER, dedup token built from ALL records, empties included)
        |
        v
ClickHouseWriter.doInsert -> doInsertBatch (L232-251)
        |
        +--- SCHEMA/DEBEZIUM_CDC, (bypassRowBinary || metadata-free) ---> doInsertJson -> V1 | V2 ---> writeRecordsJson(...)
        |                                                                                              |
        +--- SCHEMA/DEBEZIUM_CDC, otherwise --------------------------> doInsertRawBinary -> V1 | V2 -> writeRecordsRowBinary(...)
        |                                                                                              |
        |                                      +--------------- per record -------------------------- +
        |                                      | value()==null (tombstone)?  -> skip (existing)
        |                                      | NEW shared helper: DEBEZIUM_CDC and jsonMap null/empty? -> DEBUG log, continue
        |                                      | else serialize row (JSON: MetadataFreeJsonWriter | Jackson; RowBinary: per-column doWriteCol)
        |                                      +------------------------------------------------------
        v
HTTP insert (zero bytes if every record skipped: accepted, Written Rows 0)  --- insert_deduplication_token = topic-partition-min-max
        |
   failure path:  writer throws -> doInsertRawBinary catch (WRT-11 guard) -> Processing.doInsert wraps RuntimeException(queryId, e)
                 -> ProxySinkTask.put catch -> Utils.handleException (D-05 guard) -> RetriableException | DLQ (tolerance=all) | RuntimeException (tolerance=none)
```

### Recommended change map (current line numbers in `ClickHouseWriter.java`; re-read before editing, they will shift commit to commit)

| Item | Where | Change |
|------|-------|--------|
| JSONP-09 | `writeRecordsJson` L1353-1405: after `if (record.getSinkRecord().value() != null) {` (L1357) | Call the helper, `continue` on true. Delete the inner `if (record.getJsonMap() == null \|\| ...isEmpty())` block at L1366-1374 (keep the `metadataFree && DEBEZIUM_CDC` branch itself for `cdcRow`). `metadataFree` stays in use, so the `shouldSkipTableMetadata()` count is unchanged |
| JSONP-09 | `writeRecordsRowBinary` L1274-1290: after `if (record.getSinkRecord().value() != null) {` (L1277) | Same helper call, before the column loop |
| WRT-10 | `doWriteCol` else branch L1091-1095 | Replace `LOGGER.error(...)` + `throw new RuntimeException()` with `throw new DataException(<formatted constant>)` |
| WRT-11 | `doInsertRawBinary` catch L1175-1191 | Read `e.getMessage()` once; null message yields no match |
| WRT-12 | `doWriteDates` L461, L515, L523-526 | Mask both logs; add column and type to the `DataException` text |
| D-09 | `doWriteColValue` catch L854-857 | Replace the throwable-bearing log; keep `throw e` |
| D-05 | `Utils.handleException` L121-127 | `msg != null &&` guard |

### Pattern 1: one shared, allocation-free skip helper
**What:** a private method taking the `Record`, returning `boolean`, doing the type check first so non-CDC records cost one enum comparison, logging at DEBUG only when it skips.
**When to use:** both serialization loops; nothing else.
**Example** (skeleton; name and static-ness are discretion):
```java
// Source: derived from the existing block at ClickHouseWriter.java L1365-1374 [VERIFIED: Read this session]
private static final String SKIP_EMPTY_CDC_LOG_FORMAT =
        "Skipping empty CDC record (no values) topic [{}] partition [{}] offset [{}]";

private static boolean skipEmptyCdcRecord(Record record) {
    if (record.getSchemaType() != SchemaType.DEBEZIUM_CDC) {
        return false;
    }
    Map<String, Data> values = record.getJsonMap();
    if (values != null && !values.isEmpty()) {
        return false;
    }
    LOGGER.debug(SKIP_EMPTY_CDC_LOG_FORMAT,
            record.getTopic(),
            record.getRecordOffsetContainer().getPartition(),
            record.getRecordOffsetContainer().getOffset());
    return true;
}
```
Keep the log text byte-identical to today's: `Skipping empty CDC record (no values) topic [{}] partition [{}] offset [{}]` [VERIFIED: ClickHouseWriter.java L1369-1372]. Do not write the literal `shouldSkipTableMetadata()` in this method's comments or Javadoc (Pitfall 1).

### Pattern 2: data-free messages from `Column` metadata
The existing templates [VERIFIED: ClickHouseWriter.java L553-567]:
```java
private static final String NESTED_NULL_MESSAGE_FORMAT = "column '%s' (%s) %s: null in non-nullable slot";
...
private String buildNestedNullMessage(Column container, String slotKind) {
    return String.format(NESTED_NULL_MESSAGE_FORMAT, container.getName(), renderColumnType(container), slotKind);
}
```
`renderColumnType` renders the connector `Type` enum name, not a ClickHouse type string (L569-587: `default: rendered = col.getType().name();`, wrapped in `Nullable(...)` when nullable), so an `Int32` column renders as `INT32`, a `DateTime64` as `DateTime64`, an `Array(Int32)` as `Array(INT32)`, a tuple as `Tuple(...)`. D-10 locks this renderer; the ledger row should show an example so operators are not surprised.

### Pattern 3: reflective seam for Docker-free writer tests
`MetadataFreeJsonInsertUnitTest` already sets the private `csc` reflectively (`WRITER_SINK_CONFIG_FIELD = "csc"`, L53-63) and `ClickHouseWriterTest` sets `chc` (L694-705). Reuse both. New reflection needs: `writeRecordsJson(List<Record>, Table, OutputStream)` [L1353] and `writeRecordsRowBinary(List<Record>, Table, OutputStream, boolean)` [L1274] via `getDeclaredMethod(...).setAccessible(true)`. Both write to the passed stream only; `statistics` is non-null when the writer is built as `new ClickHouseWriter(new SinkTaskStatistics(null, 0))`.

### Anti-Patterns to Avoid
- **Filtering in `ProxySinkTask`/`Processing`/`Record`:** changes min/max offsets, `BEFORE/AFTER` and the dedup token; a fully filtered partition would never advance. Locked out by CONTEXT.
- **Predicate on the cleaned map:** `cleanupExtraFields` empties a schemaless record with only unknown fields on the bypass route, which must still land a defaults-only row.
- **A skip that reads config, time or retry state:** it must be a pure function of the record so a replay builds the identical batch.
- **Passing the exception to the logger or as a log argument:** reload4j renders `getMessage()` and the stack text, and the trailing-`Throwable` rule of SLF4J turns a last argument that is a `Throwable` into the logged throwable.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Capturing log output in tests | A custom `Logger` stub or stdout redirect | `org.apache.log4j.AppenderSkeleton` subclass attached to `Logger.getLogger(ClickHouseWriter.class)` | Same backend the code logs through; reads `LoggingEvent.getRenderedMessage()`, `getThrowableInformation()`, `getThrowableStrRep()` |
| Message-less-exception tests | Mocking `ClickHouseWriter` | Subclass overriding protected `doInsertRawBinaryV1/V2` plus a mocked `ClickHouseHelperClient` injected as `chc` | AGENTS.md forbids mocking the writer in state-machine tests; the protected hooks exist for exactly this |
| Exactly-once state assertions | A new state fake | `InMemoryState` + `Processing` as in `ProcessingTest` | Existing pattern; `InMemoryDBWriter` for the writer side |
| Row read-back in live tests | `countRows` alone | Typed SQL read-back with `assertEquals` (snapshot each `GenericRecord` inside the iteration) | STATE.md: client-v2 `GenericRecord` is a reused view; `ExplicitJsonInsertPathLiveTest.allRows` is the template |

**Key insight:** every piece of test infrastructure this phase needs exists except the log-capture helper and an accessor on `InMemoryDLQ`; adding production seams would cost more rebase churn than reflection in tests.

## Common Pitfalls

### Pitfall 1: a comment or Javadoc that spells `shouldSkipTableMetadata()` breaks the anchor
**What goes wrong:** `verify-merge-invariants.sh` counts lines with `grep -cF -e "shouldSkipTableMetadata()"` and expects exactly 8 [VERIFIED: fork-anchors.txt L20 `...ClickHouseWriter.java	shouldSkipTableMetadata()	8	JSONP-01`; `grep -cF` on the file returned 8 this session]. A prose mention such as "never calls shouldSkipTableMetadata()" in the new helper counts as a ninth.
**How to avoid:** write "the metadata-free predicate" in comments; reference it in prose only. Check `grep -cF "shouldSkipTableMetadata()" ClickHouseWriter.java` equals 8 before each commit. The same applies to `buildRowBinaryUnsupportedTypeMessage` (pinned at 5, L23) and the `if (value.getObject() == null && col.isNullable()) {` line (pinned at 1).
**Warning signs:** script prints `ANCHOR VIOLATION: <sha> ... shouldSkipTableMetadata() expected 8 got 9 (JSONP-01)`.

### Pitfall 2: the gate checks every commit against that commit's own anchor file
**What goes wrong:** the script loops over every commit in `PRE_TAG..HEAD` that carries `fork-anchors.txt` and checks each commit's blobs against that commit's copy [VERIFIED: verify-merge-invariants.sh L212-247]. A code commit without its anchor row, or an anchor row in a later commit, fails the gate at the earlier commit. Baseline run: `anchor_commits=225 ... result=PASS`, wall time 3m13s, so run it once per plan, not per commit.
**How to avoid:** code, anchor row, FORK-DELTA row and ledger row in one commit (already locked). An anchor pattern must be a fixed string whose `grep -cF` count in the file is stable and measured in that commit. Add rows for `Utils.java` too (the file has no anchor rows yet).

### Pitfall 3: the byte change across an upgrade is real, and the dedup is by token
`QueryIdentifier.getDeduplicationToken()` is `topic-partition-minOffset-maxOffset` and every V2 insert path sets it as `insert_deduplication_token` [CITED: .planning/research/PITFALLS.md Pitfall 3; CONTEXT "Established Patterns"]. A connector that crashed after `BEFORE_PROCESSING` under the old jar and retries under the new one sends a block without the phantom rows. If the first attempt landed, the token drops the retry and the phantom rows stay; if not, the new block lands without them. This is not a within-process determinism problem (the skip is a pure function), but Phase 20's rollout note must say it. Put it in the ledger row for JSONP-09; state it as token-based and do not copy gg1's "rows can be written twice" wording.

### Pitfall 4: the RowBinary live test passes or fails for the wrong reason on a plain table
See Baseline. On an all-non-null, no-default table an empty CDC record fails the batch today; a "mixed batch lands the real rows" test is red for that reason, not because of a phantom row. Use a DEFAULT-column table (`id Int32 DEFAULT 0, ... `) to reproduce the phantom rows, and optionally a plain table for the failing-batch case. In RowBinary the CDC path skips `validateDataSchema` (L1224, L1301), so the table needs `_version UInt64` and `is_deleted UInt8` columns as in the existing explicit-route template.

### Pitfall 5: bypass-route assertions on `ts`
The bypass route keeps Jackson defaults, so real rows' `DateTime64` reads `9999-12-31 23:59:59` (observed; FORK-DELTA Known costs item 4, L-03). Do not assert `ts` on that route and do not "fix" it here.

### Pitfall 6: client V2 wraps, V1 does not
V2: the writer exception is inside the client's `ClickHouseException: Insert failed (...)`; V1: it reaches `doInsertRawBinary`'s catch unwrapped [VERIFIED: baseline chains above]. Tests must walk the cause chain to the deepest cause (or search the chain), never `instanceof` the top exception (CONTEXT). The `Utils.handleException` first branch uses `getRootCause(e, true)`, which stops at the first server error in the chain; on V2 the new `DataException` sits below that wrapper, so do not assert it is "the root cause" as seen by `handleException`. Classification is by type and root cause, and `DataException` vs `RuntimeException` does not change it (the wrapper `RuntimeException(queryId, e)` is what `e instanceof DataException` sees at L106).

### Pitfall 7: a null-message `SocketTimeoutException` was never an NPE in `Utils`
`SocketTimeoutException` extends `IOException`, and `Utils.handleException` tests `rootCause instanceof SocketTimeoutException` (L112) before the `IOException` branch (L121), so it was already retriable there. The "now retriable" change is end to end: before WRT-11 it died in `doInsertRawBinary`'s catch. The D-07 unit case for a null-message `SocketTimeoutException` in `Utils` therefore passes before the change; it is a pin, not a red-green test. The red-green case is a null-message plain `IOException` (and the writer-level `new RuntimeException()`).

### Pitfall 8: DateTime64 branch dereferences `csc` inside the try
`doWriteDates` DateTime64 reads `csc.getDateTimeFormats()` inside the `try` (L484). On a writer built without a config (the usual Docker-free shape) that throws a `NullPointerException`, which the `catch (Exception e)` swallows, and a "masked log" test then passes while proving nothing about the `DateTimeParseException` message. Set `csc` reflectively (`new ClickHouseSinkConfig(minimalProps())`) and assert the logged exception class is `DateTimeParseException`. Guard against a vacuous pass by asserting in the test that the raw parse exception for the sentinel does contain it (`Text 'SENTINEL...' could not be parsed at index 0` [VERIFIED: ran `ZonedDateTime.parse` this session]).

### Pitfall 9: the sentinel for the `:854` test must be chosen so the unmasked code leaks it
`UUID.fromString("SENTINEL-zq9")` gives `IllegalArgumentException: Invalid UUID string: SENTINEL-zq9`; a string with four dashes instead gives `NumberFormatException: Error at index 0 in: "zzzzzzzz"`, which leaks a segment only [VERIFIED: ran on JDK 21 this session; the test JVM is the JDK 17 toolchain, so see A2]. Use a sentinel with no dash and under 37 characters, and assert once in the test that `UUID.fromString(sentinel)` really embeds it. UUID column plus a STRING value reaches the path through `doWritePrimitive` (`case UUID: ... UUID.fromString((String) value)`, L991-992) inside `doWriteColValue`'s try.

### Pitfall 10: computing "value length" inside the catch can itself throw
`(String) value.getObject()` can throw `ClassCastException` inside a `catch (Exception e)` that exists to handle failures. Use `String.valueOf(value.getObject()).length()`; `value.getObject()` is non-null at that point (guard at L414).

### Pitfall 11: the `:854` masking is partial end to end
The rethrown exception is logged again with its throwable by `doInsertRawBinary` (`LOGGER.error("Error inserting records", e)` at L1172 and L1188 [VERIFIED: Read]) and by the Connect framework when the task fails. After D-09 the value can still reach the log through those lines (for example `Invalid UUID string: <value>`). The D-09 log-capture test captures `doWriteColValue` called directly, so it will pass; the FORK-DELTA residual and the 999.x backlog entry must say the leak path includes those two ERROR lines, not only "Connect status and the DLQ". Do not widen the code fix (CONTEXT defers it).

### Pitfall 12: FORK-DELTA has stale statements beyond the ones CONTEXT lists
CONTEXT lists Known costs item 2, the item 6 T-13-19 half and the metadata-free row's "(L-06)". Also stale after this phase: **item 3** ("13-REVIEW WR-03: ... message-less `RuntimeException`", L134) and the **WR-04 clause of item 5** ("13-REVIEW WR-04 (a null `getMessage()` dereference in the `doInsertRawBinary` catch block)", L145) [VERIFIED: grep of docs/FORK-DELTA.md this session]. Close both in the commits that fix them.

### Pitfall 13: hot-path regression
`writeRecordsJson`/`writeRecordsRowBinary` run per record. The helper checks the schema type first and allocates nothing on the non-skip path (AGENTS.md hot-path rule). Do not use `Optional`, lambdas or `String.format` there. The DEBUG call allocates only when a record is actually skipped.

### Pitfall 14: JMX counters include skipped records
`Processing.doInsert` calls `statistics.recordTopicStats(records.size(), ...)` for the whole batch [VERIFIED: Processing.java L68-69], so skipped CDC records are still counted as processed. D-04 adds no metric; mention the counting in the ledger so operators comparing row counts to message counts understand the gap.

## Code Examples

### WRT-10: missing non-nullable value
```java
// Source: pattern of ClickHouseWriter.java L553-567 [VERIFIED: Read this session]; text locked by CONTEXT D-10
private static final String MISSING_NON_NULLABLE_MESSAGE_FORMAT =
        "column '%s' (%s): no value provided for a non-nullable column without a default; "
                + "add the field, make the column Nullable, or give it a DEFAULT";

// doWriteCol, else branch (today L1091-1095):
} else {
    throw new DataException(
            String.format(MISSING_NON_NULLABLE_MESSAGE_FORMAT, name, renderColumnType(col)));
}
```
The branch is reached when `!fieldExists && !col.hasDefault() && !col.isNullable() && col.getType() != Type.VARIANT`. For a tuple child `name` is `col.getName()`, which is the dotted DESCRIBE path (`at.a`), because `Table.updateParent` adds the DESCRIBE sub-column `Column` objects as `tupleFields` [VERIFIED: Table.java L194, Column.java L232-236]. Delete the `LOGGER.error(...)` line per D-11. Do not touch the sibling `RuntimeException("An attempt to write null into not nullable column '%s'")` at L1056 and L1071 (L-12, pinned by `NestedNullableCompositeWriteTest` L375).

### WRT-11: null-safe lookup
```java
// Source: ClickHouseWriter.java L1175-1178 today [VERIFIED: Read]; behaviour for non-null messages unchanged
} catch (Exception e) {
    // Note: this part will be removed once V1 is deprecated
    String failureMessage = e.getMessage();
    Optional<String> updateTableException = failureMessage == null
            ? Optional.empty()
            : UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE.keySet().stream()
                    .filter(failureMessage::contains).findFirst();
    if (updateTableException.isPresent() && retry) {
        ... // unchanged
```
The map is `Map.of("ClickHouseException: Code: 33", 33, "ClickHouseException: Code: 131", 131)` [VERIFIED: L66]. Error path only; the `Optional` here is not in the per-record loop.

### D-05: `Utils.handleException`
```java
// Source: Utils.java L121-127 [VERIFIED: Read]
} else if (rootCause instanceof IOException) {
    final String msg = rootCause.getMessage();
    if (msg != null && (msg.indexOf(CLICKHOUSE_CLIENT_ERROR_READ_TIMEOUT_MSG) == 0
            || msg.indexOf(CLICKHOUSE_CLIENT_ERROR_WRITE_TIMEOUT_MSG) == 0)) {
```
Line 123 is byte-identical to upstream v1.7.0 line 124 [VERIFIED: `git show v1.7.0:.../Utils.java`], so it needs the anchor and FORK-DELTA rows.

### WRT-12 / D-09: masked logs
```java
// One helper, two call sites (DRY). Constants per AGENTS.md "static string constants".
private static final String DATETIME_PARSE_FAILURE_LOG_FORMAT =
        "Error parsing date time string for column `{}` (ClickHouse type {}, Kafka type {}): "
                + "exception {}, value length {}";

private void logDateTimeParseFailure(String columnName, Type type, Data value, Exception e) {
    LOGGER.error(DATETIME_PARSE_FAILURE_LOG_FORMAT, columnName, type, value.getFieldType(),
            e.getClass().getName(), String.valueOf(value.getObject()).length());
}
// DateTime  catch (L460):  logDateTimeParseFailure(columnName, type, value, e); unsupported = true;
// DateTime64 catch (L514): the same call. e itself is never an argument.

// doWriteColValue generic catch (L854): no throwable, keep the rethrow
} catch (Exception e) {
    LOGGER.error(WRITE_VALUE_FAILURE_LOG_FORMAT, col.getName(), columnType,
            value == null ? "<value null>" : value.getFieldType(), e.getClass().getName());
    throw e;
}
```
The unsupported-conversion text (`String.format("(Potentially) Not implemented conversion from %s to %s", value.getFieldType(), type)`, L524) gets the column name appended from a constant; it is shared by Date, Date32, DateTime and DateTime64, so all four gain the column (acceptable, data-free). Keep the existing prefix so any grep on it still matches.

### Log-capture helper (D-12) - shape
```java
// src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java (location is discretion)
public final class LogCapture implements AutoCloseable {
    private final org.apache.log4j.Logger logger;
    private final org.apache.log4j.Level previousLevel;
    private final List<org.apache.log4j.spi.LoggingEvent> events = new CopyOnWriteArrayList<>();
    private final org.apache.log4j.AppenderSkeleton appender = new org.apache.log4j.AppenderSkeleton() {
        @Override protected void append(org.apache.log4j.spi.LoggingEvent e) { events.add(e); }
        @Override public void close() {}
        @Override public boolean requiresLayout() { return false; }
    };
    public static LogCapture attach(Class<?> loggerClass) { ... logger.addAppender(appender); }
    /** Rendered message, getThrowableStrRep() lines, and every getMessage() down the cause chain. */
    public List<String> everyRenderedText() { ... }
    @Override public void close() { logger.removeAppender(appender); logger.setLevel(previousLevel); }
}
```
Use `try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) { ... }` so detach is guaranteed. `getThrowableInformation()` is null when no throwable was logged; handle that. Scanning `getThrowableStrRep()` covers the stack's `Exception: message` lines as well as the cause chain walk.

### Docker-free null-message writer test - shape
```java
// Subclass overrides the protected V1/V2 hooks (ClickHouseWriter.java L1215, L1292, both protected) and getTable/updateMapping as needed.
// chc = Mockito.mock(ClickHouseHelperClient.class), injected reflectively into the private field "chc";
// when(chc.isUseClientV2()).thenReturn(false /* V1 hook */ or true /* V2 hook */).
// csc injected reflectively (needed by updateMapping()'s metadata-free predicate on the retry path).
RuntimeException boom = new RuntimeException();               // getMessage() == null
RuntimeException thrown = assertThrows(RuntimeException.class, () -> writer.doInsertRawBinary(records, table, queryId, false, true));
assertSame(boom, thrown);                                     // surfaces as itself, no NPE
```
Positive twin: hook throws `new RuntimeException("ClickHouseException: Code: 33")`; the retry path calls `urgentTableUpdate` -> `updateMapping(db)` -> `getTable(...)`; override `updateMapping` (public) and `getTable` (protected) to return a stub `Table`, count hook invocations, expect exactly 2 hook calls (the original plus one retry with `retry=false`) and the second exception rethrown.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Empty-CDC skip only inside the metadata-free branch | One shared helper on all three routes | This phase | Closes FORK-DELTA Known costs item 2 |
| Message-less `RuntimeException` for a missing non-nullable value | `DataException` naming column and rendered type | This phase | Closes Known costs item 3 |
| Upstream-identical raw-value DateTime logs | Metadata-only logs (class, length) | This phase | Closes T-13-19 |

**Deprecated/outdated:** the `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE` string matching is already marked `@Deprecated` upstream ("will be removed once V1 is deprecated"); do not extend it.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `-PspotlessFiles=<regex>` limits `spotlessApply` to touched files (the Spotless Gradle plugin's documented property). Not run this session. Spotless here is only `importOrder()` + `removeUnusedImports()` (`build.gradle.kts` L495-501), so a full `spotlessApply` is low-risk anyway; check `git diff --stat` for unrelated files afterwards | Pitfalls / process | A wider reformat than "touched files only" produces rebase churn in a fork that tracks upstream |
| A2 | JDK 17's `UUID.fromString` message form for a dash-free string equals JDK 21's (`Invalid UUID string: <value>`); only JDK 21 was run locally | Pitfall 9 | The sentinel test could pass vacuously on JDK 17; the in-test self-check (`assertTrue(thrown.getMessage().contains(sentinel))` on a direct `UUID.fromString`) catches it |
| A3 | A null-valued `SinkRecord` yields the same zero-byte stream as an all-skipped CDC batch | Spike | Low: the writer loops show identical behaviour, and D-03 adds the real all-empty-batch live tests per route |
| A4 | Setting `ProxySinkTask`'s private final `processing`/`dbWriter` by reflection works on the JDK 17 toolchain (non-static final instance field of a normal class) | Open Question 1 | If it does not, fall back to the Docker-free `Processing` + `Utils.handleException` pairing |
| A5 | `Mockito.mock(ClickHouseHelperClient.class)` returns `false` for `isUseClientV2()` and an empty list for `extractTablesMapping(...)` by default | Code Examples | Test setup needs explicit stubbing; trivial |

## Open Questions

1. **How is a null-message exception injected into a `ClickHouseSinkTask.put()`-level pin (D-07)?**
   - What we know: `ProxySinkTask` is `final` and builds its own `ClickHouseWriter` and `Processing` in its constructor (`ProxySinkTask.java` L40-51); `ClickHouseSinkTask.setErrorReporter` is the only seam. After WRT-10 no natural live path produces a null-message exception. `Utils.handleException` is called from `ProxySinkTask.put` (L100) and `ChunkFlusher` (L72).
   - What's unclear: the least-invasive injection.
   - Recommendation: a live `ClickHouseBase` test that starts a real task (the writer needs a reachable server to start) and then swaps `ProxySinkTask.processing` by reflection for `new Processing(new InMemoryState(), throwingDbWriter, reporter, csc, statistics)` where the `DBWriter` throws `new RuntimeException()` / `new SocketTimeoutException()` (A4). Assert on the cause chain, DLQ size via an `InMemoryDLQ` accessor, and `RetriableException` for the timeout. If reflection is rejected, cover classification Docker-free by driving `Processing.doLogic` with a throwing `DBWriter` and passing the thrown exception to `Utils.handleException` (this reproduces `ProxySinkTask.put`'s catch exactly). `InMemoryDLQ` keeps its `Throwable` private with no accessor (`InMemoryDLQ.java`), so add an additive getter (test-only file).

2. **Should the 999.x backlog entry and the FORK-DELTA residual name the two extra log lines (Pitfall 11)?**
   - What we know: `doInsertRawBinary` logs the same throwable at L1172/L1188.
   - What's unclear: whether the user wants that stated now. It does not change scope; it corrects the claim.
   - Recommendation: state it in the residual and the backlog entry; no code change.

3. **Closing FORK-DELTA Known costs items 3 and 5 (Pitfall 12).**
   - Recommendation: include both in the doc updates of the WRT-10 and WRT-11 commits, even though CONTEXT's D-13 list does not name them.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | Live tests, full-suite gate | yes | Docker Engine 29.8.2; `colima list` shows `vz` Running | none; use `colima start --profile vz` (STATE/memory: bare `colima` hits the wrong profile) |
| ClickHouse `latest` container | Live tests | yes (pulled by Testcontainers) | 26.9.16.11 | none |
| Gradle wrapper | All tests | yes | 9.6.1 | - |
| JDK 17 toolchain | Test JVM | yes (configured toolchain) | local `java` CLI is 21.0.12 | - |
| `yq` | `CLICKHOUSE_VERSION` matrix lookup (AGENTS.md) | no | - | not needed: this milestone tests `latest` x V2 only |
| `gh` | CI log reading | no (deliberate, STATE.md) | - | unauthenticated REST API |
| Bash invariant gate | Phase gate | yes | baseline PASS, 3m13s | - |

**Missing dependencies with no fallback:** none.
**Environment quirks:** run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Select the client with `CLIENT_VERSION=V1|V2` (default V2). Docker-free classes must not extend `ClickHouseBase`, whose `@Tag("clickhouse")` would drop them out of `testUnit`. Commands: `./gradlew testUnit --tests "*Name"` (Docker-free), `./gradlew test --tests "*Name"` (live), add `--rerun` for the cache-busted gate run.

## Security Domain

`security_enforcement` is enabled (ASVS level 1, block on high).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | - |
| V3 Session Management | no | - |
| V4 Access Control | no | - |
| V5 Input Validation | partly | Record values are parsed by `ZonedDateTime.parse`, `UUID.fromString` and numeric constructors; failures must not echo the value |
| V6 Cryptography | no | - |
| V7 Error Handling and Logging | yes | Metadata-only exception and log text; no throwable-with-message logging for record data |
| V8 Data Protection | yes | Record values must not leave through logs, Connect status, DLQ headers or JMX |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Record value in an ERROR log (T-13-19: `LOGGER.error("...{}", value.getObject())` and `e.getMessage()`) | Information disclosure | Log class, length and column only; sentinel log-capture test over message, throwable message and causes |
| Record value in an exception message that reaches Connect status or DLQ headers | Information disclosure | Build messages from `Column` metadata via constants (WRT-10, D-08); the `:854` rethrow is a recorded residual |
| Value reaching the log through a second, throwable-bearing line | Information disclosure | Known: L1172/L1188 log the rethrown exception (Pitfall 11); record, do not widen |
| Log forging through a column name | Tampering | Names come from server `DESCRIBE` metadata, not from records; low risk, no new control |
| Bad batch retried forever because an error is mis-classified | Denial of service | Classification unchanged (type, code, root cause); D-05 and WRT-11 only remove NPEs |

## Project Constraints (from CLAUDE.md)

- Work through a GSD command before editing (`.claude/CLAUDE.md` "GSD Workflow Enforcement"). Version, `CHANGELOG.md` stay untouched this phase (CONTEXT D-13), although AGENTS.md's generic PR checklist says to bump them.
- AGENTS.md: do not commit without being asked. This repo's GSD execution commits per plan (see `git log`: `fix(13-04): ...`); the locked process rule "each fix lands as its own commit" governs, under the user's GSD flow.
- High-risk files (`ClickHouseWriter.java`, `Processing.java`, `KeeperStateProvider.java`, `ClickHouseSinkConfig.java`, `RangeContainer.java`): extra care; do not break the BEFORE/AFTER invariant. `Processing`, `ProxySinkTask`, `Record`, `RangeContainer`, `KeeperStateProvider` are out of scope here.
- Never log secrets or record data; error messages must be actionable with context; SLF4J levels (TRACE per record, DEBUG per batch, INFO lifecycle).
- Extract string constants into static finals; DRY; useful comments only (invariants, not a restatement of the code); try-with-resources for `AutoCloseable` (the `LogCapture` helper).
- Hot paths (`ClickHouseWriter`, `Processing`): no per-record allocations, boxing or `Optional`.
- Tests: read back and `assertEquals`; never delete or weaken existing tests; descriptive names; JUnit 5 + Mockito; read `ClickHouseTestHelpers.java` before writing tests; new functionality needs feature-test coverage; do not mock `ClickHouseWriter` in state-machine tests.
- Do not upgrade dependencies without asking; keep changes additive for rebasability.
- Java 17; Spotless handles import order and unused imports only.

## Sources

### Primary (HIGH confidence)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - Read lines 1-300, 395-640, 780-910, 1000-1800 this session
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` - Read lines 30-199
- `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/DebeziumRecordConvertor.java` L62-97, `.../Record.java`, `.../EmptyRecordConvertor.java`, `.../sink/db/mapping/Table.java` L100-215, `.../Column.java` L150-240
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`, `.../processing/Processing.java` L40-100, `ClickHouseSinkTask.java`
- `.planning/scripts/fork-anchors.txt` L12-23, `.planning/scripts/verify-merge-invariants.sh` L1-260 (baseline run: `result=PASS`)
- Existing tests read: `ExplicitJsonInsertPathLiveTest`, `MetadataFreeJsonInsertUnitTest`, `NestedNullableCompositeWriteTest`, `ProcessingTest` L1-115, `ClickHouseBase`, `ClickHouseWriterTest` L680-720, `InMemoryDLQ`, `FailureTest`
- Live runs this session (scratch tests deleted): empty-body spike (V1, V2) and CDC baseline (V1, V2) against ClickHouse 26.9.16.11
- `docs/FORK-DELTA.md` (rows L20-60, Known costs L122-147), `.planning/phases/18-writer-correctness/18-CONTEXT.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`
- `git show v1.7.0:` copies of `ClickHouseWriter.java` and `Utils.java` for the upstream-identical checks

### Secondary (MEDIUM confidence)
- `.planning/research/PITFALLS.md` (Pitfalls 3-6), `ARCHITECTURE.md` (empty-CDC, WR-03, WR-04, T-13-19), `FEATURES.md` - milestone research, consistent with the code as read
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` - ledger format

### Tertiary (LOW confidence)
- none

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new dependency; reload4j classpath presence verified
- Architecture: HIGH - hoist points, error path and anchors read in source; empty-body acceptance and baseline observed live on both clients
- Pitfalls: HIGH for items 1-2, 4-12 (read or run this session); MEDIUM for Pitfall 3's rollout wording (inferred from the token mechanism, not re-measured here) and Pitfall 9 on JDK 17 (A2)

**Research date:** 2026-10-12
**Valid until:** 2026-11-11 (code is stable; re-check the `latest` server version and the 8-count anchor if `ClickHouseWriter.java` changes before planning)
