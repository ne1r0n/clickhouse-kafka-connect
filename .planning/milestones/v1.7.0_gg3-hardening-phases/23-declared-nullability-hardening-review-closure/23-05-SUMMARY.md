---
phase: 23-declared-nullability-hardening-review-closure
plan: "05"
subsystem: testing
tags: [declared-nullability, tuple, unpaired-element-warn, clickhouse-describe, junit5, backlog-999.4]

requires:
  - phase: 23-declared-nullability-hardening-review-closure
    provides: "23-03 delimited fallback-WARN column assertions and the sentinel root; 23-04 quote-aware normalise"
provides:
  - "DeclaredNullability.UNPAIRED_ELEMENT_WARNING: a Tuple Column that lacks a declared element logs one metadata-only WARN at every DESCRIBE and still applies the declared nullability to the elements that paired"
  - "D-01 rule in the DeclaredNullability class Javadoc and the FORK-DELTA COLS-10 row"
  - "Docker-free 999.4 pin: discoverAsDescribeTableV2 runs the R-01 rows through the real shouldSkipDescribeRow and buildDescribedTable"
  - "Backlog 999.4 (Tuple element named size/size0 dropped by discovery) and 999.5 (doubled backslashes from fromJsonRow) in ROADMAP"
  - "Ledger row L-51, COLS-14 anchors row, evidence entry E-05 with the SC3 reading"
affects: [23-06, phase-24-changelog, phase-24-fork-delta, ROLL-03, backlog-999.4]

actuals:
  tokens: 14800
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Collect-then-log: the pass gathers unpaired declared elements in a LinkedHashMap walk and logs once after the assignments are applied, without changing the true return"
    - "Skip-aware discovery helper in a Docker-free test: the describeTableV2 loop mirrored for fixture rows so structural-row skips are exercised for real"
    - "Scoped AGENTS.md exemption recorded in the comment of each flipped existing test, naming the user decision"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-BEHAVIOUR-LEDGER.md
    - .planning/ROADMAP.md
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md

key-decisions:
  - "D-01: an unpaired declared element logs one WARN and the pass still applies; falling back cannot repair the missing element's bytes and would bring back the double flag"
  - "D-02: no dedup and no static state; two apply calls give two events"
  - "D-19 (user Option B): the three collided tests gain element rows with names and assertions unchanged; the root-only input moved to sibling tests that assert the WARN"
  - "D-03/D-04: 999.4 is confirmed for size and size0, pinned Docker-free, and deferred; ClickHouseHelperClient.java is unchanged"

patterns-established:
  - "Shared assertOnlyUnpairedWarning / applyOnSentinelRoot helpers keep the nine expansion pins to a few lines each"

requirements-completed: [COLS-14, DEBT-10]

