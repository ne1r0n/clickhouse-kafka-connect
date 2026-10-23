# 1.7.0_gg4, 2026-10-23

This is the fork release built on the same upstream base, `1.7.0`, as `1.7.0_gg3`, plus upstream's untagged tail
`a6f1df83`. It lists only the fork's own changes since `v1.7.0_gg3`. See [docs/FORK-DELTA.md](docs/FORK-DELTA.md) for
everything the fork patches on top of upstream, and work through
[docs/ROLLOUT-v1.7.0_gg4.md](docs/ROLLOUT-v1.7.0_gg4.md), the operator checklist with a self-check per item, before
upgrading from `v1.7.0_gg3`. If you are coming from an earlier release, start with the `1.7.0_gg3` checklist.

## Important Changes

* Rename producer fields before you upgrade. The connector now matches a Kafka record field to a column name, a `Tuple`
  element name or an `Enum` label as it is written in `CREATE TABLE`, on client V1 and on client V2. A field that is
  still named in the earlier, escaped form (a doubled backslash, a backslash and a letter for a line feed, a tab or a
  carriage return, a backslash before a slash) no longer matches: after the upgrade a root field under the earlier name
  stores the column's DEFAULT with no error and no log line, and a `Tuple` element under it fails the record. Rename
  those fields to the names written in `CREATE TABLE`. Let the old release consume everything that was produced under
  the earlier names, stop it cleanly, switch the producers, then upgrade. Tables that earlier releases could not map
  (a double quote in a name on client V2, a tab or a carriage return in a name on client V1) are mapped now, so records
  for them start to land. Rows stored by earlier releases are not repaired.
* A rollback to `v1.7.0_gg3` with batches in flight needs a clean stop for a table with a `Tuple` element named `size`,
  `size0` or `size` followed by digits, and for a table whose field names changed under the notice above. The upgrade
  needs a clean stop for a table whose `Tuple` element named `size` stored a shifted row on `v1.7.0_gg3`, and for the
  producers' rename. No other change in this release needs one. The rollout checklist has the query that finds batches
  in flight and the procedure.
* Two writer failure texts change. On connectors whose `clickhouseSettings` set
  `input_format_binary_read_json_as_string` to `1` or `true`, the JSON library's own message, which could name a `Map`
  key, no longer reaches the task status, the stack-trace header of the dead letter queue or the `Error inserting
  records` log lines. An unknown `Enum8` or `Enum16` label now fails as `java.lang.IllegalArgumentException` where it
  was `java.lang.NullPointerException`. An alert that matches `through reference chain`, `No serializer found` or
  `NullPointerException` for these failures stops matching; `Failed to write column ` keeps matching.
* The bundled dependencies moved: guava `33.7.2-jre` (was `33.4.6-jre`), httpclient5 `5.6.5` (was `5.6.2`), httpcore5
  `5.4.4`, Jackson `2.22.4` and gson `2.14.0`. The connector is compiled against `connect-api` `4.4.0`, which the
  archive does not bundle, so the Connect worker's version governs at runtime. The oldest worker the connector is
  verified against is Apache Kafka 3.9, as shipped in `confluentinc/cp-kafka-connect-base:7.9.7`, by a static check of
  every Kafka reference of the compiled connector. KIP-1161 applies only from Kafka 4.2 workers, and none of the
  connector's seven list-type configuration keys carries a validator for it to tighten.
* The JMX MBean name's `version=` segment changes from `v1.7.0_gg3` to `v1.7.0_gg4`, and the client name sent on every
  request is now `clickhouse-kafka-connect/v1.7.0_gg4`. The HTTP client token in the user agent changes with the
  httpclient5 version. Dashboards, alerts and exporter rules that spell out the old values stop matching.
* A rollback goes back to the archive built at tag `v1.7.0_gg3`. The build of the rolled-back branch tip ends green but
  writes an archive whose inner jar nests 25 dependency jars instead of their classes, so never deploy an archive built
  from the rolled-back branch tip.

## Known Issues

* If the declared type of a column cannot be read, the connector keeps the earlier behaviour for that column: it drops
  the sub-columns named `size`, `size0` or `size` followed by digits, so a `Tuple` with such an element is still written
  short. It logs `Could not read the declared type ...` once per column and table description.
* Not measured on a record: a column name, `Tuple` element name or `Enum` label that holds a control character other
  than a line feed, a tab or a carriage return, and a name that holds a `.`. For a backspace, a form feed, a NUL and a
  bell the table description was measured and the table is mapped, but no insert was run.
* A Jackson exception whose cause chain also holds a non-Jackson `IOException` still passes through with its message, so
  that a timeout keeps its retry. On the JSON-as-string route that chain comes only from a record value whose own getter
  throws such an exception, and Connect's value types have no such getter.
* On the `insertFormat=json` route a Jackson failure can still occur. Whether its text carries a `Map` key into the
  task status, the dead letter queue or the logs was not measured.

## Bug Fixes

* A `Tuple` element named `size`, `size0` or `size` followed by digits, at any depth of a column's type, is no longer
  dropped when the connector reads the table's columns, so the connector now writes the whole tuple for such a column.
  Earlier releases wrote a shorter one, which the server rejected or, for some values, stored wrong. Rows stored by
  earlier releases are not repaired. One case keeps the earlier behaviour: if the declared type of the
  parent cannot be read, a `Tuple` element named `size` or `size0` is still dropped, so that tuple is still written
  short. The connector logs a `Could not read the declared type ...` WARN per such parent and a `Declared Tuple
  element(s) ... have no matching DESCRIBE sub-column` WARN naming the dropped elements, and the table is still served.
  A parent is also unreadable when it is absent from the described rows, not only when its type cannot be parsed.

