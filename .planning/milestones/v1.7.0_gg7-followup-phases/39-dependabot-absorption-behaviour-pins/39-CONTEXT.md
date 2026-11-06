# Phase 39: Dependabot Absorption & Behaviour Pins - Context

**Gathered:** 2026-11-01
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

**Goal**: The code phases start from a tree that already holds both open Dependabot updates, each in its own commit behind its own gate, so a later red is attributable to one change. The same tree pins the behaviour that later phases could disturb without noticing: how a wrapped `DataException` is classified through `ProxySinkTask.put`, and how client 0.11.0 forwards a `custom_*` key. The integration harness stops counting connectors that are not its own (F-3). `src/main` does not change.

**Depends on**: Nothing in this milestone. Phase 38 published `v1.7.0_gg6` (`72e8ccff`). Dependabot PRs #21 (`setup-gradle` 6.4.0 → 6.4.1) and #22 (gson 2.14.0 → 2.15.0) are open on `origin`. Measured 2026-11-01: `origin/custom_build...sync/v1.7.0` counts 1 and 13, and the 1 is the gg6 merge `72e8ccff` itself.

**Requirements**: DEP-01, DEP-02, PIN-02, PIN-03, PIN-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free, live]* gson 2.15.0 is on `runtimeClasspath` (by `dependencyInsight`) from its own commit, which also updates the gson row of `docs/FORK-DELTA.md`. On that commit `testUnit` and the full suite on `latest` × V2 each pass twice, the second run with `--rerun`, with the 18 named skips (DEP-01).
  2. *[Docker-free]* `tests.yaml`, `nightly-integration-tests.yaml`, `release.yaml` and `pre-release.yaml` each pin `gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1`, the commit and not the tag object `7296c772…`. `release.yaml` and `pre-release.yaml` keep their existing mutual diff unchanged (the bump touches only the setup-gradle pin line in each), `integrationTests.yaml` is untouched, and the four pin-guard classes pass under `testUnit --rerun`. The FORK-DELTA rows change in the same commit (DEP-02).
  3. *[Docker-free]* With Docker unreachable, tests drive `ProxySinkTask.put` under `errors.tolerance=none` and `errors.tolerance=all`. They assert today's outcome for each wrapped-`DataException` shape the research named: the `RuntimeException(queryId, DataException)` wrapper, a retriable server code in the chain, and a `SocketTimeoutException` root. For each shape and each tolerance value, the test asserts whether `put` throws a `RetriableException`, fails the task or routes the record to the DLQ. The tests pass on the unchanged code (PIN-02).
  4. *[Docker-free]* A test shows how client 0.11.0 forwards a `custom_*` key from `jdbcConnectionProperties` (the doubled encoding), with no Docker. Its name and failure message call it a client-version tripwire, to be re-read when the client changes (PIN-03).
  5. *[live, record]* One recorded `integrationTest` run shows the harness counting only the connector it started (F-3). The fix touches only `src/integrationTest` (PIN-04).

**Plans**: TBD

**Research flag**: Standard. Confirm at plan time:
- whether PRs #21 and #22 are still open and unmerged on `origin`. If the user merged either there, absorb it by merging `origin/custom_build` instead of the PR head, and re-run the anchors gate. The left count of `origin/custom_build...sync/v1.7.0` is then above 1;
- which FORK-DELTA rows the two bumps change (the research names rows 72, 76 and 79);
- how the F-3 fix tells the harness's own connector apart, and the cost of the one `integrationTest` run (about 33 minutes in the gg5 nightly).

**Note**:
- These two bumps are the user's requirements. No other dependency moves in this milestone without asking (AGENTS.md).
- New tests use no Mockito, `mockStatic` or `mockConstruction`, because of the OpenJ9 crash. Use subclass seams and `StubClickHouseServer`.
- GHSA-chpw-2mqm-95cc does not apply here, so do not claim that 6.4.1 closes a vulnerability.
- `setup-gradle` 6.4.1 first runs on GitHub at the first push. A push needs the user's direct approval (see User-Owned Gates). So the planner either asks for a scratch push in this phase, or leaves the first run to the Phase 44 rehearsal push.

Phase gate: cache-busted `testUnit` with Docker unreachable, a full suite on `latest` × V2 with `SELECT version()` recorded, the anchors invariant, and an empty `src/main` diff.

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
