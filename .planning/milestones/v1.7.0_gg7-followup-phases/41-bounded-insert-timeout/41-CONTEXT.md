# Phase 41: Bounded Insert Timeout - Context

**Gathered:** 2026-11-02
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

**Goal**: With `async=false`, an insert into a server that stops answering fails after `clickhouseClientInsertTimeoutMs` as a retriable error, instead of blocking the task forever (F-2). An operator who sets `socket_timeout` in `jdbcConnectionProperties` keeps that value. A red pin proves the hang before the fix, and the cases that stay unbounded are written down. There is no new config key, and `async_insert` is not touched.

**Depends on**: Phase 40 (the same `ClickHouseWriter` and helper-client seam)

**Requirements**: PIN-01, TMO-01, TMO-02, TMO-03

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `StubClickHouseServer` has a stall responder. A pin wrapped in `assertTimeoutPreemptively` drives a V2 insert with `async=false` against it. On the unfixed tree the pin was run and failed instead of hanging, and the run is recorded (PIN-01).
  2. *[Docker-free]* After the fix, a per-insert `socket_timeout` taken from `clickhouseClientInsertTimeoutMs` at the V2 insert sites makes the same pin pass: the insert ends within the bound with a retriable error. A second test puts `socket_timeout` in `jdbcConnectionProperties` and shows that the operator's value is used. `ClickHouseSinkConfig` is unchanged, and so is the `async_insert` (CFG-02) anchor (TMO-01).
  3. *[Docker-free]* Through `ProxySinkTask.put`, the stalled insert is pinned under `errors.tolerance=none` and `errors.tolerance=all`. Under both it surfaces as retriable: the task is not failed and no DLQ record is written. If either outcome is not retriable on the fixed tree, the phase stops for the user, because classification changes belong to RSK-02 (TMO-02).
  4. *[record]* FORK-DELTA records two limits: a write-side stall (the peer stops reading the request body) is not bounded, and the three V1 `future.get()` sites stay unbounded. The F-2 rollout item is recorded with its evidence for Phase 44. It states the retriable failure after the timeout, the duplicate risk with `exactlyOnce=false` and the `socket_timeout` override (TMO-03).

**Plans**: TBD

**Research flag**: Settle these before planning:
- Verify on client 0.11.0 that a per-insert `socket_timeout` (through `InsertSettings`) reaches the socket on the insert path, and list every V2 insert site. If it does not reach the socket, stop for the user: the client-level alternative bounds every request, and the user did not choose it.
- The behaviour change: with the default `clickhouseClientInsertTimeoutMs`, an `async=false` insert that legitimately runs longer starts failing and retrying. TMO-01 takes the existing setting as the bound, and the rollout item states the change.
- Trace how an operator's `socket_timeout` reaches the client today, so the override test runs the real path.

**Note**: Never use `async=true` or `async_insert` as the fix: each timed-out batch would leak a thread and a connection. No Mockito.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

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
