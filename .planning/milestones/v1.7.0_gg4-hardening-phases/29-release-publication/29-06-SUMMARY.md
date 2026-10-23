---
phase: 29-release-publication
plan: "06"
subsystem: release
tags: [release, review, operator-docs, disposition, pub-12]

requires:
  - phase: 29-release-publication
    provides: "plans 29-01 to 29-05: VERSION bump, evidence E-01..E-43, the gg4 rollout note, CHANGELOG, FORK-DELTA and the close record"
provides:
  - "29-REVIEW.md: four orchestrator-spawned, fresh-context gsd-code-reviewer rounds over the ten operator files (13, 4, 2, 1 findings; 20 in all)"
  - "29-REVIEW-DISPOSITION.md: 20 findings, all fixed, open: 0, round3_decision recorded"
  - "Fix commits CR-01..IN-09 on the gg4 note, the gg1 note, CHANGELOG, FORK-DELTA and the close record; E-50 evidence entry"
affects: [29-07, 29-08]

actuals:
  tasks: 3

key-files:
  created:
    - .planning/phases/29-release-publication/29-REVIEW.md
    - .planning/phases/29-release-publication/29-REVIEW-DISPOSITION.md
  modified:
    - docs/ROLLOUT-v1.7.0_gg4.md
    - docs/ROLLOUT-v1.7.0_gg1.md
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - .planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md

requirements-completed: [PUB-12]
---

# Phase 29 Plan 06: Independent review of the operator documents

## Rounds

| Round | Reviewer | Scope | Findings |
|-------|----------|-------|----------|
| 1 | gsd-code-reviewer spawned by the orchestrator, fresh context | the ten files, diff base `c1dc6e59` | 13 (1 critical, 8 warning, 4 info) |
| 2 | fresh reviewer | the round-1 fix commits | 4 (2 warning, 2 info) |
| 3 | fresh reviewer | the round-2 fix commits | 2 (info) |
| 4 | fresh reviewer (authorised by the user) | the round-3 fix commits | 1 (info) |

Fixes were applied by gsd-executor agents dispatched with fix-only prompts (no executor reviewed). The reviewer's Fix text was wrong or incomplete in 6 places and the executors corrected it against code and evidence: CR-01 (no producer pause), IN-04 (Enum labels were V2-only), WR-10 (the "reverse order" wording), IN-08 (resume before the check), and IN-07 (went further).

## Key findings fixed

- CR-01: the rollback told the operator to rename the producers back before gg4 had drained, which stores column DEFAULTs silently. The order is now pause, drain, stop, check, switch back, redeploy gg3, resume.
- WR-01..WR-10: upgrade-direction clean-stop check, overclaims (rejected before the token, unit test on every build), jspecify and others missing from the dependency list, one wording for the unreadable-declared-type residual, a false "safe" for gg1 multi-topic connectors, and the producer-pause window.

## Deviations

1. **Round 3 decision (blocking-human).** Round 3 still had findings. The user was asked directly and answered "Fix them, then round 4" (recorded as `round3_decision` in the disposition file).
2. **IN-09 not reviewed.** Round 4 raised IN-09 (Info, CHANGELOG scope). The user authorised four rounds, not five, so the orchestrator fixed it in `be5a339f` (wording; reflow `b3035d25`) and re-ran every owning check. No later round read it.
3. **Stale clause in two owning checks.** ROLL05-OK and NOTE-FINAL-OK end in a `git diff --quiet c1dc6e59 HEAD -- CHANGELOG.md docs/FORK-DELTA.md ...` clause that is false after plan 29-05; it was dropped for those runs. NOTE-FINAL-OK also needs the server version `26.9.19.9` masked in its address scan. Plan 29-07 should use the same variants (`/tmp/gsd29/checks/03-2x.sh`, `04-4x.sh`).
4. **E-08 verdict** settled from `pending (29-05)` to `yes` (commit `7c839738`) so the docs-frozen check passes; ops04 stays open by nature and is re-read in 29-07.

## Checks on the committed tip

REVIEW-29-ROUND-OK, REVIEW-ROUNDS-OK, DISPOSITION-29-OK and DOCS-FROZEN-OK print; the owning checks of plans 29-03, 29-04 and 29-05 pass (variants above). No `src`, test, build, `.github` or invariant-script change; nothing pushed or tagged.

## Self-Check: PASSED
