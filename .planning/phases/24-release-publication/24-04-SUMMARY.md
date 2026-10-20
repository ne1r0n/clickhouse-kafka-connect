---
phase: 24-release-publication
plan: "04"
subsystem: release
tags: [release, changelog, fork-delta, raw-numbers, coverage-audit, security-record, evidence]

requires:
  - phase: 24-release-publication
    provides: plan 24-01 evidence E-01..E-05, plan 24-02 evidence E-10..E-16 and the plan 24-03 rollout note docs/ROLLOUT-v1.7.0_gg3.md
provides:
  - "CHANGELOG.md section 1.7.0_gg3, 2026-10-20 in the gg2 shape, released sections byte-identical to P24_BASE (PUB-09)"
  - "docs/FORK-DELTA.md: rows for every Phase 21-23 ID, Known costs 6 (measured), 10 and 11 (new), after-gg3 raw numbers, gg3 syncer paragraph, exemption rule"
  - "22-SECURITY.md R-22-02 aligned to the D-03 measurement, threats_open still 0"
  - "Evidence E-30..E-33; E-03, E-04, E-05, E-15, E-16 flipped to yes"
affects: [24-05, 24-06, 24-07]

actuals:
  tokens: 23889
  tasks: 3
  commits: 3
plan_head_before: 673f25e18aeb8add401037ab5274b2b13cb31d30
plan_head_after: 068882ce9f315695810c467578d4d89f49e3eaab
commits: 3

tech-stack:
  added: []
  patterns:
    - "One canonical wording per shared fact: the CHANGELOG and FORK-DELTA restate the rollout note, and a script fails on a disagreement"
    - "Coverage audit: every Phase 21-23 code ID is named in a Pinned-by cell, and every anchor row it backs is covered"

key-files:
  created: []
  modified:
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - docs/ROLLOUT-v1.7.0_gg3.md
    - .planning/phases/22-writer-record-data-exposure/22-SECURITY.md
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md

key-decisions:
  - "CHANGELOG date is 2026-10-20 (UTC, the day written); plan 24-06 compares it with the publication date (assumption A10)"
  - "FORK-DELTA Known costs 10 and 11 are written as knowingly open with the fix sketch pointed at the ROADMAP backlog entries, and no workaround"
  - "The size0 consistency rule was applied to every prose unit, so two FORK-DELTA rows and one note bullet gained the codes 32 or 33"

requirements-completed: [PUB-09, ROLL-03]

coverage:
  - id: D1
    description: "CHANGELOG 1.7.0_gg3 section: four sections in order, Map-key exception (D-04 branch 1), size-element Known Issue with codes 32 and 33, no planning IDs, no escaped-name line, released tail byte-identical"
    requirement: PUB-09
    verification:
      - kind: other
        ref: "plan 24-04 Task 1 verify command CHANGELOG-GG3-OK (E-30)"
        status: pass
    human_judgment: true
    rationale: "Operator-facing wording of a security-relevant exception and of a no-fix known issue; the pre-gate review should read it against the rollout note and E-03"
  - id: D2
    description: "FORK-DELTA rows for DEPS-01, CI-18, CI-19, WRT-13..15, HARN-08, COLS-12..14, DEBT-10, DEBT-11; every anchor row they back is covered (uncovered IDs: [])"
    requirement: PUB-09
    verification:
      - kind: other
        ref: "plan 24-04 Task 2 verify commands FORK-DELTA-ROWS-OK and FORK-DELTA-PATHS-OK (E-31, E-32)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Known costs item 6 states the measured Map-key result with server 26.9.19.9 and the live test; 22-SECURITY R-22-02 says the same, threats_open 0, one row, original cell count"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "FORK-DELTA-ROWS-OK (item6, r22) and E-03 flipped to yes"
        status: pass
    human_judgment: false
  - id: D4
    description: "Raw numbers for gg3 (130/22783/534 and 33/2245/145), order before/gg1/gg2/gg3, recount 38 identical / 14 import-only / 11 logic / 8 files only here, skip inventory 7 and 1, cross-document consistency script clean"
    requirement: PUB-09
    verification:
      - kind: other
        ref: "plan 24-04 Task 3 verify command RAW-NUMBERS-GG3-OK (E-33)"
        status: pass
    human_judgment: false

duration: 9min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 04: CHANGELOG, FORK-DELTA and the security record for v1.7.0_gg3 Summary

**A plain-language 1.7.0_gg3 CHANGELOG section, FORK-DELTA rows and Known costs 6, 10 and 11 for every Phase 21-23 patch with an empty coverage-gap list, R-22-02 aligned to the measured Map-key result, and refreshed gg3 raw numbers, with the three operator documents checked against each other by script**

## Performance

- **Duration:** 9 min
- **Started:** 2026-10-20T16:53:25Z
- **Completed:** 2026-10-20T17:03Z
- **Tasks:** 3
- **Files modified:** 5 (no new files); nothing under `src`, `build.gradle.kts`, `gradle` or `.github` changed

## Accomplishments

