---
phase: 22-writer-record-data-exposure
plan: "03"
subsystem: writer
tags: [wrt-13, wrt-15, data-exception, withheld-message, record-data-exposure, fork-anchors, behaviour-ledger]

requires:
  - phase: 22-02
    provides: "ScriptedWriteFailureWriter, CountingHelperClient and the WRT-14 pin (184 cases) that must stay green and byte-identical"
provides:
  - "doWriteColValue's generic catch passes IO-bearing chains, ConnectExceptions and exact RuntimeExceptions through, and replaces everything else with a value-free DataException"
  - "WithheldMessageException: sanitized cause chain with original frames, fixed note, no suppressed, cycle and depth guard"
  - "Column.convertEnumValues fixed text 'No content in enum' (D-14)"
  - "WriterValueFreeFailureUnitTest, WithheldMessageExceptionUnitTest and ThrowableTexts (Docker-free D-12 layer)"
  - "FORK-DELTA, fork-anchors, ROADMAP 999.3 and 22-BEHAVIOUR-LEDGER.md rows L-42..L-47 in the same commit (WRT-15)"
affects: [22-04, 22-05, phase-24-release]

actuals:
  tokens: 21800
  tasks: 3
  commits: 2
plan_head_before: 6ecfae89f4bddc7ccfa49a4ab81987c28c8fcfea
plan_head_after: 53c433a94f7634bde93bc78a8764d6502cf83886

tech-stack:
  added: []
  patterns:
    - "Pass-through-or-replace catch: metadata-only DataException plus a message-withholding cause copy that keeps the frames"
    - "Red-before in a throwaway worktree of the unchanged tree, with display-name prefixes that say which cases must fail and which must stay green"

key-files:
  created:
    - src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java
    - src/test/java/com/clickhouse/kafka/connect/sink/util/ThrowableTexts.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/util/WithheldMessageExceptionUnitTest.java
    - .planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/ROADMAP.md
    - .planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md

key-decisions:
  - "ConnectException is referenced fully qualified in ClickHouseWriter.java and the one new import sits right after the Utils import, so the spotless baseline (46 files, same md5) is unchanged (D-16)"
  - "The F2 flip of schemaWithFixedStringMismatchTest was made only because D-05 forces it, shown by a forced-red run on the unchanged test first; it is an equally strong exact-class check, and it is surfaced to the user rather than decided by them"
  - "The D-04 pin uses a record text of at most 36 characters (UUID.fromString fails with a value-free 'UUID string too large' beyond that), with a self-check that the raw JDK message carries the refresh key"

patterns-established:
  - "ThrowableTexts.allTexts: identity-guarded walk over cause and suppressed edges plus a UTF-8 printStackTrace rendering, used for every sentinel-absence assertion"

requirements-completed: [WRT-13, WRT-15]

coverage:
  - id: D1
    description: "An invalid UUID string and an unparseable INT128 string written through doWriteColValue, and through doInsertRawBinary on client V1 and V2, throw a DataException naming the column, the Type token and the exception class, with the sentinel absent from every thrown text, the rendered stack trace and every captured log event"
    requirement: WRT-13
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java#WRT-13 value-free (10 cases)"
        status: pass
    human_judgment: false
  - id: D2
    description: "IO chains, ConnectExceptions and exact RuntimeExceptions pass through unchanged (D-01, D-03), a record value with Code 33 or 131 no longer triggers the V1 mapping refresh (D-04), and Column.convertEnumValues carries no label (D-14)"
    requirement: WRT-13
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java#WRT-13 pass-through (6), D-04 V1 (2), D-04 V2 unchanged (2), D-14 (1)"
        status: pass
    human_judgment: false
  - id: D3
    description: "WithheldMessageException keeps frames and class names, never text, drops suppressed exceptions, and terminates on cycles and over-deep chains"
    requirement: WRT-13
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/util/WithheldMessageExceptionUnitTest.java (11 cases)"
        status: pass
    human_judgment: false
  - id: D4
    description: "WRT-14 holds: the classification pin passes unmodified with the same 184 cases, and the pin, its fixture and CountingHelperClient are byte-identical to the last test(22-02) commit"
    requirement: WRT-13
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java (tests=184 failures=0); git diff --quiet 0590d20c HEAD -- pin fixture CountingHelperClient"
        status: pass
    human_judgment: false
  - id: D5
    description: "The F2 forced flip of schemaWithFixedStringMismatchTest is equally strong and green live on latest x V2"
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java#schemaWithFixedStringMismatchTest (tests=1 failures=0)"
        status: pass
    human_judgment: true
    rationale: "CONTEXT's Flip, don't delete item names only the WriterLogMaskingUnitTest method; this second flip is forced by D-05 and acted on by the executor, so the user should confirm it"
  - id: D6
    description: "WRT-15: FORK-DELTA, six fork-anchors rows, ROADMAP 999.3 and the behaviour ledger land in the same twelve-file commit, and the anchors gate passes"
    requirement: WRT-15
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD -> anchor_rows=65 anchor_exempt=1 result=PASS; git show --name-only c713a837 = the twelve-file set"
        status: pass
    human_judgment: false

