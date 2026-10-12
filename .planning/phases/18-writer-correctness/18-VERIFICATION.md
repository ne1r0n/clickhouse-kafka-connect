---
phase: 18-writer-correctness
verified: 2026-10-12T18:10:00Z
status: human_needed
score: 4/6 must-haves verified
covered_files:
  - ".planning/phases/18-writer-correctness/18-01-PLAN.md"
  - ".planning/phases/18-writer-correctness/18-01-SUMMARY.md"
  - ".planning/phases/18-writer-correctness/18-02-PLAN.md"
  - ".planning/phases/18-writer-correctness/18-02-SUMMARY.md"
  - ".planning/phases/18-writer-correctness/18-03-PLAN.md"
  - ".planning/phases/18-writer-correctness/18-03-SUMMARY.md"
  - ".planning/phases/18-writer-correctness/18-04-PLAN.md"
  - ".planning/phases/18-writer-correctness/18-04-SUMMARY.md"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/util/Utils.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/CdcTestFixtures.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/EmptyCdcSkipRouteLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ExplicitJsonInsertPathLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/CapturingClickHouseWriter.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/EmptyCdcSkipUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/MissingNonNullableValueWriteTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/processing/EmptyCdcExactlyOnceStateTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java"
covered_digest: "v3:sha256:1014cd9c8db1ec19fc98c8c65c4d2a62449c7eacc29f1f0dd4d50a487bc7e4b9"
behavior_unverified: 1
overrides_applied: 0
behavior_unverified_items:
  - truth: "An all-empty Debezium batch (empty insert body) is accepted by client V1 and by request compression (LZ4/gzip), as FORK-DELTA and ledger row L-30 claim"
    test: "Run an all-empty CDC batch (op=t, op=u null after, op=d null before) through put() with CLIENT_VERSION=V1, and with compressClientRequest/LZ4 on V2"
    expected: "put() returns without exception and the table gains 0 rows"
    why_human: "The live tests ran client V2 uncompressed only (project constraint). The Docker-free tests replace doInsertJson/doInsertRawBinary, so the V1 piped stream and a zero-byte compressed body are never exercised. Presence checks cannot see whether the client accepts a zero-length body."
human_verification:
  - test: "All-empty CDC batch on client V1 and with request compression"
    expected: "put() succeeds and 0 rows land; if either errors, short-circuit the network insert in the writer when every record is skipped, or soften the FORK-DELTA/L-30 wording to 'client V2, uncompressed, verified'"
    why_human: "Not exercised by any test (see behavior_unverified_items; review finding WR-04)"
  - test: "Upgrade carry-over for a batch left BEFORE_PROCESSING by the previous release (backstop truth, ledger L-30)"
    expected: "The batch is retried with the same insert_deduplication_token; whether its earlier phantom rows remain depends on whether the first attempt landed. Confirm this is stated in Phase 20's rollout note"
    why_human: "Plan 18-03 tags this truth verification: backstop (documented, deliberately not tested). It is non-inferable from the code, so the verifier abstains. The documentation half is present in 18-BEHAVIOUR-LEDGER.md L-30."
---

# Phase 18: Writer Correctness Verification Report

**Phase Goal:** Four `ClickHouseWriter` defects are fixed without disturbing exactly-once: empty Debezium records no longer write phantom rows on any route, a missing non-nullable value names its column, a message-less exception no longer turns into a `NullPointerException`, and DateTime parse failures stop logging record data.
**Verified:** 2026-10-12T18:10:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

