---
gsd_state_version: "1.0"
milestone: v1.7.0_gg6-maintenance
milestone_name: Dependency Absorption, Upstream Tail & Advisory Debt
current_phase: 33
current_phase_name: Dependency Sweep & Protobuf Decision
status: verifying
stopped_at: Completed 33-03-PLAN.md
last_updated: "2026-10-28T20:14:15.167Z"
last_activity: 2026-10-28
last_activity_desc: Phase 33 execution started
state_head: d06f8e5c07d079bcd7c5d6ca5dbbd095acd7db51
progress:
  total_phases: 6
  completed_phases: 0
  total_plans: 3
  completed_plans: 3
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-28 after starting milestone v1.7.0_gg6-maintenance)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 33 — Dependency Sweep & Protobuf Decision

## Current Position

Phase: 33 (Dependency Sweep & Protobuf Decision) — EXECUTING
Plan: 3 of 3
Status: Phase complete — ready for verification
Last activity: 2026-10-28 — Phase 33 execution started

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: `v1.7.0_gg5-ci`: 16; `v1.7.0_gg4-hardening`: 32; `v1.7.0_gg3-hardening`: 23 (+ quick 261020-vcr); `v1.7.0_gg2-hardening`: 29; `v1.7.0_gg1-sync`: 25; `v1.6.0_gg1-sync`: 24; `v1.2.9_gg4-tests`: 16
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
| 25 | 8 | - | - |
| 26 | 5 | - | - |
| 27 | 5 | - | - |
| 28 | 6 | - | - |
| 29 | 8 | - | - |
| 30 | 3 | - | - |
| 31 | 6 | - | - |
| 32 | 7 | - | - |

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
| Phase 25 P01 | 67min | 3 tasks | 1 files |
| Phase 25 P02 | 12min | 2 tasks | 2 files |
| Phase 25 P03 | 45min | 3 tasks | 4 files |
| Phase 25 P04 | 15min | 2 tasks | 6 files |
| Phase 25 P05 | 25min | 3 tasks | 9 files |
| Phase 25 P06 | 15min | 3 tasks | 7 files |
| Phase 25 P07 | 22min | 3 tasks | 8 files |
| Phase 25 P08 | 16min | 3 tasks | 6 files |
| Phase 26 P01 | 14 min | 3 tasks | 102 files |
| Phase 26 P02 | 11 min | 3 tasks | 9 files |
| Phase 26 P03 | 15 min | 3 tasks | 38 files |
| Phase 26 P04 | 25 min | 2 tasks | 3 files |
| Phase 26 P05 | 30 min | 2 tasks | 1 files |
| Phase 27 P01 | 7 min | 2 tasks | 2 files |
| Phase 27 P02 | 153 min | 3 tasks | 8 files |
| Phase 27 P03 | 9 min | 3 tasks | 8 files |
| Phase 27 P04 | 18 min | 3 tasks | 13 files |
| Phase 27 P05 | 22 min | 3 tasks | 6 files |
| Phase 28 P01 | 11 min | 2 tasks | 2 files |
| Phase 28 P02 | 10 min | 3 tasks | 32 files |
| Phase 28 P03 | 21 min | 3 tasks | 10 files |
| Phase 28 P04 | 12min | 2 tasks | 3 files |
| Phase 28 P05 | 25min | 3 tasks | 10 files |
| Phase 28 P06 | 71 min | 3 tasks | 8 files |
| Phase 29 P01 | 27 min | 4 tasks | 4 files |
| Phase 29 P02 | 10 min | 3 tasks | 1 files |
| Phase 29 P03 | 11min | 3 tasks | 4 files |
| Phase 29 P04 | 10min | 3 tasks | 2 files |
| Phase 29 P05 | 8 min | 3 tasks | 9 files |
| Phase 30 P01 | 45 min | 3 tasks | 8 files |
| Phase 30 P02 | 2 min | 2 tasks | 2 files |
| Phase 31 P01 | 26 min | 2 tasks | 1 files |
| Phase 31 P02 | 6 min | 3 tasks | 1 files |
| Phase 31 P03 | 3min | 2 tasks | 2 files |
| Phase 31 P04 | 7 min | 3 tasks | 7 files |
| Phase 32 P01 | 20 min | 3 tasks | 5 files |
| Phase 32 P02 | 8 min | 3 tasks | 2 files |
| Phase 32 P03 | 9 min | 3 tasks | 3 files |
| Phase 33 P01 | 27 min | 2 tasks | 2 files |
| Phase 33 P02 | 15 min | 3 tasks | 8 files |
| Phase 33 P03 | 52 min | 3 tasks | 5 files |

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

