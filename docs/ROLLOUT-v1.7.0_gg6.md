# Rollout checklist: `v1.7.0_gg5` to `v1.7.0_gg6`

**The connector source changes in this release.** The previous release changed no connector code; this one does. `git diff --shortstat v1.7.0_gg5 HEAD -- src/main` on the release tip reports 7 files changed, 112 insertions and 31 deletions: the buffer strategies and `ChunkFlusher`, `ClickHouseSinkConfig`, `ProxySinkTask`, `Utils` and `ClickHouseHelperClient`. The five changes at a glance:

1. A buffered connector under a topic-changing SMT now commits offsets under the original topic and partition (section B).
2. The ClickHouse Java client moves to `0.11.0`, with one item that breaks an upgrade on client V2: `custom_` keys in `jdbcConnectionProperties` (section D).
3. A new setting, `additionalRetriableErrorCodes` (section C).
4. Dependency changes, among them a security release of `lz4-java` (section E).
5. The release identity, which the JMX name and the client name carry, reads `v1.7.0_gg6` (section E).

The fork's own changes are listed in [`../CHANGELOG.md`](../CHANGELOG.md), and [`FORK-DELTA.md`](FORK-DELTA.md) lists everything the fork patches on top of upstream. If you are coming from a release before `v1.7.0_gg5`, work through [`ROLLOUT-v1.7.0_gg5.md`](ROLLOUT-v1.7.0_gg5.md) first and return here afterwards.

This note names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value, such as `<connector-name>`, `<bootstrap>`, `<group>`, `<topic>` or `<dir>`; replace each one with your own value.

**How each claim is labelled.** Every effect of this release has one row in a table below, with a label for how the claim is known. There are four labels:

- `measured`: a command or a test ran, and the row names it.
- `read from the code`: the row names a file and a site; nothing was run.
- `derived from Kafka consumer-group semantics (not measured)`: the claim follows from how Kafka and Kafka Connect define the behaviour; no broker and no Connect worker ran for it.
- `measured in research (not re-run)`: the maintainers measured it while preparing the release, on throwaway clones and in the public `confluentinc/cp-kafka-connect-base:7.9.7` image, and did not repeat it on the release tip.

A row that makes claims of different strength says which label each claim has. The column `Applies when` reads `either client` (the row holds on client library `0.9.6` and on `0.11.0`; it does not say client V1 or client V2), `client 0.11.0 ships` (the row exists because the client moves to `0.11.0`) or `this release`.

## Before you upgrade

Work through these six steps before you roll the new archive out. Each names the rows it comes from.

