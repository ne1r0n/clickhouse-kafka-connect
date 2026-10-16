---
phase: 18-writer-correctness
plan: 01
subsystem: sink-writer
tags: [clickhouse-writer, error-handling, exactly-once-adjacent, null-message, DataException, fork-anchors]
requires:
  - phase: 17-ci-build-hygiene
    provides: green tests.yaml on ubuntu-24.04 and a stable test classpath
provides:
  - "doInsertRawBinary rethrows a message-less failure as itself (WRT-11)"
  - "Utils.handleException treats a null IOException message as not a timeout (D-05)"
  - "A missing non-nullable value throws a column-naming, data-free DataException; the old ERROR line is gone (WRT-10)"
  - "18-BEHAVIOUR-LEDGER.md with rows L-23..L-26"
  - "Three fork-anchors.txt blocks, three FORK-DELTA Connector source rows, Known costs items 3 and 5 updated"
affects: [18-02, 18-03, 18-04, 19-columns, 20-release]
plan_head_before: 2db8fa2221ef21be42dff61aae783e93afde1c07
plan_head_after: 78379fb7d6c95ccd8d71b1fc0c1f0562d8eca163
actuals:
  tokens: 15896
  tasks: 3
  commits: 3
tech-stack:
  added: []
  patterns:
    - "Reflective swap of Processing.dbWriter in a started ClickHouseSinkTask to inject a writer failure while the real catch, wrapping and Utils.handleException run"
    - "Same-instance dispatch: a test writer throws the stored exception itself so assertSame and cause-chain identity checks work"
key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MissingNonNullableValueWriteTest.java
    - .planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
key-decisions:
  - "ClickHouseWriter.java is already IS DIRTY under the Spotless IDE hook at the phase start (upstream-identical import order); it was not reformatted, to keep the fork rebasable"
  - "FORK-DELTA item 3 says the column shows in the Connect status trace and the DLQ stack trace, not the DLQ headers, because Processing wraps every write failure with the queryId as its message"
patterns-established:
  - "Every fix commit carries its code, tests, fork-anchors rows, FORK-DELTA row and ledger rows (D-13)"
requirements-completed: [WRT-11, WRT-10]
coverage:
  - id: D1
    description: "WRT-11: a message-less exception from the RowBinary hook is rethrown as itself (V1 and V2 hook), empty message rethrown, Code 33 retry-once unchanged"
    requirement: WRT-11
    verification:
      - kind: unit
        ref: "ClickHouseWriterNullMessageUnitTest (5 cases)"
        status: pass
    human_judgment: false
  - id: D2
    description: "WRT-11 through ClickHouseSinkTask.put(): tolerance none keeps the instance in the chain with no NPE, tolerance all fills the DLQ with the whole batch, a message-less SocketTimeoutException is retriable"
    requirement: WRT-11
    verification:
      - kind: integration
        ref: "WriterErrorPathLiveTest WRT-11 cases (V1 and V2)"
        status: pass
    human_judgment: false
  - id: D3
    description: "D-05: Utils.handleException treats a null IOException message as not a timeout; timeout prefixes stay retriable"
    requirement: WRT-11
    verification:
      - kind: unit
        ref: "UtilsNullMessageClassificationUnitTest (6 cases), UtilsTest (14 cases, unchanged)"
        status: pass
      - kind: integration
        ref: "WriterErrorPathLiveTest D-05 cases (V1 and V2)"
        status: pass
    human_judgment: false
  - id: D4
    description: "WRT-10: a missing non-nullable value throws a DataException naming the column (dotted path for a tuple field) and its renderColumnType type, with no record value; the ERROR line is gone"
    requirement: WRT-10
    verification:
      - kind: unit
        ref: "MissingNonNullableValueWriteTest (7 cases)"
        status: pass
      - kind: integration
        ref: "WriterErrorPathLiveTest WRT-10 case (V1 and V2)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Anchors gate: every fork-anchors row holds at HEAD and verify-merge-invariants.sh 54e63648 HEAD prints result=PASS"
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD"
        status: pass
    human_judgment: false
duration: 15min
completed: 2026-10-12
status: complete
---

# Phase 18 Plan 01: Null-message family and missing-value message Summary

**A message-less writer failure now surfaces as itself instead of a NullPointerException (RowBinary catch and `Utils.handleException`), and a missing non-nullable value throws a data-free `DataException` that names the column and its connector type, in three self-contained fix commits that each carry their own anchors, FORK-DELTA row and ledger rows.**

## Performance
- **Duration:** 15 min
- **Started:** 2026-10-12T13:27:08Z
- **Completed:** 2026-10-12T13:42:28Z (code and gate; SUMMARY and state follow)
- **Tasks:** 3
- **Files modified:** 9 (2 in `src/main`, 4 new test classes plus one extended, `fork-anchors.txt`, `FORK-DELTA.md`, the new ledger)

