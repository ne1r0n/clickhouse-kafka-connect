# Project Research Summary

**Project:** ClickHouse Kafka Connect `custom_build` fork, milestone `v1.7.0_gg7-maintenance`
**Domain:** Maintenance of a Kafka Connect sink fork (exactly-once state machine, Debezium CDC). No new connector features.
**Researched:** 2026-11-01
**Confidence:** HIGH overall. MEDIUM on Connect behaviour after a failed `start()`, gson release-note detail, and ClickHouse dedup defaults.

## Executive Summary

No new library, plugin or tool. This milestone is targeted fixes to existing components, two Dependabot absorptions (gson 2.14.0 to 2.15.0; `setup-gradle` 6.4.0 to 6.4.1), documentation, Phase 37 dispositions and a release.

The genuine bugs: (1) resource leaks, since `ClickHouseHelperClient` (and its V2 HTTP pool) is never closed. There are three leak sites, not one: `ClickHouseWriter.start()`, `KeeperStateProvider` (exactly-once only), and the V1 per-batch `chcTmp` in `getMutationRequest`. (2) An insert with `async=false` can block forever, because `future.get(clickhouseClientInsertTimeoutMs)` runs on an already-completed future and `socket_timeout` defaults to 0. The fix is a per-insert socket timeout on the V2 client. Probes showed that a socket timeout surfaces as `SocketTimeoutException`, already classified as `RetriableException`. A write-side stall (peer stops reading) stays unbounded and must be recorded as a known limit.

Main risks are behavioural. F-2 turns an indefinite hang into a retriable failure after 4 minutes by default; with `exactlyOnce=false`, a batch that landed before the timeout is duplicated on redelivery. Wrapped `DataException` and FUT-05 are documentation and pin decisions, not code changes, unless the user decides otherwise. OI-35-1 (one line in `Processing.java`) and any Keeper state-key change need explicit user approval, and the state key must not move. Release gates (allow-list from the final tree, CHANGELOG date at merge, one green CI run on the published SHA, human approval for `--no-ff`) are where earlier milestones lost time.

## Key Findings