- **CHANGELOG (`7caf8a94`).** `# 1.7.0_gg3, 2026-10-20` sits above the gg2 section and everything from `# 1.7.0_gg2, 2026-10-14` down is byte-identical to `P24_BASE`. Important Changes: the value-free error text (with the headline "the only exception we know of is a Map key" on the `input_format_binary_read_json_as_string` route, measured on 26.9.19.9), the new unpaired-element WARN, the MBean and client name, the shape-limited clean stop for a rollback. Known Issues: one bullet for the Tuple element named `size` or `size0` (and `size<digits>` under an `Array`), codes 32 or 33, no fix. Bug Fixes: the backslash-n and line-break Nullable(Tuple) pairing, and the client V1 mapping refresh. Build and CI: three dependency versions leave the shipped jar unchanged, the pin-guard test, the inline-mock removal. No escaped-name line.
- **FORK-DELTA rows (`177f247c`).** Intro lines 5 and 7, ledger pointer (L-42..L-48 in `22-BEHAVIOUR-LEDGER.md`, L-49..L-51 in `23-BEHAVIOUR-LEDGER.md`), MBean and VERSION rows at gg3, CI row with CI-18 and CI-19, slf4j-reload4j row with DEPS-01 and the runtime-classpath-only statement, BLD-14 row re-established for Lombok 1.18.48 (273, 273 and 274 warnings; per-entry identical jar), catalog row with lombok `1.18.48` (upstream `1.18.46`) and spotless `8.10.4` (upstream `8.10.1`), jacoco row recording that `DeclaredNullability` and `WithheldMessageException` are not in the 15-class list, WRT-13 row with `JsonAsStringKeyResidualLiveTest`, declared-nullability row with DEBT-10 and DEBT-11, and a new Test harness row for `CountingHelperClient`, `ScriptedWriteFailureWriter`, `ThrowableTexts`, `InMemoryDLQ.connectHeaders` and `DeclaredNullabilityFixtures` (HARN-08).
- **Known costs.** Item 6 now states the measured Map-key result (server 26.9.19.9, client V2, the three destinations and two non-destinations, Connect-typed values only) and points at the live test; item 10 (999.4) and item 11 (999.5) are new, each closed as knowingly open with the fix sketch's location in the ROADMAP backlog. "For the next syncer" gains the gg3 paragraph and the `fork-anchor-exemptions.txt` rule.
- **22-SECURITY R-22-02.** The parenthesis "(verifier measurement, review WR-01)" is replaced by the measured statement and a pointer to `JsonAsStringKeyResidualLiveTest` and E-03. One row, six pipes, `threats_open: 0`.
- **Coverage audit (E-31).** All twelve IDs are named in at least one Pinned-by cell (DEPS-01 and WRT-13 in two rows each), and the ten anchor rows backed by one of them are covered: `uncovered IDs: []`. Path resolution (E-32): ten `.planning/` paths cited, none dead.
- **Raw numbers (`068882ce`, E-33).** Measured at the commit that carries the rows: `src build.gradle.kts gradle .github` 130 files, 22783 insertions, 534 deletions; `src/main` 33 files, 2245 insertions, 145 deletions. The gg1, gg2 and before rows reproduce from their tags. Recount: 63 files at `v1.7.0`, 38 byte-identical, 25 differing (14 import-order only, 11 with fork logic), 8 files only here (the seventh and eighth are `DeclaredNullability`-era plus `WithheldMessageException`). Skip inventory re-counted at 7 `@Disabled` and 1 `Assumptions.assumeTrue`, still 18.
- **Evidence flips.** E-03, E-04, E-05, E-15 and E-16 moved from `pending (24-04)` to `yes`. E-04 also backs the 21-IN-03 disposition, which plan 24-05 writes.

## Task Commits

1. **Task 1: CHANGELOG section** - `7caf8a94` (docs; `CHANGELOG-GG3-OK`, E-30)
2. **Task 2: FORK-DELTA rows, Known costs, R-22-02** - `177f247c` (docs; `FORK-DELTA-ROWS-OK`, `FORK-DELTA-PATHS-OK`, E-31, E-32, five verdict flips)
3. **Task 3: raw numbers, recount, consistency** - `068882ce` (docs; `RAW-NUMBERS-GG3-OK`, E-33; also the three consistency fixes of Deviation 1)

**Plan metadata:** the commit of this SUMMARY and the state update.

## Files Created/Modified

