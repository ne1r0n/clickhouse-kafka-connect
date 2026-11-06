# Phase 41: Bounded Insert Timeout - Research

**Researched:** 2026-11-02
**Domain:** ClickHouse Java client V2 (0.11.0) per-request socket timeout, `ClickHouseWriter` V2 insert sites, Docker-free stall pins (JDK `HttpServer` stub), Kafka Connect retry classification
**Confidence:** HIGH (every mechanism question was settled by a probe on the 0.11.0 test classpath, most also end to end through the real writer and `ProxySinkTask.put`; one real-server run on ClickHouse 26.10.3.7)

<user_constraints>
## User Constraints (from CONTEXT.md)

CONTEXT.md was auto-generated (discuss skipped via `workflow.skip_discuss`). It has no locked decisions and no deferred ideas.

### Locked Decisions
None. (The `## Decisions` section of 41-CONTEXT.md holds only the line below.)

### Claude's Discretion
"All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions."

### Deferred Ideas (OUT OF SCOPE)
"None — discuss phase skipped."

### Boundaries carried from the ROADMAP phase section (treat as locked; they are the user's decisions in the milestone)
- **Goal:** with `async=false`, an insert into a server that stops answering fails after `clickhouseClientInsertTimeoutMs` as a retriable error. An operator's `socket_timeout` in `jdbcConnectionProperties` keeps that value. A red pin proves the hang before the fix. The cases that stay unbounded are written down. **No new config key, `async_insert` is not touched.**
- **Mechanism chosen by the user:** a per-insert `socket_timeout` (through `InsertSettings`) at the V2 insert sites. The client-level alternative was **not** chosen. If the per-insert option did not reach the socket the phase had to stop for the user. It does reach the socket (see Probe Evidence), so no stop is needed.
- **Never** `async=true` / `async_insert` as the fix (each timed-out batch leaks a thread and a connection). **No Mockito** in the new tests.
- If either TMO-02 outcome is not retriable on the fixed tree, stop for the user: classification changes belong to RSK-02 (Phase 42). Both outcomes are retriable (see Probe Evidence), so no stop is needed.
- Phase gate: cache-busted `testUnit`, a full suite on `latest` x V2, and the anchors invariant.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PIN-01 | `StubClickHouseServer` has a stall responder; a red F-2 pin uses `assertTimeoutPreemptively`, so unfixed code fails instead of hanging the suite | Stub design (latch released before `server.stop`), measured red: unfixed writer blocks past an 8 s guard on all three V2 routes; `assertTimeoutPreemptively` throws `org.opentest4j.AssertionFailedError` "... execution timed out after N ms" (Probe 4, Probe 7) |
| TMO-01 | A stalled `async=false` insert fails after `clickhouseClientInsertTimeoutMs` as a retriable error, through a per-insert `socket_timeout` on the V2 insert sites; an operator `socket_timeout` in `jdbcConnectionProperties` still wins, pinned by a test; `async_insert` untouched | `InsertSettings.setNetworkTimeout(long, ChronoUnit)` reaches the socket (Probes A-G); per-insert value **overrides** the client-level one, so the writer must skip it when the operator named `socket_timeout` (Probe D1/D2, Probe 4) |
| TMO-02 | Classification of the timeout failure is pinned through `ProxySinkTask.put` under both `errors.tolerance` values | Fixed-tree result: `RetriableException`, `dlq=0`, one POST, under `none` and `all`, on JSON, String and RowBinary routes (Probe 4) |
| TMO-03 | Limits recorded: write-side stall (peer stops reading the body) and the three V1 `future.get()` sites stay unbounded | Write-side stall still blocked after 15 s with per-insert 500 ms (Probe 6); V1 sites are `ClickHouseWriter.java:1460, 1586, 1741`; V1 library default `socket_timeout` is 30000 ms and bounded a read-side stall at 30.1 s (Probe 4 V1) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md`, `./AGENTS.md`, `./.claude/CLAUDE.md` and the task prompt. The planner must verify compliance.

- `src/main` changes are allowed in this phase. `ClickHouseWriter.java` is **high risk** (binary format inserts; needs thorough testing and a full suite). `ClickHouseSinkConfig.java` must be **unchanged** (empty diff).
- No Mockito, `mockStatic` or `mockConstruction` in the new tests (OpenJ9 crash, HARN-08). Use real objects, subclass seams, `CountingHelperClient`, `StubClickHouseServer`.
- Never `async_insert` / `async=true` as a fix. The CFG-02 anchor `//this.addClickHouseSetting("async_insert", "0", false);` (count 1 in `ClickHouseSinkConfig.java`) must stay.
- Never run `spotlessApply` over the whole tree (46 pre-existing violations). Use the scoped per-file hook (recipe below). `ClickHouseWriter.java` and `ClickHouseHelperClient.java` are both "IS DIRTY" at baseline with **import-order-only** diffs (measured today); `StubClickHouseServer.java` is "IS CLEAN".
- New functionality needs feature-test coverage; tests read data back / assert real outcomes; DRY; static string constants extracted; Google Java Style; Lombok where it fits; SLF4J (TRACE per-record, DEBUG batch, INFO lifecycle); do not log secrets or record data (this includes `jdbcConnectionProperties`, which can carry credentials).
- Hot paths (`ClickHouseWriter`): no per-record work, no avoidable allocation. A per-batch `InsertSettings` setter is fine; do **not** re-parse `jdbcConnectionProperties` per batch (compute once in the helper constructor).
- Do not edit published notes (`docs/ROLLOUT-v1.7.0_gg6.md`, `CHANGELOG.md` published sections). `VERSION` and `CHANGELOG.md` are Phase 44's. Do not commit unless the plan says so; never push.
- `h` is a zsh alias for `history`: multi-line shell logic goes under `bash -c` / `/bin/bash`; Python helpers under `python3 -I`.
- Docker is colima profile **`vz`** (bare `colima status` hits the default, stopped profile). Gradle commands use `env -u TESTCONTAINERS_HOST_OVERRIDE` and `-PjavaVersion=21` (HotSpot, avoids the OpenJ9 `./gradlew test` exit-255 crash).
- No new dependency (AGENTS.md: never upgrade dependencies without asking). None is needed.

## Summary

F-2 is real and cheap to fix. With `async=false` the V2 client runs the HTTP call on the caller thread inside `client.insert(...)`, so `awaitInsertResponse`'s `future.get(timeoutMs)` is handed an already-completed future and can never time out. The only thing that bounds a stalled V2 insert is a socket read timeout. The per-insert `InsertSettings` option `socket_timeout` (public typed setter `setNetworkTimeout(long, ChronoUnit)`) reaches the socket on client 0.11.0: against a loopback server that accepts and never answers, the insert ends in ~`N` ms with `DataTransferException > SocketTimeoutException`, one POST (no client-side retry), and the wire request is byte-identical to the unfixed one (the option is not sent to the server). So the user's chosen mechanism works and **no user stop is required**.

