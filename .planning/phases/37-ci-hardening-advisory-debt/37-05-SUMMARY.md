---
phase: 37-ci-hardening-advisory-debt
plan: "05"
subsystem: ci
tags: [advisory-ledger, release-record, phase-gate, junit, fork-anchors]

requires:
  - phase: 37-ci-hardening-advisory-debt
    provides: "plans 37-01 to 37-04: the bump script, the compare chain, the release guard, the carried review fixes, E-01 to E-15, anchors rows=172"
provides:
  - "37-ADVISORY-LEDGER.md: 29 findings in 36 rows, 15 fixed, 3 closed, 18 dispositioned, every pointer resolving, every disposition with an owner and a re-open trigger (E-16)"
  - "37-RELEASE-RECORD.md: the real v1.7.0_gg5 release as measured, the stated deviation from the ROADMAP wording of criterion 5, the carried items and six handoffs to Phase 38 (E-17)"
  - "the Phase 37 gate on one head: testUnit with Docker unreachable, the full suite on latest with client V2, anchors mirror, tail merge gate, scope facts, decision go (E-18)"
affects: [38-release]

plan_head_before: 7f2b8268d8cc6c4a85f0158963d72913ef663069
plan_head_after: 31d0fff6cdfb80b0130a52173660246e63fdc7f1

actuals:
  tokens: 10517
  tasks: 3
  commits: 5

tech-stack:
  added: []
  patterns:
    - "commit the ledger first, then the evidence entry that names its commit, because an entry cannot name the commit that carries it"
    - "check scratch refs on origin by the plan's own prefix (scratch/cih-04-*), not by scratch/* (two user-owned refs from Phase 36 exist)"

key-files:
  created:
    - .planning/phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md
    - .planning/phases/37-ci-hardening-advisory-debt/37-RELEASE-RECORD.md
  modified:
    - .planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md
    - .planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md

key-decisions:
  - "The four src/main rows (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01) and the null assertion of 28 IN-05 stay dispositions with owner the user: Phase 37 changes src/main only with the user's approval, which has not been given"
  - "ADV-03 is recorded as measured: no workflow run created v1.7.0_gg5, and the caveat that no run exercised the publish path stays open until Phase 38; the ROADMAP wording of criterion 5 is quoted and not edited"
  - "The increment-version.yml header comment (it names ReleaseWorkflowGuardUnitTest, the pinning class is IncrementVersionWorkflowGuardUnitTest) is an open item owned by the user with trigger 'next edit of increment-version.yml'; Phase 38 may not edit .github/"

requirements-completed: [ADV-01, ADV-02, ADV-03]

coverage:
  - id: D1
    description: "Every Phase 30 row, every Phase 31 row and each of the 16 Phase 29 close-record findings is fixed with a resolving commit pointer or dispositioned with an owner and a re-open trigger; the four src/main rows and the 28 IN-05 null assertion are dispositions; no existing assertion deleted or weakened"
    requirement: "ADV-01, ADV-02"
    verification:
      - kind: unit
        ref: "python3 -I 37-ledger-check.py check 37-ADVISORY-LEDGER.md (OK ledger: 36 rows, 0 problems); P37-05-T1-OK; 37-COMMAND-EVIDENCE.md E-16"
        status: pass
    human_judgment: false
  - id: D2
    description: "The real v1.7.0_gg5 release is on the record from the REST API (release, tag commit, asset name, size, SHA-256, the three release.yaml runs); the record states that no workflow run created it and that the publish-path caveat stays open until Phase 38"
    requirement: "ADV-03"
    verification:
      - kind: other
        ref: "P37-05-T2-OK (the record repeats the E-08 values and contains the finding keys, the deviation heading and sentence; the phrases that would close the caveat count 0); 37-COMMAND-EVIDENCE.md E-17"
        status: pass
    human_judgment: true
    rationale: "Whether the ROADMAP wording of criterion 5 (the creating run, the caveat closed) is reworded to what the facts allow, or the criterion is accepted as met with the stated deviation, is the orchestrator's and the user's decision"
  - id: D3
    description: "Phase gate on one head: Docker-unreachable testUnit 1633 tests (baseline 1475 plus 158 measured additions), full suite on latest with client V2 2231 tests with 18 named skips and Docker-only count 598 as in Phase 36, anchors rows=172 bad=0, tail gate result=PASS, empty src/main diff, decision go"
    requirement: "ADV-01, ADV-02, ADV-03"
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --rerun (exit 0); P37-05-T3-OK; P37-05-T3-TESTS-OK; 37-COMMAND-EVIDENCE.md E-18"
        status: pass
    human_judgment: false
  - id: D4
    description: "The optional CI dry run of the release workflow (E-12) is a user-gated handoff, expected RED, never counted as a pass"
    requirement: "CIH-04"
    verification: []
    human_judgment: true
    rationale: "It needs a push to origin and a workflow dispatch by the user; no agent may push or dispatch"

