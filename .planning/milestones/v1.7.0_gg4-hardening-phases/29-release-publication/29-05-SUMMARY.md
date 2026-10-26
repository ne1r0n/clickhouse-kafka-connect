---
phase: 29-release-publication
plan: "05"
subsystem: release
tags: [release, changelog, fork-delta, milestone-close, review-dispositions, raw-numbers, consistency-check]

requires:
  - phase: 29-release-publication
    provides: "plans 29-01 to 29-04: the VERSION bump, the archive compare, E-01..E-30 and the gg3 to gg4 rollout note"
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: "25-REVIEW findings and the dependency, archive and guard rows of FORK-DELTA"
  - phase: 28-raw-identifier-names-in-discovery
    provides: "28-REVIEW findings, the L-58..L-66 ledger and Known costs item 11"
provides:
  - "CHANGELOG.md: the dated 1.7.0_gg4 section with the rename notice first, Known Issues, Bug Fixes and Build and CI; released sections untouched"
  - "docs/FORK-DELTA.md refreshed once: gg4 text, L-52..L-66 pointer, Pinned-by cells for 26 Phase 25-28 IDs, after-gg4 raw numbers, recounts, E-24 measurement in item 11, gg4 syncer paragraph"
  - "29-MILESTONE-CLOSE-RECORD.md: 16 carried findings, 28-REVIEW IN-03 fulfilled, three user reminders, milestone audit and housekeeping"
  - "25-, 26-, 27- and 28-REVIEW-DISPOSITION.md at open: 0 in the 25 file's schema"
  - "29-COMMAND-EVIDENCE.md E-40..E-43; E-03, E-04, E-07, E-24 flipped to yes"
affects: [29-06, 29-07, 29-08]

actuals:
  tokens: 18260
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "One canonical wording per shared fact: /tmp/gsd29-scripts/consistency.py compares the note, the CHANGELOG section and FORK-DELTA on version, client name, bundled versions, worker, KIP, nested-jar count and server version"
    - "FORK-DELTA coverage audit by script: every listed requirement ID is named in a Pinned-by cell and every anchor row it backs is covered"

key-files:
  created:
    - .planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md
  modified:
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - docs/ROLLOUT-v1.7.0_gg4.md
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-REVIEW-DISPOSITION.md
    - .planning/phases/26-size-named-tuple-elements-in-discovery/26-REVIEW-DISPOSITION.md
    - .planning/phases/27-writer-failure-path-test-debt/27-REVIEW-DISPOSITION.md
    - .planning/phases/28-raw-identifier-names-in-discovery/28-REVIEW-DISPOSITION.md

key-decisions:
  - "CHANGELOG date is 2026-10-23, the day the section was written (assumption A13); the 29-07 bundle compares it with the publication day"
  - "The after-gg4 raw numbers keep upstream v1.7.0 as baseline and say in one sentence that upstream's tail a6f1df83 (2 files, 11 insertions, 9 deletions in the wider scope) sits inside them"
  - "28-REVIEW IN-03 is recorded fixed against the CHANGELOG commit aad30ee6, with plan 29-04's note commit fae7e42a named in the close record"
  - "The 16 carried findings are deferred, not fixed; the doc halves of 25 IN-04 (d), 28 IN-02 and 28 IN-04 are written in FORK-DELTA and said so in the close record"

requirements-completed: [PUB-11]

plan_head_before: a867cbb00e600f6ea280b0d18f37403960233d6f
plan_head_after: 5e69fad6049af85b0318a5cbcb014a6fce65a59d
duration: 8min
completed: 2026-10-23
status: complete

coverage:
  - id: D1
    description: "CHANGELOG.md starts with a dated, plain-language 1.7.0_gg4 section (intro, Important Changes led by the rename notice, Known Issues, Bug Fixes with the four Phase 26-28 bullets minus planning IDs, Build and CI); the released tail is byte-identical to P29_BASE"
    requirement: PUB-11
    verification:
      - kind: other
        ref: "CHANGELOG-GG4-OK (verify command 1 of Task 1, run as /tmp/gsd29-scripts/p05/cmd1.sh)"
        status: pass
    human_judgment: false
  - id: D2
    description: "FORK-DELTA is refreshed once: stale text, ledger pointer, MBean and VERSION rows, Pinned-by coverage of 26 Phase 25-28 IDs with an empty gap list, three doc halves, after-gg4 rows equal to git diff --shortstat v1.7.0 HEAD, recounted figures and the gg4 syncer paragraph"
    requirement: PUB-11
    verification:
      - kind: other
        ref: "FORK-DELTA-ROWS-AND-PATHS-OK and RAW-NUMBERS-GG4-OK (verify commands 1 and 2 of Task 2)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The note, the CHANGELOG section and FORK-DELTA agree on v1.7.0_gg4, 33.7.2-jre, 5.6.5, cp-kafka-connect-base:7.9.7, KIP-1161, pre-v1.7.0_gg4, the 25 nested jars and server 26.9.19.9"
    requirement: PUB-11
    verification:
      - kind: other
        ref: "python3 -I /tmp/gsd29-scripts/consistency.py all (failed=0), E-43"
        status: pass
    human_judgment: false
  - id: D4
    description: "The close record holds the 16 carried findings under verbatim REVIEW titles with reason, owner, re-open trigger and source, 28-REVIEW IN-03 as fulfilled, and three reminder-only user items; the four dispositions read open: 0 with deferred rows naming the close record"
    requirement: PUB-11
    verification:
      - kind: other
        ref: "CLOSE-RECORD-29-OK and DISPOSITIONS-25-28-OK (verify commands of Task 3)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The wording of the CHANGELOG section, the Known Issues and the carried-finding reasons matches what the evidence supports and overclaims nothing"
    requirement: PUB-11
    verification: []
    human_judgment: true
    rationale: "A reading task: the fresh-context review rounds of plan 29-06 check the CHANGELOG section, FORK-DELTA and the close record against the evidence"
