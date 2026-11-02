---
phase: 41-bounded-insert-timeout
plan: "03"
subsystem: testing
tags: [phase-gate, merge-invariants, fork-anchors, rollout-item, client-v2, socket-timeout, junit5, clickhouse-kafka-connect]

requires:
  - phase: 41-bounded-insert-timeout
    provides: the fix commit 14952562 (per-insert socket_timeout at three client V2 insert sites), the pin commit 0a8e1e53, the measured baseline (E-01) and the progress-header probe outcome (E-06)
provides:
  - "E-08: the gate on the fix commit's tree, a cache-busted testUnit with Docker unreachable (1745 tests, 0 failures, 0 errors, 2 skips) and the full suite on ClickHouse latest x client V2 (2343 tests, 0 failures, 0 errors, the 18 named skips), both exactly the measured baseline plus 26, with InsertStallBoundPinUnitTest 15/0 and InsertSocketTimeoutOptionUnitTest 11/0 in both"
  - "E-09: the F-2 rollout item for Phase 44 as 13 labelled claims led by the client V2 scope, with the progress-header claim carried exactly as E-06 recorded it (unmeasured, label inferred)"
  - "E-10: the phase gate, verify-merge-invariants.sh 54e63648 c03171b0 ends result=PASS with exit 0, anchors mirror rows=184 bad=0, src/main differs from phase start only in ClickHouseWriter.java and ClickHouseHelperClient.java, config/VERSION/CHANGELOG/published notes untouched, origin did not move"
affects: [44-release]

actuals:
  tokens: 4000
  tasks: 2
  commits: 3

plan_head_before: 8ba2c6f1a6276101f4f870fa3f3f514392bfd985
plan_head_after: 772a930a120d9fa6b1c6d2ac807d0a0097fc03bb
commits: 3

tech-stack:
  added: []
  patterns:
    - "Gate totals read from JUnit XML copies and compared with the measured baseline plus the phase's added cases, skips checked by name, the new classes checked by count in both runs"
    - "Cumulative invariant script run once, alone, on an explicit SHA descending from the last code commit with an equal non-planning tree"

key-files:
  created: []
  modified:
    - .planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md

key-decisions:
  - "The progress-header row of the rollout item carries label inferred and outcome unmeasured, copied from E-06, with Phase 44 told to state it as derived (mechanism, not measured) or measure it first"
  - "The exactly-once half of the duplicate risk (whether the server drops a repeat with the reused deduplication token) is labelled read from the code for the token and derived for the server behaviour, never measured"
  - "The invariant script ran on the E-09 commit, which descends from the fix commit with an equal non-planning tree, so one run covers every commit of the phase"

patterns-established:
  - "A rollout claim that a probe could not exercise is recorded with the label the probe produced, not upgraded to measured"

requirements-completed: [TMO-01, TMO-02, TMO-03]

coverage:
  - id: D1
    description: "The fix stands on the user's target: a cache-busted testUnit with Docker unreachable and the full suite on ClickHouse latest x client V2 both exit 0 with 0 failures and 0 errors, total equal to the measured baseline plus 26, 2 and the 18 named skips, both new classes green in both runs, ClickHouse version and digest unchanged across the run"
    requirement: "TMO-01, TMO-02"
    verification:
      - kind: other
        ref: "python3 -I 41-evidence-check.py junit /tmp/gsd41/exec/xml-gate-testunit tests=1745 failures=0 errors=0 skipped=2; junit /tmp/gsd41/exec/xml-gate-full tests=2343 failures=0 errors=0 skipped=18; skips OK (E-08, verify printed P41-03-T1-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The F-2 rollout item is recorded with its evidence for Phase 44: 13 claims, the client V2 scope first, the retriable failure under both tolerances with no DLQ record, the behaviour change for a quiet slow insert, the progress-header outcome as E-06 recorded it, the duplicate risk with exactlyOnce=false, the socket_timeout override with socket_timeout=0 and its remedies, and the unbounded cases"
    requirement: "TMO-03"
    verification:
      - kind: other
        ref: "python3 -I 41-evidence-check.py expect 41-COMMAND-EVIDENCE.md E-09 (14 checks, 0 failed); the required-statement and 13-labelled-row check in the plan's verify (E-09, verify printed P41-03-T2-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The anchors invariant holds across every commit of the phase and src/main changed only where the phase declared: verify-merge-invariants.sh result=PASS exit 0, mirror rows=184 bad=0, only the writer and the helper differ from phase start, ClickHouseSinkConfig.java byte-identical, CFG-02 line counts 1, no asynchronous insert line added, VERSION, CHANGELOG.md and the published notes untouched, origin did not move, nothing pushed"
    requirement: "PIN-01, TMO-01, TMO-02, TMO-03"
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 54e63648 c03171b0f6a81993f06671b0bfcc56065f9ee0d6 (invariants: provenance_paths=21 anchor_commits=1387 anchor_rows=184 marker_commits=1453 anchor_exempt=19 result=PASS); E-10 expect 14 checks, 0 failed"
        status: pass
    human_judgment: false

duration: 46min
completed: 2026-11-02
status: complete
---

# Phase 41 Plan 03: Gate and close Summary

**The Phase 41 fix passes its gate on ClickHouse latest x client V2 (every existing test plus the 26 new cases green, Docker-free and against the real server), the F-2 rollout item is recorded for Phase 44 as 13 labelled claims led by the client V2 scope, and the cumulative anchors invariant passes across every commit of the phase.**

## Performance

