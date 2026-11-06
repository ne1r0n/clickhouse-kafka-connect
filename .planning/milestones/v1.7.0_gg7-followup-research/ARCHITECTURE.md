# Architecture Research

**Domain:** Kafka Connect sink connector (ClickHouse), maintenance milestone `v1.7.0_gg7`: follow-up debt F-1, F-2, F-3, `Processing.doLogic` wrapped-`DataException`, FUT-05, OI-35-1..3, IN-04
**Researched:** 2026-11-01
**Confidence:** HIGH for the lifecycle, timeout and classification findings (read from `src/main` at the working tip, plus four probes run against the built classes and the client 0.11.0 jar). MEDIUM for Kafka Connect's behaviour when `start()` throws, and for `Client.close()` idempotency (not probed).

This is not a new design. Every item is a small, targeted change to an existing component. The useful output is: where each resource is created, who is supposed to release it, where each timeout is (and is not) applied, and how a thrown exception becomes retriable or fatal.

## Standard Architecture

### System Overview (resource ownership, the part this milestone touches)

```
Kafka Connect worker thread (put / preCommit / close / stop are called on one task thread)
┌──────────────────────────────────────────────────────────────────────────────┐
│ ClickHouseSinkTask                                                            │
│   start(): new ProxySinkTask(...) ; proxySinkTask.start() ; createDelivery... │
│   stop():  deliveryStrategy.stop() ; proxySinkTask.stop()                     │
├──────────────────────────────────────────────────────────────────────────────┤
│ ProxySinkTask  (ctor runs BEFORE start())                                     │
│   stateProvider = exactlyOnce ? new KeeperStateProvider(csc)  <-- CLIENT #2   │
│                               : new InMemoryState()                           │
│   dbWriter      = new ClickHouseWriter(statistics)                            │
│   stop(): dbWriter.stop() ; statistics.unregisterMBean()   (stateProvider     │
│           is never released)                                                  │
│   put(): Processing.doLogic -> catch -> Utils.handleException                 │
├──────────────────────────────────────────────────────────────────────────────┤
│ Processing.doLogic -> doInsert -> dbWriter.doInsert                           │
│   doInsert wraps EVERY failure in RuntimeException(queryId, cause)            │
├──────────────────────────────────────────────────────────────────────────────┤
│ ClickHouseWriter                                                              │
│   start(): chc = new ClickHouseHelperClient(...)     <-- CLIENT #1 (long-lived)│
│            ping, version, updateMapping, startBackgroundTableSync             │
│   scheduledExecutor "clickhouse-table-sync" -> TableMappingRefresher          │
│            -> updateMapping() -> chc.extractTablesMapping()  (uses CLIENT #1) │
│   V2 insert: chc.getClient().insert(...) -> awaitInsertResponse(future)       │
│   V1 insert: getMutationRequest() builds a throwaway helper client            │
│                                                       <-- CLIENT #3 (per batch)│
│   stop(): shuts the executor down; chc is NOT closed                          │
├──────────────────────────────────────────────────────────────────────────────┤
│ ClickHouseHelperClient implements AutoCloseable                               │
│   ctor ALWAYS builds a V2 Client (HTTP pool) and a V1 ClickHouseNode, even    │
│   with clientVersion=V1 ("We are creating two clients")                       │
│   close() closes the V2 Client only. Already correct; nobody calls it in main.│
└──────────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities (relevant to the milestone)

| Component | Today | Needed |
|-----------|-------|--------|
| `ClickHouseHelperClient` (`sink/db/helper`) | Owns one V2 `Client` (and a V1 node). `close()` exists and works. Builds the V2 client in `createClientV2()` with no socket, connect or execution timeout from the connector's config. `timeout` (`timeoutSeconds`) is used only as the ping timeout. | One new builder field for the socket timeout (F-2). No change to `close()`. |
| `ClickHouseWriter.start/stop` (`sink/db`) | `start()` creates `chc` inline (line 87) and returns `false` on three failure paths with the client already built and never released. `stop()` shuts the table-sync executor down and stops there. | `stop()` closes `chc` after the executor is down (F-1). The failure returns release it too. A `protected` factory method for the helper client so a test can count closes. |
| `ClickHouseWriter.getMutationRequest` (V1 insert path) | Builds a full `ClickHouseHelperClient` per batch only to call `getServer()` on it, and never closes it. That builds a V2 `Client` and HTTP pool per batch. | Close it (try-with-resources). The `ClickHouseNode` it returns is a plain object and stays valid after `close()`. |
| `KeeperStateProvider` (high risk) | `KeeperStateProvider(csc)` builds its own helper client (line 50) and never closes it. Constructed in the `ProxySinkTask` constructor, before `start()`. | `close()` that releases the client only when this instance created it. The state logic is not touched. |
| `StateProvider` (interface) | No lifecycle method. | `default void close() {}` so `InMemoryState` and every test implementation compile unchanged. |
| `ProxySinkTask.stop` | `dbWriter.stop(); statistics.unregisterMBean();` | Try/finally: writer, state provider, then MBean, so a failing `stop` cannot leave the MBean registered. |
| `Utils.handleException` | Classifies by (1) ClickHouse server error code on the first server exception in the chain, (2) `e instanceof DataException`, (3) deepest cause being `SocketTimeoutException`, `UnknownHostException`, `SocketException` (opt-in), or an `IOException` with the client's "Read/Write timed out after" prefix. No `RetriableException` pass-through. | Decide the `RetriableException` pass-through (F-2, see Finding 4). Otherwise tests only. |
| `Processing.doLogic` (high risk) | Line 137 reads `record.getSinkRecord().topic()` (the transformed topic) into a local named `originalTopic`. | OI-35-1 one-line change to `originalTopic()`, behind a user decision. |

## Findings (verified in this research)

### Finding 1: F-1 is three leaks, not one (HIGH)

`36-FOLLOWUPS.md` names `ClickHouseWriter.stop()`. A grep of `src/main` for every construction of `ClickHouseHelperClient` finds three, and `close()` is called on none of them:

| # | Site | Lifetime | Leak rate | Affects |
|---|------|----------|-----------|---------|
| 1 | `ClickHouseWriter.start`, `ClickHouseWriter.java:87` | task | one V2 client per task start | every mode |
| 2 | `KeeperStateProvider(csc)`, `KeeperStateProvider.java:50` | task | one V2 client per task start | `exactlyOnce=true` only; not in `36-FOLLOWUPS.md` |
| 3 | `ClickHouseWriter.getMutationRequest`, `ClickHouseWriter.java:1786` | one insert batch | one V2 client per batch | `clientVersion=V1` only; not in `36-FOLLOWUPS.md` |

Site 3 matters most for scale. It is per batch, so a long-running V1 connector leaks one HTTP pool per insert. The helper client's constructor builds a V2 `Client` even when V1 is selected (`ClickHouseHelperClient.java:104-106`), so the V1 path pays the V2 cost on every batch. Upstream `upstream/main` (`9bed6f79`) has the same three sites and no `chc.close`, so these are fork-side fixes with no upstream change to rebase onto. Open an upstream PR only if the user wants one; do not wait for it.

The V1 `ClickHouseClient` objects are fine: `pingV1`, `versionV1`, `showTablesV1` and the insert paths close theirs (try-with-resources or explicit close).

### Finding 2: where each timeout is applied, and where it is not (HIGH)

- `timeoutSeconds` (`csc.getTimeout()`, default 30 s) reaches `ClickHouseHelperClient.timeout` and is used only as the argument of `client.ping(timeout)` and the V1 ping. It is not a socket or request timeout.
- `createClientV2()` sets no socket, connect or execution timeout. The client's defaults apply: `socket_timeout` = 0 (infinite), `async` = false.
- `awaitInsertResponse` (`ClickHouseWriter.java:1773`) calls `future.get(clickhouseClientInsertTimeoutMs)`. With `async=false` the V2 client runs the whole insert inline inside `client.insert(...)` (`Client.runAsyncOperation` calls `supplier.get()` and returns `CompletableFuture.completedFuture(...)`). The call blocks before `awaitInsertResponse` is entered, so the guard is unreachable. Exceptions also propagate directly from `client.insert(...)`, not through `ExecutionException`.
- All three V2 insert sites pass `client.insert(...)` as the argument of `awaitInsertResponse`: `doInsertRawBinaryV2:1324`, `doInsertJsonV2:1599` (via a variable), `doInsertStringV2:1746`.
- `clickhouseClientInsertTimeoutMs` is already validated against `consumer.override.max.poll.interval.ms` in `ClickHouseSinkConfig` (lines 428-437), so the operator contract "the insert is bounded by this value" already exists in the config docs. The fix honours it; it adds no config.

**Probes (run 2026-11-01 against `build/classes/java/main` and client-v2 0.11.0, stalled loopback server that accepts and never answers):**

| Probe | Result |
|-------|--------|
| P1: `socket_timeout` unset, 2-byte body | Blocked until killed at 25 s. Reproduces F-2 on the real client. |
| P2: `?socket_timeout=1500` through `jdbcConnectionProperties` | Failed after 1,524 ms, one TCP accept (the client does not retry it), exception chain `DataTransferException > SocketTimeoutException`, `Utils.handleException` classified it as `RetriableException`. |
| P3: `socket_timeout=1500`, 64 MiB body, peer holds the socket open and never reads | Blocked more than 40 s. Java's `SO_TIMEOUT` is a read timeout only. A peer that stops reading is bounded only by TCP's own timers. |

Consequences:

1. An operator can already work around F-2 today with `jdbcConnectionProperties=socket_timeout=<ms>` (the helper forwards `k=v` pairs through `setOption`). Say so in the rollout note.
2. The connector fix is a socket timeout on the writer's V2 client, driven by the existing `clickhouseClientInsertTimeoutMs`. It needs no change in `Utils.handleException`, because the root cause arrives as `SocketTimeoutException` (P2).
3. It bounds the read side only. The write-side stall (P3) stays unbounded and must be recorded as a known limit, not claimed as fixed. Bounding it needs a watchdog that tears the client down, which is new design and out of scope.
4. The timeout applies to every request on that client: `DESCRIBE`, `SELECT version()`, and the background table refresh as well as inserts. At the 4-minute default this is harmless, and it is an operator-visible change worth one line in the rollout note.

### Finding 3: failure wrapping hides the timeout guard and the `DataException` (HIGH, probed)

`Processing.doInsert` wraps every writer failure: `throw new RuntimeException(queryId.toString(), e)` (lines 72 and 96). `ProxySinkTask.put` then calls `Utils.handleException(e, ...)` on that wrapper, and `ChunkFlusher.putDirect` calls it a second time on whatever `put` rethrew.

`Utils.handleException` has no rule that looks for a `RetriableException` or finds a `DataException` in the chain. `getRootCause(e, true)` returns the first server exception, or the deepest cause. Probe P4, run on the built classes:

| Input to `handleException` (`errorsTolerance=false`) | Result |
|-----------------------------------------------------|--------|
| `RuntimeException(q, RetriableException("insert timed out", TimeoutException))` (what `awaitInsertResponse` produces after `Processing` wraps it) | `RuntimeException`, not retriable |
| the bare `RetriableException("insert timed out", TimeoutException)` | `RuntimeException`, not retriable |
| `RuntimeException(q, DataException)` (what `Processing` produces for a writer `DataException`) | `RuntimeException` |

So:

- **`awaitInsertResponse`'s `RetriableException` never reaches Connect as retriable.** Its Javadoc says it does. It is reachable only when an operator sets `async=true` through `jdbcConnectionProperties`, and no test exercises it (only `NewUpstreamConfigDefaultsUnitTest` mentions the config name). This is a latent bug on a rarely used path. Recommendation: add a `RetriableException`-in-chain pass-through to `Utils.handleException`, placed after the server-error block, and pin it. It is small, it matches the documented intent, and it also makes the double-handling in `ChunkFlusher` idempotent. It is a user decision because `Utils.java` is upstream code and the change reclassifies a failure from fatal to redelivered.
- **The line-106 branch `e instanceof DataException && !errorsTolerance` is unreachable on the `Processing` path.** It can only fire for a bare `DataException`. This is the "`Processing.doLogic` wrapped-`DataException` case". With `errors.tolerance=none` the task fails either way, so the visible difference is the exception class in the task status. `34-03-SUMMARY.md` records the two remaining unpinned cases: a `DataException` whose chain holds a retriable server error is retriable today, and one whose root cause is `SocketTimeoutException` or `UnknownHostException` is retriable today under `errors.tolerance=all`. Recommendation: **pin, do not change.** The root-cause-first rule makes both retriable, which is the safer failure mode, and `Processing.java` is a high-risk file. Fixing it would mean unwrapping in `Processing` or searching the chain in `Utils`, and neither is a genuine bug fix.

### Finding 4: shutdown ordering inside `ClickHouseWriter.stop()` (MEDIUM)

The table refresher is the only concurrent user of `chc`: `TableMappingRefresher.run` calls `chWriter.updateMapping`, which calls `chc.extractTablesMapping`. It catches `Exception` and logs. `stop()` calls `shutdownNow()` and waits 5 s; a V2 request blocked in a socket read is not interruptible, so that wait can time out (it only logs). Therefore:

1. Shut the executor down first (unchanged).
2. Then close `chc`. Closing releases a blocked refresher, which then throws into the refresher's own `catch (Exception)`. `updateMapping` resets `isUpdateMappingRunning` in a `finally`.
3. Make it idempotent: `ClickHouseSinkTask.stop` reaches the writer on a failed start, and Connect may call `stop` after a failed `start`. Null the reference in a `finally` (check test usage of `getServer()` after `stop()` first), and do not assume `Client.close()` is idempotent.
4. Also close in the three `return false` branches of `start()` (ping failed, version unreadable or too old, no tables). That makes the fix independent of whether Connect calls `stop()` after a throwing `start()`. Connect's `WorkerSinkTask.close()` does call `task.stop()` as far as I recall, but that is from memory, so do not rely on it.

### Finding 5: `KeeperStateProvider` is built before `start()` (MEDIUM)

`ProxySinkTask`'s constructor creates the provider, so its client exists from construction, and `ProxySinkTask.stop()` is the only place that can release it. If the `KeeperStateProvider` constructor itself throws after building its client (`init()` swallows V2 errors but not all V1 ones), nothing holds a reference to close. That residue is small; note it, do not add constructor try/catch machinery for it.

Do not share one helper client between the writer and the provider as the fix. The two builders differ: the writer's carries `targetTableFilter`, `sslSocketSni`, `clusterClause` and replica pinning; the provider's carries none of them. The existing `KeeperStateProvider(ClickHouseHelperClient)` constructor leaves `csc` null and would throw in `init()` (it reads `csc.getZkDatabase()`), so it is a test-only path and must stay untouched.

## Recommended Change Set (new vs modified)

### Modified, production

| File | Change | Risk |
|------|--------|------|
| `sink/db/ClickHouseWriter.java` (high risk) | `stop()` closes `chc` after the executor; `start()` failure returns close it; `getMutationRequest` closes `chcTmp`; new `protected ClickHouseHelperClient createHelperClient(...)` factory used by `start()`; `start()` passes `csc.getClickhouseClientInsertTimeoutMs()` to the builder. No change to the binary-format paths. | High file, small diff. Full suite plus a diff-shape check that the type-mapping code is byte-identical. |
| `sink/db/helper/ClickHouseHelperClient.java` | `ClickHouseClientBuilder` gets `setSocketTimeoutMs(long)`; `createClientV2()` applies `clientBuilder.setSocketTimeout(ms, ChronoUnit.MILLIS)` before the `jdbcConnectionProperties` loop, so an operator's `socket_timeout` still wins (same pattern as `IGNORE_UNKNOWN_CONFIG_KEY`). Unset or 0 leaves the client default, which keeps `KeeperStateProvider`'s client and every test builder unchanged. | Low. Needs fork-anchor rows. |
| `sink/state/provider/KeeperStateProvider.java` (high risk) | `close()` releasing the client it created (an `ownsClient` flag, false for the test constructor). | Consistency-critical file, but the change is outside `getStateRecord`/`setStateRecord`. Require the existing state-machine tests green and no diff in those two methods. |
| `sink/state/StateProvider.java` | `default void close() {}`. | Low. |
| `sink/ProxySinkTask.java` | `stop()` as try/finally over writer, provider, MBean. | Low. |
| `util/Utils.java` | Optional, user decision: `RetriableException` pass-through after the server-error block. | Medium: reclassifies a failure; upstream file. |
| `sink/processing/Processing.java` (high risk) | Optional, user decision (OI-35-1): line 137 `getSinkRecord().topic()` to `getSinkRecord().originalTopic()`. Phase 35's P6b measured `ProcessingTest` staying green. Only the Direct path with `reportInsertedOffsets=true` under a renaming SMT changes (it commits nothing today). | High file, one line. Own commit and own full-suite gate. |

### New, tests and fixtures only

| Item | Purpose |
|------|---------|
| `StubClickHouseServer.stall()` responder (additive, in `src/test/.../db/helper/`) | A responder that holds the exchange until `close()`. Reuses the existing Docker-free `com.sun.net.httpserver` stub, so F-2 needs no Docker. |
| F-2 pin: read-stall bounded and retriable | Writer-level test: stalled stub, `clickhouseClientInsertTimeoutMs` of a few hundred ms, assert a `RetriableException` within a bound and one request at the stub. Mirrors P2. Add a characterization test of today's behaviour first (red: blocks), using a `@Timeout`. |
| F-1 pin: close counted over start/stop | A `ClickHouseWriter` subclass overrides `createHelperClient` to return a subclass of `ClickHouseHelperClient` that counts `close()` calls (`CountingHelperClient` is the existing pattern). Cover: start then stop = 1 close; failed start = 1 close; stop twice = no exception and still 1 close; V1 insert path closes `chcTmp`. |
| F-1 pin for `KeeperStateProvider` | A Docker-free test that `ProxySinkTask.stop()` releases the provider. |
| IN-04 pin: `custom_` forwarding on 0.11.0 | `StubClickHouseServer` captures `rawUri()` and headers. Same shape as `ClientEndpointAndOptionsPinUnitTest.insertOnce`, which already uses it. Pure test, no production change. |
| `Utils` / `ProxySinkTask` pins for the wrapped cases | The two unpinned cases from `34-03-SUMMARY.md` plus the `awaitInsertResponse`-through-`Processing` case (today's result: `RuntimeException`). Flip the last one if the pass-through is approved. |
| F-3 harness fix (`src/integrationTest` / `ConfluentPlatform`) | Count only the named connector's own task status in the remaining helpers; test code only. |

### Not changed, on purpose

- **`ClickHouseSinkConfig`.** No new config. F-2 reuses `clickhouseClientInsertTimeoutMs`; FUT-05 is documentation. This keeps the high-risk config file byte-identical to upstream and avoids ConfigDef drift.
- **`TableMappingRefresher`.** It needs no change. Its lifecycle is owned by the writer's executor, which `stop()` already shuts down.
- **Client V1 timeouts.** V1 `future.get()` calls (lines 1423, 1549, 1704) stay unbounded. V1 is opt-in legacy. State this in FORK-DELTA and the rollout note rather than widening the milestone.
- **The Keeper client's timeout.** Applying the socket timeout to `KeeperStateProvider`'s client would bound state writes inside the exactly-once critical section. It is safe in principle (BEFORE/AFTER retry reconstructs the same block) but it edits a consistency-critical file beyond a `close()`. Record it as a follow-up, not part of this milestone.
- **FUT-05 as code.** A retry budget needs per-task state that `Utils.handleException` (a static, stateless function) does not have. That is a new feature and is out of scope. Do FUT-05 as: name the ID in `docs/ROLLOUT-v1.7.0_gg6.md`'s successor note, add it to FORK-DELTA, and keep the existing pins (an added code raises `RetriableException`, bypasses `errors.tolerance` and the DLQ, and has no budget).
- **OI-35-2 and OI-35-3.** Fixing a many-to-one rename or a partition-changing SMT means changing the state key, which `35-SECURITY.md` says stops for a user decision. Recommend documentation (an injective rename is required for exactly-once) and no code.

## Data Flow Changes

### Insert and failure path (after the change)

```
put(records)
  -> Processing.doLogic -> doInsert -> ClickHouseWriter.doInsert
       -> client.insert(...)            [V2 client now has socket_timeout = insertTimeoutMs]
            |-- server stalls -> SocketTimeoutException after the bound   (was: blocks forever)
            '-- DataTransferException > SocketTimeoutException
       <- wrapped by Processing.doInsert: RuntimeException(queryId, cause)
  -> ProxySinkTask.put catch -> Utils.handleException
       root cause SocketTimeoutException -> RetriableException        (existing rule, unchanged)
  -> Connect redelivers the batch
       exactly-once: same offsets + dedup token -> ClickHouse drops a duplicate
       at-least-once (exactlyOnce=false): a batch that landed before the timeout is inserted again
