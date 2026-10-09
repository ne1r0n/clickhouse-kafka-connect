---
gsd_state_version: "1.0"
milestone: v1.7.0_gg1-sync
milestone_name: Upstream v1.7.0 Sync & Follow-up Hardening
current_phase: 14
current_phase_name: Dependency Bumps & CI
status: planning
stopped_at: Phase 14 context gathered
last_updated: "2026-10-09T10:41:00.734Z"
last_activity: 2026-10-08
last_activity_desc: Phase 13 complete, transitioned to Phase 14
state_head: 864d82464c1edbeccc1f21420a0acfcfecf39165
progress:
  total_phases: 4
  completed_phases: 2
  total_plans: 13
  completed_plans: 13
  percent: 50
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-08 after Phase 13)

**Core value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.
**Current focus:** Phase 14 — Dependency Bumps & CI

## Current Position

Phase: 14 — Dependency Bumps & CI
Plan: Not started
Status: Ready to plan
Last activity: 2026-10-08 — Phase 13 complete, transitioned to Phase 14

Progress: [█████░░░░░] 50%

## Performance Metrics

**Velocity:**

- Total plans completed this milestone: 0 (`v1.6.0_gg1-sync`: 24; `v1.2.9_gg4-tests`: 16)
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 12 | 5 | - | - |
| 13 | 8 | - | - |
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
| Phase 13 P01 | 10 min | 2 tasks | 7 files |
| Phase 13 P05 | 8 min | 3 tasks | 7 files |
| Phase 13 P07 | 12 min | 2 tasks | 7 files |
| Phase 13 P02 | 12 min | 2 tasks | 1 files |
| Phase 13 P03 | 13 min | 3 tasks | 4 files |
| Phase 13 P06 | 6 min | 3 tasks | 3 files |
| Phase 13 P04 | 8 min | 2 tasks | 5 files |
| Phase 13 P08 | 6 min | 2 tasks | 3 files |

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
- [Phase 13]: 13-01: R-01 fixed by routing keyed on shouldSkipTableMetadata(), unconditional, no opt-out flag; metadata-free route uses a private MetadataFreeJsonWriter (mapper copy), shared DataJson.OBJECT_MAPPER untouched
- [Phase 13]: 13-01: metadata-free JSON encodings are Timestamp decimal seconds, Date epoch days, Time ms, Decimal BigDecimal, bytes raw via writeUTF8String; empty CDC records skipped (D-19); behaviour ledger L-01..L-07 in 13-01-SUMMARY.md
- [Phase 13]: 13-05: DESC-05 skips DESCRIBE rows below a JSON/Dynamic ancestor at any depth via an ancestor-prefix walk; Tuple/Map/Array(JSON) rows stay un-skipped
- [Phase 13]: 13-05: COLS-07 Array recursion passes hasDefaultValue, isSubColumn in signature order; CFG-04 USERNAME ConfigDef default is usernameDefault; ledger L-16..L-18 in 13-05-SUMMARY.md
- [Phase 13]: 13-07: ClickHouseBase.dropSharedDatabaseOrAttribute is package-private static with a nested DatabaseDrop action, so a shared-container drop failure is attributed (IllegalStateException with class, database, cause) and pinned Docker-free (DEBT-04, D-14)
- [Phase 13]: 13-02: MetadataFreeJsonWriter needed no fix; all D-04 parity cells, boundaries and D-07 nested cells already match RowBinary — Live twin-table matrix passed on first run; Timestamp-encoding mutation made it fail, so the matrix is not vacuous
- [Phase 13]: 13-03: WRT-07 needed no writer change (pinned live with a seen-RED mutation); WRT-08/D-09/D-10/D-16 implemented in doWriteColValue container loops, nested null scalar/Tuple slots throw a data-free DataException (root cause of Processing's RuntimeException), nested null Array/Map coerce to empty; ledger L-09..L-13 in 13-03-SUMMARY.md
- [Phase 13]: 13-06: HeaderToValue non-STRING target pinned as current behaviour (schema path throws DataException, schemaless overwrites with String); no main change per D-15
- [Phase 13]: 13-06: JsonColumnTest asserts Base64 String for four configs and signed-byte Array(Nullable(Int64)) for the two Gson configs (bytes.to.base64 only affects Struct input)
- [Phase 13]: 13-04: JSONP-08 delivers evaluation order only (metadata-free predicate first, no csc==null short-circuit, NPE on null config preserved); dated D-13 correction in REQUIREMENTS.md
- [Phase 13]: 13-04: TYPE-08 gate narrowed to INT256/UINT256 by sharing the DYNAMIC/UNKNOWN case block in validateDataSchema; Int128/UInt128 unchanged; ledger L-14, L-15 in 13-04-SUMMARY.md
- [Phase 13]: 13-08: Phase 13 gate green on ClickHouse latest (26.9.13.15) x V2: 803 tests, 0 failures, 18 named skips; behaviour ledger L-01..L-19 consolidated; Enum8/Enum16/Tuple nullability defect logged as backlog 999.1

### Pending Todos

None.

### Blockers/Concerns

- [Env]: This Claude Code shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal string `"null"`, which breaks Testcontainers' Ryuk reaper. Run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`, as `config.json`'s `build_command`/`test_command` already do.
- [Tooling]: The `gh` CLI is not installed locally (deliberately, D-11). CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 req/hr) or the web UI.
- [GSD]: Worktree isolation quirk, dormant but recurring: a plain `gsd_run query dispatch-isolation` read re-persists the sentinel. Any deliberate degrade must be re-forced with `--force-isolation none` immediately before each dispatch.
- [Publish]: ⚠ **Planning history after publication is local only.** Every `.planning/` commit after `c61d90ce` lives only on local `sync/v1.6.0`. That covers 11-07 bookkeeping, review, verification, UAT, security, and this milestone's archive commits. `custom_build` is protected and equals `origin`. Decide how and whether to publish them; pushing a tag or branch also triggers `tests.yaml`. **Scheduled:** PUB-04 (Phase 15), published via merge.
- [CI]: Pushing a tag onto the same SHA as a branch push starts a second `tests.yaml` run in the `github.sha` concurrency group and cancels the branch run. This happened on `c61d90ce`. **Scheduled:** CI-08 (Phase 14), which must land before any tag push.
- [Prod]: R-01 fixed by routing in Phase 13 (JSONP-05..07) — a deliberate behaviour change for explicit-JSON + Struct pipelines (rows now land). Rollout must cover it, the MBean rename (D-07), the compression setting (CMP), the nested-null `DataException` (WRT-08) and the `Int256`/`UInt256` fail-fast (TYPE-08). Source for the prose: `phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` (L-01..L-19). **Scheduled:** ROLL-01, CHANGELOG/FORK-DELTA (Phase 15).
- [Phase 13]: ⚠ `io.debezium.time.Timestamp` (epoch millis) is not converted on the metadata-free JSON route — ClickHouse reads it as seconds (13-REVIEW WR-02, ledger L-05). Five other review findings (WR-01, WR-03..05) are open in `13-REVIEW-DISPOSITION.md`; WR-05 is backlog 999.1. None blocks Phase 14; decide before Phase 15 publication whether WR-02 is fixed or documented as a rollout caveat.

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

`v1.6.0_gg1-sync` closed with a clear open-artifact audit, so nothing was acknowledged. Its 17 review and verification follow-ups are all in scope for `v1.7.0_gg1-sync` (see `REQUIREMENTS.md`).

## Session Continuity

Last session: 2026-10-09T10:41:00.674Z
Stopped at: Phase 14 context gathered
Resume file: .planning/phases/14-dependency-bumps-ci/14-CONTEXT.md

## Operator Next Steps

- Plan Phase 14 with `/gsd-discuss-phase 14` or `/gsd-plan-phase 14`
- Optional before Phase 15: `/gsd-code-review 13 --fix` for the five open Phase 13 review warnings, and `/gsd-map-codebase` (the codebase map predates the v1.7.0 merge; drift gate advisory)
