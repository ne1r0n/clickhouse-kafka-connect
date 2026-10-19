# Phase 22 Evidence

- Phase: 22 (Writer Record Data Exposure)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for HARN-08 and WRT-13..WRT-15, read by the verifier together with 22-BEHAVIOUR-LEDGER.md. The file holds commands, counts, display names and message formats only, never a sentinel or record value. Nothing here was pushed.
- Entry format: one `### E-NN: {title}` section per entry, with `key: value` lines for the measured facts. Long output stays in `/tmp/gsd22/exec/`.

## Index

| ID | Plan | What it records | Status |
|----|------|-----------------|--------|
| E-01 | 22-01 | Spotless baseline (D-16), anchors mirror, HARN-08 before and after runs, F7 reading | recorded |
| E-02 | 22-02 | WRT-14 pin on the unchanged writer | recorded |
| E-03 | 22-03 | WRT-13 red-before and green-after, D-03 result, WRT-15 commit | recorded |
| E-04 | 22-04 | Live put() evidence and DLQ headers | pending |
| E-05 | 22-05 | Phase close gate and SC1..SC4 readings | pending |

### E-01: Spotless baseline, anchors mirror and the HARN-08 migration (22-01)

Recorded 2026-10-19T20:07:27Z. Scratch output is in `/tmp/gsd22/exec/` (`harn08-before.log`, `harn08-before.xml`, `harn08-after.log`, `harn08-after.xml`, `testunit-22-01.log`, `anchors-22-01.txt`, `spotless-gate.sh`).

spotless_base_rev: fefd8648524f8b906a22f47540391f170e2357af
spotless_base_files: 46
spotless_base_md5: f2e32c7bc069ecd155830f82d26285d6
spotless_baseline_red_touched_by_phase: ClickHouseWriter.java 9/10, Column.java 13/14, ClickHouseSinkTaskWithSchemaTest.java 27/28
spotless_after_22_01: SPOTLESS-BASELINE-EQUAL
anchors_mirror_22_01: rows=59 bad=0
harn08_before: tests=5 failures=0 mockito_lines=5
harn08_after: tests=5 failures=0 mockito_lines=0
testunit_22_01: Tests summary: 577 tests, 575 succeeded, 0 failed, 2 skipped
harn08_commit: 84e773be9d20bbe67a8850c05904a099636239e1

