# Rollout checklist: `v1.7.0_gg1` to `v1.7.0_gg2`

If you are coming from a release before `v1.7.0_gg1`, work through [`ROLLOUT-v1.7.0_gg1.md`](ROLLOUT-v1.7.0_gg1.md) first and return here afterwards.

This note covers only what changed between `v1.7.0_gg1` and `v1.7.0_gg2`. Both releases are built on the same upstream base, `1.7.0`. The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream.

Like its predecessor, this is a checklist and not a deployment record. It names no host, connector, cluster or production system, and every environment-specific value is a placeholder: `<db>`, `<table>`, `<column>`, `<key_column>`, `<zkDatabase>`, `<topic>`, `<connector-name>` or `<alert-rule-directory>`. Replace each one with your own value.

Every item has the same three parts:

- **What changed** describes the difference in behaviour between the two releases.
- **Am I affected?** gives a check you can run against your own configuration or your own ClickHouse server, and says what an empty result means.
- **What to do** gives the action when the check shows that you are affected.

Tags such as (ledger L-30) point at entries in the fork's two behaviour ledgers, `18-BEHAVIOUR-LEDGER.md` and `19-BEHAVIOUR-LEDGER.md`, which are kept in this repository's planning directory. This note restates everything an operator needs from them. Where a statement rests on a measurement, the ClickHouse server version it was measured on is named.

The items, in the order to work through them:

