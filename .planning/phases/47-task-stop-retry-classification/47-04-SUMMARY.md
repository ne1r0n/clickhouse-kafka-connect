---
phase: 47-task-stop-retry-classification
plan: "04"
subsystem: verification
tags: [phase-gate, testunit, full-suite, tail-gate, anchors, hand-off, gg8, evidence-only]
status: complete

requires:
  - phase: 47-task-stop-retry-classification
    provides: plan 47-01's stop() fix and ClickHouseSinkTaskStopUnitTest (E-01..E-04), plan 47-02's RetriableRedeliveryLimitPinUnitTest and FORK-DELTA item 17 (E-05..E-07), plan 47-03's FUT-05 closure and gg8 CHANGELOG section (E-08, E-09)
provides:
  - E-10, the Phase 47 gate on the phase tip a9669f7b (cache-busted testUnit with Docker unreachable, one --rerun full suite on latest x V2, per-testcase deltas, scope, anchors, tail gate, nothing pushed)
  - E-11, the Phase 49 hand-off inventory (14 rows) located live and shown unedited
  - the evidence index E-01..E-11, every row pass
affects: [49]

actuals:
  tokens: 3900
  tasks: 2
  commits: 2
plan_head_before: a9669f7b0d8e54af8a8840e5d1650cd7ae91c58c
plan_head_after: 61989a0951b459899ea67554b6b66b11a4d81945

tech-stack:
  added: []
  patterns: [gate re-checked against the Gradle XML snapshot by the read-only helpers, per-testcase delta against the baseline dump with the two committed expected lists, tail gate run once on an explicit SHA in the background while the builds ran]

key-files:
  created: []
  modified:
    - .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md

key-decisions:
  - "No new decision: the plan is measurement and inventory. Nothing tracked changed except the evidence file; the hand-off items are recorded, never edited (D-2, U-3)"

requirements-completed: [LIF-01, LIF-02, RTY-01, RTY-02]

coverage:
  - id: D1
    description: "Phase gate on the phase tip a9669f7b: cache-busted testUnit with DOCKER_HOST=unix:///nonexistent.sock is green at 2295 tests (E-01's 2181 plus 114) with 2 skips, and its per-testcase delta against E-01 removes nothing and adds exactly the 6 LIF-01 and 108 RTY-01 rows"
    requirement: LIF-01
    verification:
      - kind: other
        ref: "P47-04-T1-OK (Task 1 automated verify; E-10, gate XML snapshot re-checked live)"
        status: pass
    human_judgment: false
  - id: D2
    description: "One --rerun full suite on ClickHouse latest (26.10.3.7, digest sha256:739e099a...) x client V2 is green at 2898 tests (E-01's 2784 plus 114) with the 18 named skips; the by-class delta removes nothing and adds only ClickHouseSinkTaskStopUnitTest (6) and RetriableRedeliveryLimitPinUnitTest (108); the eight E-01 pin classes keep their counts"
    requirement: RTY-01
    verification:
      - kind: other
        ref: "P47-04-T1-OK (Task 1 automated verify; E-10, gate XML snapshot re-checked live)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Scope on the phase tip: src/main differs from the base only in ClickHouseSinkTask.java (a stop() body that is the planned try/finally); Processing, ProxySinkTask, Utils, KeeperStateProvider, ClickHouseSinkConfig, ChunkFlusher and the strategies are byte-identical to the base, so the BEFORE/AFTER flag sequence is unchanged; tests only added (the two pins); build files, workflows and VERSION unchanged; CHANGELOG is the gg8 section followed by the base content; docs changed only in FORK-DELTA; gate scripts and plan-time files unedited; no merge landed"
    requirement: LIF-02
    verification:
      - kind: other
        ref: "P47-04-T1-OK (git diff and git show checks against B0 427f30b4; E-10)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Anchors invariant, once, on an explicit SHA: mirror rows=218 bad=0; the unedited standing tail gate over 12e2cf17..a9669f7b prints provenance_paths=11 anchor_commits=108 anchor_rows=218 marker_commits=126 anchor_exempt=0 result=PASS; nothing pushed (pre-v1.7.0_gg8 absent on origin, the origin sync branch absent as in E-01, upstream main unmoved at 83808df9)"
    requirement: RTY-02
    verification:
      - kind: other
        ref: "P47-04-T1-OK (live tail gate re-run by the verify on the recorded gate_head; E-10)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Phase 49 hand-off recorded, not edited: the gg7 rollout note identical to its tag with rows F5 (line 52), R8 (line 126), the FUT-05 line (130) and the residuals bullet (208) located; gg6 row C2 (114) located and unchanged; the gg7 CHANGELOG Known Issues bullets (85 and 87) located; the gg8 section Unreleased; FORK-DELTA raw numbers unchanged and item 8's Future Requirements reference found stale; planning records and backlog candidates listed; evidence index E-01..E-11 complete, every row pass"
    requirement: LIF-02
    verification:
      - kind: other
        ref: "P47-04-T2-OK (Task 2 automated verify; E-11)"
        status: pass
    human_judgment: false
