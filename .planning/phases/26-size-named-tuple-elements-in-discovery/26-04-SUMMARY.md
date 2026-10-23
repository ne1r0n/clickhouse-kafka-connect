---
phase: 26-size-named-tuple-elements-in-discovery
plan: "04"
subsystem: testing
tags: [clickhouse, tuple, describe-table, live-test, round-trip, rowbinary, rollout-facts, junit5]

requires:
  - phase: 26-size-named-tuple-elements-in-discovery
    provides: "47 DESCRIBE fixtures and DescribeFixtureCatalog (26-01); the extraction commit 9c186046 (26-02); the fix commit 7c00953e (26-03)"
provides:
  - TupleSizeNamedElementRoundTripLiveTest, the live round-trip of 46 catalog shapes plus the R-01 crafted value and the zero-WARN assertion
  - E-08 and E-09, the same class measured on the pre-fix tree and on the fix tree
  - E-10, the manual CLIENT_VERSION=V1 run of the describe pins and the drift premise
  - E-11, five rollout facts for Phase 29 with their provenance (the rollback fact marked assumed)
  - FORK-DELTA Known costs item 10 cites E-08 to E-10, and the shouldSkipDescribeRow row names the round-trip class
affects: [26-05, 28-jacoco-fork-report, 29-release]

actuals:
  tokens: 13394
  tasks: 2
  commits: 2
plan_head_before: afd4c2e545967d26c0767bb80ed2f0ca0a58b27b
plan_head_after: f1fca3a36ad72b5075c8b49f5e9a7c6cb682deb1

tech-stack:
  added: []
  patterns:
    - "one table-driven live class over the committed DDL catalog, run unchanged on an older tree to measure what that tree did"
    - "metadata-only outcome lines (key, outcome, server code name, row count) so a measurement run never prints a record value"
    - "a catalog key without a case fails at discovery, so a shape cannot be dropped silently"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md

key-decisions:
  - "The round-trip matrix is the catalog minus x08; every other key needs a case or the class fails at discovery, and an extra case for an unknown key fails too"
  - "A root UInt8 column takes a Connect INT8 in the test, because validateDataSchema compares a root column's Connect width with the column width; tuple elements are not checked, so they take INT32"
  - "The rollback and block-deduplication interaction stays assumed (research A5) in E-11; no measurement was attempted in this plan"

patterns-established:
  - "Pinned both ways: the unchanged class fails exactly the affected set on the pre-fix worktree and passes everywhere on the fix tree"

requirements-completed: [DESC-09, DESC-07]

coverage:
  - id: D1
    description: "All 46 round-trippable catalog shapes store through the real sink task on the default RowBinary route with client V2 and read back equal (whole value text), with zero WARN on the helper and DeclaredNullability loggers across the sink task's discovery and a second describeTable"
    requirement: DESC-09
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java#DESC-09 round-trip: {key} (46 tests, 0 failures; E-09)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The crafted R-01 value reads back tupleElement(c,'size') = 7 and hex(tupleElement(c,'v')) = 6162630378797A on the fix tree, and the same class on the refactor(26-02) worktree fails exactly the 25 affected keys (24 puts rejected with rows=0, r01 stored as 1667391751 / 78797A)"
    requirement: DESC-09
    verification:
      - kind: integration
        ref: "TupleSizeNamedElementRoundTripLiveTest#DESC-09 round-trip: r01; E-08 against E-09 (ROUNDTRIP-PINNED-BOTH-WAYS)"
        status: pass
    human_judgment: false
  - id: D3
    description: "One manual CLIENT_VERSION=V1 run of ClickHouseHelperClientForkLiveTest, TableTest and DescribeFixtureDriftLiveTest passed, and the drift class printed client=V1 so all 47 snapshots went through describeTableV1"
    requirement: DESC-07
    verification:
      - kind: integration
        ref: "CLIENT_VERSION=V1 ./gradlew test --tests '*ClickHouseHelperClientForkLiveTest' --tests '*TableTest' --tests '*DescribeFixtureDriftLiveTest' (113 tests, 0 failures; E-10; V1-RUN-GREEN)"
        status: pass
    human_judgment: false
  - id: D4
    description: "E-11 records five rollout facts for Phase 29, each marked measured (with its evidence entry) or assumed; the rollback fact is assumed"
    requirement: DESC-09
    verification: []
    human_judgment: true
    rationale: "The facts are operator guidance; whether the wording is adequate for the Phase 29 note is a judgment call, and the assumed fact is not testable here"
  - id: D5
    description: "FORK-DELTA Known costs item 10 gains one sentence citing E-08 to E-10, and the shouldSkipDescribeRow row names TupleSizeNamedElementRoundTripLiveTest"
    requirement: DESC-09
    verification:
      - kind: other
        ref: "grep -c TupleSizeNamedElementRoundTripLiveTest docs/FORK-DELTA.md (1); verify-merge-invariants.sh anchor_rows=77 result=PASS (ROLLOUT-FACTS-RECORDED)"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-10-21
status: complete
---

# Phase 26 Plan 04: Live Round-Trip of Size-Named Tuple Shapes Summary

**A 46-case live round-trip of every writable catalog shape, plus the R-01 crafted value, passes on ClickHouse 26.9.19.9 with client V2 and fails on exactly the 25 affected shapes when run unchanged on the pre-fix tree; the manual client V1 run and five rollout facts for Phase 29 are recorded.**

## Performance

- **Duration:** about 25 min (approximate; the start time was not recorded)
- **Started:** about 2026-10-21T21:15Z
- **Completed:** 2026-10-21T21:45Z
- **Tasks:** 2
- **Files modified:** 3 (1 new test class, FORK-DELTA, the evidence file)

