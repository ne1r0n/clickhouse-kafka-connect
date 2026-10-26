---
phase: 31-fork-owned-nightly-integration-tests
plan: "05"
subsystem: testing
tags: [ci, github-actions, roadmap, phase-gate, nightly]

requires:
  - phase: 31-fork-owned-nightly-integration-tests
    provides: "31-02 branch decision, 31-03 byte guard, 31-04 gated nightly workflow and guards, 31-06 green measurement (E-18)"
provides:
  - "E-15: the Phase 31 gate on the final tree (anchors 131 rows PASS, testUnit 1374 tests 0 failures, src/main equals v1.7.0_gg4) and both workflows' GitHub state"
  - "E-16: the user's NIT-03 decision, move, received directly by the orchestrator"
  - "NIT-03 moved to Phase 32 in REQUIREMENTS.md, ROADMAP.md and STATE.md"
affects: [phase-32-release-publication]

actuals:
  tokens: 4000
  tasks: 3
  commits: 2
plan_head_before: f0f213bf5a11dcc43195e3993dda8bd896d185eb

key-files:
  created: []
  modified:
    - .planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md
    - .planning/ROADMAP.md
    - .planning/REQUIREMENTS.md
    - .planning/STATE.md

key-decisions:
  - "NIT-03 moves to Phase 32 (user answer: move, 2026-10-26): GitHub dispatches a workflow only from the default branch, and the nightly reaches custom_build only at publication"
  - "Open questions 1, 2 and 4 stay at their defaults"
  - "The plan said eight ROADMAP edits; E-15's list has nine, and all nine were applied"

requirements-completed: []

coverage:
  - id: D1
    description: "Phase 31 gate on the final tree and the GitHub state of both workflows are recorded"
    requirement: NIT-01
    verification:
      - kind: other
        ref: "31-COMMAND-EVIDENCE.md E-15 (ANCHORS-GATE-OK, SUITE-GATE-OK, PHASE-STATE-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The user's NIT-03 decision is recorded verbatim and the planning documents agree with it"
    requirement: NIT-03
    verification:
      - kind: other
        ref: "Task 3 verify script, NIT03-RECORD-OK conditions (decision, moved row, requirements, criterion, Phase 30 and Sync Mechanics text unchanged)"
        status: pass
    human_judgment: true
---

# Phase 31 Plan 05: Phase gate and NIT-03 decision Summary

The Phase 31 gate passed on the final tree, and the user decided `move`: the nightly's first manual run (NIT-03) is now a Phase 32 requirement.

## Accomplishments
- Task 1 (executor, `8b9f1453`): recorded E-15, the anchors gate, the cache-busted `testUnit` run, the green measurement E-18 (no skip list needed), the Actions API state of `integrationTests.yaml` (`disabled_manually`, no run after 2026-10-16T10:00:00Z) and the nightly's absence from `custom_build`.
- Task 2: the blocking decision went to the user directly; the answer was `move`.
- Task 3 (orchestrator, inline, `7f47af2f`): recorded E-16, applied 4 REQUIREMENTS edits, 9 ROADMAP edits and the STATE line, and flipped E-15 to `Matches text: yes`. Nothing was pushed.

## Deviations
None. NIT-01 and NIT-03 stay unchecked in REQUIREMENTS.md until Phase 32 and the verifier.

## Carried to Phase 32
Disk (about 13.8 GB of images on a 14 GB runner), memory, x86 against arm64, Docker Hub limits, `latest` drift, the Java 17 worker FORK-DELTA row (CIG-02) and the post-merge dispatch with `NIGHTLY_ENABLED` unset until it is recorded.
