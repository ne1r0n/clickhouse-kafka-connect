# Phase 40: Helper Client Closing - Research

**Researched:** 2026-11-01
**Domain:** Resource lifecycle (HTTP client pools) in a Kafka Connect sink task: `ClickHouseWriter`, `KeeperStateProvider`, `ProxySinkTask`
**Confidence:** HIGH (every research-flag question was settled from code read this session, a runnable probe, or the Connect worker source)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md `## Decisions` holds only the line below; the discuss step was skipped by setting.

### Claude's Discretion
"All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions."

### Deferred Ideas (OUT OF SCOPE)
"None — discuss phase skipped."

### Binding scope taken from the ROADMAP and the orchestrator (treat as locked)
- Three sites, no more by default: the writer's long-lived `chc`, the throwaway client built per batch in `getMutationRequest` (V1 path), and the client `KeeperStateProvider` creates in exactly-once mode.
- The writer half and the Keeper half are two separate commits, each behind its own gate. The Keeper half leaves the diff of `getStateRecord` and `setStateRecord` empty.
- The field keeps the name `chc`. New tests use no Mockito, `mockStatic` or `mockConstruction` (OpenJ9 crash). Tests are only added, never weakened or deleted.
- A `src/main` change outside the declared scope is a user-owned gate (ROADMAP "User-Owned Gates", Phases 40-43). See Open Question OQ-1.
- Never run `spotlessApply` over the whole tree (46 pre-existing violations). Use per-file scoped checks.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CLS-01 | `ClickHouseWriter.stop()` closes `chc` after the table-sync executor stops, idempotently and null-safe; `chc` is also closed on the failure paths of `start()`. | `closeHelperClient()` after the executor block; `start()` wrapper closing on every non-success exit (6 exits enumerated in "start() exits"). `Client.close()` is idempotent on 0.11.0 (probe). |
| CLS-02 | The V1 `getMutationRequest` path closes its throwaway helper client on every batch; the returned node stays usable. | One definition serves all 4 call sites; `try`-with-resources around `getServer()`; `ClickHouseNode` is a plain value that survives `close()` (probe) and serves a real V1 insert against `StubClickHouseServer` (probe). |
| CLS-03 | `StateProvider` gets a default `close()`, `KeeperStateProvider` overrides it and closes only a client it created, and `ProxySinkTask.stop()` calls it in `try/finally` together with the writer and MBean. `getStateRecord` and `setStateRecord` are unchanged. | `ownedClient` field set only in the `csc` constructor path; nested `try/finally` in `ProxySinkTask.stop()`; no new `ProxySinkTask` constructor (TAL-02 pin). |
| CLS-04 | A counting seam (`CountingHelperClient`, no Mockito) pins all three sites on V1 and V2, including a failed `start()` and a double `stop()`. | `createHelperClient(builder)` factory in the writer, a factory-taking package-private `KeeperStateProvider` constructor, extended `CountingHelperClient`. Case matrix below. |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` (imports `AGENTS.md`) and `./.claude/CLAUDE.md`:
- `ClickHouseWriter.java` and `KeeperStateProvider.java` are high-risk files: extra care, thorough tests, `BEFORE`/`AFTER` flag sequence untouched. Do not change batch composition after `BEFORE`.
- Do not use `InMemoryState` in production paths; do not store connector state in ClickHouse tables (`KeeperMap` only).
- Java 17, Lombok for boilerplate, SLF4J (INFO lifecycle, DEBUG batch, TRACE per record), Google Java Style via Spotless, constants extracted to `static final` strings, comments only for non-obvious invariants, never summarise the logic in English.
- Never log secrets or record data. Use try-with-resources for every `AutoCloseable`.
- Hot paths (`ProxySinkTask`, `ClickHouseWriter`): avoid per-record allocation. This phase adds none; see Pitfall 9 for the per-batch cost that stays.
- Tests: JUnit 5; read `ClickHouseTestHelpers.java` before writing tests; only add tests; descriptive names; negative tests for error handling; live tests read data back and `assertEquals` against the originals.
- Do NOT commit without being asked (orchestrator says: do not commit in this research step). The GSD workflow requires work to enter through a GSD command.
- Behaviour preservation (`.claude/CLAUDE.md`): code changes are limited to genuine bug fixes; changes additive to keep upstream rebases cheap; Docker (colima) must run for ClickHouse-backed tests.
- No project skills exist (`.claude/skills` etc. absent) [VERIFIED: CLAUDE.md "Project Skills" section].

## Summary

F-1 is three leaks, all in the fork and all also in `upstream/main`, so each fix is a fork-only delta. A `ClickHouseHelperClient` owns one V2 `Client` (its HTTP connection pool) and a V1 `ClickHouseNode` value. The constructor builds the V2 client for every helper, even for `client_version=V1` [VERIFIED: ClickHouseHelperClient.java:104-106]. Only `close()` releases it, and nothing calls it today. The production default of `client_version` is **V1** [VERIFIED: ClickHouseSinkConfig.java:380], so the per-batch `chcTmp` leak in `getMutationRequest` is the hot path of every default-configured connector, not an edge case. One V2 client is built and dropped per insert batch there.

All four research-flag questions are settled (see "Research Flag Answers"). `Client.close()` is idempotent on 0.11.0 and a ping after close returns `false` instead of throwing (probe, this session). Kafka Connect does call `SinkTask.stop()` after a `start()` that throws, in both 3.9 (the production worker line) and 4.1, so `stop()` must tolerate a writer whose failed `start()` already closed and nulled `chc`. No test uses a writer after `stop()`. The three tests that reflect on `chc` keep working because the field name and type do not change.

The seam needed for CLS-04 does not exist yet (`createHelperClient` is new), so "red on the unchanged tree" means: declarations and seams present, behaviour absent. That tree is a behaviour-neutral refactor, run red in the working tree, recorded, then fixed. Two existing tests constrain the design: `ProxySinkTaskConstructorUnitTest` pins exactly three `ProxySinkTask` constructors, and `SourcePlanningIdGuardUnitTest` fails on any planning identifier (including `F-1`) in `src/main`.

**Primary recommendation:** Writer commit: add `protected createHelperClient(builder)`, a private null-safe `closeHelperClient()` called after the executor block in `stop()` and from a thin `start()` wrapper on every non-success exit, and a `try`-with-resources around `chcTmp.getServer()`. Keeper commit: `default void close() {}` on `StateProvider`, an `ownedClient`-guarded `close()` on `KeeperStateProvider` with a factory-taking package-private constructor, and a nested `try/finally` in `ProxySinkTask.stop()` with no new constructor. Record every red result before the first fix.

## Architectural Responsibility Map

All capabilities live in one tier, the connector process inside the Kafka Connect worker (API / Backend tier). The ClickHouse server and Keeper are external dependencies, not owners.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Release the writer's long-lived HTTP pool | Connector task (`ClickHouseWriter.stop`) | Connect worker (calls `SinkTask.stop`) | The writer built the client, so it closes it; the worker only triggers the lifecycle. |
| Release the per-batch V1 helper | Connector task (`getMutationRequest`) | — | Created and consumed inside one method; close at the point of last use. |
| Release the Keeper state client | State provider (`KeeperStateProvider.close`) | `ProxySinkTask.stop` (orchestrates) | Provider owns the client; `ProxySinkTask` owns the provider lifecycle. |
| Stop ordering and failure isolation | `ProxySinkTask.stop` | — | Only place that sees writer, MBean and provider together. |
| Exactly-once state read/write | `KeeperStateProvider` (unchanged) | ClickHouse Keeper | Must stay byte-identical. |

## Standard Stack

No new library, plugin or tool. Everything is already on the classpath. [VERIFIED: ClickHouseHelperClient.java:46 `public class ClickHouseHelperClient implements AutoCloseable {`; `close()` at :706-711]

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:client-v2` | 0.11.0 | `Client.close()` releases the pool | Already pinned; idempotent close verified by probe |
| `com.clickhouse:clickhouse-client` (V1) | 0.11.0 | `ClickHouseNode` value, per-call `ClickHouseClient` (already closed by try-with-resources) | Already pinned |
| JUnit Jupiter | 5.11.3 | New tests | Existing |

[VERIFIED: probe classpath listing: `client-v2-0.11.0.jar`, `clickhouse-client-0.11.0.jar`, `httpclient5-5.6.5.jar`]

### Supporting (test only, all existing)
| Item | Purpose | When to Use |
|------|---------|-------------|
| `CountingHelperClient` (`src/test/.../sink/db`) | Real `ClickHouseHelperClient` subclass; extend additively to count `close()` | Every counting pin |
| `StubClickHouseServer` (`src/test/.../sink/db/helper`, package-private) | In-JVM HTTP stub for the V1 per-batch pin | Only tests in package `...sink.db.helper` can use it |
| `InMemoryDBWriter`, `InMemoryState`, `InMemoryDLQ` | Seams for `ProxySinkTask.stop()` pin | `ProxySinkTask` test |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Count via the factory seam | Stub-socket observation (research STACK.md) | Needs no production seam but cannot see `chcTmp`, the Keeper client or a failed `start()`; rejected |
| `Mockito.spy` on the helper | — | Forbidden (OpenJ9 `retransformClasses` crash); one inline spy already exists at `ClickHouseWriterTest` line 444 |
| Reuse `chc.getServer()` instead of building `chcTmp` | Upstream PR #861 `initializeOnlyClient()` | Removes the per-batch V2 construction but changes behaviour and conflicts with #861; out of scope (Pitfall 9) |

**Installation:** none.

**Version verification:** no package is added or changed, so no registry check applies. `client-v2` 0.11.0 was read from the resolved test classpath this session.

## Package Legitimacy Audit

No external package is installed or upgraded in this phase. The audit table is empty by design: **Packages removed (SLOP): none. Packages flagged (SUS): none.** The legitimacy seam was not run because there is nothing to check. AGENTS.md forbids dependency moves without asking.

## Research Flag Answers (all four RESOLVED)

### RF-1: Is `Client.close()` on client 0.11.0 idempotent? RESOLVED: yes

Probe (scratch dir `/tmp/p40probe`, outside the repo; the project's test classpath; JDK 21): build a `ClickHouseHelperClient` against `127.0.0.1:8123`, then `close()` three times, read `getServer()` after close, and call `getClient().ping(1000)` after close. [VERIFIED: probe run this session]

```text
first close ok
second close ok (idempotent)
node after close: ClickHouseNode [uri=http://127.0.0.1:8123/default, options={network_compression_method=lz4,compress=false}]@805931038
ping after close -> false
third close ok
```

Consequences: a double close cannot throw, a refresher thread that races `stop()` gets `false` from a ping instead of an exception, and `getServer()` stays usable after close. The null guard and the "closes once" counting pins do not depend on this (a counting seam counts calls, not effects), and `closeHelperClient()` still nulls the field first so the second `stop()` is a no-op. The probe ran on HotSpot 21; the library code is the same on OpenJ9 17 (no JVM-specific path), tagged [ASSUMED] for OpenJ9 (A1).

### RF-2: Does any existing test use the writer after `stop()`? RESOLVED: no

Evidence [VERIFIED: grep of `src/test` and `src/integrationTest` this session]:
- Direct `ClickHouseWriter.stop()` calls exist in exactly three places: `ClickHouseWriterTest.java` lines 134, 385, 513 (`runWithWriter` and two `finally` blocks, each followed only by `ClickHouseTestHelpers.dropTable(chc, topic)` on the test's own client) and `WriterErrorPathLiveTest.java:314` (`writer.stop()` then `task.stop()`; the task's own `ProxySinkTask.dbWriter` is the original unstarted writer, so no double stop on the same instance).
- A grep for `stop()` followed within six lines by `put`/`start`/`doInsert`/`getServer`/`updateMapping` returns only restarts that build a new task (`ClickHouseSinkTaskBufferTest.maybeRestart` creates `new ClickHouseSinkTask()`; `ClickHouseCloudTest` calls `start(props)` which builds a new `ProxySinkTask` and writer).
- `ScriptedWriteFailureWriter.installInto(task)` swaps `Processing.dbWriter` only; `ProxySinkTask.dbWriter` stays the original, never-started `ClickHouseWriter` whose `chc` is null, so `task.stop()` in `WriterFailureClassificationUnitTest` and `ValueFreeDlqHeadersUnitTest` is a null-guarded no-op after the fix, and the injected `CountingHelperClient` is still closed only by its own try-with-resources.

### RF-3: Which three tests reflect on `chc`? RESOLVED

String constant `"chc"` is used by exactly these three [VERIFIED: grep]:
1. `ClickHouseWriterTest.java:691` `private static final String WRITER_HELPER_CLIENT_FIELD = "chc";` (read at :699, written at :703, used with a `Mockito.spy` at :444-451).
2. `ClickHouseWriterNullMessageUnitTest.java:34` `private static final String WRITER_HELPER_CLIENT_FIELD = "chc";` (`installHelperClient` sets it at :111-117).
3. `ScriptedWriteFailureWriter.java:42` `private static final String HELPER_CLIENT_FIELD = "chc";` (`setWriterField` at :77-81; the same helper also writes `"csc"`).

Keep the name `chc`, keep it non-final and of type `ClickHouseHelperClient`. `WriterErrorPathLiveTest` reflects on other fields only (`readField`/`writeField` with `proxySinkTask`, `processing`, `dbWriter`). The `csc` field is reflected by six more classes; leave it too.

### RF-4: What does Connect do after a `start()` that throws? RESOLVED: it calls `stop()`

`WorkerTask.doRun()` calls `doStart()` then `execute()` inside `try`, and `doClose()` in `finally`; `doClose()` calls `close()`, and `WorkerSinkTask.close()` calls `task.stop()` (warning, not failing, if it throws). [CITED: raw.githubusercontent.com/apache/kafka/4.1/.../WorkerTask.java and .../3.9/.../WorkerTask.java doRun(), WorkerSinkTask.java close()] Production's worker is Confluent 7.9.7, which is Apache Kafka 3.9 [CITED: docs/FORK-DELTA.md Phase 25 row "connect-api-7.9.7-ccs.jar ... (Apache Kafka 3.9)"].

Consequences, both required:
- `stop()` after a failed `start()` is the normal case, not a corner: it must tolerate `chc == null` (already closed by the failure path). That is why the failure path nulls the field.
- The orchestrator rule still holds: close inside the writer's own failure paths and do not rely on `stop()`. Two failure shapes exist where `stop()` cannot help: `new ProxySinkTask(...)` throwing in `ClickHouseSinkTask.start()` leaves `proxySinkTask == null`, so `ClickHouseSinkTask.stop()` skips it entirely (Keeper-constructor failure, see OQ-1), and `ClickHouseHelperClient`'s own constructor throwing midway (residual R-2).

## Architecture Patterns

### System Architecture Diagram

```text
Kafka Connect worker thread
  |  start(props)                               stop()  (also runs after a start() that threw)
  v                                               |
ClickHouseSinkTask --new--> ProxySinkTask         v
  |                          | ctor: exactlyOnce ? KeeperStateProvider(csc)      <-- SITE 3: owns helper #K
  |                          |                 : InMemoryState
  |                          | ctor: new ClickHouseWriter(statistics)
  |  proxySinkTask.start()   v
  |   registerMBean(); dbWriter.start(csc) ----> ClickHouseWriter.start
  |                                               | chc = createHelperClient(builder)   <-- SITE 1: helper #W
  |                                               | ping / version / updateMapping / table-sync executor
  |                                               | any non-success exit --> closeHelperClient()  [new]
  |  put(records) -> Processing.doLogic -> ClickHouseWriter.doInsert*
  |                    client_version=V1 (production DEFAULT): per batch, 4 call sites
  |                      getMutationRequest -> createHelperClient(builder for `database`)
  |                                              <-- SITE 2: helper #T per batch
  |                                              getServer(); close() immediately [new]
  v
ProxySinkTask.stop()   [new shape, nested try/finally]
  1. dbWriter.stop():  shutdownNow executor, awaitTermination(5 s)  ->  closeHelperClient()  [new]
  2. statistics.unregisterMBean()
  3. stateProvider.close():  KeeperStateProvider closes ownedClient only  [new]
```

### Recommended Project Structure (files touched)

```text
src/main/java/com/clickhouse/kafka/connect/sink/
├── db/ClickHouseWriter.java                      # writer commit
├── state/StateProvider.java                      # Keeper commit (default close)
├── state/provider/KeeperStateProvider.java       # Keeper commit
└── ProxySinkTask.java                            # Keeper commit (stop() only)
src/test/java/com/clickhouse/kafka/connect/sink/
├── db/CountingHelperClient.java                  # additive extension (writer commit)
├── db/WriterHelperClientClosingUnitTest.java     # new, package sink.db, no network
├── db/helper/WriterV1BatchClientClosingUnitTest.java  # new, package sink.db.helper (needs the package-private stub)
├── state/provider/KeeperStateProviderClosingUnitTest.java  # new (Keeper commit)
└── ProxySinkTaskStopUnitTest.java                # new (Keeper commit)
docs/FORK-DELTA.md, .planning/scripts/fork-anchors.txt   # rows in the same commit as the code
```

### Pattern 1: writer half (skeleton, compiled in a scratch copy of `src/main`)

The skeleton below was applied to a copy of `src/main/java`, compiled with `javac` against the project's resolved classpath (110 classes, no error), and measured against the anchor rows. [VERIFIED: scratch compile and `grep -cF` this session]

```java
// start(): thin wrapper, the existing body is renamed startInternal (diff stays small and additive)
@Override
public boolean start(ClickHouseSinkConfig csc) {
    boolean started = false;
    try {
        started = startInternal(csc);
        return started;
    } finally {
        if (!started) {
            closeHelperClient();
        }
    }
}

// inside startInternal: first and last line of the builder chain change, the chain itself does not
chc = createHelperClient(new ClickHouseHelperClient.ClickHouseClientBuilder(csc.getHostname(), csc.getPort(), csc.getProxyType(), csc.getProxyHost(), csc.getProxyPort())
        .setDatabase(csc.getDatabase())
        /* ... unchanged chain ... */
        .setCompressionMethod(csc.isSetCompressionMethod()));

// stop(): the executor block is unchanged; one call is appended after it
        closeHelperClient();
    }

protected ClickHouseHelperClient createHelperClient(ClickHouseHelperClient.ClickHouseClientBuilder builder) {
    return builder.build();
}

private void closeHelperClient() {
    ClickHouseHelperClient client = chc;
    chc = null;
    if (client == null) {
        return;
    }
    try {
        client.close();
    } catch (Exception e) {
        LOGGER.warn("Failed to close ClickHouse helper client", e);
    }
}

// getMutationRequest: one definition serves all four call sites
ClickHouseNode server;
try (ClickHouseHelperClient chcTmp = createHelperClient(new ClickHouseHelperClient.ClickHouseClientBuilder(/* unchanged */)
        .setDatabase(database)
        /* ... unchanged chain ... */
        .setCompressionMethod(csc.isSetCompressionMethod()))) {
    server = chcTmp.getServer();
}
```

`start()` exits that leave a live client behind today, all covered by the wrapper [VERIFIED: ClickHouseWriter.java:103-106, 112-115, 119-122, 123-126, 134-139, plus a throw from `updateMapping` at :133]:
1. `!chc.ping()` returns `false`.
2. Fewer than two version parts returns `false`.
3. Server older than 23.3 returns `false`.
4. `chc.version()` or `Integer.parseInt` throws, caught, returns `false`.
5. `mapping.isEmpty()` without db-topic split returns `false` (before the executor starts).
6. `updateMapping` throws (for example `extractTablesMapping` fails) and the exception escapes `start()`. This one is easy to miss: the wrapper closes in `finally` and the exception still propagates unchanged.

The three success exits (JSON skip at :131, and the final `return true`) keep the client open until `stop()`.

`stop()` ordering is already correct for the executor: `shutdownNow()` then `awaitTermination(5 s)` inside a `try` that catches `Exception` and nulls the executor in `finally`. If the refresher is blocked in a socket read the wait only logs, so closing next makes its call fail into `TableMappingRefresher.run`'s `catch (Exception e)`; `updateMapping` resets `isUpdateMappingRunning` in its own `finally`. [VERIFIED: ClickHouseWriter.java:156-176, 179-193; TableMappingRefresher.java run()]

### Pattern 2: Keeper half (skeleton, same scratch compile)

```java
// StateProvider.java: no abstract method added, so no implementer (InMemoryState, BaseStateProviderImpl) changes
default void close() {}

// KeeperStateProvider.java: the csc-only constructor delegates; the body of the old constructor becomes the 2-arg one
public KeeperStateProvider(ClickHouseSinkConfig csc) {
    this(csc, ClickHouseHelperClient.ClickHouseClientBuilder::build);
}

KeeperStateProvider(ClickHouseSinkConfig csc,
        Function<ClickHouseHelperClient.ClickHouseClientBuilder, ClickHouseHelperClient> helperClientFactory) {
    /* ... existing body ... */
    chc = helperClientFactory.apply(new ClickHouseHelperClient.ClickHouseClientBuilder(hostname, port, csc.getProxyType(), csc.getProxyHost(), csc.getProxyPort())
            /* ... unchanged chain ... */
            .setCompressionMethod(csc.isSetCompressionMethod()));
    ownedClient = chc;
    /* ... ping / init unchanged ... */
}

@Override
public void close() {
    ClickHouseHelperClient client = ownedClient;
    ownedClient = null;
    if (client != null) {
        client.close();
    }
}

// ProxySinkTask.java: stop() only, no constructor change
public void stop() {
    try {
        dbWriter.stop();
    } finally {
        try {
            statistics.unregisterMBean();
        } finally {
            stateProvider.close();
        }
    }
}
```

- `ownedClient` is assigned only in the `csc` path. The injected constructor `KeeperStateProvider(ClickHouseHelperClient chc)` never assigns it, so `close()` can never close an injected client. `chc` itself is never nulled, so `getStateRecord` and `setStateRecord` stay byte-identical and still usable after `close()`.
- A nested `finally` propagates the last exception thrown (a later step masks an earlier one). The roadmap asks only that every step runs. If masking matters, record the first exception and `addSuppressed` the rest; that is a few lines more and stays additive. Planner's call; the simple form meets the success criterion.
- Reads still work after close (`getStateRecord` uses a per-call V1 `ClickHouseClient` and `chc.getServer()`); only the V2 write path (`queryV2`) needs the closed client. Nothing writes state after `stop()`.

### Pattern 3: the counting seam and the case matrix

Extend `CountingHelperClient` additively (existing constructor `CountingHelperClient(boolean useClientV2)` and its four current users stay unchanged; the class is `final` and holds no static state):
- `closeCalls()` incremented in an overridden `close()` that then calls `super.close()`.
- A constructor taking a real `ClickHouseClientBuilder` (what the factory receives), so the helper points at the real target (a `StubClickHouseServer` port or the Docker server). `isUseClientV2()` must then answer from the builder when no flag was forced (the current override returns the constructor flag; make the flag nullable).
- Scripted overrides for the no-network cases: `ping()`, `version()`, `extractTablesMapping(...)`, and for the Keeper V1/V2 `init()` path `queryV2` (throw: the existing `catch (Exception e)` logs and continues) and `queryV1` (return a `java.lang.reflect.Proxy` of `ClickHouseResponse` whose `close()` is a no-op). All are public non-final methods [VERIFIED: ClickHouseHelperClient.java:225, 269, 313, 339, 602].
- `close()` also records whether a thread named `clickhouse-table-sync` was alive at that instant, which pins "after the executor has stopped" without timing.

Factories hold the created helpers in an instance list (writer subclass field, or a lambda-captured list): no static state, so counts cannot leak between tests.

| # | Site | Case | V1 / V2 | Expected on the red tree | Expected after the fix |
|---|------|------|---------|--------------------------|------------------------|
| W1 | writer | `start()` succeeds then `stop()` | both | closes 0 (red) | 1 |
| W2 | writer | `stop()` twice | both | closes 0 (red) | still 1, no throw |
| W3 | writer | `stop()` without `start()` | both | 0 created, no throw (green control) | same |
| W4 | writer | ping false | both | 0 (red) | 1, `start()` returns false, later `stop()` adds none |
| W5 | writer | version has fewer than two parts | V2 | 0 (red) | 1 |
| W6 | writer | version older than 23.3 | V2 | 0 (red) | 1 |
| W7 | writer | `version()` throws | V2 | 0 (red) | 1 |
| W8 | writer | no tables, no db-topic split | V2 | 0 (red) | 1, no executor thread |
| W9 | writer | `extractTablesMapping` throws | V2 | 0 (red) | 1 and the exception escapes unchanged |
| W10 | writer | success with `tableRefreshInterval=1`, then `stop()` | V2 | never closed (red) | 1; no `clickhouse-table-sync` thread alive at the close |
| W11 | `chcTmp` | two batches through the V1 insert against `StubClickHouseServer` | V1 | each `chcTmp` closes 0 (red) | each closes exactly once; the stub saw both inserts with the expected bodies |
| W12 | `chcTmp` | negative control: V2 batch | V2 | no extra helper (green control) | same |
| K1 | Keeper | `new KeeperStateProvider(csc, factory)` then `close()` | both | 0 (red) | 1 |
| K2 | Keeper | `close()` twice | both | 0 (red) | still 1 |
| K3 | Keeper | injected-client constructor | V2 | tripwire, see below | same |
| P1 | `ProxySinkTask` | `dbWriter.stop()` throws: MBean still unregistered, provider still closed | n/a | MBean not unregistered, no `close` (red) | all three ran, the writer's exception reaches the caller |
| P2 | `ProxySinkTask` | `statistics.unregisterMBean()` path throws is hard to script; instead, `stateProvider.close()` runs once per `stop()` call | n/a | 0 (red) | once per call |

Notes for the table:
- W11 uses the verified recipe: a writer subclass in package `com.clickhouse.kafka.connect.sink.db.helper`, `csc` and `chc` set by the reflection idiom the existing tests use, a schemaless `Map` record, and `doInsertJsonV1(records, table, queryId)` (protected). The probe produced one `POST /?...query_id=q1&...insert_deduplication_token=t-0-0-0...` carrying `INSERT INTO `t` FORMAT JSONEachRow` and `{"id":1}`. [VERIFIED: probe run this session] This is the Docker-free proof that "the returned node still serves the insert".
- K3: `KeeperStateProvider(ClickHouseHelperClient chc)` leaves `csc` null and `init()` dereferences `csc.getZkDatabase()`, so the constructor throws `NullPointerException` and no instance with an injected client can exist [VERIFIED: KeeperStateProvider.java:70-75 and :77-81]. The "leaves an injected client open" rule is therefore satisfied by construction. The pin: `assertThrows(NullPointerException.class, ...)` and `closeCalls() == 0` on the injected helper, labelled a tripwire (if someone repairs that constructor, the pin tells them to add a real injected-client case). Leave the constructor untouched (research ARCHITECTURE.md says the same).
- P1/P2 need no new constructor: use the package-private `ProxySinkTask(csc, errorReporter, DBWriter)` seam with a `DBWriter` whose `stop()` throws, and replace the `private final StateProvider stateProvider` by reflection with an `InMemoryState` subclass that counts `close()`. `Field.set` on a `private final` instance field works after `setAccessible(true)` [VERIFIED: probe `FinalProbe`: `modifiers=private final`, `swapped=true`, `stop()` ok]. Do not add a constructor: see Pitfall 1.
- The test config must set `client_version` explicitly. The Docker-free `ClickHouseSinkConfig(new HashMap<>(props))` defaults to V1, and the key is `client_version` (`ClickHouseSinkConnector.CLIENT_VERSION`) [VERIFIED: ClickHouseSinkConnector.java:39, ClickHouseSinkConfig.java:380].
- Tests extend nothing from `ClickHouseBase` (its `@Tag("clickhouse")` would drop them out of `testUnit`), and every `ProxySinkTask` that runs in a Docker-free test calls `stop()` in `finally` (JMX MBean leak sweep).

Recommended extra live pins (Claude's discretion, in the full suite only): one `ClickHouseBase` test that runs real V1 batches against Docker ClickHouse through a `ClickHouseWriter` subclass whose `createHelperClient` returns a `CountingHelperClient`, reads the rows back with `assertEquals`, and counts the `chcTmp` closes; and one Keeper round trip (`setStateRecord` then `getStateRecord` through a fresh provider, then `close()`, count 1). The full suite on `CLIENT_VERSION=V2` otherwise exercises the V1 insert path only in tests that force V1, so the stub pin carries most of the V1 protection.

### Anti-Patterns to Avoid
- **Closing whatever is in the field without nulling it:** a second `stop()` double-closes; tests that swap `chc` double-close.
- **Closing before the executor is down:** the refresher then throws on a closed client (it is caught, but noisy). Close last.
- **Sharing the writer's helper with the Keeper provider** to halve the leak: the builders differ (`targetTableFilter`, `sslSocketSni`, `clusterClause`, replica pinning on the writer only). Rejected in ARCHITECTURE.md Anti-Pattern 3.
- **Overridable call from a constructor** for the Keeper seam: a test subclass would run before its own fields exist. Use the factory argument.
- **Renaming, wrapping or making `final` the `chc` field.**

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Counting `close()` | A Mockito spy or `mockStatic` | Extend the real-subclass `CountingHelperClient` | OpenJ9 `retransformClasses` crash (Phase 18 deferred item, gg2, gg4, gg6 full suites) |
| Closing the per-batch helper | A pool or cache of helpers keyed by database | `try`-with-resources at the one definition | Cache invalidation, and per-database nodes change the insert target |
| Ordering of executor vs close | `sleep`-based timing in a test | A thread-alive check inside the counting `close()` | Deterministic, no wall-clock |
| Faking `ClickHouseResponse` | A hand-written 30-method implementation | `java.lang.reflect.Proxy` returning defaults | `close()` is the only method the Keeper V1 `init()` calls on it |
| Evidence tooling | A new checker | Byte copy of `39-evidence-check.py` as `40-evidence-check.py` (`cmp` it) | Phase 39 did the same from Phase 38; modes `junit`, `failed --expect`, `skips`, `anchors --rows N` already exist |

**Key insight:** the fix is lifecycle plumbing; the risk is in the seams and the gates, not in the close calls themselves.

## Common Pitfalls

### Pitfall 1: adding a `ProxySinkTask` constructor breaks an existing pin
**What goes wrong:** a convenience constructor taking a `StateProvider` makes `ProxySinkTaskConstructorUnitTest` fail.
**Why:** that test asserts the exact set of three signatures and "A two-argument constructor must stay gone" [VERIFIED: ProxySinkTaskConstructorUnitTest.java:82 `assertEquals(3, constructors.length);` and :90]. Anchor rows also pin `this(clickHouseSinkConfig, errorReporter, connectorName, null);` (1), `dbWriter != null ? dbWriter : new ClickHouseWriter(this.statistics)` (1) and `final ErrorReporter errorReporter) {` (0).
**How to avoid:** change `stop()` only; inject the provider by reflection in the test.
**Warning signs:** a diff in `ProxySinkTask.java` outside `stop()`.

### Pitfall 2: a planning identifier in `src/main` fails `testUnit`
**What goes wrong:** a comment or log line saying `F-1`, `CLS-01`, `Phase 40`, `RESEARCH` or a `.planning/` path in `src/main`.
**Why:** `SourcePlanningIdGuardUnitTest` scans every `.java` under `src/main/java`; its pattern is quoted: `"\\b(?:D|L|R|F|A|E|T|IN|WR|CR|RF)-\\d{1,3}\\b"` and `"\\bPhase \\d+"` and `"\\.planning/"` [VERIFIED: SourcePlanningIdGuardUnitTest.java:37-50]. `F-1` matches the first alternative.
**How to avoid:** write comments about the invariant ("the node is a value object; the client behind the helper is not needed once it is read"), never about the ticket. Add comments only where an invariant is not obvious, per AGENTS.md.

### Pitfall 3: new text moves an anchored count
**What goes wrong:** invariant 3 of the merge gate evaluates the anchors file at every commit in range, so a count that drifts fails the gate at that commit.
**Why:** counts are lines containing the fixed string. Measured on the skeleton: `new ClickHouseHelperClient.ClickHouseClientBuilder(` stays 2 (writer) and 1 (Keeper); `.setCompressionMethod(csc.isSetCompressionMethod())` stays 2 and 1; `.setTargetTableFilter(csc.getTargetTableFilter())` stays 1; `shouldSkipTableMetadata()` stays 8; `final String connectorName` stays 2. [VERIFIED: `grep -cF` on the scratch copies]
**How to avoid:** keep the builder chains untouched except the first and last line, never put an anchored string into a new comment or Javadoc, and run `python3 -I .planning/phases/40-.../40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows N` before each commit (currently `rows=172 bad=0` [VERIFIED: ran it this session]).

### Pitfall 4: the red tree cannot compile if it needs the fix's own API
**What goes wrong:** `provider.close()` does not exist on the unchanged tree, so the Keeper tests do not compile, and a compile error is not a recorded red.
**How to avoid:** the "red tree" is seams and declarations only: `createHelperClient` returning `builder.build()`, the factory constructor, and `default void close() {}` on `StateProvider` (the final design's no-op). `KeeperStateProvider.close()`, `closeHelperClient()` and the `ProxySinkTask.stop()` change are the fixes and stay out until the reds are recorded. Run the new classes with `testUnit --tests ...`, then `failed DIR --expect Class#method ...` to record exactly which cases failed.

### Pitfall 5: the Keeper seam in the writer commit
**What goes wrong:** the "writer half is one commit and passes its own gate before the Keeper half starts" clause is broken if Keeper edits are in the tree during gate A.
**How to avoid:** to satisfy "all reds before any fix" and "separate gates", record the Keeper reds early, then `git stash push -- <Keeper files>` (the seam in `KeeperStateProvider.java`, `StateProvider.java`, the two Keeper-side test classes) before running gate A. Simpler alternative if the planner reads criterion 1 per half: red-then-fix for the writer half, commit, then red-then-fix for the Keeper half. Recommend the strict reading with the stash (OQ-2 records the choice).

### Pitfall 6: the default `client_version` is V1
**What goes wrong:** a Docker-free test that forgets `client_version` runs the V1 path (`pingV1`, `versionV1`, per-batch `chcTmp`) and not V2. A test written for V2 then fails on a stub that does not answer `SELECT VERSION()`.
**How to avoid:** set `client_version` explicitly per case. Also note the full suite runs with `CLIENT_VERSION=V2` for tests built through `ClickHouseBase` only.

### Pitfall 7: `StubClickHouseServer` is package-private
**What goes wrong:** a test in `com.clickhouse.kafka.connect.sink.db` cannot see it.
**How to avoid:** put the one stub-backed class in `com.clickhouse.kafka.connect.sink.db.helper` (the writer's `protected createHelperClient` and the public `CountingHelperClient` are reachable from there). Do not widen the stub's visibility unless the planner wants to touch an existing helper.

### Pitfall 8: spotless baseline is not clean on three touched files
**What goes wrong:** running `spotlessApply` in write mode on `ClickHouseWriter.java`, `KeeperStateProvider.java` or `StateProvider.java` reorders imports and drags unrelated hunks into a high-risk file.
**Measured baseline at HEAD (scoped check, read mode):** `ClickHouseWriter.java` DIRTY (21 changed lines, imports only), `KeeperStateProvider.java` DIRTY (5, imports only), `StateProvider.java` DIRTY (5, imports only), `ProxySinkTask.java` CLEAN, `CountingHelperClient.java` CLEAN. [VERIFIED: ran the hook this session; "IS CLEAN" is printed on stderr, the formatted file on stdout]
**How to avoid:** command form `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > out 2> err`. For the three dirty files require that the formatted output differs from the file by the same import-only hunks and nothing else (new lines Spotless-clean, new imports placed so they add no hunk); for clean files and new files run write mode on that one file and require `IS CLEAN`.

### Pitfall 9: the V1 per-batch V2 construction stays
**What goes wrong:** claiming the leak is "gone" while every V1 batch still builds a V2 `Client` and its executors, then closes it.
**Why:** `ClickHouseHelperClient`'s constructor builds `createClientV2()` unconditionally, and `getMutationRequest`'s builder never calls `.useClientV2(...)`, whose default is `true` [VERIFIED: ClickHouseHelperClient.java:105 `this.client = createClientV2();`, :728 `private boolean useClientV2 = true;`]. The fix stops the leak; it does not remove the per-batch cost, which is pre-existing. Upstream PR #861 (open, 2026-10-26) adds `.useClientV2(false).initializeOnlyClient()` for exactly this helper and builds `new ClickHouseClientBuilder(csc)`, touching the same lines; it adds no close. [CITED: `gh pr diff 861 --repo ClickHouse/clickhouse-kafka-connect`] Record in FORK-DELTA: "superseded in part if #861 lands".
**How to avoid:** do not widen scope to reuse `chc.getServer()`; note it as a follow-up for the user.

### Pitfall 10: zsh and shell traps from Phase 39
`h` is a zsh alias for `history`: run multi-line helper blocks under `/bin/bash` with a sourced function. `${var}` before a colon or caret in zsh. There is no `timeout` binary on macOS (it silently failed in the first scratch Gradle call this session); poll in a loop. `TESTCONTAINERS_HOST_OVERRIDE` is set to a literal in this shell and breaks Ryuk: every Gradle run starts with `env -u TESTCONTAINERS_HOST_OVERRIDE`. Quote helper expressions that hold `?`, `~`, `^` or `>`.

### Pitfall 11: OpenJ9 crash and run overlap
The default local JDK is Semeru/OpenJ9 17 and the full suite can die natively in Mockito's inline bytecode generator (exit 255, ~8 GB core dump in the repo root). Phase 39 ran every gate with `-PjavaVersion=21` (HotSpot) and `./gradlew --stop` between runs. Do not overlap Gradle runs: the Phase 39 cumulative gate took 37-39 minutes because four runs overlapped under swap pressure. Delete any core dump, re-run once.

## Code Examples

### Counting factory in a writer test (no Mockito, no static state)

```java
// Source: pattern of existing CountingHelperClient users plus the skeleton above
final List<CountingHelperClient> created = new ArrayList<>();
ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0)) {
    @Override
    protected ClickHouseHelperClient createHelperClient(ClickHouseHelperClient.ClickHouseClientBuilder builder) {
        CountingHelperClient helper = new CountingHelperClient(builder).pingReturns(false);
        created.add(helper);
        return helper;
    }
};
assertFalse(writer.start(csc));
writer.stop();
writer.stop();
assertEquals(1, created.size());
assertEquals(1, created.get(0).closeCalls());
```
`pingReturns` and the `CountingHelperClient(builder)` constructor are the additions to the test double; the method names are a suggestion, not existing API.

### Keeper counting pin

```java
// Source: skeleton above; needs `queryV2` scripted to throw (init() catches Exception and logs)
List<CountingHelperClient> created = new ArrayList<>();
KeeperStateProvider provider = new KeeperStateProvider(csc, builder -> {
    CountingHelperClient helper = new CountingHelperClient(builder).pingReturns(true).queryV2Throws();
    created.add(helper);
    return helper;
});
provider.close();
provider.close();
assertEquals(1, created.get(0).closeCalls());
```

### Gate recipe (verbatim from the Phase 39 evidence, E-03; the planner adapts the log names)

```sh
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd40/exec/<name>.log 2>&1
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit build/test-results/testUnit tests>=1

/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
./gradlew --stop
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd40/exec/<name>.log 2>&1
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit build/test-results/test tests>=1
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows <172 + new rows>
```

Red evidence uses `... testUnit --rerun --tests '*WriterHelperClientClosingUnitTest' ...` then `failed build/test-results/testUnit --expect Class#method ...` with the red cases from the matrix. [CITED: 39-evidence-check.py docstring, `failed DIR [--expect Class#method ...]`]

## Gate Plan

Baseline after Phase 39 (the values the gates must reproduce, plus the new tests): `testUnit` 1687 tests, 2 skipped; full suite 2285 tests, 18 named skips, on ClickHouse `latest` x client V2. [CITED: 39-04-SUMMARY.md:122 "ran 1687 tests (E-07's total), 2 skipped; the full suite with `--rerun` ran 2285 tests ... the 18 named skips"] ClickHouse reported `26.10.3.7` with digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` at that gate [CITED: 39-COMMAND-EVIDENCE.md E-03]; `latest` can drift, so record `SELECT version()` before and after each full run and compare the digest. The new Docker-free tests add N cases to both totals; the skip count must stay 2 and 18, and the `skips` helper hard-codes the 18 named skips, so add no `@Disabled` and no `assumeTrue`.

1. **Evidence scaffolding:** `40-COMMAND-EVIDENCE.md` with the Phase 39 entry format (`### E-NN:` heading, `Backs:`, `sh` fence, `text` fence of `key: value` facts, UTC timestamp, `Verdict:`), `40-evidence-check.py` as a `cmp`-equal copy of the Phase 39 helper, scratch dir `/tmp/gsd40/exec`.
2. **Red run, writer half:** seams plus tests in the working tree, no fix. Record the failed set and the SHA-256 of `git diff` of the seam patch (the Phase 39 plan 04 pattern). Controls (W3, W12) must pass.
3. **Red run, Keeper half:** same for the Keeper tests (see Pitfall 5 for keeping it out of gate A).
4. **Writer commit (CLS-01, CLS-02):** fix, green class run, scoped Spotless, anchors mirror, `testUnit` with Docker unreachable, full suite on `latest` x V2, FORK-DELTA and anchors rows in the same commit. The cumulative merge-invariant script is not needed per half.
5. **Keeper commit (CLS-03):** fix, same gate plus a targeted run of the exactly-once classes first: `DedupTokenRetryLiveTest`, `KeeperStateProviderRestartLiveTest`, `ClickHouseSinkTaskBufferTest`, `ClickHouseSinkTaskWithSchemaTest`, `BufferedOffsetCommitUnitTest`, `ProcessingTest` [VERIFIED: grep for `exactlyOnce` and `new KeeperStateProvider` in `src/test`]. Add the diff check: `git diff <writer-commit> HEAD -- src/main/java/.../KeeperStateProvider.java` shows hunks only in the constructor header, the field, and `close()`; `getStateRecord` and `setStateRecord` appear in no hunk.
6. **Phase gate:** after both commits, the cumulative invariant script once on the final commit with an explicit SHA, run alone in the background with no commits meanwhile: `bash .planning/scripts/verify-merge-invariants.sh 54e63648 <final-sha> > /tmp/gsd40/exec/invariants-40.log 2>&1` (expect `result=PASS`, about 39 minutes). [CITED: 39-04-SUMMARY.md "The invariant gate took about 39 minutes"; E-03 command]

Cheap per-wave checks while developing: `... ./gradlew --console=plain testUnit -PjavaVersion=21 --tests '*ClosingUnitTest' --tests '*ProxySinkTaskStopUnitTest' --tests '*ProxySinkTaskConstructorUnitTest' --tests '*SourcePlanningIdGuardUnitTest'` (the last two are the existing guards this phase can break).

## FORK-DELTA and anchors rows

### docs/FORK-DELTA.md (table `Connector source`, columns `Behaviour | Where it lives | Why upstream does not provide it | Pinned by`)

Add two rows, ordered by file path then position (`sink/ProxySinkTask.java`, `sink/db/ClickHouseWriter.java`, `sink/state/...` sort as the file already does). Each lands in the commit of the code it describes.
- **Writer row (writer commit):** `ClickHouseWriter.stop()` closes `chc` after the table-sync executor stops, once and null-safe; `start()` closes it on every non-success exit; `getMutationRequest` closes its per-batch helper right after reading the node; all three build helpers through the protected `createHelperClient(builder)`. Where: `sink/db/ClickHouseWriter.java`. Why upstream does not: `upstream/main` has the same three sites and no close (research read `upstream/main` at `9bed6f79`); state "fork-only until upstream resolves it; partly superseded if upstream PR #861 lands (it removes the V2 construction of the per-batch helper but adds no close)". Pinned by: `CLS-01, CLS-02, CLS-04`, plus the new test class names.
- **Keeper row (Keeper commit):** `StateProvider.close()` (default no-op), `KeeperStateProvider.close()` closing only a client it created (`ownedClient`), `ProxySinkTask.stop()` running writer stop, MBean unregister and provider close in nested `try/finally`. Where: `sink/state/StateProvider.java`, `sink/state/provider/KeeperStateProvider.java`, `sink/ProxySinkTask.java`. Pinned by `CLS-03, CLS-04` and the test class names; say that `getStateRecord` and `setStateRecord` are unchanged.
- **Update the existing harness row** that describes `CountingHelperClient` ("counts `pinReplica` and `unpinReplica` and opens no connection"): it now also counts `close()`, can be built from a real builder, and has scripted `ping`/`version`/mapping answers. Same commit as the test-double change. [VERIFIED: docs/FORK-DELTA.md "Hand-written test doubles" row]
- CHANGELOG and `VERSION` are not touched (release phase, RLS-01); see A2.

### .planning/scripts/fork-anchors.txt (four TAB-separated fields: path, fixed string, count, backs-ID)

Existing rows that must not move (all verified unchanged on the skeleton): the five listed in Pitfall 3 plus `ProxySinkTask.java` rows 178-185.

Proposed new rows with the counts measured on the skeleton (re-measure with `grep -cF -e` on the real commit; the executor owns the final numbers):

| Commit | Path | Fixed string | Count | Backs |
|--------|------|--------------|-------|-------|
| writer | `.../sink/db/ClickHouseWriter.java` | `createHelperClient(` | 3 | CLS-01 |
| writer | `.../sink/db/ClickHouseWriter.java` | `closeHelperClient()` | 3 | CLS-01 |
| writer | `.../sink/db/ClickHouseWriter.java` | `ClickHouseHelperClient chcTmp = new` | 0 | CLS-02 |
| Keeper | `.../sink/state/provider/KeeperStateProvider.java` | `ownedClient` | 4 | CLS-03 |
| Keeper | `.../sink/state/provider/KeeperStateProvider.java` | `public void close()` | 1 | CLS-03 |
| Keeper | `.../sink/ProxySinkTask.java` | `stateProvider.close()` | 1 | CLS-03 |

The mirror `--rows` argument becomes 172 plus the rows added so far. A comment line above the group follows the file's style ("# Phase 40 (v1.7.0_gg7): ..."), and the comment must not itself contain an anchored string.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Helper clients never closed (fork and upstream) | Closed at the three sites | This phase | Fewer open pools on workers that restart or rebalance; V1 connectors stop leaking one pool per batch |
| `Client.close()` idempotence unknown | Verified idempotent on 0.11.0 | 2026-11-01 probe | Double close safe at the library level |

**Deprecated/outdated:** the research note (STACK.md) that the V1 insert path has "4 call sites" vs PITFALLS.md's "3": the code has 4 (`getMutationRequest` at lines 1407, 1409, 1537, 1692 of `ClickHouseWriter.java`) [VERIFIED: grep]. Irrelevant to the fix, which is at the single definition (:1784).

## Runtime State Inventory

Not a rename or migration phase: omitted. The fix changes in-process object lifecycle only; no stored data, service config, OS registration, secret or build artifact carries a changed name. State key and KeeperMap rows are explicitly untouched.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Client.close()` idempotence and `getServer()`-after-close hold on the OpenJ9 17 JVM as on the HotSpot 21 used for the probe | RF-1 | Low: library code with no JVM-specific branch; gates run on HotSpot 21 anyway |
| A2 | `VERSION` and `CHANGELOG.md` are not edited in Phase 40 (release phase RLS-01 owns them), although AGENTS.md says to bump both before a PR | FORK-DELTA rows | Planner adds an unwanted release edit, or omits a wanted one |
| A3 | A full suite takes about 3 minutes on an idle host (project memory "Phase 26 orchestration notes") and testUnit well under that; Phase 39 recorded 37-39 minutes only for the overlapped cumulative gate | Gate Plan | Task timeouts mis-sized; `test_gate_timeout` is 2400 s in config |
| A4 | Reflective `Field.set` on `ProxySinkTask.stateProvider` (private final) also works on OpenJ9 17 | Pattern 3 | P1/P2 would need a package-private hook instead; gates use HotSpot 21 |
| A5 | The Keeper-constructor failure (V1 `init()` throwing) leaks the client and is outside the declared scope | OQ-1 | If the user wants it covered, one more `src/main` hunk and one more pin |

## Open Questions (RESOLVED)

> Autonomous-run decision: OQ-1 resolved as the recommended default (NO: do not close the Keeper client on constructor failure; out of declared scope; recorded as a FORK-DELTA residual). OQ-2..OQ-4 as stated. VERSION/CHANGELOG stay with the release phase.
1. **OQ-1: close the Keeper client when its own constructor fails? NEEDS A USER DECISION (default: no).**
   - What we know: `KeeperStateProvider(csc)` builds the helper, then `init()`. On V2, `init()` swallows exceptions. On V1, `chc.queryV1(createTable)` can throw out of the constructor [VERIFIED: KeeperStateProvider.java:85-94]. Then `new ProxySinkTask(...)` throws inside `ClickHouseSinkTask.start()`, `proxySinkTask` stays null, and `ClickHouseSinkTask.stop()` skips it, so Connect's `stop()` cannot rescue it. The client leaks once per failed start.
   - What's unclear: whether this counts as inside the declared Keeper scope. ROADMAP says "the client that `KeeperStateProvider` creates" and names only `close()` and `ProxySinkTask.stop()`. A `src/main` change outside a phase's declared scope is a user gate.
   - Recommendation: do not include by default; record it as residual R-1 in FORK-DELTA with an owner and trigger. If the user approves, it is a 5-line `try { init(); } catch (RuntimeException e) { close(); throw e; }` in the 2-arg constructor plus one scripted `queryV1`-throws pin (K4).
2. **OQ-2: red-first granularity. RESOLVED (planner confirms): strict reading.** Record all red runs before the first fix, keeping the Keeper files stashed during gate A (Pitfall 5). The per-half alternative is allowed if the planner reads criterion 1 per half; both satisfy "separate commits and gates".
3. **OQ-3: nested `finally` masking. RESOLVED: use the plain nested form** (meets the criterion). Upgrade to `addSuppressed` only if a reviewer asks.
4. **OQ-4: is a second `start()` on one writer (the old `chc` overwritten unclosed) in scope? RESOLVED: no.** `ClickHouseSinkTask.start()` builds a new `ProxySinkTask` and a new writer every time, so production never calls `start()` twice on one writer, and the `start()` wrapper only closes on failure. Record as residual R-3.
5. **R-2 (residual, not a question): `ClickHouseHelperClient`'s constructor can throw after `createClientV2()` succeeded (`createClientV1()` fails), leaking that V2 client with no object to close.** Out of scope: it is in a different file with 13 anchor rows and no caller can close what was never returned. Record in FORK-DELTA.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | Full-suite gate, live pins | yes (`colima list`: `vz` Running, 8 CPU, 16 GiB) | docker CLI 29.8.2 at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker` | none; never run a bare `colima start` (wrong default profile) |
| JDK 21 HotSpot | Gate JVM (`-PjavaVersion=21`) | yes | OpenJDK 21.0.12.1 Homebrew | Semeru/OpenJ9 17 (crash risk) |
| Gradle wrapper | All runs | yes (probe ran `--offline`) | 9.8.2 (per Phase 39 E-01) | none |
| python3 | evidence helper | yes | system | none |
| `gh` | re-reading upstream PRs | yes | authenticated | `git` plus REST GET |
| ClickHouse image `latest` | full suite | pulled by Testcontainers (always-pull policy for floating tags) | 26.10.3.7 at the Phase 39 gate | none |
| `timeout` command | — | no (macOS) | — | poll loops |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** `timeout` (not needed).
`TESTCONTAINERS_HOST_OVERRIDE` is set to a literal in this shell: always `env -u`. `DOCKER_HOST` points at the `vz` socket. [VERIFIED: shell inspection this session]

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json`. [VERIFIED: config.json]. The requirement-to-pin map is in the case matrix (Pattern 3) and the Gate Plan.

## Security Domain

`security_enforcement` is enabled (ASVS level 1, block on high). [VERIFIED: config.json]

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication / V3 Session | no | Credentials handling unchanged; clients are built from the same config |
| V4 Access Control | no | — |
| V5 Input Validation | no | No new input |
| V6 Cryptography | no | No change to TLS or SNI handling (`sslSocketSni` stays on the writer's builder only) |
| V7 Error Handling and Logging | yes | The new `LOGGER.warn("Failed to close ClickHouse helper client", e)` carries no password, URL or record data; do not log the builder or the node |
| V12 Resources / availability | yes | This phase is the fix: unreleased HTTP pools and sockets are a slow resource-exhaustion path on long-lived workers |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Resource exhaustion from leaked pools (V1 default: one per batch) | Denial of service | Close at each site, pinned by counting tests |
| Secret leakage through close-failure logging | Information disclosure | Log a constant message plus the exception only; never the connection URL (`ClickHouseHelperClient` already logs the URL at INFO in its constructor, unchanged and outside this phase) |
| State corruption in exactly-once mode through an edited read/write path | Tampering | Keeper diff check: `getStateRecord`/`setStateRecord` in no hunk |

## Sources

### Primary (HIGH confidence)
- Code read this session: `ClickHouseWriter.java` (55-205, 1384-1440, 1515-1570, 1670-1722, 1742-1887), `ClickHouseHelperClient.java` (40-267, 695-814), `KeeperStateProvider.java` (all), `ProxySinkTask.java` (all), `StateProvider.java`, `ClickHouseSinkTask.java`, `BaseStateProviderImpl.java`, `TableMappingRefresher.java`, `StubClickHouseServer.java`, `CountingHelperClient.java`, the listed tests, `SourcePlanningIdGuardUnitTest.java`, `ProxySinkTaskConstructorUnitTest.java`.
- Probes run this session against the project's resolved test classpath (scratch dir outside the repo): `Probe` (close idempotence), `V1Probe` (V1 insert against `StubClickHouseServer`), `FinalProbe` (final-field swap), a scratch `javac` of the skeleton, and `grep -cF` anchor measurements.
- Planning artifacts: 40-CONTEXT.md, ROADMAP.md Phase 40, REQUIREMENTS.md CLS-01..04, STATE.md, `.planning/research/{STACK,ARCHITECTURE,PITFALLS,FEATURES}.md`, 36-FOLLOWUPS.md, Phase 39 SUMMARY/COMMAND-EVIDENCE/evidence-check.py, `verify-merge-invariants.sh`, `fork-anchors.txt`, `docs/FORK-DELTA.md`.

### Secondary (MEDIUM confidence)
- Apache Kafka `WorkerTask.doRun()` and `WorkerSinkTask.close()` at tags 3.9 and 4.1 (raw GitHub, summarised by the fetch tool: structure confirmed, long quotes not reproduced): `finally { doClose(); }` and `task.stop()` inside `close()`.
- Upstream PRs #861, #855, #857 via `gh pr view` and `gh pr diff`: all OPEN as of this session; #861 touches the same builder blocks and adds no close.

### Tertiary (LOW confidence)
- None used for decisions. Timing figures in A3 come from project memory, not from a run this session.

No research-seam cache entries were written and `classify-confidence` was not run: no web search provider was needed, every finding rests on direct code reads, probes, or the two primary-source fetches above.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new dependency and versions read from the classpath.
- Architecture: HIGH, skeleton compiled against the real classpath and checked against the anchor rows.
- Pitfalls: HIGH, the guards (`SourcePlanningIdGuardUnitTest`, `ProxySinkTaskConstructorUnitTest`, anchors) were read, not recalled.
- Gate counts: MEDIUM-HIGH, taken from Phase 39 records; the new totals depend on how many cases the executor writes.

**Research date:** 2026-11-01
**Valid until:** 2026-11-15 (upstream PR #861 can merge or rebase at any time; the ClickHouse `latest` digest also drifts)
