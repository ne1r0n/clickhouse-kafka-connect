# Rollout checklist: `v1.7.0_gg3` to `v1.7.0_gg4`

**Rename before you upgrade.** This release matches a Kafka record field to a column name, a Tuple element name or an `Enum` label as it is written in `CREATE TABLE`. A producer that names a field in the earlier, escaped form (a doubled backslash, a backslash and a letter for a line feed, a tab or a carriage return, a backslash before a slash) must be renamed to the name written in `CREATE TABLE` before you upgrade. See [item 1](#item-1-names-and-enum-labels-as-written-in-create-table).

If you are coming from a release before `v1.7.0_gg3`, work through [`ROLLOUT-v1.7.0_gg3.md`](ROLLOUT-v1.7.0_gg3.md) first, which in turn sends you to the gg2 and gg1 notes, and return here afterwards.

This note covers only what changed between `v1.7.0_gg3` and `v1.7.0_gg4`. The upstream base is `1.7.0` plus upstream's untagged tail `a6f1df83`, which this release absorbs. The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream.

Like its predecessors, this is a checklist and not a deployment record. It names no host, connector, cluster or production system, and every environment-specific value is a placeholder: `<db>`, `<zkDatabase>`, `<topic>`, `<partition>`, `<dbTopicSplitChar>`, `<connector-name>`, `<alert-rule-directory>`, `<connector-config-directory>`, `<connector-log-file>` or `<rollback-commit>`. Replace each one with your own value. In the `text` blocks, other angle-bracket words stand for the part that the connector or the server fills in.

Every item has the same three parts: **What changed** describes the difference in behaviour between the two releases, **Am I affected?** gives a check you can run against your own configuration, your own logs or your own ClickHouse server and says what an empty result means, and **What to do** gives the action when the check shows that you are affected.

Tags such as (ledger L-58) point at entries in the fork's two behaviour ledgers, `27-BEHAVIOUR-LEDGER.md` and `28-BEHAVIOUR-LEDGER.md`, which are kept in this repository's planning directory. This note restates everything an operator needs from them.

Every statement about behaviour says how it is known: **measured** (every measurement here ran on ClickHouse server 26.9.19.9, with client V2 unless the text names client V1), **read from connector code**, or **inferred, to be pinned live**.

The items, in the order to work through them:

1. [Names and Enum labels as written in CREATE TABLE](#item-1-names-and-enum-labels-as-written-in-create-table)
2. [Tuple elements named size](#item-2-tuple-elements-named-size)
3. [Writer error text](#item-3-writer-error-text)
4. [In-flight batches](#item-4-in-flight-batches)
5. [JMX MBean name and client name](#item-5-jmx-mbean-name-and-client-name)
6. [Dependencies, build and the Connect worker](#item-6-dependencies-build-and-the-connect-worker)
7. [Rollback](#item-7-rollback)

---

## Item 1: Names and Enum labels as written in CREATE TABLE

### What changed

The connector matches a Kafka record field to a column, to a Tuple element or to an `Enum` label by name. Up to `v1.7.0_gg3` it compared the field with the name as it came out of the server's table description (the JSON escaping of the name on client V2, the client's own decoding of it on client V1), and for a name that holds a backslash, a line feed, a tab, a carriage return, a slash or a double quote that form was not the name as written in `CREATE TABLE`. `v1.7.0_gg4` compares the field with the name as written in `CREATE TABLE`, on client V2 and on client V1 (ledger L-58, L-59). A field that is still named in the earlier form no longer matches.

Measured on ClickHouse 26.9.19.9, per character, for the name of a root column and for the name of a Tuple element (the client V2 column is from the run of both releases on one server, the client V1 column from the fork's own table of forms, measured on the same server version). `a` and `b` stand for the characters around the one named:

| Character in the name written in `CREATE TABLE` | Earlier release, client V2: field name that matched | Earlier release, client V1 (the default client) | Now, both clients: field name that matches |
|---|---|---|---|
| backslash | `a\\b` (two backslashes) | `a\b` matched | `a\b` (one backslash) |
| backslash followed by `n` | `a\\nb` (two backslashes, `n`) | `a\nb` (backslash, `n`) matched | `a\nb` (backslash, `n`) |
| line feed | `a\nb` (backslash, `n`) | the line feed matched, and the connector logged its metadata-only fallback WARN four times per table | the line feed itself |
| tab | `a\tb` (backslash, `t`) | the table was not mapped | the tab itself |
| carriage return | `a\rb` (backslash, `r`) | the table was not mapped | the carriage return itself |
| slash | `a\/b` (backslash, slash) | the slash came back as the replacement character U+FFFD, so no field matched | `a/b` |
| double quote | the table was not mapped | the double quote came back as the replacement character U+FFFD, so no field matched | `a"b` |

A name that holds a backtick together with a backslash follows the backslash row. A backtick, an apostrophe and non-ASCII letters in a column or element name gave the same name on both releases (measured on both clients). A table that was not mapped (a double quote on client V2, and a tab or a carriage return on client V1) is mapped now, so records for it start to land; on client V1 a slash or a double quote in a name was read as the replacement character, so that table was mapped but no field matched it (ledger L-59, L-60).

**Root silent, element loud.** After the upgrade a field that is still named in the earlier form is handled as follows. A root field is ignored and the column's `DEFAULT` is stored, with no error and no WARN. A field for a Tuple element, in a Tuple, an Array of Tuples, a Nullable Tuple or a Map value Tuple, is not found, and the put fails and stores no row. The root cause is a `DataException`, and its text names a field and never a value (ledger L-62):

```text
column '<path>' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT
```

`<path>` is the raw column path of the container followed by the element name, and the type is the connector's spelling of the element type. Measured on client V2 for eight characters in the four Tuple shapes, and the same test class passed on client V1. The root case was also measured on its own for the backslash name, and the root column stored its `DEFAULT` on both releases when it was sent the other release's name.

**Enum labels.** A label of an `Enum8` or `Enum16` column is keyed under the label as written in `CREATE TABLE`. Before, a label that holds a backslash, a line feed, a tab, a carriage return, a slash, a double quote or an apostrophe was keyed in an escaped form, so a record value that equals the label as written failed as a lookup miss. Measured on client V2 with twelve label forms: the label as written lands and reads back its code for all twelve on this release, and on the earlier release it landed for three (the form with only an underscore, the form with a backtick and the form with non-ASCII letters) and failed for nine. Seven of the nine failed as the withheld lookup miss, and for a double quote and an apostrophe the table was not mapped, so the put failed with `Connection to ClickHouse is not active.`, a text that names a connection and not the label. On this release a value in the earlier key form fails as the lookup miss for the same nine (ledger L-63). A table whose `Enum` type holds an apostrophe label followed by another label was not mapped on client V2 and made client V1 throw a `NumberFormatException`; it is mapped now. A label that holds `)`, `,` or `=` was cut at the first `)` by the old reader, which is read from the unchanged code and not measured on a live table (ledger L-64).

### Am I affected?

Three checks. All three only read. Replace `<db>` with the database the connector writes to. The first query is the one of item 3 of the gg3 note, unchanged. It lists a column whose name holds a backslash, a line feed, a slash or a tab, and a column whose declared type holds a Tuple element name with a backslash or a slash:

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

It misses three tables that this release changes: a root column whose name holds a carriage return and a root column or a Tuple element whose name holds a double quote. The second query is a companion that lists them:

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND (
        position(name, '\r') > 0
        OR position(name, '"') > 0
        OR match(replaceRegexpAll(type, '\'(?:[^\'\\\\]|\\\\.)*\'', ''''''), '`[^`]*(\\\\r|")[^`]*`')
      )
ORDER BY `table`;
```

**Empty result of both:** no column of `<db>` has a name that holds a backslash, a line feed, a tab, a carriage return, a slash or a double quote, and no declared type has a backtick-quoted Tuple element name that holds one, as far as the type text shows, so the rename of this item does not apply to that database. **Non-empty result of either:** each listed table is a suspect, and a producer that writes to it must use the names as written in `CREATE TABLE` from the upgrade on. Measured on the server named above over 36 fixture tables (twelve name forms, each as a root column, a Tuple element and an `Enum` label), the first query listed 14 tables and the second listed 4. Together they list the 16 tables whose names this release changes and one more, a table with a backtick in a Tuple element name, which does not change. Both returned nothing on a database of look-alike tables. Neither lists a Tuple element name that holds only an apostrophe, and this release does not change those names. The queries read the declared type text, so a table they list is a suspect, not a finding.

The third query lists `Enum8` and `Enum16` columns that have a label with a backslash, a double quote or a slash in its text. The server prints a line feed, a tab, a carriage return and an apostrophe in a label with a backslash, so those are listed too:

```sql
SELECT `table`, name AS `column`, type
FROM system.columns
WHERE database = '<db>'
  AND match(type, 'Enum(8|16)\\(')
  AND arrayExists(label -> match(label, '[\\\\"/]'), extractAll(type, '\'((?:[^\'\\\\]|\\\\.)*)\''))
ORDER BY `table`;
```

**Empty result:** no `Enum` column of `<db>` has a label of the kinds above, so no label needs the rename. **Non-empty result:** each listed table has a label that this release keys differently, and a producer that sends such a label must send it as written in `CREATE TABLE`. Measured on the server named above, it listed exactly the nine fixture tables whose labels have an earlier key form and none of the three that have none, with no over-flag, and it returned nothing on the look-alike database. It was run against `Enum8` tables only, not against an `Enum16`, a `Nullable(Enum8(...))` or an `Enum` inside a Tuple, so for those it is a suggestion.

The queries read table definitions and cannot see your producers. To find out whether a producer uses the earlier form, compare the field names it writes for a listed column with the names in the table above: two backslashes where `CREATE TABLE` has one, or a backslash and a letter where `CREATE TABLE` has a line feed, a tab or a carriage return, is the earlier form. If you cannot read a producer's field names, treat a listed table as affected.

### What to do

1. Rename each producer field to the name as written in `CREATE TABLE`, and send each `Enum` label as written there.
2. Do it in this order, because the new release judges each record by the names that record carries and a record that is already in a topic keeps the names it was produced with. Let the old release consume everything that was produced under the earlier names until the connector has no lag on those topics, stop the connector cleanly (item 4), switch the producers to the new names, then upgrade and start the connector. Measured per record on client V2; that this order is needed for the records already in a topic is inferred, to be pinned live.
3. Rows stored earlier are not repaired. A root column that stored its `DEFAULT`, or a Tuple row that failed, stays as it was written.
4. A root field that still uses the earlier name stores its `DEFAULT` and leaves no log line, so the only sign is the data. Check the columns that item 1 lists after the upgrade.
5. Records for tables that were not mapped start to land after the upgrade. If a producer writes to such a table and you did not expect records there to land, stop that producer first.
6. Not measured on a record: a name or label that holds a control character other than a line feed, a tab or a carriage return, a name that holds a dot, and `auto.evolve` with a field that still uses the earlier name.

[Item 4](#item-4-in-flight-batches) covers a batch that is in flight when you upgrade, and [item 7](#item-7-rollback) covers the other direction.

---

## Item 2: Tuple elements named size

### What changed

The gg3 note listed a known defect: the table description dropped a Tuple element named `size` or `size0` at any depth of a column's type, and an element named `size` followed by digits wherever an `Array` encloses its Tuple, so the connector wrote fewer fields than the table declares. `v1.7.0_gg4` keeps those elements, so the connector writes every field that the table declares.

- **Which tables change.** Measured on ClickHouse 26.9.19.9 with client V2, the real table-description code of both releases was run over the same 47 fixture tables on one server. The column descriptions differ for exactly 25 of them, all of them tables with a Tuple element named `size`, `size0` or `size` followed by digits, and they are identical for the other 22: look-alikes such as `size1` outside an `Array`, `sizes` and `Size`, a Tuple with elements named `keys` and `values`, and the two over-flag shapes below. On `v1.7.0_gg3` each of the 25 logged one unpaired-element WARN; on `v1.7.0_gg4` none of the 47 logged it.
- **What an insert does.** Measured with one record per table, for 7 of the 25 tables that differ; the other 18 were detected by the description and the WARN only, and no insert was run for them. On `v1.7.0_gg3` five shapes (`Tuple(size UInt16, v String)`, `Array(Tuple(size UInt16, v String))`, `Array(Tuple(size1 UInt16, v String))`, `Array(Tuple(v String, size1 UInt16))` and `Tuple(size Tuple(a UInt8, b String))`) and `Array(Tuple(size0 String, size String))` failed with the server error code 32 or 33 and stored no row; the code depends on where the parse runs out of bytes and so on the shape and the record, and in this measurement it was 33 for five of the six and 32 for one. One shape, `Tuple(size UInt32, v String)`, stored a row that is not the record: `v1.7.0_gg3` wrote only the string, so the server read the `UInt32` from the first bytes it found and stored a wrong value (a `size` of 1667391751 for a record whose `size` is 7) and a shortened `v`. On `v1.7.0_gg4` all of these store the record and read back equal. Three look-alike shapes stored the record equal on both releases. The fork's own live test, which inserts one record into each of 46 tables of the catalog and reads each row back, passes on `v1.7.0_gg4` (measured on the same server version).
- **The unpaired-element WARN is gone for these shapes.** The line `have no matching DESCRIBE sub-column` of the gg3 note is no longer logged for them. The WARN is still in the code for any other element that has no matching sub-column (read from connector code).
- **One residual, read from connector code.** If the connector cannot read a column's declared type, it keeps the earlier behaviour for that column and drops its sub-columns named `size`, `size0` or `size` followed by digits. It then logs the WARN below once per column and table description.
- **Rows stored earlier are not repaired.** A table that earlier releases failed to write holds no row to repair. A row that an earlier release stored shifted, as in the `Tuple(size UInt32, v String)` measurement, stays as it was written. A shorter write that happens to parse was measured for that one shape only.

The residual WARN reads:

```text
Could not read the declared type <type> of column <column> of table <db>.<table> (<reason>); its sub-columns named size, size0 or size followed by digits are treated as structural, as earlier releases did
```

No fixture table logged this line, so the text is shown to match its search below and has not been seen in a run.

### Am I affected?

Two checks, both read-only. The first reads the declared type text of your columns and lists suspects, with the query of item 2 of the gg3 note, unchanged. Replace `<db>` with the database the connector writes to:

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

The meaning of the result flipped with the upgrade. Before it, each listed table is a table whose inserts fail or store a shifted row. After it, each listed table is a table that this release now writes in full. **Empty result:** no column of `<db>` has a declared type with an element of that shape, as far as the type text shows, so this item does not concern that database. **Non-empty result:** each listed table is a suspect. The query reads text, so it has two known over-flags, and a table it lists is not necessarily one that changes. Measured on the server named above, it listed 27 of the 47 fixture tables: the 25 that change and two that do not, `s4_o01`, whose column has the type `Array(Map(String, Tuple(size1 UInt8)))`, and `x08`, whose column is `JSON(a Tuple(size UInt8, v String))`. A `JSON` column's typed paths are dropped from the table description on purpose, so its description is the same on both releases. The query listed none of the 19 look-alike tables and nothing for a database that held only look-alikes.

The second check reads connector logs. Replace `<connector-log-file>` with a log file that covers a task start. On a `v1.7.0_gg3` log it names the tables that had the defect:

```sh
grep -F 'have no matching DESCRIBE sub-column' <connector-log-file>
```

**Empty result:** that log shows no unpaired element for any table the connector described. It says nothing about a period the file does not cover, and `v1.7.0_gg2` never logs the line. On a `v1.7.0_gg4` log an empty result is expected for the shapes of this item (none of the 47 fixture tables logged the line). **Non-empty result:** each line names a table and the element paths that the connector could not pair, and on a `v1.7.0_gg4` log it is a different cause than the names of this item. The residual of "What changed" has its own line:

```sh
grep -F 'Could not read the declared type' <connector-log-file>
```

**Empty result:** the connector read the declared type of every table it described. **Non-empty result:** each line names a column whose sub-columns named `size`, `size0` or `size` followed by digits the connector drops, as earlier releases did. Run on a scratch log that held the real `v1.7.0_gg3` line and a line built from the message constant between unrelated lines, each command printed exactly its own line, and on a file of unrelated lines both printed nothing.

### What to do

Nothing is needed for the fix itself. For a table the query lists, check the rows that were stored before the upgrade: a table whose shape stored a shifted row keeps it. Read [item 4](#item-4-in-flight-batches) before the upgrade and before a rollback.

---

## Item 3: Writer error text

### What changed

Two failure texts change. Both change text only: a batch that fails this way failed in `v1.7.0_gg3` too, and the retry handling does not change.

**The Map key of the gg3 note is gone on the JSON-as-string route.** This route is on for a connector whose `clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true`. In `v1.7.0_gg3`, a failure of the JSON library on that route was passed on with its message, and when the message named a path through a Map, the Map key, which is record data, reached the Connect task status, the DLQ header `__connect.errors.exception.stacktrace` and the throwable of the `Error inserting records` ERROR logs (item 1 of the gg3 note). In `v1.7.0_gg4` the failure is reported like any other value-conversion failure, with the message withheld (ledger L-52):

```text
Failed to write column `<column>`: Kafka value of type <Kafka type> (value class: <Java class>) could not be written as ClickHouse type JSON: <Jackson exception class>
Caused by: <Jackson exception class> (message withheld: may contain record data)
```

So the text `(through reference chain: ...)` and the Jackson message no longer appear in those three places. This is read from connector code, and the suite pins that the Map key reaches none of the three. The retry classification is unchanged, and a Jackson exception over a timeout still keeps its retry. There is one accepted residual, as `FORK-DELTA.md` states it in Known costs: a Jackson exception whose cause chain also holds a non-Jackson `IOException`, beneath or above it, still passes through with its message. On this route that chain comes only from a record value whose own getter throws it, and Connect's value types have no getter that Jackson calls.

The route of an explicit `insertFormat=json` can raise a Jackson failure too. This release does not change that route, and whether its text carries a Map key into the task status, the DLQ header or the logs is not measured.

**An unknown Enum label is an `IllegalArgumentException`.** For an `Enum8` or `Enum16` column on the RowBinary route, on both clients, the class in the `Failed to write column` text is now `java.lang.IllegalArgumentException`, where it was `java.lang.NullPointerException` (ledger L-54). The column is named in the text, and the `Caused by:` line carries the class and no message. The lookup-miss text of the connector (`Enum label not found for column <column>`) is withheld on this path, as every message is, so an operator reads the exception class and the column and not that sentence. That is a known limit of this release: showing it would need a different kind of exception, a decision that has not been taken. The retry classification is unchanged (not retriable). A record that sends a label in the earlier key form after the upgrade fails this way ([item 1](#item-1-names-and-enum-labels-as-written-in-create-table)).

### Am I affected?

Two checks. Both only read files. The first finds the connectors on the JSON-as-string route. Replace `<connector-config-directory>` with the directory that holds your connector configurations:

```sh
grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>
```

**Empty result:** no configuration file in that directory sets the option, so the route is off for those connectors and nothing in the first change applies to them. **Non-empty result:** each listed line is a connector whose `clickhouseSettings` mentions the option. It is on the route when the value is `1` or `true`. The search covers files only. If you create connectors through the Connect REST interface and keep no file, read each connector's stored configuration the way you usually do and look for the same name. Run on a scratch directory, the command listed both the `=1` and the `=true` form and not an unrelated configuration, and printed nothing for a directory that held only the unrelated one.

The second finds alert rules and log matchers on texts that stopped appearing. Replace `<alert-rule-directory>` with the directory that holds your rules:

```sh
grep -rnF -e 'through reference chain' -e 'No serializer found' -e 'NullPointerException' <alert-rule-directory>
```

**Empty result:** no matcher in that directory uses any of the three texts, and nothing needs to change. **Non-empty result:** each listed line uses a text that a connector on the JSON-as-string route or an unknown Enum label no longer produces in these places. A matcher on `NullPointerException` also catches other failures, so read what the rule is for before you change it. The search compares literal text, so a matcher that writes a character of these texts with a regular-expression escape (for example `through\sreference\schain`) is not listed; read such rules yourself. Run on scratch directories, the command listed each of the three texts once, printed nothing for a directory that held only an unrelated rule, and did not list the rule that wrote the space as `\s`.

### What to do

- Alert on `Failed to write column ` and on the exception class instead of the texts that stopped: the Jackson class names for the JSON-as-string route and `java.lang.IllegalArgumentException` for an unknown Enum label. `Failed to write column ` finds every value-conversion failure of the writer.
- For a connector on the JSON-as-string route, the Map key no longer reaches the three places, except through the accepted residual above. Keep handling those places as you handle record data if your records can hold a value whose own getter throws an `IOException`.
- If you use the `insertFormat=json` route, treat the same three places as able to carry a Jackson message, since that was not measured.

---

## Item 4: In-flight batches

### What changed

A batch that one release leaves in `BEFORE_PROCESSING` is retried by the other release under the same `insert_deduplication_token`, which the connector builds from the topic, the partition and the first and last offset of the batch, with `exactlyOnce=false` as well as `exactlyOnce=true` (read from connector code, and used in every measurement below). A range that is already in `AFTER_PROCESSING` is dropped and not inserted again. Whether a clean stop matters depends on whether the other release reads the record's fields in the same way and builds the same bytes. Each measurement below ran on ClickHouse 26.9.19.9 with client V2 and `errors.tolerance` at its default, and with the row of the other release landed by SQL under the token the connector sends, because one server cannot be shared between two releases. Each change below is answered for both directions.

- **Names and Enum labels ([item 1](#item-1-names-and-enum-labels-as-written-in-create-table)), both directions.** A batch that is retried across the release change meets the other release's name rule. Measured for the backslash name on client V2 only, in both directions with the same three outcomes. On a table with a deduplication window and with the other release's row landed, the retry of a root field is dropped by the token and the landed row stays, and the retry of a Tuple element fails before any insert with a `DataException` (`column '<path>' (INT32): no value provided for a non-nullable column without a default; ...`) and the landed row stays. On a table without a window and with nothing landed, the retry of a root field stores one row that holds the column's `DEFAULT`. So the rename and the backlog precondition of item 1 apply in both directions, and a clean stop is what leaves no range in flight. The other eleven name forms and client V1 were not run in this way.
- **Size-named Tuple elements ([item 2](#item-2-tuple-elements-named-size)), upgrade: stop cleanly first.** The six inserted shapes that failed on `v1.7.0_gg3` stored no row, so for a table of exactly one of those shapes a range that `v1.7.0_gg3` left in flight holds nothing, and the retry by `v1.7.0_gg4` lands once (measured). The shape that stored a shifted row is different. Measured: with the row of `v1.7.0_gg3` landed under the token, the retry by `v1.7.0_gg4` is dropped on a table with a deduplication window, so the shifted row stays and the correct row is not stored, and on a table without a window the correct row is stored as a second row. Whether a window applies is the table's own setting. Only 7 of the 25 differing tables were inserted, so for each of the other 18 shapes it is not known whether `v1.7.0_gg3` failed or stored a shifted row. Stop cleanly first for every table that the query of item 2 lists. For the shifted shape and for every one of the 18 shapes that were not inserted, a range in flight can meet a shifted row (inferred, to be pinned live).
- **Size-named Tuple elements, rollback: stop cleanly first.** A range that `v1.7.0_gg4` left in `BEFORE_PROCESSING` is retried by `v1.7.0_gg3` under the same token. The bytes of `v1.7.0_gg3` are short, and what the server does with them depends on whether they can be parsed. For a shape whose short bytes cannot be parsed, the server rejects them before it looks at the token. Measured for one such shape, `Tuple(size UInt16, v String)`, with the rows of `v1.7.0_gg4` landed by SQL: the retry failed with the server error code 33 and left exactly the one landed row, so the task fails, or the batch goes to the dead letter queue, although the rows had landed. For a shape whose short bytes do parse, such as `Tuple(size UInt32, v String)` ([item 2](#item-2-tuple-elements-named-size)), the retry is dropped by the token on a table with a deduplication window and stored as an extra shifted row on a table without one; this direction was not run (inferred from the upgrade measurement, to be pinned live). A table whose declared type differs only by a name that is not size-named has the same bytes on both releases, and its retry was dropped by the token with one row left. The code is 32 or 33 depending on the shape and the record, so do not match one code.
- **Writer error text and Enum error text ([item 3](#item-3-writer-error-text)).** On the upgrade, no clean stop is needed, because only the text of a failure changes and not a byte of the batch or its token (read from connector code). On a rollback, no clean stop is needed for the same reason.
- **The MBean version and the client name ([item 5](#item-5-jmx-mbean-name-and-client-name)).** On the upgrade, no clean stop is needed, because the version is not part of the batch or of its token (read from connector code). On a rollback, no clean stop is needed for the same reason.
- **Bundled dependency patch bumps ([item 6](#item-6-dependencies-build-and-the-connect-worker)).** On the upgrade, no clean stop is needed, and on a rollback, no clean stop is needed, because no change in the bytes sent to ClickHouse is expected. That is inferred, to be pinned live: it is not measured, and the suite and the JSON-route live tests are green on the release tip.

### Am I affected?

You are affected if a connector writes a table that a query of [item 1](#item-1-names-and-enum-labels-as-written-in-create-table) or of [item 2](#item-2-tuple-elements-named-size) lists, or if its producers still use the earlier field names of item 1, and you upgrade or want to roll back. **Empty result of those queries:** no table of that database has those shapes, so neither change needs a clean stop for that database. A connector with `exactlyOnce=false` has no state table to query, so the check below does not exist for it, but the failure does: a range that Kafka delivers again to `v1.7.0_gg3` for a table of a size-named shape is expected to fail with code 32 or 33 even when `v1.7.0_gg4` stored its rows, if the short bytes cannot be parsed, and to be dropped or stored again as an extra shifted row if they do parse (inferred, to be pinned live). The measurements above ran with the token that `exactlyOnce=false` also sends. Stop its tasks cleanly, so that the offsets are committed, before you redeploy `v1.7.0_gg3`.

### What to do

Before an upgrade, rename the producer fields as [item 1](#item-1-names-and-enum-labels-as-written-in-create-table) says, and for a table of the shifted shape of [item 2](#item-2-tuple-elements-named-size) stop the connector cleanly first.

To confirm that a connector with `exactlyOnce=true` stopped cleanly before an upgrade, run the query of step 2 below while the connector is stopped. The query is release independent: the state records, the key rule and the token are the same code in `v1.7.0_gg3` and `v1.7.0_gg4` (read from connector code). **Empty result:** no range is in flight, so it is safe to start `v1.7.0_gg4`. **Non-empty result:** start the `v1.7.0_gg3` jar again, let the listed ranges reach `AFTER_PROCESSING`, stop it cleanly and run the query again; repeat until it is empty. A task that was killed, or a worker that restarted, leaves its range in `BEFORE_PROCESSING`, which is what the query finds. A connector with `exactlyOnce=false` has no state table. Recognise its clean stop by two facts: its consumer group has no lag on the topics it consumes (lag is counted from the committed offsets), and the connector was paused or deleted through the Connect REST interface, not killed with its worker. That is how Kafka Connect commits offsets, and it is inferred here, to be pinned live.

Before a rollback, do not rename the producer fields back yet. The producers stay on the names of this release until `v1.7.0_gg4` has consumed everything that was produced under them: pause the producers of the affected topics, and wait until the connector has no lag on those topics. A producer that is renamed back while `v1.7.0_gg4` is still consuming makes it read each new record under the wrong names, which stores the column's `DEFAULT` for a root field with no error and no log line, and fails the put for a `Tuple` element ([item 1](#item-1-names-and-enum-labels-as-written-in-create-table)). A record that was produced under the names of this release and is still in a topic when `v1.7.0_gg3` starts is read by `v1.7.0_gg3` under its own rule in the same way (inferred, to be pinned live). Only after the connector has stopped cleanly and the check below is empty, switch the producers back to the earlier names, then redeploy `v1.7.0_gg3` and resume the producers. For each connector with `exactlyOnce=true` that writes a table of either shape, run these steps once the connector has no lag:

1. Stop or pause the connector with your usual procedure, and wait until its tasks have stopped.
2. Query the shared KeeperMap state table for the topics that connector consumes. The table is named by the connector's `zkDatabase` setting. Replace the `'<topic>'` entries with one entry for each topic the connector consumes, adding an entry per further topic and writing a single topic in both entries. With `enableDbTopicSplit=true`, also add the entry `'<db><dbTopicSplitChar><topic>'` for each topic, where `<db>` is the connector's `database` setting: the connector keys a topic that its `dbTopicSplitChar` divides into exactly two parts under the topic name itself, and any other topic under that prefixed form. A topic name that ends with `dbTopicSplitChar` can also be keyed without those trailing characters, so list that form as well. An entry that matches no key changes nothing. This key form is read from the connector code, not measured. Run the query in the database the connector connects to (its `database` setting, where the connector creates the state table), or write the table as `` `<db>`.`<zkDatabase>` ``:

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

   **Empty result:** no batch of the listed topics is in flight, so it is safe to redeploy `v1.7.0_gg3`. **Non-empty result:** each listed `key` is a topic and partition with a batch that may or may not have reached ClickHouse; do not redeploy `v1.7.0_gg3` yet. On a state table created by the connector's own `KeeperStateProvider` of this release, measured on the server named above, the query returned the two in-flight keys of the two listed topic forms (one plain, one with the `<db><dbTopicSplitChar>` prefix), left out a key that was already `AFTER_PROCESSING` and a topic that merely shares a prefix, and returned nothing once both ranges were `AFTER_PROCESSING`. The test wrote the prefixed key itself, so which key the connector writes for a topic is read from the connector code. The query only reads, so you can run it as often as you like.
3. If the result is not empty, start the `v1.7.0_gg4` jar again, let the listed ranges reach `AFTER_PROCESSING`, stop it cleanly and run the query again. Repeat until it is empty, then switch the producers back to the earlier names if you renamed them for item 1, and continue with step 2 of [item 7](#item-7-rollback).

For a connector with `exactlyOnce=false` that writes such a table, there is no state table to query. Once it has no lag, stop its tasks cleanly before you switch the producers back and redeploy `v1.7.0_gg3`, so that its offsets are committed and Kafka does not deliver a range again.

[Item 1 of the gg2 note](ROLLOUT-v1.7.0_gg2.md#item-1-in-flight-batches-and-the-deduplication-token) and [item 1 of the gg1 note](ROLLOUT-v1.7.0_gg1.md#item-1-exactly-once-pre-upgrade-check), to which it points, explain why the state table is shared; that explanation is not repeated here. Both notes carry the key rule of step 2 above. Do not empty or edit the state table to get past a non-empty result: other connectors keep their state in it.

---

## Item 5: JMX MBean name and client name

### What changed

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both now carry `v1.7.0_gg4` where they carried `v1.7.0_gg3`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg4`, plus `,topic=<topic>` for per-topic beans. Measured on ClickHouse 26.9.19.9 with client V2, the per-task bean and the per-topic bean of `v1.7.0_gg4` both carried `version=v1.7.0_gg4`, and those of `v1.7.0_gg3` carried `version=v1.7.0_gg3`.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg4`. The client sends it as the prefix of the HTTP user agent. Measured, the rest of the user agent is the client library token (`clickhouse-java-v2/0.9.6`, the same in both runs), the operating system and Java version of the host, and the HTTP client token, which changed from `Apache-HttpClient/5.6.2` to `Apache-HttpClient/5.6.5` because the bundled httpclient5 changed ([item 6](#item-6-dependencies-build-and-the-connect-worker)). A matcher on the whole user agent therefore changes too, not only one on the client name.

### Am I affected?

Search your dashboards, alert rules and JMX exporter rules for matchers that spell out `version=v1.7.0_gg3`, and for filters on the client name `clickhouse-kafka-connect/v1.7.0_gg3` or on a complete user agent that contains `Apache-HttpClient/5.6.2`. **Empty result:** no matcher spells out an old value, and nothing needs to change. The query of [item 7 of the gg1 note](ROLLOUT-v1.7.0_gg1.md#item-7-jmx-mbean-name-and-client-name) lists the client names that your server has seen, and works unchanged. After the upgrade it shows `clickhouse-kafka-connect/v1.7.0_gg4` for the new build. The `jmx-export-connector.yml` in this repository captures the version with the pattern `version=(.*)`, so it keeps working across releases (read from the file).

### What to do

Change matchers that spell out an old value. If you already use the wildcard forms from item 7 of the gg1 note, they keep working across releases and nothing needs to change.

---

## Item 6: Dependencies, build and the Connect worker

### What changed

Dependency updates were merged from Dependabot and from upstream's tail before this release. The statements below are build and archive measurements, or readings of public records with their source and date. No ClickHouse server is involved.

- **The Connect API is compile-only.** The connector is compiled against `connect-api` `4.4.0`. The Confluent archive does not bundle it: measured on the archive built at the tip, it holds no `org/apache/kafka` entry. The Connect worker that runs the connector therefore supplies the Kafka classes, and the worker's version governs at runtime.
- **The production worker** is `confluentinc/cp-kafka-connect-base:7.9.7`, which is Confluent Platform 7.9 and Apache Kafka 3.9. This is cited: the fork's maintainers state it, and the three API jars of that image (`connect-api`, `connect-runtime` and `kafka-clients`) were read when the dependencies were absorbed.
- **The oldest worker the connector is verified against is Apache Kafka 3.9, by a static check, not by a run on a live worker.** Every one of the 176 Kafka API references of the compiled connector (39 classes and 137 members) was found in that worker's API jars, and also in `connect-api` `4.1.1` and `4.4.0`. A unit test fails the build when the compiled connector references a Kafka class or member that is not on the committed reference list; the list was checked once against the worker jars named above, and the test does not read any worker jar. A worker older than Kafka 3.9 was not checked.
- **KIP-1161** makes the validation of list-type configurations stricter, and it applies from Kafka 4.2 workers on. It is listed under "Upgrading to 4.2.0" in the Kafka upgrade notes (read from the Apache Kafka repository at tag `4.4.0` on 2026-10-23; the page for 4.4 on the Kafka website returned HTTP 404 that day). The connector declares seven list-type configuration keys and none of them carries a validator (read from connector code, counted by search), so there is nothing in them for that validation to tighten. The 4.4.0 upgrade notes name two items for Kafka Connect, an option for the automatic creation of the internal topics (KIP-1209) and a bootstrap resolve timeout (KIP-909), and both concern the worker. The production worker is Kafka 3.9.
- **Build tools.** The archive is built with Shadow `9.6.1` and Gradle `9.8.1`. They are build tools and need no action from operators. Measured by a per-entry compare of the archive built at tag `v1.7.0_gg3` with the archive built at the tip, using an allow-list that names every permitted difference, the result is PASS: the archive built at the tip has no nested jar, its manifest differs only in the implementation version, and every other difference is one of the version changes in the table below or one of 21 classes of the connector itself (the classes that this release's code changed or added, and the version class). The archive of the tree that a rollback leaves on the branch does not have that property ([item 7](#item-7-rollback)).
- **Bundled versions that changed** (from the same compare; for `jspecify`, which has no `pom.properties` and so no `VERSION` line in the compare, from the allow-list row and the dependency resolution, where it is guava `33.7.2-jre`'s dependency selected by conflict resolution). Others did not change, among them `lz4-java` `1.12.0`, `slf4j-api` `2.0.7`, `jackson-annotations` `2.22` and `commons-lang3` `3.20.0`:

| Library | `v1.7.0_gg3` | `v1.7.0_gg4` |
|---|---|---|
| guava | `33.4.6-jre` | `33.7.2-jre` |
| httpclient5 | `5.6.2` | `5.6.5` |
| httpcore5 and httpcore5-h2 | `5.4.3` | `5.4.4` |
| jackson-core and jackson-databind | `2.22.3` | `2.22.4` |
| gson | `2.13.1` | `2.14.0` |
| j2objc-annotations | `3.0.0` | `3.1` |
| error_prone_annotations | `2.38.0` | `2.50.0` |
| jspecify | `1.0.0` | `1.0.1` |

**Two advisories, each fact labelled by its source and read on 2026-10-23.** This note states what the sources say and makes no assessment of what either advisory means for your deployment.

- **Guava, `CVE-2026-102554`, also published as `GHSA-xxph-c9ww-hj94`.** NVD (status "Awaiting Analysis", no product configuration listed) describes Guava versions 4.0 through 33.7.1 and a denial of service by `OutOfMemoryError` when instances of `CompactHashMap`, `CompactHashSet` or `MapMakerInternalMap` are deserialized. The OSV record lists the GHSA as an alias and carries a Git range with a fix commit and unresolved ranges ("introduced 4.0, fixed 33.7.2" and "introduced 4.0, last affected 33.7.1") but no Maven range, and its version query for the Maven package `com.google.guava:guava` returns nothing for `33.4.6-jre` and for `33.7.2-jre`. The GHSA is a repository advisory of `google/guava` and is absent from GitHub's global advisory database (its REST endpoint answered 404). The sources disagree about `33.4.6-jre`, which `v1.7.0_gg3` ships: the range in NVD's text includes it, and OSV's version query does not list it. `33.7.2-jre` is what `v1.7.0_gg4` ships.
- **httpclient5, `CVE-2026-64607`, also published as `GHSA-hjcp-jmpx-g3qm`.** GitHub's global advisory (severity medium, titled "Apache HttpComponents Client: Connection Leak on Content-Encoding Decode Error Leads to Pool Exhaustion DoS") gives the range from `5.0-alpha1` up to but not including `5.6.3`, with `5.6.3` as the first patched version. NVD's text says "from 5.0-alpha1 through 5.6.2". OSV gives the same range, and its version query for the Maven package lists the advisory for `5.6.2` and not for `5.6.5`. `v1.7.0_gg3` ships `5.6.2` and `v1.7.0_gg4` ships `5.6.5`.

### Am I affected?

Two checks, neither of which runs anything against ClickHouse. First, ask your deployment which Kafka version your Connect worker runs. A worker older than Kafka 3.9 is outside what was verified, and nothing in this note says whether the connector runs on it. The static check above covered the 3.9 jars, `4.1.1` and `4.4.0`; a worker of another version was not checked. A worker of 4.2 or newer applies the KIP-1161 validation to its own and other plugins' configurations, not to the keys of this connector. Second, if you scan the archive you deploy, compare the report with the two advisories above. **Empty result:** if your scanner reports neither advisory for the bundled `guava` and `httpclient5` of `v1.7.0_gg4`, nothing needs to change. If it reports one, the sources above say what each of them publishes for the versions in the table, and this note does not decide between them.

### What to do

Nothing changes in your build or deployment: deploy the archive as before. A worker older than Kafka 3.9 is outside what was verified. For a scanner report, use the sources and read dates above.

---

## Item 7: Rollback

### What changed

Nothing in the data format is migrated, so a rollback is a code rollback. Tag `pre-v1.7.0_gg4` names commit `69cd680b`, the published tip of `custom_build` before this release: the published `v1.7.0_gg3` tree with its corrected note, plus the nine dependency merges that landed on `custom_build` after tag `v1.7.0_gg3` (Connect API, Gradle wrapper, Shadow, httpclient5, gson, Jackson, slf4j, commons-lang3 and the versions plugin). Compared with tag `v1.7.0_gg3`, that tree differs in 22 files (measured with `git diff --stat`): the build files, the Gradle wrapper, the dependency catalog, the gg3 note, the CHANGELOG, FORK-DELTA and planning files. The tag is published together with this release. Item 7 of the gg3 note describes the rollback to `v1.7.0_gg2`; the rollback in this item goes back to `v1.7.0_gg3`.

**The rolled-back branch tip does not build the archive you want.** Measured: `./gradlew createConfluentArchive` on the tree of `69cd680b` ends green and writes a zip named `clickhouse-kafka-connect-v1.7.0_gg3.zip`, like the right one, but its inner jar holds 25 dependency jars as nested entries instead of their classes (135 file entries, against 7376 for the archive built at tag `v1.7.0_gg3`). The release workflow at that commit builds with `./gradlew createConfluentArchive` and would build that archive, and the push workflow builds no archive and would not notice (read from the workflow files at that commit).

### Am I affected?

You are affected if you need to go back after upgrading, or if you want to know in advance what going back costs. This item has no query of its own; the checks are those of [item 4](#item-4-in-flight-batches).

### What to do

1. Before you change the branch, work through [item 4](#item-4-in-flight-batches) for each connector it applies to, with either `exactlyOnce` setting, in the order it gives for a rollback: the producers stay on the new names until the connector has consumed what they produced and has stopped cleanly, and they go back to the earlier names only after that, before `v1.7.0_gg3` starts. A rollback is the one direction in which a batch can be stuck, and the commit of the next step removes this document from the branch. Keep the `v1.7.0_gg4` archive you deployed, or rebuild it from tag `v1.7.0_gg4` (check out the tag and run `./gradlew createConfluentArchive`), because item 4 may need you to start it again.
2. Back out the whole release in one commit on `custom_build`, by restoring the tree of tag `pre-v1.7.0_gg4`:
   1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing, with one expected exception: a fresh checkout of the published tip lists ` M gradlew.bat` without any change of yours, because the committed `gradlew.bat` has Windows line endings and `.gitattributes` asks git to normalise `*.bat` files (measured in the dry run of this rollback, in a fresh worktree; the same line appears again after the rollback commit, and it does not stop step 2). Commit whatever else it lists, or set it aside with `git stash -u` (a plain `git stash` leaves untracked files behind), or work in a fresh clone. The next command discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
   2. Run `git read-tree -u --reset pre-v1.7.0_gg4`, then `git commit -m "Roll back v1.7.0_gg4"`. This commit is `<rollback-commit>` below; `git rev-parse HEAD` right after the commit prints it. `git diff --quiet HEAD pre-v1.7.0_gg4 && echo ok` prints `ok`.

   Use this form rather than `git revert -m 1` on the release merge. It restores a known tree and works on whatever tip `custom_build` has reached, including after later commits.
3. Switch the producers back to the earlier names where item 1 made you rename them, and redeploy the artifact built at tag `v1.7.0_gg3`. Resume the producers after the connector has started. If you kept the archive you deployed for `v1.7.0_gg3`, use it. Otherwise check out tag `v1.7.0_gg3` and run `./gradlew createConfluentArchive`. No release asset is published for any tag, so the archive is always your own build. Never deploy an archive built from the rolled-back branch tip: that build passes and its jar nests 25 dependency jars, as the paragraph above shows.

To go forward again, revert the rollback commit with `git revert <rollback-commit>`. The rollback removes this document from the branch; read it again with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg4.md`.

Rolling back brings back:

- field names matched in the earlier escaped form, and the tables that were not mapped, so producers must be renamed back after `v1.7.0_gg4` has consumed what they produced under the new names and before the old release reads their records, in the order of [item 4](#item-4-in-flight-batches) ([item 1](#item-1-names-and-enum-labels-as-written-in-create-table));
- for a Tuple element named `size`, `size0` or `size` followed by digits, the failed inserts of the affected shapes, the stored shifted row of one shape, and the unpaired-element WARN ([item 2](#item-2-tuple-elements-named-size));
- the Map key in the writer error text on the JSON-as-string route, and the `NullPointerException` text for an unknown `Enum` label ([item 3](#item-3-writer-error-text));
- a retry failure for a range that is in `BEFORE_PROCESSING` when you roll back ([item 4](#item-4-in-flight-batches));
- the `v1.7.0_gg3` MBean `version=` segment and client name ([item 5](#item-5-jmx-mbean-name-and-client-name));
- the `v1.7.0_gg3` bundled dependency versions, because you redeploy its archive ([item 6](#item-6-dependencies-build-and-the-connect-worker));
- the earlier, shorter key rule for `enableDbTopicSplit=true` in [`ROLLOUT-v1.7.0_gg1.md`](ROLLOUT-v1.7.0_gg1.md) and [`ROLLOUT-v1.7.0_gg2.md`](ROLLOUT-v1.7.0_gg2.md), which can miss a key; take the rule from step 2 of item 4 of the gg3 note;
- on the branch, the gg4 documents themselves: this note, the CHANGELOG section and the FORK-DELTA rows.
