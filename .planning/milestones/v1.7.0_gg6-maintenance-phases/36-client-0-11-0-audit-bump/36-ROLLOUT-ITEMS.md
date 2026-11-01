# Rollout items for Phase 38: ClickHouse Java client 0.11.0

This file is the record that Phase 36 hands to Phase 38 (RLS-01 and RLS-02). Phase 38 places it in `docs/ROLLOUT-v1.7.0_gg6.md`. It is not the release note: `docs/ROLLOUT-v1.7.0_gg6.md` does not exist yet, and this phase edits nothing under `docs/` for it.

It names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket value such as `<connector-name>` or `<dir>`. Where a claim rests on a command, a test or a measurement, the claim names it and says how it is known.

There are three evidence labels.

- `measured`: a command, a test or an evidence entry of `36-COMMAND-EVIDENCE.md`, named in the Evidence column.
- `read from the code`: a file and a site, named in the Evidence column. Nothing was run.
- `measured in research (not re-run)`: a section of `36-RESEARCH.md`, named in the Evidence column. The measurement was taken during planning on throwaway clones and in the public `confluentinc/cp-kafka-connect-base:7.9.7` image; Phase 36 did not repeat it.

Each item has an Applies-when value. `either client` means the item holds on client 0.9.6 and on client 0.11.0. `client 0.11.0 ships` means the item exists only because the client moves to 0.11.0. If the CLT-09 decision reverts the bump and the connector stays on 0.9.6, Phase 38 keeps only the `either client` rows.

## Items

