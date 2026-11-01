# Feature Research — v1.7.0_gg6-maintenance

**Domain:** Maintenance milestone on a Kafka Connect sink fork (retriable-error config, client 0.9.6 -> 0.11.0 operator-visible behaviour, release-version bump workflow, CI archive compare)
**Researched:** 2026-10-28
**Confidence:** HIGH for the upstream tail, `increment-version.yml` and the archive compare (read from code in this repo). MEDIUM for the client 0.11.0 interactions (release notes read, not yet run against the connector).

Scope: only the NEW work. Existing fork features are not re-researched.

## 1. `additionalRetriableErrorCodes` (upstream commit c7303abc, merged on upstream/main as 9bed6f79)

### What upstream built (read from `git show c7303abc`)

| Aspect | Upstream behaviour |
|--------|--------------------|
| Key and type | `additionalRetriableErrorCodes`, `ConfigDef.Type.LIST`, default `""`, Importance LOW, width LONG |
| Parse | `parseErrorCodes`: comma-split, trim, skip empty tokens, `Integer.parseInt`, reject `<= 0` and non-integers (`abc`, `21.6`, `-1`, `0`, `99999999999`), de-duplicate into an unmodifiable `TreeSet` |
| Validation | `ErrorCodeListValidator` accepts the Connect-parsed `List<String>` form and a raw string. It throws `ConfigException`. The `ClickHouseSinkConfig` constructor throws a bare `IllegalArgumentException` for the same bad values (the constructor reads the raw prop map, not the validated `ConfigDef`), and the unit test pins both |
| Plumbing | `Utils.handleException(e, errorsTolerance, retryOnSocketException, additionalRetriableErrorCodes, records)`, a signature change with all callers updated: `ProxySinkTask` and `ChunkFlusher`. `ChunkFlusher` falls back to `emptySet()` when its config is null |
| Built-in list | The 15-code `switch` becomes `Utils.DEFAULT_RETRIABLE_ERROR_CODES` (`Set.of`, public static). The set is `3, 107, 159, 164, 202, 203, 209, 210, 241, 242, 252, 285, 319, 425, 999`. A code is retriable if it is in the default set OR the additional set. Additional codes can only add, never remove |
| Retriable outcome | `RetriableException` straight from the code check, before the `DataException`, socket and IO branches. It bypasses `errors.tolerance` and the DLQ, and has no retry budget (the config text and CHANGELOG say so) |
| Client classes | Both V2 `ServerException.getCode()` and V1 `ClickHouseException.getErrorCode()` are classified through the same set. The root cause is found by `getRootCause(e, true)` |
| Test seam | `ProxySinkTask` gains a package-private constructor taking a `DBWriter`, so tests inject a fail-once writer. `AdditionalRetriableErrorCodesTest` pins the redelivery. `UtilsTest` pins V1 and V2, and a parameterised test pins every default code |
| Motivation | 216 `QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING`. Client V2 re-sends with the same query id after a `NoHttpResponseException`, the first attempt is still running, and the task dies. Upstream issues 847 and 455 |

### What the fork's requirements must pin

The fork's `Utils.handleException` is currently byte-identical to the pre-c7303abc upstream shape (fork delta in `Utils.java` is the merge-order difference only). The tail merge is therefore a clean take-upstream with one hazard.

- The `handleException` signature change breaks every fork caller and every fork test that calls the 4-argument form. Requirement: grep all fork-added call sites (`ClickHouseWriter` paths, tests in `src/test` and `src/testFixtures`, `WriterFailureClassificationUnitTest`) and migrate them in the same commit.
- The fork's value-free failure path (`WithheldMessageException`, `DataException` from `doWriteColValue`) must still reach `handleException` as a non-server error. Pin that a configured code cannot reclassify a `DataException`: only a server-error root cause is checked against the set.
- Classification order is unchanged: server code check first, then `DataException` when tolerance is off, then `SocketTimeoutException`, `UnknownHostException`, `retryOnSocketException` + `SocketException`, then the IOException timeout-message match. The fork pins this order in `WriterFailureClassificationUnitTest`; upstream's tail does not move it.
- `retryOnSocketException` exists in the tree but is not adopted by the fork (FORK-DELTA item 9). `additionalRetriableErrorCodes` follows the same rule: the config exists after the merge. Requirement: record in FORK-DELTA whether it is adopted or merely present, and state the default (empty) so a connector without the key is bit-for-bit as `v1.7.0_gg5`.
- Config surface is a high-risk file (`ClickHouseSinkConfig`). Requirement: a `ConfigDef` pin that the key is defined once, LIST type, default empty, and a pin that the connector's `config()` / validate endpoint rejects a bad value with a `ConfigException` (the REST validation path), not only the constructor path.

### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Merge c7303abc and the 4-arg to 5-arg signature migration in one commit | Fork callers and tests will not compile otherwise | LOW | Run `verify-merge-invariants.sh`. Every fork test with a `handleException` call gets `Collections.emptySet()` |
| Default is empty and behaviour is unchanged when unset | An existing connector must not change on upgrade | LOW | Pin: `ClickHouseSinkConfig` with no key gives an empty set. A rollout note line says "no change unless set" |
| Parse/validate matrix pinned (blank, whitespace, duplicates, `abc`, `21.6`, `-1`, `0`, int overflow) | Operators paste code lists by hand | LOW | Upstream tests cover it. Take them as-is and add one case: a trailing comma and a `Connect-parsed List` form |
| A configured code yields `RetriableException`; an unconfigured one yields the same `RuntimeException` as today | The only contract of the feature | LOW | Both client classes (V2 `ServerException`, V1 `ClickHouseException`) |
| Every default code still retriable, with or without extras | Guards the `switch` to `Set` rewrite | LOW | Upstream's parameterised test; keep it |
| CHANGELOG and rollout line stating "no retry budget, bypasses `errors.tolerance` and the DLQ" | A wrongly listed permanent code loops forever and stalls the partition | LOW | Operator-facing and the main footgun |
| FORK-DELTA / anchors updated for the merged files | Fork-anchors invariant counts `Utils.java` and `ClickHouseSinkConfig.java` rows | LOW | Counts may shift; check `fork-anchor-exemptions.txt` |

### Differentiators (worth doing, the fork can stay without them)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Live pin of 216 on a real server via `FailOnceDBWriter`-style fault injection against ClickHouse `latest` | Shows the motivating case end to end, not just a mock | MEDIUM | Real 216 is hard to provoke. A writer-level fail-once over a live table is enough and matches the fork's "read back and assertEquals" rule |
| Pin the interaction with client 0.11.0 (see section 2): 159 stays retriable at the connector even though the client stopped retrying it | Proves the two layers do not leave a gap | LOW | One unit test with `ServerException(159, ...)` |
| WARN log naming the configured extra codes once at `start()` | Operators can see the setting took effect | LOW | Must not log anything but integers. Not in upstream |

### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| A retry budget or backoff for additional codes | "Do not loop forever on a mis-listed code" | Diverges from upstream in a high-risk file. The Connect framework owns redelivery | Document it. Operators use `errors.retry.*` only for the framework's own paths |
| A "remove from default list" companion setting | Operators want 159 non-retriable after the client change | New surface beyond upstream's tail, out of scope ("no new connector features beyond what upstream's tail brings") | Defer. Note it as a future requirement if 159 storms appear |
| Mapping names (`TOO_MANY_PARTS`) instead of numbers | Readability | Needs a name table that drifts across server versions | Numbers only, as upstream |
| Honouring the set for non-server errors | "Make socket errors configurable too" | `retryOnSocketException` already covers the socket case | Keep the server-code scope |

## 2. Client 0.9.6 -> 0.11.0 behaviour changes that surface to operators