* On connectors whose `clickhouseSettings` set `input_format_binary_read_json_as_string` to `1` or `true`, a record
  value the JSON serializer cannot write no longer puts a `Map` key from the record into the task status, the DLQ
  `exception.stacktrace` header or the `Error inserting records` log. The failure is now reported like other conversion
  failures, as `Failed to write column ...` naming the serializer's exception class with its message withheld. A failure
  whose cause chain also holds a stream or timeout error still passes through unchanged, so a timeout is still retried;
  on this route only a record value whose own getter throws such an error builds that chain, and Connect's value types
  have no such getter.

* An `Enum8` or `Enum16` value whose label is not one of the column's labels now fails with `Failed to write column
  ...: java.lang.IllegalArgumentException` instead of `java.lang.NullPointerException`. The label stays out of the
  message. The `No content in enum` text, reachable only from a hand-built column, now names the column.

* A column or `Tuple` element whose name contains a backslash, a slash, a line feed, a tab, a carriage return or a
  double quote is now matched under its name as written in `CREATE TABLE`, on both clients, and an `Enum8` or `Enum16`
  label holding such a character (or an apostrophe) is matched under its own text. Earlier releases on client V2 matched
  only an escaped form of such a name, stored the column's DEFAULT for a root field that used the real name, failed the
  record for a `Tuple` element, and left a table unmapped for a double quote in a name or an apostrophe label followed
  by another label. Earlier releases on client V1, the default, left a table with a tab or a carriage return in a name
  unmapped, read a slash or a double quote in a name as a replacement character so no field matched it, and logged a
  fallback WARN for a line feed. A record that still uses the escaped form now gets the DEFAULT at the root and fails
  for a `Tuple` element, so rename such fields before upgrading. Rows stored by earlier releases are not repaired.

## Build and CI

* The Confluent archive is built with Gradle wrapper `9.8.1` and Shadow `9.6.1`. The archive task is repaired, because
  Shadow 9 turned the earlier build line into a green build of an archive whose inner jar nests the dependency jars. A
  new archive check fails the build on a nested jar, on an `org/apache/kafka` entry or on a missing connector or client
  class, and the CI workflow runs it after the archive task.
* `connect-api` `4.4.0` is compile-only. A unit test lists every Kafka class and member the compiled connector
  references and fails when a new reference is not on the reference list.
* Upstream's guava override and httpclient5 pin came in with its untagged tail, and nine Dependabot merges (Connect API,
  Gradle wrapper, Shadow, httpclient5, gson, Jackson, slf4j, commons-lang3 and the versions plugin) landed on the
  fork's branch after `v1.7.0_gg3`. The shipped archive differs from the `v1.7.0_gg3` one only in the bundled versions
  above and in the connector's own classes, which the compare checked entry by entry.
* `slf4j-reload4j` is declared through the version catalog and moves with the other `slf4j` libraries.
* The workflow pin-guard test strips comments string- and line-comment-aware, accepts a trailing comment on the pinned
  runner label, rejects commented-out fork-count text, and reads the `dependabot.yml` layouts that YAML allows.
* A test fails when a line of `src/main` carries a planning identifier.
* The fork coverage report lists 21 classes.
* Two Gradle tasks capture and compare the server's `DESCRIBE` rows for 47 fixture tables: `captureDescribeFixtures`
  writes them, and `describeFixtureDrift` fails when a newer server changes a row. Both need Docker and run on demand.

# 1.7.0_gg3, 2026-10-20

This is the fork release built on the same upstream base, `1.7.0`, as `1.7.0_gg2`. It lists only the fork's own changes
since `v1.7.0_gg2`. See [docs/FORK-DELTA.md](docs/FORK-DELTA.md) for everything the fork patches on top of upstream,
and work through [docs/ROLLOUT-v1.7.0_gg3.md](docs/ROLLOUT-v1.7.0_gg3.md), the operator checklist with a self-check per
item, before upgrading from `v1.7.0_gg2`. If you are coming from an earlier release, start with the `1.7.0_gg2`
checklist.

## Important Changes

* When the connector cannot convert a record value into a ClickHouse column on the RowBinary route (an invalid UUID
  string, a number that cannot be parsed or is too wide, a `Decimal` out of range, a `FixedString` that is too long, an
  unknown `Enum` label), the error text no longer carries the value. The Connect task status, the stack-trace header of
  the dead letter queue and the `Error inserting records` log lines now name the column, the ClickHouse type and the
  exception class, and the original frames stay visible. An alert that matches `Failed to write column ` keeps matching;
  an alert that matches a library message such as `Invalid UUID string` stops matching. The class-name and message
  headers of the dead letter queue are unchanged.
* The only exception to that we know of is a Map key, on connectors whose `clickhouseSettings` sets
  `input_format_binary_read_json_as_string` to `1` or `true` and whose records carry a value Jackson cannot serialize.
  There the JSON library's own failure text is passed on unchanged, and it can name the Map key in the task status, the
  stack-trace header of the dead letter queue and the `Error inserting records` log lines (measured on ClickHouse
  26.9.19.9). No Map value, no Struct field value and no list element appeared in any of them. The rollout checklist has
  a check for your connector configurations.
* New WARN, logged every time a table is described, when a `Tuple` element that a column's declared type names has no
  matching column in the table description. The line names the database, table, column, element paths and declared type
  and carries no value. It marks a table hit by the known issue below, so a table it names may already have been
  failing before the upgrade. The connector describes every table that its `targetTableFilter` lets through, so the
  line can name a table that this connector never writes.
* The JMX MBean name's `version=` segment changes from `v1.7.0_gg2` to `v1.7.0_gg3`, and the client name sent on every
  request is now `clickhouse-kafka-connect/v1.7.0_gg3`. Dashboards, alerts and exporter rules that spell out the old
  values stop matching.
* A rollback to `v1.7.0_gg2` with batches in flight needs a clean stop only for a table with a `Nullable(Tuple(...))`
  whose element name holds a backslash followed by `n`, or a line break. The upgrade needs no clean stop, and neither
  does any other change in this release. The rollout checklist has the query that finds such tables and the procedure.

## Known Issues

* A `Tuple` element named `size` or `size0` at any depth of a column's type, or an element named `size` followed by
  digits where an `Array` encloses its Tuple, is dropped when the connector reads the table. On the default route with
  client V2, inserts that were run into such tables (four shapes: `Tuple(size ...)`, `Array(Tuple(size ...))`,
  `Array(Tuple(size1 ...))` and `Array(Tuple(v String, size1 ...))`) failed with the server error code 32 or 33 and
  stored no row, on `v1.7.0_gg2` and on this release alike. The other affected shapes were detected by the WARN and
  were not inserted, and a shorter row that happens to parse could store a wrong value instead of failing; neither
  was measured. This release adds the WARN described above that names such a table, but it has no fix. The rollout
  checklist has a query that lists suspect tables and the log line that confirms one.

## Bug Fixes

* A `Nullable(Tuple(...))` whose element name holds a backslash followed by `n`, or a line break, now pairs with its
  declared type, so an insert into it stores the row instead of failing with code 33 (measured for a `Nullable(Tuple)`,
  an `Array` of it and a `Map` of it), and the fallback WARN for such a table stops.
* On client V1, a record whose text contains a ClickHouse code 33 or code 131 message no longer triggers a
  table-mapping refresh and a retry of the insert.

## Build and CI

* Three dependency updates merged from Dependabot, lombok `1.18.48`, spotless `8.10.4` and slf4j-reload4j `2.0.20`,
  leave the jar inside the shipped Confluent archive unchanged. slf4j-reload4j is on the runtime classpath only, so it
  is not in the archive.
* The workflow pin-guard test also checks the fork-count step and accepts a trailing comment on the pinned runner
  label.
* The writer error-path tests no longer inline-mock the connector's `ClickHouseHelperClient`; they use a subclass.

# 1.7.0_gg2, 2026-10-14

This is the fork release built on the same upstream base, `1.7.0`, as `1.7.0_gg1`. It lists only the fork's own changes
since `v1.7.0_gg1`. See [docs/FORK-DELTA.md](docs/FORK-DELTA.md) for everything the fork patches on top of upstream,
and work through [docs/ROLLOUT-v1.7.0_gg2.md](docs/ROLLOUT-v1.7.0_gg2.md), the operator checklist with a self-check per
item, before upgrading from `v1.7.0_gg1`. If you are coming from an earlier release, start with the `1.7.0_gg1` rollout
checklist.

## Important Changes

* `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` values are now stored as the record says, at the top
  level and inside `Array` and `Map` columns, and a null entry in such an `Array` or `Map` stores NULL instead of failing
  the batch. A record that omits such a column, where the column has no `DEFAULT`, now stores NULL instead of failing
  schema validation. The limitation that the `1.7.0_gg1` startup WARN announced is closed, and that WARN is gone, so a
  log alert that matches it stops matching. Rows that earlier releases already stored wrongly are not repaired by the
  connector; the rollout checklist has queries that list the suspects.
* A single-field `Nullable(Tuple(a T))` whose field `a` is null now fails the batch with a `DataException` that names the
  tuple field, where the earlier release silently stored a NULL tuple. Under `errors.tolerance=none` the task stops, and
  stops again on every retry because the batch is rebuilt identically; under `errors.tolerance=all` the batch goes to
  the dead letter queue. A NULL for the whole tuple still stores NULL.
* New WARN, once per affected column each time a table is described, when the declared type of a column that contains a
  `Tuple` cannot be parsed or does not match the column structure. The column then keeps the nullability that
  `DESCRIBE` reported, so it is still exposed to the old behaviour. The line names the database, table, column and type
  and carries no value.
* With `debeziumCDCEnabled=true`, empty Debezium records (a truncate event, or an event whose value struct is null) no
  longer insert a row on the `bypassRowBinary=true` JSON route and on RowBinary, as they already did not on the explicit
  `insertFormat=json` route. Offsets, the exactly-once state and the deduplication token still cover the whole batch.
  Phantom rows written by earlier releases are not removed.
* A missing value for a non-nullable column without a default now fails with a `DataException` that names the column and
  its type, instead of a `RuntimeException` without a message. The ERROR line `Column <name> is not nullable and no value
  is provided` is no longer logged. Failure handling is unchanged.
* Changed log lines that alert rules may match: the DateTime parse failure line now reads
  `Error parsing date time string for column '<name>' ...` and never carries the value, and the generic write failure
  line `Error writing value of ...` carries the exception class instead of a stack trace. A matcher on the prefix of
  either line still matches; one on the old colon form does not.
* A batch that `v1.7.0_gg1` left in flight under `exactlyOnce=true` and that `v1.7.0_gg2` retries keeps whatever the
  earlier attempt stored, because ClickHouse drops a retry that carries the same deduplication token even when its bytes
  differ. Stop connectors cleanly and check for in-flight batches before upgrading; the first item of the rollout
  checklist has the procedure.
* The JMX MBean name's `version=` segment changes from `v1.7.0_gg1` to `v1.7.0_gg2`, and the client name sent on every
  request is now `clickhouse-kafka-connect/v1.7.0_gg2`. Dashboards, alerts and exporter rules that spell out the old
  values stop matching.

## Bug Fixes

* A writer failure whose exception has no message now surfaces as itself instead of being replaced by a
  `NullPointerException`, and a socket timeout without a message at that point is retried where it used to fail the task.
* Error handling no longer throws a `NullPointerException` for an `IOException` without a message. The failure now goes
  through the `errors.tolerance` handling, so the original exception is no longer lost.
* Date, Date32, DateTime and DateTime64 conversion errors now name the column.
* The DateTime parse-failure log and the generic write-failure log no longer carry record values. One known open item
  remains: the message of a rethrown write failure can still carry a value into the task status, the dead letter queue
  headers and two `Error inserting records` log lines.

## Build and CI

* The test workflow pins every action to a commit SHA (Node 24 releases), runs on `ubuntu-24.04` instead of the floating
  `ubuntu-latest` label, uses a read-only token and does not persist credentials in the checkout.
* The upstream `test-head.yaml` workflow is deleted from the fork.
* Dependabot also covers GitHub Actions, in one monthly grouped pull request that moves the commit pins together.
* The test classpath drops a duplicate lz4 library, so the tests load one copy. This is test-only; the shipped
  classpaths are unchanged.
* `lombok.config` gains a line that selects Jackson 2 for `@Jacksonized`, which removes a compile-time warning. The
  generated code of the one class that uses `@Jacksonized` is unchanged.

# 1.7.0_gg1, 2026-10-10

This is the fork release built on upstream `1.7.0` (the section below). It lists only the fork's own changes since
`v1.6.0_gg1`. See [docs/FORK-DELTA.md](docs/FORK-DELTA.md) for everything the fork patches on top of upstream, and
work through [docs/ROLLOUT-v1.7.0_gg1.md](docs/ROLLOUT-v1.7.0_gg1.md), the operator checklist with a self-check per item,
before upgrading.

## Important Changes

* With an explicit `insertFormat=json`, `Struct` and Debezium CDC records now land rows through the JSON writer.
  Previously they were routed to RowBinary against a column-less table and silently dropped while the offsets still
  advanced. The change is unconditional and one-way. Recovering rows lost under `exactlyOnce=true` needs a consumer
  offset reset and a KeeperMap state clear; the procedure is in the rollout checklist.
* Explicit `insertFormat=json` together with `bypassRowBinary=true` and `Struct`/CDC records now goes through the
  metadata-free writer instead of the legacy Jackson path, so the stored Timestamp, Date, Time, Decimal and bytes values
  change for the same input. Under `exactlyOnce=true` run the pre-upgrade check in the rollout checklist first.
* A null in a nested non-nullable `Array` element, `Map` value or `Tuple` field now throws `DataException` naming
  the column, and the whole batch fails (dead-lettered under `errors.tolerance=all`, task-stopping under
  `errors.tolerance=none`). A null nested `Array` or `Map` is coerced to an empty container.
* `Int256` and `UInt256` columns now fail fast on RowBinary with an actionable message naming the column and the
  `insertFormat=json` and `bypassRowBinary=true` alternatives, replacing "Data schema validation failed.".
* New startup WARN, once per column, for `Nullable(Enum8)`, `Nullable(Enum16)` and `Nullable(Tuple(...))` slots.
  Values in such a slot may be stored as NULL or shifted with no error. The root cause is not fixed in this release.
* Compression: the fork's forced `network_compression_method=lz4` setting is retired onto upstream's
  `setCompressionMethod` option (default `true`), and `compress=false` is always sent. A `compress=true` in
  `jdbcConnectionProperties` re-enables response compression on client V2 and must not be combined with
  `setCompressionMethod=false` against a ClickHouse 26.9+ server.
* The JMX MBean name's `version=` segment changes from `v1.6.0_gg1` to `v1.7.0_gg1`, and the client name sent on every
  request is now `clickhouse-kafka-connect/v1.7.0_gg1`. Dashboards, alerts and exporter rules that spell out the old
  values stop matching; item 7 of the rollout checklist gives matcher examples that keep working across releases.

## Bug Fixes

* Metadata-free JSON encodings: Timestamp as decimal seconds, Date as epoch days, Time as milliseconds, Decimal as a
  number and bytes as raw escaped bytes instead of base64.
* Debezium `io.debezium.time.Timestamp` and `io.debezium.time.NanoTimestamp` are converted to decimal seconds on the
  metadata-free JSON route. Previously the raw number was read as seconds.
* Empty CDC records are skipped on the metadata-free JSON route instead of writing an all-defaults row.
* `DESCRIBE` rows below `JSON`/`Dynamic` columns are skipped at any depth.
* `Array(Tuple)` element columns carry the parent's default and sub-column flags, so a missing tuple field without a
  default fails loudly instead of writing a stray marker byte.
* The `ConfigDef` default shown for `username` is now `default`.

## Dependencies

* Bumped `at.yawk.lz4:lz4-java` from `1.10.2` to `1.12.0`, past the advisory-free floor `1.11.4`: `1.11.2` and
  `1.11.3` are still affected by three published advisories, all fixed in `1.11.4`. See
  [docs/FORK-DELTA.md](docs/FORK-DELTA.md).
* Bumped `com.fasterxml.jackson` from `2.22.2` to `2.22.3`.
* Bumped `okhttp` from `5.4.0` to `5.5.0` (integration tests).
* Bumped `org.json` from `20260522` to `20260814` (tests).
* Bumped `mockito` from `5.23.0` to `5.24.0` (tests).

# 1.7.0, 2026-09-30

## Important Changes

* Fixed issue with server response compression when using ClickHouse 26.9+. Problem
  occurs because default `network_compression_method` value was switched from `LZ4` to `ZSTD`.
  But client do not detect it automatically in payload from server.
  (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/841)

## Bug Fixes

* Fixed NPE on null optional Map fields; they are written as empty ClickHouse maps. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/820)

## Confluent Archive

* Now includes shaded `org.lz4` to solve conflict between versions provided by `org.apache.kafka:connect-api > org.apache.kafka:kafka-clients`.

## Dependencies

* Bumped `com.clickhouse:*` from `0.9.5` to `0.9.6`.
* Fixed resolution of `org.lz4` library.

# 1.6.0, 2026-09-11

## Improvements

* New `retryOnSocketException` setting (default `false`). When enabled, a `java.net.SocketException` (broken pipe,
  connection reset) from the ClickHouse client is retried like the existing timeout cases instead of failing the
  task. With client V2's persistent connections a single server-side connection reset otherwise fails every task at once. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/817`)

