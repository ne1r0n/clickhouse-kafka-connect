# Rollout checklist: `v1.7.0_gg6` to `v1.7.0_gg7`

**The connector source changes in this release.** `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` on the release tip reports 8 files changed, 194 insertions and 22 deletions. The five changes at a glance:

1. The helper clients that a started task holds are now closed when it stops or when its start fails; row F5 lists the paths that still leave one open (section A).
2. On client V2, an insert that gets no answer from the server now ends with a retriable error after `clickhouseClientInsertTimeoutMs`; this can produce a duplicate, and it does not reach client V1 (section B).
3. The in-memory map of inserted offsets reads the consumer's topic, which changes what `reportInsertedOffsets=true` reports under a topic-renaming SMT, and its limits under other topic-changing SMTs are stated (sections C and D).
4. A `RetriableException` anywhere in the cause chain of a failure is now retriable, and the connector logs one WARN at task start for a code in `additionalRetriableErrorCodes` that it does not treat as transient (sections E and F).
5. `gson` moves to `2.15.0`, and the release identity, which the JMX name and the client name carry, reads `v1.7.0_gg7` (section G, which also holds the rollback).

The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream. If you are coming from a release before `v1.7.0_gg6`, work through [`ROLLOUT-v1.7.0_gg6.md`](ROLLOUT-v1.7.0_gg6.md) first and return here afterwards.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value, such as `<connector-name>`, `<topic>`, `<group>` or `<dir>`; replace each one with your own value.

**How each claim is labelled.** Every effect of this release has one row in a table below, with a label for how the claim is known. There are four labels:

- `measured`: a test or a command ran, and the row names it.
- `read from the code`: the row names a file and a site; nothing ran for this claim.
- `derived`: the claim follows from other rows or from a stated mechanism; nothing ran for this claim itself.
- `derived (mechanism, not measured)`: the mechanism is stated and the effect was not observed.

A row that makes claims of different strength says which label each claim has, in the form `claim: label; claim: label`.

**Which connectors a row concerns.** The column `Applies when` limits each row. A connector runs client V2 only when its configuration sets `client_version` to a value other than `V1`; a connector that does not set `client_version` runs client V1, the default (read from the code: `ClickHouseSinkConfig` reads `client_version` with the default `V1`, and `ClickHouseWriter` selects client V2 for any other value). Every statement that holds only on client V2 says `client V2`. The values are:

- `client V2`, `client V1`, `client V1 and client V2` and `either client`: the row holds on that client, or on both. `client V2, async=false` and the like add a setting the row needs; `client V2, exactlyOnce=false` and `client V2, exactlyOnce=true` add the state mode.
- `exactlyOnce=true`: the row holds with that setting. A value can be followed by a parenthesis that says which claim of the row it covers.
- `reportInsertedOffsets=true, bufferCount 0, injective topic-renaming SMT`, `many-to-one rename or partition-changing SMT` and `any connector that sets additionalRetriableErrorCodes`: the row concerns exactly that configuration. `bufferCount 0` is the unbuffered delivery path, the default. With `exactlyOnce=false` and `ignorePartitionsWhenBatching=true` that path returns Connect's own offsets and keeps no inserted-offset map, and a row about reported offsets excludes that combination.
- `this release`: the row describes the release as a whole. `CI only`: the row concerns the build and test workflows, not the archive. `as named in the row`: the row names its cases itself.

## Before you upgrade

Work through these steps before you roll the new archive out. Each names the section it comes from.

