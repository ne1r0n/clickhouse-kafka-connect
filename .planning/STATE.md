---
gsd_state_version: "1.0"
milestone: v1.7.0_gg4-hardening
milestone_name: Dependency Absorption, Discovery Defects & Review Debt
status: planning
current_phase: "25"
current_phase_name: "Dependency Absorption & Upstream Mini-Sync"
last_updated: "2026-10-21T11:12:30.000Z"
last_activity: 2026-10-21
progress:
  total_phases: 5
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-21 after starting milestone v1.7.0_gg4-hardening)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 25 — Dependency Absorption & Upstream Mini-Sync (fix the pin-guard reader first, then absorb `origin/custom_build`'s nine Dependabot merges and upstream's tail `a6f1df83` before any code work)

## Current Position

Phase: 25 of 29 (Dependency Absorption & Upstream Mini-Sync) — not started (ready to plan)
Plan: Not started
Status: Ready to plan
Last activity: 2026-10-21 — Roadmap created for v1.7.0_gg4-hardening (Phases 25-29, 36/36 requirements mapped)

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: `v1.7.0_gg3-hardening`: 23 (+ quick 261020-vcr); `v1.7.0_gg2-hardening`: 29; `v1.7.0_gg1-sync`: 25; `v1.6.0_gg1-sync`: 24; `v1.2.9_gg4-tests`: 16
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 12 | 5 | - | - |
| 13 | 8 | - | - |
| 14 | 4 | - | - |
| 15 | 8 | - | - |
| 16 | 3 | - | - |
| 17 | 4 | - | - |
| 18 | 4 | - | - |
| 19 | 10 | - | - |
| 20 | 8 | - | - |
| 21 | 5 | - | - |
| 22 | 5 | - | - |
| 23 | 6 | - | - |
| 24 | 7 | - | - |

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
| Phase 16 P01 | 4 min | 3 tasks | 4 files |
| Phase 16 P02 | 7 min | 3 tasks | 5 files |
| Phase 17 P01 | 5 min | 3 tasks | 8 files |
| Phase 17 P02 | 4 min | 3 tasks | 7 files |
| Phase 17 P03 | 17 min | 3 tasks | 1 files |
| Phase 17 P04 | 9 min | 2 tasks | 3 files |
| Phase 18 P01 | 15 min | 3 tasks | 9 files |
| Phase 18 P02 | 8min | 2 tasks | 7 files |
| Phase 18 P03 | 5min | 2 tasks | 7 files |
| Phase 18 P04 | 29min | 2 tasks | 5 files |
| Phase 19 P01 | 14 min | 3 tasks | 3 files |
| Phase 19 P02 | 15min | 2 tasks | 9 files |
| Phase 19 P03 | 8 min | 3 tasks | 2 files |
| Phase 19 P04 | 17min | 2 tasks | 13 files |
| Phase 19 P06 | 10min | 2 tasks | 1 files |
| Phase 19 P07 | 4 min | 1 tasks | 6 files |
| Phase 19 P08 | 9 min | 2 tasks | 10 files |
| Phase 19 P09 | 6 min | 2 tasks | 2 files |
| Phase 19 P10 | 10 min | 3 tasks | 2 files |
| Phase 20 P01 | 47 min | 3 tasks | 2 files |
| Phase 20 P02 | 15 min | 2 tasks | 3 files |
| Phase 20 P03 | 10 min | 3 tasks | 2 files |
| Phase 20 P04 | 19 min | 3 tasks | 6 files |
| Phase 20 P05 | 20 min | 3 tasks | 8 files |
| Phase 20 P06 | 45 min | 3 tasks | 1 files |
| Phase 20 P07 | 35 min | 3 tasks | 2 files |
| Phase 21 P01 | 65 min | 3 tasks | 1 files |
| Phase 21 P02 | 8 min | 2 tasks | 5 files |
| Phase 21 P03 | 15 min | 2 tasks | 5 files |
| Phase 21 P04 | 7 min | 3 tasks | 3 files |
| Phase 21 P05 | 32h 4m (about 10 min of work) | 2 tasks | 1 files |
| Phase 22 P01 | 12 min | 2 tasks | 3 files |
| Phase 22 P02 | 8 min | 2 tasks | 3 files |
| Phase 22 P03 | 18 min | 3 tasks | 13 files |
| Phase 22 P04 | 6 min | 2 tasks | 5 files |
| Phase 22 P05 | 9 min | 2 tasks | 1 files |
| Phase 23 P01 | 15 min | 2 tasks | 7 files |
| Phase 23 P02 | 14 min | 3 tasks | 7 files |
| Phase 23 P03 | 8 min | 2 tasks | 3 files |
| Phase 23 P04 | 35 min | 3 tasks | 8 files |
| Phase 23 P05 | 8 min | 3 tasks | 8 files |
| Phase 23 P06 | 9 min | 3 tasks | 3 files |
| Phase 24 P01 | 10 min | 3 tasks | 4 files |
| Phase 24 P02 | 16 min | 3 tasks | 1 files |
| Phase 24 P03 | 6 min | 3 tasks | 2 files |
| Phase 24 P04 | 9 min | 3 tasks | 5 files |
| Phase 24 P05 | 20 min | 3 tasks | 10 files |
| Phase 24 P06 | 47 min | 3 tasks | 1 files |
| Phase 24 P07 | 25 min | 3 tasks | 2 files |

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
- `custom_build` is the fork's GitHub default branch (since 2026-10-14). Dependabot and scheduled workflows read it, so a workflow file merged there can start running on its own schedule.
- Do not inline-mock `ClickHouseHelperClient` with Mockito in tests that share a JVM with the full suite. It intermittently crashes the OpenJ9 test JVM in `retransformClasses`. Override the writer one level up instead (see `CapturingClickHouseWriter`).