* New option `enableReplicaPinning` on schema error retry should solve the problem with schema changes on cluster. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/827) 

* Tested support of Decimal Avro types. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/802)

## Dependencies

* Updated `com.fasterxml.jackson.core:jackson-core` from `2.21.5` to `2.22.2`
* Updated `org.hamcrest:hamcrest` from `2.2` to `3.0`
* Updated `com.google.protobuf` from `0.9.5` to `0.10.0`
* Updated `com.fasterxml.jackson.core:jackson-annotations` from `2.21` to `2.22`
* Updated `org.apache.httpcomponents.client5:httpclient5` from `5.5.1` to `5.6.2`

## Bug Fixes

* With client V2, server errors arrive as `com.clickhouse.client.api.ServerException` rather than the V1
  `ClickHouseException`, so the retriable error-code list in `Utils.handleException` never matched and tasks
  failed instead of retrying. Both exception types now share the same list. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/818)

* Fixed `enableDbTopicSplit=true` failing to insert into every database but the one set in the `database` config.
  `DESCRIBE TABLE` was built from the configured database rather than the database requested by the caller, so
  tables in the other databases were never resolved and records were dropped or rejected with
  `Table <db>.<table> does not exist`. Multi-character separators such as `dbTopicSplitChar=__` are covered.
  (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/580)

