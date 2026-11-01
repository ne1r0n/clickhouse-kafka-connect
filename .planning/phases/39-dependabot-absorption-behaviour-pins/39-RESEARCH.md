# Phase 39: Dependabot Absorption & Behaviour Pins - Research

**Researched:** 2026-11-01
**Domain:** Fork maintenance of a Kafka Connect sink (Gradle/Java 17): absorbing two Dependabot PRs, Docker-free behaviour pins, one integration-harness fix. No `src/main` change.
**Confidence:** HIGH. Every state claim below was read from the tree or measured this session. The three MEDIUM/ASSUMED items are in the Assumptions Log.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md `## Decisions` holds only this (discuss phase skipped via `workflow.skip_discuss`):

> ### Claude's Discretion
> All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Claude's Discretion
All implementation choices (see above). Constraints that still bind, from CONTEXT.md `## Phase Boundary` and its Note:
- These two bumps are the user's requirements. No other dependency moves in this milestone without asking (AGENTS.md).
- New tests use no Mockito, `mockStatic` or `mockConstruction`, because of the OpenJ9 crash. Use subclass seams and `StubClickHouseServer`.
- GHSA-chpw-2mqm-95cc does not apply here, so do not claim that 6.4.1 closes a vulnerability.
- `setup-gradle` 6.4.1 first runs on GitHub at the first push. A push needs the user's direct approval (User-Owned Gates). The planner either asks for a scratch push in this phase, or leaves the first run to the Phase 44 rehearsal push. (**See Finding F2: this premise is partly stale.**)
- `src/main` does not change.
- Phase gate: cache-busted `testUnit` with Docker unreachable, a full suite on `latest` x V2 with `SELECT version()` recorded, the anchors invariant, and an empty `src/main` diff.

### Deferred Ideas (OUT OF SCOPE)
None — discuss phase skipped.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DEP-01 | gson `2.15.0` absorbed in its own commit; full suite on `latest` x V2 and `testUnit` green twice; FORK-DELTA gson row updated | F1 (merge shape), F3 (measured gate numbers on a throwaway copy of the tree), F5 (FORK-DELTA row 72), Gate Recipe |
| DEP-02 | `setup-gradle` `6.4.1` pinned to `8da46b53…` with `# v6.4.1` in four workflows; release/pre-release stay equal; `integrationTests.yaml` untouched; guard classes green; FORK-DELTA rows updated | F2 (PR head already green on GitHub), F4 (guard classes measured green with no test edit), F5 (FORK-DELTA row 79), Gate Recipe |
| PIN-02 | Wrapped `DataException` behaviour pinned Docker-free through `ProxySinkTask.put` under both tolerance values | F6 (measured outcome matrix), Pattern 1, Pitfalls 4-5 |
| PIN-03 | `custom_*` forwarding on client 0.11.0 pinned Docker-free, labelled a client-version tripwire | F7 (re-measured URIs), Pattern 2, Pitfall 6 |
| PIN-04 | F-3 fixed in `src/integrationTest` only; verified by one `integrationTest` run | F8 (what F-3 is at HEAD), Pattern 3, Open Question 1 |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Read from `./CLAUDE.md` -> `AGENTS.md` and `./.claude/CLAUDE.md`. They bind the plans:
- **Do not make commits without the workflow asking; do not change PR title/description.** (AGENTS.md Review rules.) Research here commits nothing.
- **GSD workflow enforcement:** repo edits go through a GSD command (`/gsd-execute-phase` for planned phase work).
- **Never delete or weaken an existing test; only add tests** (AGENTS.md Testing Discipline). Read `ClickHouseTestHelpers.java` before writing tests. When writing records, read back and `assertEquals` (n/a here: no ClickHouse writes).
- **Spotless:** `./gradlew spotlessApply` after changes. In this repo Spotless checks import order and unused imports only (`importOrder()` and `removeUnusedImports()`; `googleJavaFormat()` is commented out in `build.gradle.kts`). **Do not run it repo-wide** (see Pitfall 8).
- **Do not upgrade any dependency without asking**: the two bumps are user requirements; nothing else moves.
- **Behaviour preservation / rebasability:** additive new test files or cases, small diffs; the fork runs in production.
- **Test target:** ClickHouse `latest` + client V2 only. Docker is colima profile `vz` (bare `colima status` hits the wrong profile).
- **Java 17, Lombok, SLF4J, JUnit 5; static string constants; no secrets or record data in logs.**
- **Environment (STATE.md + MEMORY):** this shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal `null` (re-measured: `THO=null`). Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. A bare second `./gradlew test` echoes the first run: bust the cache with `--rerun`.

## Summary

Phase 39 is five independent pieces of work on a tree with no `src/main` change. Both Dependabot PRs are still open, unmerged, and merge cleanly into `sync/v1.7.0` today. The gson bump is a one-line catalog change; the `setup-gradle` bump is the same line in four workflow files. Both PR heads **already ran `tests.yaml` green on GitHub** (runs 41297643137 and 41297596686), so the "first run on GitHub" risk in CONTEXT.md is largely retired without any push by us. I applied both bumps to a throwaway copy of the tree (`git archive`, outside the repo) and measured: six workflow-guard classes pass (184 tests) with no test edit; `testUnit` is 1672 tests with 2 skips; the full suite on ClickHouse 26.10.3.7 (`latest`) with client V2 is 2270 tests, 18 skipped (7 named + 11 state-mismatch cases), 0 failures.

The two pins are cheap and fully Docker-free. For PIN-02 I measured today's outcome of every wrapped-`DataException` shape through the real `ProxySinkTask.put` under both tolerance values (matrix in F6). The key subtlety: `Processing.doInsert` wraps every writer failure in `RuntimeException(queryId, e)`, so `Utils.handleException` never sees a bare `DataException` from the writer; the "direct `DataException` is rethrown under `none`" branch is unreachable through `put`. PIN-03 is the existing throwaway probe `ZzCustomProbeTest` made permanent. I re-ran it against client 0.11.0 this session; the result matches the Phase 36 record, with one trap: the order of the forwarded query parameters is not stable.

PIN-04 (F-3) needs a scope decision. At HEAD, the one counter in the harness (`generateData`) is already scoped to its own connector by name, and `getConnectors()` (the all-connectors accessor) is only logged at two sites. So "fix F-3" at HEAD means: pin it with a leftover-connector regression, remove or scope the unscoped accessor, and apply the Phase 36 review's IN-06 hygiene. One `integrationTest` run (about 33 minutes in the nightly) records it.

**Primary recommendation:** Absorb #22 then #21 each as its own `--no-ff` merge of the fetched PR head (using `git merge --no-commit` so the FORK-DELTA row lands in the same commit), gate each, then add the PIN-02 and PIN-03 test files (new files or one additive case, no `src/main`), then the F-3 harness change plus one local `integrationTest` run, then the phase gate. No scratch push is needed in this phase.

## Architectural Responsibility Map