1. On a connector that runs client V2 with `async=false`, compare the longest insert time you see against `clickhouseClientInsertTimeoutMs` (default four minutes), and decide whether to raise it or to set `socket_timeout` (section B). With `exactlyOnce=false`, a duplicate is possible after an insert that timed out and that the server committed anyway, in a table that does not deduplicate the repeat.
2. A connector that runs client V1, which is every connector without `client_version`, has no connector-side insert bound in this release (sections A and B).
3. A connector with `reportInsertedOffsets=true` under a topic-changing SMT: check the limits before relying on the reported offsets (sections C and D).
4. A connector with `errors.tolerance=all`: a failure that carries a `RetriableException` no longer reaches the dead letter queue and is redelivered with no retry budget (section E).
5. A connector that sets `additionalRetriableErrorCodes`: expect one WARN at task start for any code outside the vetted set (section F).
6. Update dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg6` or `clickhouse-kafka-connect/v1.7.0_gg6` (section G).

## A. Helper clients closed on stop

The `v1.7.0_gg6` note listed as a known limit that a stopping task never closed its helper client, so every task start left at least one client V2 object and its connection pool open for the life of the worker process. It named two more places that left clients open: the Keeper state provider that `exactlyOnce=true` builds, and, on client V1, the helper client that every insert batch builds. This release closes those clients; row F5 lists what it leaves open.

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| F1 | `ClickHouseWriter.stop()` closes its helper client once, after it stops the table-sync executor and waits up to five seconds for it; a second `stop()`, or one after a failed `start()`, closes nothing more; every failing exit of `start()` closes the client it opened. | closing: measured; five-second wait: read from the code | either client | `WriterHelperClientClosingUnitTest` counts the opens and closes of helper clients through a test seam, needs no ClickHouse server, and asserts each of these cases. The five-second wait is the `awaitTermination` timeout in `ClickHouseWriter.stop`; nothing ran for the wait itself. |
| F2 | On client V1, each insert batch builds one helper client, which builds a client V2 instance, to read its node, and now closes it right after; the node stays usable for the insert. | closing: measured; per-batch construction: read from the code | client V1 | `WriterHelperClientClosingUnitTest` asserts the close and that the node still serves the insert. `ClickHouseWriter.getMutationRequest` builds the helper client and closes it in a `finally`; the three client V1 insert methods call it. |
| F3 | With `exactlyOnce=true`, `KeeperStateProvider.close()` closes, once, only a client its own constructor built. The stop of the task's proxy (`ProxySinkTask.stop()`) stops the writer, the MBean and the state provider in turn; a throw in one still runs the others, the first exception reaches the caller and later ones are attached to it as suppressed. The state read and write paths are unchanged. | closing and stop order: measured; state paths unchanged: read from the code | exactlyOnce=true (stop order on either client) | `KeeperStateProviderClosingUnitTest` and `ProxySinkTaskStopUnitTest`. The state paths: `git diff v1.7.0_gg6 HEAD` of `KeeperStateProvider.java` shows no change in `getStateRecord` or `setStateRecord`. |
| F4 | For an operator: fewer HTTP connection pools stay open on a worker that restarts or rebalances tasks often. | derived (mechanism, not measured) | either client | Rows F1 to F3 state what is now closed. No worker was observed, with or without the change. |
| F5 | Not fixed: a `KeeperStateProvider` constructor that fails on client V1; a `ClickHouseHelperClient` constructor that fails after it built its client V2 instance; a second `start()` on one writer; `ClickHouseSinkTask.stop()` without `try`/`finally` around the delivery strategy's `stop()`; and the five-second wait, which can end with a table refresh still running on a closed client. | read from the code | as named in the row | `KeeperStateProvider` (the constructor throws after its client exists, and no object is left to close it), `ClickHouseHelperClient` (the constructor builds the client V2 instance first), `ClickHouseWriter.start` (it assigns its client without closing an earlier one), `ClickHouseSinkTask.stop` and `ClickHouseWriter.stop`. Nothing ran for these. |

What to do: nothing to configure. The residuals of row F5 remain.

## B. A bounded insert on client V2

This section concerns client V2 only. A connector that does not set `client_version` runs client V1, the default, and nothing in this section applies to it except rows T1 and T12. The rows describe an insert that gets no answer from the server; `async=false` is the client's default (read from the code: the comment on `ClickHouseWriter.awaitInsertResponse`).

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| T1 | Scope: the bounded insert of this section exists on client V2 only. A connector that does not set `client_version` runs client V1, the default, and nothing here applies to it except this row and row T12. On client V1, the client V1 library's own `socket_timeout` of 30000 ms stays the only read bound of an insert. | scope and default: read from the code; 30-second bound: measured | client V1 and client V2 | `ClickHouseSinkConfig` reads `client_version` with the default `V1`, and `ClickHouseWriter.startInternal` selects client V2 for any other value. The bound: a stub server that stopped answering ended a client V1 insert after 30140 ms, and the library's `socket_timeout` option defaults to 30000 ms; the probe ran on the tree before the bound was added, and the client V1 insert methods are not part of that change. |
| T2 | On client V2 with `async=false`, an insert into a server that stops answering now fails after `clickhouseClientInsertTimeoutMs` (default four minutes) with a retriable error (a `RetriableException` out of `put`). Before, it blocked the task. | measured | client V2, async=false | A stub server that accepts a request and stops answering, driven through `ProxySinkTask.put` on the JSON, String and RowBinary routes with an insert timeout of 300 ms: before the change each insert was still blocked at the probe's 8-second guard; after it (a probe on a scratch copy of the change) each ended with a `RetriableException` within 304 to 432 ms. `InsertStallBoundPinUnitTest` pins the outcome. |
| T3 | The failure is retriable under `errors.tolerance=none` and under `all`: the task does not fail and no dead letter queue record is written. | measured | client V2, async=false | `InsertStallBoundPinUnitTest.stalledV2InsertThroughPutIsRetriableWithNoDlqRecord`, six cases: the three routes under each tolerance value. |
| T4 | One request is sent per stalled insert (the client does not retry it), and the option is not sent to the server. | measured | client V2 | The stub server saw one request per stalled insert, and the request's query string held no `socket_timeout`. |
| T5 | A quiet insert, during which the server sends nothing while it works, that legitimately runs longer than `clickhouseClientInsertTimeoutMs` now fails and is redelivered, and the server may still commit it. | first run, the failure and the commit: measured; second run, the rows the failed insert committed: derived | client V2, async=false | Against ClickHouse `26.10.3.7` with a table whose insert takes about two seconds, a 1000 ms value failed the insert at 1003 ms and the row was committed. A second run with a connector-shaped insert and a 3000 ms value failed at 3030 ms. On a table created for that probe, the count after that run and an 8000 ms control run was 4000 rows, and the control run reported 2000 written rows, so the failed insert's rows were committed as well. The count and the control's figure are measured; the share of the failed insert is their difference. |
| T6 | An insert during which the server keeps sending progress headers could reset the per-insert timer and escape the bound; whether a connector insert does so was not measured. The client sends `send_progress_in_http_headers=1` on its insert requests. What two probes showed on ClickHouse `26.10.3.7`: a connector-shaped insert, with the rows in the request body, sent no progress header while the server worked and one at its end, so the per-insert timeout bounded it (a 3000 ms value failed at 3030 ms; an 8000 ms control succeeded at 4110 ms); a request through the client's query path, an `INSERT ... SELECT`, streamed a header about every 0.2 s, and a 1000 ms `socket_timeout` did not end it, which finished at 2056 ms. Whether a body insert that streams progress headers escapes the bound was not measured. | derived (mechanism, not measured) | client V2 | The mechanism is read from the code: the per-insert value is a socket read timeout (`ClickHouseHelperClient.setInsertSocketTimeoutV2`; the comment on `ClickHouseWriter.awaitInsertResponse` calls it a read-inactivity timeout, not a cap on total insert time), and a read timeout restarts when a byte arrives. The two probes are quoted for what they showed; neither is a connector insert that streamed headers. |
| T7 | With `exactlyOnce=false`, a timed-out insert that the server committed is redelivered and inserted again, so the batch can be inserted twice, a duplicate, in a table that does not deduplicate. By default each attempt carries the same `insert_deduplication_token`, so a table that deduplicates for that token may drop the repeat; with `ignorePartitionsWhenBatching=true` no token is sent. | redelivery and duplicate: derived; same token on each attempt: read from the code | client V2, exactlyOnce=false | The timed-out insert still committing is row T5. The redelivery path is read from the code: `put` raises a `RetriableException`, Kafka Connect redelivers the batch, and `Processing.doLogic` inserts it again from its in-memory state; `ClickHouseWriter` sets the token when `QueryIdentifier.getDeduplicationToken` has one, which the path through `doLogic` gives and the `ignorePartitionsWhenBatching=true` path does not. What a table does with the repeat follows from block deduplication; no probe ran. |
| T8 | With `exactlyOnce=true`, the retry of a timed-out insert reuses the `insert_deduplication_token`; whether the server drops the repeat depends on the target table's deduplication settings, which were not measured. | token reuse: read from the code; server deduplication: derived | client V2, exactlyOnce=true | `QueryIdentifier.getDeduplicationToken` builds `<topic>-<partition>-<min offset>-<max offset>` from the batch, each client V2 insert method of `ClickHouseWriter` sets it, and the exactly-once state machine rebuilds the same batch for a retry. The server half follows from ClickHouse's block deduplication; no probe ran. |
| T9 | A `socket_timeout` that the operator names in `jdbcConnectionProperties` keeps its value for every request, inserts included, and the per-insert value is not set; an explicit `socket_timeout=0` keeps inserts unbounded. | measured | client V2 | `InsertStallBoundPinUnitTest` (the two operator cases) and `InsertSocketTimeoutOptionUnitTest` (the helper sets no per-insert value when the operator names the key). A probe with `socket_timeout=1500` and an insert timeout of 300 ms ended at 1506 ms. A run against a changed copy that set the per-insert value unconditionally failed the operator cases. |
| T10 | Remedies for a legitimately slow insert: raise `clickhouseClientInsertTimeoutMs` (minimum 100) or set `socket_timeout` in `jdbcConnectionProperties`. | read from the code | client V2 | `ConfigDef.Range.atLeast(100)` on the option in `ClickHouseSinkConfig`; `ClickHouseHelperClient.setInsertSocketTimeoutV2` leaves its value unset when the operator names the key. |
| T11 | A write-side stall, a peer that stops reading the request body, is not bounded, because `socket_timeout` bounds reads only. | measured | client V2 | A stub server that accepted the connection and never read a 64 MiB body: the insert was still blocked after 15 s with a per-insert value of 500 ms. |
| T12 | Client V1 inserts have no connector timeout: the three client V1 insert methods wait with `future.get()` and no timeout argument. The client V1 library's own `socket_timeout` default of 30000 ms ended a read-side stall after about 30.1 s. A write-side stall on client V1 was not measured. | no connector timeout, three wait sites: read from the code; 30.1 s read bound: measured | client V1 | `ClickHouseWriter` (`doInsertRawBinaryV1`, `doInsertJsonV1` and `doInsertStringV1` each call `future.get()` with no timeout). A stub server that stopped answering returned a client V1 insert after 30140 ms with a retriable error, and the library's `socket_timeout` option defaults to 30000. |
| T13 | Requests other than client V2 inserts, namely `DESCRIBE`, the version query, the table refresh and, with `exactlyOnce=true`, the Keeper state queries, stay unbounded unless the operator sets `socket_timeout`; the ping has its own timeout. Before this release an operator could already bound a stalled insert the same way. | unbounded requests: read from the code; operator bound: measured | client V2 | `ClickHouseHelperClient` builds one client per helper, the per-insert value is set only in the client V2 insert methods of `ClickHouseWriter`, and `ping` takes the connector's own timeout. Operator bound: a probe on a stalled stub server in which `socket_timeout=400` in `jdbcConnectionProperties`, written with and without a leading `?`, ended the insert after 407 ms and 403 ms. |
| T14 | The help text of `clickhouseClientInsertTimeoutMs` still reads "Maximum time in milliseconds to wait for a single ClickHouse insert response" and tells you to keep it below `consumer.override.max.poll.interval.ms`. With `async=false` on client V2 the bound is carried by the per-insert `socket_timeout`, which restarts on every byte received, so it is not a cap on the total insert time. | read from the code | client V2, async=false | The option's documentation string in `ClickHouseSinkConfig`, and the comment on `ClickHouseWriter.awaitInsertResponse`. |
| T15 | Supersedes a statement of the `v1.7.0_gg6` note: its known limits said the four-minute insert timeout has no effect while `async=false`. That was true of `v1.7.0_gg6`; on client V2 it no longer holds (rows T2 and T3). The client V1 half of that statement stands, and the client V1 library's own bound is now measured (row T12). | read from the code | client V2 (the client V1 half stands) | `ROLLOUT-v1.7.0_gg6.md`, "Known limits carried into this release", second bullet, and the same statement in the known issues of the `v1.7.0_gg6` section of the changelog; rows T2 and T12. |

What to do:

1. On a connector that runs client V2, compare the longest insert time you see against `clickhouseClientInsertTimeoutMs`. For a legitimately slow insert, raise `clickhouseClientInsertTimeoutMs` or set `socket_timeout` in `jdbcConnectionProperties` (rows T9 and T10).
2. With `exactlyOnce=false`, expect a possible duplicate after an insert that timed out and that the server committed anyway, unless the table deduplicates the repeat (rows T5 and T7). With `exactlyOnce=true` the retry reuses its deduplication token (row T8).
3. Do not read the bound as a cap on the total insert time or as covering a write-side stall, a client V1 insert or a request other than an insert (rows T6, T11 to T14).

## C. The consumer's topic in the inserted-offset map

A topic-renaming SMT is a single message transform, such as `RegexRouter`, that changes the record's topic. An injective rename maps each source topic to its own target topic. The unbuffered delivery path, the default with `bufferCount` 0, is called the Direct path in this note; with `reportInsertedOffsets=true` it commits the offsets that the connector recorded for its inserts, except with `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, where it returns Connect's own offsets.

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| R1 | `Processing.doLogic` reads the consumer's topic from `SinkRecord.originalTopic()` (line 137 of `Processing.java`). Only the in-memory inserted-offset map reads it, so with `reportInsertedOffsets=true` under an injective topic rename the Direct path now reports offsets under the consumer's topic, and `onPartitionRemoved` clears them. Before, it reported nothing for the renamed topic. With no topic-changing SMT nothing changes, and with `exactlyOnce=false` and `ignorePartitionsWhenBatching=true` nothing changes either: `DirectDeliveryStrategy.preCommit` returns Connect's own offsets there, and `Processing.doLogic` keeps no state, so the map stays empty. | the one-line change, the four red cases, the reporting and the clearing: measured; only the in-memory map reads the topic, the exception for `ignorePartitionsWhenBatching=true`: read from the code | reportInsertedOffsets=true, bufferCount 0, injective topic-renaming SMT, not exactlyOnce=false with ignorePartitionsWhenBatching=true (that path returns Connect's own offsets and is unchanged) | `OriginalTopicInsertedOffsetsUnitTest`, which needs no ClickHouse server: on the tree before the change four of its six cases failed by assertion (among them `directDeliveryReportsTheConsumersTopicUnderAnInjectiveRename` and `closeClearsTheInsertedOffsetOfTheRevokedConsumerPartition`); on the changed tree all six pass. The change is one line in `Processing.java`, and `StateRecord.getOriginalTopic` is read only in `BaseStateProviderImpl.onStateUpdate`. The exception is the first branch of `DirectDeliveryStrategy.preCommit` and of `Processing.doLogic`. `FORK-DELTA.md`, Known costs and open items, item 16. |
| R2 | Not changed by row R1: the exactly-once state key `<transformed topic>-<partition>`, the KeeperMap row `(key, minOffset, maxOffset, state)`, table routing, and the BEFORE and AFTER flag sequence. | unchanged: read from the code; pinned by a test: measured | exactlyOnce=true | `git diff v1.7.0_gg6 HEAD` of `Processing.java` is that one line; `KeeperStateProvider.setStateRecord` writes the key, the two offsets and the state, and no original topic. `OriginalTopicInsertedOffsetsUnitTest.stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename` pins the key and the flag order. |

