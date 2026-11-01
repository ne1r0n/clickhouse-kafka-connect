# Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs - Context

**Gathered:** 2026-10-29
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

With `bufferCount > 0` and a topic-renaming SMT such as `RegexRouter`, the connector commits consumer offsets for the partitions it was assigned, so a restart or rebalance resumes from the last flushed offset. Revoked partitions drop their buffered records, and the exactly-once buffer groups records by original partition. The non-buffered path and the non-renamed case behave exactly as before. `Processing.doLogic` is checked for the same defect and the outcome recorded (a state-key change stops for a user decision). Fixes upstream #777 on the fork (upstream PR #786 closed unmerged). Lands on client 0.9.6, after the Phase 34 tail merge, before the client bump.

Requirements: BUF-01..BUF-07. Research flag items: where the live pin lives (test vs integrationTest harness), the Processing.doLogic two-topics-one-target question measured with InMemoryState, and where the first start after the upgrade resumes (derive from Kafka consumer-group semantics; measure in the repo's own harness if cheap). Write the tests red first. The real-environment rerun is out of scope for this repository. Pushes nothing.

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
