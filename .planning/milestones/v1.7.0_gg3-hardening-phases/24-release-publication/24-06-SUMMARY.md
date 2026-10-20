---
phase: 24-release-publication
plan: "06"
subsystem: release
tags: [suite-gate, ci, rehearsal, push, github-actions]

requires:
  - phase: 24-release-publication
    provides: "24-05: frozen, reviewed tree (21-24 dispositions open: 0, 21-23 SECURITY threats_open: 0)"
provides:
  - "SUITE-06 gate: testUnit green with Docker stopped (838 tests, 2 skipped); two cache-busted full runs green (1281 tests, 0/0, 18 named skips) on ClickHouse 26.9.19.9 x client V2; anchors PASS; spotless baseline-equal"
  - "Rehearsal push of sync/v1.7.0 at X0=3ab55e1da878c6619f711d1220793e9d2456fa9d on the user's own 'push'; one green tests.yaml run 39470352668 under X0"
  - "Release bundle (measured) for plan 24-07's approval gate, verbatim under ## Approval bundle"
affects: [24-07]

actuals:
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md

key-decisions:
  - "The user answered 'push' directly to the orchestrator at the blocking-human gate; Task 3 (re-checks, push, CI watch, records) ran inline in the orchestrator, never in a subagent"
  - "latest moved 26.9.18.11 (Phase 20 gate) -> 26.9.19.9 (this gate); local and CI both green, no test edited"
  - "The inline 24-05 review (not a separately spawned gsd-code-reviewer) was surfaced at the gate and accepted by the user's 'push'"

patterns-established: []

requirements-completed: [SUITE-06]

coverage:
  - id: D1
    description: "Frozen tree gated: Docker-stopped testUnit green, two full runs green with the 18 named skips, anchors invariant PASS, spotless baseline-equal on X"
    requirement: "SUITE-06"
    verification:
      - kind: integration
        ref: "E-60: FREEZE-OK, TESTUNIT-DOCKER-STOPPED-GREEN, SUITE-TWICE-GREEN, SPOTLESS-BASELINE-EQUAL-ON-X; anchors result=PASS"
        status: pass
    human_judgment: false
  - id: D2
    description: "Rehearsal push of sync/v1.7.0 at X0 with exactly one green tests.yaml run under X0; origin moved only by the new branch"
    requirement: "PUB-10"
    verification:
      - kind: e2e
        ref: "E-62: REHEARSAL-PUSHED-ONLY, REHEARSAL-RUN-GREEN; run 39470352668 completed/success"
        status: pass
    human_judgment: false
  - id: D3
    description: "The push happened only on the user's own explicit answer, given directly to the orchestrator and recorded verbatim"
    requirement: "PUB-10"
    verification: []
    human_judgment: true
    rationale: "The approval itself is a human act; E-62 records the verbatim answer and the receiver"

duration: 47min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 06: Suite gate and CI rehearsal Summary

**The frozen v1.7.0_gg3 tree passed the full gate (1281 tests x2, 0/0, 18 named skips; Docker-stopped testUnit 838/0; anchors PASS) and ran green on CI under its own SHA after the user's direct "push"**

## Performance

- **Duration:** about 47 min end to end (Task 1 gate about 19 min; CI run about 6 min; the rest re-checks, polling and records)
- **Started:** 2026-10-20T17:16:21Z
- **Completed:** 2026-10-20T18:05:00Z (approx)
- **Tasks:** 3 (Task 1 subagent; Task 2 checkpoint; Task 3 orchestrator inline)
- **Files modified:** 1 (24-COMMAND-EVIDENCE.md, two commits)

## Accomplishments

- SUITE-06:
  - `testUnit` green with colima `vz` genuinely stopped (`docker info` failed): 49 classes, 838 tests, 2 skipped, 0/0, no live class.
  - Two full `test --rerun` runs, each 90 classes, 1281 tests, 0 failures, 0 errors, 18 skips equal by name to FORK-DELTA's inventory. Run 2 is provably later than run 1.
  - Pinned counts: `NullableTupleMatrixLiveTest` 33, `DeclaredNullabilityTest` 62, `ClickHouseHelperClientForkUnitTest` 51, `JsonAsStringKeyResidualLiveTest` 4, `DedupTokenRetryLiveTest` 8.
  - Server `26.9.19.9` in both runs, client V2; the `latest` image was created 2026-10-14T18:05:21Z.
  - Anchors invariant `result=PASS` (69 rows); spotless numstat equal to `P24_BASE` (46 lines); OPS-01 holds.
