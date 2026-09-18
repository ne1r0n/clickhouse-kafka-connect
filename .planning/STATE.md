---
gsd_state_version: "1.0"
milestone: v1.6.0_gg1-sync
milestone_name: Upstream v1.6.0 Sync
status: planning
last_updated: "2026-09-18T10:34:54.928Z"
last_activity: 2026-09-18
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-18)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** `v1.2.9_gg4-tests` shipped and archived. No milestone scheduled — planning the next one, or leaving the fork stable until an upstream rebase or a production need reopens it.

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-09-18 — Milestone v1.6.0_gg1-sync started

## Performance Metrics

**Velocity:**

- Total plans completed: 16
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 3 | - | - |
| 02 | 5 | - | - |
| 03 | 3 | - | - |
| 04 | 3 | - | - |
| 05 | 2 | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 40 min | 2 tasks | 2 files |
| Phase 01 P02 | 55min | 2 tasks | 5 files |
| Phase 01 P03 | 20 min | 2 tasks | 2 files |
| Phase 02 P01 | 10 min | 3 tasks | 3 files |
| Phase 02 P05 | 24min | 3 tasks | 2 files |
| Phase 02 P02 | 20min | 3 tasks | 2 files |
| Phase 02-fork-smt-coverage P03 | 8min | 3 tasks | 3 files |
| Phase 02 P04 | 16min | 3 tasks | 2 files |
| Phase 03 P01 | 24min | 3 tasks | 5 files |
| Phase 03 P02 | 26min | 3 tasks | 2 files |
| Phase 03 P03 | 22min | 3 tasks | 3 files |
| Phase 04 P01 | ~25min (continuation session) | 4 tasks | 3 files |
| Phase 04 P02 | ~55min | 3 tasks | 2 files |
| Phase 04 P03 | ~45 min | 3 tasks | 5 files |
| Phase 05 P01 | 10 min | 2 tasks | 2 files |
| Phase 05 P02 | ~20 min | 3 tasks | 1 files |

## Accumulated Context

### Decisions

Cleared at the `v1.2.9_gg4-tests` close. The full decision log with rationale and outcomes lives in `PROJECT.md` → Key Decisions; per-phase decision detail is archived under `.planning/milestones/v1.2.9_gg4-tests-phases/`.

Carried forward because they constrain how future work must be done, not just what was done:

- Bug fixes live in the phase whose tests expose them — there is no separate bug-fix phase.
- A main-code fix reaching outside the phase's declared scope stops for a user decision rather than being taken autonomously. The fork runs in production.
- Type coverage asserts typed SQL read-back, never `countRows` — a row-count assertion passes while a `JSON` column holds the wrong nested value.
- The test JVM runs `file.encoding=windows-1252` on purpose. Any new reader of ClickHouse output must specify UTF-8 explicitly.
- client-v2 `GenericRecord` from a `Records` iterator is a mutable, reused row view. Test helpers must snapshot values into a `Map` during iteration, never collect `GenericRecord` references into a list.
- New sink-task tests must create their tables before `start()` — `ClickHouseWriter.start()` fails against an empty database.

### Pending Todos

None yet.

### Blockers/Concerns

Resolved entries cleared at the `v1.2.9_gg4-tests` close. Open items:

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper and makes ClickHouse-backed tests fail spuriously. Run Gradle tests as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (already baked into `config.json`'s `build_command`/`test_command`). Docker runs on colima profile `vz`; profile `default` is stopped.
- [Prod]: ⚠ **Unfixed production bug, pinned only.** `insertFormat=json` + a `Struct` record + `bypassRowBinary` at its default `false` silently discards every row (no throw, `Written Rows: [0]`). Pinned by a labelled test at the user's direction; the fail-fast fix is not pre-approved (D-01/D-07) and needs its own plan.
- [Prod]: ⚠ **Retriable ClickHouse errors are likely not retried.** `Utils.handleException`'s `instanceof ClickHouseException` switch never matches under client V2 — the real type is `com.clickhouse.client.api.ServerException` — so the whole retriable/non-retriable error-code table is unreachable and every error falls through to the unconditional fallback. Out of the shipped milestone's declared scope.
- [Tooling]: The `gh` CLI is not installed locally (deliberately — D-11), so CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr budget) or the web UI. A rendered job summary has no unauthenticated endpoint — that one check is a human glance at the run's Summary tab.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel, so any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-09-18
Stopped at: Milestone `v1.2.9_gg4-tests` closed and archived — ROADMAP collapsed, REQUIREMENTS archived and removed, PROJECT.md evolved, tag created. Closeout type `override_closeout` (phases 1–4 verification stale by sequence; no milestone audit run) — see MILESTONES.md.
Resume file: None

## Operator Next Steps

- Start the next milestone with /gsd-new-milestone
