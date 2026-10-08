---
gsd_state_version: "1.0"
milestone: v1.7.0_gg1-sync
milestone_name: Upstream v1.7.0 Sync & Follow-up Hardening
current_phase: 13
current_phase_name: Fork Hardening on the Merged Tree
status: planning
stopped_at: Phase 13 context gathered
last_updated: "2026-10-08T13:49:00.330Z"
last_activity: 2026-10-08
last_activity_desc: Phase 12 complete, transitioned to Phase 13
state_head: 43f4f60cec704b15d9b2d4d5aaf7e7bdcfd05bc6
progress:
  total_phases: 4
  completed_phases: 1
  total_plans: 5
  completed_plans: 5
  percent: 25
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-08 after Phase 12)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 13 — Fork Hardening on the Merged Tree

## Current Position

Phase: 13 — Fork Hardening on the Merged Tree
Plan: Not started
Status: Ready to plan
Last activity: 2026-10-08 — Phase 12 complete, transitioned to Phase 13

Progress: [███░░░░░░░] 25%

## Performance Metrics

**Velocity:**

- Total plans completed this milestone: 0 (`v1.6.0_gg1-sync`: 24; `v1.2.9_gg4-tests`: 16)
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 12 | 5 | - | - |
| 13 | TBD | - | - |
| 14 | TBD | - | - |
| 15 | TBD | - | - |

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

`v1.7.0_gg1-sync` locked decisions live in `REQUIREMENTS.md` (decisions table at the top), and plans do not re-open them: sync by `git merge v1.7.0` (tag `379b8d01`, never bare `upstream/main`); D-17 retired onto upstream's `setCompressionMethod`, including its unconditional `compress=false`; R-01 fixed by **routing**, not by throwing, with wrong logical-type values fixed; CR-02 pins may be re-pinned to the correct bytes; the five dependency bumps, with lz4-java targeting 1.12.0; publication in scope; sync first, hardening on top.

Roadmap decisions (2026-10-07):

- Four phases (12-15), coarse, in the research's order.
- The merge-line decisions (CMP, WRT-06/09, BLD-09/13, EOS-01) live in Phase 12 rather than in hardening.
- Phase 13 is kept whole despite owning 15 requirements, because its writer items serialise on `ClickHouseWriter.java` anyway.
- CI-08 lands in Phase 14 and is proven end to end by Phase 15's published-SHA run.
- [Phase 12]: 12-01: invariant 0 accepts a merge-commit rollback tag whose every parent is an ancestor of MERGE^1 (pre-v1.7.0-sync = c61d90ce is the --no-ff merge, not an ancestor of sync/v1.6.0-derived branches)
- [Phase 12]: 12-01: merge commit M = 54e63648 (v1.7.0 into sync/v1.7.0), zero authored lines; fork-anchors.txt (39 rows) authored after M from measured counts
- [Phase 12]: WRT-09 delivered as evidence plus new pins: the only MAP CR-02 pin is nullable and keeps {0x01}; non-nullable #820 pins ({0x00}, {0x00,0x00}) added beside it; awaiting user confirmation of the interpretation
- [Phase 12]: MAP null guard scoped to Nullable columns (WRT-06); nested null Maps fall to upstream's mapTmp == null branch and are left to Phase 13 WR-02
- [Phase 12]: Fork LZ4 capability rule deleted (BLD-09); lz4-java stays at upstream's 1.10.4 on runtimeClasspath and clickhouseDependencies, bumps remain Phase 14
- [Phase 12]: 12-03: D-17 retired onto setCompressionMethod; compress=false alone suffices on V2 and V1 against 26.9.12.8, V1 ?compress=true is a no-observation (does not reach node options)
- [Phase 12]: 12-04: EOS-01 restart-read test seen RED (4 of 6, expected BEFORE_PROCESSING but was NONE) against the pre-v1.7.0 summary-count read; KeeperStateProvider.java restored byte-identical to v1.7.0; D-16 hunt not needed
- [Phase 12]: 12-05: D-17 load-bearing-control sentence derived only from the D-12 probe rows (Recorded Fact 26), keeping the V1 'not observed' clause
- [Phase 12]: 12-05: full suite on latest 26.9.12.8 x V2 green (61 classes, 691 tests, 18 named skips); #800/cluster buildFromClause tests are compile-only
- [Phase 12]: UAT 8/8 (2026-10-08): user accepted the additive WRT-09 pins, the compile-only SYNC-10 reading, the D-17 FORK-DELTA wording, and one full-suite ordering as sufficient for now (Phase 15 still requires two clean runs)

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Publish]: ⚠ **Planning history after publication is local only.** Every `.planning/` commit after `c61d90ce` lives only on local `sync/v1.6.0`. That covers 11-07 bookkeeping, review, verification, UAT, security, and this milestone's archive commits. `custom_build` is protected and equals `origin`. Decide how and whether to publish them; pushing a tag or branch also triggers `tests.yaml`. **Scheduled:** PUB-04 (Phase 15), published via merge.
- [CI]: Pushing a tag onto the same SHA as a branch push starts a second `tests.yaml` run in the `github.sha` concurrency group and cancels the branch run. This happened on `c61d90ce`. **Scheduled:** CI-08 (Phase 14), which must land before any tag push.
- [Prod]: ⚠ R-01's silent data loss is still pinned, not fixed (KEEP, Phase 9 D-01). The `lz4` pin (D-17) and the operator-visible MBean name change (D-07) are the two things to check at production rollout. **Scheduled:** R-01 → JSONP-05..07 (Phase 13); D-17 retirement done in Phase 12 (CMP-01..03); rollout checklist → ROLL-01 (Phase 15).
- [Research flag, Phase 13]: Spike before planning on JSON-route type fidelity (`Timestamp`/`Date`/`Time`/`Decimal`/`bytes` through Jackson) and on WR-02's nested-vs-top-level null discriminator in `doWriteColValue`.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

`v1.6.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. Its 17 review and verification follow-ups are all in scope for `v1.7.0_gg1-sync` (see `REQUIREMENTS.md`).

## Session Continuity

Last session: 2026-10-08T13:49:00.292Z
Stopped at: Phase 13 context gathered
Resume file: .planning/phases/13-fork-hardening-on-the-merged-tree/13-CONTEXT.md

## Operator Next Steps

- Plan Phase 13 with `/gsd-discuss-phase 13` or `/gsd-plan-phase 13` (research flag: spike JSON-route type fidelity and WR-02's null discriminator first)
