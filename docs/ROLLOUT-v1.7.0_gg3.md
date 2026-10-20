# Rollout checklist: `v1.7.0_gg2` to `v1.7.0_gg3`

If you are coming from a release before `v1.7.0_gg2`, work through [`ROLLOUT-v1.7.0_gg2.md`](ROLLOUT-v1.7.0_gg2.md) first, which in turn sends you to [`ROLLOUT-v1.7.0_gg1.md`](ROLLOUT-v1.7.0_gg1.md), and return here afterwards.

This note covers only what changed between `v1.7.0_gg2` and `v1.7.0_gg3`. Both releases are built on the same upstream base, `1.7.0`. The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream.

Like its predecessors, this is a checklist and not a deployment record. It names no host, connector, cluster or production system, and every environment-specific value is a placeholder: `<db>`, `<zkDatabase>`, `<topic>`, `<connector-name>`, `<alert-rule-directory>`, `<connector-config-directory>`, `<connector-log-file>` or `<rollback-commit>`. Replace each one with your own value. In the `text` blocks, other angle-bracket words stand for the part that the connector or the server fills in.

Every item has the same three parts:

- **What changed** describes the difference in behaviour between the two releases.
- **Am I affected?** gives a check you can run against your own configuration, your own logs or your own ClickHouse server, and says what an empty result means.
- **What to do** gives the action when the check shows that you are affected.

Tags such as (ledger L-42) point at entries in the fork's two behaviour ledgers, `22-BEHAVIOUR-LEDGER.md` and `23-BEHAVIOUR-LEDGER.md`, which are kept in this repository's planning directory. This note restates everything an operator needs from them. Where a statement rests on a measurement, the ClickHouse server version it was measured on is named.

The items, in the order to work through them:

