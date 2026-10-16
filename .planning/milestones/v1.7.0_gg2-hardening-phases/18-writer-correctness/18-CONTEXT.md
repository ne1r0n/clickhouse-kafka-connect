# Phase 18: Writer Correctness - Context

**Gathered:** 2026-10-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Four `ClickHouseWriter` defects are fixed without disturbing exactly-once:

- **JSONP-09:** Empty Debezium records (`op=t`, or a null `before`/`after` struct) insert no row on any of three routes: explicit `insertFormat=json` (metadata-free), upstream `bypassRowBinary=true` JSON, and RowBinary.
- **WRT-10:** A missing value for a non-nullable column without a default raises a `DataException` that names the column and its ClickHouse type. This includes a field inside `Array(Tuple(...))`.
- **WRT-11:** An exception with a null message that reaches the writer's error path surfaces as itself, not as an NPE.
- **WRT-12:** DateTime parse failures stop logging record data.

Requirements: JSONP-09, WRT-10, WRT-11, WRT-12.

This discussion widened the scope, within the same defect families, by:

- the same null-message NPE in `Utils.handleException` (D-05);
- the value leak in the generic `catch (Exception e)` log in `doWriteColValue` (D-09);
- a column name added to the DateTime `DataException` (D-08).

No change to `ProxySinkTask`, `Processing`, `Record`, `RangeContainer` or `KeeperStateProvider`.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
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
  - Phase gate: a cache-busted full suite on `latest` × V2, plus `verify-merge-invariants.sh 54e63648 HEAD`.

### Empty-CDC batches (JSONP-09)
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

### Null-message handling (WRT-11 + sibling)
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

### Missing non-nullable value (WRT-10)
- **D-10:** The message format, kept in a static constant beside `NESTED_NULL_MESSAGE_FORMAT`, is:
  `column '<name>' (<rendered type>): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT`
  - Render the type with the existing `renderColumnType`.
  - For a tuple child, `<name>` is the dotted path (e.g. `at.a`).
  - Throw `org.apache.kafka.connect.errors.DataException`.
- **D-11:** **Drop** the `LOGGER.error("Column {} is not nullable and no value is provided", name)` line before the throw. The exception carries the information, and callers already log it: the generic catch for tuple children, and `Utils.handleException`.
- Tests assert `DataException`, the column name, the rendered type, and that no record value appears in the message. Cover a flat column and a field inside `Array(Tuple(...))` (pattern: `NestedNullableCompositeWriteTest.assertNestedNullThrows`).

### Log masking (WRT-12 + `:854`)
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