```

The last line is the operator-facing cost of F-2 and belongs in the rollout note: with `exactlyOnce=false` a timed-out insert that actually landed becomes a duplicate on redelivery. The existing 4-minute design already accepted this; the fix makes it real.

### Task lifecycle (after the change)

```
ClickHouseSinkTask.stop()
  -> deliveryStrategy.stop()
  -> ProxySinkTask.stop()
       try { dbWriter.stop() }                 executor shutdown (<= 5 s) -> chc.close()
       finally { stateProvider.close() }       KeeperStateProvider closes its own client, InMemoryState no-op
       finally { statistics.unregisterMBean() }
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: mocking `ClickHouseHelperClient` with Mockito inline mocks
**What people do:** `mock(ClickHouseHelperClient.class)` to count `close()` calls.
**Why it's wrong:** `CountingHelperClient` documents that inline-mocking this class crashed the Semeru/OpenJ9 test JVM natively (HARN-08). The project memory records an intermittent OpenJ9 GPF with exit 255.
**Do this instead:** subclass it, as `CountingHelperClient` does. Add the `createHelperClient` factory seam so the subclass can be injected.

### Anti-Pattern 2: fixing F-2 by flipping `async=true`
**What people do:** make the client asynchronous so `future.get(timeout)` becomes effective.
**Why it's wrong:** the insert body would be serialised on a common-pool thread instead of the task thread, and `CompletableFuture.cancel(true)` does not interrupt a blocked socket write. The timed-out thread leaks. It also leaves the `RetriableException` classification gap of Finding 3.
**Do this instead:** a socket timeout on the client. Keep `awaitInsertResponse` as the guard for operators who set `async=true` themselves.

