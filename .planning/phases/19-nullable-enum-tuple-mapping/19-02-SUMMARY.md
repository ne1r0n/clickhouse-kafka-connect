---
phase: 19-nullable-enum-tuple-mapping
plan: "02"
subsystem: testing
tags: [clickhouse, nullable-enum, rowbinary, column-mapping, discovery-warn, fork-anchors, junit5, testcontainers]

requires:
  - phase: 19-01
    provides: NullableRoundTrip shared live helpers, twin-table convention, Tuple spike bundle
provides:
  - "COLS-08: Column.extractColumn Enum branch carries Nullable, so Nullable(Enum8|Enum16) slots get their RowBinary null marker at the top level, in Array and in Map"
  - "COLS-11: discovery WARN narrowed to Nullable(Tuple( only, with anchors row, FORK-DELTA rows and pin flips in the fix commit"
  - NullableEnumRoundTripLiveTest (6 live cases, both twins) and NullableEnumWriteTest (14 Docker-free cases)
  - 19-BEHAVIOUR-LEDGER.md rows L-31..L-34 for Phase 20 (CHANGELOG, FORK-DELTA, ROLL-02)
affects: [19-03, 19-04, 19-05, phase-20-rollout]

actuals:
  tokens: 12300
  tasks: 2
  commits: 2

plan_head_before: 0f35a4c0cad782585d02e7c44a77d6ed45c468b9
plan_head_after: a8501693203237358136a60ba77a94e156d56014
commits: 2

tech-stack:
  added: []
  patterns:
    - "Flip, never delete: a pin whose truth changes is renamed and retargeted with a written rationale and keeps its class case count"
    - "Mutation-proof pins: remove the one-line fix, show the new pins red (6/6 live, 11/14 unit), restore with git checkout"
    - "Same-commit bookkeeping: code, WARN narrowing, pin flips, fork-anchors, FORK-DELTA and ledger land together"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableEnumRoundTripLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/NullableEnumWriteTest.java
    - .planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkLiveTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md

key-decisions:
  - "Enum fix is one builder call (.isNullable(isNull)) in Column.extractColumn; ClickHouseWriter, Table, Processing and state classes are untouched, so retried batches stay byte-identical"
  - "The WARN narrows to Tuple in the same commit as the Enum fix (D-09); IN-01, IN-03 and IN-04 stay with the no-go plan (D-12)"
  - "Ledger row L-33 is the written rationale for reversing L-13; no existing test pinned L-13's nested Enum behaviour, so no further pins flip"

patterns-established:
  - "Unknown enum label on a nullable column still throws (RuntimeException type not pinned, RESEARCH Pitfall 9): behaviour preserved, not blessed"

requirements-completed: [COLS-08, COLS-11]

coverage:
  - id: D1
    description: "COLS-08: Nullable(Enum8|Enum16) NULL and non-NULL round-trip at the top level, in Array and in Map on both twins (RowBinary and RowBinaryWithDefaults), including boundary codes, NULL neighbours, empty containers, equal-label ordering and omitted fields"
    requirement: COLS-08
    verification:
      - kind: integration
        ref: "NullableEnumRoundTripLiveTest (6 cases, 0 failures; seen red 6/6 under mutation)"
        status: pass
    human_judgment: false
  - id: D2
    description: "COLS-08: Docker-free mapping of Nullable Enum roots, Array element, Map value and Tuple field, and the RowBinary bytes with and without defaults"
    requirement: COLS-08
    verification:
      - kind: unit
        ref: "NullableEnumWriteTest (14 cases, 0 failures; 11 seen red under mutation)"
        status: pass
    human_judgment: false
  - id: D3
    description: "COLS-11 / D-09 / D-11: discovery WARN names Tuple only, WARN pins flipped with rationale and kept at baseline counts, anchors row 57 retargeted"
    requirement: COLS-11
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest 44 cases, ClickHouseHelperClientForkLiveTest 12 cases, 0 failures"
        status: pass
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD -> result=PASS"
        status: pass
    human_judgment: false
  - id: D4
    description: "Ledger rows L-31..L-34 and FORK-DELTA rows describe the operator-visible change accurately for Phase 20"
    verification: []
    human_judgment: true
    rationale: "Prose accuracy of operator-facing documentation is checked structurally (row ids, columns, pinned-by names) but not semantically by a test; Phase 20 reviews it when it writes the CHANGELOG and ROLLOUT text"

