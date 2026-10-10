# Rollout checklist: `v1.6.0_gg1` to `v1.7.0_gg1`

This checklist is for anyone building or running the connector from this fork who is moving from `v1.6.0_gg1` to `v1.7.0_gg1`. Release `v1.7.0_gg1` builds on upstream `1.7.0`. The fork's own changes since `v1.6.0_gg1` are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and the full list of what the fork patches on top of upstream is in [`FORK-DELTA.md`](FORK-DELTA.md).

It is a checklist, not a deployment record: it names no host, connector, cluster or production version. Every environment-specific value is a placeholder such as `<db>`, `<connector-name>`, `<zkDatabase>`, `<topic>` or `<partition>`. Replace them with your own.

Each item has three parts:

- **What changed**: the behaviour difference between the two releases.
- **Am I affected?**: a self-check you can run against your own configuration or your own ClickHouse server. Every query says what an empty result means.
- **What to do**: the action if the check shows you are affected.

Tags such as (ledger L-20) point at entries of the fork's behaviour ledger, [`13-BEHAVIOUR-LEDGER.md`](../.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md), kept in this repository's planning directory; this document restates everything an operator needs from it. ClickHouse versions are written as major.minor.

The items, in the order you should work through them:

1. [Exactly-once pre-upgrade check](#item-1-exactly-once-pre-upgrade-check)
2. [Struct and CDC records with an explicit JSON insert format](#item-2-struct-and-cdc-records-with-an-explicit-json-insert-format)
3. [JSON-route type notes](#item-3-json-route-type-notes)
4. [Nested-null DataException and the Nullable Enum/Tuple limitation](#item-4-nested-null-dataexception-and-the-nullable-enumtuple-limitation)
5. [Int256 error message and Array(Tuple) missing fields](#item-5-int256-error-message-and-arraytuple-missing-fields)
6. [Compression](#item-6-compression)
7. [JMX MBean name and client name](#item-7-jmx-mbean-name-and-client-name)
8. [Rollback](#item-8-rollback)

---

## Item 1: Exactly-once pre-upgrade check

### What changed

With `exactlyOnce=true`, the connector records each batch's offset range in a ClickHouse KeeperMap table before it inserts (`BEFORE_PROCESSING`) and after the insert succeeds (`AFTER_PROCESSING`). A batch the old jar left in `BEFORE_PROCESSING` is retried by whichever jar starts next. ClickHouse block deduplication then drops the retry only if the retried block matches the earlier attempt. If the new jar encodes the same batch differently, deduplication does not match and rows can be written twice.

Which routes re-encode a batch that the old jar could already have completed:

| Route (records are `Struct`/CDC unless noted) | Re-encoded since `v1.6.0_gg1`? | Check |
|---|---|---|
| Explicit `insertFormat=json` together with `bypassRowBinary=true` | Yes. In `v1.6.0_gg1` this wrote rows through the legacy Jackson path. It now uses the metadata-free writer, so Timestamp, Date, Time, Decimal and bytes values change and empty CDC records are skipped (ledger L-22, and L-20 for Debezium timestamps). | **Required** when `exactlyOnce=true` |
| Explicit `insertFormat=json` with `bypassRowBinary` unset or `false` | No batch to match: in `v1.6.0_gg1` this route wrote nothing (item 2). | Precautionary |
| `bypassRowBinary=true` without an explicit `insertFormat=json` | Unchanged; the only difference is that a null non-nullable Map in a field cleanup now becomes `{}`, which stores the same empty map. | Precautionary |
| RowBinary (the default for Avro and Protobuf) | The upstream fix for a null optional Map used to emit stray bytes (a desynchronised stream, so an error or garbage, never a completed batch to match). The nested empty-container coercion either failed server-side before (nothing stored) or produced the same bytes by accident. | Precautionary |
| Schemaless JSON and String records | Unchanged. | Precautionary |
| Compression settings (item 6) | Transport framing only; the stored values are identical. | Precautionary |

The required/precautionary split rests on an inference: ClickHouse block deduplication hashes the parsed data of the inserted part (column names, types and the data stream), not the bytes of the HTTP request. This is inferred from the ClickHouse documentation of `replicated_deduplication_window`, not measured here. If you prefer to be conservative, treat every row of the table as required. Also note that non-replicated MergeTree tables deduplicate only when `non_replicated_deduplication_window` is set (the default is 0, meaning off), so for those tables exactly-once never rested on deduplication in the first place.

### Am I affected?

You are affected if any connector runs with `exactlyOnce=true`. Then:

1. Stop or pause the connector cleanly.
2. In the connector's database, query the KeeperMap state table. The table is named by the connector's `zkDatabase` setting (default `connect_state`) and stored under its `zkPath` (default `/kafka-connect`). **The table is shared:** every `exactlyOnce` connector with the same `zkDatabase` and `zkPath` writes into it, and a row's `key` names a topic and partition, not a connector. Scope the query to the topics this connector consumes:

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

The `key` column holds `<topic>-<partition>`. The `arrayStringConcat(...)` expression strips the trailing `-<partition>`, so `'orders'` matches `orders-0` but not `orders-v2-0`. With `enableDbTopicSplit=true` the topic part is prefixed by `<db>` and the configured `dbTopicSplitChar`, so list `'<db><dbTopicSplitChar><topic>'` instead of `'<topic>'`. Without the topic filter the result also lists the in-flight batches of every other connector on the table and may never become empty. The state values the connector writes are `BEFORE_PROCESSING` and `AFTER_PROCESSING`.

**Empty result:** no batch is in flight and nothing is waiting for a retry, so it is safe to upgrade. **Non-empty result:** each listed `key` has a batch that may or may not have reached ClickHouse; do not upgrade yet.

The query is derived from the connector's source (the table is created as `` `key` String, minOffset BIGINT, maxOffset BIGINT, state String `` with the KeeperMap engine). KeeperMap needs `keeper_map_path_prefix` configured on the server, so the query was not executed against a live KeeperMap table when this document was written; run it on your own server and report any mismatch.

### What to do

- Result empty: upgrade.
- Result non-empty: start the old `v1.6.0_gg1` jar again, let those ranges reach `AFTER_PROCESSING`, stop it cleanly and run the query again. Repeat until it is empty, then upgrade.
- A row for a topic or partition the connector no longer consumes stays in `BEFORE_PROCESSING` and never clears. Confirm that no running connector owns it, then ignore it, or delete that one key with the item 2 `DELETE` example.
- The check is **required** for connectors that combine explicit `insertFormat=json` with `bypassRowBinary=true` and write `Struct`/CDC records under `exactlyOnce=true`, because that is the one route whose stored values change for a batch the old jar could have completed. It is **precautionary** for every other route in the table above.

---

## Item 2: Struct and CDC records with an explicit JSON insert format

### What changed

With an explicit `insertFormat=json` (any letter case), `Struct` and Debezium CDC records now go to the JSON writer and land rows (ledger L-01). In `v1.6.0_gg1` they went to RowBinary against a column-less synthetic table: the insert ran, the connector marked the offset range `AFTER_PROCESSING`, and no row was stored. The change is unconditional, has no opt-out flag, and is one-way.

### Am I affected?

Search your connector configurations for `insertFormat=json` together with a value converter that produces schema'd records (Avro, Protobuf, JSON with schemas, or Debezium envelopes):

```sh
grep -rniE 'insertFormat"?[[:space:]]*[:=][[:space:]]*"?json' <connector-config-directory>
```

**Empty result:** no connector uses this route and the item does not apply to you. **Non-empty result:** check which of those connectors read `Struct`/CDC records. If you ran any of them against `v1.6.0_gg1`, rows may have been silently lost.

### What to do

Step 1 always; step 2 only for connectors with `exactlyOnce=true`. To recover the lost data, run them in this order:

1. Stop the connector, then reset the consumer group offsets of the affected topic-partitions to the position from which data must be recovered.
2. Clear the KeeperMap state rows for those topic-partitions. Skipping this step makes the connector see the replayed ranges as already stored (state `AFTER_PROCESSING`) and drop them.

Step 2 as an example (not run, adjust to your own topic-partitions):

```sql
-- EXAMPLE: delete the state rows of the affected topic-partitions
-- (with enableDbTopicSplit=true each key is '<db><dbTopicSplitChar><topic>-<partition>')
DELETE FROM `<zkDatabase>` WHERE `key` IN ('<topic>-<partition>', '<topic>-<partition>');

-- EXAMPLE: or delete the state rows of every partition of the affected topics
DELETE FROM `<zkDatabase>` WHERE arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>');
```

Do not `TRUNCATE` the table. It is shared by every `exactlyOnce` connector with the same `zkDatabase` and `zkPath` (item 1), so truncating it also erases their state. A batch another connector left in `BEFORE_PROCESSING` is then no longer retried as the same block, and deduplication cannot catch it.

Then start the connector. Rolling back to `v1.6.0_gg1` re-introduces the silent loss (item 8). If no connector combines an explicit `insertFormat=json` with schema'd records, there is nothing to recover.

---

## Item 3: JSON-route type notes

### What changed

The JSON routes encode some Kafka types differently from RowBinary. The explicit `insertFormat=json` route is the metadata-free writer, which runs no `DESCRIBE` and encodes without column metadata. Differences:

- **Encodings (ledger L-02).** Timestamp is an unquoted decimal-seconds number with exactly three fractional digits; Date is an unquoted integer count of epoch days; Time is its integer milliseconds; Decimal is a JSON number; bytes are raw bytes inside a JSON string (quote, backslash and control bytes escaped), never base64. A `ByteBuffer` emits its remaining bytes, whereas RowBinary writes the whole backing array; the two differ when the buffer position is not zero.
- **Upstream bypass route unchanged (ledger L-03).** `bypassRowBinary=true` without an explicit `insertFormat=json` keeps Jackson's defaults: epoch milliseconds that ClickHouse reads as seconds, and base64 bytes. This differs from the explicit json route above and is deliberate, so upstream's bytes do not change.
- **Date into DateTime (ledger L-04).** On the metadata-free route a Kafka Date written to a `DateTime` or `DateTime64` column lands its day count as seconds (a 1970 timestamp), because the same literal must also serve `Date` and `Date32`. Date into `Date` and `Date32` is correct.
- **CDC conversion scope (ledger L-05, narrowed by L-20).** Connect Timestamp, `io.debezium.time.Date` and `io.debezium.time.MicroTimestamp` are converted. As of this release `io.debezium.time.Timestamp` (epoch milliseconds) and `io.debezium.time.NanoTimestamp` (epoch nanoseconds) are converted too (ledger L-20): they are written as decimal seconds with three and nine fractional digits. Before, the raw number passed through and ClickHouse read it as seconds: a `DateTime64` column silently clamped to the maximum date for `Timestamp`, and the server rejected a `NanoTimestamp` batch with `DECIMAL_OVERFLOW`. Small `NanoTimestamp` values appear in scientific notation (for example `1E-9`); the server accepts them. Only `io.debezium.time.ZonedTimestamp` strings still pass through unchanged: they parse only when the server's `date_time_input_format` is `best_effort` and fail when it is `basic`. `best_effort` is the default on ClickHouse 26.9, the version this was checked on; older ClickHouse releases default to `basic`. Check the value (below) rather than assume it.
- **Null handling (ledger L-08).** With all top-level fields null: `Nullable(Int32)` stores NULL, a `Nullable` column with a `DEFAULT` stores the default, non-nullable columns store their default or type default. Nested `null` in `Array(Int32)` or `Map(String, String)` is coerced to the element default. These non-nullable outcomes rely on the server keeping `input_format_null_as_default=1` (the default). The connector deliberately does not force it; a server or profile that sets it to 0 changes them.
- **Explicit `insertFormat=json` plus `bypassRowBinary=true` (ledger L-22).** In `v1.6.0_gg1` this combination used the legacy Jackson path. It now uses the metadata-free writer, so stored Timestamp, Date, Time, Decimal and bytes values change for the same input (ledger L-02) and an empty CDC record is skipped instead of producing an all-defaults row. Someone who set both options to work around item 2 sees different stored values after the upgrade. No automated test pins stored values for exactly this combination (only the routing is pinned), so verify it on a copy of your data before relying on it.

### Am I affected?

Search your configurations for the options involved:

```sh
grep -rniE 'insertFormat"?[[:space:]]*[:=][[:space:]]*"?json|bypassRowBinary"?[[:space:]]*[:=][[:space:]]*"?true' <connector-config-directory>
```

**Empty result:** neither JSON route is in use and this item does not apply. **Non-empty result:** note which connectors set `insertFormat=json` only, `bypassRowBinary=true` only, or both. Both together is the case of the last note above (ledger L-22) and also triggers the required check in item 1.

Then check the two server settings the notes depend on, as the user the connector connects as:

```sql
SELECT name, value, changed
FROM system.settings
WHERE name IN ('date_time_input_format', 'input_format_null_as_default');
```

**Empty result:** your server is too old for these settings and the notes about them do not apply. **Two rows:** decide on `value`, not on `changed`, because the defaults differ between ClickHouse releases. `date_time_input_format = 'basic'`, changed or not, means Debezium `ZonedTimestamp` strings will be rejected; `best_effort` means they parse. `input_format_null_as_default = 0` means the null coercions above do not hold; `1` means they hold.

### What to do

- Compare a sample of stored values for each affected connector against its source, especially Timestamp, Date, Time and bytes columns, and Debezium Timestamp/NanoTimestamp columns.
- If you wrote `DateTime` columns from Kafka Date fields on the metadata-free route, correct that mapping (ledger L-04).
- Keep `date_time_input_format` at `best_effort` for users that carry `ZonedTimestamp` data, and keep `input_format_null_as_default` at `1`.
- For a connector that sets both options under `exactlyOnce=true`, run the check of item 1 first (ledger L-22).

---

## Item 4: Nested-null DataException and the Nullable Enum/Tuple limitation

### What changed

**Stricter nested nulls (ledger L-10, L-11, L-13, L-21).** On the RowBinary route, a null in a nested non-nullable scalar or Tuple slot (an `Array` element, a `Map` value, a `Tuple` field) now throws `DataException`, with the message `column '<name>' (<rendered type>) <slot>: null in non-nullable slot`. The message names the column and slot only, never a value. The whole batch fails and no row is written. A null `Array` or `Map` in a nested non-nullable slot is coerced to an empty container instead (ledger L-09). Top-level nulls are unchanged: a null in a non-nullable scalar column still raises upstream's `RuntimeException("An attempt to write null into not nullable column '<name>'")` (ledger L-12).

How the failure surfaces: with `errors.tolerance=all` the whole batch goes to the dead letter queue; with `errors.tolerance=none` the task stops, and stops again on every retry because the batch is rebuilt identically. The `DataException` is the root cause of a wrapping `RuntimeException`, so read the cause chain.

**Known limitation, not fixed in this release.** The connector does not write the null marker for `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` slots, at the top level or nested in `Array`, `Map` or `Tuple`. Values in such a slot may be stored as NULL or shifted with no error at all, and null entries fail the batch with `DataException`. The defect is also present in upstream `1.7.0`. It is silent for non-null values, so it is not enough to rely on the exception.

The connector logs a startup WARN once per column and process (ledger L-21). Its text:

```
Column <db>.<table>.<column> has type <raw type>, which contains Nullable(Enum8), Nullable(Enum16) or Nullable(Tuple(...)). The connector writes such slots without their null marker, so values in that slot may be stored as NULL or shifted with no error, and a null entry fails the batch with DataException. See Known costs in docs/FORK-DELTA.md (backlog 999.1).
```

The WARN is raised when the connector discovers table metadata, so it also appears for tables the connector never writes (the `targetTableFilter` setting narrows discovery), and for connectors whose route cannot hit the defect (schemaless JSON, String records, `bypassRowBinary=true`). The metadata-free route (explicit `insertFormat=json`) runs no `DESCRIBE`, so it never warns.

### Am I affected?

```sql
SELECT database, table, name, type
FROM system.columns
WHERE database = '<db>'
  AND (type LIKE '%Nullable(Enum%' OR type LIKE '%Nullable(Tuple%');
```

**Empty result:** no column in `<db>` has the affected type shape, so the limitation does not apply and the only change for you is the stricter nested-null failure. **Non-empty result:** each listed column is exposed if the connector writes that table over RowBinary (Avro, Protobuf or other schema'd records without `bypassRowBinary=true`).

For the stricter nested nulls, check whether your producers can emit a null element, map value or tuple field into a non-nullable nested slot. If you cannot rule it out, decide before the upgrade whether you want `errors.tolerance=all` with a dead letter queue (the whole batch is dead-lettered) or a stopping task.

### What to do

- For a listed column that the connector writes over RowBinary: change the target type (for example drop the `Nullable` wrapper and use a default value), or write through the JSON route, and verify stored values by reading them back.
- Decide on `errors.tolerance` for nested-null failures and monitor the dead letter queue accordingly.
- Do not treat the absence of a `DataException` as proof of correct storage for columns returned by the query above.

---

## Item 5: Int256 error message and Array(Tuple) missing fields

### What changed

**Int256 and UInt256 (ledger L-15).** RowBinary cannot write `Int256` or `UInt256`. For a non-nullable, no-default root column of those types the operator-visible error changes:

- Before (`v1.6.0_gg1`): `Data schema validation failed.`
- Now: `Column [<name>] has unsupported type [INT256] for RowBinary serialization. Use insertFormat=json or bypassRowBinary=true instead.` (`[UINT256]` for `UInt256`). The message never contains a field value.

`Nullable` or defaulted `Int256`/`UInt256` columns, and every run with `bypassSchemaValidation=true`, skip that validation and fail mid-stream with the same message. `Int128` and `UInt128` are unaffected.

**Array(Tuple) with a missing field (ledger L-17).** Array element columns now carry the parent's default and sub-column flags. For `Array(Tuple(...))`, a record that lacks a tuple field which has no default now fails loudly (an `ERROR` log line `Column <name> is not nullable and no value is provided` and a `RuntimeException` without a message) instead of writing a stray `0x01` marker.

### Am I affected?

```sql
SELECT database, table, name, type
FROM system.columns
WHERE database = '<db>'
  AND (type LIKE '%Int256%' OR type LIKE '%UInt256%' OR type LIKE '%Array(Tuple%');
```

**Empty result:** no such column exists, so neither change applies. **Non-empty result:** for `Int256`/`UInt256` columns, the connector could not write them over RowBinary before either, only the message differs, so update any alert or log matcher that keyed on `Data schema validation failed.`. For `Array(Tuple` columns, check whether your records can omit a tuple field that has no default.

### What to do

- For `Int256`/`UInt256`: set `insertFormat=json` or `bypassRowBinary=true` for that connector, as the message says.
- For `Array(Tuple`: make sure every tuple field is present in every record, or give the field a default in the table definition.

---

## Item 6: Compression

### What changed

The fork's forced `network_compression_method=lz4` setting is retired onto upstream's `setCompressionMethod` option (default `true`):

- With `setCompressionMethod` at its default, `network_compression_method=lz4` is sent on every client the connector builds.
- `compress=false` is sent unconditionally on every client.
- A `compress=true` in `jdbcConnectionProperties` re-enables response compression on client V2 and wins over the connector's `compress=false`. Against a server whose default is ZSTD (ClickHouse 26.9 and later), at least one of the two controls (the lz4 setting or `compress=false`) must be in effect, otherwise reads fail with `Invalid LZ4 magic byte`. Do not combine `compress=true` with `setCompressionMethod=false` on such a server.

The lz4 library is also updated; see the CHANGELOG. Stored values are unaffected by compression.

### Am I affected?

Search the configurations:

```sh
grep -rniE 'compress=true|setCompressionMethod"?[[:space:]]*[:=][[:space:]]*"?false|set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false' <connector-config-directory>
```

**Empty result:** the defaults apply and nothing is required. **Non-empty result:** a connector with `compress=true` in `jdbcConnectionProperties` and `setCompressionMethod=false` is at risk on a ClickHouse 26.9 or later server.

Then check whether a server profile pins or constrains the setting the connector sends per request:

```sql
SELECT profile_name, user_name, role_name, setting_name, value, min, max, writability
FROM system.settings_profile_elements
WHERE setting_name = 'network_compression_method';
```

**Empty result:** no profile touches `network_compression_method`, and the connector's per-request value is accepted. **Non-empty result:** a constraint that forbids changing the setting (a constant or read-only writability) is expected to reject the connector's per-request `lz4`. That effect was not probed when this document was written, so treat the query as a check, not a verdict.

### What to do

- Leave `setCompressionMethod` at `true` unless you have a reason not to.
- If you set `compress=true` in `jdbcConnectionProperties`, keep `setCompressionMethod=true`.
- If a profile constrains `network_compression_method`, either allow `lz4` for the connector's user or test the connector against that profile before the upgrade.

---

## Item 7: JMX MBean name and client name

### What changed

The MBean name embeds the release version:

- Before: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.6.0_gg1` (plus `,topic=<topic>` for per-topic beans).
- After: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg1` (plus `,topic=<topic>`).

The `connector=<connector-name>` segment is unchanged since `v1.6.0_gg1`, but it is absent in upstream builds, so a dashboard built for upstream does not match. Only the `version=` segment changes between these two releases, and it changes with every release.

The client name the connector sends on every request also changes, to `clickhouse-kafka-connect/v1.7.0_gg1`.

### Am I affected?

Search your dashboards, alert rules and JMX exporter rules for matchers on the full MBean name (anything that spells out `version=v1.6.0_gg1`). Also search for filters on the client name, which the client sends as the prefix of the HTTP user agent and which `system.query_log` records in `http_user_agent`:

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

**Empty result:** the connector's requests do not appear in the query log of this server in that window (or logging is off), so there is nothing to compare; check your own filter definitions instead. **Non-empty result:** each distinct value is a release the server has seen; any filter that spells out the old value needs the new one.

### What to do

Stop matching on the exact `version=` value in dashboards, alerts and exporter rules, so the next release does not break them. The wildcard syntax depends on the kind of matcher:

- JMX `ObjectName` pattern: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask*,version=*,*`. The trailing `,*` covers the other properties, such as `topic=`.
- Regular expression (JMX exporter rules, Prometheus relabelling, most dashboard matchers): `version=[^,]*`. In a regular expression `version=*` means "version followed by zero or more `=`", so a rule copied from the `ObjectName` form silently stops matching.

Update filters on the client name the same way.

---

## Item 8: Rollback

### What changed

Nothing in the data format is migrated, so a rollback is a code rollback. Tag `pre-v1.7.0-sync` (`c61d90ce`) names the previous release tree, which is the tree of the `v1.6.0_gg1` release.

### Am I affected?

You are affected if you need to go back after upgrading, or you want to know in advance what going back costs.

### What to do

1. On `custom_build`, back out the whole release in one commit: `git revert -m 1 <merge-commit>`, where `<merge-commit>` is the `v1.7.0_gg1` merge commit, tagged `v1.7.0_gg1`.
2. Redeploy the `v1.6.0_gg1` artifact.
3. Run the exactly-once check of item 1 in reverse: stop cleanly, run the same query, and go back only when no row is outside `AFTER_PROCESSING`. A batch the new jar left in `BEFORE_PROCESSING` is then retried by the old jar.

To go forward again after `git revert -m 1`, revert the revert commit first (`git revert <revert-commit>`); merging `sync/v1.7.0` again without that brings nothing back.

Rolling back re-introduces the silent loss of item 2 (explicit `insertFormat=json` with `Struct`/CDC records writes no rows while the offsets still advance) and the Debezium Timestamp mis-storage of item 3 (millisecond values read as seconds). It also brings back the old error message for `Int256`/`UInt256` columns and the less strict nested-null handling.
