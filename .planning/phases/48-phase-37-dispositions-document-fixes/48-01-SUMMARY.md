---
phase: 48-phase-37-dispositions-document-fixes
plan: "01"
subsystem: testing
tags: [baseline, attribution, evidence, path-check, id-scan, fork-delta, rollout-notes]
status: complete

requires:
  - phase: 47-task-stop-retry-classification
    provides: the sync/v1.7.0 tip, fork-anchors.txt (218 rows), the tail gate scripts, the testUnit 2295 and full-suite 2898 expectations
provides:
  - 48-COMMAND-EVIDENCE.md with the header, the E-01..E-17 index and entries E-01 and E-02
  - a green, attributable baseline on the untouched base B0 (anchors, gate self-test, tail gate, cache-busted testUnit with Docker unreachable, one full suite on latest x V2, status dumps)
  - the release path recorded frozen (5 allow-lists, gg6 and gg7 lists identical to their tags, compare script md5, gg6 note differing from its tag in three hunks)
  - 48-path-check.py (FORK-DELTA evidence-path resolver) and 48-id-scan.py (planning-identifier scan), calibrated and frozen
affects: [48-02, 48-03, 48-04, 48-05, 49]

actuals:
  tokens: 12619
  tasks: 2
  commits: 3
plan_head_before: c77d1a147ae4acd37100b73db08f1851047bc055
plan_head_after: 58be22298929531722a12e5b8cb1468dd034edf2

tech-stack:
  added: []
  patterns: [stdlib-only record scripts that read git objects with a fixed argument list, calibration table pinned at plan time, md5 recorded in evidence so later gates run the same bytes]

key-files:
  created:
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py
  modified: []

key-decisions:
  - "Both check scripts matched every calibration row on the first run; no expected number was touched"
  - "The XML snapshots stay under /tmp/gsd48/exec; E-01 records their per-file md5 manifests and the status dump md5 values so a later re-measure can be re-based if /tmp is cleared"

requirements-completed: [DOC-01, DOC-02, P37-01, P37-03]

coverage:
  - id: D1
    description: "Attribution baseline on the untouched base B0 (c77d1a14): anchors rows=218 bad=0, gate self-test clean, unedited tail gate PASS with anchor_rows=218, cache-busted testUnit with Docker unreachable 2295 tests green (2 skips, eight touched classes counted), one full suite 2898 tests green on ClickHouse 26.10.3.7 x client V2 with the 18 named skips, digest recorded, per-testcase dumps kept"
    requirement: P37-01
    verification:
      - kind: other
        ref: "P48-01-T1-OK (Task 1 automated verify; E-01)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Release path recorded frozen: 5 allow-lists, gg6 and gg7 lists byte-identical to their tag copies, compare script md5 a637e0cc8fb33a33e7673ac682fa63c8, gg6 note differing from v1.7.0_gg6 in exactly 2a3,4 112c114 146c148, ESCAPED_NEWLINE counts 1 (main) and 9 (test references)"
    requirement: P37-03
    verification:
      - kind: other
        ref: "P48-01-T1-OK (Task 1 automated verify; E-01)"
        status: pass
    human_judgment: false
  - id: D3
    description: "48-path-check.py reads FORK-DELTA on B0 as candidates=99 resolved=69 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=2 (the 25 and 35 evidence paths) and the CHANGELOG and the gg6, gg1, gg7 notes as unresolved=0"
    requirement: DOC-02
    verification:
      - kind: other
        ref: "P48-01-T2-OK (Task 2 automated verify; E-02)"
        status: pass
    human_judgment: false
  - id: D4
    description: "48-id-scan.py reads 3 hits on the gg6 note on B0 (lines 3, 114, 148), 0 on its v1.7.0_gg6 tag copy, 26 on gg1, 1 on gg7, 0 on CHANGELOG; both scripts stdlib-only and write-free, md5 values in E-02"
    requirement: DOC-01
    verification:
      - kind: other
        ref: "P48-01-T2-OK (Task 2 automated verify; E-02)"
        status: pass
    human_judgment: false

duration: 82min
completed: 2026-11-06
---

# Phase 48 Plan 01: Baseline, release-path freeze and document check scripts Summary

**Phase 48 base B0 `c77d1a14` measured green and attributable (testUnit 2295, full suite 2898 on ClickHouse 26.10.3.7 x V2, anchors 218, tail gate PASS), the release path recorded frozen, and two frozen stdlib scripts that read the DOC-01 and DOC-02 defects on the base.**

## Performance

- **Duration:** 82 min (about 79 min of it the gate self-test, which ran in the background and was the last fact E-01 needed)
- **Started:** 2026-11-06T07:11Z
- **Completed:** 2026-11-06T08:33Z
- **Tasks:** 2
- **Files created:** 3 (evidence file and two scripts); no other tracked file changed

## Accomplishments

