---
phase: 44-release-publication
plan: "04"
subsystem: release
tags: [release, allow-list, confluent-archive, gates, invariants, v1.7.0_gg7]

requires:
  - phase: 44-release-publication
    provides: plan 44-03 (the release documents frozen at c2bf5f0d, evidence E-01 to E-10) and plan 44-01 and 44-02 outputs
provides:
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt (18 rows, generated last from the final tree)
  - evidence E-11 (allow-list, verdict pass) and E-12 (Docker-free gates, verdict fail on one hygiene scan)
affects: [44-05]

actuals:
  tokens: 14000
  tasks: 2
  commits: 3
plan_head_before: d0ab16b6984c5f06d429a9f0b1b51b89498aa302
plan_head_after: d62f0af2bb3b3f7735829da13bdb77cc2f6339a5

tech-stack:
  added: []
  patterns:
    - "allow-list rows derived by owner from the compare's own UNALLOWED set (two gson globs after at least two literal segments, the gson module descriptor and fourteen connector classes as exact paths, the manifest), then proved by cover, three mutations and the shipped-lists tests"

key-files:
  created:
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
  modified:
    - .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md

key-decisions:
  - "The plan is halted, not completed: E-12 reads verdict fail because the strict hygiene scan of 44-REVIEW.md has 3 hits (a verb the helper's outside-system pattern also matches), and this plan never edits a document; Task 3 was not started because its precondition needs E-12 to read pass"
  - "The invariant script ran alone: no Gradle build overlapped it (operator instruction), so the Docker-free testUnit ran after it ended"

requirements-completed: []

coverage:
  - id: D1
    description: "The gg7 allow-list was generated last from clean exports of tag v1.7.0_gg6 and of the tip, holds 18 rows, covers the 120 differences once each, passes the local compare (exit 0, RESULT: PASS, 0 unallowed, 0 stale rows) and fails on a stale row, a deleted gson row and a leading-wildcard row (exit 1, 1, 2)"
    requirement: "RLS-02"
    verification:
      - kind: other
        ref: "bash verify block of plan 44-04 Task 1: P44-04-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "The Docker-free half of RLS-04 on gated head b265f5a4: testUnit with Docker unreachable (1826 tests, 0 failures, 2 skipped), anchors mirror rows=190, the invariant script result=PASS, scope and number facts equal to fresh runs; the strict hygiene scan of 44-REVIEW.md is red"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "E-12 docker_free_decision: no-go (gate_failures: hygiene_scan_all)"
        status: fail
    human_judgment: true
    rationale: "A single hygiene hit on reviewers' wording blocks the gate by the plan's own rule; whether to accept a wording change in a planning record is the orchestrator's decision"

duration: 55min
completed: 2026-11-02
status: halted
---

# Phase 44 Plan 04: The allow-list and the Docker-free gate Summary

**The 18-row gg7 allow-list is committed and proven in both polarities; the Docker-free gates are green except one strict hygiene scan of 44-REVIEW.md, so the plan halted before the two full suites.**

## Performance

- **Duration:** about 55 min (40 of them the invariant script, run alone)
- **Started:** 2026-11-02T22:33:48Z
- **Completed (halted):** 2026-11-02T23:28Z
- **Tasks:** Task 1 complete, Task 2 recorded with verdict fail, Task 3 not started
- **Files modified:** 2 (the allow-list and the evidence file)

## Gate

- `gated_head`: `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd` (the E-11 commit; the current tip `d62f0af2` equals it outside `.planning` and under `.planning/scripts`).
- `last_non_planning_commit`: `a43ff32f39647dfc3afb9849f6729b251f2619d2`; allow-list commit and `last_scripts_commit`: `402b6e2ea99a65e80c42f710e2b93277c6f78c64`.
- `gate_decision`: not reached (E-13 not started). `docker_free_decision`: `no-go` (E-12, verdict fail).
- testUnit with Docker unreachable: 1826 tests, 0 failures, 0 errors, 2 skipped, every pin class green, no JVM crash, default JDK 17 line.
- Full suites on `latest` with client V2, `SELECT version()` and image digest: not run. The pinned values of E-01 are `26.10.3.7` and `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`.
- Invariant script, once on `b265f5a4`, no overlap: `invariants: provenance_paths=21 anchor_commits=1534 anchor_rows=190 marker_commits=1600 anchor_exempt=19 result=PASS`, exit 0, started 22:37:10Z, finished 23:25:44Z; its files stay at `/tmp/gsd44/exec/invariants.log` and `invariants.exit` for Task 3.

## Allow-list