duration: 18min
completed: 2026-10-19
status: complete
---

# Phase 22 Plan 03: WRT-13 Value-Free Writer Failures Summary

**doWriteColValue's generic catch now throws a metadata-only DataException with a message-withholding WithheldMessageException cause for value-conversion failures, while IO chains, ConnectExceptions and exact RuntimeExceptions pass through unchanged, so retry classification is untouched (WRT-14 pin unmodified, 184 cases) and the record value no longer reaches Connect status, DLQ stack traces or logs.**

## Performance

- **Duration:** 18 min
- **Started:** 2026-10-19T20:18:23Z
- **Completed:** 2026-10-19T20:36:00Z
- **Tasks:** 3 (tracer, expansion, docs and commit)
- **Files modified:** 13 (5 created, 8 modified, as in the fix and evidence commits)

## Accomplishments
- The invalid-UUID path is value-free end to end. The thrown chain, its rendered stack trace and both reachable log events carry the column, the `Type` token and the exception class and no value, for the writer and for `doInsertRawBinary` on V1 and V2. All four tracer tests were seen red on the unchanged writer first (4 of 4), with exactly the flipped method red in `WriterLogMaskingUnitTest` (1 of 10).
- Expansion covers an unparseable INT128, a Decimal out of range, a failure in a Tuple child (innermost column named, one generic-catch line per level), a null `Data`, six D-03 pass-through shapes, the D-04 Code 33 and 131 cases on V1 (before: two hook calls, one pin, one mapping update; after: one, zero, zero) and V2 unchanged, and D-14. In the throwaway worktree of the unchanged tree 13 of 21 cases were red and the 8 non-change cases were green.
- `WithheldMessageException` is pinned for the note, `toString`, frames, dropped suppressed, class order, a cycle (two nodes) and the depth boundary (exactly `MAX_CAUSE_DEPTH` copied whole, longer cut).
- WRT-15 landed in one `fix(22-03)` commit of exactly twelve files with the real anchors gate ending `anchor_exempt=1 result=PASS`.

## Task Commits

Tasks 1 and 2 were held uncommitted by design (WRT-15 puts the code and its doc edits in one commit), so Task 3 holds the one code commit:

