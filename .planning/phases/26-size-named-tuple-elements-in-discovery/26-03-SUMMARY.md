---
phase: 26-size-named-tuple-elements-in-discovery
plan: "03"
subsystem: database
tags: [clickhouse, describe-table, tuple, discovery, classifier, fork-anchors, junit5]

requires:
  - phase: 26-size-named-tuple-elements-in-discovery
    provides: "47 committed DESCRIBE fixtures and snapshots (26-01); DescribeTableBuilder and DeclaredNullability.elementKey (26-02)"
provides:
  - ParentShape, which reads a DESCRIBE parent's declared type (array depth, declared element keys, offsets-row test)
  - DescribeTableBuilder.decide, the row classifier with the cascade guard, raw-type tracking and the metadata-only D-04 WARN
  - Column.declaredElement and the one fork line in Table.updateParent's Array branch
  - a three-argument shouldSkipDescribeRow that delegates to decide
  - DescribeTableBuilderUnitTest (120 Docker-free tests over the 47 captured fixtures) and the D-10 flip of the 999.4 pin
  - the DESC-10 documents and anchors (FORK-DELTA, CHANGELOG, ROADMAP 999.4, fork-anchors) in the fix commit
affects: [26-04, 26-05, 28-jacoco-fork-report, 29-release]

actuals:
  tokens: 14149
  tasks: 3
  commits: 2
plan_head_before: d1d32787dda69d5fcd1614d8f682b35bf882c6a0
plan_head_after: dd82dd15564b05f9a50949a061470234b0cc5bae

tech-stack:
  added: []
  patterns:
    - "the DDL of each captured fixture is the oracle: tupleFields are compared with the declared element keys, never with a hand-written list"
    - "a verdict about a DESCRIBE row travels to Table as a boolean on the Column, so Table keeps one extra condition instead of a second parser"
    - "red first on the extraction tree, then the characterization snapshots of the previous plan go red on exactly the measured affected set"

key-files:
  created:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/ParentShape.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Table.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/resources/describe-fixtures/ (25 .columns.txt snapshots)
    - docs/FORK-DELTA.md
    - CHANGELOG.md
    - .planning/ROADMAP.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md

key-decisions:
  - "The classifier lives in DescribeTableBuilder (package-private decide) and the parent reading in a public ParentShape in sink.db.mapping, so unwrapSimpleAggregate is shared at package visibility and DeclaredNullability gains no second public helper"
  - "A structural size twin is never added to the skip set and never registers its raw type, so a declared element's children are Columns while a structural row below a declared element is still dropped"
  - "An unreadable or unknown parent keeps gg3's behaviour (size and size0 dropped, size<digits> kept unmarked) with at most one metadata-only WARN per parent per DESCRIBE pass"
  - "FORK-DELTA's Adopted-wholesale file-count claim was re-measured against v1.7.0 (37 identical, 14 import-only, 12 fork logic, 10 files only here) because Table.java and the two new classes change it"

patterns-established:
  - "Docker-free pins feed raw JSONEachRow lines through DescribeFixtureCatalog.describeLine so they exercise fromJsonRow and the production loop"
  - "Fork anchors for a one-line change inside an upstream-identical file: the new line at count 1 plus upstream's old line at count 0"

requirements-completed: [DESC-08, DESC-09, DESC-10]

coverage:
  - id: D1
    description: "A declared Tuple element named size, size0 or size<digits> becomes a Column at both drop sites; for all 47 fixtures the tupleFields of every Tuple equal the declared element keys in declared order"
    requirement: DESC-08
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilderUnitTest.java#DESC-08 declared elements: k (47 rows)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every structural row stays out: no .null Column, no Map keys row in a Tuple, nested size1 offsets rows registered once and in no Tuple, nothing below a JSON root; a structural twin never enters the skip set"
    requirement: DESC-08
    verification:
      - kind: unit
        ref: "DescribeTableBuilderUnitTest#DESC-08 structural rows, cascade guard (both tests), DESC-05 JSON root, DESC-08 rule: {0} (17 rows)"
        status: pass
    human_judgment: false
  - id: D3
    description: "An unreadable parent fails closed to gg3's behaviour with one metadata-only WARN (no throwable, no message, no value)"
    requirement: DESC-08
    verification:
      - kind: unit
        ref: "DescribeTableBuilderUnitTest#D-03, D-04: an unreadable parent fails closed with one metadata-only WARN"
        status: pass
    human_judgment: false
  - id: D4
    description: "The D-04 pin is flipped to D-10 (a size-named Tuple element becomes a Column and discovery logs no WARN), seen red on the extraction tree first"
    requirement: DESC-09
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest#sizeNamedTupleElementIsKeptByDiscoveryWithoutWarning (3 rows); red run E-05"
        status: pass
    human_judgment: false
  - id: D5
    description: "The fix commit carries the code, the tests, the 25 re-captured snapshots and every DESC-10 document and anchor edit, and the full suite ran on exactly its tree"
    requirement: DESC-10
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun (1663 tests, 0 failed, 18 skipped); verify-merge-invariants.sh anchor_rows=77 result=PASS"
        status: pass
    human_judgment: false
  - id: D6
    description: "The live round-trip of every affected shape and the manual client V1 run (rest of DESC-09) are not part of this plan"
    requirement: DESC-09
    verification: []
    human_judgment: true
    rationale: "Plan 26-04 records the round-trip on the pre-fix and fix trees and the CLIENT_VERSION=V1 run; DESC-09 completes there"

