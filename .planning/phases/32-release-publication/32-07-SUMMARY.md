---
phase: 32-release-publication
plan: "07"
subsystem: nightly-first-run
tags: [nightly, nit-03, ci]
requires:
  - phase: 32-release-publication
    provides: "v1.7.0_gg5 published (plan 32-06)"
provides:
  - "E-80 GitHub after publication, E-81 the nightly's first manual run (green), E-82 NIGHTLY_ENABLED set by the operator"
key-files:
  modified:
    - .planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md
    - .planning/STATE.md
---

# Phase 32 Plan 07: the nightly's first manual run

Run inline by the orchestrator. POST-PUBLICATION-STATE-OK, then the user dispatched the nightly: run 40474535605 on custom_build (3a3a15a9) ended success (63 tests, 0 failures, 9 skipped, about 33 minutes); NIGHTLY-DISPATCH-RECORDED printed. The one scheduled run (09:02:27Z) was skipped. NIT-03 is met; the operator set NIGHTLY_ENABLED after the run (their report).

## Deviations

- The workflow-scoped runs listing returned no run for about 25 minutes after the user's report; the repository-wide listing showed it. The first poll therefore found nothing (E-81).
- The user's account dispatched release.yaml run 40474419526 (publish) one minute before the nightly and cancelled it during the archive build (mis-click, their answer). No release or tag was created (E-81, E-82).
- The token route for job logs was not needed.
