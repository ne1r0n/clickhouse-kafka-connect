# Rollout checklist: `v1.7.0_gg5` to `v1.7.0_gg6`

**The connector source changes in this release.** The previous release changed no connector code; this one does. `git diff --shortstat v1.7.0_gg5 HEAD -- src/main` on the release tip reports 7 files changed, 112 insertions and 31 deletions: the buffer strategies and `ChunkFlusher`, `ClickHouseSinkConfig`, `ProxySinkTask`, `Utils` and `ClickHouseHelperClient`. The five changes at a glance:

1. A buffered connector under a topic-changing SMT now commits offsets under the original topic and partition (section B).
2. The ClickHouse Java client moves to `0.11.0`, with one item that breaks an upgrade: `custom_` keys in `jdbcConnectionProperties` (section D).
3. A new setting, `additionalRetriableErrorCodes` (section C).
4. Dependency changes, among them a security release of `lz4-java` (section E).
5. The release identity, which the JMX name and the client name carry, reads `v1.7.0_gg6` (section E).

The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream. If you are coming from a release before `v1.7.0_gg5`, work through [`ROLLOUT-v1.7.0_gg5.md`](ROLLOUT-v1.7.0_gg5.md) first and return here afterwards.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value, such as `<connector-name>`, `<bootstrap>`, `<group>`, `<topic>` or `<dir>`; replace each one with your own value.

**How each claim is labelled.** Every effect of this release has one row in a table below, with a label for how the claim is known. There are four labels:

- `measured`: a command or a test ran, and the row names it.
- `read from the code`: the row names a file and a site; nothing was run.
- `derived from Kafka consumer-group semantics (not measured)`: the claim follows from how Kafka and Kafka Connect define the behaviour; no broker and no Connect worker ran for it.
- `measured in research (not re-run)`: the maintainers measured it while preparing the release, on throwaway clones and in the public `confluentinc/cp-kafka-connect-base:7.9.7` image, and did not repeat it on the release tip.

A row that makes claims of different strength says which label each claim has. The column `Applies when` reads `either client` (the row holds on client `0.9.6` and on `0.11.0`), `client 0.11.0 ships` (the row exists because the client moves to `0.11.0`) or `this release`.

## Before you upgrade

Work through these six steps before you roll the new archive out. Each names the rows it comes from.

