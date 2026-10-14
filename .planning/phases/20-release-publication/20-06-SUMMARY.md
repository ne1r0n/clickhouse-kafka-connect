---
phase: 20-release-publication
plan: "06"
subsystem: release
tags: [suite-gate, ci, rehearsal, push, github-actions]

requires:
  - phase: 20-release-publication
    provides: "20-05: frozen, reviewed tree (20-REVIEW open: 0, 19-SECURITY threats_open: 0)"
provides:
  - "SUITE-05 gate: testUnit green with Docker stopped; two cache-busted full runs green (988 tests, 0/0, 18 named skips) on ClickHouse 26.9.18.11 x client V2; anchors PASS"
  - "Rehearsal push of sync/v1.7.0 at X0=d316bc454f99615e2d9c49a7e9632ec94eed80e5 on the user's own 'push'; one green tests.yaml run 38578966373 under X0"
  - "Release bundle (measured) for plan 20-07's approval gate, verbatim under ## Approval bundle"
affects: [20-07, 20-08]

actuals:
  tokens: 9000
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md

key-decisions:
  - "The user answered 'push' directly to the orchestrator at the blocking-human gate; Task 3 (push, CI watch, records) ran inline in the orchestrator, never in a subagent"
  - "latest moved 26.9.17.8 (Phase 19 gate) -> 26.9.18.11 (this gate); both green, no test edited"

patterns-established: []

requirements-completed: [SUITE-05, PUB-08]

coverage:
  - id: D1
    description: "Frozen tree gated: Docker-stopped testUnit green, two full runs green with the 18 named skips, anchors invariant PASS"
    requirement: "SUITE-05"
    verification:
      - kind: integration
        ref: "E-60: FREEZE-OK, TESTUNIT-DOCKER-STOPPED-GREEN, SUITE-TWICE-GREEN; anchors result=PASS"
        status: pass
    human_judgment: false
  - id: D2
    description: "Rehearsal push of sync/v1.7.0 at X0 with exactly one green tests.yaml run under X0; origin moved only by the new branch"
    requirement: "PUB-08"
    verification:
      - kind: e2e
        ref: "E-62: REHEARSAL-PUSHED-ONLY, REHEARSAL-RUN-GREEN; run 38578966373 completed/success"
        status: pass
    human_judgment: false
  - id: D3
    description: "The push happened only on the user's own explicit answer, given directly to the orchestrator and recorded verbatim"
    requirement: "PUB-08"
    verification: []
    human_judgment: true
    rationale: "The approval itself is a human act; E-62 records the verbatim answer and the receiver"

duration: 45min
completed: 2026-10-14
status: complete
---

# Phase 20 Plan 06: Suite gate and CI rehearsal Summary

**The frozen v1.7.0_gg2 tree passed the full gate (988 tests x2, 0/0, 18 named skips, Docker-stopped testUnit, anchors PASS) and ran green on CI under its own SHA after the user's direct "push"**

## Performance

- **Duration:** about 45 min end to end (gate 17 min; CI run about 7 min; the rest polling and records)
- **Started:** 2026-10-14T13:50:22Z
- **Completed:** 2026-10-14T14:35:00Z (approx)
- **Tasks:** 3 (Task 1 subagent; Task 2 checkpoint; Task 3 orchestrator inline)
- **Files modified:** 1 (20-COMMAND-EVIDENCE.md, two commits)

## Accomplishments

- SUITE-05: `testUnit` green with colima `vz` genuinely stopped (45 classes, 556 tests, 0/0); two full `test --rerun` runs each 85 classes, 988 tests, 0 failures, 0 errors, 18 skips equal by name to FORK-DELTA's inventory; `NullableTupleMatrixLiveTest` exactly 30 cases, `DedupTokenRetryLiveTest` 8; run 2 provably later than run 1; server `26.9.18.11` both runs, client V2; anchors invariant `result=PASS`.
- PUB-08 rehearsal: `git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0` (no force, no tag) at X0, after PUSH-READY and MERGE-DRY-RUN-OK re-passed on X0. Exactly one `tests.yaml` run under X0: id 38578966373, event push, completed/success in about 7 min on `ubuntu-24.04`; zero cancelled. Origin `custom_build` still `be6b8861`, `main` unchanged, tag list byte-identical.
- E-60, E-61, E-62 committed; REHEARSAL-PUSHED-ONLY and REHEARSAL-RUN-GREEN printed.

