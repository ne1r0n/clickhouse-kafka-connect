---
phase: 13-fork-hardening-on-the-merged-tree
plan: "08"
subsystem: testing
tags: [phase-gate, full-suite, behaviour-ledger, backlog, clickhouse-latest, client-v2]

requires:
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: "Plans 13-01..13-07 and their per-plan Behaviour ledger sections (L-01..L-19)"
provides:
  - "Phase 13 gate evidence: one cache-busted full suite on ClickHouse latest x client V2 with exactly the 18 named skips, plus testUnit"
  - "13-BEHAVIOUR-LEDGER.md: single ledger L-01..L-19 and the sections Phase 15 copies into CHANGELOG, FORK-DELTA and the rollout checklist"
  - "ROADMAP backlog item 999.1 for the pre-existing Column.extractColumn nullability defect"
affects: [15-suite-gate-publication-rollout, PUB-01, PUB-02, ROLL-01]

actuals:
  tokens: 4400
  tasks: 2
  commits: 1
plan_head_before: 35b9f93739a1cfdde88d82494e0a4b48ba2271a1
plan_head_after: 97ecab566fd12eac229d73c12b6bf12ed064d7ae

tech-stack:
  added: []
  patterns:
    - "Gate plans record XML-derived totals and the skipped (class, method) set, not the Gradle console summary"
    - "Backlog entry appended as a pure append to ROADMAP.md, never an anchor-based replace"

key-files:
  created:
    - .planning/phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md
    - .planning/phases/999.1-column-extractcolumn-nullability-enum-tuple/.gitkeep
  modified:
    - .planning/ROADMAP.md

key-decisions:
  - "No main code or test was edited in this gate plan: the suite was green on the first run, so the stop rule never fired"
  - "The ledger adds a column for the source plan and a Phase 15 inputs-by-destination section beyond the plan's minimum, so Phase 15 can write each of CHANGELOG, FORK-DELTA and ROLL-01 from one list"
  - "No extra ledger entry exists beyond L-19: no plan recorded one, and 13-06 recorded 'no behaviour change'"

requirements-completed: [JSONP-05, JSONP-06, JSONP-07, JSONP-08, WRT-07, WRT-08, DESC-05, TYPE-08, COLS-07, CFG-04, DEBT-01, DEBT-02, DEBT-03, DEBT-04, DEBT-05]

