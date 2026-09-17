---
gsd_state_version: "1.0"
current_phase: 4
current_phase_name: JSON Insert Path & Connector Plumbing
status: planning
stopped_at: Phase 03 complete, ready to plan Phase 4
last_updated: "2026-09-17T09:51:39.528Z"
last_activity: 2026-09-17
last_activity_desc: Phase 03 complete, transitioned to Phase 4
state_head: 50e4701eb91d1aa3a1510d71ecb750b588fbf2b1
progress:
  total_phases: 5
  completed_phases: 3
  total_plans: 11
  completed_plans: 11
  percent: 60
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-17)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 4 — JSON Insert Path & Connector Plumbing

## Current Position

Phase: 4 — JSON Insert Path & Connector Plumbing
Plan: Not started
Status: Ready to plan
Last activity: 2026-09-17 — Phase 03 complete, transitioned to Phase 4

Progress: [██████░░░░] 60%

## Performance Metrics

**Velocity:**

- Total plans completed: 11
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 3 | - | - |
| 02 | 5 | - | - |
| 03 | 3 | - | - |

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
- [Phase 02]: HeaderToValue D-02 stale-schema-cache guard (sourceSchema field, reference comparison) is a verbatim template for the identical bug in HeadersToValue if a future plan touches it.
- [Phase 02]: HeadersToValue D-02 stale-schema-cache guard mirrors the 02-02 HeaderToValue fix exactly (sourceSchema field, reference comparison). — Both fork SMTs had the identical lazily-cached valueSchema bug; the fix shape is now consistent across both classes.
- [Phase 02]: Corrected a stale per-plan commit ledger (.git/gsd-plan-head-before-02-03) left over from a superseded milestone's differently-scoped phase 02-03. — The stale baseline (2026-09-13, pre-restructuring) would have inflated actuals.commits to 48 unrelated commits instead of this plan's real 3.
- [Phase 02]: Phase 02 Plan 04: Applied D-01 fix exactly as locked in CONTEXT.md - List/Map recursion branches in ConvertToMapOrJson.convertFieldValue(), fixing nested Struct-in-collection reflection bug. — Only the two required imports and two branches were added; no other production change.
- [Phase 02]: Phase 02 Plan 04: CONV-05 pins the ConfigException case-sensitivity quirk (output.json.library=JACKSON rejected) as-is per D-13, with an inline comment recording it is intentional, not a regression.
- [Phase 02]: Code review (0 critical / 3 warning / 2 info) surfaced three follow-ups now tracked in PROJECT.md Active: end-to-end coverage for an actual null entry in a Nullable map column (WR-01), fail-loud on a null in a non-nullable MAP/ARRAY element slot (WR-02), and a pinned contract for HeaderToValue's non-STRING column collision (WR-03). None block Phase 2; all are unscheduled.
- [Phase 03]: Phase 03 Plan 01: TYPE-07 identifier test reproduced the deferred Phase 1 D-11 reader bug; shipped conditional StandardCharsets.UTF_8 fix on both describeTable readers.
- [Phase 03]: Phase 03 Plan 01: Fixed ClickHouseBase.createClient() test-harness gap (missing targetTableFilter wiring) as a Rule 3 blocking-issue deviation, mirroring the existing ClickHouseWriter.start() production idiom.
- [Phase 03]: Phase 03 Plan 02: Used HeaderToValue (singular) instead of HeadersToValue for the TYPE-07 SMT header case, matching the String-column requirement exactly. — HeadersToValue always produces a Map<String,String> of all headers; HeaderToValue maps one header directly to a named String field.
- [Phase 03]: Phase 03 Plan 02: ClickHouse JSON-type columns reject a bare scalar at the root (Code 117, JSON object should start with '{') - TYPE-07 non-empty json_col fixtures wrap the value under a synthetic key. — Live-verified this session; the empty-object edge stays a true root {}.
- [Phase 03]: Phase 03 Plan 02: client-v2 GenericRecord returned by Records' iterator is a mutable, reused row view - test helpers must snapshot row values into a Map during iteration, never collect GenericRecord references into a list. — Live-verified this session via a full false-positive test-pass/failure cycle; documented inline as a Javadoc on the allRows helper for future test authors.
- [Phase 03]: Phase 03 Plan 03: doWriteColValue default branch and validateDataSchema now share one private message-builder helper so the D-05 actionable message (naming column, type, insertFormat=json, bypassRowBinary=true) cannot drift between the two RowBinary fail-fast gates.
- [Phase 03]: Phase 03 Plan 03: Fixed a self-introduced bug in the new live test's own Struct fixture (raw String json_col value rejected by ClickHouse's JSON type) before any commit existed - not a production bug, no Rule 1-4 classification needed.
- [Phase 03]: Code review (0 critical / 3 warning / 1 info) surfaced three follow-ups now tracked in PROJECT.md Active: `shouldSkipDescribeRow` only walks the immediate parent so a multi-level JSON/Dynamic subcolumn path leaks as log noise (P3-WR-01); `describeTableV2` uses `this.database` instead of its `database` parameter, dormant because every caller passes the same value (P3-WR-02); and `validateDataSchema`'s gate is narrower than `doWriteColValue`'s catch-all, leaving INT128/INT256/UINT128/UINT256 on the old bare message (P3-WR-03). The verifier independently re-confirmed all three against source and ruled none of them blocking — P3-WR-03 in particular is unchanged from pre-Phase-3 behavior, so it is not a regression. None block Phase 4; all are unscheduled.

### Pending Todos

None yet.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper and makes ClickHouse-backed tests fail spuriously. Run Gradle tests as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Docker runs on colima profile `vz`; profile `default` is stopped.
- [GSD]: ~~`workflow.test_command`/`workflow.build_command` unset~~ — RESOLVED in Phase 3. Both are now set in `.planning/config.json` to `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` / `... assemble`, with `workflow.test_gate_timeout=2400` (the full suite takes ~4.5 min and the 600s default was too tight). The post-merge and regression gates now run the real suite instead of `true`.
- [GSD]: Worktree isolation auto-degrades to sequential execution (#683) while `custom_build` has unpushed commits, because the harness forks worktrees off `origin/HEAD`. Expect one-plan-at-a-time waves until the branch is pushed. Also: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel as a side effect, so the degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Phase 4]: Research flag: check how client-v2 `0.9.6` handles an empty `jsonInsertColumns` list before writing COLS-02.
- [Phase 5]: The `gh` CLI is not installed locally, so CI runs must be checked through the GitHub web UI or API.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-09-17T09:29:17.961Z
Stopped at: Phase 03 complete, ready to plan Phase 4
Resume file: None