1. Search every connector's `jdbcConnectionProperties` for keys that start with `custom_`, for `decompress=true`, and for any expectation that `ssl` or `sslmode` there changes the transport (rows D1, D7, D8). On client V2 (`client_version` set to a value other than `V1`), a `custom_` key makes the task fail after the upgrade; client V1, which a connector runs when it does not set `client_version`, is not affected (row D7).
2. For a connector with `bufferCount > 0` and a topic-changing SMT, describe its consumer group and decide where the first start should resume (rows B3, B4).
3. Check that the Connect runtime has the three `SinkRecord` original-topic accessors (row B6).
4. Check the platform and the temp folder of a worker that may take the ZSTD path (row D9).
5. Review `retryOnSocketException`, proxy rules that match the `User-Agent`, and log alerts that match the old texts (rows D3, D5, D6).
6. Update dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg5` or `clickhouse-kafka-connect/v1.7.0_gg5` (row E3).

A connector that sets `additionalRetriableErrorCodes` lists only transient codes (row C2).

## B. Buffered connectors under topic-renaming SMTs

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| B1 | With `bufferCount > 0` and an SMT that changes the topic (for example `RegexRouter`), the connector now returns offsets under the original topic and partition, at the last flushed offset plus one. Before, `preCommit` returned offsets keyed by the renamed topic; row B2 says what Connect does with such a key. | measured | this release | `BufferedOffsetCommitUnitTest` (Docker-free) and `BufferedOffsetCommitLiveTest` (ClickHouse `latest`, client V2): each value `preCommit` returns equals the end offset of its original partition, and Connect's assignment filter drops nothing. The map `preCommit` returns is observed, not a consumer group on a broker. |
| B2 | Kafka Connect commits a task-provided offset only for a partition the consumer is assigned, so the old keys, which named the renamed topic, committed nothing for the group. | derived from Kafka consumer-group semantics (not measured) | this release | Connect's sink offset contract; how the worker handles a key it does not own was not re-measured here, and no broker ran. |
| B3 | The first start on the new archive resumes at the group's existing committed offset if one exists, otherwise at the position `auto.offset.reset` selects. With `earliest` and no committed offset, the retained topic is reread once, a full duplicate load for a table without deduplication. From the first successful flush on, later restarts and rebalances stop rereading. | derived from Kafka consumer-group semantics (not measured) | this release | Kafka consumer-group semantics; no broker ran. Connect's default for sink consumers is not verified here: check `consumer.auto.offset.reset` and `consumer.override.auto.offset.reset`. |
| B4 | A consumer group that stays empty for longer than the broker's `offsets.retention.minutes` loses even a stale committed offset. | derived from Kafka consumer-group semantics (not measured) | this release | Kafka broker offset-retention semantics; no broker ran. |
| B5 | Not affected: unbuffered connectors, buffered connectors whose topics are not renamed, and the `bufferCount` and `bufferFlushTime` contract. Table routing still follows the renamed topic. A revoked partition drops its buffered records, and the exactly-once buffer groups records by their original partition. | controls: measured; unbuffered path: read from the code | this release | The control tests of `BufferedOffsetCommitUnitTest` and `BufferedOffsetCommitLiveTest`; `DirectDeliveryStrategy.preCommit` returns Connect's own offsets unless `reportInsertedOffsets=true`. |
| B6 | Minimum Connect runtime: the fix calls `SinkRecord.originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset()`. A runtime without them fails with `NoSuchMethodError`: exactly-once on the first `put`, while at-least-once leaves the inserted chunk buffered and redelivers it as a duplicate. | accessors present: measured; failure modes: read from the code | this release | The `javap` check of the `connect-api` jar of the `confluentinc/cp-kafka-connect-base:7.9.7` worker image and the Kafka API list that `ConnectApiReferencesUnitTest` guards; failure modes at `ExactlyOnceBufferStrategy.put` and `ChunkFlusher.flush`, with `AtLeastOnceBufferStrategy.put` catching only `RuntimeException`. |
| B7 | Fork-only: upstream issue 777, which describes the defect, is open, and upstream pull request 786 was closed without being merged. The next sync with upstream must reconcile the fix. | measured | this release | Two read-only GitHub API reads when this note was written: the issue state `open`, and the pull request state `closed` with `merged: false`. They are re-read when the release is published. |
| B8 | Documented limits, not fixed: the Direct path with `reportInsertedOffsets=true` under a rename commits nothing; a many-to-one rename (two source topics into one target) shares one state record and can drop or reject records; exactly-once buffering is claimed only for a rename that maps each source topic to its own target; the state-removal call for a revoked partition uses the original key while the state is held under the renamed topic, so it removes nothing under a rename. | Direct path and many-to-one rename: measured; the other two: read from the code | this release | The Direct-path and many-to-one behaviours were measured by a scratch test probe of the maintainers, which is not in the repository and not something you run; `ProcessingRenamedTopicStateKeyUnitTest` pins the state key (the renamed topic and the partition). Read from the code: `ExactlyOnceBufferStrategy.put` groups by original partition while `Processing` keys state by the renamed topic, and `ClickHouseSinkTask.close` calls `onPartitionRemoved`. |

### What changes

With `bufferCount > 0` and an SMT that changes the record's topic, earlier releases returned offsets keyed by the renamed topic from `preCommit` (row B1). Kafka Connect accepts an offset only for a partition the consumer was assigned, so nothing was committed for the group (row B2). This release returns the original topic and partition at the last flushed offset plus one, so after a restart or a rebalance the group resumes from the last flushed offset instead of rereading from where `auto.offset.reset` points.

The edit is small and sits in three buffer files (`ChunkFlusher`, `AtLeastOnceBufferStrategy`, `ExactlyOnceBufferStrategy`). `Processing.java`, the state key, the KeeperMap row schema and the `BEFORE` and `AFTER` flag sequence are untouched (read from the code: the diff of those files since `v1.7.0_gg5` is empty). The fix is fork-only (row B7).

Affected: connectors with `bufferCount > 0`, with at-least-once or exactly-once buffering, and an SMT that changes the topic. Not affected: unbuffered connectors, buffered connectors without renamed topics, and the `bufferCount` and `bufferFlushTime` contract (row B5).

### First start after the upgrade

Every claim in this subsection carries the label `derived from Kafka consumer-group semantics (not measured)` in its row (B3, B4); no broker and no Connect worker ran for it.

The previous archive never advanced the group's committed offset for these partitions. The first start on the new archive therefore resumes exactly where a restart of the previous archive would have: at the group's existing committed offset if one exists (for example one left from a period before `bufferCount` was turned on), otherwise at the position `auto.offset.reset` selects.

Connect's default `auto.offset.reset` for sink consumers is not verified here, so check the worker's `consumer.auto.offset.reset` and any connector-level `consumer.override.auto.offset.reset` before relying on it. With `earliest` and no committed offset, the connector rereads the retained topic from its beginning once; the upgrade restarts the connector, so that one reread coincides with the upgrade. From the first successful flush on, the group's offsets advance and later restarts and rebalances no longer reread.

A group that stays without commits can lose even a stale committed offset once it has been empty for longer than the broker's `offsets.retention.minutes`; check that setting on your brokers (row B4).

### What to do

1. Before the upgrade, run a read-only check against your own cluster. Describe the group; `<group>` is `connect-<connector-name>` unless the connector overrides the group id:

   ```sh
   kafka-consumer-groups --bootstrap-server <bootstrap> --describe --group <group>
   ```

   The output shows whether committed offsets exist and where they are.

2. Decide where the first start should resume. Either accept one reread, when the table deduplicates or the load is idempotent, or stop the connector and set the group's offsets to the position you want before starting the new archive, with `kafka-consumer-groups --bootstrap-server <bootstrap> --group <group> --topic <topic> --reset-offsets <position option> --execute`. The offset reset changes live state: try it first on a group that no live pipeline uses, and verify the options against your Kafka version.
3. After the first successful flush, run the describe command again and confirm that the committed offsets advance.

### Minimum Connect runtime

The fix calls `SinkRecord.originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset()` (row B6). All three are present in the `connect-api` jar of the `confluentinc/cp-kafka-connect-base:7.9.7` worker image, and the connector's Kafka API list is guarded by `ConnectApiReferencesUnitTest`. A Connect runtime without them fails with `NoSuchMethodError`, and the two failure modes are read from the code:

- Exactly-once: `ExactlyOnceBufferStrategy.put` calls `originalTopic()` before any insert, so the first `put` fails.
- At-least-once: `ChunkFlusher.flush` inserts the chunk first and reaches the new accessors only afterwards. `NoSuchMethodError` is an `Error`, and `AtLeastOnceBufferStrategy.put` catches only `RuntimeException`, so the inserted chunk stays buffered with no offset recorded and is redelivered as a duplicate.

Only runtimes below the supported floor are affected; nothing here ran an older runtime.

## C. additionalRetriableErrorCodes

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| C1 | New setting `additionalRetriableErrorCodes`, default empty. Parsing trims each entry, skips empty entries, removes duplicates, and rejects a value that is not an integer or is zero or below. An unset or empty value changes nothing. The built-in set holds 15 codes, including 159. | measured | this release | `AdditionalRetriableErrorCodesPinUnitTest` (Docker-free); the constant `ADDITIONAL_RETRIABLE_ERROR_CODES` and its validator in `ClickHouseSinkConfig`; the built-in set `DEFAULT_RETRIABLE_ERROR_CODES` in `Utils`. |
| C2 | A listed code that the server returns raises `RetriableException`. Kafka Connect redelivers the batch with no retry budget, outside `errors.tolerance` and the dead letter queue, so only transient codes belong in the list. | read from the code | this release | `Utils.handleException` checks the root cause against the built-in and the added codes before it reaches the `errors.tolerance` branch; the setting's own description in `ClickHouseSinkConfig` says the same. |
| C3 | Two behaviours of `handleException` are retriable today, and no test asserts them: a `DataException` whose cause chain holds a ClickHouse server error with a retriable code (a built-in or an added one), and a `DataException` whose root cause is a socket timeout or an unknown host under `errors.tolerance=all`. A decision on changing them is open. | read from the code | this release | `Utils.handleException`, which classifies the root cause before it looks at the `DataException`. |

### What the setting does

`additionalRetriableErrorCodes` takes a comma-separated list of ClickHouse server error codes, for example `additionalRetriableErrorCodes=<code>,<code>`. It defaults to empty; with it unset the connector behaves as before. The parse rules are those of row C1. The built-in set of 15 codes, 159 (`TIMEOUT_EXCEEDED`) among them, stays in force, and the setting adds to it; it does not change the ClickHouse Java client's own retry settings.

### The bypass of errors.tolerance

A code in the list makes the connector throw `RetriableException`, and Kafka Connect then redelivers the same batch. That redelivery has no retry budget and does not go through `errors.tolerance` or the dead letter queue (row C2). A code that is not transient therefore makes the task loop on the same batch: list only codes that are transient, such as an overloaded server or a table that is briefly read-only.

### Two retriable behaviours that no test asserts

Row C3 names two cases that are retriable today although no test asserts them. A decision on changing either is open; this release leaves both as they are.

## D. ClickHouse Java client 0.11.0

The connector's ClickHouse Java client moves from `0.9.6` to `0.11.0`. The connector keeps working on the new client, and one effect breaks an upgrade for a connector on client V2 that sets `custom_` keys in `jdbcConnectionProperties` (row D7). A row says in `Applies when` whether it holds on both client lines or only because the client moves.

Which client a connector runs decides which rows describe it. A connector runs client V2 only when its configuration sets `client_version` to a value other than `V1`; without `client_version` it runs client V1 (read from the code: `ClickHouseSinkConfig` and `ClickHouseWriter.start`). Rows D1 to D8, D10 and D11 were measured or read for client V2 and are not a statement about client V1. Rows D9 and D12 concern the archive that both clients ship in. Rows D13 and D14 also hold on client V1. What the move to `0.11.0` changes in the V1 library artifacts (`clickhouse-client`, `clickhouse-http-client` and `clickhouse-data`, which share the version with the V2 client) was not audited beyond rows D13 and D14 and one V1-mode run of the live tests in the maintainers' research (35 tests, no failure; `measured in research (not re-run)`).

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| D1 | `ssl` and `sslmode` in `jdbcConnectionProperties` never changed V2's transport on either client line: V2 TLS follows the `https` scheme that the connector's `ssl` flag selects. Client 0.9.6 only warned about them. Client 0.11.0 fails at start with `Unknown and unmapped config properties: [ssl, sslmode]` unless unknown keys are ignored; the connector now ignores them, and logs a WARN that names both. | measured | either client | `ClientEndpointAndOptionsPinUnitTest` (Docker-free) and `Client011BehaviourPinLiveTest` (ClickHouse `latest`, client V2); the option is set in `ClickHouseHelperClient` (`IGNORE_UNKNOWN_CONFIG_KEY`). |
| D2 | Error 159 (`TIMEOUT_EXCEEDED`) is still a `RetriableException`, but the client no longer retries it itself: 4 attempts on 0.9.6, 1 on 0.11.0. Against a 1 second `max_execution_time` the failure took about 4025 ms on 0.9.6 and about 1006 ms on 0.11.0. | attempts: measured; cadence: measured in research (not re-run) | client 0.11.0 ships | Attempts: `ClientErrorShapePinUnitTest` and `Client011BehaviourPinLiveTest`. Cadence: measured by the maintainers on throwaway clones of both client lines against a real server and not repeated on the release tip. |
| D3 | A 502 or 503 from a proxy. On 0.9.6 a 502 is a `ClientException` and a 503 a code-0 `ServerException`, one attempt, never retriable. On 0.11.0 both are a `java.net.ConnectException` after 4 attempts, retriable only with `retryOnSocketException=true`. The default is `false`, so the task fails as before. | measured | client 0.11.0 ships | `ClientErrorShapePinUnitTest` and `Client011BehaviourPinLiveTest`; the default of `retryOnSocketException` is in `ClickHouseSinkConfig`. |
| D4 | An unexpected HTTP status such as 418: a code-0 `ServerException` on 0.9.6, a `ClientException` with `Unexpected result status 418` on 0.11.0. Non-retriable on both. | measured | client 0.11.0 ships | `ClientErrorShapePinUnitTest`. |
| D5 | The `User-Agent` changes. `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.9.6 (<os>; jvm:<jvm>) Apache-HttpClient/<version>` becomes `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.11.0 (lv:java/<jvm>; os:<os>; hc:ahc_<version>)`. Check proxy and firewall rules that match it. | prefix: measured; tail: read from the code | client 0.11.0 ships | Prefix: `ClientInsertWirePinUnitTest`. Tail: the public `client-v2` 0.11.0 sources (`Client.CLIENT_USER_AGENT`, and `WorkloadIdentitySupport`, which adds `lv:java/`, `os:` and `hc:ahc_`) and the 0.9.6 sources (`HttpAPIClientHelper`, which appends `jvm:` and `Apache-HttpClient`); no request was captured for the tail. |
| D6 | Log text changes. `Insert failed (Attempt: 4/4 - Duration: PT0.02S)` becomes `Insert request failed (attempt: 4, duration: 9ms, queryId: <id>)`. Server messages gain ` (queryId= <id>)`. The text of a non-ClickHouse HTTP error changes too, to `<body> (transport error: 500)` for a 500. Update log alerts that match these strings. | insert and queryId texts: read from the code; non-ClickHouse HTTP error text: measured in research (not re-run) | client 0.11.0 ships | Insert and queryId texts: the public sources, `Client.requestExMsg` and `HttpAPIClientHelper` of 0.11.0 against `Client` of 0.9.6. HTTP error text: measured by the maintainers against a stub server and not repeated here; no test pins any of these texts. |
| D7 | `jdbcConnectionProperties` keys that start with `custom_` (for example `?custom_settings=...`) are forwarded to the server on V2 requests by 0.11.0, and the operator's percent-encoding is doubled. The server rejects each forwarded key with `Code: 115 ... UNKNOWN_SETTING`, and code 115 is not in the connector's retriable set, so the task fails. Client 0.9.6 ignored these keys. On client V1, `custom_settings` is the client's own option and keeps working. **This breaks an upgrade on client V2 (`client_version` set to a value other than `V1`): remove or rename such keys before moving.** | forwarding: measured; rejection: measured; task failure: read from the code; `custom_settings` on V1: measured | client 0.11.0 ships | Forwarding: a stub-server probe of the maintainers on both client lines (request lines below). Rejection: one `SELECT 1` request with a `custom_myparam=7` parameter sent over HTTP to a throwaway ClickHouse 26.10.3.7 container (answer below); it was not sent through the connector. Task failure: `Utils.DEFAULT_RETRIABLE_ERROR_CODES` holds no 115. `custom_settings` on V1: `NetworkCompressionPinLiveTest` (ClickHouse `latest`) reads `custom_settings=network_compression_method%3D...,max_threads%3D3` into the server's settings through client V1; it was re-run on the release tip and its 9 tests passed. Which client a connector runs (read from the code): `ClickHouseSinkConfig` reads `client_version` with the default `V1`, and `ClickHouseWriter.start` selects client V2 for any other value. |
| D8 | `decompress=true` (request compression) in `jdbcConnectionProperties`: LZ4 blocks on 0.9.6, ZSTD blocks on 0.11.0, where `compression.method` defaults to `ZSTD`, and a 1,000,000-byte incompressible body fails client-side with `ZstdException: Destination buffer is too small`. The connector never sets `decompress`, so this is operator-set only. | default method: read from the code; client-side failure: measured in research (not re-run) | client 0.11.0 ships | Default method: `ClientConfigProperties.COMPRESSION_METHOD` in the public 0.11.0 sources; the 0.9.6 request path is `LZ4Entity`. Client-side failure: a stub probe of the maintainers, not repeated on the release tip. |
| D9 | `zstd-jni` 1.5.7-23 (classifier `cloud`) is new in the archive, with natives for linux/amd64, linux/aarch64 and darwin/aarch64 only. On any other platform a ZSTD path cannot load the native and fails with `UnsatisfiedLinkError: Unsupported OS/arch`. With `/tmp` mounted `noexec` it fails with `failed to map segment from shared object` unless `-DZstdTempFolder=<dir>` or `-Djava.io.tmpdir=<dir>` names an executable folder. The default configuration never loads the native. | archive and natives: measured; worker behaviour: measured in research (not re-run); loader behaviour: read from the code | client 0.11.0 ships | Archive and natives: `compare_confluent_archives.py` on the archives built from tag `v1.7.0_gg5` and the release tip lists exactly the three natives as new. Worker behaviour (the `noexec` failure, the default configuration never loading the native): the maintainers' probe in the `confluentinc/cp-kafka-connect-base:7.9.7` image on linux/aarch64. Loader behaviour: `Native.java` of zstd-jni 1.5.7-23. |
| D10 | The client no longer sets 804800-byte socket buffers. Without the option the socket keeps the operating system's default buffer size, and `socket_rcvbuf` and `socket_sndbuf` are still accepted through `jdbcConnectionProperties`. The throughput effect was not measured. | read from the code | client 0.11.0 ships | `ClientConfigProperties` and `HttpAPIClientHelper` of the public sources of both client lines: 0.9.6 declares both keys with the default 804800, 0.11.0 declares them with no default and applies them only when set. |
| D11 | A batch replayed after the upgrade (an exactly-once `BEFORE_PROCESSING` retry) is byte-identical on the wire to the pre-upgrade attempt for the 48-case type matrix, plain and with defaults. It carries the same `insert_deduplication_token` and differs only in the `User-Agent` header. No clean stop is required for that matrix. A column type outside the matrix was not compared. | golden: measured; token: read from the code | client 0.11.0 ships | Golden: `RowBinaryClientParityLiveTest` against `rowbinary-golden-sha256.txt` (96 bodies, blessed on client 0.9.6, green on 0.11.0) and `ClientInsertWirePinUnitTest` for the body, the token and the headers. Token: `QueryIdentifier.getDeduplicationToken` builds it from the batch, not from the client. |
| D12 | The archive grows. `verifyConfluentArchive` counts 7801 inner entries for the `v1.7.0_gg5` archive and 7933 for the release tip (+132); the archive compare counts 7399 and 7513 files, 4 only in the old archive, 118 only in the new one and 120 changed; the zip grows from 12453556 to 13558634 bytes. The guava override is redundant on 0.11.0, and the httpclient5 and lz4 pins still win. | measured | client 0.11.0 ships | `./gradlew createConfluentArchive` on an export of tag `v1.7.0_gg5` and of the release tip, and `.github/scripts/compare_confluent_archives.py` on the two archives; its version table keeps guava 33.7.2-jre and httpclient5 5.6.5 on both sides. |
| D13 | The default stays: `setCompressionMethod` true with `compress=false`. V2 of client 0.11.0 decodes ZSTD with `setCompressionMethod=false` and `compress=true`, while client V1 cannot decode ZSTD on either client line, so the default keeps V1 readable and keeps the zstd native unloaded. The V1 half holds on either client; the V2 decode statement holds only on 0.11.0. | measured | either client | `NetworkCompressionPinUnitTest` (Docker-free), `NetworkCompressionPinLiveTest` and `Client011BehaviourPinLiveTest` (ClickHouse `latest`). |
| D14 | The `Read timed out after` and `Write timed out after` IOException prefixes come from the client's own piped streams in `clickhouse-data`, are unchanged between the two lines, and are still retriable. | measured | either client | `ClientTimeoutPrefixPinUnitTest`. |

### `custom_` keys in `jdbcConnectionProperties` (this breaks an upgrade on client V2)

Client 0.11.0 forwards every `jdbcConnectionProperties` key that starts with `custom_` to the server as a query parameter on V2 requests, and it percent-encodes the operator's value a second time. Client 0.9.6 ignored these keys. A stub-server probe of the maintainers builds a V2 client with `?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true` and prints the request line of the insert. Measured on both client lines (the operator's `%3D` becomes `%253D` on 0.11.0):

```text
0.11.0: /?custom_myparam=7&custom_settings=max_threads%253D3&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4
0.9.6:  /?query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4
```

A real server rejects the forwarded parameter. Measured by sending `SELECT 1` with a `custom_myparam=7` parameter over HTTP to a throwaway ClickHouse 26.10.3.7 container (the request did not go through the connector); the answer was:

```text
Code: 115. DB::Exception: Setting custom_myparam is neither a builtin setting nor started with the prefix 'SQL_' registered for user-defined settings. (UNKNOWN_SETTING) (version 26.10.3.7 (official build))
```

The connector does not retry code 115 (read from the code: it is not in `Utils.DEFAULT_RETRIABLE_ERROR_CODES`), so with `errors.tolerance=none` the task fails; with `errors.tolerance=all` the failed records are handed to the dead letter queue reporter (where one is configured) instead of failing the task, so the symptom is rows that never reach the table, which is easy to miss (read from the code: `ProxySinkTask.put` and `Utils.handleException`).

What to search for: `custom_` in every `jdbcConnectionProperties` value of every connector, before moving. A connector runs client V2 only when its configuration sets `client_version` to a value other than `V1`; without `client_version` it runs client V1 (read from the code: `ClickHouseSinkConfig` and `ClickHouseWriter.start`). On client V1, `custom_settings` is the client's own option and keeps working, so keep it there; the search also finds V1 connectors, which need no change for this item. On client V2 the remedy is to remove or rename the key. The connector has no switch that restores the old behaviour in this release.

### zstd-jni: platforms and the temp folder

Each fact carries the label of row D9.

- The `cloud` jar that client 0.11.0 declares carries three natives, `darwin/aarch64`, `linux/aarch64` and `linux/amd64` (measured: the archive compare lists exactly those three as new).
- The plain `zstd-jni` jar carries more platforms, among them linux arm, i386, loongarch64, mips64, ppc64, ppc64le, riscv64 and s390x, windows aarch64, amd64 and x86, freebsd, aix and darwin x86_64 (Intel macOS); the `cloud` jar carries none of them (measured in research, not re-run).
- On an unsupported platform the load throws `UnsatisfiedLinkError: Unsupported OS/arch, cannot find /<os>/<arch>/libzstd-jni-1.5.7-23.<ext>`. The library has no pure-Java fallback (read from the code).
- With `/tmp` mounted `noexec` the failure reads `failed to map segment from shared object`. Two remedies work: `-DZstdTempFolder=<dir>` or `-Djava.io.tmpdir=<dir>`, with `<dir>` an executable folder (measured in research, not re-run).
- `ZstdNativePath` is a third system property. It loads the given file and has no fallback (read from the code).
- An `Error`, not an exception, escapes the failing load. `Utils.getRootCause` returns null for it, so the task fails without a retry (read from the code).
- The default configuration never reaches the ZSTD path: the connector's `setCompressionMethod` true with `compress=false`, or with `compress=true`, mapped no zstd native in the maintainers' probe (measured in research, not re-run). A connector reaches it only with `compress=true` together with `setCompressionMethod=false`, or with `decompress=true`.
- Alpine and other musl workers were not tested.

### A batch replayed across the upgrade (the clean-stop requirement)

A `BEFORE_PROCESSING` batch that is replayed after the upgrade is rebuilt by the new task from the stored offset range. The same writer serialises it into the same bytes, and the same path sends it with the same body and the same `insert_deduplication_token`, so ClickHouse's block deduplication sees the block the old task would have sent. The `User-Agent` header is the only request header that differs (row D11).

- The golden (measured): 96 RowBinary bodies, 48 type cases in two variants, blessed on client 0.9.6 and green on 0.11.0.
- The wire pin (measured): `ClientInsertWirePinUnitTest` shows a 100,000-byte body, the token and no `Content-Encoding` arriving unchanged on 0.11.0.
- The token (read from the code): `QueryIdentifier.getDeduplicationToken` builds `topic-partition-minOffset-maxOffset` from the batch, not from the client.

No clean stop is required, for the compared matrix only. The scope is the 48 type cases in both variants: a column type outside the matrix, a time-zoned `DateTime` and a `Variant` fed plain text were not compared.

### What to do for the client

1. Search your `jdbcConnectionProperties` for `custom_` keys, for `decompress=true`, and for any expectation that `ssl` or `sslmode` there changes the transport (rows D1, D7, D8).
2. If you set `compress=true` together with `setCompressionMethod=false`, check the worker's platform and its temp folder first (row D9).
3. Review `retryOnSocketException` and your log alerts against rows D3 and D6, and proxy rules against row D5.

## E. Dependencies, release identity and rollback

| ID | Effect | Label | Applies when | How it is known |
|----|--------|-------|--------------|-----------------|
| E1 | `lz4-java` moves from `1.12.0` to `1.12.1`, a security release (`GHSA-48mj-8f7c-7qfm`), and the shipped archive carries `1.12.1`. | measured | this release | The version table of `.github/scripts/compare_confluent_archives.py` on the archives built from tag `v1.7.0_gg5` and from the release tip reads `at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1`; the catalog entry is `lz4Java` in `gradle/libs.versions.toml`. |
| E2 | `okhttp` moves to 5.5.1 (integration tests only), Gradle to 9.8.2 (build only) and `setup-java` to 6.1.0 (CI only). `protobuf-java` stays on 3.x. None of them changes the archive's behaviour. | read from the code | this release | `gradle/libs.versions.toml` (`okhttp`, `libprotobuf`), `gradle/wrapper/gradle-wrapper.properties` and the pinned action in `.github/workflows/pre-release.yaml`. |
| E3 | The JMX `version=` segment and the client name read `v1.7.0_gg6`. Update matchers that spell out the old value. | release identity: read from the code; both classes differ in the archive compare: measured | this release | `Version.ARTIFACT_VERSION` is generated from `VERSION` and is used by `ClickHouseHelperClient` (client name) and `SinkTaskStatistics` (MBean name); `MBeanNameUnitTest` pins the MBean shape. The archive compare lists `Version`, `ClickHouseHelperClient` and `SinkTaskStatistics` as changed. |
| E4 | Rollback: tag `pre-v1.7.0_gg6` is commit `3a3a15a9`, the same commit as tag `v1.7.0_gg5`. Redeploy the `v1.7.0_gg5` archive. Rolling back reverses row B1 and rows D1 to D10. | rebuild and compare: measured; effects of rolling back: read from the code | this release | `git rev-parse` of both tags; a rebuild of tag `v1.7.0_gg5` has the size of the published `v1.7.0_gg5` asset and compares equal to it entry by entry with `.github/scripts/compare_confluent_archives.py`; the effects of rolling back follow from `git diff v1.7.0_gg5 -- src/main`. |

### Release identity

The MBean name embeds the release version, and so does the client name that the connector sends on every request. Both read `v1.7.0_gg6` where they read `v1.7.0_gg5`:

- MBean: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<connector-name>,name=SinkTask<id>,version=v1.7.0_gg6`, plus `,topic=<topic>` for per-topic beans.
- Client name: `clickhouse-kafka-connect/v1.7.0_gg6`, the prefix of the HTTP user agent. The rest of the user agent changes with client 0.11.0 (row D5).

