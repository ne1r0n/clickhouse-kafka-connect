---
gsd_state_version: "1.0"
current_phase: 01
current_phase_name: Test Infrastructure & Green Baseline
status: executing
stopped_at: Completed 01-02-PLAN.md
last_updated: "2026-09-15T22:05:14.916Z"
last_activity: 2026-09-16
last_activity_desc: Phase 01 execution started
state_head: 1adf5f6bb5c3f2a1cc84dc1160501ef79a22e202
progress:
  total_phases: 5
  completed_phases: 0
  total_plans: 3
  completed_plans: 2
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-15)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 01 — Test Infrastructure & Green Baseline

## Current Position

Phase: 01 (Test Infrastructure & Green Baseline) — EXECUTING
Plan: 3 of 3
Status: Ready to execute
Last activity: 2026-09-16 — Phase 01 execution started

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: 0
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 40 min | 2 tasks | 2 files |
| Phase 01 P02 | 55min | 2 tasks | 5 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Bug fixes live in the phase whose tests expose them. There is no separate bug-fix phase.
- [Roadmap]: JaCoCo is wired in Phase 1 so all later tests appear in coverage. CI changes and final verification come last (Phase 5).
- [Roadmap]: Phases 2, 3, and 4 are independent test tracks that depend only on Phase 1.
- [Phase 01]: 01-01: untouched suite is already green on ClickHouse latest x client V2 (0 failures); no D-01/D-02/D-04 fixes needed — Baseline capture revealed 14 apparent failures were a sandbox TESTCONTAINERS_HOST_OVERRIDE=null artifact, not real test/code issues; corrected re-run showed 187 tests, 0 failed, 7 skipped
- [Phase 01]: 01-01: D-06 smoke test checks image reference + well-formed version instead of image-label equality for latest — No ClickHouse image carries a usable version label (ClickHouse issue #54224); this is the closest observable equivalent, as D-06 itself anticipated
- [Phase 01]: 01-02: Container-per-fork singleton (PullPolicy.alwaysPull, if (db==null)) plus per-class database replaced the per-class container; @AfterEach MBean sweep and opt-in uniqueConnectorName added as a base-class safety net rather than retrofitting existing test files
- [Phase 01]: 01-02: Ryuk reliably reaps the shared container within ~15-20s of Gradle exit in this environment; no JVM shutdown hook was added
- [Phase 01]: 01-02: D-11 UTF-8 fix proven RED (windows-1252 mojibake) then GREEN via ClickHouseTestHarnessTest#helpersDecodeResponsesAsUtf8; no maxParallelForks change needed (0 flakes observed)

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 1]: Colima was not running at initialization, and every ClickHouse-backed test needs Docker. Whether the baseline on `latest` × V2 is green is still unknown.
- [Phase 3]: Research flag: audit `DESCRIBE ... describe_include_subcolumns=1` output on `latest`, and establish RowBinary ground truth for `JSON`/`Dynamic`, before writing DESC-03 and TYPE-06.
- [Phase 4]: Research flag: check how client-v2 `0.9.6` handles an empty `jsonInsertColumns` list before writing COLS-02.
- [Phase 5]: The `gh` CLI is not installed locally, so CI runs must be checked through the GitHub web UI or API.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-09-15T22:05:14.902Z
Stopped at: Completed 01-02-PLAN.md
Resume file: None
