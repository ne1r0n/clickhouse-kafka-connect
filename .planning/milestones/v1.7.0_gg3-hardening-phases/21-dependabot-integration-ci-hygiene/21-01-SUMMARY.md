---
phase: 21-dependabot-integration-ci-hygiene
plan: "01"
subsystem: ops
tags: [github-actions, dependabot, keychain, evidence, operator-checks]

requires: []
provides:
  - "21-EVIDENCE.md with header, the E-01..E-13 index, E-01 (OPS facts at phase start) and E-02 (the user's OPS answers verbatim)"
  - "E-01 workflow_updated_at 2026-10-16T09:38:23Z and E-02 ops01_disable_instant_resolved 2026-10-16T09:38:23Z: the inputs of plan 21-05's cutoff_u"
affects: [21-05 closing OPS-01 observation (E-13), Phase 23 DEBT-12 (17-REVIEW WR-01 residue), Phase 24 pre-gate OPS-01 re-check]

actuals:
  tasks: 3
  commits: 2

plan_head_before: 607c24d67cbb08ed47a956118a23d92571c94f16
plan_head_after: 7897a39b9bbb5286adcea8ceb4d91672712d4a73

tech-stack:
  added: []
  patterns:
    - "Operator answers recorded verbatim under 'given directly to the orchestrator' headers; a same-session follow-up gets its own labelled header so each check keeps exactly one first answer"

key-files:
  created:
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md
  modified: []

key-decisions:
  - "OPS-01: the first answer ('1. disabled') gave no date, so ops01_dates_agree is no and the user's same-session resolution ('OPS-01: time is right') sets ops01_disable_instant_resolved to the API instant 2026-10-16T09:38:23Z; cutoff_u for E-13 is therefore that instant"
  - "OPS-02 recorded as unmeasured, not pass: neither the answer ('2. dependabot works') nor the two screenshots show a 'Last checked' time for the gradle entry; the Dependabot tab shows an open-PR-limit banner (limit 5), not a configuration error"
  - "OPS-03 pass: the user says the token was deleted, and the Keychain lookup exits 44 both at phase start and after the answer"

patterns-established:
  - "Screenshot content is described only under an 'orchestrator annotation (not the user's words)' line, with inferences marked as such"

requirements-completed: [OPS-03]

coverage:
  - id: D1
    description: "E-01 holds the ten REST, GitHub-clock and Keychain facts with a D-11 window-aware verdict (unmeasured before 2026-10-17T12:00:00Z)"
    requirement: OPS-01
    verification:
      - kind: other
        ref: "21-01 Task 1 automated verify (E01-RECORDED), re-run by the orchestrator"
        status: pass
    human_judgment: false
  - id: D2
    description: "E-02 holds the user's three answers verbatim, the disable-instant resolution, the date agree line and the three per-check verdicts"
    requirement: OPS-01
    verification:
      - kind: other
        ref: "21-01 Task 3 automated verify (E02-RECORDED)"
        status: pass
    human_judgment: true
  - id: D3
    description: "OPS-02 gradle entry shows a recent Last checked time with no configuration error"
    requirement: OPS-02
    verification:
      - kind: manual
        ref: "21-EVIDENCE.md E-02 (user answer and screenshots)"
        status: unmeasured
    human_judgment: true
  - id: D4
    description: "OPS-03 token gone on GitHub (user's words) and the Keychain item gone (exit 44)"
    requirement: OPS-03
    verification:
      - kind: manual
        ref: "21-EVIDENCE.md E-01 keychain_lookup_exit and E-02 keychain_lookup_exit_after"
        status: pass
    human_judgment: true

duration: 65min
completed: 2026-10-17
status: complete
---

# Phase 21 Plan 01: OPS operator checks at phase start Summary

**`21-EVIDENCE.md` opens with E-01, the public-API view of the nightly workflow, Dependabot and the Keychain. E-02 follows with the user's own answers to OPS-01..OPS-03. OPS-03 passes, the OPS-01 disable instant is resolved for plan 21-05, and OPS-02 is unmeasured because no gradle "Last checked" time was reported.**

## Performance
- **Duration:** about 65 min, most of it waiting for the operator answers
- **Started:** 2026-10-17T07:33:50Z (executor dispatch)
- **Completed:** 2026-10-17T08:38:00Z
- **Tasks:** 3 (tracer by the executor, blocking-human checkpoint, Task 3 inline by the orchestrator)
- **Files modified:** 1 created; nothing under `src/`, `build.gradle.kts`, `gradle/` or `.github/`

