# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- ✅ **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (shipped 2026-10-20)
- ✅ **v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt** — Phases 25-29 (shipped 2026-10-23)
- ✅ **v1.7.0_gg5-ci CI & Release Workflow Modernization** — Phases 30-32 (shipped 2026-10-27)
- ✅ **v1.7.0_gg6-maintenance Maintenance: Dependency, Upstream Tail, Client 0.11.0 & Release** — Phases 33-38 (shipped 2026-11-01)
- 🚧 **v1.7.0_gg7 Follow-up Debt, Dependency Sweep & Document Cleanup** — Phases 39-44 (in progress)

## Phases

<details>
<summary>✅ v1.2.9_gg4-tests Fork Test Coverage (Phases 1-5) — SHIPPED 2026-09-18</summary>

- [x] Phase 1: Test Infrastructure & Green Baseline (3/3 plans) — completed 2026-09-16
- [x] Phase 2: Fork SMT Coverage (5/5 plans) — completed 2026-09-16
- [x] Phase 3: Table Metadata & JSON/Dynamic Types (3/3 plans) — completed 2026-09-17
- [x] Phase 4: JSON Insert Path & Connector Plumbing (3/3 plans) — completed 2026-09-18
- [x] Phase 5: CI & Final Verification (2/2 plans) — completed 2026-09-18

