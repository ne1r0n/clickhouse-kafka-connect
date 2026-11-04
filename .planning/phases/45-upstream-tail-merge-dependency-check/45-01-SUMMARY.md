---
phase: 45-upstream-tail-merge-dependency-check
plan: "01"
subsystem: upstream-sync
tags: [git-merge, upstream-tail, merge-gate, evidence, maven-central-integrity]

requires:
  - phase: 44-release-publication
    provides: the sync/v1.7.0 tip, the tail gate scripts, fork-anchors.txt (190 rows)
provides:
  - local lightweight tag pre-v1.7.0_gg8 on the pre-merge tip (never pushed)
  - merge commit M 12e2cf17 taking upstream 83808df9 (PRs #839, #857 and three bumps) with every hunk on the fork side
  - record commit 43676e9a (conflicted-paths file and MERGE-CHECKLIST-v1.7.0-tail-3.md, Recorded Facts 46-47)
  - 45-COMMAND-EVIDENCE.md with E-01..E-04 (baseline suites, merge, tail gate, bare-merge break inventory)
affects: [45-02, 45-03, 45-04, 45-05, 45-06, phase-46, phase-47, phase-48, phase-49]

actuals:
  tokens: 12400
  tasks: 2
  commits: 23
plan_head_before: 2c19e93fca9e155f08ddd646eb06f8e990d94ecf
plan_head_after: 1c613a92fe69b73cf51f617693503998535621f0

tech-stack:
  added: [org.apache.kafka:connect-json 4.4.0 (test scope, upstream's catalog line)]
  patterns: [tagless mini-sync shape: rollback tag, one mechanical merge, record commit, unedited tail gate]

key-files:
  created:
    - .planning/MERGE-CHECKLIST-v1.7.0-tail-3.md
    - .planning/scripts/conflicted-paths-v1.7.0-tail-3.txt
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md
  modified:
    - build.gradle.kts
    - gradle/libs.versions.toml
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java

key-decisions:
  - "Every conflicted hunk and the auto-merged describeTableV1 dead-resource hunk take the fork side; #857 and the connect-json line are taken as merged and handled in plans 45-02 and 45-03"
  - "Nothing is pushed: pre-v1.7.0_gg8 is local only and origin has no such tag"

requirements-completed: [UPS-01]

coverage:
  - id: D1
    description: "Upstream tail 83808df9 merged into sync/v1.7.0 as one mechanical two-parent commit with local rollback tag pre-v1.7.0_gg8 as first parent; first-parent diff is exactly six paths and the five fork-side paths are byte-identical to the first parent"
    requirement: UPS-01
    verification:
      - kind: other
        ref: "P45-01-T1-MERGE-OK (Task 1 first automated verify block)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Conflicted-paths record and per-path resolution checklist committed after M; the unedited tail gate over M..record commit ends provenance_paths=11 anchor_rows=190 anchor_exempt=0 result=PASS"
    requirement: UPS-01
    verification:
      - kind: other
        ref: "P45-01-T1-GATE-OK and P45-01-T1-COMMITTED (Task 1 verify blocks 2 and 3)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Bare-merge break inventory: compile tasks pass except compileTestJava with exactly 2 javac errors in ClickHouseWriterRecordContextTest; connect-json 4.4.0 SHA-1 equals Maven Central; static facts for plans 45-02 and 45-03"
    requirement: UPS-01
    verification:
      - kind: other
        ref: "P45-01-T2-OK (Task 2 verify block)"
        status: pass
    human_judgment: false

duration: 64min
completed: 2026-11-04
status: complete
---

# Phase 45 Plan 01: Upstream Tail Merge Summary

**Upstream tail 83808df9 (PRs #839 and #857 plus three bumps) merged into sync/v1.7.0 as one mechanical commit with a local rollback tag, per-path checklist and a passing unedited tail gate, after a green baseline on the untouched tip and a measured bare-merge break inventory.**

## Performance

- **Duration:** 64 min (about 55 min of it the gate self-test, whose ST4 runs the cumulative gate)
- **Started:** 2026-11-04T19:08:00Z
- **Completed:** 2026-11-04T20:12:00Z
- **Tasks:** 2
- **Files modified:** 6 in M (build.gradle.kts, gradle/libs.versions.toml, ClickHouseWriter.java and three new upstream tests) plus 3 new `.planning` files

## Accomplishments

- E-01: upstream main still `83808df9`, newest upstream tag `v1.7.0`, no `v1.7.1` tag, conflict set exactly the six roadmap paths, origin `custom_build` unmoved (`94834763`), self-test `selftest: no failures`, anchors `rows=190 bad=0`, helper md5 unchanged.
- Baseline totals on the untouched tip (ClickHouse `26.10.3.7`, digest `sha256:739e099a...30b1ce`, client V2), which later plans compute expected counts from: **testUnit 1826 tests, 0 failures, 0 errors, 2 skipped (90 files)**; **full suite 2425 tests, 0 failures, 0 errors, 18 skipped, 138 files, 176 s**; 599 Docker-only tests (suite minus testUnit). No JVM crash. The research rehearsal's totals (2245 tests, from a crashed run) were not used.
- E-02/E-03: tag `pre-v1.7.0_gg8` = `baa404cd`; M = `12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10` (parents `baa404cd` and `83808df9`); M changes exactly six paths (+298/-5, no `.planning` path); five fork-side paths byte-identical to M's first parent; catalog differs by one added line. Record commit `43676e9a`. Tail gate: `provenance_paths=11 anchor_commits=2 anchor_rows=190 marker_commits=20 anchor_exempt=0 result=PASS` in 10 s.
- E-04: on the bare merge compileJava, compileTestFixturesJava, compileIntegrationTestJava pass; compileTestJava fails with exactly 2 javac errors (`new SinkTaskStatistics(0)`, lines 41 and 63 of `ClickHouseWriterRecordContextTest`). connect-json 4.4.0 jar SHA-1 `d9a11942...` equals Maven Central's. #857's seam is live (`writeColumnWithRecordContext(` 2 lines), `writeRecordsRowBinary` is package-private, the 216-case matrix and its scripted writer are unchanged since the tag.

## Task Commits

1. **Task 1: tail-merge tracer** - `baa404cd` (E-01 evidence, before the tag), `12e2cf17` (M), `43676e9a` (record), `201bec4a` (E-02, E-03)
2. **Task 2: break inventory** - `1c613a92` (E-04)

**Plan metadata:** the docs(45-01) completion commit follows this file (SUMMARY only; STATE.md is committed by the orchestrator).

`actuals.commits: 23` is the measured `git rev-list --count plan_head_before..plan_head_after`, which counts upstream's 18 merged commits and M along with this plan's 5 first-parent commits.

## Files Created/Modified

- `.planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md` - Phase 45 evidence: header, index E-01..E-18, entries E-01..E-04
- `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md` - resolution record: Sections 1, 2, 2a, 3, gate commands, Recorded Facts 46-47
- `.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt` - the six-line merge-tree conflict list read by the tail gate
- `build.gradle.kts`, `gradle/libs.versions.toml`, `ClickHouseWriter.java`, three upstream tests - arrive with M, unedited

## Decisions Made

None beyond the plan: every hunk fork side, nothing pushed, nothing repaired (repairs belong to plans 45-02 and 45-03).

## Deviations from Plan

### Auto-fixed Issues

None. One observation, not a rule trigger:

- `CHANGELOG.md` had **two** conflict hunks (the top of the file and the `Known Issues` block) where the plan said one. Both were resolved to the fork side, which is the plan's stated intent; the file is byte-identical to M's first parent. Recorded in E-02 and the checklist (Section 1, hunks `2`).
- The six conflicted files were resolved with a small scratch script (`/tmp/gsd45/exec/keep_head.py`, not committed) that keeps only the HEAD lines of each marker hunk, instead of one Edit-tool call per hunk. The result is the same per-hunk fork-side choice; the auto-merged catalog line and the three no-op bumps survived, and no `-X ours` or whole-file `--ours` was used. The dead-resource hunk was dropped with an explicit Edit.

**Total deviations:** 0 auto-fixed. **Impact:** none.

## Issues Encountered

None. Gate self-test took about 55 minutes (expected: ST4 runs the cumulative gate). No Docker, JVM or network problem occurred; connect-json resolved on the first build after M.

Operator note for Phase 49: `origin_custom_build` did not move (`94834763` prefix), so there are no new origin commits to absorb. `pre-v1.7.0_gg8` does not exist on origin.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 45-02: M is committed, the bare-merge breaks are measured and match the prediction (2 javac errors, one testUnit red to be confirmed by 45-02, one live red WRT-10 for 45-03).
- Later plans must use the baseline above (testUnit 1826/2 skipped, full suite 2425/18 skipped) before adding their own tests.

## Self-Check: PASSED

- Files: MERGE-CHECKLIST-v1.7.0-tail-3.md, conflicted-paths-v1.7.0-tail-3.txt, 45-COMMAND-EVIDENCE.md present.
- Commits `baa404cd`, `12e2cf17`, `43676e9a`, `201bec4a`, `1c613a92` are ancestors of HEAD; `refs/tags/pre-v1.7.0_gg8` equals M's first parent.
- Verify blocks P45-01-T1-MERGE-OK, P45-01-T1-GATE-OK, P45-01-T1-COMMITTED and P45-01-T2-OK printed on the committed tip.
