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

This item is completed in a later step of the release work.

### Am I affected?

### What to do

---

## Item 3: Backslashes and line breaks in names

### What changed

This item is completed in a later step of the release work.

### Am I affected?

### What to do

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
