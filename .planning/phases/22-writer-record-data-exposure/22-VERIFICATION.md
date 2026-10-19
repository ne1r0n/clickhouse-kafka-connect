---
phase: 22-writer-record-data-exposure
verified: 2026-10-20T00:30:00Z
status: human_needed
score: 7/7 must-haves verified
covered_files:
  - .planning/phases/22-writer-record-data-exposure/22-01-PLAN.md
  - .planning/phases/22-writer-record-data-exposure/22-01-SUMMARY.md
  - .planning/phases/22-writer-record-data-exposure/22-02-PLAN.md
  - .planning/phases/22-writer-record-data-exposure/22-02-SUMMARY.md
  - .planning/phases/22-writer-record-data-exposure/22-03-PLAN.md
  - .planning/phases/22-writer-record-data-exposure/22-03-SUMMARY.md
  - .planning/phases/22-writer-record-data-exposure/22-04-PLAN.md
  - .planning/phases/22-writer-record-data-exposure/22-04-SUMMARY.md
  - .planning/phases/22-writer-record-data-exposure/22-05-PLAN.md
  - .planning/phases/22-writer-record-data-exposure/22-05-SUMMARY.md
  - docs/FORK-DELTA.md
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
  - src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java
  - src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
covered_digest: "v3:sha256:9aa465907da7690be7857c46b2ac8b27c5968de68652570b241c80ac4f4608ba"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Confirm the second flipped upstream test, ClickHouseSinkTaskWithSchemaTest.schemaWithFixedStringMismatchTest (ledger L-47)"
    expected: "The user accepts that assertInstanceOf(IllegalArgumentException.class, rootCause) became assertInstanceOf(WithheldMessageException.class, rootCause) plus assertEquals(IllegalArgumentException.class.getName(), withheld.getOriginalClassName()). I judge this equally strict: it still discriminates IllegalArgumentException from any other class, and the flip is forced by D-05, since the root cause can no longer be the original. The project rule (never delete or weaken existing tests) and CONTEXT's 'Flip, don't delete' approval name only the WriterLogMaskingUnitTest method, and no user response has been received."
    why_human: "Project policy on existing tests; the approval is not on record"
  - test: "Run a real Connect worker (connect-runtime is not on the test classpath) with a DLQ and a record with an invalid UUID string, then read the task status trace and the __connect.errors.exception.* headers"
    expected: "Task status trace and the exception.stacktrace header contain 'Failed to write column `<col>`', the ClickHouse type and the original exception class; the record value is in neither header, and the exception.message header is Processing's queryId text (D-15)"
    why_human: "Plan 22-04 assumptions A3 and A4: the Connect status rendering and DeadLetterQueueReporter's header algorithm are reproduced inside the tests, not run against the real runtime"
  - test: "Decide whether the Jackson residual (WR-01) is acceptable for the stated goal 'without ever carrying the record value'"
    expected: "Either accept it as an opt-in, narrowly reachable residual that FORK-DELTA item 6 already discloses, or schedule the one-line fix (exclude JsonProcessingException from the IOException pass-through) plus a sentinel-map-key test"
    why_human: "Scope and risk judgment on a locked decision (D-01); 22-REVIEW-DISPOSITION.md still has all six findings 'open'"
  - test: "Decide whether the HARN-08 wording 'overrides one level up, in the CapturingClickHouseWriter pattern' is met by a Mockito-free ClickHouseHelperClient subclass (F7 reading)"
    expected: "Accept the F7 reading (operative clauses: no Mockito, OpenJ9 item closed) or ask for a writer-level counting seam, which would need a production change"
    why_human: "Requirement wording departs from the delivered design"
---

# Phase 22: Writer Record-Data Exposure Verification Report

**Phase Goal:** A value-conversion failure in the writer tells operators which column, ClickHouse type and exception class failed, in the Connect task status, the DLQ headers and the ERROR logs, without ever carrying the record value. Retry classification is provably unchanged. This closes backlog 999.3 and FORK-DELTA Known costs item 6.
**Verified:** 2026-10-20
**Status:** human_needed
**Re-verification:** No, initial verification

I did not take SUMMARY.md on trust. I read the production diff (`git diff fefd8648 HEAD -- src/main`), the tests that guard it and the docs diff. I re-ran 233 Docker-free tests and the anchors gate myself. I also re-read the XML results of the orchestrator's full suite. No gap blocks the goal. Four judgment items remain (see Human Verification Required).

## Goal Achievement

