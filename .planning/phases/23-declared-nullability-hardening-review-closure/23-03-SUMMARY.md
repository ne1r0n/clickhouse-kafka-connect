---
phase: 23-declared-nullability-hardening-review-closure
plan: "03"
subsystem: testing
tags: [java, junit5, declared-nullability, mutation-testing, slf4j, review-closure]

requires:
  - phase: 23-declared-nullability-hardening-review-closure
    provides: 23-02 DeclaredNullabilityFixtures (ROOT_SENTINEL, assertWarnNamesColumn) and the three contiguous `FALLBACK_WARNING, columnName, ` WARN calls
provides:
  - the three pre-existing fallback-WARN content assertions (19-REVIEW WR-02) name the column through the sentinel root and the delimited `column <name> of table` segment
  - the DEBT-10 premise test for the sentinel
  - evidence entry E-03 listing both D-17 mutation runs and the 14 testcases that went red under each
affects: [23-05 COLS-14, 23-06 DEBT-12 closure pointers (WR-02 fixed pointer)]

actuals:
  tokens: 1800
  tasks: 2
  commits: 2
plan_head_before: 453e9eff7c7863156397174957361a716b9c5c42
plan_head_after: 97219bbcd2d53421b556b2f8770d56bacbb63f78

tech-stack:
  added: []
  patterns:
    - "A WARN-content assertion is proven by mutation: drop the argument, drop the placeholder, see the testcase red with its marker text, revert, see it green"
    - "A premise test pins why a sentinel is valid (absent from fixed WARN text, plain ASCII, distinct from db and table)"

key-files:
  created: []
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md

key-decisions:
  - "The tracer and the expansion share one test(23-03) commit: Task 1 left the converted method uncommitted so that nothing red or half-converted reaches history, matching the plan's one-test-commit shape"
  - "The premise test uses assertNotEquals for the database and table checks instead of assertFalse(equals), which gives a readable failure and adds no assertion lines to the converted methods"

patterns-established:
  - "Mutation 2 (placeholder dropped) shifts every later SLF4J argument by one slot, so the sentinel lands in the table slot; only the delimited segment catches it"

requirements-completed: [DEBT-10]

coverage:
  - id: D1
    description: "unparsableDeclaredTypeFailsOpenWithOneMetadataWarning, structureMismatchFailsOpenWithOneWarning and the discovery fixture fallbackWarningNamesDatabaseAndTable use ROOT_SENTINEL and assertWarnNamesColumn; the bare one-letter contains checks are gone and no assertion line was dropped (8, 7 and 3 lines before and after)"
    requirement: DEBT-10
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest + ClickHouseHelperClientForkUnitTest: 39 and 45 testcases, 0 failures; testUnit 809 tests, 807 succeeded, 0 failed, 2 skipped"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every fallback-WARN column assertion (the three converted ones plus the COLS-12 (D-07) / (D-08 flip) / (D-09) pins) fails with the `WR-02 column segment: ` text under both D-17 mutations: 14 of 14 under the dropped column argument and 14 of 14 under the dropped placeholder"
    requirement: DEBT-10
    verification:
      - kind: unit
        ref: "/tmp/gsd23/exec/d17-fallback-m1-{dn,fork}.xml and d17-fallback-m2-{dn,fork}.xml (rc=1 both); names in 23-EVIDENCE.md E-03"
        status: pass
    human_judgment: false
  - id: D3
    description: "The sentinel premise (not a substring of FALLBACK_WARNING, matches [a-z_]+, differs from db and t) is pinned by a test"
    requirement: DEBT-10
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#DEBT-10: the sentinel root occurs in no fixed WARN text, is plain ASCII and differs from the database and table"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-20
status: complete
---

# Phase 23 Plan 03: DEBT-10 Fallback-WARN Column Assertions Summary

**The three fallback-WARN content assertions now check the delimited `column nullability_root of table` segment against a sentinel root, and each was seen red under both D-17 mutations (column argument dropped, placeholder dropped).**