- 18 rows: `com/google/gson/*`, `META-INF/maven/com.google.code.gson/gson/*`, `META-INF/versions/9/module-info.class`, fourteen exact connector class paths, `META-INF/MANIFEST.MF`. `cover` reads `OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0`.
- Research cross-check: the glob set equals the 18 rows of research section 4 (`research_list_equal: yes`).
- Mutations: stale row exit 1 (`STALE-ROW zz/nothing/*`), deleted gson row exit 1 (102 `UNALLOWED` lines), `*.class` row exit 2.
- Dry-run summary: `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`.
- `final_tip_bytes`: 13564145 (`final_base_bytes` 13558634). Entry counts 7933 and 7932 equal E-05's.

## Task Commits

1. **Task 1: the allow-list** - `402b6e2e` (the list alone), evidence `b265f5a4` (E-11, pass)
2. **Task 2: Docker-free gates** - `d62f0af2` (E-12, verdict fail)
3. **Task 3: two full suites** - not started

**Plan metadata:** the commit that carries this SUMMARY.

## Files Created/Modified

- `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt` - the allow-list `release.yaml` reads for `v1.7.0_gg7`
- `.planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md` - E-11 and E-12 added, index rows flipped (E-11 pass, E-12 fail)

## Decisions Made

See the frontmatter. The plan is halted on a recorded red rather than patched: editing `44-REVIEW.md` is a document change this plan does not make.

## Deviations from Plan

**1. The invariant script was not overlapped with Gradle.** The plan starts it first and runs `testUnit` beside it; the operator's instruction said not to overlap, so the script ran alone (about 48 min) and `testUnit` followed. No result differs for it.

**2. Waiting on the background script used bounded until-loops, not an exit notification alone.** The harness cannot idle a dispatch, so each wait was one bounded until-loop of at most 175 s on the exit file; the script was started once and never restarted.

**Total deviations:** 2 process deviations, 0 auto-fixed. **Impact on plan:** none on the results.

## Issues Encountered

- **44-REVIEW.md fails the strict hygiene scan (3 hits, lines 116, 204, 244).** The hits are a bare four-letter verb that the helper's fifth default pattern (built to catch an outside-system name) also matches, in the reviewers' round text. No earlier plan scanned that file; plan 44-04 Task 2 names it. The default mode fails on it too. Every other added file and the added lines of the modified documents are clean in both modes.
- A stray `__pycache__` file from a scratch import of the hygiene helper was deleted by name; nothing generated is left untracked.

## Open for the orchestrator

- **Decision needed (the red of E-12).** Options: (a) authorise a neutral wording change of the three reviewer sentences in `44-REVIEW.md` (a `.planning` file outside `.planning/scripts`, so the Freeze rule does not move the gated head), then re-run the hygiene scan as a new evidence entry, record the E-12 outcome, and run Task 3; (b) accept the three hits as a helper false positive with the user's direct approval and record that; (c) change the helper (it is a byte copy and is never edited by this phase). The invariant result and the green testUnit above stay valid for options (a) and (b) because the gated head does not change.
- No crash record, no `digest_same_as_e01` value (suites not run), no research difference.
- `integration_sources_after_run`: `51200928` (a diagnostic log-line fix in the integration test after the Phase 39 recorded run; `integrationTest` is not re-run).
- Note row D4 states the E-05 archive sizes (13558633 and 13564144 bytes); a rebuild on a later date is one byte larger because `manifest.json` holds the build date (cause established in E-12 `archive_size_delta_cause`). The entry counts in the row match. The note is frozen and was not edited; if the sizes should be cited as exact, that is a plan 44-03 re-open.
- STATE.md and ROADMAP.md were not advanced and no requirement was marked complete, because the plan did not complete. `.planning/config.json` and `.planning/state.json` remain modified and uncommitted (orchestrator-managed).

## User Setup Required

None - no external service configuration required.

## Handoffs

- **Plan 44-05:** after Task 3 passes, the gated SHA for the user pre-checks is `last_non_planning_commit` `a43ff32f39647dfc3afb9849f6729b251f2619d2` with the scripts commit `402b6e2ea99a65e80c42f710e2b93277c6f78c64`; `final_tip_bytes` for the dry-run report is 13564145. The bundle stays void until E-13 reads pass.
- **Task 3 on resume:** the suites need the same colima `vz` Docker and the local `latest` image (never pulled); the invariant files are already in `/tmp/gsd44/exec/`.

## Next Phase Readiness

Blocked on the E-12 decision above.

## Self-Check: PASSED

The allow-list and the evidence file exist; commits `402b6e2e`, `b265f5a4` and `d62f0af2` are ancestors of HEAD; Task 1's verify printed `P44-04-T1-OK` on `b265f5a4`; nothing was pushed, no `v1.7.0_gg7` tag exists, one worktree.

---
*Phase: 44-release-publication*
*Completed (halted): 2026-11-02*
