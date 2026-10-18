---
phase: 21-dependabot-integration-ci-hygiene
plan: "05"
subsystem: infra
tags: [phase-close-gate, anchors-gate, spotless, dependabot, github-actions, ops-observation, evidence]

requires:
  - phase: 21-dependabot-integration-ci-hygiene
    provides: "21-02/21-03: the DEPS-01 merge 82524e12 and the anchor exemption; 21-04: the pin-guard rules; 21-01: E-01 and E-02 (the OPS-01 facts at phase start and the operator answers)"
provides:
  - "E-12: the Phase 21 close gate at the tip (anchors gate with anchor_exempt=1 PASS, self-test 16 PASS, testUnit SUCCESS, spotless baseline-equal, scope and no-push facts)"
  - "E-13: the OPS-01 closing observation on GitHub's clock after the D-11 window, verdict pass (integrationTests.yaml still disabled_manually, zero schedule runs after the disable, control query 2)"
  - "ROADMAP Phase 21 criterion 4 and the 'before Phases 22-23' conditions of criterion 1 are met; Phase 21 can be verified and closed (D-12)"
affects: [21-verification, 22, 24 pre-gate OPS-01 re-check]

actuals:
  tokens: 1400
  tasks: 2
  commits: 2

plan_head_before: 4ba6ce23dd09589edad48168c53251b439a22923
plan_head_after: 7c06f32ba97dd1069f54a2aaac4a5fba972c0530

tech-stack:
  added: []
  patterns:
    - "Closing observation keyed on the GitHub Date header, never the local clock, with a control query that proves the created> filter can match"
    - "A blocking-human precondition that a reply alone cannot satisfy: the continuation re-measured the Date header and re-read E-02 itself"

key-files:
  created:
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-05-SUMMARY.md
  modified:
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md

key-decisions:
  - "E-13 cutoff_u is 2026-10-16T09:38:23Z: E-02 says ops01_dates_agree: no only because the first answer stated no date, and the user's resolution confirmed the API instant, so the earlier-of rule gives the same instant as E-01"
  - "The user's reply 'continue' and the orchestrator's own Date header reading did not stand in for step 0; the continuation ran its own curl -sSI and its own E-02 read before writing E-13"

patterns-established:
  - "Wait-for-window tasks stop with no evidence write and no commit, then re-run step 0 unchanged on continuation"

requirements-completed: [DEPS-01, OPS-01]

coverage:
  - id: D1
    description: "Phase close gate at the tip: the same anchors gate Phases 22-24 run exits 0 with an empty stderr and anchor_exempt=1 result=PASS; the self-test, testUnit and spotless baseline-equal are green; scope is exactly Phase 21's files; nothing was pushed"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-05-PLAN.md Task 1 verify commands CLOSE-GATE-GREEN and SCOPE-AND-NO-PUSH-OK, recorded in 21-EVIDENCE.md E-12 (cfa9fba3)"
        status: pass
    human_judgment: false
  - id: D2
    description: "OPS-01 closing observation: at 2026-10-18T17:11:13Z by GitHub's clock integrationTests.yaml is still disabled_manually with E-01's updated_at, zero schedule runs of it and zero repo-wide schedule runs were created after 2026-10-16T09:38:23Z, the control query returns 2, and the file is byte-identical to v1.7.0"
    requirement: OPS-01
    verification:
      - kind: other
        ref: "21-05-PLAN.md Task 2 verify command OPS01-OBSERVED, recorded in 21-EVIDENCE.md E-13 (7c06f32b)"
        status: pass
    human_judgment: false

duration: 32h 4m elapsed wall-clock (about 10 min of work; the rest is the D-11 wait and the checkpoint)
completed: 2026-10-18
status: complete
---

# Phase 21 Plan 05: Phase Close Gate and OPS-01 Closing Observation Summary

**Phase 21 closed on measured facts: the anchors gate passes at the tip with `anchor_exempt=1 result=PASS` (E-12), and GitHub's REST API shows the nightly `integrationTests.yaml` still `disabled_manually` with no scheduled run created after the disable, observed on GitHub's own clock after the D-11 window (E-13).**

## Performance

- **Duration:** 32h 4m elapsed wall-clock. Task 1 ran 2026-10-17T09:12Z to 09:20Z; Task 2 stopped on its precondition at 09:20Z and ran in this continuation at 2026-10-18T17:10Z to 17:12Z.
- **Started:** 2026-10-17T09:12:00Z
- **Completed:** 2026-10-18T17:13:00Z
- **Tasks:** 2
- **Files modified:** 1 (21-EVIDENCE.md), plus this SUMMARY