1. Search every connector's `jdbcConnectionProperties` for keys that start with `custom_`, for `decompress=true`, and for any expectation that `ssl` or `sslmode` there changes the transport (rows D1, D7, D8). A `custom_` key makes the task fail after the upgrade.
2. For a connector with `bufferCount > 0` and a topic-changing SMT, describe its consumer group and decide where the first start should resume (rows B3, B4).
3. Check that the Connect runtime has the three `SinkRecord` original-topic accessors (row B6).
4. Check the platform and the temp folder of a worker that may take the ZSTD path (row D9).
5. Review `retryOnSocketException`, proxy rules that match the `User-Agent`, and log alerts that match the old texts (rows D3, D5, D6).
6. Update dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg5` or `clickhouse-kafka-connect/v1.7.0_gg5` (row E3).

A connector that sets `additionalRetriableErrorCodes` lists only transient codes (row C2).

## B. Buffered connectors under topic-renaming SMTs

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| B1 | With `bufferCount > 0` and an SMT that changes the topic (for example `RegexRouter`), the connector now returns offsets under the original topic and partition, at the last flushed offset plus one. Before, `preCommit` returned offsets keyed by the renamed topic; row B2 says what Connect does with such a key. | measured | this release | `BufferedOffsetCommitUnitTest` (Docker-free) and `BufferedOffsetCommitLiveTest` (ClickHouse `latest`, client V2): each value `preCommit` returns equals the end offset of its original partition, and Connect's assignment filter drops nothing. The map `preCommit` returns is observed, not a consumer group on a broker. |
| B2 | Kafka Connect commits a task-provided offset only for a partition the consumer is assigned, so the old keys, which named the renamed topic, committed nothing for the group. | derived from Kafka consumer-group semantics (not measured) | this release | Connect's sink offset contract; how the worker handles a key it does not own was not re-measured here, and no broker ran. |
| B3 | The first start on the new archive resumes at the group's existing committed offset if one exists, otherwise at the position `auto.offset.reset` selects. With `earliest` and no committed offset, the retained topic is reread once, a full duplicate load for a table without deduplication. From the first successful flush on, later restarts and rebalances stop rereading. | derived from Kafka consumer-group semantics (not measured) | this release | Kafka consumer-group semantics; no broker ran. Connect's default for sink consumers is not verified here: check `consumer.auto.offset.reset` and `consumer.override.auto.offset.reset`. |
| B4 | A consumer group that stays empty for longer than the broker's `offsets.retention.minutes` loses even a stale committed offset. | derived from Kafka consumer-group semantics (not measured) | this release | Kafka broker offset-retention semantics; no broker ran. |
| B5 | Not affected: unbuffered connectors, buffered connectors whose topics are not renamed, and the `bufferCount` and `bufferFlushTime` contract. Table routing still follows the renamed topic. A revoked partition drops its buffered records, and the exactly-once buffer groups records by their original partition. | controls: measured; unbuffered path: read from the code | this release | The control tests of `BufferedOffsetCommitUnitTest` and `BufferedOffsetCommitLiveTest`; `DirectDeliveryStrategy.preCommit` returns Connect's own offsets unless `reportInsertedOffsets=true`. |
| B6 | Minimum Connect runtime: the fix calls `SinkRecord.originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset()`. A runtime without them fails with `NoSuchMethodError`: exactly-once on the first `put`, while at-least-once leaves the inserted chunk buffered and redelivers it as a duplicate. | accessors present: measured; failure modes: read from the code | this release | The `javap` check of the `connect-api` jar of the `confluentinc/cp-kafka-connect-base:7.9.7` worker image and the Kafka API list that `ConnectApiReferencesUnitTest` guards; failure modes at `ExactlyOnceBufferStrategy.put` and `ChunkFlusher.flush`, with `AtLeastOnceBufferStrategy.put` catching only `RuntimeException`. |
| B7 | Fork-only: upstream issue 777, which describes the defect, is open, and upstream pull request 786 was closed without being merged. The next sync with upstream must reconcile the fix. | measured | this release | Two read-only GitHub API reads when this note was written: the issue state `open`, and the pull request state `closed` with `merged: false`. They are re-read when the release is published. |
| B8 | Documented limits, not fixed: the Direct path with `reportInsertedOffsets=true` under a rename commits nothing; a many-to-one rename (two source topics into one target) shares one state record and can drop or reject records; exactly-once buffering is claimed only for a rename that maps each source topic to its own target; the state-removal call for a revoked partition uses the original key while the state is held under the renamed topic, so it removes nothing under a rename. | Direct path and many-to-one rename: measured; the other two: read from the code | this release | The Direct-path and many-to-one behaviours were measured by a scratch test probe of the maintainers, which is not in the repository and not something you run; `ProcessingRenamedTopicStateKeyUnitTest` pins the state key (the renamed topic and the partition). Read from the code: `ExactlyOnceBufferStrategy.put` groups by original partition while `Processing` keys state by the renamed topic, and `ClickHouseSinkTask.close` calls `onPartitionRemoved`. |

### What changes

With `bufferCount > 0` and an SMT that changes the record's topic, earlier releases returned offsets keyed by the renamed topic from `preCommit` (row B1). Kafka Connect accepts an offset only for a partition the consumer was assigned, so nothing was committed for the group (row B2). This release returns the original topic and partition at the last flushed offset plus one, so after a restart or a rebalance the group resumes from the last flushed offset instead of rereading from where `auto.offset.reset` points.

The edit is small and sits in three buffer files (`ChunkFlusher`, `AtLeastOnceBufferStrategy`, `ExactlyOnceBufferStrategy`). `Processing.java`, the state key, the KeeperMap row schema and the `BEFORE` and `AFTER` flag sequence are untouched (read from the code: the diff of those files since `v1.7.0_gg5` is empty). The fix is fork-only (row B7).

Affected: connectors with `bufferCount > 0`, with at-least-once or exactly-once buffering, and an SMT that changes the topic. Not affected: unbuffered connectors, buffered connectors without renamed topics, and the `bufferCount` and `bufferFlushTime` contract (row B5).

### First start after the upgrade

Every claim in this subsection carries the label `derived from Kafka consumer-group semantics (not measured)` in its row (B3, B4); no broker and no Connect worker ran for it.

The previous archive never advanced the group's committed offset for these partitions. The first start on the new archive therefore resumes exactly where a restart of the previous archive would have: at the group's existing committed offset if one exists (for example one left from a period before `bufferCount` was turned on), otherwise at the position `auto.offset.reset` selects.

Connect's default `auto.offset.reset` for sink consumers is not verified here, so check the worker's `consumer.auto.offset.reset` and any connector-level `consumer.override.auto.offset.reset` before relying on it. With `earliest` and no committed offset, the connector rereads the retained topic from its beginning once; the upgrade restarts the connector, so that one reread coincides with the upgrade. From the first successful flush on, the group's offsets advance and later restarts and rebalances no longer reread.

A group that stays without commits can lose even a stale committed offset once it has been empty for longer than the broker's `offsets.retention.minutes`; check that setting on your brokers (row B4).

### What to do

1. Before the upgrade, run a read-only check against your own cluster. Describe the group; `<group>` is `connect-<connector-name>` unless the connector overrides the group id:

   ```sh
   kafka-consumer-groups --bootstrap-server <bootstrap> --describe --group <group>
   ```

   The output shows whether committed offsets exist and where they are.

2. Decide where the first start should resume. Either accept one reread, when the table deduplicates or the load is idempotent, or stop the connector and set the group's offsets to the position you want before starting the new archive, with `kafka-consumer-groups --bootstrap-server <bootstrap> --group <group> --topic <topic> --reset-offsets <position option> --execute`. The offset reset changes live state: try it first on a group that no live pipeline uses, and verify the options against your Kafka version.
3. After the first successful flush, run the describe command again and confirm that the committed offsets advance.

### Minimum Connect runtime

The fix calls `SinkRecord.originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset()` (row B6). All three are present in the `connect-api` jar of the `confluentinc/cp-kafka-connect-base:7.9.7` worker image, and the connector's Kafka API list is guarded by `ConnectApiReferencesUnitTest`. A Connect runtime without them fails with `NoSuchMethodError`, and the two failure modes are read from the code:

- Exactly-once: `ExactlyOnceBufferStrategy.put` calls `originalTopic()` before any insert, so the first `put` fails.
- At-least-once: `ChunkFlusher.flush` inserts the chunk first and reaches the new accessors only afterwards. `NoSuchMethodError` is an `Error`, and `AtLeastOnceBufferStrategy.put` catches only `RuntimeException`, so the inserted chunk stays buffered with no offset recorded and is redelivered as a duplicate.

Only runtimes below the supported floor are affected; nothing here ran an older runtime.

## C. additionalRetriableErrorCodes

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| C1 | New setting `additionalRetriableErrorCodes`, default empty. Parsing trims each entry, skips empty entries, removes duplicates, and rejects a value that is not an integer or is zero or below. An unset or empty value changes nothing. The built-in set holds 15 codes, including 159. | measured | this release | `AdditionalRetriableErrorCodesPinUnitTest` (Docker-free); the constant `ADDITIONAL_RETRIABLE_ERROR_CODES` and its validator in `ClickHouseSinkConfig`; the built-in set `DEFAULT_RETRIABLE_ERROR_CODES` in `Utils`. |
| C2 | A listed code that the server returns raises `RetriableException`. Kafka Connect redelivers the batch with no retry budget, outside `errors.tolerance` and the dead letter queue, so only transient codes belong in the list. | read from the code | this release | `Utils.handleException` checks the root cause against the built-in and the added codes before it reaches the `errors.tolerance` branch; the setting's own description in `ClickHouseSinkConfig` says the same. |
| C3 | Two behaviours of `handleException` are retriable today, and no test asserts them: a `DataException` whose cause chain holds a ClickHouse server error with a retriable code (a built-in or an added one), and a `DataException` whose root cause is a socket timeout or an unknown host under `errors.tolerance=all`. A decision on changing them is open. | read from the code | this release | `Utils.handleException`, which classifies the root cause before it looks at the `DataException`. |

### What the setting does

`additionalRetriableErrorCodes` takes a comma-separated list of ClickHouse server error codes, for example `additionalRetriableErrorCodes=<code>,<code>`. It defaults to empty; with it unset the connector behaves as before. The parse rules are those of row C1. The built-in set of 15 codes, 159 (`TIMEOUT_EXCEEDED`) among them, stays in force, and the setting adds to it; it does not change the ClickHouse Java client's own retry settings.

### The bypass of errors.tolerance

A code in the list makes the connector throw `RetriableException`, and Kafka Connect then redelivers the same batch. That redelivery has no retry budget and does not go through `errors.tolerance` or the dead letter queue (row C2). A code that is not transient therefore makes the task loop on the same batch: list only codes that are transient, such as an overloaded server or a table that is briefly read-only.

### Two retriable behaviours that no test asserts

Row C3 names two cases that are retriable today although no test asserts them. A decision on changing either is open; this release leaves both as they are.