### Observable Truths (ROADMAP success criteria, then plan must-haves)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC1 | A Docker-free test pins `Utils.handleException`'s retriable vs non-retriable classification both ways for every failure kind reaching `doWriteColValue`'s generic catch, green before and after the writer change | VERIFIED | `WriterFailureClassificationUnitTest` has 184 cases over 21 kinds (non-retriable and retriable kinds), driven through the real `ProxySinkTask.put`, `ChunkFlusher.putDirect`, `Processing` and `doInsertRawBinary`. E-02 shows it green with `git diff --quiet fefd8648 HEAD -- src/main` (writer unchanged) at commit 0590d20c. It is still 184/184 green after the fix commit c713a837 (XML `tests="184" failures="0"`). The fixtures were not edited after 0590d20c (E-03 `pin_unchanged_since`). I re-ran it: 233 of 233 pass in the batch. |
| SC2 | For an invalid UUID and an unparseable number, Connect task status, DLQ exception headers and both `Error inserting records` logs name the column, type and exception class, and the record value is absent from every cause-chain message and every captured log line | VERIFIED (with the D-15 and D-13 readings; see human items) | Code: `ClickHouseWriter.java:903-910` throws `DataException(buildValueConversionFailureMessage(...), WithheldMessageException.of(e))`. The message is built only from `col.getName()`, `Data.getFieldType()`, the value's class name, the `Type` and the exception class name. `WithheldMessageException` reads no original text, so `getMessage`, `toString` and the printed trace carry the fixed note and the class name. Tests: `WriterValueFreeFailureUnitTest` (21 cases; sentinel absent from the chain, the `printStackTrace` rendering and captured logs on V1 and V2; self-checks that the raw JDK message embeds the sentinel, so the absence check is not vacuous). `WriterErrorPathLiveTest` (10 tests: 6 original + 4 new, which is the real count; the plan's 7 was a miscount) puts an invalid UUID and an unparseable Int128 through `put()` on ClickHouse latest x V2. `ValueFreeDlqHeadersUnitTest` checks the DLQ headers. Readings: the DLQ `exception.message` header stays `Processing`'s queryId wrapper text and the column, type and class are in the `exception.stacktrace` header (D-15; `Processing` is untouched). The `ServerException` branch log of `doInsertRawBinary` is unreachable for a client-side conversion failure (D-13), so "both logs" is met by the generic-branch log plus the written trace. |
| SC3 | `ClickHouseWriterNullMessageUnitTest` no longer inline-mocks `ClickHouseHelperClient` with Mockito; every WRT-11 assertion is kept; the Phase 18 OpenJ9 `retransformClasses` item is closed | VERIFIED (F7 reading, human decision requested) | `grep -c org.mockito` is 0 in both the test class and `CountingHelperClient` (5 before). The diff against fefd8648 shows every `assertSame(failure, thrown)` and `hookCalls` assertion kept, `never().pinReplica()` became `assertEquals(0, chc.pinCalls())` and `times(1)` became `assertEquals(1, ...)` for pin and unpin. Names and `@DisplayName`s are unchanged. XML: tests=5, 0 failures. The delivered design is a Mockito-free `ClickHouseHelperClient` subclass, not a writer-level override. `doInsertRawBinary` calls `chc.pinReplica()` directly, so a writer-level counter would need a production change. |
| SC4 | FORK-DELTA item 6, the WRT-12 row, and the ROADMAP 999.3 entry change in the same commit as the writer code; the anchors invariant passes at every commit | VERIFIED | `git show --stat c713a837` lists `ClickHouseWriter.java`, `Column.java`, `WithheldMessageException.java`, `docs/FORK-DELTA.md`, `.planning/ROADMAP.md` (the 999.3 `Delivered:` line) and `fork-anchors.txt` in one commit. `src/main` changed in that commit only. I re-ran `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`: rc=0, empty stderr, `anchor_commits=435 anchor_rows=65 anchor_exempt=1 result=PASS`. |
| M1 | Retry classification is unchanged by construction | VERIFIED | `passesThroughUnchanged` (`ClickHouseWriter.java:643-653`) rethrows unchanged any `ConnectException`, any exact `RuntimeException` and any chain holding an `IOException`. `Utils.handleException` branches only on `ServerException`/`ClickHouseException`, `DataException && !errorsTolerance`, `SocketTimeoutException`, `UnknownHostException`, `SocketException` and `IOException` with the timeout prefixes. All of the retry shapes are `IOException`s and keep their identity. The remaining exceptions were non-retriable before and are non-retriable after (a `DataException` replaces the `IllegalArgumentException`). The pin shows the same outcome table before and after. |
| M2 | D-04: a record value containing `ClickHouseException: Code: 33` or `131` no longer triggers the V1 mapping refresh | VERIFIED | The replacement message holds no record text, so the `failureMessage::contains` match in `doInsertRawBinary` cannot fire on it. `WriterValueFreeFailureUnitTest` has the two V1 cases (red before, green after per E-03) and two V2-unchanged cases. |
| M3 | HARN-08 tracer: the class drives the real catch via `CountingHelperClient`; `git diff --quiet fefd8648 HEAD -- src/main` held up to plan 22-03 | VERIFIED | The class compiles Mockito-free and runs 5/5. The only `src/main` commit after fefd8648 is the intended c713a837. |

