---
phase: 27-writer-failure-path-test-debt
verified: 2026-10-23T02:00:00Z
status: human_needed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/27-writer-failure-path-test-debt/27-01-PLAN.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-01-SUMMARY.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-02-PLAN.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-02-SUMMARY.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-03-PLAN.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-03-SUMMARY.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-04-PLAN.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-04-SUMMARY.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-05-PLAN.md"
  - ".planning/phases/27-writer-failure-path-test-debt/27-05-SUMMARY.md"
  - "CHANGELOG.md"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/SourcePlanningIdGuardUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterColumnContextTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java"
covered_digest: "v3:sha256:fc94cd2a51aedf0789558df891c6a7dd4795115b2278c05fb56e557ccf446cb3"
behavior_unverified: 0
overrides_applied: 1
overrides:
  - must_have: "With a sentinel Map key on the JSON-as-string route, the key reaches none of the task status, the DLQ stack-trace header and the Error inserting records throwable; the exception sentence in FORK-DELTA Known costs item 6 is gone"
    reason: "Narrowed by user decision: a Jackson exception whose cause chain also holds a non-Jackson IOException (jackson_over_io, unchecked_io) still passes through unchanged so a timeout beneath it stays retriable (D-02). Pinned as accepted residual by same-instance rows in WriterValueFreeFailureUnitTest and documented in Known costs item 6, the CHANGELOG bullet and the ledger. ROADMAP.md text was not edited."
    accepted_by: "user (answer 'Accept residual, proceed.' recorded in 27-02-PLAN must_haves and 27-COMMAND-EVIDENCE E-03 residual_signoff)"
    accepted_at: "2026-10-22"
human_verification:
  - test: "Read the E-13 'assertion diff check' and 'predicate changes to existing assertions' tables in 27-COMMAND-EVIDENCE.md (the end-of-phase human check declared in 27-05-PLAN.md <verify><human-check>)."
    expected: "Every removed src/test line is one of the sanctioned items (JsonAsStringKeyResidualLiveTest flip and scrub, NO_ENUM_CONTENT_MESSAGE text, FixedString assertThrows, CountingHelperClient counter, NullableTupleMatrixLiveTest premise lines); E-04 holds the user's own answer; assertJacksonRouteReached still requires the Jackson class identity; every other changed predicate is a signed-off flip or a stricter check."
    why_human: "The plan makes this a human sign-off on 'no existing assertion deleted or weakened' (T-27-17). A diff cannot judge intent or whether the widened predicate is acceptable; it is also still listed as open."
  - test: "Decide WR-01 from 27-REVIEW.md: keep IllegalArgumentException (D-06) or throw a ConnectException subtype so the column-named lookup-miss text reaches the operator."
    expected: "A recorded decision. Today the operator sees only 'Failed to write column `c`: ... ClickHouse type Enum8: java.lang.IllegalArgumentException'; the 'Enum label not found for column c' text is withheld by WithheldMessageException on the writer path."
    why_human: "The criterion as written is met, but operator actionability (AGENTS.md error-message rule) is a product decision that reopens locked D-06."
  - test: "Judgment-tier prohibitions in the plans (e.g. 27-03 'MUST NOT edit ClickHouseWriter', 27-04 'comment-only scrub commits', 27-02 'MUST NOT wrap the Jackson-over-timeout pass-through') carry only a non-authoritative judge verdict here."
    expected: "Human review recommended: unverified-prohibition flag. Spot evidence: 27-03 fix commit touches only Column.java under src/main; both scrub commits show zero non-comment changed lines under src/main."
    why_human: "Judgment-tier prohibitions are soft-gated and must not be silently passed."
---

# Phase 27: Writer Failure Path & Test Debt Verification Report