- PUB-10 rehearsal:
  - PUSH-READY, MERGE-DRY-RUN-OK and ROLLBACK-ON-TIP-OK re-passed on X0.
  - Then `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0` (no force, no tag).
  - Exactly one `tests.yaml` run under X0: id 39470352668, event push, completed/success in about 6 min on `ubuntu-24.04`; zero cancelled.
  - Origin `custom_build` still `6473e24f`, `main` unchanged, tag list byte-identical.
- E-60, E-61 and E-62 committed; REHEARSAL-PUSHED-ONLY and REHEARSAL-RUN-GREEN printed.

## The rehearsal answer

**Verbatim:** `push`. It is the user's own answer, given directly to the orchestrator at the Task 2 `blocking-human` checkpoint after the measured bundle (items 1-8) was presented. Task 3 ran inline in the orchestrator; no subagent held push authority. The answer is recorded in E-62.

## Task Commits

1. **Task 1: suite gate and pre-push checks** - `3ab55e1d` (docs). X0 is this commit.
2. **Task 3: rehearsal run record (E-62)** - `779e8eaf` (docs)

**Plan metadata:** see close-out commit

## Files Created/Modified

- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md`: E-60 (gate), E-61 (OPS-01, pre-push checks, dry-run merge and rollback on it), E-62 (push, run, X0 line, ref snapshots)

## Decisions Made

- `latest` moved one patch release between the Phase 20 gate (26.9.18.11) and this gate (26.9.19.9). Both were green, and nothing was edited. CI pulled its own `latest` and was green too.
- The user was shown the gate's "also surface" items before answering: FORK-DELTA's published-tense text (24-REVIEW IN-02), the inline rather than separately spawned 24-05 review, and the narrowed E-13 shape query. The user answered `push` without asking for a further review.

## Deviations from Plan

- **Executor context by path, not inline.** The orchestrator passed the GSD executor context files to the plan executors as an absolute-path Read list rather than inlining them. Each executor read them, so the plan steps are unchanged.
- Otherwise the plan was executed exactly as written: Task 1 ran in a subagent, Task 2 was presented to the user, and Task 3 ran inline after the verbatim answer. The D-10 contingency did not fire (no E-63).

## Issues Encountered

None.

## User Setup Required

None. No CI-log token was needed, because the run was green.

## Next Phase Readiness

- `sync/v1.7.0` is on origin at X0 with one green run.
- Plan 24-07 presents the approval bundle below. On the user's own "publish", it pushes the rollback tag, makes the rehearsed `--no-ff` merge, pushes `custom_build`, waits for its single green run, pushes the `v1.7.0_gg3` tag, then deletes the scratch branch.
- Commits after X0 (`779e8eaf` and this close-out) touch only `.planning/`, so the X0..HEAD guard holds.

## Self-Check: PASSED

- `24-COMMAND-EVIDENCE.md` holds E-60, E-61, E-62 and the line `X0=3ab55e1da878c6619f711d1220793e9d2456fa9d`.
- Commits `3ab55e1d` and `779e8eaf` exist on `sync/v1.7.0`.
- REHEARSAL-PUSHED-ONLY and REHEARSAL-RUN-GREEN printed after E-62 was committed.

## Approval bundle

# Release bundle: merge sync/v1.7.0 into custom_build and tag v1.7.0_gg3 (plan 24-07)

Every item below was measured by a command in plan 24-06, or read by command from a committed file.

## 1. The tip

- X0 = `3ab55e1da878c6619f711d1220793e9d2456fa9d` (`sync/v1.7.0` on origin, one green CI run under this SHA).
- `git rev-list --left-right --count origin/custom_build...X0` printed `0	159`. The left 0 means origin's `custom_build` (`6473e24f`) has no commit the tip lacks (D-10). The right 159 are every commit since `6473e24f`, from Phases 21-24.
- Non-planning diff against `6473e24f`: **28 files changed, 4605 insertions(+), 224 deletions(-)**:
  - `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, and the new `docs/ROLLOUT-v1.7.0_gg3.md`;
  - 4 `src/main` files from Phases 22-23 (`ClickHouseWriter`, `Column`, `DeclaredNullability`, the new `WithheldMessageException`), none from Phase 24;
  - 20 `src/test` files, 10 of them new, including the Phase 24 `JsonAsStringKeyResidualLiveTest`.