To list the client names that your server has seen, run this read-only query on it:

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

What to do: change dashboard, alert and exporter matchers that spell out `version=v1.7.0_gg5` or `clickhouse-kafka-connect/v1.7.0_gg5`; matchers that do not spell out the version are unaffected.

### Rollback

The release workflow creates the release and its tag at the merged commit and attaches the archive. Nothing in the data format changes, so a rollback is a code rollback. Tag `pre-v1.7.0_gg6` names commit `3a3a15a9`, the published tip of `custom_build` before this release, which is also tag `v1.7.0_gg5`. The tag is published together with this release.

1. Start from the published tip with nothing uncommitted. Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that `git status --porcelain` prints nothing; a fresh checkout of the release tip lists nothing (measured). Set whatever it lists aside with `git stash -u`, keep it on its own branch, or work in a fresh clone, so that none of it becomes part of the rollback; do not commit it on `custom_build`, because step 2 branches from there and step 4 publishes that branch: the `git read-tree` of step 2 discards uncommitted and staged changes to tracked files without asking, and it overwrites untracked files at paths that the tag contains.
2. Restore the tree of the tag in one commit on a new branch, so that your local `custom_build` stays as published. This commit is `<rollback-commit>`:

```sh
git switch -c rollback-v1.7.0_gg6
git read-tree -u --reset pre-v1.7.0_gg6
git commit -m "Roll back v1.7.0_gg6"
git diff --quiet HEAD pre-v1.7.0_gg6 && echo ok
```