What to do: nothing, unless you enabled `reportInsertedOffsets=true` under a topic-renaming SMT and do not run `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, where Connect's own offsets are returned and nothing changes; then expect offsets under the consumer's topic, and read section D if the rename or the SMT is of another kind.

## D. Limits under topic-changing SMTs

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| R3 | Limits: exactly-once, the in-memory state machine and `reportInsertedOffsets` are supported only for an injective rename and a partition-preserving SMT. A many-to-one rename shares one state record, and the Direct path then reports the shared maximum offset plus one for the first record's topic (a topic at offset 10 was reported at 110). A partition-changing SMT keeps the transformed partition in the state key, and `onPartitionRemoved` does not clear it. Do not enable `reportInsertedOffsets` with them. | the two limit cases: measured; Connect's handling of an offset above the consumer position: derived | many-to-one rename or partition-changing SMT | `OriginalTopicInsertedOffsetsUnitTest.limitManyToOneRenameReportsTheSharedMaximumForTheFirstSourceTopic` and `limitPartitionChangingSmtKeepsTheTransformedPartitionInTheKey`. That Kafka Connect may commit an offset above the consumer's position follows from consumer-group semantics; no broker ran. `FORK-DELTA.md`, item 16. |

What to do: do not enable `reportInsertedOffsets` with a many-to-one rename or a partition-changing SMT, and do not rely on exactly-once or the in-memory state machine with them.

## E. RetriableException passes through as retriable

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| R4 | `Utils.handleException` treats a `RetriableException` that is the exception, or anywhere in its cause chain, as retriable. The rule comes after the server-error rule and before the `DataException` rule, and the walk of the chain stops at the first repeated cause. A `RetriableException` thrown by the writer (the insert timeout with `async=true`, the DDL-propagation timeout, or a `DataException` wrapping one) is therefore retriable under both `errors.tolerance` values. Under `all`, a batch that went to the dead letter queue is now redelivered with no retry budget; under `none`, the task no longer fails on it. | the rule and its ordering, the 30 of 38 red cases: measured; redelivery by Kafka Connect with no retry budget, the writer sites: read from the code | either client | `RetriableExceptionPassThroughUnitTest` and `HandleExceptionCauseCycleUnitTest`, which need no ClickHouse server; 30 of the 38 cases of the first failed on the tree before the change, and all of them pass after it. The cases drive `ProxySinkTask.put` with a failing writer and a recording dead letter queue, not a running Kafka Connect worker, so the redelivery and the missing retry budget are read from the code: `put` raises a `RetriableException` and Kafka Connect redelivers the batch. The writer sites are `ClickHouseWriter.awaitInsertResponse` and `ClickHouseWriter.refreshTableAfterDDL`. `FORK-DELTA.md`, item 17. |
| R5 | A `RetriableException` anywhere in the chain overrides the server-code classification: a `RetriableException` over a non-retriable ClickHouse code, for example 60, is now retriable. | measured | either client | The two cases of `RetriableExceptionPassThroughUnitTest` named for a retriable exception over a non-retriable server code, each under both tolerance values. |
| R6 | Updates row C3 of the `v1.7.0_gg6` note: the two retriable `DataException` behaviours it names are now pinned by `WrappedDataExceptionClassificationPinUnitTest`. A decision on changing either is still open. | measured | either client | `WrappedDataExceptionClassificationPinUnitTest`; `FORK-DELTA.md`, item 17. |

Under `errors.tolerance=all`, a batch whose failure carries a `RetriableException` no longer reaches the dead letter queue: Kafka Connect redelivers it, with no retry budget. Under `errors.tolerance=none`, the task does not fail on it.

What to do: if you rely on the dead letter queue to take such a batch out of the way, expect it to be redelivered instead, and look at the cause of a batch that keeps failing (rows R4 and R5).

## F. A start-up warning for additionalRetriableErrorCodes

When a task starts, the connector logs this WARN once if `additionalRetriableErrorCodes` names a code outside the vetted set; `{}` is replaced by the unvetted codes in ascending order, written as a list in square brackets:

```text
additionalRetriableErrorCodes lists {}, which the connector does not treat as transient. A batch that fails with such a code is redelivered with no retry budget and outside errors.tolerance and the dead letter queue, so a permanent error blocks the partition until the setting is changed.
```

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| R7 | One WARN at task start, with the text above, naming the codes of `additionalRetriableErrorCodes` that are outside the vetted set, which is `Utils.DEFAULT_RETRIABLE_ERROR_CODES` (15 codes) plus 216. It is logged before the connection step, so also when ClickHouse is down. Start-up still succeeds. It carries no record data, and a transient code logs nothing. The connector does not judge whether a listed code is transient beyond that vetted set and does not budget retries; the format check of the setting, a positive integer, still applies. | text and vetted set: read from the code; behaviour: measured | any connector that sets additionalRetriableErrorCodes | `ClickHouseSinkTask.java`: the text is a string constant, `start` calls the check before it builds the proxy task, and the codes are sorted in a `TreeSet`. `UnvettedRetriableCodesWarnUnitTest` and `UnvettedRetriableCodesWarnLiveTest` assert the behaviour, including the sorted list form, for example `[60, 62]`; `FORK-DELTA.md`, item 24. |
| R8 | There is no retry budget for a redelivered batch, and no setting removes a code from the built-in set. | read from the code | any connector that sets additionalRetriableErrorCodes | `Utils.handleException` throws a `RetriableException` for a listed code and Kafka Connect redelivers the batch; `ClickHouseSinkConfig` defines `additionalRetriableErrorCodes` as the only setting of this kind, and `retryCount` only repeats the pings and queries of the helper client. `FORK-DELTA.md`, item 24. |

What to do: list only codes that are transient, such as an overloaded server or a table that is briefly read-only. A permanent code makes the task loop on the same batch.

FUT-05 stays open.

It is the open follow-up for a retry budget, or for a setting that removes a built-in code, and [`FORK-DELTA.md`](FORK-DELTA.md) tracks it under Known costs and open items.

## G. Dependencies, release identity and rollback

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D1 | The shipped archive carries `gson` `2.15.0` where the `v1.7.0_gg6` archive carries `2.14.0`. The archive compare lists 120 differing entries: under `com/google/gson/` 101 changed and 1 present only in the older archive (`com/google/gson/internal/LinkedTreeMap$1.class`), 3 more gson entries under `META-INF`, 14 connector classes and the manifest. Its version table lists 18 libraries and differs for `gson` only (`com.google.code.gson:gson 2.14.0 -> 2.15.0`). | measured | this release | `.github/scripts/compare_confluent_archives.py` run on a local rebuild of tag `v1.7.0_gg6` and a local rebuild of the release tip, with a comment-only allow-list so that every difference is listed; the version is the `gson` entry of `gradle/libs.versions.toml`. |
| D2 | `gradle/actions/setup-gradle` moves to `6.4.1` in the four fork workflows that use it. This concerns CI only and changes nothing in the archive; no vulnerability is claimed closed by it. | read from the code | CI only | The `uses:` lines of `nightly-integration-tests.yaml`, `pre-release.yaml`, `release.yaml` and `tests.yaml` in `.github/workflows`, each pinned to a full commit with the version in a comment. |
| D3 | The release identity, which the JMX `version=` segment and the client name carry, reads `v1.7.0_gg7`. Update matchers that spell out the old value. | identity: read from the code; the identity classes differ in the archive compare: measured | this release | `Version.ARTIFACT_VERSION` is generated from `VERSION` and is used by `ClickHouseHelperClient` (client name) and `SinkTaskStatistics` (MBean name). The archive compare lists `Version` and `SinkTaskStatistics` as changed, and `SinkTaskStatistics` has no source change since `v1.7.0_gg6`, so its difference is the inlined identity. |
| D4 | The archive holds 7933 inner entries for `v1.7.0_gg6` and 7932 for the release tip. Local rebuilds are 13558633 and 13564144 bytes. These are sizes of local rebuilds, not of a release asset. | measured | this release | `./gradlew createConfluentArchive` run on an export of tag `v1.7.0_gg6` and on an export of the release tip; the `verifyConfluentArchive:` line of each run states the inner entry count. |

### Release identity

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both read `v1.7.0_gg7` where they read `v1.7.0_gg6`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg7`, plus `,topic=<topic>` for per-topic beans.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg7`, the prefix of the HTTP user agent.

What to do: change dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg6` or `clickhouse-kafka-connect/v1.7.0_gg6`; matchers that do not spell out the version are unaffected.