---

# Phase 47 Plan 04: Phase gate and Phase 49 hand-off inventory Summary

**The Phase 47 tip a9669f7b passes the roadmap's gate: cache-busted testUnit 2295 tests and the full suite on ClickHouse 26.10.3.7 x client V2 2898 tests, both exactly E-01 plus the 114 planned rows with no removed row and no failure, the only production change the `stop()` try/finally, the anchors tail gate PASS with anchor_rows=218 and nothing pushed; the Phase 49 hand-off is a 14-row table, located live and left unedited.**

## Performance

- **Duration:** about 20 minutes (start 2026-11-05T21:15:29Z, last task commit about 21:30Z; the Task 1 verify, whose tail gate takes about 8 minutes, was run twice, once before and once after the E-11 commit).
- **Tasks:** 2 (tracer plus auto), 1 file changed (0 created, 1 modified: the evidence file), 2 commits before this SUMMARY.

## Accomplishments

- Task 1 (tracer, E-10): re-reads first: `upstream main` is still `83808df9a0993cf49b2ceaa4633c782e550c0180`, `pre-v1.7.0_gg8` is absent on origin and the origin sync branch is absent, as in E-01. The head was read once as `gate_head` = `a9669f7b0d8e54af8a8840e5d1650cd7ae91c58c` and the standing tail gate was started in the background on that explicit SHA (it reads git objects only). Cache-busted testUnit with `DOCKER_HOST=unix:///nonexistent.sock`: exit 0, 2295 tests, 0 failures, 0 errors, 2 skips; the exact per-testcase delta against E-01 prints `removed=0 added=114 expected=114 match=yes`. One `--rerun` full suite on `latest` x V2: exit 0, 2898 tests, 0 failures, 0 errors, 18 skips (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`), the by-class delta `removed=0 added=114 expected=114 match=yes` (exit 0), `ClickHouseSinkTaskStopUnitTest` 6 and `RetriableRedeliveryLimitPinUnitTest` 108 green, and the eight E-01 pin classes at their counts (14, 38, 216, 4, 21, 19, 2, 15). Server `26.10.3.7` at digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, identical to E-01. Scope: `main_diff_since_base` is `ClickHouseSinkTask.java` only, no `src/main` file added, the `stop()` token list equals the planned `try`/`finally` with every other line of the file unchanged, the ten `Processing`/`ProxySinkTask`/`Utils`/`KeeperStateProvider`/`ClickHouseSinkConfig`/`ChunkFlusher`/strategy files byte-identical to the base, the test tree changed only by the two added pins, build files, workflows and `VERSION` unchanged, CHANGELOG equal to the gg8 section plus the base content, docs changed only in FORK-DELTA with the five final checks satisfied, gate scripts and plan-time files unedited, no merge since the base. Anchors mirror `rows=218 bad=0`; tail gate exit 0, `provenance_paths=11 anchor_commits=108 anchor_rows=218 marker_commits=126 anchor_exempt=0 result=PASS`.
- Task 2 (E-11): located with live `grep -n` and shown unedited: gg7 rollout F5 line 52, R8 line 126, the FUT-05 line 130 (`FUT-05 stays open.`), the residuals bullet line 208 (the gg7 note is identical to tag `v1.7.0_gg7`); gg6 C2 line 114 (unchanged since the base); the gg7 CHANGELOG Known Issues bullets on the retry budget (line 85) and the open decision (line 87); the gg8 section Unreleased; FORK-DELTA's `## The raw numbers` section identical to the base's; item 8's reference to the Future Requirements list found stale; the three planning records with the old `stop()` claim; the two backlog candidates. The index E-01..E-11 reads pass throughout (`index 11 --done` OK).

## Gate totals

| Run | Tests | Failures | Errors | Skipped | Delta against E-01 |
|---|---|---|---|---|---|
| testUnit, Docker unreachable (`gate-testunit`) | 2295 (2181 + 114) | 0 | 0 | 2 | removed 0, added 114 (exact) |
| Full suite, ClickHouse `latest` 26.10.3.7 x V2 (`gate-suite`) | 2898 (2784 + 114) | 0 | 0 | 18 (7 named, 11 state-mismatch cases) | removed 0, added 114 (by class: only the two new classes) |

No JVM crash occurred and `-PjavaVersion=21` was not used (default toolchain in both runs; no core dump or javacore in the root). Upstream did not move and origin was not crossed: nothing was pushed, `pre-v1.7.0_gg8` is absent and the origin sync branch is absent.

## Commits

| Commit | Message | Files |
|---|---|---|
| `638beecbe74617ad65d3da5616bb49baf5649ef5` | record the Phase 47 gate (E-10) | `47-COMMAND-EVIDENCE.md` |
| `61989a0951b459899ea67554b6b66b11a4d81945` | record the Phase 49 hand-off inventory (E-11) | `47-COMMAND-EVIDENCE.md` |

## Deviations from Plan

None - plan executed exactly as written.

Process notes, not deviations. (1) The plan's verify blocks were extracted verbatim from the plan into `/tmp/gsd47/exec/verify-04-t1.sh` and `verify-04-t2.sh` (a `set -e` prefix, otherwise unchanged). Their final "tracked files are clean" check needs the evidence committed, so each ran on a committed tip: Task 1's verify after the E-10 commit (before E-11 existed) and again on the final tip after the E-11 commit, Task 2's after the E-11 commit; all three printed their OK line (`P47-04-T1-OK` twice, `P47-04-T2-OK`). (2) The `index` check of the helper with `10 --done` fails while E-11 is still pending; the plan only asks for `index 11 --done`, which printed OK. (3) `.planning/STATE.md`, `.planning/ROADMAP.md` and `.planning/REQUIREMENTS.md` were not updated or staged, per the orchestrator's instructions; `.planning/config.json` and `.planning/state.json` stay dirty and unstaged.

## Authentication Gates

None.

## Issues Encountered

None.

## Hand-off to Phase 49

Recorded in E-11, not edited in this phase (D-2, U-3). Line numbers are those on the phase tip.

| Item | Current location | What Phase 49 does |
|---|---|---|
| gg7 rollout row F5 | `docs/ROLLOUT-v1.7.0_gg7.md` line 52 | the gg8 rollout note corrects it: the task-level `stop()` residual is closed (FORK-DELTA item 13, E-03); the other residuals stay open; the gg7 note stays a copy of its tag |
| gg7 rollout row R8 | same file, line 126 | the gg8 note corrects it: the missing budget is a recorded limit by decision (FORK-DELTA item 17, E-05, E-06); no setting removes a code by decision (item 24, E-08) |
| gg7 FUT-05 status line | same file, line 130 | the gg8 note says FUT-05 is closed by decision (item 24, E-08) |
| gg7 residuals bullet | same file, line 208 | the gg8 note corrects the task-level `stop()` part |
| gg6 rollout row C2 (Phase 48 edits the gg6 note, Phase 49 reviews it) | `docs/ROLLOUT-v1.7.0_gg6.md` line 114, "That follow-up is FUT-05, open in FORK-DELTA" | the clause is stale now that FORK-DELTA closes FUT-05; correct it with the Phase 48 edits or record why not |
| gg8 CHANGELOG section | `CHANGELOG.md` lines 1-22 (`# 1.7.0_gg8, Unreleased`, only the `stop()` bullets) | complete and date it: the upstream tail, the timeout limits, the RTY-01 limit bullet and the FUT-05 closure bullet |
| the two superseded gg7 Known Issues bullets | `CHANGELOG.md` line 85 (retry budget) and line 87 (open decision on writer `DataException` shapes), inside the unedited gg7 section | the gg8 section says both are superseded by the recorded decisions (items 17 and 24) |
| `VERSION` | unchanged since the phase base | bump when the section is dated |
| FORK-DELTA raw numbers | `docs/FORK-DELTA.md` line 149 (`## The raw numbers`), identical to the base | refresh with the release's gate totals (this phase's gate: testUnit 2295, suite 2898, 18 skips, anchors 218) |
| FORK-DELTA item 8 reference | `docs/FORK-DELTA.md` line 232 names the Future Requirements list of `.planning/REQUIREMENTS.md`, which holds no `FUT-0` identifier | point it at a list that carries FUT-01..FUT-04, or restore the identifiers there (Phases 48-49) |

