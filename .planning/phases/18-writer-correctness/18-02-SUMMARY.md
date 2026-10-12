---
phase: 18-writer-correctness
plan: 02
subsystem: sink-writer
tags: [clickhouse-writer, log-masking, DateTime, DateTime64, reload4j, log-capture, fork-anchors, T-13-19]
requires:
  - phase: 18-writer-correctness
    provides: "18-01 ledger rows L-23..L-26, the column-naming DataException, Phase 18 anchor block"
provides:
  - "DateTime and DateTime64 parse-failure ERROR logs carry column, types, exception class and value length only (WRT-12, D-08)"
  - "The unsupported-conversion DataException names the column (D-08)"
  - "The generic doWriteColValue catch logs metadata with no throwable and rethrows the original (D-09)"
  - "LogCapture, a reusable reload4j log-capture test helper (D-12)"
  - "ROADMAP backlog 999.3 for the rethrown-message residual"
  - "Ledger rows L-27, L-28, L-29; six fork-anchors.txt rows; two FORK-DELTA Connector source rows; item 6 rewritten"
affects: [18-03, 18-04, 19-columns, 20-release]
plan_head_before: 0ced2194f3c0bfa75f5ee66404a29a182f27dd14
plan_head_after: 9f070f98a516c3e536155396a5061065abe27839
actuals:
  tokens: 10600
  tasks: 2
  commits: 2
tech-stack:
  added: []
  patterns:
    - "LogCapture: attach a reload4j appender to a class logger in try-with-resources, snapshot level, rendered message and throwable texts (every line of the string representation plus every message down the cause chain), restore the previous level on close"
    - "Sentinel masking test: a unique dash-free value, the exact masked line asserted present, the sentinel asserted absent from allTexts(), and the raw JDK exception asserted to embed the sentinel so the absence check is not vacuous"
key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/ROADMAP.md
    - .planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md
key-decisions:
  - "Value length is String.length() of the value's string form, in UTF-16 code units (the sentinel plus U+1F600 logs 18); stated in ledger row L-27"
  - "The rethrown-message residual (T-18-07) is recorded, not fixed: FORK-DELTA Known costs item 6, ROADMAP 999.3, and a test that pins the sentinel in the rethrown IllegalArgumentException message"
patterns-established:
  - "Static log formats in constants beside NESTED_NULL_MESSAGE_FORMAT; the exception reaches the logger only as its class name"
requirements-completed: [WRT-12]
coverage:
  - id: D1
    description: "WRT-12 / D-08: DateTime and DateTime64 parse failures log column, ClickHouse type, Kafka type, exception class and value length; no value and no parse message"
    requirement: WRT-12
    verification:
      - kind: unit
        ref: "WriterLogMaskingUnitTest (DateTime, DateTime64, empty string, UTF-16 length, null-value pin)"
        status: pass
    human_judgment: false
  - id: D2
    description: "D-08: the unsupported-conversion DataException keeps its prefix, adds the column and carries no value"
    requirement: WRT-12
    verification:
      - kind: unit
        ref: "WriterLogMaskingUnitTest conversion DataException case"
        status: pass
    human_judgment: false
  - id: D3
    description: "D-09: the generic doWriteColValue catch logs column, types and exception class with no throwable, and the original exception is rethrown unchanged (residual pinned)"
    requirement: WRT-12
    verification:
      - kind: unit
        ref: "WriterLogMaskingUnitTest UUID log, UUID rethrow and DateTime-through-doWriteColValue cases"
        status: pass
    human_judgment: false
  - id: D4
    description: "D-12: LogCapture attaches, snapshots message and throwable texts down the cause chain, and detaches on close"
    requirement: WRT-12
    verification:
      - kind: unit
        ref: "WriterLogMaskingUnitTest LogCapture self-test"
        status: pass
    human_judgment: false
  - id: D5
    description: "Anchors gate and full testUnit: every fork-anchors row holds at HEAD, verify-merge-invariants.sh 54e63648 HEAD prints result=PASS, testUnit has 492 tests with 0 failures and 0 errors"
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD; ./gradlew testUnit --rerun"
        status: pass
    human_judgment: false
duration: 8min
completed: 2026-10-12
status: complete
---

# Phase 18 Plan 02: WRT-12 log masking Summary