### Rollback

The release workflow creates the release and its tag at the merged commit and attaches the archive. Nothing in the data format changes, so a rollback is a code rollback. Tag `pre-v1.7.0_gg7` names commit `72e8ccff`, the published tip of `custom_build` before this release, which is also tag `v1.7.0_gg6`. The tag is published together with this release.

1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing; on the dry run below, the merge of the release tip printed nothing (measured). Set whatever it lists aside with `git stash -u`, keep it on its own branch, or work in a fresh clone, so that none of it becomes part of the rollback; do not commit it on `custom_build`, because step 2 branches from there and step 4 publishes that branch: the `git read-tree` of step 2 discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
2. Restore the tree of the tag in one commit on a new branch, so that your local `custom_build` stays as published. This commit is `<rollback-commit>`:

```sh
git switch -c rollback-v1.7.0_gg7
git read-tree -u --reset pre-v1.7.0_gg7
git commit -m "Roll back v1.7.0_gg7"
git diff --quiet HEAD pre-v1.7.0_gg7 && echo ok
```

```text
ok
```

   After this step `git status --porcelain` printed nothing on the dry run (measured, git 2.50.1; no other git version was tried).

3. Redeploy the `v1.7.0_gg6` archive. If you kept the one you deployed before, use it. A GitHub release asset exists for `v1.7.0_gg6` (13558692 bytes, SHA-256 `e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af`). Otherwise download that asset rather than rebuilding it: a local rebuild of tag `v1.7.0_gg6` is 59 bytes smaller than the asset, and the archive compare lists six connector class files of it as different from the asset's (measured on one local rebuild; for two of the six, `javap` shows a difference in how a string concatenation is compiled; which compiler built the asset was not established).
4. Publish the rollback commit. Where `custom_build` accepts pushes, run `git push origin rollback-v1.7.0_gg7:custom_build`, which does not depend on which branch is checked out. Where it requires a pull request, run `git push origin rollback-v1.7.0_gg7`, open the pull request into `custom_build` and merge it with a merge commit, so the rollback commit keeps its SHA for the forward path below. Never force-push.

