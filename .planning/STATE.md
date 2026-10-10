---
gsd_state_version: "1.0"
milestone: v1.7.0_gg2-hardening
milestone_name: Follow-up Hardening & CI Hygiene
current_phase: 16
current_phase_name: Early Docs Publication
status: executing
stopped_at: Completed 16-01-PLAN.md
last_updated: "2026-10-10T17:44:38.637Z"
last_activity: 2026-10-10
last_activity_desc: Phase 16 execution started
state_head: 2a4f85f2ce20a3b0e28b4ec61657ffdfb77cb4a4
progress:
  total_phases: 5
  completed_phases: 0
  total_plans: 3
  completed_plans: 1
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-10 after v1.7.0_gg1-sync milestone)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 16 — Early Docs Publication

## Current Position

Phase: 16 (Early Docs Publication) — EXECUTING
Plan: 2 of 3
Status: Ready to execute
Last activity: 2026-10-10 — Phase 16 execution started

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: `v1.7.0_gg1-sync`: 25; `v1.6.0_gg1-sync`: 24; `v1.2.9_gg4-tests`: 16
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 12 | 5 | - | - |
| 13 | 8 | - | - |
| 14 | 4 | - | - |
| 15 | 8 | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion. Per-plan metrics for Phases 1-5 are archived with the `v1.2.9_gg4-tests` milestone. The rows for Phases 6-11 below are from `v1.6.0_gg1-sync` and are kept for reference.*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 06 P01 | 20 min | 2 tasks | 222 files |
| Phase 06 P02 | 9 min | 3 tasks | 5 files |
| Phase 06 P03 | ~15min | 3 tasks | 1 files |
| Phase 07 P01 | 4 min | 2 tasks | 4 files |
| Phase 07 P02 | 8 min | 2 tasks | 3 files |
| Phase 07 P03 | 55min | 2 tasks | 18 files |
| Phase 07 P04 | 22 min | 2 tasks | 4 files |
| Phase 09 P01 | 25 min | 4 tasks | 2 files |
| Phase 09 P02 | 20 min | 3 tasks | 4 files |
| Phase 09 P03 | ~15min | 3 tasks | 2 files |
| Phase 10 P01 | 16 min | 3 tasks | 8 files |
| Phase 10 P02 | 4 min | 3 tasks | 0 files |
| Phase 10 P03 | 7min | 3 tasks | 2 files |
| Phase 11 P01 | 7 min | 3 tasks | 4 files |
| Phase 11 P02 | 12 min | 2 tasks | 2 files |
| Phase 11 P03 | 7 min | 2 tasks | 3 files |
| Phase 11 P08 | 25 min | 3 tasks | 4 files |
| Phase 11 P04 | 6 min | 3 tasks | 0 files |
| Phase 11 P05 | 11 min | 2 tasks | 0 files |
| Phase 11 P07 | 10 min | 2 tasks | 1 files |
| Phase 12 P01 | 11 min | 2 tasks | 9 files |
| Phase 12 P02 | 7 min | 3 tasks | 8 files |
| Phase 12 P03 | 11 min | 3 tasks | 5 files |
| Phase 12 P04 | 12 min | 2 tasks | 1 files |
| Phase 12 P05 | 5 min | 2 tasks | 2 files |
| Phase 13 P01 | 10 min | 2 tasks | 7 files |
| Phase 13 P05 | 8 min | 3 tasks | 7 files |
| Phase 13 P07 | 12 min | 2 tasks | 7 files |
| Phase 13 P02 | 12 min | 2 tasks | 1 files |
| Phase 13 P03 | 13 min | 3 tasks | 4 files |
| Phase 13 P06 | 6 min | 3 tasks | 3 files |
| Phase 13 P04 | 8 min | 2 tasks | 5 files |
| Phase 13 P08 | 6 min | 2 tasks | 3 files |
| Phase 14 P01 | 25 min | 2 tasks | 3 files |
| Phase 14 P02 | 30 min | 2 tasks | 5 files |
| Phase 14 P03 | 25 min | 2 tasks | 2 files |
| Phase 15 P01 | 10 min | 2 tasks | 7 files |
| Phase 15 P02 | 5 min | 2 tasks | 1 files |
| Phase 15 P03 | 14 min | 2 tasks | 6 files |
| Phase 15 P04 | 35 min | 2 tasks | 2 files |
| Phase 15 P05 | 9 min | 2 tasks | 2 files |
| Phase 15 P06 | 35min | 2 tasks | 1 files |
| Phase 15 P07 | 21 min | 3 tasks | 1 files |
| Phase 16 P01 | 4 min | 3 tasks | 4 files |

## Accumulated Context

### Decisions

The full decision log is in `PROJECT.md` → Key Decisions. Per-phase decision logs are archived with each milestone's phase artifacts under `milestones/`. Carried forward because they still constrain how work must be done:

