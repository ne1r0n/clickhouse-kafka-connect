---
gsd_state_version: "1.0"
milestone: v1.6.0_gg1-sync
milestone_name: Upstream v1.6.0 Sync
current_phase: 08
current_phase_name: Orchestration & JMX Re-plumbing
status: executing
stopped_at: Phase 8 context gathered
last_updated: "2026-09-19T12:27:16.886Z"
last_activity: 2026-09-19
last_activity_desc: Phase 08 execution started
state_head: 340de46d0d3661f1def276eae1a795d887366811
progress:
  total_phases: 6
  completed_phases: 2
  total_plans: 10
  completed_plans: 7
  percent: 33
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-18)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 08 — Orchestration & JMX Re-plumbing

## Current Position

Phase: 08 (Orchestration & JMX Re-plumbing) — EXECUTING
Plan: 1 of 3
Status: Executing Phase 08
Last activity: 2026-09-19 — Phase 08 execution started

## Performance Metrics

**Velocity:**

- Total plans completed: 7 (all from `v1.2.9_gg4-tests`)
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 06 | 3 | - | - |
| 07 | 4 | - | - |
| 08 | 0 | - | - |
| 09 | 0 | - | - |
| 10 | 0 | - | - |
| 11 | 0 | - | - |

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

## Accumulated Context

### Decisions

Milestone-level decisions are locked in `REQUIREMENTS.md` (the "Locked decisions" table) and `PROJECT.md` → Key Decisions. Carried forward because they constrain how work must be done:

