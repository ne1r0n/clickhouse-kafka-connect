# Phase 46: Insert Timeout Limits - Context

**Gathered:** 2026-11-05
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

An operator can trust what `clickhouseClientInsertTimeoutMs` says. The gg7 fix bounded a stalled client V2 `async=false` insert and left four cases open: an insert that keeps streaming progress headers, a write-side stall (the server stops reading the request body), the client V1 `future.get()` inserts and the non-insert requests. Each of them is now either bounded, failing within the bound as a retriable error, or written down as a limit with a measured number that shows why it cannot be bounded. The help text then names exactly what the setting bounds.

</domain>

<decisions>
## Implementation Decisions

### User decisions (research flags, answered 2026-11-05 via AskUserQuestion)

- **U-1 (V2 progress-streaming insert):** RECORD as a limit in FORK-DELTA and the help text with the measured numbers (escapes a 3000 ms bound, ends at 8141 ms with async_insert=0 and many blocks; default server async_insert=1 fails at 3057 ms). Do NOT add a whole-insert watchdog.
- **U-2 (client V1 inserts):** BOUND the three `future.get()` sites with `future.get(timeoutMs)`; retriable under both tolerances, no DLQ record. An operator-named `socket_timeout` in `jdbcConnectionProperties` still wins and skips the bound.
- **U-3 (operator gate):** the connector bound is skipped whenever the operator named `socket_timeout` (V2 and V1); required by success criterion 3.
- **U-4 (non-insert requests):** RECORD ONLY, each with its measured number. No client-level timeout, no Keeper bound. The anchor row counting 0 for a client-level timeout stays.
- **U-5 (V2 ping):** RECORD the `ping(timeout)` no-op and CORRECT FORK-DELTA item 14 ("ping has its own timeout" is false on V2). No code change.
- **U-6 (wording):** recorded items read "not bounded by decision, with the measured number", not "cannot be bounded".
- **U-7 (V2 write stall):** BOUND it with the write-phase deadline (armed before client.insert, disarmed once the body is sent; serialization time counts). Approved knowing it also fails a V2 insert still sending its body when the bound elapses, even with progress; consequence pinned and stated in help text and FORK-DELTA. Operator-named socket_timeout skips it.

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