The class XML reports `tests="5" skipped="0" failures="0" errors="0"` both before the edit and after it. The D-16 gate (whole-tree `spotlessApply` numstat on a throwaway `git archive` copy of HEAD plus the working tree's changed and untracked files) printed SPOTLESS-BASELINE-EQUAL after the edit, so the new fixture and the edited test are spotless-clean on their own.

(a) D-10 constructor facts. With a dummy builder (`127.0.0.1:8123`, no proxy, no credentials) the real `ClickHouseHelperClient` constructor opens no connection and owns no thread, as measured in 22-RESEARCH ("HARN-08 facts"). `CountingHelperClient` is therefore a plain subclass overriding `isUseClientV2()`, `pinReplica()` and `unpinReplica()`, and each test closes it with try-with-resources (the inherited `close()` closes the V2 client). Nothing is bytecode-instrumented, so the Phase 18 deferred item (OpenJ9 `retransformClasses` crash risk in ClickHouseWriterNullMessageUnitTest) is closed by HARN-08. Other Mockito use in the suite stays out of scope (22-CONTEXT domain). `git diff --quiet fefd8648 HEAD -- src/main` holds.

(b) F7 reading of HARN-08 and ROADMAP SC3, stated for the verifier: the operative clauses are "no longer inline-mocks ClickHouseHelperClient with Mockito" and "closes the Phase 18 OpenJ9 retransformClasses deferred item". The phrase "overrides one level up, in the CapturingClickHouseWriter pattern" is met by the locked D-10 design, a hand-written Mockito-free subclass written in CapturingClickHouseWriter's no-Mockito style. `doInsertRawBinary` calls `chc.isUseClientV2()`, `chc.pinReplica()` and `chc.unpinReplica()` directly, so no writer-level override can count them without a production change, which D-10 forbids.

D-16 plan-time import probe, relied on by plan 22-03: a new `org.apache.kafka.connect.errors.*` import in ClickHouseWriter.java moves its numstat from 9/10 to 10/11, because the `org.*` block is the block spotless moves. An import placed right after `com.clickhouse.kafka.connect.util.Utils` keeps it at 9/10.

### E-02: WRT-14 pin on the unchanged writer (22-02)

Recorded 2026-10-19. Scratch output is in `/tmp/gsd22/exec/` (`pin-tracer.log`, `pin-tracer.xml`, `pin-full.log`, `pin-full.xml`, `testunit-22-02.log`). The pin drives the real `ProxySinkTask.put` (shape A) or `ChunkFlusher.putDirect` (shape B), `Processing.doLogic`, the real `ClickHouseWriter.doInsert`, `doInsertBatch` and `doInsertRawBinary`, and the handler, through `ScriptedWriteFailureWriter` (a Mockito-free writer whose RowBinary hooks run the real `doWriteColValue`). It asserts outcomes and two reach markers only.

pin_class: WriterFailureClassificationUnitTest
pin_testcases: 184
pin_kinds: 21
pin_commit: 0590d20ce1bade8deac21ae73f954546185be91d
writer_unchanged: git diff --quiet fefd8648 HEAD -- src/main (exit 0)
testunit_22_02: Tests summary: 761 tests, 759 succeeded, 0 failed, 2 skipped
spotless_after_22_02: SPOTLESS-BASELINE-EQUAL
anchors_mirror_22_02: rows=59 bad=0

The tracer commit is `dbab8472` (INVALID_UUID only, 8 cases). `pin_testcases` is the count plan 22-03 must see unchanged: 168 matrix cases (21 kinds x clientV2 {false, true} x errorsTolerance {false, true} x 2 entries), 8 more for `SOCKET_EXCEPTION` with `retryOnSocketException=true`, and 8 on the ignorePartitions route (INVALID_UUID and SOCKET_TIMEOUT). The full testUnit count is the 577 of E-01 plus these 184. No OpenJ9 crash occurred in any of the Gradle runs of this plan, so no core dumps needed cleanup.

Outcome table (what the pin asserts today; the outcome is the same for clientV2 false and true and for both entries; `SOCKET_EXCEPTION` becomes RETRIABLE in both columns when `retryOnSocketException=true`):

| Kind | RetryClass | tolerance none | tolerance all |
|------|------------|----------------|---------------|
| INVALID_UUID | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| UNPARSEABLE_INT128 | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| INT128_TOO_WIDE | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| DECIMAL_OUT_OF_RANGE | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| FIXED_STRING_TOO_LONG | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| UNKNOWN_ENUM_LABEL | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| JSON_COLUMN_FLAG_OFF | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| ARITHMETIC_SCRIPTED | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| NESTED_EXACT_RUNTIME_SCRIPTED | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| NESTED_DATA_EXCEPTION | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| IO_PLAIN | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| IO_NO_MESSAGE | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| IO_READ_TIMEOUT_PREFIX | RETRIABLE | RETRIABLE | RETRIABLE |
| IO_WRITE_TIMEOUT_PREFIX | RETRIABLE | RETRIABLE | RETRIABLE |
| IO_TIMEOUT_TEXT_NOT_AT_START | NON_RETRIABLE | RUNTIME_WRAP | RETURNS |
| SOCKET_EXCEPTION | SOCKET | RUNTIME_WRAP | RETURNS |
| SOCKET_TIMEOUT | RETRIABLE | RETRIABLE | RETRIABLE |
| UNKNOWN_HOST | RETRIABLE | RETRIABLE | RETRIABLE |
| IO_IN_MAP_VALUE | RETRIABLE | RETRIABLE | RETRIABLE |
| IO_IN_TUPLE_FIELD | RETRIABLE | RETRIABLE | RETRIABLE |
| IO_IN_ARRAY_ELEMENT | RETRIABLE | RETRIABLE | RETRIABLE |

The DLQ count is 3 (the batch size) whenever the outcome is RETURNS and 0 otherwise. `NESTED_DATA_EXCEPTION` is RUNTIME_WRAP, not DATA_EXCEPTION, because `Processing.doInsert` wraps every writer failure in a `RuntimeException` before the handler's `e instanceof DataException` test, so that branch never fires for writer failures. The `DATA_EXCEPTION` outcome stays in the enum and is observed by no case.

Recipes: no recipe was adjusted from 22-RESEARCH. The nested IO byte budgets were confirmed with a scratch write of the same Map and Array values into a counting stream (Map writes 1, 1, 1 and then the 4-byte Int32; Array writes 1 and then the 4-byte Int32), so the failure lands on the value, as intended. The scratch probe was deleted and never committed.

Shape inventory (every route from the writer to the handler):

- Shape A: `ProxySinkTask.put` catches what `Processing.doLogic` throws (the `RuntimeException(queryId.toString(), e)` wrap) and calls the handler once, then reports to the DLQ itself when tolerance is on. Entry `PROXY_SINK_TASK_PUT`.
- Shape B: `ChunkFlusher.putDirect` calls `ProxySinkTask.put` and runs the handler a second time over what A threw. The Direct, AtLeastOnceBuffer and ExactlyOnceBuffer delivery strategies all end in `putDirect` (22-RESEARCH). Entry `CHUNK_FLUSHER_PUT_DIRECT`.
- ignorePartitions overload: with `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, `Processing.doLogic` takes the second `doInsert` overload, which wraps the same way. Pinned for INVALID_UUID and SOCKET_TIMEOUT across both tolerances and both entries.
- Modeled V2 client wrap: for `clientV2=true` the scripted hook wraps a `RuntimeException` once in `com.clickhouse.client.api.ClickHouseException` with the original as cause (the shape measured in 22-RESEARCH D-13) and passes an `IOException` through. The real client also keeps an `IOException` as the deepest cause, so root-cause classification is the same. Plan 22-04's live V2 test exercises the real client.

### E-03: WRT-13 red-before and green-after, the D-03 result and the WRT-15 commit (22-03)

Recorded 2026-10-19. Scratch output is in `/tmp/gsd22/exec/` (`tracer-red.log`, `tracer-red-wvf.xml`, `tracer-red-wlm.xml`, `tracer-green.log`, `tracer-green-pin.xml`, `expansion-red.log`, `expansion-red.xml`, `expansion-green.log`, `f2-forced-red.log`, `f2-forced-red.xml`, `f2-live.log`, `f2-live.xml`, `testunit-22-03.log`, `gate-22-03.out`). The red-before runs of the new cases used a throwaway `git worktree` of the last committed tree (the unchanged writer), removed afterwards (`git worktree list` shows one entry).

d03_result: one value-carrying exact RuntimeException was reachable (Column.convertEnumValues, F1), fixed by D-14; none in clickhouse-data or JDK code (javap: BinaryStreamUtils has none, ClickHouseChecker throws IllegalArgumentException only)
tracer_red: WriterValueFreeFailureUnitTest red 4 of 4 total on the unchanged writer; wlm_red=1 of 10 (only the flipped method)
expansion_red: 21 cases, 13 red; WRT-13 value-free 10 of 10 red, D-04 V1 2 of 2 red, D-14 1 of 1 red; WRT-13 pass-through 6 of 6 green, D-04 V2 unchanged 2 of 2 green
green_after: TRACER-GREEN EXPANSION-GREEN
pin_unchanged_since: 0590d20ce1bade8deac21ae73f954546185be91d
pin_testcases_after: 184
f2_forced_red: tests=1 failures=1 type=org.opentest4j.AssertionFailedError (the unchanged assertion red on the changed writer, message naming the assertion text and WithheldMessageException; D-05 forces the flip)
f2_live: tests=1 failures=0
anchors_mirror_22_03: rows=65 bad=0
spotless_after_22_03: SPOTLESS-BASELINE-EQUAL
testunit_22_03: Tests summary: 793 tests, 791 succeeded, 0 failed, 2 skipped
wrt13_commit: c713a8379b546a14d1cb4f6a574bb0278aadcfb0
wrt13_commit_files: 12
gate_22_03: invariants: provenance_paths=21 anchor_commits=420 anchor_rows=65 marker_commits=467 anchor_exempt=1 result=PASS

D-13 branch trace (22-RESEARCH, re-confirmed by the new cases): on both clients a conversion failure reaches the generic second catch of `doInsertRawBinary`, so the `Error inserting records` log carries the throwable of that catch. V2 throws the `com.clickhouse.client.api.ClickHouseException` insert wrapper synchronously with the writer exception as cause, and V1 throws the raw writer exception. The `ServerException` branch is unreachable for a client-side conversion failure, because the writer throws mid-stream before the server answers. The Docker-free cases assert this with a tripwire: no captured throwable text line starts with `com.clickhouse.client.api.ServerException`.

D-03 pass-through shapes pinned green on both the unchanged and the changed writer: the JSON hint, the unsupported JSON field type, a scripted L-12 RuntimeException (same instance), a stream IOException (same instance), an IOException inside a Tuple field (exact RuntimeException wrapper with the same cause) and the WRT-08 DataException.

Residual (flagged assumption A2): an IOException chain passes through unchanged under the locked D-01, including a Jackson `JsonProcessingException` on the JSON-as-string route. Whether a Jackson reference chain can name a record's map key was not measured. It is recorded in FORK-DELTA Known costs item 6 and is not fixed here.
