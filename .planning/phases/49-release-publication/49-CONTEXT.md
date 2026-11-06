# Phase 49: Release & Publication - Context

**Gathered:** 2026-11-06
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss). The phase is a recipe (the gg7 publication recipe in ROADMAP) plus user-owned gates, so the only choices are defaults below. No new user question was needed; every push, the merge, the publish run and the scratch-branch deletion stay the user's, asked at their gate.

<domain>
## Phase Boundary

`v1.7.0_gg8` is prepared and, only on the user's direct word, published on `custom_build`. This phase edits documents, `VERSION`, the gg8 allow-list and `fork-anchors.txt` rows only. It edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file. A defect found here goes back to its phase as a gap, or is deferred with a reason, an owner and a re-open trigger in the milestone close record.

Requirements: RLS-01 (VERSION via `bump_version.py`, CHANGELOG, FORK-DELTA, anchors), RLS-02 (gg8 allow-list generated last, dry run 0 unallowed 0 stale), RLS-03 (`docs/ROLLOUT-v1.7.0_gg8.md`, independent fresh-context reviews), RLS-04 (two equal green full suites on pinned `latest` x V2, testUnit Docker-free, anchors gate), RLS-05 (user-approved `--no-ff` merge, one green `tests.yaml`, publish run), RLS-06 (delete the two `scratch/clt-07-*` branches after a separate confirmation).
</domain>

<decisions>
## Implementation Decisions

### User decisions (answered 2026-11-06 via AskUserQuestion; the selected options are the user's explicit words)

- **U-1 (rollback tag):** retag local `pre-v1.7.0_gg8` from `baa404cd` to `94834763` before the gates, so it equals gg7's meaning; the tail gate re-runs on the final head and the retag is recorded in the evidence. Local only, never pushed by this step.
- **U-2 (origin merge):** make one local tree-neutral merge of `origin/custom_build` (`94834763`) into `sync/v1.7.0` as the first change of Plan 1 (the allow-list baseline otherwise resolves to gg6, about 120 UNALLOWED). Keep the scratch-branch dry run on origin; its push is asked separately at the approval bundle.
- **U-3 (date):** set the CHANGELOG heading date to the UTC merge date in a one-line commit once the push day is known, before the first push; the reviewer is told about that one-line change.
- **U-4 (origin writes):** the orchestrator runs each origin write only after the user's direct word for that exact step, one step per word (as gg7).

### Defaults taken by Claude

- **D-1 (order):** docs and counts first, the allow-list generated last from the final tree (a later `src/main` or build change would change the row count; regenerate, never copy), then the two full suites and the review rounds, then the gate. Docker-free work is separated from live work.
- **D-2 (gg8 allow-list):** 14 exact rows and no wildcard measured at Phase 48 tip (the 12 of E-14 plus `DeclaredNullability$1.class` and `DeclaredNullability$Assignment.class`); totals against gg7 `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501`. Regenerate from the final tree; any wildcard row must pass `AllowListWildcardRuleUnitTest` (two literal path segments after stripping `outer/`).
- **D-3 (CHANGELOG):** complete the existing `# 1.7.0_gg8, Unreleased` section; add the three upstream-tail items of `1.7.1, Unreleased` (#838 escaped `/` in DESCRIBE, `additionalRetriableErrorCodes`, record-context serialization failures, with the fork's value-free policy stated), the Phase 46 timeout limits, the Phase 47 retry outcome (a `RetriableException` from `put` is redelivered without bound, a limit by decision) and the FUT-05 decision (no validator), and Known Issues for the two-ERROR-line family per FORK-DELTA item 14 (as reworded in Phase 48), naming the removed public `DeclaredNullability.ESCAPED_NEWLINE` (user-approved U-4). The heading date is the merge date in UTC, set at the gate and not before.
- **D-4 (rollout note):** one new `docs/ROLLOUT-v1.7.0_gg8.md` in the gg7 note's shape, every effect labelled `measured`, `read from the code` or `derived`, and each claim pointing at evidence that survives archiving (FORK-DELTA items, tests by name, tags), not at `.planning` paths.
- **D-5 (raw numbers):** FORK-DELTA "The raw numbers" is refreshed once, with the gate totals measured on the final tree: testUnit 2325 (measured at Phase 48 review fix), full suite re-measured here (expected 2928 with the 18 named skips, derived until the suites run), anchors as the mirror reads.
- **D-6 (independent review):** before the push gate the orchestrator spawns a fresh `gsd-code-reviewer` on `docs/ROLLOUT-v1.7.0_gg8.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md` and the gg6 and gg1 notes edited in Phase 48; rounds repeat until one finds nothing, at most three. A fourth round needs the user. Every finding's disposition is recorded.
- **D-7 (gates):** the gates run orchestrator-inline. Nothing is pushed, merged, published or deleted by an executor. Plans stop at each gate and the orchestrator asks the user, in their own words, one gate at a time.
- **D-8 (before the gate):** recheck upstream (tags past `v1.7.0`, open PRs #861, #855, #854, #788) and `origin/custom_build...sync/v1.7.0` divergence (the user merges Dependabot PRs on origin directly).
- **D-10 (research recommendations taken as defaults):** FORK-DELTA gets one measured sentence of gate totals at the end of "The raw numbers" plus the refreshed shortstat table; the RLS-04 anchors gate is the tail form once on the explicit final SHA plus the mirror `--rows 218`; the 128/143 test-tree figure is recounted (115) and corrected; only two upstream-tail CHANGELOG items are new (#838, #640; `additionalRetriableErrorCodes` already shipped in gg6); the allow-list is measured, not copied (projected 17 exact rows); ROADMAP step 1 left-count wording (1, not 0) is corrected in the close record.
- **D-9 (Phase 48 hand-offs):** the E-18 inventory of Phase 48 applies: gg8 CHANGELOG Known Issues and rollout carry the item-14 wording, raw numbers use 2325, and the 12 `.planning/phases/45..47` evidence paths in FORK-DELTA move at the milestone close.

### Claude's Discretion
Wording, section order and the evidence-entry layout, guided by ROADMAP, the gg7 note and Phase 43 and 47 precedent.
</decisions>

<canonical_refs>
## Canonical References

- `.planning/ROADMAP.md` Phase 49 and "User-Owned Gates"
- `.planning/REQUIREMENTS.md` RLS-01..RLS-06
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md` E-14, E-15, E-18, E-19 (hand-off inventory and counts)
- `.planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md` E-11 (Phase 49 hand-offs)
- `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md` (tail gate)
- `.planning/scripts/` (allow-lists, `fork-anchors.txt`, `bump_version.py` if present) and `.github/scripts/compare_confluent_archives.py`
- `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg7.md`, `docs/ROLLOUT-v1.7.0_gg6.md`, `CHANGELOG.md`
- gg7 publication recipe: `.planning/milestones/` gg7 phase records and memory notes on publication gotchas
</canonical_refs>

<code_context>
## Existing Code Insights
Documents and scripts only; see the phase research.
</code_context>

<specifics>
- Tag `pre-v1.7.0_gg8` is local only; the recipe pushes it by name after the publish run.
- Origin currently has `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` and no gg8 refs.
</specifics>

<deferred>
- Backlog P1-P16 test-scope advisories, the Confluent `7.9.1-ccs` question, the JSON route wrapping of non-Jackson IOExceptions, `close(partitions)` without `finally`, the `keepFirst` idiom in `stop()` and the first-put matrix through `putDirect`: milestone-close backlog, not this phase.
</deferred>
