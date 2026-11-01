# Phase 34: Upstream Tail Merge - Context

**Gathered:** 2026-10-28
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

The working tree holds upstream's untagged tail: `9bed6f79`, or whatever `upstream/main` reads immediately before the merge. It is merged in the fork's sync shape while the client is still 0.9.6, so every compile break is attributable to the tail. `additionalRetriableErrorCodes` is a supported, pinned setting, and leaving it unset changes nothing. In `ProxySinkTask`, the fork's JMX `connectorName` plumbing and upstream's `DBWriter` test seam coexist without an ambiguous constructor, and new anchor rows protect both. The BUF fix in Phase 35 edits `ChunkFlusher` again, and it starts from this tree.

Requirements: TAL-01, TAL-02, TAL-03, TAL-04, TAL-05. Research flag: if upstream/main moved past 9bed6f79, re-measure the conflict set; if upstream tagged a release past v1.7.0, STOP (tagged sync FUT-01, user decides scope). Pushes nothing.

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