## The rehearsal answer

**Verbatim:** `push` — the user's own answer, given directly to the orchestrator at the Task 2 `blocking-human` checkpoint after the measured bundle (items 1-8) was presented verbatim. Task 3 ran inline in the orchestrator; no subagent held push authority. Recorded in E-62 before any Task 3 command ran.

## Task Commits

1. **Task 1: suite gate and pre-push checks** - `d316bc45` (docs) — X0 is this commit
2. **Task 3: rehearsal run record (E-62)** - `fdd52313` (docs)

**Plan metadata:** see close-out commit

## Files Created/Modified

- `.planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md` - E-60 (gate), E-61 (pre-push checks, dry-run merge), E-62 (push, run, X0 line, ref snapshots)

## Decisions Made

- `latest` moved one patch release between Phase 19's gate (26.9.17.8) and this gate (26.9.18.11); both green, nothing edited. CI pulled its own `latest` and was green too.
- The executor's Task 1 commit trailer read `Co-Authored-By: Claude Sonnet 5.5` (subagent harness attribution); recorded as-is, cosmetic.

## Deviations from Plan

None - plan executed exactly as written (Task 1 subagent, Task 2 presented to the user, Task 3 inline after the verbatim answer).

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `sync/v1.7.0` is on origin at X0 with one green run; plan 20-07 presents the approval bundle below and, on the user's own approval, pushes the rollback tag, makes the rehearsed `--no-ff` merge, pushes `custom_build`, waits for its single green run, pushes the `v1.7.0_gg2` tag, then deletes the scratch branch.
- Commits after X0 (`fdd52313`, this close-out) touch only `.planning/` — the X0..HEAD guard holds.

## Approval bundle

# Release bundle: merge X0 into custom_build and tag v1.7.0_gg2 (plan 20-07)

Every item below was measured by a command in plan 20-06, or read by command from a committed file.

## 1. The tip

