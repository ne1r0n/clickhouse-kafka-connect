---
phase: 17-ci-build-test-hygiene
plan: "04"
subsystem: infra
tags: [github-actions, ci, runner-image, token-permissions, dependabot, default-branch]

requires:
  - phase: 17-ci-build-test-hygiene
    provides: plan 17-03 pushed SHA X=0c90d331 and the green run 38172811430 read through the API (E-10)
provides:
  - E-11, the CI-17 web-UI-only evidence from the run's own log text (runner image ubuntu-24.04, token permissions Contents/Metadata read, four pinned action downloads, job summary)
  - STATE.md Blockers [CI] lines recording the CI-17 outcome and the main-default-branch finding (F1)
  - ROADMAP.md Phase 20 operator checks for Dependabot enablement and the default branch (D-05, F1), with the unobservable test-head nightly premise removed from the Phase 17 note, Phase 20 note and Structure Notes
affects: [phase-20-release]

actuals:
  tokens: 3100
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Evidence sourced from the job log through a user-created read-only token instead of a web-UI paste, recorded as a deviation with file hashes"

key-files:
  created: []
  modified:
    - .planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md
    - .planning/STATE.md
    - .planning/ROADMAP.md

key-decisions:
  - "Task 1's paste was replaced by an API download of the job log, using a fine-grained read-only token (one repository, Actions: Read-only) that the user created and stored in the macOS Keychain; the orchestrator fetched the log, this executor never touched the token"
  - "The default branch stays undecided: the user stated no intent (E-10), so Phase 20's operator checks list it as a manual check and no repository setting was changed"
  - "Phase 20 carries the manual checks for Dependabot enablement and the default branch; the test-head nightly check is dropped because CI-16 was proven statically and main never ran the file on a custom_build schedule"

requirements-completed: [CI-17]

coverage:
  - id: D1
    description: "CI-17 web-UI items: the run's own Set up job text shows Image ubuntu-24.04 (version 20261004.327.1), a GITHUB_TOKEN Permissions block of exactly Contents: read and Metadata: read, and Download action repository lines naming all four SHAs pinned in tests.yaml; the job summary shows Gradle maxParallelForks=2 and the Coverage Summary"
    requirement: "CI-17"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-11 (E-11-OK, python3 -I content check on /tmp/gsd17/exec/setup-job.txt and job-summary.txt)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The default-branch finding F1 and the D-05 Dependabot check are carried into Phase 20's operator checks; STATE and ROADMAP no longer say test-head.yaml runs nightly from custom_build"
    requirement: "CI-17"
    verification:
      - kind: other
        ref: "RECORDS-OK (python3 -I content check on STATE.md and ROADMAP.md)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Local tail only: nothing pushed, origin heads exactly custom_build and main, tags byte-identical to the 17-03 before-snapshot, the plan commit absent from origin, the three files committed"
    requirement: "CI-17"
    verification:
      - kind: other
        ref: "LOCAL-TAIL-OK (git ls-remote --heads/--tags comparison)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The log-sourced stand-in for the user's paste (raw API log with workflow-command markers normalized, summary reconstructed from the log) is accepted as equivalent to the web-UI paste D-13 asked for"
    requirement: "CI-17"
    verification: []
    human_judgment: true
    rationale: "Provenance differs from the plan's mechanism (API download, not a web-UI paste, and a reconstructed rather than rendered Summary page); the user and verifier decide whether it satisfies D-13. The quoted lines are the log's own text either way."

duration: 9min
completed: 2026-10-12
status: complete
plan_head_before: 7f4a443854af783150f7edd854b2087b8647f696
plan_head_after: 640488956f28e4be59c6da9fe1472892470d858f
---

# Phase 17 Plan 04: CI-17 web-UI evidence and the default-branch finding Summary

**E-11 pins the CI-17 run's runner image (`ubuntu-24.04`, 20261004.327.1), a token holding only `Contents: read` / `Metadata: read`, and all four SHA-pinned action downloads from the job's own log text, and STATE/ROADMAP now hand Phase 20 an accurate operator checklist (Dependabot enablement, default branch `main`) instead of an unobservable nightly check.**

## Performance

- **Duration:** about 9 min (2026-10-12 08:05Z to 08:14Z)
- **Tasks:** 2 (Task 1 human-action, resolved by the orchestrator before dispatch; Task 2 auto, run here)
- **Files modified:** 3 (`17-EVIDENCE.md`, `STATE.md`, `ROADMAP.md`)

## Accomplishments