Two findings change the plan shape. (1) The per-insert option **overrides** the client-level one (an operator's `socket_timeout=300` lost to a per-insert 3000 in Probe D1). Setting it unconditionally would silently defeat "an operator's `socket_timeout` keeps that value". The writer must therefore skip the per-insert option when the operator named `socket_timeout` in `jdbcConnectionProperties`; the cleanest place is `ClickHouseHelperClient`, which already owns that string and the `setReplicaTagHeaderV2(InsertSettings)` precedent. (2) The connector's default `client_version` is **V1** (`props.getOrDefault(CLIENT_VERSION, "V1")`), so the fix is inert for any connector that did not set `client_version` to something other than `V1`. This belongs in the rollout item.

Through the real `ClickHouseWriter` and `ProxySinkTask.put` (writer run against the stub, helper client scripted with `CountingHelperClient`), the fixed tree throws a `RetriableException` within ~300-430 ms, writes no DLQ record, and does not fail the task, under both `errors.tolerance=none` and `all`, on all three V2 routes (JSON, String, RowBinary). `Processing` wraps the failure in `RuntimeException(queryId, e)` and `Utils.handleException` drills to the root `SocketTimeoutException`, which it already maps to `RetriableException`. No classification change is needed, so RSK-02 stays Phase 42's.

**Primary recommendation:** add `ClickHouseHelperClient.setInsertSocketTimeoutV2(InsertSettings, long)` (skips when the operator named `socket_timeout`; otherwise `settings.setNetworkTimeout(Math.min(timeoutMs, Integer.MAX_VALUE), ChronoUnit.MILLIS)`) and call it right after `chc.setReplicaTagHeaderV2(insertSettings);` at the three V2 insert sites; give `StubClickHouseServer` a `stalling()` factory whose latch `close()` releases **before** `server.stop(0)`; write the red pins first and record the red run, then the fix.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Bound on a stalled V2 insert | Connector client layer (`ClickHouseHelperClient` + `ClickHouseWriter` insert sites) | ClickHouse Java client (HC5 socket read timeout) | The connector owns which timeout value applies to an insert; the client enforces it on the socket |
| Operator override of `socket_timeout` | `ClickHouseHelperClient` (owns `jdbcConnectionProperties`) | Client builder `setOption` | The helper already forwards `k=v` pairs and can tell whether the key was named |
| Timeout value (`clickhouseClientInsertTimeoutMs`) | `ClickHouseSinkConfig` (read-only here) | — | Existing setting; unchanged. The writer reads `csc.getClickhouseClientInsertTimeoutMs()` |
| Retry classification of the timeout | `Utils.handleException` via `ProxySinkTask.put` | Kafka Connect framework (redelivery) | Existing rule root-cause `SocketTimeoutException` -> `RetriableException`; Phase 41 only pins it |
| Duplicate protection after a timed-out-but-landed insert | ClickHouse block deduplication + `exactlyOnce` state machine | — | With `exactlyOnce=false` there is no dedup token: the retry duplicates (measured, Probe 8) |
| Stall simulation in tests | Test fixture (`StubClickHouseServer`, JDK `HttpServer`) | — | Docker-free, no new dependency |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:client-v2` | 0.11.0 | `Client.insert(...)`, `InsertSettings.setNetworkTimeout` | Already the project's V2 client (catalog `clickHouseDriver`); no bump allowed in this milestone [VERIFIED: gradle cache `client-v2-0.11.0.jar`, test classpath printed this session] |
| Apache HttpClient5 | 5.6.5 | Socket/response timeout enforcement under the client | Transitive, on the classpath [VERIFIED: test classpath `httpclient5-5.6.5.jar`] |
| JUnit Jupiter | 5.14.4 | `assertTimeoutPreemptively`, parameterized tests | Existing test stack [VERIFIED: test classpath `junit-jupiter-api-5.14.4.jar`] |
| JDK `com.sun.net.httpserver` | JDK 17/21 | Stall stub | Already used by `StubClickHouseServer` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `CountingHelperClient` (test, public, `sink.db`) | in repo | Real helper client built from the real builder, with scripted ping/version/tables so the writer starts without a server | Writer-level and `ProxySinkTask.put`-level pins |
| `InMemoryDLQ` (test, `sink.dlq`) | in repo | Assert "no DLQ record" | TMO-02 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Per-insert `socket_timeout` | Client-level `Client.Builder.setSocketTimeout` | **Rejected by the user.** It bounds ping, DESCRIBE, version, table refresh and the Keeper state queries as well (`ClickHouseHelperClient` builds one client for all of them) |
| Per-insert `socket_timeout` | `async=true` + `future.get(timeout)` | **Forbidden.** `cancel(true)` does not stop the worker; each timed-out batch leaks a thread and a held connection |
| Unconditional per-insert set | Skip when operator named the key | Unconditional is **wrong**: per-insert beats the client-level value, so it would override the operator |

**Installation:** none. No package is added or changed.

**Version verification:** versions read from the Gradle-resolved test runtime classpath on 2026-11-02 (`./gradlew -q --offline -I <init script> printTestCp`). `build.gradle.kts`/catalog untouched.

## Package Legitimacy Audit

No external package is installed, upgraded or removed in this phase, so the audit table is empty by construction.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none) | — | — | — | — | — | — |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Kafka Connect worker thread
   |  put(records)
   v
ProxySinkTask.put ---------------------------------------------------------+
   | Record.convert, group by topic/partition                              |
   v                                                                       |
Processing.doLogic -> doInsert                                             |
   | catch (Exception e) { throw new RuntimeException(queryId, e) }        |
   v                                                                       |
ClickHouseWriter.doInsert -> doInsertBatch -> route by SchemaType          |
   | SCHEMA_LESS -> doInsertJsonV2   | STRING -> doInsertStringV2          |
   | SCHEMA/CDC  -> doInsertRawBinaryV2                                    |
   v                                                                       |
InsertSettings (db, replica tag, dedup token, query id, server settings)   |
   + NEW: chc.setInsertSocketTimeoutV2(settings, insertTimeoutMs) <-- skipped if the operator named socket_timeout
   v
client.insert(table, writer, format, settings)   [async=false: runs INLINE on this thread]
   |  HTTP POST over pooled socket, read timeout = per-insert socket_timeout (or the operator's client-level value)
   v
ClickHouse  --(no answer within N ms)--> SocketTimeoutException
   ^                                           |
   |                                           v
   |        DataTransferException > SocketTimeoutException (thrown directly by insert(...), not via ExecutionException)
   |                                           |
   +-------------------------------------------+--> Processing wraps -> ProxySinkTask.put catch ->
                                                    Utils.handleException: root cause SocketTimeoutException -> RetriableException
                                                    (thrown BEFORE the tolerance/DLQ branch, so tolerance none|all both retriable, DLQ empty)
                                                    -> Kafka Connect redelivers the batch (errors.retry.timeout)
```

### Recommended Project Structure
```
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java   # + setInsertSocketTimeoutV2, + once-computed operator flag
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java                # + 3 one-line calls (after setReplicaTagHeaderV2)
src/test/java/.../sink/db/helper/StubClickHouseServer.java                              # + stalling() factory, latch released first in close(); class + API made public
src/test/java/.../sink/db/helper/InsertStallBoundPinUnitTest.java                       # PIN-01 / TMO-01 (writer-level, 3 routes + override + unit-level helper cases)
src/test/java/.../sink/InsertStallClassificationUnitTest.java                           # TMO-02 (ProxySinkTask.put, both tolerances)
docs/FORK-DELTA.md, .planning/scripts/fork-anchors.txt                                  # record rows
```
`ClickHouseSinkConfig.java` stays untouched.

### The three V2 insert sites (all of them) [VERIFIED: ClickHouseWriter.java Read this session; `grep -n "client.insert(\|new InsertSettings()"`]
`Client.insert` has 8 overloads; the connector uses only the `DataStreamWriter` ones.

| Site | Method | `new InsertSettings()` | `chc.setReplicaTagHeaderV2(insertSettings);` | `client.insert` call | Route |
|------|--------|------------------------|---------------------------------------------|----------------------|-------|
| 1 | `doInsertRawBinaryV2` (def. line 1317) | 1337 | 1339 | 1361 `awaitInsertResponse(client.insert(table.getName(), dataWriter, format, insertSettings), queryId)` | `SCHEMA`/`DEBEZIUM_CDC` with RowBinary |
| 2 | `doInsertJsonV2` (def. 1597) | 1613 | 1615 | 1633-1636: 5-arg with `insertColumns` or 4-arg, into `insertFuture`, then `awaitInsertResponse(insertFuture, queryId)` | `SCHEMA_LESS`, JSON-bypass routes |
| 3 | `doInsertStringV2` (def. 1751) | 1761 | 1763 | 1783 `awaitInsertResponse(client.insert(table.getName(), dataWriter, clickHouseFormat, insertSettings), queryId)` | `STRING_SCHEMA` |

Nothing else on V2 inserts: `KeeperStateProvider` writes state through `client.query` (`INSERT INTO ... VALUES`), not `Client.insert`, and stays unbounded (see Limits). Line numbers are for HEAD `77c39790` and shift once the three lines are added.

### Pattern 1: Per-insert socket timeout through a helper next to the replica-tag precedent
**What:** one `ClickHouseHelperClient` method owns the policy (skip when operator named the key; clamp to int range); the writer calls it once per site.
**When to use:** every V2 `InsertSettings` built in `ClickHouseWriter`.
**Example (design sketch; the probe compiled and ran exactly this logic against the real writer):**
```java
// ClickHouseHelperClient.java — beside setReplicaTagHeaderV2(InsertSettings)
// computed once in the constructor from the operator's jdbcConnectionProperties, never per batch
private final boolean operatorSetSocketTimeout;   // true if a "socket_timeout=" pair is present (value irrelevant, even 0)

public void setInsertSocketTimeoutV2(InsertSettings settings, long timeoutMs) {
    if (settings == null || operatorSetSocketTimeout) {
        return;                                   // the operator's client-level value wins: a per-insert value would override it
    }
    settings.setNetworkTimeout(Math.min(timeoutMs, Integer.MAX_VALUE), ChronoUnit.MILLIS);
}

// ClickHouseWriter.java — at each of the three sites, directly after the replica tag
chc.setReplicaTagHeaderV2(insertSettings);
chc.setInsertSocketTimeoutV2(insertSettings, csc.getClickhouseClientInsertTimeoutMs());
```
`operatorSetSocketTimeout` must use the **same parse** as `createClientV2` (`startsWith("?")` stripped, split on `&`, `split("=", 2)`, `kv.length == 2 && !kv[0].isEmpty()`, **exact, case-sensitive** key `ClientConfigProperties.SOCKET_OPERATION_TIMEOUT.getKey()` i.e. `socket_timeout`; `SOCKET_TIMEOUT=5` is ignored by the client, Probe 2). DRY: extract that loop into one private static parser used by both `createClientV2` and the flag (behaviour-identical; existing `ClientEndpointAndOptionsPinUnitTest` and `NetworkCompressionPinUnitTest` already cover `createClientV2` option forwarding). The clamp keeps a `long` setting from exceeding the client's `Integer` option range; `Integer.MAX_VALUE` ms (about 24.8 days) is "effectively unbounded".

### Pattern 2: Stall responder with a latch that `close()` releases first
**What:** the stub owns a `CountDownLatch`; the stalling responder reads the body (so the stall is read-side, matching F-2), records the request (the existing handler already does that before calling the responder), then awaits the latch with a bounded wait; `close()` does `release.countDown()` **then** `server.stop(0)`.
**Why the order matters (measured, Probe 3):** `StubClickHouseServer` uses the default executor, so a blocked handler runs on the single `HTTP-Dispatcher` thread, and in JDK 21 `HttpServer.stop(0)` ends with `dispatcherThread.join()`. With the latch not released, `stop(0)` was still blocked after 3000 ms; with `countDown()` first it finished in 19 ms. A stall test that closes the stub in try-with-resources would otherwise hang the suite at the end of every case, including the green ones.
```java
// Source: probe (Probe 3) + existing StubClickHouseServer shape
static StubClickHouseServer stalling() throws IOException {
    CountDownLatch release = new CountDownLatch(1);
    return new StubClickHouseServer(exchange -> awaitQuietly(release), release);  // private ctor stores the latch
}
@Override public void close() { release.countDown(); server.stop(0); }
// awaitQuietly: release.await(STALL_CAP_SECONDS, SECONDS) with a cap so a test bug cannot hang the suite; on release just exchange.close()
```
Because the dispatcher is held, a stalling stub serves exactly one in-flight request; that is what the pins need (they also assert **one** POST, i.e. no client retry). If a case needs a second request, give the stub `server.setExecutor(Executors.newCachedThreadPool())` (as the probes did) instead. Visibility: `StubClickHouseServer` is currently package-private in `sink.db.helper`, but `ProxySinkTask`'s injectable constructor is package-private in `sink`. Make the stub `public` (with `Request`, `Responder`, `port()`, `requests()`, the factories public), as `CountingHelperClient` already is, so the TMO-02 test can sit in package `sink`.

### Pattern 3: Writer-level pin, three routes, red -> green
Drive the real `ClickHouseWriter` (subclass overriding `createHelperClient` to return a scripted `CountingHelperClient`, the existing `WriterHelperClientClosingUnitTest.CountingWriter` shape) with `client_version=V2`, `bypassFieldCleanup=true`, `clickhouseClientInsertTimeoutMs=300`, port = the stalling stub. Wrap the insert in `assertTimeoutPreemptively(Duration.ofSeconds(10), ...)`. At writer level the failure is the client's own `DataTransferException` (the writer adds no `RetriableException` on this path), so assert that its root cause is `java.net.SocketTimeoutException` (via `Utils.getRootCause(e, true)`) and that the stub saw exactly one POST; the retriable classification is asserted one layer up in the `ProxySinkTask.put` pin. Cover all three routes by parameterizing over `{JSON schemaless Map, String value, Struct + one `Int32` column}` (all three ran red/green in the probe). Always `writer.stop()` / `unregisterStatistics()` in `finally`, **after** the stub is closed or released, and use a unique topic per case (MBean names collide, Phase 40 E-03 attempt 1).

### Pattern 4: `ProxySinkTask.put` pin (TMO-02)
`new ProxySinkTask(config, dlq, writer)` (package-private ctor, test in package `sink`), `task.start()` (the scripted helper makes `start()` succeed), `task.put(records)` inside `assertTimeoutPreemptively`. Parameterize `errors.tolerance` over `ClickHouseSinkConfig.ERROR_TOLERANCE_NONE` and `ClickHouseSinkConfig.ERROR_TOLERANCE_ALL`. Assert `RetriableException`, `dlq.size() == 0`, one POST at the stub, and that the cause chain contains the `SocketTimeoutException`. Follow `WrappedDataExceptionClassificationPinUnitTest` for the outcome enum style. `finally`: `task.stop()` after releasing the stall.

### Anti-Patterns to Avoid
- **Unconditional per-insert set.** Overrides the operator (Probe D1: jdbc 300, per-insert 3000 -> fired at 3003 ms). Fails TMO-01's "operator wins".
- **Client-level `setSocketTimeout`.** Not chosen; also bounds ping/DESCRIBE/Keeper queries. Add a **count-0 anchor** so it cannot slip in.
- **Treating the operator test as red-before-fix.** It is green on the unfixed tree (the operator path already works). It is a *guard against a naive fix*, not a red pin. Say so in the evidence and prove its discriminating power by a recorded mutation (drop the skip, watch it go red).
- **Wall-clock assertions with tight lower bounds** in parallel forks. Prefer outcome assertions (a delayed-OK responder that answers after 800 ms: operator 1500 ms wins -> insert succeeds; a naive per-insert 300 ms fails) over `elapsed >= x`.
- **Closing the stalling stub before releasing the latch** (hangs `stop(0)`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bounding a blocked socket read | A watchdog thread / `future.cancel(true)` / `Thread.interrupt` | `InsertSettings.setNetworkTimeout` (per-request HC5 response timeout) | A blocked HC5 socket read is not interruptible; abandoned threads overlap with the redelivered batch |
| Detecting the operator's `socket_timeout` | A second hand-written split loop | One shared private parser used by `createClientV2` and the flag | Two parsers drift (`?` prefix, `=` in values, empty keys) |
| HTTP stub for the stall | WireMock / MockWebServer / Toxiproxy | `StubClickHouseServer` + latch | No new dependency, Docker-free, already the repo's pattern |
| Retry classification of the timeout | A new `RetriableException` pass-through | Existing `Utils.handleException` root-cause rule | Already correct for this shape; the pass-through is RSK-02 (Phase 42) |
| Timeout assertion in tests | `Thread.sleep` + manual thread kill | `assertTimeoutPreemptively` | Makes the unfixed tree fail instead of hanging (PIN-01) |

**Key insight:** the client already does all the work; the phase is one option at three sites plus the discipline of "operator wins", the stub's release order, and honest records of what stays unbounded.

## Probe Evidence (all run 2026-11-02 on the test classpath, client-v2 0.11.0, HotSpot JDK 21; scratch code in `/tmp/p41`, not in the repo)

Stub = JDK `HttpServer` on `127.0.0.1:0`, POST handler reads the body then blocks on a latch. "chain" is the exception cause chain.

| Probe | Setup | Result |
|-------|-------|--------|
| A | `client.insert(...)` with no timeout, `async=false` | Blocked for the whole 4000 ms guard, 1 POST. **Reproduces F-2.** |
| B | `setNetworkTimeout(500, MILLIS)` on `InsertSettings` | Failed at 503 ms, 1 POST, chain `DataTransferException > SocketTimeoutException`, `Utils.handleException(RuntimeException(q, ...))` -> `RetriableException` |
| C / C2 | `setOption("socket_timeout", Integer 500)` / `(…, Long 500L)` | Same as B (503 ms). Both numeric types accepted |
| D1 | client-level `socket_timeout=300` via jdbc + per-insert 3000 | Fired at **3003 ms**: per-insert **overrides** client-level |
| D2 | client-level 3000 + per-insert 300 | 304 ms |
| D3 / D4 | jdbc `socket_timeout=400`, with and without leading `?` | 407 / 403 ms: the operator path works today, both forms |
| E | per-insert `Integer.MAX_VALUE` | Still waiting at the 2000 ms guard (effectively unbounded) |
| F | per-insert `5_000_000_000` ms (> int) | No conversion error; still waiting at 3 s guard. Clamp anyway |
| G | per-insert `0` | Unbounded (waits to guard) |
| Probe 2 | `client.getConfiguration()` for `socket_timeout` | `""`->`0` (key present, default), `socket_timeout=400`->`400`, `?socket_timeout=0`->`0`, `ssl=true&socket_timeout=77&sslmode=strict`->`77`, `SOCKET_TIMEOUT=5`->`0` (ignored). The config always carries the key, so it cannot tell "operator set 0" from "default"; parse the jdbc string instead |
| Probe 3 | default-executor `HttpServer`, handler stalled, then `stop(0)` | Latch released first: stop done in 19 ms. Not released: **still blocked after 3003 ms** |
| Probe 4 (unfixed tree, real writer + `ProxySinkTask.put`, `client_version=V2`, insert timeout 300 ms, JSON/String/RowBinary) | stall stub | **HUNG** past the 8 s guard on all three routes under `none` and `all`; 1 POST |
| Probe 4 (fix applied in a scratch copy of `ClickHouseWriter` + `ClickHouseHelperClient`) | same | `none`: 432 ms (JSON), 338 ms (String), 350 ms (RowBinary); `all`: 304-308 ms. Always `RetriableException`, chain `RetriableException > RuntimeException > DataTransferException > SocketTimeoutException`, `dlq=0`, 1 POST |
| Probe 4 operator cases (fixed copy) | jdbc `socket_timeout=1500` + insert timeout 300 | 1506 ms: **operator wins** (a naive per-insert would end at ~300 ms). jdbc `socket_timeout=200` + insert timeout 5000 -> 207 ms. Mixed `ssl=true&socket_timeout=1500&sslmode=strict` -> 1507 ms |
| Probe 4 wire | POST query string, patched vs unpatched | Identical: `query_id=…&insert_deduplication_token=…&wait_end_of_query=1&send_progress_in_http_headers=1&query=INSERT…FORMAT JSONEachRow&network_compression_method=lz4&input_format_skip_unknown_fields=1`. `socket_timeout` is **not** sent to the server |
| Probe 4 V1 (`client_version=V1`, unfixed tree, insert timeout 300) | stall stub | Returned after **30140 ms** with a `RetriableException` (chain `RuntimeException > ExecutionException > ClickHouseException > ConnectException`): bounded by the V1 library's own default `socket_timeout`; `awaitInsertResponse` is not involved on V1 |
| Probe 5 | `ClickHouseClientOption` defaults | `SOCKET_TIMEOUT` key `socket_timeout` default **30000**; `CONNECTION_TIMEOUT` `connect_timeout` 5000; `MAX_EXECUTION_TIME` 0 |
| Probe 6 | per-insert 500 ms, 64 MiB body, peer accepts and never reads | **Still blocked after 15000 ms.** `SO_TIMEOUT` is a read timeout; the write-side stall is unbounded (the TMO-03 limit) |
| Probe 7 | `assertTimeoutPreemptively(200 ms, stalled read)` | `org.opentest4j.AssertionFailedError`: `stall pin ==> execution timed out after 200 ms` |
| Probe 8 (**real ClickHouse 26.10.3.7**, table `(id UInt32, d UInt8 DEFAULT sleep(2)) ENGINE=Memory`, insert takes ~2 s) | per-insert 5000 / 1000, operator jdbc 1000 | no timeout: 2096 ms OK; per-insert 5000: 2061 ms OK; **per-insert 1000: failed at 1003 ms**; operator 1000: 1002 ms. The real server accepts the request (the option is not a server setting). Afterwards `count()` = **4**: the two timed-out inserts also landed. A legitimately slow insert starts failing, and the failed one still commits |

## Operator `socket_timeout` today: the real path [VERIFIED: ClickHouseHelperClient.java Read this session]
`ClickHouseWriter.start` builds the helper with `.setJdbcConnectionProperties(csc.getJdbcConnectionProperties())` (ClickHouseWriter.java:107; also the V1 per-batch helper at :1829). `ClickHouseSinkConfig` stores `props.getOrDefault(JDBC_CONNECTION_PROPERTIES, jdbcConnectionPropertiesDefault).trim()` (config line 275; `JDBC_CONNECTION_PROPERTIES = "jdbcConnectionProperties"`). `createClientV2()` (ClickHouseHelperClient.java:165-223) sets `IGNORE_UNKNOWN_CONFIG_KEY` first, then loops the string:

```java
if (jdbcConnectionProperties != null && !jdbcConnectionProperties.isEmpty()) {
    String props = jdbcConnectionProperties.startsWith("?") ? jdbcConnectionProperties.substring(1) : jdbcConnectionProperties;
    for (String pair : props.split("&")) {
        String[] kv = pair.split("=", 2);
        if (kv.length == 2 && !kv[0].isEmpty()) {
            clientBuilder.setOption(kv[0], kv[1]);
        }
    }
}
```
So `socket_timeout=<ms>` becomes a **client-level** option (it bounds every request: ping, DESCRIBE, queries, inserts). `NetworkCompressionPinUnitTest` already pins that `?socket_timeout=12345` lands in `chc.getClient().getConfiguration()` (lines 79-83). The override test runs the real path by passing the string through `ClickHouseSinkConfig` -> writer -> `CountingHelperClient(builder)`; the Docker-free proof is the stall-stub timing/outcome, plus an assertion-only unit test on the helper method (`InsertSettings.getAllSettings()` has no `socket_timeout` key when the operator named it, and carries it otherwise; no network, no clock).

## Classification trace under both tolerances [VERIFIED: ProxySinkTask.java Read; Utils.java Read; Probe 4]
`ProxySinkTask.put` catches the `Processing` failure and calls `Utils.handleException(e, errorTolerance, retryOnSocketException, additionalCodes, records)` before the `if (errorTolerance)` branch that fills `failedMessages`/DLQ. `handleException`: `getRootCause(e, true)` walks to the deepest cause (no server exception in this chain) = `java.net.SocketTimeoutException`; the clause `rootCause instanceof SocketTimeoutException` throws `new RetriableException(e)`. That throw precedes the tolerance branch, so `errors.tolerance=all` does not route the batch to the DLQ. Measured on the fixed copy: `RetriableException`, `dlq=0`, 1 POST, both values. This is the same rule `WrappedDataExceptionClassificationPinUnitTest` already pins for a synthetic `SocketTimeoutException`; TMO-02 adds the real-client shape. Note `ConnectException` (V1 chain) is a different root; V1 is not in scope.

## Common Pitfalls

### Pitfall 1: Stub `close()` hangs on a stalled handler
**What goes wrong:** the test body finishes (or the red pin fails) and try-with-resources `stub.close()` blocks forever.
**Why:** default-executor handler holds the `HTTP-Dispatcher` thread; JDK `stop(0)` joins it (Probe 3).
**How to avoid:** latch released first in `close()`; capped `await`; assert `stub.close()` is last.
**Warning signs:** the fixed-tree case passes but the class never finishes.

### Pitfall 2: Operator override silently lost
**What goes wrong:** the per-insert value replaces the operator's client-level `socket_timeout`.
**Why:** request settings are merged over client config (Probe D1).
**How to avoid:** skip when the operator named the key; test with operator value **larger** than the insert timeout (1500 vs 300) so the naive version fails; record a mutation run.
**Warning signs:** an override test that only uses an operator value smaller than the insert timeout passes under both implementations.

### Pitfall 3: Red pin that hangs or leaks
**What goes wrong:** on the unfixed tree the insert blocks in a non-interruptible socket read; `assertTimeoutPreemptively` fails the case but the worker thread stays blocked.
**How to avoid:** the `finally` order is stub close/release -> `writer.stop()` (IMMEDIATE close aborts any in-flight request) -> `unregisterStatistics()`. The red-reason rule (Phase 40 style): a red counts only when every failing case is an `org.opentest4j.AssertionFailedError` whose message contains `execution timed out after`; a compile error or any other exception is not a recorded red.

### Pitfall 4: Fix is inert on the default client
**What goes wrong:** operators on the default see no change and assume F-2 is closed everywhere.
**Why:** `ClickHouseSinkConfig.java:380` is `props.getOrDefault(CLIENT_VERSION, "V1")`; `ClickHouseWriter.java:100` is `boolean useClientV2 = clientVersion.equals("V1") ? false : true;`. The fix covers only `client_version` set to anything other than `V1` (the production setting, V2).
**How to avoid:** say it in the FORK-DELTA row and the rollout item; V1 keeps its library 30 s read bound.

### Pitfall 5: Legitimately slow insert now fails and, with `exactlyOnce=false`, duplicates
**What goes wrong:** an insert that takes longer than `clickhouseClientInsertTimeoutMs` (default 4 min) used to succeed; it now fails retriable and is redelivered.
**Evidence:** Probe 8: per-insert 1000 ms vs a 2 s insert: client failed at 1003 ms, server still committed the row (count 4 for 4 inserts). With `exactlyOnce=false` there is no dedup token, so redelivery inserts again (duplicate). With `exactlyOnce=true` the retry reuses `insert_deduplication_token`; dedup works only if the target engine deduplicates (not probed).
**How to avoid:** rollout item states the behaviour change, the duplicate risk, and that the operator can raise `clickhouseClientInsertTimeoutMs` (min 100, `ConfigDef.Range.atLeast(100)`) or set `socket_timeout` in `jdbcConnectionProperties`.

### Pitfall 6: Spotless dirt on the two production files
**What goes wrong:** a scoped check reports DIRTY and the executor "fixes" it with `spotlessApply`, reordering imports of untouched lines.
**How to avoid:** baseline is import-order-only for both files (measured). A new import (`java.time.temporal.ChronoUnit`) goes in the file's existing import block position; the post-edit Spotless diff must differ from the baseline diff only in import lines. Run only the per-file IDE hook (recipe below).

### Pitfall 7: MBean name collision between cases
**What goes wrong:** `InstanceAlreadyExistsException` from a leftover topic MBean (Phase 40 E-03 attempt 1).
**How to avoid:** unique topic per case; `task.stop()` / `writer`'s `unregisterStatistics()` in `finally`.

### Pitfall 8: Timing flakiness in parallel forks
**What goes wrong:** `maxParallelForks = cpus/2`; a tight bound flakes.
**How to avoid:** insert timeout 300 ms (min the config allows is 100 in production; tests may go lower but should not), `assertTimeoutPreemptively` bound 10 s (the red run costs that bound per case once), outcome assertions over elapsed lower bounds.

## Code Examples

### Stalling stub factory and close order
```java
// Source: StubClickHouseServer.java (existing shape) + Probe 3
static StubClickHouseServer stalling() throws IOException {
    CountDownLatch release = new CountDownLatch(1);
    return new StubClickHouseServer(exchange -> awaitQuietly(release), release);
}
@Override public void close() {
    release.countDown();   // BEFORE stop: a stalled handler owns the dispatcher thread
    server.stop(0);
}
```

### PIN-01 shape (red on the unfixed tree)
```java
// Source: Probes 4 and 7; test-side names are suggestions
assertTimeoutPreemptively(Duration.ofSeconds(BOUND_SECONDS), () -> {
    Throwable thrown = assertThrows(Throwable.class, () -> writer.insertJsonV2(records, table, queryId));
    assertInstanceOf(SocketTimeoutException.class, Utils.getRootCause((Exception) thrown, true));
    assertEquals(1, posts(stub).size());
});
```

### TMO-02 shape
```java
// Source: Probe 4; WrappedDataExceptionClassificationPinUnitTest outcome style
ProxySinkTask task = new ProxySinkTask(config(tolerance), dlq, writer);   // package-private ctor, test in package sink
task.start();
try {
    assertTimeoutPreemptively(Duration.ofSeconds(BOUND_SECONDS), () -> {
        RetriableException e = assertThrows(RetriableException.class, () -> task.put(records));
        assertEquals(0, dlq.size());
        assertInstanceOf(SocketTimeoutException.class, Utils.getRootCause(e, true));
    });
} finally {
    stub.close();   // releases the stall first
    task.stop();
}
```
Config keys, all [VERIFIED: ClickHouseSinkConfig.java:30-101 Read]: `HOSTNAME = "hostname"`, `PORT = "port"`, `SSL_ENABLED = "ssl"`, `ERRORS_TOLERANCE = "errors.tolerance"`, `ERROR_TOLERANCE_ALL = "all"`, `ERROR_TOLERANCE_NONE = "none"`, `BYPASS_FIELD_CLEANUP = "bypassFieldCleanup"`, `JDBC_CONNECTION_PROPERTIES = "jdbcConnectionProperties"`, `CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS = "clickhouseClientInsertTimeoutMs"`, `clickhouseClientInsertTimeoutMsDefault = TimeUnit.MINUTES.toMillis(4)`; client selection key `CLIENT_VERSION = "client_version"` [VERIFIED: ClickHouseSinkConnector.java:39] (use the constant: `"clientVersion"` silently selects V1, which cost a probe run).

### Operator-wins unit case (no network, no clock)
```java
InsertSettings settings = new InsertSettings();
helperWithJdbc("socket_timeout=1500").setInsertSocketTimeoutV2(settings, 300);
assertFalse(settings.getAllSettings().containsKey(SOCKET_TIMEOUT_KEY));      // operator's client-level value stands
InsertSettings other = new InsertSettings();
helperWithJdbc("ssl=true").setInsertSocketTimeoutV2(other, 300);
assertEquals(300L, other.getNetworkTimeout());
```
(`getNetworkTimeout()` returns the option or the client default 0 as a `Long`, [VERIFIED: javap -c on `CommonSettings.getNetworkTimeout`]; `getAllSettings()` is on `InsertSettings` [VERIFIED: javap]. Confirm the exact `getAllSettings` key spelling when writing the test.)

## Gates, records and recipes

### Gate recipe (Phase 40's, adapted) [VERIFIED: 40-COMMAND-EVIDENCE.md E-03, E-07, E-11 Read]
Baselines now (task prompt, from Phase 40): `testUnit` 1719 tests / 2 skips; full suite 2317 tests / 18 skips on ClickHouse `latest` x V2; `fork-anchors.txt` rows=180 bad=0. Expected new totals = baseline + the number of new test cases (compute from the parameterized matrix; do not guess).

```sh
# red run (plan "tests first"): cache-busted, Docker unreachable, HotSpot 21
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*InsertStallBoundPinUnitTest' --tests '*InsertStallClassificationUnitTest' > /tmp/gsd41/exec/red.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd41/exec/xml-red
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py junit /tmp/gsd41/exec/xml-red --class <Class> tests=N failures=M
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py failed /tmp/gsd41/exec/xml-red --expect <Class>#<method> ...
# save the red tree as a patch (new files need `git add -N` first, `git reset -q --` after)
# gate on the fix commit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py junit <xml-dir> tests=N failures=0 errors=0 skipped=18
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py skips <xml-dir>
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows <180 + new>
bash .planning/scripts/verify-merge-invariants.sh 54e63648 <final SHA>      # cumulative, ~41 min, run once on the final SHA, alone
```
`41-evidence-check.py` is a **byte copy** of `39-evidence-check.py` (`cp` then `cmp`), never edited. Docker for the full suite: `docker` CLI at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`, colima profile `vz` Running (checked), image `clickhouse/clickhouse-server:latest` = 26.10.3.7. Record version and digest before and after as Phase 40 did.

The red tree to record: the unfixed `ClickHouseWriter`/`ClickHouseHelperClient` plus the stub release-order change plus the new tests. Expected reds: the three writer-level stall cases and the six `ProxySinkTask.put` cases (3 routes x 2 tolerances, or the routes you choose), each `AssertionFailedError ... execution timed out after`. Expected greens on the unfixed tree: the operator-override cases (operator path already works) and the unit-level helper cases only if the helper method exists, so write helper-method unit cases in the same commit as the fix and record them as "added with the fix".

### Scoped Spotless recipe (never the whole tree)
```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd41/exec/sc-<name>.out 2> /tmp/gsd41/exec/sc-<name>.err
```
stderr says `IS CLEAN` or `IS DIRTY`; stdout is the formatted file when dirty. Baselines measured 2026-11-02: `ClickHouseWriter.java` DIRTY (import-order only), `ClickHouseHelperClient.java` DIRTY (import-order only), `StubClickHouseServer.java` CLEAN. New test files must come back CLEAN.

### `docs/FORK-DELTA.md` rows to change (in the code commit) [VERIFIED: FORK-DELTA.md Read lines 187-262]
1. **New row in "Connector source"** (grouped by area, then file path then position; the CLS row at line 38 is the model): V2 inserts carry a per-insert `socket_timeout` taken from `clickhouseClientInsertTimeoutMs` (clamped to `Integer.MAX_VALUE` ms) through `ClickHouseHelperClient.setInsertSocketTimeoutV2`, skipped when `jdbcConnectionProperties` names `socket_timeout`; `ClickHouseSinkConfig` unchanged; `async_insert` untouched. Files: `sink/db/ClickHouseWriter.java`, `sink/db/helper/ClickHouseHelperClient.java`. Upstream: `upstream/main` (`9bed6f79`) has the same three `awaitInsertResponse` call sites and no per-insert timeout [VERIFIED: `git show upstream/main:.../ClickHouseWriter.java | grep` this session: lines 1098, 1314, 1459, def 1486], so this is fork-only. Pinned by: TMO-01, TMO-02, PIN-01 and the new test class names.
2. **Known cost 14** (line 228, currently "has no effect while `async=false` (published knowingly open)") is rewritten: closed on client V2 by the row above, with the residual limits stated (below). Keep the number 14 and the owner/re-open-trigger form of the neighbouring items.
3. TMO-03 limits, written into item 14 (or a new item 24; do not renumber 15-23): (a) a **write-side stall** (the peer stops reading the request body) is unbounded: Probe 6, 64 MiB body, still blocked after 15 s with per-insert 500 ms; `SO_TIMEOUT` bounds reads only. (b) The three client V1 `future.get()` sites (`ClickHouseWriter.java:1460, 1586, 1741`) have no connector timeout; **new evidence replaces "library's own timeout options were not examined"**: the V1 library's default `socket_timeout` is 30000 ms and a read-side stall returned after 30140 ms (Probe 4 V1); a write-side stall on V1 was not probed. (c) Recommended extra sentence (not in the ROADMAP list): requests outside the V2 inserts keep the client default `socket_timeout=0`: ping has its own timeout, but DESCRIBE, version, table refresh and `KeeperStateProvider` state queries are unbounded unless the operator sets `socket_timeout`. (d) The fix is V2-only and `client_version` defaults to V1.
4. The "Known costs" intro and the closing paragraph at line 288 list "open items 12 to 23"; keep them consistent with whatever item numbers you add.

### `.planning/scripts/fork-anchors.txt` rows (same commit as the code; 4 TAB-separated fields; the file is `rows=180` today)
Proposed (final strings depend on the chosen method/constant names; re-measure with `grep -cF -e`):
```
# Phase 41 (v1.7.0_gg7): V2 inserts carry a per-insert socket_timeout unless the operator named it (TMO-01); the 0 row pins that the client-level alternative stays out.
src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java	chc.setInsertSocketTimeoutV2(insertSettings, 	3	TMO-01
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	settings.setNetworkTimeout(	1	TMO-01
src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java	clientBuilder.setSocketTimeout(	0	TMO-01
```
`rows=183` afterwards (comment lines are skipped by the mirror). **Unchanged and must stay:** `ClickHouseSinkConfig.java	//this.addClickHouseSetting("async_insert", "0", false);	1	CFG-02`. Existing writer rows that the three added lines could touch were checked: `createHelperClient(` 3, `closeHelperClient()` 3, `closeQuietly(chcTmp)` 1, `new ClickHouseHelperClient.ClickHouseClientBuilder(` 2, `.setCompressionMethod(csc.isSetCompressionMethod())` 2, `csc.getJsonInsertColumns()` 1: none of those patterns appears in the new lines. The helper's CLT-03 rows (`.addEndpoint(endpoint)` 1, `.addEndpoint(url)` 0, `clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true");` 1) are unaffected by extracting the parser **provided** the loop's `clientBuilder.setOption(kv[0], kv[1]);` line is the only change to that area; re-run the mirror after the edit.

### F-2 rollout item draft for Phase 44 (record in the evidence with these labels; RLS-03 requires `measured` / `read from the code` / `derived`)
| Claim | Label | Evidence |
|-------|-------|----------|
| On client V2 with `async=false`, an insert into a server that stops answering fails after `clickhouseClientInsertTimeoutMs` (default 4 min) with a retriable error; before, it blocked forever | measured | Probe 4 unfixed vs fixed; the PIN-01 red run |
| The failure is retriable under `errors.tolerance=none` and `all`; the task is not failed and no DLQ record is written | measured | TMO-02 |
| An insert that legitimately runs longer than the setting now fails and is redelivered; the server may still commit it | measured | Probe 8 (real ClickHouse 26.10.3.7: failed at 1003 ms, row landed) |
| With `exactlyOnce=false` the redelivery inserts the batch again (duplicate); with `exactlyOnce=true` the retry reuses the dedup token | duplicate: derived from Probe 8 plus `Processing`/`insertSettings.setDeduplicationToken` read from the code; token reuse: read from the code (`ClickHouseWriter.java:1341-1344`) | |
| An operator's `socket_timeout` in `jdbcConnectionProperties` keeps its value (it bounds every request, inserts included) | measured | override pin; Probe 4 |
| Only `client_version` other than `V1` is affected; the default is V1 | read from the code | `ClickHouseSinkConfig.java:380`, `ClickHouseWriter.java:100` |
| A write-side stall is not bounded | measured | Probe 6 |
| V1 inserts: no connector timeout; the V1 library's own 30 s `socket_timeout` ended a read-side stall at 30.1 s | measured | Probe 4 V1, Probe 5 |
| Before this phase an operator could already bound F-2 with `socket_timeout=<ms>` in `jdbcConnectionProperties` | measured | Probe D3/D4 |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `awaitInsertResponse` `future.get(timeout)` as the only bound | Per-insert socket timeout bounds the inline call; `awaitInsertResponse` stays for `async=true` | This phase | `awaitInsertResponse` is unchanged (also unreachable-by-design with `async=false`); do not delete it |
| 0.9.6 failure wrapper text `Insert failed (Attempt: 1/4 - Duration: PT0S)` (still named in `ScriptedWriteFailureWriter.V2_INSERT_FAILED_MESSAGE`) | 0.11.0 timeout surfaces as `DataTransferException > SocketTimeoutException` directly from `client.insert` | client 0.11.0 | New pins assert on the root cause, not on message text |

**Deprecated/outdated:** the published statement "four-minute insert timeout has no effect while `async=false`" (`docs/ROLLOUT-v1.7.0_gg6.md:272`, `CHANGELOG.md:83`) stays as published history; the correction belongs in the gg7 note (Phase 44), not an edit of published text.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | all Gradle runs | yes | `java -version`: OpenJDK 21.0.12.1 (Homebrew); Gradle toolchain via `-PjavaVersion=21` | — |
| Gradle wrapper | build/test | yes | runs offline (probe init script worked with `--offline`) | — |
| Docker (colima profile `vz`) | full suite on `latest` x V2, real-server evidence | yes | colima `vz` Running, 8 CPU / 16 GiB; `docker ps` answers; default profile is Stopped | `testUnit` needs no Docker (run with `DOCKER_HOST=unix:///nonexistent/docker.sock`) |
| ClickHouse image `clickhouse/clickhouse-server:latest` | full suite, Probe 8 | yes | 26.10.3.7 (the digest Phase 40 recorded was `sha256:739e099a…`; re-record it, `latest` drifts) | — |
| python3 | evidence helper (`python3 -I`) | yes | — | — |
| `yq` | AGENTS.md matrix lookup | not checked | — | not needed: the user fixed the target to `latest` x V2 |

**Missing dependencies with no fallback:** none. **Missing with fallback:** none.

## Security Domain

`security_enforcement` is enabled (absent = enabled; config shows `true`, ASVS level 1, block on `high`).

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | credentials untouched |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes (minor) | the operator-key parser handles an untrusted-ish config string: tolerate `null`, empty, leading `?`, `=` inside values, empty keys, same rules as `createClientV2`; no exception on malformed pairs |
| V6 Cryptography | no | — |
| V7 Error Handling and Logging | yes | new WARN/INFO text, if any, must not echo `jdbcConnectionProperties` (it can hold credentials) or record data; log at most the effective timeout and the fact "operator socket_timeout kept" |
| V12 / availability | yes | the phase is itself an availability fix: a stalled server no longer pins the task thread past `max.poll.interval.ms` |

### Known Threat Patterns for this stack
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Server or network black hole blocks the consumer thread, forcing a rebalance | Denial of service | Per-insert socket read timeout (this phase); residual: write-side stall, V1 sites, non-insert queries (recorded) |
| Timed-out-but-landed insert redelivered | Tampering (duplicate data) | `exactlyOnce=true` dedup token; documented risk with `exactlyOnce=false` |
| Secret leakage through a new log line | Information disclosure | never log the jdbc string, password, or batch contents |
| Stall test leaving a live listener/thread | Denial of service (CI) | release the latch, close stub, close writer in `finally`; loopback `127.0.0.1` only (the stub already binds loopback) |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | An operator explicitly writing `socket_timeout=0` in `jdbcConnectionProperties` means "keep unbounded"; the writer should honour it (skip the per-insert value) rather than treat 0 as "unset" | Pattern 1 | Low. Reads "an operator's `socket_timeout` keeps that value" literally. If the user wants 0 treated as unset, the flag becomes "value != 0" |
| A2 | With an operator-set `async=true` in `jdbcConnectionProperties` both bounds apply (the per-insert socket timeout also ends the worker thread) and nothing breaks | Pattern 1 | Low; not probed. A one-case probe or an explicit "not covered" note settles it |
| A3 | With `exactlyOnce=true` the retry of a timed-out-but-landed insert is deduplicated only if the target engine deduplicates (`replicated_deduplication_window` / `non_replicated_deduplication_window`) | Pitfall 5, rollout item | Medium for the rollout wording; not probed in this phase (Phase 44's note should label it `derived` or measure it) |
| A4 | `Integer.MAX_VALUE` ms clamp is needed hygiene (Probe F showed no conversion error for 5e9 ms, so the necessity is unproven, only the ambiguity) | Pattern 1 | Negligible; the clamp is harmless |
| A5 | A server that streams progress headers mid-insert would reset the inactivity timer. The client sends `wait_end_of_query=1&send_progress_in_http_headers=1`; Probe 8 shows a quiet 2 s insert trips a 1 s timeout, so the default case is as designed | Pitfall 5 | Low; only matters for an operator-supplied exotic setting |
| A6 | No other test reads `ClickHouseHelperClient.java`/`ClickHouseWriter.java` as source text and counts tokens that the parser extraction could change | Pattern 1 | Low; `grep -rln` for those file names in `src/test` returned no match this session, only the anchors file and CI scripts read them |

## Open Questions (RESOLVED)

1. **Does a per-insert `socket_timeout` through `InsertSettings` reach the socket on client 0.11.0 on the insert path? (ROADMAP research flag 1)** — RESOLVED: **yes**. `InsertSettings.setNetworkTimeout(long, ChronoUnit)` (equivalently `setOption("socket_timeout", Integer|Long)`) ended a stalled `client.insert` in 503 ms (Probes B/C/C2), through the real writer in 304-432 ms on all three routes (Probe 4), against a real ClickHouse 26.10.3.7 at 1003 ms (Probe 8). One POST, no client retry, wire unchanged. **No user stop.**

2. **Which V2 insert sites exist?** — RESOLVED: exactly three, `doInsertRawBinaryV2`, `doInsertJsonV2`, `doInsertStringV2` in `ClickHouseWriter` (table above). `KeeperStateProvider` uses `client.query` for state writes, not `Client.insert`.

3. **How does an operator's `socket_timeout` reach the client today, and does it survive the fix? (flag 3)** — RESOLVED: `jdbcConnectionProperties` -> `createClientV2` loop -> `clientBuilder.setOption` = client-level, unbounded-by-default requests then bounded (Probes D3/D4, `NetworkCompressionPinUnitTest:79-83`). **A per-insert value overrides it** (D1), so the writer must skip the per-insert option when the operator named the key. Design settled (helper flag computed once; explicit `0` honoured, assumption A1).

4. **How can `StubClickHouseServer` get a stall responder?** — RESOLVED: a latch-owning `stalling()` factory; `close()` releases before `server.stop(0)` (Probe 3 proved the hang otherwise); make the stub `public` so a test in package `sink` can reach it.

5. **How does the stalled insert classify through `ProxySinkTask.put` under `errors.tolerance` none/all?** — RESOLVED: retriable under both, no DLQ, task not failed (Probe 4, JSON/String/RowBinary). **No stop; RSK-02 not needed for this shape.**

6. **The behaviour change with the default timeout. (flag 2)** — RESOLVED as a documentation item, not a decision: TMO-01 takes the existing setting as the bound. Evidence: Probe 8. Rollout item draft above, with duplicate risk (`exactlyOnce=false`) and the two operator remedies (raise `clickhouseClientInsertTimeoutMs`, or set `socket_timeout`).

7. **Does the fix reach default deployments?** — RESOLVED, **new finding to carry into the rollout item**: no. `client_version` absent means V1 (`ClickHouseSinkConfig.java:380`), and V1 inserts use `future.get()` with no connector timeout (but the V1 library's own 30 s read bound applied in Probe 4 V1).

8. **TMO-03 wording for V1.** — RESOLVED: record "three V1 `future.get()` sites have no connector-level bound" **and** the measured 30 s library bound for a read-side stall; correct Known cost 14's "library's own timeout options were not examined". No V1 test is added (a 30 s test is not worth the suite time; evidence only).

9. **Needs a user decision? ** — **None blocking.** Two low-risk interpretations to confirm in passing (the plan can proceed on the recommendations and surface them at the checkpoint): (a) A1, explicit `socket_timeout=0` from the operator is honoured as "unbounded"; (b) the optional extra FORK-DELTA sentence that DESCRIBE/version/refresh/Keeper queries remain unbounded (documentation only, not in the ROADMAP's two limits).

## Sources

### Primary (HIGH confidence)
- Probes run this session on the Gradle test runtime classpath (client-v2 0.11.0, httpclient5 5.6.5, JUnit 5.14.4), JDK 21; real ClickHouse `26.10.3.7` in colima profile `vz` (Probe 8).
- `javap` on `com.clickhouse.client.api.insert.InsertSettings`, `com.clickhouse.client.api.internal.CommonSettings`, `com.clickhouse.client.api.ClientConfigProperties`, `Client$Builder` (client-v2-0.11.0.jar).
- Repo source read this session: `ClickHouseWriter.java` (insert sites, `awaitInsertResponse`, `getMutationRequest`), `ClickHouseHelperClient.java` (`createClientV2`, constructor, `setReplicaTagHeaderV2`), `ClickHouseSinkConfig.java` (constants, insert timeout ConfigDef, `CLIENT_VERSION` default), `ClickHouseSinkConnector.java:39`, `ProxySinkTask.java`, `Processing.java`, `Utils.java`, `ClickHouseSinkTask.java`, `StubClickHouseServer.java`, `CountingHelperClient.java`, `WriterHelperClientClosingUnitTest.java`, `WrappedDataExceptionClassificationPinUnitTest.java`, `NetworkCompressionPinUnitTest.java`, `ClientTimeoutPrefixPinUnitTest.java`, `ScriptedWriteFailureWriter.java`.
- `git show upstream/main:.../ClickHouseWriter.java` (upstream has no per-insert timeout).
- `.planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md` (gate and red-run recipes), `.planning/research/{STACK,ARCHITECTURE,PITFALLS,SUMMARY}.md` (milestone research; its P1-P4 probes are reproduced and extended above).

### Secondary (MEDIUM confidence)
- `.planning/research/STACK.md` F-2 table (client source read at tag `v0.11.0` by the milestone research); consistent with every probe here.

### Tertiary (LOW confidence)
- None used as a basis for a recommendation; see the Assumptions Log.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new library; versions read from the resolved classpath.
- Architecture: HIGH, the whole fixed path was run end to end (scratch copy of the two production files, real writer, `ProxySinkTask.put`, three routes, both tolerances, operator cases) and one real-server run.
- Pitfalls: HIGH for the stub release order, override precedence, V1 default and the duplicate/landing behaviour (all measured); MEDIUM for exactly-once dedup on non-replicated engines (A3).

**Research date:** 2026-11-02
**Valid until:** 2026-11-30 (client pinned at 0.11.0 for the milestone; re-run Probes B/D1 if the client version changes, as PIN-03 does for `custom_*`)