### Anti-Pattern 3: sharing one helper client across writer and Keeper provider
**What people do:** pass the writer's `chc` into `KeeperStateProvider` to halve the leak.
**Why it's wrong:** the builders differ (table filter, SNI, cluster clause, replica pinning), and the injected-client constructor is already half-broken (`csc` is null).
**Do this instead:** keep two clients and close both.

### Anti-Pattern 4: changing `Processing.doInsert`'s `RuntimeException` wrap
**What people do:** unwrap or rethrow typed exceptions in `Processing` to fix classification.
**Why it's wrong:** it changes what `ChunkFlusher`, DLQ stack traces and `statistics.recordTopicStatsOnFailure` see, in the file with the exactly-once invariant.
**Do this instead:** classify in `Utils.handleException`, which already walks the chain.

## Integration Points

### External services

| Service | Integration pattern | Notes |
|---------|---------------------|-------|
| ClickHouse HTTP (V2 client 0.11.0) | `Client.Builder.setSocketTimeout(long, ChronoUnit)`, option key `socket_timeout`, default 0. `setExecutionTimeout` maps to the server-side `max_execution_time` and is not a client bound. | P2 shows no client retry on a socket timeout (one accept). Re-run P2 after any client bump. |
| Kafka Connect runtime | `put`, `preCommit`, `close` and `stop` on the task thread. `RetriableException` is redelivered with no budget. | `stop()` after a failed `start()` is believed but not verified, so close inside `start()` failure paths too. |
| GitHub Actions / Dependabot | PR #22 gson 2.14.0 to 2.15.0; PR #21 `setup-gradle` 6.4.0 to 6.4.1 (SHA re-pin behind the pin guard). | Independent of the code changes. |