- **E-11, verdict pass.** Run 38172811430, job 114568815601, X=`0c90d331`. Quoted verbatim from the log: `Image: ubuntu-24.04` with `Version: 20261004.327.1`; the GITHUB_TOKEN Permissions block, exactly `Contents: read` and `Metadata: read` (assumption A2 confirmed); `Download action repository` lines for `actions/checkout@3d3c42e5…`, `actions/setup-java@de7274f0…`, `gradle/actions@3f5f9ada…` and `actions/upload-artifact@cf430e03…`, each equal to a SHA pinned in `tests.yaml`; the summary lines `runner nproc=4, Gradle maxParallelForks=2 …` and the Coverage Summary (full suite instruction 84.7%, line 84.5%, branch 75.4%; fork-changed classes line 87.3%, branch 80.8%). Index row added.
- **STATE.md, two `[CI]` lines replaced.** The `ubuntu-latest` line now records Phase 17's `ubuntu-24.04` pin, SHA-pinned Node 24 actions, read-only token and `persist-credentials: false`, and CI-17 run 38172811430 on `0c90d331` green; the one-branch-per-SHA rule is kept. The `test-head.yaml` line now says the file is deleted (CI-16, static proof), the fork's default branch is `main` so schedules and Dependabot read `main`, the user's intent is undecided, and Phase 20 carries the manual checks.
- **ROADMAP.md, three sentences replaced** (before and after below). Diff: 7 added and 3 deleted lines in ROADMAP.md, 2 and 2 in STATE.md, inside the plan's limits of 3 and 2 deletions.
- **Nothing pushed.** `LOCAL-TAIL-OK`: origin heads are exactly `custom_build` and `main`, `git ls-remote --tags origin` is byte-identical to `/tmp/gsd17/exec/origin-tags-before.txt`, the plan commit is not on origin.

### Sentences replaced

| File | Before | After |
|------|--------|-------|
| ROADMAP Phase 17 note | `test-head.yaml` runs on a nightly schedule from the default branch, `custom_build`. The effect of CI-16 on scheduled runs is therefore only observable after Phase 20 publishes it. This phase proves the fix statically. | `test-head.yaml` is deleted (D-01) and CI-16 is proven statically. The fork's default branch is `main`, not `custom_build` (research F1), so no schedule on `custom_build` ever ran it and publishing the deletion changes no observable schedule. |
| ROADMAP Phase 20 note | After publication, check once that `test-head.yaml`'s nightly schedule no longer fails (CI-16, landed in Phase 17). | Manual checks for the user, by hand, after publication (D-05, F1): whether Dependabot version updates are enabled in the fork's settings; which branch is the GitHub default (`main` at Phase 17, intent undecided, E-10; Dependabot and scheduled workflows read only the default branch); no `test-head.yaml` nightly run exists to check. No agent changes repository settings. |
| ROADMAP Structure Notes (CI-17 bullet) | CI-16 is the exception: its effect on the nightly schedule shows only once the file reaches the default branch, in Phase 20. | CI-16 is proven statically in Phase 17; the fork's default branch is `main`, so no schedule observes it (F1). |
| STATE Blockers `[CI]` (runner) | ⏰ `ubuntu-latest` moves to Ubuntu 26 from **2026-10-19** … Node 20 deprecation warnings affect `checkout@v4` and `upload-artifact@v4` (Phase 17). | ✓ 2026-10-12: Phase 17 pinned `tests.yaml` to `ubuntu-24.04` … CI-17 run 38172811430 on `0c90d331` was green … the one-branch-per-SHA rule still holds. |
| STATE Blockers `[CI]` (test-head) | `test-head.yaml` calls `tests.yaml` as a reusable workflow … Its nightly schedule runs from the default branch `custom_build` and very likely fails every night … | `test-head.yaml` is deleted (CI-16, proven statically in Phase 17, E-04). The fork's GitHub default branch is `main` … carried as undecided. Phase 20 carries the manual checks. |

## Task Commits

1. **Task 1 (paste of Set up job and job summary):** no commit; resolved by the orchestrator before dispatch (see Deviations)
2. **Task 2 (E-11, STATE, ROADMAP):** `64048895` (docs) - `docs(17-04): record the CI-17 web-UI evidence and carry the default-branch finding to Phase 20 (CI-17, D-05)`

**Plan metadata:** the SUMMARY/STATE/ROADMAP/REQUIREMENTS close-out commit follows this file; it is a local tail that Phase 20 publishes.

## Files Created/Modified

- `.planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md` - E-11 entry and Index row
- `.planning/STATE.md` - the two `[CI]` Blockers lines
- `.planning/ROADMAP.md` - Phase 17 note, Phase 20 operator checks, Structure Notes bullet

