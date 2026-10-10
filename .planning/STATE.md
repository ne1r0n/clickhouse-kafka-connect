---
gsd_state_version: "1.0"
milestone: v1.7.0_gg1-sync
milestone_name: Upstream v1.7.0 Sync & Follow-up Hardening
status: Awaiting next milestone
stopped_at: Phase 15 complete — all phases complete
last_updated: "2026-10-10T13:41:32.995Z"
last_activity: 2026-10-10
last_activity_desc: Milestone v1.7.0_gg1-sync completed and archived
state_head: cb6918f90315a423cde60fc6837634a6e758beb1
progress:
  total_phases: 4
  completed_phases: 4
  total_plans: 25
  completed_plans: 25
  percent: 100
current_phase: 15
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-10 after v1.7.0_gg1-sync milestone)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Planning next milestone (`/gsd-new-milestone`)

## Current Position

Phase: Milestone v1.7.0_gg1-sync complete
Plan: —
Status: Awaiting next milestone
Last activity: 2026-10-10 — Milestone v1.7.0_gg1-sync completed and archived

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

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Publish]: ⚠ The published `docs/ROLLOUT-v1.7.0_gg1.md` (at `f328fd21`) still offers `TRUNCATE TABLE` on the shared KeeperMap state table (15-REVIEW WR-01 / T-15-14), plus the WR-02/WR-03 wording defects. They are fixed by the local docs-only commit `505e4981`, which is not on origin. Push it, or let the next milestone's `--no-ff` merge carry it.
- [Publish]: Local `sync/v1.7.0` is 17+ commits past `f328fd21` (Phase 15 tail and milestone bookkeeping). These commits are local-only until the next `--no-ff` merge into `custom_build`. Milestone tags such as `v1.7.0_gg1-sync` stay local.
- [CI]: Two branches pushed at one SHA still cancel each other. `ubuntu-latest` moves to Ubuntu 26 from 2026-10-19, and Node 20 deprecation warnings affect `checkout@v4` and `upload-artifact@v4`.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

`v1.7.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. The close was an override closeout because the Phase 12/13 verification digests had gone stale and no milestone audit was run (see `MILESTONES.md`). Its eleven open follow-ups are under `PROJECT.md` → Requirements → Active.

`v1.6.0_gg1-sync` also closed with a clear audit. All 17 of its follow-ups were closed in `v1.7.0_gg1-sync`.

## Session Continuity

Last session: 2026-10-10T11:31:21.703Z
Stopped at: Milestone v1.7.0_gg1-sync archived
Resume file: None

## Operator Next Steps

- Decide when `505e4981` reaches `origin` (the published rollout doc still has the `TRUNCATE` example)
- Start the next milestone with `/gsd-new-milestone`