This is a build/CI/test phase, so tiers are build-system tiers, not web tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| gson version | Gradle version catalog (`gradle/libs.versions.toml`) | `build.gradle.kts` (3 configurations consume it) | One catalog line feeds `implementation`, `clickhouseDependencies`, `testFixturesImplementation` |
| `setup-gradle` pin | GitHub Actions workflow files (4) | `docs/FORK-DELTA.md` (documentation only) | Pin lives in `uses:` lines; no test ties the doc to the workflow |
| Wrapped-`DataException` classification | `Utils.handleException` (called from `ProxySinkTask.put`) | `Processing.doInsert` (wraps) | The wrapper is built in `Processing`; classification is in `Utils`; the pin must cross both |
| `custom_*` forwarding | ClickHouse Java client 0.11.0 (`Client.Builder.setOption`) | `ClickHouseHelperClient.createClientV2` (feeds options) | The connector forwards every `key=value`; the client decides what reaches the wire |
| F-3 connector counting | `src/integrationTest` harness (`ConfluentPlatform`) | Kafka Connect REST API (`/connectors/{name}/status`) | Test-only; no `src/main` involvement |

## Standard Stack

### Core
No new libraries, plugins or tools. Two version bumps of existing components.

| Component | From -> To | Purpose | Verified |
|-----------|-----------|---------|----------|
| `com.google.code.gson:gson` | 2.14.0 -> **2.15.0** | Serialize-only JSON (`DataJson.GSON`, `ConvertToMapOrJson` `jsonLibrary=gson`) | `[VERIFIED: Maven Central]` `maven-metadata.xml` `<latest>2.15.0</latest>`, `<release>2.15.0</release>`, `lastUpdated 20261024012745`; `gson-2.15.0.jar` returns HTTP 200 |
| `gradle/actions/setup-gradle` | v6.4.0 -> **v6.4.1** | Gradle setup/cache in 4 workflows | `[VERIFIED: git ls-remote https://github.com/gradle/actions.git]` `refs/tags/v6.4.1` = `7296c772c13d12d9871528814b51fb2143c138ef` (tag object), `refs/tags/v6.4.1^{}` = `8da46b539cd8fe6ab27ca75128919a18d757604c` (the commit to pin) |

### Supporting (existing, reused by the new tests)
| Existing class | Location | Use here |
|----------------|----------|----------|
| `StubClickHouseServer` (package-private) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/` | PIN-03: real V2 client over a real loopback socket, no Docker |
| `ClientErrorShapes.line()` / `Line` | same package | PIN-03: the client-version switch that makes the pin a tripwire |
| `InMemoryDBWriter` | `src/main/java/com/clickhouse/kafka/connect/sink/db/` | PIN-02: subclass to inject a failure |
| `InMemoryDLQ` | `src/test/java/com/clickhouse/kafka/connect/sink/dlq/` | PIN-02: count and inspect DLQ records |
| `ServerExceptions.create(code, message, transportStatus)` | `src/test/java/com/clickhouse/kafka/connect/sink/util/` | PIN-02: builds a `ServerException` on any client line |
| `SchemalessTestData.createPrimitiveTypes(topic, partition, n)` | `src/testFixtures/.../sink/helper/` | PIN-02: a batch of `SinkRecord`s |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `git merge --no-ff --no-commit <PR head>` + edit + `git commit` | `git merge --no-ff` then a second docs commit | Criterion 1 requires the FORK-DELTA row "from its own commit"; two commits break that. The gg6 sweep (`6bf1c43b`) merged with no doc edit; this phase needs the `--no-commit` variant |
| New test class for PIN-03 | Add one `@Test` to `ClientEndpointAndOptionsPinUnitTest` | The sibling already has the private `insertOnce(...)` helper (DRY). Adding a case is additive and allowed; a new class must duplicate ~15 lines |
| WireMock/MockWebServer/Toxiproxy | `StubClickHouseServer` | Rejected by milestone STACK research: new test dependency, Shadow/archive surface |

**Installation:** none. The bumps are:
```
gradle/libs.versions.toml:  gson = "2.15.0"
.github/workflows/{tests,nightly-integration-tests,release,pre-release}.yaml:
        uses: gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1
