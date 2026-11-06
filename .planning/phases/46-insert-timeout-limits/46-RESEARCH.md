# Phase 46: Insert Timeout Limits - Research

**Researched:** 2026-11-05
**Domain:** Bounding (or recording) the five cases `clickhouseClientInsertTimeoutMs` leaves open after gg7, on ClickHouse Java client 0.11.0 (client V2 `async=false`, client V1 `future.get()`), plus rewriting the setting's help text and pinning it Docker-free
**Confidence:** HIGH for the client mechanics and the measured numbers (client sources read, every mechanism run end to end against the real connector writer and against a real ClickHouse `26.10.3.7`); MEDIUM for Linux CI buffer behaviour of the write-stall pin (measured on macOS only); the real repo is untouched, `git status` shows only the two pre-existing dirty `.planning/config.json` and `.planning/state.json`

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md is "Auto-generated (discuss skipped via workflow.skip_discuss)". Its `## Decisions` section holds only the `### Claude's Discretion` block below.

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Deferred Ideas (OUT OF SCOPE)
None — discuss phase skipped.

Constraints restated by the ROADMAP Phase 46 section (binding, they come from the user's own roadmap): never use `async=true` or `async_insert` as a fix (a timed-out batch would leak a thread and a connection); no Mockito, extend `StubClickHouseServer`; no config key is added; `async_insert` is not touched; an operator's `socket_timeout` in `jdbcConnectionProperties` still wins; the `ClickHouseSinkConfig` diff is the help text and nothing else; **a mechanism that bounds requests beyond the case being fixed stops for the user** (flagged below, not chosen); a bound added here fails an insert that legitimately runs longer than the bound, and the rollout item states it; phase gate is cache-busted `testUnit`, a full suite on `latest` x V2, and the anchors invariant. Every decision that belongs to the user is in "Decisions that belong to the user at plan time".
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TMO-01 | The `clickhouseClientInsertTimeoutMs` help text says what the setting really bounds; a Docker-free test pins the text against the measured behaviour | Current text quoted verbatim below; the measured "bounds / does not bound" list (table "Measured outcome per case"); a draft text per outcome; pin design = one constant list of required phrases, each tied to a measured row, asserted against `ClickHouseSinkConfig.CONFIG.configKeys().get(...).documentation`; the text is written last (after U-1..U-5 settle) |
| TMO-02 | An insert that streams progress headers is bounded, or its escape is re-measured against a real server and kept as a recorded limit with the number | The escape is re-measured **and refined**: it reproduces on `26.10.3.7` only with `async_insert=0` in force and the input split into many blocks (8141 ms against a 3000 ms bound through the real writer), and does **not** reproduce under the server's default `async_insert=1` (3057 ms, bounded). A whole-insert wall-clock mechanism that bounds it exists and was run (3005 ms); using it changes the setting's meaning for every V2 insert, so bounding vs recording is U-1. A Docker-free red pin is feasible with a raw-socket stub (measured red: 6315 ms against a 1500 ms bound) |
| TMO-03 | Write-side stalls, client V1 `future.get()` inserts and non-insert requests are each bounded with a red-first pin, or recorded with a measurement showing why they cannot be | Write stall V2: red (blocked > 20 s with `socket_timeout=500`), bounded by a write-phase deadline that cancels the request (3009 ms), disarmed after the body is sent so it does not touch progress-streaming inserts. V1: write stall blocked > 80 s (library default 30 s does not help), bounded by `future.get(timeout)` at the three sites (3268 ms, clean release measured). Non-insert: full inventory with put-path flags; V2 requests are unbounded (blocked > 50 s), V1 ends at the library's 30.03 s; bounding the swallow-to-null requests would turn a hang into a misleading non-retriable "Table ... does not exist". The wording "cannot be bounded" is not literally true for any case (mechanisms exist), so "recorded" outcomes must say "not bounded by decision" (U-6) |
</phase_requirements>

## Pre-flight check (done first)

| Check | Result | Evidence |
|---|---|---|
| Tree | `sync/v1.7.0` at `2b419d4b`, Phase 45 complete; only `.planning/config.json` and `.planning/state.json` dirty | [VERIFIED: `git status --short`, `git log --oneline` run 2026-11-05] |
| Anchors | `fork-anchors.txt` has 200 data rows; the working-tree mirror reports `rows=200 bad=0` | [VERIFIED: the mirror loop from 45-RESEARCH run on the real tree] |
| Anchor rows this phase touches | Quoted verbatim in "Anchors" below; the `0` row `clientBuilder.setSocketTimeout(` pins that the client-level alternative stays out | [VERIFIED: `.planning/scripts/fork-anchors.txt:218-222`, Read] |
| Real server | `clickhouse/clickhouse-server:latest`, `SELECT version()` = `26.10.3.7`, image `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (same digest Phase 41, 44 and 45 recorded) | [VERIFIED: `docker inspect gsd46-ch --format '{{.Image}}'`, `curl ... SELECT version()`] |
| Server default `async_insert` | `async_insert 1 changed 0`, `wait_for_async_insert 1`, `send_progress_in_http_headers 0`, `http_headers_progress_interval_ms 100`, `max_execution_time 0`, `function_sleep_max_microseconds_per_block 3000000` | [VERIFIED: `SELECT name, value, changed FROM system.settings WHERE name IN (...)` on `26.10.3.7`] |
| Existing pins green on the unchanged tree | `InsertStallBoundPinUnitTest` 15 tests (7.2 s), `InsertSocketTimeoutOptionUnitTest` 23, `RetriableExceptionPassThroughUnitTest` 38, 0 failures, JDK 21 (`-PjavaVersion=21`), Docker unreachable | [VERIFIED: `testUnit` in a throwaway clone, JUnit XML summed] |
| Tail gate on the unchanged tree | `PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD` printed `invariants: provenance_paths=11 anchor_commits=47 anchor_rows=200 marker_commits=65 anchor_exempt=0 result=PASS`, exit 0, 2 min 56 s (the tail form is fast; the cumulative form is the slow one) | [VERIFIED: run in the throwaway clone of `2b419d4b`, output saved] |

## Summary

The gg7 bound is a per-insert `socket_timeout`, an inactivity limit that the client applies to reads only. Client 0.11.0 has no total-time and no write-time bound in its HTTP layer: the only timeouts `HttpAPIClientHelper` applies are connect, connection-request and the socket/response timeout (`SOCKET_OPERATION_TIMEOUT`), so a case that keeps the read side busy (progress headers) or blocks the write side (a peer that stops reading) cannot be reached by any configuration option. What does reach them is `Client.cancelTransportRequest(queryId)`, a public client-side abort of the in-flight request by query id, which the connector can call from a small watchdog. I ran every candidate end to end against the real connector writer, a raw-socket stub and a real ClickHouse `26.10.3.7`, and the numbers below are measurements, not inferences.

Seven findings change the plan relative to the roadmap text.

1. **The progress-header escape is narrower than gg7 recorded.** It needs `async_insert=0` in force and the input split into blocks of work each shorter than the inactivity bound. Under the server's default `async_insert=1` on `latest` (the connector never sets `async_insert`), the same slow insert streams one progress header at its end and the 3000 ms per-insert bound fails it at 3057 ms. With `async_insert=0` and 500-row blocks it escapes: 8141 ms against a 3000 ms bound, control 8147 ms, client V1 8154 ms. This closes the gg7 "the case with the server's default async_insert was not measured" gap.
2. **A whole-insert wall-clock bound exists and works for both open V2 cases** (`cancelTransportRequest` from a scheduled watchdog: progress insert ended at 3005 ms, write stall at 3014 ms, no thread or socket left behind), **and a narrower one exists** (a write-phase deadline, disarmed when the body has been fully written: write stall 3009 ms, progress-streaming insert untouched, 8056 ms). Which one, if either, is U-1: the wall-clock form changes the setting from an inactivity bound to a cap on total insert time for every V2 insert, which the roadmap reserves for the user.
3. **Client V1 can be bounded with `future.get(timeout)` and releases cleanly.** The V1 write stall is blocked in `future.get()` (jstack), not in the pipe write, so a bounded get reaches it: 3268 ms; after four and two timed-out inserts no TCP socket and no thread in socket I/O remained. The V1 library's 30 s default does not bound a write stall (blocked > 80 s) or a progress-streaming insert (8154 ms against a 3000 ms `socket_timeout`).
4. **`client_version` defaults to `V1`**, so the connector's default path was never touched by gg7, and the V1 write stall is the open case on that default path.
5. **V2 `ping(timeout)` is a no-op on a stalled server.** `Client.ping(long)` calls `future.get(timeout)` on a future that `async=false` has already completed, so the timeout never fires: measured `ping(timeout=2000)` blocked > 50 s. FORK-DELTA item 14 says "ping has its own timeout", which is wrong for client V2. The same defect (a timeout on an already-completed future) is the one gg7 found in `awaitInsertResponse`.
6. **Non-insert requests split cleanly.** Client V2: `DESCRIBE`, table refresh, version query, `ALTER`, and the Keeper state write are all unbounded (blocked > 50 s each); client V1: all end at 30.03 s (library default), ping at 2049 ms. The V2 requests that swallow their exception (`describeTableV2`, `showTablesV2`, `versionV2`) return null/empty, which `getTable` turns into a non-retriable `RuntimeException("Table ... does not exist")`; a bound there converts a hang into a task failure or a DLQ batch. That is why recording them is the recommendation, and a bound is U-4.
7. **A timed-out insert is not rolled back.** After a client abort of a slow `async_insert=0` insert into a MergeTree table, 2000 of 4000 rows were visible at once and 3500 twelve seconds later (the server logged `ExceptionWhileProcessing` code 210 at 6043 ms). With `exactlyOnce=false` the redelivery duplicates what landed. This is the F-2 rollout consequence, now with a MergeTree number.

**Primary recommendation:** Bound the write-side stall on client V2 with a write-phase deadline (cancel the request by query id if the body is not fully sent within `clickhouseClientInsertTimeoutMs`), bound the three client V1 sites with `future.get(timeoutMs)`, gate both off when the operator named `socket_timeout`, and record the V2 progress-header escape, the non-insert requests and the V2 ping no-op in FORK-DELTA with the numbers below. The user decides whether the progress-header escape is bounded instead (U-1), whether V1 gets the wall-clock bound (U-2), and whether any non-insert request is bounded (U-4, U-5). No `Utils`, `ClickHouseSinkConfig` (beyond the help text), `Processing` or `ProxySinkTask` code changes in any of these shapes.

## Decisions that belong to the user at plan time

| # | Decision | Options measured | Recommended default (planner may propose, user confirms) |
|---|---|---|---|
| U-1 | The client V2 insert that streams progress headers (TMO-02): bound it or record it | **A (record):** keep the current inactivity bound; FORK-DELTA gets the measured escape (8141 ms against 3000 ms, `async_insert=0` + block-split shape) and the measured non-reproduction under default `async_insert=1` (3057 ms). **B (whole-insert watchdog):** a scheduled task calls `client.cancelTransportRequest(queryId)` at `clickhouseClientInsertTimeoutMs`; measured 3005 ms with a `RetriableException`. It makes the setting a cap on total V2 insert time: any V2 insert still running at the bound fails and is redelivered, even while the server reports progress, and the server may keep and partly commit it (3500 of 4000 rows on MergeTree). An insert beyond `max.poll.interval.ms` (default 5 min) already forces a rebalance, so the practical loss is the last minute of the default 4 min. **C (disable progress headers per insert):** set `send_progress_in_http_headers=0` on the insert; measured bounded at 3032 ms, but it replaces a connector default (`ClickHouseSinkConfig` line "We set this so our ResponseSummary has actual data in it") and removes the keep-alive bytes proxies may rely on; excluded by TMO-01's "help text only" diff. **D (server `max_execution_time`):** measured code 159 at 3056 ms (retriable), but it needs a live, cooperating server, does nothing for a stalled server or a write stall (blocked > 12 s), and the server cancels the query | **A** for the phase, with B written out as the follow-up. Reason: the escape needs two non-default conditions, B is the only option that changes the semantics for every V2 insert, and ClickHouseWriter is the highest-risk file. If the user wants the setting to be a hard cap, take B and add its rollout row |
| U-2 | Client V1 whole-insert bound at the three `future.get()` sites | **A (bound):** `future.get(timeoutMs, MILLISECONDS)`; write stall 3268 ms, progress insert 3062 ms, clean release (0 sockets, no stuck thread). New consequence: a V1 insert that streams progress for longer than the bound now fails; a quiet V1 insert already fails at the library's 30 s. **B (record):** FORK-DELTA records V1 write stall (> 80 s blocked), V1 progress escape (8154 ms against `socket_timeout=3000`), library 30 s default | **A.** The write stall is a genuine hang on the connector's default client; `future.get(timeout)` is what `awaitInsertResponse` already does for V2 `async=true`; no thread is added. The V1 progress case is cut as a side effect (V1 cannot separate the two), and the help text says so |
| U-3 | Gate both bounds off when the operator named `socket_timeout` in `jdbcConnectionProperties` | Required by success criterion 3 ("still wins") and by the existing pins (`operatorSocketTimeoutLongerThanTheInsertTimeoutStillApplies` runs a 1000 ms answer against a 300 ms insert timeout; it would fail without the gate; `socket_timeout=0` must keep inserts unbounded, `41-RESEARCH` / FORK-DELTA item 14). V1 reads the same operator key from the URL options | Gate on the existing `operatorSetSocketTimeout` field for V2 and V1. This is not a free choice but the planner must say so in the help text: an operator who names `socket_timeout` gets no connector bound of any kind |
| U-4 | Non-insert requests: record, or bound some of them | **A (record all, recommended):** per-request measurements in the inventory table. **B (bound the put-path V2 Keeper state requests only):** per-request `QuerySettings.setNetworkTimeout(clickhouseClientInsertTimeoutMs)` in `ClickHouseHelperClient.queryV2`; retriable via the `SocketTimeoutException` root, but `queryV2` retries `retry` times so the wall time is `retry x` the bound, the setting then also governs non-inserts (its name and help text say "insert"), and `ALTER` / DESCRIBE variants are not covered. **C (client-level `socket_timeout`):** the gg7-rejected mechanism; bounds every V2 request including legitimate long ones; the `0` anchor row on `clientBuilder.setSocketTimeout(` must be rewritten | **A.** Bounding the swallow-to-null requests is a classification change (Phase 47 territory), and B widens a setting named "insert". The user can pick B for Keeper, which is the one put-path request with a clean retriable shape |
| U-5 | V2 `ping(timeout)` no-op | **A (record, corrects FORK-DELTA's "ping has its own timeout"):** measured blocked > 50 s with `ping(timeout=2000)`; it runs at start, not on the put path. **B (fix):** implement the existing `timeout` key with a per-request `QuerySettings.setNetworkTimeout` on a `SELECT 1`; V1 ping already works (2049 ms) | **A** unless the user wants the fix; B is small and uses an existing key but is a start-path change outside the five roadmap cases |
| U-6 | FORK-DELTA wording for recorded cases | Success criterion 2 says "a measurement that shows why they cannot be bounded", but mechanisms exist for write stall, progress headers and V1 | Record "not bounded by decision, bounded by X (measured number) if the user chooses" and the measured escape number; do not claim impossibility |

## Architectural Responsibility Map

This phase is a client-timeout and documentation phase inside one JVM sink connector; there is no browser/SSR/CDN tier. Tiers are connector layers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| V2 insert request lifecycle (send body, wait response, abort) | `sink.db.ClickHouseWriter` (three `client.insert(...)` sites) | ClickHouse Java client 0.11.0 (`Client.insert`, `cancelTransportRequest`) | The writer owns the call; the client owns the socket and the abort |
| Per-insert option and operator precedence | `sink.db.helper.ClickHouseHelperClient` (`setInsertSocketTimeoutV2`, `operatorSetSocketTimeout`) | — | One place decides whether the operator named `socket_timeout`; the new bounds must read the same flag |
| V1 insert wait | `sink.db.ClickHouseWriter` (`future.get()` x3) | ClickHouse V1 client (`ClickHouseClient`, shared worker pool) | The wait is in the writer; the worker thread is the library's |
| Retry classification of a timed-out insert | `util.Utils.handleException` (a `RetriableException` anywhere in the chain is retriable) | `Processing.doInsert` (wraps every writer failure in `RuntimeException(queryId, e)`) | Already correct; the writer only has to throw a `RetriableException` |
| Non-insert requests | `ClickHouseHelperClient`, `KeeperStateProvider` | `ClickHouseWriter.getTable/updateMapping/evolveTableSchema` | Recorded, not changed (U-4) |
| Help text | `sink.ClickHouseSinkConfig` (`ConfigDef` documentation, line 836) | `ClickHouseSinkConfig.CONFIG.configKeys()` for the pin | Text only |
| Fork contract and rollout statement | `docs/FORK-DELTA.md` (item 14, the `Known cost` row at line 52) | Phase 49's gg8 note | Phase 46 writes the limits; Phase 49 writes the release note |

## Standard Stack

No new library. The phase uses what is already shipped.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| ClickHouse Java client (client-v2, clickhouse-client, clickhouse-http-client, clickhouse-data) | 0.11.0 | V2 `Client.insert`, `InsertSettings.setNetworkTimeout`, `Client.cancelTransportRequest`; V1 `ClickHouseClient`/`ClickHouseRequest` | `clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)` [VERIFIED: `gradle/libs.versions.toml:2`, Read in Phase 45; sources of all four jars downloaded from Maven Central this session and read] |
| Kafka Connect API | 4.4.0 | `RetriableException` | The writer already throws it in `awaitInsertResponse` [VERIFIED: `ClickHouseWriter.java:1981`] |
| JUnit 5 + the in-repo `StubClickHouseServer`, `CountingHelperClient`, `InMemoryDLQ` | repo | Docker-free pins | No Mockito (OpenJ9 crash, HARN-08) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Per-insert watchdog + `cancelTransportRequest` | `Client.Builder.useAsyncRequests(true)` + `future.get(timeout)` | Roadmap forbids: each timed-out batch leaks a thread and a connection. The client documents `cancelTransportRequest` as client-side only |
| Watchdog | Client-level `setSocketTimeout` | Reads only (does not reach write stall or progress); bounds every V2 request; rejected in gg7; `0` anchor row |
| Watchdog | `setExecutionTimeout` / `max_execution_time` client option | Not sent to the server and not a transport timeout in 0.11.0; `getOperationTimeout()` is only used as `future.get(timeout)` on already-completed futures (`Client.java:2231`, `2299`, `2392`, `1483`), the same no-op [VERIFIED: sources read] |

**Installation:** none. **Version verification:** `clickHouseDriver = 0.11.0`; the sources jars read (`client-v2-0.11.0-sources.jar`, `clickhouse-client-0.11.0-sources.jar`, `clickhouse-http-client-0.11.0-sources.jar`, `clickhouse-data-0.11.0-sources.jar`) were fetched from `repo1.maven.org/maven2/com/clickhouse/<artifact>/0.11.0/` into separate empty directories and read only.

## Package Legitimacy Audit

No external package is installed or changed by this phase, so no `package-legitimacy` check applies (the seam supports `npm|pypi|crates` only; Phase 45 recorded the Maven gap). The four client jars are the already-pinned `com.clickhouse:*:0.11.0` coordinates; the sources jars were downloaded for reading and are not on any classpath.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none new) | — | — | — | — | — | — |

**Packages removed due to [SLOP] verdict:** none. **Packages flagged [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
 Kafka put(records) ─► ProxySinkTask.put ─► Processing.doLogic ─► Processing.doInsert
                                                                      │ wraps every failure: RuntimeException(queryId, e)   (Processing.java:72)
                                                                      ▼
                                             ClickHouseWriter.doInsert ─► getTable ─► [updateMapping / describeTable]   non-insert, put path
                                                                      │             ─► [evolveTableSchema -> ALTER]      non-insert, put path
                                                                      ▼
                                              route: RowBinary | JSON | String, then client V2 or V1
                                                  │                                   │
                          V2 (client_version=V2) ▼                                   ▼ V1 (default client_version)
          insertSettings: queryId, socket_timeout=T           getMutationRequest ─► piped stream ─► worker thread (shared pool)
          client.insert(..., DataStreamWriter) runs                 │                         │ future = request.execute()
          SYNCHRONOUSLY on the SinkTask thread (async=false)        │ body written into pipe  │ socket write / read
          body written by the lambda -> socket                      ▼                         ▼
                  │  write blocks if peer stops reading     future.get()   <── today: no bound; library read timeout 30 s only
                  │  read of headers: SO_TIMEOUT per read   (recommended: future.get(T))
                  ▼
        [recommended] watchdog task, armed before client.insert, disarmed when the DataStreamWriter has written and closed the body
                  │ fires at T ─► client.cancelTransportRequest(queryId) ─► socket closed ─► TransportException("Request was cancelled on client side")
                  ▼
        writer catches, sees the watchdog flag, throws RetriableException ─► Processing wrap ─► Utils.handleException finds RetriableException in chain
                  ▼
        RetriableException out of ProxySinkTask.put under errors.tolerance none AND all, no DLQ record
```

### Recommended Project Structure

No new package. Touch points (all existing):
```
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java        # three V2 insert call sites, three V1 future.get() sites
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java   # expose the operator flag (accessor) if the writer needs it
src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java       # help text only (lines 836-851)
src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java   # raw-socket responders (additive)
src/test/java/com/clickhouse/kafka/connect/sink/                                # new fork-owned pin classes, beside InsertStallBoundPinUnitTest
docs/FORK-DELTA.md                                                              # item 14 and the row at line 52
```

### Measured outcome per case (the table the help text and FORK-DELTA are written from)

All times are wall clock from the call to the exception or return, measured 2026-11-05 on ClickHouse `26.10.3.7` (real server cases) or a loopback stub, JDK 21, connector writer unless the row says "direct client".

| # | Case | Client | Unchanged tree | Mechanism measured | Result with mechanism |
|---|------|--------|----------------|--------------------|-----------------------|
| 1 | Quiet read stall (server accepts, never answers) | V2 | bounded: per-insert `socket_timeout` (gg7) | — | already bounded (existing pins green) |
| 2 | Progress headers, `async_insert=0`, 500-row blocks, MV with `sleepEachRow(0.002)`, 4000 rows, insert timeout 3000 | V2 | **escapes: 8141 ms OK** (control without bound 8147 ms) | whole-insert watchdog | 3007 ms `RetriableException > TransportException(Request was cancelled on client side) > RequestFailedException(Request aborted)` |
| 2b | Same, per-insert `max_execution_time=3` (direct client) | V2 | — | server limit | 3056 ms `ServerException(Code: 159 ... Timeout exceeded: elapsed 3015.807 ms, maximum: 3000.000 ms)`; 159 is in `DEFAULT_RETRIABLE_ERROR_CODES` |
| 2c | Same, per-insert `send_progress_in_http_headers=0` (direct client) | V2 | — | inactivity bound, no headers | 3032 ms `SocketTimeoutException(Read timed out)` |
| 2d | Same shape, **server default `async_insert=1`** (needs `function_sleep_max_microseconds_per_block` raised, or the single 4000-row block is rejected with code 160) | V2 | **bounded: 3057 ms** `DataTransferException > SocketTimeoutException(Read timed out)`; raw wire: 1 progress header at 8.047 s | — | — |
| 2e | Same as 2 with default block settings (single block, 0.5 ms/row, 2 s) | raw wire | quiet: 1 progress header at 2.009 s | — | the per-insert bound applies |
| 2f | Progress headers every 100 ms for 6 s from a raw-socket stub, insert timeout 1500 | V2 / V1 | **escapes: 6315 ms OK / 6175 ms OK** | whole-insert watchdog / `future.get(T)` | (Docker-free red pin shape) |
| 3 | Write-side stall (peer reads the request head, never the body), 64 MiB, `socket_timeout=500` | V2 | **blocked > 20 s** (also with `max_execution_time=3`: blocked > 12 s) | write-phase deadline / whole-insert watchdog | 3009 ms / 3014 ms `TransportException(Request was cancelled on client side)` |
| 3b | Write stall body size on macOS loopback | V2 | 2048 KB: sent, then read timeout at 539 ms; 4096 KB and larger: blocked at 6 s | — | threshold between 2 and 4 MiB here |
| 4 | V1 write stall, 64 MiB JSON | V1 | **blocked > 80 s** (library 30 s default); blocked > 25 s with operator `socket_timeout=1000`; jstack: the SinkTask thread is in `ClickHouseWriter.doInsertJsonV1` -> `CompletableFuture.get` | `future.get(T)` | 3268 ms `RetriableException > TimeoutException`; after 4 and 2 timed-out inserts: 0 TCP sockets, no thread in socket I/O, idle `ClickHouseWorker-n` pool threads only |
| 5 | V1 progress headers, `async_insert=0` shape, operator `socket_timeout=3000` | V1 | **escapes: 8154 ms OK** | `future.get(T)` | 3062 ms `RetriableException > TimeoutException` |
| 5b | V1 quiet read, server default `async_insert=1`, `socket_timeout=3000` | V1 | bounded 3061 ms `ExecutionException > ClickHouseException(Read timed out) > ConnectException` (retriable: the `ClickHouseException` carries an error code) | — | — |
| 6 | Non-insert against a server that accepts and never answers | V2 | **blocked > 50 s**: `describeTable`, `version`, `showTables`, `alterTableAddColumns`, `queryV2` (Keeper write), `ping(timeout=2000)` (each ended only when the helper client was closed) | — | — |
| 6b | Same | V1 | **ends at 30.03 s**: describe (returns null), version (null), showTables (empty), ALTER (`RuntimeException > ClickHouseException(Code: 159. Execution timed out)`), Keeper write (same code); ping 2049 ms | — | — |

Server state after aborts (real server, `dst` MergeTree + the same slow MV, `async_insert=0`): client abort at 3 s left 2000 rows at once and 3500 rows 12 s later; the server logged `ExceptionWhileProcessing`, code 210, 6043 ms. A `Memory` table left 0 rows after three failed `async_insert=0` inserts (client abort, server `max_execution_time`, socket timeout; the count rose only by the one control insert that completed), and the timed-out `async_insert=1` insert (2d) landed in full (12000 rows after three runs of 4000). The outcome depends on engine and `async_insert`; do not state a general rule. [VERIFIED: probe runs and `SELECT count()`; Phase 41 Probe 8 had already recorded a landed quiet insert]

### The shape that makes a real server stream progress headers (TMO-02 recipe)

```sql
CREATE TABLE sink_null (id UInt32, s String) ENGINE = Null;
CREATE TABLE dst       (id UInt32, s String) ENGINE = Memory;     -- or MergeTree ORDER BY id
CREATE MATERIALIZED VIEW mv_slow TO sink_null AS SELECT id, s FROM dst WHERE sleepEachRow(0.002) = 0;
```
Insert 4000 rows with these per-query settings (through `clickhouseSettings` for the writer): `async_insert=0, send_progress_in_http_headers=1, wait_end_of_query=1, input_format_skip_unknown_fields=1, http_headers_progress_interval_ms=50, max_insert_block_size=500, max_block_size=500, min_insert_block_size_rows=0, min_insert_block_size_bytes=0, input_format_parallel_parsing=0`. Raw wire (a Python socket reading the response): `HTTP/1.1 200 OK` and then one `X-ClickHouse-Progress` header per block at 1.009, 2.011, 3.015, 4.019, 5.025, 6.028, 7.036, 8.040 s, 8 headers, total 8.042 s. [VERIFIED: raw-socket capture on `26.10.3.7`]

What does not work, measured so the planner does not retry it: without `max_block_size=500` and `input_format_parallel_parsing=0` the view receives one 4000-row block and the server rejects the sleep (`Code: 160 ... The maximum sleep time is 3000000 microseconds. Requested: 8000000 microseconds per block (of size 4000)`); the three subsets tried (`max_block_size=500&input_format_parallel_parsing=0`; the three `*_insert_block_size*` settings plus `input_format_parallel_parsing=0`; `max_block_size=500` alone) all failed that way within 15 ms. With the server default `async_insert=1` the same insert streams no progress while it works, so the escape needs `async_insert=0` (set by the operator in `clickhouseSettings`, by the user's profile, or by an older server default). The earlier gg7 shape (E-21, `sleepEachRow(0.002)` MV, 8141 ms) reproduces to the millisecond figure.

A body insert through the connector, not only `INSERT ... SELECT`, therefore can be made to stream progress, which settles the roadmap flag; but only for a pipeline with many blocks and `async_insert=0`.

### Pattern 1: write-phase deadline (recommended for the V2 write stall)

**What:** Arm a scheduled task before `client.insert(...)` that calls `client.cancelTransportRequest(queryId)` after `clickhouseClientInsertTimeoutMs`; disarm it from inside the `DataStreamWriter` lambda after `out.close()`. If it fired, throw `RetriableException`.
**When to use:** `async=false` V2 inserts (the three sites), gated off when `operatorSetSocketTimeout`.
**Measured:** write stall 3009 ms; progress-streaming insert untouched (8056 ms, `body fully written; watchdog disarmed`); the whole-insert variant is the same code without the disarm (U-1 B).
**Example (a measured scratch prototype, not production code):**
```java
// Source: Client.java:2692 (client-v2-0.11.0-sources.jar): public void cancelTransportRequest(String queryId)
// scratch prototype, run this session; ScheduledThreadPoolExecutor(1, daemon) with setRemoveOnCancelPolicy(true)
AtomicBoolean fired = new AtomicBoolean();
ScheduledFuture<?> guard = WATCHDOG.schedule(
        () -> { fired.set(true); client.cancelTransportRequest(queryId.getQueryId()); },
        timeoutMs, TimeUnit.MILLISECONDS);
DataStreamWriter dataWriter = out -> {
    write(records, out);
    out.close();
    guard.cancel(false);            // write-phase form; omit this line for the whole-insert form
};
try (InsertResponse r = client.insert(table, dataWriter, format, insertSettings).get()) {
    ...
} catch (RuntimeException e) {
    if (fired.get()) throw new RetriableException("ClickHouse insert body not sent within " + timeoutMs + " ms (QueryId: [" + queryId.getQueryId() + "])", e);
    throw e;
} finally {
    guard.cancel(false);
}
```
Properties verified: the client registers the request under the query id (`registerTransportReq`, `Client.java:1880`) and unregisters it in `finally` (`1925`), so a late fire is a no-op; a cancelled operation is not retried (`failIfCancelled` / `requestIsNotCancelled`, `Client.java:2137-2141`, `2117-2123`); the connector always sets the query id (`insertSettings.setQueryId(queryId.getQueryId())` at all three sites); default client retry causes are `NoHttpResponse, ConnectTimeout, ConnectionRequestTimeout, ServerRetryable` (`ClientConfigProperties.java:157-159`), so a socket timeout is not retried and one POST is made. [VERIFIED: sources read, plus 4 scratch runs]

### Pattern 2: bounded V1 wait

**What:** Replace `future.get()` at the three V1 sites with `future.get(timeoutMs, MILLISECONDS)`; on `TimeoutException` call `future.cancel(true)` and throw `RetriableException` (the shape `awaitInsertResponse` already has for V2 `async=true`).
**Measured:** 3268 ms (write stall), 3062 ms (progress); the try-with-resources that closes the piped stream and the `ClickHouseClient` released the connection (no TCP socket, no thread in socket I/O afterwards). On the unchanged tree `future.get()` is the line the SinkTask thread blocks on (jstack: `ClickHouseWriter.doInsertJsonV1(ClickHouseWriter.java:1742)` under `CompletableFuture.get`).
**Why `future.get(T)` works on V1 and not on V2:** V1 `execute()` is `CompletableFuture.supplyAsync(..., getExecutor())` when `async` is true (the V1 default, `ClickHouseDataConfig.DEFAULT_ASYNC = true`), so the wait is real; V2 runs the supplier inline (`runAsyncOperation` returns `CompletableFuture.completedFuture(resultSupplier.get())` when `async=false`, `Client.java:2535-2542`). [VERIFIED: sources read]

### Anti-Patterns to Avoid
- **`Client.Builder.useAsyncRequests(true)` or `async` in `jdbcConnectionProperties` as the V2 fix.** Roadmap-forbidden; `future.cancel(true)` on a `CompletableFuture` does not abort the worker.
- **Reusing the writer's `scheduledExecutor` (`clickhouse-table-sync`) for the watchdog.** It runs `updateMapping`; a DESCRIBE blocked on client V2 (case 6) would hold the single thread and the watchdog would never fire. Use a separate daemon executor shut down in `stop()`.
- **A client-level `socket_timeout`.** Bounds every V2 request (gg7 decision, anchor row `clientBuilder.setSocketTimeout(` = 0) and still does not reach write stalls or progress.
- **Firing the watchdog without the operator gate.** `socket_timeout=0` or any operator value must keep today's behaviour (existing pins).
- **Adding `awaitInsertResponse` timeouts back as the fix.** With `async=false` its `future.get(timeout)` is already a no-op; leave it untouched (its code is unchanged since gg7).
- **Bounding `describeTableV2` / `showTablesV2` / `versionV2` without fixing their swallow.** A bounded hang returns null/empty and `getTable` throws a non-retriable `RuntimeException("Table %s does not exist")` (`ClickHouseWriter.getTable`, Read) which fails the task under `none` and DLQs the batch under `all`.

## Non-insert request inventory (TMO-03)

[VERIFIED: grep of `src/main/java` for `executeAndWait|queryRecords|client.query(|ping(|getClient()`, plus the callers read]

| Request | Client V2 call | Client V1 call | Runs on | Put path? | Measured bound (stalled server) | Failure shape |
|---|---|---|---|---|---|---|
| ping | `client.ping(timeout)` (`ClickHouseHelperClient.java:285`) | `clientPing.ping(server, timeout)` (`269`) | `ClickHouseWriter.start` (`:117`), `KeeperStateProvider` constructors (`:69`, `:78`) | no (start) | V2 **blocked > 50 s** with `timeout=2000` (future already complete, `Client.java:1492-1497`: `timeout > 0 ? future.get(timeout, TimeUnit.MILLISECONDS) : future.get()`); V1 2049 ms | false after `retry` tries (V1) |
| version | `queryRecords("SELECT VERSION()")` (`:321`) | `executeAndWait` (`:311`) | `ClickHouseWriter.start` (`:123`) | no | V2 > 50 s; V1 30.03 s (returns null) | null -> start fails |
| table list | `queryV2` (`:467`, retry loop `:373-389`) | `executeAndWait` (`:452`) | `updateMapping` <- `start` (`:147`), `TableMappingRefresher` thread, **`getTable` on a table not in the mapping (put thread)**, `urgentTableUpdate` | **yes** (cache miss, urgent update) | V2 > 50 s; V1 30.03 s | swallowed to empty list |
| DESCRIBE | `client.query(...).get()` (`:559`) | `executeAndWait` (`:536`) | `extractTablesMapping`, `getTable` fallback, `urgentTableUpdate`, `refreshTableAfterDDL` | **yes** | V2 > 50 s; V1 30.03 s | swallowed to null |
| ALTER (`alter_sync=1`) | `client.query(sql, settings).get()` (`:622`) | `executeAndWait` (`:609`) | `evolveTableSchema` when `auto.evolve` | **yes** (`doInsert` -> `evolveTableSchema`) | V2 > 50 s; V1 30.03 s | `RuntimeException("Failed to execute ALTER TABLE: ...")`, cause chain root is the timeout (V1: `ClickHouseException` code 159) |
| Keeper state read | always the V1 client (`KeeperStateProvider.getStateRecord` `:114`, `client.read(chc.getServer())` `:121`) | same | `Processing.doLogic` line 163, `exactlyOnce=true` | **yes** | V1 default 30.03 s (even when `client_version=V2`; measured for the V1 query path, the same code) | `RuntimeException` over `ClickHouseException` |
| Keeper state write (BEFORE / AFTER flag) | `queryV2(insertStr)` (`:163`) | `queryV1` (`:170`) | `Processing.doLogic`, 14 `setStateRecord` call sites, `exactlyOnce=true` | **yes** | V2 > 50 s; V1 30.03 s | `RuntimeException(e)`; root is the timeout once bounded |
| Keeper table create | `queryV2` / `queryV1` | | `KeeperStateProvider.init` | no (start) | as above | logged |

Two second-order effects worth recording: a table-refresh thread blocked on V2 holds `isUpdateMappingRunning=true`, so every later `updateMapping` returns `false` and callers fall to a direct `describeTable` (`ClickHouseWriter.getTable`); and a start-path request that never returns leaves `SinkTask.start` blocked.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Abort an in-flight V2 request | Closing sockets reflectively, `Thread.interrupt()`, or a second client | `Client.cancelTransportRequest(String queryId)` | Public, documented as client-side only, handles the register/unregister race and the retry loop (`failIfCancelled`) |
| Detecting that the operator named `socket_timeout` | A second parser | the existing `ClickHouseHelperClient.operatorSetSocketTimeout` (via a small accessor) | One parser shared with `createClientV2`; anchor row `operatorSetSocketTimeout` = 3 |
| A raw HTTP stub | Mockito mocks of the client | an additive raw-`ServerSocket` mode in `StubClickHouseServer` | The com.sun `HttpServer` reads the whole body before the responder runs and sends headers in one call, so it can neither stall the write side nor stream progress headers |
| Retriable classification of the timeout | A new exception type or a `Utils` branch | `RetriableException` from the writer | `Utils.handleException` already walks the chain and rethrows a `RetriableException` found anywhere, `Processing` wraps every writer failure first |

**Key insight:** every wall-clock mechanism here is a deadline on a blocking call that the library gives no deadline for; the only tools are a bounded `get` where the library really is async (V1) and an abort by id where it is not (V2). Anything else either only reaches reads or changes who owns the timeout.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted.

## Common Pitfalls

### Pitfall 1: A progress pin that passes on the unchanged tree
**What goes wrong:** a stub that answers slowly but quietly (`StubClickHouseServer.okAfter`) is bounded by the existing `socket_timeout`, so the "progress" pin is green before any change.
**Why:** `com.sun.net.httpserver` sends headers in one `sendResponseHeaders` call; the stub has no way to emit a header line every 100 ms.
**How to avoid:** a raw-socket responder. Measured bytes accepted by the real client V2 and V1: `HTTP/1.1 200 OK\r\n`, then repeated `X-ClickHouse-Progress: {"read_rows":"1","read_bytes":"1","total_rows_to_read":"0","written_rows":"0","written_bytes":"0"}\r\n` every 100 ms for the stall length, then `X-ClickHouse-Summary: {"written_rows":"1","written_bytes":"3"}\r\nContent-Length: 0\r\nConnection: close\r\n\r\n`. The stub must read the whole request first (chunked body: read until the `0\r\n\r\n` terminator; the V2 body has no `Content-Length`). Red on the unchanged tree: 6315 ms (V2) and 6175 ms (V1) against a 1500 ms bound. **Warning sign:** the pin is green before the fix.

### Pitfall 2: A write-stall pin that cannot block
**What goes wrong:** the body fits in the loopback socket buffers and the insert ends with a read timeout instead of blocking.
**Why:** measured on macOS: 2048 KB sent and then timed out at 539 ms, 4096 KB and larger blocked. Linux buffer sizes were not measured.
**How to avoid:** a non-reading raw responder (accept, read the request head only, never read the body) and a body of at least 64 MiB (64 records of 1 MiB, so no large single allocation); assert on a preemptive guard (`assertTimeoutPreemptively`, as the existing stall pin does). Red on the unchanged tree: blocked > 20 s (V2), > 80 s (V1). Close the stub before the test ends so the blocked client write fails and its thread ends (the same release order `StubClickHouseServer.close` already documents).

### Pitfall 3: Two timeouts racing and a stale cancel
**What goes wrong:** the watchdog fires on an insert that already finished, or on a later insert.
**How to avoid:** cancel the `ScheduledFuture` in `finally`; the client's `ongoingRequests` map is keyed by query id and the connector's id is unique per insert (a UUID in `QueryIdentifier`), and an unregistered id is a no-op (`Client.java:2692-2698`). Use `setRemoveOnCancelPolicy(true)` or the executor's queue grows by one task per insert.

### Pitfall 4: The operator gate and the existing pins
**What goes wrong:** a bound added without the gate breaks `operatorSocketTimeoutLongerThanTheInsertTimeoutStillApplies` (insert timeout 300 ms, answer after 1000 ms, operator `socket_timeout=6000`) and the `socket_timeout=0` unbounded contract.
**How to avoid:** gate on `operatorSetSocketTimeout` for V2 and V1; keep the three existing classes unchanged and green (15 + 23 + 38 tests today).

### Pitfall 5: V1 `future.get(T)` before the body is in the pipe
**What goes wrong:** on V1 the connector thread writes the body into the pipe before it reaches `future.get`; if the pipe blocks first, `get(T)` is never reached. A 64 MiB body fit in the pipe (jstack showed the thread in `future.get`), so the common case is covered, but a body larger than the pipe capacity can block in the write, where the library's `BlockingPipedOutputStream` raises `Write timed out after <timeout> ms` (an `IOException` that `Utils.handleException` already treats as retriable by message prefix, `CLICKHOUSE_CLIENT_ERROR_WRITE_TIMEOUT_MSG`). Not measured beyond 64 MiB. [ASSUMED]

### Pitfall 6: Help text claims that are not measured
**What goes wrong:** the help text promises a hard cap (the current text does) or lists a bound that only holds in one client.
**How to avoid:** every sentence of the new text maps to a row of "Measured outcome per case"; the pin asserts exactly those phrases, and the "does not bound" list names the non-insert requests and the progress-header case if U-1 is A.

### Pitfall 7: Operational traps (from MEMORY.md and Phase 45)
- `TESTCONTAINERS_HOST_OVERRIDE` is the literal `"null"` here: run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Docker is the colima profile `vz`.
- `./gradlew test` exit 255 is the intermittent OpenJ9 GPF in Mockito inline mocking: delete the `core.*.dmp` (about 8 GB), `javacore.*`, `Snap.*.trc`, `jitdump.*` one by one (never `git clean`), re-run once; `-PjavaVersion=21` is the escape hatch and was used for every Gradle run here. The new pins use no Mockito.
- Two full runs need `--rerun`; `latest` drifts (check the image digest first: it is `739e099a...`, version `26.10.3.7`).
- zsh expands `$M:path` as a history modifier; no `timeout` binary on macOS; `grep --include=*.java` needs quoting in zsh.
- Do not commit probes; this research ran everything in `/tmp/gsd46` (a `git clone --local`, sources jars, Python stubs, Java probes) and removed the container.

## Anchors (the invariant)

Verbatim rows this phase touches [VERIFIED: `.planning/scripts/fork-anchors.txt:219-222`, Read]:
```
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java	chc.setInsertSocketTimeoutV2(insertSettings, csc.getClickhouseClientInsertTimeoutMs());	3	TMO-01
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	settings.setNetworkTimeout(	1	TMO-01
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	operatorSetSocketTimeout	3	TMO-01
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	clientBuilder.setSocketTimeout(	0	TMO-01
```
Rules (from the tail-3 checklist and Phase 45): rows change only in the same commit as, or after, the code they pin; the gate checks every commit in `PRE_TAG..HEAD` against that commit's own copy of the file; the file now has 200 data rows. Consequences for the recommended code: an accessor that reads `operatorSetSocketTimeout` raises that row from 3 to 4 (update the row in the same commit); the three `chc.setInsertSocketTimeoutV2(...)` call lines must stay textually identical (count 3) or the row is rewritten with them; the `0` row on `clientBuilder.setSocketTimeout(` must stay 0 unless the user picks U-4 C. New rows to propose (counts measured on the real tip before committing): the watchdog schedule call, the V1 `future.get(` bound helper, and a `0` row on a bare `future.get()` in `ClickHouseWriter.java` (it is 3 today, so the row is `future.get()  0` after the change). The tail gate command, unedited, is in `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md` (Gate commands): `PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <range end>`.

## Code Examples

### What the current help text says (verbatim, to be replaced as text only)
```java
// Source: ClickHouseSinkConfig.java:836-851 (Read this session)
configDef.define(CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS,
        ConfigDef.Type.LONG,
        clickhouseClientInsertTimeoutMsDefault,
        ConfigDef.Range.atLeast(100),
        ConfigDef.Importance.LOW,
        "Maximum time in milliseconds to wait for a single ClickHouse insert response. " +
                "Must stay below consumer.override.max.poll.interval.ms: if an insert hangs longer than the " +
                "poll interval, Kafka assumes the worker is dead and rebalances the partitions mid-insert. " +
                "Leave a comfortable margin (e.g. at least 2x the slowest expected insert) so transient slow " +
                "inserts do not race the poll interval. On timeout a RetriableException is thrown, so the Connect " +
                "framework retries the batch; in exactly-once mode the retry reuses insert_deduplication_token " +
                "and ClickHouse drops duplicates. Default: " + clickhouseClientInsertTimeoutMsDefault + " ms.",
        group, ++orderInGroup, ConfigDef.Width.SHORT, "ClickHouse client insert timeout in ms."
);
```
Related values [VERIFIED: grep this session]: `clickhouseClientInsertTimeoutMsDefault = TimeUnit.MINUTES.toMillis(4)` (`ClickHouseSinkConfig.java:98`); `this.clientVersion = props.getOrDefault(CLIENT_VERSION, "V1");` (`:380`); `this.addClickHouseSetting("send_progress_in_http_headers", "1", false);` with the comment `//We set this so our ResponseSummary has actual data in it` (`:303`); `this.addClickHouseSetting("wait_end_of_query", "1", false);` (`:300`); `//this.addClickHouseSetting("async_insert", "0", false);` (`:301`, an anchor row, CFG-02).

### Draft help text if the recommended package is taken (U-1 A, U-2 A, U-4 A)
```text
Maximum time in milliseconds the connector gives one ClickHouse insert before it fails the batch with a
RetriableException, so the Connect framework retries it (in exactly-once mode the retry reuses
insert_deduplication_token). What it bounds: on client V2 (client_version=V2), a server that stops answering
(an inactivity limit that restarts on every byte received) and a server that stops reading the request body (the
whole batch must be sent within this time); on client V1 (the default client_version), the wait for the insert
response, including a stalled body write. What it does not bound: on client V2, an insert during which the server
keeps sending progress headers (it runs to its natural end); the time spent serializing the batch; and every
request that is not an insert (DESCRIBE, table refresh, version query, ping, ALTER, Keeper state reads and
writes), which have no connector limit on client V2 and the client library's 30 s inactivity limit on client V1.
A timed-out insert may still be committed, in part or in full, by the server, so with exactlyOnce=false the retry
can duplicate rows. When jdbcConnectionProperties names socket_timeout the connector adds no limit of its own.
Keep it below consumer.override.max.poll.interval.ms ... Default: 240000 ms.
```
Planner note: every clause above corresponds to a measured row; if U-1 chooses B, replace the progress clause with "a hard limit on total insert time on client V2"; if U-2 chooses B, say V1 inserts keep only the library's 30 s inactivity limit and the write stall is not bounded.

### Help-text pin (Docker-free, no Mockito)
```java
// documentation of the key is read from the registered ConfigDef, the pattern AdditionalRetriableErrorCodesPinUnitTest uses
ConfigDef.ConfigKey key = ClickHouseSinkConfig.CONFIG.configKeys().get(ClickHouseSinkConfig.CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS);
String doc = key.documentation;
for (String phrase : MEASURED_BOUNDS_AND_LIMITS) {   // one constant list, each phrase tied to a measured row id
    assertTrue(doc.contains(phrase), phrase);
}
```
Source: pattern at `AdditionalRetriableErrorCodesPinUnitTest.java:196` (`ClickHouseSinkConfig.CONFIG.configKeys().get(ERROR_KEY)`) [VERIFIED: Read].

### StubClickHouseServer extension (additive, no Mockito)
Existing shape [VERIFIED: `StubClickHouseServer.java:26-162`, Read]: a `com.sun.net.httpserver.HttpServer` context reads `exchange.getRequestBody().readAllBytes()` before calling the `Responder`; factories `stalling()`, `okAfter(Duration)`, `ok()`, `status(...)`, `serverException(...)`; `close()` releases a latch before `server.stop(0)`. Needed additions, each a static factory that shares `requests()`/`close()` semantics: (1) `progressHeaders(interval, total)`: raw `ServerSocket`, daemon accept thread, reads the full request (head, then chunked terminator or `Content-Length`), writes the byte sequence in Pitfall 1; records a `Request` like the HTTP mode. (2) `notReadingBody()`: raw `ServerSocket`, reads the head only, then holds the connection until `close()`. Neither needs a new dependency. The HTTP-mode fields (`server`, `handlers`) are `final`, so the cleaner change is a second small private class or a nested raw mode selected by the factory; the planner picks, keeping the six existing users (`InsertStallBoundPinUnitTest`, `ClientErrorShapePinUnitTest`, `ClientEndpointAndOptionsPinUnitTest`, `WriterHelperClientClosingUnitTest`, `ClientInsertWirePinUnitTest`, `ClientErrorShapes`) unchanged and green.

### Pin shape through `ProxySinkTask.put` (criterion 3)
Reuse `InsertStallBoundPinUnitTest`'s writer (`StallPinWriter` over `CountingHelperClient`) and its put test shape: `assertThrows(RetriableException.class, () -> task.put(List.of(sinkRecord)))` inside `assertTimeoutPreemptively`, `assertEquals(0, dlq.size())`, one POST, parameterized over `Route` (JSON, STRING, ROW_BINARY) x `errors.tolerance` (none, all). Differences for the new cases: the root cause is no longer `SocketTimeoutException` (it is `TransportException`/`RequestFailedException` on V2, `TimeoutException` on V1), so assert `RetriableException` in the chain, not the root class. Why it is retriable under both tolerances: `Processing.doInsert` wraps the writer failure as `new RuntimeException(queryId.toString(), e)` (`Processing.java:72`), and `Utils.handleException` throws a `RetriableException` for one found anywhere in the cause chain before the `DataException` rule and before the tolerance branch (`Utils.java`, the loop `for (Throwable t = e; t != null; t = t.getCause())`, Read). `ChunkFlusher.putDirect` (`:63-76`) calls the same function, so the buffered path needs no extra change. Operator-wins pins stay as they are; add a V1 pair (operator `socket_timeout` named: no bound; not named: bound).

### Red-first order
1. Add the new pins and the stub responders; run on the unchanged tree: progress (V2, V1), write stall (V2, V1), V1 quiet stall must fail (preemptive guard) or be recorded as a limit pin; keep the run output as evidence.
2. Fix; the same pins go green; the three existing classes stay green.
3. Help text and its pin last.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `awaitInsertResponse`'s `future.get(timeout)` as the V2 bound | per-insert `socket_timeout` (gg7); abort by id is the only tool for write and progress | Phase 41 (gg7); this phase | the future-timeout idiom is a no-op wherever `async=false` has completed the future (`ping`, `awaitInsertResponse`, `queryAll`) |
| "the case with the server's default `async_insert` was not measured" (FORK-DELTA item 14) | measured: bounded on `26.10.3.7` (3057 ms) | this phase | narrows the recorded limit |
| "a write-side stall on client V1 was not measured" | measured: blocked > 80 s | this phase | V1 write stall is a real hang on the default client |
| "ping has its own timeout" | false on client V2 with `async=false` | this phase | correct FORK-DELTA item 14 |

**Deprecated/outdated:** the roadmap's V1 site lines `1460, 1586, 1741` (Phase 41) are now `1611, 1742, 1898` after the tail merge [VERIFIED: grep this session].

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Linux CI buffers behave like macOS for the write-stall pin; 64 MiB blocks on both | Pitfall 2 | Medium: the pin would not block on CI and would pass vacuously. Mitigate: a body larger than the measured threshold by 16x, and assert the stub received the head and that the insert is still running at the guard before the fix |
| A2 | The tail gate takes about three minutes on a tip with more commits (measured 2 m 56 s on the unchanged tree) | Pre-flight | None: the planner runs it once at the end |
| A3 | A V1 body larger than the pipe capacity raises `Write timed out after` and is already retriable | Pitfall 5 | Low-medium: V1 write stall for huge bodies might block before `future.get`; not measured beyond 64 MiB |
| A4 | RowBinary and String routes behave like the measured JSON route for V1 write stall and V2 write stall | Measured outcome | Low: all routes share the same `client.insert` / piped-stream mechanism; the pins are parameterized over the three routes and will show it |
| A5 | `max.poll.interval.ms` default of 5 minutes (used in the U-1 reasoning) | U-1 | Low: the connector's own help text and WARN already name `consumer.override.max.poll.interval.ms`; the default is Kafka's, not read this session |
| A6 | An operator on client V1 who names `socket_timeout` has chosen their own transport limits and should get no connector bound | U-3 | Low: stated by success criterion 3 for the V2 `socket_timeout` case; extending it to V1 is the consistent reading |

## Open Questions

1. **Should the write-phase deadline also cover the time to serialize the batch?**
   - Known: the writer lambda serializes and writes in one pass, so the deadline measured from `client.insert` covers serialization plus transfer; a very large batch with slow `Struct` conversion counts against the bound.
   - Unclear: whether the gg7 help text's "wait for a single insert response" ever meant to include serialization. The old per-insert `socket_timeout` never counted it.
   - Recommendation: arm the watchdog right before `client.insert(...)`, state "time spent sending the batch, serialization included" in the help text, and pin that the deadline is disarmed after `out.close()`.
2. **Does U-1 B fit the milestone?**
   - Known: B works (3005 ms) and is the only way to make "the setting is a total-time cap" true.
   - Unclear: the user's tolerance for failing a long-but-progressing insert.
   - Recommendation: user decision; the plan carries both branches as separately revertable commits.
3. **Is a V1 pin for the quiet read stall worth its suite time?**
   - Known: the library's 30 s default ends it (30140 ms in Phase 41), and with `future.get(T)` it ends at T.
   - Recommendation: one V1 quiet-stall pin at a 300 ms bound is cheap (the bounded tree ends in about 300 ms); the unchanged-tree red run takes 30 s once and is recorded as evidence, not repeated in the suite.
4. **Where do the new pins' measured numbers live?** Recommend a `46-COMMAND-EVIDENCE.md` in the Phase 41/44 style, listing the shapes in "Measured outcome per case" with the server version and digest.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | live re-measurements, full suite | yes | `colima list`: `vz` Running (8 CPU, 16 GiB) | `testUnit` runs with `DOCKER_HOST=unix:///nonexistent.sock` (used this session) |
| `clickhouse/clickhouse-server:latest` | TMO-02 live record | yes (local) | `26.10.3.7`, `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`; also local `26.3`, `25.8` | — |
| JDK | Gradle | yes | default `java` OpenJDK 21.0.12.1; the toolchain's OpenJ9 17 is the crash-prone one | `-PjavaVersion=21` (used for every run here) |
| Gradle wrapper | build | yes | 9.8.2, `--offline` works for the compile and `testUnit` (all dependencies cached) | — |
| `python3 -I` | probe stubs | yes | system | none needed in the repo |
| Maven Central | sources jars (read only) | yes | 0.11.0 sources for four client artifacts downloaded into separate empty directories | the compiled jars in the Gradle cache plus `javap` |

**Missing dependencies with no fallback:** none.

## Validation Architecture

Skipped: `.planning/config.json` has `"nyquist_validation": false` [VERIFIED: `config.json` read this session]. The phase gate, which the roadmap defines, is: cache-busted `testUnit` with Docker unreachable; a full suite on `latest` x V2 (two `--rerun` runs per the Phase 45 rule, `SELECT version()` recorded); the invariant script once on an explicit SHA; the new pins seen red on the unchanged tree and green after the fix. Quick commands (all `-PjavaVersion=21`, `env -u TESTCONTAINERS_HOST_OVERRIDE`): `DOCKER_HOST=unix:///nonexistent.sock ./gradlew --offline --console=plain testUnit --tests '*InsertStallBoundPinUnitTest' --tests '*InsertSocketTimeoutOptionUnitTest' --tests '*RetriableExceptionPassThroughUnitTest'` (9 s measured, 76 tests, all green on the unchanged tree).

## Security Domain

`security_enforcement` is enabled (`"security_enforcement": true`, `"security_asvs_level": 1`) [VERIFIED: `config.json`].

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V7 Error Handling and Logging | yes | The new `RetriableException` messages carry the insert timeout and the query id only; never a record value, a column name or `getMessage()` of a cause (STATE.md standing rule: a new writer message must not embed a record value) |
| V12 / availability (resource exhaustion) | **yes, central** | A server or network black hole must not pin the SinkTask thread past `max.poll.interval.ms` (the goal of the phase); the watchdog executor is one daemon thread, tasks are cancelled and removed after each insert, and it is shut down in `stop()` |
| V14 Configuration | yes | No config key is added; the gate on the operator's `socket_timeout` is the only config interaction |
| V2, V3, V4, V5, V6 | no | no auth, session, access-control, input-validation or crypto change |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Peer stops reading the request body, blocking the worker thread forever | Denial of service | Write-phase deadline with `cancelTransportRequest` (V2), `future.get(timeout)` (V1) |
| Slow-but-chatty server (progress headers) holding the thread past the poll interval | Denial of service | Recorded limit or whole-insert watchdog (U-1) |
| Watchdog thread leak or unbounded task queue | Denial of service | One daemon thread, `setRemoveOnCancelPolicy(true)`, shutdown in `stop()`; leak check: 0 TCP sockets and no stuck thread after 3 (V2) and 4 (V1) timed-out inserts |
| Timed-out insert partially committed, redelivered | Tampering (data duplication) | Documented (F-2 rollout consequence); `exactlyOnce=true` reuses the dedup token; no change to the `BEFORE`/`AFTER` sequence |
| Record value leaking into a new timeout message | Information disclosure | Message text fixed to timeout, query id; a pin that the message carries no record sentinel |

## Project Constraints (from CLAUDE.md)

- `AGENTS.md`: do not change dependencies; only add tests, never delete or weaken; Spotless / Google Java Style (46 pre-existing violating files, so format per touched file); `VERSION` and `CHANGELOG.md` are Phase 49's; do not use `InMemoryState` in production paths; **do not change batch composition after setting `BEFORE`** (the retry after a timeout must send the identical block: the timeout fix throws before any state change and does not touch batching); `ClickHouseWriter.java` and `ClickHouseSinkConfig.java` are high-risk files (extra tests); use `ClickHouseHelperClient` for client settings; logging at TRACE/DEBUG/INFO as stated; no record values in logs.
- `.claude/CLAUDE.md`: behaviour preservation (code changes limited to genuine bug fixes; the fork runs in production); test target is ClickHouse `latest` with client V2; rebasability (additive changes, small targeted fixes); no Mockito inline mocks; GSD workflow enforcement.
- STATE.md standing rules that bind this phase (Phase 45 research recorded them): writer failure text stays value-free; a main-code change outside a phase's declared scope stops for a user decision (the roadmap restricts Phase 46 to `ClickHouseWriter`/`ClickHouseHelperClient` timeout code, the help text, `StubClickHouseServer` and docs; no `Utils`, `Processing`, `ProxySinkTask` diff); two-run gates use `--rerun`.
- ROADMAP Phase 46 note: each code change is its own commit and revert unit (Phase 47's note states the rule for its phase; apply the same shape here: pins, V2 fix, V1 fix, help text + pin, FORK-DELTA, in that order).

## Sources

### Primary (HIGH confidence)
- This session's runs: real ClickHouse `26.10.3.7` (image `sha256:739e099a...`), the real `ClickHouseWriter` and `ClickHouseHelperClient` built from a throwaway `git clone --local` of `2b419d4b` in `/tmp/gsd46` (compile, `testUnit` subset, Java probes `Probe`, `ProbeStall`, `ProbeNonInsert`, Python raw-socket stubs `blackhole`, `progressstub`, `rawwire`); `jstack` and `lsof` on the probe JVMs; `SELECT count()`, `system.settings`, `system.query_log` on the server
- Sources read: `com.clickhouse:client-v2:0.11.0` (`Client.java` insert, ping, cancel, runAsyncOperation, `ClientConfigProperties`, `HttpAPIClientHelper`, `InsertSettings`, `CommonSettings`, `QuerySettings`), `clickhouse-client:0.11.0` (`AbstractClient`, `ClickHouseClient`, `ClickHouseConfig`, `ClickHouseClientOption`), `clickhouse-http-client:0.11.0`, `clickhouse-data:0.11.0` (`ClickHouseDataStreamFactory`, `BlockingPipedOutputStream`, `ClickHouseDataConfig`)
- Repo files read: `ClickHouseWriter.java` (100-135, 164-185, 236-275, 1335-1365, 1415-1440, 1440-1500, 1570-1630, 1700-1800, 1850-2093), `ClickHouseHelperClient.java` (86-760), `Utils.java`, `Processing.java:55-80`, `ProxySinkTask.java:95-160`, `KeeperStateProvider.java:55-200`, `ClickHouseSinkConfig.java` (285-330, 420-440, 825-860), `StubClickHouseServer.java`, `InsertStallBoundPinUnitTest.java`, `InsertSocketTimeoutOptionUnitTest.java` (1-60), `CountingHelperClient.java` (1-80), `fork-anchors.txt` (1-14, 218-239), `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md`, `45-RESEARCH.md`, `46-CONTEXT.md`, `REQUIREMENTS.md`, ROADMAP Phase 46 and 47 sections, `docs/FORK-DELTA.md` item 14 and the row at line 52, `docs/ROLLOUT-v1.7.0_gg7.md` row T6, `41-RESEARCH.md` probe table, `41-COMMAND-EVIDENCE.md` E-06, `44-COMMAND-EVIDENCE.md` E-21

### Secondary (MEDIUM confidence)
- Phase 41 and 44 recorded numbers (30140 ms V1 quiet stall; 8141 ms / 3007 ms progress shape) as cross-checks; this session reproduced the progress numbers (8141 ms, 8147 ms control)

### Tertiary (LOW confidence)
- Linux CI loopback buffer behaviour and Kafka's default `max.poll.interval.ms` (A1, A5)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH (no change; coordinates unchanged)
- Mechanisms per case: HIGH (each run end to end against the real writer or client, with a measured red on the unchanged tree and a measured result with the mechanism)
- Server-side progress behaviour: HIGH for `26.10.3.7` (raw wire captures and writer runs); MEDIUM across versions (an older server's default `async_insert` and block handling were not probed)
- Non-insert inventory: HIGH for the call sites and the stalled-server numbers; MEDIUM for put-path frequency (read from code, not counted at runtime)
- Pitfalls: HIGH for the stub and gate pitfalls, MEDIUM for the Linux write-stall threshold (A1)

**Research date:** 2026-11-05
**Valid until:** 2026-11-12 (`latest` moves; re-record `SELECT version()` and the image digest before the live record, and re-run the red pins on the unchanged tree)
