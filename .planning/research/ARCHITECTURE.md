# Architecture Research

**Domain:** Maintenance sync of a patched fork of an upstream Kafka Connect sink (ClickHouse): upstream tail merge, ClickHouse client `0.9.6` -> `0.11.0`, `additionalRetriableErrorCodes`, CI changes
**Milestone:** v1.7.0_gg6-maintenance
**Researched:** 2026-10-28
**Confidence:** HIGH on merge mechanics and conflict surface (measured with `git merge-tree` against `upstream/main` = `9bed6f79`), MEDIUM on 0.11.0 behavioural effects (release notes only, nothing run yet)

All conflict findings below were produced by `git merge-tree --write-tree --name-only` of `sync/v1.7.0` (`0b6ed117`) and of `origin/custom_build` (`3a3a15a9`) against `upstream/main`. Both give the same five conflicted paths.

## Standard Architecture

### System Overview: where the three changes land

```
Kafka Connect runtime
        |
        v
+-----------------------------------------------------------------------------+
| ClickHouseSinkTask (fork: connectorName from props "name")                  |
|   -> ProxySinkTask  [TAIL CONFLICT: fork ctor(connectorName) vs upstream    |
|        |               test-seam ctor(DBWriter); put() now passes            |
|        |               cfg.getAdditionalRetriableErrorCodes() to handleException]
|        v                                                                    |
|   ChunkFlusher (upstream, auto-merges: same extra arg)                      |
|        v                                                                    |
|   Processing (state machine, untouched by this milestone)                   |
+--------+--------------------------------------------------------------------+
         v
+-----------------------------------------------------------------------------+
| ClickHouseWriter  /  MetadataFreeJsonWriter (fork)   -- untouched by tail   |
|   builds ClickHouseHelperClient via ClickHouseClientBuilder                 |
+--------+--------------------------------------------------------------------+
         v
+-----------------------------------------------------------------------------+
| ClickHouseHelperClient  [CLIENT BUMP lands here]                            |
|   V2: new Client.Builder()...setClientName / compressServerResponse(false)  |
|       / serverSetting(network_compression_method, lz4) / setOption(k,v) from|
|       jdbcConnectionProperties                                              |
|   V1: ClickHouseNode + ClickHouseClient (compile-only gate, not run in CI)  |
+--------+--------------------------------------------------------------------+
         v
ClickHouse server (HTTP)  -- errors come back as ServerException (V2) /
                              ClickHouseException (V1)
         v
Utils.handleException(e, tolerance, retryOnSocketException,
                      additionalRetriableErrorCodes, records)   [TAIL: new 5-arg]
   DEFAULT_RETRIABLE_ERROR_CODES (15 codes, includes 159)  U  additional codes
   -> RetriableException (Connect redelivers) | DataException | RuntimeException/DLQ
```

### Component Responsibilities (only those this milestone touches)

| Component | Responsibility | Milestone change | New or modified |
|-----------|----------------|------------------|-----------------|
| `util/Utils.handleException` | Maps a failure to retry / DLQ / task failure | Signature gains `Set<Integer> additionalRetriableErrorCodes`; `switch` becomes `DEFAULT_RETRIABLE_ERROR_CODES.contains(...)`. Fork's null-message guard (WRT-11) sits in a different hunk and survives | Modified (upstream tail) |
| `sink/ClickHouseSinkConfig` | Config definition and parsing | New key `additionalRetriableErrorCodes` (`LIST`, default empty, `ErrorCodeListValidator`, `parseErrorCodes`, getter via class-level `@Getter`). Auto-merges next to the fork's `TARGET_TABLE_FILTER` / `jsonInsertFormatExplicit` | Modified (upstream tail, no conflict) |
| `sink/ProxySinkTask` | Orchestrates one put | Tail adds a package-private ctor taking a `DBWriter` test seam and the extra `handleException` arg. Conflicts with the fork's `connectorName` ctor (ORCH-02) | Modified, conflict |
| `sink/ChunkFlusher` | Buffered-path insert | Passes the new arg. Auto-merges | Modified (no conflict) |
| `db/helper/ClickHouseHelperClient` | Builds V1/V2 clients, describe, query | Only `clickHouseDriver` line changes. Possible compile-level adaptation; behaviour effects listed under Client bump below | Modified only if the compile gate or live pins require it |
| `gradle/libs.versions.toml` | Version catalog | `clickHouseDriver = "0.11.0"` is a fork-only override (upstream main still `0.9.6`) | Modified |
| `build.gradle.kts` (`confluentJar`, `clickhouseDependencies`) | Bundles the Confluent archive | 0.11.0 `client-v2` adds `com.github.luben:zstd-jni:1.5.7-23` as a compile dependency, so the archive gains a native-lib jar | Unchanged file, changed output |
| `.planning/scripts/*` (anchors, gate, allowlist) | Sync safety net | New anchor rows, one new allowlist file, optional CI consumer | New rows / new file |
| `.github/workflows/*` | CI and release | setup-java SHA bump (4 files), `increment-version.yml` fix, archive-compare consumer | Modified |

## Recommended Project Structure

