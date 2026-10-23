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

A name that holds a backtick together with a backslash follows the backslash row. A backtick, an apostrophe and non-ASCII letters in a column or element name gave the same name on both releases (measured on both clients). A table that was not mapped (a double quote on client V2, and a tab, carriage return, slash or double quote on client V1) is mapped now, so records for it start to land (ledger L-59, L-60).

**Root silent, element loud.** After the upgrade a field that is still named in the earlier form is handled as follows. A root field is ignored and the column's `DEFAULT` is stored, with no error and no WARN. A field for a Tuple element, in a Tuple, an Array of Tuples, a Nullable Tuple or a Map value Tuple, is not found, and the put fails and stores no row. The root cause is a `DataException`, and its text names a field and never a value (ledger L-62):

```text
column '<path>' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT
```

`<path>` is the raw column path of the container followed by the element name, and the type is the connector's spelling of the element type. Measured on client V2 for eight characters in the four Tuple shapes, and the same test class passed on client V1. The root case was also measured on its own for the backslash name, and the root column stored its `DEFAULT` on both releases when it was sent the other release's name.

**Enum labels.** A label of an `Enum8` or `Enum16` column is keyed under the label as written in `CREATE TABLE`. Before, a label that holds a backslash, a line feed, a tab, a carriage return, a slash, a double quote or an apostrophe was keyed in an escaped form, so a record value that equals the label as written failed as a lookup miss. Measured on client V2 with twelve label forms: the label as written lands and reads back its code for all twelve on this release, and on the earlier release it landed for three (the plain control entry, a backtick and non-ASCII letters) and failed for nine. Seven of the nine failed as the withheld lookup miss, and for a double quote and an apostrophe the table was not mapped, so the put failed with `Connection to ClickHouse is not active.`, a text that names a connection and not the label. On this release a value in the earlier key form fails as the lookup miss for the same nine (ledger L-63). A table whose `Enum` type holds an apostrophe label followed by another label was not mapped on client V2 and made client V1 throw a `NumberFormatException`; it is mapped now. A label that holds `)`, `,` or `=` was cut at the first `)` by the old reader, which is read from the unchanged code and not measured on a live table (ledger L-64).

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

This item is written in the next commit of this release.

### Am I affected?

Empty result: see the next commit.

### What to do

See the next commit.

---

## Item 3: Writer error text

### What changed

This item is written in the next commit of this release.

### Am I affected?

Empty result: see the next commit.

### What to do

See the next commit.

---

## Item 4: In-flight batches

### What changed

This item is written in the next commit of this release.

### Am I affected?

Empty result: see the next commit.

### What to do

See the next commit.

---

## Item 5: JMX MBean name and client name

### What changed

This item is written in the next commit of this release.

### Am I affected?

Empty result: see the next commit.

### What to do

See the next commit.

---

## Item 6: Dependencies, build and the Connect worker

### What changed

This item is written in the next commit of this release.

### Am I affected?

Empty result: see the next commit.

### What to do

See the next commit.

---

## Item 7: Rollback

### What changed

Nothing in the data format is migrated, so a rollback is a code rollback. Tag `pre-v1.7.0_gg4` names commit `69cd680b`, the published tip of `custom_build` before this release: the published `v1.7.0_gg3` tree with its corrected note, plus the nine dependency merges that landed on `custom_build` after tag `v1.7.0_gg3` (Connect API, Gradle wrapper, Shadow, httpclient5, gson, Jackson, slf4j, commons-lang3 and the versions plugin). Compared with tag `v1.7.0_gg3`, that tree differs in 22 files (measured with `git diff --stat`): the build files, the Gradle wrapper, the dependency catalog, the gg3 note, the CHANGELOG, FORK-DELTA and planning files. The tag is published together with this release. Item 7 of the gg3 note describes the rollback to `v1.7.0_gg2`; the rollback in this item goes back to `v1.7.0_gg3`.

