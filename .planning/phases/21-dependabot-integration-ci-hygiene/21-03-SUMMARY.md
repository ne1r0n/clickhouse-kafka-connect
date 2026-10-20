---
phase: 21-dependabot-integration-ci-hygiene
plan: "03"
subsystem: testing
tags: [merge-invariants, anchors-gate, exemption, selftest, bash, dependabot]

requires:
  - phase: 21-dependabot-integration-ci-hygiene
    provides: "21-02: merge 82524e12 and E-04, the gate's single violation at 6473e24f"
provides:
  - ".planning/scripts/fork-anchor-exemptions.txt with one SHA- and row-pinned row for 6473e24f"
  - "verify-merge-invariants.sh reads ANCHOR_EXEMPTIONS_FILE from RANGE_END's tree, forgives only the invariant 3 count mismatch of an exactly matching row, and reports anchor_exempt=N"
  - "Self-test cases ST5-ST9 (twelve), proven fail-first against the pre-patch gate and a SHA-only mutant"
  - "E-08 and E-09 in 21-EVIDENCE.md"
affects: [21-05 phase close gate, 22, 23, 24 gate command and FORK-DELTA hand-off]

actuals:
  tokens: 5000
  tasks: 2
  commits: 2

plan_head_before: a044ddeda61f2016cb3c6c4627d0a5c6da31b838
plan_head_after: 046fe59defa5ed7cab9dc8cb29c25b4e36fd9d28

tech-stack:
  added: []
  patterns:
    - "Exemption keyed on the full 40-hex commit SHA plus the exact anchor path and fixed-string as that commit's own anchors copy spells them, matched with grep -qxF"
    - "Fail-first proof by running the new self-test against the pre-patch gate and against an over-broad mutant in throwaway worktrees"

key-files:
  created:
    - .planning/scripts/fork-anchor-exemptions.txt
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-03-SUMMARY.md
  modified:
    - .planning/scripts/verify-merge-invariants.sh
    - .planning/scripts/verify-merge-invariants-selftest.sh
    - .planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md

key-decisions:
  - "Only invariant 3's count-mismatch branch consults the exemption keys; PRE_TAG stays pre-v1.7.0-sync and the gate command for Phases 22-24 stays verify-merge-invariants.sh 54e63648 HEAD"
  - "A missing exemption file at the range end means no exemptions, so the gate fails closed on 6473e24f"
  - "ST4's needle now names the TARGET_TABLE_FILTER row it breaks, so it cannot pass on the exempted 6473e24f violation"

patterns-established:
  - "Another published bump of an anchored line is absorbed by adding a row to fork-anchor-exemptions.txt, never by editing the script"

requirements-completed: [DEPS-01]

coverage:
  - id: D1
    description: "The real-history gate over pre-v1.7.0-sync..HEAD exits 0 with empty stderr and anchor_exempt=1 result=PASS, and exits 1 with exactly the 6473e24f slf4j-reload4j line when the exemption is disabled"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-03 Task 1 automated verify (EXEMPTION-COMMITTED, GATE-EXEMPT-PASS)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Twelve self-test cases ST5-ST9 pass on the patched gate (16 PASS), fail in exactly seven named places on the pre-patch gate and in exactly two on a SHA-only mutant, with no ref moved"
    requirement: DEPS-01
    verification:
      - kind: other
        ref: "21-03 Task 2 automated verify (SELFTEST-RUNS-OK, SELFTEST-COMMITTED)"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-10-17
status: complete
---

# Phase 21 Plan 03: Anchor exemption for the published Dependabot commit Summary

**A SHA- and row-pinned exemption list in the merge-invariants gate forgives exactly the 6473e24f slf4j-reload4j anchor mismatch, so Phases 22-24 keep running the gate unchanged over the full range, with twelve fail-first self-test cases proving nothing broader is forgiven**

## Performance

- **Duration:** 15 min
- **Started:** 2026-10-17T08:49:03Z
- **Completed:** 2026-10-17T09:04Z
- **Tasks:** 2
- **Files modified:** 5 (two scripts changed, one exemption file created, the evidence file and this summary)

## Accomplishments