No new source packages are expected. The new or changed artifacts are:

```
.planning/
├── scripts/
│   ├── fork-anchors.txt                    # + rows (see "Anchors at risk"), VERSION row moves at release
│   ├── fork-anchor-exemptions.txt          # only needed if a Dependabot commit lands on origin first
│   ├── conflicted-paths-v1.7.0-tail-2.txt  # committed merge-tree output for this tail merge
│   └── confluent-archive-allowlist-33.txt  # generated LAST, from the final tree (zstd-jni etc.)
├── MERGE-CHECKLIST-v1.7.0-tail-2.md        # same shape as MERGE-CHECKLIST-v1.7.0-tail.md
src/main/java/.../sink/ProxySinkTask.java   # dual-ctor re-expression (post-merge commit)
src/test/java/.../util/UtilsNullMessageClassificationUnitTest.java  # 4-arg call -> 5-arg
docs/ROLLOUT-v1.7.0_gg6.md, docs/FORK-DELTA.md, CHANGELOG.md, VERSION
.github/workflows/{release,pre-release}.yaml  # archive-compare consumer (see CI section)
.github/workflows/increment-version.yml       # vnull fix
```

### Structure Rationale

- **Name the conflict file and rollback tag with a suffix** (`-tail-2`): `pre-v1.7.0-tail` and `conflicted-paths-v1.7.0-tail.txt` already exist, and the gate takes both as environment variables.
- **Allowlist is per release and last.** `compare-confluent-archives.py` fails on an unused row (STALE-ROW), so the file is only correct for one exact tree. It cannot be written before the final dependency set is fixed.

## Conflict Surface of the Upstream Tail

Upstream `a6f1df83..9bed6f79` is 7 commits, 27 files, +711/-382. It changes **no build file, no workflow and no `gradle/` file**, so the tail does not overlap the Dependabot bumps or the client bump textually.

### Conflicted paths (measured, 5)