duration: 15min
completed: 2026-10-31
status: complete
---

# Phase 37 Plan 05: Advisory ledger, release record and the phase gate Summary

**The 29 carried review findings are closed in a 36-row ledger (15 fixed and 3 closed with resolving commits, 18 dispositioned with an owner and a re-open trigger, the four `src/main` rows left as dispositions), the real `v1.7.0_gg5` release is recorded as measured with the statement that no workflow run created it and the publish-path caveat stays open until Phase 38, and the phase gate is green on one head: testUnit with Docker unreachable 1633 tests, full suite on ClickHouse `latest` with client V2 2231 tests with 18 named skips, anchors `rows=172 bad=0`, tail gate `result=PASS`, empty `src/main` diff, decision go.**

## Performance

- **Duration:** about 15 min wall clock (full suite 2 min 43 s)
- **Started:** 2026-10-31T09:40Z (approximate, first command)
- **Completed:** 2026-10-31T09:57Z
- **Tasks:** 3 (all auto)
- **Files modified:** 4 in the diff (2 created, 2 modified), all under `.planning`

## Gate

- `gated_head`: `b2b2d265db1437706ab95c8c3fbecc786e052352` (HEAD when the gate started; the gate evidence commit `31d0fff6` and this SUMMARY are `.planning`-only)
- `last_non_planning_commit` (the gated SHA that `37-USER-DISPATCH-DRYRUN.md` names): `1c583b46249ab9bd30ff717c256c7f583c4f0ed9`. Superseded after the code review: the four review-fix commits changed files outside `.planning`, and the handoff named `27f9ff892eb6d4208a4f4e90ae204d6996fa72de` (see `37-REVIEW-FIX.md`). Superseded again by the two security-fix commits of UF-37-1 and UF-37-2 (`37-SECURITY.md`): the handoff now names `e1e05604fba53aaa2aa1b33dc66ec33624343dbe`. Plan 37-05 made no commit outside `.planning`, so this is the last commit of plan 37-04, and the handoff's check `git diff --quiet <gated sha> "$SHA" -- . ':(exclude).planning'` holds for any later `.planning`-only tip. The handoff's commit and file figures were stale (198 commits, 75 files); a separate commit refreshed them (208 commits up to the gated sha, 85 files, 7501 insertions, 607 deletions against the published tip).
- `gate_decision`: **go**, `gate_failures`: none.
- Cache-busted `testUnit` with `DOCKER_HOST` unreachable: exit 0, 1633 tests, 0 failures, 0 errors, 2 skipped; the E-01 baseline of 1475 plus 158 measured additions (E-04 64 plus E-15 94) equals it; all 13 tallies of the classes this phase wrote or extended are green (48, 16, 27, 19, 34, 44, 21, 13, 11, 7, 3, 51, 2).
- Full suite on ClickHouse `latest` with client V2: exit 0, 2231 tests, 0 failures, 0 errors, 18 skipped (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`), 2 min 43 s; Docker-only count 2231 minus 1633 is 598, equal to Phase 36's 2072 minus 1474. No OpenJ9 crash file appeared, so no re-run and no `-PjavaVersion=21` run.
- `SELECT version()`: `26.10.3.7`; digest `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (same as E-01, no drift).
- Anchors mirror `rows=172 bad=0`; tail merge gate `invariants: provenance_paths=27 anchor_commits=145 anchor_rows=172 marker_commits=152 anchor_exempt=0 result=PASS`; `src/main`, build files, `VERSION`, `CHANGELOG.md`, `tests.yaml`, the nightly and `integrationTests.yaml` unchanged since the phase start; the 24 paths changed outside `.planning` are all on the allowed list; cumulative assertion diff clean; hygiene scan over 32 files and the extra pattern scan clean; nothing pushed, no mutating `gh` call, one worktree.

## Accomplishments

- E-16: `37-ADVISORY-LEDGER.md` holds 36 rows for the 29 findings (7 of Phase 30, 6 of Phase 31, 16 of the Phase 29 close record). 15 fixed and 3 closed, each with a pointer that resolves and lies in the phase's history (25 IN-04 d points at the Phase 30 commit `496de32a`); 18 dispositions each with an owner (the user, except 30 IN-04: Phase 38 RLS-05) and a re-open trigger. The four `src/main` rows, 28 IN-05's null assertion (owner the user: tightening it changes an assertion), 25 WR-02's classpath half, 28 IN-04's live half and 27 IN-02's stale class name are dispositions. The 25 WR-02 (a) and 28 IN-04 (a) rows read `fixed` because E-14 and E-15 say `committed`.
- E-17: `37-RELEASE-RECORD.md` repeats the E-08 values and states `created_by_workflow_run: no` and `publish_path_exercised: no`: the release and its asset were created by the user's own account, the only publish-mode run was cancelled before its publish step, and the later success was a dry run that deletes its own draft. It states the deviation from ROADMAP criterion 5 and that the caveat stays open until Phase 38 publishes through `release.yaml`. UF-36-4 (with the zstd VERSION row) and F-1 to F-3 are carried by disposition; six numbered handoffs go to Phase 38.
- E-18: the gate above, one head, decision go; E-12 (the CI dry run, expected RED) stays pending with the user.

## Task Commits

1. **Task 1: the ledger** - `d95f800a` docs (ledger_commit), `48d9695d` docs (E-16)
2. **Task 2: the release record** - `b2b2d265` docs (record and E-17); `32534877` docs (refresh of the dry-run handoff figures, see Deviations)
3. **Task 3: the gate** - `31d0fff6` docs (E-18)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan).