```text
ok
```

   After this step `git status` lists ` M gradlew.bat` without any change of yours: the tag's `gradlew.bat` has Windows line endings, and `.gitattributes` asks git to normalise `*.bat` files. While that line shows, `git switch` and `git revert` stop with "local changes would be overwritten". Run each of them with git's attributes switched off, as in `git --attr-source="$(git hash-object -t tree /dev/null)" revert <rollback-commit>` (git 2.40 or later); the forward path below does so. Measured on the dry run.

3. Redeploy the `v1.7.0_gg5` archive. If you kept the one you deployed before, use it. A GitHub release asset exists for `v1.7.0_gg5` (published 2026-10-28, SHA-256 `4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767`). Otherwise check out tag `v1.7.0_gg5` in a clean worktree and run `./gradlew createConfluentArchive`. A rebuild from the tag is not byte-identical to that asset, although it has the asset's size; it compares equal to it entry by entry with `.github/scripts/compare_confluent_archives.py` (measured by the maintainers' rebuild). The `v1.7.0_gg5` archive carries client `0.9.6` and `lz4-java` `1.12.0`.
4. Publish the rollback commit. Where `custom_build` accepts pushes, run `git push origin rollback-v1.7.0_gg6:custom_build`, which does not depend on which branch is checked out. Where it requires a pull request, run `git push origin rollback-v1.7.0_gg6`, open the pull request into `custom_build` and merge it with a merge commit, so the rollback commit keeps its SHA for the forward path below. Until the rollback commit is on `origin/custom_build`, GitHub keeps running the `v1.7.0_gg6` workflows there.

