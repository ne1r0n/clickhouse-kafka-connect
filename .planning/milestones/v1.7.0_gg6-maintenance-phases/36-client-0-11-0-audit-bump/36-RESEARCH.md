# Phase 36: Client 0.11.0 Audit & Bump - Research

**Researched:** 2026-10-29
**Domain:** ClickHouse Java client 0.9.6 to 0.11.0 under the fork's connector: one catalog bump, the two required `createClientV2` fixes, behaviour pins on both client lines, an exactly-once byte comparison, D-17 rewording, archive listings, and the CLT-09 one-commit revert
**Confidence:** HIGH (every behavioural claim below was run this session on throwaway clones of `6a43dd2d`, against ClickHouse `latest` = 26.10.3.7; the bump, both fixes, the pin classes, the byte comparison, the revert and the worker-JVM zstd probes were all executed)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
CONTEXT.md is "Auto-generated (discuss skipped via workflow.skip_discuss)". Its `## Decisions` section holds only the `### Claude's Discretion` block below. The phase boundary it restates is governing:

The connector runs on ClickHouse Java client 0.11.0, and every behaviour change is recorded. Classification, endpoint, config-key and compression behaviours that operators rely on are pinned first on 0.9.6, then on 0.11.0. The two required `createClientV2` fixes land. An exactly-once retry across the upgrade either sends identical bytes or needs a documented clean stop. The archive's change is recorded. The bump is one catalog commit, so if the audit fails gg6 ships on 0.9.6 by reverting it (CLT-09). Builds on the post-tail, post-BUF tree (Phases 34-35).

Order: 0.9.6 pins first, then the bump with the two fixes, then the pin class, byte comparison, doc rewording, archive listings, the two full runs last. Make the CLT-09 revert really one commit; the 10 three-argument `ServerException` sites (UtilsTest 8, AdditionalRetriableErrorCodesTest 1, AdditionalRetriableErrorCodesPinUnitTest 1) must compile on both clients or ride in the catalog commit. Record `SELECT version()` in every evidence block.

CLT-07's manual nightly dispatch needs the scratch branch pushed to origin: that is the user's decision and must NOT be done by an agent. Pushes nothing; prepare everything and leave CLT-07 as a user-gated item (stop and ask the user at that point). Dependency bump authorization: the milestone ROADMAP plus the user's /gsd-autonomous request; disclose in plan text. Out of scope: `ServerTimeoutExceeded` in the client retry list, `socket_rcvbuf`/`socket_sndbuf`, removing D-17.

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Deferred Ideas (OUT OF SCOPE)
None — discuss phase was skipped. REQUIREMENTS.md "Out of Scope" adds: adding `ServerTimeoutExceeded` to the client retry list; setting `socket_rcvbuf`/`socket_sndbuf`; removing the D-17 `lz4` default; protobuf 4.x; running the upgrade on production connectors.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CLT-01 | Pins on 0.9.6 record current behaviour: 159 retriable, 502/503 and unexpected-status `ClientException` classification, timeout-message prefixes | Measured shape table (stub server, real client, real socket); 7 + 2 Docker-free pins green on 0.9.6 and on 0.11.0; version-keyed rows; both timeout prefixes produced by real client stream classes |
| CLT-02 | `clickHouseDriver` to 0.11.0 as one catalog change with `# FORK OVERRIDE (upstream 0.9.6)`, anchor row, Docker-free catalog pin | Exact catalog line and four modules on one ref; mid-file anchor placement that keeps `git revert` clean; `ClientCatalogPinUnitTest` |
| CLT-03 | `createClientV2` passes only `protocol://host:port`, sets `ignore_unknown_config_key=true` before the loop; both pinned live | Code quoted; fix measured red on bare 0.11.0 (3 pins), green on 0.9.6 unfixed, 0.9.6 fixed, 0.11.0 fixed; both fixes are no-ops on 0.9.6 |
| CLT-04 | `Client011BehaviourPinLiveTest`: 159, 502/503 with and without `retryOnSocketException`, `ClientException` non-retriable, ZSTD decode with `setCompressionMethod=false` + `compress=true` | Live 159 (real `max_execution_time`), ZSTD live pin keyed by client line, 502/503 via the shared stub; all measured on both lines |
| CLT-05 | D-17 stays; rationale reworded to 0.11.0 measurements in three places | Measured: V1 still cannot decode ZSTD on 0.11.0, V2 can; three exact sites with line numbers |
| CLT-06 | RowBinary byte comparison 0.9.6 vs 0.11.0; replay across the upgrade | 48 serialisable cases x 2 variants: identical length and SHA-256 on both lines under three time zones; wire body and URL identical; clean-stop wording |
| CLT-07 | Date/datetime live pins pass on non-UTC; Connect-worker `integrationTest` and one manual nightly pass on 0.11.0 | Non-UTC mechanism (`TZ=`) proven to reach the test JVM; 14 date executions green under 6 zones on 0.11.0; local harness command; user-gated dispatch described step by step |
| CLT-08 | Before/after Confluent archive listings; `zstd-jni` addition; guava CVE override | Archive diff measured (+131 inner entries, +1,101,785 bytes zipped); zstd-jni arrives as the `cloud` classifier (3 platforms); guava override is redundant, not lost |
| CLT-09 | If the audit fails, revert the one catalog commit | Revert rehearsed: one clean `git revert`, 74 targeted tests green on 0.9.6 afterwards; what it keeps and what a docs revert adds |
</phase_requirements>

## STOP-condition and premise check (done first)

| Check | Result | Evidence |
|---|---|---|
| Repo state | `sync/v1.7.0` at `6a43dd2d`; only `.planning/config.json` and `.planning/state.json` dirty (pre-existing). Nothing in the repo tree was modified by this research | [VERIFIED: `git status --short`, `git log --oneline`] |
| Origin | `refs/heads/custom_build` = `3a3a15a9ae064ffe09c696b8bb87bce23419d463`; no `sync/v1.7.0` on origin; Dependabot branch `dependabot/gradle/clickHouseDriver-0.11.0` at `6a53525b738d90ee4c98a3761ebebe11421cf1f0` | [VERIFIED: `git ls-remote origin`] |
| PR #17 (fork repo; plain `gh pr view 17` resolves to upstream's unrelated PR 17, so always pass `-R ne1r0n/clickhouse-kafka-connect`) | OPEN, base `custom_build`, one file (`gradle/libs.versions.toml`), mergeable. Only check: `ClickHouse latest Client version V2 tests` FAILURE after 54 s, i.e. at compile. Read-only; no PR write was made | [VERIFIED: `gh pr view 17 -R ne1r0n/clickhouse-kafka-connect --json ...`, `gh pr checks 17 -R ...`] |
| Client 0.11.0 on Maven Central | `client-v2`, `clickhouse-client`, `clickhouse-http-client`, `clickhouse-data`, `clickhouse-jdbc` all list `<latest>0.11.0</latest>`, lastUpdated 2026-10-21. Nothing newer exists to prefer | [VERIFIED: `repo1.maven.org/maven2/com/clickhouse/<artifact>/maven-metadata.xml`] |
| Default branch / nightly | `defaultBranchRef` = `custom_build`; `Nightly Integration Tests (ClickHouse latest, client V2)` is an active workflow (id 397634804) | [VERIFIED: `gh repo view`, `gh workflow list -R ...`] |
| ClickHouse `latest` | `26.10.3.7`, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, created 2026-10-26 (same digest as the Phase 34/35 gates) | [VERIFIED: `docker image inspect`, `SELECT version()` through the server container] |
| Baselines to carry | testUnit 1456 / 0 / 0 / 2 skipped; full suite 2047 / 0 / 0 / 18 skipped; anchors `rows=148 bad=0` | [VERIFIED: `35-COMMAND-EVIDENCE.md` E-09 via `35-evidence-check.py get`; `35-evidence-check.py anchors` printed `rows=148 bad=0`] |

No STOP condition. Re-read origin and PR #17 at the gate (Dependabot rebases its head; ask the user not to merge PR #17 or any Dependabot PR on origin until publication, as in Phase 33).

## Summary

The bare bump is a one-line catalog change that breaks exactly two things. (1) `compileTestJava`: 10 three-argument `new ServerException(` sites in 3 test files fail because 0.11.0 has only the four-argument constructor (`required: int,String,int,String found: int,String,int`); `compileJava`, `compileTestFixturesJava` and `compileIntegrationTestJava` pass with no source change, and main compiles with the same 100 deprecation warnings as before. (2) At runtime every V2 call dies: 0.11.0's `addEndpoint(String)` keeps the URL path, so the connector's `http://host:port/<database>?...` endpoint turns every request into `POST /<database>`, and any `jdbcConnectionProperties` key the client does not know (`ssl`, `sslmode`, `load_balancing_policy`, ...) throws `ClientMisconfigurationException` unless `ignore_unknown_config_key=true`. `testUnit` hides both (121 targeted Docker-free tests stay green on the bump once the 10 sites are made to compile); live, 8 of 25 targeted tests go red. The V1 mode fails too, because `ClickHouseHelperClient`'s constructor always builds the V2 client.

Both fixes are safe on 0.9.6 (0.9.6 strips the path itself and only warns on unknown keys; `ignore_unknown_config_key` is just one more warned-about key and never reaches the server). So they land as their own commit on 0.9.6, before the catalog commit, and CLT-09's revert is one clean `git revert` of a commit that holds the catalog line, its pin test and its anchor row (rehearsed). The 10 `ServerException` sites are made two-line-compatible by a reflection factory (`ServerExceptions.create`) committed before the bump, so they ride in neither the catalog commit nor the revert.

The audit found behaviour changes that are operator-visible and must be pinned or recorded: 502/503 become client-retried `ConnectException`s (retriable only with `retryOnSocketException=true`); 159 is no longer retried inside the client (1 attempt, was 4); the User-Agent tail changes; log messages change; `zstd-jni` enters the archive as the `cloud` classifier with natives for only linux/amd64, linux/aarch64 and darwin/aarch64 (and fails under a noexec `/tmp` unless a temp folder is set); `decompress=true` switches request compression to ZSTD and fails on incompressible bodies (`ZstdException: Destination buffer is too small`); and **any `jdbcConnectionProperties` key starting with `custom_` is now forwarded to the server and rejected with error 115** (`UNKNOWN_SETTING`). The last one is new relative to the roadmap and needs a user decision (Open Question 1). The RowBinary bytes are identical across the clients, so no clean stop is required on the byte account.

**Primary recommendation:** commit in this order, all on `sync/v1.7.0`, nothing pushed: factory + 10 call sites; the Docker-free and live pins green on 0.9.6 (bare-bump red set recorded from a throwaway worktree); the `createClientV2` fixes with three anchor rows; the catalog commit (line + `ClientCatalogPinUnitTest` + one mid-file anchor row); docs rewording; evidence; then cache-busted `testUnit`, two `--rerun` full suites on `latest` x V2, and the user-gated nightly dispatch.

## Architectural Responsibility Map

Tiers are connector layers, not web tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Which client version ships | Version catalog `gradle/libs.versions.toml` (`clickHouseDriver`) | `build.gradle.kts` dependency blocks | One ref feeds four modules; the single place a revert has to touch |
| Endpoint and client options | `ClickHouseHelperClient.createClientV2` | `ClickHouseClientBuilder.setJdbcConnectionProperties` | The only production code that talks to `Client.Builder`; construction-time, not a hot path |
| Error classification | `Utils.handleException` (high-risk, untouched) | The client's own exception shapes | The connector classifies what the client throws; the pins observe the client side |
| RowBinary serialisation | `ClickHouseWriter.writeRecordsRowBinary` + `BinaryStreamUtils` (clickhouse-data) | Client transport (passes the stream through) | Bytes are produced by the connector; the client only frames them |
| Exactly-once replay | `Processing` + `QueryIdentifier.getDeduplicationToken` (`topic-partition-min-max`) | `insert_deduplication_token` URL parameter | Token and body are version independent (measured) |
| Archive content | `confluentJar` shadow task over the `clickhouseDependencies` configuration | `verifyConfluentArchive` | `zstd-jni` enters only through client-v2's POM |
| Native loading (`zstd-jni`) | Worker JVM + Connect `PluginClassLoader` | `java.io.tmpdir`, `-DZstdTempFolder` | Extracts a `.so` into a temp folder and `System.load`s it |

## Standard Stack

One dependency moves. No new library is chosen by the phase; one arrives transitively.

| Item | From | To | Notes |
|---|---|---|---|
| `com.clickhouse:client-v2`, `clickhouse-client`, `clickhouse-http-client`, `clickhouse-data` | 0.9.6 | 0.11.0 | All four share `version.ref = "clickHouseDriver"`; all four exist at 0.11.0 on Maven Central [VERIFIED: metadata above] |
| `com.github.luben:zstd-jni` (classifier `cloud`) | absent from the archive | `1.5.7-23` | Declared by client-v2 0.11.0's POM with `<classifier>cloud</classifier>`; Maven Central latest is `1.5.7-25` (2026-10-28); license BSD 2-Clause; source `github.com/luben/zstd-jni`; 164 published versions [VERIFIED: client-v2-0.11.0.pom lines 156-163; `maven-metadata.xml`; POM] |

