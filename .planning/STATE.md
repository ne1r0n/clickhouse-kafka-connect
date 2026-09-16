---
gsd_state_version: "1.0"
current_phase: 3
current_phase_name: Table Metadata & JSON/Dynamic Types
status: planning
stopped_at: Phase 02 complete, ready to plan Phase 3
last_updated: "2026-09-16T18:50:42.776Z"
last_activity: 2026-09-16
last_activity_desc: Phase 02 complete, transitioned to Phase 3
state_head: a7a6d20c71ca4292fe6433bcdc6ca0f8024c22d3
progress:
  total_phases: 5
  completed_phases: 2
  total_plans: 8
  completed_plans: 8
  percent: 40
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-16)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 02 — Fork SMT Coverage

## Current Position

Phase: 3 — Table Metadata & JSON/Dynamic Types
Plan: Not started
Status: Ready to plan
Last activity: 2026-09-16 — Phase 02 complete, transitioned to Phase 3

Progress: [████░░░░░░] 40%

## Performance Metrics

**Velocity:**

- Total plans completed: 8
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 3 | - | - |
| 02 | 5 | - | - |

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

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Bug fixes live in the phase whose tests expose them. There is no separate bug-fix phase.
- [Roadmap]: CI changes and final verification come last (Phase 5).
- [Roadmap]: Phases 2, 3, and 4 are independent test tracks that depend only on Phase 1.
- [Phase 01]: The suite was already green on ClickHouse `latest` × client V2 with no fixes (baseline 187 tests / 0 failed / 7 skipped; 194 / 0 / 7 after the harness work). The smoke test checks the image reference and version format, because `latest` images carry no version label.
- [Phase 01]: `ClickHouseBase` gives one shared ClickHouse container per test JVM fork, a fresh empty database per test class, an inherited `@Tag("clickhouse")`, an `@AfterEach` MBean leak sweep, and opt-in `uniqueConnectorName(TestInfo)`. Proxy tests keep their own 23.8 container. New sink-task tests must create their table(s) before `start()`, because `ClickHouseWriter.start()` fails against an empty database.
- [Phase 01]: JaCoCo is report-only and attached to `test` (`jacocoTestReport` plus `jacocoForkReport` over 15 explicit fork-changed classes). `testUnit` runs everything not tagged `clickhouse`, needs no Docker, and is not part of `check`.
- [Phase 02]: Non-ASCII TOF-01 test data built from int[] Unicode code points at runtime instead of \u escape literals in source, because the editing tool chain silently decoded literal backslash-u escape text into real non-ASCII bytes.
- [Phase 02]: Approved out-of-scope ClickHouseWriter MAP-branch fix (RowBinary null marker for Nullable map values) at the 02-05 Task 2 blocking-human checkpoint after independent diagnosis verification.
- [Phase 02]: HDRS-09 fixture keeps Map(String, Nullable(String)) DDL as locked (D-08); rejected weakening to Map(String, String).
- [Phase 02]: [Phase 02]: HeaderToValue D-02 stale-schema-cache guard (sourceSchema field, reference comparison) is a verbatim template for the identical bug in HeadersToValue if a future plan touches it.
- [Phase 02]: HeadersToValue D-02 stale-schema-cache guard mirrors the 02-02 HeaderToValue fix exactly (sourceSchema field, reference comparison). — Both fork SMTs had the identical lazily-cached valueSchema bug; the fix shape is now consistent across both classes.
- [Phase 02]: Corrected a stale per-plan commit ledger (.git/gsd-plan-head-before-02-03) left over from a superseded milestone's differently-scoped phase 02-03. — The stale baseline (2026-09-13, pre-restructuring) would have inflated actuals.commits to 48 unrelated commits instead of this plan's real 3.
- [Phase 02]: Phase 02 Plan 04: Applied D-01 fix exactly as locked in CONTEXT.md - List/Map recursion branches in ConvertToMapOrJson.convertFieldValue(), fixing nested Struct-in-collection reflection bug. — Only the two required imports and two branches were added; no other production change.
- [Phase 02]: Phase 02 Plan 04: CONV-05 pins the ConfigException case-sensitivity quirk (output.json.library=JACKSON rejected) as-is per D-13, with an inline comment recording it is intentional, not a regression.

### Pending Todos

None yet.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper and makes ClickHouse-backed tests fail spuriously. Run Gradle tests as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Docker runs on colima profile `vz`; profile `default` is stopped.
- [GSD]: `workflow.test_command` and `workflow.build_command` are unset, and execute-phase's post-merge gate does not detect Gradle, so that gate currently runs nothing on this project. Executors ran `./gradlew test` themselves in Phase 1.
- [Phase 3]: Research flag: audit `DESCRIBE ... describe_include_subcolumns=1` output on `latest`, and establish RowBinary ground truth for `JSON`/`Dynamic`, before writing DESC-03 and TYPE-06.
- [Phase 4]: Research flag: check how client-v2 `0.9.6` handles an empty `jsonInsertColumns` list before writing COLS-02.
- [Phase 5]: The `gh` CLI is not installed locally, so CI runs must be checked through the GitHub web UI or API.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-09-16T18:32:48.600Z
Stopped at: Phase 02 complete, ready to plan Phase 3
Resume file: None