coverage:
  - id: D1
    description: "A Tuple Column that lacks a declared element makes apply return true, apply the declared nullability to the element that paired and log exactly one metadata-only WARN naming the column, the unpaired element, the table and the raw type, with no throwable"
    requirement: COLS-14
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#COLS-14 unpaired-element WARN (D-01, D-02): a Tuple Column that lacks a declared element logs one metadata-only WARN naming it and still applies the declared nullability"
        status: pass
    human_judgment: false
  - id: D2
    description: "Ordering, nesting, Array, Map value, root SimpleAggregateFunction, positional, empty-row, null-tupleFields and no-dedup edges of the unpaired-element WARN"
    requirement: COLS-14
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest COLS-14 unpaired-element WARN (D-01) and (D-02) pins (9 testcases)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The three D-19 collided tests are flipped with names and assertions unchanged (15 COLS-10 (D-11 flip) testcases survive) and the root-only input is asserted to WARN in sibling tests"
    requirement: COLS-14
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest#tupleRootRowAloneLogsOneUnpairedElementWarning and #repeatedDiscoveryOfARootRowAloneWarnsEveryTime"
        status: pass
    human_judgment: false
  - id: D4
    description: "Known defect 999.4 pinned Docker-free through the real shouldSkipDescribeRow: a Tuple element named size or size0 is dropped by discovery and one WARN names it"
    requirement: COLS-14
    verification:
      - kind: unit
        ref: "ClickHouseHelperClientForkUnitTest#COLS-14 unpaired-element WARN (D-04, known defect 999.4) (3 cases from 23-EVIDENCE.md R-01)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The new WARN's column assertions fail under both D-17 mutations (column argument dropped, column placeholder dropped) while the fallback-WARN column assertions stay green"
    requirement: DEBT-10
    verification:
      - kind: unit
        ref: "d17-unpaired-m1 and d17-unpaired-m2: 16 of 16 COLS-14 testcases red with the WR-02 column segment text, 14 fallback testcases green"
        status: pass
    human_judgment: false
  - id: D6
    description: "The six live classes that exercise the pass stay green on latest x V2 (81 testcases) and testUnit is green"
    requirement: COLS-14
    verification:
      - kind: integration
        ref: "live-six-23-05: NullableTupleMatrixLiveTest 33, SimpleAggregateTupleRoundTripLiveTest 2, NullableEnumRoundTripLiveTest 6, NestedNullRowBinaryLiveTest 10, NestedNullableCompositeWriteTest 18, ClickHouseHelperClientForkLiveTest 12"
        status: pass
      - kind: unit
        ref: "./gradlew testUnit --rerun: 838 tests, 836 succeeded, 0 failed, 2 skipped"
        status: pass
    human_judgment: false
  - id: D7
    description: "ROADMAP Phase 23 SC3 (amended 2026-10-20 per 23-CONTEXT D-01) matches the shipped behaviour: log one WARN and still apply"
    requirement: COLS-14
    verification: []
    human_judgment: true
    rationale: "Whether the shipped log-and-still-apply behaviour satisfies SC3 is the verifier's reading; E-05 sc3_reading records the match and the amendment, but the judgment is not asserted by a test"

plan_head_before: 3c50c412dbdd6fe8e01bcf01f4e14530195ce1f6
plan_head_after: 55965c45ced8e588cf344ce42eb23b02990133b1
commits: 2

duration: 8min
completed: 2026-10-20
status: complete
---

# Phase 23 Plan 05: Unpaired-element WARN for declared Tuple elements with no column Summary

**`DeclaredNullability.apply` now logs one metadata-only WARN naming every declared Tuple element that has no DESCRIBE sub-column and still applies the declared nullability to the elements that paired; the `size`/`size0` discovery defect behind it is pinned Docker-free and recorded as backlog 999.4.**

## Performance

- **Duration:** about 8 min
- **Started:** 2026-10-20T12:01:48Z
- **Completed:** 2026-10-20T12:09Z
- **Tasks:** 3
- **Files modified:** 8 (7 in the `fix(23-05)` commit, 1 evidence file in the `docs(23-05)` commit)

## Accomplishments

- Production: `collectTuple` keys the declared elements in a `LinkedHashMap`, records each paired key and, after the loop, adds `prefix + key` for every declared key that did not pair to a new `unpaired` list threaded through `collect`. `apply` logs `UNPAIRED_ELEMENT_WARNING` once, after the assignments are applied, and returns true. The fallback paths are untouched, there is no static state, and the type is still parsed once per root per DESCRIBE.
- D-01 is written down in the class Javadoc ("A Tuple Column never legitimately lacks a declared element") and in the FORK-DELTA COLS-10 row, with `COLS-14` added to its requirements cell.
- Tracer: the sibling of `missingDeclaredElementDoesNotShiftPairing` failed alone on the unchanged code (1 of 53); the pinned test's block is byte-identical to 16e0b2d6 (md5 `370e9b49a3e07474a4ee625a7e459b3e`).
- D-19 (user Option B): against the new pass exactly the three collided tests failed. They now carry their element rows with names and assertions unchanged, each with a scoped AGENTS.md exemption in its `// COLS-14 flip (D-19)` comment, and the root-only inputs live in `tupleRootRowAloneLogsOneUnpairedElementWarning` (2 cases) and `repeatedDiscoveryOfARootRowAloneWarnsEveryTime`.
- Expansion: nine `DeclaredNullabilityTest` pins (declaration order, nested path, Array, Map value, root SimpleAggregateFunction, positional, empty row, null `tupleFields`, no dedup) and three D-04 cases through the new `discoverAsDescribeTableV2` helper. By file swap all 16 `COLS-14 unpaired-element WARN` testcases failed on the unchanged code and the other 97 of 113 passed.
- D-17 for the new WARN: both mutations failed all 16 testcases with the `WR-02 column segment: ` text while the 14 fallback-WARN column testcases stayed green.
- Bookkeeping in the same commit: FORK-DELTA clause, anchors row `UNPAIRED_ELEMENT_WARNING` 2 `COLS-14` (mirror rows=69 bad=0), ledger row L-51, and ROADMAP backlog entries 999.4 and 999.5 (14 lines added, none removed). Evidence entry E-05 follows in its own commit.