**`v1.7.0_gg4-hardening` (closed 2026-10-24):** its roadmap decisions are archived in `milestones/v1.7.0_gg4-hardening-ROADMAP.md` (Milestone Summary), and its per-phase decision log is archived with the phase artifacts in `milestones/v1.7.0_gg4-hardening-phases/`. Standing rules that outlive it:

- A release phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file. Review findings it cannot fix are deferred with a reason, an owner and a re-open trigger in a milestone close record (Phase 29, D-05).
- A snapshot or suite difference after a behaviour-preserving extraction is a stop, not an expectation update. The Phase 26 `DescribeTableBuilder` extraction matched all 47 fixtures on its first run.
- A size-named Tuple row is kept only when the parsed parent Tuple declares that key; an unreadable parent keeps the old behaviour plus one metadata-only WARN. Names and Enum labels are matched under the raw `CREATE TABLE` identifier.
- The Confluent archive rules live once in `ConfluentArchiveCheck`, run by the `verifyConfluentArchive` finalizer and in CI before the tests. `connect-api` 4.4.0 is compile-time only; the worker supplies the API at runtime.
- Archive identity against the previous release is pinned by a per-release allow-list (`confluent-archive-allowlist-29.txt`). The compare is a manual release-time step.

**`v1.7.0_gg5-ci` roadmap (2026-10-24):** three phases under `coarse` granularity. The guard against unpinned actions (CIG-01) and the release dry run (REL-05) sit in Phase 30, because the release phase cannot edit a `.github/` file. The nightly and the byte guard for upstream's `integrationTests.yaml` sit in Phase 31. FORK-DELTA, CHANGELOG, `VERSION` and the rollout note are written once, in Phase 32.
- [Phase 30]: Dry run uses a throwaway tag dry-run-<run_id>-<run_attempt> with a plain gh release delete --yes; no tag-cleanup flag, the real VERSION tag is never touched (UD-1)
- [Phase 30]: Pin guard fails closed on local ./ and docker:// uses values; Dependabot exclude-paths lists only integrationTests.yaml (D-10)
- [Phase 30]: 30-02: the increment-version.yml vnull defect (semver.inc returns null for vX.Y.Z_ggN, measured E-06) stays unfixed and is recorded in FORK-DELTA; the stale default-branch and inert clauses of the Dependabot row are carried to Phase 32 (CIG-02)
- [Phase 31]: 31-02: no integration-test skip list is built; E-06b (accept) holds skip-entries: 0, so E-07 records the D-07 not-needed branch and the phase-gate run is 31-06's E-18
- [Phase 31]: 31-03: upstream integrationTests.yaml pinned by SHA-256 07dff3d0...5109 and length 4812 (blob 3e9b9be0) in a Docker-free testUnit guard, seen red on a one-byte edit — Upstream's file must neither drift nor be re-armed; the failure message carries the sync update procedure
- [Phase 31]: 31-03: Actions API reports workflow 384641581 disabled_manually, 2 runs, none after 2026-10-16T10:00:00Z (E-11) — Online half of NIT-02; 31-05 and Phase 32 re-read the three URLs
- [Phase 31]: 31-04: nightly timeouts are step 75 / job 85 minutes from E-18 (unamended by E-06b); the job is gated on workflow_dispatch or vars.NIGHTLY_ENABLED == 'true' — Whole-task measured wall 36 min x 2 rounded up; a manual dispatch always starts and a scheduled run starts only after the operator sets the variable (D-01, D-05)
- [Phase 31]: 31-04: nightly guard fixtures are edits of the real workflow's non-comment lines with the timeouts pinned at 80/70; failed-test annotation names are cut at 300 characters, the step summary keeps full names — The guard cannot drift from the file it protects; one escaped, capped annotation line cannot inject a workflow command (D-10, Pitfall 11)
- [Phase 32]: Archive allow-list for gg5 holds four exact-path rows (Version, ClickHouseHelperClient, SinkTaskStatistics, MANIFEST.MF) because javac inlines Version.ARTIFACT_VERSION; totals only-a 0 only-b 0 changed 4 (32-AMENDMENT-01)
- [Phase 32]: gg5 rollout note: four-row archive wording (32-AMENDMENT-01); nightly off-switch stated as delete or false only; skipped-job claim backed by GitHub docs quote in E-10
- [Phase 32]: Four-row archive wording (32-AMENDMENT-01) used in CHANGELOG, FORK-DELTA and E-20: the gg5 archive differs from gg4 only in the release identity (Version constant, the two classes that inline it, manifest Implementation-Version)
- [Phase 32]: The nightly is described as running the whole integrationTest task when dispatched or scheduled; its green run is stated as local (E-18) because no GitHub run exists before plan 32-07

