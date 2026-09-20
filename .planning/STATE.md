---
gsd_state_version: "1.0"
milestone: v1.6.0_gg1-sync
milestone_name: Upstream v1.6.0 Sync
current_phase: 10
current_phase_name: Test Harness Reconciliation
status: executing
stopped_at: Phase 10 context gathered
last_updated: "2026-09-20T13:59:08.087Z"
last_activity: 2026-09-20
last_activity_desc: Phase 09 complete, transitioned to Phase 10
state_head: cb0b8bae0ce4c4f02143310c67e2403cc85348ad
progress:
  total_phases: 6
  completed_phases: 4
  total_plans: 16
  completed_plans: 13
  percent: 67
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-20)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 10 — Test Harness Reconciliation

## Current Position

Phase: 10 (Test Harness Reconciliation) — READY TO EXECUTE
Plan: Not started
Status: Ready to execute
Last activity: 2026-09-20 — Phase 09 complete, transitioned to Phase 10

## Performance Metrics

**Velocity:**

- Total plans completed: 13 (all from `v1.2.9_gg4-tests`)
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 06 | 3 | - | - |
| 07 | 4 | - | - |
| 08 | 3 | - | - |
| 09 | 3 | - | - |
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
| Phase 09 P01 | 25 min | 4 tasks | 2 files |
| Phase 09 P02 | 20 min | 3 tasks | 4 files |
| Phase 09 P03 | ~15min | 3 tasks | 2 files |

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