## Task Commits

1. **Task 1 (tracer): unpaired-element WARN end to end** - not committed on its own (plan: one `fix(23-05)` commit)
2. **Task 2: expansion pins and the D-04 known-defect 999.4 pin** - not committed on its own
3. **Task 3: bookkeeping, D-17 mutations, six live classes, commit** - `f83ac147b5aa733c0a7b326d603a416b5cbba38c` (fix)

**Evidence:** `55965c45ced8e588cf344ce42eb23b02990133b1` (docs, E-05). **Plan metadata:** the summary and state commits that follow.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java` - `UNPAIRED_ELEMENT_WARNING`, `unpaired` list through `collect`/`collectTuple`, D-01 Javadoc paragraph
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - 9 new testcases (53 to 62 with the tracer sibling), two helpers, two premise assertions in the `DEBT-10:` test
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - D-19 flips, `formerlyUnflaggedTypes()` and `discoveryDropsSizeNamedElements()` providers, three siblings, `discoverAsDescribeTableV2` (45 to 51 testcases)
- `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `23-BEHAVIOUR-LEDGER.md` (L-51), `.planning/ROADMAP.md` (999.4, 999.5), `23-EVIDENCE.md` (E-05)

## Decisions Made

- Followed D-01, D-02, D-03, D-04 and D-19 as locked in CONTEXT; no new checkpoint was needed.
- The literal `keeping the nullability DESCRIBE reported` is held in a private test constant (`FALLBACK_KEEPING_CLAUSE`) rather than repeated, in line with the project's static-constant convention.
- The D-04 pin asserts that every field of the Tuple is non-nullable, which covers the `size0` case where DESCRIBE reported `Nullable(String)` for the paired element.

## Deviations from Plan

None - plan executed exactly as written.

Notes that are not deviations: the swap run temporarily removed the two Task 1 premise assertions from the test file, as the plan prescribes, and restored the file with `cmp`.

## Issues Encountered

None. No OpenJ9 GPF occurred; every run passed or failed as predicted on the first attempt.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or file access. The new WARN prints only DDL element names, column, database, table and raw type (T-23-12), and no throwable.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- COLS-14 closes ROADMAP Phase 23 SC3 (amended text) and the unpaired-WARN half of SC4. L-51 is queued for the Phase 24 CHANGELOG, FORK-DELTA and ROLL-03 text. Plan 23-06 records the D-01 rule in the 19-REVIEW IN-04 disposition.
- Backlog 999.4 is a real defect for tables with a Tuple element named `size` or `size0`; it is deliberately not fixed here (D-03, D-05).
- Nothing is pushed. `.planning/state.json` and `.planning/milestone.lock` were left untouched.

## Self-Check: PASSED

- Key files exist on disk (DeclaredNullability.java, DeclaredNullabilityTest.java, ClickHouseHelperClientForkUnitTest.java, docs/FORK-DELTA.md, fork-anchors.txt).
- Commits `f83ac147b5aa733c0a7b326d603a416b5cbba38c` and `55965c45ced8e588cf344ce42eb23b02990133b1` are ancestors of HEAD.
- Plan gates printed: TRACER-AND-D19-SEEN-RED, TRACER-GREEN, NO-MUTABLE-STATIC-STATE, EXPANSION-DISCRIMINATES, EXPANSION-GREEN, UNPAIRED-MUTATIONS-RED, LIVE-SIX-GREEN, SPOTLESS-BASELINE-EQUAL, COLS14-COMMITTED.

---
*Phase: 23-declared-nullability-hardening-review-closure*
*Completed: 2026-10-20*