| # | Effect | Label | Applies when | Evidence |
|---|--------|-------|--------------|----------|
| 1 | `ssl` and `sslmode` in `jdbcConnectionProperties` never changed V2's transport on either client line: V2 TLS follows the `https` scheme that the connector's `ssl` flag selects. Client 0.9.6 only warned about them. Client 0.11.0 fails at start without the fix with `Unknown and unmapped config properties: [ssl, sslmode]`. With the fix both are ignored with a WARN that names them. | measured | either client | E-03 (the bare 0.11.0 red set), E-05 (the fix, a no-op on 0.9.6), E-06 (green on 0.11.0) |
| 2 | Error 159 (`TIMEOUT_EXCEEDED`) is still a `RetriableException`, but the client no longer retries it itself: 4 attempts on 0.9.6, 1 on 0.11.0. Against a 1 second `max_execution_time` the failure took about 4025 ms on 0.9.6 and 1006 ms on 0.11.0. | attempts: measured; cadence: measured in research (not re-run) | client 0.11.0 ships | attempts: E-03 and E-06 (`ClientErrorShapePinUnitTest`, `Client011BehaviourPinLiveTest`); cadence: `36-RESEARCH.md` section "Measured error shapes" |
| 3 | A 502 or 503 from a proxy. On 0.9.6 a 502 is a `ClientException` and a 503 a code-0 `ServerException`, one attempt, never retriable. On 0.11.0 both are a `java.net.ConnectException` after 4 attempts and are retriable only with `retryOnSocketException=true`. The default is `false`, so the task fails as before. | measured | client 0.11.0 ships | E-03, E-06 (`ClientErrorShapePinUnitTest`, `Client011BehaviourPinLiveTest`) |
| 4 | An unexpected HTTP status such as 418: a code-0 `ServerException` on 0.9.6, a `ClientException` with `Unexpected result status 418` on 0.11.0. Non-retriable on both. | measured | client 0.11.0 ships | E-03, E-06 (`ClientErrorShapePinUnitTest`) |
| 5 | The User-Agent changes. `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.9.6 (<os>; jvm:<jvm>) Apache-HttpClient/<version>` becomes `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.11.0 (lv:java/<jvm>; os:<os>; hc:ahc_<version>)`. Check proxy or firewall rules that match it. | the connector-name-and-client-version prefix: measured; the tail text: measured in research (not re-run) | client 0.11.0 ships | prefix: `ClientInsertWirePinUnitTest` in E-06; tail: `36-RESEARCH.md` section "Operator-visible effects" |
| 6 | Log text changes. `Insert failed (Attempt: 4/4 - Duration: PT0.02S)` becomes `Insert request failed (attempt: 4, duration: 9ms, queryId: <id>)`. Server messages gain ` (queryId= <id>)`. A non-ClickHouse HTTP error reads `<body> (transport error: 500)` instead of `Code: 0. DB::Exception: <Unreadable error message> (transport error: 500)`. Update log alerts that match these strings. | measured in research (not re-run); not pinned | client 0.11.0 ships | `36-RESEARCH.md` section "Operator-visible effects", rows 5 and 6 of its table |
| 7 | `jdbcConnectionProperties` keys that start with `custom_` (for example `?custom_settings=...`) are forwarded to the server on V2 requests by 0.11.0, and the operator's percent-encoding is doubled. The server rejects them with `Code: 115 ... UNKNOWN_SETTING`, which is not retriable, so the task fails. Client 0.9.6 ignored them. Remove or rename such keys before moving. | forwarding: measured; the server's rejection: measured in research (not re-run) | client 0.11.0 ships | forwarding: E-11 stub probe on both client lines; rejection: `36-RESEARCH.md` section "Operator-visible effects", row 7 |
| 8 | `decompress=true` (request compression) in `jdbcConnectionProperties`: LZ4 blocks on 0.9.6, ZSTD blocks on 0.11.0 (`compression.method` defaults to `ZSTD`), and a 1,000,000-byte incompressible body fails client-side with `ZstdException: Destination buffer is too small`. The connector never sets `decompress`, so this is operator-set only. | measured in research (not re-run) | client 0.11.0 ships | `36-RESEARCH.md` section "Operator-visible effects", row 8 of its table (a stub probe) |
| 9 | `zstd-jni` 1.5.7-23 (classifier `cloud`) is new in the archive, with natives for linux/amd64, linux/aarch64 and darwin/aarch64 only (the plain jar carries 18 platforms). On any other platform a ZSTD path cannot load the native. With `/tmp` mounted `noexec` it fails with `failed to map segment from shared object` unless `-DZstdTempFolder=<dir>` or `-Djava.io.tmpdir=<dir>` names an executable folder. The default configuration (the lz4 setting on, `compress=false`) never loads the native, even when an operator sets `compress=true`. | archive and natives: measured; worker behaviour: measured in research (not re-run) | client 0.11.0 ships | archive and natives: E-09; worker behaviour: `36-RESEARCH.md` section "CLT-08", the zstd-jni in a Connect plugin classloader table, probed in the `confluentinc/cp-kafka-connect-base:7.9.7` image on linux/aarch64; Alpine and other musl workers were not tested |
| 10 | The client no longer sets 804800-byte socket buffers (`socket_rcvbuf`, `socket_sndbuf`). The operating system auto-tunes, and both keys are still accepted through `jdbcConnectionProperties`. The throughput effect was not measured. | read from the code | client 0.11.0 ships | `ClientConfigProperties` of the client's two source jars (0.9.6 sets both; 0.11.0 declares them with no default and skips them when unset); `36-RESEARCH.md` section "V1 client path and socket buffers" |
| 11 | A batch replayed after the upgrade (an exactly-once `BEFORE_PROCESSING` retry) is byte-identical on the wire to the pre-upgrade attempt for the 48-case type matrix, plain and with defaults, including nested Tuple and Map nullable markers. It carries the same `insert_deduplication_token` and differs only in the User-Agent header. No clean stop is required. A column type outside the matrix was not compared. | golden: measured; token format: read from the code | client 0.11.0 ships | golden: E-04 on 0.9.6 and E-08 on 0.11.0; token format: `QueryIdentifier.getDeduplicationToken` |
| 12 | The Confluent archive grows from 7802 to 7933 inner entries (+131, about +1.1 MB zipped): 117 entries are new, 4 are gone, 94 changed. The guava override in `build.gradle.kts` is redundant on 0.11.0, not lost, and the httpclient5 and lz4 pins still win. | measured | client 0.11.0 ships | E-09 |
| 13 | The D-17 default (`setCompressionMethod` true, `compress=false`) stays. V2 of client 0.11.0 decodes ZSTD with `setCompressionMethod=false` and `compress=true`, while client V1 cannot decode ZSTD on any client line, so the default keeps V1 readable and keeps the zstd native unloaded. The V1 half holds on either client; the V2 decode statement holds only on client 0.11.0. | measured | either client | E-06 (`Client011BehaviourPinLiveTest`), E-10 |
| 14 | The `Read timed out after` and `Write timed out after` IOException prefixes are produced by the client's own piped streams in `clickhouse-data`, are unchanged between the two lines, and are still retriable. | measured | either client | E-03, E-06 (`ClientTimeoutPrefixPinUnitTest`) |

## Exactly-once replay across the upgrade

The wording below is the E-08 result, with its labels.

A `BEFORE_PROCESSING` batch replayed after the upgrade is rebuilt by the new task from the stored offset range. The same writer serialises it into the same bytes, and the same path sends it with the same body and the same `insert_deduplication_token`. ClickHouse's block deduplication therefore sees the block the old task would have sent. The User-Agent header is the only request header that differs.

- The golden (measured): the RowBinary body of 96 lines, 48 type cases in two variants, was blessed on client 0.9.6 (E-04). The same 96 lines are green on client 0.11.0 under four time zones (E-08).
- The wire pin (measured): `ClientInsertWirePinUnitTest` shows a 100,000-byte body, the token and no `Content-Encoding` arriving unchanged on 0.11.0 (E-06, E-08).
- The token (read from the code): `QueryIdentifier.getDeduplicationToken` builds `topic-partition-minOffset-maxOffset` from the batch, not from the client. The query-id, writer, processing and state sources are unchanged since the phase start.
- Scope: the 48 type cases, both variants. A column type outside the matrix, a time-zoned `DateTime` and a `Variant` fed plain text were not compared.

