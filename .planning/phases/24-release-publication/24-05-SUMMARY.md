---
phase: 24-release-publication
plan: "05"
subsystem: release
tags: [release, milestone-close-record, review-disposition, pre-gate-review, rollout-note, evidence]

requires:
  - phase: 24-release-publication
    provides: plans 24-01 to 24-04 (the live test, evidence E-01..E-33, the rollout note, the CHANGELOG and FORK-DELTA rows)
provides:
  - "24-MILESTONE-CLOSE-RECORD.md: 17 carried findings, 2 fulfilled, the jacoco gap and one Phase 24 info item, three user reminders, the audit sentence, housekeeping (D-06, D-11)"
  - "21-, 22- and 23-REVIEW-DISPOSITION.md flipped to fixed or deferred, open: 0, totals 7, 6, 6"
  - "24-REVIEW.md: a pre-gate review of the six Phase 24 files with the nine mandated confirmations, two rounds (20-D-07)"
  - "24-REVIEW-DISPOSITION.md: 5 findings, open: 0; three wording fixes in the rollout note, CHANGELOG and FORK-DELTA"
  - "Evidence E-40"
affects: [24-06, 24-07]

actuals:
  tokens: 10625
  tasks: 3
  commits: 7
plan_head_before: f8241023c51702e41e4a2e9a7f0c83ece006249f
plan_head_after: 4ed569602ed3b9837fcbe2dbd648d19c1d10c1f0
commits: 7

tech-stack:
  added: []
  patterns:
    - "Review before the gate: the reviewer reads operator text against the evidence entry that backs it, and a fix is re-proven by the owning plan's checks"
    - "Close record: one heading per carried finding with Reason, Owner, Re-open trigger, Source; deferred dispositions name the record"

key-files:
  created:
    - .planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md
    - .planning/phases/24-release-publication/24-REVIEW.md
    - .planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md
  modified:
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-REVIEW-DISPOSITION.md
    - .planning/phases/22-writer-record-data-exposure/22-REVIEW-DISPOSITION.md
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-REVIEW-DISPOSITION.md
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md
    - docs/ROLLOUT-v1.7.0_gg3.md
    - CHANGELOG.md
    - docs/FORK-DELTA.md

key-decisions:
  - "The WR-01 fix keeps the D-02 phrase 'whatever the connector settings' and scopes it to the measured route, instead of dropping it as the round 1 suggestion read: D-02 is a locked decision and the scoping alone removes the contradiction"
  - "IN-02 (FORK-DELTA written in the tense of publication) is skipped by design: the gg2 text did the same before its tag existed, and plan 24-06's bundle states it"
  - "The Phase 24 info item IN-03 is carried under 'Other carried items' of the close record, not in the carried-advisories section, because CLOSE-RECORD-OK requires exactly the 17 Phase 21 to 23 findings there"

requirements-completed: [ROLL-03, PUB-09]

coverage:
  - id: D1
    description: "Close record with the 17 carried findings (reason, owner, trigger, source), 21 IN-03 and 23 IN-04 as fulfilled, the jacocoForkReport gap, the three user reminders and the audit and housekeeping sections"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-05 Task 1 verify command CLOSE-RECORD-OK"
        status: pass
    human_judgment: true
    rationale: "The reasons, owners and re-open triggers are judgments about what the next milestone should pick up; the user reads them in /gsd-complete-milestone"
  - id: D2
    description: "21, 22 and 23 dispositions: 19 rows, open: 0, 21 IN-03 fixed by 177f247c, 23 IN-04 fixed by 9d4cb9b2, the other 17 deferred to the close record"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-05 Task 1 verify command DISPOSITIONS-21-23-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "Pre-gate review 24-REVIEW.md of the six Phase 24 files: round 1 found 2 warnings and 3 info, round 2 over the fix commits found nothing, nine confirmations given"
    requirement: PUB-09
    verification:
      - kind: other
        ref: "plan 24-05 Task 2 verify command REVIEW-24-OK"
        status: pass
    human_judgment: true
    rationale: "The review is an inline reading by the executing agent, not an independent reviewer; the user decides at plan 24-06 whether its verdicts are enough for the publication gate"
  - id: D4
    description: "Five findings dispositioned with open: 0; three fixes re-proven by the plan 24-03 and 24-04 checks (E-40); documents frozen"
    requirement: PUB-09
    verification:
      - kind: other
        ref: "plan 24-05 Task 3 verify commands DISPOSITION-24-OK and DOCS-FROZEN-OK"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 05: Milestone close record, review dispositions and the pre-gate review Summary

