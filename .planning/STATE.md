---
gsd_state_version: "1.0"
milestone: v1.7.0_gg1-sync
milestone_name: Upstream v1.7.0 Sync & Follow-up Hardening
status: planning
last_updated: "2026-10-07T16:17:45.876Z"
last_activity: 2026-10-07
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-07 after v1.6.0_gg1-sync milestone)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Planning next milestone — `v1.6.0_gg1-sync` shipped and archived 2026-10-07

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-10-07 — Milestone v1.7.0_gg1-sync started

## Performance Metrics

**Velocity:**

- Total plans completed: 24 (all from `v1.2.9_gg4-tests`)
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 06 | 3 | - | - |
| 07 | 4 | - | - |
| 08 | 3 | - | - |
| 09 | 3 | - | - |
| 10 | 3 | - | - |
| 11 | 8 | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion. Per-plan metrics for Phases 1-5 are archived with the `v1.2.9_gg4-tests` milestone.*
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

## Accumulated Context

### Decisions

The full decision log is in `PROJECT.md` → Key Decisions. The per-phase decision log for `v1.6.0_gg1-sync` is archived with the phase artifacts in `milestones/v1.6.0_gg1-sync-phases/`. Carried forward because they still constrain how work must be done:

- Prefer upstream wherever its implementation passes the fork's tests. Deliberate exceptions, each recorded in `docs/FORK-DELTA.md`: JMX `connector=<name>`, `Type.DYNAMIC` and its fail-fast, `targetTableFilter`, the describe sub-column skip, the metadata-free JSON path (KEEP), and the fork-only pins (ClickHouse client `0.9.6`, Kafka Connect API `4.1.1`).
- Any future upstream sync follows the `v1.6.0_gg1-sync` shape: scratch branch, rollback tag, one mechanical merge commit, re-apply by intent, and a non-force `--no-ff` merge into `custom_build` after human approval. Start it from `docs/FORK-DELTA.md` and `.planning/MERGE-CHECKLIST.md`.
- Bug fixes live in the phase whose tests expose them; there is no separate bug-fix phase.
- A main-code change reaching outside a phase's declared scope stops for a user decision. The fork runs in production.
- Type coverage asserts typed SQL read-back, never `countRows`.
- The test JVM runs `file.encoding=windows-1252` on purpose. Any new reader of ClickHouse output must specify UTF-8 explicitly.
- client-v2 `GenericRecord` from a `Records` iterator is a mutable, reused row view. Snapshot it into a `Map` during iteration.
- New sink-task tests must create their tables before `start()`, because `ClickHouseWriter.start()` fails against an empty database.
- A two-run acceptance gate must bust Gradle's cache (`--rerun`). A bare second `./gradlew test` echoes the first run's results.

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Publish]: ⚠ **Planning history after publication is local only.** Every `.planning/` commit after `c61d90ce` lives only on local `sync/v1.6.0`. That covers 11-07 bookkeeping, review, verification, UAT, security, and this milestone's archive commits. `custom_build` is protected and equals `origin`. Decide how and whether to publish them; pushing a tag or branch also triggers `tests.yaml`.
- [CI]: Pushing a tag onto the same SHA as a branch push starts a second `tests.yaml` run in the `github.sha` concurrency group and cancels the branch run. This happened on `c61d90ce`. Tracked in PROJECT.md → Active.
- [Prod]: ⚠ R-01's silent data loss is still pinned, not fixed (KEEP, Phase 9 D-01). The `lz4` pin (D-17) and the operator-visible MBean name change (D-07) are the two things to check at production rollout.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

`v1.6.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. Review and verification follow-ups (17) are tracked in `PROJECT.md` → Requirements → Active, not here.

## Session Continuity

Last session: 2026-10-07
Stopped at: Milestone v1.6.0_gg1-sync completed and archived (override closeout)
Resume file: None

## Operator Next Steps

- Decide whether to publish the post-`c61d90ce` planning history (see [Publish] above)
- Start the next milestone with `/gsd-new-milestone`
