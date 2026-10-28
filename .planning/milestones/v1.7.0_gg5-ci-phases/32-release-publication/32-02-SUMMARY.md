---
phase: 32-release-publication
plan: "02"
subsystem: release-engineering
tags: [rollout-note, rollback-rehearsal, operator-docs, nightly-toggle, mbean, evidence]

requires:
  - phase: 32-release-publication
    provides: "plan 32-01 evidence E-01..E-05 (MBean and client name, archive compare, release path, origin ground truth)"
provides:
  - docs/ROLLOUT-v1.7.0_gg5.md, the five-claim operator note (CIG-02)
  - 32-COMMAND-EVIDENCE.md E-10 (claims 1 and 4 re-run on the tip) and E-11 (rollback rehearsal on a dry-run merge)
  - local lightweight tag pre-v1.7.0_gg5 on 21c51598 (not pushed)
affects: [32-03, 32-04, 32-05, 32-06, 32-07]

plan_head_before: b7592d1d72546c29bee09e0ecca1ff0eb8734075
plan_head_after: 143a66cf6b48e129b9cda2355136e22b9ce04bbb

actuals:
  tokens: 4900
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "rollback section rehearsed by a script on a dry-run --no-ff merge of the committed tip onto the rollback target"
    - "every output block of the note is copied from an E-nn entry; unmeasured statements are left out"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg5.md
  modified:
    - .planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md

key-decisions:
  - "Four-row wording (32-AMENDMENT-01) applied in the note: the archive differs from the gg4 one only in the release identity, namely the version constant, the two classes that inline it and the manifest Implementation-Version"
  - "Turning the nightly off is stated as deleting NIGHTLY_ENABLED or setting it to false; how GitHub compares other values was not measured and is left out"
  - "The note says a scheduled run with a false job condition shows its job as skipped, backed by GitHub's documentation quote recorded in E-10"

requirements-completed: [CIG-02]

coverage:
  - id: D1
    description: "docs/ROLLOUT-v1.7.0_gg5.md: one page of 116 lines with five numbered claim sections, each with a command block and a text output block"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-02 Task 3 verify: NOTE-FINAL-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "Claim 1: git diff --quiet v1.7.0_gg4 against the tip over src/main prints 0 (E-10)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-02 Task 1 verify: NOTE-FRAME-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "Claims 2 to 4: archive built by the wrapper's Gradle with verifyConfluentArchive, MBean and client name v1.7.0_gg5 on 26.10.3.7 with client V2, nightly toggle with the workflow's exact grep lines"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-02 Task 2 verify: ITEMS-2-4-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Claim 5: the rollback to tag pre-v1.7.0_gg5 ran as written on a dry-run merge of the tip onto 21c51598 and printed ok, doc gone, one rollback heading, nightly absent, VERSION v1.7.0_gg4, revert restores (E-11)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-02 Task 3 verify: ROLLBACK-REHEARSED-OK"
        status: pass
    human_judgment: false
  - id: D5
    description: "Operator readability and wording of the note (review happens in the phase review round, D-04)"
    requirement: CIG-02
    verification: []
    human_judgment: true
    rationale: "Whether the note reads clearly to an operator is a judgment that the phase's review rounds decide; no test asserts it"

duration: 8min
completed: 2026-10-26
status: complete
---

# Phase 32 Plan 02: The gg5 rollout note Summary

**A 116-line operator note with five measured claims (empty `src/main` diff, wrapper-built verified archive, `v1.7.0_gg5` MBean and client name, nightly toggle, rollback to `pre-v1.7.0_gg5`), whose rollback was run as written on a dry-run merge of the tip onto `21c51598`.**

## Performance

- **Duration:** about 8 min
- **Started:** 2026-10-26T21:11:00Z (approximate)
- **Completed:** 2026-10-26T21:19:00Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 2 tracked (the new note and the evidence file); the local tag and scratch files are outside the tree

## Accomplishments