**Catalog today** [VERIFIED: `gradle/libs.versions.toml:1-3`]:
```
[versions]
clickHouseDriver = "0.9.6"
kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)
```
and lines 38-41 [VERIFIED: same file]:
```
clickhouse-client = { module = "com.clickhouse:clickhouse-client", version.ref = "clickHouseDriver" }
clickhouse-http-client = { module = "com.clickhouse:clickhouse-http-client", version.ref = "clickHouseDriver" }
clickhouse-data = { module = "com.clickhouse:clickhouse-data", version.ref = "clickHouseDriver" }
clickhouse-client-v2 = { module = "com.clickhouse:client-v2", version.ref = "clickHouseDriver" }
```
The marker precedent is the `kafka` line: two spaces, `# FORK OVERRIDE (upstream <value>)`. The new line is exactly:
```
clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)
```
It carries no marker today (upstream's own value since v1.7.0; FORK-DELTA line 71 says so, and must change).

### What the bump changes transitively (`dependencies --configuration ...`, 0.9.6 vs 0.11.0)

[VERIFIED: `./gradlew dependencies --configuration runtimeClasspath|clickhouseDependencies|testRuntimeClasspath|integrationTestRuntimeClasspath` on the base clone and on the bump clone, diffed]

| Module | 0.9.6 tree | 0.11.0 tree | Resolved (both) |
|---|---|---|---|
| `com.github.luben:zstd-jni` | not in `clickhouseDependencies` (kafka-clients brings 1.5.7-17 on `runtimeClasspath` only) | `1.5.7-23` direct under client-v2 (cloud classifier); on `runtimeClasspath` kafka-clients' `1.5.7-17 -> 1.5.7-23`; on `testRuntimeClasspath` `1.5.6-4 -> 1.5.7-23` | **new in the archive** |
| `com.google.guava:guava` | client-v2 requests `33.4.6-jre -> 33.7.2-jre` (the catalog pin wins) | client-v2 requests `33.7.2-jre` itself, no arrow | `33.7.2-jre` both |
| `jspecify`, `error_prone_annotations`, `j2objc-annotations` | arrows `1.0.0 -> 1.0.1`, `2.36.0 -> 2.50.0`, `3.0.0 -> 3.1` | no arrows (client-v2 requests `2.50.0`, `3.1` directly) | same |
| `httpclient5` | `5.4.4 -> 5.6.5` | `5.6.4 -> 5.6.5` | `5.6.5` (pin still wins) |
| `httpcore5`, `httpcore5-h2` | `5.3.4 -> 5.4.4` | `5.4.3 -> 5.4.4` | `5.4.4` |
| `at.yawk.lz4:lz4-java` | `1.10.2 -> 1.12.1` | `1.11.4 -> 1.12.1` | `1.12.1` (pin wins) |
| `commons-compress` | `1.28.0` | `1.28.0` (its own tree now lists lang3 `3.18.0 -> 3.20.0`) | `1.28.0`, lang3 `3.20.0` |
| `RoaringBitmap`, `asm`, `slf4j-api`, `commons-codec`, `commons-io` | `1.0.6`, `9.7`, `2.0.7`, `1.19.0`, `2.20.0` | same | same |

Nothing else differs on any of the four configurations. Main compiles with the same warning inventory: 100 `[deprecation]` warnings on both lines (`ClickHouseProxyType` 22, `ClickHouseResponse` 17, `ClickHouseClient` 14, `ClickHouseNode` 11, `ClickHouseClientOption` 11, ...), so 0.11.0 deprecates nothing the connector newly uses [VERIFIED: `compileJava --rerun-tasks`, warnings counted on both clones].

**Package Legitimacy Audit.** The seam (`gsd_run query package-legitimacy check`) covers npm, pypi and crates, not Maven; Maven coordinates were verified against Maven Central metadata and the POM instead.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `com.clickhouse:*` 0.11.0 | Maven Central | same group as the 0.9.6 already shipped | n/a | `github.com/ClickHouse/clickhouse-java` (POM `Implementation-URL`) | OK (same publisher, same group) | Approved by ROADMAP + the user's /gsd-autonomous request; disclose in plan text |
| `com.github.luben:zstd-jni:1.5.7-23` (cloud) | Maven Central | many years; 164 versions; already on the fork's classpaths through kafka-clients | n/a | `github.com/luben/zstd-jni` | OK by registry and POM facts [ASSUMED: no seam verdict available for Maven] | Arrives transitively; planner records it, no separate install task |

Packages removed due to SLOP: none. Flagged SUS: none. The cloud jar in the Gradle cache has SHA-256 `37589ad51ad534e4fcee7cd5a3e006d73fac9d98dc3f06c07f35eadab432e7b3` [VERIFIED: `shasum -a 256`].

## Architecture Patterns

### System Architecture Diagram

```
jdbcConnectionProperties "?ssl=true&sslmode=strict" + hostname/port/ssl flag/database
        |
        v
ClickHouseHelperClient ctor  --builds BOTH clients, always (V1 mode too)-->  createClientV1()  (ClickHouseNode URL keeps path + query; V1 understands ssl/sslmode)
        |
        v createClientV2()
   endpoint = protocol://host:port           <<< fix 1: no database, no query (0.11.0 keeps a path; 0.9.6 stripped it)
   Client.Builder.setOption(ignore_unknown_config_key, true)   <<< fix 2: BEFORE the loop (operator value, set later, wins)
   for each "k=v" of jdbcConnectionProperties: setOption(k, v)  (V1-only keys are warned about, not fatal)
        |
        v
   Client (0.9.6 | 0.11.0) --HTTP--> ClickHouse
        |  insert body  = RowBinary produced by ClickHouseWriter (identical bytes on both lines)
        |  URL          = /?query_id=..&insert_deduplication_token=topic-p-min-max&query=INSERT..FORMAT RowBinary&network_compression_method=lz4
        v
   exception shapes: ServerException / ClientException / ConnectionInitiationException <- ConnectException ...
        v
   Utils.handleException (classification; unchanged)  -->  RetriableException | RuntimeException | DLQ
```

### Recommended commit sequence (all on `sync/v1.7.0`, nothing pushed)

| # | Commit | Contents | Gate |
|---|---|---|---|
| 0 | `chore(36)` helpers | `36-evidence-check.py` and `36-hygiene-check.py` as byte copies of the Phase 35 helpers; empty `36-COMMAND-EVIDENCE.md` skeleton | `cmp` against the Phase 35 files |
| 1 | `test(36)` factory | new `src/test/java/.../sink/util/ServerExceptions.java`; the 10 call sites switched to `ServerExceptions.create(...)` (import placed in lexicographic position in `AdditionalRetriableErrorCodesPinUnitTest`, none needed in `UtilsTest`: same package) | on 0.9.6: the 3 classes keep their counts; bare 0.11.0 `compileTestJava` passes (measured: 82 targeted tests green on 0.11.0) |
| 2 | `test(36)` 0.9.6 pins | `StubClickHouseServer`, `ClientErrorShapePinUnitTest`, `ClientTimeoutPrefixPinUnitTest`, `ClientEndpointAndOptionsPinUnitTest`, `Client011BehaviourPinLiveTest` (version-keyed), optionally the RowBinary golden live test | all green on 0.9.6 on the unfixed tree; then in a throwaway worktree with only the catalog line changed: red set = exactly the 3 endpoint/options pins (record with `failed --expect`) |
| 3 | `fix(36)` | `createClientV2` two fixes + `IGNORE_UNKNOWN_CONFIG_KEY` constant + 3 anchor rows (CLT-03) | pins unchanged on 0.9.6 (measured 68/68 targeted); anchors `rows=151 bad=0` |
| 4 | `build(36)` catalog | the one catalog line + `ClientCatalogPinUnitTest` + one anchor row inserted after the `kafka = "4.4.0"` row (mid-file) | targeted Docker-free + `ConnectApiReferencesUnitTest` green on 0.11.0 (measured 82/82); anchors `rows=152 bad=0` |
| 5 | `docs(36)` | D-17 rewording at the three sites, FORK-DELTA rows, gg1 rollout warning (ONE commit, so a no-go can revert it beside commit 4) | hygiene helper 0 hits |
| 6 | `docs(36)` evidence and records | archive listings, byte comparison, probe results, Phase 38 rollout items | hygiene + secrets helpers |
| 7 | gate | cache-busted `testUnit`; two `--rerun` full suites; anchors mirror; cumulative gate; CLT-09 decision record | phase gate |
| 8 | STOP for the user | CLT-07 nightly dispatch; resume to record the user's report | see "CLT-07" |

The anchor-row placement in commit 4 matters: it is inserted next to the other `gradle/libs.versions.toml` rows, not appended at the end of `fork-anchors.txt`. Later commits append their own blocks at the end; an end-of-file row would sit against them and make `git revert` of commit 4 conflict. With the mid-file placement the revert applied with no conflict (rehearsed below). The gate's invariant 3 evaluates each commit against its own copy of the anchors file, so a row may appear in the same commit as, or after, the code it pins: rows for commit 3 land in commit 3, the CLT-02 row in commit 4 [CITED: 34-RESEARCH "Gate invocation"].

### The two fixes (CLT-03)

Current code [VERIFIED: `ClickHouseHelperClient.java:164-215`], the lines that change:
```java
        String url = String.format("%s://%s:%d/%s%s",
                protocol,
                hostname,
                port,
                database,
                tmpJdbcConnectionProperties
        );

        LOGGER.info("ClickHouse URL: {}", url);


        Client.Builder clientBuilder = new Client.Builder()
                .addEndpoint(url)
...
        if (jdbcConnectionProperties != null && !jdbcConnectionProperties.isEmpty()) {
            String props = jdbcConnectionProperties.startsWith("?") ? jdbcConnectionProperties.substring(1) : jdbcConnectionProperties;
            for (String pair : props.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && !kv[0].isEmpty()) {
                    clientBuilder.setOption(kv[0], kv[1]);
                }
            }
        }
```
The fix (applied on a clone; `git diff` is 5 insertions, 1 deletion; the V1 URL builder and the `LOGGER.info("ClickHouse URL: {}", url)` line stay):
```java
    public static final String IGNORE_UNKNOWN_CONFIG_KEY = "ignore_unknown_config_key";   // beside LZ4 (line 82)
...
        String endpoint = String.format("%s://%s:%d", protocol, hostname, port);

        Client.Builder clientBuilder = new Client.Builder()
                .addEndpoint(endpoint)
...
        clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true");

        if (jdbcConnectionProperties != null && !jdbcConnectionProperties.isEmpty()) {
```
The key is a connector-side string constant, not `ClientConfigProperties.IGNORE_UNKNOWN_CONFIG_KEY`, because that constant exists only in 0.11.0 (`ClientConfigProperties.java:322` in the 0.11.0 sources) and would not compile on 0.9.6, which would defeat the one-commit revert. Setting it before the loop means an operator's own `ignore_unknown_config_key=false` in `jdbcConnectionProperties` still wins. Not a hot path (client construction). Anchor rows (counts measured with `grep -cF -e` semantics; the helper's `anchors` mode reported `rows=151 bad=0` after adding them):

| Path | Pattern | Count | ID |
|---|---|---|---|
| `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` | `.addEndpoint(endpoint)` | 1 | CLT-03 |
| same | `.addEndpoint(url)` | 0 | CLT-03 |
| same | `clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true");` | 1 | CLT-03 |

Why the 0.9.6 side is safe [VERIFIED: sources `client-v2-0.9.6-sources.jar` read; runs below]: `Client.Builder.addEndpoint(String)` on 0.9.6 does `new URL(endpoint)` and keeps only protocol, host and port, so the path and query the connector used to pass were silently dropped; passing `protocol://host:port` is byte-for-byte the endpoint 0.9.6 already used. `ClientConfigProperties.parseConfigMap` on 0.9.6 only `LOG.warn`s `Unknown and unmapped config properties`, and forwards just `clickhouse_setting_*` and `http_header_*` keys, so `ignore_unknown_config_key` is one more warned key; the recorded request URI on 0.9.6 with the fix has no `ignore_unknown_config_key` and no `sslmode`.

**Measured red/green matrix** (the same four Docker-free pin classes, 14 tests incl. the catalog pin, run on each tree; `Tests summary` lines):

| Tree | Result |
|---|---|
| 0.9.6, main unfixed | 13 pass, 1 fail = the catalog pin (expected: the catalog is not bumped) |
| 0.9.6, main fixed | identical: 13 pass, 1 fail = catalog pin |
| 0.11.0, main unfixed | 11 pass, **3 fail = the three `ClientEndpointAndOptionsPinUnitTest` tests** |
| 0.11.0, main fixed | 14 pass |