1. [Record values in writer error text](#item-1-record-values-in-writer-error-text)
2. [The unpaired-element WARN and Tuple elements named size](#item-2-the-unpaired-element-warn-and-tuple-elements-named-size)
3. [Backslashes and line breaks in names](#item-3-backslashes-and-line-breaks-in-names)
4. [In-flight batches](#item-4-in-flight-batches)
5. [JMX MBean name and client name](#item-5-jmx-mbean-name-and-client-name)
6. [Dependency updates](#item-6-dependency-updates)
7. [Rollback](#item-7-rollback)

---

## Item 1: Record values in writer error text

### What changed

When the connector cannot convert one record value into a ClickHouse column on the RowBinary route, on both clients, the error text no longer carries the value. Before, the library's own message was kept, and messages such as `Invalid UUID string` or `For input string` repeat the value they could not convert. That message reached the Connect task status, the stack-trace header of the dead letter queue (DLQ) and the `Error inserting records` ERROR logs (ledger L-42). It applies to any failure of a value conversion inside the writer: an invalid UUID string, a number that cannot be parsed or is too wide for `INT128` or `UINT128`, a `Decimal` out of range, a `FixedString` that is too long, an unknown `Enum` label.

The writer now fails with a `DataException` whose text names the column, the Kafka type, the Java class of the value and the ClickHouse type, and never the value or its length:

```text
Failed to write column `<column>`: Kafka value of type <Kafka type> (value class: <Java class>) could not be written as ClickHouse type <Type>: <exception class>
```

`<Type>` is the connector's own spelling of the ClickHouse type, for example `UUID`, `INT128` or `Decimal`, not `DECIMAL` (ledger L-45). The stack traces in the task status, in the DLQ header and in the `Error inserting records` logs now end like this, followed by the original frames, so the line of code that failed is still visible (ledger L-43):

```text
Caused by: <exception class> (message withheld: may contain record data)
```

A renderer that prints the class name and the message of the cause shows `com.clickhouse.kafka.connect.util.WithheldMessageException: (message withheld: may contain record data)`. Suppressed exceptions are not shown.

**What does not change in the DLQ.** With `errors.tolerance=all`, the header `__connect.errors.exception.class.name` stays `java.lang.RuntimeException`, and the header `__connect.errors.exception.message` stays the text `Topic: [..], Partition: [..], MinOffset: [..], MaxOffset: [..], (QueryId: [..])`. The column, the ClickHouse type and the exception class are in the header `__connect.errors.exception.stacktrace` (ledger L-48).

**What passes through unchanged.** A failure whose cause chain holds an `IOException`, any Connect exception (a `DataException` included) and a plain `RuntimeException` raised by the connector itself keep their text, so the retry handling still recognises network and timeout failures (ledger L-42).

**Grep targets.** `Failed to write column ` matches the new text and also the older text the connector already used when a value of the wrong Java type reached a column (`Failed to write column ... cannot convert Kafka value of type ...`). The line `Error writing value of` is unchanged (ledger L-45). The library messages, such as `Invalid UUID string` and `For input string`, no longer appear in these places, so an alert that matches them stops matching.

**Client V1 only.** A record value that itself contains `ClickHouseException: Code: 33` or `ClickHouseException: Code: 131` no longer triggers one table-mapping refresh and one retry of the insert. Before, the library message carried the value and matched the text that starts that refresh. Client V2 never did, and real server and client failures still start it. You notice one `Error code 33. Trying to update table mapping` WARN less for such a record (ledger L-44).

**The exception to "no record value".** Measured on ClickHouse 26.9.19.9 with client V2, through the connector's `put()` with `errors.tolerance=none` and with `all`: the only exception we know of is a Map key, on connectors whose `clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true` and whose records carry a value Jackson cannot serialize. On that route a JSON column is written by the JSON library, and its failure text is passed on unchanged. The text names the path to the entry it could not serialize, and a Map key on that path is record data:

```text
Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class java.lang.Object and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: java.util.HashMap["<field name>"]->java.util.HashMap["<map key>"])
```

In the measurement the Map key reached three places and did not reach two:

| Place | Map key | Any value |
|-------|---------|-----------|
| The cause chain that `put()` throws, shown in the Connect task status | yes | no |
| The DLQ header `__connect.errors.exception.stacktrace` | yes | no |
| The throwable of the `Error inserting records` ERROR logs | yes | no |
| The DLQ header `__connect.errors.exception.message` | no | no |
| The `Error writing value of` log line | no | no |

No value of a Map, no value of a Struct field and no element of a list appeared anywhere: for a list, the text names an index (`java.util.ArrayList[1]`), not the element. The measurement used Connect value types only. A custom object whose own getter or exception text carries data is not a Connect value type and was not measured. The route is opt-in: with the setting absent, a JSON column fails earlier, in schema validation, with a text that carries no record value.

### Am I affected?

Two checks. Both only read files.

1. Does a connector use the route of the exception? Replace `<connector-config-directory>` with the directory that holds your connector configurations:

```sh
grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>
```

**Empty result:** no configuration file in that directory sets the option, so the route is off for those connectors and the statement "no longer carries the record value" holds for them without an exception. **Non-empty result:** each listed line is a connector whose `clickhouseSettings` mentions the option. It is on the route when the value is `1` or `true`. The search covers files only. If you create connectors through the Connect REST interface and keep no file, read each connector's stored configuration the way you usually do and look for the same name.

2. Does an alert rule or log matcher depend on a library message that no longer appears? Replace `<alert-rule-directory>` with the directory that holds your rules:

```sh
grep -rnF -e 'Invalid UUID string' -e 'For input string' <alert-rule-directory>
```

**Empty result:** no matcher in that directory uses either text, and nothing needs to change. **Non-empty result:** each listed line uses a text that this release no longer produces for these failures. The search compares literal text, so a matcher that writes a character of these texts with a regular-expression escape (for example `Invalid\sUUID\sstring`) is not listed; read such rules yourself. Run on scratch directories, the command listed each of the two texts once, printed nothing for a directory that held only an unrelated rule, and printed nothing for a directory whose only rule wrote the space as `\s`.

### What to do

- Alert on `Failed to write column ` instead of the library messages.
- For a connector with the option set, treat the three places of the table above as able to carry a Map key, and handle them as you handle record data.
- On client V1, expect one `Error code 33. Trying to update table mapping` WARN less for a record whose text contains the key.

---

## Item 2: The unpaired-element WARN and Tuple elements named size

### What changed

`v1.7.0_gg3` logs a new WARN. It appears when a Tuple element that a column's declared type names has no matching sub-column in the table description that the connector reads from the server (ledger L-51):

```text
Declared Tuple element(s) [<element path>] of column <column> of table <db>.<table> (declared type <type>) have no matching DESCRIBE sub-column; applying the declared nullability to the elements that paired
```

The line is logged once per affected column each time the connector describes a table, with no throttling, and it carries the element paths, the column, the database, the table and the declared type, never a value. A line break in the declared type prints as `\n`. The connector describes every table of the database that its `targetTableFilter` lets through, at task start and at every mapping refresh, not only the tables that its topics write to, so the line can name a table that this connector never writes.

**What the WARN marks: a known defect that is older than this release.** The table description drops a Tuple element named `size` or `size0` at any depth of a column's type. It also drops an element named `size` followed by digits (`size1`, `size12`) wherever an `Array` encloses its Tuple. The connector then writes fewer fields than the table declares, and every insert into that table fails with the server error code 32 (`ATTEMPT_TO_READ_AFTER_EOF`) or code 33 (`CANNOT_READ_ALL_DATA`) and stores no row. Measured on ClickHouse 26.9.19.9 with client V2, one record per table, on `v1.7.0_gg2` and on `v1.7.0_gg3` alike:

- code 32 for `Tuple(size UInt16, v String)`, `Array(Tuple(size UInt16, v String))` and `Array(Tuple(size1 UInt16, v String))`;
- code 33 for `Array(Tuple(v String, size1 UInt16))`;
- the stored row for `Tuple(size1 UInt8, v String)`, `Map(String, Tuple(size1 UInt8, v String))` and `Tuple(sizes UInt8, v String)`, which are not affected.

`v1.7.0_gg2` fails the same way and logs no WARN, so a table that the new WARN names may already have been failing before you upgrade. A wrongly stored value is possible in principle when the shorter row happens to parse; that case was not measured. The server refuses to create a table with a Tuple element named `null` (code 36), so no such table exists to be affected.

### Am I affected?

Two checks, both read-only. The first reads the declared type text of your columns and lists suspects. Replace `<db>` with the database the connector writes to:

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '[(,] ?size0? [A-Za-z]')
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''),
                 'Array\\((?:[^()]|\\((?:[^()]|\\((?:[^()]|\\([^()]*\\))*\\))*\\)|\\()*?[(,] ?size[0-9]+ [A-Za-z]')
      )
ORDER BY `table`;
```

**Empty result:** no column of `<db>` has a declared type with an element of that shape, as far as the type text shows. **Non-empty result:** each listed table is a suspect. The query reads text, so it has one known over-flag: it also lists a table such as one with a column of type `Array(Map(String, Tuple(size1 UInt8)))`, which the connector does not affect. Run on the server named above, it listed the thirteen affected fixture tables and that one over-flag, and nothing for a database that held nineteen look-alike tables (names such as `size_x`, `sizes` and `Size`, a plain column named `size`, and an `Enum` label that holds the text `size`).

The second check is exact, because the line comes from the same code path as the insert. Replace `<connector-log-file>` with a log file of a `v1.7.0_gg3` connector that covers a task start:

```sh
grep -F 'have no matching DESCRIBE sub-column' <connector-log-file>
```

**Empty result:** that log shows no unpaired element for any table the connector described, so none of those tables is affected. It says nothing about a period the file does not cover, and `v1.7.0_gg2` never logs the line. **Non-empty result:** each line names a table and the element paths that the connector dropped; inserts into that table fail. Run on a scratch log file that held the line captured from a real run between two unrelated lines, the command printed that line; on a file with only the unrelated lines it printed nothing.

### What to do

This release has no fix for the defect, and the fix is tracked for a later release. Inserts into such a table fail on the route that was measured, the default route with client V2, whatever the connector settings. Client V1 and the route of an explicit `insertFormat=json`, which runs no table description and so logs no WARN, were not measured. This note offers no workaround and no schema change, because the fork has not tested one. The WARN changes nothing about what the connector does; it only tells you which table fails and why.

---

## Item 3: Backslashes and line breaks in names

### What changed

A `Nullable(Tuple(...))` column whose Tuple element name holds a backslash followed by `n`, or a line break, now pairs with its declared type. That holds when the Tuple is the column itself, an `Array` element, a `Map` value or a field of another Tuple (ledger L-50).

On `v1.7.0_gg2` such a name did not pair. The connector logged the following WARN at every table description, kept the nullability that the server reported, and every insert into the table failed with the server error code 33 (`CANNOT_READ_ALL_DATA`) and stored no row:

```text
Could not take element nullability for column <column> of table <db>.<table> from its declared type <type> (structure mismatch); keeping the nullability DESCRIBE reported for it
```

`v1.7.0_gg3` pairs the name, applies the declared nullability, logs no such WARN for the table and stores the row. Measured on ClickHouse 26.9.19.9 with client V2: of 44 fixture tables, 14 `Nullable(Tuple)`-bearing ones with a backslash-n or line-break element name came out different. On `v1.7.0_gg2` the four that were inserted (a `Nullable(Tuple)`, an `Array(Nullable(Tuple))` and a `Map` of `Nullable(Tuple)` with a literal backslash-n name, and a `Nullable(Tuple)` with a real line break) failed with code 33 and stored 0 rows. On `v1.7.0_gg3` all of them stored the row and read back equal. The other 30, among them every table with a plain name, every table whose type holds no `Nullable(Tuple(...))` at any depth, and the `Enum` label shapes, are identical on both releases. A name with a single backslash, a trailing backslash or a tab was not affected: both releases store the record.

### Am I affected?

Replace `<db>` with the database the connector writes to:

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND position(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), 'Nullable(Tuple(') > 0
  AND match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*\\\\n[^`]*`')
ORDER BY `table`;
```

**Empty result:** no column of `<db>` has a `Nullable(Tuple(` type with a backtick-quoted name that holds a backslash followed by `n`, so nothing in that database changes. **Non-empty result:** each listed table has a column of the changed shape. Under `v1.7.0_gg2` the inserts of the four shapes listed above failed, and under `v1.7.0_gg3` they store the row. For the other listed shapes (an element that is itself `Nullable`, a nested Tuple) the two releases build different column descriptions in the same way, but no insert was run, so read their outcome as likely, not measured. Run on the server named above, the query listed exactly the 14 fixture tables of the shape and none of the look-alike tables, and it listed no table that was not of the shape. It reads the declared type text.

### What to do

Nothing is needed for the fix itself. If you have a table of this shape, read [item 4](#item-4-in-flight-batches) before a rollback.

**A known limitation, the same on both releases.** The connector matches a record's field names against the column and element names in its own escaped form of the server's name. A backslash, a slash or a tab in a column or element name therefore means the Kafka field must use the connector's escaped form as its name. For a column name, a field that uses the natural name is ignored and the column's `DEFAULT` is stored, with no error. For an element name inside a Tuple, the natural field name makes the record fail. The escaped form is the following (measured on ClickHouse 26.9.19.9, client V2, identical on `v1.7.0_gg2` and `v1.7.0_gg3`):

| Character in the server name | Server name | Kafka field name that stores the value |
|------------------------------|-------------|----------------------------------------|
| backslash | `a\b` | `a\\b` (two backslashes) |
| backslash and `n` | `a\nb` (backslash, `n`) | `a\\nb` (two backslashes, `n`) |
| line break | `a`, line feed, `b` | `a\nb` (backslash, `n`) |
| slash | `a/b` | `a\/b` |
| tab | `a`, tab, `b` | `a\tb` (backslash, `t`) |

A name that holds a double quote leaves the table unmapped: the connector does not map the table at all. A `Nullable(Tuple)` with a slash in an element name fails with code 33 on both releases, because its declared type does not pair. To list the tables of your database with such names, replace `<db>`:

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        position(name, '\\') > 0
        OR position(name, '\n') > 0
        OR position(name, '/') > 0
        OR position(name, '\t') > 0
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*\\\\[^`]*`')
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*/[^`]*`')
      )
ORDER BY `table`;
```

**Empty result:** no root column name holds a backslash, a line break, a slash or a tab, and no element name in a declared type holds a backslash or a slash, so the limitation does not apply to that database. **Non-empty result:** each listed table needs the escaped field names above. The query does not list a table whose name holds a double quote, because that table is not mapped at all. Run on the server named above, it listed exactly the ten fixture tables and none of the look-alike tables, and it printed nothing for a database that held only look-alikes.

---

## Item 4: In-flight batches

### What changed

A batch that one release leaves in `BEFORE_PROCESSING` is retried by the other release under the same `insert_deduplication_token`, which the connector builds from the topic, the partition and the first and last offset of the batch, so it does not depend on how the batch is encoded. Whether a clean stop matters therefore depends on whether the other release encodes the batch to different bytes. Measured on ClickHouse 26.9.19.9 with client V2, the real table-description code of `v1.7.0_gg2` and of `v1.7.0_gg3` was run over 44 fixture tables on the same server, and 30 of them gave identical column descriptions. Bytes can differ only through the nullability flags of that description. The 14 tables that differ are exactly the shape of [item 3](#item-3-backslashes-and-line-breaks-in-names).

For every other change, one sentence each way:

- **Error text and stack traces ([item 1](#item-1-record-values-in-writer-error-text)).** On the upgrade, no clean stop is needed, because only the text of a failure changes and not a byte of the batch or its token. On a rollback, no clean stop is needed for the same reason.
- **The client V1 table-mapping refresh ([item 1](#item-1-record-values-in-writer-error-text)).** On the upgrade, no clean stop is needed, because the refresh only decides whether the insert is tried once more and the batch it sends is the same. On a rollback, no clean stop is needed for the same reason.
- **The unpaired-element WARN ([item 2](#item-2-the-unpaired-element-warn-and-tuple-elements-named-size)).** On the upgrade, no clean stop is needed, because the WARN is a log line and the nullability it applies is the same as before. On a rollback, no clean stop is needed, because the WARN disappears and a table that fails keeps failing.
- **The MBean version and the client name ([item 5](#item-5-jmx-mbean-name-and-client-name)).** On the upgrade, no clean stop is needed, because the version is not part of the batch or of its token. On a rollback, no clean stop is needed for the same reason.

For the one shape that differs, the two directions are not alike.

- **Upgrade: no clean stop is needed.** In the measurement, the four such shapes that were inserted failed on `v1.7.0_gg2` with code 33 and stored 0 rows, so a range of such a table that `v1.7.0_gg2` left in flight holds no row, and the retry by `v1.7.0_gg3` lands once, with the declared bytes. The other differing shapes were not inserted ([item 3](#item-3-backslashes-and-line-breaks-in-names)), and the same outcome is likely for them, not measured.
- **Rollback: stop cleanly first.** A range that `v1.7.0_gg3` left in `BEFORE_PROCESSING` is retried by `v1.7.0_gg2` under the same `insert_deduplication_token`. The bytes of `v1.7.0_gg2` fail with code 33 even when `v1.7.0_gg3` had stored the rows, because the server reads the bytes before it looks at the token. In the measurement, the rows that `v1.7.0_gg3` would have stored were written by SQL under the token the connector sends, and the retry by `v1.7.0_gg2` failed with code 33 and left exactly the one correct row. A table with a plain element name and the same declared type, whose bytes are the same on both releases, dropped the retry by its token and also kept one row. So the task fails, or the batch goes to the dead letter queue, even though the rows had landed. That is why the check below exists.

### Am I affected?

You are affected if a connector runs with `exactlyOnce=true` and writes a table that the query of [item 3](#item-3-backslashes-and-line-breaks-in-names) lists, and you want to roll back. **Empty result of that query:** no table of that database has the shape, so a rollback needs no clean stop for this reason. A connector with `exactlyOnce=false` keeps no offset state, so the procedure below does not apply to it; a range that Kafka delivers again is handled by the token as described in [item 1 of the gg2 note](ROLLOUT-v1.7.0_gg2.md#item-1-in-flight-batches-and-the-deduplication-token).

### What to do

Before a rollback, for each connector with `exactlyOnce=true` that writes such a table:

1. Stop or pause the connector with your usual procedure, and wait until its tasks have stopped.
2. Query the shared KeeperMap state table for the topics that connector consumes. The table is named by the connector's `zkDatabase` setting. Replace each `<topic>` with one of your topics, writing the same topic twice if you have only one:

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

   **Empty result:** no batch of those topics is in flight, so it is safe to redeploy `v1.7.0_gg2`. **Non-empty result:** each listed `key` is a topic and partition with a batch that may or may not have reached ClickHouse; do not redeploy `v1.7.0_gg2` yet. On a state table created by `v1.7.0_gg3` itself, measured on the server named above, the query returned the one in-flight key of the listed topic, left out a topic that merely shares its prefix, and returned nothing once that range was `AFTER_PROCESSING`. The query only reads, so you can run it as often as you like.
3. If the result is not empty, start the `v1.7.0_gg3` jar again, let the listed ranges reach `AFTER_PROCESSING`, stop it cleanly and run the query again. Repeat until it is empty, then continue with step 2 of [item 7](#item-7-rollback).

[Item 1 of the gg2 note](ROLLOUT-v1.7.0_gg2.md#item-1-in-flight-batches-and-the-deduplication-token) explains why the state table is shared and how the key looks with `enableDbTopicSplit=true`; those explanations are not repeated here. Do not empty or edit the state table to get past a non-empty result: other connectors keep their state in it.

---

## Item 5: JMX MBean name and client name

### What changed

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both now carry `v1.7.0_gg3` where they carried `v1.7.0_gg2`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg3`, plus `,topic=<topic>` for per-topic beans. Measured on ClickHouse 26.9.19.9 with client V2, the per-task bean and the per-topic bean of `v1.7.0_gg3` both carried `version=v1.7.0_gg3`, and those of `v1.7.0_gg2` carried `version=v1.7.0_gg2`.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg3`. The client sends it as the prefix of the HTTP user agent. The rest of the user agent belongs to the client library and did not change between the two releases.

### Am I affected?

Search your dashboards, alert rules and JMX exporter rules for matchers that spell out `version=v1.7.0_gg2`, and for filters on the client name `clickhouse-kafka-connect/v1.7.0_gg2`. **Empty result:** no matcher spells out the old value, and nothing needs to change. The query of [item 7 of the gg1 note](ROLLOUT-v1.7.0_gg1.md#item-7-jmx-mbean-name-and-client-name) lists the client names that your server has seen, and works unchanged. After the upgrade it shows `clickhouse-kafka-connect/v1.7.0_gg3` for the new build.

### What to do

Change matchers that spell out the old value. If you already use the wildcard forms from item 7 of the gg1 note, they keep working across releases and nothing needs to change.

---

## Item 6: Dependency updates

### What changed

Three dependency updates were merged from Dependabot before this release: `lombok` from `1.18.46` to `1.18.48`, `spotless` from `8.10.1` to `8.10.4` and `slf4j-reload4j` from `2.0.17` to `2.0.20`. They change the files `build.gradle.kts` and `gradle/libs.versions.toml` only. These are build measurements, and no ClickHouse server is involved.

- **The shipped archive is unchanged by all three.** The archive that `./gradlew createConfluentArchive` writes was built at tag `v1.7.0_gg2` and at the tree with the three updates, on the same day. The two jars inside are identical entry by entry (7375 entries, none added, none removed, none changed), and so are the two archives as files.
- **`slf4j-reload4j` is not in the archive.** It is on the runtime classpath of a source checkout, which tests and local runs use. The archive carries no `reload4j` file, and its only logging classes are those of `slf4j-api`.
- **Lombok** `1.18.48` generated class files that are byte for byte the same as those of `1.18.46`, and **spotless** runs only when you build.
- **The release date in the archive is the build date.** The `release_date` field of the archive's `manifest.json` is the day of the build, so two builds on different days differ in that field and in no other, and only a per-entry compare of the jars is a fair test across days.

### Am I affected?

Only if you run from a source checkout, because its runtime classpath holds `slf4j-reload4j`. The deployed archive is not affected.

### What to do

Nothing.

---

## Item 7: Rollback

### What changed

Nothing in the data format is migrated, so a rollback is a code rollback. Tag `pre-v1.7.0_gg3` names commit `6473e24f`, the published `v1.7.0_gg2` tree plus the three dependency updates of [item 6](#item-6-dependency-updates). Compared with tag `v1.7.0_gg2`, that tree differs in `build.gradle.kts` and `gradle/libs.versions.toml` only, and an archive built from either tree holds the same jar. The tag is published together with this release. Item 6 of the gg2 note still describes the older rollback, to `v1.7.0_gg1`; the rollback in this item goes back to `v1.7.0_gg2`.

### Am I affected?

You are affected if you need to go back after upgrading, or if you want to know in advance what going back costs.

### What to do

1. Before you change the branch, work through [item 4](#item-4-in-flight-batches) for each connector it applies to, with either `exactlyOnce` setting. A rollback is the one direction in which a batch can be stuck, and the commit of the next step removes this document from the branch. Keep the `v1.7.0_gg3` archive you deployed, or rebuild it from tag `v1.7.0_gg3` (check out the tag and run `./gradlew createConfluentArchive`, as step 3 does for `v1.7.0_gg2`), because item 4 may need you to start it again.
2. Back out the whole release in one commit on `custom_build`, by restoring the tree of tag `pre-v1.7.0_gg3`:
   1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing. Commit whatever it lists, or set it aside with `git stash -u` (a plain `git stash` leaves untracked files behind), or work in a fresh clone. The next command discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
   2. Run `git read-tree -u --reset pre-v1.7.0_gg3`, then `git commit -m "Roll back v1.7.0_gg3"`. This commit is `<rollback-commit>` below; `git rev-parse HEAD` right after the commit prints it. `git diff --quiet HEAD pre-v1.7.0_gg3 && echo ok` prints `ok`.

   Use this form rather than `git revert -m 1` on the release merge. It restores a known tree and works on whatever tip `custom_build` has reached, including after later commits.
3. Redeploy the artifact built at tag `v1.7.0_gg2`. If you kept the archive you deployed for `v1.7.0_gg2`, use it. Otherwise check out tag `v1.7.0_gg2` and run `./gradlew createConfluentArchive`. No release asset is published for any tag, so the archive is always your own build. A build of `pre-v1.7.0_gg3` has the same jar contents.

To go forward again, revert the rollback commit with `git revert <rollback-commit>`. The rollback removes this document from the branch; read it again with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md`.

Rolling back brings back:

- record values in the writer error text of the task status, the DLQ stack-trace header and the `Error inserting records` logs, with the library messages and without the `Failed to write column ` match for conversion failures, and on client V1 the table-mapping refresh that a record text containing `Code: 33` or `Code: 131` starts ([item 1](#item-1-record-values-in-writer-error-text));
- the failure of inserts into a table with a `Nullable(Tuple(...))` whose element name holds a backslash and an `n`, or a line break, with the server error code 33 and the `Could not take element nullability` WARN at every table description ([item 3](#item-3-backslashes-and-line-breaks-in-names));
- the silence about a table with a Tuple element named `size` or `size0`: its inserts fail as before, with code 32 or 33, but the WARN that points at the table is gone ([item 2](#item-2-the-unpaired-element-warn-and-tuple-elements-named-size));
- the `v1.7.0_gg2` MBean `version=` segment and client name ([item 5](#item-5-jmx-mbean-name-and-client-name));
- on the branch, the gg3 documents themselves: this note, the CHANGELOG section and the FORK-DELTA rows;
- a retry failure for a range that is in `BEFORE_PROCESSING` when you roll back, for the one table shape of item 4 ([item 4](#item-4-in-flight-batches)).