- X0 = `d316bc454f99615e2d9c49a7e9632ec94eed80e5` (`sync/v1.7.0`, on origin, one green CI run under this SHA).
- `git rev-list --left-right --count origin/custom_build...X0` printed `4	159` at the gate (the left 4 are the prior release merges, folded in as first-parent history by the release `--no-ff` merge; the right 159 are everything since `be6b8861`, Phases 17-20).
- Non-planning diff against `be6b8861`: 42 files changed, 5899 insertions(+), 287 deletions(-) — CI/build/docs (`.github/dependabot.yml` new, `.github/workflows/test-head.yaml` deleted, `tests.yaml`, `CHANGELOG.md`, `VERSION`, `build.gradle.kts`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg2.md` new, `lombok.config`), 5 `src/main` files (Phases 17-19; none from Phase 20), 28 test files. Plus 97 `.planning/` paths. Evidence commits after X0 (`fdd52313` E-62 and the 20-06 close-out) touch only `.planning/`.

## 2. The gate (E-60) and the rehearsal run (E-62)

- Gate on X: FREEZE-OK; `clean compileJava` generated `ARTIFACT_VERSION = "v1.7.0_gg2"`; `testUnit` green with Docker genuinely stopped (45 classes, 556 tests, 0/0); two full `test --rerun` runs each 85 classes, 988 tests, 0 failures, 0 errors, 18 named skips equal to FORK-DELTA's inventory; `NullableTupleMatrixLiveTest` exactly 30, `DedupTokenRetryLiveTest` 8; run 2 provably after run 1; server `26.9.18.11` both runs (client V2); anchors invariant `result=PASS`.
- Rehearsal run on X0: id 38578966373, `.github/workflows/tests.yaml`, event push, head_branch `sync/v1.7.0`, completed/success, started 2026-10-14T14:15:57Z, updated 2026-10-14T14:23:10Z (about 7 min), runner label `ubuntu-24.04`, the only run on X0, zero cancelled. URL: https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38578966373

## 3. Review and security records (read from committed files)

- `20-REVIEW-DISPOSITION.md`: `open: 0`, `total: 6`, all fixed (docs-only). Deferred findings: none.
- `19-REVIEW-DISPOSITION.md`: `open: 0`, `total: 8`; 2 fixed, 6 deferred with written reasons (WR-01, WR-02, IN-01, IN-02, IN-03, IN-04), carried in `20-MILESTONE-CLOSE-RECORD.md` with IN-04's owner and re-open trigger.
- `19-SECURITY.md`: `threats_open: 0` (ASVS L1, block on high); 32/34 closed; T-19-12 and T-19-26 open at medium (non-blocking); T-19-11 (high) closed by the auditor with the IN-03/IN-04 rare fail-open note — weighed and accepted by the user at the rehearsal gate.
- Close record carried items: the five 19-REVIEW advisories, IN-04 with its pin, the three D-11 manual checks (`Outcome: pending (plan 20-08)`), the `/gsd-audit-milestone` instruction, the Keychain token revocation.

## 4. The documents shipped

- `docs/ROLLOUT-v1.7.0_gg2.md` — six items in order: (1) In-flight batches and the deduplication token, (2) Nullable Enum and Nullable Tuple values, (3) Empty Debezium records, (4) Error messages and log lines, (5) JMX MBean name and client name, (6) Rollback.
- `CHANGELOG.md` — heading `# 1.7.0_gg2, 2026-10-14` (matches today's UTC date); gg1 section onward byte-identical to base; no Dependencies subsection (no dependency change).
- `docs/FORK-DELTA.md` — raw numbers measured: wider scope 119 files, 18802 insertions, 529 deletions; `src/main` 32 files, 2042 insertions, 143 deletions; of the 63 `src/main` files present at `v1.7.0`, 38 byte-identical, 25 differ (14 import-order only, 11 fork logic). The gg2 tag commands are re-run in plan 20-07 once the tag exists.

## 5. The merge plan (each step in plan 20-07, behind its own approval)

1. Rollback tag first: `git push origin refs/tags/pre-v1.7.0_gg2:refs/tags/pre-v1.7.0_gg2` (the tag on `be6b8861`; `tests.yaml` triggers on branches only, so a tag push starts no run).
2. The worktree `--no-ff` merge of X0 by hash into `be6b8861` (the rehearsed shape: parents `be6b8861` and X0, tree equal to X0).
3. The checks on the merge commit M: shape, no TRUNCATE in the rollout doc, the rollback re-check (`docs/ROLLOUT-v1.7.0_gg1.md` item 8 against M).
4. `git push origin refs/heads/custom_build:refs/heads/custom_build`.
5. The single CI run on M; then, only after green, `git push origin refs/tags/v1.7.0_gg2:refs/tags/v1.7.0_gg2`.
6. `git push origin --delete refs/heads/sync/v1.7.0`.

One branch per SHA: M differs from X0 (PUB-08 adjacency), so the two runs cannot cancel each other.

## 6. What "publish" makes permanent, and the rollback path

The merge makes the 159 commits first-parent-reachable from `custom_build`; the `v1.7.0_gg2` tag pins the release. Rollback: from a clean, fast-forwarded `custom_build`, `git read-tree -u --reset pre-v1.7.0_gg2` and commit (rollout item 6 / the gg1 note's corrected item 8 shape); the `pre-v1.7.0_gg2` tag is published with the release for exactly this.

## 7. What the release does not contain

No `src/main` change in Phase 20 (the five Phase 20 files are CHANGELOG, VERSION, FORK-DELTA, the gg2 rollout note and one test class). No dependency change. The gg1 docs are unchanged.

## 8. After the tag (plan 20-08) — and no agent changes repository settings

Three manual checks follow the tag, presented to the user: Dependabot enablement, the default-branch intent (`main` at Phase 17, undecided), and the `exclude-paths` Dependabot key (17-REVIEW WR-01: no Dependabot config error, the gradle entry still runs). The milestone audit (`/gsd-audit-milestone`) and the Keychain token revocation are carried in the close record.

---
*Phase: 20-release-publication*
*Completed: 2026-10-14*
