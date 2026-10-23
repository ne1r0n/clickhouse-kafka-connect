---
phase: 27-writer-failure-path-test-debt
plan: "03"
subsystem: writer
tags: [kafka-connect, clickhouse, enum, value-free-failures, wrt-18]

requires:
  - phase: 27-writer-failure-path-test-debt
    provides: plan 27-02 fix(27-02), the E-05 baseline (anchor_rows=80, 1166 testUnit tests) and the value-free writer test scaffolding
provides:
  - Column.convertEnumValues reports a label that is not among the column's enum values as an IllegalArgumentException "Enum label not found for column <name>", so no Enum8 or Enum16 call site can unbox a null
  - the no-content text now names the column, "No content in enum column <name>", still an exact RuntimeException that passes through doWriteColValue unchanged
  - four ColumnTest rows and two WriterValueFreeFailureUnitTest rows pinned before Phase 28 measures enum labels in the same Column code
  - fork-anchors rows (82), FORK-DELTA rows, CHANGELOG bullet and ledger rows L-54..L-56
affects: [27-04, 27-05, 28, 29]

actuals:
  tokens: 10650
  tasks: 3
  commits: 2

plan_head_before: daffc6fbd6fa474341e6175984e35f77ed9bce62
plan_head_after: d1a2871f279331601ce0a6f42f2d6d0f9207f9cd

tech-stack:
  added: []
  patterns:
    - "Lookup miss detected at the lookup: Integer code = map.get(label); null throws a column-named IllegalArgumentException before any unboxing"
    - "Red first on the unchanged Column (E-06), then fix, anchors, FORK-DELTA, CHANGELOG and the full suite on exactly the staged tree in one fix commit"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - CHANGELOG.md
    - .planning/phases/27-writer-failure-path-test-debt/27-BEHAVIOUR-LEDGER.md
    - .planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md

key-decisions:
  - "Research Open Question 1 decided as recommended: the lookup-miss text is proven at Column level; at writer level the operator sees the IllegalArgumentException class and the column named by the DataException prefix, because WithheldMessageException withholds the IllegalArgumentException's message. Making the text visible would need a pass-through type, a different decision left to the user."
  - "Both new texts are private static final format constants in Column and take only Column.name, never the label."

requirements-completed: [WRT-18]

coverage:
  - id: D1
    description: "An unknown Enum8 or Enum16 label, and a null label, fail at Column.convertEnumValues with an IllegalArgumentException whose message is exactly 'Enum label not found for column <name>' and carries no label"
    requirement: "WRT-18"
    verification:
      - kind: unit
        ref: "ColumnTest#unknownEnum8LabelIsALookupMissNamingTheColumn, unknownEnum16LabelIsALookupMissNamingTheColumn, nullEnumLabelIsALookupMiss"
        status: pass
    human_judgment: false
  - id: D2
    description: "Through doWriteColValue an unknown Enum8/Enum16 label surfaces as the value-free DataException ending java.lang.IllegalArgumentException with a withheld cause, the label absent from the chain, the rendering and the logs"
    requirement: "WRT-18"
    verification:
      - kind: unit
        ref: "WriterValueFreeFailureUnitTest#WRT-18 value-free: an unknown {Enum8,Enum16} label (2 rows)"
        status: pass
    human_judgment: false
  - id: D3
    description: "A Column without enum values throws the exact RuntimeException 'No content in enum column <name>', which still passes through unchanged and carries no label"
    requirement: "WRT-18"
    verification:
      - kind: unit
        ref: "ColumnTest#enumWithoutValuesNamesTheColumn; WriterValueFreeFailureUnitTest#D-14: an Enum8 column without enum values throws the fixed text with no label in any text"
        status: pass
    human_judgment: false
  - id: D4
    description: "Classification of an unknown Enum label stays NON_RETRIABLE (WriterFailureClassificationUnitTest UNKNOWN_ENUM_LABEL unchanged)"
    requirement: "WRT-18"
    verification:
      - kind: unit
        ref: "WriterFailureClassificationUnitTest (216 cases)"
        status: pass
    human_judgment: false
  - id: D5
    description: "FORK-DELTA rows, the CHANGELOG bullet and ledger L-54 state that the lookup-miss text is withheld on the writer path and only the class and column are operator-visible"
    requirement: "WRT-18"
    verification: []
    human_judgment: true
    rationale: "Whether the wording reads correctly to operators, and whether the user wants the lookup-miss text made operator-visible (Open Question 1), is a judgment no test asserts"

duration: 9min
completed: 2026-10-23
status: complete
---

# Phase 27 Plan 03: Enum lookup miss at the lookup Summary

**Column.convertEnumValues now throws a value-free IllegalArgumentException naming the column on an unknown or null Enum label, instead of returning null that the Enum8/Enum16 write unboxes into a NullPointerException; the no-content text names the column too**

## Performance

- **Duration:** 9 min
- **Started:** 2026-10-23T00:17:56Z
- **Completed:** 2026-10-23T00:26:00Z
- **Tasks:** 3
- **Files modified:** 8 (6 in the fix commit, 2 phase documents)

## Accomplishments