- `CHANGELOG.md` - the 1.7.0_gg3 section (+60 lines)
- `docs/FORK-DELTA.md` - rows, Known costs 6/10/11, raw numbers, syncer guidance
- `docs/ROLLOUT-v1.7.0_gg3.md` - one phrase in the item 7 bullet ("with code 32 or 33")
- `.planning/phases/22-writer-record-data-exposure/22-SECURITY.md` - R-22-02 row text
- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` - E-30..E-33 and five flipped verdicts
- Scratch, not committed: `/tmp/gsd24-scripts/cov.py`, `recount.py`, `consistency.py`, `fd-edit-task2.py`, `fd-edit-task3.py`, `pin-guard-24-04.sh`; `/tmp/gsd24/exec/v03-*.sh` and `v04-*.sh` (the plans' verify commands extracted for re-runs)

## Decisions Made

- The CHANGELOG exception bullet is a separate bullet after the headline bullet, so the headline stays a plain statement and only the exception names a server version.
- Item 10 names `Table.SIZE_FIELD_MATCHER` beside `shouldSkipDescribeRow`; the pattern lives in `Table.java`, which is byte-identical to upstream, which is why the fix "belongs in upstream-region code".
- Item 11 says "describeTable returns no table" for a double quote rather than the plan's "describeTableV2 fails", because that is what E-16 measured on both releases.
- The gg3 paragraph of "For the next syncer" and the intro lines are written in the past tense of publication, as the gg2 text was before its tag existed. They are true only after plan 24-07 publishes `v1.7.0_gg3` and `pre-v1.7.0_gg3`; the approval bundle in plan 24-06 should say so.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug: documents disagreeing] Three prose units mentioned `size0` without the codes 32 and 33**
- **Found during:** Task 3, first run of `consistency.py`
- **Issue:** The plan's rule is that every paragraph or bullet that mentions `size0` also mentions 32 and 33. The rollback bullet of the note (item 7), the FORK-DELTA `shouldSkipDescribeRow` row and the declared-nullability row mentioned `size0` without them.
- **Fix:** The note bullet now reads "its inserts fail as before, with code 32 or 33". The two FORK-DELTA rows name the codes and point at Known costs item 10. Plan 24-03's five checks were re-run on the committed tip and print `ROLLOUT-FRAME-OK`, `ITEMS-1-7-OK`, `ITEMS-2-3-OK`, `ROLLOUT-T3-OK`, `ROLLOUT-FINAL-OK`.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg3.md`, `docs/FORK-DELTA.md`
- **Committed in:** `068882ce`

**2. [Rule 1 - Bug: verify trap] Two sentences of my own contained the phrase the verify forbids**
- **Found during:** Task 2, before the verify run
- **Issue:** `FORK-DELTA-ROWS-OK` fails when "was not measured" appears anywhere in the file; my item 6 and item 10 each said something "was not measured" about a different thing.
- **Fix:** Reworded to "has not been measured". No meaning changed.
- **Committed in:** `177f247c`

### Plan text adapted

- A9 held: the phrase "of the N `src/main` files that differ" is unchanged, so the regex needed no adaptation.
- The plan's Task 1 intro asks for the gg2 shape; the intro lines follow the gg2 section line for line with the gg2/gg3 names swapped.

---

**Total deviations:** 2 auto-fixed (Rule 1), 0 architectural.
**Impact on plan:** None on scope. Both are corrections to my own text or to a one-phrase consistency gap. `build.gradle.kts` was not edited (the jacoco gap is recorded, not fixed).

## Issues Encountered

- A background run of `.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (not part of the plan, started as a sanity check) did not finish within two minutes because it evaluates every commit of the range; I stopped it. Plan 24-06 owns the invariants run on the publication tip.

## Known Stubs

None.

## Threat Flags

None. Only documents and planning records changed. T-24-12: `consistency.py` and `RAW-NUMBERS-GG3-OK` pass and the first failure was fixed at its source. T-24-13: `CHANGELOG-GG3-OK` compares the released tail with `P24_BASE` byte for byte. T-24-14: `FORK-DELTA-ROWS-OK` checks `threats_open: 0`, exactly one R-22-02 row and six pipes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 24-05 can write the 21-IN-03 disposition flip from E-04 and E-05 (BLD-14 row text is in place) and the other carried findings.
- Plan 24-06 must re-run `CHANGELOG-GG3-OK`, `FORK-DELTA-ROWS-OK`, `FORK-DELTA-PATHS-OK`, `RAW-NUMBERS-GG3-OK` and `/tmp/gsd24-scripts/consistency.py` on the publication tip, compare the CHANGELOG date `2026-10-20` with the publication date, and mention the forward-looking wording above in the bundle.
- Plan 24-07 re-runs the two `v1.7.0 v1.7.0_gg3` shortstat commands once the tag exists; they must equal 130/22783/534 and 33/2245/145 if nothing under `src`, the build files or `.github` changes before the merge.
- For the pre-gate review: CHANGELOG bullets 1 and 2 (the Map-key exception) and the Known Issues bullet (no fix, no workaround) carry the judgment-heavy statements.

## Self-Check: PASSED

- Files: `CHANGELOG.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg3.md`, `22-SECURITY.md`, `24-COMMAND-EVIDENCE.md` exist; `### E-30:` to `### E-33:` present; no `pending (24-04)` verdict remains.
- Commits `7caf8a94`, `177f247c`, `068882ce` are ancestors of HEAD; `git rev-list --count 673f25e1..HEAD` = 3 before this SUMMARY.
- Acceptance: `CHANGELOG-GG3-OK`, `FORK-DELTA-ROWS-OK`, `FORK-DELTA-PATHS-OK`, `RAW-NUMBERS-GG3-OK` and plan 24-03's five checks printed on the committed tip; `git diff --name-only 07bf1e76 HEAD` outside `.planning` lists `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg3.md` and the Phase 24 live test only.

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