```

**Version verification:** done above against Maven Central and the gradle/actions git remote this session. Both PRs' diffs were read from `git diff origin/custom_build...origin/dependabot/...`: PR #22 changes only `gradle/libs.versions.toml`; PR #21 changes only the four workflow files (one `uses:` line each).

## Package Legitimacy Audit

No new package is introduced. Both changes are version bumps of components already shipped, proposed by Dependabot and required by the user.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `com.google.code.gson:gson` 2.15.0 | Maven Central | 15+ yrs (existing dependency; 2.15.0 published 2026-10-24) | n/a (not exposed by the seam) | github.com/google/gson | Not checkable by the seam (it supports npm/pypi/crates only) | Approved: existing dependency, same coordinates, user requirement. `[VERIFIED: Maven Central]` |
| `gradle/actions/setup-gradle` v6.4.1 | GitHub Action (not a package) | existing pin | n/a | github.com/gradle/actions | n/a | Approved: pinned by commit `8da46b53…`, user requirement |

**Seam note (cross-ecosystem confusion, recorded so nobody acts on it):** `package-legitimacy check --ecosystem npm gson` returned `SUS`. That verdict is for the unrelated **npm** package named `gson`; it says nothing about the Maven artifact `com.google.code.gson:gson`. The seam has no Maven ecosystem. No `checkpoint:human-verify` is needed for an existing dependency's version bump.
**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none that apply (the npm `gson` verdict is an ecosystem mismatch)

## Findings (measured or read this session)

### F1. PR state and the absorption shape
- Both PRs open and unmerged: `[VERIFIED: GitHub REST API, unauthenticated]` #22 "Bump com.google.code.gson:gson from 2.14.0 to 2.15.0", head `b5cccbd324635d2fb19fc225e09bb59f7fe81590`; #21 "Bump gradle/actions/setup-gradle from 6.4.0 to 6.4.1 in the github-actions group", head `14be67ce7b4e97682bdf54cda89b73d45978a2b1`. They are the only two open PRs. `git ls-remote origin refs/pull/21/head refs/pull/22/head` returns the same two SHAs; `origin/custom_build` = `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`.
- `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` is **`1 16`** today, not the roadmap's `1 13`: the right count grew by local planning commits. The left `1` is the gg6 merge `72e8ccff` (parents `3a3a15a9` and `a46be37f`, the second being X, the gg6 scratch tip, an ancestor of HEAD). `git diff --stat 72e8ccff HEAD -- . ':!.planning'` is empty: the non-planning tree equals the published tree, and `git diff X 72e8ccff` is empty.
- Both PR heads have `72e8ccff` as parent. Dry-run `git merge-tree --write-tree HEAD <head>` for each prints a tree OID with no conflict lines, so both merge cleanly. The first merge pulls `72e8ccff` into this branch's history (its tree equals this branch's tree); afterwards `origin/custom_build...HEAD` reads `0 N`.
- Dirty `.planning/config.json` and `.planning/state.json` are not touched by either PR, so `git merge` proceeds; never `git add -A`.
- **If the user merged either PR on origin before execution**, absorb `origin/custom_build` instead and re-run the anchors gate (research flag in ROADMAP). Re-run the three lines above immediately before the first merge, and record the SHAs and time (Dependabot rebases PR branches when `custom_build` moves, so heads can change).

### F2. `setup-gradle` 6.4.1 has already run green on GitHub (CONTEXT premise is partly stale)
`[VERIFIED: GitHub Actions API]` Because `tests.yaml` triggers on every branch push, Dependabot's own PR branches ran it:
- head `14be67ce` (the `setup-gradle` 6.4.1 bump): run `41297596686`, workflow path `.github/workflows/tests.yaml`, `success`, job "ClickHouse latest Client version V2 tests" 10:27:57Z to 10:33:18Z, step 4 "Set up Gradle" `success`, all 11 main steps `success`.
- head `b5cccbd3` (gson 2.15.0): run `41297643137`, `success`.
The `uses:` line in the merge will be byte-identical to the one that ran. What is still unexercised on GitHub: the combined tree (both bumps), and the `workflow_dispatch`-only workflows (`nightly-integration-tests.yaml`, `release.yaml`, `pre-release.yaml`) with 6.4.1; their first run is the Phase 44 rehearsal. **Recommendation: no scratch push in Phase 39**; cite the two runs as evidence and leave the dispatch-only workflows to Phase 44. A push stays a user-owned gate in any case.

### F3. Gate numbers measured on a throwaway copy (not the real branch)
I extracted `git archive HEAD` to `/tmp/gsd39/wt` (outside the repo, no `.git`), applied both bumps with `sed`, and ran (JDK 21, `-PjavaVersion=21`, swap was 17.9 of 19.4 GB used so JDK 17/OpenJ9 was avoided):

| Check | Result |
|-------|--------|
| `dependencyInsight --dependency gson --configuration runtimeClasspath` | `com.google.code.gson:gson:2.15.0` (on the real tree today: `2.14.0`); one resolved version, `\--- runtimeClasspath` |
| Six guard classes, Docker unreachable (`DOCKER_HOST=unix:///nonexistent/docker.sock`), `testUnit --rerun` | 184 tests, 184 succeeded, 0 skipped, 15 s |
| Full `testUnit --rerun`, Docker unreachable | 1672 tests, 1670 succeeded, 0 failed, 2 skipped |
| Full `test --rerun` on ClickHouse `latest` x V2 | **2270 tests, 2252 succeeded, 0 failed, 18 skipped**, BUILD SUCCESSFUL in 2m 47s |
| `37-evidence-check.py skips build/test-results/test` | `OK skips: named=7 state_mismatch_cases=11 unexpected=0` |
| Docker-only delta | 2270 - 1672 = 598, equal to the value Phase 37 and 38 recorded |
| ClickHouse | `SELECT version()` = `26.10.3.7`; `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (same digest as the gg6 gate) |

These are the numbers the bump commits should reproduce **before** any new test is added. After PIN-02/PIN-03/PIN-04 land, totals rise; record the new baseline then, do not compare to 2270.

### F4. The "four pin-guard classes", and no test edit is needed
The roadmap does not name them. The Phase 37 evidence (`37-COMMAND-EVIDENCE.md` line 682) ran exactly: `ReleaseWorkflowGuardUnitTest`, `TestsWorkflowPinGuardUnitTest`, `WorkflowActionPinGuardUnitTest`, `DependabotProtobufIgnoreGuardUnitTest`. Run those four, plus `NightlyWorkflowGuardUnitTest` (the nightly file changes too) and `UpstreamIntegrationWorkflowBytesGuardUnitTest`; all six passed in F3.
- The only `v6.4.0` literals in tests are synthetic fixtures: `WorkflowActionPinGuardUnitTest.java:233` (`" # v6.4.0"`) and `TestsWorkflowPinGuardUnitTest.java:709` (`pinnedUses("gradle/actions/setup-gradle", "v6.4.0")`). They pass unchanged. **Do not edit them** (AGENTS.md: never weaken tests; PITFALLS: do not track the bump in fixtures).
- `integrationTests.yaml` SHA-256 is `07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109` (matches the `UpstreamIntegrationWorkflowBytesGuardUnitTest` pin recorded in STATE). Neither PR touches it.
- "release.yaml and pre-release.yaml stay byte-equal" is, literally, "their mutual diff does not change": they already differ in 7 hunks (title, `--prerelease`, expectation). Measured signature: `diff release.yaml pre-release.yaml | shasum -a 256` starts `ac7dce11e09bbe30` before and after the bump. Use that as the gate.
- Anchors: `.planning/scripts/fork-anchors.txt` has 172 rows; the setup-gradle rows are `.github/workflows/release.yaml<TAB>gradle/actions/setup-gradle@<TAB>1<TAB>REL-03` and the pre-release twin (the count stays 1). No anchor row contains a gson or setup-gradle version. Neither bump moves an anchor. Baseline `37-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172` printed `rows=172 bad=0` today.

### F5. FORK-DELTA rows that change
`grep -n "6\.4\.0\|2\.14\.0\|3f5f9ada" docs/FORK-DELTA.md` matches **only two lines** (file line numbers, not table-row numbers):
- **line 72** (version-catalog row): "...Jackson `2.22.4` (upstream `2.22.3`) and gson `2.14.0` (upstream `2.13.1`) in the shipped set..." -> `2.15.0`. Goes in the gson commit.
- **line 79** (`tests.yaml` row): "...(checkout v7.0.1, setup-java v6.1.0, setup-gradle v6.4.0 by its peeled commit, upload-artifact v7.0.2)..." -> `v6.4.1`. Goes in the setup-gradle commit.
Lines 81 (release/pre-release) and 86 (nightly) mention SHA pinning generically and carry no version, so they need no edit. The research's "rows 72, 76 and 79" is stale: line 76 is blank. The only other places with `2.14.0` are history and must not change: `CHANGELOG.md:186` and `docs/ROLLOUT-v1.7.0_gg4.md:310`. Phase 44 writes the gg7 CHANGELOG/rollout/allow-list gson rows.

### F6. PIN-02: today's outcome matrix through `ProxySinkTask.put` (measured)
`Processing.doInsert` (both overloads, `Processing.java:70-72` and `:94-96`) catches every writer failure and rethrows `new RuntimeException(queryId.toString(), e)`. `ProxySinkTask.put` then calls `Utils.handleException(e, errorTolerance, isRetryOnSocketException(), getAdditionalRetriableErrorCodes(), records)` with that wrapper. So through `put`, `e instanceof DataException` is **never true** for a writer failure.

I ran the real `ProxySinkTask(config, InMemoryDLQ, FailingWriter)` (package-private 3-argument constructor) with 3 records, `retryOnSocketException` default, no added codes:

| Failure injected into `DBWriter.doInsert` | `errors.tolerance=none` | `errors.tolerance=all` |
|---|---|---|
| plain `DataException("x")` | **fails the task**: `RuntimeException("Number of records: 3")`, cause `RuntimeException` (the queryId wrapper); DLQ 0 | **DLQ**: `put` returns, DLQ holds 3 (all records of the batch) |
| `DataException` caused by `ServerException` code 241 (default-retriable) | **`RetriableException`**, DLQ 0 | **`RetriableException`** (bypasses tolerance and DLQ), DLQ 0 |
| `DataException` caused by `ServerException` code 60 (not retriable) | fails the task (`Number of records: 3`) | DLQ 3 |
| `DataException` caused by `SocketTimeoutException` | **`RetriableException`** | **`RetriableException`**, DLQ 0 |
| `DataException` caused by `UnknownHostException` | `RetriableException` | `RetriableException`, DLQ 0 |
| (control) `RuntimeException` caused by `SocketTimeoutException` | `RetriableException` | `RetriableException` |

Notes the pins must state:
1. Under `none`, a `SocketTimeoutException`-rooted `DataException` is **retriable through `put`** only because the `Processing` wrapper hides the `DataException` from the `e instanceof DataException && !errorsTolerance` branch. Called directly on `Utils.handleException` (which is what `AdditionalRetriableErrorCodesPinUnitTest` does), the same bare `DataException` is rethrown unchanged under `none`. The two layers differ; the PIN-02 tests pin the `put` layer.
2. A `RetriableException` thrown by `put` carries the `RuntimeException` wrapper as its cause, whose cause is the original `DataException` instance (assert with `assertSame`, walking the chain).
3. `[VERIFIED: probe]` A `DataException` raised during **conversion** (`Record.convert`, before `Processing`), for example a `SinkRecord` with `Schema.INT32_SCHEMA` and value `5`, escapes `put` as a raw `DataException` ("No converter was found due to unexpected object type java.lang.Integer") under **both** tolerance values with DLQ 0: it never reaches `handleException`. This is outside the three shapes the roadmap names. Add it as an optional fourth control row if the planner wants the unwrapped path pinned; it is a finding worth recording either way.
4. `AdditionalRetriableErrorCodesPinUnitTest` already pins the plain-`DataException`-through-`put` case under `none` (non-retriable, `DataException` kept in the chain) and `assertDoesNotThrow` under `all` with a `null` reporter. Its javadoc says the two retriable wrapped cases are "deliberately not asserted here"; the new class asserts them. Leave that class unchanged.
5. FORK-DELTA "Known cost 17" (file line 232) says these `DataException` cases "stay retriable and no test asserts them". After PIN-02 that sentence is stale; update it in the PIN-02 commit (do not use future-tense publication claims; Phase 43 owns the past-tense sweep of lines 6, 139 and 286).

### F7. PIN-03: `custom_*` forwarding on client 0.11.0 (re-measured this session)
I compiled the Phase 36 probe against the project's test runtime classpath (obtained with a temporary Gradle init script in `/tmp`, nothing added to the repo) and ran it with the real `ClickHouseHelperClient.ClickHouseClientBuilder` against `StubClickHouseServer`, no Docker. `client-v2 impl = client-v2 0.11.0 (revision: c1834d0)`:

| `jdbcConnectionProperties` | Request URI the stub saw |
|---|---|
| `?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true` | `/?custom_myparam=7&custom_settings=max_threads%253D3&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4` |
| `?custom_myparam=7` | `/?custom_myparam=7&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4` |
| `?custom_a=b%20c` | `/?query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4&custom_a=b%2520c` |

This matches `36-04-SUMMARY.md` E-11 for 0.11.0. Facts: every `custom_*` key is forwarded; the operator's percent-encoding is doubled (`%3D` -> `%253D`, `%20` -> `%2520`), so the server would receive the literal text `max_threads%3D3`; `ssl` is not forwarded (already pinned by `ClientEndpointAndOptionsPinUnitTest`). **The position of the `custom_*` keys in the query string is not stable** (row 1: before `query=`; row 3: after `network_compression_method`). A raw-string equality assertion would be flaky. On client 0.9.6 nothing is forwarded `[CITED: 36-04-SUMMARY.md E-11, not re-measured; 0.9.6 is not on this classpath]`.
The connector code that feeds this: `ClickHouseHelperClient.createClientV2` splits `jdbcConnectionProperties` on `&`, then `=` (limit 2), and calls `clientBuilder.setOption(kv[0], kv[1])` for each pair, after `clientBuilder.setOption(IGNORE_UNKNOWN_CONFIG_KEY, "true")` `[VERIFIED: ClickHouseHelperClient.java:201-213]`. The operator-facing consequence is gg6 rollout row D7 in `docs/ROLLOUT-v1.7.0_gg6.md`.

### F8. PIN-04 (F-3): what the harness actually does at HEAD
`[VERIFIED: read src/integrationTest this session]`
- `ConfluentPlatform.generateData` (lines 499-534) names its connector `"DatagenConnector_" + topicName + UUID.randomUUID()`, creates it through the **private** `createConnector` (no RUNNING wait, because Datagen tasks finish in about 5 s), counts the marker `Stopping connector: generated the configured` in `getConnectorTasksStatus(connectorName).toString()` (the REST path `/connectors/{name}/status`), and deletes the connector in `finally`. That is already scoped to its own connector (the Phase 36 E-16 fix).
- `getConnectors()` (line 417, `/connectors?expand=status&expand=info`, every connector) is **only logged**: `ClickHouseSinkConnectorIntegrationTest.java:171` and `:183` (`LOGGER.info(confluentPlatform.getConnectors())`). No other caller exists in `src/integrationTest`, `src/test` or `src/testFixtures`.
- `createConnectorAndWaitUntilRunning` (line 366) checks the first task only and first polls after 5 s; it is used for the sink connector, whose tasks do not finish on their own. `36-FOLLOWUPS.md` records this as correct.
- So no remaining helper "counts across `getConnectors()` output". FORK-DELTA Known cost 15 (file line 228) says "Re-open trigger: ... the next edit of a `ConfluentPlatform` helper that counts across `getConnectors()`": PIN-04 is that edit.
- Remaining hygiene from `36-REVIEW.md` IN-06 (all in `generateData`): exit condition `finished != totalWorkers` (use `<`); `Pattern.compile(...)` recompiled on every poll; magic values `2 * 1000`, `120`; `long finished = 0;` dead initialiser; `deleteConnectors` in `finally` can throw `IOException` and mask the real "Data generation failed".
- Spotless baseline (measured on the throwaway copy): `spotlessCheck` fails today on exactly two files, both integration-test sources with import-order drift, `ClickHouseCloudTest.java` and **`ClickHouseSinkConnectorIntegrationTest.java`** (which holds the two `getConnectors()` log lines). `ConfluentPlatform.java` is clean. Do not run `spotlessApply` on them.
- Cost of the recorded run: nightly run 40474535605 was 63 tests, 0 failures, 9 skipped, 33 min `[CITED: STATE.md [CI] 2026-10-27]`; the seven `stockGen*` tests alone took 10m18s locally `[CITED: 36-USER-DISPATCH-FIX.md]`. Needed images are already cached locally (`cp-kafka-connect-base:7.9.7`, `cp-server-connect-datagen:0.6.4-7.6.0`, `cp-kafka:7.9.10`, `cp-schema-registry:7.9.10`, `toxiproxy:2.7.0`, `clickhouse-server:latest`, `ryuk:0.14.0`).

## Architecture Patterns

### System Architecture Diagram

```
Dependabot PR heads (origin)            sync/v1.7.0 (local, unpushed)
  #22 b5cccbd3 ──fetch──┐                    │
  #21 14be67ce ──fetch──┤   git merge --no-ff --no-commit
                        ▼        │
              [merge commit A: gson]  ── edit FORK-DELTA l.72 ── gate A
              [merge commit B: setup-gradle] ── edit FORK-DELTA l.79 ── gate B
                        │
        new test files (src/test, Docker-free)         harness change (src/integrationTest)
   PIN-02: ProxySinkTask.put ◄─ InMemoryDBWriter       PIN-04: ConfluentPlatform.generateData
           subclass throws DataException(cause)                + leftover-connector pin
           ─► Processing.doInsert wraps RuntimeException(queryId,e)    │
           ─► Utils.handleException ─► RetriableException │ RuntimeException │ DLQ (InMemoryDLQ)
   PIN-03: ClickHouseClientBuilder.setJdbcConnectionProperties(?custom_...)
           ─► Client.Builder.setOption ─► real V2 client ─► StubClickHouseServer (loopback)
           ─► assert parsed query map, switch on ClientErrorShapes.line()
                        │
                        ▼
   phase gate: testUnit (Docker unreachable, --rerun) │ full suite latest×V2 + SELECT version()
               │ anchors mirror + cumulative gate │ empty src/main diff │ one integrationTest run
