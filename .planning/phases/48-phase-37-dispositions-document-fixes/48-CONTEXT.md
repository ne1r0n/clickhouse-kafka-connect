# Phase 48: Phase 37 Dispositions & Document Fixes - Context

**Gathered:** 2026-11-06
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss), decisions taken after 48-RESEARCH.md

<domain>
## Phase Boundary

Each carried `src/main` advisory row, the null assertion, the 28 IN-05(b) items, the broad allow-list glob rows and the review Infos ends fixed (tests first, one commit each) or closed with a written reason in a new disposition ledger; the published documents stop pointing at planning identifiers and moved paths. See ROADMAP Phase 48 and 48-RESEARCH.md (U-1..U-11 there).
</domain>

<decisions>
## Implementation Decisions

### User decisions (answered 2026-11-06 via AskUserQuestion; the user's explicit approvals in their own words are the selected options below)

- **U-1 / U-2:** CLOSE 26 IN-02 (`shouldSkipDescribeRow` overloads) and 27 WR-01 (`Column.convertEnumValues` unknown enum label) with written reasons carrying the measured cost from 48-RESEARCH.md. No production change for either.
- **U-4 / U-5:** APPROVED both additive fixes, each its own commit, tests first: move the unused public `ESCAPED_NEWLINE` from `src/main` into the test fixtures (28 IN-01; one full suite on `latest` x V2 after it); add the null-binding test tying the `null` in `DescribeUnreadableEnumMockServerTest` to the `Enum type could not be read` log on both clients (no existing assertion changes; seen red via a non-enum failure).
- **U-6 / U-7:** APPROVED editing the published gg6 rollout note (row D7 planning identifier replaced by a public reference; FUT-05 clauses in row C2 and the correction notice replaced by a reference to FORK-DELTA item 17) and the gg1 note (43 IN-02). All of these go into Phase 49's independent review.
- **U-3, U-8..U-11 (accept all recommendations):** close 27 IN-01 as superseded by Phase 45 (pointers f13d8378, 0c8be1da, 876a9bd1); close the other `src/main` Infos with reasons plus ONE documentation sentence for the double-ERROR-line family (45 IN-04, 46 WR-05); add an additive allow-list unit test over every shipped list (NO edit to `.github/scripts/compare_confluent_archives.py`); keep the removed `KeyToValueTest` path in FORK-DELTA row 134 and name the surviving package path; name the new ledger by file plus `git ls-files '*48-DISPOSITION-LEDGER.md'` in item 20.

### Defaults taken by Claude

- **D-1:** the criterion-3 dry run is recorded as two evidence entries: the published gg6 vs gg7 pair with the gg7 list (0 unallowed, 0 stale), and the gg8 draft against the tip (11 exact rows, no wildcards); the tip vs gg7 list fails by design until Phase 49 writes the gg8 list.
- **D-2:** FORK-DELTA evidence paths under `.planning/phases/45..47` are handed to the milestone close (they move there); recorded in the ledger hand-off.
- **D-3:** this is the last phase that may edit `src/main`, tests, `build.gradle.kts` or `.github/`; any production diff beyond 28 IN-01's constant move stops for a user decision. No existing assertion is weakened.

### Claude's Discretion
Everything else, guided by ROADMAP, 48-RESEARCH.md and codebase conventions.
</decisions>

<code_context>
## Existing Code Insights
See 48-RESEARCH.md.
</code_context>