Rehearsed on a dry-run merge of the release tip onto `3a3a15a9`: the block above, run unchanged, printed `ok`; after the rollback commit the branch has `VERSION` `v1.7.0_gg5`, has no copy of this note, and has a tree equal to tag `v1.7.0_gg5`, and `git revert <rollback-commit>`, run with git's attributes switched off as in step 2, restores the tree you rolled back from. To go forward again, once the rollback commit is on `origin/custom_build`, update your local branch with `git --attr-source="$(git hash-object -t tree /dev/null)" switch custom_build` and `git pull --ff-only`, run `git --attr-source="$(git hash-object -t tree /dev/null)" revert <rollback-commit>` on it, and publish the revert as in step 4: push it, or put it on a new branch and open a pull request where `custom_build` requires one. The rollback removes this document from the branch, so read it with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg6.md`.

Rolling back brings back:

- the `v1.7.0_gg5` MBean `version=` segment and client name, once the `v1.7.0_gg5` archive is deployed (row E3);
- row B1 reversed: with a topic-changing SMT and `bufferCount > 0`, offsets are again keyed by the renamed topic (section B);
- rows D1 to D10 reversed: client `0.9.6`, with its retry cadence, error shapes, `User-Agent`, log texts, socket buffers and the quiet handling of `custom_` keys (section D);
- the `additionalRetriableErrorCodes` setting is not known to the older connector, so a configuration that sets it has no effect (section C);
- on the branch, the `v1.7.0_gg6` documents are gone, this note included.

## Known limits carried into this release

These limits exist in this release as they did before it; none is fixed here.

- `ClickHouseWriter.stop()` never closes its `ClickHouseHelperClient`, so every task start leaves one V2 client and its connection pool open for the life of the worker process. It matters on workers that restart or rebalance tasks often (observed on both client lines; source: `ClickHouseWriter.stop`).
- The four-minute insert timeout (`clickhouseClientInsertTimeoutMs`, applied in `awaitInsertResponse`) has no effect while `async=false`, because the insert returns an already completed future, and the client's `socket_timeout` defaults to 0, so an insert can block on a stalled server (observed on both client lines; source: `awaitInsertResponse` and the client timeout options).
- The documented limits of buffered connectors under a rename (row B8): the Direct path with `reportInsertedOffsets=true` commits nothing under a rename, a many-to-one rename shares one state record, exactly-once buffering is claimed only for a rename that maps each source topic to its own target, and the state-removal call for a revoked partition removes nothing under a rename.
- The two retriable `DataException` behaviours of row C3 are not asserted by any test, and a decision on changing them is open.