---

# Phase 29 Plan 05: CHANGELOG, FORK-DELTA and the milestone paper close Summary

**The `1.7.0_gg4` CHANGELOG section now leads with the rename notice and agrees with the rollout note and a once-refreshed FORK-DELTA (26 Phase 25-28 IDs pinned, after-gg4 raw numbers 277/29679/643 and 37/2877/241 measured, E-24 measurement in Known costs item 11), and the milestone's 16 unfixed review findings sit in a close record with reasons, owners and triggers while four disposition files read `open: 0`.**

## Performance

- **Duration:** 8 min (18:38Z to 18:46Z)
- **Tasks:** 3 (1 tracer, 2 auto)
- **Files modified:** 9 (1 created); nothing under `src/`, a test, `build.gradle.kts`, `gradle/` or `.github/` changed (`git diff --quiet c1dc6e59 HEAD -- src build.gradle.kts gradle .github` exits 0)

## Accomplishments

- **CHANGELOG (Task 1, tracer).** Dated `2026-10-23`. Intro names the upstream base `1.7.0`, the tail `a6f1df83`, FORK-DELTA and the gg4 note. Important Changes, in order: the rename notice (field names as written in `CREATE TABLE`, DEFAULT stored with no log line at the root, element loud, the order of the rename), the clean-stop rule per direction, the two writer texts and the alerts that stop matching, the bundled versions with the Connect API compile-only and the oldest verified worker, the MBean and client name, and the rollback archive warning. Known Issues: the unreadable declared type, the unmeasured control characters and dots, the accepted Jackson residual, the `insertFormat=json` text. Bug Fixes: the four existing bullets without `(backlog 999.4)` and `(backlog 999.5)`. Build and CI: eight bullets. Everything from `# 1.7.0_gg3, 2026-10-20` down equals `P29_BASE`.
- **FORK-DELTA (Task 2).** Intro and operator pointer to gg4, ledger pointer `L-52`..`L-66`, MBean and VERSION rows, Pinned-by additions (BLD-16, DEPS-02, DEPS-03 on the dependency rows, DEBT-13 and DEBT-14 on the CI row with their rules, DEBT-16 on the test-doubles row, DESC-09 on the describe-builder row), the three doc halves (`SinkTaskStatistics` outside the 21-class list, `pre-release.yaml` Gradle `7.4.2`, item 11 with the E-24 escapes), two `after gg4` rows, two commands, the upstream-tail sentence, rewritten comparison and "One number" paragraphs, and the gg4 syncer paragraph with the two moved sentences.
- **Raw numbers.** Wider scope 277 files, 29679 insertions, 643 deletions; `src/main` 37, 2877, 241; scopes add up (229 + 3 + 37 + 1 + 3 + 4 files). Recount: 63 present at `v1.7.0`, 36 byte-identical, 14 import-only, 13 fork logic, 10 files only here; since gg3 `ClickHouseFieldDescriptor` and `Table` left the byte-identical group. Skip sources still 7 `@Disabled` and 1 `assumeTrue`.
- **Close record and dispositions (Task 3).** 16 carried findings under the REVIEW files' own titles; 28-REVIEW IN-03 fulfilled (commits `fae7e42a`, `aad30ee6`); six next-milestone lines; two inferred claims with triggers; three user reminders (PRs #4 and #10, the OPS-04 check, the end of the freeze) with web-UI steps; audit and housekeeping sections. The 25, 26, 27 and 28 files: `open: 0`, totals 7, 15, 4, 9, 35 rows in one table schema; 28 IN-03 `fixed` with Source `aad30ee6`.

## Task Commits

1. **Task 1 (tracer): CHANGELOG section and the consistency check** - `aad30ee6` (docs)
2. **Task 2: FORK-DELTA refresh** - `17775f4b` (docs)
3. **Task 3: close record and the 25-28 dispositions** - `5e69fad6` (docs)