duration: 15min
completed: 2026-10-12
status: complete
---

# Phase 19 Plan 02: Nullable(Enum) mapping fix Summary

**`Nullable(Enum8)` and `Nullable(Enum16)` slots now get their RowBinary null marker through a one-line `.isNullable(isNull)` in `Column.extractColumn`, pinned live on both twin tables and Docker-free, with the discovery WARN narrowed to Tuple in the same commit.**

## Performance

- **Duration:** about 15 min across two executor contexts (Task 1 about 5 min, Task 2 about 10 min including the 3 minute anchors gate)
- **Tasks:** 2 of 2
- **Files:** 9 changed (3 created, 6 modified); 657 insertions, 56 deletions over the plan

## Accomplishments

- Fixed backlog 999.1 for Enum: a NULL for a `Nullable(Enum8|Enum16)` slot no longer fails the batch or shifts bytes, and non-NULL values store their exact label and code.
- Narrowed the startup WARN to `Nullable(Tuple(` (`NULLABLE_TUPLE`, `hasNullableTuple`, `warnOnceIfNullableTuple`) and flipped its pins instead of deleting them.
- Pinned the fix with 6 live cases (both twins: top level, wide Array/Map matrix, omitted fields) and 14 Docker-free cases (mapping plus exact bytes).
- Proved the pins by mutation: with the Enum-branch line removed, 6/6 live and 11/14 unit cases fail.
- Started the Phase 19 behaviour ledger (L-31..L-34) and kept fork-anchors, FORK-DELTA and the ledger in step with the code.

## Task Commits

1. **Task 1 (tracer): COLS-08 end to end plus Tuple-only WARN, flips, anchors, FORK-DELTA, ledger** - `598f2caf` (fix)
2. **Task 2: full COLS-08 matrix on both twins and Docker-free pins, seen red by mutation** - `a8501693` (test)

Plan metadata (this SUMMARY, STATE, ROADMAP, REQUIREMENTS) is committed separately after these.

## TDD Gate Compliance

The plan fixes its own two-commit grouping (a `fix(19-02)` commit carrying the fix and its flips, then a `test(19-02)` commit carrying the expansion), which takes precedence over separate RED/GREEN commits. Failing-first evidence is recorded instead of extra commits:

- **RED, Task 1 (both twins PLAIN and DEFAULT, before the Enum line):** the put of a batch with a NULL for top-level `Nullable(Enum8)` column `ne` failed with `RuntimeException: An attempt to write null into not nullable column 'ne'` and left no row. Green on both twins after the fix.
- **RED, Task 2 (mutation, never committed):** `.isNullable(isNull)` removed from the Enum branch.
  - Live, 6/6 failed: `COLS-08 top-level Nullable(Enum8)` PLAIN and DEFAULT; `COLS-08 Nullable(Enum8|Enum16) top level, Array and Map round-trip` PLAIN and DEFAULT; `COLS-08 omitted Nullable(Enum) fields store NULL` PLAIN and DEFAULT.
  - Unit, 11/14 failed: Enum8 and Enum16 mapping, Array element, Map value, Tuple field, value bytes (`00 02`, `00 00 02`), NULL bytes for Enum8 and Enum16, Enum16 `00 E8 03`, boundary-code bytes, `02 00 01 01` array bytes. The 3 that stayed green pin unchanged behaviour: non-nullable Enum mapping, unknown label still throws, NULL in a non-nullable Enum8 still fails.
  - Source restored with `git checkout`, `git diff --quiet -- src/main` succeeded, suites green again.
- A first mutation attempt did not apply (BSD `sed -i` misparsed the expression), so its green runs were discarded and the mutation was redone with a Python edit; the reported results are from the applied mutation only.

## Renamed test methods

- `ClickHouseHelperClientForkUnitTest`: `nullableEnumOrTupleTypesAreFlagged` -> `nullableTupleTypesAreFlagged`
- `ClickHouseHelperClientForkLiveTest`: `describeTableWarnsOnceForNullableEnumOrTupleColumns` -> `describeTableWarnsOnceForNullableTupleColumnsOnly`

## Pin-flip audit (step 4a)

`grep -rlF "Nullable(Enum" src/test src/testFixtures src/integrationTest` returns exactly the two flipped WARN pin classes (`ClickHouseHelperClientForkUnitTest`, `ClickHouseHelperClientForkLiveTest`) and the two new COLS-08 classes. No Enum pin was missed. Ledger row L-33 is L-13's written flip rationale ("Reverses L-13 ... no dedicated test"); the D-16 pins are Tuple pins and flip in 19-04 (go) or stay true (no-go).