coverage:
  - id: D1
    description: "Full suite on ClickHouse latest (26.9.13.15) x client V2 with Gradle cache busted: 68 classes, 803 tests, 0 failures, 0 errors, 18 skipped equal to FORK-DELTA's named skips pair by pair"
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun (BUILD SUCCESSFUL, XML-summed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "testUnit passes (431 tests, 2 skipped, 0 failures, 0 errors, run with --rerun)"
    verification:
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun"
        status: pass
    human_judgment: false
  - id: D3
    description: "All seven new Phase 13 test classes report tests > 0 with 0 failures, 0 errors and 0 skips in the full run"
    verification:
      - kind: integration
        ref: "build/test-results/test/TEST-*.xml for the seven classes listed below"
        status: pass
    human_judgment: false
  - id: D4
    description: "13-BEHAVIOUR-LEDGER.md carries L-01..L-19 with source plan, decision IDs, pinning tests and Phase 15 destination, plus the R-01 backfill, nested-null split, TYPE-08 and JSON-route gap sections"
    verification:
      - kind: other
        ref: "plan verify loop: grep -q L-01..L-19 and the three section titles, KeeperMap, root cause, ROLL-01"
        status: pass
    human_judgment: true
    rationale: "Whether the ledger wording is what Phase 15 needs for operator-facing prose is an editorial judgement"
  - id: D5
    description: "Column.extractColumn Enum8/Enum16/Tuple nullability defect logged as ROADMAP backlog 999.1, not fixed; FORK-DELTA.md and CHANGELOG.md untouched by any Phase 13 commit; merge-invariant script passes"
    verification:
      - kind: other
        ref: "grep -c '(BACKLOG)' ROADMAP.md = 1; git log --grep='(13-0' -- docs/FORK-DELTA.md CHANGELOG.md is empty; verify-merge-invariants.sh result=PASS"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-10-08
status: complete
---

# Phase 13 Plan 08: Phase gate, behaviour ledger and backlog Summary

**The merged and hardened tree passes one cache-busted full suite on ClickHouse latest x client V2 (803 tests, 0 failures, 0 errors, exactly the 18 named skips), and 13-BEHAVIOUR-LEDGER.md now holds every Phase 13 behaviour change (L-01..L-19) for Phase 15 to write CHANGELOG, FORK-DELTA and rollout prose from.**

## Performance

- **Duration:** 6 min (the full suite itself took 2 min 49 s of that)
- **Started:** 2026-10-08T17:59:55Z
- **Completed:** 2026-10-08T18:06:00Z
- **Tasks:** 2
- **Files modified:** 3 (2 created, 1 modified); no source file under `src/` changed

## Accomplishments

- Gate run: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` exited 0, `BUILD SUCCESSFUL`. No test failed, so the stop rule (no edits to main code or tests, `checkpoint:decision` on a finding) never applied.
- Consolidated the ledger from the six plan sections that carry entries (13-06 is test-only) into one table with the source plan, decision IDs, pinning tests, operator visibility and the Phase 15 destination for each of 19 entries, and added the sections Phase 15 needs verbatim.
- Logged the pre-existing Enum8/Enum16/Tuple nullability loss in `Column.extractColumn` as backlog item 999.1 instead of fixing it.

## Gate evidence

**Server and client.** ClickHouse `latest` = `26.9.13.15` (from the run's `ClickHouseServerVersionSmokeTest` line and the image label `com.clickhouse.build.version`), client V2. RESEARCH's probes were valid for 26.9.12.8; the server has moved one patch release (26.9.12.8 to 26.9.13.15) and nothing failed, so no drift effect on the JSON route or DESCRIBE tests was observed.

**Full suite (`test --rerun`, XML-derived from `build/test-results/test/*.xml`):**

| Classes | Tests | Skipped | Failures | Errors |
|---------|-------|---------|----------|--------|
| 68 | 803 | 18 | 0 | 0 |

The 803/0/18 totals equal the orchestrator's post-wave-3 gate (803 tests, 0 failures, 18 skipped).

**`testUnit` (`--rerun`):** 35 classes, 431 tests, 0 failures, 0 errors, 2 skipped (the two `ConvertToMapOrJsonPerformanceTestJson` `@Disabled` methods). The first invocation was served up to date in 723 ms, so it was re-run with `--rerun`; both runs gave the same counts.

**Skipped set versus FORK-DELTA's 18 named skips, pair by pair** (no extra, none missing):

| # | Class | Method | In FORK-DELTA |
|---|-------|--------|---------------|
| 1 | `ClickHouseSinkTaskTest` | `clientNameTest()` | yes |
| 2 | `ClickHouseSinkTaskWithSchemaTest` | `schemaWithNestedTupleMapArrayAndVariant()` | yes |
| 3 | `ClickHouseSinkTaskWithSchemaTest` | `schemaWithTupleOfMapsWithVariantTest()` | yes |
| 4-14 | `ClickHouseSinkTaskWithSchemaTest` | `exactlyOnceStateMismatchTest`, cases `[1]` to `[11]` (the console log attributes all 11 to that method) | yes |
| 15 | `InsertFormatPerformanceTest` | `compareInsertPerformance()` | yes |
| 16 | `JsonColumnTest` | `compareAllConfigurations()` | yes |
| 17 | `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testWideStructPerformance()` | yes |
| 18 | `transforms.ConvertToMapOrJsonPerformanceTestJson` | `testDeepNestedStructPerformance()` | yes |

**New Phase 13 test classes in that run** (all with 0 failures, 0 errors, 0 skips):

| Class | Tests |
|-------|-------|
| `MetadataFreeJsonWriterTest` | 33 |
| `MetadataFreeJsonTypeParityLiveTest` | 8 |
| `NestedNullRowBinaryLiveTest` | 10 |
| `WideIntegerTypeGateLiveTest` | 3 |
| `ColumnArrayRecursionTest` | 9 |
| `ClickHouseSinkConfigUsernameDefaultUnitTest` | 3 |
| `ClickHouseBaseTearDownAttributionUnitTest` | 2 |

**Phase invariants.** `verify-merge-invariants.sh` against the merge commit and `HEAD`: `invariants: provenance_paths=21 anchor_commits=61 anchor_rows=40 marker_commits=108 result=PASS` (also PASS before the commit). `git log --grep='(13-0' -- docs/FORK-DELTA.md CHANGELOG.md` prints nothing, so no Phase 13 commit touched either file. `git diff HEAD -- src` is empty.

## Task Commits

1. **Task 1: Full-suite gate on latest x V2 with the 18-skip check, plus testUnit** - no commit (evidence only, as planned)
2. **Task 2: Consolidate the behaviour ledger, log the Column nullability defect, final invariant gate** - `97ecab56` (docs)

**Plan metadata:** the `docs(13-08)` commit that follows this file (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified

- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` - the ledger table L-01..L-19; sections "R-01 backfill procedure", "Nested-null semantics by route", "TYPE-08 message change", "Known JSON-route gaps" and "Phase 15 inputs by destination"
- `.planning/ROADMAP.md` - new `## Backlog` section at the end with `### Phase 999.1: Column.extractColumn drops Nullable for Enum8/Enum16 and Tuple (BACKLOG)`; a pure append of 11 lines, `### Phase 13:` still appears once
- `.planning/phases/999.1-column-extractcolumn-nullability-enum-tuple/.gitkeep` - backlog phase directory

## Decisions Made

- The ledger keeps each plan's wording for the behaviour column and adds the decision IDs and destinations beside it, so nothing was reinterpreted while consolidating. L-15, L-16 and L-18 carry the requirement ID where `13-CONTEXT.md` has no numbered decision.
- Phase 15 inputs are also grouped by destination (CHANGELOG, FORK-DELTA, ROLL-01) at the end of the ledger, so each Phase 15 document can be written from one list.
- The backlog directory slug was written by hand (`column-extractcolumn-nullability-enum-tuple`) because `generate-slug` dropped the first words of the title.

## Deviations from Plan

None - plan executed exactly as written.

The backlog Goal line has no trailing period, matching the plan's text. `project_code` is unset, so the backlog directory carries no prefix.

## Issues Encountered

- `testUnit` was reported up to date on its first call (723 ms); re-running with `--rerun` produced the same 431/2/0/0, so the gate does not rest on a cached result.
- The server tag `latest` moved from 26.9.12.8 (RESEARCH) to 26.9.13.15; no test depended on the difference.

## Known Stubs

None.

## Threat Flags

None. T-13-18 is mitigated: the ledger cites test names, decision IDs and message formats only. A search for `password`, `secret` and `jdbc:` finds only the config key name `PASSWORD` in L-18. No package or dependency change (T-13-SC).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 13 has all eight plans summarised, the gate is green, and the invariants hold. Phase 14 (dependency bumps, CI) can start; it must re-run `MetadataFreeJsonWriterTest` and `MetadataFreeJsonTypeParityLiveTest` after the Jackson bump (BLD-11).
- Phase 15 has the ledger to write CHANGELOG (PUB-01), FORK-DELTA (PUB-02) and the rollout checklist (ROLL-01) from. Its own two full-suite runs will also show whether the DEBT-04 stricter `tearDown()` exposes a flaky drop (L-19).
- Open item: backlog 999.1 (Enum8/Enum16/Tuple nullability) is parked, not scheduled.

## Self-Check: PASSED

- FOUND: `13-BEHAVIOUR-LEDGER.md` and `.planning/phases/999.1-column-extractcolumn-nullability-enum-tuple/.gitkeep`.
- FOUND commit `97ecab56` (an ancestor of HEAD; `rev-list --count` from `plan_head_before` is 1).
- Task 2 acceptance criteria re-run: the three section titles plus `KeeperMap`, `root cause` and `ROLL-01` present; `grep -c '(BACKLOG)'` is 1 and the entry names Enum8 and Tuple; `grep -c '^### Phase 13:'` is 1; the invariant script prints `result=PASS`.
- Task 1 acceptance criteria: totals, server version and the pair-by-pair skip comparison are recorded above, and each new Phase 13 class is listed with its test count and zero failures, errors and skips.

---
*Phase: 13-fork-hardening-on-the-merged-tree*
*Completed: 2026-10-08*