**The rolled-back branch tip does not build the archive you want.** Measured: `./gradlew createConfluentArchive` on the tree of `69cd680b` ends green and writes a zip named `clickhouse-kafka-connect-v1.7.0_gg3.zip`, like the right one, but its inner jar holds 25 dependency jars as nested entries instead of their classes (135 file entries, against 7376 for the archive built at tag `v1.7.0_gg3`). The release workflow at that commit builds with `./gradlew createConfluentArchive` and would build that archive, and the push workflow builds no archive and would not notice (read from the workflow files at that commit).

### Am I affected?

You are affected if you need to go back after upgrading, or if you want to know in advance what going back costs. This item has no query of its own; the checks are those of [item 4](#item-4-in-flight-batches).

### What to do

1. Before you change the branch, work through [item 4](#item-4-in-flight-batches) for each connector it applies to, with either `exactlyOnce` setting. A rollback is the one direction in which a batch can be stuck, and the commit of the next step removes this document from the branch. Keep the `v1.7.0_gg4` archive you deployed, or rebuild it from tag `v1.7.0_gg4` (check out the tag and run `./gradlew createConfluentArchive`), because item 4 may need you to start it again.
2. Back out the whole release in one commit on `custom_build`, by restoring the tree of tag `pre-v1.7.0_gg4`:
   1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing, with one expected exception: a fresh checkout of the published tip lists ` M gradlew.bat` without any change of yours, because the committed `gradlew.bat` has Windows line endings and `.gitattributes` asks git to normalise `*.bat` files (measured in the dry run of this rollback, in a fresh worktree; the same line appears again after the rollback commit, and it does not stop step 2). Commit whatever else it lists, or set it aside with `git stash -u` (a plain `git stash` leaves untracked files behind), or work in a fresh clone. The next command discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
   2. Run `git read-tree -u --reset pre-v1.7.0_gg4`, then `git commit -m "Roll back v1.7.0_gg4"`. This commit is `<rollback-commit>` below; `git rev-parse HEAD` right after the commit prints it. `git diff --quiet HEAD pre-v1.7.0_gg4 && echo ok` prints `ok`.

   Use this form rather than `git revert -m 1` on the release merge. It restores a known tree and works on whatever tip `custom_build` has reached, including after later commits.
3. Redeploy the artifact built at tag `v1.7.0_gg3`. If you kept the archive you deployed for `v1.7.0_gg3`, use it. Otherwise check out tag `v1.7.0_gg3` and run `./gradlew createConfluentArchive`. No release asset is published for any tag, so the archive is always your own build. Never deploy an archive built from the rolled-back branch tip: that build passes and its jar nests 25 dependency jars, as the paragraph above shows.

To go forward again, revert the rollback commit with `git revert <rollback-commit>`. The rollback removes this document from the branch; read it again with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg4.md`.

Rolling back brings back:

- field names matched in the earlier escaped form, and the tables that were not mapped, so producers must be renamed back before the old release reads their records ([item 1](#item-1-names-and-enum-labels-as-written-in-create-table));
- for a Tuple element named `size`, `size0` or `size` followed by digits, the failed inserts of the affected shapes, the stored shifted row of one shape, and the unpaired-element WARN ([item 2](#item-2-tuple-elements-named-size));
- the Map key in the writer error text on the JSON-as-string route, and the `NullPointerException` text for an unknown `Enum` label ([item 3](#item-3-writer-error-text));
- a retry failure for a range that is in `BEFORE_PROCESSING` when you roll back ([item 4](#item-4-in-flight-batches));
- the `v1.7.0_gg3` MBean `version=` segment and client name ([item 5](#item-5-jmx-mbean-name-and-client-name));
- the `v1.7.0_gg3` bundled dependency versions, because you redeploy its archive ([item 6](#item-6-dependencies-build-and-the-connect-worker));
- the earlier, shorter key rule for `enableDbTopicSplit=true` in [`ROLLOUT-v1.7.0_gg1.md`](ROLLOUT-v1.7.0_gg1.md) and [`ROLLOUT-v1.7.0_gg2.md`](ROLLOUT-v1.7.0_gg2.md), which can miss a key; take the rule from step 2 of item 4 of the gg3 note;
- on the branch, the gg4 documents themselves: this note, the CHANGELOG section and the FORK-DELTA rows.
