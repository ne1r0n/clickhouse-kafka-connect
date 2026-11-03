---
phase: 43-document-fixes-phase-37-dispositions
plan: "02"
subsystem: docs
tags: [rollout-notes, fork-delta, publication-facts, github-rest-api, evidence]

requires:
  - phase: 43-document-fixes-phase-37-dispositions
    provides: plan 43-01 evidence file E-01..E-05, the three byte-copied helpers
  - phase: 38-release-publication
    provides: the gg6 release record whose facts E-06 re-reads
provides:
  - "gg1 rollout note item 6 carries the gg6 evidence labels and names both client lines (gg1 half of WR-08)"
  - "gg6 rollout note: correction notice above the summary and FUT-05 named in row C2 (user decision)"
  - "FORK-DELTA describes the gg6 publication in the past tense, reproduces its numbers from the tags, and states the closed facts of items 12, 22, 23 and the protobuf row"
  - "43-COMMAND-EVIDENCE.md E-06..E-08"
affects: [43-03, 44-release-notes]

actuals:
  tokens: 9500
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns:
    - "published-note edits located by content, one Edit per fixed old string, gated by line-scope checks on the commit diff"
    - "facts read live from unauthenticated GitHub REST GETs before they are written into documents"

key-files:
  created:
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-02-SUMMARY.md
  modified:
    - docs/ROLLOUT-v1.7.0_gg1.md
    - docs/ROLLOUT-v1.7.0_gg6.md
    - docs/FORK-DELTA.md
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-COMMAND-EVIDENCE.md

key-decisions:
  - "FUT-05 is named only in row C2 and the correction notice of the gg6 note, per the user's recorded decision 'gg6 note C2 + gg7 handoff (Recommended)'; the gg7 handoff stays with plan 43-03"
  - "Row C2's new sentence is scoped to the Kafka Connect redelivery of the RetriableException, because retryCount (helper-client pings and queries only) and errors.retry.timeout (read only to log a WARN) govern something else; its label stays 'read from the code'"

patterns-established:
  - "Each published fact is written with its source: read from the GitHub REST API, read from git, or read from the release record"

requirements-completed: [DOC-01]

plan_head_before: 12d98995c36e41d61a91adbc1ab3cff88659f5a2
plan_head_after: 247019ba0948de4c9232245508f2931c5d9f31ab

coverage:
  - id: D1
    description: "Item 6 of the gg1 note no longer says 'of any line', labels the ZSTD decode measured and the native/temp-folder fact as measured by the maintainers in research and not re-run, links rows D9 and D13, and names client lines 0.9.6 and 0.11.0; exactly the preface line and the item 6 line changed"
    requirement: DOC-01
    verification:
      - kind: other
        ref: "Task 1 automated verify (P43-02-T1-OK): fixed-string checks and the LS scope check on commit 965e646f (E-07)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Row C2 of the gg6 note names FUT-05 with the FORK-DELTA link in a sentence scoped to the redelivery, label unchanged; a correction notice sits above the summary paragraph; FUT-05 occurs exactly twice"
    requirement: DOC-01
    verification:
      - kind: other
        ref: "Task 1 automated verify: retry-key greps (3 setRetry sites, helper loops pingV1 pingV2 queryV1 queryV2, CONNECTOR_RETRY_TIMEOUT only in ClickHouseSinkConfig) and scope removed=1 added=3 (E-07)"
        status: pass
    human_judgment: true
    rationale: "Phase 44's independent reviewer reads the published wording of the notice and row C2 for tone and for claims no stronger than their evidence"
  - id: D3
    description: "FORK-DELTA lines 5, 146 and 295 and the protobuf row and items 12, 22 and 23 describe the gg6 publication in the past tense with the run and release ids; the gg6 commands name the tags and reproduce 331/39665/1161, 40/2989/272, 79/7939/478 and 7/112/31; the 14 raw-table rows are unchanged; exactly ten physical lines changed"
    requirement: DOC-01
    verification:
      - kind: other
        ref: "Task 2 automated verify (P43-02-T2-OK): 14 old phrases absent, numbers check against the tags, LS scope removed=10 added=10, raw table rows byte-identical, links OK (E-08)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The publication facts (run 41298074815, release 430511927, PRs #10 and #17 closed unmerged by dependabot[bot] on 2026-11-01, one tests run on the merge, the two tags, equal trees of the merge and its second parent) were re-read before any text was written"
    requirement: DOC-01
    verification:
      - kind: other
        ref: "E-06: 7 unauthenticated GET requests, all 30 checks equal the release record"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-11-02
status: complete
---

# Phase 43 Plan 02: Rollout-note and FORK-DELTA corrections Summary

**Two published rollout notes and FORK-DELTA corrected against facts re-read live from the GitHub REST API: gg1 item 6 carries the gg6 evidence labels, gg6 row C2 names FUT-05 with a correction notice, and FORK-DELTA tells the gg6 publication in the past tense with tag-based commands that reproduce the table.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-11-02T19:35:23Z
- **Completed:** 2026-11-02T19:39:00Z
- **Tasks:** 2
- **Files modified:** 4 (3 documents, 1 evidence file) plus this summary

## Accomplishments