**Phase Goal:** On the opt-in JSON-as-string route, a Jackson failure no longer carries a Map key into the task status, the DLQ stack-trace header or the `Error inserting records` throwable; a timeout beneath it stays retriable; an unknown Enum label names its column instead of a `NullPointerException`; the remaining 22-REVIEW and 24-REVIEW advisories and carried test-debt items are closed with no existing assertion deleted or weakened.
**Verified:** 2026-10-23
**Status:** human_needed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `WriterFailureClassificationUnitTest` gains Jackson alone, Jackson over `SocketTimeoutException`, Jackson over timeout-text `IOException` and plain `IOException` rows, green on the unchanged writer; existing 184 cases byte-identical | VERIFIED | Diff vs phase base `b8f67b3b` removes 0 lines (`git diff -U0 ... \| grep -c '^-'` = 0). The four enum constants `JACKSON_ALONE`, `JACKSON_OVER_SOCKET_TIMEOUT` (RETRIABLE), `JACKSON_OVER_TIMEOUT_TEXT` (RETRIABLE), `PLAIN_IO_CONTROL` are present. E-02 shows 216 = 184 + 32 green with `src/main` identical to base at the time of the test commit `108f79a5`. I re-ran the class: 309 targeted unit tests, 0 failed |
| 2 | Sentinel Map key reaches none of task status, DLQ stack-trace header, `Error inserting records` throwable; `JacksonException` excluded link by link; timeout beneath stays retriable; live test flipped with flip seen red; Known costs item 6 exception sentence gone | VERIFIED (override: narrowed by user decision) | `ClickHouseWriter.passesThroughUnchanged` (line 653-664) walks the chain and counts only `IOException && !(JacksonException)`. `JsonAsStringKeyResidualLiveTest` now asserts key-absent for all three destinations with the withheld-Jackson line present; I re-ran it live against ClickHouse 26.9.x: 4/4 pass. Red-first: E-03 `live_red` (2 failures carrying `SENTKEYa1b2`) and `predicate_red`. Timeout beneath Jackson: `JACKSON_OVER_*` RETRIABLE rows plus same-instance pass-through rows. Residual (Jackson over a non-Jackson `IOException`) stays pass-through by user decision of 2026-10-22; item 6 now documents that residual instead of the old Map-key exception. ROADMAP text itself was not edited |
| 3 | Unknown Enum label fails naming the column and a lookup-miss cause, not NPE; `No content in enum` names the column; both value-free; `Failed to write column ` prefix byte-identical | VERIFIED (criterion met as written; see WR-01) | `Column.convertEnumValues` (lines 530-538) throws `IllegalArgumentException("Enum label not found for column %s")` and `RuntimeException("No content in enum column %s")`. `ColumnTest` rows red in E-06, green now; `WriterValueFreeFailureUnitTest` pins the writer-level text `Failed to write column ...: ...Enum8: java.lang.IllegalArgumentException` with no label. Prefix constant `WRITE_FAILURE_COLUMN_PREFIX` = ``Failed to write column ` `` unchanged |
| 4 | Advisories closed: single metadata derivation; no planning IDs in `src/main` comments, live-test Javadoc and display names; FixedString test fails if `put` does not throw (seen red); `useClientV2Calls` removed or asserted; NullableTuple premise loops fail with a message; no assertion deleted or weakened; anchors invariant passes | VERIFIED (human sign-off item open) | Metadata: `sourceTypeName` / `valueClassName` helpers and `WRITE_TYPE_MISMATCH_FAILURE_FORMAT` shared by both messages (lines 666-677, 912). Grep of `src/main/java` and `JsonAsStringKeyResidualLiveTest` for planning-ID patterns: 0 hits; `SourcePlanningIdGuardUnitTest` 18/18 green. FixedString: `assertThrows` + `finally`, red in E-11 (old form passed with no throw). `grep -rn useClientV2Calls src` empty. NullableTuple: `assertNotNull` with message on premise row and three lookups, mutation-proven in E-12. Gate script at HEAD: `result=PASS` (anchor_rows=83); recorded PASS at each fix commit (E-05, E-07, E-10, E-13) |

**Score:** 4/4 truths verified, 0 present-but-behavior-unverified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ClickHouseWriter.java` | Jackson exclusion, shared metadata helpers | VERIFIED | Substantive and wired into `doWriteColValue` catch (lines 907-923) |
| `Column.java` | Lookup-miss IAE, column-named no-content text | VERIFIED | Called from the Enum8/Enum16 write path |
| `SourcePlanningIdGuardUnitTest.java` | Standing guard, seen red | VERIFIED | Red on unscrubbed tree (E-09), planted-ID mutation fails (E-10) |
| Test files (WFC, ValueFree, Column, ColumnContext, live, FixedString, NullableTuple, CountingHelperClient) | Pins and debt closure | VERIFIED | Present, run green |
| `docs/FORK-DELTA.md`, `CHANGELOG.md` | Item 6 and row text updated | VERIFIED | Item 6 and `doWriteColValue` row describe the closed route and the accepted residual |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `doWriteColValue` generic catch | `passesThroughUnchanged` | call at line 919 | WIRED | Non-pass-through becomes `DataException` + `WithheldMessageException` |
| `Column.convertEnumValues` | writer Enum8/Enum16 case | `doWritePrimitive` | WIRED | Exception converted by the generic catch |
| Jackson-excluded `IOException` | retry classification | `Utils.handleException` chain | WIRED | `JACKSON_OVER_*` rows RETRIABLE |