- The merge runs on scratch branch `sync/v1.6.0` with a `pre-v1.6.0-sync` tag on the pre-merge tip. `custom_build` is already published — it is only brought forward by a **non-force** merge once green.
- Prefer upstream wherever its implementation passes the fork's tests. The policy does **not** apply to JMX naming (upstream's version fails JMX-01/02) nor to the fork-only dependency pins (ClickHouse client `0.9.6`, Kafka Connect API `4.1.1`).
- Do not interleave `.planning/` commits with in-progress conflict resolution — a commit containing a `<<<<<<<` marker in tracked source is the SYNC-06 failure signal.
- Bug fixes live in the phase whose tests expose them — there is no separate bug-fix phase.
- A main-code change reaching outside a phase's declared scope stops for a user decision. The fork runs in production.
- Type coverage asserts typed SQL read-back, never `countRows`.
- The test JVM runs `file.encoding=windows-1252` on purpose. Any new reader of ClickHouse output must specify UTF-8 explicitly.
- client-v2 `GenericRecord` from a `Records` iterator is a mutable, reused row view — snapshot into a `Map` during iteration.
- New sink-task tests must create their tables before `start()` — `ClickHouseWriter.start()` fails against an empty database.
- [Phase 06]: Landed the 581-commit v1.6.0 merge as one mechanical commit (673d29c3) on scratch branch sync/v1.6.0, tagged from pre-v1.6.0-sync; all 40 conflicted paths verified byte-identical to upstream via a committed invariant script. — D-01/D-02: keeps every fork re-application in Phases 7-10 a reviewable diff against upstream instead of a judgement call inside a 500-file merge.
- [Phase 06]: Phase 06 Plan 02: Re-applied all six fork build.gradle.kts additions (JaCoCo report-only, testUnit + check-exclusion guard, JDK-17 test-JVM launcher + windows-1252 encoding, LZ4 capability resolution, ben-manes dependencyUpdates, Spotless config) onto upstream v1.6.0's version-catalog build file; landed VERSION=v1.6.0_gg1 and the .gitignore/tests.yaml settlement; proved all four D-12 resolved-version facts against the live dependency graph. — Keeps every fork re-application a reviewable diff against upstream, per D-01/D-02; the gate (help+dependencies) is the only pass/fail signal for Phase 6.
- [Phase 06]: [Phase 06]: Phase 06 Plan 03: Generated .planning/MERGE-CHECKLIST.md (40-row Section 1 from conflicted-paths.txt, 8-row Section 2, Recorded Facts closing SYNC-04/SYNC-05); flipped Phase 6's 5 build/infra rows to done; re-proved D-02/SYNC-06 invariants across the full 588-commit range. Corrected a research-prose error about Processing.java's merge status and a plan verify-block premise about merge-commit counts (both documented as deviations, no repo state altered). — Turns the merge's conflict surface into a per-path gate Phases 7-11 flip inside their own commits, with Phase 11 asserting zero pending rows before publication; closes SYNC-03/04/05/06, completing Phase 6's 14 requirements.
- [Phase 07]: Phase 07 Plan 01: Skipped tree-wide spotlessApply (unrelated 52-file reformat since Phase 6 merge); verified the 4 edited files independently pass spotlessCheck instead. — Running spotlessApply would violate the plan's own scope boundary (MUST NOT touch other lines of ClickHouseSinkTask.java) and break its single-hunk-diff acceptance criterion.
- [Phase 07]: Phase 07 Plan 02: Re-threaded targetTableFilter server-side table filtering (config key + buildTableFilterClause + builder wiring) and re-expressed shouldSkipDescribeRow over a loop-local Map<String, Column> since upstream's Table is now immutable; pinned describeTableV2's reader to UTF-8. — D-08/D-16/D-19: keeps every fork re-application a reviewable diff against upstream, matches the locked map-based signature, and closes two of the five silently-vanishing fork behaviours before Phase 11's live audit.
- [Phase 07]: Phase 07 Plan 03: Landed the remaining fork config settings (jsonInsertColumns key, explicit-format flag, async_insert un-forcing) and the planned four-item Phase 10 test carve-out, then discovered and Rule-3-fixed a much larger compile gap: twelve additional fork test files depended on pre-merge ClickHouseBase/ClickHouseTestHelpers methods (createProps/createClient/createTable) the mechanical merge silently dropped, undetected by planning's D-05 blast-radius measurement. — Every redirect targets an already-existing equivalent (getBaseProps, ClickHouseTestHelpers.createClient, a local queryV2-based createTable helper) so D-06 and D-09's locked restrictions on ClickHouseBase/ClickHouseTestHelpers hold exactly as decided; two files (MBeanNameUnitTest.java needs Phase 8's buildMBeanName, ClickHouseTestHarnessTest.java needs Phase 10's shared-container strategy) remain deliberately uncompiled and are flagged as a CRITICAL blocker for plan 07-04's testUnit gate.
- [Phase 07]: Phase 07 Plan 04: Pinned all thirteen new upstream ClickHouseSinkConfig options at their defaults (NewUpstreamConfigDefaultsUnitTest), fixed a test-construction NPE in RowBinaryUnsupportedTypeGateTest's JSON validateDataSchema case, landed a user-approved build.gradle.kts test-compile exclusion for two future-phase files, and ran testUnit green with Docker genuinely stopped (300 tests, 0 failures); flipped Phase 7's twelve merge-checklist rows and recorded the two free wins (map null-marker guard confirmed with WR-01 stated open, describe-table database fix confirmed). — CFG-03/WRT-04/FREE-02/FREE-03 close out Phase 7's 17 requirements; the checklist flips keep Phase 11's zero-pending publication gate meaningful.
- [Phase 07]: Phase 07 gap-closure (post-execution, commits `2c6a0f56` `92c9cece` `a6141221` `e411353c` `95e49964`): the orchestrator ran the full live suite — the first execution of anything since the 581-commit merge, because the test tree did not compile until plan 07-03 — and got 12 failures. Three belonged to Phase 7 and were fixed, along with both Critical code-review findings. **`targetTableFilter` was appended AFTER `group by` instead of into the `where` clause, so every non-empty filter threw `ILLEGAL_TYPE_OF_ARGUMENT` — the behaviour was entirely broken, not merely mis-escaped**, and `buildTableFilterClause` additionally allowed a `x\' OR 1=1-- ` backslash-before-quote breakout into the `system.columns` discovery query. Also null-guarded `doWriteColValue`'s MAP/TUPLE/VARIANT cases (an NPE mid-RowBinary-stream on `Array(Nullable(Tuple(...)))`) and `validateDataSchema`'s default branch. Suite went 12 → 7 failures, all 7 carry-ins outside Phase 7. — Phase 7's own acceptance gate (`testUnit`) is Docker-free by design and structurally could not have caught any of these; the live run is what proved the re-applied behaviours. Worth repeating at the end of Phases 8 and 10 rather than trusting a green `testUnit`.

### Pending Todos