duration: 15min
completed: 2026-10-21
status: complete
---

# Phase 26 Plan 03: Size-Named Tuple Elements in Discovery Summary

**Discovery now keeps a declared Tuple element named size, size0 or size<digits> at both drop sites by reading the parent's declared type, while every structural DESCRIBE row (Array offsets, String lengths, null maps, Map keys, anything below a JSON root) stays out, proven by 120 Docker-free tests over 47 captured fixtures and a 1663-test live suite on ClickHouse 26.9.19.9.**

## Performance

- **Duration:** about 15 min (approximate; the start time was not recorded)
- **Started:** about 2026-10-21T21:12Z
- **Completed:** 2026-10-21T21:27Z
- **Tasks:** 3
- **Files modified:** 38 (2 new Java classes, 6 edited src/main files with ParentShape new, 25 snapshots, 4 documents and anchors, the evidence file)

## Accomplishments

- `ParentShape` parses the parent's raw DESCRIBE type once (Array depth, declared element keys through `DeclaredNullability.elementKey`, and `isOffsetsRow` built from strings, never integers). `DescribeTableBuilder.decide` applies E-02's nine steps: a size-named row is kept as `KEEP_DECLARED` when the parent declares it and it is not an offsets row by type and depth, and is otherwise structural; a parent that cannot be read fails closed.
- `Column.declaredElement` carries the verdict to `Table.updateParent`, whose Array branch changed by one condition (`!child.isDeclaredElement()`); the Map and Tuple branches are untouched.
- Red first (E-05): on the extraction tree exactly the 25 "DESC-08 declared elements" rows, the 25 "D-10 no WARN" rows, the cascade-children test, the fail-closed test and the three flipped pin rows failed. After the fix the targeted run was 237 of 237 green; the full `testUnit` failed exactly the 25 "DESC-07 snapshot" rows of the measured affected set, which were re-captured as pure additions (E-06).
- One `fix(26-03)` commit (`7c00953e`) holds the code, the tests, the snapshots, FORK-DELTA, CHANGELOG, the ROADMAP 999.4 Delivered line and the anchors; its tree equals the tree the full suite ran on (1663 tests, 0 failures, 18 named skips, DESC-05 and DESC-02 live rows green, `DescribeFixtureDriftLiveTest` 94). The tail gate prints `anchor_rows=77 result=PASS` (E-07).

## Task Commits

1. **Task 1: Red first (E-05)** - nothing committed by design; the tests and E-05 went into the fix commit and the next commit
2. **Task 2: The classifier at both drop sites** - committed with Task 3 as one commit by the plan's one-commit rule
3. **Task 3: Re-capture, DESC-10 documents and anchors, full suite, one commit** - `7c00953e` (fix)

**Evidence commit:** `dd82dd15` (docs, E-07)

**Plan metadata:** the SUMMARY and the STATE and ROADMAP updates follow in a docs(26-03) commit.

## Files Created/Modified

