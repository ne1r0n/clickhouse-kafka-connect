---
phase: 17-ci-build-test-hygiene
plan: "03"
subsystem: infra
tags: [github-actions, ci, gradle, jacoco, clickhouse-client-v2, branch-push-proof]

requires:
  - phase: 17-ci-build-test-hygiene
    provides: plan 17-01 hardened tests.yaml (SHA pins, ubuntu-24.04, read-only token, reportTestForks) and plan 17-02 build and test debt closure
provides:
  - E-09, the D-10 local gate on the final Phase 17 code (testUnit 464 tests, full suite 840 tests, 18 named skips, anchors PASS)
  - E-10, one green tests.yaml run on pushed SHA 0c90d3314c1cf96cb68aed0fe3788c869341e7cd, read through the API (CI-17 API-observable part)
  - the scratch branch origin/sync/v1.7.0 created, observed and deleted, with origin heads, tags and custom_build unchanged
affects: [17-04, phase-20-release]

actuals:
  tokens: 4400
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Branch-only CI proof push: explicit single refspec, run located by full 40-hex SHA plus workflow path, branch deleted after green"
    - "Blocking-human D-11 gate answered directly to the orchestrator; the push step runs inline in the orchestrator"

key-files:
  created: []
  modified:
    - .planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md

key-decisions:
  - "The push was taken on the user's own direct 'push' to the orchestrator (2026-10-11T20:34:36Z); Task 3 ran inline in the orchestrator, not in the dispatched executor"
  - "Finding F1 (the fork's default branch is main) was surfaced, not acted on: the user did not state a default-branch intent, so Phase 20's operator checks carry it as undecided and no repository setting was changed"
  - "CI-17 stays in progress after this plan: plan 17-04 also declares CI-17 and owns the web-UI items (runner image, token permissions block, job summary)"

patterns-established:
  - "Pattern: green-then-delete scratch branch with before/after ref snapshots (heads, tags, custom_build) byte-compared"

requirements-completed: []