**A close record that carries 17 review findings with reason, owner and trigger, 21/22/23 dispositions at `open: 0`, and a two-round pre-gate review whose two warnings (the route of the `size` defect, an "ever stored" overclaim in item 4) were fixed in the rollout note, CHANGELOG and FORK-DELTA before the suite gate**

## Performance

- **Duration:** about 20 min (start time not captured at the first command; estimated from the previous plan's end)
- **Started:** 2026-10-20T16:58Z (estimate)
- **Completed:** 2026-10-20T17:18Z
- **Tasks:** 3
- **Files modified:** 10 (3 new); no file under `src`, `build.gradle.kts`, `gradle` or `.github` changed

## Accomplishments

- **Close record (`dab5a52c`).** `24-MILESTONE-CLOSE-RECORD.md` holds the 17 carried findings (21 WR-01..03, IN-01, IN-02, IN-04; 22 WR-01, IN-01..05; 23 WR-01, WR-02, IN-01..03), each with Reason, Owner (the next milestone), Re-open trigger and Source. 22 WR-01 points at `JsonAsStringKeyResidualLiveTest` and E-03 (the D-04 branch 1 result). 21 IN-03 and 23 IN-04 are recorded as fulfilled with their commits `177f247c` and `9d4cb9b2`. The jacoco class-list gap has an owner and a trigger. The three user reminders (the `dependencies` label; PRs #4 and #9; PRs #6, #7, #8 and the DEPS-01-style merge) each carry exact web-UI steps and "Outcome: reminder only; nothing waits on it (D-11)". The audit sentence (`/gsd-audit-milestone` before `/gsd-complete-milestone`) and the housekeeping list are in.
- **Disposition flips (`dab5a52c`).** The 21, 22 and 23 files read `open: 0` with totals 7, 6 and 6 and 19 rows: two `fixed` with the SHA of the fixing commit and 17 `deferred` with a Source naming the close record.
- **Pre-gate review (`91fa4137`, `cc266815`).** Round 1 over `P24_BASE..HEAD` found no Critical and two Warnings: WR-01 (the `size` defect stated without the route E-15 measured, and the note's "whatever the connector settings" contradicting its own qualifier) and WR-02 (item 4 saying `v1.7.0_gg2` "never stored a batch of that shape" for 14 shapes when E-11 inserted four). Three Info: IN-01 (CHANGELOG insert outcome), IN-02 (FORK-DELTA tense of publication), IN-03 (planning identifiers in the new test). Round 2 over the fix commits found nothing.
- **Fixes (`0c08e663`, `cd40803c`, `6ee4a22a`).** One commit per finding, wording only. All nine plan 24-03 and 24-04 checks print OK again, `consistency.py` reports 0 failures, and the FORK-DELTA raw numbers still hold because nothing under `src` changed.
- **Disposition (`4ed56960`).** `24-REVIEW-DISPOSITION.md`: 3 fixed (with commits), 1 skipped by design, 1 deferred to the close record, `open: 0`, `total: 5`, with the nine confirmation verdicts.

## Mandated confirmation verdicts

1. Headline and Map-key exception match E-03, no nonexistent connector key: holds. 2. Every quoted query and command has evidence, detection queries proven against look-alikes: holds. 3. No TRUNCATE or unscoped DELETE: holds. 4. Rollback by `read-tree` with `pre-v1.7.0_gg3`, E-22 and E-04: holds. 5. Released CHANGELOG sections and gg1/gg2 notes unchanged: holds. 6. No secret, host, record value or production identifier: holds. 7. The three documents agree: failed in round 1 (WR-01, WR-02), holds after the fixes. 8. The new test follows AGENTS.md: holds. 9. No workaround for the `size` item, no CHANGELOG line for the escaped-name limitation: holds.

## Task Commits

1. **Task 1: close record and 21/22/23 flips** - `dab5a52c` (docs; `CLOSE-RECORD-OK`, `DISPOSITIONS-21-23-OK`)
2. **Task 2: pre-gate review, round 1** - `91fa4137` (docs; `REVIEW-24-OK`)
3. **Task 3: fixes and disposition** - `0c08e663` (WR-01), `cd40803c` (WR-02), `6ee4a22a` (IN-01), `cc266815` (round 2 and E-40), `4ed56960` (disposition and the IN-03 close-record item; `DISPOSITION-24-OK`, `DOCS-FROZEN-OK`)

**Plan metadata:** the commit of this SUMMARY and the state update.

## Files Created/Modified

- `.planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md` - new
- `.planning/phases/24-release-publication/24-REVIEW.md` - new, two rounds
- `.planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md` - new
- `.planning/phases/21-.../21-REVIEW-DISPOSITION.md`, `22-.../22-REVIEW-DISPOSITION.md`, `23-.../23-REVIEW-DISPOSITION.md` - flipped
- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` - E-40
- `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md` - three wording fixes
- Scratch, not committed: `/tmp/gsd24-scripts/v05-1.sh` to `v05-5.sh` (the plan's verify commands), `flip-21-23.py`, `stage-hunks.py`, `pin-24-05.sh`

## Decisions Made

See `key-decisions`. In short: the D-02 phrase is kept and scoped; IN-02 is skipped by design; IN-03 sits in "Other carried items".

## Skill adaptation

The `gsd-code-review` skill was not invoked. An executor cannot spawn the reviewer sub-agent, and the skill's file-list handling is not usable on macOS (no `realpath -m`), so the review was done inline by reading `~/.claude/gsd-core/agents/gsd-code-reviewer.md` and applying its procedure and schema (Phase 15/16/20 precedent, assumption A11). The review is therefore the executing agent's reading, not an independent reviewer's; the user should weigh that at plan 24-06. The heading rule was followed: every finding is a `### <ID>:` heading and the round 2 text adds none.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug: documents overclaiming] Two warnings and one info found by the review and fixed in the reviewed files**
- **Found during:** Task 2 (review), fixed in Task 3
- **Issue:** WR-01, WR-02 and IN-01 as above.
- **Fix:** Wording changes in `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md` and `docs/FORK-DELTA.md`, one commit per finding.
- **Verification:** the nine checks of plans 24-03 and 24-04, `consistency.py`, `DOCS-FROZEN-OK` (E-40).
- **Committed in:** `0c08e663`, `cd40803c`, `6ee4a22a`

### Plan text adapted

- The plan's Task 3 says to add a deferred Info to "the close record's carried section". `CLOSE-RECORD-OK` requires exactly 17 blocks there, so IN-03 went under "Other carried items" with an Owner and a trigger.
- The round 1 fix suggestion for WR-01 dropped the phrase "whatever the connector settings"; the committed text keeps it (D-02 is locked) and scopes it to the measured route. The check `ITEMS-2-3-OK` requires the phrase.

---

**Total deviations:** 1 auto-fixed (Rule 1), 0 architectural.
**Impact on plan:** None on scope. The fixes stay inside the reviewed files and inside D-01..D-15.

## Issues Encountered

- The first run of the item 2 and item 3 check printed no `ITEMS-2-3-OK` after the WR-01 edit, because it requires the D-02 phrase; the wording was corrected before the commit.

## Known Stubs

None.

## Threat Flags

None. Only documents and planning records changed. T-24-15: `REVIEW-24-OK` rejects deeper finding headings and a total that differs from the heading count, and `DISPOSITION-24-OK` requires one row per heading. T-24-16: `DOCS-FROZEN-OK` shows `src/main`, `src/testFixtures`, the build, `.github` and the invariant scripts unchanged since `P24_BASE`. T-24-17: `CLOSE-RECORD-OK` and `DISPOSITIONS-21-23-OK` pass.

## User Setup Required

None - no external service configuration required. The three GitHub web-UI reminders are in the close record and wait on nothing.

## Next Phase Readiness

- Plan 24-06 (suite gate, rehearsal, approval bundle) can run: every Phase 21 to 24 disposition file reads `open: 0`, and the documents are frozen (`DOCS-FROZEN-OK`).
- For the bundle: say that FORK-DELTA's intro line and its gg3 syncer paragraph are written in the tense of publication and become true at plan 24-07 (IN-02); compare the CHANGELOG date `2026-10-20` with the publication date (A10); say that the pre-gate review was inline, not an independent reviewer.
- The end-of-phase code review that `/gsd-execute-phase` runs may append to `24-REVIEW.md`; its findings need `### ID:` headings continuing the numbering (IN-04 onward, WR-03 onward).

## Self-Check: PASSED

- Files: the three new files and all modified files exist; `### E-40:` is in the evidence.
- Commits `dab5a52c`, `91fa4137`, `0c08e663`, `cd40803c`, `6ee4a22a`, `cc266815`, `4ed56960` are ancestors of HEAD; `git rev-list --count f8241023..HEAD` = 7 before this SUMMARY.
- Acceptance: `CLOSE-RECORD-OK`, `DISPOSITIONS-21-23-OK`, `REVIEW-24-OK`, `DISPOSITION-24-OK` and `DOCS-FROZEN-OK` printed on the committed tip; the nine plan 24-03 and 24-04 checks print OK.

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
