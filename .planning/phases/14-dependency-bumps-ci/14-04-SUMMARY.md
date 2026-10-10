---
phase: 14-dependency-bumps-ci
plan: "04"
subsystem: ci
tags: [github-actions, tests-yaml, ci-observation, push, d-13]

requires:
  - phase: 14-03
    provides: "Final Phase 14 code commit 7bdf8328 and the D-09 local gate (Recorded Fact 36)"
provides:
  - "Phase 12 D-01 amended by Phase 14 D-13 (12-CONTEXT.md, commit 54b5ce2f)"
  - "sync/v1.7.0 published to origin at 54b5ce2f, branch only, no tag, custom_build untouched"
  - "One green tests.yaml push run on 54b5ce2f (run 38037397969) with the compile step and fork-count annotation observed"
  - "Recorded Fact 37 (CI-08, CI-09, CI-10 scratch-branch CI evidence), local-only until Phase 15"
affects: [15]

actuals:
  tokens: 1500
  tasks: 3
  commits: 2

plan_head_before: 3feb22d2133fe2eb6f9b5452e4acf06201204071
plan_head_after: 97d03f10

tech-stack:
  added: []
  patterns:
    - "Locate a CI run by head_sha plus workflow path and assert cardinality exactly one; read steps from /jobs and measurements from check-run annotations (no token needed)"

key-files:
  created: []
  modified:
    - .planning/phases/12-upstream-v1-7-0-merge-reconcile/12-CONTEXT.md
    - .planning/MERGE-CHECKLIST-v1.7.0.md

key-decisions:
  - "D-13: the user answered \"push\" directly to the orchestrator; the orchestrator ran Task 3 inline (the executor refuses relayed approvals)"
  - "Pushed with the explicit refspec refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0 only; the post-push evidence commit stays local until Phase 15 (PUB-04)"

requirements-completed: [CI-08, CI-09, CI-10]

coverage:
  - id: D1
    description: "Branch-only push: origin sync/v1.7.0 equals the pushed HEAD, custom_build unchanged, origin tag list byte-identical before and after"
    requirement: CI-08
    verification:
      - kind: other
        ref: "plan Task 3 verify block 1 printed REFS-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "Exactly one completed, successful push run of tests.yaml on 54b5ce2f, no cancelled sibling (D-11b)"
    requirement: CI-08
    verification:
      - kind: other
        ref: "plan Task 3 verify block 2 printed RUN-OK; https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38037397969"
        status: pass
    human_judgment: false
  - id: D3
    description: "Compile integration-test sources (step 6) succeeded before Run tests (step 7) on a real runner"
    requirement: CI-10
    verification:
      - kind: other
        ref: "GET /actions/runs/38037397969/jobs step list"
        status: pass
    human_judgment: false
  - id: D4
    description: "CI fork count measured: runner nproc=4, derived maxParallelForks=2"
    requirement: CI-09
    verification:
      - kind: other
        ref: "GET /check-runs/114170598071/annotations, annotation titled \"Test fork count\""
        status: pass
    human_judgment: false
---

# Phase 14 Plan 04: CI observation behind the D-13 gate — Summary

**The fixed `tests.yaml` ran once on CI hardware for the pushed `sync/v1.7.0` HEAD and passed, with the compile step in place and a measured fork count of 2 (nproc=4).**

## Performance

- Task 1 and the checkpoint: 2026-10-09 (executor). Task 3: 2026-10-10, ~08:17–08:30 UTC (orchestrator, inline).
- CI run duration: 6m 32s (08:18:04Z → 08:24:36Z).

## Accomplishments

- **Decision:** the user replied "push" at the blocking-human D-13 checkpoint, directly to the orchestrator.
- **Push:** `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0` published `54b5ce2f3fbe7aafb544f53e95283ddede9aa7b1`. Afterwards `ls-remote` showed origin `sync/v1.7.0` = `54b5ce2f…`, origin `custom_build` = `c61d90ce5dfb32ea5876dffacc8bb54d8846739a` (unchanged), and the origin tag list was byte-identical (`cmp`) to `build/phase14-origin-tags-before.txt`.
- **Run:** id `38037397969`, https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38037397969. Event `push`, branch `sync/v1.7.0`, conclusion `success`. It was the only `tests.yaml` run for that SHA, with no cancelled sibling.
- **Steps:**
  1. Set up job
  2. checkout
  3. JDK 17
  4. Gradle
  5. **Report test fork count**
  6. **Compile integration-test sources**
  7. **Run tests**
  8. coverage summary
  9. JaCoCo upload
  10. test reports upload

  Steps 18–21 were post steps and Complete job. All succeeded.
- **Fork count (CI-09):** `runner nproc=4, derived maxParallelForks=2 (build.gradle.kts: availableProcessors/2, minimum 1)` on runner `GitHub Actions 1000000181`, label `ubuntu-latest`.
- **Invariants:** `provenance_paths=21 anchor_commits=91 anchor_rows=40 marker_commits=138 result=PASS`.

## Task Commits

1. **Task 1: pre-push readiness and the D-13 amendment.** `54b5ce2f`, `docs(14-04)`, amends Phase 12 D-01.
2. **Task 2: D-13 decision.** Checkpoint, no commit. Answer: push.
3. **Task 3: push, observe and record.** `97d03f10`, `docs(14-04): record the scratch-branch CI evidence (CI-08, CI-09, CI-10)`. This commit is local only.

## Files Created/Modified

- `.planning/phases/12-upstream-v1-7-0-merge-reconcile/12-CONTEXT.md`: the D-13 amendment under D-01.
- `.planning/MERGE-CHECKLIST-v1.7.0.md`: Recorded Fact 37.
- Untracked evidence: `build/phase14-origin-tags-before.txt` and `build/phase14-origin-tags-after.txt`.

## Decisions Made

- Task 3 ran inline in the orchestrator, because the user's answer went to the orchestrator and gsd-executor refuses relayed approvals at blocking-human gates.

## Deviations from Plan

None. The plan expects this inline route for a relayed answer (threat T-14-12).

## Issues Encountered

None blocking. The run's annotations surfaced four kinds of warning, none of them failing:
- **Node.js 20 deprecation:** affects `actions/checkout@v4` and `actions/upload-artifact@v4`.
- **Ubuntu 26 migration:** `ubuntu-latest` moves to Ubuntu 26 from 2026-10-19.
- **javac deprecation and unchecked warnings.**
- **Lombok "Ambiguous: Jackson2 and Jackson3 exist" warning:** worth a look in Phase 15.

## Known Stubs

None.

## Threat Flags

- **T-14-11 and T-14-13 are mitigated:**
  - REFS-OK held.
  - The tag lists are identical.
  - `custom_build` is unchanged.
- **T-14-12 is mitigated:** the human answered directly.

## Next Phase Readiness

Phase 15 can rely on the new trigger, which produced exactly one run for a branch push. Two residual risks remain:

1. **Two branches at one SHA:** if two branches are pushed at the same SHA, their runs cancel each other. Push the published SHA on one branch only.
2. **`pre-v1.7.0-sync` (c61d90ce):** this tag predates the fix. Pushing it still runs the old workflow, on c61d90ce, not on the published SHA.

Local commits after `54b5ce2f` stay unpushed until PUB-04.

## Self-Check: PASSED