```

### Recommended structure (all additive)
```
src/test/java/com/clickhouse/kafka/connect/sink/
├── WrappedDataExceptionClassificationPinUnitTest.java   # PIN-02, package sink (3-arg ProxySinkTask ctor is package-private)
src/test/java/com/clickhouse/kafka/connect/sink/db/helper/
├── ClientEndpointAndOptionsPinUnitTest.java             # + one @Test for PIN-03 (reuses insertOnce), or a new sibling class
src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/
├── ConfluentPlatform.java                               # PIN-04
docs/FORK-DELTA.md                                       # l.72 (gson commit), l.79 (setup-gradle commit), l.228/l.232 (with the commits that fix them)
```

### Pattern 1: PIN-02, a matrix driven through the real `put`
**What:** One parameterized test over (failure shape x tolerance) with an expected `Outcome` of `RETRIABLE`, `FAILS_TASK`, `DLQ`. A small helper builds `ProxySinkTask` with a `DBWriter` that throws, calls `put`, and classifies what happened.
**When:** Docker-free; extend nothing (extending `ClickHouseBase` adds the `clickhouse` tag and drops the class out of `testUnit`).
**Example** (every value below appears in a file read this session: `ClickHouseSinkConfig.java:43,68-69`, `AdditionalRetriableErrorCodesPinUnitTest.java`, `InMemoryDLQ.java`, `ServerExceptions.java`):
```java
// Source: pattern of AdditionalRetriableErrorCodesTest / AdditionalRetriableErrorCodesPinUnitTest (this repo)
private static final class FailingDBWriter extends InMemoryDBWriter {
    private final Supplier<RuntimeException> failure;
    FailingDBWriter(Supplier<RuntimeException> failure) { this.failure = failure; }
    @Override
    public void doInsert(List<Record> records, QueryIdentifier queryId, ErrorReporter errorReporter) {
        throw failure.get();
    }
}

