<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md `## Implementation Decisions` reads, verbatim: "All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions."

The ROADMAP Phase 42 text (goal, 4 success criteria, Research flag, Note) is the binding scope. The user's decision of 2026-11-01 that the three edits land one commit each, tests first, each behind its own gate, is part of the phase goal in CONTEXT.md `<domain>`.

### Claude's Discretion
All implementation choices (see above).

### Deferred Ideas (OUT OF SCOPE)
"None — discuss phase skipped."

Out of scope by the ROADMAP Note, verbatim: "Any diff beyond RSK-01's one line, RSK-02's pass-through or RSK-03's WARN stops for a user decision." And from the Research flag: "A state-key change is out of scope."
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RSK-01 | `Processing.doLogic` line 137 reads `originalTopic()` (OI-35-1); the state key and KeeperMap rows do not change, all 14 `ProcessingTest` cases are unchanged, and the remaining `onPartitionRemoved` mismatch is recorded. | Flow trace of the line-137 value (below): it reaches only `StateRecord.originalTopic`, read in exactly one place, the key of the in-memory `lastInsertedOffsets` map. Measured by a before/after trace probe: the whole state-provider call sequence is identical; only that key differs. 14/14 `ProcessingTest` pass on the edited scratch tree. |
| RSK-02 | `Utils.handleException` passes a `RetriableException` through as retriable; the set of changed rows in the "184-case" classification matrix is named and equals the actual diff. | The matrix is 216 executions today, not 184. Probed before/after on two scratch variants: changed existing rows = none (0 of 216, 0 of 14 PIN-02, 0 of 15 TMO, 0 of 1757 `testUnit`, 0 of 2355 full suite for the chain variant). The added rows and their outcomes are tabulated. |
| RSK-03 | Start-up logs a WARN for non-transient codes in `additionalRetriableErrorCodes`; no validator and no retry budget; `ClickHouseSinkConfig` is unchanged. | Recommended site `ClickHouseSinkTask.start`, recommended definition "built-in set plus 216", prototype run Docker-free through a failed `start()`: one WARN naming the code, none for built-in codes or 216. |
| RSK-04 | OI-35-2 and OI-35-3 are documented as limits (injective rename, partition-preserving SMT); the state key does not change. | Draft FORK-DELTA wording below, including a hazard the probes found that the wording must state. |
</phase_requirements>

# Phase 42: Isolated High-Risk Edits - Research

**Researched:** 2026-11-02
**Domain:** Kafka Connect sink connector internals (exactly-once state machine, retry classification, start-up logging), edited under a three-commit, three-gate discipline
**Confidence:** HIGH for RSK-01 and RSK-02 (code read and probed on scratch copies); MEDIUM for RSK-03's definition of "transient" (a judgment, see Assumptions Log)

## Summary

**No STOP-level finding.** The RSK-01 line-137 value does not reach the state key, a KeeperMap row or table routing. The RSK-02 change flips no existing classification row. Neither research flag triggers its stop rule. Three things need the user's eye before or at plan approval, none of them a stop (details in Open Questions): (1) RSK-02 scope: a bare-only pass-through (`e instanceof RetriableException`) is a production no-op, and only a pass-through that looks down the cause chain fixes the real defect, so this research recommends the chain form; (2) RSK-03's meaning of "transient" is a judgment, recommended as "the connector's built-in retriable set plus code 216"; (3) after RSK-01, a non-injective rename or a partition-changing SMT with `reportInsertedOffsets=true` on the Direct path reports an offset taken from the shared state range, which can exceed the source topic's own position. This was measured on scratch; the limits wording (RSK-04) must say it.

RSK-01. `originalTopic` (line 137) is passed as the sixth argument of every `StateRecord` constructed in `doLogic` and set on the record read back from the provider (line 164). Its only reader in `src/main` is `BaseStateProviderImpl.onStateUpdate`, which uses it as the topic half of the key of an in-memory `lastInsertedOffsets` map. `KeeperStateProvider` never reads it: the row is `(key, minOffset, maxOffset, state)` with `key` built from the transformed topic. A trace probe over four scenarios (injective rename with replay and overlap, many-to-one rename, partition-changing SMT, no SMT) shows the full `getStateRecord`/`setStateRecord`/`onStateUpdate` sequence, keys, ranges, BEFORE/AFTER order, table topics and offsets identical before and after; the only difference is the key of `lastInsertedOffsets`.

RSK-02. Today `handleException` has no `RetriableException` handling. `Processing.doInsert` wraps every writer failure in `RuntimeException(queryId, e)`, so a writer-thrown `RetriableException` (the `async=true` insert timeout, the DDL-propagation timeout) never reaches Connect as retriable: it fails the task under `errors.tolerance=none` and goes to the DLQ under `all`. A bare-only pass-through changes none of those production shapes. The chain form (a `RetriableException` anywhere in the cause chain, after the server-error block) turns them retriable. Neither form changes any pre-existing pinned row.

**Primary recommendation:** Three plans plus one gate plan, in the order RSK-01, RSK-02, RSK-03. Each: tests that compile and fail on the unchanged tree for the stated reason, then the production edit, then scoped Spotless, the anchors mirror, cache-busted `testUnit` and a full `latest` x V2 suite, with a per-testcase before/after status diff as the "changed rows equal the list" evidence. Run the 40-minute invariants script once, at the end, alone.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Exactly-once state key and BEFORE/AFTER flags | `sink/processing/Processing` + `sink/state/provider/*` | ClickHouse Keeper (KeeperMap table) | `Processing.doLogic` builds the key from the transformed topic and partition; the provider persists `(key, minOffset, maxOffset, state)`. Must not move. |
| Inserted-offset reporting (`reportInsertedOffsets`) | `sink/state/BaseStateProviderImpl` (`lastInsertedOffsets`) | `sink/DirectDeliveryStrategy.preCommit`, `ClickHouseSinkTask.close` | In-memory only; keyed for Connect's consumer assignment, so it must carry the consumer's coordinates. This is what RSK-01 changes. |
| Table routing | `sink/db/ClickHouseWriter` (reads `first.getSinkRecord().topic()`) | `Utils.getTableName` | Independent of `Processing`'s local; unchanged by RSK-01. |
| Retry classification | `util/Utils.handleException` | `ProxySinkTask.put`, `ChunkFlusher.putDirect` (callers) | One static, stateless function decides retriable / data exception / wrap / return. RSK-02 edits only this. |
| Start-up warnings | `sink/ClickHouseSinkTask.start` | `ClickHouseSinkConfig` (read only) | Config is parsed and validated there; the task already logs a start-up WARN (`bufferFlushTime` ignored). RSK-03 adds one there, with `ClickHouseSinkConfig` unchanged. |
| Records of limits and rows | `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt` | Phase 44 (rollout note, raw numbers) | Rows land in the code commit that causes them; raw numbers are refreshed once in Phase 44. |

## Standard Stack

No library is added or changed. The phase edits three existing files and adds tests on the existing stack.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit Jupiter | 5.11.3 [CITED: `./.claude/CLAUDE.md` Technology Stack] | Parameterized and plain tests, no tag so they stay in `testUnit` | Project standard; `testUnit` excludes tag `clickhouse` (`build.gradle.kts:283`, `excludeTags("clickhouse")`) |
| Gradle wrapper | 9.8.2 [VERIFIED: `gradle/wrapper/gradle-wrapper.properties`, `distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.2-bin.zip`] | Test runs | `./.claude/CLAUDE.md` still says 7.4.2; the wrapper is authoritative |
| ClickHouse Java client V2 | 0.11.0 [VERIFIED: `gradle/libs.versions.toml`, `clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)`] | Production client; not touched | Phase 36 override |
| JDK | OpenJDK 21.0.12.1 Homebrew HotSpot [VERIFIED: `java -version`] run as `-PjavaVersion=21` | Test JVM | Avoids the OpenJ9 Mockito crash recorded in project memory |