## Accomplishments

- `TupleSizeNamedElementRoundTripLiveTest` creates each catalog table before `start()`, puts its records through `ClickHouseSinkTask`, and compares the whole `toString` of every column (so an element swap fails), with a second record on `s4_a01` (zero size, empty string) and `s4_a04` (empty array), `x04` at 4294967296, and `r01` compared by hex. Each case asserts zero WARN on the helper and `DeclaredNullability` loggers across discovery and a second `describeTable`, and prints one metadata-only `TUPLE_SIZE_ROUND_TRIP` line.
- Pinned both ways (E-08 against E-09): on a worktree of `refactor(26-02)` (`9c186046`) the unchanged class failed exactly the 25 keys of E-02's `affected_prefix` (24 puts rejected with `rows=0`: 23 `CANNOT_READ_ALL_DATA` and `n03`'s `ATTEMPT_TO_READ_AFTER_EOF`), the other 21 behaved identically, and `r01` stored size 1667391751 with hex `78797A`, the gg3 R-01 result. On the fix tree all 46 pass.
- E-10: the manual `CLIENT_VERSION=V1` run (113 tests, 0 failures) printed `DESCRIBE_FIXTURE_DRIFT client=V1`, so all 47 fixture snapshots went through `describeTableV1`. As a recorded extra, the round-trip class also passed 46 of 46 with the sink task on client V1.
- E-11: five rollout facts for Phase 29, four measured with their entry, the rollback and block-deduplication one assumed (research A5) and never cited as measured.

## Task Commits

1. **Task 1: live round-trip, pre-fix run (E-08), fix run (E-09)** - `0a6112fb` (test)
2. **Task 2: V1 run (E-10), rollout facts (E-11), FORK-DELTA follow-up** - `f1fca3a3` (docs)

**Plan metadata:** the SUMMARY commit and the STATE and ROADMAP commit follow this file.

## TDD Gate Compliance

RED is the pre-fix run (E-08) on the `refactor(26-02)` tree, which has no fix: the unchanged class failed the 25 affected cases for the planned reason (the writer sent a shorter tuple, the server refused it or `r01` stored a shifted value), and no unaffected case failed. Semantic assessment: the target cases executed, failed on the intended behaviour and not on setup, and `r01`'s message carries the stored value. GREEN is `7c00953e` (plan 26-03), so this plan has one `test(26-04)` commit and no `feat(26-04)` commit by design; the fix-tree run is E-09.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/TupleSizeNamedElementRoundTripLiveTest.java` - the 46-case live round-trip, the R-01 crafted value and the zero-WARN assertion
- `docs/FORK-DELTA.md` - Known costs item 10 gains the measured-live sentence; the `shouldSkipDescribeRow` row is also pinned by the new class
- `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md` - E-08 to E-11 and their index rows

## Decisions Made

- The matrix is the catalog minus `x08` (a JSON column, DESC-05); a catalog key without a case, or a case for an unknown key, fails at discovery, so nothing is dropped silently.
- Root-column Connect types follow `validateDataSchema` (INT8 for a root UInt8); tuple elements are not width-checked and take INT32. This is a test-side choice, not a connector finding.
- The `rows=` figure in the outcome lines is the total row count of the case's table (each case owns its table), which is stricter than counting only the case's ids.
- The rollback fact stays assumed; Phase 29 measures it or words it as a precaution.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug, in the new test] Root UInt8 columns given a Connect INT32**
- **Found during:** Task 1, first fix-tree run (44 of 46)
- **Issue:** `s4_u12` and `s4_u13` (root columns `size UInt8` and `size0 UInt8`) failed with "Data schema validation failed": `validateDataSchema` refuses a Connect INT32 for a UInt8 root column.
- **Fix:** the test uses INT8 for those two root columns. No `src/main` change; both E-08 and E-09 ran the corrected class.
- **Files modified:** `TupleSizeNamedElementRoundTripLiveTest.java`
- **Verification:** 46 of 46 on the fix tree
- **Committed in:** `0a6112fb`

**2. [Rule 1 - Bug, in the evidence] E-08 failure-code counts**
- **Found during:** Task 2, while writing E-11 fact (1)
- **Issue:** E-08 as committed in `0a6112fb` said "24 x CANNOT_READ_ALL_DATA, 1 x ATTEMPT_TO_READ_AFTER_EOF"; the run holds 24 failed puts (23 and 1) and `r01` stored wrong.
- **Fix:** corrected to 23, 1 and the stored-wrong `r01`, recounted from the XML.
- **Files modified:** `26-COMMAND-EVIDENCE.md`
- **Committed in:** `f1fca3a3`

---

**Total deviations:** 2 auto-fixed (both Rule 1, test and evidence text only)
**Impact on plan:** none on the connector; no `src/main` file was touched.

## Issues Encountered

None beyond the two deviations. The full suite was not re-run in this plan (the plan's gate is the tail gate, `anchor_rows=77 result=PASS`, and the full suite runs at the phase gate in 26-05).

## Known Stubs

None.

## Threat Flags

None. The class prints only key, outcome, server code name and row count; the crafted string is compared by hex; the evidence quotes hex and synthetic constants (T-26-10). T-26-09 is transferred: E-11 marks the rollback fact assumed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 26-05 (the phase gate). Requirement DESC-09 and DESC-07 are complete on the evidence of this plan; DESC-08 and DESC-10 stay with the phase close if the shared-ID gate still holds them.
- Phase 28 note: `DescribeTableBuilder` and `ParentShape` are the `jacocoForkReport` inputs for DEBT-17.

---
*Phase: 26-size-named-tuple-elements-in-discovery*
*Completed: 2026-10-21*

## Self-Check: PASSED