private static Outcome drive(Supplier<RuntimeException> failure, String tolerance, InMemoryDLQ dlq,
                             List<SinkRecord> records) throws Exception {
    Map<String, String> props = new HashMap<>();
    props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
    props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, tolerance);   // ERROR_TOLERANCE_NONE | ERROR_TOLERANCE_ALL
    ProxySinkTask task = new ProxySinkTask(new ClickHouseSinkConfig(props), dlq, new FailingDBWriter(failure));
    try {
        task.put(records);
        return Outcome.DLQ;                                         // returned normally: records went to the reporter
    } catch (RetriableException e) {                                // RetriableException IS a RuntimeException:
        return Outcome.RETRIABLE;                                   // catch it before the generic branch
    } catch (RuntimeException e) {
        return Outcome.FAILS_TASK;
    } finally {
        task.stop();
    }
}
```
Assert beyond the outcome: for `DLQ`, `dlq.size() == records.size()` and each `dlq.exceptions()` entry is a `RuntimeException` whose cause chain contains the injected `DataException` (`assertSame`); for `RETRIABLE`, `dlq.size() == 0` (tolerance and DLQ bypassed) and the chain contains the injected cause; for `FAILS_TASK`, `getMessage().equals("Number of records: " + records.size())` and the chain holds the `DataException`.

### Pattern 2: PIN-03, order-insensitive query map, switched on the client line
**Example** (values from `ClientEndpointAndOptionsPinUnitTest.java`, `ClientErrorShapes.java` and the F7 probe):
```java
// Source: Phase 36 ZzCustomProbeTest asset, made permanent; measured on client 0.11.0 this session
StubClickHouseServer.Request request = insertOnce("?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true");
Map<String, String> query = rawQueryMap(URI.create(request.rawUri()).getRawQuery()); // split on & then =, keep raw values
if (ClientErrorShapes.line() == ClientErrorShapes.Line.V0_11_0) {
    assertEquals("7", query.get("custom_myparam"), TRIPWIRE + request.rawUri());
    assertEquals("max_threads%253D3", query.get("custom_settings"), TRIPWIRE + request.rawUri()); // doubled encoding
} else {
    assertTrue(query.keySet().stream().noneMatch(k -> k.startsWith("custom_")), TRIPWIRE + request.rawUri());
}
```
`TRIPWIRE` is a constant such as `"client-version tripwire: this pins what client X does with custom_* keys in jdbcConnectionProperties; re-read docs/ROLLOUT-v1.7.0_gg6.md row D7 before changing the expectation. URI: "`. The test name and `@DisplayName` must contain "client-version tripwire" (success criterion 4). `ClientErrorShapes.line()` already throws `AssertionError("client-v2 X has no pinned error-shape row: add a Line")` for an unknown client, which is the tripwire for a future bump.

### Pattern 3: PIN-04, scoped count plus a leftover-connector pin
**What:** Keep the per-name REST scoping; add a regression that creates a finished Datagen connector and leaves it, then runs `generateData` for another topic and asserts it completes with the right total. Move the marker into a `private static final Pattern`, use `<`, wrap the `finally` delete in a `try/catch (IOException)` that logs a WARN, name the poll constants. Log one INFO line per run that names the connector and the `finished of total` counts so the recorded run shows what was counted. Replace the two `LOGGER.info(confluentPlatform.getConnectors())` calls with a per-connector status read, or delete `getConnectors()` so no all-connectors accessor remains.
**Constraint:** to leave a connector behind from a test, `createConnector` (private) needs a public or package-visible entry; `createConnectorAndWaitUntilRunning` is unusable for Datagen (the task can be seen FAILED before RUNNING).

### Anti-Patterns to Avoid
- **`assertThrows(RuntimeException.class, ...)` to mean "not retriable".** `RetriableException` is a `RuntimeException`; the assertion passes for both. Use the catch ordering above or `assertFalse(thrown instanceof RetriableException)` as the existing tests do.
- **Raw-URI equality for PIN-03.** Parameter order varies between runs.
- **Editing the synthetic `# v6.4.0` fixtures or pinning the tag object `7296c772…`.** Pin the `^{}` commit.
- **`git merge origin/custom_build` instead of the PR heads while the PRs are unmerged.** It is the fallback only if the user merged a PR on origin.
- **Mockito anywhere in the new tests** (OpenJ9 crash; also the repo rule).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Fake ClickHouse HTTP endpoint | A new HTTP stub or a dependency | `StubClickHouseServer` | Already speaks enough of the HTTP interface for the real V2 client |
| Failure injection into the writer | Mockito mock of `ClickHouseWriter`/`ClickHouseHelperClient` | Subclass of `InMemoryDBWriter` | AGENTS.md: do not mock `ClickHouseWriter` in state machine tests; OpenJ9 crash |
| DLQ capture | A custom reporter | `InMemoryDLQ` | Existing; has `size()` and `exceptions()` |
| Building a `ServerException` | `new ServerException(...)` with a version-specific constructor | `ServerExceptions.create(code, message, transportStatus)` | Client 0.9.6 and 0.11.0 have different constructors; the helper keeps the CLT-09 revert conflict-free |
| Telling the client line | Parsing jar names | `ClientErrorShapes.line()` | Reads the `client-v2` `Implementation-Version` and fails on an unknown line |
| Gate evidence parsing (skips, anchors, junit) | New scripts | `37-evidence-check.py` / `38-evidence-check.py` subcommands (`skips`, `anchors`, `failed`, `junit`) | Already encode the 18-skip rule (7 named + 11 state-mismatch cases); copy to the phase directory, run with `python3 -I` |
| SHA of an annotated tag | Copying the `7296c772…` tag object | `git ls-remote <repo> refs/tags/v6.4.1 refs/tags/v6.4.1^{}` and take the `^{}` line | Tag-object pin is the known trap (gg6 did this for setup-java) |