- **Base B0 = `c77d1a147ae4acd37100b73db08f1851047bc055`** (`docs(48): record U-12 and U-13`, branch `sync/v1.7.0`). Dirty paths were only `.planning/config.json` and `.planning/state.json`.
- **Baseline totals later plans compute expected counts from:** testUnit with `DOCKER_HOST=unix:///nonexistent.sock` **2295** tests, 0 failures, 0 errors, 2 skips (105 XML files); full suite on `latest` x V2 **2898** tests, 0 failures, 0 errors, 18 named skips (`skips`: named=7 state_mismatch_cases=11 unexpected=0; 154 XML files). Both equal the research and Phase 47 values.
- **Eight per-class counts (testUnit, all green):** `ProxySinkTaskStopUnitTest` 4, `ClickHouseSinkTaskStopUnitTest` 6, `UnvettedRetriableCodesWarnUnitTest` 21, `RetriableExceptionPassThroughUnitTest` 38, `DescribeUnreadableEnumMockServerTest` 2, `CompareConfluentArchivesScriptUnitTest` 58, `DeclaredNullabilityTest` 69, `SourcePlanningIdGuardUnitTest` 18. Live readers of the constant plan 48-03 moves: `NullableTupleMatrixLiveTest` 33, `SimpleAggregateTupleRoundTripLiveTest` 2 (suite).
- **Server and image:** ClickHouse `26.10.3.7`, `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (the same digest Phase 47 recorded; `latest` did not drift).
- **Gates:** anchors mirror `rows=218 bad=0`; self-test `selftest: no failures` (exit 0, 16 PASS lines); tail gate over `12e2cf17..c77d1a14`: `anchor_commits=128 anchor_rows=218 marker_commits=146 anchor_exempt=0 result=PASS`, exit 0.
- **Release path frozen:** 5 allow-lists; gg6 and gg7 lists identical to their tag copies; `compare_confluent_archives.py` md5 `a637e0cc8fb33a33e7673ac682fa63c8`; gg6 note differs from `v1.7.0_gg6` in exactly `2a3,4 112c114 146c148`; `ESCAPED_NEWLINE` once in `DeclaredNullability.java`, 9 `DeclaredNullability.ESCAPED_NEWLINE` references under `src/test`.
- **Scripts (frozen, md5 in E-02):** `48-path-check.py` `641402547d365dc5b663d6c88d6e1604`, `48-id-scan.py` `0922b7a9ab304cb8a77f181798827ed6`; both committed together in `1f1fa429`.
- **Upstream and origin:** upstream `main` still `83808df9a0993cf49b2ceaa4633c782e550c0180` (no move); origin has no `sync/v1.7.0` branch; `pre-v1.7.0_gg8` is absent on origin and exists as a local tag at `baa404cd5ed95c556db0acff84def1024c3bafe3` (local only).

## Task Commits

1. **Task 1: baseline and release path (E-01)** - `a877fac1` (docs)
2. **Task 2: check scripts** - `1f1fa429` (docs); **calibration record (E-02)** - `58be2229` (docs)

## Files Created/Modified

- `.planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md` - header, index E-01..E-17, E-01 and E-02 (E-01 also holds the per-file md5 manifests of both XML snapshots)
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-path-check.py` - FORK-DELTA evidence-path resolver with the six by-design absences
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-id-scan.py` - planning-identifier scan of a published document

## Decisions Made

- Both scripts matched the calibration table on the first run, so there was no fix cycle; nothing about the expected numbers changed.
- Per the plan-check note, E-01 records md5 manifests of the XML snapshots (reproduced in the file, also under `/tmp/gsd48/exec/manifest-xml-*.md5`) and the status-dump md5 values. JUnit XML carries timestamps, so a re-run reproduces testcase names and statuses, not bytes; the re-measure rule is stated in E-01.

## Deviations from Plan

None - plan executed exactly as written. Notes on the plan-check items, none of which changed the plan:

- (a) The evidence chain reads `/tmp/gsd48/exec`; the manifests are recorded in E-01 as asked.
- (b) `/tmp/gsd48/scripts/resolve.py` was present (not rebuilt) and was read as the token-rule reference; `/tmp/gsd48/probes` holds only `Probe48EnumCauseUnitTest.java`, which this plan does not read.
- (c) The local tag `pre-v1.7.0_gg8` is recorded in E-01 (`local_pre_tag`).
- No JVM crash: no `-PjavaVersion=21` run, no core dump, no crash file in the root. The default toolchain (OpenJDK 21.0.12.1) was used for every run.

## Issues Encountered

None. The self-test is slow (about 79 min wall clock, the same shape as Phase 47's 63 min); it ran in the background alongside the Gradle baselines.

## Known Stubs

None.

## Threat Flags

None. No network endpoint, auth path, file access pattern or schema changed; the scripts only read git objects.

## Next Phase Readiness

Ready for plan 48-02 (additive test pins). It computes expected counts from testUnit 2295 and full suite 2898, and uses the status dumps `/tmp/gsd48/exec/status-base-testunit.tsv` and `status-base-suite.tsv`. STATE.md, `config.json` and `state.json` were not staged by any commit of this plan.

## Self-Check: PASSED

- Files exist: `48-COMMAND-EVIDENCE.md`, `48-path-check.py`, `48-id-scan.py` (tracked, committed).
- Commits exist on HEAD: `a877fac1`, `1f1fa429`, `58be2229`; `git rev-list --count c77d1a14..HEAD` read 3 before this SUMMARY commit.
- `P48-01-T1-OK` and `P48-01-T2-OK` printed on the committed tip; no tracked file outside the three created changed since B0.