**Score:** 7/7 truths verified; 0 present-but-behavior-unverified (the Connect runtime rendering is routed to human as a separate item).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/.../util/WithheldMessageException.java` | Value-free copy of a cause chain | VERIFIED | 79 lines; suppression disabled, frames copied, cycle- and depth-bounded (32). Wired from `ClickHouseWriter`. 11 unit tests pass. |
| `ClickHouseWriter.java` generic catch | Value-free `DataException` | VERIFIED | Substantive, wired and exercised end-to-end by the live and Docker-free tests. |
| `Column.convertEnumValues` | Fixed text, no label | VERIFIED | `"No content in enum"`. |
| `CountingHelperClient` | Mockito-free test double | VERIFIED | Used by `ClickHouseWriterNullMessageUnitTest`. `useClientV2Calls()` is never asserted (review IN-05). |
| `WriterFailureClassificationUnitTest`, `WriterValueFreeFailureUnitTest`, `ValueFreeDlqHeadersUnitTest`, `WriterErrorPathLiveTest` (new cases), `WithheldMessageExceptionUnitTest` | Pins and sentinel tests | VERIFIED | All present and green. |
| `docs/FORK-DELTA.md`, `ROADMAP.md` 999.3 | Same-commit doc edits | VERIFIED | Present in c713a837. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `doWriteColValue` generic catch | `Processing.doInsert` wrap, `Utils.handleException`, DLQ reporter, `doInsertRawBinary` logs | `DataException` plus `WithheldMessageException` cause | WIRED (pinned by the classification matrix and the live put() tests) |
| `ClickHouseWriterNullMessageUnitTest` | `doInsertRawBinary` | reflection into the `chc` field | WIRED |

### Data-Flow Trace (Level 4)

Not applicable to dynamic rendering. The relevant flow is the exception text, traced from the throw site to Connect status (live `put()` test), the DLQ headers (header-algorithm reproduction) and captured log events (`LogCapture`).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 22 Docker-free classes | `./gradlew testUnit --rerun --tests` on the six classes | 233 tests, 233 succeeded, 0 failed | PASS |
| Full suite on latest x V2 (orchestrator run) | read `build/test-results/test/*.xml` | 90 files, 1231 tests, 18 skipped, 0 failures, 0 errors; the seven Phase 22 classes present; no `src`/`build.gradle.kts` change since | PASS |
| Anchors invariant | `verify-merge-invariants.sh 54e63648 HEAD` | rc=0, `result=PASS` | PASS |
| Jackson residual measured (scratch, not committed) | `ObjectMapper.writeValueAsBytes(Map{"SENTINEL_KEY"->unserializable bean})` | `JsonMappingException`: `(through reference chain: java.util.HashMap["SENTINEL_KEY"]->...)`. The Map key appears in the message. | CONFIRMS WR-01 |

### Probe Execution

SKIPPED: the phase declares no probes.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| HARN-08 | 22-01 | Null-message test off Mockito, assertions kept, OpenJ9 item closed | SATISFIED (F7 reading) | See SC3 |
| WRT-13 | 22-03, 22-04 | Value-free failure in Connect status, DLQ headers and ERROR logs naming column, type and class | SATISFIED (D-13 and D-15 readings; A3 and A4 routed to human) | See SC2 |
| WRT-14 | 22-02 | Classification unchanged and pinned by a Docker-free test before and after | SATISFIED | See SC1 and M1 |
| WRT-15 | 22-03 | FORK-DELTA item 6, WRT-12 row and ROADMAP 999.3 change in the same commit | SATISFIED | See SC4 |

All four IDs are claimed by plan frontmatter and mapped to Phase 22 in REQUIREMENTS.md and the traceability table. No orphaned requirement.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ClickHouseWriter.java` | 643-653 | Blanket `IOException` pass-through also admits Jackson `JsonProcessingException` (review WR-01) | WARNING | A Map key can reach status, DLQ trace and logs on the opt-in `binaryFormatWrtiteJsonAsString` route when a nested value fails to serialize. See the assessment below. |
| `Column.java` / `ClickHouseWriter.java` | 524-530, ~1040 | Unknown enum label reports `NullPointerException`; `No content in enum` drops the column name (IN-01, IN-02) | INFO | Diagnosability only; no leak. |
| `ClickHouseSinkTaskWithSchemaTest.java` | ~908-915 | Assertions only inside `catch`, no `fail()` (IN-04) | INFO | Pre-existing structure; the flip did not weaken it, but the test passes vacuously if `put` stops throwing. |
| `ClickHouseWriter.java` | 660, 894 | Duplicated type and class derivation (IN-03); planning IDs in `src/main` comments and an unused counter (IN-05) | INFO | Style. |
| TBD/FIXME/XXX | - | None added in `src` | - | No debt-marker blocker. |

### WR-01 assessment (reported accurately)

The goal says "without ever carrying the record value". For the two cases the success criteria name, an invalid UUID and an unparseable number, and for every `doWritePrimitive`, date, decimal, `FixedString` and enum failure, the claim holds and is tested. The pass-through is a deliberate design decision (D-01) made to keep stream and timeout failures retriable.

I confirmed the review's mechanism with a scratch Jackson run: a failure inside a nested serializer yields a `JsonMappingException` whose message ends with `(through reference chain: java.util.HashMap["<map key>"]->...)`. It is an `IOException`, so it passes through unchanged. A map key is record data. Reaching it needs three conditions together:
- the opt-in `binaryFormatWrtiteJsonAsString` JSON column route;
- a `Map` inside a `Struct` that holds a value Jackson cannot serialize (Connect's normal value types serialize fine);
- an error tolerance setting that does not hide the failure.

It leaks keys, not field values, and is hard to trigger in practice.

So the residual is real, narrow and disclosed. FORK-DELTA item 6 says "whether a Jackson reference chain ... can name a record's map key was not measured". My measurement above answers it: yes, it can. The item 6 wording ("T-18-07 is closed") is true for conversion failures but overstated for this one sub-path. I do not mark SC2 failed, because SC2 names the UUID and number cases. I do not call the goal's "ever" fully proven either. Recommended: a one-line change that excludes `JsonProcessingException` from the pass-through, plus a sentinel-map-key test, or an explicit sentence in FORK-DELTA item 6 recording that the residual was measured and exists. Nothing else in the cause-chain analysis depends on Jackson exceptions being treated as `IOException`, so the retry classification cost is nil. It is for the user to decide: D-01 is a locked decision, and the disposition ledger still lists WR-01 as `open`.

### Test-weakening check (ledger L-47)

`schemaWithFixedStringMismatchTest` before: `assertInstanceOf(IllegalArgumentException.class, Utils.getRootCause(e), msg)`. After: `assertInstanceOf(WithheldMessageException.class, root, msg)` plus `assertEquals("java.lang.IllegalArgumentException", root.getOriginalClassName(), msg)`. The exact original class is still pinned, and a wrong class would fail it, so the check is equally strict and arguably stronger. It is forced: D-05 makes the old assertion red (E-03 `f2_forced_red`). It is still a modification of an upstream test, and the user has not confirmed it. That is recorded as a human item, not as given. The `WriterLogMaskingUnitTest` flip is covered by CONTEXT's approval; it keeps a raw-JDK self-check and adds the absence assertions.

### Human Verification Required

1. **Confirm the L-47 flip** of `ClickHouseSinkTaskWithSchemaTest.schemaWithFixedStringMismatchTest`. **Expected:** user accepts the replacement assertion described above. **Why human:** project rule on existing tests, with no recorded approval.
2. **Real Connect runtime check (A3, A4).** Run a worker with a DLQ and an invalid-UUID record. **Expected:** the task-status trace and the `exception.stacktrace` header name the column, type and class; neither header holds the value; `exception.message` is the queryId text. **Why human:** `connect-runtime` is not on the test classpath, so the tests reproduce the rendering algorithm rather than run it.
3. **Decide on WR-01** (above): accept as a disclosed residual or schedule the fix. **Why human:** locked-decision scope call; ledger still `open`.
4. **Accept the HARN-08 F7 reading** of "overrides one level up". **Why human:** the delivered design (helper-client subclass) differs from the requirement's literal wording.

Also for the closing record: ROADMAP still shows Phase 22 as "In Progress" in the progress table and as unchecked in the milestone list. That is a close-out bookkeeping step, not a code gap.

### Gaps Summary

No blocking gaps. The code does what the goal states for the named failure kinds, retry classification is pinned unchanged by a 184-case matrix that is green before and after, the HARN-08 migration is real, and the same-commit doc edits and the anchors invariant hold. The status is `human_needed` rather than `passed` only because of the four judgment items above, chiefly the WR-01 residual (now measured) and the unconfirmed L-47 flip.

---

_Verified: 2026-10-20_
_Verifier: Claude (gsd-verifier)_
