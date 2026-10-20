---
phase: 20-release-publication
plan: "04"
subsystem: release
tags: [changelog, fork-delta, review-disposition, milestone-close-record, raw-numbers, documentation]

requires:
  - phase: 20-release-publication
    provides: "20-03 rollout note (item titles, the L-38 wording) and the E-xx evidence file"
  - phase: 19-nullable-enum-tuple-mapping
    provides: "ledger L-31..L-41 and the eight 19-REVIEW findings to disposition"
  - phase: 18-writer-correctness
    provides: "ledger L-23..L-30"
provides:
  - "CHANGELOG.md section 1.7.0_gg2 (PUB-07, D-10) with Important Changes, Bug Fixes and Build and CI"
  - "docs/FORK-DELTA.md refreshed for v1.7.0_gg2: identity text, archived ledger paths, gg2 raw-numbers rows and commands, read-past-end wording"
  - "19-REVIEW-DISPOSITION.md with all eight findings dispositioned (open: 0, total: 8)"
  - "20-MILESTONE-CLOSE-RECORD.md carrying the deferred advisories, the fulfilled L-38 obligation, IN-04, the three manual checks, the audit instruction and the token reminder"
  - "Evidence E-40 (raw numbers and counts), E-41 (coverage audit), E-42 (path resolution)"
affects: [20-05 review, 20-06 rehearsal, 20-07 publication, 20-08 manual checks, complete-milestone]

actuals:
  tokens: 9380
  tasks: 3
  commits: 5

plan_head_before: 47ac11eabe4a5ffa961095c9a8339d64b9c263ef
plan_head_after: a443e4c34d3419ae91bb232e32ae90223ee4fe89

tech-stack:
  added: []
  patterns:
    - "Raw numbers are re-derived from git inside the verify, so the document cannot drift from the tree"
    - "A review ID reused by a different finding is disambiguated in every artifact: advisories are headed '19-REVIEW <ID>', the obligation is named by its ledger ID"

key-files:
  created:
    - .planning/phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md
    - .planning/phases/20-release-publication/20-04-SUMMARY.md
  modified:
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - .planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md
    - .planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md

key-decisions:
  - "CHANGELOG heading carries 2026-10-14, the UTC date the release content was frozen; no Dependencies subsection because gradle/libs.versions.toml is unchanged since be6b8861"
  - "WR-03 is fixed by commit 3e39867d, which touches only docs/FORK-DELTA.md; WR-01, WR-02, IN-01..IN-03 are deferred to the close record with written reasons (D-08)"
  - "The L-38 obligation is recorded as fulfilled under its own heading and never as a deferred advisory, because ROADMAP and STATE label it WR-02 and 19-REVIEW.md reused that ID"

patterns-established: []

requirements-completed: [PUB-07, ROLL-02]

coverage:
  - id: D1
    description: "Plain-language 1.7.0_gg2 CHANGELOG section above the untouched gg1 section, with the carried L-38 obligation"
    requirement: PUB-07
    verification:
      - kind: other
        ref: "Task 1 verify command (CHANGELOG-GG2-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "FORK-DELTA refreshed: identity text, resolvable paths, coverage of every Phase 17-19 code ID, measured raw numbers and counts, read-past-end wording"
    requirement: PUB-07
    verification:
      - kind: other
        ref: "Task 2 verify command (FORK-DELTA-GG2-OK); E-40, E-41, E-42"
        status: pass
    human_judgment: false
  - id: D3
    description: "All eight 19-REVIEW findings dispositioned and the milestone close record written"
    requirement: PUB-07
    verification:
      - kind: other
        ref: "Task 3 verify command (DISPOSITION-CLOSE-RECORD-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "CHANGELOG prose reads as plain operator language and agrees with the rollout note"
    requirement: ROLL-02
    verification: []
    human_judgment: true
    rationale: "Plain-language quality and agreement with the rollout note are judged by the 20-05 reviewer; the verify only checks structure, mandated facts and the absence of planning IDs"

duration: ~19min
completed: 2026-10-14
status: complete
---

# Phase 20 Plan 04: CHANGELOG, FORK-DELTA, review dispositions and close record Summary