- `src/main/java/.../sink/db/mapping/ParentShape.java` - reads a parent's declared type for the rule
- `src/main/java/.../sink/db/helper/DescribeTableBuilder.java` - `RowDecision`, `decide`, raw-type tracking, `UNREADABLE_PARENT_WARNING`
- `src/main/java/.../sink/db/helper/ClickHouseHelperClient.java` - two- and three-argument `shouldSkipDescribeRow` delegating to `decide`
- `src/main/java/.../sink/db/mapping/Column.java`, `Table.java`, `DeclaredNullability.java` - the carrier field, the one Table condition, the widened `unwrapSimpleAggregate`
- `src/test/java/.../DescribeTableBuilderUnitTest.java`, `ClickHouseHelperClientForkUnitTest.java` - the unit table and the D-10 flip
- `src/test/resources/describe-fixtures/` - 25 re-captured `.columns.txt` snapshots
- `docs/FORK-DELTA.md`, `CHANGELOG.md`, `.planning/ROADMAP.md`, `.planning/scripts/fork-anchors.txt` - DESC-10 documents and anchors
- `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md` - E-05, E-06, E-07

## Decisions Made

See `key-decisions` above. The fix is reversible only with a clean stop for an affected table, as the task's reversibility rating states: a retried batch now carries the whole tuple.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical] Re-measured the FORK-DELTA claim about upstream's `src/main` tree**
- **Found during:** Task 3 (FORK-DELTA edits)
- **Issue:** The Adopted-wholesale row said 38 of 63 upstream files were byte-identical, 25 differed (14 import-only, 11 with fork logic) and eight files exist only here. Moving `Table.java` out of the byte-identical set and adding `ParentShape` and `DescribeTableBuilder` made that claim false for this tree.
- **Fix:** Re-measured against tag `v1.7.0` with a script (63 present, 37 identical, 14 import-only, 12 fork logic, 10 files only here) and rewrote the row. The gg3-dated "raw numbers" section was left alone because it describes the published release.
- **Files modified:** docs/FORK-DELTA.md
- **Verification:** the measuring script's output is the source of the numbers
- **Committed in:** 7c00953e

**2. [Rule 3 - Blocking] Removed the now-unused `Type` import from ClickHouseHelperClient**
- **Found during:** Task 2
- **Issue:** Moving the JSON/Dynamic ancestor walk into `DescribeTableBuilder` left the import unused in `ClickHouseHelperClient.java` (the plan says to remove an import left unused).
- **Fix:** Deleted the import line.
- **Files modified:** src/main/java/.../sink/db/helper/ClickHouseHelperClient.java
- **Verification:** compile and the full suite
- **Committed in:** 7c00953e

---

**Total deviations:** 2 auto-fixed (1 missing critical documentation accuracy, 1 blocking)
**Impact on plan:** Both necessary for correct documents and a clean compile. No scope creep: no src/main file outside `files_modified` was touched.

## Issues Encountered

- I applied the Task 2 main-source edits before the Task 1 red run by mistake. I noticed before running anything, copied them to `/tmp/gsd26/exec/stash-main/` (no `git stash`), restored the three tracked files with `git checkout -- <file>` and removed the new file, ran the red run on a tree where `git diff HEAD -- src/main` was empty, then restored the files. The red evidence is therefore a true extraction-tree run.
- The throwaway-worktree spotless check showed `Column.java`, `Table.java` and `ClickHouseHelperClient.java` differ from spotless output only in the position of the lombok/slf4j import block. That is the baseline rewrite that predates the phase (E-04 recorded it for `ClickHouseHelperClient.java`); `Table.java` keeps upstream's import order on purpose. The five other Java files are byte-identical.
- The plan's reasoned list of 25 affected fixtures equalled E-02's measured list, so the plan text and E-02 did not differ.

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None. The new WARN carries metadata only (raw type, parent column, database, table, exception simple class name) and is asserted to have no throwable; no network endpoint, auth path or schema at a trust boundary was added.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 26-04 can record the live round-trip on the pre-fix tree (`d1d32787`) and the fix tree, the manual `CLIENT_VERSION=V1` run of the drift class and the rollout facts. DESC-09 completes there; this plan delivered only its Docker-free half.
- `docs/FORK-DELTA.md`, the CHANGELOG `# 1.7.0_gg4, unreleased` section and ROADMAP 999.4 name only what is measured at this commit.

## Self-Check: PASSED

- Created files exist: `ParentShape.java`, `DescribeTableBuilderUnitTest.java`, this SUMMARY.
- Commits `7c00953e` and `dd82dd15` are ancestors of HEAD; the fix commit's tree equals the tree the suite ran on.
- The plan's three verification commands print FIX-ONE-COMMIT and DESC10-DOCS, and the suite and gate conditions behind FIX-SUITE-GATED hold (1663 tests, 0 failed, 18 skipped, `anchor_rows=77 result=PASS`).

---
*Phase: 26-size-named-tuple-elements-in-discovery*
*Completed: 2026-10-21*