### Supporting (test fixtures to reuse, none new)
| Fixture | Location | Use |
|---------|----------|-----|
| `LogCapture` | `src/test/java/com/clickhouse/kafka/connect/sink/util/LogCapture.java` (`LogCapture.attach(Class)`, `events("WARN")`, `event.message()`) | WARN assertions for RSK-03; reload4j is already on the test classpath |
| `RenamedTopicRecords`, `RecordingDBWriter` | `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/` | Renaming-SMT records (`renamed`, `withTransformedPosition`) and an insert-recording writer, as `BufferedOffsetCommitUnitTest` uses them |
| `InMemoryState`, `InMemoryDBWriter` | `src/main/java/.../sink/state/provider/InMemoryState.java`, `.../sink/db/InMemoryDBWriter.java` | Docker-free `Processing` and `ProxySinkTask` (via the package-private `ProxySinkTask(config, errorReporter, DBWriter)` constructor) |
| `ServerExceptions.create(code, msg, http)` | `src/test/java/.../sink/util/ServerExceptions.java` | Server-error shapes for RSK-02 controls |
| `SchemalessTestData.createPrimitiveTypes(topic, partition, n)` | test helper used by PIN-02 | Records for `put` |

**Do not use Mockito, `mockStatic` or `mockConstruction`** in the new tests (project note for this milestone: OpenJ9 crash); use subclass seams as the Phase 39 pins do.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Chain pass-through in `Utils` | Bare `e instanceof RetriableException` only | Literal reading of the roadmap sentence, but a production no-op (see RSK-02). Rows identical, value zero. |
| "Built-in set plus 216" as the transient set | A curated deny-list of permanent codes (60, 62, 81, ...) | Needs per-code judgment (60 `UNKNOWN_TABLE` can be transient while a table is created); false negatives silent. |
| WARN in `ClickHouseSinkTask.start` | WARN in `ProxySinkTask` constructor or `ClickHouseSinkConfig` | The constructor runs in many tests (noise); `ClickHouseSinkConfig` must stay unchanged. |

**Installation:** none.

## Package Legitimacy Audit