* Records are now batched per database when `enableDbTopicSplit=true`. A batch is inserted into the database of its
  first record, and stripping the database prefix off the topic left records for `dbA.t` and `dbB.t` sharing a batch
  key, so a single `put()` spanning multiple databases wrote all of its records into the first one. Note for
  `exactlyOnce=true` users: because batch composition changes, a batch left in the `BEFORE` state by an earlier
  version is not deduplicated on the first insert after upgrading. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/580)

* The connector no longer refuses to start with `Did not find any tables in destination` when `enableDbTopicSplit=true`
  and the configured database holds no tables, which is a valid multi-database setup.
* With internal buffering (`bufferCount > 0`, `exactlyOnce=false`), a retriable insert failure such as code 252
  `TOO_MANY_PARTS` left the failed batch in the buffer. Kafka Connect redelivers exactly that batch, so every retry
  appended another copy and the eventual successful flush wrote each record several times. The batch is now removed
  from the buffer when the flush fails, leaving the redelivery as the single copy.
  (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/801)
  and the configured database holds no tables, which is a valid multi-database setup. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/580)

* Fixed conversion from small integers to bigger ones. Previously Int32 values cannot be written to Int64 without 
transformation. Now number types can be written to wider number columns. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/642)

# 1.5.0, 2026-08-05