| Path | Cause | Resolution shape | Risk |
|------|-------|------------------|------|
| `CHANGELOG.md` | Fork's top section `# 1.7.0_gg5` vs upstream's new `# Unreleased` entry for `additionalRetriableErrorCodes` | Union in the merge commit M (both blocks, each line once). The `1.7.0_gg6` section is authored later, in the release commit, and absorbs the upstream entry | Low |
| `util/Utils.java` | Import block only: fork's sorted import order vs upstream's added `java.util.Set` | Union of imports in M. Nothing else conflicts: the `switch` -> `Set` hunk and the fork's `msg != null && (msg.indexOf(` guard are separate hunks and auto-merge | Low, but verify the WRT-11 anchor still counts 1 |
| `sink/ProxySinkTask.java` | **Real semantic conflict.** Fork: `ProxySinkTask(cfg, reporter, String connectorName)` and `new SinkTaskStatistics(connectorName, id)`. Upstream: public `(cfg, reporter)` delegating to package-private `(cfg, reporter, DBWriter)` which injects the writer | Not expressible as a union. See "ProxySinkTask resolution" | **High** |
| `test/.../ClickHouseSinkTaskMappingTest.java` | Imports only (`Supplier`, static `assertEquals` vs fork's `SinkRecord`, `Test`) | Union in M | Low |
| `test/.../db/ClickHouseWriterTest.java` | Imports only (large block) | Union in M; the fork's WRT pins in the same file auto-merge | Low |

### Auto-merged but semantically broken (the dangerous class)

The 2026-10-21 tail merge had the same trap: a clean textual merge that does not compile.

| Path | Why | Fix (post-M commit) |
|------|-----|---------------------|
| `src/test/.../util/UtilsNullMessageClassificationUnitTest.java` (fork-only test) | Line 32 calls the old 4-arg `Utils.handleException(e, tolerance, NO_SOCKET_RETRY, new ArrayList<>())`. No upstream hunk touches this file, so merge-tree reports no conflict, and the test source set stops compiling | Add `Collections.emptySet()` as the 4th argument, matching what upstream did in `UtilsTest` |
| `src/test/.../ClickHouseSinkTaskWithSchemaTest.java`, `...SchemalessTest.java`, `...StringTest.java`, `...BufferTest.java`, `...ProxyTest.java`, `FailureTest`, `TableTest`, `ClickHouseHelperClientTest` | Upstream's `CreateTableStatement` refactor (`create()`, `Supplier` templates, default `MergeTree` engine, deprecated public ctor) rewrote these. They auto-merge with fork hunks | Gate with `compileTestJava` and `compileIntegrationTestJava`, then the full suite. `new CreateTableStatement()` has 113 uses in the fork tree; the deprecation is a warning only (no `-Werror`) |
| `src/integrationTest/...` (3 upstream-touched files) | Same refactor; the fork also changed the integration harness (`ConfluentPlatform`) | `compileIntegrationTestJava` is a mandatory gate, since `testUnit` and `test` never compile that source set's behaviour |

### ProxySinkTask resolution

Both sides add a third constructor parameter, and `null` is ambiguous between `String` and `DBWriter`. Upstream's own `this(clickHouseSinkConfig, errorReporter, null)` stops compiling once the fork's `String` overload exists, and `AdditionalRetriableErrorCodesTest` calls `new ProxySinkTask(config, null, writer)`.

Recommended shape (single post-M re-expression commit; M itself takes the fork side of both hunks, which is what the provenance invariant allows because every line exists in one parent):

```java
public ProxySinkTask(ClickHouseSinkConfig cfg, ErrorReporter reporter, String connectorName) {
    this(cfg, reporter, connectorName, null);
}
ProxySinkTask(ClickHouseSinkConfig cfg, ErrorReporter reporter, DBWriter dbWriter) {   // upstream test seam
    this(cfg, reporter, null, dbWriter);
}
ProxySinkTask(ClickHouseSinkConfig cfg, ErrorReporter reporter, String connectorName, DBWriter dbWriter) {
    ...
    this.statistics = new SinkTaskStatistics(connectorName, id);
    this.dbWriter = dbWriter != null ? dbWriter : new ClickHouseWriter(this.statistics);
    processing = new Processing(stateProvider, this.dbWriter, errorReporter, cfg, statistics);
}
```

- The fork's two production callers (`ClickHouseSinkTask:48`, `WriterFailureClassificationUnitTest:315`, `ValueFreeDlqHeadersUnitTest:119`) keep the 3-arg `String` form unchanged.
- Upstream's `AdditionalRetriableErrorCodesTest` compiles unchanged and runs with `connectorName == null`. `SinkTaskStatistics(null, id)` is already legal and pinned (`MBeanNameUnitTest`, renders `connector=null`).
- Do not make the 4-arg ctor public: keeps the fork's public surface identical to gg5.

## Fork-Anchors at Risk

Gate semantics that matter (read from `verify-merge-invariants.sh`):

- Invariant 1 (provenance): every line of every changed path in merge commit M must exist in one of M's two parents. **A resolution that needs authored lines cannot live in M.** Hence the two-step: mechanical M, then re-expression commits.
- Invariant 2: no conflict marker in any commit in `PRE_TAG..range-end`.
- Invariant 3: every commit in the range satisfies its own copy of `fork-anchors.txt`. An anchor can only move in the commit that rewrites the file.
- The `UPSTREAM_REF` must be the full SHA (`9bed6f79...`), never `upstream/main`, and `git ls-remote upstream main` must be re-checked immediately before the merge (upstream may move again).

| Anchor / fork behaviour | Files | Risk in this milestone | Why |
|-------------------------|-------|------------------------|-----|
| `WRT-11`: `if (msg != null && (msg.indexOf(` count 1 | `Utils.java` | **Medium.** Silently lost if someone resolves `Utils.java` with "take theirs" | The only conflict in this file is imports, but "theirs" is the tempting wholesale answer. Anchor catches it (count 0) |
| `CFG-01/CFG-02`: `TARGET_TABLE_FILTER` 3, `jsonInsertFormatExplicit` 2, commented `async_insert` 1 | `ClickHouseSinkConfig.java` | Low | Upstream adds around them; auto-merged, counts unchanged |
| `ORCH-02` (`connector=%s`) | `SinkTaskStatistics.java` | **Gap.** The anchor is on the MBean format, not on the plumbing in `ProxySinkTask`/`ClickHouseSinkTask` | If `ProxySinkTask` were resolved to upstream's side, the compile error in `ClickHouseSinkTask` would catch it loudly, but `new SinkTaskStatistics(id)` still exists nowhere. Add a row on `ProxySinkTask.java`: `new SinkTaskStatistics(connectorName, id)` count 1 and `new SinkTaskStatistics(id)` count 0 |
| `BLD-07`: `VERSION v1.7.0_gg5` 1 | `VERSION` | Moves at release | Same commit must rewrite the anchors file (`v1.7.0_gg6`) |
| `kafka = "4.4.0"` 1 (BLD-08) | `libs.versions.toml` | Low, unless a Kafka bump PR appears | A Dependabot commit that changes an anchored line needs an exemption row (the DEPS-03 mechanism) or an anchors edit in the same commit |
| `CMP-01/CMP-02`: lz4 setting, `compressServerResponse(false)`, `options.put(COMPRESS, false)`, `NETWORK_COMPRESSION_METHOD_` 0 | `ClickHouseHelperClient.java`, `ClickHouseWriter.java`, `KeeperStateProvider.java` | **Medium for the client bump.** The anchors stay true on 0.11.0, but their *justification* (client cannot decode ZSTD) may no longer hold | See Client bump. Do not remove the code; re-measure and update the FORK-DELTA row |
| `BLD-09`: `withCapability("org.lz4:lz4-java")` 1 | `build.gradle.kts` | Low | lz4 `1.12.1` (PR #19) and client-v2 `0.11.0` (its own `at.yawk.lz4:lz4-java:1.11.4`) both resolve to the fork's catalog version; `TestRuntimeClasspathLz4UnitTest` and `Lz4RoundTripLiveTest` guard it |
| `BLD-13`: `clickhouse.jdbc` / `clickhouse-jdbc` 0 | `build.gradle.kts`, toml | Low | The 0.11.0 bump touches only the `clickHouseDriver` version line; no JDBC module is introduced |
| `CI-05`: `timeout-minutes: 45` 1 | `tests.yaml` | Low unless a step is added to `tests.yaml` | Keep new CI steps out of `tests.yaml` (see CI section) |
| `REL-0x` rows on `release.yaml` / `pre-release.yaml` | workflows | **Medium** for the archive-compare step | `gh release create` count 2, `./gradlew createConfluentArchive` 1, `!inputs.dry_run && ...` 3, `inputs.dry_run || ...` 5: any added step that repeats those expressions breaks the counts. Add steps with different text and extend the rows in the same commit |
| `REL-02`: `node-version: '24'` 1, `npm install fs` 0 | `increment-version.yml` | Low | The vnull fix is a change to the `Increment version` step only |

New anchor rows to add (additive, one commit with the code that makes them true):
- `libs.versions.toml`: `clickHouseDriver = "0.11.0"` count 1, with a `# FORK OVERRIDE (upstream 0.9.6)` comment in the style of the `kafka` line. Every future upstream sync would otherwise conflict on this line with no record of why. (Anchor substring must be the value part only so the trailing comment does not matter.)
- `Utils.java`: `DEFAULT_RETRIABLE_ERROR_CODES.contains(errorCode)` count 1 and `case 159:` count 0, pinning that the switch does not come back and the 159 default is kept.
- `ProxySinkTask.java` rows above.

## Architectural Patterns

### Pattern 1: Mechanical merge, then re-expression commits

**What:** M = `git merge --no-commit` of the full upstream SHA with every conflict resolved by union or by taking one parent's lines; no `.planning` commit between `merge --no-commit` and M; the resolution record is committed *after* M (a new file inside M fails invariant 1). Semantic fixes (ProxySinkTask dual ctor, test call sites) are separate later commits.
**When to use:** every upstream sync in this fork. It is what `verify-merge-invariants.sh` enforces.
**Trade-offs:** M does not compile (ProxySinkTask is on the fork side, `UtilsNullMessageClassificationUnitTest` is stale, and the test side of upstream's `AdditionalRetriableErrorCodesTest` needs the DBWriter ctor). That is accepted by the existing gate; compile is asserted at the first post-M commit.

### Pattern 2: Fork override recorded in the catalog line and an anchor

**What:** the Kafka line carries `# FORK OVERRIDE (upstream 2.7.0)`. Give the client line the same marker and an anchor row.
**Why:** upstream is still on `0.9.6` (its `07/14/26/client_version_upgrade` branch was merged into main at 0.9.6), so this is a standing divergence, not a catch-up.

### Pattern 3: Absorb Dependabot by merging the PR heads, not by merging on origin

**What:** In the scratch branch, `git merge origin/dependabot/...` each head (each is a single commit, one line in `libs.versions.toml` or the wrapper, or four `uses:` lines). GitHub marks a PR merged when its head commit is reachable from the base branch, so the PRs close themselves when `custom_build` takes the scratch branch.
**Why:** the user merging on origin first diverges `sync/v1.7.0` from `origin/custom_build` again (it did in gg3 and gg4) and forces `fork-anchor-exemptions.txt` rows for anchored lines. None of the six open PRs touches an anchored line, so no exemption is expected. Confidence MEDIUM on the "PR closes itself" behaviour (standard GitHub behaviour, not tested here). The merge-tree check of each head against the others is conflict free in pairs tested against `custom_build`; re-run it on the actual scratch tip.

### Pattern 4: Pins before the bump, not after

For the client bump, write the classification and compression pins on `0.9.6` first (they pass), then change the one line and read which pins move. A pin that flips on the bump is the intended signal; a pin written after the bump only records the new behaviour.

## Client Bump (0.9.6 -> 0.11.0) Integration

### Facts

- Maven Central has 0.11.0 for `clickhouse-client`, `clickhouse-http-client`, `clickhouse-data`, `client-v2` and `clickhouse-jdbc`, so the single `clickHouseDriver` version ref (shared by the four catalog entries the fork uses) resolves for all of them. V1 is therefore still present and still compiled.
- The fork's client calls are in `ClickHouseHelperClient` (builder, ping, query, describe), `ClickHouseWriter` (`client.insert(...)`, `InsertSettings`, `catch (ServerException)`), `KeeperStateProvider` and `Utils`. Imports used: `Client`, `Client.Builder`, `InsertSettings`, `QuerySettings`, `Records`, `GenericRecord`, `ServerException`, `DataStreamWriter`, `ProxyType`, plus the V1 types.
- 0.10.0 and 0.11.0 breaking changes (release notes, MEDIUM until measured):

| Change | Where it meets the fork | Expected effect |
|--------|------------------------|-----------------|
| 0.11.0: server error 159 is not retried by the client by default (own cause `ServerTimeoutExceeded`, not in default `client_retry_on_failures`) | `Utils.DEFAULT_RETRIABLE_ERROR_CODES` already contains 159 | Connector behaviour is unchanged at the classification level: the first 159 now escapes the client at once and `handleException` throws `RetriableException`, so Connect redelivers sooner and the in-client attempts disappear. Exactly-once is unaffected (a redelivered `BEFORE` batch carries the same dedup token). Operators can restore client retries with `jdbcConnectionProperties` (`client_retry_on_failures=...`), because `createClientV2` already forwards every `k=v` pair through `setOption`. The value replaces the default list, so the note must say to list the defaults too |
| 0.11.0: socket buffers have no default | Fork never sets `socket_rcvbuf`/`socket_sndbuf` | OS auto-tuning applies; throughput change is possible, correctness is not. Not covered by any test; measure in the nightly or leave as a rollout-note line |
| 0.11.0: ZSTD added for the block compression stream, new compile dependency `zstd-jni` | D-17 workaround (`compress=false`, `network_compression_method=lz4`) | Likely still harmless, but the stated reason ("client 0.9.6 cannot decode ZSTD") may be gone. `NetworkCompressionPinLiveTest` only asserts successful round trips, so it will keep passing and will not tell you. Re-measure the stripped-both case on 0.11.0 and rewrite FORK-DELTA's CMP row and Recorded Fact 26 either way. Keep the code and anchors (additive, and 0.11.0 behaviour on older servers is unmeasured) |
| 0.11.0: User-Agent format changed to `<client name> clickhouse-java-v2/<ver> (lv:...; os:...; hc:...)` | `CONNECT_CLIENT_NAME`; `ClickHouseSinkTaskTest:271` finds the connector with `position(http_user_agent, CONNECT_CLIENT_NAME) > -1` | The client name stays a substring, so the test should keep passing. Operators who parse `http_user_agent` in `system.query_log` need a rollout line |
| 0.10.0: HTTP 503 becomes `java.net.ConnectException` (client-retried by default); unknown HTTP statuses become `ClientException` | `Utils.handleException` maps `SocketException` only when `retryOnSocketException` is true; `ConnectException` is a `SocketException` | A 503 that exhausts client retries used to surface as a `ServerException` and now surfaces as a `ConnectException`, which the connector fails on unless `retryOnSocketException=true`. Possible behaviour regression for proxy/LB 503s. Needs a unit pin on the classification and a line in the note. Confidence LOW-MEDIUM: derived from release notes, not observed |
| 0.10.0: `Client.Builder#build()` throws `ClientMisconfigurationException` instead of `IllegalArgumentException` for auth/SSL misconfiguration | `createClientV2` is called from the helper constructor; no fork code catches `IllegalArgumentException` around it | Different exception type in a start-up failure message only. Grep confirms nothing catches the old type |
| 0.10.0: null password no longer becomes the literal `null` | `setPassword(this.password)` | Strictly a fix |
| New `provided`-scope deps in client-v2 (OpenTelemetry, Jackson, Gson) | Not bundled | No archive change; only matters if observability recorders are enabled, which the fork does not |

### Archive and dependency closure effects

`confluentJar` bundles the `clickhouseDependencies` configuration, which resolves `client-v2` transitively. With 0.11.0 the closure gains `com.github.luben:zstd-jni:1.5.7-23` (native libraries), `httpcore5`/`httpcore5-h2` 5.4.3, `error_prone_annotations` 2.50.0 and `j2objc` 3.1, and `guava` 33.7.2-jre (now equal to the fork's explicit guava pin). Consequences:

1. `verifyConfluentArchive` (no nested jar, no Kafka API class, required classes present) is the first gate; native `.so`/`.dll` entries inside the shaded jar are not a nested jar, so it should pass.
2. The archive compare against the gg5 release differs by far more than the release identity this time: the allowlist for gg6 must list the zstd-jni rows and every bumped jar. Generate it from the final tree, never before.
3. Archive size and platform coverage of the bundled native libs should be recorded in the rollout note (Confluent Hub consumers see a larger archive).
4. The explicit `implementation(libs.guava)` / `clickhouseDependencies(libs.guava) // temp to override client's with CVE` become redundant. Leave them: they are upstream lines, removing them only creates future conflicts.

### What is not verifiable in this fork's gate

The suite runs ClickHouse `latest` x client V2 only. Client V1 code (`createClientV1`, `queryV1`, `pingV1`, `doInsertRawBinaryV1`) is compile-gated only against 0.11.0. State that explicitly in the rollout note rather than claiming V1 coverage.

## Data Flow

### Failure classification flow after the tail and the bump

```
insert / query throws
    |
    v
client 0.11.0 decides whether to retry in-client
    (159: no;  503: yes as ConnectException;  other defaults unchanged)
    |
    v  exception reaches the connector
Processing.doLogic -> ProxySinkTask.put catch / ChunkFlusher.flush catch
    |
    v
Utils.handleException(e, errorsTolerance, retryOnSocketException,
                      cfg.getAdditionalRetriableErrorCodes(), records)
    rootCause = ServerException (V2) | ClickHouseException (V1)
        code in DEFAULT (15)  or  code in additional  -> RetriableException
        else                                         -> fall through
    DataException && !tolerance -> rethrow
    SocketTimeout / UnknownHost / (retryOnSocketException && Socket) -> Retriable
    IOException "Read/Write timed out after" (null-safe, fork WRT-11) -> Retriable
    tolerance -> DLQ,  else RuntimeException(records.size())
```

### Key data flows

1. **Operator lever:** `additionalRetriableErrorCodes=216,...` -> `ClickHouseSinkConfig.parseErrorCodes` -> immutable `TreeSet` -> passed per call, no state. It adds nothing to the exactly-once machinery: a `RetriableException` leaves `BEFORE` set, the same batch is rebuilt, and dedup handles the retry. Redelivery bypasses `errors.tolerance` and the DLQ, per upstream's own text, which must be repeated in the rollout note.
2. **Release identity:** `VERSION` -> generated `Version.ARTIFACT_VERSION` -> inlined into `Version`, `ClickHouseHelperClient`, `SinkTaskStatistics` classes and the manifest. Those four paths are the only legitimate archive differences when nothing else changes; this milestone has many more.

## Recommended Build Order

Rule that drives it: **put the lowest-risk, most mechanical changes first so every later red is attributable; put the only optional, behaviour-bearing change (the client) last among code changes, so descoping it is a one-commit revert.**

### Why the tail goes before the client bump (and after the trivial bumps)

1. The tail and the client bump are textually independent (no shared file except `gradle/libs.versions.toml`, and the tail does not touch it). So order is decided by risk isolation, not by conflict avoidance.
2. Merged on `0.9.6`, the tail's effect is isolated: any red is a merge-resolution bug, not a client effect.
3. The client audit and live pins then target the **final** `Utils.handleException` (5-arg) and `ProxySinkTask` shape. Written the other way round, `UtilsNullMessageClassificationUnitTest`, `WriterFailureClassificationUnitTest` and the new 159/503 pins would be authored against the 4-arg signature and rewritten.
4. `additionalRetriableErrorCodes` is the operator-facing remedy for codes the new client stops retrying or newly raises, so it should exist before the client behaviour is documented.
5. If the 0.11.0 audit fails or a defect is found, gg6 can still ship with `0.9.6` (upstream is on `0.9.6` too); PR #17 stays open. The tail merge's `PRE_TAG`/gate range is unaffected.

### Sequence

| Step | Work | Gate before moving on |
|------|------|-----------------------|
| 0 | Re-fetch upstream; `git ls-remote upstream main` must still be `9bed6f79`. Create `pre-v1.7.0_gg6` at the `custom_build` tip (`3a3a15a9`) and keep it for the release; keep the sync branch off origin until the end. Do not let the user merge Dependabot PRs on origin | Rollback tag resolves; `HEAD` is a descendant of `3a3a15a9`'s parents (the gate accepts a merge tag whose every parent is an ancestor) |
| 1 | **Toolchain and test-only bumps, one commit each, PR head merged:** Gradle wrapper `9.8.2` (#20) first, then lz4-java `1.12.1` (#19), okhttp `5.5.1` (#18), protobuf-java `4.36.2` (#10), setup-java `6.1.0` (#16) | `compileJava compileTestJava compileIntegrationTestJava`, `testUnit` after each; one full `./gradlew test` at the end of the group. #10 changes `libprotobuf`, which also selects `protoc` (`build.gradle.kts` line 541), so it regenerates test protos: gate `ProtobufIngestTest`. #19 gates `TestRuntimeClasspathLz4UnitTest` and `Lz4RoundTripLiveTest`. #16 gates the three pin-guard unit tests (its diff is already SHA + `# v6.1.0` form, the SHA `b843ebf9...` resolves to tag v6.1.0 in `actions/setup-java`, so the guard regex `@[0-9a-f]{40}\s+#\s+v\d+\.\d+\.\d+` is satisfied; no manual re-pin is needed). Anchors file unchanged |
| 2 | **Tail merge M** onto the step-1 tip: `PRE_TAG=pre-v1.7.0-tail-2` at the first parent, `UPSTREAM_REF=9bed6f79...` (full SHA), `CONFLICTED_PATHS_FILE` committed in the commit after M. Resolve the 5 conflicts as in the table. No other change in M | Tail gate (the unedited script) ends `result=PASS`; `provenance_paths` equals the changed-path count |
| 3 | **Re-expression commits** (each its own commit, anchors file in the same commit when counts move): ProxySinkTask dual ctor; `UtilsNullMessageClassificationUnitTest` 5-arg call; new anchor rows; a fork-owned pin that the 15-code default set equals the former `switch` list and that `ClickHouseSinkConfig` default for the new key is empty (`NewUpstreamConfigDefaultsUnitTest` already pins upstream defaults, so extend it); `MERGE-CHECKLIST-v1.7.0-tail-2.md` | `compileJava compileTestJava compileIntegrationTestJava`, `testUnit`, full suite twice; tail gate again; cumulative gate |
| 4 | **Client bump, pins first.** (a) On `0.9.6`, add 159/503/`ConnectException` classification pins and a live compression pin that records the stripped-both outcome. (b) One-line `clickHouseDriver = "0.11.0"` commit with the `# FORK OVERRIDE` marker and anchor row. (c) Run the full suite twice; read every flipped pin. (d) Only then adapt source if compile or a pin demands it | Full suite green twice; archive built and `verifyConfluentArchive` passes; zstd-jni present in the closure is recorded; decision recorded: ship 0.11.0 or revert (b) |
| 5 | **CI and advisory work** (independent of 1-4 textually; schedule after 4 so the compare consumer is written against the real archive): `increment-version.yml` vnull fix; archive-compare consumer; Phase 30/31 WR/IN rows; gg4 close-record findings | Pin-guard unit tests, `NightlyWorkflowGuardUnitTest`, `UpstreamIntegrationWorkflowBytesGuardUnitTest` (upstream `integrationTests.yaml` byte-identical) |
| 6 | **Release:** `VERSION=v1.7.0_gg6` with the anchors row in the same commit; CHANGELOG (absorbs upstream's `Unreleased` entry); FORK-DELTA; `ROLLOUT-v1.7.0_gg6.md`; generate `confluent-archive-allowlist-33.txt` from the final tree and run the compare; independent review of operator documents; cumulative gate; two full runs; human approval; `--no-ff` merge; one green `tests.yaml` run on the published SHA; tag after that run | As in gg5 |

## CI Changes Integration

| Item | Where | Recommendation | Risk |
|------|-------|----------------|------|
| setup-java `6.1.0` (#16) | `tests.yaml`, `release.yaml`, `pre-release.yaml`, `nightly-integration-tests.yaml` | Take the Dependabot head as is (see step 1). The existing `dependabot.yml` already keeps these pins current and excludes only `integrationTests.yaml` | Low |
| `increment-version.yml` writes `vnull` | The `Increment version` step: `semver.inc('v1.7.0_gg5', ...)` returns null | Fix inside the step, not by a new action: handle the `_ggN` suffix explicitly (split off `_gg<N>`, increment `N`, or bump the base). Keep `node-version: '24'` 1 and `npm install fs` 0 anchors intact. The workflow needs a dispatchable unit-level pin: extract the version arithmetic to a script-free `node -p` expression that a JUnit test can execute with the same semver version, or test the YAML text only and say so | Low-medium: this workflow was never run for a `_gg` version |
| CI consumer for the archive compare | **`release.yaml` / `pre-release.yaml`, not `tests.yaml`** | The allowlist is valid for exactly one tree (STALE-ROW), so a per-push compare would fail on every unrelated change. The release workflows already build the archive and are the only place an allowlist for "this release vs previous release" is meaningful. Fetch the previous release asset with `gh release download v1.7.0_gg5` (the real gg5 release exists, per PROJECT.md), run `python3 -I .planning/scripts/compare-confluent-archives.py A.zip B.zip allowlist-33.txt`, fail on any violation. In dry-run it must run too and must not publish | Medium: adds steps to files with count-based anchors. Choose step text that does not repeat `gh release create`, `--draft`, or the two `dry_run` expressions, and extend the REL rows in the same commit |
| Keep `tests.yaml` stable | `tests.yaml` | `TestsWorkflowPinGuardUnitTest` checks its structure closely, and the `CI-05` and SHA-keyed concurrency decisions are anchored. Add nothing there unless required | Low |
| Nightly | `nightly-integration-tests.yaml` | After the client bump, the first manual nightly run on the new client is the only place the integration tests (Connect worker in Docker) see 0.11.0. Treat one green manual run as a release gate for the client decision, since `integrationTests.yaml` stays disabled | Low |

## Anti-Patterns

### Anti-Pattern 1: Resolving `ProxySinkTask` by taking upstream's side

**What people do:** `git checkout --theirs` to make the test seam compile.
**Why it's wrong:** `connectorName` disappears, the MBean `connector=` segment and ORCH-02 vanish, and the only loud signal is a compile error in `ClickHouseSinkTask`. A "fix" there usually restores a 2-arg ctor and loses the segment silently.
**Do this instead:** fork side in M, dual-ctor re-expression in the next commit, plus the anchor rows above.

### Anti-Pattern 2: Bumping the client inside the tail merge, or inside any merge commit

**What people do:** fold `clickHouseDriver` into M "since we are touching dependencies".
**Why it's wrong:** M must contain only lines present in a parent (invariant 1) and a red full suite then has two possible causes. The gg4 checklist rule 2 says the same.
**Do this instead:** separate, later, single-line commit with pins written first.

### Anti-Pattern 3: Merging Dependabot PRs on `origin/custom_build` before the sync branch is done

**What people do:** click merge on the six PRs.
**Why it's wrong:** diverges the working branch again, requires a DEPS-01-shaped merge, and every anchored line a PR touches needs an exemption row bound to a published SHA.
**Do this instead:** merge the PR heads on the scratch branch (Pattern 3).

### Anti-Pattern 4: Treating a green `NetworkCompressionPinLiveTest` as proof the D-17 rationale still holds

**What people do:** bump, see green, move on.
**Why it's wrong:** the test asserts round-trips succeed with the controls in place; it never asserted the failure without them. On 0.11.0 it passes whether or not the workaround is still needed.
**Do this instead:** measure the stripped-both case, record it, and update the FORK-DELTA row and Recorded Fact 26 text. Keep the code.

### Anti-Pattern 5: Generating the archive allowlist early

**What people do:** write `allowlist-33.txt` after the client bump.
**Why it's wrong:** the compare script fails on unused rows; any later bump or the version change makes it stale.
**Do this instead:** generate it as the last content change before the release gate.

## Scaling Considerations

Not applicable in the usual sense. The relevant "scale" is the size of the divergence from upstream:

| Concern | Now | After gg6 | Mitigation |
|---------|-----|-----------|------------|
| Fork-only version pins vs upstream | 1 (`kafka`) | 2 (`kafka`, `clickHouseDriver`) | `# FORK OVERRIDE` marker plus anchor; each sync resolves the `[versions]` block to the fork's value |
| Files differing from upstream | 287 wide / 37 in `src/main` | +1 to +3 (ProxySinkTask already counted; anchors, workflows, scripts) | Keep changes additive |
| Archive third-party closure | gg5 set | + zstd-jni and bumped jars | Per-release allowlist, generated last |

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| ClickHouse `latest` (26.10.x at last run) | Testcontainers, V2 only | V1 not exercised; ZSTD default network compression on 26.9+ is the reason D-17 exists |
| `upstream` remote | Named permanent remote; merge by full SHA | Still no tag past `v1.7.0`; `upstream/main` = `9bed6f79` at research time |
| GitHub Releases | `gh release create` / `gh release download` | The gg5 release is the baseline asset for the archive compare |
| Dependabot | `.github/dependabot.yml` (gradle monthly, actions monthly) | PR #4 (`0.10.0`) is stale and superseded by #17 |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `ProxySinkTask` / `ChunkFlusher` -> `Utils.handleException` | direct static call, now with the retriable-code set | Only two production callers; no fork code calls it |
| `ClickHouseSinkConfig` -> `Utils` | getter `getAdditionalRetriableErrorCodes()` (class-level `@Getter`) | Immutable set; no ConfigDef name collision with fork keys |
| `ClickHouseHelperClient` -> client lib | `Client.Builder` + `setOption` passthrough from `jdbcConnectionProperties` | The passthrough is the supported way to set `client_retry_on_failures` or socket buffers without a code change |
| Anchors file -> gate | every commit in range checked against its own copy | New rows must land in the commit that makes them true |
| Allowlist -> compare script -> (new) release workflow | file path argument | Introduces the first CI consumer; allowlist must be committed before the release workflow runs on that SHA |

## Open Questions / Gaps

- **503 and `ConnectException` classification (LOW-MEDIUM).** Derived from the 0.10.0 notes. Needs a live or scripted proxy test before the rollout note claims anything.
- **ZSTD request compression default (LOW).** The 0.11.0 note says ZSTD "is used by default" for client requests but the sentence is garbled ("used by default not to compress"). Measure what the V2 insert path sends with the connector's settings (`Lz4RoundTripLiveTest` already builds a `compressClientRequest(true)` client and is the natural place).
- **zstd-jni native packaging in the shaded jar and on the Confluent Hub archive** (platform libs present, size, `Multi-Release` handling since `addMultiReleaseAttribute` is false). Measure when the archive is first built on 0.11.0.
- **Whether upstream moves again** before the merge. Re-run `git ls-remote upstream main` and re-run `git merge-tree` immediately before step 2; the conflict list above is only valid for `9bed6f79`.
- **Dependabot PR heads on a changed base.** The pairwise merge-tree check ran against `custom_build`; re-run on the real scratch tip after each merge.

## Sources

- `git merge-tree --write-tree --name-only` of `HEAD` and `origin/custom_build` against `upstream/main` (`9bed6f79`), and `git diff a6f1df83 upstream/main` (local, HIGH)
- `.planning/MERGE-CHECKLIST-v1.7.0-tail.md`, `.planning/scripts/verify-merge-invariants.sh`, `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `compare-confluent-archives.py`, `confluent-archive-allowlist-32.txt` (local, HIGH)
- `docs/FORK-DELTA.md` rows for ORCH-02, WRT-11, CMP-01..03 and Known costs 8 (local, HIGH)
- ClickHouse Java client release notes v0.10.0 and v0.11.0 (github.com/ClickHouse/clickhouse-java/releases), and the Dependabot PR #17 body on `ne1r0n/clickhouse-kafka-connect` (MEDIUM: release text, nothing executed)
- Maven Central POMs for `client-v2`, `clickhouse-client`, `clickhouse-http-client`, `clickhouse-data` at `0.9.6` and `0.11.0` (repo1.maven.org; HIGH for dependency closure)
- `gh api repos/actions/setup-java/git/ref/tags/v6.1.0` -> `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` (HIGH)
- Upstream `CHANGELOG.md` Unreleased entry for `additionalRetriableErrorCodes` (issues 847, 455) (HIGH)

---
*Architecture research for: v1.7.0_gg6-maintenance integration*
*Researched: 2026-10-28*
