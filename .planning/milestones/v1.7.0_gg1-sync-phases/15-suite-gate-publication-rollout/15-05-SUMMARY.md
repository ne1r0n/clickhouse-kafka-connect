---
phase: 15-suite-gate-publication-rollout
plan: 05
subsystem: release-documents
tags: [rollout, changelog, exactly-once, keepermap, operator-docs, pub-01, roll-01]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: 15-01 code (Debezium Timestamp/NanoTimestamp, Nullable Enum/Tuple WARN text), 15-02 review IN-01, 15-03 VERSION and ledger entries L-20..L-22
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: behaviour ledger L-01..L-19 and the R-01 backfill procedure copied verbatim
provides:
  - docs/ROLLOUT-v1.7.0_gg1.md, a generic eight-item operator checklist with a self-check per item
  - CHANGELOG.md section "# 1.7.0_gg1, 2026-10-10" above upstream's 1.7.0 section
affects: [15-06, 15-07, 15-08]

plan_head_before: 56049d5e7c96d3d9997ed113219f52b4d1c39786
plan_head_after: cf34c07a9704b29971cb9348f0e00872a76819ff

actuals:
  tokens: 7000
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Per-release rollout file beside FORK-DELTA, one file per sync, never rewritten"
    - "Every operator self-check query is followed by a sentence stating what an empty and a non-empty result mean"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg1.md
  modified:
    - CHANGELOG.md

key-decisions:
  - "CHANGELOG date is 2026-10-10 (the day this plan ran); plan 15-07 re-checks it before the rehearsal push and it must not change afterwards"
  - "The exactly-once check is marked required only for explicit insertFormat=json plus bypassRowBinary=true with Struct/CDC records under exactlyOnce=true (L-22) and precautionary elsewhere; the inference (deduplication hashes parsed part data) and the non_replicated_deduplication_window caveat are stated in the doc"
  - "The Debezium timestamp fix is attributed as 13-REVIEW WR-02 everywhere; bare 'WR-02 strictness' is used only for the nested-null DataException (L-10)"
  - "The rollout doc states that no automated test pins stored values for explicit json plus bypassRowBinary=true (15-03's L-22 finding) and tells operators to verify on a copy of their data"
  - "The rollout doc states that the Nullable Enum/Tuple WARN also fires on routes where the defect cannot occur (15-02 IN-01)"

requirements-completed: [ROLL-01, PUB-01]

coverage:
  - id: D1
    description: "An operator can read docs/ROLLOUT-v1.7.0_gg1.md alone and, for each of eight changes, see what changed, run a self-check against their own setup and know what to do; it names no internal host, connector, address, credential or production version"
    requirement: ROLL-01
    verification:
      - kind: other
        ref: "Task 1 automated checks: ROLLOUT-CONTENT-OK (all 41 required strings), BACKFILL-VERBATIM-OK, PUBLIC-SCAN-CLEAN"
        status: pass
    human_judgment: false
  - id: D2
    description: "The two R-01 backfill steps are copied as written from the behaviour ledger, and the exactly-once query's columns match KeeperStateProvider's CREATE statement"
    requirement: ROLL-01
    verification:
      - kind: other
        ref: "grep -qxF of the two ledger lines against the doc, plus grep of 'minOffset BIGINT, maxOffset BIGINT, state String' in KeeperStateProvider.java"
        status: pass
    human_judgment: false
  - id: D3
    description: "CHANGELOG.md starts with '# 1.7.0_gg1, 2026-10-10' in upstream's Important Changes / Bug Fixes / Dependencies style, names R-01, WR-02 strictness, compression, lz4 1.12.0 and the four other bumps, links FORK-DELTA and the rollout doc, and leaves every upstream line unchanged"
    requirement: PUB-01
    verification:
      - kind: other
        ref: "Task 2 automated checks: CHANGELOG-SECTION-OK, UPSTREAM-UNTOUCHED (git diff -U0 e4755e38..HEAD has no removed line)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The rollout doc's prose is accurate and sufficient for an operator who has never seen the planning files: required/precautionary split, the Nullable Enum/Tuple wording, the self-check queries"
    verification: []
    human_judgment: true
    rationale: "The queries were derived from source and the research notes, not all run on a live server (the KeeperMap query needs keeper_map_path_prefix; the profile-constraint effect was not probed; the http_user_agent column for the client name was not probed). Wording adequacy is a maintainer judgment"

duration: 9min
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 05: Operator rollout checklist and CHANGELOG section Summary

**A public, placeholder-only eight-item rollout checklist (exactly-once pre-upgrade check against the KeeperMap state table, R-01 backfill copied verbatim, JSON-route notes, nested-null and Nullable Enum/Tuple limitation, TYPE-08, compression, MBean name, rollback) plus a `# 1.7.0_gg1, 2026-10-10` CHANGELOG section in upstream style.**

## Performance

- **Duration:** 9 min
- **Completed:** 2026-10-10
- **Tasks:** 2
- **Files modified:** 2 (one new, one extended)

## Accomplishments