- E-06: seven unauthenticated GET requests (no token, no `gh`) returned exactly the expected publication facts: publish run `41298074815` (`Release custom_build (publish)`, `workflow_dispatch`, head `72e8ccff`, success), release `430511927` by `github-actions[bot]` with one asset of 13558692 bytes, PRs #10 and #17 closed unmerged by `dependabot[bot]` on 2026-11-01, one `tests.yaml` run on the merge (`41297502026`, push, success), both tags on origin, and equal trees of the merge and its second parent over `src`, `build.gradle.kts`, `gradle` and `.github`. No mismatch, so no stop.
- Row C2 basis read from the code before the sentence was written: `getRetry()` occurs at exactly three `.setRetry(csc.getRetry())` sites, the helper client's `retry` field is read only in `pingV1`, `pingV2`, `queryV1` and `queryV2`, and `CONNECTOR_RETRY_TIMEOUT` occurs only in `ClickHouseSinkConfig.java`.
- gg1 note (D1 `965e646f`): preface and item 6 changed; `of any line` is gone; the zstd-jni native and temp folder are labelled as measured by the maintainers in research and not re-run, with a link to rows D9 and D13 of the gg6 note.
- gg6 note (D1): the correction notice names the two rows that differ from tag `v1.7.0_gg6` (D7 and C2); row C2 states that no connector setting caps the Kafka Connect redelivery of the `RetriableException` and names FUT-05, label unchanged.
- FORK-DELTA (D2 `870fe9d8`): ten physical lines in the past tense; the three gg6 `git diff --shortstat` commands name `v1.7.0_gg6` and `v1.7.0_gg5 v1.7.0_gg6`, and run on the tags they print the published rows exactly; items 12, 22 and 23 and the protobuf row state the closed facts.

## Task Commits

1. **Task 1 (tracer): E-06 facts, gg1 half of WR-08, FUT-05 in gg6 row C2** - `965e646f` (docs, the two notes) and `c96ba6d3` (docs, evidence E-06 and E-07).
2. **Task 2: FORK-DELTA past tense, tag-based commands, items 12, 22, 23** - `870fe9d8` (docs, FORK-DELTA) and `247019ba` (docs, evidence E-08).

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan).

## Files Created/Modified

- `docs/ROLLOUT-v1.7.0_gg1.md` - the preface sentence and the item 6 sentence (2 lines).
- `docs/ROLLOUT-v1.7.0_gg6.md` - the correction notice paragraph (plus one blank line) and row C2 (1 line).
- `docs/FORK-DELTA.md` - ten physical lines (intro paragraph, protobuf row, raw-numbers paragraph, two command lines, gg5-to-gg6 paragraph, items 12, 22, 23, syncer paragraph).
- `.planning/phases/43-document-fixes-phase-37-dispositions/43-COMMAND-EVIDENCE.md` - index rows E-06..E-08 flipped to pass; entries E-06, E-07, E-08.

## Decisions Made

- FUT-05 is named only in row C2 and the correction notice, as the user decided; the pointer line after "Before you upgrade" was not edited, and the gg7 handoff ("FUT-05 stays open") is plan 43-03's.
- The C2 sentence is scoped to the redelivery the row describes and spells out what `retryCount` and `errors.retry.timeout` do instead, so it claims nothing beyond what the three greps show.
- The compare result and the by-hand rollback tag in the FORK-DELTA syncer paragraph are taken from `38-RELEASE-RECORD.md` (re-checked in this run: `exit 0, 0 unallowed, 0 stale rows`, rollback tag pushed by hand after the publish run); the other publication facts are those of E-06.

## Deviations from Plan

None - plan executed exactly as written. Two cosmetic notes: the plan's `<automated>` blocks were extracted to scripts and run under `/bin/bash` with every clause kept; and the E-08 entry's UTC timestamp line reads `19:38:30Z`, about four seconds after the clock actually read when the entry was written (it was estimated rather than read from `date -u`). The evidence rule forbids editing a recorded entry, so it stays; it does not affect any fact in the entry.

## Issues Encountered

None. `origin/custom_build` equalled `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` at every gate; no rate limit hit (7 of 60 requests used); no JVM or Gradle run was needed.

## Known Stubs

None.

## Threat Flags

None - documentation edits only; no endpoint, auth path, file access or schema change. The evidence holds ids, sizes, dates, SHAs and bot logins only, and `43-evidence-check.py secrets` reported 0 token shapes before each evidence commit.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 43-03 (items 19, 20 and 21 of FORK-DELTA, the disposition ledger, the gg7 handoff including "FUT-05 stays open"). Pointers it can consume: `notes_commit` `965e646f934cded3a1d1a855edc19c8a0cdfe145` (E-07) and `forkdelta_commit` `870fe9d8f918d67516666e8272960ab1df628621` (E-08).
- Phase 44's independent reviewer should read the notice and row C2 of the gg6 note and item 6 of the gg1 note (judgment-dependent deliverable D2).
- DOC-01 is completed by this plan for the document half; DOC-02 is not touched. `.planning/config.json` and `.planning/state.json` stay modified for the orchestrator; nothing was pushed.

## Self-Check: PASSED

- Files exist: the three documents, the evidence file and this summary.
- Commits exist on HEAD: `965e646f`, `c96ba6d3`, `870fe9d8`, `247019ba`.
- `git diff` of `src`, `VERSION`, `CHANGELOG.md` and `build.gradle.kts` since the plan start is empty.

---
*Phase: 43-document-fixes-phase-37-dispositions*
*Completed: 2026-11-02*