All four defects are fixed in `src/main`, and every behavior I could exercise passes. Two items are left for a human decision. Neither is a gap in the four fixes.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1 (JSONP-09): an empty Debezium record inserts no row on explicit `insertFormat=json`, upstream `bypassRowBinary=true` JSON and RowBinary. Offsets and exactly-once state advance, an all-empty batch succeeds, and the skip is decided in the writer so a retried batch is byte-identical | ✓ VERIFIED (client V2) | `skipEmptyCdcRecord` (ClickHouseWriter.java:1296) is a pure function of the `Record`. It is called in `writeRecordsJson` (1405, serves explicit and bypass) and in the RowBinary loop (1321), and an anchor pins 3 occurrences. I ran `EmptyCdcSkipRouteLiveTest` (4 routes), `ExplicitJsonInsertPathLiveTest`, `EmptyCdcSkipUnitTest` and `EmptyCdcExactlyOnceStateTest` against ClickHouse `latest` with client V2: all passed. The live tests read back typed rows (2 real rows after the mixed batch, unchanged after the all-empty batch, 3 after the empty-first batch). I removed both call sites in the working tree: 18 of 23 tests failed, covering all four routes, then I restored the file (`git diff --quiet -- src/main` is clean). The state test passed with `Processing` ending AFTER_PROCESSING over the full range |
| 2 | SC2 (WRT-10): a missing non-nullable column with no default, including a field in `Array(Tuple(...))`, fails with a `DataException` naming the column and its type, with no record value | ✓ VERIFIED | `doWriteCol` (ClickHouseWriter.java:1109) throws `DataException(String.format(MISSING_NON_NULLABLE_MESSAGE_FORMAT, name, renderColumnType(col)))`. The old message-less `RuntimeException` and the ERROR line are gone. The "present null" path keeps upstream's `RuntimeException` (lines 1073 and 1085). `MissingNonNullableValueWriteTest` (flat, Array(Tuple), empty array, default/Nullable/Variant bytes, defaultsSupport true and false) and the live `Array(Tuple)` case in `WriterErrorPathLiveTest` passed. Note: "ClickHouse type" is the connector's rendered name (`INT32`), a choice locked in D-10 and written into the ledger and FORK-DELTA |
| 3 | SC3 (WRT-11): a null-message exception on `ClickHouseWriter`'s error path surfaces as itself. Classification is unchanged. A Docker-free test pins both | ✓ VERIFIED | `doInsertRawBinary` reads `e.getMessage()` once and treats null as no match. `Utils.handleException` checks `msg != null` before the timeout test. `ClickHouseWriterNullMessageUnitTest` (V1 and V2 hooks, Code 33 retry, empty message) and `UtilsNullMessageClassificationUnitTest` passed. `WriterErrorPathLiveTest` passed for `errors.tolerance=none` and `all`, for `RetriableException` on a message-less `SocketTimeoutException`, and for the IOException cases. Only the message-less cases change outcome (ledger L-23, L-24); non-null messages are untouched |
| 4 | SC4 (WRT-12): an unparseable `DateTime`/`DateTime64` string logs column, ClickHouse and Kafka types, exception class and value length. A log-capture test shows the raw value appears neither directly nor via the parse exception. `docs/FORK-DELTA.md` has a row | ✓ VERIFIED | `logDateTimeParseFailure` (ClickHouseWriter.java:535) logs metadata only and reduces the exception to its class name. Both DateTime branches use it. The generic catch (871) logs the class name with no throwable. `WriterLogMaskingUnitTest` (sentinel absence across message, throwable and cause chain, plus an exact-line presence check, empty value, UTF-16 length of 18) passed. FORK-DELTA rows exist at lines 40 and 41, and Known costs item 6 records the residual |
| 5 | Backstop (JSONP-09 concurrency): a batch left BEFORE_PROCESSING by the previous release and retried by this one is documented, not tested | ? UNCERTAIN (insufficient_spec) | Plan 18-03 tags it `verification: backstop`. The documentation half exists (ledger L-30). The behavior itself is non-inferable, so I abstain. See human item 2 |
| 6 | Plan 18-03/18-04 truth: the all-empty batch completes on client V1 and the insert hook still opens with an empty body | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED | The V2 uncompressed path passes live. No test runs V1 or request compression with a zero-byte body (review finding WR-04). Code is present and wired. See human item 1 |