**Key insight:** every seam this phase needs already exists. The risk is in process (gate shape, commit shape, scope of F-3), not in building anything.

## Common Pitfalls

### Pitfall 1: The first merge drags `72e8ccff` in, and a "clean first-parent diff" claim goes wrong
**What goes wrong:** the merge of a PR head adds the gg6 publication merge to local history. If the plan says "the commit touches only X", the diff to check is the **first-parent** diff (`git diff HEAD^1 HEAD`), not `git show` of a combined diff.
**How to avoid:** assert `git diff --stat HEAD^1 HEAD` lists exactly `gradle/libs.versions.toml` and `docs/FORK-DELTA.md` (gson commit), and exactly four workflow files plus `docs/FORK-DELTA.md` (setup-gradle commit). Record `git rev-list --left-right --count origin/custom_build...HEAD` after each (`0 N` once `72e8ccff` is in).
**Warning signs:** a conflict, or a diff touching `.planning`.

### Pitfall 2: A plain second `./gradlew test` echoes the first run
**How to avoid:** the second run of each pair uses `--rerun` (and `rm -rf build/test-results/test` before the junit helpers read XML). Criterion 1 requires "twice, the second with `--rerun`" for both `testUnit` and the full suite.

### Pitfall 3: OpenJ9 / swap crash on the default JDK 17
**What goes wrong:** exit 255 from "Test Executor finished with non-zero exit value 255", an intermittent GPF in Mockito inline mocking, an ~8 GB core dump. Swap was 17.9 of 19.4 GB used at research time, so expect it.
**How to avoid:** `./gradlew --stop` before each suite; use `-PjavaVersion=21` from the first run and use the **same JVM line for both runs of a pair** (Phase 38 rule G-4). On a crash: delete the dump by name, re-run once, record both. A crash is not a test failure.

### Pitfall 4: `RetriableException` swallowed by a generic assertion (see Anti-Patterns)

### Pitfall 5: Asserting the wrong layer for the socket-timeout shape
A `DataException` with a `SocketTimeoutException` root is retriable through `put` under **both** tolerances (the wrapper hides the `DataException`), but rethrown as `DataException` under `none` when handed directly to `Utils.handleException`. Name the layer in the test and `@DisplayName`; do not copy expectations from `AdditionalRetriableErrorCodesPinUnitTest`.

### Pitfall 6: PIN-03 expected string drift and a missing 0.9.6 branch
Compare parsed values, not the raw URI. Keep both `Line` branches (the CLT-09 one-commit revert to 0.9.6 must still pass, as `operatorIgnoreFlagWins` already does). The 0.9.6 expectation is cited, not re-measured here (Assumption A3).

### Pitfall 7: Bump gate confounded by new tests
Gate each bump commit **before** any new test lands, so a later red is attributable to one change (the phase goal). Order: gson, setup-gradle, then tests, then harness.