## Decisions Made

- The log, not a paste, is the source of E-11 (below); E-11 says so explicitly and records every file hash, so a reader can re-derive the quoted lines.
- F1 stays undecided and Phase 20 owns the two manual checks; no GitHub setting was touched (D-05).

## Deviations from Plan

**1. [Task 1 mechanism - D-13] API download instead of a web-UI paste**
- **Found during:** Task 1 (resolved by the orchestrator before this executor started)
- **Issue:** Job logs return 403 without a token, so the plan asked the user to paste the "Set up job" section and the job summary.
- **What happened:** At the user's own request to the orchestrator, the user created a fine-grained read-only token (single repository, Actions: Read-only) and stored it in the macOS Keychain. The orchestrator downloaded the log (`GET /repos/ne1r0n/clickhouse-kafka-connect/actions/jobs/114568815601/logs`, HTTP 200, 2817 lines). The token was never printed, written to a file or committed, and this executor did not use it.
- **Evidence integrity:** the same evidence, verbatim from the API. `job-114568815601.log` sha256 `8e25a2e5…0eb6`; `setup-job.raw.txt` (log lines 1-35) sha256 `a1660e95…cdd7`; `job-summary.txt` sha256 `3190c504…2262`. Not a copy of the rendered Summary page: there is no REST endpoint for it, so the summary was reconstructed from the `### Test fork count` heading and message the step writes (step script in the log) plus the `## Coverage Summary` block printed by `jacoco_summary.py`. E-11 states this.
- **Impact:** same four facts, observed from the run's own log text. Flagged as coverage item D4 for the verifier because provenance differs from the plan's wording.

**2. [Rule 3 - Blocking] Raw-log workflow-command markers normalized for the plan's checker**
- **Found during:** Task 2, verify block 1
- **Issue:** The raw API log wraps blocks in `##[group]<title>` and `##[endgroup]` lines, which the web UI does not show. The checker looks for a bare `GITHUB_TOKEN Permissions` heading, so on the raw file it found no heading and the permissions set came out empty. Assumption A9 says to stop rather than loosen the check.
- **Fix:** The check was not loosened. The verbatim file was kept as `/tmp/gsd17/exec/setup-job.raw.txt` (sha256 unchanged) and `/tmp/gsd17/exec/setup-job.txt` was regenerated from it with a deterministic `sed` that deletes `##[endgroup]` lines and drops the `##[group]` prefix, keeping every timestamp and every other character (new sha256 `60797bdd…f721`; `diff` shows only the 4 headings and 4 end markers). E-11 documents the transformation.
- **Files modified:** scratch only (`/tmp/gsd17/exec/`), nothing tracked
- **Verification:** `E-11-OK` printed with the permissions set exactly `{Contents: read, Metadata: read}`.

**3. [Rule 1 - Bug in own edit] E-11 first draft split by its own quoted heading**
- **Found during:** Task 2, verify block 1 (`recorded False`)
- **Issue:** The verbatim summary heading `### Test fork count` at line start inside E-11's code fence made the plan's section scan end E-11 early, so `Verdict: pass` fell outside the section.
- **Fix:** Quote the heading inline in prose. The other summary lines stay verbatim.
- **Files modified:** `17-EVIDENCE.md` (before the task commit)

---

**Total deviations:** 3 (1 evidence-mechanism change decided upstream, 1 blocking-format fix, 1 self-inflicted edit fix)
**Impact on plan:** None on scope or outcome. All three are documented, no check was loosened and no tracked file differs from the plan's intent.

## Authentication Gates

The Task 1 gate was a human-action checkpoint; it was satisfied by the user creating a read-only token, as described above. Recorded as normal flow.

## Issues Encountered

None beyond Deviations 2 and 3.

## Known Stubs

None.

## Threat Flags

None for the repository: no file, endpoint or auth path was added. For the record, a read-only GitHub token now exists in the user's macOS Keychain (outside the repository; the user may revoke it once Phase 17 is closed).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 17's four plans have SUMMARYs; criterion 3 is evidenced (E-10 plus E-11).
- Phase 20's operator checks now carry Dependabot enablement and the default-branch decision as manual checks; the default-branch intent is still undecided.

---
*Phase: 17-ci-build-test-hygiene*
*Completed: 2026-10-12*

## Self-Check: PASSED

Found: 17-EVIDENCE.md (E-11 present), 17-04-SUMMARY.md, the three /tmp/gsd17/exec evidence files; commit 64048895 is an ancestor of HEAD; E-11-OK, RECORDS-OK and LOCAL-TAIL-OK printed; measured commits 1 (ledger base 7f4a4438).
