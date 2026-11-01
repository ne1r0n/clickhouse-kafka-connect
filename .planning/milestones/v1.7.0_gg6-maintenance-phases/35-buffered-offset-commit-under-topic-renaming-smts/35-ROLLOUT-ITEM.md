# Rollout item for Phase 38: buffered connectors under topic-renaming SMTs now commit offsets

This file is the record Phase 35 hands to Phase 38 (RLS-01 and RLS-02), which places it in `docs/ROLLOUT-v1.7.0_gg6.md`. It is not the release note: `docs/ROLLOUT-v1.7.0_gg6.md` does not exist yet and Phase 35 does not create it.

The item names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value, such as `<connector-name>`, `<topic>`, `<group>` or `<bootstrap>`; replace each one with your own value.

Every claim carries one of three labels, and names the command, test, entry or code site behind it:

- `measured`: a command or test that ran, recorded in `35-COMMAND-EVIDENCE.md` under the entry named.
- `read from the code`: a file and site named, not run.
- `derived from Kafka consumer-group semantics (not measured)`: follows from how Kafka and Kafka Connect define the behaviour. Nothing in this repository ran a broker or a Connect worker for it.

## What changes

With `bufferCount > 0` and an SMT that changes the record's topic (for example `RegexRouter`), previous releases returned offsets keyed by the renamed topic from `preCommit`. Measured: the red runs of E-02 (Docker-free) and E-05 (ClickHouse `latest`, client V2) on the unchanged tree show the returned keys are the renamed topic, for example `<table>-0`, where the original topic and partition are `<topic>-0`.

Kafka Connect accepts a task-provided offset only for a partition the consumer was assigned. A key that names the renamed topic is not an assigned partition, so nothing was committed for the group. This is derived from Connect's sink offset contract (not measured): the worker's handling of keys it does not own was not re-measured here.

This release returns the original topic and partition at the last flushed offset plus one. After a restart or a rebalance the group resumes from the last flushed offset instead of rereading from where `auto.offset.reset` points. Measured, E-05: on ClickHouse `latest` 26.10.3.7 with client V2, each value `preCommit` returns equals the end offset of its original partition, and applying Connect's assignment filter to the returned map removes nothing. This observes the map `preCommit` returns, not a consumer group on a broker (see "First start after the upgrade").

A partition that is revoked loses its buffered records, and the exactly-once buffer groups records by their original partition, so a revoked partition leaves no stale rows in a later chunk. Measured: E-02 (Docker-free) and E-05 (live).

Table routing still follows the renamed topic. Measured: E-02 (the routing controls of both buffer strategies pass).

The edit is at three sites in `ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java` and `ExactlyOnceBufferStrategy.java`, 5 lines added and 4 removed (read from the code: the fix commit of E-03, `diff_numstat`). `Processing.java`, the state key, the KeeperMap row schema and the `BEFORE`/`AFTER` flag sequence are untouched (measured: `processing_untouched_in_main_tree` in E-06 and E-07).

Upstream status: upstream issue #777 describes this defect and is still open, and upstream pull request #786 was closed without merging (measured at the time of writing: `gh issue view` and `gh pr view`, E-08). The fix is fork-only; the next sync with upstream must reconcile it.

## Who is affected

Affected: connectors with `bufferCount > 0`, with at-least-once or exactly-once buffering, and an SMT that changes the topic.

Not affected, measured by the control tests of E-02 and E-05:

- Unbuffered connectors. The connector returns Connect's own offsets.
- Buffered connectors whose topics are not renamed.
- The `bufferCount` and `bufferFlushTime` contract.

The unbuffered path returns Connect's own offsets (read from the code: `DirectDeliveryStrategy.preCommit`, which returns `currentOffsets` unless `reportInsertedOffsets=true`).

Not part of this item: the state key and `reportInsertedOffsets` reporting in `Processing.doLogic`, and the many-to-one rename. They are recorded in `35-BUF03-RECORD.md` as open items OI-35-1 and OI-35-2. Exactly-once with buffering needs a rename that maps each source topic to its own target.

## First start after the upgrade

Label: derived from Kafka consumer-group semantics (not measured). This section was not measured: the decision is recorded in E-08 (`first_start_measurement: not measured`, with reasons).

The previous jar never advanced the group's committed offset for these partitions. The first start on this jar therefore resumes exactly where a restart of the previous jar would have:

- at the group's existing committed offset, if one exists (for example one left from a period before `bufferCount` was turned on);
- otherwise at the position `auto.offset.reset` selects.

Kafka Connect's default for sink consumers is `earliest`. That default is not verified here. Check the deployed worker's `consumer.auto.offset.reset` and any per-connector `consumer.override.auto.offset.reset` before relying on it.

With `earliest` and no committed offset, the connector rereads the retained topic from its beginning once. For tables without deduplication that is a full duplicate load. Upgrading restarts the connector, so that one reread coincides with the upgrade. From the first successful flush on, the group's offsets advance and later restarts and rebalances no longer reread.