**Score:** 4/6 truths verified (1 present, behavior-unverified; 1 abstained as non-inferable)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `sink/db/ClickHouseWriter.java` | skip helper, `DataException`, null-safe catch, masked logs | ✓ VERIFIED | Substantive and wired. Diff against the phase start shows exactly these edits. `shouldSkipTableMetadata()` is still at 8 occurrences |
| `util/Utils.java` | null-safe IOException branch | ✓ VERIFIED | One-line guard at line 123 |
| `docs/FORK-DELTA.md` | rows for each patch, Known costs updated | ✓ VERIFIED | Rows at lines 40, 41, 42, 43, 44 and 51. Items 2, 3 and 6 updated |
| `18-BEHAVIOUR-LEDGER.md` | rows L-23 to L-30 | ✓ VERIFIED | All eight rows present |
| `.planning/scripts/fork-anchors.txt` | Phase 18 anchor rows | ✓ VERIFIED | 10 Phase 18 rows (59-70); `verify-merge-invariants.sh 54e63648 HEAD` printed `result=PASS` (anchor_rows=55) |
| 11 new/edited test files | one per claim | ✓ VERIFIED | All present and substantive, and all ran green. `ROADMAP.md` has the `### Phase 999.3:` backlog entry |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `doInsertBatch` | `writeRecordsJson` / RowBinary loop | `doInsertJson`, `doInsertRawBinary` | ✓ WIRED | Both routes reach the skip (ClickHouseWriter.java:238-241) |
| `skipEmptyCdcRecord` | `Processing` state | record list untouched upstream of the writer | ✓ WIRED | `EmptyCdcExactlyOnceStateTest` shows the full range and dedup token kept |
| `doWriteCol` throw | `put()` cause chain | `DataException` | ✓ WIRED | Live `Array(Tuple)` case sees `column 'at.a' (` in the chain |
| `Utils.handleException` | DLQ / `RetriableException` | tolerance handling | ✓ WIRED | Live tests cover both tolerance settings |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| RowBinary / JSON loops | `record.getJsonMap()` | `Record.convert` from `SinkRecord` | Yes (live read-back equals source) | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free Phase 18 tests | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun-tasks --tests '*ClickHouseWriterNullMessageUnitTest' --tests '*UtilsNullMessageClassificationUnitTest' --tests '*WriterLogMaskingUnitTest' --tests '*EmptyCdcSkipUnitTest' --tests '*EmptyCdcExactlyOnceStateTest'` | 45 tests, 45 succeeded | ✓ PASS |
| Live tests on ClickHouse `latest`, client V2 | `... --tests '*EmptyCdcSkipRouteLiveTest' --tests '*WriterErrorPathLiveTest' --tests '*MissingNonNullableValueWriteTest' --tests '*ExplicitJsonInsertPathLiveTest'` | 24 tests, 24 succeeded | ✓ PASS |
| Mutation: remove both skip call sites | same skip tests, working tree edited, then restored | 18 of 23 failed, all four routes red; `git diff --quiet -- src/main` clean after | ✓ PASS (tests are not vacuous) |

I did not rerun the 902-test full suite. I took the 18-04 gate's result from the orchestrator and the SUMMARY, and checked the phase tests above directly.

### Probe Execution

| Probe | Command | Result | Status |
|-------|---------|--------|--------|
| merge invariants | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` | `invariants: provenance_paths=21 anchor_commits=248 anchor_rows=55 marker_commits=295 result=PASS` | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| JSONP-09 | 18-03, 18-04 | Empty CDC record skip on all routes | ✓ SATISFIED (V2) | Truth 1; V1/compression part is human item 1 |
| WRT-10 | 18-01, 18-04 | Column-naming `DataException` | ✓ SATISFIED | Truth 2 |
| WRT-11 | 18-01, 18-04 | Null-message guard | ✓ SATISFIED | Truth 3 |
| WRT-12 | 18-02, 18-04 | DateTime parse log masking | ✓ SATISFIED | Truth 4 |

All four IDs appear in plan frontmatter and in REQUIREMENTS.md (lines 25-28 and the traceability table, marked Complete). There are no orphaned requirements: REQUIREMENTS.md maps no other ID to Phase 18.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| ClickHouseWriter.java | 868-872, 1109 | Rethrown exception message can still carry a record value (UUID, numeric parse) to Connect status, DLQ headers and the `Error inserting records` logs | ⚠️ Warning (disclosed) | The phase does not claim to close this. It is recorded in FORK-DELTA item 6 and backlog 999.3. Review WR-01 notes that the "closed" wording overstates it |
| WriterLogMaskingUnitTest.java | 56 | Raw non-ASCII emoji literal; `build.gradle.kts` sets no `options.encoding` | ℹ️ Info | Passes here. May fail under a non-UTF-8 default compile charset (review WR-03) |
| ClickHouseWriterNullMessageUnitTest.java | 118 | Inline Mockito mock of `ClickHouseHelperClient` | ℹ️ Info | Passed in every run I made; recorded in `deferred-items.md` as an intermittent OpenJ9 crash risk (review WR-02) |

No `TBD`, `FIXME` or `XXX` markers were added by the phase. The `TODO` lines in `ClickHouseWriter.java` and `Utils.java` are not in the phase diff. The 18-REVIEW findings (0 critical, 4 warning, 5 info) are all still open in `18-REVIEW-DISPOSITION.md`. They are advisory and do not change the status.

### Human Verification Required

#### 1. All-empty CDC batch on client V1 and with request compression

**Test:** Run an all-empty batch through `put()` with `CLIENT_VERSION=V1`, and on V2 with LZ4 or gzip request compression on.
**Expected:** `put()` succeeds and 0 rows land.
**Why human:** No test exercises a zero-byte body on those paths. If either errors, an all-empty Debezium batch would stall the partition under `errors.tolerance=none`. The fix is to skip the network insert when every record is skipped, or to soften the FORK-DELTA and L-30 wording to "client V2, uncompressed".

#### 2. Upgrade carry-over for a BEFORE_PROCESSING batch

**Test:** Confirm Phase 20's rollout note states that a batch left BEFORE_PROCESSING by the previous release is retried with the same dedup token, and that its phantom rows stay only if the first attempt landed.
**Expected:** The wording matches ledger row L-30.
**Why human:** The truth is tagged `backstop` (documented, not tested), so the verifier abstains.

### Gaps Summary

There are no blocking gaps. The four defects are fixed in the code and pinned by tests that fail when the fix is removed. Exactly-once is preserved: the skip runs inside the serialization loops, so offsets, `Processing` state and the dedup token cover the whole batch. The status is `human_needed` only because of the two items above.

---

_Verified: 2026-10-12T18:10:00Z_
_Verifier: Claude (gsd-verifier)_