## Files Created/Modified

- `.planning/phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md` - new, 36 rows in three tables plus the carried-items pointer
- `.planning/phases/37-ci-hardening-advisory-debt/37-RELEASE-RECORD.md` - new, the ADV-03 record
- `.planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md` - entries E-16 to E-18, index rows flipped to pass (E-12 still pending)
- `.planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md` - two figures refreshed

## Decisions Made

- The four `src/main` rows and the null assertion are left as dispositions because the user has not approved a `src/main` change; the ledger and the SUMMARY say they need his approval if he wants them fixed.
- The release record states the deviation from ROADMAP criterion 5 instead of claiming it closed; the ROADMAP wording is not edited.
- The `increment-version.yml` header-comment item is recorded with an honest owner (the user), because plan 37-05 may not edit workflows and Phase 38 may not edit `.github/`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The ledger and its evidence are two commits, not one**
- **Found during:** Task 1.
- **Issue:** the plan commits the ledger and E-16 together, but E-16 must record `ledger_commit`, which resolves with `git cat-file`; an entry cannot name the commit that carries it.
- **Fix:** the ledger is committed first (`d95f800a`, which is `ledger_commit`) and the E-16 entry second (`48d9695d`), with the gates measured at the ledger commit. Message of the first commit as the plan names it.
- **Committed in:** `d95f800a`, `48d9695d`.

**2. [Rule 3 - Blocking] The `scratch/*` clauses of the Task 2 and Task 3 verifies check `scratch/cih-04-*`**
- **Found during:** Task 2 and Task 3 verify.
- **Issue:** `test -z "$(git ls-remote --heads origin 'scratch/*')"` fails because the user-owned refs `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` exist on origin (Phase 36; recorded in E-01).
- **Fix:** the clauses check `scratch/cih-04-*`, the prefix this plan's handoff would create; E-18 records `scratch_branch_on_origin: absent` and `scratch_refs_on_origin` equal to the E-01 set. The two refs were not touched.

**3. [Plan wording] The `37-USER-DISPATCH-DRYRUN.md` figures refreshed**
- **Found during:** Task 2 (the instruction to keep the handoff consistent with the gated SHA).
- **Issue:** the handoff still read 198 commits and 75 changed files (7218 insertions, 584 deletions) from plan 37-03; against the published tip the push carries 208 commits up to the gated sha and 85 files (7501 insertions, 607 deletions).
- **Fix:** two figures edited in a separate commit `32534877`. The plan lists only the two records and the evidence as written files, so this is outside its file list; it changes no instruction of the handoff, and the handoff's `<gated sha>` still resolves to `1c583b46` because this plan made no non-`.planning` commit.

**4. [Note] The handoff list stays at six items**
- The plan's check expects `handoff_items` 6. The open item on the `increment-version.yml` header comment is a separate section of the record and a trailing paragraph after the six numbered handoffs, and is carried into this SUMMARY's Handoffs as an additional item.

---

**Total deviations:** 2 blocking verify or method issues, 1 wording note outside the file list, 1 structure note. **Impact on plan:** none on scope or results; no `src/main`, test, workflow, build file, `VERSION`, `CHANGELOG.md` or origin change. Carry-overs (a) to (e) were applied: `tests>=N` is quoted (no stray `=N` file exists), evidence keys are lowercase `[a-z0-9_]+`, no new Javadoc text was written, path lists in verify commands are C-sorted, and the scratch ref clause checks only what the plan intends.

## Issues Encountered

- No OpenJ9 crash, core dump or javacore appeared in any run; the host swap was nearly full but both Gradle runs completed in one attempt.
- The first hygiene check of the ledger flagged one word of the scope paragraph; it was reworded before the commit.
- A built-in guard moved the 3-minute tail gate to the background; it finished with `result=PASS`.