A group that stays without commits can lose even a stale committed offset once the group is empty for longer than the broker's offset retention. Verify the broker's `offsets.retention.minutes`.

## What to do

1. Before the upgrade, a read-only check (derived; standard Kafka tooling, run it against your own cluster). Describe the group:

   ```sh
   kafka-consumer-groups --bootstrap-server <bootstrap> --describe --group <group>
   ```

   `<group>` is `connect-<connector-name>` unless the connector overrides the group id. The output shows whether committed offsets exist and where they are.

2. Decide where the first start should resume (derived, not measured). Either accept one reread when the table deduplicates or the load is idempotent, or stop the connector and set the group's offsets to the position you want before starting this jar, with `kafka-consumer-groups --bootstrap-server <bootstrap> --group <group> --topic <topic> --reset-offsets <position option> --execute`. The offset reset is flagged: try it on a non-production group first and verify the options against your Kafka version.

3. After the first successful flush, run the describe command again and confirm that the committed offsets advance (derived).

## Minimum Connect runtime

The fix calls `SinkRecord.originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset()`.

- All three are present in `connect-api-7.9.7-ccs.jar` of the Confluent Platform worker image `confluentinc/cp-kafka-connect-base:7.9.7` (measured, E-03 `worker_accessors_present`, read with `javap`).
- All three are present in connect-api 4.1.1 and 4.4.0. Measured: the BLD-16 re-bless of E-03 reports `classes-missing 0 members-missing 0` for each of the worker, 4.1.1 and 4.4.0 reference sets.

A Connect runtime without them fails with `NoSuchMethodError` (derived from reading the code and how the JVM links methods, not measured):

- **Exactly-once:** `ExactlyOnceBufferStrategy.put` calls `originalTopic()` before any insert, so the first `put()` fails.
- **At-least-once:** `ChunkFlusher.flush` inserts the chunk first and reaches the new accessors only afterwards. `NoSuchMethodError` is an `Error` and `AtLeastOnceBufferStrategy.put` catches only `RuntimeException`, so the inserted chunk stays buffered with no offset recorded and is redelivered as a duplicate.

Only runtimes below the supported floor are affected, and BLD-16 guards the floor in CI; nothing here exercised an older runtime.

## Evidence labels

| Claim | Label | Evidence |
|-------|-------|----------|
| Before the fix, `preCommit` returned keys named by the renamed topic | measured | E-02 and E-05 red runs |
| Connect does not commit a key outside the consumer's assignment | derived from Kafka consumer-group semantics (not measured) | Connect's sink offset contract; context in E-05 `live_pin_observes` |
| After the fix, `preCommit` returns the original topic and partition at the end offset, and Connect's assignment filter drops nothing | measured | E-05 `live_class_xml`, `live_pin_observes` |
| A revoked partition's buffered records are dropped; the exactly-once buffer groups by original partition | measured | E-02 `red_failed` and `unit_pins_xml` (E-03), E-05 `live_mutation_a_failed` |
| Table routing follows the renamed topic | measured | E-02 (routing controls) |
| Unbuffered and not-renamed connectors, and the buffer contract, are unchanged | measured | E-02 controls, E-05 `buffer_class_xml` and `direct_offset_tests_xml` |
| The unbuffered path returns Connect's own offsets | read from the code | `DirectDeliveryStrategy.preCommit` |
| The fix is a small edit at three sites in three buffer files; `Processing.java` and the state key are untouched | read from the code | E-03 `diff_numstat`; measured by E-06 and E-07 `processing_untouched_in_main_tree` |
| Upstream #777 is open and #786 closed unmerged | measured | E-08 |
| Where the first start resumes | derived from Kafka consumer-group semantics (not measured) | E-08 `first_start_measurement_reasons` |
| Kafka Connect's sink `auto.offset.reset` default is `earliest` | derived from Kafka consumer-group semantics (not measured) | unverified; check `consumer.auto.offset.reset` and `consumer.override.auto.offset.reset` |
| A read-only describe shows the group's committed offsets | derived from Kafka consumer-group semantics (not measured) | standard Kafka tooling |
| An offset reset sets the first start's position | derived from Kafka consumer-group semantics (not measured) | standard Kafka tooling; try on a non-production group |
| The three accessors exist in the worker jar and in connect-api 4.1.1 and 4.4.0 | measured | E-03 `worker_accessors_present`, `rebless_summary` |

## Handoffs

- Phase 38, RLS-01: the FORK-DELTA row, its item-9 clause and the 7 anchor rows already landed with the fix. The Kafka API list was re-blessed once for this phase, and Phase 36 re-blesses it again for the client bump. `VERSION`, `CHANGELOG.md` and the rollout note were not touched by Phase 35.
- Phase 38, RLS-02: this item is the buffered offset-commit item its criterion checks.
- Milestone close: OI-35-1, OI-35-2 and OI-35-3 of `35-BUF03-RECORD.md`.