**`v1.7.0_gg6-maintenance` roadmap (2026-10-28):** six phases under `coarse` granularity, in the user's order:
1. the dependency sweep (33);
2. the upstream tail (34);
3. the buffered offset-commit fix, upstream #777 (35);
4. client 0.11.0 (36);
5. CI hardening and the advisory debt (37);
6. the release (38).

The BUF fix lands after the tail, which also edits `ChunkFlusher`. It lands before the client bump, so it is releasable on 0.9.6, and the CLT-09 one-commit revert cannot touch it. The client bump is the last code change. The archive-compare consumer and every workflow fix sit in Phase 37, because the release phase cannot edit `.github/`. The gg6 allow-list, `VERSION`, CHANGELOG and the rollout note are written in Phase 38. Phase 35 records the BUF rollout item, and Phase 38 places it in the note.
- [Phase 33]: 33-01: PR #19 (lz4-java 1.12.1, GHSA-48mj-8f7c-7qfm) absorbed from its fetched head 2a806038 as own --no-ff merge 6bf1c43b; local pre-v1.7.0_gg6 tag on origin tip 3a3a15a9, not pushed
- [Phase 33]: 33-02: PRs #18 (okhttp 5.5.1), #20 (Gradle 9.8.2) and #16 (setup-java v6.1.0) absorbed unchanged from fetched heads, each as its own --no-ff merge; full suite green on the sweep tip 79f56a5e (1961 tests, 18 skipped)
- [Phase 33]: Phase 33 plan 03: protobuf-java and protoc stay on 3.x via exact-name semver-major ignores in the gradle Dependabot entry only (the protobuf Gradle plugin stays updatable); cause is the ProtobufIngestTest VerifyError against the Confluent 7.9.1 test stack (run 39501799049); re-open trigger FUT-03
- [Phase 33]: Phase 33 plan 03: PR #10 is recorded as closing-at-publication (ROADMAP criterion 4); no user words received and no agent touched the PR; REQUIREMENTS.md SWP-03 wording is slightly stricter and is flagged for the user to align

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation ` immediately before each dispatch.
- [Publish] 2026-10-20: `v1.7.0_gg3` is on `origin/custom_build` at `3376a18f`, published by Phase 24's `--no-ff` merge of `sync/v1.7.0` (X = `8b1c318d`, rehearsed at X0 = `3ab55e1d`) onto `6473e24f`, with `tests.yaml` run 39472696952 green. Tags `v1.7.0_gg3` (`3376a18f`) and `pre-v1.7.0_gg3` (`6473e24f`) are on origin, and the scratch branch `sync/v1.7.0` was deleted from origin. The Dependabot freeze is over. Later `.planning/` commits on local `sync/v1.7.0` are a local tail and are not pushed.
- [Publish] 2026-10-20: the corrected rollout note is on `origin/custom_build` at `6a2a87a5`, a `--no-ff` merge of `sync/v1.7.0` (S = `a403cc1d`) onto `3376a18f`, made after the user's direct `publish`, with `tests.yaml` run 39496482074 green (quick 261020-vcr). It fixes the 14 findings of 24-REVIEW rounds 3 to 5 in `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md` and `docs/FORK-DELTA.md`. Tags `v1.7.0_gg3` (`3376a18f`) and `pre-v1.7.0_gg3` (`6473e24f`) did not move, and no tag was pushed.
- [Publish] 2026-10-23: `v1.7.0_gg4` is on `origin/custom_build` at `21c51598`, published by Phase 29's `--no-ff` merge of `sync/v1.7.0` (X = `3d847009`, rehearsed at X0 = `ef19fbc8`) onto `69cd680b`, made after the user's direct `publish`, with `tests.yaml` run 40022281005 green (X0's rehearsal run: 40017036598). Tags `v1.7.0_gg4` (`21c51598`) and `pre-v1.7.0_gg4` (`69cd680b`) are on origin; `sync/v1.7.0` was deleted from origin. The Dependabot freeze is over; later `.planning` commits stay a local tail on `sync/v1.7.0`, unpushed.
- [Publish]: `origin/custom_build` was `6473e24f` until the gg3 merge, then `3376a18f`, then `6a2a87a5` (the corrected-note merge), and is now `21c51598` (the gg4 merge). Dependabot PRs #4 (client 0.9.6 → 0.10.0), #6 (commons-lang3 3.21.0), #7 (ben-manes versions 0.66.0), #8 (Gradle wrapper 9.8.1) and #9 (Kafka `connect-api` 4.1.1 → 4.4.0) are open, using all 5 open-PR slots. #4 and #9 bump fork-only pins, and `dependabot.yml` has no `ignore` for them. Any merge on `origin/custom_build` diverges the next milestone's base, which then opens with a DEPS-01-shaped merge. Milestone tags such as `v1.7.0_gg2-hardening` stay local.
- [CI]: The nightly `integrationTests.yaml` has been disabled by the user since 2026-10-16T09:38:23Z (Phase 21 OPS-01). The file stays upstream-identical. Phase 24 re-checks that no scheduled run followed.
- [CI]: Two branches pushed at one SHA still cancel each other, so the one-branch-per-SHA rule holds.
- [Dependabot]: the `dependencies` label exists since 2026-10-21 (Phase 25 OPS-04, user-created, API 200). PRs #4 (client 0.9.6 → 0.10.0) and #10 (protobuf-java 4.36.2) are still open and must stay unmerged on `origin/custom_build` until publication; E-25 (a post-label PR without the "labels could not be found" comment) is pending the next monthly run and is re-checked in Phase 29.
- [Phase 25]: 25-REVIEW left three warnings open (WR-01 archive-step pin accepts `-x verifyConfluentArchive`/`--dry-run`/`|| true`/step `if:`; WR-02 `ConnectApiReferencesUnitTest` resolves inherited members on the test classpath and drops unresolved ones; WR-03 descriptor-only Kafka types unchecked) and four info items (stale `build/confluentArchive` dirs, `META-INF/versions/*` and `.zip`/`.war` nesting, brittle anchors, merge-carried hygiene incl. `pre-release.yaml` still pinning Gradle 7.4.2). Ledger: `25-REVIEW-DISPOSITION.md`. Trigger: the next edit of those guards (Phase 28 review debt).
- [Phase 25]: the working tree keeps an unstaged CRLF diff on `gradlew.bat` after the Dependabot wrapper merge (committed blob equals origin's); plan verify strings must exclude it.
- [Phase 26]: 26-REVIEW has 2 open info items after the `--fix` pass (commits `a58c66da..0a1c79f8`: 9 fixed, IN-02 and IN-06 skipped by decision) and the 2026-10-22 re-review (`4271be66`): IN-12 `DescribeTableBuilder.columnsByName` stays last-wins, reachable only by a declared JSON or Dynamic element named `size<digits>` at Array depth 2 or more, not reproduced; IN-13 the fork-owned `allColumnsMap.merge` in upstream `Table.registerValidColumn` has no `fork-anchors.txt` row, so an upstream sync could revert it unnoticed by the merge gate. Ledger `26-REVIEW-DISPOSITION.md`; `26-SECURITY.md` is `threats_open: 0`; `26-VERIFICATION.md` regenerated 2026-10-22 (passed, 5/5) after the fixes; `26-UAT.md` complete (24 passed, 0 issues).
- [Env]: the full suite crashed the OpenJ9 (Semeru 17) test JVM five times on 2026-10-21/22 at the known `Mockito.spy` frame, with host swap at about 97%; `-PjavaVersion=21` (HotSpot) passes, and a plain re-run passed once swap pressure eased. Expect the crash whenever the host is swapping; it is not a test failure.
- [Release] 2026-10-23: `VERSION` reads `v1.7.0_gg4` and the CHANGELOG has its `1.7.0_gg4` section; both are published at `21c51598`.
- [Publish] 2026-10-26: `v1.7.0_gg5` is on `origin/custom_build` at `3a3a15a9`, published by Phase 32's `--no-ff` merge of X0 (`41e7d546`) onto `21c51598` after the user's direct `publish`, with `tests.yaml` run 40416215635 green on the merge and run 40414210750 green on X0; tags `v1.7.0_gg5` (`3a3a15a9`) and `pre-v1.7.0_gg5` (`21c51598`) are on origin, `sync/v1.7.0` was deleted from origin, and the nightly is on `custom_build` with `NIGHTLY_ENABLED` unset; its first manual run is next in plan 32-07. Later `.planning` commits are a local tail.
- [Release] 2026-10-26: `VERSION` reads `v1.7.0_gg5` and the CHANGELOG has its `1.7.0_gg5` section; both are published at `3a3a15a9`.
- [CI] 2026-10-27: the nightly's first manual run 40474535605 on `custom_build` (`3a3a15a9`) ended success (63 tests, 0 failures, 9 skipped, 33 min; NIT-03); its one scheduled run so far was skipped; `NIGHTLY_ENABLED` was set by the operator after the run (their report, E-82). An accidental `release.yaml` dispatch (run 40474419526) was cancelled by the user before publishing; no release or tag resulted.
- [Roadmap] 2026-10-24: GitHub documents that `workflow_dispatch` triggers only for a workflow file on the default branch. Phase 30's dry runs can use the existing `release.yaml` and `pre-release.yaml` on the scratch branch, but the new Phase 31 nightly reaches `custom_build` only at publication. Phase 31 research confirms the behaviour and settles how the NIT-03 dispatch runs before any schedule can fire. If NIT-03 can only finish after publication, the user decides whether it moves to Phase 32.
- [Roadmap] 2026-10-24: `TestsWorkflowPinGuardUnitTest` pins `dependabot.yml`'s `exclude-paths` list of the four upstream workflows (`UPSTREAM_WORKFLOWS`). Letting Dependabot keep the three newly pinned release-path files current changes that assertion, which needs the user's sign-off (Phase 30).
- Phase 31-01: all 54 connector integration tests fail (Java 17 connector jar vs Java 11 Connect worker in cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0); Task 3 blocking-human decision (E-06) pending before 31-02
- [Roadmap] 2026-10-26: NIT-03 moved to Phase 32 by the user's decision (D-02, 31-COMMAND-EVIDENCE.md E-16). Phase 31 delivered the measurement, the gated nightly, the NIT-02 byte guard and any skip list. The nightly reaches custom_build only with Phase 32's publication, and NIGHTLY_ENABLED stays unset until the dispatch run is recorded.
- [Roadmap] 2026-10-28: Dependabot PRs #10 and #16–#20 are open on `origin`, and PR #4 is gone (superseded by #17). This replaces the PR lists in the older `[Publish]` and `[Dependabot]` lines above. Phase 33 absorbs #16, #18, #19 and #20 from their PR heads on the scratch branch, Phase 36 absorbs #17, and #10 is declined. Ask the user not to merge any of them on `origin` until publication, because a merge there diverges the scratch branch again.
- [Roadmap] 2026-10-28: BUF-03 (Phase 35). `Processing.doLogic` keys exactly-once state by the transformed topic. A fix that changes the state key would orphan the KeeperMap state of existing exactly-once deployments, so it stops for a user decision before it lands. The operator's production connectors run `exactlyOnce=false`.
- [Roadmap] 2026-10-28: Pushes. CLT-07's manual nightly dispatch (Phase 36) needs the scratch branch on `origin`, and so does any Phase 37 dry run of the archive-compare step. Each push publishes the local `.planning` tail one way and runs `tests.yaml` once, so it needs the user's direct approval and a SHA that no other pushed branch holds.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| deferred_items | 18/deferred-items.md: `ClickHouseWriterNullMessageUnitTest` inline-mocks `ClickHouseHelperClient` (intermittent OpenJ9 `retransformClasses` crash risk) | closed in Phase 22 (HARN-08, Mockito-free `CountingHelperClient`) | 2026-10-16 | v1.7.0_gg2-hardening |

`v1.7.0_gg4-hardening` closed after a `tech_debt` milestone audit with no gaps. The open-artifact audit was clear, so nothing was acknowledged. The close was an override closeout: `init.manager` read Phases 25–28 as stale and Phase 27 success criterion 2 was narrowed by the user. The 16 unfixed review findings are carried in `milestones/v1.7.0_gg4-hardening-phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md`.

`v1.7.0_gg2-hardening` closed after a passed milestone audit. The open-artifact audit found one deferred item, and the user acknowledged it. The close was an override closeout, because the Phase 16–19 verification digests had gone stale after Phase 20 edited the files they covered (see `MILESTONES.md`). Its follow-ups are now scheduled as this milestone's 20 requirements (`REQUIREMENTS.md` → Traceability).

`v1.7.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. The close was an override closeout, because the Phase 12/13 verification digests had gone stale and no milestone audit was run. All eleven of its follow-ups were closed in `v1.7.0_gg2-hardening`.

`v1.6.0_gg1-sync` also closed with a clear audit. All 17 of its follow-ups were closed in `v1.7.0_gg1-sync`.

## Session Continuity

Last session: 2026-10-28T20:14:15.144Z
Stopped at: Completed 33-03-PLAN.md
Resume file: None

## Operator Next Steps

- Discuss and plan Phase 33 with /gsd-discuss-phase 33, then /gsd-plan-phase 33
- Do not merge Dependabot PRs on `origin` until `v1.7.0_gg6` is published. The milestone absorbs them from their PR heads on the scratch branch.
- Phase 33 asks you to close PR #10 (protobuf-java 4.x), or to confirm that it may stay open until the `ignore` reaches `custom_build`
- Phase 35 reruns your stand at `~/dev/clickhouse_mvp/local_kc_test` on the fixed jar
- Phase 36's manual nightly dispatch, and any Phase 37 dry run, need you to approve a push of the scratch branch and to start the runs from GitHub's `Run workflow` control