## Known Stubs

None.

## Threat Flags

None. The plan writes two records and evidence, runs the gate and reads origin with plain `git ls-remote`; no endpoint, auth path or file access pattern was added.

## User Setup Required

None. The optional CI dry run needs a push and a dispatch by the user; see `37-USER-DISPATCH-DRYRUN.md`.

## Open for the user and the orchestrator

- **E-12 pending (user-gated).** The optional CI dry run of the release workflow is expected RED at the compare step, is never counted as a pass, and its resume path is E-19 (the user's own report recorded verbatim, E-12 left pending). The gated SHA it refers to is `1c583b46249ab9bd30ff717c256c7f583c4f0ed9` as of this plan, since superseded (see `last_non_planning_commit` above); the handoff holds the current one, `e1e05604fba53aaa2aa1b33dc66ec33624343dbe`.
- **ROADMAP criterion 5 wording (O-6).** It asks for the workflow run that created the release and says the record closes the "no run exercised the publish path" caveat. The measured facts allow neither: no run created `v1.7.0_gg5`, and the caveat stays open until Phase 38. The orchestrator decides whether to reword the criterion; this plan did not edit it.
- **Dispositions that need the user's approval if he wants them fixed:** the four `src/main` rows (26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01) and the null assertion of 28 IN-05 (it changes an existing assertion). Also open with the user: the header comment of `.github/workflows/increment-version.yml`, which names `ReleaseWorkflowGuardUnitTest` instead of `IncrementVersionWorkflowGuardUnitTest` (trigger: next edit of `increment-version.yml`).
- **Orchestrator bookkeeping:** `ADV-01`, `ADV-02` and `ADV-03` are listed in `requirements-completed`, with ADV-03 met as measured (record complete, caveat open, deviation stated); REQUIREMENTS.md, ROADMAP.md and STATE.md were not touched by this run.

## Handoffs

- **Phase 38** (the six items of `37-RELEASE-RECORD.md`):
  1. Bump `VERSION` to `v1.7.0_gg6` with `python3 -I .github/scripts/bump_version.py gg` from the repository root (it moves the `VERSION` row of `fork-anchors.txt` in the same step) and commit both files together; Phase 38 edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file.
  2. Write `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt` last, from the final tree, baseline `v1.7.0_gg5`, never copied from the gg5 list; prove it with the E-07 run A recipe (compare exits 0, no STALE-ROW).
  3. The release dry run of RLS-05 is the first CI exercise of the compare chain unless the user ran the handoff (E-12, expected RED; E-19 would hold the report); a CI-only defect found there returns to Phase 37 as a gap because Phase 38 may not edit `.github/`.
  4. The ADV-03 caveat closes only with the gg6 publish through `release.yaml` from `custom_build` and its recorded run (RLS-05).
  5. The anchors file holds `rows=172`; the `VERSION` row and the release rows are the first things the cumulative gate checks.
  6. The compare is strict on a dry run too: a green dry run means a passing compare.
  - Carried alongside as information: the `increment-version.yml` header comment (owner the user, trigger next edit of that file; Phase 38 may not edit `.github/`, so it is not Phase 38's); UF-36-4 and F-1 to F-3 with their owners and triggers in the record; the four `src/main` rows and the 28 IN-05 null assertion as dispositions.
- **Orchestrator, if the user runs the dry run:** record the report verbatim as `### E-19: CIH-04 dry-run dispatch, user-reported`, add index row E-19, leave E-12 `pending`, mark nothing passed through it, commit documents only, never touch `.github/`, and act only on the user's own message.

## Next Phase Readiness

Phase 37 is gated and ready for verification: every plan has a SUMMARY, the evidence index reads pass for E-01 to E-11 and E-13 to E-18 and pending (user-gated) for E-12, `testUnit` is green at 1633 tests, the full suite is green at 2231 tests with 18 named skips, and the anchors mirror reads `rows=172 bad=0`. Phase 38 can start from the six handoffs.

## Self-Check: PASSED

- Files: `37-ADVISORY-LEDGER.md`, `37-RELEASE-RECORD.md`, `37-COMMAND-EVIDENCE.md` and `37-USER-DISPATCH-DRYRUN.md` exist; the plan assets and both helper files are unchanged against the phase start.
- Commits `d95f800a`, `48d9695d`, `32534877`, `b2b2d265`, `31d0fff6` are ancestors of `HEAD`.
- All three plan `<automated>` blocks print their OK marker (`P37-05-T1-OK`, `P37-05-T2-OK` and `P37-05-T3-OK` with the scratch-ref clauses adjusted as in deviation 2, plus `P37-05-T3-TESTS-OK`).

---
*Phase: 37-ci-hardening-advisory-debt*
*Completed: 2026-10-31*
