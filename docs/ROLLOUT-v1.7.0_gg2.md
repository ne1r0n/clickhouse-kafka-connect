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
2. [Nullable Enum and Nullable Tuple values](#item-2-nullable-enum-and-nullable-tuple-values)
3. [Empty Debezium records](#item-3-empty-debezium-records)
4. [Error messages and log lines](#item-4-error-messages-and-log-lines)
5. [JMX MBean name and client name](#item-5-jmx-mbean-name-and-client-name)
6. [Rollback](#item-6-rollback)

---

## Item 1: In-flight batches and the deduplication token

### What changed

`v1.7.0_gg2` encodes some batches differently from `v1.7.0_gg1`. Empty Debezium records are now skipped on more routes (item 3), and `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` slots are now written with their null markers. A batch that `v1.7.0_gg1` left in `BEFORE_PROCESSING` is retried by `v1.7.0_gg2`, and the retry carries the same `insert_deduplication_token` as the first attempt. The connector builds that token from the topic, the partition and the first and last offset of the batch, so the token does not depend on how the batch is encoded (ledger L-30).

**What ClickHouse does with that retry.** Measured on ClickHouse 26.9.18.11 with client V2, the server decides by the token and ignores the bytes:

- On a table that deduplicates, a retry under the same token is dropped even when its bytes differ from the first attempt's. If the `v1.7.0_gg1` attempt landed its rows, those rows stay exactly as they are, including phantom rows and wrongly stored Enum and Tuple values, and the change of encoding writes no row a second time. If the `v1.7.0_gg1` attempt did not land, the `v1.7.0_gg2` block lands once, with the corrected encoding. When the earlier attempt landed, the connector marked the range `AFTER_PROCESSING`, and in both cases it carried on with the next range. Identical data under a different token is stored again, so the token is what counts, not the data. The earlier `v1.7.0_gg1` attempt was simulated in this measurement: the test inserts the rows that `v1.7.0_gg1` wrote for a batch with empty Debezium records (two real rows and two all-defaults rows) under the token the connector sends, and then lets the connector retry the range. The wrongly stored Enum and Tuple values follow from the same rule and were not run through a retry.
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

---

## Item 2: Nullable Enum and Nullable Tuple values

### What changed

Values in `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` columns are now written with their null markers and read back as written. This holds at the top level and inside `Array` and `Map` columns (ledger L-31, L-35, L-36). A record that omits such a column, where the column has no `DEFAULT`, now passes schema validation and stores NULL (ledger L-32, L-37). A null entry in an `Array` or `Map` of those types now stores NULL, where before it failed the whole batch with a `DataException` (ledger L-33, L-36).

The limitation that item 4 of the gg1 note describes is closed. Its startup WARN is gone for Enum columns (ledger L-34) and for Tuple columns (ledger L-39), so a log alert that matches the old text stops matching. The old text began like this, with the column's own database, table, column and type filled in:

```text
Column <db>.<table>.<column> has type <raw type>, which contains Nullable(Enum8), Nullable(Enum16) or Nullable(Tuple(...)).
```

**Rows stored by earlier releases are not repaired by the connector.** The connector does not rewrite stored data, and it does not remove phantom rows that an earlier release wrote. The queries below only help you find the rows to re-load.

What `v1.7.0_gg1` did with each shape, measured on ClickHouse 26.9.18.11 with client V2 and a plain `MergeTree` table:

- Top-level `Nullable(Enum8)` with a label in every record: every row was stored with NULL, with no error.
- Top-level `Nullable(Enum16)` with a label in every record: the batch failed with the server error `CANNOT_READ_ALL_DATA` and nothing was stored.
- `Array(Nullable(Enum8))` and `Map(String, Nullable(Enum8))` with labels and no null element: every element or value was stored as NULL, with no error.
- `Nullable(Tuple(a Nullable(Int32), b String))` with a non-NULL tuple in every record: each record was stored as a wrong row (tuple not NULL, `a` NULL, `b` empty), and for each record one more row appeared under a key that no record carries. No error was raised.
- `Nullable(Tuple(a Int32))`, a single field, with a null `a`: a NULL tuple was stored, with no error.

Earlier observations in the ledger add that `v1.7.0_gg1` failed a NULL in a top-level `Nullable(Enum8)` column with `An attempt to write null into not nullable column '<column>'` (ledger L-31), and failed a `Nullable(Tuple(a Int32, b String))` column with `CANNOT_READ_ALL_DATA` (ledger L-35). A batch that failed stored nothing, so there is nothing to find for those two shapes.

**One shape now fails where it used to store silently.** For a single-field `Nullable(Tuple(a T))`, a record that sends a null for the one field `a` now fails the batch with a `DataException`. Its text is `column '<column>' (Nullable(Tuple(...))) tuple field '<field>': null in non-nullable slot`. It names the column and the tuple field and never a value. With `errors.tolerance=none` the task stops, and stops again on every retry because the batch is rebuilt identically. With `errors.tolerance=all` the whole batch goes to the dead letter queue. The `DataException` is the root cause of a wrapping `RuntimeException`, so read the cause chain. A NULL for the whole tuple still stores NULL. `v1.7.0_gg1` stored a NULL tuple in this case and raised no error (ledger L-38).

**A new WARN.** When the declared type of a column that contains a Tuple cannot be parsed, or does not line up with the column's structure, the connector logs this line once per affected column each time it describes the table:

```text
Could not take element nullability for column <column> of table <db>.<table> from its declared type <type> (<reason>); keeping the nullability DESCRIBE reported for it
```

The reason is an exception class name, or `structure mismatch`. The line carries the column, database, table, declared type and reason, never a value. A column that logs it keeps the nullability that `DESCRIBE` reported and behaves as it did before the fix, so treat that column as still exposed to the old behaviour (ledger L-40).

### Am I affected?

List the candidate columns with the `system.columns` query of [item 4 of the gg1 note](ROLLOUT-v1.7.0_gg1.md#item-4-nested-null-dataexception-and-the-nullable-enumtuple-limitation); it works unchanged. Then run the query for each shape that applies. Replace `<db>`, `<table>` and `<column>`, and replace `<key_column>` with a column that identifies a row. Each query lists the key, the data part the row sits in and a reason. Each one only reads.

Every query here finds **suspects, not proof**: stored data cannot always tell a legitimate NULL from a wrongly stored one. Each was run on ClickHouse 26.9.18.11 against rows written by `v1.7.0_gg1` and against rows written by `v1.7.0_gg2` from the same records. It flagged the wrong rows of the first and nothing of the second, except the last query, where `v1.7.0_gg2` refuses the batch and stores no row. An empty result means that no row matches the shape. It does not show that the column is healthy.

**`Nullable(Tuple(...))` with nullable elements.** This lists tuples that are not NULL but whose elements are all NULL or empty, and it lists NULL tuples that sit in the same data part as such a suspect, because phantom rows carry a meaningless key and cannot be recognised by content. On the `v1.7.0_gg1` rows it flagged all three wrong rows and all three phantom rows:

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    if(<column> IS NULL,
       'NULL tuple in a part that also holds a suspect (possible phantom row)',
       'tuple is not NULL but every element is NULL or empty') AS reason
FROM `<db>`.`<table>`
WHERE _part IN (
        SELECT _part
        FROM `<db>`.`<table>`
        WHERE <column> IS NOT NULL
          AND toString(assumeNotNull(<column>)) = toString(defaultValueOfTypeName(toTypeName(assumeNotNull(<column>))))
    )
  AND (
        <column> IS NULL
        OR toString(assumeNotNull(<column>)) = toString(defaultValueOfTypeName(toTypeName(assumeNotNull(<column>))))
    )
ORDER BY part, row_key;
```

A hit can also be a legitimate tuple of NULL and empty string, or a legitimate NULL. An empty result does not rule out a wrongly stored tuple whose elements are not all NULL or empty.

**Top-level `Nullable(Enum8)` or `Nullable(Enum16)`.** One text serves both widths. It lists rows where the column is NULL, which is how `v1.7.0_gg1` stored every Enum8 label:

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'Nullable(Enum) column holds NULL' AS reason
FROM `<db>`.`<table>`
WHERE <column> IS NULL
ORDER BY part, row_key;
```

A hit is a suspect if your source never sends NULL for that column. For an Enum16 column, `v1.7.0_gg1` refused the batch instead of storing NULL, so a NULL there does not come from this defect.

**`Array(Nullable(Enum8))`.** This lists rows whose array holds a NULL element:

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'array holds a NULL element' AS reason
FROM `<db>`.`<table>`
WHERE arrayExists(x -> x IS NULL, <column>)
ORDER BY part, row_key;
```

**`Map(String, Nullable(Enum8))`.** This lists rows whose map holds a NULL value:

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'map holds a NULL value' AS reason
FROM `<db>`.`<table>`
WHERE arrayExists(x -> x IS NULL, mapValues(<column>))
ORDER BY part, row_key;
```

Both are suspects if your source never sends a null element or value.

**Single-field `Nullable(Tuple(a T))`.** This lists rows whose tuple is NULL, which is how `v1.7.0_gg1` stored a record whose one field was null:

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'single-field Nullable(Tuple) holds a NULL tuple' AS reason
FROM `<db>`.`<table>`
WHERE <column> IS NULL
ORDER BY part, row_key;
```

A hit is a suspect if your source never sends a NULL for the whole tuple or a null for its one field.

### What to do

- Re-load the rows that matter from the source. The connector does not rewrite stored data, so a suspect that turns out to be wrong stays wrong until you replace it.
- Ignore a hit that matches a legitimate NULL in your source.
- Check whether your producers can send a null for the one field of a single-field `Nullable(Tuple(...))`, and decide on `errors.tolerance` before you upgrade: `none` stops the task, `all` sends the batch to the dead letter queue.
- Remove log alerts that match the old startup WARN. Consider an alert on `Could not take element nullability`, because a column that logs it is still exposed to the old behaviour.

---

## Item 3: Empty Debezium records

### What changed

With `debeziumCDCEnabled=true`, an empty Debezium record no longer inserts a row on any route. An empty record is a truncate event (`op=t`) or an event whose value struct is null, for example an update with a null `after`. `v1.7.0_gg1` already skipped them on the explicit `insertFormat=json` route. `v1.7.0_gg2` now skips them also on the `bypassRowBinary=true` JSON route and on RowBinary (ledger L-30).

Before, the `bypassRowBinary=true` route stored an all-defaults row for every empty record: measured on `v1.7.0_gg1` and ClickHouse 26.9.18.11, the key was 0, the amount 0, the name empty, `_version` 0 and `is_deleted` 0. RowBinary stored such a row on tables with `DEFAULT` or `Nullable` columns, and failed the whole batch on a plain table with non-nullable columns.

The offsets, the exactly-once state and the deduplication token still cover the whole batch, skipped records included. A batch that holds only empty records sends an empty insert of 0 rows. JMX counters still count skipped records as processed. Each skipped record is logged once at DEBUG as `Skipping empty CDC record (no values) ...`, on every route. Phantom rows written before the upgrade are not removed.

### Am I affected?

You are affected if a connector runs with `debeziumCDCEnabled=true`. To find phantom rows written earlier, run this against the target table. Replace `<key_column>` with the column that holds the row key and `<version_column>` with the column that holds the source version (`_version` in the measured fixture):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'key and version are both 0 (possible phantom row of an empty CDC record)' AS reason
FROM `<db>`.`<table>`
WHERE <key_column> = 0
  AND <version_column> = 0
ORDER BY part, row_key;
```

**Empty result:** no row has both the key and the version at 0. That does not prove the table is clean, for example when your phantom rows carry a different default. **Non-empty result:** each row is a suspect, not proof, because a real row can legitimately have key 0; compare it with the source. On the `v1.7.0_gg1` fixture it listed both all-defaults rows, and on the rows `v1.7.0_gg2` wrote from the same records it listed nothing. If your table has no version column the query does not apply as written; compare row counts with the source instead.

### What to do

- Decide whether to remove existing phantom rows. This note gives no delete statement, because a hit is a suspect. Compare each one with the source, then change the table the way you normally do.
- Expect the DEBUG skip line on every route. No change is needed for new records.
- If a batch of such records was in flight during the upgrade, read item 1.

---

## Item 4: Error messages and log lines

### What changed

**Missing value for a non-nullable column (ledger L-25).** On the RowBinary route, a record that has no value for a non-nullable column without a default now fails with a `DataException` where it used to fail with a `RuntimeException` that had no message. This applies to flat columns when you write CDC records or set `bypassSchemaValidation=true`, and to any field inside an `Array(Tuple(...))`. The text is:

```text
column '<name>' (<type>): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT
```

A tuple field is named by its dotted path, and the type is the connector's own spelling, for example `INT32` for `Int32`. The text carries no record value. How the failure is handled does not change: `errors.tolerance=none` fails the task and `all` sends the batch to the dead letter queue. The column name is visible in the cause chain and the stack trace. The dead letter queue's exception-message header carries the query id instead, because the connector wraps every write failure in a `RuntimeException` whose message is the query id.

**The matching ERROR line is gone (ledger L-26).** The line `Column <name> is not nullable and no value is provided` is no longer logged. The failure is logged by the callers.

**DateTime parse failures (ledger L-27).** On the RowBinary route, a `DateTime` or `DateTime64` column that receives a string it cannot parse now logs the following, never the value. The old form was `Error parsing date time string: <value>`, followed for `DateTime64` by the exception text. The number is the length of the value in UTF-16 code units, so a character outside the Basic Multilingual Plane counts as 2:

```text
Error parsing date time string for column '<name>' (ClickHouse type <T>, Kafka type <K>): exception <exception class>, value length <n>
```

A matcher on the prefix `Error parsing date time string` still matches; one on the colon form does not.

**Date conversion failures (ledger L-28).** For `Date`, `Date32`, `DateTime` and `DateTime64` columns, the `DataException` `(Potentially) Not implemented conversion from <K> to <T>` now ends with ` for column '<name>'`. The same text is logged once as an ERROR.

**Generic write failures (ledger L-29).** When the write of a value throws, the ERROR line no longer carries a stack trace. It is now:

```text
Error writing value of <K> to the column `<col>` of type <T>: exception <class>
```

The exception that is rethrown, and its message, are unchanged. That is a known open item of the fork (see [`FORK-DELTA.md`](FORK-DELTA.md)): the message can still carry a value into the task status, the dead letter queue headers and the two `Error inserting records` ERROR lines. Do not assume that logs and headers are free of record values.

**Failures without a message (ledger L-23, L-24).** A writer failure whose exception has no message now surfaces as itself. Before, it was replaced by a `NullPointerException` with the text `Cannot invoke "String.contains(java.lang.CharSequence)" because the return value of "java.lang.Exception.getMessage()" is null` in the task status, the dead letter queue reason and the logs. A socket timeout without a message at that point is now retried where it used to fail the task (ledger L-23). In the error handler of every route and both clients, an `IOException` root cause without a message now goes through the `errors.tolerance` handling. Before, a `NullPointerException` left the handler: under `none` the task failed with it and the original failure was lost, and under `all` the batch went to the dead letter queue with the `NullPointerException` as its reason (ledger L-24).

### Am I affected?

Search your alert rules and log matchers for the old texts. This command only reads files:

```sh
grep -rnF -e 'is not nullable and no value is provided' -e 'Error parsing date time string:' -e 'String.contains' -e 'writes such slots without their null marker' <alert-rule-directory>
```

**Empty result:** no matcher in that directory uses a text this release removes or changes, and nothing needs to change. **Non-empty result:** each listed line uses one of them. The search compares literal text, so a matcher that writes a character of these texts with a regular-expression escape (for example the dot in `String.contains`) is not listed; read such rules yourself. Run on a scratch directory with one file per old text, the command listed each of them once, and it printed nothing for a directory that held only an unrelated rule.

### What to do

- Missing value: match the `DataException` text `no value provided for a non-nullable column without a default` instead of the removed ERROR line.
- DateTime parse failure: match the prefix `Error parsing date time string` without the colon.
- `NullPointerException` from a message-less failure: nothing replaces it. Remove the matcher, and alert on the original exception class instead.
- Startup WARN of the Nullable Enum and Tuple limitation: remove the matcher (item 2).

---

## Item 5: JMX MBean name and client name

### What changed

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both now carry `v1.7.0_gg2` where they carried `v1.7.0_gg1`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg2`, plus `,topic=<topic>` for per-topic beans. In the measured run, the per-task bean and the per-topic bean of `v1.7.0_gg2` both carried `version=v1.7.0_gg2`, and those of `v1.7.0_gg1` carried `version=v1.7.0_gg1`.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg2`. The client sends it as the prefix of the HTTP user agent. The rest of the user agent belongs to the client library and did not change between the two releases.

### Am I affected?

Search your dashboards, alert rules and JMX exporter rules for matchers that spell out `version=v1.7.0_gg1`, and for filters on the client name `clickhouse-kafka-connect/v1.7.0_gg1`. The query of [item 7 of the gg1 note](ROLLOUT-v1.7.0_gg1.md#item-7-jmx-mbean-name-and-client-name) lists the client names that your server has seen, and works unchanged. It showed `clickhouse-kafka-connect/v1.7.0_gg1` for the old build and `clickhouse-kafka-connect/v1.7.0_gg2` for the new one.

### What to do

Change matchers that spell out the old value. If you already use the wildcard forms from item 7 of the gg1 note, they keep working across releases and nothing needs to change.

---

## Item 6: Rollback

### What changed

Nothing in the data format is migrated, so a rollback is a code rollback. Tag `pre-v1.7.0_gg2` names commit `be6b8861`, the published `v1.7.0_gg1` tree with its corrected documents, and it is published together with this release. Item 8 of the gg1 note still describes the older rollback, to `pre-v1.7.0-sync`, which goes back to `v1.6.0_gg1`. The rollback in this item goes back to `v1.7.0_gg1`.

### Am I affected?

You are affected if you need to go back after upgrading, or if you want to know in advance what going back costs.

### What to do

1. Back out the whole release in one commit on `custom_build`, by restoring the tree of tag `pre-v1.7.0_gg2`:
   1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing. Commit whatever it lists, or set it aside with `git stash -u` (a plain `git stash` leaves untracked files behind), or work in a fresh clone. The next command discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
   2. Run `git read-tree -u --reset pre-v1.7.0_gg2`, then `git commit -m "Roll back v1.7.0_gg2"`. This commit is `<rollback-commit>` below; `git rev-parse HEAD` right after the commit prints it. `git diff --quiet HEAD pre-v1.7.0_gg2 && echo ok` prints `ok`.

   Use this form rather than `git revert -m 1` on the release merge. It restores a known tree and works on whatever tip `custom_build` has reached, including after later commits.
2. Redeploy the `v1.7.0_gg1` artifact.
3. Run the check of item 1 in reverse: stop cleanly, run the same KeeperMap query, and go back only when it returns nothing. This is the mirror image of item 1. A batch that `v1.7.0_gg2` left in `BEFORE_PROCESSING` is retried by `v1.7.0_gg1` under the same `insert_deduplication_token`. By the rule measured in item 1, a retry under the same token is dropped on a table that deduplicates. So if the `v1.7.0_gg2` attempt landed, the `v1.7.0_gg1` retry is dropped and the rows of `v1.7.0_gg2` stay. If it did not land, the `v1.7.0_gg1` block lands once with the old encoding, which brings back the wrong values of item 2 and the phantom rows of item 3 for that range.

To go forward again, revert the rollback commit with `git revert <rollback-commit>`. The rollback removes this document from the branch; read it again with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg2.md`.

Rolling back brings back:

- the `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` defects of item 2, together with their startup WARN;
- the all-defaults rows from empty Debezium records on the `bypassRowBinary=true` route and on RowBinary (item 3);
- the `NullPointerException` that masks failures without a message, and DateTime parse logs that carry the value (item 4);
- the `v1.7.0_gg1` MBean version and client name (item 5);
- on the branch, the CI workflow as it was before its hardening: the floating `ubuntu-latest` runner label and actions pinned by tag instead of by commit.