Red shapes on bare 0.11.0 [VERIFIED]: the request goes to `POST /mydb?query_id=...` (path `/mydb`, where 0.9.6 sends `/?query_id=...`); live, V2 `ping()` returns false and `ClickHouseSinkJdbcPropertiesTest` throws `com.clickhouse.client.api.ClientMisconfigurationException: Unknown and unmapped config properties: [ssl, sslmode]` and `[health_check_interval, load_balancing_policy, failover]` at `Client.<init>(Client.java:186)` from `ClickHouseHelperClient.createClientV2(ClickHouseHelperClient.java:213)`. 0.11.0 maps `ssl_mode` (underscore, `ClientConfigProperties.java:135`) but not `sslmode`, and nothing maps `ssl`, so `ssl`/`sslmode` stay never-honoured on V2 on both lines (they were warned about on 0.9.6; V2 TLS comes only from the `https` endpoint scheme, i.e. the connector's `ssl` flag).

### Pattern: one reflection factory makes the 10 sites compile on both lines

```java
// Source: rehearsal /tmp/gsd36/rehearsal (compiled and run on 0.9.6 and on 0.11.0); path src/test/java/com/clickhouse/kafka/connect/sink/util/ServerExceptions.java
public final class ServerExceptions {
    private static final String NO_QUERY_ID = "";
    private ServerExceptions() {}
    public static ServerException create(int code, String message, int transportStatus) {
        try {
            try {   // 0.11.0: only (int, String, int, String)
                return ServerException.class.getConstructor(int.class, String.class, int.class, String.class)
                        .newInstance(code, message, transportStatus, NO_QUERY_ID);
            } catch (NoSuchMethodException noQueryId) {   // 0.9.6: (int, String, int) and (int, String)
                return ServerException.class.getConstructor(int.class, String.class, int.class)
                        .newInstance(code, message, transportStatus);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ServerException has neither known constructor", e);
        }
    }
}
```
[VERIFIED: `javap` of both jars] 0.9.6: `ServerException(int, java.lang.String)` and `ServerException(int, java.lang.String, int)`; 0.11.0: only `ServerException(int, java.lang.String, int, java.lang.String)` plus `getQueryId()`. Call sites are `UtilsTest.java` lines 84, 93, 102, 112, 130, 139, 147, 162 (8), `AdditionalRetriableErrorCodesTest.java:42`, `AdditionalRetriableErrorCodesPinUnitTest.java:85` [VERIFIED: `grep -rn "new ServerException(" src` = 10 lines in 3 files; javac reported exactly these 10]. V1's `ClickHouseException(int, String, ClickHouseNode)` constructors are untouched (`v1ServerError` and the `UtilsTest` `ClickHouseException` sites compile on both). Riding in the catalog commit is the alternative and also yields a one-commit revert, but it makes the catalog commit more than a catalog change; the factory commit keeps commit 4 to one line, one test file and one anchor row.

## Existing tests touching client behaviour, and the pin design (items 3 and 7)

Existing: `UtilsTest` (default-code matrix incl. 159, `ServerException`/`ClickHouseException` retriable, timeouts), `WriterFailureClassificationUnitTest` and `UtilsNullMessageClassificationUnitTest` (the `Read timed out after` / `Write timed out after` prefix rules, typed by hand), `AdditionalRetriableErrorCodes*`, `NetworkCompressionPinUnitTest` (config only) and `NetworkCompressionPinLiveTest` (V2/V1 round trips, CMP-03), `Lz4RoundTripLiveTest`, `ClickHouseSinkJdbcPropertiesTest` (uses `?ssl=false&sslmode=none` and `?load_balancing_policy=random&health_check_interval=5000&failover=2` live on V2: red on bare 0.11.0, but needs Docker), and two stub-HTTP precedents (`DescribeUnreadableEnumMockServerTest`, `ClickHouseReplicaPinningMockServerTest`, both `com.sun.net.httpserver.HttpServer`). None builds a real client against an HTTP error, and none produces the timeout prefixes from the client's own streams.

### Measured error shapes (stub server + real client + real socket; `client.insert(...).get()` then `Utils.handleException`)

`attempts` = requests the stub saw (client default `retry=3`); `noSock` / `sock` = classification with `retryOnSocketException` false / true [VERIFIED: runs on both clones, main fixed]:

| Scenario | 0.9.6 | 0.11.0 |
|---|---|---|
| HTTP 502 | `ClientException("Server returned '502 Bad gateway'. Check network and proxy settings.")`, attempts 1, noSock FAIL, sock FAIL | `ConnectionInitiationException` <- `java.net.ConnectException("Server returned '502 Bad gateway'...")`, attempts 4, noSock FAIL, **sock RETRIABLE** |
| HTTP 503 | `ServerException` code 0, "Code: 0. DB::Exception: <Unreadable error message> (transport error: 503)", attempts 1, FAIL / FAIL | `ConnectionInitiationException` <- `ConnectException("Server returned '503 Service Unavailable'. Check network settings.")`, attempts 4, FAIL / **RETRIABLE** |
| HTTP 418 (unexpected) | `ServerException` code 0 (transport error: 418), attempts 1, FAIL / FAIL | `ClientException("Unexpected result status 418")`, attempts 1, FAIL / FAIL |
| code 159 (500 + `X-ClickHouse-Exception-Code: 159`) | `ServerException` 159, attempts **4**, RETRIABLE / RETRIABLE | `ServerException` 159, attempts **1**, RETRIABLE / RETRIABLE (message now ends ` (queryId= <id>)`) |
| code 252 | attempts 4, RETRIABLE both | attempts 4, RETRIABLE both |
| code 60 | attempts 1, FAIL both | attempts 1, FAIL both |
| socket timeout (`?socket_timeout=300`) | `DataTransferException` <- `SocketTimeoutException`, RETRIABLE both | same |
| connection refused | `ConnectionInitiationException` <- `HttpHostConnectException`, noSock FAIL, sock RETRIABLE, `ping()` false | same |

Reasons, read from the 0.11.0 sources: `HttpAPIClientHelper.doExecuteRequest` maps 502 and 503 onto `java.net.ConnectException` (lines 843-846) and any status outside {200, 407, 502, 503, 400, 401, 403, 500, 404} onto `ClientException("Unexpected result status N")`; 0.9.6 maps only 502 (to `ClientException`) and sends every other `>= 400` through `readError` (`ServerException`). `ServerException.discoverIsRetryable` on 0.11.0 no longer lists 159, and `ClientFaultCause.ServerTimeoutExceeded` exists but is outside the default `client_retry_on_failures` (`NoHttpResponse, ConnectTimeout, ConnectionRequestTimeout, ServerRetryable`), which is why 159 is attempted once [VERIFIED: sources of both jars]. Because `ConnectException` is a `SocketException`, a 502/503 that exhausts the client's 4 attempts is redelivered by Connect only when `retryOnSocketException=true`; with the default it fails the task exactly as it did on 0.9.6.

Live 159 cadence [VERIFIED: real server, `SELECT count() FROM numbers(100000000000)` with `max_execution_time=1`]: 0.9.6 took 4025 ms (4 attempts of about 1 s), 0.11.0 took 1006 ms; both reach `Utils.handleException` as `Code: 159 ... TIMEOUT_EXCEEDED` and become `RetriableException`.

**503 and `ConnectException` fault injection is feasible and cheap: pin it, do not label it "read from the code".** The stub is `com.sun.net.httpserver.HttpServer` on `127.0.0.1:0`, already used twice in this repo; no Docker, no proxy container. Connection refused needs no stub (a closed port).

### Timeout-message prefixes

`Read timed out after` / `Write timed out after` are produced by the V1 piped streams in `clickhouse-data` (`BlockingInputStream.updateBuffer`, `BlockingPipedOutputStream.updateBuffer` and `close` in both jars; text `Read timed out after %d ms`, `Write timed out after %d ms`), not by client V2 [VERIFIED: grep of both source jars; the V1 piece is byte-identical between the lines]. A real producer needs no server: `ClickHouseDataStreamFactory.getInstance().createPipedOutputStream(8, 1, 50)` (buffer 8 bytes, queue 1, timeout 50 ms) read with no writer throws `IOException: Read timed out after 50 ms`; writing 32-byte chunks with no reader throws `Write timed out after 50 ms`, and the stream's own `close()` then throws `Close stream timed out after 50 ms` (the pin must assert that, not use try-with-resources). Both are `RetriableException` through `handleException` [VERIFIED: `ClientTimeoutPrefixPinUnitTest`, 2/2 on both lines].

### Pin classes (names; which are Docker-free)

| Class (package `com.clickhouse.kafka.connect.sink.db.helper` unless noted) | Kind | Tests | Pins | Measured |
|---|---|---|---|---|
| `StubClickHouseServer` | helper (not a test) | n/a | records requests, scripted `status(...)`, `serverException(code, body)`, `ok()` responders | used by the three classes below |
| `ClientErrorShapePinUnitTest` | Docker-free | 7 | rows 502, 503, 418, 159, 252, 60 keyed by client line + `ClientException` never retriable | 7/7 on 0.9.6 and on 0.11.0 |
| `ClientTimeoutPrefixPinUnitTest` | Docker-free | 2 | the two prefixes from real stream classes, retriable | 2/2 both lines |
| `ClientEndpointAndOptionsPinUnitTest` | Docker-free | 3 | request path is `/` and database in `X-ClickHouse-Database`; `?ssl=true&sslmode=strict` builds and neither key reaches the server nor does `ignore_unknown_config_key`; the other V1-only keys of `ClickHouseSinkJdbcPropertiesTest` | green 0.9.6 unfixed/fixed, 0.11.0 fixed; **3 red on bare 0.11.0** |
| `com.clickhouse.kafka.connect.ci.ClientCatalogPinUnitTest` | Docker-free | 2 | the exact marked line, exactly one `clickHouseDriver` line, four modules on the ref | red on 0.9.6 by design; green after commit 4 |
| `Client011BehaviourPinLiveTest` (extends `ClickHouseBase`) | live | 3 | real 159 as `RetriableException`; `?ssl=true&sslmode=strict` pings and answers `version()` on a real server; ZSTD decode keyed by line | 3/3 on both lines |

The error-shape class detects the line from `Client.class.getPackage().getImplementationVersion()`, which reads `client-v2 0.9.6 (revision: 5777f5a)` / `client-v2 0.11.0 (revision: c1834d0)` [VERIFIED: jar manifests], and fails with `client-v2 X has no pinned error-shape row: add a Line` for any other version, so the next bump is forced to review the table. This is what keeps every pin green after a CLT-09 revert without a skip (the 18 named skips must stay exactly 18; no `@Disabled`, no `Assumptions`). `@DisplayName` text is what `36-evidence-check.py failed --expect Class#name` reads, truncated at the first `(`: avoid parentheses in names you intend to `--expect`.

CLT-04's literal wording puts the 502/503 shapes in `Client011BehaviourPinLiveTest`. The Docker-free class carries them (so `testUnit` guards them); the live class should call the same assertion helper for one 502 and one 503 case rather than repeat the table (DRY), so the criterion's class name is honoured without duplicating rows. Planner's call.

**ZSTD live pin.** The server default is ZSTD on this `latest`: with the connector's lz4 setting off and `?compress=true`, `getSetting('network_compression_method')` reads `ZSTD`. 0.11.0 decodes 2000 rows (`method=ZSTD`, and with `?compress=true&clickhouse_setting_network_compression_method=zstd` forced, `method=zstd`); 0.9.6 fails both with `ClientException: Invalid LZ4 magic byte: '-112'` (0x90, the ZSTD frame magic) [VERIFIED: live runs]. With the connector default (lz4 setting on) both lines read `method=lz4` and return the 2000 rows.

**Non-UTC.** The workstation JVM default is already `Europe/Tallinn` (`user.timezone` printed by a probe), CI is UTC. `TZ=<zone> env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` reaches the forked test JVM (`user.timezone=America/Los_Angeles`, `env.TZ` set); `JAVA_TOOL_OPTIONS=-Duser.timezone=<zone>` works too and also affects the Gradle daemon [VERIFIED: `TZPROBE` test]. The date/datetime live pins are `ClickHouseSinkTaskWithSchemaTest` `supportDatesTest`, `supportArrayDateTime64Test`, `supportZonedDatesStringTest`, `supportFormattedDatesStringTest`, `testAvroDateAndTimeTypes` and all of `MetadataFreeJsonTypeParityLiveTest` (14 executions); `ClickHouseSinkTaskWithSchemaProxyTest.supportDatesTest` is the proxy twin. Result: **14/14 green on 0.11.0 under `UTC`, `America/Los_Angeles`, `Asia/Kolkata`, `Europe/Tallinn`, `Pacific/Kiritimati` (+14), `Pacific/Pago_Pago` (-11), and on 0.9.6 under `UTC`, `America/Los_Angeles`, `Asia/Kolkata`.** The Docker-free `MetadataFreeJsonWriterTest.cdcDebeziumDateIsEpochDayUnderAnyTimeZone` already pins three zones with `TimeZone.setDefault`.

## CLT-06: RowBinary byte comparison

**How the connector serialises.** `ClickHouseWriter.doInsertRawBinaryV2` builds a `DataStreamWriter` whose body is `writeRecordsRowBinary(records, table, out, supportDefaults)` and hands it to `client.insert(table.getName(), dataWriter, format, insertSettings)`; `format` is `ClickHouseFormat.RowBinary` or `RowBinaryWithDefaults`. Every value goes through `doWriteCol`/`doWriteColValue`/`doWritePrimitive`, which call `BinaryStreamUtils.write*` from `clickhouse-data` (and the connector's own null-marker and varint logic for Tuple, Map, Array, Variant) [VERIFIED: `ClickHouseWriter.java:1280-1331, 1366-1385` read; `grep` of `BinaryStreamUtils` calls]. Between 0.9.6 and 0.11.0 `BinaryStreamUtils.java` differs only by an added `writeBFloat16` method, and `ClickHouseColumn`/`ClickHouseDataType` add QBit, Geometry and MultiPoint support; the connector's own `Column`/`Type` mapping does not use them [VERIFIED: `diff -u` of both source jars].

**Capture seam.** `CapturingClickHouseWriter` (test source set, already used by `NullableTupleMatrixLiveTest`, `EmptyCdcSkipUnitTest`) runs the real `writeRecordsRowBinary` into a `ByteArrayOutputStream` and records `Capture(route, body, deduplicationToken, supportDefaults)`; it overrides only the two network hooks. DESCRIBE needs ClickHouse, so the harness is live (the same DESCRIBE seam as the Nullable matrices); the bytes depend on no server behaviour.

**Harness shape** (throwaway prototype `Zz36RowBinaryBytesTest`, 50 cases x 2 variants; condensed here because `/tmp` may vanish):
```java
record Case(String name, String ddlType, Schema schema, List<Object> values) {}   // one table `zz36_<case>_<variant>`: (id Int32, c <ddlType> [, d UInt32 DEFAULT 42])
// per case: CREATE TABLE ... ENGINE = MergeTree ORDER BY tuple(); Table t = chc.describeTable(db, name);
//   records = values -> Struct(schema{id, c}) -> new SinkRecord(topic, 0, null, null, valueSchema, struct, i) -> Record.convert(sr, false, ".", db, false)
//   CapturingClickHouseWriter w = new CapturingClickHouseWriter(t, csc);
//   w.doInsert(records, new QueryIdentifier(t.getCleanName(), 0, 0, n - 1, "q"));
//   print  variant|name|route|defaults|len|sha256(body)
```
The "defaults" variant adds a `DEFAULT` column so the table takes the `RowBinaryWithDefaults` route (extra default markers). Element schemas that are optional must be one shared `Schema` instance used both for the array/map and for the `Struct`s put into it, or `Struct.put` throws `Struct schemas do not match`.

**Type matrix (48 serialised cases, both variants):** Int8/16/32/64, UInt8/16/32/64, Float32/64 (incl. NaN, -0.0), Bool, String (empty, unicode, NUL), FixedString(8), UUID, Enum8, Enum16, Date, Date32, DateTime, DateTime64(3/6/9), Decimal(10,2), Decimal(38,10), Nullable(Int32/String/DateTime64(3)/Decimal/UUID), LowCardinality(String), LowCardinality(Nullable(String)), Array(Int32), Array(Nullable(String)), Array(Array(Int32)), Array(Tuple), **Array(Nullable(Tuple(a Int32, b Nullable(String))))**, Map(String,Int32), Map(Int32,String), Map(String,Nullable(String)), Map(String,Array(Int32)), Map(String,Map(String,String)), Map(String,Tuple), **Map(String,Nullable(Tuple(a Int32, b Nullable(String))))**, Tuple, Tuple with nested Tuple and Nullable element, **Nullable(Tuple)**, **Tuple(a Int32, n Nullable(Tuple(d String, e Nullable(Int32))))**, SimpleAggregateFunction(sum, UInt64). Two cases are not serialisable by the connector on either line and throw identical errors (so they are not counted): `DateTime('Europe/Riga')` (`Column [c] has unsupported type [UNKNOWN] for RowBinary serialization`) and a `Variant(Int64, String)` fed a plain String (`cannot convert Kafka value of type STRING ... to ClickHouse type VARIANT`, my schema, not a client effect).

**Result** [VERIFIED: two Gradle runs on two clones, same ClickHouse container image, same TZ]: all 96 serialised lines (48 cases x `plain`/`defaults`) have identical length and SHA-256 on client 0.9.6 and 0.11.0; the 4 error lines are identical too. The SHA-256 of the concatenated result lines (version line excluded) is `c6460e5fedf5b943...` on both. The same run on 0.11.0 under `TZ=UTC` and `TZ=America/Los_Angeles` matches the `Europe/Tallinn` run on 0.9.6 line for line, so the bytes are time-zone independent.

**Wire level.** A stub server recorded the real `client.insert(...)` request for a 1,000,000-byte body with `insert_deduplication_token=topic-0-5-9`: the URI is `/?query_id=q-9&insert_deduplication_token=topic-0-5-9&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4` on both lines, there is no `Content-Encoding`, and the body SHA-256 equals the payload SHA-256 on both (`dc7643e3...`), also with `setCompressionMethod=false` and with `?ssl=true&sslmode=strict`. The only request-header difference is `User-Agent` (see rollout facts). `QueryIdentifier.getDeduplicationToken()` is `String.format("%s-%s-%s-%s", topic, partition, minOffset, maxOffset)` [VERIFIED: `QueryIdentifier.java:56-61`], independent of the client.

**What a `BEFORE_PROCESSING` replay across the upgrade implies.** After a rolling restart the new task reads the state `BEFORE_PROCESSING [min, max]`, rebuilds the same batch and sends the same body under the same token on the same URL; ClickHouse's block deduplication therefore sees what the old task would have sent. **No clean-stop requirement follows from the bytes.** Suggested Phase 38 wording (label: measured): "A batch replayed after the upgrade is byte-identical on the wire to the pre-upgrade attempt for the 48-case type matrix (RowBinary and RowBinaryWithDefaults), including nested Nullable Tuple/Map shapes, carries the same `insert_deduplication_token`, and differs only in the User-Agent header. This holds for the type matrix; a column type outside it was not compared." If the planner wants a re-runnable proof, commit the harness as a live test with a blessed golden file `src/test/resources/rowbinary-golden-sha256.txt` generated on 0.9.6 before the bump (the BLD-16 bless/verify pattern); the cost is that every future writer change needs a re-bless (Open Question 6). Without it the proof lives in the evidence file only.

One more byte-level fact for the rollout note: operators who set `?decompress=true` (client request compression) get LZ4 blocks on 0.9.6 (block magic `0x82`) and **ZSTD blocks on 0.11.0 (`0x90`, `compression.method` defaults to `ZSTD`)**, and on 0.11.0 a 1,000,000-byte incompressible body fails client-side with `ClickHouseException` caused by `com.github.luben.zstd.ZstdException: Destination buffer is too small` (`CompressedBlockOutputStream$ZSTDOutputStream.compressData`, 0.11.0 source line 145). Compressible bodies of 1 B to 100 kB were sent fine on both lines. Not a default-configuration path (the connector never sets `decompress`), so label: measured, operator-set only.

## CLT-05 / D-17: what is measurable on 0.11.0

[VERIFIED: live probes on the real server, 26.10.3.7, both lines]

| Control state | V2, 0.9.6 | V2, 0.11.0 | V1, 0.9.6 | V1, 0.11.0 |
|---|---|---|---|---|
| connector default (`compress=false`, lz4 setting on) | ok, `lz4` | ok, `lz4` | ok | ok |
| `compress=false`, lz4 setting off | ok (server answers `ZSTD`, uncompressed body) | ok | ok, `ZSTD` | ok, `ZSTD` |
| `compress=true`, lz4 setting on | ok, `lz4` | ok, `lz4` | ok, `lz4` | ok, `lz4` |
| `compress=true`, lz4 setting off | **fails** `Invalid LZ4 magic byte: '-112'` | **ok, decodes ZSTD (loads `zstd-jni`)** | **fails** `Magic is not correct - expect [-126] but got [-112]` | **fails, same message** |

So on 0.11.0 the V2 failure that motivated D-17 is gone, but the V1 path still cannot decode ZSTD (the V1 modules are unchanged in the relevant code: 6 files differ in `clickhouse-client`, 6 in `clickhouse-http-client`, 9 in `clickhouse-data`, none a decoder), and with the lz4 setting on the worker never loads a `zstd-jni` native even when an operator sets `compress=true` (below). That is the reworded rationale: D-17 stays because V1 still needs it and because it keeps the zstd natives unloaded; it is no longer what protects V2 reads.

**The three rewording sites** (all text, no assertion changes):
1. The class Javadocs of `NetworkCompressionPinUnitTest` (lines 13-25: "ClickHouse 26.9 defaults the method to ZSTD, which client 0.9.6 cannot decode ...") and `NetworkCompressionPinLiveTest` (lines 30-42: "client 0.9.6 only decodes LZ4"). There is no Javadoc on D-17 in `src/main`: `grep` of `src/main` for `ZSTD|zstd|0.9.6|D-17|Recorded Fact` finds nothing, and the `setCompressionMethod` `ConfigDef` text is `Whether to set network compression method on the ClickHouse client. default: true` (`ClickHouseSinkConfig.java:894-903`). [VERIFIED: grep and reads]
2. `docs/FORK-DELTA.md`: the D-17 row at line 118 (its sentence "Against ClickHouse 26.9 with client `0.9.6`, ..." and the closing "(Recorded Fact 26)"), and the "eighth behaviour" paragraph at line 254. Recorded Fact 26 itself is the dated 0.9.6 probe at `.planning/MERGE-CHECKLIST-v1.7.0.md:108`; leave it as the 0.9.6 record and cite this phase's evidence entry beside it ("measured on 0.9.6 in Recorded Fact 26 and on 0.11.0 in `36-COMMAND-EVIDENCE.md` E-nn"). Also stale after the bump: line 71 ("ClickHouse client `0.9.6` is upstream's own value since v1.7.0 and is no longer a fork override"), line 128 (guava row: "It overrides client-v2 0.9.6's transitive guava `33.4.6-jre` by conflict resolution") and line 129 (httpclient5 row: "Without the pin client-v2 0.9.6 requests httpclient5 `5.4.4`").
3. `docs/ROLLOUT-v1.7.0_gg1.md` lines 238-244 (Item 6, bullet 3: "Against a server whose default is ZSTD (ClickHouse 26.9 and later), at least one of the two controls ... must be in effect, otherwise reads fail with `Invalid LZ4 magic byte`. Do not combine `compress=true` with `setCompressionMethod=false` on such a server."). Add a dated clause: on client 0.9.6 and on V1 of any line that combination fails; on V2 of client 0.11.0 it reads, and loads the zstd native (platforms below). The gg4 precedent corrected the published gg1 and gg2 notes.

## CLT-08: archive listings and the `zstd-jni` finding

**Commands** (the archive is built by `./gradlew createConfluentArchive`, finalised by `verifyConfluentArchive`; the zip is `build/confluent/clickhouse-kafka-connect-<VERSION>.zip`, here `...-v1.7.0_gg5.zip`; `prepareConfluentArchive` also leaves the unzipped tree in `build/confluentArchive/`) [VERIFIED: `build.gradle.kts` task block; runs]:
```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # on the pre-bump tip, then copy the zip to a fresh dir, e.g. /tmp/gsd36/before/
# ... catalog commit ...
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # copy to /tmp/gsd36/after/
printf '# empty\n' > /tmp/gsd36/empty-allowlist.txt
python3 -I .planning/scripts/compare-confluent-archives.py before.zip after.zip /tmp/gsd36/empty-allowlist.txt > 36-ARCHIVE-DIFF.txt   # exit 1 by design: every difference is UNALLOWED
unzip -l after.zip; unzip -l <inner -confluent.jar>    # for the listing files; extract only into fresh empty directories
```
`verifyConfluentArchive` printed `7802 inner entries, no nested jar, no Kafka API class` before and `7933 inner entries, no nested jar, no Kafka API class` after (+131).

**Measured diff** [VERIFIED: the comparison above, built from the same source tree with only the catalog line different]: outer zip 12,456,701 -> 13,558,486 bytes (+1,101,785); inner jar 30,513,286 -> 33,230,479 bytes; the compare script reports `files-a 7400 files-b 7513 only-a 4 only-b 117 changed 94 same 7302`, `MANIFEST.MF: identical`, `nested jars in b: 0`, `kafka classes in b: 0`.
- ONLY-A (4): `client-v2-version.properties`, `com/clickhouse/client/api/internal/ClickHouseLZ4InputStream.class`, `ClickHouseLZ4OutputStream.class`, `LZ4Entity.class` (replaced by `CompressedBlock*` classes).
- ONLY-B (117): the new client-v2 classes (`CompressionMethod`, `Session`, `TransportException`, `observability/*` incl. optional Micrometer and OpenTelemetry recorders, `data_formats/*` JSON readers, `internal/CompressedBlock*`, `ClientUtils`, `CredentialsManager`, `SslContextProvider`, `Versions`, ...), `com/github/luben/zstd/*` (33 classes + 4 in `util`), `META-INF/versions/22/com/github/luben/zstd/*` (17), and **three zstd natives**: `darwin/aarch64/libzstd-jni-1.5.7-23.dylib` (601,336 B), `linux/aarch64/libzstd-jni-1.5.7-23.so` (701,896 B), `linux/amd64/libzstd-jni-1.5.7-23.so` (900,661 B).
- CHANGED (94): client-v2, V1 client, http-client and data classes plus `clickhouse-client-version.properties`.
- Unchanged byte for byte: guava, httpclient5, httpcore5, lz4-java and its natives, commons-compress, RoaringBitmap, jackson, gson (all in the 7302 "same"). `net/jpountz/util/*/liblz4-java.*` natives for 9 platforms are still shipped.

**Why only three natives.** client-v2 0.11.0's POM declares `com.github.luben:zstd-jni:1.5.7-23` with `<classifier>cloud</classifier>` (`client-v2-0.11.0.pom` lines 156-163). The plain `zstd-jni-1.5.7-23.jar` carries 18 platform natives (aix/ppc64; darwin aarch64, x86_64; freebsd amd64, i386; linux aarch64, amd64, arm, i386, loongarch64, mips64, ppc64, ppc64le, riscv64, s390x; win aarch64, amd64, x86); the `cloud` jar carries only linux/amd64, linux/aarch64 and darwin/aarch64 [VERIFIED: `unzip -l` of both cached jars]. A worker on linux/arm, i386, ppc64le, s390x, riscv64, Windows, FreeBSD, AIX or Intel macOS cannot load the native (`zstd-jni` has no pure-Java fallback: `Native.load` throws `UnsatisfiedLinkError: Unsupported OS/arch, cannot find /<os>/<arch>/libzstd-jni-1.5.7-23.<ext>`) [VERIFIED: `Native.java` of 1.5.7-23 read]. Whether the linux/amd64 `.so` runs on musl (Alpine) workers was not tested [ASSUMED]. It matters only when a ZSTD path executes (below).

**Guava: redundant, not lost.** The catalog `guava = "33.7.2-jre"` (`gradle/libs.versions.toml`), `implementation(libs.guava)` (`build.gradle.kts:97`) and `clickhouseDependencies(libs.guava) // temp to override client's with CVE` (`build.gradle.kts:117`) forced 33.7.2-jre over client-v2 0.9.6's `33.4.6-jre`. client-v2 0.11.0 requests `33.7.2-jre` itself. Removing both guava lines on the 0.11.0 clone still resolved `com.google.guava:guava:33.7.2-jre` on `runtimeClasspath` and `clickhouseDependencies` (`dependencyInsight`: `\--- com.clickhouse:client-v2:0.11.0`), and the guava classes in the archive are byte-identical before and after [VERIFIED]. So the CVE-fixed guava (FORK-DELTA records `CVE-2026-102554` / `GHSA-xxph-c9ww-hj94` as affecting guava through 33.7.1) now arrives from the client, and the fork's override is a no-op; it was not removed and nothing regresses. Do not edit `build.gradle.kts` in this phase (the stale comment and the pin stay; record them as a Phase 37/follow-up note). The httpclient5 pin is still effective (`5.6.4 -> 5.6.5`), and so are lz4 (`1.11.4 -> 1.12.1`).

**zstd-jni in a Connect plugin classloader and under noexec `/tmp`** [VERIFIED: probes in the production worker image `confluentinc/cp-kafka-connect-base:7.9.7` (Temurin 17.0.19, Linux/aarch64), the archive's inner jar loaded through the worker's own `org.apache.kafka.connect.runtime.isolation.PluginClassLoader(URL, URL[], ClassLoader)`, a throwaway ClickHouse 26.10.3.7 container on a private Docker network with a random password passed by environment]:

| Scenario | Result |
|---|---|
| default `/tmp`, `compress=true`, lz4 setting off | `Zstd` is loaded by `PluginClassLoader` (child-first, not the worker's `zstd-jni-1.5.6-4.jar`); 2000 rows decoded, server method `ZSTD`; native extracted to a temp file and mapped |
| the worker's own `Zstd` loaded first (AppClassLoader, 1.5.6-4) and then the plugin's | both coexist in one JVM, each with its own native; query ok |
| `/tmp` mounted `noexec` | fails: `ExceptionInInitializerError: java.lang.UnsatisfiedLinkError: /tmp/libzstd-jni-1.5.7-23....so: failed to map segment from shared object` |
| noexec `/tmp` + `-DZstdTempFolder=<exec dir>` | works |
| noexec `/tmp` + `-Djava.io.tmpdir=<exec dir>` | works |
| connector defaults (lz4 setting on, `compress=false`), noexec `/tmp` | ok, `serverMethod=lz4`, no zstd native mapped |
| operator `compress=true` with the lz4 setting on, noexec `/tmp` | ok, `serverMethod=lz4`, no zstd native mapped |

`Native.load` honours the system properties `ZstdNativePath` (load that file, no fallback), `ZstdTempFolder`, and then `java.io.tmpdir` [VERIFIED: `Native.java`]. An `Error` (not an `Exception`) escapes the failing load, so `Utils.getRootCause` returns null for it and the task fails (non-retriable). So the D-17 default also protects noexec-`/tmp` and unsupported-platform workers from a startup-time surprise: the connector never reaches the ZSTD path unless an operator turns on both `compress=true` and `setCompressionMethod=false`, or `decompress=true`.

## V1 client path and socket buffers (item 7)

V1 (`useClientV2=false`, `CLIENT_VERSION=V1`) is gated beyond compilation by two things. First, the `ClickHouseHelperClient` constructor builds both clients (`this.client = createClientV2(); this.server = createClientV1();`, lines 103-105), so V1 mode runs the V2 builder and fails on bare 0.11.0: `ClickHouseSinkJdbcPropertiesTest` with `CLIENT_VERSION=V1` on the unfixed bump failed 2/2 with `Unknown and unmapped config properties`; on the fixed bump `ClickHouseSinkJdbcPropertiesTest`, `NetworkCompressionPinLiveTest`, `ClickHouseSinkTaskSchemalessTest` and `ClickHouseSinkTaskStringTest` under `CLIENT_VERSION=V1` ran 35/35 green [VERIFIED]. Second, V1 behaviour on 0.11.0 is the V1 behaviour of 0.9.6 (module diffs above; the V1 ZSTD decode failure is identical, table above). Add the 35-test V1 command to the phase gate; `tests.yaml` runs V2 only.

Socket buffers (record only; out of scope to change): 0.9.6 defaults `socket_rcvbuf` and `socket_sndbuf` to `804800` bytes each (`ClientConfigProperties.java:68,70` of the 0.9.6 sources); 0.11.0 declares both with no default and `applyIfSet` skips them, so the OS auto-tunes (`ClientConfigProperties.java:78,85`) [VERIFIED: source reads]. Both keys are still accepted through `jdbcConnectionProperties` as the opt-back path. No throughput was measured; the rollout note may claim only "read from the code: the client stopped setting 804800-byte buffers; throughput effect not measured". Other 0.11.0 default changes read from the sources: `compression.method` default `ZSTD` (request compression), TLS keeps default hostname verification and still skips it when a socket SNI is set (the connector's `sslSocketSni` path), `ssl_mode` key added.

## CLT-07: the integration harness and the user-gated nightly dispatch

**Local run** (Phase 31 E-18: `55 tests, 54 succeeded, 0 failed, 1 skipped`, 29.5 min warm, class setup about 95 s; images all local) [CITED: `.planning/milestones/v1.7.0_gg5-ci-phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md` E-18]:
```bash
rm -rf build/test-results/integrationTest build/reports/tests/integrationTest
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest
```
`JAVA_HOME` is the HotSpot Temurin 17 toolchain (avoids the OpenJ9 crash noted in memory); the task depends on `prepareConfluentArchive`, so it exercises the real shaded archive in a real Connect worker (`confluentinc/cp-server-connect:7.9.6`, Java 17). The class has `stockGenWithJdbcPropSingleTaskTest` (`clickhouse_sink_with_jdbc_prop.json`), the Connect-worker twin of the jdbc fix. Time budget for the executor: about 30 minutes, run it in the background and wait for the notification.

A **targeted** Connect-worker run on the fixed 0.11.0 clone, `integrationTest --tests '*ClickHouseSinkConnectorIntegrationTest.stockGen*'`: see "Late results" at the end of this document.

**The nightly dispatch is user-gated; the agents must not push.** Facts: the nightly workflow is on the default branch `custom_build` and active; `workflow_dispatch` runs the workflow file of the ref the user selects, so the run needs a branch holding the Phase 36 code on `origin`. Pushing any branch triggers `tests.yaml` once (a tag push does not), and two branches pushed at one SHA cancel each other's runs, so the pushed SHA must be held by no other branch. The push also publishes the local `.planning` tail one way (the scratch branch holds every local commit).

What the executor prepares, then stops (a `checkpoint:human-action` task; executors refuse relayed approvals, so the stop is orchestrator-inline as in Phase 30):
1. Finish every commit that changes `src`, `build.gradle.kts`, `gradle/` or `.github/`; run the phase gate first. The dispatched SHA must equal the gated head outside `.planning` (`git diff --quiet <gated_head> <sha> -- . ':(exclude).planning'`).
2. Write `36-USER-DISPATCH.md` (no secrets, placeholders for environment values) with: the SHA to push (`git rev-parse HEAD`), proof that no other origin branch holds it (`git ls-remote origin | grep <sha>` empty), `git ls-remote origin refs/heads/custom_build` expected `3a3a15a9...` (and that no Dependabot PR was merged), and the commands below.
3. Ask the user and stop. The user runs (standard `git`/`gh` usage, deliberately not executed during this research; `gh workflow run ... --ref` selecting a branch is [ASSUMED] to dispatch that ref's copy of the workflow, and the Actions UI "Run workflow" branch selector is the equivalent):
   ```bash
   git push origin <sha>:refs/heads/scratch/clt-07-client-0.11.0        # user's direct approval; triggers tests.yaml once
   gh workflow run nightly-integration-tests.yaml -R ne1r0n/clickhouse-kafka-connect --ref scratch/clt-07-client-0.11.0   # or "Run workflow" in the Actions UI, selecting that branch
   gh run list -R ne1r0n/clickhouse-kafka-connect --workflow nightly-integration-tests.yaml --limit 1
   ```
   The job timeout is 85 minutes (75 for the test step); the local run took 29.5 minutes.
4. What the user reports, directly: the run URL and id; the `headSha` of the run (must equal the pushed SHA); the conclusion; start and end times; the totals line of the `Summarise failed tests` step and the `integration-test-results` artifact counts (tests, failures, errors, skipped; expected `55` total with `1` skipped as locally, to be re-derived); the ClickHouse server version the run used (the artifact's logs, or `latest` digest drift note); the conclusion of the `tests.yaml` run on the same push (informational); and, after the run, deletion of the scratch branch by the user (`git push origin --delete scratch/clt-07-client-0.11.0`) or an instruction to keep it until Phase 38.
5. The executor records the report verbatim in `36-COMMAND-EVIDENCE.md` as "user-reported", marks CLT-07 passed only on a green conclusion, and commits docs only.

If the nightly is red, check first whether the failure also occurs on the 0.9.6 pre-bump tip (that needs a second user push); the inherited-red nightly note in memory concerns upstream's `integrationTests.yaml`, not this fork-owned workflow.

## CLT-09: go/no-go mechanics

- **Go.** After commit 6: cache-busted `testUnit` (`rm -rf build/test-results/testUnit` plus `--rerun`; expected count = 1456 + new Docker-free tests, recompute), then **two** `--rerun` full suites, each `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun`, same image digest, `SELECT version()` recorded (`docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'`), `35-evidence-check.py skips` still exactly the 18 named skips, `failed` empty. Expected size: 2047 + the new classes' counts (my design adds 14 Docker-free and 3 live; recompute, do not reuse). Plus the targeted V1 command and the date pins under two non-UTC zones.
- **No-go: the revert recipe, rehearsed.** Rehearsal clone with commits 1-4 and a later live-pin commit: `git revert --no-edit <catalog sha>` applied with **no conflict** and touched exactly three paths (the catalog line, `ClientCatalogPinUnitTest.java` deleted, the one anchor row removed); afterwards the anchors mirror read `rows=152 bad=0` (one fewer than before the revert, 153 -> 152), `dependencies` showed `client-v2:0.9.6`, and 74 targeted tests passed on 0.9.6: `Client011BehaviourPinLiveTest` (live, version-keyed), the three Docker-free pin classes, `UtilsTest`, `AdditionalRetriableErrorCodes*`, `ConnectApiReferencesUnitTest` [VERIFIED: runs].
- **What the revert keeps** (safe on 0.9.6, measured): the `ServerExceptions` factory and the 10 call sites; the two `createClientV2` fixes and their anchor rows; every pin class (they carry a 0.9.6 row); the golden test if committed. **What it removes:** the catalog line, its pin test and its anchor row. **What a no-go must additionally revert or reword:** the docs commit (commit 5), because it states 0.11.0 facts: revert it too (it is one commit, nothing builds on it), and record in the no-go note which Phase 36 rows survive. RLS-02's gg6 note then says the client stayed 0.9.6 and keeps only the items that hold on 0.9.6 (the `ssl`/`sslmode` never-honoured item and the fixes' no-op nature).
- The two fixes are not rolled back by CLT-09 even on no-go, which also means PR #17 can stay open for a later attempt.

## FORK-DELTA, anchors, rollout mechanics (item 10)

- **Anchors.** Format: tab-separated `path TAB fixed-string TAB expected-count TAB backs-ID`; `#` lines are comments; the count is the number of lines containing the string. Current state `rows=148 bad=0` [VERIFIED: `anchors` helper]. This phase adds 4 rows (CLT-03 x3 in commit 3, CLT-02 x1 in commit 4): `rows=152`. The CLT-02 row, verbatim: `gradle/libs.versions.toml	clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)	1	CLT-02`, inserted after the existing `gradle/libs.versions.toml	kafka = "4.4.0"	1	BLD-08` row (line 49) [VERIFIED: file lines 49-50]. The gates: per-commit anchors mirror (`python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py anchors .planning/scripts/fork-anchors.txt`), the tail gate with the Phase 34 values (`PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=<full sha from 34 E-02> CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh <M3> <explicit sha>`, expected `provenance_paths=27 ... anchor_rows=152 ... anchor_exempt=0 result=PASS`), and the cumulative gate once in the background (`bash .planning/scripts/verify-merge-invariants.sh 54e63648 <explicit sha>`, about 25 minutes, Phase 35: `provenance_paths=21 ... anchor_exempt=19 result=PASS`). Always pass explicit SHAs; the gate scripts must stay byte-identical (`git diff --quiet <G> <head> -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh`).
- **BLD-16 re-bless: not needed.** `ConnectApiReferencesUnitTest` was green on the bumped tree with the fixes (82 targeted tests): no `src/main` line of this phase touches a Kafka/Connect API. Re-run it in commit 4's gate; re-bless only if it goes red.
- **FORK-DELTA rows to touch** (section "Build and dependencies", ordered by path): line 71 (catalog pins: add `clickHouseDriver = "0.11.0"` as a fork override of upstream `0.9.6`, with why: the Dependabot PR #17, this phase's pins and CLT evidence, and the two `createClientV2` fixes it needs); a new row in "Connector source" for the `createClientV2` pair (before the first `sink/ClickHouseSinkConfig.java` row ordering rule: by file path, `db/helper/ClickHouseHelperClient.java` sorts with the other helper-client rows) naming the pins and `CLT-03`; line 118 (D-17), 128 (guava: redundant on 0.11.0), 129 (httpclient5 pin text), 254 (paragraph). No test guards the FORK-DELTA table format; format is by convention. Phase 38 (RLS-01) refreshes the raw numbers once.
- **Rollout items for Phase 38** (record file `36-ROLLOUT-ITEMS.md`, placeholders only, each labelled `measured` or `read from the code`): see "Operator-visible effects" below.
- **Spotless.** `spotlessApply` on the whole tree changes 50 tracked files at `6a43dd2d`; never run it repo-wide. New files measured clean (applying Spotless to the new classes left them byte-identical). The factory swap in `AdditionalRetriableErrorCodesPinUnitTest` becomes a new Spotless hunk if the import is left where `ServerException` was; put `import com.clickhouse.kafka.connect.sink.util.ServerExceptions;` after `...sink.helper.SchemalessTestData` and before `...connect.util.QueryIdentifier` (measured: with it there, the file is unchanged by Spotless). `AdditionalRetriableErrorCodesTest.java` and `UtilsTest.java` were already dirty at baseline; keep their Spotless hunk sets unchanged (compare in a detached worktree, as Phase 34 E-09). `ClickHouseHelperClient.java` is already dirty: the 5-line fix must not move any existing hunk.
- **Hot path.** None touched: `createClientV2` runs once per task start. The pins run an in-JVM HTTP server per test; keep ports ephemeral (`127.0.0.1:0`), close the stub in try-with-resources. `ClickHouseHelperClient` is the only production file changed.

## Operator-visible effects (inputs for the Phase 38 note)

| # | Effect | Label | Evidence |
|---|---|---|---|
| 1 | `ssl` and `sslmode` in `jdbcConnectionProperties` never changed V2's transport (V2 TLS follows the `https` scheme from the connector's `ssl` flag); on 0.11.0 they would be fatal without the fix, with it they are ignored with a WARN | measured | stub: never in the request URI; bare-bump red; `ssl_mode` is the 0.11.0 key |
| 2 | 159: no in-client retry on 0.11.0 (1 attempt vs 4; 1006 ms vs 4025 ms against a 1 s `max_execution_time`); still a `RetriableException` | measured | live + stub |
| 3 | 502/503: client-retried 3 times, then `ConnectException`; retriable only with `retryOnSocketException=true` (0.9.6: single attempt, never retriable) | measured | stub |
| 4 | Unexpected HTTP status: `ClientException("Unexpected result status N")` (0.9.6: code-0 `ServerException`), non-retriable both | measured | stub |
| 5 | User-Agent tail: `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5` becomes `clickhouse-kafka-connect/<VERSION> clickhouse-java-v2/0.11.0 (lv:java/17.0.9; os:Mac OS X; hc:ahc_5.6.4)` | measured | stub header capture |
| 6 | Log text changes: `Insert failed (Attempt: 4/4 - Duration: PT0.02S)` becomes `Insert request failed (attempt: 4, duration: 9ms, queryId: <id>)`; server messages gain ` (queryId= <id>)`; a non-ClickHouse HTTP error reads `<body> (transport error: 500)` instead of `Code: 0. DB::Exception: <Unreadable error message> (transport error: 500)` | measured | stub |
| 7 | `jdbcConnectionProperties` keys starting with `custom_` (for example the V1-style `?custom_settings=max_threads%3D3`) are forwarded to the server as settings on 0.11.0 and rejected with `Code: 115 ... UNKNOWN_SETTING` (0.9.6 ignored them with a WARN); not retriable, so the task fails | measured | stub URI + live 26.10.3.7 |
| 8 | `decompress=true`: ZSTD request blocks (0.9.6: LZ4); incompressible bodies fail client-side | measured | stub |
| 9 | zstd natives only for linux/amd64, linux/aarch64, darwin/aarch64; noexec `/tmp` needs `-DZstdTempFolder` or `-Djava.io.tmpdir`; the default configuration never loads them | measured | worker-JVM probes, archive listing |
| 10 | Socket buffers: the client no longer sets 804800-byte buffers; opt back with `socket_rcvbuf`/`socket_sndbuf` | read from the code; throughput not measured | source diff |
| 11 | Exactly-once replay across the upgrade: identical RowBinary bytes and token; no clean stop required | measured | byte comparison |
| 12 | Archive: +131 inner entries, +1.1 MB; guava override redundant | measured | compare script |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---|---|---|---|
| A fake ClickHouse for error shapes | a Mockito mock of `Client` or `ClickHouseHelperClient` | `com.sun.net.httpserver.HttpServer` (two precedents here) | A mock cannot reproduce 0.11.0's 502-to-`ConnectException` mapping or the retry count; also inline-mocking `ClickHouseHelperClient` crashed the OpenJ9 test JVM (see `CapturingClickHouseWriter` Javadoc) |
| Capturing RowBinary bytes | a recording `OutputStream` wrapper in production code | `CapturingClickHouseWriter` | It runs the real serialisers and overrides only the two network hooks |
| Archive diff | `unzip -l` plus `diff` | `.planning/scripts/compare-confluent-archives.py` with an empty allow-list | It hashes every inner entry and normalises the manifest |
| Evidence tallies, red sets, anchors count | ad hoc greps | `36-evidence-check.py` (byte copy of the Phase 35 helper) | Same semantics as Phases 33-35 |
| Spotless impact | `spotlessApply` on the repo | detached worktree + `cmp` on the touched file | 50 tracked files are already dirty |
| ServerException construction | per-site `if` on the client version | `ServerExceptions.create` | One place, compiles on both lines |

**Key insight:** the only production change is five lines in one construction-time method; everything else is observation. The risk is in what the client does differently around that method, which is why the pins observe real sockets and real streams instead of re-typing the exception shapes.

## Common Pitfalls

### Pitfall 1: Trusting PR #17's CI or `testUnit`
**What goes wrong:** PR #17 is red at `compileTestJava` (the 10 sites); once those compile, the Docker-free tests (121 targeted: `UtilsTest`, `AdditionalRetriableErrorCodes*`, `NetworkCompressionPinUnitTest`, `ClickHouseHelperClientForkUnitTest`, both stub-server tests) are green on the bump without the fixes, while 8 of 25 targeted live tests are red (endpoint path, unknown keys).
**How to avoid:** the bare-bump red set is recorded from a throwaway worktree in commit 2's evidence; the gate includes live suites.

### Pitfall 2: Putting the new `ignore_unknown_config_key` key through the 0.11.0-only constant
`ClientConfigProperties.IGNORE_UNKNOWN_CONFIG_KEY` does not exist on 0.9.6; the revert would break compilation. Use the connector's own string constant.

### Pitfall 3: An end-of-file anchor row for CLT-02
A later commit appends a block at the end of `fork-anchors.txt`; reverting the catalog commit then conflicts. Insert beside the other catalog rows.

### Pitfall 4: A pin that skips on the wrong client
Any `@Disabled` or `Assumptions` skip adds to the 18 named skips and fails `skips`. Key expectations by the resolved client line and fail on an unknown line.

### Pitfall 5: `Struct schemas do not match` in the byte harness
Optional element schemas must be one shared instance for the container schema and the `Struct`s.

### Pitfall 6: zsh and tooling traps seen again this session
`echo =====` expands as a command, unmatched globs (`--include=*.java`) abort the command, and plain `gh pr view 17` answers for upstream's repository. Quote globs; pass `-R ne1r0n/clickhouse-kafka-connect`. Docker is colima profile `vz`; only `$HOME` is shared into the VM, so a throwaway directory under `/tmp` mounts empty (use a directory under `$HOME` and remove it). Never run Gradle in two clones with `--rerun` over the same container image while timing-sensitive pins (159 cadence) are measured.

### Pitfall 7: `latest` drift
The image is `26.10.3.7` (digest above). If a full suite goes red on unchanged code, check the digest first, and record it in every evidence block.

## Code Examples

### Version-keyed shape row (core of `ClientErrorShapePinUnitTest`)
```java
// Source: prototype run on 0.9.6 and 0.11.0 (7/7 each)
private record Shape(String deepestCause, int serverCode, int attempts, boolean retriable, boolean retriableOnSocketException) {}
assertShape(
        new Shape("ClientException", -1, 1, false, false),          // client-v2 0.9.6
        new Shape("ConnectException", -1, 4, false, true),          // client-v2 0.11.0
        StubClickHouseServer.status(502, "Bad Gateway"));
// observe(): insert(...).get() against the stub, deepest cause class, stub.requests().size(),
//            Utils.handleException(thrown, false, <retryOnSocketException>, Set.of(), new ArrayList<>())
```

### Catalog pin (`ClientCatalogPinUnitTest`)
```java
private static final String VERSION_LINE = "clickHouseDriver = \"0.11.0\"  # FORK OVERRIDE (upstream 0.9.6)";
assertEquals(1, Files.readAllLines(Path.of("gradle", "libs.versions.toml")).stream().filter(l -> l.equals(VERSION_LINE)).count());
// plus: the four client modules each contain `version.ref = "clickHouseDriver"`
```

### Archive/zstd probe (worker image)
```bash
# compile the probe against the archive jar with a local JDK: javac --release 17; load via the worker's PluginClassLoader
docker run --rm --network <private-net> -e PROBE_CH_PASSWORD=<generated> -v $HOME/<throwaway>:/probe:ro [--tmpfs /tmp:rw,noexec,nosuid] \
  --entrypoint java confluentinc/cp-kafka-connect-base:7.9.7 -cp '/usr/share/java/kafka/*:/probe/classes-main' [-DZstdTempFolder=/ztmp] \
  ProbeMain /probe/plugin/<archive>-confluent.jar /probe/classes <clickhouse-container> 8123 <user> '?compress=true' false
```

## State of the Art

| Old approach (0.9.6) | Current (0.11.0) | Impact |
|---|---|---|
| `addEndpoint(String)` drops path and query | keeps a path | fix 1 |
| unknown config keys warned | fatal unless `ignore_unknown_config_key=true` | fix 2 |
| 159 retried by the client | not retried (`ServerTimeoutExceeded` opt-in cause) | cadence record |
| 502 `ClientException`, 503 code-0 `ServerException` | both `ConnectException`, client-retried | `retryOnSocketException` now matters |
| LZ4-only response and request blocks | LZ4 and ZSTD, request default ZSTD | zstd natives, D-17 rationale |
| 804800-byte socket buffers | OS auto-tuning | record only |
| `custom_` keys ignored | forwarded as server settings | Open Question 1 |

**Deprecated/outdated:** the `build.gradle.kts` comment `// temp to override client's with CVE` and FORK-DELTA's "overrides client-v2 0.9.6's transitive guava" after the bump.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | zstd-jni is legitimate (Maven has no seam verdict; judged from group, POM, source repo, history, and its presence behind kafka-clients already) | Package Legitimacy Audit | Low |
| A2 | The linux/amd64 `cloud` native runs on musl (Alpine) workers | CLT-08 | Medium for operators on Alpine images; not tested |
| A3 | The first nightly run expects the same 55-test shape as Phase 31's local run (54 pass, 1 skipped) | CLT-07 | Low; the executor re-derives from the first local run |
| A4 | A scratch branch named `scratch/clt-07-client-0.11.0` is acceptable to the user; any name works | CLT-07 | None |
| A5 | No upstream clickhouse-java issue exists for the 0.11.0 ZSTD request-compression failure (one web search found none) | CLT-06 | Low; do not state it as unreported |
| A6 | Operators' `jdbcConnectionProperties` contain no `custom_*` keys | Operator effects 7 | Medium: if any do, the bump breaks them unless Open Question 1 is accepted |
| A7 | `gh workflow run nightly-integration-tests.yaml --ref <branch>` runs the branch's copy of the workflow file (Phase 31 E-16 says GitHub dispatches only workflows present on the default branch, which this one is) | CLT-07 | Low: the UI branch selector is the fallback; the user chooses the mechanism |

## Open Questions (RESOLVED)

1. **A third `createClientV2` option for the `custom_` forwarding?**
   - Measured: a one-line `clientBuilder.setOption("custom_settings_prefix", "<unlikely prefix>")` before the loop restores 0.9.6's URL (the `custom_*` keys are ignored again) and makes `?custom_settings=...` and `?custom_myparam=7` pass against the real server on 0.11.0; `custom_settings_prefix` is just another warned key on 0.9.6 (same mechanism as fix 2, not run on 0.9.6 itself) [VERIFIED on 0.11.0; 0.9.6 side by source reading].
   - RESOLVED (planning default, user may override): do not change `createClientV2` beyond the two required fixes (the roadmap names `ClickHouseHelperClient` the bump's only production change and does not list this); record effect 7 in the rollout items with its evidence; add a Docker-free pin that documents the 0.11.0 behaviour only if the user accepts the fix. **Surface this in the plan summary as a user decision**; it is a behaviour change to the production path, and the safe alternative is a rollout warning.
2. **Where do the fixes land relative to the catalog commit?** RESOLVED: before it, as their own commit on 0.9.6 (measured safe), so the revert stays one commit.
3. **How do the 10 `ServerException` sites compile on both lines?** RESOLVED: the reflection factory commit before the pins; riding in the catalog commit is the fallback.
4. **Pin 502/503 or label "read from the code"?** RESOLVED: pin, measured through an in-JVM stub; no Docker.
5. **CLT-06 outcome?** RESOLVED: identical for the 48-case matrix; no clean-stop requirement; wording above.
6. **Commit a golden file for the byte comparison?** RESOLVED (default: yes, `src/test/resources/rowbinary-golden-sha256.txt` blessed on 0.9.6 before commit 4, a live `RowBinaryClientParityLiveTest` verifying it, with a `-D` re-bless switch like BLD-16); fallback: evidence-only if the user prefers no pin on writer output.
7. **What is "the guava CVE override lost"?** RESOLVED: redundant, not lost (measured); FORK-DELTA wording says so.
8. **D-17?** RESOLVED: stays; rewording facts above.
9. **CLT-07?** RESOLVED: executor runs the local harness and stops; the user pushes and dispatches; steps and report fields above.
10. **PR #17 after publication?** RESOLVED: superseded by commit 4; closing it is the user's action (no PR write here); until publication, do not merge PR #17 on origin.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | live pins, byte harness, probes, local `integrationTest` | yes | `colima list`: `vz` Running aarch64, 8 CPU, 16 GiB; only `$HOME` is mounted into the VM | Docker-free pins and the compile gates run without it |
| `clickhouse/clickhouse-server:latest` | live pins, suites | yes | 26.10.3.7, `sha256:739e099abd11...` | none |
| `confluentinc/cp-kafka-connect-base:7.9.7` | worker-JVM zstd probes | yes | Temurin 17.0.19, aarch64 | none needed for the gates |
| `cp-server-connect:7.9.6` + the `ConfluentPlatform` image set | local `integrationTest` | yes (Phase 31 E-18 list) | all local | cold pull adds time |
| Java toolchain | Gradle (OpenJ9 default for tests; HotSpot Temurin 17 at `$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`) | yes | 17 | `-PjavaVersion=21` |
| `gh`, network | origin/PR re-read (read-only) | yes | worked | `git ls-remote` only |
| `python3 -I`, `javap`, `unzip` | helpers, listings | yes | system | none |

**Missing dependencies with no fallback:** none. **Throwaway artifacts of this research** (may disappear): `/tmp/gsd36/{base,bump,bump2,rehearsal}` (clones), `/tmp/gsd36/proto/` (prototype test sources), `/tmp/gsd36/out/` (raw outputs), `/tmp/gsd36/{src96,src110}` (client source jars), `/tmp/gsd36/arch96`, `/tmp/gsd36/arch110` (extracted archives). They are removed at the end of the session.

## Validation Architecture

> `workflow.nyquist_validation` is `false` in `.planning/config.json`; the section is included in brief because the research brief asked for it.

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter), Testcontainers for `@Tag("clickhouse")` (`ClickHouseBase`) |
| Docker-free gate | `rm -rf build/test-results/testUnit && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun` (baseline 1456 / 0 / 0 / 2) |
| Targeted pins | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientCatalogPinUnitTest'` |
| Live | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun --tests '*Client011BehaviourPinLiveTest'` |
| V1 | same with `CLIENT_VERSION=V1` and `--tests '*ClickHouseSinkJdbcPropertiesTest' --tests '*NetworkCompressionPinLiveTest' --tests '*ClickHouseSinkTaskSchemalessTest' --tests '*ClickHouseSinkTaskStringTest'` (35 tests) |
| Non-UTC | prefix any live command with `TZ=America/Los_Angeles` (and a second zone) and select the 6 date selectors |
| Full | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun` twice (baseline 2047 / 0 / 0 / 18) |

| Req | Behaviour | Type | Command | File exists? |
|---|---|---|---|---|
| CLT-01 | error shapes, prefixes | Docker-free | targeted pins | Wave 0 |
| CLT-02 | catalog line | Docker-free | `*ClientCatalogPinUnitTest` | Wave 0 |
| CLT-03 | endpoint, options | Docker-free + live | `*ClientEndpointAndOptionsPinUnitTest`, `*Client011BehaviourPinLiveTest` | Wave 0 |
| CLT-04 | 159, ZSTD, 502/503 | live | live command | Wave 0 |
| CLT-06 | byte parity | live | `*RowBinaryClientParityLiveTest` (if committed) | Wave 0 (optional) |
| CLT-07 | non-UTC, harness, nightly | live + user | TZ commands, `integrationTest`, user dispatch | exists / user |
| CLT-08 | archive | script | `createConfluentArchive` + compare script | exists |
| CLT-09 | revert | git + targeted | revert recipe | n/a |

Wave 0 gaps: `ServerExceptions.java`, `StubClickHouseServer.java`, the four pin classes, `Client011BehaviourPinLiveTest.java`, optional golden test and resource, `36-evidence-check.py`, `36-hygiene-check.py`, `36-COMMAND-EVIDENCE.md`. Sampling: per commit the narrowest `--tests`; per wave cache-busted `testUnit`; phase gate as in CLT-09.

## Security Domain

`security_enforcement` is true (ASVS level 1, block on high).

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2/V3/V4 | no | no change |
| V5 Input Validation | yes | `jdbcConnectionProperties` is operator input: parsed by the existing `split("&")`/`split("=", 2)`; `ignore_unknown_config_key=true` turns config typos into warnings (record it); the `custom_` forwarding is an input-to-server-setting path (Open Question 1) |
| V6 Cryptography | yes | TLS stays the client's: default hostname verification kept (0.11.0 `ssl_mode` default `STRICT`; hostname check skipped only when a socket SNI is set, as 0.9.6); no connector code change |
| V7 Error handling and logging | yes | the new WARN text lists unknown property names; 0.9.6's WARN listed names and values (`{ssl=true, ...}`), 0.11.0 lists names only; the connector logs `ClickHouse URL: <url>` including `jdbcConnectionProperties` as before (existing, unchanged); evidence and the rollout items use placeholders and must not contain a password or token |
| V14 Configuration | yes | a native library (`zstd-jni`) is now in the shipped archive for 3 platforms; loading needs an exec temp folder |

| Threat | STRIDE | Mitigation |
|---|---|---|
| Native library loaded from a world-writable temp folder | Tampering / Elevation | `java.io.tmpdir` on a private exec mount or `-DZstdTempFolder`; default configuration never loads it |
| Credentials in probe or evidence output | Information disclosure | the probe used a random per-run password passed by environment and removed with its container; `36-evidence-check.py secrets FILE` on the evidence; hygiene helper on every committed file |
| Silent acceptance of a mistyped client option | Tampering | the WARN text names the keys; documented in the fork row |
| Server-setting injection through `custom_*` keys | Tampering | Open Question 1 |

## Project Constraints (from CLAUDE.md)

- Do not commit without the user asking and do not change PR titles/descriptions; this research committed nothing and pushed nothing; plans commit on `sync/v1.7.0` under the GSD workflow the user started.
- High-risk files (`Processing`, `KeeperStateProvider`, `ClickHouseWriter`, `ClickHouseSinkConfig`, `RangeContainer`): none is touched. `ClickHouseHelperClient` is the only production file.
- Dependency upgrades need the user's permission: authorised by the milestone ROADMAP and the user's /gsd-autonomous request; disclose in plan text. Do not upgrade anything else (the `zstd-jni` 1.5.7-25 newer than the client's 1.5.7-23 is noticed and not taken).
- Only add tests; never delete or weaken existing ones. The 10-site edit changes construction only, not an assertion. Read back and `assertEquals` for live writes (the byte harness compares hashes; the live pins read data back).
- Behaviour preservation and rebasability: small targeted changes; the fork runs in production.
- Never log secrets or record data; comments only for non-obvious invariants; constants for strings.
- Gradle always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`; GSD verbs use the space form.

## Sources

### Primary (HIGH confidence, read or executed this session)
- Repo files: `gradle/libs.versions.toml`, `build.gradle.kts` (dependencies, `confluentJar`, `createConfluentArchive`, `verifyConfluentArchive`), `ClickHouseHelperClient.java` 1-260, `Utils.java` 1-260, `ClickHouseWriter.java` 1218-1431, `QueryIdentifier.java`, `fork-anchors.txt`, `compare-confluent-archives.py` header, `35-evidence-check.py` header, `35-hygiene-check.py`, the Phase 35 research and evidence, `docs/FORK-DELTA.md` rows, `docs/ROLLOUT-v1.7.0_gg1.md`, `NetworkCompressionPin*Test`, `CapturingClickHouseWriter`, `ClickHouseSinkJdbcPropertiesTest`, the two stub-server tests, `.github/workflows/nightly-integration-tests.yaml`, Phase 31 E-18.
- Client sources (`*-sources.jar` for `client-v2`, `clickhouse-client`, `clickhouse-http-client`, `clickhouse-data`, 0.9.6 and 0.11.0) and `zstd-jni-1.5.7-23-sources.jar` (`Native.java`), read and diffed in throwaway directories; `javap` of both `client-v2` jars; both POMs.
- Executed: dependency reports (4 configurations x 2 versions), `compileJava`/`compileTestFixturesJava`/`compileTestJava`/`compileIntegrationTestJava` on the bare bump, targeted Docker-free and live runs on 0.9.6 and 0.11.0 (error-shape/endpoint/timeout/catalog pins, live pins, jdbc-properties, compression, date pins under six zones, V1 mode, 16 further live classes: 177 tests, 176 passed, 1 skipped on the fixed 0.11.0 clone), the byte harness and wire capture on both lines, archive builds and the compare script, worker-JVM zstd probes, the commit sequence and `git revert` rehearsal, Spotless comparison in detached worktrees, `gh`/`git ls-remote` read-only checks.

### Secondary (MEDIUM)
- Maven Central metadata for `com.clickhouse` artifacts and `com.github.luben:zstd-jni`.

### Tertiary (LOW)
- One web search for an upstream issue on the ZSTD request-compression failure: no hit (A5).

## Metadata

**Confidence breakdown:**
- Standard stack and transitive changes: HIGH (dependency reports, archive diff).
- Architecture and the two fixes: HIGH (red on bare 0.11.0, green on all other trees, revert rehearsed).
- Pins and shapes: HIGH (stub + live, both lines).
- Byte comparison: HIGH for the 48-case matrix; types outside it not compared.
- zstd/worker facts: HIGH for linux/aarch64 Temurin 17 in the production worker image; the musl and non-probed platforms are assumptions.
- Nightly dispatch: MEDIUM (workflow facts verified; the run itself is the user's).

**Research date:** 2026-10-29
**Valid until:** until Maven Central publishes a client newer than 0.11.0 or `origin/custom_build` moves; otherwise 30 days.

## Late results (measured after the sections above were written)

- **Connect-worker harness, targeted, fixed 0.11.0 clone, V2.** `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest --tests '*ClickHouseSinkConnectorIntegrationTest.stockGen*'`: **7 tests, 7 succeeded, 0 failed, 0 skipped, BUILD SUCCESSFUL in 12m 26s** (`stockGenSingleTaskTest`, `stockGenWithJdbcPropSingleTaskTest`, `stockGenSingleTaskSchemalessTest`, `stockGenSingleTaskInterruptTest`, `stockGenMultiTaskInterruptTest`, `stockGenMultiTaskTopicTest`, `stockGenMultiTaskSchemalessTest`). The shaded 0.11.0 archive therefore loaded and ran inside the real worker, including the jdbc-properties connector. The 48 `avroSchemaTest`/`protoSchemaTest` cases were not run; the executor's full local `integrationTest` covers them. [VERIFIED: run this session]
- **Large live classes, fixed 0.11.0 clone, V2.** `ClickHouseSinkTaskWithSchemaTest` + `ClickHouseSinkTaskSchemalessProxyTest` + `ClickHouseSinkTaskWithSchemaProxyTest`: **122 tests, 109 succeeded, 0 failed, 13 skipped**; the 13 skips are inside the named inventory (2 `@Disabled` methods `schemaWithNestedTupleMapArrayAndVariant`, `schemaWithTupleOfMapsWithVariantTest`, and 11 `exactlyOnceStateMismatchTest` cases). [VERIFIED: run this session]
- Together with the 16-class run in "Sources" (177 tests, 176 passed, 1 skipped) this is the targeted evidence that the bump with both fixes passes beyond the pin classes. It does not replace the two full `--rerun` suites, which stay the gate.

## Appendix: prototype sources (compiled and run on both client lines)

These are the throwaway prototypes behind the measurements. Names, `@DisplayName` text and constants may be adjusted; keep the behaviour. Package for the db-helper classes is `com.clickhouse.kafka.connect.sink.db.helper`; `ServerExceptions` is in the body above. The byte harness printed hashes (evidence); the committed form, if the golden file is chosen, compares each case against the blessed resource instead of printing.

### A. `StubClickHouseServer` (helper)
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-JVM HTTP stub for the client pins: records every request and answers each with one scripted
 * response. It speaks just enough of ClickHouse's HTTP interface to drive the real client over a real
 * socket, so no Docker is needed.
 */
final class StubClickHouseServer implements AutoCloseable {

    record Request(String method, String rawUri, Map<String, String> headers, byte[] body) {}

    @FunctionalInterface
    interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    private static final String EXCEPTION_CODE_HEADER = "X-ClickHouse-Exception-Code";

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    StubClickHouseServer(Responder responder) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, values.get(0)));
            requests.add(new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    headers,
                    exchange.getRequestBody().readAllBytes()));
            responder.respond(exchange);
        });
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    List<Request> requests() {
        return requests;
    }

    static Responder status(int status, String body) {
        return exchange -> send(exchange, status, body);
    }

    static Responder serverException(int code, String body) {
        return exchange -> {
            exchange.getResponseHeaders().add(EXCEPTION_CODE_HEADER, String.valueOf(code));
            send(exchange, 500, body);
        };
    }

    static Responder ok() {
        return exchange -> {
            exchange.getResponseHeaders().add("X-ClickHouse-Summary", "{\"written_rows\":\"1\",\"written_bytes\":\"3\"}");
            send(exchange, 200, "");
        };
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
```

### B. `ClientErrorShapePinUnitTest`
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.ClientException;
import com.clickhouse.client.api.ServerException;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.kafka.connect.util.Utils;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin of what the connector sees when the real V2 client meets an HTTP error. One row
 * per client line: a client bump that changes a row fails here and the row is edited on purpose.
 */
class ClientErrorShapePinUnitTest {

    private enum Line { V0_9_6, V0_11_0 }

    private record Shape(String deepestCause, int serverCode, int attempts, boolean retriable, boolean retriableOnSocketException) {}

    private static final Pattern IMPLEMENTATION_VERSION = Pattern.compile("^client-v2 (\\d+\\.\\d+\\.\\d+)");

    private static Line line() {
        String implementation = Client.class.getPackage().getImplementationVersion();
        Matcher m = IMPLEMENTATION_VERSION.matcher(String.valueOf(implementation));
        assertFalse(!m.find(), "unreadable client-v2 Implementation-Version: " + implementation);
        return switch (m.group(1)) {
            case "0.9.6" -> Line.V0_9_6;
            case "0.11.0" -> Line.V0_11_0;
            default -> throw new AssertionError("client-v2 " + m.group(1) + " has no pinned error-shape row: add a Line");
        };
    }

    private static Throwable deepest(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private static boolean classifies(Exception e, boolean retryOnSocketException) {
        try {
            Utils.handleException(e, false, retryOnSocketException, Set.of(), new ArrayList<>());
            return false;
        } catch (RetriableException r) {
            return true;
        } catch (RuntimeException r) {
            return false;
        }
    }

    private static Shape observe(StubClickHouseServer.Responder responder) throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(responder);
                ClickHouseHelperClient chc = new ClickHouseHelperClient.ClickHouseClientBuilder(
                                "127.0.0.1", stub.port(), ClickHouseProxyType.IGNORE, null, -1)
                        .setDatabase("pin")
                        .useClientV2(true)
                        .build()) {
            Exception thrown = null;
            try {
                chc.getClient()
                        .insert("t", new ByteArrayInputStream(new byte[] {1}), ClickHouseFormat.RowBinary, new InsertSettings())
                        .get();
            } catch (Exception e) {
                thrown = e;
            }
            assertNotNull(thrown, "the stub answered an error, so the insert must fail");
            Throwable root = deepest(thrown);
            int code = root instanceof ServerException se ? se.getCode() : -1;
            return new Shape(root.getClass().getSimpleName(), code, stub.requests().size(), classifies(thrown, false), classifies(thrown, true));
        }
    }

    private static void assertShape(Shape v096, Shape v0110, StubClickHouseServer.Responder responder) throws Exception {
        assertEquals(line() == Line.V0_9_6 ? v096 : v0110, observe(responder));
    }

    @Test
    @DisplayName("CLT-01: a 502 is a ClientException on 0.9.6 and a retried ConnectException on 0.11.0")
    void badGateway() throws Exception {
        assertShape(
                new Shape("ClientException", -1, 1, false, false),
                new Shape("ConnectException", -1, 4, false, true),
                StubClickHouseServer.status(502, "Bad Gateway"));
    }

    @Test
    @DisplayName("CLT-01: a 503 is a code-0 ServerException on 0.9.6 and a retried ConnectException on 0.11.0")
    void serviceUnavailable() throws Exception {
        assertShape(
                new Shape("ServerException", 0, 1, false, false),
                new Shape("ConnectException", -1, 4, false, true),
                StubClickHouseServer.status(503, "Service Unavailable"));
    }

    @Test
    @DisplayName("CLT-01: an unexpected status stays non-retriable with and without retryOnSocketException")
    void unexpectedStatus() throws Exception {
        assertShape(
                new Shape("ServerException", 0, 1, false, false),
                new Shape("ClientException", -1, 1, false, false),
                StubClickHouseServer.status(418, "teapot"));
    }

    @Test
    @DisplayName("CLT-01: server error 159 is a RetriableException, retried 3 times by the client on 0.9.6 and not at all on 0.11.0")
    void timeoutExceeded() throws Exception {
        String body = "Code: 159. DB::Exception: Timeout exceeded. (TIMEOUT_EXCEEDED)";
        assertShape(
                new Shape("ServerException", 159, 4, true, true),
                new Shape("ServerException", 159, 1, true, true),
                StubClickHouseServer.serverException(159, body));
    }

    @Test
    @DisplayName("CLT-01: a client-retried retriable code (252) is a RetriableException after 4 attempts on both lines")
    void tooManyParts() throws Exception {
        String body = "Code: 252. DB::Exception: Too many parts. (TOO_MANY_PARTS)";
        Shape both = new Shape("ServerException", 252, 4, true, true);
        assertShape(both, both, StubClickHouseServer.serverException(252, body));
    }

    @Test
    @DisplayName("CLT-01: a non-retriable server code (60) is attempted once and fails on both lines")
    void unknownTable() throws Exception {
        String body = "Code: 60. DB::Exception: Table does not exist. (UNKNOWN_TABLE)";
        Shape both = new Shape("ServerException", 60, 1, false, false);
        assertShape(both, both, StubClickHouseServer.serverException(60, body));
    }

    @Test
    @DisplayName("CLT-01: a ClientException is never retriable")
    void clientExceptionIsNotRetriable() {
        Exception e = new ClientException("unexpected");
        assertFalse(classifies(e, false));
        assertFalse(classifies(e, true));
    }
}
```

### C. `ClientTimeoutPrefixPinUnitTest`
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseDataStreamFactory;
import com.clickhouse.data.ClickHousePipedOutputStream;
import com.clickhouse.kafka.connect.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin of the two IOException message prefixes that {@code Utils.handleException} keys
 * on. The client's piped streams produce them, so the pin builds a real stream with a 50 ms
 * timeout instead of typing the text.
 */
class ClientTimeoutPrefixPinUnitTest {

    private static final int TIMEOUT_MS = 50;
    private static final int BUFFER_BYTES = 8;
    private static final int QUEUE_SLOTS = 1;
    private static final String READ_PREFIX = "Read timed out after";
    private static final String WRITE_PREFIX = "Write timed out after";

    private static void assertRetriable(IOException e) {
        assertThrows(RetriableException.class, () -> Utils.handleException(e, false, false, Set.of(), new ArrayList<>()));
    }

    @Test
    @DisplayName("CLT-01: a starved piped input stream throws an IOException that starts with 'Read timed out after' and is retriable")
    void readTimeoutPrefix() throws Exception {
        try (ClickHousePipedOutputStream stream =
                ClickHouseDataStreamFactory.getInstance().createPipedOutputStream(BUFFER_BYTES, QUEUE_SLOTS, TIMEOUT_MS)) {
            IOException e = assertThrows(IOException.class, () -> stream.getInputStream().read());
            assertTrue(e.getMessage().startsWith(READ_PREFIX), e.getMessage());
            assertRetriable(e);
        }
    }

    @Test
    @DisplayName("CLT-01: a full piped output stream throws an IOException that starts with 'Write timed out after' and is retriable")
    void writeTimeoutPrefix() throws Exception {
        ClickHousePipedOutputStream stream =
                ClickHouseDataStreamFactory.getInstance().createPipedOutputStream(BUFFER_BYTES, QUEUE_SLOTS, TIMEOUT_MS);
        try {
            byte[] chunk = new byte[BUFFER_BYTES * 4];
            IOException e = assertThrows(IOException.class, () -> {
                for (int i = 0; i < 16; i++) {
                    stream.write(chunk);
                }
            });
            assertTrue(e.getMessage().startsWith(WRITE_PREFIX), e.getMessage());
            assertRetriable(e);
        } finally {
            // close() waits on the same full queue and times out with its own message
            assertThrows(IOException.class, stream::close);
        }
    }
}
```

### D. `ClientEndpointAndOptionsPinUnitTest`
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.data.ClickHouseFormat;
import java.io.ByteArrayInputStream;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pins for CLT-03: the V2 client is built with {@code protocol://host:port} only, so the
 * request path stays "/", and {@code jdbcConnectionProperties} keys that only client V1 knows
 * ({@code ssl}, {@code sslmode}) do not stop the client from being built or reach the server.
 */
class ClientEndpointAndOptionsPinUnitTest {

    private static final String DATABASE = "pin_db";
    private static final String V1_ONLY_PROPERTIES = "?ssl=true&sslmode=strict";
    private static final String IGNORE_KEY = "ignore_unknown_config_key";

    private static StubClickHouseServer.Request insertOnce(String jdbcConnectionProperties) throws Exception {
        try (StubClickHouseServer stub = new StubClickHouseServer(StubClickHouseServer.ok());
                ClickHouseHelperClient chc = new ClickHouseHelperClient.ClickHouseClientBuilder(
                                "127.0.0.1", stub.port(), ClickHouseProxyType.IGNORE, null, -1)
                        .setDatabase(DATABASE)
                        .useClientV2(true)
                        .setJdbcConnectionProperties(jdbcConnectionProperties)
                        .build()) {
            chc.getClient()
                    .insert("t", new ByteArrayInputStream(new byte[] {1, 2, 3}), ClickHouseFormat.RowBinary, new InsertSettings())
                    .get();
            assertEquals(1, stub.requests().size());
            return stub.requests().get(0);
        }
    }

    @Test
    @DisplayName("CLT-03: the request path is '/' and the database travels in the X-ClickHouse-Database header")
    void endpointHasNoPath() throws Exception {
        StubClickHouseServer.Request request = insertOnce("");
        assertEquals("/", URI.create(request.rawUri()).getPath());
        assertEquals(DATABASE, request.headers().get("X-ClickHouse-Database"));
    }

    @Test
    @DisplayName("CLT-03: ?ssl=true&sslmode=strict builds a working V2 client and never reaches the server")
    void v1OnlyPropertiesAreAccepted() throws Exception {
        StubClickHouseServer.Request request = insertOnce(V1_ONLY_PROPERTIES);
        assertEquals("/", URI.create(request.rawUri()).getPath());
        assertFalse(request.rawUri().contains("sslmode"), request.rawUri());
        assertFalse(request.rawUri().contains(IGNORE_KEY), request.rawUri());
    }

    @Test
    @DisplayName("CLT-03: V1-only keys from the live jdbcConnectionProperties of the other pins are accepted too")
    void otherV1OnlyKeysAreAccepted() throws Exception {
        StubClickHouseServer.Request request = insertOnce("?load_balancing_policy=random&health_check_interval=5000&failover=2");
        assertTrue(request.rawUri().startsWith("/?"), request.rawUri());
    }
}
```

### E. `ClientCatalogPinUnitTest` (package `com.clickhouse.kafka.connect.ci`)
```java
package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin for CLT-02: the four ClickHouse client artifacts share one version ref, and that
 * ref is the fork's override of upstream's 0.9.6, marked so the next upstream sync sees it.
 */
class ClientCatalogPinUnitTest {

    private static final Path CATALOG = Path.of("gradle", "libs.versions.toml");
    private static final String VERSION_LINE = "clickHouseDriver = \"0.11.0\"  # FORK OVERRIDE (upstream 0.9.6)";
    private static final String REF = "version.ref = \"clickHouseDriver\"";
    private static final List<String> CLIENT_MODULES = List.of(
            "module = \"com.clickhouse:clickhouse-client\"",
            "module = \"com.clickhouse:clickhouse-http-client\"",
            "module = \"com.clickhouse:clickhouse-data\"",
            "module = \"com.clickhouse:client-v2\"");

    @Test
    @DisplayName("CLT-02: clickHouseDriver is 0.11.0 and carries the FORK OVERRIDE (upstream 0.9.6) marker")
    void versionLineIsTheMarkedOverride() throws IOException {
        List<String> lines = Files.readAllLines(CATALOG);
        assertEquals(1, lines.stream().filter(l -> l.equals(VERSION_LINE)).count());
        assertEquals(1, lines.stream().filter(l -> l.startsWith("clickHouseDriver")).count());
    }

    @Test
    @DisplayName("CLT-02: all four client modules ride the one clickHouseDriver ref")
    void everyClientModuleUsesTheRef() throws IOException {
        List<String> lines = Files.readAllLines(CATALOG);
        for (String module : CLIENT_MODULES) {
            assertTrue(lines.stream().anyMatch(l -> l.contains(module) && l.contains(REF)), module);
        }
    }
}
```

### F. `Client011BehaviourPinLiveTest`
```java
package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.QuerySettings;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.util.Utils;
import java.util.ArrayList;
import java.util.Set;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Live pins against the real server (CLT-04). Expectations that differ between the client lines are
 * keyed by the resolved client-v2 version, so the class stays green if the bump is reverted.
 */
class Client011BehaviourPinLiveTest extends ClickHouseBase {

    private static final String ZSTD_ROWS_QUERY =
            "SELECT getSetting('network_compression_method') AS m, number AS n, repeat('x', 100) AS s FROM numbers(2000)";
    private static final int ZSTD_ROWS = 2000;

    private static boolean runsClient011() {
        return String.valueOf(Client.class.getPackage().getImplementationVersion()).startsWith("client-v2 0.11.");
    }

    private ClickHouseHelperClient build(boolean setCompressionMethod, String jdbcConnectionProperties) {
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(getBaseProps());
        return new ClickHouseHelperClient.ClickHouseClientBuilder(
                        csc.getHostname(), csc.getPort(), csc.getProxyType(), csc.getProxyHost(), csc.getProxyPort())
                .setDatabase(csc.getDatabase())
                .setUsername(csc.getUsername())
                .setPassword(csc.getPassword())
                .sslEnable(csc.isSslEnabled())
                .setTimeout(csc.getTimeout())
                .setRetry(csc.getRetry())
                .useClientV2(true)
                .setCompressionMethod(setCompressionMethod)
                .setJdbcConnectionProperties(jdbcConnectionProperties)
                .build();
    }

    @Test
    @DisplayName("CLT-04: a real server error 159 reaches Utils.handleException as a RetriableException")
    void timeoutExceededIsRetriable() throws Exception {
        try (ClickHouseHelperClient chc = build(true, "")) {
            Exception thrown = assertThrows(Exception.class, () -> chc.getClient()
                    .query("SELECT count() FROM numbers(100000000000)", new QuerySettings().serverSetting("max_execution_time", "1"))
                    .get());
            assertThrows(RetriableException.class, () -> Utils.handleException(thrown, false, false, Set.of(), new ArrayList<>()));
        }
    }

    @Test
    @DisplayName("CLT-04: ?ssl=true&sslmode=strict still pings and queries a real server on V2")
    void v1OnlyJdbcPropertiesStillConnect() {
        try (ClickHouseHelperClient chc = build(true, "?ssl=true&sslmode=strict")) {
            assertTrue(chc.ping());
            assertNotNull(chc.version());
        }
    }

    @Test
    @DisplayName("CLT-04: V2 decodes ZSTD responses with setCompressionMethod=false and compress=true on 0.11.0, and fails the known way on 0.9.6")
    void zstdResponseDecodes() throws Exception {
        try (ClickHouseHelperClient chc = build(false, "?compress=true")) {
            if (runsClient011()) {
                try (Records records = chc.queryV2(ZSTD_ROWS_QUERY)) {
                    int rows = 0;
                    for (GenericRecord record : records) {
                        rows++;
                        assertEquals("ZSTD", record.getString("m"));
                    }
                    assertEquals(ZSTD_ROWS, rows);
                }
            } else {
                RuntimeException e = assertThrows(RuntimeException.class, () -> {
                    try (Records records = chc.queryV2(ZSTD_ROWS_QUERY)) {
                        records.forEach(r -> {});
                    }
                });
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                assertTrue(root.getMessage().contains("Invalid LZ4 magic byte"), root.getMessage());
            }
        }
    }
}
```

### G. RowBinary byte harness prototype (`Zz36RowBinaryBytesTest`, package `com.clickhouse.kafka.connect.sink`)
```java
package com.clickhouse.kafka.connect.sink;

import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import org.apache.kafka.connect.data.*;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

/** Throwaway CLT-06 harness: prints one sha256 per (type case, variant) of the exact RowBinary body. */
public class Zz36RowBinaryBytesTest extends ClickHouseBase {
    record Case(String name, String ddlType, Schema schema, List<Object> values) {}

    static final Schema I8 = Schema.INT8_SCHEMA, I16 = Schema.INT16_SCHEMA, I32 = Schema.INT32_SCHEMA, I64 = Schema.INT64_SCHEMA,
            F32 = Schema.FLOAT32_SCHEMA, F64 = Schema.FLOAT64_SCHEMA, BO = Schema.BOOLEAN_SCHEMA, S = Schema.STRING_SCHEMA;
    static Schema opt(Schema s) { return new SchemaBuilder(s.type()).optional().build(); }
    static Schema optLogical(Schema s) { SchemaBuilder b = new SchemaBuilder(s.type()).name(s.name()).optional(); if (s.parameters() != null) b.parameters(s.parameters()); return b.build(); }
    static Schema tupleAB() { return SchemaBuilder.struct().field("a", I32).field("b", S).build(); }
    static Schema tupleABopt() { return SchemaBuilder.struct().field("a", I32).field("b", opt(S)).build(); }
    static Struct ab(int a, String b) { Struct s = new Struct(tupleAB()); s.put("a", a); s.put("b", b); return s; }
    static final Schema NTAB = new SchemaBuilder(Schema.Type.STRUCT).field("a", I32).field("b", opt(S)).optional().build();
    static Struct abo(int a, String b) { Struct s = new Struct(NTAB); s.put("a", a); s.put("b", b); return s; }

    static Case c(String name, String ddl, Schema schema, Object... values) { return new Case(name, ddl, schema, Arrays.asList(values)); }

    static List<Case> cases() {
        List<Case> l = new ArrayList<>();
        l.add(c("int8", "Int8", I8, (byte) -5, (byte) 127, (byte) -128));
        l.add(c("int16", "Int16", I16, (short) -5, (short) 32767, (short) -32768));
        l.add(c("int32", "Int32", I32, -5, Integer.MAX_VALUE, Integer.MIN_VALUE));
        l.add(c("int64", "Int64", I64, -5L, Long.MAX_VALUE, Long.MIN_VALUE));
        l.add(c("uint8", "UInt8", I16, (short) 0, (short) 200, (short) 255));
        l.add(c("uint16", "UInt16", I32, 0, 40000, 65535));
        l.add(c("uint32", "UInt32", I64, 0L, 3000000000L, 4294967295L));
        l.add(c("uint64", "UInt64", I64, 0L, Long.MAX_VALUE, 1234567890123L));
        l.add(c("float32", "Float32", F32, 1.5f, -0.0f, Float.NaN, Float.MAX_VALUE));
        l.add(c("float64", "Float64", F64, 1.5d, -0.0d, Double.NaN, Double.MIN_VALUE));
        l.add(c("bool", "Bool", BO, true, false));
        l.add(c("string", "String", S, "", "hello", "unicodé 中文 😀", "a\u0000b"));
        l.add(c("fixedstring8", "FixedString(8)", S, "abc", "12345678", "é"));
        l.add(c("uuid", "UUID", S, "123e4567-e89b-12d3-a456-426614174000", "00000000-0000-0000-0000-000000000000"));
        l.add(c("enum8", "Enum8('a' = 1, 'b' = 2)", S, "a", "b"));
        l.add(c("enum16", "Enum16('x' = 1000, 'y' = -2)", S, "x", "y"));
        l.add(c("date", "Date", org.apache.kafka.connect.data.Date.SCHEMA, new java.util.Date(0L), new java.util.Date(86400000L * 19000)));
        l.add(c("date32", "Date32", org.apache.kafka.connect.data.Date.SCHEMA, new java.util.Date(-86400000L * 3), new java.util.Date(86400000L * 40000)));
        l.add(c("datetime", "DateTime", Timestamp.SCHEMA, new java.util.Date(0L), new java.util.Date(1700000000123L)));
        l.add(c("datetime_tz", "DateTime('Europe/Riga')", Timestamp.SCHEMA, new java.util.Date(1700000000123L)));
        l.add(c("datetime64_3", "DateTime64(3)", Timestamp.SCHEMA, new java.util.Date(0L), new java.util.Date(1700000000123L), new java.util.Date(-1500L)));
        l.add(c("datetime64_6", "DateTime64(6)", Timestamp.SCHEMA, new java.util.Date(1700000000123L)));
        l.add(c("datetime64_9", "DateTime64(9, 'UTC')", Timestamp.SCHEMA, new java.util.Date(1700000000123L)));
        l.add(c("decimal_10_2", "Decimal(10, 2)", Decimal.schema(2), new BigDecimal("12.34"), new BigDecimal("-0.01")));
        l.add(c("decimal_38_10", "Decimal(38, 10)", Decimal.schema(10), new BigDecimal("1234567890123456789.0123456789")));
        l.add(c("nullable_int32", "Nullable(Int32)", opt(I32), 1, null, 3));
        l.add(c("nullable_string", "Nullable(String)", opt(S), "x", null, ""));
        l.add(c("nullable_dt64", "Nullable(DateTime64(3))", optLogical(Timestamp.SCHEMA), new java.util.Date(5L), null));
        l.add(c("nullable_decimal", "Nullable(Decimal(10, 2))", new SchemaBuilder(Schema.Type.BYTES).name(Decimal.LOGICAL_NAME).parameter(Decimal.SCALE_FIELD, "2").optional().build(), new BigDecimal("1.50"), null));
        l.add(c("nullable_uuid", "Nullable(UUID)", opt(S), "123e4567-e89b-12d3-a456-426614174000", null));
        l.add(c("lowcard_string", "LowCardinality(String)", S, "a", "b", "a"));
        l.add(c("lowcard_nullable_string", "LowCardinality(Nullable(String))", opt(S), "a", null));
        l.add(c("array_int32", "Array(Int32)", SchemaBuilder.array(I32).build(), List.of(1, 2, 3), List.of()));
        l.add(c("array_nullable_string", "Array(Nullable(String))", SchemaBuilder.array(opt(S)).build(), Arrays.asList("a", null, "c"), List.of()));
        l.add(c("array_array_int32", "Array(Array(Int32))", SchemaBuilder.array(SchemaBuilder.array(I32).build()).build(), List.of(List.of(1, 2), List.of()), List.of()));
        l.add(c("array_tuple", "Array(Tuple(a Int32, b String))", SchemaBuilder.array(tupleAB()).build(), List.of(ab(1, "x"), ab(2, "y")), List.of()));
        l.add(c("array_nullable_tuple", "Array(Nullable(Tuple(a Int32, b Nullable(String))))", SchemaBuilder.array(NTAB).build(), Arrays.asList(abo(1, "x"), null, abo(3, null)), List.of()));
        l.add(c("map_string_int32", "Map(String, Int32)", SchemaBuilder.map(S, I32).build(), new LinkedHashMap<>(Map.of("k", 1)), new LinkedHashMap<>()));
        l.add(c("map_int32_string", "Map(Int32, String)", SchemaBuilder.map(I32, S).build(), new LinkedHashMap<>(Map.of(1, "v")), new LinkedHashMap<>()));
        Map<String, String> mns = new LinkedHashMap<>(); mns.put("a", "x"); mns.put("b", null);
        l.add(c("map_string_nullable_string", "Map(String, Nullable(String))", SchemaBuilder.map(S, opt(S)).build(), mns, new LinkedHashMap<>()));
        l.add(c("map_string_array", "Map(String, Array(Int32))", SchemaBuilder.map(S, SchemaBuilder.array(I32).build()).build(), new LinkedHashMap<>(Map.of("k", List.of(1, 2))), new LinkedHashMap<>()));
        l.add(c("map_string_map", "Map(String, Map(String, String))", SchemaBuilder.map(S, SchemaBuilder.map(S, S).build()).build(), new LinkedHashMap<>(Map.of("o", new LinkedHashMap<>(Map.of("i", "v")))), new LinkedHashMap<>()));
        l.add(c("map_string_tuple", "Map(String, Tuple(a Int32, b String))", SchemaBuilder.map(S, tupleAB()).build(), new LinkedHashMap<>(Map.of("k", ab(1, "x"))), new LinkedHashMap<>()));
        Map<String, Struct> mnt = new LinkedHashMap<>(); mnt.put("a", abo(1, "x")); mnt.put("b", null);
        l.add(c("map_string_nullable_tuple", "Map(String, Nullable(Tuple(a Int32, b Nullable(String))))", SchemaBuilder.map(S, NTAB).build(), mnt, new LinkedHashMap<>()));
        l.add(c("tuple", "Tuple(a Int32, b String)", tupleAB(), ab(1, "x"), ab(-2, "")));
        Schema nested = SchemaBuilder.struct().field("a", I32).field("n", SchemaBuilder.struct().field("d", S).field("e", opt(I32)).build()).build();
        Struct n1 = new Struct(nested); n1.put("a", 1); Struct in1 = new Struct(nested.field("n").schema()); in1.put("d", "z"); in1.put("e", 5); n1.put("n", in1);
        Struct n2 = new Struct(nested); n2.put("a", 2); Struct in2 = new Struct(nested.field("n").schema()); in2.put("d", "w"); in2.put("e", null); n2.put("n", in2);
        l.add(c("tuple_nested", "Tuple(a Int32, n Tuple(d String, e Nullable(Int32)))", nested, n1, n2));
        Schema nt = new SchemaBuilder(Schema.Type.STRUCT).field("a", I32).field("b", S).optional().build();
        Struct nts = new Struct(nt); nts.put("a", 7); nts.put("b", "q");
        l.add(c("nullable_tuple", "Nullable(Tuple(a Int32, b String))", nt, nts, null));
        Schema ntn = new SchemaBuilder(Schema.Type.STRUCT).field("a", I32).field("n", new SchemaBuilder(Schema.Type.STRUCT).field("d", S).field("e", opt(I32)).optional().build()).build();
        Struct t1 = new Struct(ntn); t1.put("a", 1); Struct t1n = new Struct(ntn.field("n").schema()); t1n.put("d", "z"); t1n.put("e", null); t1.put("n", t1n);
        Struct t2 = new Struct(ntn); t2.put("a", 2); t2.put("n", null);
        l.add(c("tuple_nullable_tuple", "Tuple(a Int32, n Nullable(Tuple(d String, e Nullable(Int32))))", ntn, t1, t2));
        l.add(c("variant", "Variant(Int64, String)", opt(S), "text", null));
        l.add(c("simple_agg", "SimpleAggregateFunction(sum, UInt64)", I64, 1L, 99L));
        return l;
    }

    static String sha(byte[] b) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte x : MessageDigest.getInstance("SHA-256").digest(b)) sb.append(String.format("%02x", x));
        return sb.toString();
    }
    static String hex(byte[] b, int max) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < Math.min(b.length, max); i++) sb.append(String.format("%02x", b[i])); return sb.toString(); }

    @Test
    void dumpHashes() throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
        System.out.println("BYTES|version|" + com.clickhouse.client.api.Client.class.getPackage().getImplementationVersion());
        try {
            for (boolean withDefaults : new boolean[]{false, true}) {
                for (Case cs : cases()) {
                    String variant = withDefaults ? "defaults" : "plain";
                    String tableName = createTopicName("zz36_" + cs.name() + "_" + variant);
                    ClickHouseTestHelpers.dropTable(chc, tableName);
                    try {
                        ClickHouseTestHelpers.executeQueryIgnoreResult(chc, "CREATE TABLE `" + tableName + "` (id Int32, c " + cs.ddlType() + (withDefaults ? ", d UInt32 DEFAULT 42" : "") + ") ENGINE = MergeTree ORDER BY tuple()");
                        Table table = chc.describeTable(chc.getDatabase(), tableName);
                        Schema value = SchemaBuilder.struct().field("id", I32).field("c", cs.schema()).build();
                        List<Record> records = new ArrayList<>();
                        int i = 0;
                        for (Object v : cs.values()) {
                            Struct st = new Struct(value);
                            st.put("id", i);
                            st.put("c", v);
                            SinkRecord sr = new SinkRecord(tableName, 0, null, null, value, st, i);
                            records.add(Record.convert(sr, false, ".", csc.getDatabase(), false));
                            i++;
                        }
                        CapturingClickHouseWriter w = new CapturingClickHouseWriter(table, csc);
                        w.doInsert(records, new QueryIdentifier(table.getCleanName(), 0, 0, i - 1, "zz36-" + cs.name()));
                        byte[] body = w.captures().get(0).body();
                        System.out.println("BYTES|" + variant + "|" + cs.name() + "|route=" + w.captures().get(0).route() + "|defaults=" + w.captures().get(0).supportDefaults() + "|len=" + body.length + "|sha=" + sha(body) + "|head=" + hex(body, 24));
                    } catch (Throwable t) {
                        Throwable r = t; while (r.getCause() != null) r = r.getCause();
                        System.out.println("BYTES|" + variant + "|" + cs.name() + "|ERR|" + t.getClass().getSimpleName() + "|" + String.valueOf(t.getMessage()).replace('\n', ' ').substring(0, Math.min(160, String.valueOf(t.getMessage()).length())) + "|root=" + r.getClass().getSimpleName());
                    } finally {
                        ClickHouseTestHelpers.dropTable(chc, tableName);
                    }
                }
            }
        } finally {
            chc.close();
        }
    }
}
```

