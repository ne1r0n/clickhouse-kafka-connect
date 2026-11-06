---
phase: 48-phase-37-dispositions-document-fixes
plan: "03"
subsystem: testing
tags: [28-in-01, constant-move, declared-nullability, compile-red, full-suite, tail-gate, fork-anchors]
status: complete

requires:
  - phase: 48-phase-37-dispositions-document-fixes
    provides: plan 48-01 baseline B0 c77d1a14 (suite 2898, dn_tests 69, planning_guard_tests 18, ntm 33, sat 2), plan 48-02 E-09 (testUnit 2320) and 48-expected-added.tsv (25 rows)
provides:
  - 28 IN-01 fixed as one revert unit: ESCAPED_NEWLINE no longer a public production constant, held once in DeclaredNullabilityFixtures
  - E-10 (compile red of 8 errors, own testUnit gate) and E-11 (tail gate PASS anchor_rows=218, one green full suite 2923 on ClickHouse 26.10.3.7 x V2)
affects: [48-05, 49]

actuals:
  tokens: 4800
  tasks: 2
  commits: 3
plan_head_before: 00d1929cd3259a61750f91475dc9a05cb4853942
plan_head_after: 0c79bccae510e59ba3548cf4c90237b0389eac1a

tech-stack:
  added: []
  patterns: [compile red as the red state of a pure move, normalisation rule that proves a move changed only references and imports, full suite gated on an explicit per-testcase delta against the baseline dump]

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md

key-decisions:
  - "The constant sits at the top of DeclaredNullabilityFixtures with no blank line added after it, so the plan's normalisation rule for that file (comments and the constant line set aside) holds"
  - "The class Javadoc paragraph of DeclaredNullability that the finding also names is left as it is (D-3): reflowing it is a production diff beyond the move"

requirements-completed: [P37-01]

coverage:
  - id: D1
    description: "28 IN-01 (U-4): with only the three ESCAPED_NEWLINE lines deleted from DeclaredNullability.java, compileTestJava fails with exactly 8 errors (6 DeclaredNullabilityTest, 1 NullableTupleMatrixLiveTest, 1 SimpleAggregateTupleRoundTripLiveTest) before any test edit; the move commit holds exactly the five files, src/main numstat 0 3, every other test line unchanged under the normalisation rule, no assertion value changed"
    requirement: P37-01
    verification:
      - kind: other
        ref: "P48-03-T1-OK (E-10)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The move's own gate: cache-busted testUnit with Docker unreachable 2320 tests (E-09's count), 0 failures, 2 skips, per-testcase delta against E-09 removed=0 added=0; DeclaredNullabilityTest 69 and SourcePlanningIdGuardUnitTest 18 at E-01's counts; anchors rows=218 bad=0"
    requirement: P37-01
    verification:
      - kind: other
        ref: "P48-03-T1-OK (E-10)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The gates a src/main change requires: unedited tail gate over 12e2cf17..e0c0c9d8 PASS with anchor_rows=218 anchor_exempt=0, and one full suite on ClickHouse latest (26.10.3.7, digest sha256:739e099a...) x client V2: 2923 tests, 0 failures, 0 errors, the 18 named skips, by-class delta against E-01 removed=0 added=25 expected=25, NullableTupleMatrixLiveTest 33 and SimpleAggregateTupleRoundTripLiveTest 2 green. A first suite attempt overlapped the tail gate and failed with 8 initializationError (Ryuk); it is recorded in E-11"
    requirement: P37-01
    verification:
      - kind: other
        ref: "P48-03-T2-OK (E-11)"
        status: pass
    human_judgment: false

duration: 32min
completed: 2026-11-06
---

# Phase 48 Plan 03: 28 IN-01 move of ESCAPED_NEWLINE Summary

**The unused public `ESCAPED_NEWLINE` constant moved out of `DeclaredNullability` (three deleted production lines, nothing else) into the test fixtures with a recorded 8-error compile red first, then the tail gate PASS and one green full suite of 2923 tests on ClickHouse 26.10.3.7 x V2.**

