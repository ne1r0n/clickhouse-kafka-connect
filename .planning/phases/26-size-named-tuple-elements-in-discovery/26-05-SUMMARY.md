---
phase: 26-size-named-tuple-elements-in-discovery
plan: "05"
subsystem: testing
tags: [phase-gate, testUnit, full-suite, anchors-invariant, spotless, openj9, hotspot]

requires:
  - phase: 26-size-named-tuple-elements-in-discovery
    provides: plans 26-01..26-04 (fixtures, DescribeTableBuilder extraction, size-named Tuple fix, live round-trip)
provides:
  - E-12 in 26-COMMAND-EVIDENCE.md, the Phase 26 gate (all of E-01..E-12 pass)
  - a measured full suite of 1709 tests, 0 failed, 18 named skips on ClickHouse latest (26.9.19.9) x client V2
affects: [27, 28, 29]

actuals:
  tokens: 6000
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Phase gate = cache-busted testUnit + one full suite with exact class counts + tail gate + self-test on one HEAD"

key-files:
  created: []
  modified:
    - .planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md

key-decisions:
  - "The gate suite was run on HotSpot OpenJDK 21 (-PjavaVersion=21) after three deterministic OpenJ9 GPF crashes on the default Semeru 17 toolchain; no test or build file was changed"

requirements-completed: [DESC-06, DESC-07, DESC-08, DESC-09, DESC-10]