- `REQUIREMENTS.md`'s header coverage count said 46 v1 requirements; the document actually lists **53**. Corrected in the Traceability block at roadmap creation — no requirement was lost, the summary count was simply wrong.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (already in `config.json`'s `build_command`/`test_command`). Docker runs on colima profile `vz`.
- [Merge]: ⚠ **Five fork behaviours vanish with no conflict marker and no compile error** unless deliberately re-applied — JMX `connector=<name>`, `async_insert` un-forcing, the UTF-8 `describeTable` readers, `targetTableFilter` + the sub-column skip, and `ClickHouseBase`'s `@Tag`/shared-container/MBean-sweep contract. Each has a named test as its canary (Phases 7, 8, 10).
- [Prod]: ⚠ **Unfixed production bug, pinned only.** R-01: `insertFormat=json` + a `Struct` record + default `bypassRowBinary` silently discards every row. Its keep/drop disposition is Phase 9's blocking-human checkpoint — it must not be decided by accident.
- [Merge]: `ClickHouseTestHelpers.java` is a modify/delete that git resolves by orphaning the fork's copy — post-merge the tree can hold two compiling copies with divergent defaults. Grep for exactly one canonical file (HARN-04, Phase 10).
- [Tooling]: The `gh` CLI is not installed locally (deliberately — D-11), so CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel, so any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch. Confirmed live in Phase 7: the wave-2 dispatch was rejected by the isolation guard after intervening `gsd_run check` calls re-persisted the sentinel to `harness-worktree`; re-forcing immediately before each `Agent()` call is the working remedy.
- [Phase 06]: ⚠ **CR-01, latent — safety-net script fails open.** `.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep` error (exit ≥2) identically to "no match" (exit 1), with stderr suppressed, so Invariant 2 reports clean on a broken pathspec or unreadable commit. The property itself currently holds — re-derived at the Phase 6 security audit with corrected exit-code branching (592 commits, 0 markers, 0 git errors) — but **fix before Phase 11**, which gates publication on this script running unattended. Recorded in `06-SECURITY.md` → Carried Defects.
- [Phase 07 → 8/10]: **RESOLVED BY USER DECISION, but both halves must be undone by their owning phase.** `compileTestJava` failed on two files after plan 07-03 — `MBeanNameUnitTest.java` (needs `ProxySinkTask.buildMBeanName`, Phase 8) and `ClickHouseTestHarnessTest.java` (needs `ClickHouseBase.sharedContainerStarts()`, Phase 10). The user chose a temporary test-compile exclusion in `build.gradle.kts` (commit `b25a71f1`) over pulling either sliver forward. Both files remain on disk byte-for-byte unmodified. **Phase 8 must delete its own `exclude("**/MBeanNameUnitTest.java")` line; Phase 10 must delete `exclude("**/ClickHouseTestHarnessTest.java")`.** Leaving either in place silently drops a canary test from the suite.
- [Phase 07 → 10]: ⚠ **UTF-8 regression in the orphaned test helper — 4 live tests failing.** `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` lines ~140 (`getAllRowsAsJson`), ~158 (`getAllRowsAsJsonCloud`) and ~318 use `new InputStreamReader(stream)` with **no charset**, so UTF-8 bytes decode under the test JVM's deliberate `file.encoding=windows-1252` and non-ASCII values come back as mojibake (`привет` → `Ð¿Ñ€Ð¸Ð²ÐµÑ‚`). The fork's explicit UTF-8 was dropped by the v1.6.0 merge — the calling line in `JsonDynamicRoundTripTest` still says "read-back through the UTF-8-safe helper". Fails `JsonDynamicRoundTripTest` TYPE-07 ×2 and `SmtSinkLandingTest` HDRS-09/CONV-08. Three-line fix, owned by Phase 10 / HARN-04.
- [Phase 07 → 8]: `MBeanRegistrationLiveTest` JMX-04 ×2 fail with NPE — the fork's JMX `connector=<name>` naming is not yet re-applied. Expected; Phase 8's canary firing as designed.
- [Phase 07 → untriaged]: `TableTest.extractNullables()` fails (expected 3, got 2). `Table.java` is byte-identical to upstream v1.6.0 and was explicitly out of Phase 7's scope, so this is pre-existing upstream behaviour meeting a fork expectation. Needs triage in whichever phase owns `Table`/mapping coverage.
- [Phase 07]: **3 Warning + 3 Info code-review findings left open** in `07-REVIEW.md` (both Criticals were fixed). Warnings: `Column.extractColumn`'s Array recursion swaps the `hasDefaultValue`/`isSubColumn` positional args (currently dead code, future landmine); `ClickHouseSinkConfig`'s `ConfigDef` default for `USERNAME` uses `passwordDefault` ("") instead of `usernameDefault` ("default") — misreports via Connect's config UI/docs, runtime unaffected; `JsonColumnTest` asserts row counts only, never reading back payload content.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

The nine follow-ups carried from `v1.2.9_gg4-tests` are out of scope for this milestone by explicit decision — see `REQUIREMENTS.md` → Future Requirements. Two (client-V2 error classification, `Map(K, Nullable(V))`) close for free and are recorded, not worked (FREE-01, FREE-02).

## Session Continuity

Last session: 2026-09-19T11:04:40.094Z
Stopped at: Phase 8 context gathered
Resume file: .planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md

## Operator Next Steps

- Plan the next phase with `/gsd-plan-phase 7`
