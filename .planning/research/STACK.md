# Technology Stack

**Project:** ClickHouse Kafka Connect `custom_build` fork, milestone `v1.7.0_gg7-maintenance`
**Researched:** 2026-11-01
**Mode:** Ecosystem, subsequent milestone (only what the NEW work needs)
**Overall confidence:** HIGH. Client behaviour was read from the `v0.11.0` source tag, the advisory and releases came from the GitHub API, and the fork code was read in the working tree.

## Bottom Line

**This milestone adds no new library, plugin or tool.** Two version bumps (gson 2.15.0, `setup-gradle` 6.4.1) are the only stack changes. F-1 and F-2 are fixable with APIs already on the classpath (`ClickHouseHelperClient.close()`, the client's `socket_timeout` option). Docker-free pins reuse `StubClickHouseServer`, which is plain JDK `com.sun.net.httpserver` and already in `src/test`. Do NOT add WireMock, MockWebServer, Awaitility, Toxiproxy or a new JaCoCo/Mockito plugin.

## Recommended Stack Changes

### Dependency and action bumps

| Technology | From -> To | Purpose | Why / risk verdict |
|------------|-----------|---------|--------------------|
| `com.google.code.gson:gson` | 2.14.0 -> **2.15.0** (Maven Central `latest`/`release`, published 2026-10-24) | JSON serialization in `DataJson.GSON` and the `ConvertToMapOrJson` SMT (`jsonLibrary=gson`) | **Take it, one-line catalog change (`gradle/libs.versions.toml` is the only file in PR #22). Risk LOW.** See the behaviour table below. |
| `gradle/actions/setup-gradle` | 6.4.0 -> **6.4.1**, pin to commit `8da46b539cd8fe6ab27ca75128919a18d757604c` | Gradle setup and cache in 4 workflows | **Take it. Risk LOW.** 6.4.1 is a security patch release with no input or behaviour changes. Pin the **commit** SHA, not the annotated-tag object `7296c772…`. |

#### gson 2.15.0 against this fork

The fork only **serializes** (`toJson` of `Map`, `Struct`, `Data`, with custom `Date`/`Timestamp` adapters and `disableHtmlEscaping`). There is no `fromJson` anywhere in `src/main` or the tests. `fromJsonRow` in `ClickHouseFieldDescriptor` is a Jackson method with a similar name, unrelated to gson.

| 2.15.0 change | Reaches the fork? | Verdict |
|---------------|-------------------|---------|
| Default `Map` for untyped deserialization is now `LinkedHashMap` (#3057) | No, read side only | None |
| `JsonReader` and `JsonTreeReader` `NumberFormatException` messages, BOM detection, `peekNumber`, non-ASCII digits, strict rejection of unpaired surrogates (#3116, `Strictness.STRICT` and `JsonReader` only) | No, read side only | None |
| `InetAddress`, `URI` adapters, `JsonPrimitive.hashCode`, `LazilyParsedNumber` is `Comparable`, wildcard types use the runtime type (#3112), `BoundField` accessibility skip, `JsonArray.removeIf` | Not on the paths used | Negligible. #3112 changes reflective type resolution only for wildcard-typed fields, and the fork serializes `Map<String,Object>` and custom-adapter types |
| New `JsonReader.getCharacterOffset` | No | None |

- No Java-baseline change found (the artifact stays a Java 8 multi-release/ModiTect jar). The compile toolchain stays JDK 17.
- The Confluent archive allow-list is path-pattern based (`com/google/gson/*`, `META-INF/maven/com.google.code.gson/gson/*`) and the gg6 file has no gson row. The gg4 and "25" files carry version text only in comments (`gson 2.13.1 -> 2.14.0`). **Integration point:** the new `confluent-archive-allowlist-v1.7.0_gg7.txt` should add (or inherit) the two gson rows with an `2.14.0 -> 2.15.0` comment. The release-time archive compare will show gson content change vs the gg6 archive.
- **Docs follow-through:** `docs/FORK-DELTA.md` row 72 names gson `2.14.0`. It must read `2.15.0` after the merge. gson is in `implementation`, `clickhouseDependencies` and `testFixturesImplementation` (`build.gradle.kts` lines 95, 118, 158), and the catalog change reaches all three.
- **Suite gate (as the milestone plans):** `testUnit` plus the full suite. The gson-specific tests are `ConvertToMapOrJson*`, `JsonSerializationEquivalenceTest`, `ClickHouseSinkTaskJsonInsertFormatTest` and `ClickHouseSinkTaskStringTest`. `JsonSerializationEquivalenceTest` is the closest guard for byte-identical serialization.

#### setup-gradle 6.4.1 against this fork

- Release notes: "Security fix ... `dependency-submission` ... GHSA-chpw-2mqm-95cc. All users should upgrade", plus "combined dependency updates" (#1105) and "latest Dependency Graph plugin" (#1106). No breaking changes, no new inputs, no Node or runner requirement changes.
- **The advisory does not affect this fork.** GHSA-chpw-2mqm-95cc (high, CVSS 8.1) is the `dependency-graph` `download-and-submit` mode used in a `workflow_run` workflow for fork PRs. No fork workflow uses `dependency-graph`, `dependency-submission` or `download-and-submit`. The bump is routine hygiene and the PR description should not claim a vulnerability was closed. Record this in the PR #21 disposition.
- **SHA re-pin, as for the earlier `actions/setup-java` bump.** The tag `v6.4.1` is an annotated tag, and `gh api .../git/ref/tags/v6.4.1` returns the tag object `7296c772…`. The commit to pin is `8da46b539cd8fe6ab27ca75128919a18d757604c`. That is the commit Dependabot's compare shows, and `…/git/tags/7296c772…` dereferences to it. v6.4.0 was `3f5f9adaf7d9fecd50b5935e54106014257a94e6` (a tag object `b9bee63e…` also exists, so the same trap applies).
- PR #21 touches exactly four files: `tests.yaml`, `release.yaml`, `pre-release.yaml` and `nightly-integration-tests.yaml`. Each carries one `uses: gradle/actions/setup-gradle@<sha> # v6.4.0` line, so the SHA and the trailing comment must both change.
- **Test pin impact:** `WorkflowActionPinGuardUnitTest` (line 233) and `TestsWorkflowPinGuardUnitTest` (line 709) contain the literal `# v6.4.0` in synthetic fixtures. Check whether they read the real workflow files. If they only build synthetic YAML they need no edit. Do not rewrite them to track the bump unless they fail.
- The `github-actions` Dependabot group is already configured (`.github/dependabot.yml`). The PR comes from that group, and the `integrationTests.yaml` exclusion stays untouched.

## Client 0.11.0 Lifecycle and Timeouts (F-1, F-2)

Source: `ClickHouse/clickhouse-java` tag `v0.11.0` (`Client.java`, `HttpAPIClientHelper.java`, `ClientConfigProperties.java`, V1 `ClickHouseClient.java` and `ClickHouseClientOption.java`). Confidence HIGH. Everything below was read from source, not documentation prose.

### F-1: close semantics

| Fact | Detail | Implication |
|------|--------|-------------|
| V2 `Client.close()` | Calls `shutdownNow()` on the operation executor **only if the client owns it** (created when `async=true` and no shared executor was supplied). Then `httpClient.close(CloseMode.IMMEDIATE)` on the Apache HC5 client. Exceptions from the executor shutdown are logged, not thrown. | Safe in `stop()`. IMMEDIATE aborts any in-flight request, so close only after the refresher has stopped and no `put` is running (Connect guarantees `put` and `stop` do not overlap). |
| Default pool | `max_open_connections=10`, `connection_ttl=-1`. There is no finaliser or shutdown hook. | An unclosed client keeps its pool and idle sockets for the life of the worker. The leak is real. |
| `ClickHouseHelperClient.close()` | Exists, `AutoCloseable`, null-safe, closes the V2 `client` only. The V1 path builds and try-with-resources-closes a `ClickHouseClient` per call (`pingV1`, query helpers). | `ClickHouseWriter.stop()` needs one call after the scheduled-executor shutdown, and the field should be nulled. |
| **Constructor builds a V2 client unconditionally** | `ClickHouseHelperClient(...)` runs `this.client = createClientV2(); this.server = createClientV1();` regardless of `useClientV2` ("We are creating two clients"). | The leak is **not limited to client V2 deployments**. A V1-selected connector also builds, and leaks, a V2 client per helper. |
| **Wider leak sites (extend F-1's scope, or record them as sibling items)** | (a) `ClickHouseWriter.getMutationRequest` (V1 insert path, 4 call sites) builds a **new** `ClickHouseHelperClient` (`chcTmp`) on every call to read `getServer()` and never closes it, so a per-insert leak with `clientVersion=V1`. (b) `KeeperStateProvider` builds its own `chc` (line 50) and neither it nor `StateProvider` has a close, so a per-task leak in exactly-once mode. (c) `ClickHouseWriter.start()` returns `false` after a failed ping or version check without closing `chc`. (d) A second `start()` would overwrite `chc` without closing the old one. | The Phase 36 text names only `ClickHouseWriter.stop()`. Planning should decide (a) to (d) explicitly. (a) can be fixed by reading the node from the existing `chc.getServer()` (the same host, with the `database` the only difference, and the V1 node is built per database) or by try-with-resources on `chcTmp`. Upstream `main` has the same leaks, so every fix is a fork-only delta to record in FORK-DELTA and the anchors file. |
| Upstream | `upstream/main` `ClickHouseWriter.stop()` shuts the executor only and also leaves `chcTmp` open. | No upstream fix to cherry-pick. Keep the change small and additive for rebasability. |

**Recommended F-1 implementation:** in `ClickHouseWriter.stop()`, after the executor block and in its own try/catch, call `chc.close()` (log, never throw) and set `chc = null`. Make `start()` close `chc` on its `return false` paths. Handle the V1 `chcTmp` per-insert leak (a) in the same plan if the reviewer accepts the scope. `ClickHouseWriter.java` is a high-risk file, so a full suite is required.

**Docker-free pin for F-1:** reuse `StubClickHouseServer`. The V2 client keeps pooled keep-alive sockets after a successful ping, and `CloseMode.IMMEDIATE` closes them, so the stub can observe the socket close. As a second observable, a query on the helper's `Client` after `stop()` fails (HC5 closed-client `IllegalStateException`, wrapped by the client). Verify empirically which one is stable before fixing the assertion shape. The V2 `Client` exposes no `isClosed` or open-client counter, so "count open clients" as worded in 36-FOLLOWUPS has no public hook. The alternative is a small package-private seam in the writer (a supplier for the helper) with a counting subclass. Prefer the stub-socket approach, which needs no production change beyond the fix.

### F-2: why the 4-minute timeout never fires, and what bounds a V2 insert

| Fact | Detail | Implication |
|------|--------|-------------|
| `async` default | `ClientConfigProperties.ASYNC_OPERATIONS = "async"`, default `"false"`. `runAsyncOperation` returns `CompletableFuture.completedFuture(resultSupplier.get())`, so the HTTP call runs on the **caller thread** before `insert(...)` returns. | Confirms F-2: `future.get(timeoutMs)` on an already-completed future cannot time out. Only the client's own timeouts can bound the call. |
| `socket_timeout` | `SOCKET_OPERATION_TIMEOUT = "socket_timeout"`, `Integer`, **default 0 (unbounded)**. Applied twice: the pool `SocketConfig` `setSoTimeout` (client-level) and per request via `createRequestHttpContext` -> `RequestConfig.setResponseTimeout(...)`, read from the **request's merged config**. | A per-request override works: `insertSettings.setOption("socket_timeout", ms)` bounds the insert only, and leaves ping, DESCRIBE and queries untouched. The client-level key (also reachable through `jdbcConnectionProperties`) would affect everything. |
| Semantics | Inactivity per read or write, not wall-clock. The server sends the HTTP response only after the insert completes (no progress headers by default), so for a stalled or very slow server the wait for the response header trips it. | Setting it to `clickhouseClientInsertTimeoutMs` is a faithful "no response within N ms" bound. The existing validation that `clickhouseClientInsertTimeoutMs < max.poll.interval.ms` already reasons about this value. |
| `max_execution_time` / `Builder.setExecutionTimeout` | `MAX_EXECUTION_TIME = "max_execution_time"`, default 0. It is the query-timeout hint sent to the **server** and the value used by `ping`/`query` wait logic (`getOperationTimeout()`), not a client-side deadline on an insert. | Not a fix for F-2. Do not use it to bound the insert. |
| `async=true` as the fix | Would make `future.get(timeout)` throw, but **`CompletableFuture.cancel(true)` does not interrupt the worker thread** (the flag is ignored), and a blocked HC5 socket read is not interruptible anyway. The client creates `newCachedThreadPool("chc-operation")`, so a stalled server leaks one live thread and one held connection per timed-out batch until the OS times out. | **Reject.** It converts "blocked call" into "blocked call plus leaked threads" and changes threading on the hot path. |
| Retry interaction | `SocketTimeout` is **not** in the default `client_retry_on_failures` (`NoHttpResponse, ConnectTimeout, ConnectionRequestTimeout, ServerRetryable`), so a read timeout is not retried inside the client and surfaces immediately. `Utils.handleException` already maps a root-cause `SocketTimeoutException` to `RetriableException` (line 112), which Connect retries, and exactly-once replays the same block under the same dedup token. | A V2 socket timeout is already classified retriable end to end. The new pin only has to prove it with a real client against a stall. |
| 0.11.0 breaking item, adjacent | Server error 159 (`TIMEOUT_EXCEEDED`) is no longer retried by default (`ServerTimeoutExceeded`). | Unrelated to the client-side stall, but do not conflate it in the pin or in the rollout text. |

**Recommended F-2 behaviour (decision to make in discuss-phase, with this as the research default):** keep `awaitInsertResponse` as is (it is correct for V1 and for `async=true`), and on V2 additionally set the per-insert option `socket_timeout = clickhouseClientInsertTimeoutMs` on `InsertSettings` at the three V2 insert sites (`ClickHouseWriter` around lines 1298, 1574 and 1722, where `insertSettings` already exists). Cast to `int` with a guard (the value is a `long`, the option is `Integer`, and `Integer.MAX_VALUE` about 24.8 days is an adequate "unbounded"). Leave the client-level default of 0 so ping and describe are unchanged. The change is additive and uses no new API. The option key is a literal in the V2 builder path (`ClientConfigProperties.SOCKET_OPERATION_TIMEOUT.getKey()` is the constant to use, not the string).

**Behaviour-preservation note:** this turns an existing "blocks forever" into a retriable failure after 4 minutes by default. An operator can legitimately have inserts longer than 4 minutes, and with `async=false` those currently succeed. The change makes them fail and retry, and exactly-once dedup then needs the earlier attempt to have landed. It needs a rollout-note entry and probably a default review (the milestone treats "pin current behaviour, fix genuine bugs" as the rule). Flag as the highest-risk decision of the milestone.

**V1 path:** `ClickHouseClient.execute` returns a real `supplyAsync(..., getExecutorService())` future when async, and a `completedFuture` otherwise (`ClickHouseClient` lines 289 to 290). V1 `socket_timeout` defaults to `ClickHouseDataConfig.DEFAULT_TIMEOUT` and `connect_timeout` to 5000 ms. The connector's V1 insert goes through `request.execute()`, whose future is genuinely asynchronous, so `awaitInsertResponse` does work for V1. I did not test V1 against a stall in this pass. Treat "V1 is already bounded" as MEDIUM confidence and confirm with the same stub test, parameterised on client version (V1 is non-default, so low priority).

**Docker-free pin for F-2:** extend `StubClickHouseServer` with a responder that accepts the request and then blocks (the `Responder` functional interface already exists at line 25). Build a real `ClickHouseHelperClient` against it, run the V2 insert path with a short `clickhouseClientInsertTimeoutMs` (for example 300 ms), and assert a `RetriableException` within a bounded wall time, with a JUnit `assertTimeoutPreemptively` wrapper as the safety net. `ClientTimeoutPrefixPinUnitTest` stays the sibling pin (it covers the V1 piped-stream message prefixes). The stub server uses `com.sun.net.httpserver` from the JDK, so no dependency is added.

### F-3 and the `custom_*` forwarding pin (IN-04)

- **F-3** is test-harness code (`ConfluentPlatform` log-text counting across `getConnectors()`), with no library involved. The existing `generateData` fix is the template: count one connector's task status and delete the connector in `finally`. No stack change.
- **IN-04 (`custom_*` key forwarding on 0.11.0):** `createClientV2` splits `jdbcConnectionProperties` and forwards every `key=value` through `clientBuilder.setOption(k, v)`. In 0.11.0, `custom_*` keys are rejected at ping on V2 (the one upgrade-breaking item in the gg6 note), and `IGNORE_UNKNOWN_CONFIG_KEY` is set to `"true"` by the fork but, per `ClientConfigProperties` line 459, only turns the "Unknown and unmapped config properties" exception into a WARN. A Docker-free pin can build the V2 `Client` against `StubClickHouseServer`, capture the request URL or query string the stub sees, and assert on what is (or is not) forwarded. The stub already records `exchange.getRequestURI()`. No new tooling is needed.

## What NOT to Add

| Avoid | Why | Use instead |
|-------|-----|-------------|
| WireMock, MockWebServer (okhttp is test-only for the harness, and `mockwebserver` is a separate artifact), Toxiproxy | The repo already has `StubClickHouseServer` on the JDK `HttpServer`. Another stub stack is a new test dependency with a Shadow and archive-compare surface for no gain. | Extend `StubClickHouseServer` with a stall responder |
| `useAsyncRequests(true)` / `async=true` as the F-2 fix | A timed-out `CompletableFuture` leaves the worker thread running and the socket held, and `cancel(true)` does not interrupt (see above). | Per-insert `socket_timeout` |
| `Builder.setExecutionTimeout` / `max_execution_time` for F-2 | Server-side query limit, not a client deadline | `socket_timeout` |
| Client-level `socket_timeout` default | Changes ping, DESCRIBE and all queries, and silently affects every deployment | Per-insert `InsertSettings` option |
| Client upgrade past 0.11.0 | Out of scope per PROJECT.md | Stay on 0.11.0 |
| gson major or any unrelated Dependabot PR | Not in scope. Protobuf 4.x is still blocked by the fork's Dependabot ignore rule. | Take PRs #21 and #22 only |
| Floating tags (`@v6`, `@v6.4.1`) for `setup-gradle` | Breaks `WorkflowActionPinGuardUnitTest` and the supply-chain stance | Commit SHA plus `# v6.4.1` comment |
| Mockito inline mocking for the new `ClickHouseWriter` pins | Intermittent OpenJ9 crash, and the writer tests already moved off it in gg3 | Real objects against `StubClickHouseServer`, or `InMemoryDBWriter` for state-machine tests |

## Other Work Items: Stack Implications

| Item | Stack impact |
|------|--------------|
| `Processing.doLogic` wrapped-`DataException` case | None. JUnit 5 plus `InMemoryDBWriter` and `InMemoryState` (do not mock `ClickHouseWriter`, per AGENTS.md). |
| FUT-05 (`additionalRetriableErrorCodes` lists only transient codes) | None. A docs and validation decision in `ClickHouseSinkConfig` (high-risk file) and the rollout note. Default retriable set: `Utils.DEFAULT_RETRIABLE_ERROR_CODES`. |
| OI-35-1..3 | None expected (test and doc advisories). |
| Phase 37 dispositions (`increment-version.yml`, allow-list globs) | None. Workflow comments and `.planning/scripts` allow-list rows. The pin guard classes are existing tests. |
| Release `v1.7.0_gg7` | New `confluent-archive-allowlist-v1.7.0_gg7.txt` (gson rows), `VERSION`, CHANGELOG, FORK-DELTA row 72 (gson version), rollout note, MBean `version=` segment and client name `v1.7.0_gg7`. |

## Version Compatibility

| Package | Compatible with | Notes |
|---------|-----------------|-------|
| gson 2.15.0 | JDK 17 toolchain, Jackson 2.22.4, Kafka Connect API 4.4.0 compile-only | Serialize-only use. gson 2.15.0 is already latest on Maven Central. |
| `setup-gradle` 6.4.1 (`8da46b53…`) | Gradle wrapper 9.x, `ubuntu-24.04` runner, the fork's SHA-pin guard | Same inputs as 6.4.0. |
| client 0.11.0 (`client-v2`, `clickhouse-client`, `clickhouse-http-client`) | ClickHouse `latest` (26.10.3.7 at gg6) | Stays. `socket_timeout` is an existing option in 0.11.0, and no 0.11.0 breaking item changes it. |

## Installation

```bash
# gson (PR #22; one-line catalog change)
#   gradle/libs.versions.toml:  gson = "2.15.0"

# setup-gradle (PR #21; four workflow files)
#   uses: gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1

# Verify the pin resolves to a commit, not a tag object
gh api repos/gradle/actions/git/ref/tags/v6.4.1 --jq .object          # tag object 7296c772...
gh api repos/gradle/actions/git/tags/7296c772c13d12d9871528814b51fb2143c138ef --jq .object   # commit 8da46b53...
```

No other dependencies to install.

## Roadmap Implications

1. **Dependabot first, one commit each.** Both are low risk and independent. Gate gson with `testUnit` plus the full suite. Gate `setup-gradle` with the pin guard tests and one CI run (the action only runs on CI, so a local suite cannot prove it).
2. **F-1 and F-2 as separate plans, tests first.** Both touch the high-risk `ClickHouseWriter.java`. F-1's scope question (the V1 `chcTmp` per-insert leak, the `KeeperStateProvider` leak, the `start()` failure paths) needs a discuss-phase decision. F-2 needs a user decision on default behaviour (see the behaviour-preservation note).
3. **Both pins reuse `StubClickHouseServer`**, so no Docker and no new dependency. The only test-fixture change is a stall responder.
4. **Release docs** must carry: gson `2.15.0`, `setup-gradle` `6.4.1` with the "advisory does not apply" statement, the F-2 behaviour change (if taken), and the F-1 leak fix (operators on workers with frequent rebalances will see fewer open connections).

## Sources

- GitHub API, `google/gson` release `gson-parent-2.15.0` (published 2026-10-24) and PRs #3057, #3116 (reader-only, strict-mode-only) — HIGH
- Maven Central `maven-metadata.xml` for `com.google.code.gson:gson` (`latest`/`release` = 2.15.0) — HIGH
- GitHub release `gradle/actions` v6.4.1, advisory GHSA-chpw-2mqm-95cc (`gh api /repos/gradle/actions/security-advisories/...`), tag and commit objects via the Git API — HIGH
- `ne1r0n/clickhouse-kafka-connect` PRs #21 and #22 (file lists, Dependabot compare range `3f5f9ada…8da46b53`) — HIGH
- `ClickHouse/clickhouse-java` tag `v0.11.0`: release notes (`gh release view`), `docs/releases/0_11_0.md`, `client-v2` `Client.java` (`close`, `runAsyncOperation`, `setSocketTimeout`, `setExecutionTimeout`, `useAsyncRequests`), `HttpAPIClientHelper.java` (`close`, `createRequestHttpContext`, retry causes), `ClientConfigProperties.java`, V1 `ClickHouseClient.java` and `ClickHouseClientOption.java` — HIGH for V2, MEDIUM for V1 (not exercised)
- Working tree: `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `KeeperStateProvider.java`, `ProxySinkTask.java`, `Utils.java`, `DataJson.java`, `ConvertToMapOrJson.java`, `StubClickHouseServer.java`, `ClientTimeoutPrefixPinUnitTest.java`, `.github/dependabot.yml`, `docs/FORK-DELTA.md` row 72, and `36-FOLLOWUPS.md` — HIGH
- `upstream/main` `ClickHouseWriter.java` (`stop()`, `chcTmp`) via `git show` — HIGH
- Not done: no Context7 and no research-seam cache entries were written (the primary sources above were read directly from tags and APIs). The empirical V1 stall behaviour and the exact post-close observable for the F-1 pin remain for the plan phase.

---
*Stack research for: v1.7.0_gg7 maintenance milestone*
*Researched: 2026-11-01*