**Plan metadata:** the SUMMARY commit that follows this file.

## Files Created/Modified

- `CHANGELOG.md` - the `1.7.0_gg4` section
- `docs/FORK-DELTA.md` - refreshed rows, numbers and syncer paragraph
- `docs/ROLLOUT-v1.7.0_gg4.md` - one sentence of item 1 corrected (deviation 1)
- `.planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md` - new
- `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md` - E-40..E-43, four verdicts flipped
- the four `*-REVIEW-DISPOSITION.md` files of Phases 25 to 28

## Decisions Made

See `key-decisions`. In addition: the Adopted-wholesale `src/main` row needed no edit, because Phase 26 had already written 36 of 63, 27, 14, 13 and ten files, and the recount reproduces those figures exactly; the "One number" paragraph was the stale text (33, 14 and 19) and now says 37, 14 and 23.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The note named a slash and a double quote on client V1 as unmapped tables**
- **Found during:** Task 1 (writing the CHANGELOG sentence on tables that start to land)
- **Issue:** Item 1 of `docs/ROLLOUT-v1.7.0_gg4.md` said a table that was not mapped included "a tab, carriage return, slash or double quote on client V1". The note's own table and `28-BEHAVIOUR-LEDGER.md` L-59 say that on V1 only a tab and a carriage return left the table unmapped, while a slash or a double quote was read as U+FFFD so the table was mapped and no field matched.
- **Fix:** The sentence now lists a double quote on V2 and a tab or carriage return on V1, and adds the V1 slash and double quote case. The CHANGELOG states the same.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg4.md`
- **Verification:** plan 29-04's verify commands re-run (NOTE-FRAME-OK, ITEMS-1-7-OK, ITEMS-2-4-OK, ITEMS-5-6-OK, address and secret scan clean); recorded in E-40
- **Committed in:** `aad30ee6`

### Planned-versus-done details

- **Task 2 coverage edits went into existing rows.** The plan names rows by line number; the file had moved since the research, so rows were found by content. DEPS-02 also changed one sentence of the dependency-pins row ("all from Dependabot commits that DEPS-01 and DEPS-02 merged"), because the Connect API, Jackson, gson and commons-lang3 values came with the nine DEPS-02 merges.
- **E-08 stays `pending (29-05)`.** The plan flips E-03, E-04, E-07 and E-24 only; E-08 backs the close record's OPS-04 reminder and the 29-07 and 29-08 bundles, which re-read the freeze, so its verdict is left for them.
- **The tag commands of the raw-numbers block could not be run on the tag.** `v1.7.0_gg4` does not exist yet; E-43 says so and plan 29-08 re-runs the two commands.

**Total deviations:** 1 auto-fixed (Rule 1), 3 planned-detail changes.
**Impact on plan:** none on the content asked for; the one fix makes the note agree with its own table and the ledger.

## Issues Encountered

None beyond the deviation. No Gradle build or test ran in this plan (documents only). Nothing was pushed, tagged on origin, merged or commented on; PRs #4 and #10 were not touched.

## Known Stubs

None. The documents have no placeholder text; the CHANGELOG, FORK-DELTA and close record were scanned for it.

## Threat Flags

None. The consistency script and the verify commands closed T-29-18 (the three documents agree), CHANGELOG-GG4-OK compares the released tail byte for byte (T-29-19), the close record carries all four fields per finding and the dispositions name it (T-29-20), and the secret-pattern scan found nothing (T-29-21).

## User Setup Required

None - no external service configuration required. The three reminders in the close record need nothing now.

## Next Phase Readiness

- Plan 29-06 spawns the fresh-context reviewer over the note, the older gg1 and gg2 notes, the CHANGELOG section, FORK-DELTA and the close record. The checks that scan the note for addresses need the server-version mask (plan 29-04 deviation 1). Reviewer fix text is to be checked against code before it is applied.
- The CHANGELOG date `2026-10-23` is compared with the publication day in the 29-07 bundle (A13).
- Plan 29-08 re-runs the two `v1.7.0_gg4` shortstat commands on the tag and the rollback script on the real merge commit; E-08 is re-read there.
- Local state outside this plan's scope remains dirty and untouched: `gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`.

## Self-Check: PASSED

Files verified on disk: `CHANGELOG.md`, `docs/FORK-DELTA.md`, `.planning/phases/29-release-publication/29-MILESTONE-CLOSE-RECORD.md`, the four disposition files, `29-COMMAND-EVIDENCE.md` (E-40 to E-43 present). Commits verified as ancestors of HEAD: `aad30ee6`, `17775f4b`, `5e69fad6`. Verify commands on the committed tip printed CHANGELOG-GG4-OK, FORK-DELTA-ROWS-AND-PATHS-OK, RAW-NUMBERS-GG4-OK, CLOSE-RECORD-29-OK and DISPOSITIONS-25-28-OK.

---
*Phase: 29-release-publication*
*Completed: 2026-10-23*