## Accomplishments
- WRT-11: `doInsertRawBinary`'s second catch reads the message once (`String failureMessage = e.getMessage();`); a null message is no match. Non-null behaviour, including the Code 33 and Code 131 refresh-and-retry-once, is unchanged and pinned both ways.
- D-05: the IOException branch of `Utils.handleException` is guarded with `msg != null &&`; the diff against the phase start is exactly that one line.
- WRT-10: `MISSING_NON_NULLABLE_MESSAGE_FORMAT` and a `DataException` replace the message-less `RuntimeException` and the ERROR line in `doWriteCol`.
- `18-BEHAVIOUR-LEDGER.md` created with L-23..L-26; FORK-DELTA gained three Connector source rows in the required order, item 3 is closed and item 5 no longer lists WR-04 as open.

## Task Commits
1. **Task 1 (tracer): WRT-11** - `7cb69de2` (fix)
2. **Task 2: D-05** - `653a085e` (fix)
3. **Task 3: WRT-10** - `78379fb7` (fix)

**Plan metadata:** recorded in the docs commit that follows this file.

## Red evidence
- **WRT-11 unit (pre-fix):** both `WRT-11: clientV2=false` and `WRT-11: clientV2=true` of "a message-less RuntimeException from the RowBinary hook is rethrown as itself..." failed with `expected: <java.lang.RuntimeException> but was: <java.lang.NullPointerException: Cannot invoke "java.lang.String.contains(java.lang.CharSequence)" because the return value of "java.lang.Exception.getMessage()" is null>`. The empty-message, Code 33 retry=true and retry=false cases passed before the fix, as the plan expected (pins).
- **WRT-11 live (pre-fix):** two of three red, as expected:
  - `WRT-11: errors.tolerance=none surfaces a message-less writer failure as itself, with no NullPointerException in the chain` (injected instance absent from the chain)
  - `WRT-11: a message-less SocketTimeoutException from the writer makes put throw RetriableException` (was a plain RuntimeException)
  - `WRT-11: errors.tolerance=all sends the whole batch to the DLQ ...` passed before the fix (pin).
- **D-05 (pre-fix):** four red, NPE message `Cannot invoke "java.lang.String.indexOf(java.lang.String)" because "msg" is null`:
  - unit: `D-05: a message-less IOException with errorsTolerance=true returns normally`
  - unit: `D-05: a message-less IOException with errorsTolerance=false fails the task with a non-retriable RuntimeException that wraps the original`
  - unit: `D-05: a message-less IOException as the root cause of a wrapper keeps the original in the chain and raises no NullPointerException`
  - live: `D-05: errors.tolerance=none keeps a message-less IOException from the writer in the chain, with no NullPointerException`
  - The SocketTimeoutException, `Read timed out after` and `Broken pipe` unit cases and the live tolerance-all case passed before the fix (pins).
- **WRT-10 (pre-fix):** four of seven unit cases red (flat x2, Array(Tuple) x2, all `expected DataException but was RuntimeException`) and the one live case red (cause chain held only `RuntimeException`s).
- Semantic assessment: every red case failed on the planned assertion for the intended reason (NPE or wrong type replacing the original), not on a compile or fixture error.

## Observed live WRT-10 message
`column 'at.a' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT` (through `put()` against `Array(Tuple(a Int32, b String))`; the dotted path comes from DESCRIBE as research expected; 0 rows landed; the sentinel value appears in no chain message or log line).

## Test counts
- `WriterErrorPathLiveTest`: V1 `[6, 0, 0, 0]`, V2 `[6, 0, 0, 0]` (tests, failures, errors, skipped).
- `ClickHouseWriterNullMessageUnitTest` 5, `UtilsNullMessageClassificationUnitTest` 6, `MissingNonNullableValueWriteTest` 7, existing `UtilsTest` 14, `NestedNullableCompositeWriteTest` 18, `RowBinaryUnsupportedTypeGateTest` 11, `ColumnArrayRecursionTest` 9; all green with 0 failures, errors and skips.

