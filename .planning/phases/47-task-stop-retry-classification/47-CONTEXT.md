# Phase 47: Task Stop & Retry Classification - Context

**Gathered:** 2026-11-05
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss), decisions taken after 47-RESEARCH.md

<domain>
## Phase Boundary

A stopping task reaches every cleanup step even when its delivery strategy's `stop()` throws; the CHANGELOG stops claiming more than the code does; an operator knows what happens to a retriable `DataException`; FUT-05 ends with a recorded decision. See ROADMAP Phase 47 and 47-RESEARCH.md.
</domain>

<decisions>
## Implementation Decisions

### User decisions (answered 2026-11-05 via AskUserQuestion)

- **U-1 (RTY-01):** RECORD the limit. No new config key, no budget code. Pin today's unbounded redelivery through `ProxySinkTask.put` for the retriable shapes (at least PIN-02 and RSK-02 shapes, the research measured 18) under `errors.tolerance=none` and `all`; document it in FORK-DELTA item 17 with the measured facts only (Connect never bounds a `RetriableException` from `put`; DLQ stays empty; task stays RUNNING; cadence is the worker's `offset.flush.interval.ms`).
- **U-2 (RTY-02):** NO validator. Keep the start-up WARN and its two pins unchanged; close FUT-05 with the reason; FORK-DELTA items 8, 17 and 24 stop listing it as open.
- **U-3 (LIF-02):** corrected `stop()` wording goes in a NEW `# 1.7.0_gg8, Unreleased` CHANGELOG section started in this phase (Phase 49 completes and dates it). The published gg7 section is NOT edited.

### Defaults taken by Claude (research recommendations; no user decision needed)

- **D-1 (LIF-01 shape):** plain `try`/`finally` in `ClickHouseSinkTask.stop()`; pin asserts both stops attempted and an exception surfaced, not which one. `close(partitions)` sibling gap is out of scope; record it as backlog.
- **D-2:** the gg7 rollout note rows F5/R8 and the "FUT-05 stays open." line are corrected in Phase 49 (hand-off recorded, not edited here). FORK-DELTA item 13 is corrected here.
- **D-3:** the pin is Docker-free with no Mockito (reflection on `proxySinkTask`/`deliveryStrategy` fields with a stub `DeliveryStrategy`, the `ProxySinkTaskStopUnitTest` idiom), seen red on the unchanged tree first and recorded before the fix; the fix is its own commit with its own gate. Any production diff beyond the `try`/`finally` stops for a user decision.

### Claude's Discretion
Everything else, guided by ROADMAP, 47-RESEARCH.md and codebase conventions.
</decisions>

<code_context>
## Existing Code Insights
See 47-RESEARCH.md.
</code_context>