- **Duration:** 46 min (the invariant script took 40 min of it)
- **Started:** 2026-11-02T09:16:58Z
- **Completed:** 2026-11-02T10:03Z
- **Tasks:** 2
- **Files modified:** 1 (the evidence file; no `src`, `docs`, `VERSION` or `CHANGELOG.md` change)

## Accomplishments

- Gate (E-08) on a head whose non-planning tree equals the fix commit: the cache-busted `testUnit` with Docker unreachable ran 1745 tests (baseline 1719 plus 26), 0 failures, 0 errors, 2 skips; the full suite with `--rerun` on ClickHouse `latest` x client V2 ran 2343 tests (baseline 2317 plus 26), 0 failures, 0 errors and exactly the 18 named skips (helper: 7 named, 11 state-mismatch cases, none unexpected). `InsertStallBoundPinUnitTest` read 15/0 and `InsertSocketTimeoutOptionUnitTest` 11/0 in both runs. `SELECT version()` read 26.10.3.7 before and after and the image digest is unchanged from E-01, so no image drift; HotSpot 21 was used throughout and no crash file appeared.
- Rollout item (E-09) for Phase 44: 13 claims, each labelled and cited, the scope row first (client V2 only; the default `client_version=V1` keeps the client V1 library's 30 s read timeout). It states the retriable failure after `clickhouseClientInsertTimeoutMs` under both tolerances with no DLQ record, the behaviour change and duplicate risk for a quiet slow insert, the `exactlyOnce=true` token reuse (read from the code) with the unmeasured server half (derived), the `socket_timeout` override including `socket_timeout=0` and the remedies, the write-side stall, the three client V1 `future.get()` sites and the unbounded non-insert requests. The published gg6 and gg1 notes, `CHANGELOG.md` and `VERSION` are untouched; the correction of the published gg6 sentence is assigned to Phase 44.
- Phase gate (E-10): `verify-merge-invariants.sh 54e63648 c03171b0` ended `result=PASS` with exit 0 (started 09:22:03Z, ended 10:02:23Z), run once and alone on the E-09 commit, whose non-planning tree equals the fix commit's. The anchors mirror reads `rows=184 bad=0`; against phase start `src/main` differs only in `ClickHouseWriter.java` and `ClickHouseHelperClient.java`; `ClickHouseSinkConfig.java` is byte-identical, the CFG-02 line counts 1 and no added `src/main` line names the asynchronous insert setting or the client asynchronous mode.

## Task Commits

1. **Task 1 (tracer): the gate on the fix commit (E-08)** - `fc63e8ef` (docs)
2. **Task 2: the F-2 rollout item (E-09)** - `c03171b0` (docs); **the Phase 41 gate (E-10)** - `772a930a` (docs)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan)

## Files Created/Modified

- `.planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md` - entries E-08, E-09, E-10; the index now reads `pass` for E-01..E-10

## Decisions Made

- The progress-header claim in E-09 is worded from E-06 as recorded (`unmeasured`, `inferred`): a connector body insert was bounded in the probe (3030 ms for a 3000 ms value), a request that streams progress headers (an `INSERT ... SELECT`) was not ended by a 1000 ms `socket_timeout`, and whether a body insert with streamed headers would escape the bound was not measured. Phase 44 carries it as `derived (mechanism, not measured)` or measures it.
- The invariant script ran on the E-09 commit rather than after E-10, so the E-10 entry could record its own result; the two commits differ only under `.planning`, which the script does not judge as production tree.

## Deviations from Plan

None - plan executed exactly as written. No test was edited, no `src` file changed, nothing was pushed.

## Issues Encountered

- The plan's `fails_when` text for Task 1 mentions "15/0 and 10/0" in one place; the criteria, the verify command and the pin counts all say 11 helper cases, and the run read 11/0. Treated as a typo in the failure text, not a gate difference.
- `.planning/config.json` and `.planning/state.json` were left modified and never staged, as instructed. `STATE.md` is left modified for the orchestrator to commit (see the return message).

## Known Stubs

None. No source file was created or modified in this plan.

## Threat Flags

None. This plan changed only the evidence file; no new network endpoint, auth path, file access pattern or schema.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 41 is complete pending the orchestrator's code review. For a post-review fix: re-run the anchors mirror (`python3 -I $X anchors .planning/scripts/fork-anchors.txt --rows 184`, seconds) and the classes the fix touches; re-run the 40-minute invariant script only if a fix changes an anchored line or `fork-anchors.txt`.
- Phase 44 (gg7 rollout note, RLS-03) writes the F-2 item from E-09: lead with the client V2 scope, carry the progress-header claim as `derived (mechanism, not measured)` or measure it, and correct the published gg6 sentence that the four-minute insert timeout has no effect while `async=false` (the published text is not edited here).
- The revert unit stays the pin commit `0a8e1e53` plus the fix commit `14952562`, reverted together newest first.

## Self-Check: PASSED

- `41-03-SUMMARY.md` and `41-COMMAND-EVIDENCE.md` exist; E-08, E-09, E-10 are in commits `fc63e8ef`, `c03171b0` and `772a930a`, all ancestors of HEAD.
- `P41-03-T1-OK` and `P41-03-T2-OK` were printed by the plans' verify commands; `python3 -I $X index $E 10 --done` reads `OK index: 10 rows`; `git rev-list --count 8ba2c6f1..HEAD` read 3 before this SUMMARY; `git ls-remote origin refs/heads/custom_build` equals `72e8ccff...`; nothing was pushed.

---
*Phase: 41-bounded-insert-timeout*
*Completed: 2026-11-02*