**An unparseable DateTime or DateTime64 string, and any value-write failure caught by `doWriteColValue`, now log only column, types, exception class and (for dates) value length, with no throwable, proven by a reload4j log-capture test; the rethrown-message leak that remains is recorded in FORK-DELTA item 6 and ROADMAP backlog 999.3 and pinned by a test.**

## Performance
- **Duration:** 8 min
- **Started:** 2026-10-12T13:48:27Z
- **Completed:** 2026-10-12T13:55:35Z (code and gate; SUMMARY and state follow)
- **Tasks:** 2
- **Files modified:** 7 (1 in `src/main`, `LogCapture` and `WriterLogMaskingUnitTest` new, `fork-anchors.txt`, `FORK-DELTA.md`, `ROADMAP.md`, the ledger)

## Accomplishments
- `logDateTimeParseFailure` is the one helper for both date-time catches. Shipped format: `Error parsing date time string for column '<name>' (ClickHouse type <T>, Kafka type <K>): exception <class>, value length <n>`. The `Error parsing date time string` prefix is kept for greps.
- The unsupported-conversion text is now `(Potentially) Not implemented conversion from <K> to <T> for column '<name>'` (Date, Date32, DateTime and DateTime64).
- The generic catch logs `Error writing value of <K> to the column `<col>` of type <T>: exception <class>` with no throwable and still executes `throw e;`. The `ClassCastException` catch is untouched.
- `LogCapture` (D-12) is a new reusable helper with a self-test; no dependency was added (`build.gradle.kts` and `libs.versions.toml` are unchanged).
- `WriterLogMaskingUnitTest` has 10 cases, all named `WRT-12: ...`.

## Task Commits
1. **Task 1 (tracer): WRT-12 DateTime, DateTime64 and the conversion DataException** - `4b824dab` (fix)
2. **Task 2: D-09 generic-catch log, residual recorded** - `9f070f98` (fix)

**Plan metadata:** recorded in the docs commit that follows this file.

Tracer gate: Task 1's `<verify>` carries only automated commands, so it was re-run after the commit and passed (`WRT12-TESTS-GREEN`, anchors, commit membership, FORK-DELTA order). Tracer verified end-to-end, then Task 2 expanded.

## Red evidence
- **Task 1, five red and two pins, as the plan expected (7 cases):** the self-test and the null-value case passed before the fix. These failed on the planned assertion (the exact masked line absent, or the old text present):
  - DateTime: captured ERROR lines were `Error parsing date time string: SENTINELq7Zx41kV` and `(Potentially) Not implemented conversion from STRING to DateTime`.
  - DateTime64: the leaking line was `Error parsing date time string: SENTINELq7Zx41kV, exception: Text 'SENTINELq7Zx41kV' could not be parsed at index 0`, so the value leaked twice.
  - DataException message: `expected: <... for column 'event_time'> but was: <(Potentially) Not implemented conversion from STRING to DateTime>`.
  - Empty string: old line `Error parsing date time string: ` (empty value).
  - UTF-16: old line `Error parsing date time string: SENTINELq7Zx41kV` followed by U+1F600.
- **Task 2, two red and one pin (3 new cases, 10 in the class):** the UUID rethrow case passed before the fix (it documents the residual). The UUID log case failed with the captured line `Error writing value of STRING to the column `order_uuid` of type UUID` lacking `: exception java.lang.IllegalArgumentException`, and that event carried the throwable (whose message embeds the sentinel). The DateTime-through-`doWriteColValue` case failed on the same missing generic line (the old line `Error writing value of STRING to the column `event_time` of type DateTime` plus a throwable).
- Semantic assessment: every red case failed on the planned assertion for the intended reason (the old unmasked or throwable-bearing log), not on a compile or fixture error. The tasks are `tracer` and `auto tdd`, not `type: tdd` plans, so `gsd_run check tdd-red-evidence` records were not required.

## Test counts and gates
- `WriterLogMaskingUnitTest`: 10 tests, 0 failures, errors or skips. `MissingNonNullableValueWriteTest` 7, `ClickHouseWriterNullMessageUnitTest` 5, `UtilsNullMessageClassificationUnitTest` 6, all green.
- Full `testUnit --rerun`: 41 classes, 492 tests, 0 failures, 0 errors, 2 skipped (the pre-existing `ConvertToMapOrJsonPerformanceTestJson`, outside this plan).
- Anchors gate at plan end: `invariants: provenance_paths=21 anchor_commits=239 anchor_rows=53 marker_commits=286 result=PASS`.
- `src/main` differs from the phase start in `ClickHouseWriter.java` and `Utils.java` only; `VERSION`, `CHANGELOG.md`, `build.gradle.kts` and `gradle/` are unchanged. `shouldSkipTableMetadata()` is still 8 lines, `buildRowBinaryUnsupportedTypeMessage` 5, `if (value.getObject() == null && col.isNullable()) {` 1.