## Improvements

* RowBinary inserts with client V2 now stream data directly to the network output stream via the client's
  `DataStreamWriter` API. Increase in performance is ~ 30% for 100k dataset. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/796)  

* Support Avro nullable multi-type unions (e.g. `[null, string, int]`) mapping to a ClickHouse `Variant`.
  Confluent's Avro converter turns the non-null branches into a Connect union struct
  (`io.confluent.connect.avro.Union`), with the `null` branch making the field optional; the connector
  resolves this to `Variant(String, Int32)`. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/799)

* Support the Avro `decimal` logical type on a `fixed` base (in addition to `bytes`). Confluent's Avro
  converter maps both representations to a Kafka Connect `Decimal` (a `BigDecimal`), which the connector
  already validates, converts and serializes to a ClickHouse `Decimal(P, S)` column. This promotes the
  `decimal_fixed_logical` schema from the incompatible to the compatible Avro test fixtures and adds
  feature-test coverage. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/798)

## Dependencies

* [build] Updated `com.diffplug.spotless` from 8.6.0 to 8.9.0.
* [build] Updated `gradle-wrapper from` 9.5.1 to 9.6.1
* [tests] Updated `com.squareup.okhttp3:okhttp` from 5.3.2 to 5.4.0.

* Support Avro record unions (e.g. `[TypeA, TypeB]`) mapping to a ClickHouse `JSON` column. Confluent's
  Avro converter turns a union of records into a Connect union struct (`io.confluent.connect.avro.Union`)
  keyed by branch record name; the connector serializes this to JSON in tagged form (which branch it was
  is preserved). Writing to a `JSON` column in RowBinary requires `input_format_binary_read_json_as_string=1`,
  so the Avro integration harness now routes JSON-target fixtures through a `setupAvroConnectorWithJson`
  connector config. This promotes the `union_two_records` schema from the incompatible to the compatible
  Avro test fixtures; the integration test reads the rows back from ClickHouse and asserts the
  tagged-by-branch `JSON` value landed, not just the row count. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/800)

## Bug Fixes 

* Fixed issue with Table Schema cache that can remember old schema instead of new one. Now if old 
schema is read proper number of columns is calculated and cache will be properly updated after some time. 
(https://github.com/ClickHouse/clickhouse-kafka-connect/issues/813)

# 1.4.0, 2026-07-15

## Security

* Upgraded `com.fasterxml.jackson.core` dependencies to version `2.21.5` to address multiple CVE's in previous releases.

## New Features

* Internal buffering now supports `exactlyOnce=true` via strict-chunking mode. When both `bufferCount > 0` and
  `exactlyOnce=true`, records are bucketed per `(topic, partition)` and flushed only in fixed `bufferCount`-sized
  chunks. Tail records below the threshold remain buffered until subsequent `put()` calls grow the bucket past
  `bufferCount`. This keeps `(minOffset, maxOffset)` reproducible across retries, allowing ClickHouse
  `insert_deduplication_token` reuse and StateProvider range comparison to work correctly. Requires `bufferFlushTime=0`
  and `ignorePartitionsWhenBatching=false` — the start-up validator throws `ConnectException` otherwise.

## Improvements
* Cast/conversion failures in `ClickHouseWriter` now surface the failing column and its types.
  Previously a type mismatch (e.g. a `java.util.Date` reaching a `UInt64` column) threw a bare
  `ClassCastException` that named only Java types, and the useful context — column name, target
  ClickHouse type, source Kafka type — was written to the container logs only. The connector now
  rethrows these as a non-retryable `DataException` carrying that context, so it also reaches the Kafka
  Connect REST status, DLQ record headers, and JMX `task-error-metrics`. Record values are never
  included in the message. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/729)