- With `.planning/` the diff is 245 files, 27099 insertions, 706 deletions.
- Commits after X0 (`779e8eaf` E-62 and the 24-06 close-out) touch only `.planning/`. Plan 24-07 merges the `sync/v1.7.0` tip X, so the merge also carries those `.planning/` commits; X0..X stays `.planning/`-only.

## 2. The gate (E-60) and the rehearsal run (E-62)

- **Gate on the frozen tree** (`a9f8bc88`; X0 adds only the E-60/E-61 evidence):
  - FREEZE-OK; `clean compileJava` generated `ARTIFACT_VERSION = "v1.7.0_gg3"`.
  - `testUnit` green with Docker genuinely stopped: 49 classes, 838 tests, 2 skipped, 0/0, no live class.
  - Two full `test --rerun` runs, each 90 classes, 1281 tests, 0 failures, 0 errors, 18 skips equal by name to FORK-DELTA's inventory. Run 2 is provably later than run 1.
  - Pinned counts: `NullableTupleMatrixLiveTest` 33, `DeclaredNullabilityTest` 62, `ClickHouseHelperClientForkUnitTest` 51, `JsonAsStringKeyResidualLiveTest` 4, `DedupTokenRetryLiveTest` 8.
  - Server `26.9.19.9` in both runs, client V2; the local `latest` image was created 2026-10-14.
  - Anchors invariant `result=PASS` (69 rows); spotless numstat equal to `P24_BASE` (46 lines); OPS-01 holds.
- **Rehearsal run on X0:** id 39470352668, `.github/workflows/tests.yaml`, event push, head_branch `sync/v1.7.0`, completed/success.
  - Started 2026-10-20T17:56:03Z, updated 2026-10-20T18:02:09Z (about 6 min), runner label `ubuntu-24.04`.
  - The only run on X0, zero cancelled.
  - URL: https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/39470352668

## 3. Review and security records (read from committed files)

- Dispositions, all `open: 0`: `21-REVIEW-DISPOSITION.md` total 7, `22-` total 6, `23-` total 6, `24-` total 5.
- `21-`, `22-` and `23-SECURITY.md`: `threats_open: 0`. Accepted threats, all closed and low: R-21-01; R-22-01 to R-22-04 (R-22-02: on the opt-in JSON-as-string route a Jackson exception chain can name a Map key, measured by E-03 and disclosed in FORK-DELTA Known costs item 6); R-23-01 to R-23-04.
- Deferred findings, carried in `24-MILESTONE-CLOSE-RECORD.md` with reason, owner, re-open trigger and source:
  - 21-REVIEW WR-01, WR-02, WR-03, IN-01, IN-02, IN-04;
  - 22-REVIEW WR-01, IN-01, IN-02, IN-03, IN-04, IN-05;
  - 23-REVIEW WR-01, WR-02, IN-01, IN-02, IN-03;
  - 24-REVIEW IN-03 (deferred) and IN-02 (skipped by design: FORK-DELTA describes the release as published, true once plan 24-07 completes).