- `Column.convertEnumValues` reads `Integer code = enumValues.get(value)` and throws `IllegalArgumentException("Enum label not found for column <name>")` on a miss (unknown label, or a null label against the production `HashMap`). The no-content branch still throws an exact `RuntimeException`, now `No content in enum column <name>`. Both texts are private format constants taking only the column name.
- Red first: on the unchanged Column the failing set was exactly the four new `ColumnTest` rows, the two new `WRT-18` rows in `WriterValueFreeFailureUnitTest` and the existing D-14 row with its updated expectation (E-06). The two writer rows ended in `java.lang.NullPointerException` there.
- Green: testUnit 1172 tests, 0 failed; the full suite on exactly the commit's tree ran 1661 tests, 0 failures, 0 errors, the 18 named skips, ClickHouse 26.9.19.9, client V2. `WriterFailureClassificationUnitTest` stays 216 and green, so the NON_RETRIABLE classification is unchanged.
- Anchors moved with the code: the Phase 22 throw-line row is gone; `NO_ENUM_CONTENT_FORMAT` 2, `ENUM_LABEL_NOT_FOUND_FORMAT` 2 and `return enumValues.get(value);` 0 are in; the negative `No content in enum %s` 0 row stays. The tail gate reports `anchor_rows=82 result=PASS`.
- FORK-DELTA (doWriteColValue row, convertEnumValues row), one CHANGELOG gg4 bullet (3 gg4 bullets, released sections byte-equal to the phase base) and ledger rows L-54, L-55, L-56 plus rollout line 3 were written in the same stage.

## Task Commits

1. **Task 1: Red first on the unchanged Column** - nothing committed by design; recorded in E-06
2. **Task 2: The lookup miss in Column.convertEnumValues, green on testUnit** - committed with Task 3 in the single fix commit
3. **Task 3: Anchors, documents, full suite, one commit** - `c1e83945` (fix)

**Plan metadata:** `d1a2871f` (docs: red run, fix gate evidence and ledger)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` - the lookup miss and the column-named no-content text, two constants
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java` - four Column-level pins (37 tests)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java` - the WRT-18 parameterized rows, `NO_ENUM_CONTENT_MESSAGE` updated (29 tests)
- `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`, `CHANGELOG.md` - moved with the code
- `27-BEHAVIOUR-LEDGER.md`, `27-COMMAND-EVIDENCE.md` - L-54..L-56, E-06, E-07

## Decisions Made

- Open Question 1 taken as the plan recommends and flagged to the user: the lookup-miss text itself is not operator-visible on the writer path, because `WithheldMessageException` withholds an `IllegalArgumentException`'s message. An operator reads the class and the column. Making the text visible would need a pass-through type thrown from `Column`, which is a different decision from D-06.

## Deviations from Plan

### Auto-fixed Issues

**1. [Plan expectation error] Spotless numstat for ClickHouseWriter.java is `10<TAB>11`, not `9<TAB>12`**
- **Found during:** Task 3 step 7
- **Issue:** Same as plan 27-02's Deviation 1. The figure comes from 27-02's used imports in ClickHouseWriter.java, which this plan does not touch. Column.java (13/14) and ColumnTest.java (5/7) are byte-equal to the base, and the other 44 lines are the base's.
- **Fix:** None to the code; recorded in E-07.
- **Committed in:** `d1a2871f`

**2. [Process error, self-corrected] Unscoped `spotlessApply` reformatted the 46 pre-existing violating files**
- **Found during:** Task 3 step 5, immediately after running it
- **Issue:** I ran `./gradlew spotlessApply` on the working tree, which rewrote 46 files that already violated Spotless at the phase base, three of which (Column.java, ColumnTest.java, WriterValueFreeFailureUnitTest.java) hold this plan's edits. The plan only measures Spotless on a temp copy.
- **Fix:** Before anything was staged, every file outside this plan was restored with `git checkout -- <path>`, and the three plan files were restored and my edits re-applied. The staged diff is the plan's own (Column.java 10/3, WriterValueFreeFailureUnitTest.java 21/1, ColumnTest.java 40/0), the full suite and every gate ran on that tree, and the temp-copy Spotless numstat confirms no other file moved. Nothing reformatted reached a commit. Recorded in E-07.
- **Files modified:** none beyond the plan's own
- **Committed in:** `d1a2871f` (E-07)

---

**Total deviations:** 1 plan-expectation error, 1 process error (self-corrected, no effect on the commit)
**Impact on plan:** None on behaviour or scope. The three plan files do not carry Spotless formatting of their own, because they are among the 46 pre-existing violations the project leaves untouched.

## Issues Encountered

None in the fix. The full suite passed on its first run (no OpenJ9 crash).

## Known Stubs

None.

## Threat Flags

None. T-27-09 (label disclosure) is mitigated by label-free formats and asserted by the Column and writer rows.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 27-04. Phase-base counts after this plan: testUnit 1172, `ColumnTest` 37, `WriterValueFreeFailureUnitTest` 29, anchors 82.
- Phase 28 can measure enum labels in Column on pinned code.
- For the user (Open Question 1): if the `Enum label not found for column <name>` text should be visible to operators on the writer path, that needs a pass-through exception type thrown from Column. It is not done here.

## Self-Check: PASSED

- All modified files exist; `c1e83945` and `d1a2871f` are ancestors of HEAD; the fix commit's tree equals the tree the full suite ran on (`1516b9a8`).
- `WRT18-RED-RECORDED`, `WRT18-GREEN`, `WRT18-ONE-COMMIT` and the suite, gate and ledger checks of Task 3 pass. The only Task 3 check that cannot pass as written is the Spotless comparison against `9<TAB>12` (Deviation 1).

---
*Phase: 27-writer-failure-path-test-debt*
*Completed: 2026-10-23*