Source: GitHub release notes for clickhouse-java v0.9.7, v0.9.9, v0.10.0 and v0.11.0. The jump skips 0.10.0 (PR #4 was left unmerged), so 0.10.0 and 0.9.7-0.9.9 changes arrive together. Items marked (fork) touch code the fork changed.

| Change in the client | Operator-visible effect | Interaction with the fork | Requirement to pin |
|----------------------|-------------------------|---------------------------|--------------------|
| 0.11.0: server error 159 `TIMEOUT_EXCEEDED` no longer retried by the client (own cause `ServerTimeoutExceeded`, not in default `client_retry_on_failures`) | The client's "Retrying." log lines for 159 disappear. Each 159 goes to the connector at once | 159 is in the connector's default retriable list, so it still becomes a `RetriableException` (Connect redelivery) rather than a task failure. What changes is who retries, not whether | One unit pin: `ServerException(159)` through `handleException` with an empty extra set is `RetriableException`. A rollout line: expect more connector-level `Deciding how to handle exception` WARNs |
| 0.11.0: `socket_rcvbuf` / `socket_sndbuf` have no default | OS TCP auto-tune applies; throughput and memory can differ | The connector never sets them (grep: no `setSocket*` in `src/main`). Operators may pass them through `clickhouseSettings` or `jdbcConnectionProperties` | Pin that the connector sets neither. Rollout line only |
| 0.11.0: ZSTD added to the Block compression stream, "used by default not to compress client requests" | Request compression default is off; response compression handled by the fork's `compress=false` + `network_compression_method=lz4` | (fork) D-17 retired onto upstream's `setCompressionMethod`. `NetworkCompressionPinUnitTest` / `NetworkCompressionPinLiveTest` already pin the V2 bytes on 26.9 | Re-run both pins on 0.11.0 and re-measure FORK-DELTA Recorded Fact 26 (is `compress=false` alone still sufficient on V2?). Do not delete the workaround on the strength of the notes |
| 0.11.0: HTTP LZ4 compression for inserts moved to `lz4-java` streams | The client uses the same `net.jpountz.lz4` package as before | (fork) The Gradle capability rule picks `at.yawk.lz4:lz4-java`. PR #19 (1.12.1) rides with it. The shadowed archive must still hold exactly one lz4 | The archive compare allow-list (section 4) gets lz4 rows. `Lz4RoundTripLiveTest` must stay green |
| 0.11.0: `User-Agent` format changed (`lv:java/..`, `os:..`, `hc:ahc_..`) | Anyone parsing `http_user_agent` in `system.query_log` breaks | (fork) The client name carries the fork version (`CONNECT_CLIENT_NAME`, MBean `version=`) | Rollout line with the old and new string. One pin on the connector's client-name suffix |
| 0.10.0: HTTP 503 now surfaces as `java.net.ConnectException` and is retried by the client by default | A load balancer 503 is retried by the client | A `ConnectException` is a `SocketException`. In `handleException` it is retriable at the connector only when `retryOnSocketException=true`. Before, a 503 was a `ServerException` with no retriable code | Pin the 503 outcome with both `retryOnSocketException` values, so the classification is recorded rather than discovered |
| 0.10.0: unknown HTTP status codes throw `ClientException` instead of `ServerException` | Non-ClickHouse HTTP bodies (proxy 404, 502 with HTML) no longer carry an error code | Not a server error in `handleException`, so it falls to the final branch (RuntimeException, or ignored with tolerance) | One pin: `ClientException` is non-retriable. Rollout line |
| 0.9.7: `Client.Builder#build()` throws `ClientMisconfigurationException` on an unknown config key (`ignore_unknown_config_key=true` restores the old behaviour) | A connector whose `jdbcConnectionProperties` or `clickhouseSettings` carried a stray key now fails at `start()` | (fork) The connector forwards `clientBuilder.setOption(k, v)` for operator keys (`ClickHouseHelperClient` line ~202) | HIGH-value pin: a live test that an unknown option key fails `start()` with a clear message, plus the rollout line. Verify the connector does not forward JDBC-only keys such as `ssl` |
| 0.9.7: date/time read and write conversions rewritten (explicit timezone handling) | Stored `Date`/`DateTime` values can shift for non-UTC JVMs | The connector's RowBinary writer writes `Date`/`DateTime` itself | The existing round-trip suites are the gate. Add one non-UTC JVM-timezone run (`-Duser.timezone`) of the date/datetime live pins. Highest silent-corruption risk of the jump |
| 0.9.9 / 0.11.0: query-timeout and execution-timeout handling | Timeout error classes and messages differ | Message-matching branch in `handleException` (`"Read timed out after"`, `"Write timed out after"`) | Pin that those message prefixes are still produced by 0.11.0 (live: a tiny `socket_timeout`). If the text changed, the IOException branch silently stops retrying |
| 0.10.0 / 0.11.0: observability SPI (metrics, spans) | Optional | None; the connector registers no recorder | None. Not a feature to adopt |
| 0.10.0: `async_insert` / `wait_end_of_query` no longer hardcoded (jdbc-v2 only) | None | The connector uses client-v2, not JDBC | Out of scope; record as not applicable |

### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| A dedicated client audit with live pins before the bump lands | PROJECT.md says the bump "gets its own audit and live pins" | MEDIUM | Re-run the compression pins, the date/time round trips, the timeout-message pin and the unknown-key start pin |
| A 0.11.0 section in the rollout note listing each effect above | Operators read this before upgrading | LOW | Follow the `docs/ROLLOUT-v1.7.0_gg5.md` shape |
| FORK-DELTA pin row for `clickHouseDriver 0.11.0` and a refreshed D-17 / Recorded Fact 26 | The fork records every pin and why | LOW | Anchors file has a VERSION row; add the catalog pin the same way |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| A one-page "client behaviour diff" test class (`Client011BehaviourPinLiveTest`) holding the 159, 503, `ClientException` and unknown-key pins in one place | A later client bump re-runs one class | MEDIUM | Keeps changes additive and rebasable |
| Run the whole suite once with `ignore_unknown_config_key=true` absent versus present | Shows the operator escape hatch works | LOW | Only if the unknown-key pin finds the connector affected |

### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Setting `client_retry_on_failures` to restore 159 retries inside the client | "Keep the old behaviour" | New connector surface; the Connect layer already retries 159 | Document the effect. Add a setting only if production shows 159 storms |
| Adopting the metrics/span SPI | Free observability | New feature, new dependencies, out of scope | Defer |
| Removing the `network_compression_method=lz4` workaround because 0.11.0 adds ZSTD | Notes mention ZSTD | The fork measured the failure on 26.9 with 0.9.6. Not re-measured on 0.11.0 | Re-measure, then decide in a separate change |

## 3. `increment-version.yml` and `_ggN` versions

### Current behaviour (read from the workflow)

`semver.inc('v1.7.0_gg5', <patch|minor|major>)` returns `null` (checked with semver 7.8.5: `valid('v1.7.0_gg5')` is `null`). The step then writes `vnull` into `VERSION`, exports `NEW_VERSION=null`, creates the branch `update-version-null` and opens a PR. Nothing validates the result. The workflow header says "Known and left unfixed ... the fork bumps VERSION by hand".

Facts that shape the fix:
- Valid forms in this repo are `vX.Y.Z` and `vX.Y.Z_ggN` (the release workflows already reject anything else with the regex message "VERSION does not look like vX.Y.Z or vX.Y.Z_ggN").
- `_ggN` is the fork release counter on a fixed upstream base: history is `v1.6.0_gg1`, `v1.7.0_gg1` ... `v1.7.0_gg5`. A new upstream base restarts at `gg1`.
- `build.gradle.kts` reads `VERSION` verbatim (`file("VERSION").readText().trim()`) and inlines it as `Version.ARTIFACT_VERSION`. The anchors file has a row `VERSION<TAB>v1.7.0_gg5<TAB>1<TAB>BLD-07` that the merge-invariants check compares, so a bump must move that row too.
- Hyphen forms behave unlike underscore forms: `inc('v1.7.0-gg5','patch')` returns `1.7.0` (the prerelease is dropped), so switching the fork to `-ggN` would make `patch` silently remove the suffix. Do not rename the scheme.
- The `choice` input (`patch|minor|major`) is interpolated into a shell string. It is a closed set so it is safe today, but it stays safe only while it is a `choice`.

### Expected behaviour to pin

| Input VERSION | Dispatch | Result |
|---------------|----------|--------|
| `v1.7.0_gg5` | new `gg` | `v1.7.0_gg6` |
| `v1.7.0_gg5` | patch | `v1.7.1_gg1` (new upstream base restarts the counter) |
| `v1.7.0_gg5` | minor | `v1.8.0_gg1` |
| `v1.7.0_gg5` | major | `v2.0.0_gg1` |
| `v1.7.0` | patch / minor / major | unchanged semver result, e.g. `v1.7.1` |
| `v1.7.0` | gg | fail: no suffix to bump (or define as `v1.7.0_gg1`; pick one and pin it) |
| anything that does not match `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$` | any | fail before writing, exit non-zero, nothing committed |
| computed value null or not matching the same regex | any | fail before writing |

### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Never write `vnull`: validate the input and the output against one regex and exit non-zero | A silently corrupt VERSION breaks the build and the release | LOW | This alone closes the carried defect. It is the minimum acceptable fix |
| Handle `_ggN` for the three existing choices | The fork's only version shape since gg1 | LOW | Split suffix, bump the base with semver, reset the suffix to `_gg1` |
| A `gg` choice (bump the counter only) | It is the bump the fork performs every milestone | LOW | Adds an option to the `choice` list |
| The bump logic is testable without a dispatch | A workflow nobody can run is the reason the bug lasted | MEDIUM | Move the logic out of the inline `node -p` into a small script with a unit test (a JUnit test next to `WorkflowActionPinGuardUnitTest`, or a script test like `verify-merge-invariants-selftest.sh`). Table-driven over the matrix above |
| Keep every action SHA-pinned, `permissions` as today, no new `uses:` | The pin guard tests read this file | LOW | `WorkflowActionPinGuardUnitTest` and `TestsWorkflowPinGuardUnitTest` must stay green. Update the header comment (the "known and left unfixed" text becomes false) |
| The generated PR also updates the `VERSION` anchor row in `.planning/scripts/fork-anchors.txt`, or the PR body says it must be done | Otherwise the merge-invariants check fails on the bump PR | LOW | `.planning/` is tracked on `custom_build`. Decide in requirements which of the two |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Replace `npm install semver` with a pure bash/regex or tiny script | Removes the Node/npm dependency (and a pinned third-party package) from a write-scope workflow | LOW | The semver library is only needed for the plain `vX.Y.Z` branch, which is simple arithmetic. Optional |
| Pass the choice through `env:` instead of `${{ }}` interpolation | Removes the injection shape for good | LOW | Pin guard may need a rule |
| PR body names old and new version and reminds about CHANGELOG | Fewer forgotten release steps | LOW | Cosmetic |

### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Auto-tagging or auto-publishing from the bump | "One button release" | The release path has deliberate gates (human approval, green SHA, absent tag) | Bump PR only |
| Changing the scheme to `-ggN` (valid semver prerelease) | Library compatibility | `patch` then drops the suffix, and every tag, the MBean, the client name and the docs use `_ggN` | Keep `_ggN`, fix the script |
| Bumping `CHANGELOG.md` automatically | Convenience | The fork's CHANGELOG sections are hand-curated | Leave manual |
| Keeping the "left unfixed" note | Cheapest | The carried defect stays open | Fix and delete the note |

## 4. CI consumer for `confluent-archive-allowlist-*.txt`

### What exists

- `compare-confluent-archives.py A.zip B.zip ALLOWLIST` (stdlib only, run `python3 -I`). Exit 0 PASS, 1 FAIL, 2 unusable input. It reads the one `lib/*-confluent.jar` in each archive in memory, compares every entry by SHA-256 plus the outer entries (`manifest.json` minus `release_date` and `version`), and fails on any `UNALLOWED` difference, any `STALE-ROW` (an allow-list row that matched nothing), a manifest difference beyond `Implementation-Version`, or any nested jar.
- Allow-list files are per release and named by phase number: `-25`, `-29`, `-32`. Each is "A = previous tag rebuilt from the tag, B = tip". A row is `glob<TAB>reason`, a star crosses slashes, a reason is required, and `com/clickhouse/` rows must be exact paths.
- Today only `verifyConfluentArchive` (structure checks) runs in CI. The entry-by-entry compare is a manual release-time step (PROJECT.md: "CI consumer for the archive compare"). The compare script has no automated test of its own (grep found no test referencing it).
- `tests.yaml` is single-job, `ubuntu-24.04`, 45 min, read-only token, `persist-credentials: false`, default shallow checkout, push-to-any-branch trigger, a build step `./gradlew createConfluentArchive`.

### Expected behaviour of a CI consumer

A CI job rebuilds the baseline archive from the previous release tag, builds the archive at the tip, and runs the script with the allow-list for the release in progress. The job fails on UNALLOWED or STALE-ROW. It is the only mechanism that detects an unintended change to the shipped jar (a dependency drift, a shaded-class change), which is the Shadow 9 nested-jar incident class.

### Design decisions the requirements must pin

1. **Allow-list naming.** `-25/-29/-32` are phase numbers, which CI cannot derive. Pin one rule: `confluent-archive-allowlist-<VERSION>.txt` (the exact content of `VERSION`) or a single `confluent-archive-allowlist.txt` replaced per release. Keying by `VERSION` makes a forgotten allow-list fail loudly at the first push after a bump. Move the existing three files by rename only (history is the evidence of past releases).
2. **Baseline.** The previous release tag (`git describe --tags --abbrev=0 --match 'v*_gg*'` or an explicit file/variable). The baseline must be built from the tag in a separate `git worktree`, never in place. Needs `fetch-depth: 0` (or a tag fetch). Say what happens when the baseline equals the tip (no tag past VERSION): the compare is a pass with an empty allow-list.
3. **Cost.** A second `createConfluentArchive` adds roughly one archive build to a ~6.5 minute workflow. Acceptable on a 45 minute limit; run it as a step after the existing archive step, with Gradle caches shared through `setup-gradle`.
4. **Where the script lives.** It is in `.planning/scripts/`. `.github/scripts/` already holds `jacoco_summary.py`. A CI-run script under `.planning/` couples CI to planning files, which a PR branch that filters `.planning/` commits would lose. Pin one: copy or move to `.github/scripts/` with the allow-lists beside it, and keep the invocation identical.
5. **Release vs push.** On a feature-branch push where `VERSION` equals the last tag, the allow-list does not exist. Pin the rule: the compare runs only when `VERSION` differs from the latest tag, or when an allow-list for the current `VERSION` exists. Otherwise a normal push would fail.
6. **Pin guards.** `TestsWorkflowPinGuardUnitTest` reads `tests.yaml` (step order, permissions, trigger, `fetch-depth` is not currently allowed to change silently). Extend it for the new step and keep SHA-pinned actions only. The new job must not use a write scope.
7. **Self-test of the script.** Add a synthetic-archive unit test (as `ConfluentArchiveCheckUnitTest` does for the verify task): PASS, UNALLOWED, STALE-ROW, nested jar, manifest change, malformed allow-list (exit 2), duplicate inner jar. Without it the CI gate is untested.
8. **gg6 will be large.** The client `0.11.0`, lz4 `1.12.1`, okhttp (test-only, not in the archive), `additionalRetriableErrorCodes` (`ClickHouseSinkConfig`, `Utils`, `ProxySinkTask`, `ChunkFlusher`) and the version identity all change entries. Write the gg6 allow-list from the first real compare output, one row per library or exact `com/clickhouse/` path, each reason naming the version bump (the 25/29 files are the template).

### Table stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| CI step that builds baseline from the previous tag and runs the compare against the release's allow-list | It is the whole deliverable | MEDIUM | Worktree build, `python3 -I`, fail on exit 1 or 2 |
| Allow-list naming that CI can resolve from `VERSION` | Phase-numbered names cannot be automated | LOW | Rename the three existing files |
| Compare output in the step summary and a `::notice` annotation | Job logs are unreadable without admin rights (the fork-count step exists for the same reason) | LOW | Print the `VERSION` lines and `RESULT:` line |
| Script self-test | A gate with no test is a trap | MEDIUM | See point 7 |
| Pin-guard update | The guard fails otherwise | LOW | Same file family as the other workflow guards |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Upload the compare report as an artifact | Review of a large allow-list diff | LOW | Retention 14 days, same as coverage artifacts |
| A "regenerate allow-list draft" mode printing rows for each UNALLOWED difference | Writing 50 rows by hand is the cost of every release | MEDIUM | Output is a draft only; a human still writes each reason |
| Compare in the release workflows too, as a gate before `gh release create` | The published archive is the compared one | MEDIUM | The release path already builds it. Do it only after the push-CI version works |

### Anti-features

| Anti-Feature | Why Requested | Why Problematic | Alternative |
|--------------|---------------|-----------------|-------------|
| Auto-generating the allow-list in CI | Stops the red builds | The allow-list is the review of what changed; generating it makes the gate vacuous | Draft mode for humans only |
| A catch-all or `com/clickhouse/*` row | Faster green | The script's own rules forbid it, and it hides the connector's classes | Exact paths |
| Downloading a published GitHub release asset as baseline | No second build | Release assets were not published for earlier tags (gg5 was the first real release); the tag build is reproducible | Build from the tag |
| Failing every push on a pre-release version | Strictness | A shallow branch push with no allow-list would block unrelated work | Rule 5 above |

## Feature Dependencies

```
Upstream tail merge (c7303abc + CreateTableStatement test refactors)
    └──requires──> scratch branch, rollback tag, verify-merge-invariants.sh, two full runs
    └──changes──> Utils.handleException signature ──requires──> migration of every fork caller and test

Client 0.11.0 bump (PR #17)
    └──requires──> own audit and live pins (section 2)
    └──requires──> lz4 1.12.1 (PR #19) decision on the same archive
    └──interacts──> additionalRetriableErrorCodes (159, 503, ClientException classification pins)
    └──interacts──> D-17 / setCompressionMethod pins (re-measure Recorded Fact 26)

Archive allow-list CI consumer
    └──requires──> allow-list naming rule, baseline-from-tag worktree, script self-test
    └──requires──> the gg6 content (client, lz4, tail) final, so the gg6 allow-list is written last
    └──requires──> tests.yaml pin-guard update

increment-version.yml fix
    └──independent of the connector code
    └──interacts──> fork-anchors VERSION row, release workflows' VERSION regex

Release (VERSION=v1.7.0_gg6)
    └──requires──> all of the above, rollout note, independent review of operator docs
```

### Dependency notes

- **Tail merge before client bump:** both touch `Utils`/classification pins. Merge the tail first so the client audit pins run against the final `handleException`.
- **Archive compare before release:** its gg6 allow-list can only be written once client, lz4 and the tail are in. It is the last code-side item.
- **`increment-version.yml` is parallelisable** with everything except the pin-guard test files it shares with the archive step.
- **FORK-DELTA / anchors** are touched by the tail, the pins and the version fix. Run the anchors check after each merge.

## MVP Recommendation

Prioritise:
1. Tail merge with the signature migration and default-empty pin (LOW, unblocks everything).
2. Client 0.11.0 audit: 159, 503 and `ClientException` classification pins, compression re-measure, unknown-key start pin, date/time non-UTC run.
3. `increment-version.yml`: validate-then-write plus `_ggN` handling and a table-driven test.
4. Archive compare in CI with the naming rule, baseline-from-tag, self-test, pin-guard update.
5. Rollout note section for each operator-visible effect above.

Defer:
- Draft-mode allow-list generation: nice, not needed to close the caveat.
- Compare gate in the release workflows: after the push-CI version proves stable.
- Removing the pure-semver dependency from `increment-version.yml`: only if the fix is already touching the step.

## Open questions for requirements (for the user)

- Is `additionalRetriableErrorCodes` adopted (documented as supported) or merely present, as `retryOnSocketException` is?
- What should `gg` do on a suffix-less `vX.Y.Z`? (fail, or create `_gg1`)
- Should the bump PR also edit `fork-anchors.txt`, or document it?
- Allow-list naming: keyed by `VERSION`, or one file replaced per release?
- Does the connector forward any JDBC-only or unknown key to `Client.Builder#setOption`? (needs a code read before the 0.9.7 `ClientMisconfigurationException` change can be classified as affected or not)

## Sources

- `git show c7303abc` and `git log upstream/main` in this repo (HIGH).
- `src/main/java/com/clickhouse/kafka/connect/util/Utils.java`, `ClickHouseHelperClient.java`, `.github/workflows/increment-version.yml`, `tests.yaml`, `release.yaml`, `.planning/scripts/compare-confluent-archives.py` and `confluent-archive-allowlist-25|29|32.txt`, `docs/FORK-DELTA.md` (HIGH).
- clickhouse-java release notes v0.9.7, v0.9.9, v0.10.0, v0.11.0 via `gh release view` (HIGH for text, MEDIUM for effect on this connector: not run yet).
- upstream issue ClickHouse/clickhouse-kafka-connect#847 (216 motivation) via `gh issue view` (HIGH).
- semver 7.8.5 behaviour checked locally with `node -p` (HIGH).