- `docs/ROLLOUT-v1.7.0_gg5.md`, 116 lines (limit 150), sections `## 1. No connector code changed`, `## 2. The release archive`, `## 3. MBean name and client name`, `## 4. Turning the nightly on and off`, `## 5. Rollback`. It carries no reviewer finding and no host, credential or production name; stand-ins are `<connector-name>`, `<topic>` and `<rollback-commit>`.
- Tracer gate: claim 1 re-run on the tip printed `0` with an empty stat (E-10); NOTE-FRAME-OK passed before any expansion.
- Claims 2 to 4 copy their command output from E-02 to E-05: the `verifyConfluentArchive` lines and the gg5 zip name, runs `40296330498` and `40296481765`, the per-task and per-topic MBean names and user agent measured on ClickHouse 26.10.3.7 with client V2, and the three grep lines of the nightly workflow (cron, dispatch, job condition), also recorded in E-10.
- Local lightweight tag `pre-v1.7.0_gg5` on `21c51598b06b6f5af58430765d57b1e5f6747c5a` (`git cat-file -t` prints `commit`); `git ls-remote --tags origin pre-v1.7.0_gg5` lists nothing. Nothing was pushed.
- Rollback script `/tmp/gsd32/rollback-demo.sh` (scratch, adapted from `/tmp/gsd29/rollback-demo.sh`): `pre-v1.7.0_gg4` became `pre-v1.7.0_gg5`, the branch prefix `gg5-rollback-demo-`, the commit message `Roll back v1.7.0_gg5`, the document path the gg5 note, the heading count `^## 5\. Rollback` printed as `rollback headings: <n>`, and the gg1-rule line replaced by `nightly workflow after rollback: absent|present` and `VERSION after rollback: ...`.
- Rehearsal: dry-run `--no-ff` merge `D` = `f42be9d9a548315a96372d78fbe8c2e981d65c0d` of the note tip `b265ed28` onto `21c51598` (parents as expected, tree `dcf1ed16...` equal to the tip's). The script log printed `ok`, `rollout doc gone from branch`, `rollback headings: 1`, `nightly workflow after rollback: absent`, `VERSION after rollback: v1.7.0_gg4`, the revert-restores line and `local tag kept after fetch: 21c51598`. Both worktrees were removed.
- Verdicts: E-02 flipped to `Matches text: yes`; E-03, E-04 and E-05 narrowed to `pending (32-03)`; E-10 and E-11 are `yes`.

## Task Commits

1. **Task 1 (tracer): note frame and claim 1, E-10** - `94e3dd96` (docs)
2. **Task 2: claims 2 to 4, E-10 grep and docs quote** - `692cc3b1` (docs)
3. **Task 3: claim 5, then E-11 and verdicts** - `b265ed28` (docs, section 5) and `143a66cf` (docs, E-11 and verdicts)

**Plan metadata:** the commit that carries this SUMMARY.

## Files Created/Modified

- `docs/ROLLOUT-v1.7.0_gg5.md` - the operator note (new)
- `.planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md` - E-10, E-11 and the verdict changes
- Not committed: `/tmp/gsd32/rollback-demo.sh`, `/tmp/gsd32/exec/nightly-toggle-grep.txt`, `/tmp/gsd32/exec/rollback-on-dry.log`, `/tmp/gsd32/docs-fetch/cond.md`; the dry-run worktrees are removed.

## Decisions Made

- 32-AMENDMENT-01 applied: the plan's "differs only in Version.class and the manifest" became "differs only in the release identity: the version constant, the two classes that inline it and the manifest `Implementation-Version`" in the note (section 1 and section 5, step 3). The plan's own text was not edited; the note's verify commands never relied on the two-row wording.
- Off-switch worded as "delete the variable or set it to `false`" because the comparison of other values (case, type) was not measured; that statement is left out.
- "A scheduled run shows its job as skipped" is backed by GitHub's documentation (quote and source URL in E-10), since the nightly is not on `custom_build` yet and no scheduled run could be observed.

## Deviations from Plan

None - plan executed exactly as written, with the binding four-row wording of `32-AMENDMENT-01.md` applied where the plan text said two rows. In section 2 the plan's "Phase 30's dry runs" was phrased "Two earlier dry runs of the release workflows" because the note is operator-facing and must not carry phase names.

## Issues Encountered

None. The scratch worktrees of Phase 29 (`/tmp/gsd29/wt-gg3`, `/tmp/gsd29/wt-tip`) still appear in `git worktree list`; they are not this plan's and were left alone.

## Known Stubs

None - documentation only.

## Threat Flags

None - no new network endpoint, auth path or file access pattern; the tag is local.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 32-03 (CHANGELOG and FORK-DELTA): E-03, E-04 and E-05 are the sources it flips to `yes`; its text must read "four rows / changed 4".
- The tag `pre-v1.7.0_gg5` is local; plan 32-06 pushes it after the green `tests.yaml` run and repeats the rollback rehearsal on the real merge.
- Dirty tree not touched: ` M gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`.

## Self-Check: PASSED

The note, the evidence file and the scratch script exist; commits `94e3dd96`, `692cc3b1`, `b265ed28` and `143a66cf` are ancestors of HEAD; NOTE-FRAME-OK, ITEMS-2-4-OK, ROLLBACK-REHEARSED-OK and NOTE-FINAL-OK printed; `git diff --name-only 8dcbbfc1 HEAD -- . ':(exclude).planning'` lists exactly `VERSION` and `docs/ROLLOUT-v1.7.0_gg5.md`; origin lists no `pre-v1.7.0_gg5`.

---
*Phase: 32-release-publication*
*Completed: 2026-10-26*
