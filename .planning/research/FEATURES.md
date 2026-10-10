# Feature Research

**Domain:** Follow-up hardening of a forked ClickHouse Kafka Connect sink (milestone `v1.7.0_gg2-hardening`). The "features" are the expected correct behaviour of eight carried-over follow-ups, written so each becomes one testable requirement.
**Researched:** 2026-10-10
**Confidence:** HIGH for behaviours I probed live or read in code; MEDIUM where marked. Live probes ran against `clickhouse/clickhouse-server:latest` = ClickHouse **26.9.14.10** (the same image the suite targets) through `clickhouse-local` and the HTTP interface. Code line numbers are at `sync/v1.7.0` HEAD `9ff1affc`.

Scope guard: existing validated features are not re-researched. Everything below is a delta on the published `v1.7.0_gg1` tree. Tests pin the target stack only: ClickHouse `latest` x client V2. No source file was modified; this file is the only artifact.

## Overview: one row per follow-up

| ID | Follow-up | Category | Complexity | Hard dependency |
|----|-----------|----------|------------|-----------------|
| F1a | 999.1 `Nullable(Enum8/Enum16)` keeps `Nullable` | Table stakes | LOW-MED (one-line root cause, wide test/doc fan-out) | none; gates F7 IN-01..05 |
| F1b | 999.1 `Nullable(Tuple(...))` keeps `Nullable` | Table stakes (milestone goal) with a fallback | HIGH | F1a first; must not touch byte-identical `Table.java` |
| F2 | 13-REVIEW WR-01 empty-CDC skip on `bypassRowBinary=true` JSON route | Table stakes | LOW | none |
| F3 | 13-REVIEW WR-03 `doWriteCol` names the column | Table stakes | LOW | none; F4 shares the null-message class |
| F4 | 13-REVIEW WR-04 null check before `getMessage().contains` | Table stakes | LOW | none |
| F5 | 14-REVIEW WR-01 response-leg LZ4 assertion | Table stakes (test debt) | LOW-MED | none |
| F6 | T-13-19 stop logging the raw DateTime string | Table stakes (security hygiene) | LOW | none; its log-capture helper is reusable by F7 IN-04 |
| F7 | 15-02 review IN-01..IN-10 | Mixed: fix 5, close-with-reason up to 5 | LOW each, MED as a bundle | F1 decides IN-01..05 |
| F8a | Early publication of `505e4981` | Table stakes (first phase) | LOW, but approval-gated | none; must precede all code |
| F8b | `v1.7.0_gg2` release notes / rollout note | Table stakes | LOW-MED | F1..F6 behaviour final |

## Feature Landscape

### Table Stakes (the milestone is incomplete without these)

---

#### F1a. 999.1, Enum: `Nullable(Enum8|Enum16)` keeps its nullability

**Verified current behaviour (HIGH).** `Column.extractColumn` strips `Nullable(` and recurses with `isNull=true` (`Column.java:274`). The `Enum` branch (`Column.java:202-211`) builds the column without `.isNullable(isNull)`, so `isNullable()==false`. In `doWriteCol` (`ClickHouseWriter.java:~1030-1100`) no null-marker byte is written for the slot. ClickHouse then reads the first payload byte as the null flag. Replayed against 26.9.14.10 with `clickhouse-local --input-format RowBinary --structure "e Nullable(Enum8('a'=1,'b'=2))"`:

| Bytes the connector writes today | Server stores |
|---|---|
| `01` (value `'a'`) | `NULL` (the value byte was consumed as the null flag) |
| `02` (value `'b'`) | `NULL` |
| `00 02` | `'b'` (only because the code is 0, which no real value uses first) |
| Enum16 code 1000 = `E8 03` | low byte `E8` read as null flag, the `03` byte leaks into the next column: `UNKNOWN_ELEMENT_OF_ENUM` or silent shift |

So the damage is silent NULLs for Enum8 and stream corruption for Enum16. A null Enum8 entry in `Array`/`Map` slots currently throws `DataException` instead (13-REVIEW WR-05, ledger L-13), which makes legal schemas fail.

**Expected behaviour after the fix (testable acceptance criteria).**
1. `Column.extractColumn("c", "Nullable(Enum8('a' = 1, 'b' = 2))", ...)` returns `Type.Enum8` with `isNullable()==true`; same for `Enum16`; a plain `Enum8(...)` stays `isNullable()==false`. This is a pure unit test, Docker-free (`ColumnTest` already has Enum cases at `ColumnTest.java:176`).
2. `Array(Nullable(Enum8))` element column and the `Map(K, Nullable(Enum8))` value column report `isNullable()==true` (same one-line cause; the Map value arrives through `Table.updateParent` MAP case but is built by the same `extractColumn`).
3. Wire format, top level: present value is `00 <code>` (Enum16: `00 <2 bytes>`), null is a single `01` with no payload. Under `RowBinaryWithDefaults` the leading default-flag byte precedes it exactly as for other nullable scalars (the existing `doWriteCol` defaults branch already writes it once `isNullable()` is true).
4. Live round trip with typed read-back, in the style of `NestedNullRowBinaryLiveTest`: `Nullable(Enum8)` values `'a'`, `'b'`, null; `Nullable(Enum16)` including a code >= 256 (the case that corrupts today); `Array(Nullable(Enum8))` `['a', NULL]`; `Map(String, Nullable(Enum8))` `{a:'a', b:NULL}` reads back `{'a':'a','b':NULL}` (today it reads `{'a':NULL,'b':NULL}`). Next column in the same row must be correct (proves no shift).
5. Unchanged guards: a null element in `Array(Enum8)` (non-nullable) still throws the nested-null `DataException` (L-10). A `DEFAULT`-carrying `Nullable(Enum8)` column behaves like other nullable-with-default scalars.
6. `LowCardinality(Nullable(Enum8))` is **not a case to support**: ClickHouse rejects it on `latest` (`ILLEGAL_TYPE_OF_ARGUMENT: DataTypeLowCardinality is supported only for numbers, strings, Date or DateTime`, probed, and `allow_suspicious_low_cardinality_types=1` does not change it). Do not write a requirement or test for it.
7. The metadata-free and `bypassRowBinary=true` JSON routes are untouched (no DESCRIBE; the JSON route lets the server parse `null`).