## Verification markers

`ENUM-T1-TESTS-GREEN` (ForkUnit 44, NestedNullableComposite 18, ColumnArrayRecursion 9, NullableEnumRoundTrip 2, ForkLive 12), `ENUM-T1-SOURCE-OK`, `ENUM-T1-DOCS-OK`, `ENUM-T1-FORMAT-OK`, `ENUM-T2-TESTS-GREEN` (14 unit, 6 live), `ENUM-T2-COMMIT-OK`, `ENUM-T2-PIN-AUDIT-OK`, and the anchors gate last line:

`invariants: provenance_paths=21 anchor_commits=266 anchor_rows=56 marker_commits=313 result=PASS`

`Table.java`, `ClickHouseWriter.java`, `ColumnTest` and `TableTest` are unchanged since the phase start. Both test files are `IS CLEAN` under the Spotless IDE hook.

## Files Created/Modified

- `src/main/java/.../sink/db/mapping/Column.java` - Enum branch gains `.isNullable(isNull)` (the only Column.java diff)
- `src/main/java/.../sink/db/helper/ClickHouseHelperClient.java` - WARN block renamed and narrowed to Tuple
- `src/test/java/.../db/helper/ClickHouseHelperClientForkUnitTest.java`, `ClickHouseHelperClientForkLiveTest.java` - WARN pins flipped with rationale
- `src/test/java/.../sink/NullableEnumRoundTripLiveTest.java` - new, 6 live cases
- `src/test/java/.../sink/db/NullableEnumWriteTest.java` - new, 14 Docker-free cases
- `.planning/scripts/fork-anchors.txt` - row 57 retargeted, one Column.java row added
- `docs/FORK-DELTA.md` - Enum source row, WARN row rewritten, Known costs item 1 rewritten, Pinned by names both new classes
- `.planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md` - rows L-31..L-34

## Decisions Made

- The Enum fix stays a DESCRIBE-time mapping flag so exactly-once retries rebuild byte-identical batches.
- Rows already stored wrongly by earlier releases are not repaired; the ledger and FORK-DELTA say so.
- The WARN text carries database, table, column and raw type only; no record value reaches it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The plan's `ENUM-T1-SOURCE-OK` check could never pass**
- **Found during:** Task 1 verify
- **Issue:** `ch=[x for x in d.split("\n") if x[:1] in "+-" ...]` counts the empty string after the diff's final newline, because `"" in "+-"` is true in Python, so the Column.java diff always counted 2 lines and `len(ch)==1` was unsatisfiable.
- **Fix:** guard `x[:1] and x[:1] in "+-"` in a copy of the check (saved under `/tmp/gsd19/scripts/t1_source.py`); it passes. The product code was correct. The plan file itself was left unchanged.
- **Files modified:** none in the repository
- **Commit:** n/a

No other deviations. Task 2's verify blocks use no similar filter and ran as written.

## Issues Encountered

- The first mutation run did not actually mutate (see TDD Gate Compliance); redone correctly.
- `verify-merge-invariants.sh` takes about 3 minutes; run in the background.

## Known Stubs

None.

## Threat Flags

None. The change adds no endpoint, auth path or file access; the WARN text still carries metadata only (T-19-05), `fork-anchors.txt` was retargeted in the fix commit (T-19-06), and the writer, state classes and `Table` are untouched (T-19-07).

## User Setup Required

None.

## Next Phase Readiness

Ready for 19-03 (the user's go/no-go on the Tuple spike). The Enum half of 999.1 ships whatever that decision is. Phase 20 should read ledger rows L-31..L-34 for the CHANGELOG and ROLL-02 text, including the log-alert note for the changed WARN text (L-34).

---
*Phase: 19-nullable-enum-tuple-mapping*
*Completed: 2026-10-12*

## Self-Check: PASSED

Created files exist (Column.java, both new test classes, ledger, FORK-DELTA); commits `598f2caf` and `a8501693` are ancestors of HEAD; `evaluation-scope --commits-only` lists both; the `COLS-08 flip (Phase 19, D-09/D-11)` comment appears once; `.isNullable(isNull)` counts 2 in Column.java; all Task 1 and Task 2 markers and the anchors gate re-ran green.