### Data-Flow Trace (Level 4)

Not applicable: no dynamic rendering artifacts; the failure text flows from `Column` metadata and class names only (value-free by construction).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Classification, value-free, Column, ColumnContext, planning-ID guard | `testUnit --tests` on the 5 classes `--rerun` | 309 tests, 0 failed | PASS |
| Live Jackson key pin and FixedString mismatch | `test --tests '*JsonAsStringKeyResidualLiveTest' --tests '*schemaWithFixedStringMismatchTest' --rerun` | 5 tests, 0 failed | PASS |
| Anchors invariant at HEAD | `verify-merge-invariants.sh` | `result=PASS` | PASS |
| Full suite | not re-run; E-13 records 1680 tests, 0 failures, 18 skips on `05ab0e3e` (no code change since) | accepted | PASS (recorded) |

### Probe Execution

Step 7c: no `probe-*.sh` declared by the phase. SKIPPED.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| WRT-16 | 27-01, 27-05 | Jackson-group classification rows, green first, 184 untouched | SATISFIED | Truth 1 |
| WRT-17 | 27-02, 27-05 | Per-link `JacksonException` exclusion, flip, item 6 | SATISFIED (narrowed by user decision) | Truth 2 |
| WRT-18 | 27-03, 27-05 | Enum lookup miss and no-content text name the column | SATISFIED | Truth 3 |
| WRT-19 | 27-02, 27-04, 27-05 | Metadata duplication removed, planning IDs scrubbed | SATISFIED | Truth 4 |
| DEBT-16 | 27-05 | FixedString, counter, premise loops | SATISFIED | Truth 4 |

No orphaned requirements: REQUIREMENTS.md maps exactly WRT-16..19 and DEBT-16 to Phase 27, all claimed by plan frontmatter.

### Anti-Patterns Found

None blocking. No TBD/FIXME/XXX introduced in changed files; stub scan not applicable.

### Review Findings

| ID | Severity | Status | Note |
|----|----------|--------|------|
| WR-01 | Warning | Open | See below |
| IN-01 | Info | Open | The Jackson exclusion lives at the call sites (innermost conversion), not in the predicate's first `if`; no test pins a `RuntimeException(JsonMappingException)` shape. Not reachable today |
| IN-02 | Info | Open | Class name `...ResidualLiveTest` is stale |
| IN-03 | Info | Open | Guard scope and prefix breadth |

**WR-01 against ROADMAP criterion 3:** The criterion reads "fails with a message that names the column and a lookup-miss cause, not with a `NullPointerException`". The writer surface is ``Failed to write column `c`: ... ClickHouse type Enum8: java.lang.IllegalArgumentException``: the column is named, the cause class is a lookup-miss `IllegalArgumentException` instead of an NPE, and `Column` itself throws the column-named text (pinned in `ColumnTest`). So the criterion is met as written, and it is not a gap. The review is right on one point: the "Enum label not found" wording never reaches Connect status, DLQ or logs because `WithheldMessageException` withholds the cause message, so an operator learns only the exception class. The plan locked this choice (D-06, ledger L-54) and flagged it to the user. It stays a WARNING with a human decision (item 2 above).

### Human Verification Required

1. **E-13 assertion-diff and predicate-changes tables** (27-05 `<human-check>`). Read both tables in `27-COMMAND-EVIDENCE.md` E-13; confirm E-04 holds your own answer `flip-as-shown`, that `assertJacksonRouteReached` still requires the Jackson class identity, and that rows (a) to (e) are signed-off flips or stricter checks. My independent diff read agrees with the table: the FixedString, NullableTuple and CountingHelperClient changes are stricter or remove an unused counter; the Enum text change is stricter.
2. **WR-01 decision**: keep D-06 (`IllegalArgumentException`) or switch to a pass-through `DataException`.
3. **Judgment-tier prohibitions**: non-authoritative; human review recommended.

### Gaps Summary

No gaps. All four ROADMAP success criteria hold in the codebase and under targeted re-runs. The status is `human_needed` solely because plan 27-05 declares an end-of-phase human check (still open) that Step 9 treats as a human-verification item, plus the open WR-01 decision and the soft-gated judgment prohibitions. Criterion 2 is carried as PASSED (override) on the user's recorded narrowing; consider editing the ROADMAP criterion text to match so the contract and the code agree.

---

_Verified: 2026-10-23_
_Verifier: Claude (gsd-verifier)_