### Internal boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `ProxySinkTask` to `StateProvider` | New `close()` | Default method, so `InMemoryState` and test fixtures are unaffected. |
| `ClickHouseWriter` to `ClickHouseHelperClient` | Factory method plus builder field | Test seam and timeout wiring share the one `start()` edit. |
| `ClickHouseWriter` to `Utils.handleException` | Exceptions through `Processing`'s wrapper | Keep the `SocketTimeoutException` root-cause rule untouched. |
| Fork anchors | `.planning/scripts/fork-anchors.txt` (path, fixed string, expected count, backs-ID) | A change to an anchored line must update the file in the same commit. Add rows for the new builder call, the `close()` calls and, if approved, the pass-through. `verify-merge-invariants.sh` evaluates the file at every commit in range. |

## Suggested Build Order

Dependencies decide the order. The test-only items have none; F-2 needs the factory seam from F-1; the optional high-risk edits go last so a revert is cheap.

1. **Dependency absorption (PR #22, then #21).** One commit each, each with a suite gate, so later failures are not confounded with a dependency change. This repeats the "dependencies first" shape of `v1.7.0_gg4`.
2. **Test-only items, in parallel, no production risk.** IN-04 `custom_` pin; F-3 harness; the Docker-free `Utils`/`ProxySinkTask` pins for the wrapped cases; the `StubClickHouseServer.stall()` responder. The characterization tests (F-2 blocking, `awaitInsertResponse` through `Processing` is not retriable) are written red-or-pinning here, before any fix.
3. **F-1, writer half.** `createHelperClient` seam, `stop()` close, start-failure closes, `chcTmp` close, with the counting pins. Full suite.
4. **F-1, Keeper half.** `StateProvider.close()`, `KeeperStateProvider.close()`, `ProxySinkTask.stop()`. Separate commit and separate gate because `KeeperStateProvider` is high risk. Verify `getStateRecord`/`setStateRecord` are byte-identical.
5. **F-2.** Builder field, `start()` wiring, stalled-stub pin flipped to green. Run P1/P2/P3 as the measured evidence, and record P3 as the known write-side limit. Then, if the user approves, the `Utils` pass-through with its pin flipped.
6. **OI-35-1 (`Processing` line 137), only on user approval.** Own commit and gate. OI-35-2/3 and FUT-05 are documentation.
7. **Documents.** The gg1 half of WR-08, FORK-DELTA lines 6, 139 and 286 to past tense, FUT-05 named by ID, the Phase 37 dispositions, and the `increment-version.yml` header comment.
8. **Release.** `VERSION=v1.7.0_gg7`, CHANGELOG, FORK-DELTA rows, anchors, rollout note with independent review, `--no-ff` merge after human approval, CI green on the published SHA.

**Gate note from project memory:** a full `./gradlew test` can crash the default JDK 17 test JVM in OpenJ9/Mockito; the `-PjavaVersion=21` escape and deleting the core dump are recorded there. Plan two green runs per high-risk change with that in mind.

## Operator-Visible Effects to Carry into the Rollout Note

| Change | Effect |
|--------|--------|
| F-1 | No behaviour change; fewer open HTTP pools and sockets on workers that restart or rebalance tasks often. A V1 connector stops leaking one pool per batch. |
| F-2 | A V2 insert (and `DESCRIBE`, version and table-refresh requests) with no server response now fails after `clickhouseClientInsertTimeoutMs` with a retriable `SocketTimeoutException`, instead of hanging. With `exactlyOnce=false` the redelivery can duplicate a batch that landed. Write-side stalls are not bounded. V1 is unchanged. `jdbcConnectionProperties=socket_timeout=<ms>` still overrides it. |
| `Utils` pass-through (if approved) | An `async=true` insert timeout becomes retriable as its Javadoc says. |
| OI-35-1 (if approved) | `reportInsertedOffsets=true` with a renaming SMT starts committing offsets. |
| FUT-05 | A code in `additionalRetriableErrorCodes` bypasses `errors.tolerance` and the DLQ and has no retry budget. List only transient codes. |

## Open Questions and Gaps

- `Client.close()` idempotency in 0.11.0 was not probed. The plan guards for it either way.
- Whether `WorkerSinkTask` calls `stop()` after a throwing `start()` is from memory. Closing inside the writer's failure paths removes the dependency.
- P3 shows the write-side stall is unbounded. Whether the production worker's TCP settings make it moot (keepalive, `tcp_retries2`) is not measured here.
- Whether any existing test calls `ClickHouseWriter.getServer()` or inserts after `stop()` (it would break if the reference is nulled) was not checked.
- Four decisions belong to the user, and each is isolated in its own commit: the `Utils` pass-through, OI-35-1, bounding the Keeper client, and whether to send the three fixes upstream.

## Sources

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` lines 80-193 (`start`, `updateMapping`, `stop`), 1280-1331, 1560-1605, 1773-1819 (`awaitInsertResponse`, `getMutationRequest`), 1868-1887 (refresher) (HIGH, source)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` lines 85-107, 165-223, 706-711 (HIGH, source)
- `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java`, `sink/ProxySinkTask.java`, `sink/ClickHouseSinkTask.java`, `sink/ChunkFlusher.java`, `util/Utils.java`, `sink/processing/Processing.java`, `sink/state/BaseStateProviderImpl.java` (HIGH, source)
- `.planning/milestones/v1.7.0_gg6-maintenance-phases/36-client-0-11-0-audit-bump/36-FOLLOWUPS.md` (F-1..F-3 as filed)
- `.planning/milestones/v1.7.0_gg6-maintenance-phases/34-upstream-tail-merge/34-03-SUMMARY.md` and `34-COMMAND-EVIDENCE.md` E-10 (the DataException interpretation and the FUT-05 item)
- `.planning/milestones/v1.7.0_gg6-maintenance-phases/35-buffered-offset-commit-under-topic-renaming-smts/35-BUF03-RECORD.md`, `35-SECURITY.md`, `35-02-SUMMARY.md` (OI-35-1..3)
- Client 0.11.0 jar `client-v2-0.11.0.jar`, read with `javap`: `ClientConfigProperties` keys `socket_timeout` (default 0), `async` (default false), `max_execution_time`; `Client$Builder.setSocketTimeout`, `setExecutionTimeout`, `useAsyncRequests`; `Client.runAsyncOperation` (inline `supplier.get()` plus `completedFuture` when `async` is false) (HIGH, bytecode)
- Probes P1 to P4 (above), run against `build/classes/java/main` with scratch programs in `/tmp/probe_h` (HIGH for the observed results; the build directory could lag the source by a commit, but `Utils.java` was read at the tip and has no `RetriableException` rule)
- `upstream/main` (`9bed6f79`): `ClickHouseWriter.java` and `KeeperStateProvider.java` contain no `close` of the helper client, and `ClickHouseWriter` has the same `chcTmp` site (HIGH, git)
- `src/test/.../db/CountingHelperClient.java`, `db/helper/StubClickHouseServer.java`, `ClientEndpointAndOptionsPinUnitTest.java`, `ClientTimeoutPrefixPinUnitTest.java` (existing test seams to reuse)

---
*Architecture research for: ClickHouse Kafka Connect `custom_build` fork, `v1.7.0_gg7` follow-up debt*
*Researched: 2026-11-01*
