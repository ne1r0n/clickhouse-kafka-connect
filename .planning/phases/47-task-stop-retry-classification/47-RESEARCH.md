# Phase 47: Task Stop & Retry Classification - Research

**Researched:** 2026-11-05
**Domain:** `ClickHouseSinkTask.stop()` cleanup under a throwing delivery strategy (LIF-01/LIF-02), what Kafka Connect does with a `RetriableException` thrown from `put` and whether a retry budget can be built without a new key (RTY-01), and the closing of FUT-05 (RTY-02), on connector tip `5d934376` (Phase 46 complete)
**Confidence:** HIGH for LIF-01 (the three `stop()` bodies read, the unchanged-tree red and the fixed green both measured through the real `ClickHouseSinkTask`/`ProxySinkTask`); HIGH for the classification matrix (18 shapes x 2 tolerance values measured through `ProxySinkTask.put`); HIGH for the Connect behaviour (source of `WorkerSinkTask` read in 3.9.1, 4.1.1 and 4.4.0, and measured on a real Connect worker, Kafka 3.9 line); MEDIUM for how operators in the wild configure `errors.retry.timeout` (inferred from the repo's own samples). The real repo is untouched: `git status` shows only the two pre-existing dirty files `.planning/config.json` and `.planning/state.json`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. `.planning/phases/47-task-stop-retry-classification/` holds no CONTEXT.md: discuss was skipped (`workflow.skip_discuss: true`) and the user records decisions after reading this research. Every choice that is the user's is in "Decisions that belong to the user at plan time" below, with a recommendation, and none is taken here.

### Claude's Discretion
All implementation choices not listed under the user decisions below. Use the ROADMAP Phase 47 section, the standing rules and the codebase conventions.

### Deferred Ideas (OUT OF SCOPE)
None recorded. Constraints restated from the ROADMAP Phase 47 note (binding, they are the user's own roadmap): `ClickHouseSinkTask`, `ProxySinkTask` and `Utils` are on the hot path and `Utils.handleException` decides classification; **any diff beyond LIF-01's `try`/`finally`, RTY-01's budget or pin and RTY-02's approved validator stops for a user decision**; each code change is its own commit and revert unit behind its own gate; preserve the `BEFORE`/`AFTER` flag sequence (AGENTS.md); a change to `KeeperStateProvider`, the exactly-once state key or KeeperMap rows is the user's gate; changing or removing an existing assertion is the user's gate in any phase; the FUT-05 validator lands only on the user's own words; phase gate is cache-busted `testUnit` with Docker unreachable, a full suite on `latest` x V2, and the anchors invariant.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| LIF-01 | `ClickHouseSinkTask.stop()` stops every delivery strategy and still reaches the later cleanup when one throws (`try`/`finally`), pinned red-first and Docker-free | Exact `stop()` shape and the three strategy `stop()` bodies below; red measured on the unchanged tree (strategy `stop()` runs once, writer `stop()` 0, state-provider `close()` 0, MBean still registered); green measured with the minimal `try`/`finally`; the pin uses reflection on two private fields and a stub of the package-private `DeliveryStrategy`, no Mockito |
| LIF-02 | The CHANGELOG `stop()` bullet and its Known Issues bullet match the fixed behaviour | Both bullets quoted verbatim with line numbers; what the pin proves and what it does not; four placement options with trade-offs (U-2); the other places that carry the same claim |
| RTY-01 | A retriable `DataException` case has a retry budget, or the missing budget is pinned and documented as a limit through `ProxySinkTask.put` under both `errors.tolerance` values | Connect source and live measurement: nothing bounds a `RetriableException` from `put`; neither `errors.retry.timeout` nor `errors.tolerance` reaches it; every in-repo option for a budget needs a new key; the 18-shape x 2-tolerance matrix measured; the unbounded-redelivery pin shape (red against a mutated budget); expected changed rows |
| RTY-02 | FUT-05 is closed. The start-up WARN is kept, and a validator is added only if the user approves it. The decision is recorded with its reason | Current state of the WARN, its pins and every place FUT-05 is named; the validator options with their upgrade consequences (U-4) |
</phase_requirements>

## Pre-flight check (done first)

| Check | Result | Evidence |
|---|---|---|
| Tree | `sync/v1.7.0` at `5d934376 docs(46): complete phase 46 insert timeout limits`; only `.planning/config.json` and `.planning/state.json` dirty | [VERIFIED: `git status --short`, `git log --oneline` run 2026-11-05] |
| Published tag | `v1.7.0_gg7` = `94834763`; `git diff v1.7.0_gg7 HEAD -- CHANGELOG.md` is empty, so the tip's CHANGELOG is byte-identical to the published gg7 one; **no `1.7.0_gg8` section exists** (the top heading is `# 1.7.0_gg7, 2026-11-02`) | [VERIFIED: `git diff`, `sed -n 1p CHANGELOG.md`] |
| Standing tail gate on the unchanged tip | `PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD` (run in a throwaway clone of `5d934376`, the unedited command of the checklist's Gate commands section) printed `invariants: provenance_paths=11 anchor_commits=89 anchor_rows=216 marker_commits=107 anchor_exempt=0 result=PASS`, exit 0 | [VERIFIED: run, output saved at `/tmp/gsd47/gate-baseline.log`] |
| `testUnit` baseline | `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock`, `--rerun`, `-PjavaVersion=21`: 103 test classes, **2181 tests, 0 failures, 0 errors, 2 skipped**, BUILD SUCCESSFUL in 28 s | [VERIFIED: run, JUnit XML summed] |
| Classification pins green on the unchanged tip | `WrappedDataExceptionClassificationPinUnitTest` 14, `RetriableExceptionPassThroughUnitTest` 38, `HandleExceptionCauseCycleUnitTest` 2, `AdditionalRetriableErrorCodesPinUnitTest` 19, `WriterFailureClassificationUnitTest` 216, `ProxySinkTaskStopUnitTest` 4, `UnvettedRetriableCodesWarnUnitTest` 21, `InsertStallBoundPinUnitTest` 15; 0 failures | [VERIFIED: run, JUnit XML] |
| Anchors file | 216 data rows | [VERIFIED: `grep -v '^#' .planning/scripts/fork-anchors.txt \| grep -c .` = 216, equals the gate's `anchor_rows=216`] |
| Docker | colima profile `vz` Running (8 CPU, 16 GiB); local images `confluentinc/cp-kafka:7.9.10`, `confluentinc/cp-kafka-connect-base:7.9.7`, `confluentinc/cp-server-connect:7.9.6` | [VERIFIED: `colima list`, `docker images`] |

## Summary

Four findings change the plan relative to the roadmap text.

1. **LIF-01 is real but latent, and it is one `try`/`finally`.** `ClickHouseSinkTask.stop()` calls `deliveryStrategy.stop()` and only then `proxySinkTask.stop()`, so a throwing strategy skips the writer stop, the MBean unregistration and the state-provider close. Measured on the unchanged tree with a stub strategy that throws: strategy `stop()` 1 call, writer `stop()` **0**, state-provider `close()` **0**, task MBean **still registered**, the strategy's exception reaches the caller. With the minimal `try`/`finally`: 1, **1**, **1**, MBean gone, same exception. No shipped strategy's `stop()` can throw today (`DirectDeliveryStrategy` is empty; the two buffered ones call `clear()` and a logger), so this is defensive, and Connect only logs a throw from `stop()` (`log.warn("Could not stop task", t)` in `WorkerSinkTask.close()`), so the skipped cleanup would stay allocated in the worker JVM. The task holds **one** strategy, not several: the roadmap's "every delivery strategy" means the one `createDeliveryStrategy` returned.
2. **Nothing in Kafka Connect bounds a `RetriableException` from `put`, and no setting the connector sees reaches it.** `WorkerSinkTask.deliverMessages` calls `task.put` directly, catches `RetriableException`, pauses the consumer and retries the same batch on the next loop iteration. `errors.retry.timeout`, `errors.tolerance` and the DLQ configuration are read only by the `RetryWithToleranceOperator` that wraps converters and transformations, never `put`. Measured on a real Connect worker (Kafka 3.9 line): a task that always throws `RetriableException` is called at 3.6 s, 60.0 s, 120.0 s... with defaults, and at exactly the same instants with `errors.retry.timeout=5000`, `errors.tolerance=all` and a DLQ topic; the DLQ stays empty and the task stays RUNNING. The cadence is the worker's `offset.flush.interval.ms` (7 s when set to 7000: calls at 7.0, 14.1, 21.1, 28.1 s). A `DataException` from `put` kills the task even under `errors.tolerance=all`.
3. **A budget needs a new config key, and no persisted state.** Both existing candidates are unusable: `errors.retry.timeout` is Connect-owned with a default of 0 and the repo's own samples all set it to `"60"` (milliseconds), so reusing it as a budget would fail those connectors on their first retriable error; `retryCount` is documented as "clickhouse driver retry" with `Range.between(3, 10)`. A task-level in-memory counter is enough and survives nothing: Connect pauses all partitions and redelivers the identical batch until `put` succeeds, so "consecutive retriable failures with no successful `put`" is exact for the direct path; a restart or rebalance restarts the budget, which is still a bound per incarnation. Because every option needs a new key, and a budget would also falsify three operator-visible texts that existing assertions pin (the start-up WARN text in two tests, the setting's own description), the roadmap rule applies: **record the limit** (U-3 recommends it).
4. **`errors.tolerance` does not change any retriable outcome.** The 18-shape matrix below shows every retriable shape (server code in the built-in set, a code added by `additionalRetriableErrorCodes`, a `RetriableException` anywhere in the chain, `SocketTimeoutException`, `UnknownHostException`, `SocketException` with `retryOnSocketException`, an IOException starting `Read timed out after`, a V1 `ClickHouseException` code 159, and the Phase 46 V2 and V1 timeout shapes) is `RETRIABLE` under both values, and a `DataException` over them is classified by the cause, not by being a `DataException` (the control `RuntimeException>SocketTimeoutException` behaves identically). "Retriable `DataException`" is therefore one classification: any retriable shape.

**Primary recommendation:** LIF-01 as the plain `try`/`finally` with a reflection-seam, red-first pin; RTY-01 as the recorded limit (an unbounded-redelivery pin over the PIN-02 and RSK-02 retriable shapes plus the Phase 46 shapes, FORK-DELTA item 17 rewritten with the measured Connect numbers, no `src/main` change); RTY-02 as keep-the-WARN, no validator, FUT-05 closed with its reason; LIF-02 as a `1.7.0_gg8 Unreleased` section started in this phase (option C of U-2). The only `src/main` change of the phase is LIF-01's +8/-5 lines.

## Decisions that belong to the user at plan time

| # | Decision | Options measured | Recommended default (planner may propose, user confirms) |
|---|---|---|---|
| U-1 | LIF-01 shape when both the strategy's and the proxy's `stop()` throw | **A (plain `try`/`finally`, roadmap-literal, +8/-5 lines):** measured: the proxy's exception surfaces and the strategy's is discarded (`suppressed=0`; Java drops the `try` exception when `finally` throws). **B (the `keepFirst` idiom of `ProxySinkTask.stop()`):** first failure thrown, later attached as suppressed; about 15 lines, mirrors the in-repo idiom at `ProxySinkTask.java:76-108`; it is arguably "a diff beyond try/finally" so it stops for the user | **A.** It is what the roadmap prescribes and the double throw is not reachable with shipped strategies. The pin asserts that both stops were attempted and that a `RuntimeException` surfaced, and does not assert which, so B stays possible later without editing an assertion |
| U-2 | Where the CHANGELOG correction lives (LIF-02) | **A (gg7 section in place):** edit the two published bullets; rewrites released history and cannot say the gg7 tag had no `try`/`finally`. **B (gg8 only, written by Phase 49):** keeps the file append-only (gg7 closed gg6's Known Issues the same way: the new section's Bug Fixes says "This closes the first item of the `1.7.0_gg6` Known Issues", and no line of an older section was removed between gg6 and gg7); but Phase 47's criterion 2 cannot be checked in-phase. **C (start `# 1.7.0_gg8, Unreleased` in Phase 47 with the `stop()` bullets, Phase 49 completes and dates it):** the text is reviewed and gated in the phase that proves it; the fork already turns an upstream `Unreleased` block into a release section (gg6). **D (A plus C)** | **C**, gg7 section untouched. The gg8 bullet states the correction of the gg7 Bug Fixes wording explicitly. The ROADMAP cross-phase table assigns `CHANGELOG.md` to "47 (`stop()` bullets)" and "49 (`1.7.0_gg8` section)", which C satisfies; A is the literal reading of "the bullets" and is the user's call because it edits a published section |
| U-3 | RTY-01 outcome | **A (bounded budget):** needs a new key (default unbounded to keep every connector as is), a decision on what exhaustion does under each tolerance (`none`: fail the task; `all`: DLQ the batch), an edit of the start-up WARN text and of the `additionalRetriableErrorCodes` description, and of two existing assertions of that WARN text (the user's gate). **B (record the limit):** unbounded redelivery pinned and written in FORK-DELTA with the measured Connect behaviour | **B.** Every budget option needs a new key (finding 3), the roadmap says to record then, and a budget turns a transient ClickHouse outage longer than N redeliveries into a failed task or a DLQ batch, which is a rollout item of its own. The full budget design is written out below so it can be taken later |
| U-4 | RTY-02: validator for `additionalRetriableErrorCodes` | **A (no validator, WARN stays, FUT-05 closed with the reason, recommended).** **B (validator in the `ConfigDef`, in `ErrorCodeListValidator.ensureValid`):** rejects a code outside the vetted set at `PUT`/create and `validate`; a running connector is unaffected until its config is next written (Connect validates on create/update only, not at worker start: `DistributedHerder.java:1135` vs `startConnector:2079`), so the break is latent and lands on the next unrelated config edit or GitOps re-apply. **C (check inside `parseErrorCodes`, called by the `ClickHouseSinkConfig` constructor):** breaks `ClickHouseSinkTask.start()` for an existing connector listing a non-vetted code on upgrade: an upgrade-breaking rollout item | **A.** "Transient" is the connector's judgment, not a ClickHouse classification (FORK-DELTA item 24 already says a listed code such as 60 can be transient while a table is created); a validator would reject legitimate uses and `ClickHouseSinkConfig` is a high-risk file. The roadmap requires the user's own words for B or C |
| U-5 | What the recorded RTY-01 limit tells operators to do | The levers that exist, all measured: none inside the connector. Outside it: fix the cause, remove the code from `additionalRetriableErrorCodes`, pause or delete the connector; the redelivery interval is the worker's `offset.flush.interval.ms` | State only the measured facts (cadence, no effect of `errors.retry.timeout`/`errors.tolerance`, task stays RUNNING, ERROR log per redelivery) and the pin; do not invent a remedy the repo did not measure |

## Architectural Responsibility Map

This is a task-lifecycle and error-classification phase inside one JVM sink connector; there is no browser/SSR/CDN tier. Tiers are connector layers plus the Connect runtime.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Task shutdown order and failure isolation | `sink.ClickHouseSinkTask.stop()` | `sink.ProxySinkTask.stop()` (already isolates writer, MBean, provider with `keepFirst`) | The task owns the strategy and the proxy; the proxy already isolates its own three steps |
| Redelivery of a failed batch, its cadence and its (absent) bound | Kafka Connect runtime (`WorkerSinkTask.deliverMessages`) | connector `Utils.handleException` decides only retriable vs not | Connect owns redelivery; the connector can only choose to throw `RetriableException` or not |
| Retriable vs non-retriable classification | `util.Utils.handleException` | `Processing.doInsert` (wraps every writer failure in `RuntimeException(queryId, e)`, `Processing.java:72` and `:96`) | Already correct and pinned; a budget would sit above it in `ProxySinkTask.put` |
| Tolerance routing (fail / DLQ) | `ProxySinkTask.put` and `ChunkFlusher.putDirect` (the connector's own `errors.tolerance` read at `ClickHouseSinkConfig.java:282`) | Connect's DLQ reporter (only for converter/transform/errant-record failures) | Connect never sees a `put` failure under `all`: the connector swallows or DLQs it itself |
| Config validation of `additionalRetriableErrorCodes` | `sink.ClickHouseSinkConfig` (`ErrorCodeListValidator`, `parseErrorCodes`) | Connect herder (validates on create/update only) | High-risk file; validator and constructor check have different blast radii |
| Operator-facing statements | `docs/FORK-DELTA.md` (items 8, 13, 17, 24), `CHANGELOG.md`, Phase 49's gg8 rollout note | the gg7 and gg6 notes (published, tag copies) | Phase 47 writes the facts; Phase 49 writes the release note |

## Standard Stack

No new library, no version change. The phase uses what ships and the existing test infrastructure.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kafka Connect API | 4.4.0 | `RetriableException`, `DataException`, `SinkTask`, `SinkRecord` | `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)` [VERIFIED: `gradle/libs.versions.toml:3`] |
| JUnit 5 (Jupiter) + the in-repo `InMemoryDBWriter`, `InMemoryState`, `InMemoryDLQ`, `ServerExceptions`, `LogCapture`, `SchemalessTestData` | repo | Docker-free pins, reflection seam as in `ProxySinkTaskStopUnitTest` | No Mockito (OpenJ9 crash; STATE.md standing rule) |
| `connect-runtime` (read only) | 3.9.1, 4.1.1, 4.4.0 sources | The redelivery semantics | Not on the classpath of the connector or its tests; sources read from Maven Central into `/tmp/gsd47` and the Confluent `cp-kafka-connect-base:7.9.7` image ran the live probe |

**Installation:** none. **Version verification:** `connect-runtime-<v>-sources.jar` for 3.9.1, 4.1.1 and 4.4.0 fetched from `repo1.maven.org/maven2/org/apache/kafka/connect-runtime/<v>/` into separate empty directories and read only; `connect-runtime` 4.1.1 is **not** in the Gradle cache (only `connect-api`, `connect-json` and `kafka-clients`), so the decompile route named in the brief was replaced by the sources jar. [VERIFIED: downloads and reads this session]

## Package Legitimacy Audit

No package is installed or changed by this phase. The sources jars were downloaded for reading and are on no classpath (the seam supports `npm|pypi|crates` only; Phase 45 and 46 recorded the Maven gap).

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none new) | — | — | — | — | — | — |

**Packages removed due to [SLOP] verdict:** none. **Packages flagged [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
 Kafka Connect WorkerSinkTask.iteration()                       (runtime, not the connector)
   poll(timeoutMs = nextCommit - now)  <-- blocks up to offset.flush.interval.ms when all partitions are paused
   deliverMessages():  task.put(batch)
          │ returns normally ──────────────► batch cleared, offsets advance, pausedForRedelivery=false
          │ throws RetriableException ─────► pauseAll(); keep messageBatch; next iteration redelivers the SAME batch
          │                                  (no counter, no deadline, not wrapped by RetryWithToleranceOperator)
          │ throws anything else ──────────► ConnectException("Exiting WorkerSinkTask due to unrecoverable exception.") task FAILED
          ▼
 ClickHouseSinkTask.put ─► DeliveryStrategy.put ─► [Direct | AtLeastOnceBuffer | ExactlyOnceBuffer]
          ▼
 ChunkFlusher.putDirect / ProxySinkTask.put ─► Processing.doLogic ─► Processing.doInsert ─► DBWriter.doInsert
          │ any writer failure ─► RuntimeException(queryId, e)
          ▼
 Utils.handleException(e, errorsTolerance, retryOnSocketException, additionalCodes, records)
   1 root cause is a ClickHouse server error: code in DEFAULT_RETRIABLE_ERROR_CODES or additionalRetriableErrorCodes -> RetriableException
   2 RetriableException anywhere in the cause chain                                                                  -> RetriableException
   3 e instanceof DataException && tolerance none                                                                     -> DataException (not reached for writer failures: Processing wraps them)
   4 root is SocketTimeout / UnknownHost / (SocketException && retryOnSocketException) / IOException "Read|Write timed out after" -> RetriableException
   5 tolerance all -> swallowed (the caller DLQs the batch);  tolerance none -> RuntimeException("Number of records: n")

 Task stop:  Connect WorkerSinkTask.close(): try { task.stop(); } catch (Throwable t) { log.warn("Could not stop task", t); }
   ClickHouseSinkTask.stop():  deliveryStrategy.stop();  proxySinkTask.stop();      <-- today: a throw in the first skips the second
   ProxySinkTask.stop():       dbWriter.stop(); statistics.unregisterMBean(); stateProvider.close();   (each isolated, keepFirst)
```

### Recommended Project Structure

No new package. Touch points:
```
src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java     # LIF-01: stop() only (+8/-5 lines)
src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java      # new, LIF-01 pin (Docker-free)
src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java # new, RTY-01 limit pin (Docker-free)
docs/FORK-DELTA.md                      # items 8, 13, 17, 24 (and the table row that says "see Known cost 24")
CHANGELOG.md                            # U-2: `# 1.7.0_gg8, Unreleased` with the stop() bullets
.planning/scripts/fork-anchors.txt      # +2 rows with the LIF-01 code commit
.planning/phases/47-.../47-COMMAND-EVIDENCE.md, 47-evidence-check.py (byte copy of 46-evidence-check.py), the status-delta helper
```

### Pattern 1: LIF-01 `stop()` (the whole `src/main` change)

**What:** run the strategy stop, and reach the proxy stop in `finally`.
**Current code** [VERIFIED: `ClickHouseSinkTask.java:179-191`, Read this session], verbatim:
```java
    @Override
    public void stop() {
        // close() is called before stop() by the framework, which already removes
        // buffered records for revoked partitions. Any remaining buffered records'
        // offsets were never committed via preCommit(), so Kafka redelivers them on
        // restart (at-least-once).
        if (deliveryStrategy != null) {
            deliveryStrategy.stop();
        }
        if (this.proxySinkTask != null) {
            this.proxySinkTask.stop();
        }
    }
```
**Minimal change** (applied in a throwaway clone, measured green, then reverted):
```java
        try {
            if (deliveryStrategy != null) {
                deliveryStrategy.stop();
            }
        } finally {
            if (this.proxySinkTask != null) {
                this.proxySinkTask.stop();
            }
        }
```
`git diff --stat` of the clone: `ClickHouseSinkTask.java | 13 ++++++++-----` (8 insertions, 5 deletions). Strategy `stop()` bodies read [VERIFIED: `DirectDeliveryStrategy.java:62` (comment only), `AtLeastOnceBufferStrategy.java:96-103` (`buffer.clear()`, `LOGGER.warn`), `ExactlyOnceBufferStrategy.java:86-96` (iterate and clear the map, `LOGGER.warn`)]: no checked exception and no call that can throw other than a logging backend failure or an `Error`. The `DeliveryStrategy` interface is package-private (`interface DeliveryStrategy`, `DeliveryStrategy.java`), and `ClickHouseSinkTask` has no setter or constructor that takes a strategy.

### Pattern 2: the LIF-01 pin without Mockito (reflection seam, the in-repo idiom)

`ProxySinkTaskStopUnitTest` swaps `stateProvider` into a `ProxySinkTask` by `Field.setAccessible` and stays in `testUnit`; the same works here: a test in package `com.clickhouse.kafka.connect.sink` implements the package-private `DeliveryStrategy` with a stub whose `stop()` counts and throws, builds a `ProxySinkTask(config, null, connectorName, recordingWriter)` over `InMemoryDBWriter` (package-private constructor, test seam already used by Phase 40/46 pins), swaps a counting `InMemoryState` in as `stateProvider`, and sets the private fields `proxySinkTask` and `deliveryStrategy` of a `new ClickHouseSinkTask()`. The measured probe (throwaway, in `/tmp/gsd47/probes`, never committed):
```java
// scratch probe, run this session; fields set with Field.setAccessible exactly as ProxySinkTaskStopUnitTest does
ClickHouseSinkTask task = new ClickHouseSinkTask();
set(task, "proxySinkTask", proxy);            // ProxySinkTask over a RecordingWriter (stop counted) and a CountingState (close counted)
set(task, "deliveryStrategy", strategy);      // stub: stop() { stopCalls++; throw failure; }
proxy.start();                                 // registers the task MBean
RuntimeException thrown = assertThrows(RuntimeException.class, task::stop);
assertSame(strategy.failure, thrown);          // holds before and after
assertEquals(1, writer.stopCalls);             // RED on the unchanged tree (0), green after
```
Measured: **unchanged tree** `PROBE strategyStopCalls=1 writerStopCalls=0 stateCloseCalls=0 mbeanRegisteredAfter=true thrownSame=true`, then `AssertionFailedError: writer must be stopped ==> expected: <1> but was: <0>`; **with the `try`/`finally`** `strategyStopCalls=1 writerStopCalls=1 stateCloseCalls=1 mbeanRegisteredAfter=false thrownSame=true`. Both-throw case: `thrown=IllegalStateException:scripted strategy stop failure suppressed=0` on the unchanged tree (the proxy is never reached), `thrown=IllegalArgumentException:proxy side failure suppressed=0` with the `try`/`finally` (U-1). The pin must clean up in `finally` (`statistics.unregisterMBean()`), because the red run leaves the MBean registered, as `ProxySinkTaskStopUnitTest` does.

What the pin proves, and the bounds of the CHANGELOG claim: with a strategy whose `stop()` throws, `ClickHouseSinkTask.stop()` still runs `ProxySinkTask.stop()` once (writer stop, MBean unregistration, state-provider close), and the strategy's exception is the one that propagates when only it throws. It does **not** prove: the both-throw outcome beyond "both attempted", that a real shipped strategy can throw, the other residuals of the gg7 Known Issues bullet (Keeper constructor on V1, helper-client constructor, second `start()`, the five-second wait), or anything about `close(partitions)`, which has the same no-`finally` shape (`ClickHouseSinkTask.java:161-169`, `proxySinkTask.onPartitionRemoved` then `deliveryStrategy.close`) and is outside this phase's scope (a diff beyond LIF-01 stops for the user). Optional extra pin rows (cheap, Docker-free): the three real strategies each reached by `stop()` with buffered records, so the "every strategy" wording is covered.

### Pattern 3: the RTY-01 limit pin (unbounded redelivery)

A pin that drives `ProxySinkTask.put` with a writer that fails every time, `REDELIVERIES` (500 measured) consecutive times with the same batch, per shape x tolerance, and asserts every call throws `RetriableException`, the writer was called 500 times, and the DLQ stayed empty. Include the connector props `errors.retry.timeout=60` (the repo-sample value) and a small `retryCount`, so the pin also states that these do not bound it at the connector layer. Measured green on the unchanged tree (500 redeliveries in 72 to 204 ms per shape x tolerance, 6 combinations), and **red against a mutated budget** (a consecutive-failure counter in `ProxySinkTask.put` that throws a `RuntimeException` after 5: `AssertionFailedError: redelivery 6 ==> Unexpected exception type thrown, expected: RetriableException but was: RuntimeException`), so it has teeth against any budget of fewer than 500 attempts. Cover both entries as `RetriableExceptionPassThroughUnitTest` does (`ProxySinkTask.put` and `ChunkFlusher.putDirect`). It is a characterization pin, so "red first" does not apply to it; the mutation run is the evidence that it can fail.

### Anti-Patterns to Avoid
- **Catching `Throwable` in `ClickHouseSinkTask.stop()` to swallow the strategy's failure.** The strategy's exception must still reach Connect; only the proxy stop is guaranteed.
- **Reusing `errors.retry.timeout` or `retryCount` as a budget** (finding 3; evidence below).
- **A budget keyed on batch identity or persisted in Keeper.** Unnecessary (Connect redelivers one identical batch until success) and the user's gate (state key, KeeperMap rows).
- **Putting a budget in `Utils.handleException`.** It is a stateless static used by both `ProxySinkTask.put` and `ChunkFlusher.putDirect`; a counter there would double count a failure that passes both layers.
- **Editing the published gg7 CHANGELOG, ROLLOUT-v1.7.0_gg7.md or ROLLOUT-v1.7.0_gg6.md in this phase.** Phase 48 owns the gg6 note edit and Phase 49 owns the gg8 note and its review.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Substituting a strategy or a provider in a test | A Mockito mock, or a new production constructor or setter for tests | `Field.setAccessible` on the private field, the in-repo idiom (`ProxySinkTaskStopUnitTest`, `ProxySinkTaskConstructorUnitTest`) | OpenJ9 crash with inline mocking; production code stays unchanged |
| Throwing writer for classification pins | A new fake | the `FailingDBWriter extends InMemoryDBWriter` shape of `WrappedDataExceptionClassificationPinUnitTest` and `RetriableExceptionPassThroughUnitTest` | Same layer (`ProxySinkTask.put` over the `DBWriter` seam), same `Processing` wrapper |
| Per-test status delta for the changed-row list | An ad hoc diff | Phase 42's `42-testcase-status.py` (`dump`, `select`, `delta`, `count`), now archived at `.planning/milestones/v1.7.0_gg7-followup-phases/42-isolated-high-risk-edits/42-testcase-status.py`; copy it byte for byte into the phase directory | Same convention: no removed row, added rows equal the committed expected list |
| Evidence entries | A new format | `46-evidence-check.py` copied as `47-evidence-check.py` | The `E-NN` entry format the verifiers read |
| Redelivery bound | A connector-side loop or sleep in `put` | Nothing in this phase (U-3 B); if a budget is taken later, a counter in `ProxySinkTask.put` | A sleep inside `put` would block the poll loop past `max.poll.interval.ms` |

**Key insight:** Connect already owns redelivery; the connector's only levers are "throw `RetriableException`" and "do not". Anything that looks like a retry budget in the connector is an exhaustion policy (fail or DLQ) that Connect does not have, which is why it needs a key and a rollout item.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted. (No stored data, service config, OS registration, secret or build artifact carries a renamed string; the phase adds no persisted state.)

## Findings by requirement

### LIF-01 (the exact shapes)

| Item | Finding | Evidence |
|---|---|---|
| `ClickHouseSinkTask` fields | `private ProxySinkTask proxySinkTask;` `private ClickHouseSinkConfig clickHouseSinkConfig;` `private ErrorReporter errorReporter;` `private DeliveryStrategy deliveryStrategy;` | [VERIFIED: `ClickHouseSinkTask.java:30-36`] |
| One strategy per task | `createDeliveryStrategy` returns `DirectDeliveryStrategy`, `ExactlyOnceBufferStrategy` or `AtLeastOnceBufferStrategy` (one) | [VERIFIED: `ClickHouseSinkTask.java:71-85`] |
| `start()` and a null strategy | `start()` builds the proxy and starts it **before** the strategy is created, so after a failed `start()` the proxy exists and the strategy is null; `UnvettedRetriableCodesWarnUnitTest` already calls `task.stop()` in that state | [VERIFIED: `ClickHouseSinkTask.java:60-63`; test `startAndCaptureWarns` `finally { task.stop(); }`] |
| `ProxySinkTask.stop()` | Each of writer stop, MBean unregistration and provider close runs, the first failure is thrown and later ones are suppressed (`keepFirst`) | [VERIFIED: `ProxySinkTask.java:76-108`] |
| What Connect does with a throw from `stop()` | `try { task.stop(); } catch (Throwable t) { log.warn("Could not stop task", t); }` then continues closing the consumer and converters | [VERIFIED: `WorkerSinkTask.java` `close()`, 4.1.1 sources] |
| What a skipped proxy stop leaves | Writer: the `clickhouse-table-sync` executor, the Phase 46 `insertWatchdog` executor (`ClickHouseWriter.java:214-217`, shut down in `stop()`) and the helper client; plus the task MBean and, with `exactlyOnce=true`, the Keeper state provider's client | [VERIFIED: `ClickHouseWriter.stop()` read; measured MBean `mbeanRegisteredAfter=true`, writer stop 0, provider close 0] |

### LIF-02 (the exact CHANGELOG text)

Published `1.7.0_gg7` section, verbatim [VERIFIED: `CHANGELOG.md:31-35` and `:51-55`, Read this session]:

Bug Fixes bullet (lines 31-35):
```
* The writer closes its helper client on `stop()` and on every failing exit of `start()`. On client V1, the helper client
  that every insert batch builds is closed after use. With `exactlyOnce=true`, the Keeper state provider closes the client
  its constructor built. The task's `stop()` stops the writer, the MBean and the state provider even when one of them
  throws. This closes the first item of the `1.7.0_gg6` Known Issues for the paths named here; its residuals are listed
  below.
```
Known Issues bullet (lines 51-55):
```
* The closing has residuals: a Keeper state-provider constructor that fails on client V1, a helper-client constructor
  that fails after it built its client V2, a second `start()` on one writer, and a task-level `stop()` that has no
  `try`/`finally` around the delivery strategy's `stop()`. The writer's `stop()` waits at most five seconds for the table
  refresh and then closes its client, so a refresh that is still running can end with an error on a closed client.
```
Reading: the Bug Fixes sentence "The task's `stop()` stops the writer, the MBean and the state provider even when one of them throws" is true of `ProxySinkTask.stop()` (pinned by `ProxySinkTaskStopUnitTest`, and the rollout note F3 says exactly "The stop of the task's proxy (`ProxySinkTask.stop()`)"), and false for `ClickHouseSinkTask.stop()` when the strategy throws, which the Known Issues bullet of the same section discloses. After LIF-01 only the `stop()` clause of the Known Issues bullet becomes stale; the other four residuals in that bullet stay true. `grep` of `.github/` and `.planning/scripts/` finds no workflow or script that reads `CHANGELOG.md`, so no tooling parses the top heading [VERIFIED: grep]; whether the GitHub release body was generated from it is not visible in the repo [ASSUMED: not derived from it].

Other places that carry the same claim, for the planner's edit list [VERIFIED: grep this session]: `docs/FORK-DELTA.md` item 13 (line 242: "`ClickHouseSinkTask.stop()` calls `deliveryStrategy.stop()` before `proxySinkTask.stop()` without `try/finally`, so a strategy whose `stop()` threw would skip the task's stop and its closes (no strategy's `stop()` throws today; outside this change's scope)"), `docs/ROLLOUT-v1.7.0_gg7.md` row F5 (line 52) and the bullet at line 208 (a published tag copy: not edited here; Phase 49's gg8 note carries the closure), and the planning records `.planning/PROJECT.md` and `.planning/MILESTONES.md` (orchestrator-owned).

Options and the recommendation are U-2. Draft text for option C, stating only what the pin proves (Phase 49 dates and completes the section):
```
# 1.7.0_gg8, Unreleased

## Bug Fixes

* `ClickHouseSinkTask.stop()` now stops the proxy task (the writer, the MBean and the state provider) even when the
  delivery strategy's `stop()` throws; when only the strategy throws, its exception is the one that reaches Kafka
  Connect. This closes the `stop()` residual of the `1.7.0_gg7` Known Issues and corrects the `1.7.0_gg7` Bug Fixes
  line "The task's `stop()` stops the writer, the MBean and the state provider even when one of them throws": that held
  for `ProxySinkTask.stop()` only. No delivery strategy's `stop()` throws today, so a running connector behaves as before.

## Known Issues

* The other residuals of the `1.7.0_gg7` closing stay open: a Keeper state-provider constructor that fails on client V1,
  a helper-client constructor that fails after it built its client V2, a second `start()` on one writer, and the
  five-second wait in the writer's `stop()`.
```

### RTY-01 (what Connect does, measured and read)

`WorkerSinkTask.deliverMessages` [VERIFIED: `WorkerSinkTask.java` 4.1.1, lines 623-658; the same text in 3.9.1 and 4.4.0: `diff` of the `deliverMessages` and `poll` method bodies against 4.1.1 printed nothing for both], verbatim excerpt:
```java
        } catch (RetriableException e) {
            log.error("{} RetriableException from SinkTask:", this, e);
            if (!pausedForRedelivery) {
                // If we're retrying a previous batch, make sure we've paused all topic partitions so we don't get new data,
                // but will still be able to poll in order to handle user-requested timeouts, keep group membership, etc.
                pausedForRedelivery = true;
                pauseAll();
            }
            // Let this exit normally, the batch will be reprocessed on the next loop.
        } catch (Throwable t) {
            log.error("{} Task threw an uncaught and unrecoverable exception. Task is being killed and will not "
                    + "recover until manually restarted. Error: {}", this, t.getMessage(), t);
            throw new ConnectException("Exiting WorkerSinkTask due to unrecoverable exception.", t);
        }
```
and `task.put(new ArrayList<>(messageBatch))` is called directly, outside any `retryWithToleranceOperator.execute`. The operator is used for the key, value and header converters (`WorkerSinkTask.java:553-565`) and, for `Stage.TASK_PUT`, only by `WorkerErrantRecordReporter` (`executeFailed`, `WorkerErrantRecordReporter.java:113`), i.e. when a task reports an errant record itself. Its retry loop `execAndRetry` is bounded by `errorRetryTimeout` (`ConnectorConfig`: "The maximum duration in milliseconds that a failed operation will be reattempted. The default is 0, which means no retries will be attempted. Use -1 for infinite retries.", `ERRORS_RETRY_TIMEOUT_DEFAULT = 0`, `ConnectorConfig.java:177-181`), but `put` never enters it. The wait between redeliveries is `poll(timeoutMs)` with `timeoutMs = max(nextCommit - now, 0)` on a consumer whose partitions are all paused, unless the task calls `SinkTaskContext.timeout(long)` (`WorkerSinkTask.poll` reads `context.timeout()`); the connector never does (`grep '\.timeout(' src/main/java` finds nothing) [VERIFIED: source and grep].

**Measured on a real Connect worker** (throwaway sink connector `ProbeSink`, compiled in `cp-kafka-connect-base:7.9.7` against its own `connect-api`, run with `connect-standalone` against a single-node KRaft `cp-kafka:7.9.10`; connect-runtime `7.9.7-ccs`, the Kafka 3.9 line; logs saved at `/tmp/gsd47/live/connect.log` and `connect-flush7000.log`; containers and network removed afterwards). Each connector had one task and one record:

| Connector | Config under test | `put` call instants (ms since task start) | Outcome |
|---|---|---|---|
| c1 | always `RetriableException`; `errors.retry.timeout=5000`, `errors.tolerance=all`, `errors.deadletterqueue.topic.name=dlq1` | 3611, 60009, 120010 | redelivered; DLQ topic stays empty; task not failed; one ERROR log line per redelivery |
| c3 | always `RetriableException`; Connect defaults | 3619, 60043 (c1/c3 identical to the millisecond range), 120051 | same |
| c3 on a worker with `offset.flush.interval.ms=7000` | defaults | 3555, 7045, 14051, 21058, 28066 | cadence follows `offset.flush.interval.ms` |
| c2 | always `DataException`; `errors.tolerance=all`, DLQ `dlq2` | 3666 (once) | `Task threw an uncaught and unrecoverable exception. Task is being killed`; `ConnectException: Exiting WorkerSinkTask due to unrecoverable exception.`; DLQ empty |

Conclusions [VERIFIED: source + measurement]: a `RetriableException` from `put` is redelivered without end at the worker's `offset.flush.interval.ms` (60 s by default); `errors.retry.timeout`, `errors.tolerance` and the DLQ settings have no effect on it; any other exception from `put`, including `DataException`, fails the task even under `errors.tolerance=all` (which is why the connector classifies and swallows or DLQs itself). A redelivery needs nothing persisted: the batch is held in `messageBatch` inside the worker and cleared only after a successful `put`.

**What the connector reads from those settings** [VERIFIED: `ClickHouseSinkConfig.java:73`, `:81`, `:282`, `:413-419`]: `errors.tolerance` into `isErrorsTolerance()` (the connector's own tolerance logic); `errors.retry.timeout` only to log a WARN when its value is below `MINIMAL_RETRY_TIMEOUT_THR_WARN = TimeUnit.SECONDS.toMillis(10)`, defaulting an absent value to `"60000"` where Connect's own default is 0. The repo's own samples set it to `"60"`: `AGENTS.md:73`, `CONTRIBUTING.md:141` and three times in `Support Scripts.md` (lines 376, 404, 434) [VERIFIED: grep]. How operators in the wild set it is not observable here [ASSUMED: many copy the samples].

**Why every budget option needs a new key** [VERIFIED: code read]:

| Candidate source for N | Why it cannot be the budget |
|---|---|
| `errors.retry.timeout` | Connect-owned with default 0; the samples set `"60"` (a 60 ms budget would fail a connector on its first retriable error, an upgrade-breaking change for every connector that follows the samples); the connector already logs a "too low" WARN for it and ignores it otherwise |
| `retryCount` | `Range.between(3, 10)`, described as "clickhouse driver retry" (`ClickHouseSinkConfig.java` `configDef.define(RETRY_COUNT`), used for the helper client's pings and queries (`setRetry(csc.getRetry())` at `ClickHouseWriter.java:116`, `:2085`, `KeeperStateProvider.java:64`); default 3 would mean three redeliveries |
| `errors.tolerance` | Selects what happens to a non-retriable failure; says nothing about how many retriable ones |
| `clickhouseClientInsertTimeoutMs` | A per-insert bound (Phase 46), not a count of redeliveries |

**Budget design, if the user takes U-3 A** (not recommended now; written so it is not rediscovered): a new opt-in key with a default that keeps today's unbounded behaviour; one in-memory pair per `ProxySinkTask` (consecutive retriable failures, time of the first), reset by any successful `put`, by `close(partitions)` and by a new task; no Keeper state and no `BEFORE`/`AFTER` change; checked in `ProxySinkTask.put`'s catch around `Utils.handleException` (a `RetriableException` out of it consumes budget), with the exhaustion outcome per tolerance (`none`: a non-retriable `RuntimeException`, which Connect turns into a failed task; `all`: send the batch to the DLQ and return, as for a non-retriable failure today). Coupling that makes it a gated change: `ChunkFlusher.putDirect` calls `ProxySinkTask.put` and then `Utils.handleException` again, so under `all` the exhausted failure must be recognisable there; the start-up WARN text ("redelivered with no retry budget and outside errors.tolerance and the dead letter queue") is asserted verbatim by `UnvettedRetriableCodesWarnUnitTest` (`EXPECTED_WARN_FORMAT`) and `UnvettedRetriableCodesWarnLiveTest`, and the `additionalRetriableErrorCodes` description says "Redelivery has no retry budget and does not go through errors.tolerance or the DLQ" (`ClickHouseSinkConfig.java:614`), so a budget edits two existing assertions (the user's gate) and the setting text. At the default cadence N redeliveries take N minutes, so a time budget in ms is the clearer unit. It converts a transient ClickHouse outage longer than the budget into a failed task (`none`) or a DLQ batch (`all`), a rollout item.

**Interaction with Phase 46** [CITED: `.planning/phases/46-insert-timeout-limits/46-RESEARCH.md`, "Server state after aborts"]: the Phase 46 bounds turn a hung insert into a `RetriableException` after `clickhouseClientInsertTimeoutMs`, so a permanently hung insert now cycles bound + 60 s forever instead of blocking; a timed-out insert is not rolled back (2000 of 4000 rows visible at once and 3500 twelve seconds later on MergeTree), so with `exactlyOnce=false` every redelivery can add rows. This belongs in the recorded limit.

### The classification matrix (measured through `ProxySinkTask.put`)

Measured 2026-11-05 on the unchanged tip with a throwaway probe (`/tmp/gsd47/probes/Rty01ProbeUnitTest.java`, a `FailingDBWriter extends InMemoryDBWriter` behind the `ProxySinkTask(config, dlq, writer)` seam, 3 schemaless records, `Processing` wrapping each writer failure in `RuntimeException(queryId, e)`). Outcome vocabulary as `WrappedDataExceptionClassificationPinUnitTest`: `RETRIABLE` = `RetriableException` out of `put`, `FAILS_TASK` = `RuntimeException("Number of records: 3")`, `DLQ(3)` = `put` returned and the InMemoryDLQ holds 3. "Connect then" is from the source and live measurement above.

| # | Writer failure shape | `none` | `all` | Pinned today by | Connect then |
|---|---|---|---|---|---|
| S01 | `DataException` plain | FAILS_TASK | DLQ(3) | PIN-02 `WRAPPED_DATA_EXCEPTION` | none: task FAILED; all: offsets advance |
| S02 | `DataException > ServerException(241)` (built-in set) | RETRIABLE | RETRIABLE | PIN-02 `RETRIABLE_SERVER_CODE_IN_CHAIN` | redelivered every `offset.flush.interval.ms`, no end |
| S03 | `DataException > SocketTimeoutException` | RETRIABLE | RETRIABLE | PIN-02 `SOCKET_TIMEOUT_ROOT` | same |
| S04 | `DataException > UnknownHostException` | RETRIABLE | RETRIABLE | PIN-02 `UNKNOWN_HOST_ROOT_CONTROL` | same |
| S05 | `DataException > RetriableException` | RETRIABLE | RETRIABLE | RSK-02 `DATA_EXCEPTION_OVER_RETRIABLE` (put matrix) | same |
| S06 | `DataException > ServerException(60)` (not retriable) | FAILS_TASK | DLQ(3) | PIN-02 `NON_RETRIABLE_SERVER_CODE_CONTROL` | as S01 |
| S07 | S06 with `additionalRetriableErrorCodes=60` | RETRIABLE | RETRIABLE | `AdditionalRetriableErrorCodesPinUnitTest` (direct layer) | same as S02, the FUT-05 shape |
| S08 | `DataException > SocketException`, `retryOnSocketException=true` | RETRIABLE | RETRIABLE | `WriterFailureClassificationUnitTest` (class `SOCKET`) | same |
| S09 | S08 with `retryOnSocketException=false` | FAILS_TASK | DLQ(3) | same | as S01 |
| S10 | `DataException > IOException("Read timed out after ...")` | RETRIABLE | RETRIABLE | `WriterFailureClassificationUnitTest` `IO_READ_TIMEOUT_PREFIX` | same |
| S11 | `DataException > IOException("plain")` | FAILS_TASK | DLQ(3) | `PLAIN_IO_CONTROL` | as S01 |
| S12 | `DataException > ClickHouseException(V1, code 159)` | RETRIABLE | RETRIABLE | `UtilsTest` (direct `handleException` layer) | same |
| S13 | `RetriableException > RuntimeException` (Phase 46 V2 write-deadline shape, `ClickHouseWriter.java:2255`) | RETRIABLE | RETRIABLE | `InsertWriteStallBoundPinUnitTest` (real writer path) | same |
| S14 | `RetriableException > TimeoutException` (Phase 46 V1 wait bound, `ClickHouseWriter.java:2016`) | RETRIABLE | RETRIABLE | `InsertV1WaitBoundPinUnitTest` | same |
| S15 | `RuntimeException > SocketTimeoutException` (control: not a `DataException`) | RETRIABLE | RETRIABLE | PIN-02 `RUNTIME_EXCEPTION_SOCKET_TIMEOUT_CONTROL` | same |
| S16 | `RuntimeException > IllegalStateException` (control) | FAILS_TASK | DLQ(3) | `RetriableExceptionPassThroughUnitTest` control | as S01 |
| S17 | `DataException > RetriableException > ServerException(60)` | RETRIABLE | RETRIABLE | closest: RSK-02 direct `WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE` (a `RuntimeException` top) | same |
| S18 | `DataException` at conversion (writer never reached) | ESCAPES_RAW | ESCAPES_RAW | PIN-02 `CONVERSION_DATA_EXCEPTION_CONTROL` | task FAILED under both values (`Throwable` branch), no DLQ |

Reading: the "retriable `DataException`" shapes are S02, S03, S04, S05, S07, S08, S10, S12 and S17, each `RETRIABLE` under both values and identical to its non-`DataException` control (S15 for S03), so `errors.tolerance` never moves a retriable row. The existing pins (PIN-02: 14 rows; RSK-02: 22 direct + 16 put rows in `RetriableExceptionPassThroughUnitTest`; `WriterFailureClassificationUnitTest`: 216 rows) already assert the first-failure outcome of S01-S17 and stay green and unchanged under the recommended branch. Shapes whose put-layer outcome under both tolerances I did not find asserted in one place: S07 and S12 (pinned at the `Utils.handleException` layer by `AdditionalRetriableErrorCodesPinUnitTest` and `UtilsTest`) and S17 (direct layer only). The new limit pin adds them at the put layer; the planner re-checks this against the test tree at plan time.

**Changed rows of the classification matrix:** under U-3 B, none: the phase adds rows and changes no existing row (expected-added file lists only the new test cases, `removed` must be empty). Under U-3 A, the existing rows are single-put outcomes and also do not change (a budget needs repeated failures), so even then the delta is added rows only, plus the edited assertions of the WARN text (the user's gate, a visible `removed`/changed pair).

### RTY-02 (FUT-05, current state)

| Item | Finding | Evidence |
|---|---|---|
| Start-up WARN | One WARN per task start naming each `additionalRetriableErrorCodes` entry outside `Utils.DEFAULT_RETRIABLE_ERROR_CODES` and 216, before the connection step; text: `additionalRetriableErrorCodes lists {}, which the connector does not treat as transient. A batch that fails with such a code is redelivered with no retry budget and outside errors.tolerance and the dead letter queue, so a permanent error blocks the partition until the setting is changed.` | [VERIFIED: `ClickHouseSinkTask.java:24-28`, `:136-143`] |
| Pins | `UnvettedRetriableCodesWarnUnitTest` (21 tests, Docker-free, starts a real task against a closed loopback port), `UnvettedRetriableCodesWarnLiveTest`, `AdditionalRetriableErrorCodesPinUnitTest` (19) | [VERIFIED: run] |
| Format check today | `ErrorCodeListValidator` (a `ConfigDef.Validator` registered on the key) calls `parseErrorCodes`, which accepts positive integers only; the `ClickHouseSinkConfig` constructor calls `parseErrorCodes` directly and does not call `CONFIG.parse` | [VERIFIED: `ClickHouseSinkConfig.java:177-217`, `:285`, `:612`] |
| `ClickHouseSinkConnector.validate` | builds a `ClickHouseSinkConfig` and ignores any exception (`catch (Exception e) { return config; }`), so a constructor-level check would surface only at task start, a `ConfigDef`-level check at `validate`/create/update | [VERIFIED: `ClickHouseSinkConnector.java:94-103`] |
| Where Connect validates | `validateConnectorConfig` runs on create/update (`StandaloneHerder.java:207`, `:273`; `DistributedHerder.java:1135`); `DistributedHerder.startConnector` (`:2079-2108`) calls `worker.startConnector` without it | [VERIFIED: 4.1.1 sources] |
| Where FUT-05 is named | `docs/FORK-DELTA.md` item 8 (line 232, "a retry budget or a "remove from default list" setting for retriable codes (FUT-05)"), item 17 (line 250, "(FUT-05)"), item 24 (line 264, title "FUT-05 stays open (published knowingly open)" and body "FUT-05 ... is not closed by this"); `docs/ROLLOUT-v1.7.0_gg7.md:130` "FUT-05 stays open." (tag copy); `docs/ROLLOUT-v1.7.0_gg6.md` row C2 (gg6 note, Phase 48 edits it); `.planning/REQUIREMENTS.md` RTY-02 | [VERIFIED: `grep -rn FUT-05` excluding `milestones/`] |

FUT-05 has two halves in FORK-DELTA's words: a retry budget (settled by RTY-01) and a "remove from default list" setting. The recommended closure records both: no budget (the limit, with the measured Connect facts), no remove-from-default-list setting, no validator, WARN kept; reason: "transient" is the connector's judgment, a validator cannot tell a transient 60 from a permanent one, and a rejecting validator breaks connectors on upgrade (constructor form) or on their next config write (`ConfigDef` form). Edits: item 8 drops FUT-05 from the open list, item 17 and item 24 are rewritten as decisions (keep the item numbers: the gg7 rollout note and the table row at `FORK-DELTA.md:36` cite "Known cost 24"), and the table row at line 36 keeps "leaves `ClickHouseSinkConfig` unchanged".

If the user approves B or C of U-4: the diff is the validator and nothing else in `ClickHouseSinkConfig`; B adds a vetted-set check in `ErrorCodeListValidator.ensureValid` (the vetted set is `Utils.DEFAULT_RETRIABLE_ERROR_CODES` plus 216, the same set the WARN uses, so extract it once), C adds it to `parseErrorCodes`. B: tests pin that `CONFIG.validate` reports the key and a running `ClickHouseSinkTask.start` with the same props still starts; C: tests pin a start-up `ConfigException`/`IllegalArgumentException` for a listed 60. Both need existing assertions reviewed: `AdditionalRetriableErrorCodesPinUnitTest` and `AdditionalRetriableErrorCodesTest` build configs with codes such as 60 and 62 (an existing assertion that would change is the user's gate), and the WARN becomes unreachable for B/C-rejected codes. The rollout note must state the break (B: next config write; C: next task start).

## Common Pitfalls

### Pitfall 1: a red pin that is red for the wrong reason
**What goes wrong:** the LIF-01 pin fails on the unchanged tree because the reflection field name is wrong or `proxy.start()` was skipped, not because `stop()` skipped the writer.
**How to avoid:** assert the specific red: strategy `stop()` called once, writer `stop()` count 0, provider `close()` count 0, MBean still registered, exception is the stub's instance (the measured red above); keep the field names in constants, the way `ProxySinkTaskStopUnitTest` does (`STATE_PROVIDER_FIELD`). Record the red run in evidence before the fix.

### Pitfall 2: the MBean left registered by the red run
**What goes wrong:** the unchanged-tree run leaves the task MBean registered and the next test in the JVM sees a name clash.
**How to avoid:** unique connector name per test and `statistics.unregisterMBean()` in `finally`.

### Pitfall 3: `finally` masks the first exception
**What goes wrong:** with both stops throwing, the proxy's exception replaces the strategy's and nothing is suppressed (measured). **How to avoid:** U-1; assert "both attempted and a `RuntimeException` surfaced" so the pin survives a later move to `keepFirst`.

### Pitfall 4: an "unbounded" pin that a small budget passes
**What goes wrong:** a pin of 3 redeliveries passes against a budget of 5. **How to avoid:** `REDELIVERIES` of 500 (measured red against a budget of 5; the run costs under 0.3 s per shape and tolerance); name the number in the pin text.

### Pitfall 5: claiming more than the measurement says about Connect
**What goes wrong:** FORK-DELTA states the 60 s cadence as a connector fact. **How to avoid:** it is the worker's `offset.flush.interval.ms` (measured 60 s default and 7 s when set to 7000); the connector can only lengthen or shorten it through `SinkTaskContext.timeout`, which it never calls. Measured on the Kafka 3.9 line (`7.9.7-ccs`) and read in 3.9.1, 4.1.1 and 4.4.0; an older runtime was not read [ASSUMED same].

### Pitfall 6: editing a published document in this phase
**What goes wrong:** the gg6 and gg7 rollout notes and the gg7 CHANGELOG section are tag copies (Phase 43 recorded that `docs/ROLLOUT-v1.7.0_gg6.md` carries a "This copy corrects two rows of the copy in tag `v1.7.0_gg6`" header, the only precedent for in-place correction, and Phase 48 edits it again). **How to avoid:** Phase 47 edits FORK-DELTA and (per U-2) starts the gg8 CHANGELOG section; it hands Phase 49 the corrections of `ROLLOUT-v1.7.0_gg7.md` rows F5, R8 and the "FUT-05 stays open." line, as items for the gg8 note.

### Pitfall 7: existing assertions on the budget-coupled texts
**What goes wrong:** a budget silently falsifies `EXPECTED_WARN_FORMAT` and the setting description. **How to avoid:** under U-3 B nothing in those texts changes; under A, stop for the user before editing any asserted text.

### Pitfall 8: operational traps (MEMORY.md and Phases 45-46)
- `TESTCONTAINERS_HOST_OVERRIDE` is the literal `"null"`: run Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Docker is colima profile `vz`; `testUnit` runs with `DOCKER_HOST=unix:///nonexistent.sock`. `-PjavaVersion=21` was used for every run here (the OpenJ9 17 toolchain is the Mockito-crash one; the new pins use no Mockito).
- macOS `sed -i` needs `-i ''`; no `timeout` binary; zsh expands `$M:path` as a modifier; `grep --include=*.java` needs quoting in zsh (the `testUnit --rerun` full run takes about 30 s here, parallel forks).
- Docker bind mounts of `/tmp` into the colima VM are empty (the first compile attempt saw no source): use `docker cp` into a created container. This research ran everything under `/tmp/gsd47` (a `git clone --local` of `5d934376`, sources jars, probes, Docker logs) and removed its containers and network; nothing was committed.

## Anchors (the invariant)

Baseline: 216 data rows; the unedited gate printed `anchor_commits=89 anchor_rows=216 marker_commits=107 anchor_exempt=0 provenance_paths=11 result=PASS` on `5d934376`. Rules (the file's header and the tail-3 checklist): rows change only in the same commit as the code they pin; the gate checks every commit in `PRE_TAG..HEAD` against that commit's copy; row format `path<TAB>fixed-string pattern<TAB>expected count<TAB>backs-ID`, count = matching lines under `grep -cF -e`.

Counts measured on the real tip for the LIF-01 patterns [VERIFIED: `grep -cF` on `ClickHouseSinkTask.java`]: `} finally {` = 0 (the file has no `finally`), `deliveryStrategy.stop();` = 1, `this.proxySinkTask.stop();` = 1. After the minimal change the clone shows `} finally {` = 1 and the other two unchanged at 1.

| Commit | Rows | Delta |
|---|---|---|
| LIF-01 code commit | `ClickHouseSinkTask.java	} finally {	1	LIF-01` and `ClickHouseSinkTask.java	this.proxySinkTask.stop();	1	LIF-01`, under a `# Phase 47 (v1.7.0_gg8): ...` comment line | `anchor_rows` 216 -> **218**; `anchor_commits` +1 per commit that rewrites the anchors file |
| RTY-01 limit branch | none (no `src/main` change) | 0 |
| RTY-02 no-validator branch | none | 0 |
| RTY-01 budget branch (U-3 A) | rows for the counter and the new key in `ProxySinkTask`/`ClickHouseSinkConfig`, counts measured on the real tip before committing | + N, in the same commit |
| RTY-02 validator branch (U-4 B/C) | rows for the validator and the shared vetted-set constant in `ClickHouseSinkConfig` | + N, in the same commit |

Untouched rows that Phase 47 must not move: `ClickHouseSinkTask.java warnOnUnvettedRetriableCodes( 2`, `unvetted.removeAll(Utils.DEFAULT_RETRIABLE_ERROR_CODES); 1` (RSK-03), `public void onPartitionsRevoked(Collection<TopicPartition> partitions) { 1` (RF-8), `Utils.java for (Throwable t = e; t != null; t = t.getCause()) 1` and `t instanceof RetriableException 1` (RSK-02), `ProxySinkTask.java stateProvider.close() 1` (CLS-03), `clickHouseSinkConfig.getAdditionalRetriableErrorCodes() 1` (TAL-04). The standing gate command is the unedited one in `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md` (Gate commands), with `<range end>` = the explicit SHA; self-test `bash .planning/scripts/verify-merge-invariants-selftest.sh` (expected last line `selftest: no failures`). The cumulative/tail form took about 3 minutes here.

## Docker-free vs Docker-needing split

| Work | Docker | How |
|---|---|---|
| LIF-01 pin, its red run, the fix and its green | **Docker-free** | `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock` |
| RTY-01 limit pin (and the matrix rows, the mutation-bite run) | **Docker-free** | same; the mutation run is a throwaway edit in a clone, never committed |
| RTY-02 (no code) | none | FORK-DELTA text and evidence |
| LIF-02 | none | CHANGELOG and FORK-DELTA text, checked by the evidence helper (`grep`-style facts) |
| Live Connect measurement (cadence, no effect of `errors.retry.timeout`/`errors.tolerance`, `DataException` kills the task) | **Docker needed once**, for the evidence entry | recipe below; images already local; `ProbeSink` lives only in `/tmp` |
| Phase gate: full suite on `latest` x V2 | **Docker needed** (src/main changed by LIF-01) | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ... --rerun`, record `SELECT version()` and the image digest first (`latest` drifts) |
| Anchors gate | none | the standing command, once, on an explicit SHA |

Live-measurement recipe (so Phase 47 can re-record it as an evidence entry): create a Docker network; start `confluentinc/cp-kafka:7.9.10` as a single KRaft node (`KAFKA_PROCESS_ROLES=broker,controller`, `CLUSTER_ID` set, replication factors 1); compile `ProbeSink` (a `SinkConnector` and `SinkTask` that print `PROBE put attempt=N t=...ms` and throw `RetriableException` or `DataException` by a `probe.mode` property) inside `confluentinc/cp-kafka-connect-base:7.9.7` with `javac -cp "/usr/share/java/kafka/*"`; copy the jar and the property files into a created container with `docker cp` (bind mounts of `/tmp` are empty under colima) and run `connect-standalone worker.properties c1.properties ...`; read the `PROBE put` lines. The repo's own harness uses `confluentinc/cp-server-connect:7.9.6` (`ConfluentPlatform.java:32`); the base image 7.9.7 was used here because it was local and carries the same Kafka 3.9 line.

## Code Examples

### The RTY-01 limit pin loop (shape of the scratch probe, measured green on the unchanged tree)
```java
// Source: scratch probe Rty01UnboundedProbeUnitTest (this session); the real pin reuses FailingDBWriter and the put seam of
// WrappedDataExceptionClassificationPinUnitTest
Map<String, String> props = new HashMap<>();
props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance);
props.put("errors.retry.timeout", "60");              // the repo-sample value: it must not bound the redelivery
ProxySinkTask task = new ProxySinkTask(new ClickHouseSinkConfig(props), dlq, writer);   // writer.doInsert always throws the shape
for (int i = 1; i <= REDELIVERIES; i++) {              // 500
    assertThrows(RetriableException.class, () -> task.put(records), "redelivery " + i);
}
assertEquals(REDELIVERIES, writer.calls);
assertEquals(0, dlq.size());
```

### Expected-added file and status delta (the changed-row list)
```
python3 -I <phase>/47-testcase-status.py dump  build/test-results/testUnit /tmp/gsd47/status-base.tsv      # on the unchanged tree (before)
python3 -I <phase>/47-testcase-status.py select <phase>/47-lif01-expected-added.tsv PASS <dump-red-run>.tsv .ClickHouseSinkTaskStopUnitTest
python3 -I <phase>/47-testcase-status.py delta  base.tsv after.tsv <phase>/47-lif01-expected-added.tsv        # exit 0 only when removed = {} and added = expected
```
The expected-added file is committed before the code commit, as Phase 42 did; the status helper is read-only. Source: `42-testcase-status.py` docstring (subcommands `dump`, `select`, `delta [--by-class]`, `count`) [VERIFIED: Read].

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| gg7 Known Issues: "a task-level `stop()` that has no `try`/`finally` around the delivery strategy's `stop()`" | `try`/`finally` (LIF-01) | this phase | closes one of five residuals of the helper-client closing |
| FUT-05 open (FORK-DELTA items 8, 17, 24; gg7 note "FUT-05 stays open.") | closed with a recorded decision (RTY-02) | this phase | the gg8 note states the closure, the gg7 and gg6 notes stay tag copies |
| "retriable `DataException`: a decision on changing them is open" (gg6 and gg7 Known Issues) | recorded limit with Connect's measured behaviour (RTY-01) | this phase | `errors.tolerance` and `errors.retry.timeout` do not reach a `RetriableException` from `put` |

**Deprecated/outdated:** the roadmap's "stops every delivery strategy" (a task has one strategy); the brief's "decompile the 4.1.1 jars from the Gradle cache" (`connect-runtime` is not cached; sources jars were read instead).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The production Connect workers behave like the Kafka 3.9 line measured and the 3.9.1/4.1.1/4.4.0 sources read (an older runtime was not read) | RTY-01, Pitfall 5 | Low: `deliverMessages` is textually identical across the three versions read; the FORK-DELTA text names the versions |
| A2 | Operators in the wild copy the samples and set `errors.retry.timeout` to `"60"` | RTY-01 (why no key is free) | Low-medium: affects only the argument that reusing the key would break connectors; the key is Connect-owned with default 0 regardless |
| A3 | The GitHub release body of `v1.7.0_gg7` is not generated from `CHANGELOG.md` (no workflow or script reads it) | LIF-02 | Low: matters only if option A (gg7 in place) is chosen |
| A4 | No shipped strategy's `stop()` can throw other than through a logging backend failure or an `Error` | LIF-01 | Low: read from the three bodies; FORK-DELTA item 13 already says "no strategy's `stop()` throws today" |
| A5 | A running connector does not re-validate its stored config at worker start (so a `ConfigDef` validator is latent) | U-4 | Low: read from `StandaloneHerder` and `DistributedHerder`; not run |

## Open Questions

1. **Should the LIF-01 pin also parameterize over the three real strategies?**
   - Known: a stub covers the throw; the real strategies' `stop()` bodies are covered only by reading and by whatever buffered tests exist.
   - Recommendation: add three cheap Docker-free rows (a real strategy built over a `ChunkFlusher` on an `InMemoryDBWriter`-backed `ProxySinkTask`, buffered records, `stop()` reached through the task) so "every strategy" is literal; additive, no production change.
2. **Does the user want the worker-level cadence in FORK-DELTA?**
   - Known: `offset.flush.interval.ms` (60 s default) sets it; it is a worker property, not a connector one.
   - Recommendation: record it with the measurement, state that it is Connect's, and do not recommend changing it.
3. **`close(partitions)` has the same missing `finally`** (`ClickHouseSinkTask.java:161-169`). Out of scope (a diff beyond LIF-01); list it in FORK-DELTA item 13 as a recorded residual if the user wants it visible.
4. **Where do the Phase 49 handoffs live?** Recommend a short list at the end of the phase SUMMARY: rewrite ROLLOUT gg7 rows F5 and R8 and the "FUT-05 stays open." line in the gg8 note (corrections of a tag copy), the gg8 CHANGELOG completion and dating, and the FORK-DELTA numbers refresh.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | live Connect measurement record, full suite | yes | `colima list`: `vz` Running (8 CPU, 16 GiB) | `testUnit` with `DOCKER_HOST=unix:///nonexistent.sock` for everything Docker-free |
| `confluentinc/cp-kafka:7.9.10`, `confluentinc/cp-kafka-connect-base:7.9.7` | live Connect probe | yes (local images) | Kafka 3.9 line, `connect-runtime-7.9.7-ccs` | — |
| `clickhouse/clickhouse-server:latest` | full suite on `latest` x V2 | yes (local) | re-record `SELECT version()` and the image digest before the gate (46 recorded `26.10.3.7`, `sha256:739e099a...`) | — |
| JDK | Gradle | yes | OpenJDK 21 default; the toolchain's OpenJ9 17 is crash-prone | `-PjavaVersion=21` |
| Gradle wrapper | build | yes | 9.8.2, `--offline` works (all dependencies cached) | — |
| Maven Central | `connect-runtime` sources jars (read only) | yes | 3.9.1, 4.1.1, 4.4.0 fetched | — |
| `python3 -I` | evidence helpers | yes | system | — |

**Missing dependencies with no fallback:** none.

## Validation Architecture

Skipped: `.planning/config.json` has `"nyquist_validation": false` [VERIFIED: `config.json:24`]. The phase gate is the roadmap's: cache-busted `testUnit` with Docker unreachable (baseline 2181 tests, 0 failures, 2 skipped; expected after LIF-01 plus the RTY-01 pin: 2181 plus the new cases, no removed row), a full suite on `latest` x V2 (src/main changes), the invariant script once on an explicit SHA (`anchor_rows=218` after LIF-01 under the recommended branch), the new LIF-01 pin seen red on the unchanged tree and green after the fix. Quick command: `DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --offline --console=plain -PjavaVersion=21 testUnit --tests '*ClickHouseSinkTaskStopUnitTest' --tests '*RetriableRedeliveryLimitPinUnitTest' --tests '*ProxySinkTaskStopUnitTest' --tests '*UnvettedRetriableCodesWarnUnitTest' --tests '*WrappedDataExceptionClassificationPinUnitTest' --tests '*RetriableExceptionPassThroughUnitTest'` (the last four are green today: 4, 21, 14, 38 tests).

## Security Domain

`security_enforcement` is enabled (`"security_enforcement": true`, `"security_asvs_level": 1`) [VERIFIED: `config.json:48-49`].

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V7 Error Handling and Logging | yes | No new log line carries a record value; the LIF-01 change adds none; a budget (if taken) would log counts and a time only, never `getMessage()` of a cause (STATE.md standing rule: writer failure text stays value-free) |
| V12 / availability (resource exhaustion) | yes | LIF-01 is the leak fix: a skipped proxy stop leaves the writer's executors, helper client, MBean and Keeper client allocated for the worker's life; the unbounded redelivery is recorded as a limit, not mitigated |
| V14 Configuration | yes (RTY-02) | No new key under the recommended branches; a validator (U-4 B/C) is the one configuration-surface change and is gated |
| V2, V3, V4, V5, V6 | no | no auth, session, access-control, input-validation or crypto change |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Resource leak on shutdown when one stop step throws | Denial of service | `try`/`finally` in `ClickHouseSinkTask.stop()` (LIF-01); `keepFirst` already in `ProxySinkTask.stop()` |
| A permanently failing batch redelivered forever, blocking the partition | Denial of service | Recorded limit (RTY-01); the opt-in budget design is written out; the start-up WARN stays for unvetted codes |
| Timed-out insert partially committed then redelivered | Tampering (duplication) | Documented (Phase 46 finding 7); `exactlyOnce=true` reuses the dedup token; no change to the `BEFORE`/`AFTER` sequence in this phase |
| Operator config that fails only on the next write (validator at `ConfigDef` level) | Denial of service (operational) | Not taken under U-4 A; if taken, the rollout note names it |

## Project Constraints (from CLAUDE.md)

- `AGENTS.md`: Java 17, Google Java Style via Spotless (46 pre-existing violating files, so format per touched file and do not run a repo-wide `spotlessApply`); only add tests, never delete or weaken; `Processing.java`, `KeeperStateProvider.java`, `ClickHouseWriter.java`, `ClickHouseSinkConfig.java` are high-risk (none is touched under the recommended branches); preserve the `BEFORE`/`AFTER` sequence and never change batch composition after `BEFORE` (LIF-01 and the limit pin touch neither); SLF4J levels (TRACE per record, DEBUG per batch, INFO lifecycle); no record values in logs; do not upgrade dependencies; `VERSION` and `CHANGELOG.md` are Phase 49's except the gg8 `stop()` bullets of U-2; no commits without the user's word beyond the GSD workflow's own per-plan commits.
- `.claude/CLAUDE.md`: behaviour preservation (code limited to genuine bug fixes; the fork runs in production), test target `latest` x client V2, additive and rebasable changes, no Mockito inline mocks, GSD workflow enforcement (`/gsd-execute-phase`).
- STATE.md standing rules that bind this phase: a main-code change outside the phase's declared scope stops for the user; each code change is its own commit and revert unit behind its own gate; two-run gates use `--rerun`; every pin red-first except characterization pins, whose teeth are shown by a mutation run.

## Sources

### Primary (HIGH confidence)
- This session's runs: throwaway `git clone --local` of `5d934376` in `/tmp/gsd47/repo` (probes `Lif01ProbeUnitTest`, `Rty01ProbeUnitTest`, `Rty01UnboundedProbeUnitTest` kept in `/tmp/gsd47/probes`; a scratch `try`/`finally` edit and a scratch budget mutation, both reverted); the standing tail gate and a cache-busted `testUnit` on the unchanged clone; a real Connect worker plus KRaft Kafka in Docker (colima `vz`), removed afterwards
- Sources read: `connect-runtime` 4.1.1 (`WorkerSinkTask`, `errors/RetryWithToleranceOperator`, `errors/WorkerErrantRecordReporter`, `ConnectorConfig`, `AbstractHerder`, `standalone/StandaloneHerder`, `distributed/DistributedHerder`), `WorkerSinkTask.deliverMessages` and `poll` diffed against 3.9.1 and 4.4.0
- Repo files read: `ClickHouseSinkTask.java`, `ProxySinkTask.java`, `ChunkFlusher.java`, `DeliveryStrategy.java`, the three strategies, `Utils.java`, `Processing.java:40-130`, `ClickHouseSinkConfig.java` (60-100, 160-220, 270-300, 400-420, 585-616), `ClickHouseSinkConnector.java:80-110`, `ClickHouseWriter.java` (`stop`, `withheldJacksonRoot`, `sameKindStandIn`, throw sites), `InMemoryDBWriter.java`, `CHANGELOG.md:1-80, 150-182`, `docs/FORK-DELTA.md` items 8, 13, 17, 24 and the row at line 36, `docs/ROLLOUT-v1.7.0_gg7.md` rows F3, F5, R4, R7, R8 and the end of section F, `docs/ROLLOUT-v1.7.0_gg6.md` row C2, `ProxySinkTaskStopUnitTest.java`, `UnvettedRetriableCodesWarnUnitTest.java`, `WrappedDataExceptionClassificationPinUnitTest.java`, `RetriableExceptionPassThroughUnitTest.java`, `WriterFailureClassificationUnitTest.java` (head), `AdditionalRetriableErrorCodesPinUnitTest.java` (head), `fork-anchors.txt`, `MERGE-CHECKLIST-v1.7.0-tail-3.md`, `46-RESEARCH.md`, `46-CONTEXT.md`, `42-testcase-status.py`, `REQUIREMENTS.md`, ROADMAP Phase 47-49 sections and the cross-phase tables, `.planning/config.json`

### Secondary (MEDIUM confidence)
- `46-RESEARCH.md` "Server state after aborts" (timed-out insert partially committed), cited not re-run

### Tertiary (LOW confidence)
- How operators set `errors.retry.timeout` in production (A2); behaviour of Connect runtimes older than 3.9 (A1)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH (no change)
- LIF-01 mechanics and pin: HIGH (red and green measured through the real classes)
- CHANGELOG text and placement options: HIGH for the text and the convention evidence; the choice is the user's
- Connect behaviour and cadence: HIGH (source in three versions plus a live run on the 3.9 line)
- Classification matrix: HIGH (18 shapes x 2 values measured at the put layer)
- Budget design: MEDIUM (a counter was mutation-tested for the pin's teeth only; the design itself was not built)
- Validator consequences: MEDIUM-HIGH (herder paths read, not run)

**Research date:** 2026-11-05
**Valid until:** 2026-11-12 (re-record the `latest` image digest and `SELECT version()` before the full suite; re-run the red pin on the unchanged tree at plan time)