## Orchestrator

Not touched by this plan (STATE.md, ROADMAP.md and REQUIREMENTS.md were reserved):

- Planning records that carry the old `stop()` claim: `.planning/MILESTONES.md:24`, `.planning/PROJECT.md:48` and `.planning/PROJECT.md:661`.
- `.planning/REQUIREMENTS.md`: LIF-01 and LIF-02 (lines 26, 27) and RTY-01 and RTY-02 (lines 37, 38) are unchecked and their traceability rows (lines 87, 88, 92, 93) read `Pending`; set them complete.
- The independent `gsd-code-reviewer` pass on FORK-DELTA and CHANGELOG, after this plan (STATE.md standing rule).

## Backlog candidates

Both recorded and not changed in this phase; each is a production change outside its scope and the user's decision (D-1, D-3):

- A `finally` in `ClickHouseSinkTask.close(partitions)` (lines 161-169): the proxy's `onPartitionRemoved` runs before `deliveryStrategy.close(partitions)` with no `finally`, so a throw from the first skips the second (FORK-DELTA item 13, read from the code).
- The `keepFirst` idiom of `ProxySinkTask.stop()` in `ClickHouseSinkTask.stop()`: when both stops throw, the proxy side's exception is thrown today and the strategy's is dropped with 0 suppressed (E-04); the idiom would attach it as suppressed. The LIF-01 pin asserts only that both stops ran and an exception surfaced, so it needs no edit.