- `docs/ROLLOUT-v1.7.0_gg1.md` has eight items in the plan's order. Each has "What changed", "Am I affected?" and "What to do" sub-headings (8, 8 and 8 by `grep -c`). Every query or grep self-check is followed by a sentence on what an empty result and a non-empty result mean.
- Item 1 gives the stop/query/upgrade-when-empty procedure and the exact query `SELECT `key`, minOffset, maxOffset, state FROM `<zkDatabase>` WHERE state != 'AFTER_PROCESSING' ORDER BY `key``. It marks the check required for explicit `insertFormat=json` plus `bypassRowBinary=true` with Struct/CDC records under `exactlyOnce=true` and precautionary for every other route, with a one-line reason per row, the deduplication inference, and the `non_replicated_deduplication_window` caveat.
- Item 2 carries the ledger's two backfill steps byte-for-byte plus an example `DELETE`/`TRUNCATE` marked as an example.
- Item 4 states the silent outcome (`stored as NULL or shifted`), quotes the WARN text from 15-01, names `targetTableFilter`, and notes the WARN also fires on routes where the defect cannot occur.
- `CHANGELOG.md` gained a 48-line section at the top; the diff against the phase-start base removes no line.

## The eight item titles

1. Exactly-once pre-upgrade check
2. R-01 behaviour change
3. JSON-route type notes
4. Nested-null DataException and the Nullable Enum/Tuple limitation
5. TYPE-08 and Array(Tuple)
6. Compression
7. JMX MBean name and client name
8. Rollback

## Task Commits

1. **Task 1: docs/ROLLOUT-v1.7.0_gg1.md** - `90e41d0f77a779ef1f9d7fc848d09c3fe8a6274d` (docs)
2. **Task 2: CHANGELOG 1.7.0_gg1 section** - `cf34c07a9704b29971cb9348f0e00872a76819ff` (docs)

**Plan metadata:** recorded in the docs commit that follows this file.

## CHANGELOG date

`2026-10-10`, from `date -u +%Y-%m-%d` on the day this plan ran. Plan 15-07 re-checks it before the rehearsal push; it must not change after that push.

## Scan and check results

| Check | Result |
|---|---|
| `ROLLOUT-CONTENT-OK` (41 required strings) | every string present (the loop printed no `MISSING:` line) |
| `BACKFILL-VERBATIM-OK` | printed |
| `PUBLIC-SCAN-CLEAN` (IPv4 shape, internal-looking host, credential-shaped assignment) on the rollout doc | printed; no hit |
| Same IPv4/hostname scan on the CHANGELOG section | clean |
| `CHANGELOG-SECTION-OK` (19 strings and the heading regex) | every string present, heading regex matches |
| `UPSTREAM-UNTOUCHED` | printed |
| `# 1.6.0_gg1` heading in CHANGELOG | count 0 |
| `WR-02` occurrences in the CHANGELOG section | two: `13-REVIEW WR-02` (Debezium fix) and `WR-02 strictness` (L-10) |
| `WR-02` occurrences in the rollout doc | two, same split |

Note on the verify script: the plan's first `<verify>` block puts three `<automated>` commands for Task 1 in one block; they were run individually, as in 15-03.

## Decisions Made

See `key-decisions` above. The two worth restating for the next plans:

- The doc tells operators that no test pins stored values for explicit json plus `bypassRowBinary=true`. FORK-DELTA (15-06) should say the same, and the 15-07 approval bundle may want to list it among knowingly-open items.
- Item 8 states that `pre-v1.7.0-sync` is `c61d90ce`, the tree of the `v1.6.0_gg1` release. `git ls-remote --tags origin` shows `v1.6.0_gg1` at `c61d90ce` already; `pre-v1.7.0-sync` is created locally and is pushed in plan 15-08.

## Deviations from Plan

None - plan executed exactly as written.

The plan's item 5 said to "quote the before and after message formats from the ledger's TYPE-08 section"; the ledger describes the new message in prose, so the exact text was taken from `buildRowBinaryUnsupportedTypeMessage` in `ClickHouseWriter.java` (`Column [<name>] has unsupported type [INT256] for RowBinary serialization. Use insertFormat=json or bypassRowBinary=true instead.`). This is a sourcing choice, not a change of content.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. T-15-13 mitigated by placeholders and the scan above (15-07 repeats a tree-wide scan). T-15-14 mitigated: the query's columns were asserted against `KeeperStateProvider`'s CREATE statement and the backfill steps against the ledger. T-15-15 mitigated: the Nullable Enum/Tuple text says values may be stored as NULL or shifted with no error and that null entries fail the batch.

## Caveats carried forward

- The KeeperMap query is derived from source, not executed: KeeperMap needs `keeper_map_path_prefix` on the server (the doc says so).
- The effect of a profile constraint on `network_compression_method` (assumption A5) was not probed; the doc presents the `system.settings_profile_elements` query as a check, not a verdict.
- The doc says the client name appears in `system.query_log.http_user_agent`; this was not probed on a live server. If wrong, only the item 7 query needs correcting.
- The deduplication inference (assumption A1) is stated in the doc as an inference.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 15-06 can link `docs/ROLLOUT-v1.7.0_gg1.md` from FORK-DELTA and reuse the wording on L-22 and the WARN's route coverage.
- 15-07 must re-check the CHANGELOG date before the rehearsal push, and run its tree-wide public-content scan over both files.

## Self-Check: PASSED

- `docs/ROLLOUT-v1.7.0_gg1.md` and `CHANGELOG.md` exist; commits `90e41d0f` and `cf34c07a` are ancestors of HEAD; `git rev-list --count 56049d5e..HEAD` was 2 before this file.
- All Task 1 and Task 2 automated checks and acceptance criteria were re-run after the commits and printed their success tokens.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*
