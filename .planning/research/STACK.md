# Technology Stack

**Project:** ClickHouse Kafka Connect `custom_build` fork, milestone `v1.7.0_gg6-maintenance`
**Researched:** 2026-10-28
**Mode:** Subsequent milestone (maintenance). Only what the NEW work needs; validated capabilities are not re-researched.
**Overall confidence:** HIGH. The client-bump findings were measured, not read off release notes: a scratch copy of `HEAD` (in `/tmp`, repo untouched) with `clickHouseDriver = 0.11.0` was built and run against ClickHouse `latest` (26.10.3.7) with client V2, including the full `./gradlew test` suite.

## Headline

The milestone needs **no new libraries**. It needs one version bump that is NOT a drop-in (client `0.9.6` -> `0.11.0`), two small production-code fixes that the bump forces, one test-source fix, one PR to reject (#10), and a re-baselined archive allowlist. Everything else Dependabot has open is safe to merge as-is.

| Bump | Verdict | Evidence |
|------|---------|----------|
| #17 `clickHouseDriver` 0.9.6 -> 0.11.0 | **Merge only together with the fixes below.** PR #17 alone is red (`compileTestJava`) and would also break at runtime | Measured; see next section |
| #19 lz4-java 1.12.0 -> 1.12.1 | **Merge first.** Security release for GHSA-48mj-8f7c-7qfm (bounds/length checks on malformed LZ4 input) in a jar that ships inside the archive | PR CI green; release notes (HIGH) |
| #18 okhttp 5.5.0 -> 5.5.1 | Merge. Integration-test source set only; blocklists 8 compromised eMudhra intermediates (CVE-2026-111943) | PR CI green; changelog |
| #20 Gradle wrapper 9.8.1 -> 9.8.2 | Merge. Patch release; works with Shadow 9.6.1 (PR CI ran `Build and verify Confluent archive` green) | PR CI green |
| #16 `actions/setup-java` 6.0.1 -> 6.1.0 | Merge. Dependabot already wrote the full SHA (`b843ebf9b913146b38da2eaa7aeddf76074ae1ad # v6.1.0`) into all four workflows, so the "needs a re-pin" step is not needed; just run `WorkflowActionPinGuardUnitTest` | PR CI green; `git diff` of the branch |
| #10 protobuf-java 3.25.9 -> 4.36.2 | **Do NOT merge. Close and add a Dependabot ignore for `protobuf-java` major >= 4.** | PR CI red: `java.lang.VerifyError ... DescriptorProtos$EnumValueOptions is not assignable to GeneratedMessageV3$ExtendableMessage` in `ProtobufIngestTest` and `ClickHouseSinkTaskWithSchemaTest` |

## The client bump: 0.9.6 -> 0.11.0 (via 0.9.7, 0.9.8, 0.9.9, 0.10.0)

Note: 0.9.7 .. 0.9.9 are on the way to 0.10.0/0.11.0, and the first breaking change that matters to this connector landed in **0.9.7**, not in 0.10/0.11. The milestone brief lists only the 0.11.0 notes; the 0.9.7 and 0.10.0 notes carry two more.

### Measured result

Scratch copy, client `0.11.0`, all four artifacts (`client-v2`, `clickhouse-client`, `clickhouse-http-client`, `clickhouse-data`) moved through the single `clickHouseDriver` catalog entry:

| Step | Result |
|------|--------|
| PR #17 as opened | `compileTestJava` fails: `UtilsTest.java:80,89,98` `constructor ServerException ... required: int,String,int,String`. `src/main` compiles unchanged (the V2 API the connector uses is source-compatible) |
| + test fix only, `testUnit` (Docker-free) | Green (exit 0). The Docker-free tests do not exercise the live client build, which is why this step hides both runtime breaks |
| + test fix, Docker-tagged subset | **23 of 30 fail** (ping returns false -> `Connection to ClickHouse is not active`; `ClientMisconfigurationException: Unknown and unmapped config properties`) |
| + the two production fixes below | **30 tests, 29 pass, 0 fail, 1 skip** (same skip as baseline) |
| + full `./gradlew test` with both fixes | **1,961 tests, 1,943 pass, 0 fail, 18 skipped.** Identical count and skips to the `v1.7.0_gg5` baseline |
| `createConfluentArchive` + `verifyConfluentArchive` | Passes (7,932 inner entries, no nested jar, no Kafka API class) |

### Behaviour and breaking changes that touch this connector

| # | Change (version) | Connector impact | Action | Confidence |
|---|------------------|------------------|--------|------------|
| 1 | **`Client.Builder.addEndpoint(String)` now keeps the URL base path** (0.9.7, issue 2344). `createClientV2` passes `http://host:port/<database>?<props>`, so every request now goes to `POST /<database>`. ClickHouse 26.10 then rejects it: `UNKNOWN_TABLE ... Unknown table expression identifier '<database>'. In scope SELECT VERSION()` | **Hard break.** Ping fails, so `ProxySinkTask.start` throws `Connection to ClickHouse is not active` for every task on client V2 | **Production fix (required):** pass only `protocol://host:port` to `addEndpoint`. The database already travels through `setDefaultDatabase`. Keep the full `url` for the log line only | HIGH (measured) |
| 2 | **`Client.Builder.build()` throws `ClientMisconfigurationException` for unknown config keys** (0.9.7, issue 2658). The connector forwards every `k=v` of `jdbcConnectionProperties` through `setOption` | The documented example `?ssl=true&sslmode=strict` and the repo's own test props (`?ssl=false&sslmode=none`, `?load_balancing_policy=random&health_check_interval=5000&failover=2`) are V1/JDBC keys. They were silently ignored on V2 under 0.9.6 and now fail `start()`. Probe: `?ssl=true&sslmode=*` fails, `?sslmode=none` fails, `?socket_timeout=5000` and `?compress=true` pass, `?ssl_mode=none` fails (`none` is no longer valid on V2) | **Production fix (required):** `clientBuilder.setOption("ignore_unknown_config_key", "true")` before the property loop (so an operator-supplied value still wins). Restores 0.9.6 behaviour; the client logs one WARN naming the ignored keys. Do not filter against `ClientConfigProperties` yourself: the prefixes (`clickhouse_setting_`, `http_header_`, `custom_`) make a hand-rolled allowlist wrong. Add a ROLLOUT line: `ssl`/`sslmode` in `jdbcConnectionProperties` were never honoured on V2; use `sslEnabled` / `ssl_mode` (values `DISABLED`, `TRUST`, `VERIFY_CA`, `STRICT`) | HIGH (measured) |
| 3 | `ServerException` constructor is now `(int code, String message, int transportProtocolCode, String queryId)` (between 0.9.6 and 0.11.0) | Test-source only. `src/main` only reads `getCode()` | **Test fix:** add a 4th arg at `UtilsTest` lines 80, 89, 98. Upstream's tail brings 5 more 3-arg sites to the same file (8 in total), so do the bump AFTER merging the upstream tail and fix all sites in one commit. Record it in FORK-DELTA as a known upstream-sync conflict until upstream bumps the client | HIGH (compile error captured in CI log) |
| 4 | **Server error 159 `TIMEOUT_EXCEEDED` is no longer retried by the client** (own cause `ClientFaultCause.ServerTimeoutExceeded`, not in the default list; 0.9.9 first made it non-retriable, 0.11.0 made it opt-in) | The connector never sets `retryOnFailures`, so it gets the new default. Connector-layer classification is unchanged: `Utils.handleException` still lists `159` and throws `RetriableException`, so Kafka Connect redelivers (and exactly-once replays through the BEFORE/AFTER state machine). What changes is that the in-client 3x re-send disappears, so a 159 reaches Connect faster | **Leave the default. Do NOT add `ServerTimeoutExceeded` to the client list.** The client's own rationale applies: a retried insert without dedup can double-write, and the connector already owns the retry with a stable block. Add a Docker-free pin that `Utils.handleException` still maps 159 to `RetriableException`, and note the new Connect-driven retry cadence in the rollout note. `additionalRetriableErrorCodes` (upstream tail) is documented as not changing client retry settings, so the two do not interact | HIGH for the client behaviour (source: `HttpAPIClientHelper` line 1181), MEDIUM for operator-visible cadence |
| 5 | **HTTP 503 and 502 become `java.net.ConnectException`** (wrapped in `ConnectionInitiationException`), retried by default via `ConnectTimeout`; unknown status codes become `ClientException` instead of `ServerException` (0.10.0) | Previously a 503 was a `ServerException` with an unlisted code, so `handleException` failed the task (or DLQ'd with `errors.tolerance=all`). Now the client retries 3x first, then the root cause is `ConnectException`, which is a `SocketException`: it becomes `RetriableException` only when `retryOnSocketException=true`. Net classification at the connector is unchanged for the default config | No code change. Pin both shapes (`ConnectException` root with and without `retryOnSocketException`, `ClientException` root -> non-retriable) in the Docker-free `WriterFailureClassificationUnitTest` family. Mention in rollout | MEDIUM (source read, not exercised against a real 503) |
| 6 | **`socket_rcvbuf` / `socket_sndbuf` have no default** (0.11.0; were 804800) | The connector never sets them, so sockets now use OS auto-tuning. Throughput on high-latency links may differ either way | No code change. Do not set them. Rollout note: an operator who needs the old behaviour adds `?socket_rcvbuf=804800&socket_sndbuf=804800` to `jdbcConnectionProperties` (both are known keys, so they pass check #2). Optionally compare once with `InsertFormatPerformanceTest` | HIGH (source), perf effect UNMEASURED |
| 7 | **ZSTD block compression decode/encode added** (0.11.0, issue 3105). Client-v2 now depends on `com.github.luben:zstd-jni:1.5.7-23:cloud` at compile scope | See D-17 verdict below. Also grows the archive | See D-17 and archive sections | HIGH (measured) |
| 8 | `User-Agent` format changed to `<client name> clickhouse-java-v2/<ver> (lv:java/..; os:..; hc:ahc_..)` (0.11.0) | The client name `clickhouse-kafka-connect/<version>` stays the prefix, and `ClickHouseSinkTaskTest` (`startsWith(CONNECT_CLIENT_NAME)`) passed. Dashboards that parse the tail of `http_user_agent` break | Rollout note only. `ROLLOUT-v1.7.0_gg5.md` currently says "the rest of the user agent did not change"; that sentence must be superseded | HIGH |
| 9 | `X-ClickHouse-Format` header now set client-wide (default `RowBinaryWithNamesAndTypes`); on ClickHouse 26.8+ the header beats a SQL `FORMAT` clause (0.11.0, issue 3086) | The connector queries through `queryV2`/`Records` and passed on 26.10 with no change, including the DESCRIBE paths | No action. Do not introduce SQL `FORMAT` clauses on V2 queries | HIGH (suite green) |
| 10 | `Date`/`Date32` read as `LocalDate` without timezone; explicit tz handling on write (0.9.7) | The connector writes RowBinary itself (`ClickHouseWriter.doWriteColValue`) and reads only DESCRIBE metadata through the client, so no data-path effect. Test read-backs passed | None | HIGH (suite green) |
| 11 | Auth/SSL builder strictness (0.10.0): `ClientMisconfigurationException` instead of `IllegalArgumentException`; user+password together with a custom `Authorization` header rejected; `ssl_mode` reintroduced (default `STRICT` on HTTPS); `access_token` now actually sent | Connector sets only user/password, optional SNI, optional proxy. SNI still disables hostname verification exactly as under 0.9.6 (source compared line by line), so `sslSocketSni` users are unaffected | None. Do not add `Authorization` through `clickhouseSettings`/headers | HIGH (source) |
| 12 | `executionTimeout` is now always milliseconds (0.10.0) | The connector never sets `max_execution_time` on the client | None | HIGH (grep) |
| 13 | JDBC-V2 changes (`async_insert` no longer forced to 0, metadata via `SHOW`, parser fixes) | The connector does not use the JDBC driver; only `clickhouse-client`/`http-client`/`data` (V1, now deprecated classes but present at 0.11.0) and `client-v2` | None. Skip all JDBC-v2 migration items | HIGH |
| 14 | Observability SPI, Micrometer and OpenTelemetry recorders (0.11.0) | Optional; OTel API is `provided` scope, Micrometer optional | **Do not add** `opentelemetry-api` or `micrometer-core`. The connector's JMX statistics (`SinkTaskStatistics`/`TopicStatistics`) are the observability surface | HIGH |

### D-17 (`network_compression_method=lz4`): still needed? safe?

**Verdict: no longer required for client V2 on 0.11.0, still safe, keep it. Do not retire it in this milestone.**

Measured on ClickHouse 26.10.3.7 (server default `network_compression_method=ZSTD`), client V2 `0.11.0`:

| `setCompressionMethod` | `jdbcConnectionProperties` | Result |
|-----------------------|---------------------------|--------|
| true (default) | `` | OK, server reports `lz4` |
| false | `` | OK, server reports `ZSTD` |
| false | `?compress=true` | **OK** (on 0.9.6 this was `Invalid LZ4 magic byte: '-112'`) |
| true | `?compress=true` | OK, server reports `lz4` |

Why keep it:
1. The old failure mode is gone on V2: with both controls absent and `compress=true`, 0.11.0 decodes ZSTD. So `FORK-DELTA` row "at least one of the two controls is required on V2" and the ROLLOUT-gg1 warning ("do not combine `compress=true` with `setCompressionMethod=false`") become stale for 0.11.0, and `NetworkCompressionPinLiveTest` / `NetworkCompressionPinUnitTest` Javadoc ("client 0.9.6 cannot decode ZSTD") must be reworded.
2. The V1 stack (`clickhouse-client`/`http-client` 0.11.0) is not in the ZSTD change list (the notes name client-v2 and jdbc-v2 only); the lz4 setting remains the only guard there.
3. `setCompressionMethod` is upstream's flag (default true) on a high-risk file (`ClickHouseSinkConfig`). Retiring it is a behaviour change with no benefit; lz4 avoids loading the zstd-jni native library on the default path.
4. All existing D-17 pins passed unchanged on 0.11.0 (`NetworkCompressionPin*`, `Lz4RoundTripLiveTest`).

Add one new live pin: V2, `setCompressionMethod=false`, `compress=true`, ZSTD server -> query round-trips. That converts today's measurement into a regression guard and documents that the 0.9.6 constraint is lifted.

### zstd-jni in the archive

Client-v2 0.11.0 adds `zstd-jni 1.5.7-23` with classifier `cloud` (1.0 MB; natives for `linux/amd64`, `linux/aarch64`, `darwin/aarch64` only; no Windows, musl, ppc64le or s390x). It is shaded into the fat jar. The default connector path (`compress=false`, `decompress=false`) never touches ZSTD, so a worker without a matching native is not affected unless an operator turns `compress=true` on. Say this in the rollout note. Medium risk to verify once: on a Connect worker with a noexec `/tmp`, zstd-jni extracts its native to a temp file on first use.

## Other compatibility answers

**lz4-java capability rule (`org.lz4:lz4-java` vs `at.yawk.lz4:lz4-java`).** Unchanged and still correct. `client-v2` 0.11.0 declares `at.yawk.lz4:lz4-java:1.11.4`; the fork pins 1.12.0 (1.12.1 after #19), higher wins inside the same module, and the capability rule only decides between the two groups. `TestRuntimeClasspathLz4UnitTest` and `Lz4RoundTripLiveTest` passed with 0.11.0. Client 0.11.0 now uses `lz4-java` directly for request compression (replacing a slow Apache Commons Compress path), so the `at.yawk` fork is exercised more than before, which is one more reason to take 1.12.1 first.

**Gradle 9.8.2 with Shadow 9.6.1.** Compatible. PR #20 CI ran the whole job including `Build and verify Confluent archive`. No `distributionSha256Sum` is set in `gradle-wrapper.properties`; Dependabot rewrites only `distributionUrl`. Optional hardening, not required.

**okhttp 5.5.1.** `integrationTest` source set only (`libs.okhttp`). Not in the shipped jar.

**protobuf-java 4.x in testFixtures.** Incompatible with the current Confluent test stack. `kafkaPlatformProtobuf = 7.9.1` (serializer/converter/provider) is compiled against protobuf 3.25 generated classes; protobuf-java 4.x moved `DescriptorProtos` off `GeneratedMessageV3`, hence the `VerifyError`. Confluent Platform **8.0.0** is the first line built on protobuf 4.x (`protobuf.version` 4.29.3 in its parent POM; 8.4.0 is current). Making #10 work therefore means moving the whole Confluent test stack (Schema Registry client, Avro/Protobuf serializers, converters, all pinned at 7.9.1) to 8.x: a separate, larger change that drags the Avro and JSON-schema test paths along and has nothing to do with production code. Defer; do not fold it into this milestone.

## Required `Dependabot` / catalog housekeeping

- `gradle/libs.versions.toml`: `clickHouseDriver = "0.11.0"`, `lz4Java = "1.12.1"`, `okhttp = "5.5.1"`. Leave `libprotobuf = "3.25.9"`. Leave `kafka = "4.4.0"` (compile-only, unchanged), `shadow = "9.6.1"`, `kafkaPlatform* = "7.9.1"`.
- `gradle/wrapper/gradle-wrapper.properties`: `gradle-9.8.2-bin.zip`.
- `.github/dependabot.yml`: add an ignore for `com.google.protobuf:protobuf-java` `version-update:semver-major` with a comment pointing at the Confluent 7.9 -> 8.x prerequisite. Without it Dependabot reopens #10 monthly.
- Keep all four ClickHouse artifacts on the one `clickHouseDriver` ref so they cannot drift.

## Archive allowlist for the CI consumer (`confluent-archive-allowlist-*.txt`)

Measured with `.planning/scripts/compare-confluent-archives.py`, baseline `HEAD` (0.9.6) vs scratch (0.11.0), empty allowlist: **exit 1; 4 only-A, 117 only-B, 96 changed, 7,299 same; 0 nested jars; 0 Kafka classes; MANIFEST identical**. The two connector-owned classes that differ are `ClickHouseHelperClient.class` and `ClickHouseHelperClient$ClickHouseClientBuilder.class` (the fixes above, plus the version constant). All other differences are the client bump. A gg6 allowlist needs these rows, each of which must match at least one difference or the script fails `STALE-ROW`:

| Glob | Why |
|------|-----|
| `com/clickhouse/client/**`, `com/clickhouse/data/**` | client 0.9.6 -> 0.11.0 classes (80 changed, 60 added, 3 removed `ClickHouseLZ4*Stream`/`LZ4Entity`, 10 `data`) |
| `META-INF/versions/11/**` | multi-release `HttpClientConnectionImpl`, `module-info` of the client |
| `com/github/luben/zstd/**`, `META-INF/versions/22/com/github/luben/**` | new zstd-jni classes (37 + 17) |
| `linux/amd64/libzstd-jni-1.5.7-23.so`, `linux/aarch64/libzstd-jni-1.5.7-23.so`, `darwin/aarch64/libzstd-jni-1.5.7-23.dylib` | zstd natives, listed exactly (no catch-all `*.so`) |
| `client-v2-version.properties` | removed in 0.11.0 |
| the four `Version`/MBean/`MANIFEST` rows as in `-32.txt` | release identity v1.7.0_gg5 -> v1.7.0_gg6 |
| `com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient*.class` | the two connector fixes (once; the Version row already covers the constant) |

The lz4 bump to 1.12.1 adds its own `net/jpountz/**` rows (the baseline for gg6 is the rebuilt `v1.7.0_gg5` tag, which carries 1.12.0). Recommended CI shape: build the tag archive and the tip archive in the workflow, run the script with the committed allowlist, fail the job on non-zero. Stdlib Python only, so no new dependency; reuse `python3 -I`. Do not commit generated archives.

## `increment-version.yml` (`vnull`)

No new library. `semver.inc('v1.7.0_gg5', ...)` returns null because `_ggN` is not valid semver. Drop the npm `semver` install and parse in the shell step: match `^v?([0-9]+)\.([0-9]+)\.([0-9]+)(_gg([0-9]+))?$`, bump the requested part, and for `_ggN` map `patch` to `N+1`. Fewer moving parts than the current Node 24 + semver 7.8.5 install, and the action SHA pins stay as they are. Add a Docker-free unit test of the extracted script (the repo already guards workflows with `WorkflowActionPinGuardUnitTest`).

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Client bump scope | Go straight to 0.11.0 behind the two fixes | Stop at 0.10.0 (Dependabot PR exists as a branch) | 0.10.0 already carries both hard breaks (0.9.7 inheritance) and the 503/`ServerException` changes, and only 0.11.0 decodes ZSTD. Stopping earlier buys no safety |
| Unknown JDBC keys | `ignore_unknown_config_key=true` | Strip unknown keys against `ClientConfigProperties` | Prefix families make a hand-written allowlist drift; the flag is the client's own switch and keeps operator overrides |
| Base-path break | Endpoint = `protocol://host:port` | Keep the path and set the HTTP path to `/` per request | The database is already sent via `setDefaultDatabase`; the path was only ever decoration |
| 159 handling | Leave the client default, rely on Connect + state machine | Add `ServerTimeoutExceeded` to `retryOnFailures` | Re-introduces a client-side re-send of a possibly dedup-less insert, and the client authors removed it for that reason |
| D-17 | Keep `setCompressionMethod` default true | Retire it now that V2 decodes ZSTD | No benefit, touches a high-risk config file, V1 still needs it, zstd-jni native stays unloaded |
| protobuf 4.x | Reject #10; revisit with Confluent 8.x | Force protobuf-java 4.x over Confluent 7.9.1 | `VerifyError`; would need the whole test stack on 8.x |
| Observability | Existing JMX statistics | Micrometer / OpenTelemetry recorders from client 0.11.0 | Optional deps, new classpath surface in a Connect plugin, not requested |

## What NOT to add

- `opentelemetry-api`, `micrometer-core` (client 0.11.0 optional recorders).
- `socket_rcvbuf`/`socket_sndbuf` settings (the point of the change is OS auto-tuning).
- `ServerTimeoutExceeded` in the client retry list.
- `jdbc-v2` / `clickhouse-jdbc` artifacts or any JDBC-v2 migration (the connector does not use JDBC).
- A zstd-jni or lz4 direct dependency: both arrive through `client-v2`; pin lz4 only through the existing catalog entry.
- `distributionSha256Sum` or a Gradle plugin change as part of #20.
- A hand-rolled allowlist of client config keys.

## Installation / change set

```bash
# gradle/libs.versions.toml (four artifacts share the one ref)
clickHouseDriver = "0.11.0"
lz4Java          = "1.12.1"
okhttp           = "5.5.1"
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.2-bin.zip
# keep: libprotobuf = "3.25.9", shadow = "9.6.1", kafka = "4.4.0", kafkaPlatform* = "7.9.1"
```

Production-code change (`ClickHouseHelperClient.createClientV2`, two lines of substance, both measured):

```java
.addEndpoint(String.format("%s://%s:%d", protocol, hostname, port))   // was: the full url incl. /<database>?<props>
...
clientBuilder.setOption("ignore_unknown_config_key", "true");           // before the jdbcConnectionProperties loop
```

## Suggested ordering (stack-driven)

1. #19 lz4 1.12.1, #18 okhttp 5.5.1, #20 Gradle 9.8.2, #16 setup-java 6.1.0: independent, each already green.
2. Upstream tail merge (adds `additionalRetriableErrorCodes`, 5 more 3-arg `ServerException` test sites).
3. Client 0.11.0 as its own commit with the two production fixes, the 8 `UtilsTest` sites, new pins (endpoint has no path; unknown JDBC key tolerated; 159 stays `RetriableException`; 503 `ConnectException` shapes; V2 decodes ZSTD with both controls off), reworded D-17 docs, FORK-DELTA/anchors rows. Run two full suites.
4. Re-baseline the archive allowlist and wire the compare into CI.
5. Close #10 with a Dependabot ignore.

## Residual gaps (not measured)

- `integrationTest` (Connect-worker Docker harness) and the nightly were not run against 0.11.0.
- Real 502/503 behaviour and the 159 cadence against a live proxy or an overloaded server are source-derived.
- Throughput effect of the dropped 804800-byte socket buffers is unmeasured.
- Client V1 path (`useClientV2=false`) was only exercised by the existing live tests (passed), not re-audited line by line.

## Sources

- ClickHouse/clickhouse-java `docs/releases/0_10_0.md`, `0_11_0.md`; GitHub release bodies v0.9.7, v0.9.8, v0.9.9, v0.10.0, v0.11.0 (HIGH, official)
- clickhouse-java v0.11.0 source: `ClientConfigProperties.parseConfigMap` (unknown keys), `ServerException`, `HttpAPIClientHelper` (retry classification lines ~1150-1190, 503 mapping ~845, SNI ~326-346) (HIGH)
- Maven Central POMs `com.clickhouse:client-v2:0.9.6` and `0.11.0`; `zstd-jni-1.5.7-23-cloud.jar` listing (HIGH)
- Dependabot PRs #10, #16-#20 on `ne1r0n/clickhouse-kafka-connect` and their check runs, incl. failed logs for runs 40416377063 (#17) and 39501799049 (#10) (HIGH)
- yawkat/lz4-java v1.12.1 release notes (security release GHSA-48mj-8f7c-7qfm); okhttp 5.5.1 changelog; actions/setup-java v6.1.0 release (HIGH; advisory text itself was not retrievable, severity unconfirmed)
- Confluent `kafka-schema-registry-parent` POMs 7.9.1 / 8.0.0 / 8.4.0 (protobuf 4.29.3 from 8.0.0) (MEDIUM: 8.4.0 value not read, only the 8.0.0 line)
- Local measurement: scratch copy of `HEAD` under `/tmp` with client 0.11.0, ClickHouse 26.10.3.7, Gradle 9.8.1 wrapper (HIGH)
