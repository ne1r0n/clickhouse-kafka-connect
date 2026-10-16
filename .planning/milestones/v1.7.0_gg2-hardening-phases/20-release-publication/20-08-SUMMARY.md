---
phase: 20-release-publication
plan: "08"
subsystem: release
tags: [manual-checks, dependabot, default-branch, milestone-close]

requires:
  - phase: 20-release-publication
    provides: "20-07: v1.7.0_gg2 published (M a246ca01 on origin/custom_build, tag pushed, run 38587062864 green)"
provides:
  - "The three D-11 manual-check outcomes recorded verbatim in 20-MILESTONE-CLOSE-RECORD.md (Dependabot enabled; default branch changed to custom_build by the user; no Dependabot config error)"
  - "STATE.md Operator Next Steps: verify Phase 20 -> /gsd-audit-milestone (replaces the stale untracked 2026-10-10 snapshot) -> /gsd-complete-milestone carrying the close record -> token revocation"
affects: [milestone-close]

actuals:
  tokens: 6000
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md
    - .planning/STATE.md

key-decisions:
  - "The user answered all three checks directly to the orchestrator (D-11); Task 2 ran orchestrator-inline as the plan directs"
  - "The filled Outcome lines are written unbulleted so the CLOSE-STEPS-RECORDED regex (^Outcome:) counts them; the pending lines had carried a list bullet"
  - "Check 3's A18 'not observable yet' path did not apply: the user's own default-branch change made the Dependabot tab observable immediately; the gradle 'Last checked' residue is carried in the close record"

patterns-established: []

requirements-completed: [PUB-08]
---

# Plan 20-08: Post-publication manual checks and close steps — Summary

**Both tasks complete.** The user performed the three D-11 checks in the GitHub web UI and answered the orchestrator directly in-session; the answers are on record for the next milestone and STATE.md points at the audit and the close. One local commit (`0e1b7496`) on `sync/v1.7.0`; nothing pushed; origin's heads and tags byte-identical before and after (CLOSE-STEPS-RECORDED).

## The three answers, verbatim (received directly by the orchestrator, 2026-10-14)

1. **Dependabot version updates:** "enabled"
2. **Default branch:** "changed to custom_build" — the user made the change themselves; no agent touched a setting. Consequence recorded: Dependabot and scheduled workflows now read the fork's files, closing Phase 17 F1.
3. **`exclude-paths` validity:** "no errors, I see only" + a screenshot of Insights > Dependency graph > Dependabot showing the `actions` entry "Running version update job now" (job 1627650108). No configuration error — `exclude-paths` did not invalidate `dependabot.yml` (17-REVIEW WR-01 largely closed). Residue carried in the close record: confirm the `gradle` entry shows a recent "Last checked" once the first jobs finish.

## What was recorded where

- `20-MILESTONE-CLOSE-RECORD.md` "Manual checks after publication": each `Outcome: pending (plan 20-08)` replaced with the verbatim answer, the UTC date and "given directly to the orchestrator"; a consequence note under check 2 and a clearly-labelled orchestrator annotation under check 3.
- `.planning/STATE.md`: the two [CI] Blockers lines that carried the checks now record the outcomes (✓ 2026-10-14) and point at the close record. `## Operator Next Steps` rewritten in order: verify Phase 20; `/gsd-audit-milestone` (D-12, replaces the stale untracked `.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md` of 2026-10-10 — never commit it); `/gsd-complete-milestone` carrying the close record (five 19-REVIEW advisories, IN-04, backlog 999.3, the three outcomes); revoke the Keychain token `gsd17-gh-actions-read` if used (it was not needed this session).

## Verification

CLOSE-STEPS-RECORDED printed: 3 outcomes, none pending, all with the direct-receipt note, no token patterns in the record, STATE.md names the audit/close/record/stale-snapshot, the stale snapshot untracked, both files committed, origin heads and tags unchanged (`/tmp/gsd20/exec/origin-heads-final.txt` / `origin-tags-final.txt` vs. the end-of-task re-snapshot), the record commit not on origin, main tree on `sync/v1.7.0`.

## Next steps (as written to STATE.md)

Phase 20 verification runs next in this session, then `/gsd-audit-milestone`, then `/gsd-complete-milestone`.