Full phase details: [`milestones/v1.2.9_gg4-tests-ROADMAP.md`](milestones/v1.2.9_gg4-tests-ROADMAP.md)
Requirements as shipped: [`milestones/v1.2.9_gg4-tests-REQUIREMENTS.md`](milestones/v1.2.9_gg4-tests-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.2.9_gg4-tests-phases/`

</details>

<details>
<summary>✅ v1.6.0_gg1-sync Upstream v1.6.0 Sync (Phases 6-11) — SHIPPED 2026-10-07</summary>

- [x] Phase 6: Merge Groundwork & Build Migration (3/3 plans) — completed 2026-09-18
- [x] Phase 7: Type Mapping, Writer, Helper Client & Config (4/4 plans) — completed 2026-09-19
- [x] Phase 8: Orchestration & JMX Re-plumbing (3/3 plans) — completed 2026-09-19
- [x] Phase 9: Metadata-free JSON Path — Keep or Drop (3/3 plans) — completed 2026-09-20
- [x] Phase 10: Test Harness Reconciliation (3/3 plans) — completed 2026-09-20
- [x] Phase 11: Full Suite, CI & Publication (8/8 plans) — completed 2026-10-07

Full phase details: [`milestones/v1.6.0_gg1-sync-ROADMAP.md`](milestones/v1.6.0_gg1-sync-ROADMAP.md)
Requirements as shipped: [`milestones/v1.6.0_gg1-sync-REQUIREMENTS.md`](milestones/v1.6.0_gg1-sync-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.6.0_gg1-sync-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening (Phases 12-15) — SHIPPED 2026-10-10</summary>

- [x] Phase 12: Upstream v1.7.0 Merge & Reconcile (5/5 plans) — completed 2026-10-08
- [x] Phase 13: Fork Hardening on the Merged Tree (8/8 plans) — completed 2026-10-08
- [x] Phase 14: Dependency Bumps & CI (4/4 plans) — completed 2026-10-10
- [x] Phase 15: Suite Gate, Publication & Rollout (8/8 plans) — completed 2026-10-10

Full phase details: [`milestones/v1.7.0_gg1-sync-ROADMAP.md`](milestones/v1.7.0_gg1-sync-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg1-sync-REQUIREMENTS.md`](milestones/v1.7.0_gg1-sync-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.7.0_gg1-sync-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene (Phases 16-20) — SHIPPED 2026-10-14</summary>

- [x] Phase 16: Early Docs Publication (3/3 plans) — completed 2026-10-11
- [x] Phase 17: CI, Build & Test Hygiene (4/4 plans) — completed 2026-10-12
- [x] Phase 18: Writer Correctness (4/4 plans) — completed 2026-10-12
- [x] Phase 19: Nullable Enum & Tuple Mapping (10/10 plans) — completed 2026-10-13
- [x] Phase 20: Release & Publication (8/8 plans) — completed 2026-10-14

Full phase details: [`milestones/v1.7.0_gg2-hardening-ROADMAP.md`](milestones/v1.7.0_gg2-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg2-hardening-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg3-hardening Record-Data Exposure & Review Debt (Phases 21-24) — SHIPPED 2026-10-20</summary>

- [x] Phase 21: Dependabot Integration & CI Hygiene (5/5 plans) — completed 2026-10-19
- [x] Phase 22: Writer Record-Data Exposure (5/5 plans) — completed 2026-10-20
- [x] Phase 23: Declared-Nullability Hardening & Review Closure (6/6 plans) — completed 2026-10-20
- [x] Phase 24: Release & Publication (7/7 plans) — completed 2026-10-20
- [x] Quick 261020-vcr: rollout-note Round 3 fixes, review rounds 4-6, republished as `6a2a87a5` — completed 2026-10-20

Full phase details: [`milestones/v1.7.0_gg3-hardening-ROADMAP.md`](milestones/v1.7.0_gg3-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg3-hardening-phases/` · Quick tasks: `milestones/v1.7.0_gg3-hardening-quick/`

</details>

<details>
<summary>✅ v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt (Phases 25-29) — SHIPPED 2026-10-23</summary>

- [x] Phase 25: Dependency Absorption & Upstream Mini-Sync (8/8 plans) — completed 2026-10-21
- [x] Phase 26: Size-Named Tuple Elements in Discovery (5/5 plans) — completed 2026-10-22
- [x] Phase 27: Writer Failure Path & Test Debt (5/5 plans) — completed 2026-10-23
- [x] Phase 28: Raw Identifier Names in Discovery (6/6 plans) — completed 2026-10-23
- [x] Phase 29: Release & Publication (8/8 plans) — completed 2026-10-24

Full phase details: [`milestones/v1.7.0_gg4-hardening-ROADMAP.md`](milestones/v1.7.0_gg4-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg4-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg4-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg4-hardening-phases/` · Research: `milestones/v1.7.0_gg4-hardening-research/`

</details>

<details>
<summary>✅ v1.7.0_gg5-ci CI & Release Workflow Modernization (Phases 30-32) — SHIPPED 2026-10-27</summary>

- [x] Phase 30: Release Workflows & Pin Guard (3/3 plans) — completed 2026-10-26
- [x] Phase 31: Fork-Owned Nightly Integration Tests (6/6 plans) — completed 2026-10-26
- [x] Phase 32: Release & Publication (7/7 plans) — completed 2026-10-27

Full phase details: [`milestones/v1.7.0_gg5-ci-ROADMAP.md`](milestones/v1.7.0_gg5-ci-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg5-ci-REQUIREMENTS.md`](milestones/v1.7.0_gg5-ci-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg5-ci-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg5-ci-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg5-ci-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg6-maintenance Maintenance: Dependency, Upstream Tail, Client 0.11.0 & Release (Phases 33-38) — SHIPPED 2026-11-01</summary>

- [x] Phase 33: Dependency Sweep & Protobuf Decision (3/3 plans) — completed 2026-10-28
- [x] Phase 34: Upstream Tail Merge (3/3 plans) — completed 2026-10-29
- [x] Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs (3/3 plans) — completed 2026-10-29
- [x] Phase 36: Client 0.11.0 Audit & Bump (4/4 plans) — completed 2026-10-31
- [x] Phase 37: CI Hardening & Advisory Debt (5/5 plans) — completed 2026-10-31
- [x] Phase 38: Release & Publication (5/5 plans) — completed 2026-11-01

Full phase details: [`milestones/v1.7.0_gg6-maintenance-ROADMAP.md`](milestones/v1.7.0_gg6-maintenance-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md`](milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg6-maintenance-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg6-maintenance-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg6-maintenance-phases/`

</details>

### 🚧 v1.7.0_gg7 Follow-up Debt, Dependency Sweep & Document Cleanup (In Progress)

**Milestone Goal:** Close every follow-up carried out of `v1.7.0_gg6-maintenance`: F-1 and F-2 in production code, F-3 in the harness, the missing pins, the OI-35 items, FUT-05 and the Phase 37 dispositions. Absorb Dependabot PRs #21 and #22, correct the stale published documents, and publish `v1.7.0_gg7` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA. (`PROJECT.md` calls this milestone `v1.7.0_gg7-maintenance`; `STATE.md` and the tooling key it as `v1.7.0_gg7`.)

**Criteria tags:** *[live]* needs Docker and ClickHouse `latest` × client V2. *[Docker-free]* runs under `testUnit` or as a build or static check with Docker unreachable. *[record]* is a committed evidence file or document whose content is checked. *[operator]* is a user action or decision, confirmed in the user's own words. *[CI]* is read from GitHub Actions.

- [x] **Phase 39: Dependabot Absorption & Behaviour Pins** - gson 2.15.0 and `setup-gradle` 6.4.1 land first, one commit and one gate each. Then Docker-free pins fix today's wrapped-`DataException` classification and today's `custom_*` forwarding on client 0.11.0, and the integration harness counts only its own connector (F-3). No `src/main` change (completed 2026-11-01)
- [ ] **Phase 40: Helper Client Closing** - F-1: a stopping task closes every helper client it created (the writer's `chc`, the V1 per-batch client, the exactly-once Keeper client), pinned by a Mockito-free counting seam seen red first. The Keeper half is its own commit behind its own gate
- [ ] **Phase 41: Bounded Insert Timeout** - F-2: a red stall pin comes first. Then a stalled `async=false` insert fails after `clickhouseClientInsertTimeoutMs` as a retriable error, through a per-insert `socket_timeout`; an operator's `socket_timeout` still wins, and the cases that stay unbounded are written down
- [ ] **Phase 42: Isolated High-Risk Edits** - OI-35-1 (`originalTopic()` at `Processing.doLogic` line 137), the `Utils.handleException` `RetriableException` pass-through and the FUT-05 start-up WARN land one commit and one gate each, tests first. The state key does not move, and OI-35-2/3 are recorded as limits
- [ ] **Phase 43: Document Fixes & Phase 37 Dispositions** - The gg1 half of WR-08, FORK-DELTA's future-tense lines and FUT-05 by ID are fixed, and every Phase 37 row has a disposition with an owner. A row that touches `src/main` is fixed only on the user's approval
- [ ] **Phase 44: Release & Publication** - `v1.7.0_gg7` is published with CHANGELOG, FORK-DELTA, the gg7 allow-list compare and an independently reviewed rollout note. Two green suites, the `--no-ff` merge only on the user's direct approval, one green `tests.yaml` on the published SHA, and the publish run creates the tag

## Phase Details

### Phase 39: Dependabot Absorption & Behaviour Pins

**Goal**: The code phases start from a tree that already holds both open Dependabot updates, each in its own commit behind its own gate, so a later red is attributable to one change. The same tree pins the behaviour that later phases could disturb without noticing: how a wrapped `DataException` is classified through `ProxySinkTask.put`, and how client 0.11.0 forwards a `custom_*` key. The integration harness stops counting connectors that are not its own (F-3). `src/main` does not change.

**Depends on**: Nothing in this milestone. Phase 38 published `v1.7.0_gg6` (`72e8ccff`). Dependabot PRs #21 (`setup-gradle` 6.4.0 → 6.4.1) and #22 (gson 2.14.0 → 2.15.0) are open on `origin`. Measured 2026-11-01: `origin/custom_build...sync/v1.7.0` counts 1 and 13, and the 1 is the gg6 merge `72e8ccff` itself.

**Requirements**: DEP-01, DEP-02, PIN-02, PIN-03, PIN-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free, live]* gson 2.15.0 is on `runtimeClasspath` (by `dependencyInsight`) from its own commit, which also updates the gson row of `docs/FORK-DELTA.md`. On that commit `testUnit` and the full suite on `latest` × V2 each pass twice, the second run with `--rerun`, with the 18 named skips (DEP-01).
  2. *[Docker-free]* `tests.yaml`, `nightly-integration-tests.yaml`, `release.yaml` and `pre-release.yaml` each pin `gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1`, the commit and not the tag object `7296c772…`. `release.yaml` and `pre-release.yaml` keep their existing mutual diff unchanged (the bump touches only the setup-gradle pin line in each), `integrationTests.yaml` is untouched, and the four pin-guard classes pass under `testUnit --rerun`. The FORK-DELTA rows change in the same commit (DEP-02).
  3. *[Docker-free]* With Docker unreachable, tests drive `ProxySinkTask.put` under `errors.tolerance=none` and `errors.tolerance=all`. They assert today's outcome for each wrapped-`DataException` shape the research named: the `RuntimeException(queryId, DataException)` wrapper, a retriable server code in the chain, and a `SocketTimeoutException` root. For each shape and each tolerance value, the test asserts whether `put` throws a `RetriableException`, fails the task or routes the record to the DLQ. The tests pass on the unchanged code (PIN-02).
  4. *[Docker-free]* A test shows how client 0.11.0 forwards a `custom_*` key from `jdbcConnectionProperties` (the doubled encoding), with no Docker. Its name and failure message call it a client-version tripwire, to be re-read when the client changes (PIN-03).
  5. *[live, record]* One recorded `integrationTest` run shows the harness counting only the connector it started (F-3). The fix touches only `src/integrationTest` (PIN-04).

**Plans**: 4/4 plans complete

Plans:
- [x] 39-01-PLAN.md — execution-start re-read of origin and PRs #21/#22 (absorption mode), then gson 2.15.0 as its own merge with its FORK-DELTA row, gated by the cumulative invariant gate started right after the merge and by testUnit and the full suite twice each (DEP-01)
- [x] 39-02-PLAN.md — `setup-gradle` 6.4.1 as its own merge pinned by the peeled commit in four workflows, guard classes, testUnit and the cumulative invariant gate (DEP-02)
- [x] 39-03-PLAN.md — Docker-free pins: the wrapped-`DataException` matrix through `ProxySinkTask.put` under both tolerances, and the `custom_*` client-version tripwire, both formatter-clean under a scoped Spotless check (PIN-02, PIN-03)
- [x] 39-04-PLAN.md — F-3 harness fix in `src/integrationTest` with a leftover-connector pin seen red against a scratch cross-connector count, scoped Spotless on the touched files, one recorded `integrationTest` class run, and the phase gate (PIN-04)

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

### Phase 40: Helper Client Closing

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

**Plans**: 3/3 plans executed

Plans:
- [x] 40-01-PLAN.md — red baseline before any fix: the counting seam (`CountingHelperClient` plus the writer's `createHelperClient` factory and the Keeper factory constructor), the 22 writer cases and the 7 Keeper and `ProxySinkTask.stop` cases run red on V1 and V2 for the right reason, both red trees saved as committed patches, src restored (CLS-04)
- [x] 40-02-PLAN.md — writer half as one commit: stop-close tracer, then the `start()` wrapper and the per-batch try-with-resources, FORK-DELTA writer, harness and Known cost 13 rows and four anchor rows in the same commit, then gate A (testUnit and the full suite) with the Keeper half absent (CLS-01, CLS-02, CLS-04)
- [x] 40-03-PLAN.md — Keeper half as its own later commit: `KeeperStateProvider.close()` closing only its own client, `ProxySinkTask.stop()` in nested try/finally, state paths byte-identical, rows in the same commit, gate B with the exactly-once classes, then the phase gate with the invariant script once on an explicit SHA (CLS-03, CLS-04)

**Research flag**: Settle these before planning:
- whether `Client.close()` on client 0.11.0 is idempotent (the research did not probe it). The null guard and the double-`stop()` pin hold either way;
- whether any existing test uses the writer after `stop()`;
- which three tests reflect on the `chc` field. Keep the name;
- what Connect does after a `start()` that throws, which is unverified. Close inside the writer's own failure paths, and do not rely on Connect calling `stop()`.

**Note**: Keep the diffs additive (`try/finally`, null guards). Upstream PRs #861, #855 and #857 touch the same files; re-read them before planning. `ClickHouseHelperClient` builds a V2 client even for `clientVersion=V1`, so the V1 cases also close a V2 client.

Phase gate: the per-half gates above, then the anchors invariant.

### Phase 41: Bounded Insert Timeout

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

### Phase 42: Isolated High-Risk Edits

**Goal**: Three small edits in high-risk files land one commit each, tests first and each behind its own gate, so any one can be reverted alone (the user's decision of 2026-11-01):
- Exactly-once processing reads the original topic where OI-35-1 found the transformed one, and the state key does not move.
- `Utils.handleException` keeps an exception that is already a `RetriableException` retriable, and the set of classification rows it changes is written down in advance.
- A connector that lists a non-transient code in `additionalRetriableErrorCodes` gets a WARN at start-up.

The OI-35 cases that the edit does not fix are written down as limits.

**Depends on**: Phase 41. The PIN-02 and TMO-02 classification pins are the baseline that RSK-02's row list is measured against.

**Requirements**: RSK-01, RSK-02, RSK-03, RSK-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free, record]* In the RSK-01 commit, `Processing.doLogic` line 137 reads `originalTopic()`. The `KeeperStateProvider` diff is empty, and the state key and the KeeperMap row format are unchanged. All 14 `ProcessingTest` cases are unchanged and pass. The remaining `onPartitionRemoved` keying mismatch is recorded in FORK-DELTA (RSK-01).
  2. *[Docker-free, record]* Before the RSK-02 change, the expected changed rows are written down: rows of the 184-case `WriterFailureClassificationUnitTest` matrix, and of the PIN-02 and TMO-02 pins. After the change, the actual diff of outcomes equals that list exactly, and a `RetriableException` passed to `handleException` comes out retriable (RSK-02).
  3. *[Docker-free]* In the RSK-03 commit, a task started with a non-transient code in `additionalRetriableErrorCodes` logs one WARN that names the code and carries no record data, and a transient code logs none. Start-up still succeeds. Nothing validates the codes or budgets retries, the `ClickHouseSinkConfig` diff is empty, and the existing `additionalRetriableErrorCodes` pins are unchanged (RSK-03).
  4. *[record]* FORK-DELTA states OI-35-2 and OI-35-3 as limits: exactly-once under a topic-renaming SMT needs an injective rename and a partition-preserving SMT. The state key does not change (RSK-04). Each of the three code commits touches only its production file, its tests and its record rows. Each is followed by cache-busted `testUnit` and a full suite.

**Plans**: TBD

**Research flag**: Settle these before planning:
- RSK-01: trace where the line 137 value flows before editing. If it reaches the state key, a KeeperMap row or table routing in a way that changes stored state, stop for the user. A state-key change is out of scope.
- RSK-02: write the expected row list from the matrix before the code change. A mismatch is a stop, not an expectation update.
- RSK-03: settle what counts as a transient code (where the list comes from, and where the WARN fires) without adding a config key.

**Note**: These are the user-approved high-risk edits (`Processing.java`, `Utils.java` and the start-up path). Any diff beyond RSK-01's one line, RSK-02's pass-through or RSK-03's WARN stops for a user decision. Preserve the `BEFORE`/`AFTER` flag sequence (AGENTS.md).

Phase gate: the per-commit gates above, then the anchors invariant.

### Phase 43: Document Fixes & Phase 37 Dispositions

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

### Phase 44: Release & Publication

**Goal**: `v1.7.0_gg7` is published on `custom_build`. Operators get an independently reviewed rollout note that states what F-1, F-2 and the four RSK items change for them, with each claim labelled by its evidence. The gg7 allow-list compare passes on the final tree, and the suite is green twice. The published SHA has one green `tests.yaml` run before the user's publish run creates the tag. Every push, the merge and the publish run happen only on the user's direct word.

**Depends on**: Phases 39–43 (it documents and publishes their outcome)

**Requirements**: RLS-01, RLS-02, RLS-03, RLS-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `VERSION` reads `v1.7.0_gg7`, set by `bump_version.py`, and the generated version agrees after `clean`. `CHANGELOG.md` has a `1.7.0_gg7` section, and `docs/FORK-DELTA.md` is final, with its raw numbers refreshed once. Every `fork-anchors.txt` row changes in the same commit as the count it changes (RLS-01).
  2. *[Docker-free]* `confluent-archive-allowlist-v1.7.0_gg7.txt` was generated last, from the final tree, with exact-path or literal-prefix rows and the gson rows. A dry run of the compare reads `0 unallowed, 0 stale rows` (RLS-02).
  3. *[record]* `docs/ROLLOUT-v1.7.0_gg7.md` covers F-1, F-2 and RSK-01..04, each claim labelled `measured`, `read from the code` or `derived`. For F-2 it states the retriable failure after the timeout, the duplicate risk with `exactlyOnce=false`, the `socket_timeout` override and the unbounded cases. Before the push gate, a fresh `gsd-code-reviewer` spawned by the orchestrator reviewed the note together with `CHANGELOG.md`, `docs/FORK-DELTA.md` and the published notes edited in Phase 43. Rounds repeated until one found nothing (at most three unless the user authorises more), and every finding's disposition is recorded (RLS-03).
  4. *[live]* On the release tip, two full `--rerun` suites pass on one pinned `latest` build with client V2, with `SELECT version()` and the named skips recorded. `testUnit` passes with Docker unreachable, and the anchors gate passes (RLS-04).
  5. *[operator, CI]* The user gives a direct approval, to the orchestrator and in their own words. After it, `custom_build` is a non-force `--no-ff` merge whose tree equals the rehearsed tip. The published SHA has exactly one `tests.yaml` run, and it is green before the user launches `release.yaml`. That publish run creates the tag `v1.7.0_gg7` and the GitHub release (RLS-04).

**Plans**: TBD

**Note**: This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (Phase 29 D-05). A defect found here goes back to its phase as a gap. Otherwise it is deferred with a reason, an owner and a re-open trigger in the milestone close record. The recipe is the gg6 one:
1. Check `origin/custom_build...sync/v1.7.0` for divergence before gating. A left count of 1 is the gg6 merge; more means `origin` moved.
2. Push the scratch branch once, at a SHA that no other pushed branch holds, and wait for green.
3. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
4. Push `custom_build` only, and wait for its single run.
5. The user launches the publish run (`release.yaml`, `dry_run` not ticked). Push `pre-v1.7.0_gg7` by name after it.
6. Delete the scratch branch.

Before the gate, recheck upstream: tags past `v1.7.0`, and PRs #861, #855 and #857. The CHANGELOG heading carries the merge date in UTC. The approval gates run orchestrator-inline, because executors refuse relayed approvals and `custom_build` is the protected base branch.

## User-Owned Gates

Each gate below is a blocking-human checkpoint. `mode: yolo`, `auto_advance: true` and `skip_discuss: true` move work between steps and phases, but they pass none of these gates. The user answers each one in their own words, directly to the orchestrator. A message relayed by an agent is not approval, and an executor that reaches one of these gates stops and returns it.

| Gate | Phase | Why it is the user's |
|------|-------|----------------------|
| A push of any branch | 39 (optional scratch push), 44 | Every push runs `tests.yaml` once and publishes the local `.planning` tail one way |
| A `src/main` change outside a phase's declared scope | 40–43 | The fork runs in production |
| A `src/main` disposition row | 43 | DOC-02 |
| A change to the exactly-once state key or KeeperMap rows | 42 | Out of scope: it would orphan stored rows. The phase stops |
| An F-2 mechanism other than a per-insert `socket_timeout` | 41 | The user chose the per-insert mechanism |
| Changing or removing an existing assertion | any | Tests are only added, never weakened |
| A fourth review round on the release documents | 44 | RLS-03 |
| The `--no-ff` merge into `custom_build` and its push | 44 | RLS-04 |
| The publish run (`release.yaml`, `dry_run` not ticked) | 44 | The user launches it |
| Dependabot PRs on `origin`, `scratch/clt-07-*`, `NIGHTLY_ENABLED` | — | They stay with the user, and agents do not touch them |

## Structure Notes

**Why six phases under `coarse` granularity.** The 23 requirements fall into six delivery boundaries, and each is proven differently:
- the dependencies and baseline pins, by one gate per bump and by Docker-free pins that pass on unchanged code;
- F-1, by counting pins seen red, with two gated halves;
- F-2, by a stall pin seen red and then green;
- the high-risk edits, by one commit and one gate each, with a row list written in advance;
- the documents and dispositions, by a ledger and the user's approvals;
- the release, by the publication recipe.

**How this differs from the research's eight-phase shape:**
- **The Dependabot and pins phases are merged (39).** Neither changes `src/main`, and together they form the baseline that the fixes are measured against. The bumps still land first, each behind its own gate.
- **PIN-01 sits in the F-2 phase (41), as its first commit.** A red stall pin committed two phases before its fix would turn every gate in between red.
- **The F-1 writer and Keeper halves share one phase (40),** because CLS-04 pins all three sites. The user's decision that the Keeper half is its own commit and gate is success criterion 3 of that phase.
- **The documents phase stays separate from the release (43).** DOC-02 edits `.github/increment-version.yml` and may edit `src/main`, and the release phase can edit neither.

**Order kept:** dependencies first; pins before fixes; F-1 before F-2, because they share the file and the seam; the high-risk edits after F-2, so RSK-02's row list is measured against the final classification pins; documents after code; release last.

**Execution model.** Phases run one after another on one working tree (`sync/v1.7.0`). Phases 40, 41 and 42 change `src/main`. Phase 43 changes it only with the user's approval, and Phases 39 and 44 do not change it.

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `gradle/libs.versions.toml` | 39 (gson) | One bump per commit |
| `.github/workflows/*` | 39 (`setup-gradle` SHA), 43 (`increment-version.yml` header comment) | Each edit passes the pin guard; Phase 44 edits none |
| `ClickHouseWriter`, the helper-client seam | 40, 41 | F-1 lands before F-2 |
| `StateProvider`, `KeeperStateProvider`, `ProxySinkTask` | 40 (Keeper half) | `getStateRecord` and `setStateRecord` stay byte-identical |
| `StubClickHouseServer` | 41 | The stall responder lands with the red pin |
| `Processing.java`, `Utils.java` | 42 | One commit each; a state-key change stops |
| `src/integrationTest` | 39 (F-3) | One recorded `integrationTest` run |
| `.planning/scripts/fork-anchors.txt` | 39–44 | Anchor rows change in the same commit as the code |
| `docs/FORK-DELTA.md` | 39 (gson and `setup-gradle` rows), 40, 41 (limits), 42 (OI-35 rows), 43 (lines 6, 139, 286), 44 (final) | Raw numbers are refreshed once, in Phase 44 |
| `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/ROLLOUT-v1.7.0_gg6.md` | 43 | Reviewed independently in Phase 44 |
| `CHANGELOG.md`, `VERSION`, `docs/ROLLOUT-v1.7.0_gg7.md`, the gg7 allow-list | 44 | The allow-list is generated last, from the final tree |

## Sync Mechanics (applies to every upstream sync)

- **Scratch branch, not `custom_build`.** All merge and hardening work happens on a scratch branch, with the pre-sync tip tagged `pre-vX.Y.Z-sync`. `custom_build` is published and protected. It moves forward only by a non-force `--no-ff` merge after human approval, which keeps `git revert -m 1` viable.
- **Upstream ref is the tag.** Never use bare `upstream/main`, because a stale `refs/upstream/main` makes it ambiguous.
- **Commit cadence.** No `.planning/` commit is interleaved with in-progress conflict resolution. No dependency bump rides inside the sync commit.
- **Gradle invocation.** Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. This shell sets the variable to the literal string `"null"`, which breaks Ryuk. Docker is colima (profile `vz`).
- **Gates.** `testUnit` (Docker-free) is the cheap gate. Any two-run gate busts Gradle's cache with `--rerun`. If the full suite goes red on unchanged code, check the `latest` image date first.
- **Pushes.** Pushing any branch triggers `tests.yaml`; a tag push does not (CI-08). Two branches pushed at one SHA still cancel each other.
- **Editing this file by script.** Slice to the target `### Phase N:` section before any find-and-replace. An unanchored replace truncated this roadmap once (`c1480f8f`).

## Backlog

### Phase 999.1: Column.extractColumn drops Nullable for Enum8/Enum16 and Tuple (BACKLOG — scheduled into Phase 19)

**Goal:** Nullable(Enum8/16) values are stored as NULL or shifted (RESEARCH Spike b2, Pitfall 8); multi-field Nullable(Tuple) fails ATTEMPT_TO_READ_AFTER_EOF and single-field Nullable(Tuple) array elements now throw under D-16 (Spike b3); found 2026-10-08, Phase 13 research, deliberately not fixed in Phase 13
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 19 (COLS-08..COLS-11, DEBT-09). The Enum half ships unconditionally. The Tuple half is a go/no-go after a spike. On no-go, COLS-10 updates this entry with the remaining Known cost.
**Requirements:** mapped under Phase 19
**Plans:** planned under Phase 19
**Delivered:** Phase 19 (go), shipped in `v1.7.0_gg2` on 2026-10-14 — `Nullable(Enum8|Enum16)` and `Nullable(Tuple)` round-trip; the discovery WARN is deleted

### Phase 999.2: Test classpath carries org.lz4:lz4-java 1.8.0 beside at.yawk.lz4 (BACKLOG — scheduled into Phase 17)

**Goal:** `testRuntimeClasspath` selects `org.lz4:lz4-java:1.8.0` (via the Confluent 7.9.1 test stack) beside `at.yawk.lz4:lz4-java`, although upstream's capability rule redirects it on `runtimeClasspath` and `integrationTestRuntimeClasspath`; it is test-only and not shipped; it is affected by the 2026-10-07 lz4 advisories, with no fix on that coordinate; fixing it needs a `build.gradle.kts` change outside Phase 14's scope; found 2026-10-09, Phase 14 research
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 17 (DEBT-07)
**Requirements:** mapped under Phase 17
**Plans:** planned under Phase 17
**Delivered:** Phase 17 (DEBT-07), shipped in `v1.7.0_gg2` on 2026-10-14 — excluded from `testRuntimeClasspath` only, pinned by `TestRuntimeClasspathLz4UnitTest`

### Phase 999.3: Rethrown write-failure messages can carry record values (BACKLOG — scheduled into Phase 22)

**Goal:** `ClickHouseWriter.doWriteColValue`'s generic catch rethrows the original exception (kept by Phase 18 D-09 so classification cannot change). For some failures its message embeds the record value (`UUID.fromString`: `Invalid UUID string: <value>`; number parsing: `For input string: ...`), which reaches the Connect task status, the DLQ headers and `doInsertRawBinary`'s two `Error inserting records` ERROR logs. Fixing it means changing the rethrown type or message without changing retriable classification. Found 2026-10-12 in the Phase 18 discussion (D-09)
**Scheduled:** 2026-10-16 into milestone `v1.7.0_gg3-hardening` as Phase 22 (WRT-13, WRT-14, WRT-15). The classification pin (WRT-14) lands and is green on the unchanged writer before the change. WRT-15 marks this entry delivered in the same commit as the writer code.
**Requirements:** mapped under Phase 22
**Plans:** planned under Phase 22
**Delivered:** Phase 22 (WRT-13..WRT-15), shipped in `v1.7.0_gg3` on 2026-10-20: a value-conversion failure names the column, the ClickHouse type and the exception class without the record value; retry classification is pinned by WRT-14

### Phase 999.4: Discovery drops a Tuple element named size or size0 (BACKLOG — scheduled into Phase 26)

**Goal:** `ClickHouseHelperClient.shouldSkipDescribeRow` drops every DESCRIBE row that ends in `.size`, `.size0` or `.null`, so a Tuple element named `size` or `size0` never becomes a Column, and the writer, which iterates `getTupleFields()`, writes a short tuple. Measured on `latest` 26.9.19.9 with client V2 (23-EVIDENCE.md R-01): the insert fails with code 33 `CANNOT_READ_ALL_DATA` and no row is stored, and a silently wrong stored value is reachable when the bytes of the shorter write happen to parse as a longer tuple. Severity: high for an affected table, since the inserts that were run into such tables failed (four shapes, code 32 or 33, `24-COMMAND-EVIDENCE.md` E-15). The server rejects a Tuple element named `null` (code 36), so only `size` and `size0` are affected. Since COLS-14 the unpaired-element WARN names the dropped element at every DESCRIBE. Fix sketch: keep a row that is a declared element of its parent Tuple while still dropping the structural rows `c.name.size`, `d.null`, `d.other.size`, `e.size0` and `e.v.size`. Parent-is-a-Tuple alone is not enough for `Array(Tuple)`, where `e.size0` is the array's offsets row. The fix touches upstream-region `ClickHouseHelperClient`, so it gets its own phase, with a FORK-DELTA row, an anchors row, a live round-trip test and the DESC-05 regression check. It flips the D-04 pin `COLS-14 unpaired-element WARN (D-04, known defect 999.4)`: the element then pairs and no WARN fires. Found 2026-10-20 in the Phase 23 discussion (D-03)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 26 (DESC-06..DESC-10). The measured defect is wider than this entry's title. `Table.updateParent`'s `SIZE_FIELD_MATCHER` is a second drop site, for `size<digits>` under an Array parent, and the user chose on 2026-10-21 to fix both sites. The live DESCRIBE spike (DESC-06) and the behaviour-preserving extraction of the describe loop (DESC-07) land before the fix. DESC-10 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 26
**Plans:** planned under Phase 26
**Delivered:** Phase 26 (DESC-06..DESC-10) on sync/v1.7.0, shipped in v1.7.0_gg4 on 2026-10-23: discovery keeps a declared Tuple element named size, size0 or size<digits> at both drop sites (DescribeTableBuilder and Table.updateParent); rows stored by earlier releases are not repaired

### Phase 999.5: Backslashes in column and element names come out of fromJsonRow doubled (BACKLOG — scheduled into Phase 28)

**Goal:** `ClickHouseFieldDescriptor.fromJsonRow` doubles every backslash before Jackson parses, so a server name with a backslash comes out doubled (`c.a\\nb`), and a Kafka field must use the doubled form for the writer to match it (23-EVIDENCE.md R-02). The D-12 live premise uses the doubled field name. The behaviour is pre-existing, and Phase 23 changes no code or test for it. Fix sketch: decide the intended name form, decode name escapes in upstream-region `fromJsonRow`, add a live round-trip pin for both forms and a rollout note, since operators' field names may depend on the doubled form. Found 2026-10-20 in Phase 23 research (D-20)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 28 (NAME-01..NAME-07). The canonical form was decided by the user on 2026-10-21: the raw identifier as written in `CREATE TABLE`, with no dual-form alias, and operators rename. The escaped-quote `normalise` pin (NAME-01) lands on the current code first. NAME-07 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 28
**Plans:** planned under Phase 28
**Delivered:** Phase 28 (NAME-01..NAME-07) on sync/v1.7.0, shipped in v1.7.0_gg4 on 2026-10-23: names and Enum labels are matched under the raw identifier as written in CREATE TABLE on both clients; a record using the earlier escaped form stores the DEFAULT at the root and fails for a Tuple element; rows stored by earlier releases are not repaired

## Progress

**Execution Order:** Phases execute in numeric order: 39 → 40 → 41 → 42 → 43 → 44.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 39. Dependabot Absorption & Behaviour Pins | v1.7.0_gg7 | 4/4 | Complete    | 2026-11-01 |
| 40. Helper Client Closing | v1.7.0_gg7 | 3/3 | In Progress | - |
| 41. Bounded Insert Timeout | v1.7.0_gg7 | 0/TBD | Not started | - |
| 42. Isolated High-Risk Edits | v1.7.0_gg7 | 0/TBD | Not started | - |
| 43. Document Fixes & Phase 37 Dispositions | v1.7.0_gg7 | 0/TBD | Not started | - |
| 44. Release & Publication | v1.7.0_gg7 | 0/TBD | Not started | - |

---
*Roadmap reorganized at the `v1.7.0_gg6-maintenance` close: 2026-11-01. Milestone `v1.7.0_gg7` phases 39-44 added: 2026-11-01.*