coverage:
  - id: D1
    description: "Cache-busted testUnit is green with 1126 tests"
    requirement: DESC-07
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun (Test result: SUCCESS, 1126 tests, 0 failed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Full suite on latest x V2: 1709 tests in 97 XML files, 0 failures, 0 errors, the 18 named skips, exact class counts, DESC-05 and DESC-02 live rows green, one server version"
    requirement: DESC-09
    verification:
      - kind: integration
        ref: "/tmp/gsd26/exec/suite-check.py over build/test-results/test (run 4, -PjavaVersion=21)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Anchors invariant at every phase commit (anchor_rows=77, result=PASS) and the self-test proves the gate can still fail (no failures)"
    requirement: DESC-10
    verification:
      - kind: other
        ref: ".planning/scripts/verify-merge-invariants.sh 38265d26 HEAD; .planning/scripts/verify-merge-invariants-selftest.sh"
        status: pass
    human_judgment: false
  - id: D4
    description: "Spotless baseline-equal and nothing pushed (origin/custom_build still 69cd680b)"
    verification:
      - kind: other
        ref: "spotless-numstat.sh baseline-v1.7.0_gg4 vs HEAD (cmp); git ls-remote origin"
        status: pass
    human_judgment: false

plan_head_before: 1f2f156182da83d8ba4bedd8d0a76952454e759d
plan_head_after: 6535a3a92812f5c9c5fd82e07d80964ef2c725b9
duration: 30min
completed: 2026-10-21
status: complete
---

# Phase 26 Plan 05: Phase gate Summary

**The Phase 26 gate is green and recorded as E-12: testUnit 1126 tests, a 1709-test full suite on ClickHouse 26.9.19.9 x client V2 with exact class counts, anchors invariant PASS at anchor_rows=77, a self-test with no failures, and nothing pushed.**

## Performance

- **Duration:** about 30 min (the 12-minute self-test ran in the background alongside the suites)
- **Started:** 2026-10-21T21:46Z (first gate command)
- **Completed:** 2026-10-21T22:13Z
- **Tasks:** 2
- **Files modified:** 1 (26-COMMAND-EVIDENCE.md)

## Accomplishments

- testUnit with `--rerun`: SUCCESS, 1126 tests, 1124 succeeded, 0 failed, 2 skipped (exactly the expected floor of 860 + 95 + 47 + 4 + 120).
- Full suite (run 4): 1709 tests in 97 XML files, 0 failures, 0 errors, the 18 named skips, all ten exact class counts (94, 95, 47, 46, 51, 66, 33, 2, 4, 3), DescribeTableBuilderUnitTest 120, ClickHouseHelperClientForkLiveTest DESC-05 and DESC-02 rows green, no stale XML, one server version (26.9.19.9, image id unchanged from SERVER_VERSION.txt).
- Tail gate `anchor_rows=77 ... result=PASS` (re-run on the post-evidence HEAD too: anchor_commits 37); self-test exit 0, 16 PASS, 0 FAIL, last line `selftest: no failures`; both gate scripts equal baseline-v1.7.0_gg4.
- Spotless: baseline and HEAD outputs both 46 lines and byte-equal.
- Freeze: origin/custom_build 69cd680bf548bfb0329142752d37f8a6c3190c26, no sync/v1.7.0 branch and no baseline tag on origin.

## Task Commits

1. **Task 1: testUnit, full suite, spotless, freeze** - no commit (the plan commits nothing in this task)
2. **Task 2: tail gate, self-test, E-12, evidence commit** - `6535a3a9` (docs)

**Plan metadata:** the docs(26-05) completion commit that carries this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md` - E-12 appended, index row E-12 flipped to pass

## Decisions Made

- Ran the gate suite with `-PjavaVersion=21` (the build's own existing property, HotSpot OpenJDK 21.0.12.1) after the default OpenJ9 toolchain crashed three times. CI uses Temurin (HotSpot) 17, so a HotSpot run is closer to CI than Semeru is. No test, build or gate file was edited.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Full suite crashed deterministically on the default OpenJ9 toolchain**
- **Found during:** Task 1 (full suite)
- **Issue:** Three consecutive runs of `./gradlew test --rerun` on Semeru 17.0.9 (OpenJ9) died at the same point (1595 tests, 19 skipped, "Gradle Test Executor N finished with non-zero exit value 255"). The javacore shows a "gpf" in `InstrumentationImpl.retransformClasses0` under Mockito `InlineBytecodeGenerator`, called from `Mockito.spy` at ClickHouseWriterTest.staleDescribeTableIsNotCachedAsCurrentSchema (line 445), the same site as the known Phase 18 and Phase 21 crashes. The host was under heavy memory pressure (swap 28.4 of 29.7 GB used, about 60 MB of free pages), which is the likely reason a re-run did not clear it. The crashed forks wrote no XML, so 0 failures in those runs does not count as a pass.
- **Fix:** Deleted each run's `core.*.dmp`, `javacore.*`, `Snap.*.trc` and `jitdump.*` files one by one, then ran the suite once with `-PjavaVersion=21`; it ended BUILD SUCCESSFUL with 1709 tests.
- **Files modified:** none
- **Verification:** the plan's own checks (counts, skip set, stale-XML, DESC rows, single server version) pass on the run-4 results; all four runs are recorded in E-12.
- **Committed in:** 6535a3a9 (E-12)

---

**Total deviations:** 1 auto-fixed (1 blocking, environmental)
**Impact on plan:** The gate result is for the same sources, tests and ClickHouse server, on HotSpot 21 instead of the default Semeru 17. Nothing in the code under test is implicated. The lasting fix (build the mocked helper without Mockito, or run on a HotSpot JDK 17 locally) stays deferred as in Phase 18's deferred-items.

## Issues Encountered

- The plan's verification line "git log baseline-v1.7.0_gg4..HEAD -- . ':(exclude).planning' lists test(26-01), refactor(26-02), fix(26-03), test(26-04) and nothing else" is slightly off: docs(26-04) `f1fca3a3` also touches non-planning `docs/FORK-DELTA.md` (it cites the live round-trip). That is the intended plan 26-04 output, not a stray change.
- The orchestrator's earlier gate run on this tip was green (1709 tests); on this host the default toolchain no longer completes because of memory pressure, so the host state, not the tree, differs.

## Requirements

DESC-06..DESC-10 are in this plan's frontmatter. `requirements.ready-ids` released all five (blocked: none) and DESC-08, DESC-09 and DESC-10 were marked Complete in REQUIREMENTS.md (DESC-06 and DESC-07 already were).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 26 has all five plans done; the phase is ready for verification and the closing steps. Nothing was pushed; origin/custom_build is still 69cd680b.
- The unstaged `gradlew.bat` CRLF diff, `.planning/state.json` and `.planning/milestone.lock` were left unstaged as instructed.
- Free memory or use a HotSpot JDK before the next full local suite, or expect the OpenJ9 Mockito crash again.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- 26-COMMAND-EVIDENCE.md holds `### E-12:` and the index lists E-01..E-12 all pass.
- Commit `6535a3a9` is an ancestor of HEAD.
- PHASE26-GATE-PASS printed by the plan's Task 2 verify command.

---
*Phase: 26-size-named-tuple-elements-in-discovery*
*Completed: 2026-10-21*
