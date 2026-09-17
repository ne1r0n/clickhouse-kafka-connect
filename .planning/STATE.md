---
gsd_state_version: "1.0"
current_phase: 05
current_phase_name: CI & Final Verification
status: executing
stopped_at: Phase 5 context gathered
last_updated: "2026-09-17T23:35:54.953Z"
last_activity: 2026-09-18
last_activity_desc: Phase 04 complete, transitioned to Phase 5
state_head: a360e762200f06c95d9d479724dfed0b4e7c6070
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 16
  completed_plans: 14
  percent: 80
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-18)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 05 — CI & Final Verification

## Current Position

Phase: 05 (CI & Final Verification) — READY TO EXECUTE
Plan: Not started
Status: Ready to execute
Last activity: 2026-09-18 — Phase 04 complete, transitioned to Phase 5

Progress: [████████░░] 80%

## Performance Metrics

**Velocity:**

- Total plans completed: 14
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 3 | - | - |
| 02 | 5 | - | - |
| 03 | 3 | - | - |
| 04 | 3 | - | - |

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
- [Phase 04]: [Phase 4] R-01 escalation resolved at the 04-01 Task 4 blocking-human checkpoint as pin-current-behavior: insertFormat=json + Struct record + bypassRowBinary unset silently drops all rows (put() returns without throwing, 0 rows land); pinned by one loudly-commented live test, no production fix made (D-01/D-07).
- [Phase 04]: [Phase 4] JSON-04's all-tombstone batch through doInsertJsonV2 empirically confirmed: an empty JSONEachRow body is accepted by ClickHouse latest as a clean zero-row insert with no throw, matching the research session's inference.
- [Phase 04]: [Phase 04] COLS-05's non-retriable classification is correct but for a different structural reason than assumed: Utils.handleException's ClickHouseException-typed switch never matches com.clickhouse.client.api.ServerException (the client-V2 exception type), so the classification comes from the unconditional fallback path, not from the switch's case list excluding error code 16 (NO_SUCH_COLUMN_IN_TABLE).
- [Phase 04]: [Phase 04] COLS-02 confirmed as a plain confirmatory pin per D-11 - Phase 3's JsonDynamicRoundTripTest already exercised doInsertJsonV2 with the default empty jsonInsertColumns list.
- [Phase 04]: [Phase 04] buildMBeanName extracted byte-for-byte per D-02 (the phase's only pre-approved main-code change); ProxySinkTask.java is the only src/main file touched anywhere in Phase 4, verified by blob-hash pins on MBeanServerUtils.java, ClickHouseSinkConfig.java, ClickHouseWriter.java and build.gradle.kts at every task's verify gate.
- [Phase 04]: [Phase 04] JMX-04's four-task live test identifies MBean identity via before/after Set<ObjectName> difference rather than any absolute SinkTask<id> name, because ProxySinkTask.id comes from a static AtomicInteger that never resets inside a test JVM fork.
- [Phase 04]: [Phase 04] Phase 4 complete: all three plans (04-01 JSON insert path, 04-02 jsonInsertColumns, 04-03 JMX + async_insert) done; full ./gradlew test is green (371 tests, 0 failed, 7 pre-existing skips).

### Pending Todos

None yet.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper and makes ClickHouse-backed tests fail spuriously. Run Gradle tests as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Docker runs on colima profile `vz`; profile `default` is stopped.
- [GSD]: ~~`workflow.test_command`/`workflow.build_command` unset~~ — RESOLVED in Phase 3. Both are now set in `.planning/config.json` to `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` / `... assemble`, with `workflow.test_gate_timeout=2400` (the full suite takes ~4.5 min and the 600s default was too tight). The post-merge and regression gates now run the real suite instead of `true`.
- [GSD]: Worktree isolation auto-degrades to sequential execution (#683) while `custom_build` has unpushed commits, because the harness forks worktrees off `origin/HEAD`. Expect one-plan-at-a-time waves until the branch is pushed. Also: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel as a side effect, so the degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Phase 4]: ~~Research flag: check how client-v2 `0.9.6` handles an empty `jsonInsertColumns` list before writing COLS-02.~~ — RESOLVED. Withdrawn by D-11 and confirmed in Phase 4: the default empty list inserts correct values (COLS-02 is a plain confirmatory pin).
- [Phase 4]: ⚠ **Unfixed production bug, pinned only.** `insertFormat=json` + a `Struct` record + `bypassRowBinary` at its default `false` silently discards every row (no throw, `Written Rows: [0]`). Pinned by a labelled test at the user's direction; the fail-fast fix is not pre-approved (D-01/D-07) and needs its own plan.
- [Phase 4]: ⚠ **Retriable ClickHouse errors are likely not retried.** `Utils.handleException`'s `instanceof ClickHouseException` switch never matches under client V2 — the real type is `com.clickhouse.client.api.ServerException` — so the whole retriable/non-retriable error-code table is unreachable and every error falls through to the unconditional fallback. Surfaced while pinning COLS-05; not a Phase 4 regression, and out of this project's declared scope.
- [Phase 5]: The `gh` CLI is not installed locally, so CI runs must be checked through the GitHub web UI or API.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-09-17T22:23:36.574Z
Stopped at: Phase 5 context gathered
Resume file: .planning/phases/05-ci-final-verification/05-CONTEXT.md
