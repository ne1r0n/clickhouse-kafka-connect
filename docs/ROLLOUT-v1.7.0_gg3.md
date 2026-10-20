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

This release has no fix for the defect, and the fix is tracked for a later release. Inserts into such a table fail whatever the connector settings: that was measured with client V2 on the default route, and the route of an explicit `insertFormat=json`, which runs no table description and so logs no WARN, was not measured. This note offers no workaround and no schema change, because the fork has not tested one. The WARN changes nothing about what the connector does; it only tells you which table fails and why.

---

## Item 3: Backslashes and line breaks in names

### What changed

A `Nullable(Tuple(...))` column whose Tuple element name holds a backslash followed by `n`, or a line break, now pairs with its declared type. That holds when the Tuple is the column itself, an `Array` element, a `Map` value or a field of another Tuple (ledger L-50).

On `v1.7.0_gg2` such a name did not pair. The connector logged the following WARN at every table description, kept the nullability that the server reported, and every insert into the table failed with the server error code 33 (`CANNOT_READ_ALL_DATA`) and stored no row:

```text
Could not take element nullability for column <column> of table <db>.<table> from its declared type <type> (structure mismatch); keeping the nullability DESCRIBE reported for it
```

`v1.7.0_gg3` pairs the name, applies the declared nullability, logs no such WARN for the table and stores the row. Measured on ClickHouse 26.9.19.9 with client V2: of 44 fixture tables, 14 `Nullable(Tuple)`-bearing ones with a backslash-n or line-break element name came out different. On `v1.7.0_gg2` the four that were inserted (a `Nullable(Tuple)`, an `Array(Nullable(Tuple))` and a `Map` of `Nullable(Tuple)` with a literal backslash-n name, and a `Nullable(Tuple)` with a real line break) failed with code 33 and stored 0 rows. On `v1.7.0_gg3` all of them stored the row and read back equal. The other 30, among them every table with a plain name, every root that is not `Nullable`, and the `Enum` label shapes, are identical on both releases. A name with a single backslash, a trailing backslash or a tab was not affected: both releases store the record.

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

This item is completed in a later step of the release work.

### Am I affected?

### What to do

---

## Item 5: JMX MBean name and client name

### What changed

This item is completed in a later step of the release work.

### Am I affected?

### What to do

---

## Item 6: Dependency updates

### What changed

This item is completed in a later step of the release work.

### Am I affected?

### What to do

---

## Item 7: Rollback

### What changed

Nothing in the data format is migrated, so a rollback is a code rollback. Tag `pre-v1.7.0_gg3` names commit `6473e24f`, the published `v1.7.0_gg2` tree plus the three dependency updates of [item 6](#item-6-dependency-updates). Compared with tag `v1.7.0_gg2`, that tree differs in `build.gradle.kts` and `gradle/libs.versions.toml` only, and an archive built from either tree holds the same jar. The tag is published together with this release. Item 6 of the gg2 note still describes the older rollback, to `v1.7.0_gg1`; the rollback in this item goes back to `v1.7.0_gg2`.

### Am I affected?

You are affected if you need to go back after upgrading, or if you want to know in advance what going back costs.

### What to do

1. Back out the whole release in one commit on `custom_build`, by restoring the tree of tag `pre-v1.7.0_gg3`:
   1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing. Commit whatever it lists, or set it aside with `git stash -u` (a plain `git stash` leaves untracked files behind), or work in a fresh clone. The next command discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
   2. Run `git read-tree -u --reset pre-v1.7.0_gg3`, then `git commit -m "Roll back v1.7.0_gg3"`. This commit is `<rollback-commit>` below; `git rev-parse HEAD` right after the commit prints it. `git diff --quiet HEAD pre-v1.7.0_gg3 && echo ok` prints `ok`.

   Use this form rather than `git revert -m 1` on the release merge. It restores a known tree and works on whatever tip `custom_build` has reached, including after later commits.
2. Before you redeploy, run the check of [item 4](#item-4-in-flight-batches): a rollback is the one direction in which a batch can be stuck.
3. Redeploy the artifact built at tag `v1.7.0_gg2`. If you kept the archive you deployed for `v1.7.0_gg2`, use it. Otherwise check out tag `v1.7.0_gg2` and run `./gradlew createConfluentArchive`. No release asset is published for any tag, so the archive is always your own build. A build of `pre-v1.7.0_gg3` has the same jar contents.

To go forward again, revert the rollback commit with `git revert <rollback-commit>`. The rollback removes this document from the branch; read it again with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md`.

Rolling back brings back:

- record values in the writer error text of the task status, the DLQ stack-trace header and the `Error inserting records` logs, with the library messages and without the `Failed to write column ` match for conversion failures, and on client V1 the table-mapping refresh that a record text containing `Code: 33` or `Code: 131` starts ([item 1](#item-1-record-values-in-writer-error-text));
- the failure of inserts into a table with a `Nullable(Tuple(...))` whose element name holds a backslash and an `n`, or a line break, with the server error code 33 and the `Could not take element nullability` WARN at every table description ([item 3](#item-3-backslashes-and-line-breaks-in-names));
- the silence about a table with a Tuple element named `size` or `size0`: its inserts fail as before, but the WARN that points at the table is gone ([item 2](#item-2-the-unpaired-element-warn-and-tuple-elements-named-size));
- the `v1.7.0_gg2` MBean `version=` segment and client name ([item 5](#item-5-jmx-mbean-name-and-client-name));
- on the branch, the gg3 documents themselves: this note, the CHANGELOG section and the FORK-DELTA rows;
- a retry failure for a range that is in `BEFORE_PROCESSING` when you roll back, for the one table shape of item 4 ([item 4](#item-4-in-flight-batches)).
