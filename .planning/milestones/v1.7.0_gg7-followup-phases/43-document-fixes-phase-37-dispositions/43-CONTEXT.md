# Phase 43: Document Fixes & Phase 37 Dispositions - Context

**Gathered:** 2026-11-02
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

**Goal**: The published documents stop carrying the errors found at the gg6 close, and every row that Phase 37 left open has a decision and an owner, so the release phase starts with no undecided debt. A row that would change `src/main` changes only on the user's approval. Without approval, the row is deferred with an owner and a re-open trigger.

**Depends on**: Phase 42 (documents and dispositions are written against the final code)

**Requirements**: DOC-01, DOC-02

**Success Criteria** (what must be TRUE):

  1. *[record]* `docs/ROLLOUT-v1.7.0_gg1.md` carries the gg1 half of WR-08. Item 6 no longer says "measured" for the `zstd-jni` native and the executable temp folder, and no longer says client V1 "of any line" where two lines were observed. FORK-DELTA lines 6, 139 and 286 (as numbered at the gg6 close, so locate them by content) describe the gg6 publish run in the past tense. FUT-05 is named by ID where the rollout note carries it (DOC-01).
  2. *[record]* A ledger has one row for each Phase 37 item: the `increment-version.yml` header comment, advisory rows 26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 and 28 IN-05(b), the broad allow-list glob rows (UF-37-3) and each open review Info. Each row is either fixed, with a commit pointer, or deferred with an owner and a re-open trigger (DOC-02).
  3. *[operator]* Each row that would change `src/main` shows the user's approval, in their own words, before its fix. The fix lands tests first, in its own commit. A row without approval is deferred.
  4. *[Docker-free]* The `increment-version.yml` header comment names the guard class that actually guards the file, and the pin guard passes. No existing assertion is deleted or weakened. Phase gate: cache-busted `testUnit`, the anchors invariant, and a full suite on `latest` × V2 if `src/main` changed.

**Plans**: TBD

**Research flag**: Settle which file names FUT-05 by ID. One option is rows C2 and C3 of the published `docs/ROLLOUT-v1.7.0_gg6.md`, a published-note edit like gg4's corrections to the gg1 and gg2 notes. The other is the gg7 note, in which case RLS-03's review checks it there. The row sources are `milestones/v1.7.0_gg6-maintenance-phases/37-ci-hardening-advisory-debt/` (`37-ADVISORY-LEDGER.md`, `37-REVIEW*.md`) and `38-release-publication/38-03-SUMMARY.md` (WR-08).

**Note**: This is the last phase that may edit `.github/`, tests, or `src/main` (with approval). The published notes edited here go into Phase 44's independent review.

</domain>

<decisions>
## Implementation Decisions

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

</decisions>

<code_context>
## Existing Code Insights

Codebase context will be gathered during plan-phase research.

</code_context>

<specifics>
## Specific Ideas

No specific requirements — discuss phase skipped. Refer to ROADMAP phase description and success criteria.

</specifics>

<deferred>
## Deferred Ideas

None — discuss phase skipped.

</deferred>