## Performance

- **Duration:** about 8 min
- **Started:** 2026-10-20T11:47:00Z (approximate)
- **Completed:** 2026-10-20T11:55:00Z
- **Tasks:** 2
- **Files modified:** 3 (two test classes and the evidence file); no production change

## Accomplishments
- `unparsableDeclaredTypeFailsOpenWithOneMetadataWarning` was the tracer. It was converted first and went red with `WR-02 column segment: ` under mutation 1 and mutation 2 (both rc=1), then green after the revert (38 testcases).
- `structureMismatchFailsOpenWithOneWarning` and the fixture of `ClickHouseHelperClientForkUnitTest.fallbackWarningNamesDatabaseAndTable` moved to `ROOT_SENTINEL`. The bare `contains(ROOT)` check is on 0 lines in DeclaredNullabilityTest (2 before) and the `"c"` contains check on 0 lines in the fork unit test (1 before). Assertion-line counts are unchanged: 8, 7 and 3.
- Both mutations were run over both classes. Exactly 14 testcases in the expected set (three converted names, the `COLS-12 (D-07) structure mismatch` pins including the kept Array pin, `COLS-12 (D-08 flip)`, `COLS-12 (D-09)`) failed with the marker text under each, with none missing. E-03 records the names, both return codes and the revert.
- The DEBT-10 premise test was added. Total after this plan: DeclaredNullabilityTest 39 testcases, fork unit test 45, `testUnit` 809 tests with 0 failed.

## Task Commits

1. **Task 1: tracer, one assertion end to end** - no separate commit by design (converted, seen red twice, reverted, green; held in the tree for the single test commit)
2. **Task 2: remaining conversions, premise test, both mutations over both classes** - `075e6466` (test)

**Evidence:** `97219bbc` (docs: E-03). **Plan metadata:** the SUMMARY and state commit that follows this file.

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - two converted methods, the premise test, `assertNotEquals` import
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - static imports of the sentinel and the assertion; converted fixture
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md` - E-03 and its Index row flipped to recorded

## Decisions Made
- The tracer's conversion stays uncommitted until Task 2 completes, so history holds one test commit with exactly the two test files and never a mutation or a red run.
- The premise test uses `assertNotEquals` for the db and table distinctness checks.

## Deviations from Plan

None - plan executed exactly as written.

The assertion-line counts of all three converted methods were measured once, together, before the first edit (the plan splits the measurement between the two tasks); the values are identical either way.

## Issues Encountered
- No OpenJ9 exit 255 occurred in any Gradle run; no core or javacore files were produced.
- The anchors-mirror loop was run under `bash -c`, as the earlier plan found it clobbers `PATH` under zsh.
- Under both mutations other testcases that assert `db.t` without a column assertion (for example `unmatchedElementAppliesNothing`) also failed, without the marker text. That is expected and they are not in the D-17 set.

## Known Stubs

None.

## Threat Flags

None - test-only change; no new network, auth, file-access or schema surface.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- 19-REVIEW WR-02 is ready for its `fixed` pointer in DEBT-12 (plan 23-06).
- Plan 23-05 adds the unpaired-WARN constant to the premise test and reuses the same two-mutation procedure for COLS-14.
- `src/main` is byte-identical to the `fix(23-02)` commit `4bbc9627`.

## Self-Check: PASSED

- Modified files exist; commits `075e6466` and `97219bbc` are ancestors of HEAD.
- Gates printed TRACER-SEEN-RED-BOTH, TRACER-GREEN, FALLBACK-MUTATIONS-RED, SPOTLESS-BASELINE-EQUAL (46 files, md5 f2e32c7bc069ecd155830f82d26285d6), DEBT10-COMMITTED; the anchors mirror reads rows=66 bad=0.
- `git diff --quiet 4bbc9627 HEAD -- src/main` holds.

---
*Phase: 23-declared-nullability-hardening-review-closure*
*Completed: 2026-10-20*
