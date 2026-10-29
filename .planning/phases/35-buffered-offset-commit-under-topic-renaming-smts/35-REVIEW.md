---
phase: 35-buffered-offset-commit-under-topic-renaming-smts
reviewed: 2026-10-29T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java
  - src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java
  - src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java
  - src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java
  - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java
  - src/test/resources/connect-api-references.txt
  - docs/FORK-DELTA.md
findings:
  critical: 0
  warning: 2
  info: 5
  total: 7
status: issues_found
---

# Phase 35: Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

The production change is three call sites (5 lines added, 4 removed, confirmed against `git diff 6ac0cdc0 HEAD --stat`). I found no wrong-key site left in the buffered paths. In `ChunkFlusher`, `AtLeastOnceBufferStrategy` and `ExactlyOnceBufferStrategy`, every `topic()`, `kafkaPartition()` and `kafkaOffset()` that feeds an offset or a revocation now reads the `original*` accessor. The only remaining `topic()` use on the buffered path is table routing in `ProxySinkTask`/`Processing`, which is intended.

For a record that never passed an SMT, `SinkRecord` initialises `originalTopic`, `originalKafkaPartition` and `originalKafkaOffset` from the record's own fields, so behaviour is identical to before. A null `kafkaPartition` NPEs in `new TopicPartition(...)` exactly as it did before. The offset+1 arithmetic is unchanged and the hot path allocates the same objects as before.

All fork SMTs build their output with `newRecord`, so they preserve the originals. A custom SMT that calls `new SinkRecord(...)` directly would reset them, but that is outside the connector's control.

The upstream claims in FORK-DELTA are accurate. `gh` shows issue #777 OPEN and PR #786 CLOSED, unmerged, closed at 2026-09-09T17:44:11Z. The `Pinned by` and `Where it lives` columns match the code. Item 9's new clause is accurate.

The test-pollution concern from Phase 34 WR-01 does not recur here:
- `BufferedOffsetCommitUnitTest` stops every `ProxySinkTask` in `@AfterEach`. `stop()` unregisters the task MBean and the lazily registered topic MBeans, and tolerates the task MBean never having been registered.
- `ProcessingRenamedTopicStateKeyUnitTest` unregisters its `SinkTaskStatistics` in `@AfterEach`.
- The live test runs `task.stop()` in a `finally` block.
- `maxParallelForks` forks are separate JVMs, so the shared JMX server is not an issue.

I found no timing dependence in the live test: `bufferFlushTime` is unset and the inserts are synchronous. The mutation sensitivity I could check by reading the code:
- A topic regression in `close` or `put` is caught.
- An offset regression in `ChunkFlusher` is caught.
- A partition-only regression is not caught (WR-01).

## Warnings

### WR-01: No test pins `originalKafkaPartition()` at the two strategy sites, so a partition-only regression passes

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java:178-221`
**Issue:** The fix changed `originalKafkaPartition()` in `AtLeastOnceBufferStrategy.close` (line 86) and `ExactlyOnceBufferStrategy.put` (line 43). Every test that exercises revocation or per-partition grouping (BUF-02 at-least-once and exactly-once, and the BUF-01 close test) builds records with `RenamedTopicRecords.renamed`, which keeps the partition. Reverting those two call sites to `kafkaPartition()` therefore leaves every close and grouping test green.

The only test that moves the partition is `preCommitFollowsTheOriginalPartitionAndOffsetWhenTheSmtChangesThemAsWell`. It exercises `ChunkFlusher` only, with no close and no second partition. The sibling `ChunkFlusher` offset code is covered, but the strategy key is a separate, uncovered site.

The FORK-DELTA row and the rollout item both state that the buffer keys by `originalKafkaPartition()`. That claim is pinned only for the topic half.
**Fix:** Add a close test, parameterised over `StrategyKind`, that feeds records through `RenamedTopicRecords.withTransformedPosition`. For example, original partition 1 becomes transformed partition 0. Buffer two original partitions that both collapse onto transformed partition 0, call `close(partition(ORIGINAL_TOPIC, 1))`, then complete a chunk of the other original partition and assert that no record of original partition 1 was written. A mutation to `kafkaPartition()` then merges the two partitions' records and fails the assertion.

### WR-02: `ChunkFlusher.flush` calls the new accessors only after the insert, and the rollout wording misdescribes the failure on a runtime that lacks them

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java:51-55`, `docs/FORK-DELTA.md` (new row, last cell), `35-ROLLOUT-ITEM.md` ("Minimum Connect runtime")
**Issue:** The documents say that on an older worker "the first buffered flush would throw `NoSuchMethodError`". Reading the code, the sequence differs by strategy:
- **Exactly-once.** `ExactlyOnceBufferStrategy.put` calls `r.originalTopic()` before any insert, so it fails on the first `put()`, not at a flush.
- **At-least-once.** `flush` runs `putDirect(chunk)` first, which inserts the rows into ClickHouse. Only then does the `for` loop reach `originalTopic()` and throw. `NoSuchMethodError` is an `Error`. `AtLeastOnceBufferStrategy.put` catches only `RuntimeException`, so its cleanup `subList(...).clear()` does not run and `buffer.clear()` is skipped. The inserted chunk stays in the buffer and no offset is recorded.