- Prefer upstream wherever its implementation passes the fork's tests. Deliberate exceptions, each recorded in `docs/FORK-DELTA.md`: JMX `connector=<name>`, `Type.DYNAMIC` and its fail-fast, `targetTableFilter`, the describe sub-column skip, the metadata-free JSON path (KEEP), and the fork-only pins (ClickHouse client `0.9.6`, Kafka Connect API `4.1.1`).
- Any future upstream sync follows the `v1.6.0_gg1-sync`/`v1.7.0_gg1-sync` shape. Work on a scratch branch with a rollback tag. Land one mechanical merge commit behind `verify-merge-invariants.sh` (`PRE_TAG`, `UPSTREAM_REF`), re-apply by intent, and gate on two full runs. Publish as a non-force `--no-ff` merge into `custom_build` after human approval. Start from `docs/FORK-DELTA.md` and `.planning/MERGE-CHECKLIST-v1.7.0.md`.
- Publish a SHA on one branch only, and push tags only after that SHA's own CI run is green.
- Bug fixes live in the phase whose tests expose them; there is no separate bug-fix phase.
- A main-code change reaching outside a phase's declared scope stops for a user decision. The fork runs in production.
- Type coverage asserts typed SQL read-back, never `countRows`.
- The test JVM runs `file.encoding=windows-1252` on purpose. Any new reader of ClickHouse output must specify UTF-8 explicitly.
- client-v2 `GenericRecord` from a `Records` iterator is a mutable, reused row view. Snapshot it into a `Map` during iteration.
- New sink-task tests must create their tables before `start()`, because `ClickHouseWriter.start()` fails against an empty database.
- A two-run acceptance gate must bust Gradle's cache (`--rerun`). A bare second `./gradlew test` echoes the first run's results.

The `v1.7.0_gg1-sync` per-phase decision log is archived with its phase artifacts (`milestones/v1.7.0_gg1-sync-phases/`), and its locked decisions with its requirements (`milestones/v1.7.0_gg1-sync-REQUIREMENTS.md`).

**`v1.7.0_gg2-hardening` roadmap decisions (2026-10-10):**

- Phase order: 16 → 17 → 18 → 19 → 20, sequential on one working tree. Phase 16 (docs-only publication, PUB-05/PUB-06) comes first by user decision. Phase 20 (release) comes last.
- Phase 16's merge gate is a path allow-list diff (`.planning/`, `docs/`, `CHANGELOG.md`) plus a tree-equality check. The upstream-sync script `verify-merge-invariants.sh` does not apply to a docs-only merge and must not be "fixed". The same holds for the Phase 20 release merge's parent checks; the anchors invariant still runs on the branch tip.
- CI/build/test hygiene (Phase 17) runs before code because `ubuntu-latest` moves to Ubuntu 26 on 2026-10-19. CI-17 is proven by an approved branch-only scratch push in Phase 17, so the release merge's run is not the new workflow's first.
- Writer fixes (Phase 18) come before 999.1 (Phase 19), because both edit `ClickHouseWriter.java` and Phase 19's flipped pins rely on Phase 18's column-naming `DataException`. The Phase 19 Tuple spike (COLS-09) runs first in its phase. Its go/no-go cannot hold the release, and the Enum fix ships either way. DEBT-09 follows the COLS-10 outcome.
- Every approval gate (Phase 16 merge, Phase 17 scratch push, Phase 20 rehearsal and release merge) needs the user's own explicit answer. A relayed approval is refused.

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Publish]: ⚠ The published `docs/ROLLOUT-v1.7.0_gg1.md` (at `f328fd21`) still offers `TRUNCATE TABLE` on the shared KeeperMap state table (15-REVIEW WR-01 / T-15-14), plus the WR-02/WR-03 wording defects. The local docs-only commit `505e4981` fixes them, but it is not on origin. **Scheduled: Phase 16** (PUB-05, PUB-06).
- [Publish]: Local `sync/v1.7.0` is 20+ commits past `f328fd21` (the Phase 15 tail and milestone bookkeeping). They stay local until Phase 16's docs-only `--no-ff` merge. Its diff outside `.planning/` is today exactly `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg1.md`. `.planning/state.json` is modified and `.planning/.gsd-ws-arg` is untracked; neither may ride into the merge. Milestone tags such as `v1.7.0_gg1-sync` stay local.
- [CI]: ⏰ `ubuntu-latest` moves to Ubuntu 26 from **2026-10-19**. Phase 16's docs-only run should land before then, on the unchanged workflow. Phase 17 pins `ubuntu-24.04`. Two branches pushed at one SHA still cancel each other. Node 20 deprecation warnings affect `checkout@v4` and `upload-artifact@v4` (Phase 17).
- [CI]: `test-head.yaml` calls `tests.yaml` as a reusable workflow, but `tests.yaml` declares no `workflow_call`. Its nightly schedule runs from the default branch `custom_build` and very likely fails every night. The fix (CI-16) lands in Phase 17 and takes effect on the schedule only once Phase 20 publishes it.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

`v1.7.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. The close was an override closeout, because the Phase 12/13 verification digests had gone stale and no milestone audit was run (see `MILESTONES.md`). Its eleven open follow-ups are now scheduled as this milestone's 26 requirements (`REQUIREMENTS.md` → Traceability).

`v1.6.0_gg1-sync` also closed with a clear audit. All 17 of its follow-ups were closed in `v1.7.0_gg1-sync`.

## Session Continuity

Last session: 2026-10-10T17:44:38.618Z
Stopped at: Completed 16-01-PLAN.md
Resume file: None

## Operator Next Steps

- Review `.planning/ROADMAP.md` (Phases 16-20) and approve or request revisions
- Plan Phase 16 (`/gsd-discuss-phase 16` or `/gsd-plan-phase 16`). Its CI run should land before 2026-10-19