Rehearsed on a dry-run merge of the release tip onto `72e8ccff`: the block above, run unchanged, printed `ok`; after the rollback commit the branch has `VERSION` `v1.7.0_gg6`, has no copy of this note, and has a tree equal to tag `pre-v1.7.0_gg7`, and `git revert <rollback-commit>` restores the tree you rolled back from. To go forward again, once the rollback commit is on `origin/custom_build`, update your local branch with `git switch custom_build` and `git pull --ff-only`, run `git revert <rollback-commit>` on it, and publish the revert as in step 4: push it, or put it on a new branch and open a pull request where `custom_build` requires one. The rollback removes this document from the branch, so read it with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg7.md`.

Rolling back brings back:

- the `v1.7.0_gg6` MBean `version=` segment and client name, once the `v1.7.0_gg6` archive is deployed (row D3);
- rows F1 to F4 reversed: a stopped task leaves its helper clients open again (section A);
- the insert bound of rows T2 to T5 reversed: on client V2 with `async=false`, an insert can again wait for ever on a server that stops answering (section B);
- rows R1 and R4 to R7 reversed: the inserted-offset map reads the renamed topic again, a `RetriableException` below the top of a cause chain is no longer retriable, and the start-up WARN is gone (sections C, E and F);
- row D1 reversed: `gson` goes back to `2.14.0`;
- on the branch, the `v1.7.0_gg7` documents are gone, this note included.

The row of this section that states the rollback:

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D5 | Rollback: tag `pre-v1.7.0_gg7` names `72e8ccff`, the published `custom_build` tip before this release, which is also tag `v1.7.0_gg6`. Redeploy the `v1.7.0_gg6` archive. Rolling back reverses rows F1 to F4, the insert bound of rows T2 to T5, rows R1 and R4 to R7, and rows D1 and D3. | rollback block on a dry-run merge: measured; what reverts: read from the code | this release | The block of step 2, extracted and run unchanged on a dry-run merge of the release tip onto `72e8ccff`, printed `ok`, left `VERSION` at `v1.7.0_gg6`, left no copy of this note and a tree equal to the tag, and a revert restored the merged tree. What reverts follows from `git diff v1.7.0_gg6 HEAD -- src/main` and, for `gson`, from row D1. |

## What this release closes from the gg6 note

The `v1.7.0_gg6` note is not edited. The rows of this note supersede the statements named in the first column; every other statement of that note stands.

| gg6 note statement | In this release | Rows |
|--------------------|-----------------|------|
| Known limits, first bullet: `ClickHouseWriter.stop()` never closes its helper client, and the Keeper state provider and every client V1 insert batch leave a client open too. | Closed on both clients for the paths named in section A; the residuals are listed in the last row of that table. | F1 to F5 |
| Known limits, second bullet: the four-minute insert timeout has no effect while `async=false`. | Closed on client V2: the insert now ends after `clickhouseClientInsertTimeoutMs`. The client V1 half stands, and the client V1 library's own read bound is now measured. | T2, T12, T15 |
| Known limits, third bullet: the unbuffered Direct path with `reportInsertedOffsets=true` under a rename commits nothing for the renamed topics (unless `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`), and the call that clears a revoked partition's entry removes nothing for a renamed topic. | Changed for an injective rename, where the Direct path now reports offsets under the consumer's topic and clears them on revocation, except with `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`, where Connect's own offsets are returned as before; the other limits of that bullet remain. | R1, R3 |
| Row C3: two retriable `DataException` behaviours that no test asserts, with a decision on changing them open. | Both are now pinned by a test; the decision on changing them is still open. | R6 |
| The nightly's Datagen completion counting, in the known issues of the `v1.7.0_gg6` section of the changelog. | Fixed in the integration test harness; it is not visible to an operator, so no row states it. | none |

## Known limits carried into this release

These limits exist in this release; none is fixed here. Each bullet points at its rows.

- The closing of helper clients has residuals: a Keeper state provider constructor that fails on client V1, a helper-client constructor that fails after it built its client V2 instance, a second `start()` on one writer, a task-level `stop()` without `try`/`finally` around the delivery strategy's `stop()`, and a five-second wait that can end with a table refresh still running on a closed client (row F5).
- The bounded insert has limits: a write-side stall, a client V1 insert and a request other than a client V2 insert are not bounded by it, an insert that streams progress headers may escape it, and the help text of `clickhouseClientInsertTimeoutMs` still describes a single insert response (rows T6 and T11 to T14).
- A many-to-one rename and a partition-changing SMT are not supported for exactly-once, the in-memory state machine or `reportInsertedOffsets` (row R3).
- A code in `additionalRetriableErrorCodes` is redelivered with no retry budget, and no setting removes a code from the built-in set; the end of section F says where this stays open (rows R7 and R8).
- Carried from the `v1.7.0_gg6` note as information: that note does not state what a first chunk with exactly the stored range does after the upgrade (the `SAME` state of `Processing.doLogic`), and its section B lists configurations it does not characterise. `Processing.doLogic` changed in this release only at the line of row R1, and this note adds no statement about either.