**The 1.7.0_gg2 CHANGELOG section, a FORK-DELTA refreshed with git-derived raw numbers (119/18802/529 and 32/2042/143), all eight 19-REVIEW findings dispositioned, and a milestone close record that keeps the fulfilled L-38 obligation apart from the deferred advisories that reuse its WR-02 label.**

## Performance

- **Duration:** about 19 minutes (start time was not recorded; estimated from the session clock)
- **Tasks:** 3 of 3
- **Commits:** 5 task commits (measured from the ledger, `47ac11ea..a443e4c3`)
- **Files:** 3 modified outside `.planning/` plus the evidence and disposition files, 1 new close record

## Accomplishments

- `CHANGELOG.md` starts with `# 1.7.0_gg2, 2026-10-14`: an intro linking FORK-DELTA and the rollout note, then Important Changes (Nullable Enum and Tuple values, the single-field Tuple `DataException`, the fail-open WARN, the empty-CDC skip, the missing-value `DataException`, changed log lines, the in-flight batch consequence, the MBean and client name), Bug Fixes (message-less failures, date errors, value-free logs) and Build and CI. It names no planning ID, and everything from the gg1 heading on is byte-identical to `P20_BASE`.
- `docs/FORK-DELTA.md` now reads `v1.7.0_gg2` in the JMX and VERSION rows, points operators at the gg2 rollout note, and cites the archived Phase 13 and 14 files at their `.planning/milestones/` paths (three paths did not resolve before). The raw-numbers table gained two `after gg2` rows and two commands; the byte-identical and import-order counts were re-measured.
- The Nullable(Tuple) row says a read-past-end code (`CANNOT_READ_ALL_DATA` or `ATTEMPT_TO_READ_AFTER_EOF`) instead of a per-twin mapping the test does not pin (19-REVIEW WR-03), committed alone as `3e39867d`.
- `19-REVIEW-DISPOSITION.md` reads `open: 0, total: 8`. The WR-02 Source cell says it is the vacuous WARN-assertion finding and that the L-38 item ROADMAP calls WR-02 is fulfilled.
- `20-MILESTONE-CLOSE-RECORD.md` carries five advisories with a reason and a re-open trigger each, the fulfilled L-38 entry, IN-04 with its pin, three manual checks with pending outcomes for plan 20-08, the `/gsd-audit-milestone` instruction and the Keychain token reminder.

## Task Commits

1. **Task 1: 1.7.0_gg2 CHANGELOG section** - `018e5d8d` (docs)
2. **Task 2: WR-03 wording, committed alone** - `3e39867d` (docs)
3. **Task 2: FORK-DELTA refresh** - `ce9ea7ae` (docs)
4. **Task 2: FORK-DELTA measurements (E-40..E-42)** - `439df14f` (docs)
5. **Task 3: dispositions and close record** - `a443e4c3` (docs)

## Measured figures (E-40)

| Scope | Files | Insertions | Deletions |
|---|---|---|---|
| `src build.gradle.kts gradle .github`, `v1.7.0` to HEAD | 119 | 18802 | 529 |
| `src/main`, `v1.7.0` to HEAD | 32 | 2042 | 143 |

`src/main` files present at `v1.7.0`: 63, of which 38 are byte-identical and 25 differ. Of the 25, 14 differ only in import order and 11 carry fork logic; `Utils.java` moved from the first group to the second since gg1 (the null-message guard). Seven files exist only here (`MetadataFreeJsonWriter`, `DeclaredNullability` and five transform classes). The same method on `v1.7.0_gg1` reproduces the published 15 and 10. Tests make up 16288 of the 18802 insertions.

## Files Created/Modified

- `CHANGELOG.md` - the gg2 section above the gg1 section
- `docs/FORK-DELTA.md` - identity, paths, wording, raw numbers, counts, Pinned-by cells
- `.planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md` - frontmatter and table dispositions
- `.planning/phases/20-release-publication/20-MILESTONE-CLOSE-RECORD.md` - new
- `.planning/phases/20-release-publication/20-COMMAND-EVIDENCE.md` - E-40, E-41, E-42

## Decisions Made