1. **Tasks 1-3: tracer, expansion, F2 flip, FORK-DELTA, anchors, ROADMAP 999.3 and ledger** - `c713a8379b546a14d1cb4f6a574bb0278aadcfb0` (fix, twelve files)
2. **Task 3: E-03 evidence** - `53c433a94f7634bde93bc78a8764d6502cf83886` (docs)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan)

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - constants `WRITE_FAILURE_COLUMN_PREFIX` and `WRITE_VALUE_CONVERSION_FAILURE_FORMAT`, helpers `passesThroughUnchanged` and `buildValueConversionFailureMessage`, and the pass-through-or-replace rethrow in the generic catch (the catch's own log line and the ClassCastException branch are untouched)
- `src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java` - sanitized cause chain
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` - D-14 fixed text (2 added lines including one comment)
- `src/test/java/com/clickhouse/kafka/connect/sink/util/ThrowableTexts.java` - shared text-walk and UTF-8 rendering helper
- `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java` - 21 Docker-free cases
- `src/test/java/com/clickhouse/kafka/connect/util/WithheldMessageExceptionUnitTest.java` - 11 contract cases
- `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterLogMaskingUnitTest.java` - the 999.3 residual flip (still 10 `@Test`)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskWithSchemaTest.java` - the F2 forced flip (still 78 `@Test`)
- `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `.planning/ROADMAP.md`, `22-BEHAVIOUR-LEDGER.md`, `22-EVIDENCE.md` - WRT-15 bookkeeping and E-03

## Renamed test methods (old -> new)
- `WriterLogMaskingUnitTest.uuidWriteFailureRethrowsOriginalWithValueInMessage` -> `uuidWriteFailureThrowsValueFreeDataException` (the flip CONTEXT names). The class keeps 10 `@Test`; the raw-JDK UUID self-check and `assertRawJdkExceptionEmbedsSentinel` (3 lines) are kept.
- `ClickHouseSinkTaskWithSchemaTest.schemaWithFixedStringMismatchTest` keeps its name; only its assertion changed (see F2 below).

## Anchor counts as measured
- ClickHouseWriter.java: `WRITE_VALUE_CONVERSION_FAILURE_FORMAT` 2 (baseline 0), `WithheldMessageException.of(` 1 (baseline 0), `WRITE_VALUE_FAILURE_LOG_FORMAT` 2 (unchanged), the CCE format line 1 (unchanged), no ConnectException import.
- WithheldMessageException.java: class declaration 1, `(message withheld: may contain record data)` 1.
- Column.java: `throw new RuntimeException("No content in enum");` 1, `No content in enum %s` 0 (negative anchor).
- fork-anchors mirror: `rows=65 bad=0` (baseline 59, six WRT-13 rows). Real gate: `invariants: provenance_paths=21 anchor_commits=420 anchor_rows=65 marker_commits=467 anchor_exempt=1 result=PASS`.
- Spotless: SPOTLESS-BASELINE-EQUAL (46 files, md5 f2e32c7bc069ecd155830f82d26285d6). Full testUnit: 793 tests, 791 succeeded, 0 failed, 2 skipped (761 plus 21 plus 11).

## Decisions Made
- F2 forced flip, surfaced to the user (see Deviations).
- The D-04 cases use a 34 to 35 character record text, because `UUID.fromString` throws a value-free `UUID string too large` beyond 36 characters, which would have made the D-04 cases vacuous. Each case self-checks that the raw JDK message carries the refresh key.
- `getMessage()` of `WithheldMessageException` returns the fixed note, not null (D-06 choice), so renderers that print `class: message` stay informative.

## Deviations from Plan

None - plan executed exactly as written.

The F2 flip is a plan-declared forced flip, not a deviation: the f2-forced-red run (one testcase, one failure of type `AssertionFailedError`, message naming `Could not detect size mismatch for FixedString` and `WithheldMessageException`) shows D-05 makes the old instance-of assertion fail, and the replacement (an exact-class check on the final `WithheldMessageException` plus an exact `getOriginalClassName()` equality with `java.lang.IllegalArgumentException`) is equally strong. CONTEXT's "Flip, don't delete" item names only the `WriterLogMaskingUnitTest` method, so **the orchestrator should surface this second flip to the user**.

## Issues Encountered
- None blocking. No OpenJ9 crash occurred in any Gradle run, so no core dumps needed cleanup.
- One thing the plan did not state: `UUID.fromString` rejects strings over 36 characters with a value-free message, so the D-04 record text had to be short (see Decisions).

## Residual (flagged assumption A2)
An `IOException` chain passes through unchanged under the locked D-01, including a Jackson `JsonProcessingException` on the JSON-as-string route. Whether a Jackson reference chain can name a record's map key was not measured. It is stated in FORK-DELTA Known costs item 6 and E-03 and is not fixed here (T-22-13, accepted). Also noted for Phase 24: `forkChangedClasses` in `build.gradle.kts` does not list `WithheldMessageException`, and item 6's heading wording is left for the FORK-DELTA completeness pass.

## Known Stubs

None.

## Threat Flags

None. The change removes a disclosure path and adds no network endpoint, auth path, file access or schema change.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 22-04 can add the live D-12 layer (`put()` with tolerance none and all, DLQ headers per D-15) on `latest` x V2 and the ledger row L-48, adding live pins to L-42 and L-43.
- Plan 22-05 owns the full suite on `latest` x V2 and the phase gate.

## Self-Check: PASSED

The five created files exist, commits `c713a837` and `53c433a9` are ancestors of HEAD, the pin, its fixture and `CountingHelperClient` are byte-identical to `0590d20c`, and TRACER-SEEN-RED, TRACER-GREEN, EXPANSION-DISCRIMINATES, EXPANSION-GREEN, DOCS-AND-ANCHORS-OK, TESTUNIT-GREEN, SPOTLESS-BASELINE-EQUAL and WRT13-COMMITTED all printed.

---
*Phase: 22-writer-record-data-exposure*
*Completed: 2026-10-19*