## Accomplishments

- E-12: at the tip `4ba6ce23`, the anchors gate exits 0 with an empty stderr and ends `anchor_exempt=1 result=PASS`. The self-test has 16 PASS lines and ends `selftest: no failures`. `testUnit --rerun` reports `Test result: SUCCESS`, 577 tests, 0 failed. The spotless diff is md5 `3393873370f9b48d2239131eedba8ff8` over 46 files, equal to E-07. Outside `.planning/`, exactly four paths changed since the merge parent. Nothing is pushed and `origin/custom_build` is still `6473e24f`.
- E-13: Task 2 step 0 was re-run from scratch. My own `curl -sSI` Date header read `Sun, 18 Oct 2026 17:10:23 GMT` and E-02 held the user's verbatim disable-instant resolution with `ops01_disable_instant_resolved: 2026-10-16T09:38:23Z`. The observation then showed state `disabled_manually`, `updated_at` unchanged from E-01, 0 workflow schedule runs and 0 repo-wide schedule runs after `cutoff_u`, control count 2, and `integrationTests.yaml` identical to v1.7.0. Verdict `pass`.
- The OPS01-OBSERVED verify command passes against the committed E-13, including its re-derivation of `cutoff_u` from E-01 and E-02.

## Task Commits

1. **Task 1: Phase close gate at the tip, recorded as E-12** - `cfa9fba3` (docs)
2. **Task 2: OPS-01 closing observation after the D-11 window, recorded as E-13** - `7c06f32b` (docs)

**Plan metadata:** the commit that adds this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md (docs: complete plan).

## Files Created/Modified

- `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` - E-12 and E-13 appended, index rows flipped to pass
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-05-SUMMARY.md` - this file

## Decisions Made

- `cutoff_u` is `2026-10-16T09:38:23Z`. E-02 records `ops01_dates_agree: no` because the first answer named no date, not because the dates conflicted. The user's resolution confirmed the API instant, so the earlier-of-U-and-resolved rule gives the same instant as E-01.
- The reply "continue" and the orchestrator's reading of the Date header did not satisfy the precondition. This continuation took its own Date header and its own E-02 read before any write.

## Deviations from Plan

**1. [Rule 3 - Blocking] Plan commit ledger missing for 21-05**
- **Found during:** SUMMARY write
- **Issue:** `.git/gsd-plan-head-before-21-05` did not exist, because Task 1's executor did not write the ledger before its first commit.
- **Fix:** Wrote the ledger with `cfa9fba3^` (`4ba6ce23`, the HEAD immediately before Task 1's commit, confirmed by `git rev-parse`). `git rev-list --count 4ba6ce23..HEAD` measured 2 at SUMMARY write, matching the two task commits.
- **Files modified:** none tracked (the ledger lives in the git dir)
- **Verification:** the measured count equals the two task commits listed above.

---

**Total deviations:** 1 auto-fixed (1 blocking bookkeeping gap)
**Impact on plan:** None on the evidence. The plan ran as written; the 21-05 plan was split across two executors by the designed D-11 wait.

## Issues Encountered

- The first Task 2 attempt stopped at step 0 with GitHub's Date header at `Sat, 17 Oct 2026 09:20:39 GMT`, before the window. That is the designed `blocking-human` wait, with no evidence write and no commit. The window had opened by the time of this continuation.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 21 can be verified and closed. All five plans have summaries, E-01 through E-13 are recorded, and OPS-02 stays `unmeasured` in E-01/E-02 because the user reported no gradle "Last checked" time. That is a Phase 21 verification question, not a plan 21-05 gap.
- Phase 22 starts from a merged, gated tree. Phase 24 re-checks OPS-01 in its pre-gate bundle.
- Nothing is pushed. `custom_build` was not checked out or modified, and `.planning/state.json` and `.planning/milestone.lock` were left unstaged.

## Self-Check: PASSED

- E-12 and E-13 present in 21-EVIDENCE.md: `grep -c '^### E-1[23]:'` is 2.
- Commits `cfa9fba3` and `7c06f32b` are ancestors of HEAD.
- Both Task 1 and Task 2 verify commands were run, each printing its marker (`CLOSE-GATE-GREEN` and `SCOPE-AND-NO-PUSH-OK` by the earlier executor, `OPS01-OBSERVED` by this one).

---
*Phase: 21-dependabot-integration-ci-hygiene*
*Completed: 2026-10-18*