- The CHANGELOG date is the UTC date of writing (2026-10-14); the release bundle shows it next to the planned publication date and it is not edited after the rehearsal.
- No Dependencies subsection: `git diff --quiet be6b8861 HEAD -- gradle/libs.versions.toml` exits 0.
- The FORK-DELTA gg2 figures are measured against HEAD because the tag does not exist yet; plan 20-07 re-runs the two tag commands.
- The ledgers of Phases 18 and 19 are named by file, not by path, in FORK-DELTA, so archiving the milestone leaves no dead path.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] FORK-DELTA Pinned-by cells missed DEBT-06 and DEBT-08**
- **Found during:** Task 2 (coverage audit, E-41)
- **Issue:** Both IDs were named only in the Known costs text, not in any Pinned-by cell. The plan expected none missing.
- **Fix:** Added `DEBT-08` to the metadata-free JSON row (its `MetadataFreeJsonWriterTest` carries the NanoTimestamp pin) and `DEBT-06` to the dependency-pins row (its `Lz4RoundTripLiveTest` carries the response-leg assertion). The audit reads clean afterwards.
- **Files modified:** `docs/FORK-DELTA.md`
- **Commit:** `ce9ea7ae`

**2. [Rule 1 - Bug] Three `.planning` paths in FORK-DELTA did not resolve**
- **Found during:** Task 2 (path resolution, E-42)
- **Issue:** The Phase 13 ledger and the Phase 13 and 14 disposition files had moved to `.planning/milestones/v1.7.0_gg1-sync-phases/` when that milestone was archived. The plan listed the ledger and disposition paths for correction (assumption A9 held).
- **Fix:** Pointed all three at the archived locations, as the plan directed; the evidence records the DEAD results from before the fix.
- **Commit:** `ce9ea7ae`

**3. [Rule 1 - Bug] "Utils.java" moved from import-order-only to logic**
- **Found during:** Task 2 (re-measuring the counts)
- **Issue:** The plan's counting method gave 14 import-order-only files and 11 with logic, against the published 15 and 10. The cause is `Utils.java`, which gained the null-message guard in Phase 18. The same method on `v1.7.0_gg1` reproduces 15 and 10, so the change is real, not a method difference.
- **Fix:** Wrote 14, 11 and "seven new files" into the Adopted-wholesale row and the comparison paragraph, and named `Utils.java` as the file that moved.
- **Commit:** `ce9ea7ae`

**Total deviations:** 3 auto-fixed (3 Rule 1, all documentation accuracy). **Impact:** none on scope; every figure now comes from git.

## Issues Encountered

None beyond the deviations above. `.planning/state.json`, `.planning/milestone.lock` and the stale `.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md` were already modified or untracked at the start and were left alone.

## Known Stubs

None.

## Threat Flags

None. The plan changed documents only.

## User Setup Required

None in this plan. The three manual checks and the Keychain token revocation are recorded in `20-MILESTONE-CLOSE-RECORD.md` for plan 20-08 and the milestone close.

## Next Phase Readiness

Everything outside `.planning/` that the release publishes is now final except the documents the review (20-05) may still correct. For the 20-05 reviewer: the CHANGELOG's plain-language wording and its agreement with the rollout note (the four wording points that 20-03 flagged still apply), and the FORK-DELTA statement that the gg2 figures reproduce against the tag (plan 20-07 checks that).

## Self-Check: PASSED

- Created and modified files exist (CHANGELOG.md, docs/FORK-DELTA.md, the disposition file, the close record, the evidence file).
- Commits `018e5d8d`, `3e39867d`, `ce9ea7ae`, `439df14f` and `a443e4c3` are ancestors of HEAD; `gsd check evaluation-scope --plan 20-04 --commits-only` resolved all five.
- Verify results: CHANGELOG-GG2-OK, FORK-DELTA-GG2-OK and DISPOSITION-CLOSE-RECORD-OK all printed; `git diff --name-only 47ac11ea HEAD` lists only CHANGELOG.md, docs/FORK-DELTA.md and `.planning/` files, and `src`, `build.gradle.kts`, `gradle`, `.github`, `VERSION` and both rollout notes are unchanged.

---
*Phase: 20-release-publication*
*Completed: 2026-10-14*