### Pitfall 8: Spotless repo-wide
`spotlessCheck` already fails on `ClickHouseCloudTest.java` and `ClickHouseSinkConnectorIntegrationTest.java` (import order, upstream's). `spotlessApply` repo-wide would rewrite them and add unrelated diff. For new files keep imports sorted and unused-free; for edits to `ClickHouseSinkConnectorIntegrationTest.java` keep the diff minimal and do not reorder the pre-existing imports. `ConfluentPlatform.java` is clean, so put new code there where possible.

### Pitfall 9: The anchors "invariant" has two meanings
`verify-merge-invariants.sh` demands a **two-parent upstream merge** as its first argument (invariant 0: "second parent is the upstream release"), so it cannot be pointed at a Dependabot merge. Phase 38 ran two forms: the fast mirror (`37-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172`, seconds) and the cumulative gate `bash .planning/scripts/verify-merge-invariants.sh 54e63648 <explicit head SHA>` with default env (`PRE_TAG=pre-v1.7.0-sync`, `UPSTREAM_REF=refs/tags/v1.7.0`), git-only, 17 to 23 minutes `[CITED: 38-04-PLAN.md]`. Run the mirror per commit and the cumulative gate once at the phase gate, in the background with an explicit SHA (never a moving ref). Both required tags exist locally (`pre-v1.7.0-sync`, `v1.7.0`, `54e63648` resolves to the v1.7.0 sync merge).

### Pitfall 10: Docker "unreachable" must be real
Use `DOCKER_HOST=unix:///nonexistent/docker.sock` on the `testUnit` command (the form every Phase 35-38 evidence used), plus `env -u TESTCONTAINERS_HOST_OVERRIDE`.

## Gate Recipe (established in Phases 37-38, adapted)

Per bump commit:
1. Before the merge: `git fetch origin`; `git ls-remote origin refs/heads/custom_build refs/pull/21/head refs/pull/22/head` (record SHAs and UTC time); `git merge-tree --write-tree HEAD <head>` prints no conflict.
2. `git merge --no-ff --no-commit FETCH_HEAD` (after `git fetch origin refs/pull/N/head`), edit the one FORK-DELTA line, `git add docs/FORK-DELTA.md`, `git commit` with the gg6 message shape ("Merge Dependabot PR #N (<sha>): bump ... ; absorbs refs/pull/N/head fetched at <UTC>...") and the repo's Co-Authored-By trailer.
3. gson: `dependencyInsight` first line reads `com.google.code.gson:gson:2.15.0`; `testUnit` twice (second `--rerun`); full suite twice (second `--rerun`), JVM line recorded, `37-evidence-check.py skips` reads `OK skips: named=7 state_mismatch_cases=11 unexpected=0`; expect 1672 testUnit tests (2 skipped) and 2270 full (18 skipped) on the bump commit.
4. setup-gradle: `grep -c 'gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1'` equals 1 in each of the four files and no `3f5f9ada`/`v6.4.0` remains in `.github`; the `^{}` lookup equals the pinned SHA; `shasum -a 256 .github/workflows/integrationTests.yaml` still starts `07dff3d0973a…5109`; `diff release.yaml pre-release.yaml | shasum -a 256` still starts `ac7dce11e09bbe30`; the six guard classes pass under `testUnit --rerun` with Docker unreachable; anchors mirror `rows=172 bad=0`.

Phase gate (after all five requirements):
```bash
# 1 cache-busted testUnit, Docker unreachable
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21
# 2 full suite, ClickHouse latest x V2, SELECT version() recorded (before and after)
D=$(ls -d /opt/homebrew/Cellar/docker/*/bin/docker | tail -1)
"$D" run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'   # 26.10.3.7 at research time
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun -PjavaVersion=21
# 3 anchors: mirror + cumulative gate (explicit SHA), 4 src/main untouched
git diff --quiet "$PHASE_START_SHA" HEAD -- src/main && echo "src/main empty"
```
`PHASE_START_SHA` must be captured at the start of execution (`git rev-parse HEAD`); `git diff --quiet origin/custom_build HEAD -- src/main` is also empty today.
The recorded `integrationTest` run (PIN-04): `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest -PjavaVersion=21 > log 2>&1` (the nightly's exact environment and task; `prepareConfluentArchive` runs as a dependency). Run it in the background on the final harness tree, alone (do not overlap it with another Docker-heavy Gradle run on this swap-pressured host), and record test totals, the generateData INFO lines and the leftover-connector test result.

## Code Examples

### Reading `custom_*` out of a stub request, order-insensitively
```java
// Source: measured URIs in F7; parameter order is not stable
private static Map<String, String> rawQueryMap(String rawQuery) {
    Map<String, String> map = new TreeMap<>();
    for (String pair : rawQuery.split("&")) {
        String[] kv = pair.split("=", 2);
        map.put(kv[0], kv.length == 2 ? kv[1] : "");
    }
    return map;
}
```

### Verifying the pinned action SHA is a commit, not the tag object
```bash
# Source: git ls-remote https://github.com/gradle/actions.git (this session)
git ls-remote https://github.com/gradle/actions.git refs/tags/v6.4.1 'refs/tags/v6.4.1^{}'
# 7296c772c13d12d9871528814b51fb2143c138ef  refs/tags/v6.4.1        <- tag object, do NOT pin
# 8da46b539cd8fe6ab27ca75128919a18d757604c  refs/tags/v6.4.1^{}     <- commit, pin this
```

## State of the Art

| Old | Current | When | Impact |
|-----|---------|------|--------|
| `setup-gradle` v6.4.0 `3f5f9ada…` | v6.4.1 `8da46b53…` | 2026-11-01 PR #21 | Release notes: security fix for `dependency-submission` (GHSA-chpw-2mqm-95cc) plus dependency updates, no input changes. **Does not affect this fork**: no workflow uses `dependency-graph`, `dependency-submission` or `download-and-submit`. Do not claim a closed vulnerability `[CITED: STACK.md, GitHub release and advisory]` |
| gson 2.14.0 | 2.15.0 (2026-10-24) | PR #22 | Read-side and strict-mode changes; serialize-side items are "Use the runtime type for a wildcard element type", "Use LinkedHashMap as the default Map" (deserialization default), "Skip accessibility check in BoundField" `[CITED: github.com/google/gson/releases/tag/gson-parent-2.15.0, fetched this session]`. The fork only calls `toJson` (`DataJson.java:23-26`, `ConvertToMapOrJson.java:134`), there is no `fromJson` in `src/main`, custom `Date`/`Timestamp` serializers are registered (`ConvertToMapOrJson.java:158,161`); risk LOW, and the full suite passed |

**Deprecated/outdated:** the CONTEXT premise that `setup-gradle` 6.4.1 has never run on GitHub (F2); the research's "rows 72, 76, 79" (F5); the roadmap's `1 13` count (now `1 16`).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | F-3's deliverable at HEAD is: a leftover-connector pin, removal or scoping of the all-connectors `getConnectors()` accessor, and the IN-06 hygiene in `generateData`; the code already scopes counting by connector name | F8, Pattern 3 | The user may mean a different residual (for example re-working `createConnectorAndWaitUntilRunning`). A wrong scope costs one `integrationTest` run. Confirm before the plan locks |
| A2 | A whole-task `integrationTest` run (about 33 min) satisfies "one recorded `integrationTest` run"; a `--tests` filtered run (about 10 min for `ClickHouseSinkConnectorIntegrationTest`) might too | F8, Gate Recipe | If the user wants only the filtered run, the whole task wastes about 20 minutes; if they want the whole task, a filtered run is short of the criterion |
| A3 | On client 0.9.6 a `custom_*` key forwards nothing (taken from Phase 36 E-11, not re-measured here; 0.9.6 is not on this classpath) | F7, Pattern 2 | Only the `V0_9_6` branch of the pin, which runs only after a CLT-09 revert |
| A4 | A scratch push is unnecessary in this phase because both PR heads' `tests.yaml` runs are green and the `uses:` line is byte-identical | F2 | The dispatch-only workflows first run at the Phase 44 rehearsal; if the user wants earlier proof they must approve a push |
| A5 | Update FORK-DELTA Known cost 15 (F-3) and Known cost 17 (wrapped `DataException`) in the commits that fix them, rather than leaving both to Phases 43/44 | F6 note 5, F8 | If deferred, the doc is wrong between commits (no test reads it), and Known cost 15's own re-open trigger fires in this phase |
| A6 | "The four pin-guard classes" are `ReleaseWorkflowGuardUnitTest`, `TestsWorkflowPinGuardUnitTest`, `WorkflowActionPinGuardUnitTest`, `DependabotProtobufIgnoreGuardUnitTest` (the set Phase 37 ran together) | F4 | Low: running six covers any other reading |
| A7 | The cumulative `verify-merge-invariants.sh` gate still passes with `72e8ccff` and the two Dependabot head commits inside `pre-v1.7.0-sync..HEAD`; its 17 to 23 minute runtime is from Phase 38's record. Not run here | Pitfall 9 | If it fails on the merge commit's shape, the phase gate stops late. Recommended mitigation: start it in the background right after the first merge |

## Open Questions

1. **What does PIN-04 / F-3 mean when the counter is already scoped?**
   - What we know: F8. `generateData` counts by connector name; `getConnectors()` is only logged.
   - What's unclear: whether the user expects more than the pin, accessor removal and hygiene (A1).
   - Recommendation: plan the Pattern 3 scope; surface A1 and A2 at the plan checkpoint so the user can redirect before the 33-minute run.
2. **Should the PIN-02 table include the conversion-time `DataException` row (F6 note 3)?**
   - What we know: it escapes `put` raw under both tolerances with no DLQ record.
   - What's unclear: whether the user regards that as intended behaviour or a defect to record.
   - Recommendation: pin it as a labelled control row (no code change), and let Phase 43/44 decide how the rollout note words it.
3. **Does the user merge #21 or #22 on origin before execution?**
   - Recommendation: re-read `refs/pull/*/head` and `origin/custom_build` at the start of plan execution; STATE already asks the user not to merge them.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 toolchain | `compileJava`, default test JVM | yes (Semeru OpenJ9 17.0.9 x3, Adoptium 17 in `~/.gradle/jdks`) | 17.0.9 | `-PjavaVersion=21` |
| JDK 21 (HotSpot) | `-PjavaVersion=21` escape | yes | 21.0.12.1 (Homebrew) | none needed |
| Gradle wrapper | all builds | yes | 9.8.2 (`gradle-wrapper.properties`) | none |
| Docker (colima profile `vz`) | full suite, `integrationTest` | yes, `DOCKER_HOST` set; profile `vz` Running (the `default` profile is Stopped, so bare `colima status` is misleading) | Docker 29.8.2 | none; Docker-free parts need none |
| ClickHouse image | full suite | yes | `latest` = 26.10.3.7, `sha256:739e099a…30b1ce` | none |
| Connect-stack images | `integrationTest` | yes, cached (see F8) | cp 7.9.x, datagen 0.6.4-7.6.0 | pull on demand |
| `gh` CLI | not needed | present at `/opt/homebrew/bin/gh` (STATE says deliberately absent; not used here) | n/a | unauthenticated REST via `curl` (used this session) |
| `yq`, `timeout` | not needed | `yq` absent; `timeout` absent in this shell | n/a | poll with a loop; never rely on `timeout` |
| Host memory | JVM stability | **tight**: swap 17.9 of 19.4 GB used | n/a | `-PjavaVersion=21`, `./gradlew --stop` first |

**Missing dependencies with no fallback:** none.

## Security Domain

`security_enforcement` is enabled (ASVS level 1, block on high).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / V14 Config (supply chain) | yes | Every action pinned to a 40-hex commit with a `# vX.Y.Z` comment; guarded by `WorkflowActionPinGuardUnitTest` (fails on anything else). Pin the `^{}` commit, never a tag or tag object |
| V2 Authentication / V3 Session / V4 Access Control | no | No code path change; workflow `permissions: contents: read` blocks are pinned by `TestsWorkflowPinGuardUnitTest` and unchanged |
| V5 Input Validation | no | Tests only; no new input parsing in `src/main` |
| V6 Cryptography | no | none |
| V8 Data Protection (logging) | yes | New tests/harness lines must not log passwords, API keys or record values (AGENTS.md); the F-3 INFO line logs a connector name and counts only |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mutable action tag swapped upstream (tag-pointer attack) | Tampering | Full commit SHA pin; verify with `git ls-remote ... ^{}`; do not trust a tag object |
| Dependabot or look-alike PR bumping `integrationTests.yaml` or protobuf | Tampering | `exclude-paths` and exact-name ignores in `dependabot.yml`; `UpstreamIntegrationWorkflowBytesGuardUnitTest` pins the file's SHA-256 |
| Dependency bump pulling a hostile artifact | Tampering | Existing coordinates only; `maven-metadata.xml` and the jar checked on Maven Central. The repo has no `gradle/verification-metadata.xml` (FORK-DELTA Known cost 18, an accepted standing item) |
| Sensitive data in test logs | Information disclosure | Metadata-only messages; the wrapped-`DataException` text carries a queryId, not record values (`ValueFreeDlqHeadersUnitTest` pins this) |

## Sources

### Primary (HIGH confidence)
- Working tree, read this session: `ClickHouseSinkTask.java`, `ProxySinkTask.java`, `Processing.java` (lines 50-100, `doInsert`), `Utils.java` (`handleException`), `ClickHouseHelperClient.java` (`createClientV2`, 160-225), `ClickHouseSinkConfig.java` (43-44, 68-69, 282-284), `DataJson.java`, `ConvertToMapOrJson.java`, `StubClickHouseServer.java`, `ClientEndpointAndOptionsPinUnitTest.java`, `ClientErrorShapes.java`, `AdditionalRetriableErrorCodesTest.java`, `AdditionalRetriableErrorCodesPinUnitTest.java`, `ValueFreeDlqHeadersUnitTest.java`, `InMemoryDLQ.java`, `ServerExceptions.java`, `ConfluentPlatform.java` (360-560, 596-647), `ClickHouseSinkConnectorIntegrationTest.java`, `build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/*`, `.github/dependabot.yml`, `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `verify-merge-invariants.sh`
- `git ls-remote https://github.com/gradle/actions.git` (v6.4.1 tag object and `^{}` commit)
- Maven Central `maven-metadata.xml` and `gson-2.15.0.jar` (HTTP 200) for `com.google.code.gson:gson`
- GitHub REST API (unauthenticated): PRs #21/#22 state, open-PR list, `custom_build` tip, Actions runs 41297596686 and 41297643137 with their job steps
- Measured on `/tmp/gsd39` (outside the repo): dependencyInsight, guard-class run, `testUnit`, full suite on ClickHouse 26.10.3.7, Spotless baseline, three standalone probes (custom_ URIs, outcome matrix, conversion `DataException`)
- `.planning/milestones/v1.7.0_gg6-maintenance-phases/` (36-FOLLOWUPS, 36-REVIEW IN-06, 36-04-SUMMARY E-11, 37/38 evidence scripts and plans) and `.planning/research/` (STACK, FEATURES, PITFALLS, ARCHITECTURE, SUMMARY)

### Secondary (MEDIUM confidence)
- `github.com/google/gson/releases/tag/gson-parent-2.15.0` (WebFetch summary; release year inferred from Maven `lastUpdated`)

### Tertiary (LOW confidence)
- None. No Context7 and no research-seam cache entries were used or written (all sources were primary: git, registries, APIs, the tree).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, versions and SHAs checked against Maven Central and the git remote.
- Architecture (absorption shape, gate recipe): HIGH, dry-run merges and a full measured suite on the bumped tree.
- Pitfalls: HIGH for items measured or read; MEDIUM for A7 (cumulative gate not run here).
- PIN-02/PIN-03 expectations: HIGH, measured through the real classes.
- PIN-04 scope: MEDIUM, the code facts are read, the intended scope is A1.

**Research date:** 2026-11-01
**Valid until:** until the user merges #21/#22 on origin, Dependabot rebases a PR head, or `origin/custom_build` moves (re-read the three `ls-remote` lines at execution start); otherwise 30 days.
