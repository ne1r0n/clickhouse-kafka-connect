---
phase: 29-release-publication
plan: "08"
subsystem: release
tags: [release, publication, tags, ci]

requires:
  - phase: 29-release-publication
    provides: "plan 29-07: X0 rehearsed green on CI, the release bundle, E-62"
provides:
  - "v1.7.0_gg4 published on origin/custom_build at 21c51598 with one green tests.yaml run (40022281005)"
  - "tags v1.7.0_gg4 (21c51598) and pre-v1.7.0_gg4 (69cd680b) on origin; sync/v1.7.0 deleted from origin"
  - "E-70..E-72 and the STATE.md publication lines on the local tail"

requirements-completed: [PUB-13]
---

# Phase 29 Plan 08: Publish v1.7.0_gg4

Task 1 (dry run, E-70, commit `3d847009`) ran in a dispatched gsd-executor with no push. Task 2 was the blocking-human decision, answered directly to the orchestrator. Task 3 ran inline in the orchestrator.

## Approval

The user's verbatim answer to the orchestrator, after the release bundle (29-07-SUMMARY "Approval bundle") and the E-70 dry run were presented: **"publish"**.

## What was pushed (no force flag, no tag pattern)

| Time (UTC) | Command |
|---|---|
| 2026-10-23T20:50:14Z | `git push origin refs/heads/custom_build:refs/heads/custom_build` (`69cd680b..21c51598`) |
| 2026-10-23T20:59:44Z | `git push origin refs/tags/v1.7.0_gg4:refs/tags/v1.7.0_gg4` |
| 2026-10-23T20:59:44Z | `git push origin refs/tags/pre-v1.7.0_gg4:refs/tags/pre-v1.7.0_gg4` |
| after both tags | `git push origin --delete refs/heads/sync/v1.7.0` |

M = `21c51598b06b6f5af58430765d57b1e5f6747c5a`, parents `69cd680b` and `3d847009`, tree equal to X. CI: run 40022281005, `tests.yaml`, success, 20:50:16Z to 20:56:40Z; exactly one run, none cancelled, re-checked about two minutes later; the tag pushes started no run.

## Deviations

1. The plan's "worktree `git status --porcelain` is empty" clause cannot hold for `custom_build` because of the known CRLF line ` M gradlew.bat` (E-30, E-70); the check was run with that one line excluded (recorded in E-71).
2. The first run of the dry-run merge check at the gate used `3d847009`, not the bundle's 272-commit count: the branch tip was 275 commits ahead of `69cd680b` (three `.planning`-only commits after X0).

## Checks

MERGE-CHECKS-ON-M-OK, PUBLISHED-AND-TAGGED-OK and RAW-NUMBERS-AND-TAIL-OK print. Dependabot heads unchanged; `main` unchanged.

## Self-Check: PASSED