- [Phase 08]: Re-plumbed JMX `connector=<name>` into upstream's new naming owner `SinkTaskStatistics` (not back into the fork's old `ProxySinkTask.buildMBeanName` home), with the per-topic MBean composing from the task-level name; un-excluded `MBeanNameUnitTest`; UTF-8'd the three charset-less test-helper readers; discharged ORCH-01/ORCH-04/FREE-01 on recorded evidence rather than new code. Suite: 636 tests, 1 failure, 0 errors, 35 explained skips against a deliberately re-baselined gate. — Threading the name into upstream's owner shrinks the fork delta so the next sync does not re-fight this conflict; the three evidence-only requirements were documentation acts the plans' own prohibitions forbade pinning with tests.
- [Phase 08]: UAT 16/16 passed (10 human checkpoints + 6 auto-covered); both human-verification items resolved and applied — `MERGE-CHECKLIST.md`'s ProcessingTest row wording corrected, and ORCH-01/ORCH-04/FREE-01 flipped to Complete in `REQUIREMENTS.md`. Security: 13 threats, 0 open (`08-SECURITY.md`). — The two doc edits invalidated `08-VERIFICATION.md`'s `covered_digest`; it was recomputed after confirming `git diff 911a3160..HEAD` touched planning documents only, with the reason recorded in the verification file itself.
- [Phase 09]: [Phase 09]: Plan 09-01 re-applied the metadata-free insertFormat=json bypass (D-01 KEEP, re-confirmed at Task 1's checkpoint) as four early-return guards behind one shared predicate shouldSkipTableMetadata(), rebuilt the synthetic Table against upstream's immutable five-argument constructor, and re-attached jsonInsertColumns to the surviving five-argument client.insert overload in doInsertJsonV2. — Operator facts 2/3 mean no upstream configuration substitutes for the bypass (suppressTableExistenceException still calls describeTable; enableDbTopicSplit still runs updateMapping; bypassFieldCleanup covers only one of three effects) — KEEP is the only way to serve a connector user who cannot DESCRIBE/SHOW the target tables. Docker-free testUnit canary (MetadataFreeJsonInsertUnitTest, 11 tests) now pins all four guard sites and the overload branch so a future merge cannot silently drop them again.
- [Phase 09]: [Phase 09]: Plan 09-02 re-enabled all 13 parked live JSON-path test methods (delete-only @Disabled removal, D-10) and adopted the TableTest.extractNullables() carry-in fix (D-14); full env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test run: 643 tests, 0 failures, 0 errors, 18 skipped (down from Phase 8's 35). — No newly-enabled method needed investigation -- all 13 passed on first live execution since the 581-commit merge, including R-01's labelled pin, proving plan 09-01's guards did not widen the bypass or damage non-JSON connectors.
- [Phase 09]: Plan 09-03 discharged JSONP-04 by reading the resolved ClickHouseWriter.java diff against v1.6.0 (three findings: five-arg-only Table construction, no mutable-Table assumptions on the JSON path, guards-only Phase 9 additions), and recorded Phase 9's bookkeeping: REQUIREMENTS.md's JSONP-03 correction blockquote, all four JSONP requirements closed, MERGE-CHECKLIST.md row 26 annotated, Recorded Fact 13 resolved (citing commit b84b179e), and new Recorded Fact 14 stating the KEEP disposition's rationale, operator facts, accepted cost and conflict-policy departure. — ROADMAP criterion 4 requires JSONP-04 evidence from reading the diff, not a green test; criterion 1 requires a recorded decision with rationale. MERGE-CHECKLIST.md's own closing sentence stated no disposition was recorded anywhere until this plan supplied one.
- [Phase 09]: Code review found CR-01 (Critical) — `doInsert`'s `csc.isAutoEvolve()` branch was the one guard site plan 09-01 missed, so `auto.evolve=true` + explicit `insertFormat=json` would `ALTER TABLE` off the synthetic Table's empty column list and then fall through to a direct `describeTable`. Escalated to UAT as the phase's single human-verification item; user chose fix-now over scheduling it. Guarded with the same shared predicate (`51baea62`) plus two pin tests; WR-01's stale `Disabled pending…` Javadoc refreshed (`99908525`). Re-verification on the fixed tree: passed, 4/4 must-haves, 645 tests / 0 failures / 0 errors / 18 skipped. — The gap undermined KEEP's own rationale: the bypass exists to avoid exactly the metadata query `auto.evolve` was about to issue on every batch. A fifth guard site that no must-have in any of the three plans covered, which is why only code review caught it.

### Pending Todos

- `REQUIREMENTS.md`'s header coverage count said 46 v1 requirements; the document actually lists **53**. Corrected in the Traceability block at roadmap creation — no requirement was lost, the summary count was simply wrong.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` (already in `config.json`'s `build_command`/`test_command`). Docker runs on colima profile `vz`.
- [Merge]: ⚠ **Six fork behaviours vanish with no conflict marker and no compile error** unless deliberately re-applied. Five are now re-applied and canary-green: `async_insert` un-forcing, the UTF-8 `describeTable` readers, `targetTableFilter` + the sub-column skip (all Phase 7), JMX `connector=<name>` (Phase 8), and the metadata-free `insertFormat=json` bypass + `jsonInsertColumns` re-attachment (Phase 9, D-11 — `MetadataFreeJsonInsertUnitTest` is the Docker-free canary, 13 tests). The bypass is the sixth entry precisely because it *just* vanished this way in the merge, and the pre-existing Docker-free coverage would not have caught it. **One remains: `ClickHouseBase`'s `@Tag`/shared-container/MBean-sweep contract — Phase 10.**
- [Prod]: ⚠ **Unfixed production bug, pinned only — disposition now decided.** R-01: `insertFormat=json` + a `Struct` record + default `bypassRowBinary` silently discards every row. Phase 9's blocking-human checkpoint resolved it: **KEEP the bypass** (D-01), so the loss is re-introduced knowingly as an accepted cost and stays pinned, not fixed. The `PIN - NOT AN ENDORSEMENT` Javadoc is byte-identical. Operator fact 2 — no live pipeline takes the `Struct` + explicit-JSON route today — is what makes that acceptable; it stops being acceptable the moment one does. The fail-fast fix remains an Active follow-up needing its own plan.
- [Merge]: `ClickHouseTestHelpers.java` is a modify/delete that git resolves by orphaning the fork's copy — post-merge the tree can hold two compiling copies with divergent defaults. Grep for exactly one canonical file (HARN-04, Phase 10).
- [Tooling]: The `gh` CLI is not installed locally (deliberately — D-11), so CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel, so any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch. Confirmed live in Phase 7: the wave-2 dispatch was rejected by the isolation guard after intervening `gsd_run check` calls re-persisted the sentinel to `harness-worktree`; re-forcing immediately before each `Agent()` call is the working remedy.
- [Phase 06]: ⚠ **CR-01, latent — safety-net script fails open.** `.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep` error (exit ≥2) identically to "no match" (exit 1), with stderr suppressed, so Invariant 2 reports clean on a broken pathspec or unreadable commit. The property itself currently holds — re-derived at the Phase 6 security audit with corrected exit-code branching (592 commits, 0 markers, 0 git errors) — but **fix before Phase 11**, which gates publication on this script running unattended. Recorded in `06-SECURITY.md` → Carried Defects.
- [Phase 10]: **Half done — Phase 8 closed its half, Phase 10 still owes its own.** `compileTestJava` failed on two files after plan 07-03; the user chose a temporary test-compile exclusion in `build.gradle.kts` (commit `b25a71f1`) over pulling either sliver forward. ✓ Phase 8 deleted `exclude("**/MBeanNameUnitTest.java")` — the canary now compiles and is green inside `testUnit`. **Phase 10 must still delete `exclude("**/ClickHouseTestHarnessTest.java")`** (needs `ClickHouseBase.sharedContainerStarts()`); leaving it in place silently drops that canary from the suite.
- ~~[Phase 07 → 10]: UTF-8 regression in the orphaned test helper — 4 live tests failing~~ — **RESOLVED in Phase 8 (plan 08-01, D-13).** All three `InputStreamReader` constructions in `src/testFixtures/.../ClickHouseTestHelpers.java` (lines 141, 160, 321) now pass `StandardCharsets.UTF_8` explicitly, so non-ASCII values no longer decode as mojibake under the deliberate `windows-1252` test JVM. `JsonDynamicRoundTripTest` TYPE-07 ×2 and `SmtSinkLandingTest` HDRS-09/CONV-08 are green. Note Phase 10 / HARN-04 still owns the separate duplicate-helper-file question.
- ~~[Phase 07 → 8]: `MBeanRegistrationLiveTest` JMX-04 ×2 NPE~~ — **RESOLVED in Phase 8.** The JMX `connector=<name>` naming is re-applied into `SinkTaskStatistics`; `MBeanRegistrationLiveTest` now passes at both task and per-topic level, including the four-tasks-two-connectors selective-unregistration path.
- ~~[Phase 10 (triaged in Phase 8)]: `TableTest.extractNullables()` fails (expected 3, got 2)~~ — **RESOLVED in Phase 9 (D-14, commit `b84b179e`).** Phase 8 named the cause — `ClickHouseHelperClient.shouldSkipDescribeRow` unconditionally strips Nullable `.null` subcolumns, a fork behaviour already pinned by `DESC-01` — and Phase 9 adopted the carry-in and changed the assertion `3` → `2` with that citation. Taken outside `JSONP-01..04` at the user's explicit decision, so the acceptance gate became a clean binary instead of a re-baselined "one known red". MERGE-CHECKLIST Recorded Fact 13 flipped to resolved. The suite is now 0 failures, 0 errors.
- [Phase 07]: **3 Warning + 3 Info code-review findings left open** in `07-REVIEW.md` (both Criticals were fixed). Warnings: `Column.extractColumn`'s Array recursion swaps the `hasDefaultValue`/`isSubColumn` positional args (currently dead code, future landmine); `ClickHouseSinkConfig`'s `ConfigDef` default for `USERNAME` uses `passwordDefault` ("") instead of `usernameDefault` ("default") — misreports via Connect's config UI/docs, runtime unaffected; `JsonColumnTest` asserts row counts only, never reading back payload content.
- [Phase 09]: **1 Info code-review finding left open** in `09-REVIEW.md` (the Critical and the Warning were both fixed). P9-IN-01: `cleanupExtraFields`'s guard reads `csc.isBypassFieldCleanup() || shouldSkipTableMetadata()`, dereferencing `csc` *before* the predicate whose stated purpose is a `csc != null` short-circuit — so the null-safety guarantee documented on `shouldSkipTableMetadata()` does not hold at that call site. Unreachable today (only called through a started writer). Fix is to swap the operands.
- [Security]: Phase 09 has **no `09-SECURITY.md`** while `workflow.security_enforcement` is `true`. Phases 6 and 8 have one; 7 and 9 do not. Run `/gsd-secure-phase 09` (and `07`) before Phase 11's publication gate, or record the omission deliberately.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

The nine follow-ups carried from `v1.2.9_gg4-tests` are out of scope for this milestone by explicit decision — see `REQUIREMENTS.md` → Future Requirements. Two (client-V2 error classification, `Map(K, Nullable(V))`) close for free and are recorded, not worked (FREE-01, FREE-02).

## Session Continuity

Last session: 2026-09-20T12:55:50.004Z
Stopped at: Phase 10 context gathered
Resume file: .planning/phases/10-test-harness-reconciliation/10-CONTEXT.md

## Operator Next Steps

- Discuss the next phase with `/gsd-discuss-phase 10` (no `10-CONTEXT.md` yet), then `/gsd-plan-phase 10`
- Phase 10 owes the last of the six silently-vanishing fork behaviours: `ClickHouseBase`'s `@Tag`/shared-container/MBean-sweep contract, plus deleting `exclude("**/ClickHouseTestHarnessTest.java")` from `build.gradle.kts` and settling the duplicate `ClickHouseTestHelpers.java` question (HARN-04)
- Optional before Phase 11: `/gsd-secure-phase 09` and `/gsd-secure-phase 07` — both phases lack a SECURITY.md while security enforcement is on