**Blast radius (the real cost).** The one-line change reaches every reader of `isNullable()` for Enum: `renderColumnType` (`ClickHouseWriter.java:~586`, error text now says `Nullable(Enum8)`), `Utils.logDiffBetweenNewAndOldTable` comparison (`Utils.java:295`), the nested-null guards (`:685, :714, :721`). Pin tests that encode the defect must be **flipped with a rationale comment, not deleted** (AGENTS.md: never delete or weaken tests; precedent: the D-09 re-pins in Phase 13):
- `NestedNullableCompositeWriteTest.java:264-268` asserts the Tuple premise "extractColumn drops Tuple nullability" (F1b).
- L-13 has no dedicated test ("follows from the loop checks pinned in L-10"), so add the Enum pin as a new positive test.
- The L-21 WARN and its tests become partly or fully obsolete: `ClickHouseHelperClient.java:490-525` (`NULLABLE_ENUM_OR_TUPLE` regex, `warnOnceIfNullableEnumOrTuple`, calls at `:579` and `:650`), `ClickHouseHelperClientForkUnitTest#nullableEnumOrTupleTypesAreFlagged` and `#warnOnceDedupesPerColumnAndRawType`, live `ClickHouseHelperClientForkLiveTest#describeTableWarnsOnceForNullableEnumOrTupleColumns`. Enum must leave the regex (or the whole WARN goes if F1b also lands). The anchor row `static boolean hasNullableEnumOrTuple(` in `.planning/scripts/fork-anchors.txt:57` must move in the same commit as the code (FORK-DELTA "for the next syncer" rule).

**Docs that must change in the same phase.** FORK-DELTA: Known costs item 1 and the WARN row (`FORK-DELTA.md:43, :124`), the `Column` row (`:44`, new patch: Enum nullability), the new patch itself recorded because it is upstream-identical code fixed in the fork (the defect exists in upstream `v1.7.0`, so an upstream PR is possible but optional). ROLLOUT-v1.7.0_gg1 item 4 and its self-check query (`ROLLOUT:30-43, :167-183`) describe the limitation as current; the gg2 note supersedes it.

**Complexity: LOW-MEDIUM.** Code change is one line; the cost is the live matrix, the WARN/anchor/doc fan-out and the flipped pins.

---

#### F1b. 999.1, Tuple: `Nullable(Tuple(...))` keeps its nullability

**Verified current behaviour (HIGH).**
- `Nullable(Tuple(...))` is **legal on `latest`**: `enable_nullable_tuple_type = 1`, tier Production (probed). Older servers needed an experimental setting, which is irrelevant to a `latest`-only suite but means the marker path is reachable only when the server accepts the type.
- The Tuple branch (`Column.java:232-236`) ignores `isNull`. So the tuple is written without its marker (`ClickHouseWriter.java` TUPLE case `:731-770`).
- **Why this is harder than Enum (the key finding).** `DESCRIBE ... describe_include_subcolumns=1` for `t Nullable(Tuple(a Int32, b String))` returns sub-columns `t.a Nullable(Int32)` and `t.b Nullable(String)` even though the elements are not nullable inside the tuple. The connector builds tuple fields from those sub-column rows (`Table.java: parent.getTupleFields().add(child)`), so every child reports `isNullable()==true` and writes its own marker byte. The wire format for `Nullable(Tuple(a Int32, b String))` is one marker, then the elements raw (probed bytes: value `(7,'xy')` = `00 07000000 02 7879`; NULL = `01`). Writing a per-child marker yields an extra byte per element: single-field tuples work by coincidence (the lone child marker doubles as the tuple flag, L-11), multi-field ones fail with `ATTEMPT_TO_READ_AFTER_EOF`.
- A genuinely nullable element is wire-distinct: `Nullable(Tuple(a Nullable(Int32)))` for `(7)` is `00 00 07000000`, for `(NULL)` is `00 01`. DESCRIBE emits an extra `t.a.null` row only in that case, and `shouldSkipDescribeRow` currently drops `.null` rows, so the sub-column list alone cannot distinguish the two shapes.

**Expected behaviour after the fix.**
1. A DESCRIBE-derived `Nullable(Tuple(...))` column reports `isNullable()==true` (unit test via `Column.extractColumn`).
2. The writer emits exactly one marker for the tuple and, for each element, a marker **only if the element is declared `Nullable` inside the tuple type**. Fixture matrix (live, typed read-back): `Nullable(Tuple(a Int32, b String))` with a value and with null; `Array(Nullable(Tuple(a Int32, b String)))` `[(1,'x'), NULL]`; the single-field `Array(Nullable(Tuple(a Int32)))` case that "worked by coincidence" must still read `[(5),NULL]`; `Nullable(Tuple(a Nullable(Int32), b String))` with `(NULL,'x')`.
3. Unchanged: non-nullable `Tuple` slots still throw the nested-null `DataException` for a null (L-11 first half, L-10).

**Design constraint (HIGH, read from the repo).** `Table.java` is listed as "Adopted wholesale, byte-identical to upstream v1.7.0" in FORK-DELTA and checked by `verify-merge-invariants.sh`. Fix in `Column.extractColumn` (fork-owned, already carries Dynamic and the Array-recursion order) and the writer TUPLE case, not in `Table.updateParent`. Concretely: parse the element list of the raw tuple type string in the Tuple branch (`splitUnlessInBrackets` already exists at `Column.java`, but element names can be backticked, unnamed (`Tuple(Int32, String)`), or contain parentheses), store per-element declared nullability on the tuple `Column`, and have the TUPLE writer case consult it instead of the child's over-reporting `isNullable()`.