- Fixed in Phase 24: 21-REVIEW IN-03 (`177f247c`), 23-REVIEW IN-04 (`9d4cb9b2`), 24-REVIEW WR-01 (`0c08e663`), WR-02 (`cd40803c`), IN-01 (`6ee4a22a`).
- The 24-05 pre-gate review was run inline by an executor following the reviewer procedure, not by a separately spawned `gsd-code-reviewer` (accepted by the user's "push" at the rehearsal gate).
- Other carried items: the `jacocoForkReport` class list omits `DeclaredNullability` and `WithheldMessageException`; the three D-11 reminders; the `/gsd-audit-milestone` instruction; housekeeping (scratch directories, any CI-log token).

## 4. The documents shipped

- `docs/ROLLOUT-v1.7.0_gg3.md`, seven items in order: (1) Record values in writer error text, (2) The unpaired-element WARN and Tuple elements named size, (3) Backslashes and line breaks in names, (4) In-flight batches, (5) JMX MBean name and client name, (6) Dependency updates, (7) Rollback.
- `CHANGELOG.md`: heading `# 1.7.0_gg3, 2026-10-20`, equal to today's UTC date (2026-10-20). The gg2 section onward is byte-identical to `P24_BASE`.
- `docs/FORK-DELTA.md` raw numbers, `v1.7.0` to `v1.7.0_gg3`: wider scope (`src build.gradle.kts gradle .github`) 130 files, 22783 insertions, 534 deletions; `src/main` 33 files, 2245 insertions, 145 deletions. The two `git diff --shortstat v1.7.0 v1.7.0_gg3` commands under the table are re-run once the tag exists.
- `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/ROLLOUT-v1.7.0_gg2.md` are unchanged.

## 5. The merge plan (each step in plan 24-07, behind its own approval)

1. D-10 re-check: origin `custom_build` still `6473e24f`, X0 an ancestor of X, X0..X `.planning/`-only, at least 25 API requests left.
2. Rollback tag first: `git push origin refs/tags/pre-v1.7.0_gg3:refs/tags/pre-v1.7.0_gg3`. The tag is on `6473e24f`, whose `tests.yaml` triggers on branches only, so the tag push starts no run.
3. In a throwaway worktree `/tmp/gsd24/cb` on `custom_build` (fast-forwarded to `6473e24f`): `git merge --no-ff -F /tmp/gsd24/merge-msg.txt <X>` by full SHA. Subject `Merge sync/v1.7.0: release v1.7.0_gg3`; the body names the version, the rollout note, the CHANGELOG section and the rollback.
4. Checks on the merge commit M before any push: MERGE-SHAPE-OK, MERGED-TREE-NO-TRUNCATE-INSTRUCTION, and `/tmp/gsd24/rollback-demo.sh` against M. A failure drops the local merge; nothing of it is published.
5. `git -C /tmp/gsd24/cb push origin refs/heads/custom_build:refs/heads/custom_build`, no other flag or refspec.
6. The single `tests.yaml` run on M; then, only after green, `git tag v1.7.0_gg3 <M>` (lightweight) and `git push origin refs/tags/v1.7.0_gg3:refs/tags/v1.7.0_gg3`.
7. `git push origin --delete refs/heads/sync/v1.7.0`.

M differs from X0, so the two runs cannot cancel each other (the `tests.yaml` concurrency group is keyed on the SHA).

## 6. What "publish" makes permanent, and the rollback path

- The merge makes the 159 commits since `6473e24f` (plus the `.planning/` close-out commits after X0) reachable from `custom_build`. The `v1.7.0_gg3` tag pins the release, and `pre-v1.7.0_gg3` names the pre-release tree.
- Rollback (rollout note item 7): from a clean, fast-forwarded `custom_build`, run `git read-tree -u --reset pre-v1.7.0_gg3` and commit. That tree is the published `v1.7.0_gg2` tree plus the three Dependabot updates already on `custom_build`.
- Then redeploy the archive built at tag `v1.7.0_gg2`, after the item 4 check. No release asset is published for any tag.
- The rollback commands were proven on a dry-run merge of X0 (E-61, E-62) and are re-run on M before its push.

## 7. What the release contains and does not contain

- No `src/main` change in Phase 24. The five Phase 24 files outside `.planning/` are `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg3.md` and `JsonAsStringKeyResidualLiveTest.java`.
- The `src/main` changes shipped are from Phases 22-23: value-free writer error text with its one measured exception (a Map key on the opt-in JSON-as-string route), the backslash-n `Nullable(Tuple)` fix, and the WARN that names a Tuple element dropped at discovery.
- No dependency change beyond the three Dependabot updates already on `custom_build`. The shipped jar built at `v1.7.0_gg2` and the one built at `6473e24f` are identical entry by entry (E-04).
- The gg1 and gg2 rollout notes are unchanged.
- Known open defects, no fix in this release: backlog 999.4 (a Tuple element named `size`/`size0` is dropped at discovery) and 999.5 (backslashes in names doubled by `fromJsonRow`), FORK-DELTA Known costs items 10 and 11.

## 8. After the tag, and no agent changes repository settings

- The three D-11 reminders follow the tag. Nothing waits on them, and they are for the user in the GitHub web UI:
  1. Create the `dependencies` label, or drop the two `labels:` requests.
  2. Close or ignore PRs #4 (client 0.9.6 to 0.10.0) and #9 (`connect-api` 4.1.1 to 4.4.0), the fork-only pins.
  3. Merge or close PRs #6, #7 and #8 once `v1.7.0_gg3` is published. Any merge diverges the next milestone's base, which then opens with a DEPS-01-style merge.
- No agent changes repository settings.
- Run `/gsd-audit-milestone` after Phase 24 verification and before `/gsd-complete-milestone`.
- No CI-log token was needed or created (the rehearsal run was green).

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