### Change record for Phase 20 (all items)
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
- The plan split for JSONP-09 and WRT-12, and the plan order. WRT-10/11 (+ the D-05 Utils commit) stay one plan. Research suggests WRT-10/11 → WRT-12 → JSONP-09, or JSONP-09 first; the planner picks.
- Whether `:854` logs a sanitized stack, i.e. stack frames without the message. Only allowed if no message text can be rendered; the default is no stack.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` § "Phase 18: Writer Correctness": goal, the 4 success criteria, notes (separate commits, WRT-10/11 coupling, JSONP-09 helper rules), research flag (empty-body insert spike)
- `.planning/REQUIREMENTS.md`: JSONP-09, WRT-10, WRT-11, WRT-12; Out of Scope table (no `serializeJsonRow` refactor, no shared nested-null helper)
- `AGENTS.md`: high-risk files, exactly-once invariant, hot-path rules, test rules (read back + `assertEquals`, `InMemoryDBWriter`, no mocking `ClickHouseWriter` in state-machine tests)

### Milestone research (already detailed for these four items)
- `.planning/research/ARCHITECTURE.md` § "Empty-CDC skip (13-REVIEW WR-01)", "WR-03", "WR-04", "T-13-19", exactly-once invariant check, file/test map
- `.planning/research/PITFALLS.md` § Pitfall 1 (dedup token, not block hash), Pitfall 4 (skip layer and predicate), Pitfall 5 (classification code), Pitfall 6 (masking both arguments, sentinel test)
- `.planning/research/FEATURES.md` § F2, F3, F4, F6 (verified current behaviour, suggested text, adjacent leaks)
- `.planning/research/SUMMARY.md`: phase ordering rationale

### Origin findings (prior milestone)
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-REVIEW.md`: WR-01 (empty-CDC), WR-03 (message-less exception), WR-04 (null-check order)
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-SECURITY.md`: T-13-19
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`: ledger format for D-13; rows L-03, L-06, L-12
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md`

### Fork bookkeeping
- `docs/FORK-DELTA.md`: rows table; Known costs items 2 and 6 (L122-147)
- `.planning/scripts/fork-anchors.txt`: the `shouldSkipTableMetadata()` count of 8 must not change; add rows for new patches
- `.planning/scripts/verify-merge-invariants.sh`: phase gate `verify-merge-invariants.sh 54e63648 HEAD`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ClickHouseWriter.buildNestedNullMessage` / `NESTED_NULL_MESSAGE_FORMAT` (L554-566) and `renderColumnType` (L569+): the template for data-free, metadata-only messages (WRT-10, D-08).
- The existing metadata-free skip in `writeRecordsJson` (L1365-1376): the code and log line to move into the shared helper.
- Protected `doInsertRawBinaryV1/V2`, `doInsertJson`: override points for Docker-free writer tests (pattern: `MetadataFreeJsonInsertUnitTest`).
- `NestedNullableCompositeWriteTest.assertNestedNullThrows`: pattern for asserting nested-slot exceptions.
- `ExplicitJsonInsertPathLiveTest#metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes` (~L378-441): template for the live per-route empty-CDC tests; it already pins an all-tombstone batch on the explicit route.
- `DebeziumRecordConvertorTest`: `op=t` / null-struct fixtures.
- `ProcessingTest` + `InMemoryDBWriter`: exactly-once state assertions without Docker.
- `InMemoryDLQ` (test fixtures): DLQ capture for the `put()`-level pins.
- `src/test/resources/log4j.properties`: reload4j config (`com.clickhouse.kafka=DEBUG`). There is no log-capture helper yet (D-12 adds one).

### Established Patterns
- Writer messages reach Connect status, DLQ headers and JMX, so they must contain metadata only.
- `Utils.handleException` classifies by exception type, ClickHouse error code and root cause, not by message. The two message-dependent spots are `doInsertRawBinary`'s V1 code-string table and the `Utils` IOException timeout prefix.
- `Processing.doInsert` wraps every failure in `RuntimeException(queryId, e)`, so tests walk the cause chain.
- The V2 insert paths set `insert_deduplication_token` = `topic-partition-minOffset-maxOffset`, so retries are deduplicated by token.

### Integration Points
- `ClickHouseWriter.writeRecordsJson` (~L1353): hoist the skip after the tombstone check.
- `ClickHouseWriter.writeRecordsRowBinary` (~L1274-1290): skip before the column loop. CDC skips `validateDataSchema` (L1224, L1301).
- `ClickHouseWriter.doWriteCol` (~L1092-1094): WRT-10 throw site, reached from the TUPLE case for each tuple field.
- `ClickHouseWriter.doInsertRawBinary` catch (~L1175-1178): WRT-11.
- `ClickHouseWriter.doWriteDates` (L412; logs L461, L515; DataException ~L525): WRT-12.
- `ClickHouseWriter.doWriteColValue` generic catch (~L854): D-09.
- `Utils.handleException` IOException branch (`Utils.java:121-127`): D-05.

</code_context>

<specifics>
## Specific Ideas

- The WRT-10 text is fixed by D-10, including the actionable hint.
- Use a unique sentinel value (not a realistic date) in the log-capture tests, and check message, throwable message and cause messages.
- Treat the empty-body insert as an assumption to prove, not a given. If the spike fails, it is a user decision point.

</specifics>

<deferred>
## Deferred Ideas

- **The `:854` rethrown exception message can carry the value** into Connect status and the DLQ (e.g. `Invalid UUID string: <value>`). Fixing it means changing the rethrown type or message without changing classification. This phase adds a backlog entry (999.x) and a FORK-DELTA Known cost; the fix itself is not in this phase.
- Short-circuiting the insert for an all-skipped batch: only if the D-01 spike shows an empty body is rejected, and only with the user's approval.
- A JMX counter for skipped empty CDC records: rejected for now (D-04).

</deferred>

---

*Phase: 18-writer-correctness*
*Context gathered: 2026-10-12*
