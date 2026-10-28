---
phase: 32-release-publication
plan: "06"
subsystem: publication
tags: [publish, tags, ci]
requires:
  - phase: 32-release-publication
    provides: "gated frozen tip (plan 32-05)"
provides:
  - "v1.7.0_gg5 on origin/custom_build at 3a3a15a9, tags v1.7.0_gg5 and pre-v1.7.0_gg5, E-70..E-73"
key-files:
  modified:
    - .planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md
    - .planning/STATE.md
---

# Phase 32 Plan 06: publish v1.7.0_gg5

Run inline by the orchestrator after the user's direct "publish". Scratch push of sync/v1.7.0 (X0 41e7d546), one green tests.yaml run (40414210750); --no-ff merge M = 3a3a15a9 onto 21c51598; checks on M passed; custom_build pushed without force; one green run (40416215635); tags v1.7.0_gg5 (M) and pre-v1.7.0_gg5 (21c51598) pushed by name; sync/v1.7.0 deleted from origin. PUBLISHED-AND-TAGGED-OK and RECORDS-AND-TAIL-OK printed. No release or workflow dispatch, no GitHub release.

## Deviations

The late re-check of M's run was at 117 s after completion, not 120 s; a further check 198 s after completion showed the same single run (E-72).
After the merge Dependabot opened a github-actions branch (first for that ecosystem) and refreshed gradle branches; none is part of the release (E-73).