## Accomplishments
- E-01 (GitHub clock `Sat, 17 Oct 2026 07:34:27 GMT`): `integrationTests.yaml` (id 384641581) is `disabled_manually` since `2026-10-16T09:38:23Z`. Zero schedule runs of it and zero repo-wide schedule runs were created after that instant, against a control count of 2 since 2026-10-01. The newest Dependabot gradle job is `gradle in /. - Update #1627525145` (2026-10-14T21:05:35Z, success), and the Keychain lookup exits 44. The nightly file is byte-identical to `v1.7.0` on HEAD and on `origin/custom_build`. Verdict `unmeasured`, because the D-11 window opens at 12:00 UTC.
- E-02: the three answers are recorded verbatim with their UTC times, along with one same-session follow-up (the OPS-01 resolution and the OPS-02 screenshots). Values: `ops01_dates_agree: no` (no date in the first answer), `ops01_disable_instant_resolved: 2026-10-16T09:38:23Z`, `keychain_lookup_exit_after: 44`, and per-check `OPS-01 disable recorded: pass`, `OPS-02: unmeasured (...)`, `OPS-03: pass`. Overall verdict `unmeasured`.
- Side finding from the OPS-02 screenshot: the Dependabot tab shows "Dependabot cannot open any more pull requests. The open pull request limit has been exceeded. The current limit is: 5." The navigation shows 1 open PR, so the banner probably dates from the 2026-10-14 jobs, when PRs #1..#5 were open. That is an inference, not an observation.

## Task Commits
1. **Task 1 (tracer): OPS-01 observation path, E-01** - `07a7312a` (docs)
2. **Task 2: operator checks** - checkpoint, no commit. The user answered directly in this session.
3. **Task 3 (orchestrator inline): E-02 verbatim answers** - `7897a39b` (docs)

**Plan metadata:** recorded in the docs commit that carries this summary.

## Files Created/Modified
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` - header, the 13-row index, E-01 and E-02. Rows E-03..E-13 stay `pending (plan 21-NN)`.

## Decisions Made
- Asked one same-session follow-up instead of recording two incomplete answers as final. The follow-up was plan Task 3 step 5's disable-instant question for OPS-01 and the "Last checked" question for OPS-02. The first answers stay unedited under their own headers.
- Recorded `ops01_dates_agree: no` with a verbatim resolution, rather than `yes`. The user's first answer stated no date, and the date came only from the follow-up confirming the API value. The cutoff for E-13 is the same either way.

## Deviations from Plan

**1. [Plan text gap] Follow-up headers for OPS-02**
- **Found during:** Task 3
- **Issue:** the plan defines a follow-up header only for the OPS-01 disable-instant resolution, and its verify counts exactly one `OPS-0N answer (...)` header per check.
- **Fix:** the OPS-02 follow-up goes under `OPS-02 follow-up answer (verbatim, given directly to the orchestrator, ...)`, which the verify does not count. The screenshots are described on an `orchestrator annotation (not the user's words):` line.
- **Committed in:** `7897a39b`

**2. [Orchestration] Executor execution context passed as files to Read, not inlined**
- The executor prompt listed `execute-plan.md`, `summary.md`, `checkpoints.md`, `worktree-path-safety.md` and `executor-examples.md` for the executor to Read at start, instead of inlining them (about 130 KB). The executor read them and followed the checkpoint protocol.

**Total deviations:** 2 (none changes scope).

## Issues Encountered
- OPS-02 cannot pass on the evidence given. The current Dependabot tab may not show a per-manifest "Last checked" field at all (research assumption A2), so the requirement wording may need the user's judgment at verification, or a new reading.

## User Setup Required
None.

## Next Phase Readiness
- Plan 21-05 Task 2 precondition (E-02 half) is met: `ops01_dates_agree: no` with a verbatim resolution and a UTC `ops01_disable_instant_resolved`. The other half, GitHub's clock at or after 2026-10-17T12:00:00Z, is not yet met at 08:38 UTC.
- OPS-02 needs a decision before the phase can pass on ROADMAP criterion 5: either a "Last checked" reading, or the user's acceptance of the screenshots and the successful 2026-10-14 gradle job as enough.

## Known Stubs
None.

## Threat Flags
None. No setting was changed by an agent, no authenticated call or `gh` was used, the Keychain was queried for its exit status only, and no token shape appears in the evidence file.

---
*Phase: 21-dependabot-integration-ci-hygiene*
*Completed: 2026-10-17*

## Self-Check: PASSED
- Created file exists: `21-EVIDENCE.md` (index, E-01, E-02 present).
- Commits `07a7312a` and `7897a39b` are ancestors of HEAD.
- Task verifies: `E01-RECORDED` (executor run and orchestrator re-run) and `E02-RECORDED` printed.
