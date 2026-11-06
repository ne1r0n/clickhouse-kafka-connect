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
  - evidence E-11 (allow-list), E-12 (Docker-free gates, verdict fail on one hygiene scan), E-20 (the hygiene re-run, supersedes E-12) and E-13 (two full suites, go)
affects: [44-05]

actuals:
  tokens: 21000
  tasks: 3
  commits: 7
plan_head_before: d0ab16b6984c5f06d429a9f0b1b51b89498aa302
plan_head_after: a6832f25dd110d082876dbe4ba0f17a8e6659db6

tech-stack:
  added: []
  patterns:
    - "allow-list rows derived by owner from the compare's own UNALLOWED set (two gson globs after at least two literal segments, the gson module descriptor and fourteen connector classes as exact paths, the manifest), then proved by cover, three mutations and the shipped-lists tests"
    - "a red gate entry is never edited: a superseding entry (E-20) re-runs the failed check after the orchestrator's fix and carries the decision"

key-files:
  created:
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
  modified:
    - .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md

key-decisions:
  - "E-12 stays verdict fail (strict hygiene hits in the reviewers' text); E-20 re-runs the scan after the orchestrator's neutral rewording 4c1f600b and carries docker_free_decision: go"
  - "The invariant script ran alone: no Gradle build overlapped it (operator instruction); testUnit and the suites followed"

requirements-completed: [RLS-02]

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
    description: "The local gates of RLS-04 on gated head b265f5a4: testUnit with Docker unreachable (1826 tests, 0 failures, 2 skipped), two full suites on one pinned latest 26.10.3.7 with client V2 (2425 tests twice, 18 named skips), the anchors mirror rows=190 and the invariant script result=PASS"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "E-12 (testUnit, scope, numbers), E-20 (hygiene re-run, go), E-13 (suites, invariants, gate_decision go); verify blocks P44-04-T2-TESTS-OK and P44-04-T3-OK (with E-20 read for the Docker-free decision)"
        status: pass
    human_judgment: true
    rationale: "The runner-side checks (scratch dry run, the single tests.yaml run, the publish run) and the user's approval are outside this gate; E-12 keeps verdict fail by design and the decision rests on the superseding E-20"

duration: 66min
completed: 2026-11-02
status: complete
---

# Phase 44 Plan 04: The allow-list and the local gates Summary

**The 18-row gg7 allow-list is committed and proven in both polarities, and the release tip is gated: testUnit with Docker unreachable, two full suites on one pinned ClickHouse 26.10.3.7 with client V2 (2425 tests each), and the cumulative invariant script all pass; the decision is go.**

## Performance

- **Duration:** about 66 min (40 of them the invariant script, run alone)
- **Started:** 2026-11-02T22:33:48Z
- **Completed:** 2026-11-02T23:38Z
- **Tasks:** 3
- **Files modified:** 2 (the allow-list and the evidence file) plus this SUMMARY

## Gate

- `gated_head`: `b265f5a45a0752f314bb95315e3cdfbeaa2b78cd`. `last_non_planning_commit`: `a43ff32f39647dfc3afb9849f6729b251f2619d2`. Allow-list commit and `last_scripts_commit`: `402b6e2ea99a65e80c42f710e2b93277c6f78c64`. The tip `a6832f25` equals the gated head outside `.planning` and under `.planning/scripts` (re-confirmed in E-20 and E-13).
- `gate_decision`: `go` (E-13). `docker_free_decision`: `no-go` in E-12, `go` in E-20 (supersedes E-12).
- testUnit with Docker unreachable: 1826 tests, 0 failures, 0 errors, 2 skipped, every pin class green, no JVM crash.
- Full suites on `latest` with client V2: 2425 tests, 0 failures, 0 errors, 18 skipped (`named=7 state_mismatch_cases=11 unexpected=0`), twice (2m 53s and 2m 45s). `SELECT version()` `26.10.3.7`, image digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, equal at all three reads and equal to E-01's. JVM line: the default JDK 17 for both, no crash file. Docker-only delta 599.
- Invariant script, once on `b265f5a4`: `invariants: provenance_paths=21 anchor_commits=1534 anchor_rows=190 marker_commits=1600 anchor_exempt=19 result=PASS`, exit 0.
- Anchors mirror `rows=190 bad=0`.

## Allow-list

- 18 rows: `com/google/gson/*`, `META-INF/maven/com.google.code.gson/gson/*`, `META-INF/versions/9/module-info.class`, fourteen exact connector class paths, `META-INF/MANIFEST.MF`. `cover` reads `OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0`.
- Research cross-check: the glob set equals the 18 rows of research section 4.
- Mutations: stale row exit 1 (`STALE-ROW zz/nothing/*`), deleted gson row exit 1 (102 `UNALLOWED` lines), `*.class` row exit 2.
- Dry-run summary: `exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`.
- `final_tip_bytes`: 13564145 (`final_base_bytes` 13558634). Entry counts 7933 and 7932 equal E-05's.

## Task Commits