On such a runtime the first buffered chunk is written to ClickHouse and then redelivered after the task dies, which is a duplicate. This only affects a Connect runtime outside the supported floor, and BLD-16 guards the floor in CI. The defect is therefore a robustness and wording problem, not a live data-loss path.
**Fix:** Either of the following removes the ordering hazard.
- In `ChunkFlusher.flush`, compute the keys and offsets in a first loop over the chunk and call `putDirect` afterwards, so a linkage failure precedes any insert.
- Alternatively, probe `SinkRecord.class.getMethod("originalTopic")` once in `ClickHouseSinkTask.start()` and fail with a clear `ConnectException`.

Also correct the wording to say that exactly-once fails on the first `put()` and at-least-once fails after the first chunk's insert.

## Info

### IN-01: The `acceptedByConnect` assertions can never fail once the key assertion has passed

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java:148-151, 172-175`; `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java:78-79, 117-118`; `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java:79-88`
**Issue:** Each use is `assertEquals(committed, acceptedByConnect(committed, expected.keySet()))`, and it follows an `assertEquals(expected, committed)` on the same keys. Once the first assertion holds, `assigned` equals `committed.keySet()`, so the filter removes nothing by construction. The helper is also a re-implementation of Connect's filter rather than Connect's own code.

It is harmless but adds no failure sensitivity, and the Javadoc presents it as evidence that Connect accepts the offsets. The accepted-offset claim really rests on the test author's model of `WorkerSinkTask`.
**Fix:** Either drop the redundant assertions, or pass an `assigned` set that is not derived from `expected` (for example the set of original partitions from the consumed records). Say plainly in the Javadoc that it models Connect's behaviour.

### IN-02: `RecordingDBWriter.Inserted` describes a consumer position but its `offset` is the transformed offset

**File:** `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java:18-31`
**Issue:** The Javadoc reads "Consumer position and table topic of one inserted record". `originalTopic` and `originalPartition` are the consumer's, but `offset` is `sinkRecord.kafkaOffset()`, the transformed offset. For the partition-moving fixture (`withTransformedPosition`) the record would carry original offset 500+ and recorded offset 0 to 9. All current assertions use `renamed`, which keeps offsets equal, so nothing fails today. A future test that feeds `withTransformedPosition` records through `assertWritten` would compare the wrong offset.
**Fix:** Record both offsets (`originalOffset` and `offset`), or read `originalKafkaOffset()` and correct the Javadoc to say which one it holds.

### IN-03: The live `scenario()` leaks the ClickHouse client when table creation fails

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java:152-163`
**Issue:** `ClickHouseTestHelpers.createClient(props)` opens a client that is closed only by the try-with-resources in `runScenario`. If `dropTable` or `TABLE.get()...execute(chc)` throws inside `scenario()`, `runScenario` is never reached and the client is never closed. It is a small test-hygiene leak, only on the failure path.
**Fix:** Wrap the setup in a try/catch that closes `chc` before rethrowing, or move table creation into `runScenario` inside the try-with-resources.

### IN-04: `ClickHouseSinkTask.close` now passes original keys to `onPartitionRemoved`, which holds transformed keys, and this is not recorded under OI-35-1

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:141-147`; `src/main/java/com/clickhouse/kafka/connect/sink/state/BaseStateProviderImpl.java:33-35`
**Issue:** `close` passes Connect's revoked (original) partitions to `proxySinkTask.onPartitionRemoved`. That call removes from `lastInsertedOffsets`, and `Processing.doLogic` populates that map under the transformed topic (the defect OI-35-1 records). Under a renaming SMT the removal is therefore a no-op, and revoked partitions' entries stay in the map. This is the same root cause as OI-35-1 and does not change the buffered paths, because they ignore `lastInsertedOffsets`. The BUF-03 record and the rollout item mention only the `preCommit` symptom of OI-35-1, and `grep` shows `onPartitionRemoved` only in the research file.
**Fix:** Add one sentence to OI-35-1 in `35-BUF03-RECORD.md` saying that `onPartitionRemoved` has the same keying mismatch, so it is fixed together with the `Processing.doLogic` change. No source edit is needed for this phase.

### IN-05: The state-key tripwire is described as the exactly-once key but exercises only `InMemoryState`

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java:22-29, 53`
**Issue:** The class Javadoc says it pins "the exactly-once state key" and that a key change "orphans the state rows of existing exactly-once deployments". The test uses `InMemoryState` with a default (non-exactly-once) config. It pins what `Processing` passes to the state provider (the transformed topic and partition). It does not exercise the real `KeeperStateProvider` key (`%s-%d`) that holds the persisted rows. A change inside `KeeperStateProvider` key construction would not trip it.

In addition, OI-35-3 covers a partition-changing SMT but not value-routed fan-out, where a single source topic maps to several tables. The fork's own `ExtractTopic` does this. With the exactly-once buffer now bucketing by original partition, one chunk of `bufferCount` records is split by `ProxySinkTask` into several per-table, per-partition batches. The retry reconstruction is deterministic, so this is probably safe. It is not probed, and the rollout item's rule ("a rename that maps each source topic to its own target") does not mention fan-out.
**Fix:** Reword the Javadoc to "the key `Processing` hands to the state provider". Optionally add a one-line OI-35-3 note that value-routed fan-out from one source partition is also unprobed for exactly-once.

---

_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
