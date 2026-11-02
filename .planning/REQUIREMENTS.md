# Requirements: ClickHouse Kafka Connect `custom_build` Fork

**Defined:** 2026-11-01
**Milestone:** v1.7.0_gg7 Follow-up Debt, Dependency Sweep & Document Cleanup
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## v1 Requirements

Requirements for this milestone. Each maps to a roadmap phase.

### Dependabot (DEP)

- [x] **DEP-01**: gson `2.15.0` is absorbed in its own commit, with the full suite on ClickHouse `latest` × client V2 and `testUnit` green twice; the gson row in `docs/FORK-DELTA.md` is updated.
- [x] **DEP-02**: `setup-gradle` `6.4.1` is pinned to commit `8da46b539cd8fe6ab27ca75128919a18d757604c` with a `# v6.4.1` comment in all four fork workflows; `release.yaml` and `pre-release.yaml` keep their existing mutual diff unchanged (the bump touches only the setup-gradle pin line in each); `integrationTests.yaml` is untouched; the four guard classes are green; the FORK-DELTA rows are updated.

### Test Pins (PIN)

- [x] **PIN-01**: `StubClickHouseServer` has a stall responder, and a red F-2 pin uses `assertTimeoutPreemptively`, so unfixed code fails instead of hanging the suite.
- [x] **PIN-02**: The wrapped `DataException` behaviour is pinned by Docker-free tests through `ProxySinkTask.put` under both `errors.tolerance` values; no code change.
- [x] **PIN-03**: Forwarding of a `custom_*` key on client `0.11.0` is pinned by a Docker-free test (IN-04), labelled a client-version tripwire.
- [x] **PIN-04**: F-3 is fixed: the integration harness counts only its own connector; verified by one `integrationTest` run.

### Client Closing (CLS)

- [x] **CLS-01**: `ClickHouseWriter.stop()` closes `chc` after the table-sync executor stops, idempotently and null-safe; `chc` is also closed on the failure paths of `start()`.
- [x] **CLS-02**: The V1 `getMutationRequest` path closes its throwaway helper client on every batch; the returned node stays usable.
- [x] **CLS-03**: `StateProvider` gets a default `close()`, `KeeperStateProvider` overrides it and closes only a client it created, and `ProxySinkTask.stop()` calls it in `try/finally` together with the writer and MBean. `getStateRecord` and `setStateRecord` are unchanged.
- [x] **CLS-04**: A counting seam (`CountingHelperClient`, no Mockito) pins all three sites on V1 and V2, including a failed `start()` and a double `stop()`.

### Insert Timeout (TMO)

- [x] **TMO-01**: A stalled insert with `async=false` fails after `clickhouseClientInsertTimeoutMs` as a retriable error, through a per-insert `socket_timeout` on the V2 insert sites; an operator `socket_timeout` in `jdbcConnectionProperties` still wins, pinned by a test; `async_insert` is not touched.
- [x] **TMO-02**: Classification of the timeout failure is pinned through `ProxySinkTask.put` under both `errors.tolerance` values.
- [x] **TMO-03**: The limits are recorded in writing: a write-side stall (peer stops reading the body) and the three V1 `future.get()` sites stay unbounded.

### Risky Edits (RSK)

- [x] **RSK-01**: `Processing.doLogic` line 137 reads `originalTopic()` (OI-35-1); the state key and KeeperMap rows do not change, all 14 `ProcessingTest` cases are unchanged, and the remaining `onPartitionRemoved` mismatch is recorded.
- [x] **RSK-02**: `Utils.handleException` passes a `RetriableException` through as retriable; the set of changed rows in the 184-case classification matrix is named and equals the actual diff.
- [x] **RSK-03**: Start-up logs a WARN for non-transient codes in `additionalRetriableErrorCodes`; no validator and no retry budget; `ClickHouseSinkConfig` is unchanged.
- [x] **RSK-04**: OI-35-2 and OI-35-3 are documented as limits (injective rename, partition-preserving SMT); the state key does not change.

### Documents and Dispositions (DOC)

- [x] **DOC-01**: The gg1 half of WR-08 is fixed in `docs/ROLLOUT-v1.7.0_gg1.md`; FORK-DELTA lines 6, 139 and 286 are in past tense; FUT-05 is named by ID in the rollout note.
- [x] **DOC-02**: Every Phase 37 row has a disposition with an owner: the `increment-version.yml` header comment, advisory rows 26 IN-02, 27 WR-01, 27 IN-01, 28 IN-01 and 28 IN-05(b), broad allow-list globs (UF-37-3) and review Infos. `src/main` rows need the user's approval.

### Release (RLS)

- [ ] **RLS-01**: `VERSION=v1.7.0_gg7` is set through `bump_version.py`; CHANGELOG, FORK-DELTA and `fork-anchors.txt` are updated in the same commits as the counts they change.
- [ ] **RLS-02**: `confluent-archive-allowlist-v1.7.0_gg7.txt` is generated from the final tree (with gson rows) and a dry run reads `0 unallowed, 0 stale rows`.
- [ ] **RLS-03**: `docs/ROLLOUT-v1.7.0_gg7.md` covers F-1, F-2 (duplicate risk with `exactlyOnce=false`) and RSK-01..04, each claim labelled `measured`, `read from the code` or `derived`, and has passed an independent review by a fresh `gsd-code-reviewer`.
- [ ] **RLS-04**: Two full runs on ClickHouse `latest` × client V2 are green; the `--no-ff` merge happens only after the user's direct approval; the published SHA has one green `tests.yaml` run; the tag is created by the publish run.

## Future Requirements

- The next *tagged* upstream sync.
- Upstream workflows still on `@v3`-era actions.
- Sending F-1 and F-2 upstream.
- Bounding write-side stalls and V1 inserts.

## Out of Scope

| Feature | Reason |
|---------|--------|
| New connector features | This milestone is debt and maintenance |
| ClickHouse client above `0.11.0` | Not part of this milestone's goal |
| Keeper state-key change | Would orphan stored KeeperMap rows |
| `async=true` as the F-2 fix | Each timed-out batch would leak a thread and a connection |
| Running the upgrade on production connectors | Outside this repository |
| Scratch branches `scratch/clt-07-*`, `NIGHTLY_ENABLED` | Stay with the user |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DEP-01 | Phase 39 | Complete |
| DEP-02 | Phase 39 | Complete |
| PIN-01 | Phase 41 | Complete |
| PIN-02 | Phase 39 | Complete |
| PIN-03 | Phase 39 | Complete |
| PIN-04 | Phase 39 | Complete |
| CLS-01 | Phase 40 | Complete |
| CLS-02 | Phase 40 | Complete |
| CLS-03 | Phase 40 | Complete |
| CLS-04 | Phase 40 | Complete |
| TMO-01 | Phase 41 | Complete |
| TMO-02 | Phase 41 | Complete |
| TMO-03 | Phase 41 | Complete |
| RSK-01 | Phase 42 | Complete |
| RSK-02 | Phase 42 | Complete |
| RSK-03 | Phase 42 | Complete |
| RSK-04 | Phase 42 | Complete |
| DOC-01 | Phase 43 | Complete |
| DOC-02 | Phase 43 | Complete |
| RLS-01 | Phase 44 | Pending |
| RLS-02 | Phase 44 | Pending |
| RLS-03 | Phase 44 | Pending |
| RLS-04 | Phase 44 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0 ✓

---
*Requirements defined: 2026-11-01*
*Last updated: 2026-11-01 after roadmap creation (Phases 39-44)*