`clean_stop_required: no`, for the compared matrix only.

## zstd-jni: platforms and the temp folder

Each fact below carries the label of item 9: the archive facts are measured (E-09), the worker facts are measured in research (not re-run) in the `confluentinc/cp-kafka-connect-base:7.9.7` image on linux/aarch64, and the loader behaviour is read from the library's `Native.java` of 1.5.7-23.

- The `cloud` jar that client 0.11.0's POM declares carries three natives: `darwin/aarch64`, `linux/aarch64` and `linux/amd64` (E-09, measured).
- The plain `zstd-jni` jar carries 18 platforms. The ones the `cloud` jar does not carry are: linux arm, i386, loongarch64, mips64, ppc64, ppc64le, riscv64 and s390x; windows aarch64, amd64 and x86; freebsd amd64 and i386; aix ppc64; and darwin x86_64 (Intel macOS) (E-09 and the research, measured).
- On an unsupported platform the load throws `UnsatisfiedLinkError: Unsupported OS/arch, cannot find /<os>/<arch>/libzstd-jni-1.5.7-23.<ext>`. The library has no pure-Java fallback (read from the code).
- With `/tmp` mounted `noexec` the failure reads `failed to map segment from shared object`. Two remedies work: `-DZstdTempFolder=<dir>` or `-Djava.io.tmpdir=<dir>`, with `<dir>` an executable folder (measured in research (not re-run)).
- `ZstdNativePath` is a third system property. It loads the given file and has no fallback (read from the code).
- An `Error`, not an exception, escapes the failing load. `Utils.getRootCause` returns null for it, so the task fails without a retry (read from the code; the research probe saw the noexec failure surface as an `ExceptionInInitializerError`, which is an `Error`).
- The plugin's `Zstd` class is loaded child-first by the plugin class loader and coexists with the worker's own `zstd-jni` 1.5.6-4 (measured in research (not re-run)).
- The default configuration never reaches the ZSTD path: the connector's lz4 setting on with `compress=false`, or with `compress=true`, mapped no zstd native in the probe (measured in research (not re-run)). An operator reaches it only with `compress=true` and `setCompressionMethod=false`, or with `decompress=true`.
- Alpine and other musl workers were not tested.

## custom_ keys in jdbcConnectionProperties

This is item 7 expanded.

The stub probe of E-11 builds a V2 client with `?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true` against an in-process stub and prints the request URI of the insert.

- Client 0.11.0 (measured, E-11): `/?custom_myparam=7&custom_settings=max_threads%253D3&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4`. Both `custom_` keys are forwarded, and the operator's `%3D` has become `%253D`.
- Client 0.9.6 (measured, E-11): `/?query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4`. No `custom_` key is forwarded.

The server then rejects the forwarded setting with `Code: 115 ... UNKNOWN_SETTING`, which the connector does not retry, so the task fails (measured in research (not re-run), against a real server).

What to search: `custom_` in every `jdbcConnectionProperties` value of every connector, before moving.

The alternative that this phase did not apply: one more client option, set before the property loop in `createClientV2`, restores the 0.9.6 behaviour. In the research it made `?custom_settings=...` and `?custom_myparam=7` pass against a real server on 0.11.0 (measured in research on 0.11.0, the Open Question 1 paragraph of `36-RESEARCH.md`; the 0.9.6 side was read from the source). It is a production behaviour change beyond the two required `createClientV2` fixes, so it stays a user decision and was not added.

## What to do

1. Search your `jdbcConnectionProperties` for `custom_` keys, for `decompress=true`, and for any expectation that `ssl` or `sslmode` there changes the transport. Items 1, 7 and 8.
2. If you set `compress=true` with `setCompressionMethod=false`, check the worker's platform and its temp folder first. Item 9.
3. Review `retryOnSocketException` and your log alerts against items 3 and 6.

## Handoffs

Phase 37:

- The archive allow-list must name the 37 `com/github/luben/zstd/` classes, the 17 `META-INF/versions/22/com/github/luben/zstd/` classes and the three natives. The compare script's VERSION table has no `zstd-jni` row.
- The stale `// temp to override client's with CVE` comment and the redundant guava pin in `build.gradle.kts` are a follow-up. This phase edits no build file.

Phase 38:

- RLS-01 refreshes the raw FORK-DELTA numbers once. FORK-DELTA's open item 8 still names client `0.10.0` as a future requirement.
- RLS-02 checks that these items are in the gg6 note. On a no-go, keep only the `either client` rows.
- PR #17 (the Dependabot bump of the same line) is superseded by the catalog commit. Closing it is the user's action at publication, and no agent comments on it. Do not merge PR #17 or any other Dependabot PR on origin before publication.
- For the milestone close: the result of CLT-07's nightly dispatch, if the user has run it.