- `verify-merge-invariants.sh` gained `ANCHOR_EXEMPTIONS_FILE` (default `.planning/scripts/fork-anchor-exemptions.txt`, read from RANGE_END's tree, empty disables, missing means none). Strict row parse (full lowercase 40-hex SHA, non-empty path, pattern and reason, exactly four TAB fields) fails a malformed row with `ANCHOR EXEMPTION VIOLATION`. The summary line gains `anchor_exempt=N` before `result=`.
- Real history: default run exits 0, stderr empty, `provenance_paths=21 anchor_commits=386 anchor_rows=59 marker_commits=433 anchor_exempt=1 result=PASS`. With `ANCHOR_EXEMPTIONS_FILE=` it exits 1 with exactly `ANCHOR VIOLATION: 6473e24f build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0 (BLD-08)` (E-08).
- Self-test: 16 PASS and `selftest: no failures` on the patched gate. Against the pre-patch gate exactly ST7-exempted-row-silent, ST8-short-sha, ST8-uppercase-sha, ST8-missing-reason, ST8-empty-reason, ST8-extra-field and ST9-exact-row fail (9 PASS). Against a SHA-only mutant exactly ST6-other-row and ST7-other-bld08-row fail (14 PASS). `git for-each-ref` is byte-identical before and after (E-09).
- The default merge lookup still resolves 54e63648 as the oldest first-parent merge, with the DEPS-01 merge 82524e12 listed first. Self-test header line 6 and a comment above the lookup now say so.

## Task Commits

1. **Task 1: Exemption file and row-exact gate lookup (tracer)** - `a231d4f1` (chore)
2. **Task 2: Self-test cases ST5-ST9, E-08, E-09** - `046fe59d` (test)

**Plan metadata:** the `docs(21-03): complete ...` commit that follows this file.

## Files Created/Modified

- `.planning/scripts/fork-anchor-exemptions.txt` - header plus one data row for `6473e24f372a8b37dfd8cc8cf3124e6cff52edca`, `build.gradle.kts`, `slf4j-reload4j:2.0.17`
- `.planning/scripts/verify-merge-invariants.sh` - exemption parsing, count-mismatch lookup, `anchor_exempt` field
- `.planning/scripts/verify-merge-invariants-selftest.sh` - helpers `exemption_row`, `exemption_range`, `exemption_case`, fixtures `x_slf4j` and `x_both`, cases ST5-ST9, ST4 needle, header fix
- `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md` - E-08 and E-09 and their index verdicts

## Decisions Made

- Followed the plan as specified. The gate command for Phases 22-24 is unchanged, `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`, with `PRE_TAG` at `pre-v1.7.0-sync` (c61d90ce).
- Added a small `exemption_case` helper (build the range end, then `expect_failure`) to keep the twelve cases DRY, beyond the two helpers the plan named.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- My first attempt at the Task 1 file-state check mangled the awk program through nested shell quoting; it was rerun from a script file with the plan's original quoting and printed `EXEMPTION-COMMITTED`.

## Hand-off to Phase 24

`docs/FORK-DELTA.md`'s "mechanical checks" paragraph (near L206) should name `.planning/scripts/fork-anchor-exemptions.txt` beside `fork-anchors.txt`. Per the ROADMAP shared-files rule Phase 21 edits only its own FORK-DELTA rows, so this sentence belongs to Phase 24's completeness pass. Recorded in E-08. Phase 24's "PRE_TAG Phase 21 chose" means the `pre-v1.7.0-sync` default plus the exemption file.

## Known Stubs

None.

## Threat Flags

None. No new network, auth or file-access surface; the change is a local data file and a bounded script branch (T-21-10 to T-21-13 mitigations are covered by ST5-ST9 and the refs comparison).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plans 21-04 and 21-05 can rely on a gate that passes over the full range at HEAD.
- Nothing was pushed; `custom_build` was never checked out or committed to. `.planning/state.json` and `.planning/milestone.lock` were left unstaged.

---
*Phase: 21-dependabot-integration-ci-hygiene*
*Completed: 2026-10-17*

## Self-Check: PASSED

- FOUND: `.planning/scripts/fork-anchor-exemptions.txt`, `.planning/scripts/verify-merge-invariants.sh`, `.planning/scripts/verify-merge-invariants-selftest.sh`, `.planning/phases/21-dependabot-integration-ci-hygiene/21-EVIDENCE.md`
- FOUND commits: `a231d4f1`, `046fe59d` (ancestors of HEAD)
- All four automated verify commands printed their markers: EXEMPTION-COMMITTED, GATE-EXEMPT-PASS, SELFTEST-RUNS-OK, SELFTEST-COMMITTED.