## Gate result
- A4 fallback (drive `Processing.doLogic` directly) was **not** needed: the reflective swap of `Processing.dbWriter` worked on the first run.
- Anchors gate after Task 1 and at plan end: `invariants: provenance_paths=21 anchor_commits=235 anchor_rows=48 marker_commits=282 result=PASS`.
- `git log --format=%s 3b9c381b..HEAD -- src/main` shows exactly the three `fix(18-01):` subjects in the order WRT-11, D-05, WRT-10; `src/main` changed only in `ClickHouseWriter.java` and `Utils.java`; `VERSION`, `CHANGELOG.md`, `UtilsTest`, `InMemoryDLQ`, `NestedNullableCompositeWriteTest` and `NestedNullRowBinaryLiveTest` are unchanged.

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - null-safe failure-message lookup; column-naming `DataException` in `doWriteCol`
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` - null guard on the IOException timeout test
- `src/test/java/.../sink/db/ClickHouseWriterNullMessageUnitTest.java` - Docker-free same-instance and retry-once pins
- `src/test/java/.../sink/WriterErrorPathLiveTest.java` - put()-level pins for WRT-11, D-05, WRT-10
- `src/test/java/.../sink/util/UtilsNullMessageClassificationUnitTest.java` - Docker-free classification pins
- `src/test/java/.../sink/db/MissingNonNullableValueWriteTest.java` - Docker-free WRT-10 message pins
- `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`, `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` - bookkeeping

## Decisions Made
None beyond the plan. See Deviations for two judgement calls.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spotless IDE-hook gate cannot be IS CLEAN for `ClickHouseWriter.java`**
- **Found during:** Task 1 commit step
- **Issue:** The plan requires `IS CLEAN` for every touched Java file. `ClickHouseWriter.java` reports `IS DIRTY` on the unmodified phase-start content (piping `git show HEAD:...` through the hook gives the same result), because the file keeps upstream's import order. The only differences the hook wants are import lines and blank lines around them. `Utils.java` and all four new or extended test classes are `IS CLEAN`.
- **Fix:** Did not reformat `ClickHouseWriter.java`; reordering its imports would churn the file against upstream and hurt rebasability (project constraint). Verified that the hook's proposed diff touches only import lines, so my edits add no new formatting debt. The `IS CLEAN` clause of the `WRT11-COMMIT-OK` and `WRT10-COMMIT-OK` checks was therefore run for every file except `ClickHouseWriter.java`; all other clauses were run unchanged and passed.
- **Files modified:** none
- **Verification:** diff of the hook output against the file, filtered of import and blank lines, is empty

**2. [Rule 3 - Blocking] Plan verify script counts an empty string as a changed line**
- **Found during:** Task 2 verification
- **Issue:** The `Utils.java` one-line-diff check builds `ch` from `d.split("\n")` with `l[:1] in "+-"`. The trailing empty string satisfies `"" in "+-"`, so `len(ch)` is 3 for a correct two-line diff and the check fails.
- **Fix:** Ran the same check with empty strings filtered out (`l and l[0] in "+-"`). Result: exactly two changed lines, the second being the required guarded condition.
- **Files modified:** none

**3. [Rule 1 - Accuracy] FORK-DELTA item 3 wording**
- **Found during:** Task 3 bookkeeping
- **Issue:** The plan's closure text says the DLQ *headers* show which column failed. `Processing.doInsert` wraps every write failure as a `RuntimeException` whose message is the queryId, so the DLQ exception-message header carries the queryId and the column appears in the cause chain and stack trace. Plan row L-25 already says this.
- **Fix:** Item 3 reads "so the Connect status trace and the DLQ stack trace show which column failed (WRT-10)".
- **Files modified:** `docs/FORK-DELTA.md`

**Total deviations:** 3 (2 blocking-check adjustments, 1 accuracy). **Impact:** none on behaviour or scope; the first two adjust how the plan's own checks were evaluated, the third aligns one sentence with the ledger.

## Issues Encountered
- Ledger row L-24 was first written in the Task 1 file by mistake; it was removed again before the Task 1 commit and added in the Task 2 commit, so each commit carries only its own rows.
- `echo =======` in zsh errors out (`=` expansion); avoided by using quoted strings.

## Authentication Gates
None.

## Known Stubs
None.

## Threat Flags
None. Mitigations for T-18-01 (no record value in the WRT-10 text; sentinel absent from every chain message and log), T-18-02 (non-null classification pinned both ways) and T-18-03 (anchor rows per commit) are implemented and tested.

## Next Phase Readiness
Plans 18-02..18-04 can proceed. Phase 19 can rely on the column-naming `DataException` (`column 'at.a' (INT32): ...`). Phase 20 should note for the CHANGELOG and ROLL-02 that log alerts keyed on `is not nullable and no value is provided` must move to the new DataException text (L-26), and that the type in that message is the connector's name (`INT32`), not the ClickHouse spelling (L-25).

---
*Phase: 18-writer-correctness*
*Completed: 2026-10-12*

## Self-Check: PASSED

- Files: the four new test classes and `18-BEHAVIOUR-LEDGER.md` exist on disk.
- Commits: `7cb69de2`, `653a085e` and `78379fb7` are ancestors of HEAD; `gsd-tools check evaluation-scope --plan 18-01 --commits-only` resolved all three.
- Acceptance criteria and plan-level `<verification>` commands were re-run after the last commit and passed, apart from the two documented check adjustments above.