## Internal Changes

* Refactored `ClickHouseSinkTask` to delegate to a `DeliveryStrategy` per delivery semantic (`DirectDeliveryStrategy`,
  `AtLeastOnceBufferStrategy`, `ExactlyOnceBufferStrategy`), with a shared `ChunkFlusher` for the insert +
  offset-tracking path. The buffering and non-buffering flows now live in separate, cohesive units instead of
  interleaved branches on the task. Behavior is unchanged.

## Bug Fixes
* Fixed `NullPointerException` when writing a `null` value into a `Nullable(JSON)` column via the binary
insert path with `input_format_binary_read_json_as_string=1`. The JSON case in `ClickHouseWriter` cast the
field straight to `String` without checking for `null` first, unlike every other nullable-aware type in the
same switch. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/562)

## Dependencies

* Bumped `org.json:json` from `20250517` to `20260522`.

# 1.3.10, 2026-06-24

## New Features
* Added `clusterName` configuration option. When set, the connector includes an `ON CLUSTER '<name>'`
clause in the distributed DDL it runs (e.g. the `ALTER TABLE ... ADD COLUMN` statements issued by
`auto.evolve`), so schema evolution propagates across all replicas in a clustered deployment. Empty by
default. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/716, https://github.com/ClickHouse/clickhouse-kafka-connect/issues/738)

## Bug Fixes
* Inserts now time out instead of blocking forever when ClickHouse becomes unresponsive (network partition,
server hang, long GC pause). Previously a stuck insert could hold the task past
`consumer.override.max.poll.interval.ms`, causing Kafka to rebalance the partitions mid-insert. The new
`clickhouseClientInsertTimeoutMs` setting (default `240000`) bounds the wait; on timeout a retriable
exception is thrown so the Connect framework retries the batch. Set this below
`consumer.override.max.poll.interval.ms`. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/756)

## Improvements
* Reduced log noise: messages about unhandled complex sub-types encountered while building table mappings
are now logged at `DEBUG` instead of `WARN`, since they are expected and not errors.
(https://github.com/ClickHouse/clickhouse-kafka-connect/pull/767)

## Dependencies
* Bumped `org.projectlombok:lombok` from `1.18.38` to `1.18.46`.
* Bumped `org.apache.httpcomponents.client5:httpclient5` (test scope) from `5.5` to `5.6.1`.

## Build
* Upgraded Gradle from `7.4.2` to `9.5.1` and migrated dependency management to a Gradle version catalog
(`gradle/libs.versions.toml`) and the `jvm-test-suite` plugin.

# 1.3.9, 2026-05-21

## Bug Fixes
* To gracefully handle live schema drift between the connector and ClickHouse, refresh the table mappings when connector encounters `Code: 131. DB::Exception: Too large string size` and log column-level schema updates. Now, the connector refreshes its table mappings automatically when ClickHouse returns either `Code: 33` or `Code: 131` and retries the insert. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/751)


# 1.3.8, 2026-05-08

## New Features
* Added `auto.evolve` configuration option for automatic table schema evolution. When enabled, the connector
  detects new fields in incoming records and issues `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` against ClickHouse.
  Disabled by default. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/277)

## Bug Fixes
* Fixed RowBinary serialization for Map columns with Nullable value types. The nullable marker byte was missing
  when writing map values, causing `CANNOT_READ_ALL_DATA` errors for `Map(K, Nullable(V))` columns. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/687)
* Fixed `com.clickhouse.kafka.connect.transforms.KeyToValue` transformation to handle different schemas. Before
  this fix, the value schema was cached and not updated, making the transformation incompatible with evolving
  schemas. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/718)

## Dependencies
* Updated clickhouse-java version from `0.9.4` to `0.9.5`

# 1.3.7, 2026-03-25

## Security
* Upgraded `com.fasterxml.jackson.core` dependencies to version with fix for https://github.com/advisories/GHSA-72hv-8253-57qq (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/690).

# Improvements
* `Gson` replaced with `Jackson` for performance and better maintainability (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/676).

# 1.3.6, 2026-03-18

## New Features
* Added internal record buffering support via `bufferCount` and `bufferFlushTime` configuration options.
  When enabled, records from multiple `poll()` calls are accumulated and flushed as a single large batch,
  reducing the number of insert operations to ClickHouse. Buffering is disabled by default (bufferCount=0) for full backward compatibility. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/658)

## Improvements
* Report inserted offsets in `preCommit()` method. Previously connector was returning same map that is passed to
  the method. This may lead to missed offsets in a situation of partition rebalance. Feature is turned off
  by default and `reportInsertedOffsets` property should be set to `true` to enable. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/669)

## Bug Fixes
* Fixed invalid concurrency handling in `ClickHouseWriter.updateMapping`. Previously flag was set not in atomic way. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/678)
* Fixes handling server error "Code 33" after schema is updated. Previously logic did not wait for target table to be updated.
  After the fix logic will use describe of a single table when detects that `updateMapping` is running. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/680)

# 1.3.5, 2025-10-11

## Improvements
* Added topic metrics to JMX and extended task metrics. Topic metrics have partition granularity - each partition has its own metrics (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/612)
* Improved failure handling when messages should be sent to DLQ. Previous implementation was sending whole batch to DLQ
  when one message failed. Now it sends only failed group of messages to DLQ. If failed because of schema validation then
  logs error message contains field name and schema type. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/590)
