---
phase: 28-raw-identifier-names-in-discovery
plan: "01"
subsystem: testing
tags: [declared-nullability, normalise, junit5, mutation-proof, evidence]

requires:
  - phase: 27-writer-failure-path-test-debt
    provides: the green Phase 27 base (1680-test suite, 83 fork-anchor rows) that E-01 re-measures
provides:
  - NAME-01 pin of the escaped-quote branch of DeclaredNullability.normalise, committed before any decode change
  - the outside-quotes row for two backslashes before n
  - 28-COMMAND-EVIDENCE.md with the index E-01..E-13, the phase base (E-01) and the NAME-01 record (E-02)
affects: [28-02, 28-03, 28-04, 28-05, 28-06]

actuals:
  tokens: 3000
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Pin a hand-written parser branch on the current code, then prove it with a working-tree mutation reverted before staging"

key-files:
  created:
    - .planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java

key-decisions:
  - "The run == 1 guard of normalise is an equivalent mutant (startsWith at the run start already implies a run of one), so its row is a behaviour pin without a mutation proof"
  - "The phase base is f3b1c487 (the orchestrator commit after planning); src is unchanged since planning commit 51c00499"

requirements-completed: [NAME-01]

coverage:
  - id: D1
    description: "escapedBacktickInsideANameKeepsTheQuoteOpen pins the 23-REVIEW WR-01 literal on the current normalise; green, and red (exactly this test) under the always-close mutation"
    requirement: NAME-01
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#escapedBacktickInsideANameKeepsTheQuoteOpen; mutation A failing set equals this test"
        status: pass
    human_judgment: false
  - id: D2
    description: "twoBackslashesBeforeNOutsideQuotesAreNotALineBreak pins that a backslash pair followed by n outside quotes is kept as text"
    requirement: NAME-01
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#twoBackslashesBeforeNOutsideQuotesAreNotALineBreak"
        status: pass
    human_judgment: false
  - id: D3
    description: "28-COMMAND-EVIDENCE.md records the phase base E-01 (testUnit 1191, full suite 1680 in 97 XML files with 18 skips, anchor_rows=83, 46-line Spotless base, image id) and E-02"
    verification:
      - kind: other
        ref: "plan 28-01 Task 1 and Task 2 automated verify (TRACER-GREEN, NAME01-GATED)"
        status: pass
    human_judgment: false

plan_head_before: f3b1c48774bc855913ee69c67b18508f23d84367
plan_head_after: 75010a63949d1d8e2c063d8de3b0a44bb5276f90

duration: 11min
completed: 2026-10-23
status: complete
---

# Phase 28 Plan 01: NAME-01 pin of the escaped-quote branch Summary

**Two Docker-free DeclaredNullabilityTest pins for DeclaredNullability.normalise (the WR-01 escaped-backtick literal, red under the always-close mutation, and the outside-quotes backslash pair), committed before any decode change, plus the Phase 28 evidence file with the measured phase base.**

## Performance

- **Duration:** 11 min
- **Started:** 2026-10-23T09:27:12Z
- **Completed:** 2026-10-23T09:38:00Z
- **Tasks:** 2
- **Files modified:** 2 (one test file, one new evidence file)

## Accomplishments
- `escapedBacktickInsideANameKeepsTheQuoteOpen` uses the two Java literals of 23-REVIEW WR-01 verbatim (checked with `grep -F` against lines 49 and 50). It is green on the current `normalise`. Under mutation A (`if (escaped == quote) {`) the failing set of DeclaredNullabilityTest is exactly this test. Under mutation B (parity inverted) it fails together with the COLS-13 trailing-backslash test. `src/main` was restored to HEAD after each.
- `twoBackslashesBeforeNOutsideQuotesAreNotALineBreak` pins that `Tuple(a Int32,\\n b String)` is returned unchanged. E-02 records that the `run == 1` guard is an equivalent mutant.
- E-01 phase base: testUnit 1191 tests (0 failed, 2 skipped); full suite 1680 tests in 97 XML files (0 failures, 0 errors, 18 skipped) on ClickHouse 26.9.19.9 (`latest`, image `sha256:bdb5c911...`); `anchor_rows=83 result=PASS`; Spotless base 46 lines; `origin_custom_build_tip` 69cd680b.
- At the test commit: testUnit 1193 tests, 0 failed; Spotless numstat byte-equal to the base; tail gate `anchor_rows=83`, `result=PASS`; no `src/main` difference from the phase base.

## Task Commits

1. **Task 1: Tracer, phase base E-01, WR-01 literal green and red under mutation** - no commit of its own (edits land in the Task 2 commits, as the plan directs)
2. **Task 2: outside-quotes row, test commit, gates, E-02** - `1f984bef` (test) and `75010a63` (docs)

**Plan metadata:** the `docs(28-01): complete ...` commit that carries this SUMMARY, STATE.md and ROADMAP.md.

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - the two new tests (15 inserted lines, 0 removed)
- `.planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md` - index E-01..E-13, E-01 and E-02 entries

## Decisions Made
- The `run == 1` guard is not mutation-provable, so it is recorded as an equivalent mutant rather than given a contrived test.
- The phase base is `f3b1c487`; the measured baselines match the planning-time ones exactly.

## Deviations from Plan

None - plan executed exactly as written.

Two environment notes, not deviations: the executor shell did not have `gsd_run` defined, so a one-line shim calling `node ~/.claude/gsd-core/bin/gsd-tools.cjs` was placed in `/tmp/gsd28/exec/gsdrun.sh`; and the E-01 timestamp was corrected to the real clock time before committing.

## Issues Encountered
None. No OpenJ9 crash occurred in any Gradle run.

## Known Stubs
None.

## Threat Flags
None. The plan adds tests and an evidence file only.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- The escaped-quote branch is pinned on the code as Phase 26 left it, so plan 28-03's rewrite of `normalise` will be a visible, red-first change.
- E-01 holds the base (`phase_base`, `origin_custom_build_tip`, class counts, Spotless base) that plans 28-02 to 28-06 measure against. Ready for 28-02.

---
*Phase: 28-raw-identifier-names-in-discovery*
*Completed: 2026-10-23*

## Self-Check: PASSED

Verified: both created and modified files exist; commits `1f984bef` and `75010a63` are ancestors of HEAD; the Task 1 and Task 2 automated verifies printed TRACER-GREEN and NAME01-GATED.