The per-phase decision logs of `v1.7.0_gg1-sync` and `v1.7.0_gg2-hardening` are archived with their phase artifacts (`milestones/<milestone>-phases/`), and their locked decisions with their requirements (`milestones/<milestone>-REQUIREMENTS.md`). Milestone-level decisions are in `PROJECT.md` → Key Decisions.

**`v1.7.0_gg3-hardening` (closed 2026-10-21):** its roadmap decisions and per-phase decision log are archived in `milestones/v1.7.0_gg3-hardening-ROADMAP.md` (Milestone Summary) and with the phase artifacts in `milestones/v1.7.0_gg3-hardening-phases/`. The key decisions are in `PROJECT.md` → Key Decisions. Two of them carry forward as standing rules:

- Writer failure text stays value-free. In `doWriteColValue`, an `IOException` chain, a `ConnectException` and an exact `RuntimeException` pass through unchanged. Everything else becomes a metadata-only `DataException` with a `WithheldMessageException` cause, and the 184-case `WriterFailureClassificationUnitTest` pins the classification. A new writer message must not embed a record value.
- Operator documents get an independent `gsd-code-reviewer` pass, spawned by the orchestrator, before any publication gate. Review rounds on fixes continue until a round finds nothing. An executor's inline review does not count as independent (Phase 24, quick 261020-vcr).

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Publish] 2026-10-20: `v1.7.0_gg3` is on `origin/custom_build` at `3376a18f`, published by Phase 24's `--no-ff` merge of `sync/v1.7.0` (X = `8b1c318d`, rehearsed at X0 = `3ab55e1d`) onto `6473e24f`, with `tests.yaml` run 39472696952 green. Tags `v1.7.0_gg3` (`3376a18f`) and `pre-v1.7.0_gg3` (`6473e24f`) are on origin, and the scratch branch `sync/v1.7.0` was deleted from origin. The Dependabot freeze is over. Later `.planning/` commits on local `sync/v1.7.0` are a local tail and are not pushed.
- [Publish] 2026-10-20: the corrected rollout note is on `origin/custom_build` at `6a2a87a5`, a `--no-ff` merge of `sync/v1.7.0` (S = `a403cc1d`) onto `3376a18f`, made after the user's direct `publish`, with `tests.yaml` run 39496482074 green (quick 261020-vcr). It fixes the 14 findings of 24-REVIEW rounds 3 to 5 in `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md` and `docs/FORK-DELTA.md`. Tags `v1.7.0_gg3` (`3376a18f`) and `pre-v1.7.0_gg3` (`6473e24f`) did not move, and no tag was pushed.
- [Publish]: `origin/custom_build` was `6473e24f` until the gg3 merge, then `3376a18f`, and is now `6a2a87a5` (the corrected-note merge). Dependabot PRs #4 (client 0.9.6 → 0.10.0), #6 (commons-lang3 3.21.0), #7 (ben-manes versions 0.66.0), #8 (Gradle wrapper 9.8.1) and #9 (Kafka `connect-api` 4.1.1 → 4.4.0) are open, using all 5 open-PR slots. #4 and #9 bump fork-only pins, and `dependabot.yml` has no `ignore` for them. Any merge on `origin/custom_build` diverges the next milestone's base, which then opens with a DEPS-01-shaped merge. Milestone tags such as `v1.7.0_gg2-hardening` stay local.
- [CI]: The nightly `integrationTests.yaml` has been disabled by the user since 2026-10-16T09:38:23Z (Phase 21 OPS-01). The file stays upstream-identical. Phase 24 re-checks that no scheduled run followed.
- [CI]: Two branches pushed at one SHA still cancel each other, so the one-branch-per-SHA rule holds.
- [Phase 23]: Backlog 999.4 is a pre-existing, high-severity defect for any table with a Tuple element named `size` or `size0`: discovery drops the element, and the inserts that were run (four shapes) failed with code 32 or 33; a wrong stored value is possible and not measured. Since COLS-14 it is named by a WARN at every DESCRIBE, but it is not fixed. The gg3 rollout note (item 2), the CHANGELOG (Known Issues) and FORK-DELTA (Known costs item 10) disclose it, measured as code 32 or 33 on the default route with client V2.
- [Dependabot]: The repository has no `dependencies` label, but both `dependabot.yml` entries request it, so every Dependabot PR gets a "labels could not be found" comment. Updates still run. Fixing it means the user creates the label (D-11), or the fork drops the request. Tracked in PROJECT.md → Active, Phase 21 follow-ups.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| deferred_items | 18/deferred-items.md: `ClickHouseWriterNullMessageUnitTest` inline-mocks `ClickHouseHelperClient` (intermittent OpenJ9 `retransformClasses` crash risk) | closed in Phase 22 (HARN-08, Mockito-free `CountingHelperClient`) | 2026-10-16 | v1.7.0_gg2-hardening |

`v1.7.0_gg2-hardening` closed after a passed milestone audit. The open-artifact audit found one deferred item, and the user acknowledged it. The close was an override closeout, because the Phase 16–19 verification digests had gone stale after Phase 20 edited the files they covered (see `MILESTONES.md`). Its follow-ups are now scheduled as this milestone's 20 requirements (`REQUIREMENTS.md` → Traceability).

`v1.7.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. The close was an override closeout, because the Phase 12/13 verification digests had gone stale and no milestone audit was run. All eleven of its follow-ups were closed in `v1.7.0_gg2-hardening`.

`v1.6.0_gg1-sync` also closed with a clear audit. All 17 of its follow-ups were closed in `v1.7.0_gg1-sync`.

## Session Continuity

Last session: 2026-10-21
Stopped at: Roadmap created for milestone `v1.7.0_gg4-hardening` (Phases 25-29, 36/36 requirements mapped); Phase 25 ready to plan
Resume file: None

## Operator Next Steps

- Discuss and plan Phase 25 with /gsd-discuss-phase 25, then /gsd-plan-phase 25
- Create the `dependencies` label in the GitHub UI when Phase 25 presents OPS-04