1. **Task 1: the allow-list** - `402b6e2e` (the list alone), evidence `b265f5a4` (E-11, pass)
2. **Task 2: Docker-free gates** - `d62f0af2` (E-12, verdict fail on one hygiene scan), halt summary `4784e602`; the orchestrator's rewording `4c1f600b`; re-run `f5282ba9` (E-20, pass)
3. **Task 3: two full suites** - `a6832f25` (E-13, pass)

**Plan metadata:** the commit that carries this SUMMARY.

## Files Created/Modified

- `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt` - the allow-list `release.yaml` reads for `v1.7.0_gg7`
- `.planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md` - E-11, E-12, E-20 and E-13 added; index rows E-11 pass, E-12 fail, E-13 pass, E-20 pass

## Decisions Made

See the frontmatter.

## Deviations from Plan

**1. The strict hygiene scan of `44-REVIEW.md` was red (E-12, verdict fail), then superseded by E-20.** The helper's fifth default pattern (built to catch an outside-system name) also matches a plain four-letter verb in three reviewer sentences (lines 116, 204, 244 of `44-REVIEW.md`); no earlier plan scanned that file. The plan forbids this plan to edit a document, so it halted. The orchestrator rewrote the three words neutrally in `4c1f600b` (a `.planning` file outside `.planning/scripts`; no finding, count, id or attestation line changed; the reviewer `agents` gate still reads four distinct ids). E-20 re-ran the scans (0 hits in both modes on 12 and 10 files), re-confirmed the freeze rule against the tip and carries `docker_free_decision: go`; E-12 was not edited.

**2. Evidence ids and the plan's verify blocks.** The re-run took the next free id E-20 (the helper's entry head allows only `E-NN`); the index row E-20 was added after E-19. Plan Task 2's verify, run as written, fails by design on three E-12 keys (`hygiene_scan_all`, `docker_free_decision`, verdict); Task 3's verify passes (`P44-04-T3-OK`) with `E-11 E-13 E-20` read in the `rows` clause in place of `E-11 E-12 E-13`; Task 2's testUnit verify prints `P44-04-T2-TESTS-OK`; Task 1's prints `P44-04-T1-OK`.

**3. The invariant script was not overlapped with Gradle.** The plan starts it first and runs `testUnit` beside it; the operator's instruction said not to overlap, so it ran alone (about 48 min) and the Gradle runs followed.

**4. Waiting on background runs used bounded until-loops** (at most 175 s each) on the exit files; the invariant script and each suite were started once and never restarted.

**Total deviations:** 4 process deviations, 0 auto-fixed. **Impact on plan:** none on the results; the E-12 record keeps its fail verdict.

## Issues Encountered

- The hygiene false positive of deviation 1.
- Row D4 of the frozen note `docs/ROLLOUT-v1.7.0_gg7.md` states the E-05 archive sizes (13558633 and 13564144 bytes); rebuilds made on a later date are one byte larger because the archive's `manifest.json` holds the build date (cause established in E-12 `archive_size_delta_cause`). The entry counts in the row match. The note was not edited; if the sizes are to be cited as exact, that is a plan 44-03 re-open.
- A stray `__pycache__` file from a scratch import of the hygiene helper was deleted by name; nothing generated is left untracked.

## Open for the orchestrator

- No crash record, `digest_same_as_e01: yes`, no research difference.
- `integration_sources_after_run`: `51200928` (a diagnostic log-line fix in the integration test after the Phase 39 recorded run; `integrationTest` is not re-run).
- The D4 size note above (a handoff for the bundle, not a gate failure).
- `.planning/config.json` and `.planning/state.json` remain modified and uncommitted (orchestrator-managed); STATE.md and ROADMAP.md were updated by the state verbs, not staged by hand.
- RLS-02 is complete with this plan (only plan 44-04 declares it). RLS-04 is also declared by plan 44-05 and completes with it.

## User Setup Required

None - no external service configuration required.

## Handoffs

- **Plan 44-05:** the gated SHA for the user pre-checks is `last_non_planning_commit` `a43ff32f39647dfc3afb9849f6729b251f2619d2` with the scripts commit `402b6e2ea99a65e80c42f710e2b93277c6f78c64`; the gated head is `b265f5a4` (the pushed SHA may differ from it only under `.planning` outside `.planning/scripts`). `final_tip_bytes` for the dry-run report is 13564145. The bundle should cite E-20 for the Docker-free decision and E-13 for the live gates, and may state the D4 size note.

## Next Phase Readiness

Ready for plan 44-05.

## Self-Check: PASSED

The allow-list and the evidence file exist; commits `402b6e2e`, `b265f5a4`, `d62f0af2`, `4c1f600b`, `f5282ba9` and `a6832f25` are ancestors of HEAD; the verifies print `P44-04-T1-OK`, `P44-04-T2-TESTS-OK` and `P44-04-T3-OK` (Task 2's first verify fails only on the three superseded E-12 keys); nothing was pushed, no `v1.7.0_gg7` tag exists, one worktree.

---
*Phase: 44-release-publication*
*Completed: 2026-11-02*