## Known Stubs

None.

## Threat Flags

None. No `src` change and no new endpoint or trust-boundary surface. T-47-19 (counts from JUnit XML re-checked live against the gate snapshots, exact and by-class deltas against E-01, the tail gate re-run live on the recorded head SHA, the index reading pass for all 11 entries), T-47-20 (`Processing`, `KeeperStateProvider`, `ProxySinkTask`, `Utils`, `ClickHouseSinkConfig`, `ChunkFlusher` and the strategies byte-identical to the base; `ProcessingTest` green in both runs), T-47-21 (no push; `pre-v1.7.0_gg8` absent and the origin sync branch absent as in E-01) and T-47-22 (the gg7 note equal to its tag, the gg6 note equal to the base, the gg7 CHANGELOG section byte-identical inside the gg8-prefixed file) are mitigated as planned.

## Ready for

Phase 47 is complete on the plan level: all four plans have a SUMMARY. Next are the orchestrator's items above (STATE, ROADMAP, REQUIREMENTS, the independent review of FORK-DELTA and CHANGELOG), then Phase 48.

## Self-Check: PASSED

The evidence file exists with E-01..E-11; commits `638beecb` and `61989a09` are ancestors of HEAD; `P47-04-T1-OK` (twice) and `P47-04-T2-OK` printed on the committed tips; `git rev-list --count a9669f7b..HEAD` was 2 before this SUMMARY; `git diff --name-only 427f30b4 HEAD -- src/main` prints only `ClickHouseSinkTask.java`; no core dump, javacore or other untracked file is left in the repository root.