coverage:
  - id: D1
    description: "D-10 local gate: testUnit and the cache-busted full suite on ClickHouse latest x client V2 are green on the final Phase 17 code, the anchors invariant prints result=PASS, touched Java files are spotless-clean and the phase scope is exactly ten non-.planning paths"
    requirement: "CI-17"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-09 (LOCAL-GATE-GREEN, ANCHORS-SPOTLESS-SCOPE-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "CI-17 API-observable part: exactly one completed, successful tests.yaml push run on the pushed SHA, job label ubuntu-24.04, every step success, one 'Test fork count' notice, no Node.js 20 / Lombok ambiguity / ubuntu-latest annotations, and both artifacts uploaded"
    requirement: "CI-17"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-10 (CI-17-RUN-GREEN, CI-17-RUN-EVIDENCE-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Push safety: only refs/heads/sync/v1.7.0 was pushed and later deleted; origin heads are again exactly custom_build and main, custom_build is still be6b8861, and origin's tag list is byte-identical before and after"
    requirement: "CI-17"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-10 (FINAL-REFS-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "CI-17 web-UI-only items (runner image version, GITHUB_TOKEN permissions block, job summary) are not readable without a token and remain for plan 17-04"
    requirement: "CI-17"
    verification: []
    human_judgment: true
    rationale: "Job logs return 403 unauthenticated; the user must paste the 'Set up job' section and the job summary from the web UI in plan 17-04"

duration: 17min
completed: 2026-10-11
status: complete
plan_head_before: f579deab6a27d9bca0a16a5082013acb0381e3dd
plan_head_after: 686036813abd34c85a415ad243846bc24afc991a
---

# Phase 17 Plan 03: CI-17 proof push and single green run Summary

**One branch-only push of `sync/v1.7.0` (X=0c90d331) produced exactly one green `tests.yaml` run on `ubuntu-24.04` with the Gradle-reported fork-count notice and both artifacts; the scratch branch was then deleted and origin's heads, tags and `custom_build` are unchanged.**

## Performance

- **Duration:** 17 min (about 20:25Z to 20:42Z)
- **Tasks:** 3 (Task 1 auto, Task 2 blocking-human decision, Task 3 auto run inline by the orchestrator)
- **Files modified:** 1 (`17-EVIDENCE.md`: E-09 and E-10, Index rows)

## Accomplishments

- **D-10 local gate (E-09, `0c90d331`).** `testUnit`: 37 classes, 464 tests, 0 failures, 0 errors, 2 skipped. Full `test --rerun` on ClickHouse `26.9.16.11` (image created 2026-10-11T16:49:40Z) x client V2: 71 classes, 840 tests, 0 failures, 0 errors, 18 skipped (7 named methods plus 11 bracketed `exactlyOnceStateMismatchTest` cases). No stale result files. Anchors invariant: `provenance_paths=21 anchor_commits=212 anchor_rows=43 marker_commits=259 result=PASS`. The four touched Java files answered `IS CLEAN`. Phase scope outside `.planning` is exactly ten paths; `src/main` and `fork-anchors.txt` are unchanged from `e0017d69`.
- **D-11 decision (Task 2).** The user answered `push` verbatim, directly to the orchestrator, at 2026-10-11T20:34:36Z, in reply to the checkpoint that carried the measured approval bundle (items 1-8, including F1). No default-branch intent was stated.
- **Push and run (E-10, `68603681`).** Task 3 ran inline in the orchestrator. At 20:34:51Z: `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0` (rc 0, no other flag or refspec). PUSH-READY re-passed before it (`rate=60`). Run `38172811430` (`https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38172811430`), `.github/workflows/tests.yaml`, event `push`, branch `sync/v1.7.0`, attempt 1, created 20:34:56Z, updated 20:40:22Z (about 5m26s), conclusion `success`. Exactly one run exists on X across all workflows, none cancelled.
- **Job evidence.** Job `114568815601` "ClickHouse latest Client version V2 tests", labels exactly `["ubuntu-24.04"]`, every one of the 14 recorded steps `success`. `Set up Gradle` succeeded under the top-level `permissions: contents: read`, which is the CI-13 proof that 17-01 left open.
- **Annotations.** One notice, title `Test fork count`, message `runner nproc=4, Gradle maxParallelForks=2 (read from the test task in build.gradle.kts)`. The other 20 annotations are pre-existing javac `[deprecation]` and `[unchecked]` warnings. No Node.js 20 deprecation, no `Ambiguous: Jackson2` and no `ubuntu-latest label will migrate` annotation.
- **Artifacts.** `gradle-test-reports` (27704420 bytes) and `jacoco-coverage-reports` (839599 bytes).
- **Cleanup.** At 2026-10-11T20:41:30Z, after green: `git push origin --delete refs/heads/sync/v1.7.0` (rc 0). Origin heads are exactly `custom_build` (`be6b8861c25f3d7611194e7150b18362c5c75e32`, never moved) and `main` (`bec167d85918e77c11621063000acc6e031ae2fa`). `git ls-remote --tags origin` is byte-identical to the before-snapshot.
- **API budget.** 60 requests remaining before the push, 50 after the evidence reads (unauthenticated core limit).

## Evidence verdicts

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-09 | CI-17 (D-10 local gate) | pass |
| E-10 | CI-17 (push and run) | pass |

Verify markers printed after the E-10 commit: `CI-17-RUN-GREEN`, `CI-17-RUN-EVIDENCE-OK`, `FINAL-REFS-OK`.

## Task Commits

1. **Task 1 (D-10 gate, push readiness, bundle):** `0c90d331` (docs) - E-09; this is the pushed SHA X
2. **Task 2 (D-11 decision):** no commit; user answered `push` directly to the orchestrator
3. **Task 3 (push, observe, delete, record):** `68603681` (docs) - E-10; local only, not pushed

## Files Created/Modified

- `.planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md` - E-09 and E-10 entries and their Index rows

## Decisions Made

- The pushed SHA X is `0c90d331` (HEAD at push time, equal to the bundle's X); the later E-10 commit `68603681` and this SUMMARY commit form a local tail that Phase 20 publishes.
- F1 was recorded as undecided rather than resolved by an agent; no GitHub setting (default branch, Dependabot, Actions) was touched.

## Deviations from Plan

None - plan executed exactly as written. Task 3 ran inline in the orchestrator as the plan's "Who runs what" section prescribes.

## Authentication Gates

None. The blocking-human D-11 checkpoint was a decision gate, answered directly by the user, and is documented as normal flow.

## Issues Encountered

None. The run was green on its first and only attempt; no re-run, second push or force operation occurred.

## Known Stubs

None.

## Threat Flags

None. The only push was the planned branch ref, with tags, `custom_build` and origin's tag list verified unchanged (T-17-13, T-17-14, T-17-15 and T-17-17 mitigated as planned; T-17-19 accepted in D-11 and disclosed in the bundle).

## User Setup Required

Plan 17-04 needs the user to paste the "Set up job" section and the job summary of run `38172811430` from the web UI, because job logs return 403 without a token.

## Next Phase Readiness

- Ready for 17-04. Open for it: the runner image version, the `GITHUB_TOKEN` permissions block and the job summary (web-UI only).
- CI-17 is not marked complete: plan 17-04 also declares CI-17 and owns those web-UI items. Only the API-observable part is proven here.
- Carried to Phase 20: finding F1 (default branch is `main`, intent undecided) with the Dependabot-enablement check (D-05); the local tail (Phase 16 bookkeeping, every Phase 17 commit, E-10 and this SUMMARY) is published only then.

## Self-Check: PASSED

- File exists: `.planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md` (contains `### E-09:` and `### E-10:` with `X=0c90d3314c1cf96cb68aed0fe3788c869341e7cd`).
- Commits `0c90d331` and `68603681` are ancestors of HEAD; `git rev-list --count f579deab6a27d9bca0a16a5082013acb0381e3dd..HEAD` gave 2 at SUMMARY write time.
- `git ls-remote --heads origin sync/v1.7.0` prints nothing; the working tree is on `sync/v1.7.0`.
- Verify markers `CI-17-RUN-GREEN`, `CI-17-RUN-EVIDENCE-OK` and `FINAL-REFS-OK` were printed by the orchestrator after the E-10 commit; the Gradle suite was not re-run for this close-out.

---
*Phase: 17-ci-build-test-hygiene*
*Completed: 2026-10-11*