## Performance

- **Duration:** about 32 min wall clock (about 7 min of Gradle runs, the first full suite attempt and the tail gate overlapped, then a second suite run of 3 min 23 s and a verify that re-runs the tail gate)
- **Started:** 2026-11-06T08:46Z
- **Completed:** 2026-11-06T09:17Z
- **Tasks:** 2
- **Files:** one production file (three lines deleted), four test files, evidence file; no other `src/main`, `.github`, `.planning/scripts` or build change

## Accomplishments

- **The move commit `c4ec0cbf`** holds exactly `DeclaredNullability.java` (numstat `0 3`: the one-line Javadoc, the constant and the blank line after it) and the four test files. `DeclaredNullabilityFixtures` holds the constant once (`"\\n"`, backslash and n) and says `{@link #ESCAPED_NEWLINE}` lives there because production code no longer reads it; the 8 references (6 in `DeclaredNullabilityTest`, 1 each in the two live tests) read `DeclaredNullabilityFixtures.ESCAPED_NEWLINE`; the two live tests gain the fixtures import and keep the `DeclaredNullability` import. No assertion value changed; the normalisation check printed `only the three main lines, the references, imports, comments and the constant`.
- **The red (E-10):** with only the three production lines deleted, `compileTestJava` exited 1 with `8 errors`: `DeclaredNullabilityTest.java=6 NullableTupleMatrixLiveTest.java=1 SimpleAggregateTupleRoundTripLiveTest.java=1`.
- **The move's gate (E-10):** cache-busted testUnit with `DOCKER_HOST=unix:///nonexistent.sock` read 2320 tests (E-09's count), 0 failures, 0 errors, 2 skips, 107 XML files; delta against E-09 `removed=0 added=0 expected=0 match=yes`; `DeclaredNullabilityTest` 69, `SourcePlanningIdGuardUnitTest` 18; anchors `rows=218 bad=0`; per-file Spotless check identical for all five files.
- **The tail gate (E-11):** over `12e2cf17..e0c0c9d8` on the explicit SHA: `invariants: provenance_paths=11 anchor_commits=149 anchor_rows=218 marker_commits=167 anchor_exempt=0 result=PASS`, exit 0. `src` was identical between the move commit and the gate head.
- **The full suite (E-11, second attempt, alone):** 2923 tests (E-01's 2898 plus 25), 0 failures, 0 errors, 18 named skips (`named=7 state_mismatch_cases=11 unexpected=0`), 156 XML files, exit 0. By-class delta against E-01 `removed=0 added=25 expected=25 match=yes`. `NullableTupleMatrixLiveTest` 33 and `SimpleAggregateTupleRoundTripLiveTest` 2 (the two live readers of the moved constant) and `DeclaredNullabilityTest` 69 are green at E-01's counts.
- **Server and image:** ClickHouse `26.10.3.7`, `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`. `latest` did not drift since E-01.
- **JVM crash and Java:** no crash, no core dump; the default toolchain ran every build, no `-PjavaVersion=21` run.
- **D-3:** the class Javadoc paragraph of `DeclaredNullability` that finding 28 IN-01 asks to reflow was left undone; it would be a production diff beyond the three-line move (to be recorded in the plan 48-05 ledger).

## Task Commits

1. **Task 1 (tracer): the move** - `c4ec0cbf` (fix), evidence E-10 `e0c0c9d8` (docs)
2. **Task 2: tail gate and full suite** - evidence E-11 `0c79bcca` (docs)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java` - three lines deleted
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java` - the constant and its Javadoc added, the class Javadoc sentence replaced
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java`, `.../sink/NullableTupleMatrixLiveTest.java`, `.../sink/SimpleAggregateTupleRoundTripLiveTest.java` - references repointed, the two live tests gain one import
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md` - E-10, E-11 and the flipped index rows

## Decisions Made

- The constant's placement in the fixtures leaves no added blank line, because the plan's normalisation rule for that file treats an added non-comment line as a change.
- Nothing about the expected numbers moved; the second suite run matched them all.

## Deviations from Plan

**1. [Rule 1 - Bug in plan gate vs action] First move commit failed the plan's own normalisation check**
- **Found during:** Task 1 verify
- **Issue:** the first move commit (`406634ed`) added the constant followed by a blank line in `DeclaredNullabilityFixtures`; the plan's verify sets aside only comment lines and the constant line for that file, so the added blank line read as a changed non-comment line (`DNF: a non-comment line other than the constant changed`).
- **Fix:** rewrote my two local, unpushed commits (`git reset --mixed` back to `00d1929c`), moved the constant to the top of the class with no added blank line, re-ran the compile, the filtered run, the full testUnit and the per-file formatting check, and re-committed. E-10 and its note describe the redone tree. Nothing was pushed or recorded elsewhere before the rewrite.
- **Files modified:** `DeclaredNullabilityFixtures.java`
- **Commit:** `c4ec0cbf` (and E-10 `e0c0c9d8`)

**2. [Rule 3 - Blocking environment] First full-suite attempt failed with 8 initializationError (Ryuk)**
- **Found during:** Task 2
- **Issue:** the first suite run overlapped the tail gate (also Testcontainers-using) and ended with 2846 tests, 8 failures, 0 errors: eight `#initializationError` testcases with `Could not connect to Ryuk at localhost:36898` (`ClickHouseSinkTaskSchemalessTest`, `ClickHouseWriterTest`, `Client011BehaviourPinLiveTest`, `EmptyCdcSkipRouteLiveTest`, `NetworkCompressionPinLiveTest`, `NullableTupleMatrixLiveTest`, `RowBinaryClientParityLiveTest`, `WideIntegerTypeGateLiveTest`). No assertion failed.
- **Fix:** stopped and reported; on the coordinator's decision, Docker and colima `vz` were checked healthy with no Testcontainers or Ryuk container left over and no test executor running, and the suite was re-run once, alone. The first attempt is stated in E-11 (log `/tmp/gsd48/exec/suite-move.log`, XML `/tmp/gsd48/exec/xml-move-suite-attempt1`); the numbers in E-11 are from the second run (log `/tmp/gsd48/exec/suite-move-rerun.log`). The Ryuk-contention cause is likely, not proven.
- **Files modified:** none
- **Commit:** `0c79bcca` (evidence)

**Total deviations:** 2 (one plan gate inconsistency fixed by re-placing a constant, one environment re-run on explicit instruction). **Impact:** none on scope or results; the production diff is still the three deleted lines.

## Authentication Gates

None.

## Issues Encountered

The Ryuk start-up failure of the first suite attempt (Deviation 2). The `/tmp/gsd48/exec` inputs were intact and their md5 manifests matched E-01, so no re-base was needed.

## Known Stubs

None.

## Threat Flags

None. The production change only deletes a constant nothing in `src/main` reads; no network endpoint, auth path or schema changed, and `Processing` and `KeeperStateProvider` are untouched.

## Next Phase Readiness

Ready for plan 48-04 (documents) and then 48-05 (dry runs, ledger, phase gate). Hand-offs: the 28 IN-01 ledger row points at the move commit `c4ec0cbf` and records the unreflowed class Javadoc paragraph; Phase 49's CHANGELOG may name the removed public constant. STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/config.json` and `.planning/state.json` were not staged by any commit of this plan.

## Self-Check: PASSED

- Files exist: all five source files, `48-COMMAND-EVIDENCE.md` (tracked, committed).
- Commits exist on HEAD: `c4ec0cbf`, `e0c0c9d8`, `0c79bcca`; `git rev-list --count 00d1929c..HEAD` read 3 before this SUMMARY commit.
- `P48-03-T1-OK` and `P48-03-T2-OK` printed on the committed tip; no worktree remains; `git status --short` lists only `.planning/config.json` and `.planning/state.json`.