1. [In-flight batches and the deduplication token](#item-1-in-flight-batches-and-the-deduplication-token)

---

## Item 1: In-flight batches and the deduplication token

### What changed

`v1.7.0_gg2` encodes some batches differently from `v1.7.0_gg1`. Empty Debezium records are now skipped on more routes (item 3), and `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` slots are now written with their null markers. A batch that `v1.7.0_gg1` left in `BEFORE_PROCESSING` is retried by `v1.7.0_gg2`, and the retry carries the same `insert_deduplication_token` as the first attempt. The connector builds that token from the topic, the partition and the first and last offset of the batch, so the token does not depend on how the batch is encoded (ledger L-30).

**What ClickHouse does with that retry.** Measured on ClickHouse 26.9.18.11 with client V2, the server decides by the token and ignores the bytes:

- On a table that deduplicates, a retry under the same token is dropped even when its bytes differ from the first attempt's. If the `v1.7.0_gg1` attempt landed its rows, those rows stay exactly as they are, including phantom rows and wrongly stored Enum and Tuple values, and the change of encoding writes no row a second time. If the `v1.7.0_gg1` attempt did not land, the `v1.7.0_gg2` block lands once, with the corrected encoding. When the earlier attempt landed, the connector marked the range `AFTER_PROCESSING`, and in both cases it carried on with the next range. Identical data under a different token is stored again, so the token is what counts, not the data.
- Which tables deduplicate: on that server `replicated_deduplication_window` is 10000 and `replicated_deduplication_window_seconds` is 3600. A retry that arrives more than 3600 seconds after the first attempt is not deduplicated by a replicated table (the measurement read these settings; it did not wait out the time limit). A non-replicated table deduplicates only when `non_replicated_deduplication_window` is above 0, and the default on that server is 0. A table with a window of 100 dropped a same-token retry, and a table with a window of 0 stored every insert. A table without a deduplication window stores a second copy of a retried batch in any release, `v1.7.0_gg1` included.
- With `exactlyOnce=false` the connector keeps no offset state, so the stop-and-check procedure below does not apply. It still sends the token for batches that belong to a partition, so a re-delivered range is dropped on a table with a window (measured on `ReplicatedMergeTree`, for identical and for changed bytes) and stored again on a table without one (measured on a plain `MergeTree`, which grew by three rows on every delivery of three rows).

**A batch interrupted across the upgrade.** Suppose a task stops after the insert but before it writes `AFTER_PROCESSING`, and the new jar starts next. The range is retried as described above: dropped if the earlier insert landed, written once if it did not. Item 6 describes the mirror image for a rollback.

**Correction to the gg1 note.** Item 1 of the gg1 note reasoned that ClickHouse deduplicates on the parsed data, and that, if a retry is encoded differently, "rows can be written twice". That was an inference. For the retry the connector sends it is wrong: the token decides, and the earlier attempt's content wins. In every measured case a retry under the same token was dropped, and the only duplicates observed were on a table without a deduplication window. The risk of this upgrade is old rows that persist, not rows written twice. The gg1 note itself stays as it was released.

### Am I affected?

The token applies to every connector, whatever its mode, so first check whether your target tables deduplicate at all. Run this in a session that can read `system.tables`, with `<db>` replaced by the database the connector writes to:

```sql
SELECT
    name AS table_name,
    engine,
    toUInt8(
        if(engine LIKE 'Replicated%MergeTree',
           coalesce(
               toUInt64OrNull(extract(engine_full, 'replicated_deduplication_window\\s*=\\s*(\\d+)')),
               (SELECT toUInt64(value) FROM system.merge_tree_settings WHERE name = 'replicated_deduplication_window')
           ) > 0,
           coalesce(
               toUInt64OrNull(extract(engine_full, 'non_replicated_deduplication_window\\s*=\\s*(\\d+)')),
               (SELECT toUInt64(value) FROM system.merge_tree_settings WHERE name = 'non_replicated_deduplication_window')
           ) > 0)
    ) AS deduplicates
FROM system.tables
WHERE database = '<db>'
  AND engine LIKE '%MergeTree'
ORDER BY table_name;
```

**Empty result:** `<db>` holds no table of a `MergeTree` family engine, or the name is wrong. **`deduplicates` is 1:** the server drops a retry or re-delivery that carries the same token, inside the window. **`deduplicates` is 0:** a retried batch is stored again, in this release and in the previous one.

The query reads the window from the table definition and falls back to the server default. It does not evaluate `replicated_deduplication_window_seconds`, it does not know about overrides set through a user profile, and it was run only on `MergeTree` and `ReplicatedMergeTree` tables. An engine such as `SharedMergeTree` on ClickHouse Cloud is not covered.

You are affected by the in-flight half if any connector runs with `exactlyOnce=true`. For a connector that writes `Nullable(Enum8)`, `Nullable(Enum16)` or `Nullable(Tuple(...))` columns (item 2) or Debezium records with `debeziumCDCEnabled=true` (item 3), treat the check below as required. For every other connector it is precautionary.

### What to do

1. Stop or pause each connector with `exactlyOnce=true` with your usual procedure, and wait until its tasks have stopped.
2. Query the shared KeeperMap state table for the topics that connector consumes. The table is named by the connector's `zkDatabase` setting. Replace each `<topic>` with one of your topics, writing the same topic twice if you have only one:

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

   **Empty result:** no batch of those topics is in flight, so it is safe to upgrade. **Non-empty result:** each listed `key` is a topic and partition with a batch that may or may not have reached ClickHouse; do not upgrade yet. On a state table created by the connector itself, the query returned the one in-flight key of the listed topic, left out a topic that merely shares its prefix, and returned nothing once that range was `AFTER_PROCESSING`. The query only reads, so you can run it as often as you like. The [gg1 note](ROLLOUT-v1.7.0_gg1.md#item-1-exactly-once-pre-upgrade-check) explains why the table is shared and how the key looks with `enableDbTopicSplit=true`; those explanations are not repeated here.
3. If the result is not empty, start the `v1.7.0_gg1` jar again, let the listed ranges reach `AFTER_PROCESSING`, stop it cleanly and run the query again. Repeat until it is empty, then upgrade.
4. If you upgraded with a range in flight, its earlier content stays. After the upgrade, run the detection queries of items 2 and 3 for the tables of that range and re-load from the source whatever turns out to be wrong. The connector does not rewrite stored rows.