**Fallback if the planner judges the parse too risky (explicit, not hidden).** Ship F1a, keep the WARN narrowed to `Nullable(Tuple(`, and record Tuple in FORK-DELTA Known costs with the probe evidence above. The milestone goal names Tuple, so this fallback must be a conscious recorded decision, not a silent drop. Recommended ordering: F1a plan first (low risk, ships alone), F1b as a second plan with its own live matrix and its own `Done when`.

**Complexity: HIGH.** Type-string parsing, nested containers (`Map(K, Nullable(Tuple))`, `Array(Array(Nullable(Tuple)))`), RowBinary byte layout, and the exactly-once rule that the same batch must serialize byte-identically on retry (the change is deterministic per column metadata, so the invariant holds, but state it as an acceptance criterion: two serializations of one batch are byte-equal).

**Operator-visible value change for both F1a and F1b (goes in F8b).** (a) Previously silently stored NULLs or shifted bytes become correct values. (b) Batches that failed with `DataException` on a null entry in `Array/Map(Nullable(Enum|Tuple))` now succeed and store NULL. (c) Rows already written wrongly are **not** repaired. (d) The startup WARN text/frequency changes (log-based alerts).

---

#### F2. 13-REVIEW WR-01: empty-CDC skip on the `bypassRowBinary=true` JSON route

**Verified current behaviour (HIGH, code read).** `DebeziumRecordConvertor.doConvert` returns an empty `jsonMap` for `op=t` and for a null `before`/`after` struct (`DebeziumRecordConvertor.java:77-97`). `doInsertBatch` (`ClickHouseWriter.java:232-242`) sends `SCHEMA`/`DEBEZIUM_CDC` to `doInsertJson` when `isBypassRowBinary() || shouldSkipTableMetadata()`. In `writeRecordsJson` (`:1351-1405`) the empty-map `continue` exists only inside the `metadataFree && DEBEZIUM_CDC` branch (`:1365-1376`); the `else` branch's `case DEBEZIUM_CDC` (`:1387`) builds an empty `data` map, `cleanupExtraFields` returns `{}`, and the writer emits `{}`: an all-defaults row (id 0, `_version` 0, `is_deleted` 0).

**Population.** `DEBEZIUM_CDC` records exist only when `debeziumCDCEnabled=true` (`Record.java:61`), i.e. upstream's convertor, which the fork "deliberately did not adopt" (FORK-DELTA item 9). The affected pipeline is `debeziumCDCEnabled=true` + `bypassRowBinary=true` with `insertFormat` unset. Expect low production exposure; this is a consistency fix.

**Expected behaviour.**
1. A `DEBEZIUM_CDC` record whose `jsonMap` is null or empty writes **nothing** on every JSON route, explicit or bypass, with the same DEBUG line (topic, partition, offset only; no values).
2. Recommended shape: hoist one guard ahead of the three-way `if` in `writeRecordsJson` (what 13-REVIEW WR-01 proposed) so both branches share one predicate. Do not adopt the wider IN-04 refactor (`serializeJsonRow`): it adds upstream-rebase churn for no behaviour change, and that is why it was deferred.
3. Tests: mirror `ExplicitJsonInsertPathLiveTest#metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes` (`:378-441`) with `BYPASS_ROW_BINARY=true` and `INSERT_FORMAT` unset: envelopes `c`, `t`, `u`-with-null-after, `d` yield exactly 2 rows. Add (i) a batch consisting only of empty CDC records (the metadata-free route sends an empty body that the server accepts as a zero-row insert per L-06; prove the same for the bypass route on V2), and (ii) a Docker-free routing/serialization test beside `MetadataFreeJsonInsertUnitTest`. Mutation-check as Phase 13 did.
4. Exactly-once: skipping inside serialization does not change batch composition or offset ranges, and is deterministic on retry, so the `BEFORE`/`AFTER` invariant holds. State this in the plan.

**Operator-visible behaviour change (F8b).** Truncate and null-struct events no longer insert a phantom all-defaults row on this route. Pipelines that already stored such rows (a `ReplacingMergeTree` row with key 0) keep them; the note should give a find query, not an automatic cleanup.

**Adjacent gap, explicitly not in scope (record, do not fix).** The default RowBinary route with `debeziumCDCEnabled=true` has no skip either: `writeRecordsRowBinary` (`:1292-1308`) runs each table column with `fieldExists=false`, so it writes defaults where every column has a default or is nullable, and otherwise fails the batch through the message-less exception of F3. Fixing it touches a hot path of upstream code; list it in FORK-DELTA Known costs item 2 as the residual.

**Complexity: LOW.**

---

#### F3. 13-REVIEW WR-03: `doWriteCol` "not nullable and no value" names the column

**Verified current behaviour (HIGH).** `ClickHouseWriter.java:1093-1094`: `LOGGER.error("Column {} is not nullable and no value is provided", name); throw new RuntimeException();`. Since COLS-07 (`Column.java` Array recursion argument order) an `Array(Tuple)` record that lacks a no-default tuple field reaches it. `Processing.doInsert` wraps every failure as `new RuntimeException(queryId.toString(), e)` (`Processing.java:72, :96`), so the Connect status and DLQ stack trace show `Caused by: java.lang.RuntimeException` with no text. The column name exists only in the connector's own log.