**Stack.** No new dependencies.
- gson 2.15.0 (PR #22): serialize-only use; changes are read-side or strict-mode. Gate with `testUnit` plus the full suite, especially `ConvertToMapOrJson*`, `JsonSerializationEquivalenceTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `ClickHouseSinkTaskStringTest`. Update FORK-DELTA row 72.
- `setup-gradle` 6.4.1 (PR #21): pin commit `8da46b539cd8fe6ab27ca75128919a18d757604c` with `# v6.4.1`, not tag object `7296c772...`. Change the same SHA in all four fork workflows (`tests.yaml`, `nightly-integration-tests.yaml`, `release.yaml`, `pre-release.yaml`). Do not touch `integrationTests.yaml`. GHSA-chpw-2mqm-95cc does not apply here; do not claim a closed vulnerability.
- Client 0.11.0 stays. Use `socket_timeout` and `ClickHouseHelperClient.close()`. No WireMock, MockWebServer, Awaitility, Toxiproxy, or new JaCoCo/Mockito plugin. Use `StubClickHouseServer` (JDK `com.sun.net.httpserver`).

**Features (scope).**
- **A. F-1:** close `chc` in `ClickHouseWriter.stop()` after the executor terminates; null the field; idempotent; close on `start()` `return false` paths. Extend to `chcTmp` and `KeeperStateProvider` (decision below).
- **B. F-2:** stalled insert fails after `clickhouseClientInsertTimeoutMs` with `RetriableException`; operator `socket_timeout` in `jdbcConnectionProperties` still wins.
- **C1. Wrapped `DataException`:** pin, do not change. Wrapper `RuntimeException(queryId, DataException)` is not retriable; retriable server code in chain is retriable; `SocketTimeoutException` root under `errors.tolerance=all` is retriable.
- **C2. FUT-05:** document by ID; listed codes raise `RetriableException`, bypass tolerance and DLQ, no retry budget. Existing pins unchanged.
- **D1. OI-35-1:** `Processing.doLogic` line 137 `topic()` to `originalTopic()`. User approval only.
- **D2. OI-35-2/3:** documented limits; injective rename and partition-preserving SMT required. No key change.
- **E1. IN-04:** Docker-free `custom_*` forwarding pin on 0.11.0 (doubled encoding). Labelled a client-version tripwire.
- **E2. F-3:** harness counts only its own connector (`src/integrationTest`).
- **F. Documents:** `ROLLOUT-v1.7.0_gg1.md` item 6; FORK-DELTA lines 6, 139, 286 to past tense; FUT-05 by ID.
- **G. Phase 37 dispositions:** one decision per row; `src/main` rows need approval.
- **H. Dependabot and release:** as above, then `VERSION=v1.7.0_gg7`, CHANGELOG, FORK-DELTA, rollout note with independent review, `--no-ff` after human approval, green CI on published SHA.

**Defer or reject:** making wrapped `DataException` never retriable; validating `additionalRetriableErrorCodes`; retry budget; Keeper state-key change (orphans rows); `async=true` as F-2 fix (leaks threads); finalizer or shutdown hook; client upgrade past 0.11.0; Mockito in new tests; floating action tags.

**Architecture.**
- `ProxySinkTask` constructor creates `KeeperStateProvider` before `start()`; `ProxySinkTask.stop()` is the only release point.
- `ClickHouseWriter.start()` creates the long-lived `chc`; the table-sync executor is its only concurrent user. `stop()` shuts the executor, then closes `chc`.
- `getMutationRequest` builds a throwaway helper per V1 batch; the returned `ClickHouseNode` survives `close()`.
- `ClickHouseHelperClient` always builds a V2 client, even for `clientVersion=V1`.
- Timeout applies to V2 only; V1 `future.get()` stays unbounded and is recorded as a limit.
- Not changed: `ClickHouseSinkConfig` (no new key), `TableMappingRefresher`, V1 timeouts, Keeper client timeout, FUT-05 code, OI-35-2/3 keys.
- Patterns: subclass-based test seams (`CountingHelperClient`) plus a `createHelperClient` factory; keep the `chc` field name (three tests reflect on it); additive `try/finally` diffs so fork-anchors counts hold.

**Critical pitfalls.**
1. **Three F-1 leak sites.** Closing only the writer `chc` leaves `chcTmp` and Keeper leaking while a start/stop test goes green. Count closes on all paths, V1 and V2, including failed start and double stop.
2. **Premature or double close.** `stop()` can run with `chc` null, after failed `start()`, or mid-refresh. Null-guarded `finally` after executor shutdown; pin double-close.
3. **F-2 can duplicate or drop batches.** `Processing.doInsert` wraps every failure, and `handleException` has no `RetriableException` pass-through, so the timeout could surface as a task failure or DLQ record. Pin classification through `ProxySinkTask.put` under both tolerance settings first. Never use `async_insert` (CFG-02 anchor).
4. **Classification changes alter DLQ and BEFORE/AFTER outcomes.** A permanent code in `additionalRetriableErrorCodes` poisons the partition with state stuck at `BEFORE`. Any change to `handleException` or `doInsert`'s catch must flip named rows in the 184-case matrix.
5. **OI-35-1 is not a complete fix.** Partition and offset still come from the transformed record; `onPartitionRemoved` has the same keying mismatch. The KeeperStateProvider diff must be empty; all 14 `ProcessingTest` cases unchanged.
6. **Upstream collision and anchors.** Upstream PRs #861, #855, #857 touch the same files. Smallest additive diffs; update `fork-anchors.txt` in the same commit as any moved count; `spotlessApply` on changed files only; recheck upstream before the code phases and the release.
7. **OpenJ9 Mockito crash.** No Mockito, `mockStatic` or `mockConstruction` in new tests. On exit 255, delete dumps, re-run once, record both; `-PjavaVersion=21` is the recorded escape.
8. **SHA re-pin.** Pin the `^{}` commit; update all four workflows; keep `release.yaml` and `pre-release.yaml` byte-equal; run the four guard classes with `testUnit --rerun`; update FORK-DELTA rows 76 and 79.
9. **Allow-list is one-tree only.** Generate `confluent-archive-allowlist-v1.7.0_gg7.txt` last from the final tree; exact-path or literal-prefix rows only; add gson rows; dry-run to `0 unallowed, 0 stale rows`.
10. **Late release gates.** CHANGELOG dated at merge; tag created only by the publish run; one green `tests.yaml` run on the published SHA; version from `bump_version.py`; rollback tested on the post-publication tip; `--no-ff` only after direct user approval.

## Implications for Roadmap

Phase numbers are provisional (the roadmapper numbers from 39).

1. **Phase 39: Dependabot absorption.** gson 2.15.0 (suite twice) and `setup-gradle` 6.4.1 across four workflows with guard classes green; FORK-DELTA rows 72, 76, 79. No research needed; `setup-gradle` needs one CI run.
2. **Phase 40: Test-only pins and fixtures.** `StubClickHouseServer` stall responder; F-2 red pin with `assertTimeoutPreemptively`; wrapped-`DataException` pins through `ProxySinkTask.put` under both tolerances; `awaitInsertResponse`-through-`Processing` characterisation; IN-04 URI pin; F-3 harness scoping; `ClientErrorShapes` hygiene.
3. **Phase 41: F-1 writer half.** `stop()` close, `start()` failure closes, `chcTmp` close, V2 and V1 counting pins, FORK-DELTA and anchors rows. Needs a scope decision; do not depend on Connect's post-failed-start behaviour.
4. **Phase 42: F-1 Keeper half.** `StateProvider.default void close()`, `KeeperStateProvider.close()` releasing only a self-created client, `ProxySinkTask.stop()` try/finally. `getStateRecord` and `setStateRecord` byte-identical.
5. **Phase 43: F-2 timeout.** Per-insert socket timeout at the V2 insert sites; red-to-green stall pin through `ProxySinkTask.put` under both tolerances; operator-override order pinned; CFG-02 anchor unchanged; rollout line with duplicate-risk wording. Needs the mechanism and default decided first.
6. **Phase 44: Optional high-risk edits (only with user approval).** `Utils` `RetriableException` pass-through with a matrix row-flip list; OI-35-1 one-line change; optional FUT-05 start-up WARN. Each in its own commit.
7. **Phase 45: Documents and Phase 37 dispositions.** Rollout note (F-1 no-op for operators; F-2 retriable failure after timeout, duplicate risk on `exactlyOnce=false`, `socket_timeout` override, write-side stall as a known limit; FUT-05 by ID; D2 limits); `ROLLOUT-v1.7.0_gg1.md` item 6; FORK-DELTA lines 6, 139, 286; one decision per Phase 37 row.
8. **Phase 46: Release.** `bump_version.py`; CHANGELOG; anchors; allow-list from the final tree; pre-release dry run; rollout note reviewed by a fresh `gsd-code-reviewer`; two full suites on ClickHouse `latest` with client V2; `--no-ff` after user approval; one green `tests.yaml` on the published SHA; tag created by the publish run.

**Ordering rationale:** dependencies first; pins before fixes; F-1 writer, then Keeper, then F-2 (shared file and seam); optional high-risk edits last, each isolated; documents after code; release last.

### Decision Points (resolve in discuss-phase)

1. **F-2 mechanism (research conflict).** STACK recommends a per-insert `InsertSettings` `socket_timeout`; FEATURES and ARCHITECTURE recommend a client-level `setSocketTimeout` in `createClientV2()`. Recommended: per-insert, since it bounds only inserts. Verify on 0.11.0 first. The operator's `jdbcConnectionProperties` `socket_timeout` must win either way.
2. **F-2 default.** Inserts over 4 minutes with `async=false` start failing and retrying. Research default: accept and document.
3. **F-1 scope.** Recommended: all three sites. The Keeper half may be deferred with an owner and trigger.
4. **Wrapped `DataException` and FUT-05.** Pin and document only.
5. **`Utils` `RetriableException` pass-through.** User decision.
6. **OI-35-1.** Approve only with the partition/offset scope stated and `onPartitionRemoved` recorded.
7. **Upstream submission.** Not required for this milestone.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | GitHub API, Maven Central, client 0.11.0 source and bytecode. V1 stall behaviour not exercised. |
| Features | HIGH | Repo and Phase 34–38 artifacts; upstream diffed. F-2 mechanism MEDIUM. |
| Architecture | HIGH | Source read at tip; probes P1–P4 run. Connect after a failed `start()` MEDIUM. |
| Pitfalls | HIGH / MEDIUM | Repo-derived HIGH; gson release-note detail and dedup defaults MEDIUM. |

### Gaps to Address

- F-2 mechanism conflict: resolve in discuss-phase; verify the per-insert option first.
- Write-side stall (P3) is unbounded by `SO_TIMEOUT`; record as a known limit.
- Connect after a throwing `start()` is unverified; close inside the writer failure paths regardless.
- `Client.close()` idempotency on 0.11.0 is not probed; guard and pin.
- Whether any test uses the writer after `stop()` is unchecked.
- V1 `future.get()` sites remain unbounded; recorded limit.
- ClickHouse dedup window per engine: verify per table for the exactly-once retry claim.
- Upstream PRs #861, #855, #857: recheck before the code phases and the release.

## Sources

**Primary (HIGH):** working-tree source; `ClickHouse/clickhouse-java` tag `v0.11.0` and `javap` of `client-v2-0.11.0.jar`; probes P1–P4; GitHub API (gson 2.15.0, `gradle/actions` v6.4.1, GHSA-chpw-2mqm-95cc); Maven Central; `upstream/main` `9bed6f79`; Phase 34–38 artifacts; `.planning/RETROSPECTIVE.md`.

**Secondary (MEDIUM):** one gson release-notes fetch; ClickHouse dedup settings from documentation knowledge; non-exhaustive upstream issue search; Connect post-failed-`start()` behaviour from memory.

---
*Research completed: 2026-11-01. Ready for roadmap after the Decision Points are resolved or explicitly deferred.*