No external package is installed or upgraded in this phase (AGENTS.md: dependency upgrades need the user's say-so, and none is needed). Audit table not applicable; the `package-legitimacy` gate is not run.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Kafka Connect worker
  |  SinkRecord (topic() = transformed, originalTopic() = consumer's)
  v
ClickHouseSinkTask.start(props) --[RSK-03: WARN once, before ProxySinkTask]--> log
ClickHouseSinkTask.put -> DeliveryStrategy
  |-- Direct:  ChunkFlusher.putDirect --+
  |-- Buffered strategies --------------+--> ProxySinkTask.put
                                              |  Record.convert (topic = transformed, strips db prefix if split)
                                              v
                                       Processing.doLogic(records of one transformed topic-partition)
                                         topic         = Record offset-container topic  (STATE KEY, table topic)
                                         originalTopic = SinkRecord.topic()  --RSK-01--> SinkRecord.originalTopic()
                                              |
                       +----------------------+------------------------------+
                       v                                                     v
        StateProvider.getStateRecord/setStateRecord                 DBWriter.doInsert
          key = "<topic>-<partition>"  (never originalTopic)          table = Record.getTopic() / first.getSinkRecord().topic()
          Keeper row = (key, minOffset, maxOffset, state)             failure -> RuntimeException(queryId, e)   (Processing.doInsert:72)
          BaseStateProviderImpl: AFTER -> lastInsertedOffsets[(originalTopic, partition)] = max+1
                                              |                                  |
                       +----------------------+                                  v
                       v                                          back in ProxySinkTask.put:139 / ChunkFlusher.putDirect:76
        DirectDeliveryStrategy.preCommit (reportInsertedOffsets=true):         Utils.handleException(e, ...)   <-- RSK-02
          keys filtered by currentOffsets (Connect's consumer keys)              server code -> RetriableException
        ClickHouseSinkTask.close -> onPartitionRemoved(Connect's keys)           [RSK-02: RetriableException in chain -> retriable]
                                                                                 DataException / socket / IO rules, then tolerance
```

### Recommended Project Structure (touched files only)

```
src/main/java/com/clickhouse/kafka/connect/
├── sink/processing/Processing.java        # RSK-01: line 137, one line, no import change
├── util/Utils.java                        # RSK-02: pass-through block in handleException
└── sink/ClickHouseSinkTask.java           # RSK-03: private static warn helper + one call in start()
src/test/java/com/clickhouse/kafka/connect/sink/
├── <RSK-01 pin class>.java                # new, Docker-free, extends nothing
├── <RSK-02 pin class>.java                # new, Docker-free, extends nothing
└── <RSK-03 pin class>.java                # new, Docker-free, extends nothing
docs/FORK-DELTA.md                          # rows and Known-cost items (see wording below)
.planning/scripts/fork-anchors.txt          # rows in the same commit as the code
```

Existing test classes (`ProcessingTest`, `WriterFailureClassificationUnitTest`, `WrappedDataExceptionClassificationPinUnitTest`, `InsertStallBoundPinUnitTest`, `AdditionalRetriableErrorCodesPinUnitTest`, `UtilsTest`, `ProcessingRenamedTopicStateKeyUnitTest`) are not edited ("Only add new tests", AGENTS.md).

### Pattern 1: RSK-01 line-137 flow, settled from the code

**What:** The value read at line 137 is used in 16 places and read in one.
[VERIFIED: `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java:137`] `String originalTopic = record.getSinkRecord().topic();`
[VERIFIED: `Processing.java:164`] `stateRecord.setOriginalTopic(originalTopic); // restore original topic`
[VERIFIED: `Processing.java:169`] `stateProvider.setStateRecord(new StateRecord(topic, partition, rangeContainer.getMaxOffset(), rangeContainer.getMinOffset(), BEFORE_PROCESSING, originalTopic));` (the same trailing argument at lines 171, 182, 184, 189, 203, 213, 215, 234, 236, 239, 241, 248, 250).
[VERIFIED: `StateRecord.java:11`] `public StateRecord(String topic, int partition , long maxOffset, long minOffset, State state, String originalTopic) {` and `equals` compares `this.topic`, `this.partition`, `this.state`, min and max offset only; `toString` omits it.
[VERIFIED: `grep -n "getOriginalTopic" -r src/main`] the only reader: `BaseStateProviderImpl.java:27` `lastInsertedOffsets.put(new TopicPartition(stateRecord.getOriginalTopic(), stateRecord.getPartition()),`. The map is an in-memory `ConcurrentHashMap` (`BaseStateProviderImpl.java:16`).
State key, verbatim: `TopicPartitionContainer.getTopicAndPartitionKey` is `return String.format("%s-%d", topic, partition);`; `KeeperStateProvider.java:158` `String key = stateRecord.getTopicAndPartitionKey();`; `KeeperStateProvider.java:160` `String insertStr = String.format("INSERT INTO `%s` SETTINGS wait_for_async_insert=1 VALUES ('%s', %d, %d, '%s');", csc.getZkDatabase(), key, minOffset, maxOffset, state);`; the table is `KeeperStateProvider.java:86-87` `(`key` String, minOffset BIGINT, maxOffset BIGINT, state String)` and ` ENGINE=KeeperMap('%s') PRIMARY KEY `key`;`. Reading a row: `KeeperStateProvider.java:130` `return new StateRecord(topic, partition, 0, 0, State.NONE, topic);` (the local `topic`, not any original topic).
Table routing is separate: `ClickHouseWriter.java` lines 1322, 1430, 1567, 1606, 1721, 1758 each read `String topic = first.getSinkRecord().topic();` and `RecordConvertor.java:10` reads `String topic = sinkRecord.topic();` for the record's offset-container topic. RSK-01 touches none of these.
**Consequence:** the edit changes exactly one thing, the topic half of the `lastInsertedOffsets` key, which matters only where that map is read: `DirectDeliveryStrategy.preCommit` with `reportInsertedOffsets=true` and `ClickHouseSinkTask.close` -> `onPartitionRemoved`. [VERIFIED: `DirectDeliveryStrategy.java:44` `if (!clickHouseSinkConfig.isReportInsertedOffsets()) {`; `ClickHouseSinkConfig.java:99` `reportInsertedOffsetsDefault = Boolean.FALSE`.] With the default config nothing changes.
**Upstream status:** `git diff --quiet upstream/main HEAD -- .../Processing.java` exits 0 (byte-identical to upstream `9bed6f79`, also to `v1.7.0`), so RSK-01 makes `Processing.java` a fork-delta file for the first time (new FORK-DELTA row, new anchors rows).

**Measured trace (scratch probe, `InMemoryState` wrapped in a recording provider, `RecordingDBWriter`):** four scenarios, base tree vs a tree with only line 137 changed. All `get`/`set`/`onStateUpdate` calls (key, state, min, max), writer table topics and offsets are identical. Only these lines differ:

| Scenario | `lastInsertedOffsets` before | after RSK-01 | `onPartitionRemoved` with Connect's original keys, before | after |
|----------|------------------------------|--------------|-----------------------------------------------------------|-------|
| injective rename `t-dev` -> `t`, partition 0, NEW, replay (SAME), overlap, NEW | `{t-0=25}` | `{t-dev-0=25}` | `{t-0=25}` (stale, never cleared) | `{}` |
| many-to-one `a-dev`(0..9) + `b-dev`(100..109) -> `t`, one batch, a first | `{t-0=110}` | `{a-dev-0=110}` | not run | not run |
| partition change: original partitions 3 and 5 -> transformed partition 0 | `{t-0=45}` | `{t-dev-0=45}` | `{t-0=45}` | `{t-dev-0=45}` (not cleared by `t-dev-3`, `t-dev-5`) |
| no SMT control | `{t-0=10}` | `{t-0=10}` | `{}` | `{}` |

State keys in every trace row stayed `t-0`; the writer saw `t@0..` offsets in both trees. [VERIFIED: scratch probe `Rsk01FlowProbe`, run on `/tmp/gsd42/scratch-base` and `scratch-varR`; outputs `/tmp/gsd42/out-flow-base.tsv`, `out-flow-varR.tsv`; scratch is not durable.]

**What the edit does not fix (the RSK-01 "remaining mismatch", and the hazard):**
- The partition half of the key is still the transformed partition (`stateRecord.getPartition()`), and the value is the shared state range's max offset + 1. Under a partition-changing SMT `onPartitionRemoved` (called with Connect's original partition) cannot clear the entry.
- Under a many-to-one rename the entry is `(first record's original topic, partition) -> max offset of the shared range + 1`. In the scratch probe `a-dev` has only offsets 0..9 (own next offset 10) yet reports 110. Before RSK-01 the key `t-0` matched no assigned partition (`preCommit` filters to `currentOffsets` keys), so nothing was committed. After it, the key is an assigned partition, so Connect would commit 110. Whether a commit beyond the consumer position is accepted is derived from Kafka consumer semantics, not measured [ASSUMED]. The wording below states the limit; the repository pin makes the first half "measured" once it lands.
- The state store itself (`InMemoryState` map, KeeperMap rows) is never touched by `onPartitionRemoved`; by design.

### Pattern 2: RSK-02 `handleException` pass-through (chain form)

**What:** One block after the server-error block and before the `DataException` check.
**When to use:** this form, unless the user chooses the bare form (see Open Questions).
**Example:**
```java
// Source: Utils.java:93-109 as read this session, with the block inserted; probed in scratch-varB
        Exception rootCause = Utils.getRootCause(e, true);
        if (isServerError(rootCause)) { /* unchanged, Utils.java:94-103 */ }

        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof RetriableException) {
                throw t == e ? (RetriableException) e : new RetriableException(e);
            }
        }

        //High-Level Explicit Exception Checking   // Utils.java:105
        if (e instanceof DataException && !errorsTolerance) {
```
[VERIFIED: `Utils.java:93` `Exception rootCause = Utils.getRootCause(e, true);`, `:94` `if (isServerError(rootCause)) {`, `:105` `//High-Level Explicit Exception Checking`, `:106` `if (e instanceof DataException && !errorsTolerance) {`.] The existing code throws `(DataException) e` at `Utils.java:108`; rethrowing the same instance mirrors it. A wrapped match is wrapped once, like the four other `new RetriableException(e)` sites.

**Why after the server block and before the `DataException` check:** server-code classification keeps precedence (it logs `Error code [...] wasn't in the acceptable list.` first); placing the block before the `DataException` check makes the outcome independent of `errors.tolerance`, as the existing root-cause rules are (probed: with the block after the check, `DataException(RetriableException)` would be a `DataException` under `none` and retriable under `all`).

**Measured before/after (scratch probe, direct `handleException` calls; "same" = the input instance is thrown, "wrap" = new `RetriableException(e)`):**

| Shape | Before (`none` / `all`) | Bare form | Chain form |
|-------|-------------------------|-----------|------------|
| S1 `RetriableException(no cause)` | RUNTIME_WRAP / RETURNS | retriable, same | retriable, same |
| S2 `RetriableException(TimeoutException)` | RUNTIME_WRAP / RETURNS | retriable, same | retriable, same |
| S3 `RuntimeException(q, RetriableException(TimeoutException))` (what `Processing.doInsert` makes of `awaitInsertResponse`) | RUNTIME_WRAP / RETURNS | unchanged | retriable, wrap |
| S4 `RuntimeException(q, RetriableException(no cause))` (DDL-propagation timeout) | RUNTIME_WRAP / RETURNS | unchanged | retriable, wrap |
| S5 `DataException(RetriableException(...))` | DATA_EXCEPTION / RETURNS | unchanged | retriable, wrap |
| S6 `RetriableException(server 60)` | RUNTIME_WRAP / RETURNS | retriable, same | retriable, same |
| S7 `RuntimeException(q, RetriableException(server 60))` | RUNTIME_WRAP / RETURNS | unchanged | retriable, wrap |
| S8 `RetriableException(RuntimeException(q, SocketTimeout))` (the second handling in `ChunkFlusher.putDirect`) | retriable, wrap (both) | retriable, same | retriable, same |
| S9 `RetriableException(RuntimeException("Number of records: 3", RuntimeException(q, IllegalStateException)))` | RUNTIME_WRAP / RETURNS | retriable, same | retriable, same |
| S10 `RuntimeException(q, IllegalStateException)` control | RUNTIME_WRAP / RETURNS | unchanged | unchanged |
| S11 `RuntimeException(q, server 241)` control | retriable, wrap (both) | unchanged | unchanged |

Through the real pipeline (`ProxySinkTask.put` and `ChunkFlusher.putDirect`, writer throwing, both tolerances):

| Writer throws | Before | Bare form | Chain form |
|---------------|--------|-----------|------------|
| W1 `RetriableException(TimeoutException)` (insert timeout with `async=true`) | FAILS_TASK (`none`), DLQ (`all`), both entries | unchanged | RETRIABLE, no DLQ record, all four combinations |
| W2 `RetriableException(no cause)` (DDL propagation timeout) | FAILS_TASK / DLQ | unchanged | RETRIABLE, no DLQ record |
| W3 `RetriableException(SocketTimeoutException)` | RETRIABLE | unchanged | unchanged |
| W4 `DataException(RetriableException)` | FAILS_TASK / DLQ | unchanged | RETRIABLE |

[VERIFIED: scratch probe `Rsk02DirectProbe`, outputs `/tmp/gsd42/out-direct-base.tsv`, `-varA.tsv`, `-varB.tsv`. The bare form is `scratch-varA`, the chain form `scratch-varB`.]

**Existing-rows result (the expected changed-row list, written before the edit):** NONE. Measured on both variants:
- `WriterFailureClassificationUnitTest`: 216 executions (25 failure kinds: 25 x 8 = 200, plus 8 socket-retry rows, plus 8 ignore-partitions rows). Outcome TSV of all 216 identical before and after, both variants. [VERIFIED: probe `Rsk02MatrixProbe`, `# counts matrix=208 ignorePartitions=8`; JUnit XML counts 216 testcases.] **The roadmap's "184" is stale:** 184 was Phase 22's count with 21 kinds; Phase 27 (WRT-16, commit `108f79a5`) added four Jackson kinds (`JACKSON_ALONE`, `JACKSON_OVER_SOCKET_TIMEOUT`, `JACKSON_OVER_TIMEOUT_TEXT`, `PLAIN_IO_CONTROL`), 21 + 4 = 25 kinds, 216 executions. Record this in the plan's evidence; do not edit the matrix to make it 184.
- `WrappedDataExceptionClassificationPinUnitTest` (PIN-02): 14 rows, none changed. `InsertStallBoundPinUnitTest` (TMO): 15 tests, none changed. `UtilsTest` 34, `UtilsNullMessageClassificationUnitTest` 6, `AdditionalRetriableErrorCodesPinUnitTest` 19, `ProcessingTest` 14: none changed.
- Whole `testUnit` (1757 tests, 2 skipped): per-testcase status identical before and after, both variants (sorted comparison; the raw order differs because `UtilsTest` "code N" dynamic tests iterate a `Set`).
- Full suite on ClickHouse `latest` x V2, chain form only (it is the superset of the bare form's effect): 2355 tests, 0 failures, 18 skipped, `BUILD SUCCESSFUL in 2m 49s`, matching the Phase 41 baseline of 2355/18. This includes `WriterErrorPathLiveTest` (10), `Client011BehaviourPinLiveTest` (6) and `ClickHouseSinkTaskWithSchemaTest#autoEvolveDdlRefreshTimeoutThrowsRetriable`, which walks the cause chain for the DDL-propagation `RetriableException` and passes either way.
- Why none changes: no existing test builds a `RetriableException` for the handler, and no writer failure in the matrices carries one in its chain (`WriterFailureClassificationUnitTest` javadoc: it "never builds an exception for the handler").

**The only changes are therefore added rows:** the table above (new test class) plus the identity change of S8 (outcome class identical, instance no longer double-wrapped). A flip of any pre-existing testcase in the before/after status diff is a stop, not an expectation update.

### Pattern 3: RSK-03 WARN at start-up

**Settled decisions:**
- **Site:** `ClickHouseSinkTask.start`, immediately after `validateBufferConfig(clickHouseSinkConfig);` [VERIFIED: `ClickHouseSinkTask.java:46`] and before `this.proxySinkTask = new ProxySinkTask(...)` [`:48`]. The WARN then fires even when ClickHouse is down at start. The class already warns at start-up (`LOGGER.warn("bufferFlushTime is set but will be ignored because bufferCount is 0");` in `createDeliveryStrategy`).
- **List source:** no config key. "Transient" is the connector's own built-in set `Utils.DEFAULT_RETRIABLE_ERROR_CODES` plus code 216. [VERIFIED: `Utils.java:65-81`] `public static final Set<Integer> DEFAULT_RETRIABLE_ERROR_CODES = Set.of(` with `3, 107, 159, 164, 202, 203, 209, 210, 241, 242, 252, 285, 319, 425, 999` (15 codes, public). 216 is `QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING`, the code the setting was added for [VERIFIED: `gh pr view 848 -R ClickHouse/clickhouse-kafka-connect`: merged 2026-10-27, "`additionalRetriableErrorCodes=216` lets a task recover from `QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING`"; issue 847: "216 means the earlier attempt is still running and the insert outcome is unknown, the same situation as 319"]. Code names checked against `ClickHouse/src/Common/ErrorCodes.cpp` on master: `M(216, QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING)`, `M(159, TIMEOUT_EXCEEDED)`, `M(60, UNKNOWN_TABLE)`.
- A listed code outside that set is "unvetted", not proven permanent; the WARN says the connector does not treat it as transient and states the consequence. This is a judgment (Assumptions Log A1).
- **What the WARN says:** one WARN per start, not per code; it names the unvetted codes in ascending order (the config set is a `TreeSet`, `ClickHouseSinkConfig.parseErrorCodes` builds `new TreeSet<>()`), the consequence (redelivery with no retry budget, outside `errors.tolerance` and the DLQ, so a permanent error blocks the partition) and the remedy (remove the code from the setting). It carries only integers and fixed text: no record, key, topic or value. Start-up always continues; nothing throws.
- **No change elsewhere:** the existing `ErrorCodeListValidator` and parser stay as they are (they check only that entries are positive integers); no retry budget. `ClickHouseSinkConfig` diff must be empty.

**Prototype result (scratch-varC, Docker-free):** `ClickHouseSinkTask.start(props)` against a refused loopback port throws `RuntimeException: Connection to ClickHouse is not active.` after the WARN has been written, so a wiring test needs no Docker. Results: key unset -> 0 WARN; `60` -> 1 WARN naming `[60]`; `216` -> 0; `159` -> 0; `62, 60,159` -> 1 WARN naming `[60, 62]`. Whole `testUnit` identical to base except the new probe. [VERIFIED: probe `Rsk03StartProbe`, `/tmp/gsd42/out-start-varC.tsv`.]

### Anti-Patterns to Avoid
- **Editing `Processing.java` imports or running the Spotless hook in write mode on it.** The file is `IS DIRTY` at baseline with an import-only difference (11 lines, equal import sets; below). It is byte-identical to upstream, which formats with no import-order block. Edit the one line with the Edit tool and nothing else.
- **A bare-only pass-through presented as the fix for the timeouts.** It is a production no-op (table above).
- **Adding a second `DEFAULT_RETRIABLE_ERROR_CODES.contains(errorCode)` line to `Utils.java`.** The anchor row counts 1 (`fork-anchors.txt`, TAL-05).
- **Planning IDs in `src/main` comments or log text** (`SourcePlanningIdGuardUnitTest` forbids `Phase \d`, `RESEARCH`, `.planning/`, and ID shapes such as `F-2`, `WRT-13`; the project rule also forbids RSK-/FUT-/OI-/TMO- by convention, which the regex does not catch).
- **Putting two commits' text into one FORK-DELTA line.** Each paragraph of the Known-costs list and each table row is one physical line; two commits editing the same line cannot be reverted independently. See Pitfall 4.
- **Asserting the hazard as "measured" in FORK-DELTA before a repository test pins it.**

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Capturing log output | a custom appender | `LogCapture.attach(ClickHouseSinkTask.class)` | Already used by the WRT and WRT-16 pins; restores the logger level on close |
| Renaming-SMT records | hand-built `SinkRecord` constructors | `RenamedTopicRecords.renamed(...)`, `withTransformedPosition(...)` | They set `originalTopic()` the way `SinkRecord.newRecord` does |
| Recording inserts | a Mockito writer | `RecordingDBWriter` (testFixtures) | Mockito is out for this milestone |
| Retriable detection of a server code | a new code list in `ClickHouseSinkTask` | `Utils.DEFAULT_RETRIABLE_ERROR_CODES` | One definition of the built-in set; the helper only adds 216 |
| Before/after outcome diff | eyeballing logs | a sorted per-testcase status dump of the JUnit XML (script below) | The row list "equals the actual diff exactly" must be mechanical |

**Key insight:** every high-risk edit here is one to a dozen lines; the risk is in what else moves. The evidence that nothing else moved is the work, not the edit.

## Runtime State Inventory

Not a rename or migration phase, but RSK-01 reads the exactly-once state key, so the five categories are answered explicitly.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | KeeperMap rows `(key, minOffset, maxOffset, state)`, key `<transformed topic>-<partition>`; no row holds `originalTopic` (`KeeperStateProvider.java:86-87`, `:160`). The rows have no TTL (`35-BUF03-RECORD.md`). | None. The key and row format do not change; no migration. Empty `KeeperStateProvider` diff is the gate. |
| Live service config | None. No setting is added or renamed. | None. |
| OS-registered state | None. | None. |
| Secrets/env vars | None. The WARN carries no secret. | None. |
| Build artifacts | None. No generated file or installed package depends on these three sources; `ConnectApiReferencesUnitTest` needs no re-bless (`SinkRecord.originalTopic()` is already in the list: `src/test/resources/connect-api-references.txt:189` `M org/apache/kafka/connect/sink/SinkRecord.originalTopic:()Ljava/lang/String;`). | None. |

## Common Pitfalls

### Pitfall 1: The state key moves by accident
**What goes wrong:** An edit that touches `topic` (the key local) instead of `originalTopic` orphans every KeeperMap row; the first batch reads `NONE` and inserts as new.
**Why it happens:** the two locals sit next to each other and `originalTopic` was historically assigned the transformed topic.
**How to avoid:** the diff of `Processing.java` is exactly one changed line (137). Keep `ProcessingRenamedTopicStateKeyUnitTest` (S1 and S2) untouched and green; add a trace pin (recording provider) that asserts the sequence of keys and states for a rename, which the probe shows stays `t-0`, BEFORE then AFTER.
**Warning signs:** `git diff -U0 Processing.java` shows more than one `-`/`+` pair; `KeeperStateProvider.java` or `StateRecord.java` in the diff.

### Pitfall 2: Reading the red wrongly
**What goes wrong:** A test that references a method that does not exist yet is "red" by compile error, which proves nothing.
**How to avoid:** write each test against existing API so it compiles on the unchanged tree and fails by assertion. RSK-01: `state.getLastInsertedOffsetsSnapshot()` / `DirectDeliveryStrategy.preCommit`. RSK-02: `Utils.handleException` and `ProxySinkTask.put`. RSK-03: `ClickHouseSinkTask.start` against a closed port, asserting the WARN count. The helper stays `private`.
**Warning signs:** a red log whose failure is `cannot find symbol`.

### Pitfall 3: The "184-case" label
**What goes wrong:** Planning against 184 rows, then finding 216 testcases and mistaking the 32 extra for changes.
**How to avoid:** the evidence says 216 and explains why (Phase 27 added four kinds). The before/after diff is over testcase names, not a count.

### Pitfall 4: FORK-DELTA lines are single physical lines
**What goes wrong:** RSK-02 and RSK-03 both append to Known-cost item 17; reverting the first conflicts with the second.
**How to avoid:** assign text so no line is edited by two commits. RSK-01 owns a new connector-source row and the rewrite of item 16 (the limits, RSK-04). RSK-02 owns its own new row and the first sentences of item 17. RSK-03 owns its own new row and, if a Known-cost sentence is wanted, a new item (24) instead of editing item 17. Do not touch "The raw numbers" (Phase 44 refreshes them once); note instead that `Processing.java` becomes a fork-delta file.

### Pitfall 5: Anchors counted per line, per commit
**What goes wrong:** `verify-merge-invariants.sh` evaluates the inventory at every commit in range; a commit that changes an anchored line without updating `fork-anchors.txt` fails the whole script after 40 minutes.
**How to avoid:** none of the three edits touches an existing anchored line (probed: `rows=184 bad=0` on all four scratch variants). Each commit adds its own rows (below) and re-runs the mirror (`python3 -I <helper> anchors .planning/scripts/fork-anchors.txt --rows N`).

### Pitfall 6: Dynamic-test order
**What goes wrong:** a raw `diff` of JUnit XML lists differs because `UtilsTest` "code N" tests iterate a `Set`. **How to avoid:** sort before diffing.

### Pitfall 7: Recording a hazard as measured without an in-repo test
The many-to-one over-report and the partition-change entry were measured on scratch. FORK-DELTA labels follow the project convention (`measured`, `read from the code`, `derived`): until a repository pin lands, label them `read from the code`. Recommended: add the two characterization pins to the RSK-01 test class, named as limits, so the label can be `measured`.

### Pitfall 8: The first ClickHouseSinkTask.start in a test leaves an MBean
`ProxySinkTask.start` registers its MBean before the connection check fails. Call `task.stop()` in a `finally` (the probe did; `stop()` tolerates a failed start). Otherwise a later test can hit `InstanceAlreadyExistsException`.

## Code Examples

### RSK-01 (the whole production change)
```java
// Source: Processing.java:137 as read this session; line number unchanged by the edit
-        String originalTopic = record.getSinkRecord().topic();
+        String originalTopic = record.getSinkRecord().originalTopic();
```

### RSK-03 helper (prototype, adjusted so the comment carries no planning ID)
```java
// Source: scratch-varC prototype; goes in ClickHouseSinkTask.java, imports java.util.Set, java.util.TreeSet and ...util.Utils (sorted, no blank lines)
    // Retriable by the connector's own set, plus the code upstream added additionalRetriableErrorCodes for.
    private static final int QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING = 216;

    private static void warnOnUnvettedRetriableCodes(ClickHouseSinkConfig cfg) {
        Set<Integer> unvetted = new TreeSet<>(cfg.getAdditionalRetriableErrorCodes());
        unvetted.removeAll(Utils.DEFAULT_RETRIABLE_ERROR_CODES);
        unvetted.remove(QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING);
        if (!unvetted.isEmpty()) {
            LOGGER.warn("additionalRetriableErrorCodes lists {}, which the connector does not treat as transient. "
                    + "A batch that fails with such a code is redelivered with no retry budget and outside "
                    + "errors.tolerance and the dead letter queue, so a permanent error blocks the partition until "
                    + "the setting is changed.", unvetted);
        }
    }
    // in start(), after validateBufferConfig(clickHouseSinkConfig);
    //     warnOnUnvettedRetriableCodes(clickHouseSinkConfig);
```
The prototype is `static` with package visibility in the scratch run; keep it `private` so the tests are red by assertion (Pitfall 2). Per Pitfall 2 the plan may also keep a package-private form if the executor prefers a unit-level test, but then the red evidence comes from the `start()` wiring test.

### RSK-03 Docker-free wiring test shape
```java
// Source: probe Rsk03StartProbe (scratch); LogCapture usage as in WriterFailureClassificationUnitTest.java:327
int closedPort; try (ServerSocket s = new ServerSocket(0)) { closedPort = s.getLocalPort(); }   // free, then closed
Map<String, String> props = new HashMap<>();
props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
props.put(ClickHouseSinkConfig.PORT, String.valueOf(closedPort));
props.put(ClickHouseSinkConfig.SSL_ENABLED, "false");
props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V2");           // ClickHouseSinkConfig has no CLIENT_VERSION constant of its own at the call site; ClickHouseSinkConnector.CLIENT_VERSION = "client_version" (ClickHouseSinkConnector.java:39)
props.put(ClickHouseSinkConfig.ADDITIONAL_RETRIABLE_ERROR_CODES, "62, 60,159");
ClickHouseSinkTask task = new ClickHouseSinkTask();
try (LogCapture log = LogCapture.attach(ClickHouseSinkTask.class)) {
    assertThrows(RuntimeException.class, () -> task.start(props));      // "Connection to ClickHouse is not active."
    List<LogCapture.CapturedEvent> warns = log.events("WARN");
    // assert: exactly 1; message contains "60" and "62" and not "159"
} finally { task.stop(); }
```
Rows to cover: key unset; each of the 15 built-in codes (0 WARN); 216 (0); `60` (1, names 60); `62, 60,159` (1, names 60 and 62 only); a huge value such as `2147483647` (1, start-up still reaches the connection step, not an exception from the WARN). Add one `[live]` `ClickHouseBase` subclass test that starts a real task with `additionalRetriableErrorCodes=60`, asserts the WARN and that one `put` of a record lands, to show "start-up still succeeds" end to end (runs in the full suite).

### RSK-01 recording state provider (trace pin, from the probe)
```java
// Source: probe Rsk01FlowProbe; extends the real InMemoryState, no Mockito
static final class Recording extends InMemoryState {
    final List<String> calls = new ArrayList<>();
    @Override public StateRecord getStateRecord(String topic, int partition) { /* record "get key state [min,max]" */ return super.getStateRecord(topic, partition); }
    @Override public void setStateRecord(StateRecord r) { /* record "set key=" + r.getTopicAndPartitionKey() + state + range */ super.setStateRecord(r); }
    @Override public void onStateUpdate(StateRecord r) { /* record "onStateUpdate key=..." */ super.onStateUpdate(r); }
}
```
Pin literal expectations from the measured trace: injective rename `t-dev`->`t`, batches 0..9, 0..9 again, 5..14, 15..24 yields keys `t-0` throughout; states `BEFORE_PROCESSING [0,9]`, `AFTER_PROCESSING [0,9]`, replay `get t-0 -> AFTER_PROCESSING [0,9]` then `onStateUpdate`, overlap `BEFORE_PROCESSING [10,14]`, `AFTER_PROCESSING [10,14]`, then `BEFORE_PROCESSING [15,24]`, `AFTER_PROCESSING [15,24]`; writer table topic `t`, offsets 0..24.

### Before/after status diff (the "diff equals the list" artifact)
```sh
# run under python3 -I; DIR = build/test-results/testUnit (or .../test) copied after each run
python3 -I - "$DIR" <<'EOF' | sort
import sys, glob, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*.xml")):
    for tc in ET.parse(f).getroot().iter("testcase"):
        st = "FAIL" if tc.find("failure") is not None or tc.find("error") is not None else ("SKIP" if tc.find("skipped") is not None else "PASS")
        print(f"{tc.get('classname')}\t{tc.get('name')}\t{st}")
EOF
```
Compare the before file and the after file with `diff`; the expected output is the added testcase rows (status PASS) and nothing else. Any changed or missing pre-existing row is a stop.

## Gate Recipe and Plan Shape

(`workflow.nyquist_validation` is `false` in `.planning/config.json`, so there is no Validation Architecture section; this is the gate recipe the plans need.)

**Shape (one commit per code change, so each reverts alone):**
1. 42-01 RSK-01 (+ RSK-04 limits): new Docker-free test class (red on unchanged tree), the one-line edit, FORK-DELTA row and item 16 rewrite, 2 anchors rows.
2. 42-02 RSK-02: scope confirmation (checkpoint), written expected-row list as a plan/evidence file, new test class (red), the pass-through, FORK-DELTA row and item 17 first sentences, 2 anchors rows.
3. 42-03 RSK-03: new test class (red), the WARN, FORK-DELTA row, 2 anchors rows.
4. 42-04 phase gate: the invariants script once, the file-list and config checks, evidence close.
Each code commit touches only its production file, its new test file(s), `docs/FORK-DELTA.md` and `.planning/scripts/fork-anchors.txt`. `VERSION` and `CHANGELOG.md` stay untouched (Phase 44 owns them; the Phase 41 gate recorded `version_changelog_untouched`).

**Proposed anchors rows (fixed string, expected count, backs-ID; re-measure against the real code):**
- `Processing.java`: `String originalTopic = record.getSinkRecord().originalTopic();` 1, RSK-01; `getSinkRecord().topic()` 0, RSK-01.
- `Utils.java`: `t instanceof RetriableException` 1, RSK-02; existing rows unchanged (`DEFAULT_RETRIABLE_ERROR_CODES.contains(errorCode)` 1, `boolean retryOnSocketException, Set` 1, `if (msg != null && (msg.indexOf(` 1).
- `ClickHouseSinkTask.java`: `warnOnUnvettedRetriableCodes(` 2 (definition and call), RSK-03; existing `public void onPartitionsRevoked(Collection<TopicPartition> partitions) {` 1 unchanged.
Mirror goes 184 -> 186 -> 188 -> 190 (`rows=N bad=0`).

**Execution-start re-measure** (do not trust this file's numbers): `git rev-parse HEAD`; `git ls-remote origin refs/heads/custom_build` (was `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`); dirty paths only `.planning/config.json` and `.planning/state.json`; anchors `rows=184 bad=0` (measured today); `git rev-parse HEAD:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (was `7ab06b3c2eebe131c06f4c1087b2c73295b443d7`, measured today); colima profile `vz` Running; `SELECT version()` and digest of `clickhouse/clickhouse-server:latest` (local image id `739e099abd11`, which is the Phase 41 digest `sha256:739e099abd11...`; `latest` can drift, re-measure); baselines `testUnit` and the full suite on the unchanged tree.

**Per-commit gate (recipe from `41-COMMAND-EVIDENCE.md` E-01, E-10):**
```sh
# helper: copy 41-evidence-check.py byte for byte to 42-evidence-check.py and never edit it (cmp-equal), as Phases 40 and 41 did
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > sc.out 2> sc.err   # prints IS CLEAN / IS DIRTY on stderr, writes nothing
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > testunit.log 2>&1
rm -rf build/test-results/test; ./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > full.log 2>&1
python3 -I 42-evidence-check.py skips build/test-results/test        # the 18 named skips
python3 -I 42-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows <N>
```
Run multi-line logic under `bash -c` (`h` is a zsh alias). `-PjavaVersion=21` keeps one HotSpot line (OpenJ9 crash under swap pressure, project memory). `env -u TESTCONTAINERS_HOST_OVERRIDE` is required (the variable is set to `192.168.64.4` in this shell). Docker CLI: `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.

**Measured timings (this machine, scratch copies):** `testUnit` re-run about 12 s of Gradle time on a warm daemon (1759 tests); full suite on `latest` x V2: 2 min 49 s (2357 tests incl. 2 probes). Recorded baselines after Phase 41: `testUnit` 1757 tests / 2 skipped; full suite 2355 / 18 skipped. Both reproduced on scratch (probes excluded). Whole-tree Spotless is never run (46 pre-existing violations).

**Spotless baselines per touched file (measured today with the scoped hook in the repo, nothing written):**

| File | Verdict | Rule for the commit |
|------|---------|---------------------|
| `Processing.java` | IS DIRTY: 11 differing lines, all import or blank, import sets equal (hook moves `import static ...State.AFTER_PROCESSING` / `BEFORE_PROCESSING` before the `org.slf4j` imports) | Never rewrite with the hook. Only line 137 changes; after the edit the same 11 import-only lines must still be the whole difference. |
| `Utils.java` | IS CLEAN | Must print IS CLEAN after the edit. |
| `ClickHouseSinkTask.java` | IS CLEAN | Must print IS CLEAN after the edit (new imports sorted: `...dlq.ErrorReporter`, `...util.Utils`, then `java.util.Collection`, `Map`, `Set`, `TreeSet`, `org...`). |
| every new test file | must print IS CLEAN | Lay out imports as `ProxySinkTaskConstructorUnitTest.java` does: sorted static block, blank line, sorted non-static block, nothing unused. |

**Red-first evidence rule (adapted from the Phase 41 red-reason rule):** each red run is on the unchanged tree, with the new test class compiled, and every failing testcase fails by assertion on the stated outcome (not a compile error, not an unexpected exception); the controls in the same class pass. Record the failing testcase names. A red fixed by editing the evidence is not allowed; re-measure in a new entry.

**Phase gate (after the last code commit and the record rows):**
- `bash .planning/scripts/verify-merge-invariants.sh 54e63648 <explicit SHA>` once and alone (about 40 minutes in Phase 41: `invariants_start_utc 09:22:03Z` to `invariants_end_utc 10:02:23Z`; a Gradle run beside it adds swap pressure). It evaluates the anchor inventory at every commit of the range, so one run on the final SHA covers all three code commits. Re-run only if a later fix changes an anchored line or `fork-anchors.txt`. Expect `result=PASS`, `anchor_rows=190`.
- `git diff --name-only <phase start> HEAD -- src/main` is exactly `Processing.java`, `Utils.java`, `ClickHouseSinkTask.java`; `ClickHouseSinkConfig.java` blob unchanged; `KeeperStateProvider.java` and `StateRecord.java` unchanged; `ProcessingTest.java` unchanged; the CFG-02 `async_insert` anchor still 1; `VERSION`, `CHANGELOG.md`, published rollout notes untouched; nothing pushed.

## FORK-DELTA wording (RSK-04 and the row text)

Labels follow the project's three (`measured`, `read from the code`, `derived`). The hazard sentences are `measured` only after the in-repo characterization pins land; until then `read from the code`.

**Item 16, rewritten (owned by the RSK-01 commit). Draft:**
"**16. The limits of the original-topic fixes (published knowingly open).** `Processing.doLogic` reads the consumer's topic from `SinkRecord.originalTopic()`, so with `reportInsertedOffsets=true` the Direct path keys its inserted offsets by the consumer's topic and `onPartitionRemoved`, which `close` calls with Connect's keys, clears them under a topic-renaming SMT. The exactly-once state key is unchanged: `<transformed topic>-<partition>`, and the KeeperMap row `(key, minOffset, maxOffset, state)` carries no original topic, so no stored row moves and changing the key would orphan every row (it stays a user decision). Exactly-once, the in-memory state machine and `reportInsertedOffsets` are supported only for (1) an injective rename, each source topic renamed to its own target (OI-35-2), and (2) an SMT that preserves the partition (OI-35-3). Outside those limits the state is shared or mis-keyed: a many-to-one rename shares one state record, and the Direct path then reports, for the first record's original topic, the maximum offset of the shared range plus one, which can exceed that topic's own position; a partition-changing SMT reports the consumer's topic with the transformed partition and the shared range's maximum, and `onPartitionRemoved` called with the consumer's partition does not clear that entry. Before this change the Direct path reported nothing for a renamed topic; now it reports offsets for these configurations, so do not enable `reportInsertedOffsets` with them. The buffered path (`bufferCount > 0`) is unaffected by this item; its limits are in the connector-source row. Owner: fork maintainer (the user). Re-open trigger: ..." keep the previous item's triggers and add: a connector that sets `reportInsertedOffsets=true` with a many-to-one rename or a partition-changing SMT.
Keep the existing sentence about a rename onto a topic another input topic already has (not characterised) unless a pin covers it.

**New connector-source rows (one per commit, each in its own line):**
- RSK-01 row: "`Processing.doLogic` reads the consumer's topic for the inserted-offset key and the revoked-partition removal: line 137 is `record.getSinkRecord().originalTopic()`. The state key, the KeeperMap row and table routing keep the transformed topic; `KeeperStateProvider` and the `BEFORE`/`AFTER` sequence are untouched. Effective only with `reportInsertedOffsets=true` under a topic-changing SMT." Where: `sink/processing/Processing.java`. Why: upstream passes the transformed topic, where Connect's own `preCommit` validation needs the consumer's. Pinned by: RSK-01, the new test class, `ProcessingRenamedTopicStateKeyUnitTest`, `ProcessingTest`. Note the `Processing.java` file now differs from upstream.
- RSK-02 row: "`Utils.handleException` throws a `RetriableException` that is the exception or anywhere in its cause chain as retriable, after the server-error rule and before the `DataException` rule, so an `async=true` insert timeout and a DDL-propagation timeout, which `Processing` wraps before the handler sees them, reach Connect as retriable under both `errors.tolerance` values instead of failing the task or going to the DLQ. No pre-existing classification row changes (216 + 14 + 15 + 1757 `testUnit` + 2355 full-suite testcases unchanged)." Where: `util/Utils.java`. Pinned by: RSK-02, the new test class, `WriterFailureClassificationUnitTest`, `WrappedDataExceptionClassificationPinUnitTest`, `InsertStallBoundPinUnitTest`.
- RSK-03 row: "`ClickHouseSinkTask.start` logs one WARN naming each code of `additionalRetriableErrorCodes` that is neither in the built-in retriable set nor 216, with the no-retry-budget consequence; it validates nothing, changes no retry behaviour and carries no record data." Where: `sink/ClickHouseSinkTask.java`. Pinned by: RSK-03, the new test class, `AdditionalRetriableErrorCodesPinUnitTest` (unchanged).
- Item 17: RSK-02 may add one sentence ("A `RetriableException` in the chain is retriable under both tolerance values") at the front; the FUT-05 sentence keeps saying there is no retry budget, and adds that a start-up WARN now names unvetted codes. Do not claim FUT-05 closed: FORK-DELTA item 8 defines it as "a retry budget or a 'remove from default list' setting for retriable codes", and neither exists.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `handleException` classifies on the deepest cause only | plus a `RetriableException` pass-through (RSK-02) | this phase | Writer-thrown `RetriableException`s become retriable end to end; the `ClickHouseWriter.awaitInsertResponse` Javadoc ("throw a RetriableException so the Connect framework retries the batch") becomes true for `async=true` |
| `originalTopic` local named for the consumer's topic but reading the transformed one | reads `SinkRecord.originalTopic()` | this phase | `reportInsertedOffsets` under a renaming SMT commits for the consumer's topic |
| Client V2 library marks 14 codes retryable | The connector keeps its own 15 (adds 159) | unchanged | `ServerException.discoverIsRetryable` in client-v2 0.11.0 is a `lookupswitch` on the 14 codes `3, 107, 164, 202, 203, 209, 210, 241, 242, 252, 285, 319, 425, 999` [VERIFIED: `javap -c` of `ServerException` from `client-v2-0.11.0.jar`]; useful as corroboration that the built-in set is the transient set, but it excludes 159 and cannot serve as the source |

**Deprecated/outdated:** the roadmap's "184-case" count (216 now); `./.claude/CLAUDE.md`'s "Gradle 7.4.2" (wrapper is 9.8.2).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | "Transient" for the WARN means the built-in retriable set plus 216; every other listed code is "unvetted". ClickHouse has no authoritative transient/permanent classification, and the roadmap does not define the term. | Pattern 3 | If the user wants a code such as 225 `NO_ZOOKEEPER` quiet, the set needs another entry, or the design switches to a deny-list of known-permanent codes. Cost: a one-line constant or a different helper; no behavioural risk (a WARN only). |
| A2 | The user's "RetriableException passed in stays retriable" is satisfied by the chain form, not only by `e instanceof RetriableException`. | Pattern 2 | If the user meant bare only, the edit is a production no-op and drop the S3/S4/S5/S7/W1/W2/W4 expectations. Both forms were probed to change zero existing rows. |
| A3 | Kafka accepts and persists a committed offset above the consumer's position, so the over-reported offset in the many-to-one scenario causes skipped records on restart. Derived from consumer-group semantics, not measured here. | Pattern 1 hazard | If Connect clamps or rejects it, the hazard text is too strong; the wording says "can exceed" and "do not enable", which holds either way. |
| A4 | The `lastInsertedOffsets` hazard on `KeeperStateProvider` (exactly-once) equals the `InMemoryState` probe, since the map lives in the shared base class. Read from the code, not run on Keeper. | Pattern 1 | Low: `BaseStateProviderImpl.onStateUpdate` is the only writer and both providers call `super.setStateRecord`. |
| A5 | The ClickHouse `latest` image is still the one Phase 41 recorded (26.10.3.7); only the local image id `739e099abd11` was matched, `SELECT version()` was not re-run in this session. | Gate recipe | `latest` drift changes baselines; re-measure at execution start (the plan already does). |

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 21 HotSpot | all test runs (`-PjavaVersion=21`) | yes | OpenJDK 21.0.12.1 Homebrew | none needed |
| Gradle wrapper | tests, Spotless hook | yes | 9.8.2 | none needed |
| Docker (colima profile `vz`) | full suite on `latest` x V2 | yes | colima `vz` Running (aarch64, 8 CPU, 16 GiB); CLI `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`; `default` context points at the `vz` socket | none; the three commits' unit gates are Docker-free |
| ClickHouse `latest` image | full suite | yes | local image id `739e099abd11` (`clickhouse/clickhouse-server:latest`) | pull |
| `gh` CLI | read upstream PR 848 / issue 847 (done) | yes | authenticated | WebFetch |
| python3 | evidence helper, status diff | yes | system | none |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** none.
Note: `TESTCONTAINERS_HOST_OVERRIDE` is set in the shell and must be unset per command (`env -u`). Other containers run on `vz` (superset stack); they do not interfere.

## Security Domain

`security_enforcement` is `true`, ASVS level 1, block on `high` (`.planning/config.json`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | no change |
| V3 Session Management | no | no change |
| V4 Access Control | no | no change |
| V5 Input Validation | yes (no change) | `additionalRetriableErrorCodes` stays validated by the existing `ErrorCodeListValidator` (positive integers); RSK-03 adds no validator by decision |
| V6 Cryptography | no | none |
| V7 Error Handling and Logging | yes | The new WARN carries only integer codes and fixed text, never a record, key, topic name or secret (AGENTS.md: do not log secrets or data in plaintext); pinned by a message assertion |

### Known Threat Patterns for this change

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Operator lists a permanent code; batch redelivered forever, partition blocked, exactly-once state stuck at `BEFORE` | Denial of service | RSK-03 WARN (informational by decision); no budget, no rejection, because a rejecting validator would stop an existing connector from starting after upgrade |
| State key moved by a careless edit orphans KeeperMap rows | Tampering (of recorded progress) | one-line diff; empty `KeeperStateProvider`/`StateRecord` diff; `ProcessingRenamedTopicStateKeyUnitTest`; new trace pin |
| Over-reported committed offset after RSK-01 under a non-injective rename or partition-changing SMT | Tampering / data loss on restart | limits documented in item 16; do-not-enable guidance; characterization pins |
| Pass-through turns a batch that previously went to the DLQ into an unbounded retry (`async=true` timeouts, `errors.tolerance=all`) | Denial of service (poison batch) | documented behaviour change; same exposure the socket-timeout rule already has; rollout note in Phase 44 |
| Log injection or disclosure through the WARN | Information disclosure | integers from a validated set only |

## Sources

### Primary (HIGH confidence)
- Repository files read this session with line numbers cited above: `Processing.java`, `Utils.java`, `ClickHouseSinkTask.java`, `ProxySinkTask.java`, `ChunkFlusher.java`, `DirectDeliveryStrategy.java`, `BaseStateProviderImpl.java`, `StateProvider.java`, `StateRecord.java`, `InMemoryState.java`, `KeeperStateProvider.java`, `TopicPartitionContainer.java`, `Record.java`, `RecordConvertor.java`, `ClickHouseSinkConfig.java` (lines 99, 117, 196-217, 285, 609-619, 853-863), `ClickHouseWriter.java` (lines 1251, 1322.., 1826), the test classes named above, `RenamedTopicRecords`, `RecordingDBWriter`, `LogCapture`, `SourcePlanningIdGuardUnitTest`, `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md` items 14, 16, 17.
- Scratch probes (not durable, in `/tmp/gsd42`): `Rsk01FlowProbe`, `Rsk02MatrixProbe`, `Rsk02DirectProbe`, `Rsk03StartProbe`, run on unchanged and edited copies of the tree; JUnit status diffs; full suite on the chain variant (2355 / 0 failed / 18 skipped, 2 min 49 s).
- `git diff upstream/main HEAD -- Processing.java` (empty), `git show upstream/main:.../Utils.java` (no pass-through), `git ls-remote origin refs/heads/custom_build`.
- Phase records: `.planning/milestones/v1.7.0_gg6-maintenance-phases/35-buffered-offset-commit-under-topic-renaming-smts/35-BUF03-RECORD.md`, `35-ROLLOUT-ITEM.md`; `.planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md` (E-01, E-10 recipe and timings), `41-REVIEW-FIX.md`; `.planning/research/PITFALLS.md` (Pitfalls 3, 4, 5), `ARCHITECTURE.md`, `FEATURES.md` (D1, D2, C2).
- Upstream: `gh pr view 848` and `gh issue view 847` on `ClickHouse/clickhouse-kafka-connect`; `ClickHouse/ClickHouse` `src/Common/ErrorCodes.cpp` on master (names for codes 3, 6, 16, 26, 27, 33, 36, 38, 41, 43, 46, 47, 53, 57, 60, 62, 81, 117, 131, 159, 164, 202, 203, 209, 210, 216, 225, 241, 242, 252, 279, 285, 319, 395, 425, 497, 516, 999).
- `javap -c` of `com.clickhouse.client.api.ServerException` in `client-v2-0.11.0.jar`.

### Secondary (MEDIUM confidence)
- none

### Tertiary (LOW confidence)
- Kafka consumer-group commit semantics for an offset above the position (A3), from general knowledge.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing new
- Architecture / flow of line 137: HIGH, read and probed both ways
- RSK-02 expected rows: HIGH for "none changed" (216 + 14 + 15 + 1757 + 2355 observed); MEDIUM on scope (bare vs chain is a user reading)
- RSK-03: MEDIUM, the transient definition is a judgment
- Pitfalls and gate recipe: HIGH, taken from Phase 39-41 records and re-run

**Research date:** 2026-11-02
**Valid until:** 2026-11-09 for the gate numbers (`latest` can drift); the code findings hold until `Processing.java`, `Utils.java` or `ClickHouseSinkTask.java` changes.

## Open Questions (RESOLVED)

> User/autonomous decisions (2026-11-02): (1) RSK-02 uses the CHAIN form (user chose via AskUserQuestion); (2) RSK-03 transient = Utils.DEFAULT_RETRIABLE_ERROR_CODES plus code 216 (autonomous: researcher recommendation); (3) RSK-01 limits wording states the changed failure mode, with two characterization pins so the label can be `measured` (autonomous: researcher recommendation).

**STOP-level items: none.** Both research-flag stop rules were tested and did not fire (RSK-01: the value does not reach the state key, a KeeperMap row or table routing; RSK-02: no existing row changes). Items 3, 4 and 7 want the user's eye at plan approval, but they are confirmations, not blockers.

1. **RSK-01: does the line-137 value reach the state key, a KeeperMap row or table routing?** RESOLVED: no. It reaches only `StateRecord.originalTopic`, read solely by `BaseStateProviderImpl.onStateUpdate:27` as the key of the in-memory `lastInsertedOffsets` map. The state key is `String.format("%s-%d", topic, partition)` with the transformed topic; the Keeper row is `(key, minOffset, maxOffset, state)`; table routing reads `first.getSinkRecord().topic()` in `ClickHouseWriter`. A before/after trace over four scenarios was identical except that map key. 14/14 `ProcessingTest` pass on the edited tree. Not a stop.

2. **RSK-02: what is the expected changed-row list?** RESOLVED: empty for every existing row, written here before the edit. 0 of 216 `WriterFailureClassificationUnitTest` executions (the "184" is stale, see Pitfall 3), 0 of 14 PIN-02, 0 of 15 TMO, 0 of 34 `UtilsTest`, 0 of 1757 `testUnit`, 0 of 2355 in the full `latest` x V2 suite (chain form; the bare form's effect is a subset). The added rows are the S1-S11 and W1-W4 tables. The plan's gate: sorted before/after testcase status diff shows only the added rows; any other change is a stop.

3. **RSK-02: bare `e instanceof RetriableException` or the chain form?** NEEDS USER CONFIRMATION, default chain form. The bare form flips only synthetic inputs and no production shape (W1, W2, W4 stay `FAILS_TASK`/DLQ), so the defect the earlier research named (an `async=true` insert timeout and the DDL-propagation timeout never reaching Connect as retriable, because `Processing.doInsert:72` wraps them) is fixed only by the chain form. The ROADMAP Note says anything beyond "RSK-02's pass-through" stops, so put this as a one-line `checkpoint:decision` at the start of plan 42-02 with the two tables above as the evidence. Rows are identical either way.

4. **RSK-03: what counts as a transient code, where does the list come from, where does the WARN fire?** RESOLVED (with assumption A1, confirm at plan approval): transient = `Utils.DEFAULT_RETRIABLE_ERROR_CODES` (15 codes, `Utils.java:65-81`) plus 216 (`QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING`, the code upstream added the setting for, PR 848 / issue 847). Any other listed code is named in one WARN. It fires from a private helper called in `ClickHouseSinkTask.start` after `validateBufferConfig` and before `ProxySinkTask` is built, so it logs even if ClickHouse is down. No new config key, no change to `ClickHouseSinkConfig`, no validator, no budget, no record data. Docker-free proof: a `start()` against a closed port throws the connection error after the WARN was written.

5. **RSK-04: FORK-DELTA wording for OI-35-2 and OI-35-3 and the `onPartitionRemoved` mismatch?** RESOLVED: drafted above (item 16 rewrite, owned by the RSK-01 commit). Limits: injective rename (OI-35-2), partition-preserving SMT (OI-35-3); the state key does not change and changing it stays a user decision; the remaining `onPartitionRemoved` mismatch is the partition half (Connect's original partition does not clear a partition-changing SMT's entry) and the many-to-one first-record keying.

6. **The roadmap says 184 cases; the test has 216.** RESOLVED: Phase 22 had 21 failure kinds (184); Phase 27 added four Jackson kinds (216). Record the correction in the plan's evidence; do not touch the matrix or the roadmap text in this phase.

7. **Does RSK-01 create a new hazard that the limits must state?** RESOLVED as documentation plus pins, flagged for the user's attention: with `reportInsertedOffsets=true` on the Direct path, a many-to-one rename or a partition-changing SMT used to commit nothing and now reports offsets derived from the shared state range (scratch: `{a-dev-0=110}` where `a-dev` is at 10; `{t-dev-0=45}` for partition-changing). This is inside the user-approved limits (injective rename, partition-preserving SMT), is not a state-key change, and is therefore not a stop; it must be worded as "do not enable `reportInsertedOffsets` with these" and ideally pinned by two characterization tests in the RSK-01 class so the label can be `measured`. The Connect-side effect of an over-reported commit is derived (A3).

8. **Where do the RSK-04 record rows land, so each commit reverts alone?** RESOLVED: item 16 with the RSK-01 commit; RSK-02 and RSK-03 each get their own new connector-source row; item 17 is edited only by RSK-02; RSK-03 uses a new item (24) if a Known-cost sentence is needed. Raw numbers are untouched until Phase 44.

9. **How many times must the 40-minute invariants script run?** RESOLVED: once, alone, on an explicit SHA after the last code commit and its record rows; it checks every commit in the range. Re-run only if a later fix changes an anchored line or `fork-anchors.txt`.

10. **How is "start-up still succeeds" shown without Docker?** RESOLVED: the helper cannot throw (it logs and returns), and the failed-start probe shows `start()` reaching the connection step after the WARN; an additional `[live]` `ClickHouseBase` test (full suite) starts a real task with `additionalRetriableErrorCodes=60` and inserts one record.