**Expected behaviour.**
1. The branch throws `org.apache.kafka.connect.errors.DataException` (already imported and used throughout this file) with a message built from `Column` metadata only: column name (for a tuple child this is the dotted path such as `at.a`, which already names the field) and rendered type via the existing `renderColumnType` (`:~580-590`). Suggested text in the style of the sibling nested-null message: `column '<name>' (<type>): no value provided for a non-nullable column without a default`. It must not contain record data.
2. Keep the existing ERROR log line (ROLLOUT-v1.7.0_gg1 L-17 paragraph tells operators to grep it); the log line text can stay identical, only the thrown exception changes.
3. Retry/DLQ classification is unchanged: `Utils.handleException` treats `RuntimeException` and `DataException` both as non-retriable once wrapped by `Processing`. The gain is visibility of the cause, not a routing change. State that precisely in the requirement to avoid over-promising "visible in DLQ headers" (the exception-message header carries the wrapper's queryId text; the cause appears in the stack-trace header and the task status trace). MEDIUM confidence on DLQ header specifics; the writer-level type and message are what the test pins.
4. Test: Docker-free, drive `doWriteCol` (protected) with `fieldExists=false` for a non-nullable, no-default column and for an `Array(Tuple)` child built as in `ColumnArrayRecursionTest`; `assertThrows(DataException.class)`, assert the message names the column and type and contains no sentinel value. No existing test pins the message-less form (`ColumnArrayRecursionTest` has no `assertThrows`).
5. Top-level null into a non-nullable column (`:1056, :1067`, upstream's `RuntimeException("An attempt to write null...")`) is a different branch and stays as is (D-11).

**Docs:** remove FORK-DELTA Known costs item 3 (`:128`) and the "without a message" clause in ROLLOUT L-17 (`:209`).

**Complexity: LOW.**

---

#### F4. 13-REVIEW WR-04: null check before `getMessage().contains(code)`

**Verified current behaviour (HIGH).** `ClickHouseWriter.java:1177-1178`: the stream `filter(code -> e.getMessage().contains(code))` runs before `e.getMessage() != null`. An exception with a null message reaching the generic `catch (Exception e)` throws an NPE from the handler and replaces the real error. The branch is the deprecated V1 error-string table (`UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE`, `:66`), reached for any non-`ServerException`.

**Reachability.** F3's `new RuntimeException()` is one null-message source, and on V1 `writeRecordsRowBinary` runs on the calling thread so it propagates unwrapped. On V2 the writer lambda's exception is wrapped by the client/`ExecutionException`, which gives a non-null message, so V2 reachability is unproven (MEDIUM). Other null-message exceptions exist (explicit `new NullPointerException()`), so the guard is justified independently of F3.

**Expected behaviour.**
1. Read the message once into a local; if null, treat as "no matching code" and fall to the `else` branch that logs and rethrows the **original** exception instance.
2. Behaviour for a non-null message containing a known code and `retry==true` is unchanged (table-mapping refresh, single retry).
3. Test (Docker-free): subclass or spy the writer so protected `doInsertRawBinaryV2` throws `new RuntimeException()` (null message); assert the same instance propagates, no NPE. Add the positive twin: message containing `"ClickHouseException: Code: 33"` triggers one retry. Pattern: the unit tests that already override `doInsertJson`/count calls (`MetadataFreeJsonInsertUnitTest`).

**Adjacent, same bug class, NOT in scope (list, do not fix):** `Utils.handleException` IOException branch does `msg.indexOf(...)` on `rootCause.getMessage()` with no null check (`Utils.java`, byte-identical to upstream except imports). Note it in FORK-DELTA Known costs so the reviewer does not rediscover it.

**Complexity: LOW.**

---

#### F5. 14-REVIEW WR-01: standing response-leg LZ4 assertion in `Lz4RoundTripLiveTest`

**Verified current state (HIGH).** The test asserts the request leg only (`CompressedReadBufferBlocks > 0`, `Lz4RoundTripLiveTest.java:~170-173`) and then reads data back; read-back passes even if the server answers uncompressed. The response leg was proven once by an uncommitted zstd probe (`Invalid LZ4 magic byte: '-112'`, MERGE-CHECKLIST-v1.7.0 Recorded Fact 32). The `@DisplayName` and Javadoc say "both legs".

**Probe result that decides the mechanism (HIGH, live on 26.9.14.10).** A SELECT of 24 MB of repetitive RowBinary over the HTTP interface, same `result_bytes` in `system.query_log` (24,000,415):

| Request | `ProfileEvents['NetworkSendBytes']` | `CompressedWriteBufferBlocks` |
|---|---|---|
| `?compress=1&network_compression_method=lz4` | **96,434** | 0 |
| no `compress` | **24,001,170** | 0 |

So: **`CompressedWriteBufferBlocks` is the wrong counter for the response leg (it stays 0 even when compressed)**, and `Content-Encoding` is not applicable because the test client uses native compression (`compressServerResponse(true)`, `useHttpCompression(false)`). The reliable server-side signal is `NetworkSendBytes` against `result_bytes` for the SELECT's own `query_id`.

**Expected behaviour.**
1. After the existing read-back, run a large SELECT on the same LZ4 client with an explicit query id (`QuerySettings.setQueryId`), `SYSTEM FLUSH LOGS`, then read `result_bytes` and `toInt64(ProfileEvents['NetworkSendBytes'])` from `system.query_log` (`type='QueryFinish'`, exactly one row, as the request-leg assertion already does).
2. Assert `NetworkSendBytes * 4 < result_bytes` (the 2000 rows x ~2.4 KB repeated payload compresses far better than 4x; probe ratio on similar data was ~250x). Also assert `result_bytes` is above a floor so a tiny result cannot pass vacuously.
3. Optional hardening (differentiator): a negative control, same SELECT through a client with `compressServerResponse(false)`, asserting `NetworkSendBytes` is within a few percent of `result_bytes`. It self-validates the threshold and catches a server that stops compressing; cost is one extra query.
4. Rename/clarify: keep "both legs" in the display name only once the assertion exists.

**Rejected alternatives (anti-feature):** asserting the `Invalid LZ4 magic byte` error under `network_compression_method=zstd` (depends on client error wording and server defaults); a counting proxy (new Toxiproxy wiring for no extra signal).

**Dependencies/risks.** None in `src/main`. Test-only; same colima/ClickHouse container. `latest` drift could in theory change accounting, which the negative control would expose.

**Complexity: LOW-MEDIUM.**

---

#### F6. T-13-19: mask the raw DateTime value in upstream's two ERROR logs

**Verified current behaviour (HIGH).** `ClickHouseWriter.java:461` logs `"Error parsing date time string: {}", value.getObject()` (DateTime branch) and `:515` logs `"Error parsing date time string: {}, exception: {}", value.getObject(), e.getMessage()` (DateTime64 branch). Both are unchanged from upstream `v1.7.0` and contradict AGENTS.md ("Do not log secrets or data in plaintext... records").

**Two traps the requirement must name.**
1. Masking only `value.getObject()` is **not enough at `:515`**: `e` is a `DateTimeParseException` whose message is `Text '<the raw string>' could not be parsed at index N`, so `e.getMessage()` leaks the same value. Log the exception class name (and nothing from its message), or drop it.
2. **`util/Mask` is not the right tool and must not be reused.** It contains only `passwordMask` (`Mask.java`), which returns `"***" + middle + "***"`, i.e. it hides the ends and **shows the middle** of the string. Use no content-derived masking at all.

**Expected behaviour.**
1. Both lines log only catalog metadata: column name (`doWriteDates` already receives `columnName`, `:412`), ClickHouse type, source Kafka field type, and the exception class. Same discipline as `buildNestedNullMessage` (built from `Column` metadata only). No substring, length-derived or hashed value.
2. The thrown `DataException` ("(Potentially) Not implemented conversion from STRING to DateTime", `:523-526`) keeps working; adding the column name to its text is an optional improvement (no test pins the string; `grep "Potentially" src/test` is empty).
3. Test: Docker-free. Prefer extracting the message into a small package-private builder and asserting a sentinel string (`"SECRET-2026"`) is absent from the result for both the DateTime and DateTime64 branches. To prove the logger actually uses it, attach a log4j (`org.apache.log4j.AppenderSkeleton`, the test classpath logs through `slf4j-reload4j`, MEDIUM: confirm the class is on the test compile classpath) appender and assert no event contains the sentinel. This is the first log-capture test in the repo (`grep Appender src/test` finds only `log4j.properties`).
4. Record the patch in FORK-DELTA as upstream-identical code fixed in the fork, with the two lines added to `.planning/scripts/fork-anchors.txt` so a future upstream sync that overwrites them is detected; remove FORK-DELTA Known costs item 6 (T-13-19 half).

**Adjacent leaks, explicitly out of scope, list in the security note (MEDIUM, from Java semantics, not exercised):** the generic `catch (Exception e)` at `:855` logs the exception with its message, and `UUID.fromString` throws `Invalid UUID string: <value>` and `NumberFormatException` messages can embed the input. Different sites, different threat; do not widen T-13-19.

**Complexity: LOW.**

---

#### F7. 15-02 review IN-01..IN-10: each fixed or closed with a written reason

Source: `15-REVIEW.md` and `15-REVIEW-DISPOSITION.md`. IN-01..IN-06 are the code review of commits `981a89d6`/`4a91b92c` (disposition `deferred`). IN-07..IN-10 are the incremental docs review (disposition `open`; WR-01..03 were fixed by `505e4981`). The milestone rule is "fixed or closed with a written reason", so every row must end with one of the two, recorded in the disposition file.

**Key structural fact:** IN-01 to IN-05 are all about the L-21 WARN (`ClickHouseHelperClient.java:490-525`, `:579`, `:650`). F1 decides their fate, so sequence F7 after F1.

| ID | Finding | Recommended disposition | Complexity |
|----|---------|------------------------|------------|
| IN-01 | WARN fires on routes where the defect cannot occur (schemaless JSON, String, `bypassRowBinary`) | If F1 removes the WARN: **close, obsolete (removed)**. If only Enum is fixed: WARN narrows to Tuple and the imprecision remains for Tuple; **close with reason "FORK-DELTA states the route restriction"** (already stated in Known costs item 1, `FORK-DELTA.md:124`) rather than plumbing route into the helper client | LOW |
| IN-02 | V1 describe hook only exercised under `CLIENT_VERSION=V1` | **Close**: V1 is out of the test matrix by project constraint; obsolete if the WARN is removed | none |
| IN-03 | Warned-key set never evicts; key joined with `.`/`:` is ambiguous | If WARN removed: **close, obsolete**. Otherwise **fix cheaply** while the file is open (join with a non-printing separator or a record key) or close with "bounded by schema size, collision only suppresses one WARN" | LOW |
| IN-04 | WARN wording is not pinned by a test | If WARN removed: **close, obsolete**. If it survives for Tuple: **fix**, F6's log-capture helper makes this a few lines | LOW (reuses F6) |
| IN-05 | Constants sit mid-class | **Close**: style-only, adds rebase churn (same reasoning as 13 IN-03). Obsolete if WARN removed | none |
| IN-06 | Edge pins cover only mantissa-less scientific form | **Fix**: add one `@CsvSource` row to `MetadataFreeJsonWriterTest.java:~325-336`, e.g. `io.debezium.time.NanoTimestamp, 123, 1.23E-7`; the server was already shown to accept it in the review | LOW |
| IN-07 | Rollout item 2 recovery steps not qualified by `exactlyOnce`; closing sentence loose | **Fix** in `docs/ROLLOUT-v1.7.0_gg1.md` (`~:94-113`): "step 1 always, step 2 only with `exactlyOnce=true`"; end with the only real condition (explicit `insertFormat=json` with schema'd records) | LOW |
| IN-08 | Item 8 omits the re-merge consequence of `git revert -m 1` | **Fix** (`ROLLOUT:~318`): add that going forward again requires reverting the revert first | LOW |
| IN-09 | FORK-DELTA says tag "once it exists"; `grep -rc` prints per-file counts, not totals | **Fix** `FORK-DELTA.md:94` and `:154-155` (use `grep -rho ... \| wc -l`) | LOW |
| IN-10 | Planning IDs (`R-01`, `WR-02 strictness`, `13-REVIEW WR-02`) unexplained in published CHANGELOG/ROLLOUT; item 6 grep pattern broader than its text; MBean `version=` and client-name changes absent from CHANGELOG; lz4 advisory reason missing | **Fix**: plain-language CHANGELOG entries (`CHANGELOG.md:10, :17, :34`), ledger path in `ROLLOUT:13`, drop the bare `set_compression_method` alternative at `ROLLOUT:246`, add the MBean/client-name and advisory lines | LOW-MED |

Process constraints (from project memory): appended review/disposition findings must keep `### ID: title` headings or the disposition step silently drops them (6 of 13 were lost once); docs get a code review **before** the publication gate (the gg1 rollout shipped a `TRUNCATE` hazard because its first docs review came after publication). IN-07..IN-10 and every new gg2 doc must go through that review before F8b's merge.

---

#### F8a. Early publication of `505e4981` (first phase, before any code)

**Verified state (HIGH).** `origin/custom_build` = `f328fd21` (`v1.7.0_gg1`). `sync/v1.7.0` is 20 commits ahead. The diff of the shipped surface is exactly two docs files: `git diff --stat origin/custom_build sync/v1.7.0 -- src build.gradle.kts gradle .github VERSION CHANGELOG.md docs` = `docs/FORK-DELTA.md` (1 line) and `docs/ROLLOUT-v1.7.0_gg1.md` (+18/-8). The published text still tells operators to `TRUNCATE TABLE` the shared KeeperMap state table (15-REVIEW WR-01), the wrong `version=*` regex advice (WR-03) and an unqualified `best_effort` default (WR-02).

**Expected behaviour (acceptance criteria).**
1. Before the merge, a recorded check that `git diff origin/custom_build sync/v1.7.0 -- src build.gradle.kts gradle .github VERSION` is empty (docs-only in the shipped sense; `.planning/` history is expected to ride along, as it did for gg1).
2. Merge by non-force `--no-ff` into `custom_build`, after the **user's own** explicit approval. `custom_build` is protected (GSD base branch) and executors refuse relayed approvals, so the orchestrator performs this step inline (project memory, Phase 11).
3. Push **one branch only**. CI triggers on branch push, not tags (CI-08); two branches at one SHA cancel each other. Do not push `.planning/.gsd-ws-arg` (untracked) or local-only milestone tags; keep `.planning/state.json` out of the commit.
4. Exactly one `tests.yaml` run on the published SHA, `conclusion=success`, none cancelled (re-use the CI-06 locator). Expect the suite to be unchanged (no code differs), so the run is a hygiene check, not a test of new behaviour.
5. Post-publication: the corrected `ROLLOUT-v1.7.0_gg1.md` is readable on `origin/custom_build`; update PROJECT.md's "knowingly open" paragraph (the unpublished-doc warning at `PROJECT.md:343` becomes history).

**Complexity: LOW, approval-gated.** Risk is procedural (wrong branch pushed, tag pushed first, stale approval), not technical.

---

#### F8b. `v1.7.0_gg2` release notes and rollout note

**Expected behaviour.**
1. `VERSION` = `v1.7.0_gg2`. The `VERSION` row of `.planning/scripts/fork-anchors.txt` moves in the **same commit**, and a `clean` build is needed because the generated `Version.java` is not an input of `generateVersionFile` (FORK-DELTA "For the next syncer"). `MBeanNameUnitTest` and any exact-version assertion re-run green.
2. `CHANGELOG.md` gets a `1.7.0_gg2` section above `1.7.0_gg1`, in plain language (see IN-10 for the identifier-opacity problem), covering: Nullable(Enum8/16) fix (and Tuple per F1b outcome), CDC-skip on `bypassRowBinary`, DataException naming the column, null-message guard, masked DateTime logs, and the CI/build items the other research files own.
3. Rollout note: recommend a **new, short `docs/ROLLOUT-v1.7.0_gg2.md`** in the gg1 format (each item: what changed, "Am I affected?" self-check, action) rather than appending to the gg1 doc, because gg1's item 4 currently documents the limitation as open. Items: (a) 999.1 value change on `Nullable(Enum)`/`Nullable(Tuple)`: reuse the gg1 item 4 discovery query (`system.columns ... type LIKE '%Nullable(Enum%' OR type LIKE '%Nullable(Tuple%'`, `ROLLOUT:183`), state the three effects listed under F1, state that already-stored wrong rows are not repaired and give a verification query shape; (b) the `bypassRowBinary` phantom-row change with a find query for key-0 rows; (c) the MBean `version=` segment changes again, so exact `version=` matchers break (same advice as gg1 item 7, which now has the regex-correct text); (d) log-text changes for log-based alerting (WARN narrowed/removed; new DataException text; masked DateTime logs).
4. Second `--no-ff` merge into `custom_build` with the same gate as F8a, exactly one green `tests.yaml` run on the published SHA, tag `v1.7.0_gg2` pushed **after** the branch run (or relying on CI-08, which makes tag pushes harmless).
5. FORK-DELTA is updated as the single source of truth: the patched-lines rows for F1/F2/F3/F4/F6, the Known-costs items 1, 2, 3, 5, 6 removed or rewritten, the raw-numbers table remeasured with the four `git diff --shortstat` commands, anchor inventory updated, `verify-merge-invariants.sh` run green.

**Complexity: LOW-MEDIUM** (mostly writing and review discipline; docs review before the gate).

---

### Differentiators (valuable, not required for the milestone to be correct)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| F5 negative control (`compressServerResponse(false)` => `NetworkSendBytes` ~ `result_bytes`) | Self-validating threshold; catches a server that silently stops compressing | LOW | One extra query in the same test |
| F6 column name in the "(Potentially) Not implemented conversion" `DataException` (`ClickHouseWriter.java:523-526`) | The actionable message AGENTS.md asks for; today the DataException names no column | LOW | No existing test pins the text; keep metadata-only |
| F1 upstream PR for `Column.extractColumn` Enum/Tuple nullability | Shrinks the fork delta at the next sync; the defect exists in upstream `v1.7.0` | LOW (writing) | Only after the fix is live-proven; do not block the release on it |
| F2 also applied to the RowBinary route | Removes the last empty-CDC inconsistency | MEDIUM | Touches the upstream hot path `writeRecordsRowBinary`; see anti-features |
| IN-04 wording pin using F6's log-capture helper | Closes a review item for ~5 lines | LOW | Only if the WARN survives for Tuple |

### Anti-Features (do not build)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Support or test `LowCardinality(Nullable(Enum))` | Named in the research question | ClickHouse `latest` rejects it, probed, even with `allow_suspicious_low_cardinality_types=1` | One sentence in FORK-DELTA/gg2 note: not a legal column type |
| Masking the DateTime string with `Mask.passwordMask` | "Consistent with the util Mask class" | It reveals the middle of the string; any content-derived masking still leaks | Log column name, type and exception class only |
| Logging `e.getMessage()` of `DateTimeParseException` "masked" | Keeps the diagnostic detail | The message embeds the input verbatim | Exception class name only |
| Per-record skip of a batch with a null in a non-nullable slot | Avoids failing the whole batch (13-REVIEW WR-05 thinking) | Changes batch composition; breaks the exactly-once identical-block invariant | Fix nullability metadata (F1) so legal slots stop throwing |
| Fixing `Table.updateParent` for Nullable(Tuple) children | It is where the children are attached | `Table.java` is byte-identical to upstream and checked by the merge-invariant script | Carry per-element nullability on the tuple `Column`, consult it in the writer |
| IN-04 (13) refactor `serializeJsonRow` / IN-01 (13) shared nested-null helper | DRY, readability | Diff churn against upstream in a high-risk file; deferred for that reason | Keep F2 as a hoisted guard only |
| Fixing `Utils.handleException`'s null-`msg` IOException branch, `UUID.fromString` message leak, `convertEnumValues` returning null for unknown member | Same bug family as F3/F4/F6 | Different sites and threats; milestone scope creep; `Utils.java` is upstream-identical | Record in FORK-DELTA Known costs; one line each |
| Asserting response compression via the `zstd` negative probe or a Toxiproxy byte counter | Avoids touching `system.query_log` | Depends on client error text / adds infrastructure | `NetworkSendBytes` vs `result_bytes` |
| Appending IN-07..IN-10 fixes to the early-publication merge | "Publish docs once" | Early publication exists to remove a data-loss hazard fast; widening it delays it | Publish `505e4981` as is (F8a), fix IN-07..10 in the gg2 docs pass with a pre-gate docs review |

## Feature Dependencies

```
F8a (early publish 505e4981)  --must precede-->  every code phase (milestone rule)

F1a (Enum nullability)
    |--requires--> flip/retire L-21 WARN Enum half  (ClickHouseHelperClient.java:490-525, anchor row 57)
    |--enables---> F7 IN-01..IN-05 disposition (obsolete if WARN removed)
    |--then------> F1b (Tuple nullability; Column.java + writer TUPLE case; NOT Table.java)

F3 (DataException w/ column) --shares null-message class with--> F4 (guard in catch)
F6 (log-capture test helper) --reused by--> F7 IN-04 (if WARN survives)
F1..F6 behaviour final       --feeds-->     F8b (CHANGELOG, ROLLOUT-v1.7.0_gg2, FORK-DELTA, VERSION bump + anchor)
F7 IN-07..IN-10 (docs)       --requires-->  docs code review BEFORE F8b gate
F8b                          --requires-->  F8a done, one green CI run per published SHA
F2 (CDC skip)                --independent; documents residual RowBinary gap in FORK-DELTA
F5 (response-leg assertion)  --independent; test-only
```

### Dependency notes

- **F1b must not touch `Table.java`.** The fork records `Table.java` as byte-identical to upstream `v1.7.0` and `verify-merge-invariants.sh` enforces it. The fix lives in `Column.java` and the `ClickHouseWriter` TUPLE case.
- **F7 waits for F1.** Five of the six code-review INs are about the WARN that F1 may delete.
- **F8b waits for everything user-visible.** The rollout note describes final behaviour; writing it first guarantees a rewrite.
- **F3 before or with F4 is cleaner** because the F3 fix removes the most likely null-message exception, but F4 stands alone and should still be tested with an artificial null-message exception.
- Files with high-risk status (AGENTS.md): `ClickHouseWriter.java` (F1, F2, F3, F4, F6 all edit it). Keep the edits as separate small commits per follow-up so each is revertable and each FORK-DELTA row maps to one commit.

## MVP Definition

### Launch With (the milestone is not shippable without these)

- [ ] F8a early publication (first phase; removes the published `TRUNCATE` hazard)
- [ ] F1a Enum nullability, with live matrix, flipped pins, WARN narrowed, docs
- [ ] F2, F3, F4 (small writer fixes, each with a Docker-free test)
- [ ] F6 mask the two DateTime logs (security hygiene; zero risk to data path)
- [ ] F5 response-leg assertion
- [ ] F7 every IN dispositioned in `15-REVIEW-DISPOSITION.md`
- [ ] F8b VERSION, CHANGELOG, ROLLOUT-v1.7.0_gg2, FORK-DELTA, second merge, one green CI run

### Include but gated (decision at plan time)

- [ ] F1b Tuple nullability: do it as its own plan after F1a. If the per-element parse proves unsafe, take the recorded fallback (WARN stays for Tuple, FORK-DELTA Known cost with the probe evidence) and say so in the gg2 note. Do not let it block F1a or the release.

### Defer (record in FORK-DELTA Known costs)

- [ ] Empty-CDC skip on the default RowBinary route (upstream hot path)
- [ ] `Utils.handleException` null-message IOException branch; other raw-value exception messages (`UUID.fromString`)
- [ ] Upstream PR for the Enum/Tuple nullability fix

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| F8a early publication | HIGH (removes published data-loss advice) | LOW | P1 |
| F1a Enum nullability | HIGH (silent wrong values / corruption today) | LOW-MED | P1 |
| F1b Tuple nullability | MEDIUM (rarer type, multi-field case hard-fails) | HIGH | P2 (gated, fallback defined) |
| F2 CDC skip on bypass route | MEDIUM (phantom row; narrow population) | LOW | P1 |
| F3 DataException names column | MEDIUM (diagnosability) | LOW | P1 |
| F4 null-message guard | LOW-MED (masks real errors on V1 / odd paths) | LOW | P1 |
| F6 DateTime log masking | MEDIUM (audit finding, privacy) | LOW | P1 |
| F5 response-leg LZ4 assertion | MEDIUM (guards future lz4/client bumps) | LOW-MED | P2 |
| F7 IN-01..IN-10 | LOW each, closes audit trail | LOW each | P2 |
| F8b release notes and rollout | HIGH (operators need the value-change note) | LOW-MED | P1 |

**Priority key:** P1 must have for launch, P2 should have, P3 future.

## Competitor Feature Analysis

Not applicable: this is a fork-hardening milestone, not a market comparison. The relevant reference is upstream `ClickHouse/clickhouse-kafka-connect` `v1.7.0`: it has the same `Column.extractColumn` Enum/Tuple defect, the same message-less `RuntimeException`, the same two DateTime ERROR logs and the same `getMessage()` ordering, so each fix is a fork patch that must be recorded in `docs/FORK-DELTA.md` and anchored.

## Sources

- Repository (HIGH): `src/main/java/.../sink/db/mapping/Column.java:189-290`; `.../sink/db/mapping/Table.java` (tuple children attach at `updateParent` TUPLE case); `.../sink/db/ClickHouseWriter.java:232-242, :412-526, :540-590, :660-770, :1030-1100, :1160-1210, :1215-1308, :1351-1405, :1521-1540`; `.../sink/db/helper/ClickHouseHelperClient.java:490-525, :579, :650`; `.../sink/data/convert/DebeziumRecordConvertor.java:77-110`; `.../util/Mask.java`; `.../util/Utils.java` (`handleException`); `.../sink/processing/Processing.java:70-96`.
- Planning (HIGH): `.planning/PROJECT.md` (Active list, Current Milestone); `.planning/ROADMAP.md` backlog 999.1/999.2; `docs/FORK-DELTA.md` (rows `:35-45`, Known costs `:118-147`); `docs/ROLLOUT-v1.7.0_gg1.md`; `.planning/milestones/v1.7.0_gg1-sync-phases/13-*/13-REVIEW.md`, `13-REVIEW-DISPOSITION.md`, `13-BEHAVIOUR-LEDGER.md` (L-03, L-06, L-11, L-13, L-17, L-21, L-22), `13-RESEARCH.md` (Spike b2/b3, Pitfall 8), `13-SECURITY.md` (T-13-19); `14-*/14-REVIEW.md`, `14-REVIEW-DISPOSITION.md`; `15-*/15-REVIEW.md`, `15-REVIEW-DISPOSITION.md`; `.planning/MERGE-CHECKLIST-v1.7.0.md` Recorded Fact 32; `git diff --stat origin/custom_build sync/v1.7.0`.
- Live probes (HIGH, ClickHouse 26.9.14.10 from the local `clickhouse/clickhouse-server:latest` image, throwaway containers removed): `DESCRIBE ... describe_include_subcolumns=1` for `Nullable(Enum8|Enum16|Tuple)` (sub-columns report `Nullable(...)` regardless of element declaration, `t.a.null` only for truly nullable elements); `FORMAT RowBinary` bytes for `Nullable(Enum8)`, `Nullable(Enum16)`, `Nullable(Tuple)` and nested-nullable-element tuples; `RowBinary` input replay showing today's mis-reads; `LowCardinality(Nullable(Enum8))` rejection; `system.settings` (`enable_nullable_tuple_type=1`, `input_format_null_as_default=1`, `date_time_input_format=best_effort`); HTTP `compress=1` vs uncompressed `NetworkSendBytes` (96,434 vs 24,001,170 for an identical 24,000,415-byte result).
- Project memory (MEDIUM, process): publication gotchas (protected `custom_build`, relayed approvals refused, tag/branch CI cancellation, appended-review heading rule, docs review before the gate).
- No web search was needed; all answers came from the code, the planning archive and live probes. Not performed: running the Gradle suite, verifying that reload4j is on the test compile classpath (F6), and V2 reachability of the F4 NPE.

---
*Feature research for: v1.7.0_gg2-hardening follow-ups*
*Researched: 2026-10-10*