## Files Created/Modified
- `src/main/java/.../sink/db/ClickHouseWriter.java` - `logDateTimeParseFailure`, three format constants, the generic-catch log
- `src/test/java/.../sink/util/LogCapture.java` - reload4j capture helper
- `src/test/java/.../sink/db/WriterLogMaskingUnitTest.java` - 10 sentinel log-capture pins
- `.planning/scripts/fork-anchors.txt` - six rows (WRT-12)
- `docs/FORK-DELTA.md` - two Connector source rows (between the nested-null split row and the WRT-10 row, in order WRT-12, WRT-12 (D-09), WRT-10) and item 6 rewritten with the residual
- `.planning/ROADMAP.md` - backlog `### Phase 999.3: Rethrown write-failure messages can carry record values (BACKLOG)`, naming the Connect status/DLQ path and the `Error inserting records` logs
- `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` - L-27, L-28, L-29

## Decisions Made
- Value length is `String.length()` in UTF-16 code units (recorded in L-27).
- The accepted residual (D-09, T-18-07) is documented and pinned, not fixed, as the plan and CONTEXT require.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spotless IDE-hook gate cannot be IS CLEAN for `ClickHouseWriter.java`**
- **Found during:** Task 1 and Task 2 commit steps
- **Issue:** Same as 18-01 deviation 1. The hook reports `IS DIRTY` for the unmodified phase-start file because upstream keeps its own import order. I checked the hook's proposed output against the file: the only differing lines are import and blank lines, so this plan adds no new formatting debt. `LogCapture.java` and `WriterLogMaskingUnitTest.java` are `IS CLEAN` (the test needed one pass of formatter output and, before Task 2, an unused-import removal).
- **Fix:** Did not reformat `ClickHouseWriter.java`, to keep the fork rebasable. The `IS CLEAN` clause of the plan's commit checks was run for the two test files; every other clause ran unchanged and passed.
- **Files modified:** none
- **Verification:** diff of the hook output against the file, filtered of import and blank lines, is empty

**2. [Rule 3 - Blocking] `timeout` is not available on this macOS shell**
- **Issue:** the plan advises running the anchors gate with a timeout of at least 300 s. `timeout` is not installed here.
- **Fix:** ran the gate through the tool's own 600 s timeout instead. It finished well inside it.

**Total deviations:** 2 (both blocking-check adjustments). **Impact:** none on behaviour or scope.

## Issues Encountered
- A first draft of `WriterLogMaskingUnitTest` imported `Column` before Task 2 used it; the unused import made Spotless dirty and was removed, then re-added with the Task 2 tests.

## Authentication Gates
None.

## Known Stubs
None.

## Threat Flags
None. T-18-05 and T-18-06 are mitigated and tested (sentinel absent from every captured message, throwable string and cause message; column name present; raw JDK exceptions self-checked to embed the sentinel; an anchor row with count 0 keeps the old throwable-bearing line from returning). T-18-07 is accepted as the plan states (FORK-DELTA item 6, backlog 999.3, UUID rethrow pin). T-18-08 is accepted.

## Next Phase Readiness
Plans 18-03 and 18-04 can proceed; `ClickHouseWriter.java` now has the WRT-12 anchors, and the ledger continues at L-30. Phase 20 should carry L-27 (old colon-form grep finds nothing), L-28 and L-29 into the CHANGELOG and the ROLL-02 log-alerting note, and decide on backlog 999.3.

---
*Phase: 18-writer-correctness*
*Completed: 2026-10-12*

## Self-Check: PASSED

- Files: `LogCapture.java`, `WriterLogMaskingUnitTest.java` and this plan's other artifacts exist on disk.
- Commits: `4b824dab` and `9f070f98` are ancestors of HEAD; `gsd-tools check evaluation-scope --plan 18-02 --commits-only` resolved both.
- Acceptance criteria and plan-level `<verification>` commands were re-run after the last commit and passed, apart from the documented Spotless adjustment for `ClickHouseWriter.java`.
