# Phase 40: Helper Client Closing - Context

**Gathered:** 2026-11-01
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

**Goal**: A task that stops releases every ClickHouse helper client it created, so connector restarts and rebalances stop leaking HTTP connection pools (F-1). There are three sites:
- the writer's long-lived `chc`;
- the throwaway client that the V1 path builds for each batch in `getMutationRequest`;
- the client that `KeeperStateProvider` creates in exactly-once mode.

A failed `start()` and a second `stop()` are both safe. The writer half and the Keeper half land as separate commits, each behind its own gate, so the exactly-once half can be reverted alone. The Keeper half leaves the state read and write paths byte-identical.

**Depends on**: Phase 39 (dependencies settled, classification pins in place)

**Requirements**: CLS-01, CLS-02, CLS-03, CLS-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* A Mockito-free counting seam (`CountingHelperClient` plus a `createHelperClient` factory) counts opens and closes at all three sites on V1 and V2, including a failed `start()` and a double `stop()`. Each case was run red on the unchanged tree, and the result was recorded before any fix (CLS-04).
  2. *[Docker-free, live]* The writer half is one commit (CLS-01, CLS-02):
     - `ClickHouseWriter.stop()` closes `chc` once, after the table-sync executor has stopped.
     - A second `stop()`, and a `stop()` with `chc` null after a failed `start()`, neither throw nor close twice.
     - Every failure path of `start()` closes the client it opened.
     - The V1 `getMutationRequest` path closes its throwaway client on every batch, and the returned node still serves the insert.
     - The field keeps the name `chc`.
     `testUnit` and a full suite pass on this commit before the Keeper half starts.
  3. *[Docker-free, live]* The Keeper half is its own later commit. `StateProvider` has a default no-op `close()`. `KeeperStateProvider.close()` closes a client it created and leaves an injected one open. `ProxySinkTask.stop()` stops the writer, unregisters the MBean and closes the state provider in `try/finally`, so a throw in one step still runs the others. The diff of `getStateRecord` and `setStateRecord` is empty. `testUnit` and a full suite, including the exactly-once tests, pass on this commit (CLS-03).
  4. *[record, Docker-free]* The FORK-DELTA rows and `fork-anchors.txt` rows land in the same commits as the code they describe, and the anchors gate passes.

**Plans**: TBD

**Research flag**: Settle these before planning:
- whether `Client.close()` on client 0.11.0 is idempotent (the research did not probe it). The null guard and the double-`stop()` pin hold either way;
- whether any existing test uses the writer after `stop()`;
- which three tests reflect on the `chc` field. Keep the name;
- what Connect does after a `start()` that throws, which is unverified. Close inside the writer's own failure paths, and do not rely on Connect calling `stop()`.

**Note**: Keep the diffs additive (`try/finally`, null guards). Upstream PRs #861, #855 and #857 touch the same files; re-read them before planning. `ClickHouseHelperClient` builds a V2 client even for `clientVersion=V1`, so the V1 cases also close a V2 client.

Phase gate: the per-half gates above, then the anchors invariant.

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