* Added support for writing boolean values to number columns. This is allowed because boolean fits into any number type. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/633)
## Bug Fixes
* Fixed error message about unhandled complex type in array (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/608)
* Fixed logging in schema validation logic. Message "Got non-root column, but its parent was not found to be updated"
  was logged as error but should be a warning. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/645)
* Fixed writing Avro timestamp value. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/599)
* Fixed negative timestamp in logs. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/614)
## Dependencies
* Updated clickhouse-java version to `0.9.4` (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/629)

# 1.3.4, 2025-10-08
## Bug Fixes
* Added column named that causes error (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/607)
* Updated clickhouse-java version `0.9.2` (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/596)
* Fixed Client Name reported for `query_log` (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/542)
* Fixed writing Avro union of `string` and `bytes` to `String` column (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/572)

# 1.3.3, 2025-08-30
## Bug Fixes
* Fixed writing JSON values to ClickHouse. Previously all JSON Object were written as objects where field value were wrapped with `{ "object": <field value> }`. Now objects stored with original structure. (https://github.com/ClickHouse/clickhouse-kafka-connect/issues/574)

## New Features
* Added support of SimpleAggregateFunction column type. (https://github.com/ClickHouse/clickhouse-kafka-connect/pull/571)

# 1.3.2, 2025-07-04
## Improvements
* Added support of writing JSON as string to a JSON column.
## Dependencies
* Upgraded dependency `org.slf4j:slf4j-reload4j` from `2.0.13` to `2.0.17`
* Upgraded dependency `org.apache.httpcomponents.client5:httpclient5` from `5.4.4` to `5.5`
* Upgraded dependency `com.fasterxml.jackson.core:jackson-annotations` from `2.19.0` to `2.19.1`
* Upgraded dependency `org.projectlombok:lombok` from `1.18.34` to `1.18.38`

# 1.3.1, 2025-05-19
## Improvements
* Ensure resources are auto-closed in exactlyOnce
* Adjust JSON parsing for backslash characters

# 1.3.0, 2025-04-14
## Improvements
* Added additional debug logging around KeeperMap and exactlyOnce

# 1.2.9, 2025-03-25
## Improvements
* Add `ignorePartitionsWhenBatching` flag to adjust batching when not using exactlyOnce
* Add version information to MBean

# 1.2.8, 2025-02-13
## Improvements
* Reduce archive size by removing unnecessary dependencies from the jar
## Bug Fixes
* Fix NPE on V1 client when Clickhouse has table with Array(Nested...) field

# 1.2.7, 2025-02-04
## Improvements
* Bump clickhouse-java to 0.8.0
## Bug Fixes
* Fix for handling of nullable fields in tuples ClickHouse data type

# 1.2.6, 2024-11-28
## Improvements
* Detect if table schema has changed and refresh the schema
* Allow bypassing field cleanup

# 1.2.5, 2024-11-21
## Improvements
* Remove redis state provide since we are using KeeperMap for state storage
* Remove unused avro property from `build.gradle.kts`
* Trim schemaless data to only pass the fields that are in the table
* Allow bypassing the schema validation

# 1.2.4, 2024-11-04
## Improvements
* Adjusting underlying client version to `0.7.0`
* Bugfix for UINT handling

# 1.2.3, 2024-09-25
## Improvements
* Tweaking schema validation to allow for UINT

# 1.2.2, 2024-09-25
## New Features
* Adding a new property `tolerateStateMismatch` to allow for the connector to continue processing even if the state stored in ClickHouse does not match the current offset in Kafka

# 1.2.1, 2024-09-18
## Improvements
* Adding some additional logging details to help debug issues

# 1.2.0, 2024-08-26
## New Features
* Adding a KeyToValue transformation to allow for key to be stored in a separate column in ClickHouse

# 1.1.4, 2024-07-30
## Bug Fixes
* Bugfix to address field value to column name mapping for Tuples

# 1.1.3, 2024-07-22
## Improvements
* Update to java-client 0.6.3
* Bugfix to address commas in the column name of enums

# 1.1.2, 2024-07-18
## New Features
* Adding a "dateTimeFormat" configuration option to allow for custom date/time formatting with String schema values
* Adding ephemeral column support and adding an error message

# 1.1.1, 2024-06-27
## Bug Fixes
* Bugfix to address string encoding issue
* Bugfix to address issue with nested types and flatten_nested setting conflict
* Bugfix to avoid storing keeper state in same column name if virtual topic is enabled
* Updated java-client to 0.6.1
* Bugfix to let create missing KeeperMap entries if there are some records present already
* Added a flag to allow bypassing RowBinary and RowBinaryWithDefaults format for schema insertions
* Bugfix to remove erroneous error messages about complex type handling

# 1.1.0, 2024-05-22
## New Features
* Updated java-client to 0.6.0-patch4
* Added config 'keeperOnCluster' to help self-hosted use exactly-once
* Added support for Tuple type
* Added support for Variant type
* Added support for Nested type
* Refactored Column class so that we use Builder pattern using Lombok
* Refactored recursive Map type parsing to iterative approach using describe_include_subcolumns=1
* Adjusted logging to reduce volume while keeping important information
* Adjusted tests to be more reliable and self-cleaning

# 1.0.17, 2024-04-10
## New Features
* Added support for ClickHouse Enum type #370
* Added extra break down of time measurement for insert operations

# 1.0.16, 2024-04-02
## Improvements
* Removed 1002 from the retriable exceptions list as it's a catch-all for all other exceptions
* Change Java Client to 0.6.0-patch3 and switch tests to use same Base class

# 1.0.15, 2024-03-20
## Improvements
* Added code 107 to the exception list and added more DLQ logging
* Added ExtractTopic transform to our source so that it would work on MSK

# 1.0.14, 2024-03-03
## Bug Fixes
* Fixed print vs log bug
* Fixed bug in overlap logic, added more tests to support this
* Fixed null-pointer bug in stop() method
* Updated dependencies

# 1.0.13, 2024-02-26
## Bug Fixes
* Fix missing jdbcConnectionProperties setup in ClickHouseWriter.getMutationRequest

# 1.0.12, 2024-02-21
## New Features
* Added support for multiple databases in single stream using a virtual topic  #41
* Add support for configuring JDBC properties in connection URL (i.e. `?auto_discovery=true`)
* Added minimum version check for ClickHouse (currently `23.3`)
* Added support for fixed_string type

# 1.0.11, 2024-01-30
## New Features
* Added support for RowBinaryWithDefaults
* Updated dependencies
* Adjusting default values for some settings (like insert_quorum)
* Added string support for DateTime64

# 1.0.10, 2023-12-11
## Bug Fixes
* Fixed writing into nullable Decimal column by @mlivirov in #276
* Adjusting the deduplication token hash by @Paultagoras in #280
* Fixed exception for when an array type column gets null value by @mlivirov in #279
* Support for data ingestion into Array(DateTime64(3)) type columns by @alexshanabrook in #260

# 1.0.9, 2023-12-06
## Improvements
* Added more logging to help debug future issues
* Restored send_progress_in_http_headers flag

# 1.0.8, 2023-12-04
## Improvements
* Remove send_progress_in_http_headers flag as it conflicts
* Updated tests
* Updated dependencies

# 1.0.7, 2023-12-02
## Bug Fixes
* Fix Handle ClickHouse JDBC Client Timeout Exception by @ygrzjh in #252
* Make sure an HTTP request is successfully processed by ClickHouse

# 1.0.6, 2023-11-22
## New Features
* Added additional logging to help debug future issues

# 1.0.5, 2023-11-13
## New Features
* Added 'zkPath' and 'zkDatabase' properties to customize exactly-once state storage

# 1.0.4, 2023-11-09
* Bugfix

# 1.0.3, 2023-11-07
## New Features
* Added support for proxy configurations
* Additional test cases
* Additional error codes added to retry list (242, 319, 999)

# 1.0.2, 2023-11-02
## New Features
* Updated dependencies
* Add BYTES type support
* Add Decimal type support
* Additional exception handling
* Retriable Exceptions are now always retried (even when errors.tolerance=all)
* Support for org.apache.kafka.connect.storage.StringConverter

# 1.0.1, 2023-09-13
## New Features
* Added support for `tableRefreshInterval` to re-fetch table changes from ClickHouse

# 1.0.0, 2023-09-07
## New Features
* Additional tests for ExactlyOnce
* Allows customized ClickHouse settings using `clickhouseSettings` property
* Tweaked deduplication behavior to account for dynamic fields
* Added support for `errors.tolerance` and the DLQ

# 0.0.18, 2023-07-18
## New Features
* Support inline schema with org.apache.kafka.connect.data.Timestamp type
* Support inline schema with org.apache.kafka.connect.data.Time type

# 0.0.17, 2023-07-13
* Updating Logo

# 0.0.16, 2023-06-21
## Improvements
* Updated state handling so that warnings are posted, rather than errors + exceptions.
* Adding 285 TOO_FEW_LIVE_REPLICAS to the retry list
* Tweaking the code to switch to JSON handling when default values are present. This should be more reliable (if the
  data structure is simple enough) while supporting defaults. This is a temporary solution until we can implement a
  longer-term fix in core code.

# 0.0.15, 2023-06-02
## Bug Fixes
* Added 202 (TOO_MANY_SIMULTANEOUS_QUERIES) Code to retry list [Issue](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/109)
* Added 252 (TOO_MANY_PARTS) to the retry list

# 0.0.14, 2023-05-26
## Bug Fixes
* Fix for LowCardinality(t) and LowCardinality(Nullable(t)) [Issue](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/105)

# 0.0.13, 2023-05-08
## Bug Fixes
* Fix null exception when getting empty record

# 0.0.12, 2023-05-05
## Bug Fixes
* Add UNEXPECTED_END_OF_FILE as RetriableException #98

# 0.0.11, 2023-04-20
## Improvements
* Implemented retry mechanism to fix [Issue](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/74)
  ** Some ClickHouse errors (159 - TIMEOUT_EXCEEDED; 164 - READONLY; 203 - NO_FREE_CONNECTION; 209 - SOCKET_TIMEOUT; 210 - NETWORK_ERROR; 425 - SYSTEM_ERROR) as well as SocketTimeoutException and UnknownHostException will result in the connector retrying the operation (based on configuration).
  This should help mitigate temporary (but unavoidable) hiccups in network operations, though this list will likely be tweaked over time as well.

# 0.0.10, 2023-04-10
## Bug Fixes
* Nullable date columns fix [Issue](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/60)

# 0.0.9, 2023-03-12
## Bug Fixes
* Implicit date conversion [Issue](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/57). [#63](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/63)
* Handle null columns [Issue](https://github.com/ClickHouse/clickhouse-kafka-connect/issues/62). [#65](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/65)

# 0.0.8, 2023-02-10
## Bug Fixes
* Support nullable values in SchemalessRecordConverter. [#55](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/55)
* Add Emojis tests & fix utf-8 bug. [#52](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/52)
* Add manifest.json. [#44](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/44)

# 0.0.7, 2023-02-01
## Bug Fixes
* Fix index error in describeTable. [#49](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/49)

# 0.0.6, 2023-01-31
## Bug Fixes
* Don't validate JSON records against the table schema. [#47](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/47)

# 0.0.5, 2023-01-31
## New features
* Add support for Amazon MSK in [#46](https://github.com/ClickHouse/clickhouse-kafka-connect/pull/46)