# Phase 12: Upstream v1.7.0 Merge & Reconcile - Research

**Researched:** 2026-10-07
**Domain:** Brownfield fork sync (Java 17 / Gradle Kotlin DSL / Kafka Connect sink to ClickHouse): one mechanical `git merge`, then by-intent reconciliation commits proven by Docker-free and live tests
**Confidence:** HIGH for conflict set, code sites and script design (read or measured this session); MEDIUM for the `KeeperStateProvider` mutation outcome and the live compression experiment (cannot be known before running them)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Branch & commit shape**

- **D-01:** The scratch branch `sync/v1.7.0` is cut from **`sync/v1.6.0` HEAD** (`3c69dd0e` at discussion time). That HEAD is `c61d90ce` plus 18 unpushed `.planning`-only commits; its code is identical to `c61d90ce`.
  - Phases 12–15 commit their planning and code together on `sync/v1.7.0`. Phase 15's non-force `--no-ff` merge into `custom_build` then publishes the post-`c61d90ce` planning history in the same move (PUB-04).
  - `pre-v1.7.0-sync` still tags **`c61d90ce`**, the pre-sync `custom_build` SHA, as SYNC-07 requires. That makes the invariant script's range `pre-v1.7.0-sync..HEAD` include the 18 planning commits. That is harmless: they touch no tracked source in the script's pathspec.
  - The branch stays local through Phases 12–14, as D-05 required in v1.6.0. Pushing any branch fires `tests.yaml`.
  - Before publishing, confirm that `.planning/.gsd-ws-arg` is not tracked. It is not tracked today.
- **D-02:** The merge commit is **mechanical with line provenance**. Every line in a conflicted path comes from one side: `pre-v1.7.0-sync` or `v1.7.0`. Hunks are resolved by choosing a side or taking the union of both; no new lines are authored.
  - The **D-17 hunks resolve to upstream's side inside the merge commit**: the V1 `getDefaultClientOptions`, the V2 builder chain and the config-key block. Choosing upstream's lines for these is still mechanical.
  - Union hunks are written out in full on both sides:
    - `ClickHouseWriter.start()` carries both `.setTargetTableFilter(...)` and `.setCompressionMethod(...)`.
    - `ClickHouseClientBuilder` carries both setter methods, each with its own `return this; }`. This is the shared-trailer trap in ARCHITECTURE.md.
    - `ClickHouseSinkConfig` carries both sides' fields, constructor assignments and `define` calls.
  - The catalog keeps the fork's `kafka = "4.1.1"` line and takes upstream's `clickHouseDriver` line. Upstream's value is now `0.9.6` too, so the `# FORK OVERRIDE (upstream 0.9.5)` comment drops as part of the mechanical choice. The rest of BLD-13 lands in the build/catalog reconcile commit.
  - New code (the scoped `&& col.isNullable()` guard, deleting now-dead D-17 constants, deleting the fork's LZ4 rule, new tests) never goes into the merge commit.
  - **Reversibility:** costly. Phases 13–15 build on this commit, so redoing it means redoing everything above it. `pre-v1.7.0-sync` plus the scratch branch keep that cheap until Phase 15 publishes.
- **D-03:** **The merge commit must pass a gate on its own SHA:** `compileJava`, `compileTestJava`, `compileTestFixturesJava` and `compileIntegrationTestJava` succeed, and Docker-free `testUnit` is green.
  - This keeps bisect usable across the sync. It is achievable because only five files conflict and D-02 resolves the D-17 hunks to upstream.
  - The known MAP-guard shadow survives into the merge commit, because the fix is new code. The Docker-free byte pins at the merge SHA still assert the pre-#820 bytes and pass. The shadow is fixed and re-pinned in the WRT reconcile commit.
  - Always invoke Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`.
- **D-04:** The by-intent reconcile work lands as **one commit per concern**, never mixed:
  1. **WRT:** scoped MAP guard (WRT-06), CR-02 re-pins with rationale (WRT-09) and the Docker-free #820 byte test.
  2. **CMP:** dead D-17 constants and comments removed if the merge left any, the CMP-02 unit pins, and the CMP-03 live test.
  3. **BLD:** delete the fork's `// Temporary fix` LZ4 capability block (`build.gradle.kts:134-138`), drop `clickhouse-jdbc` from the catalog and integrationTest (BLD-09, BLD-13), and fix the "1.10.2" wording.
  4. **EOS:** the `KeeperStateProvider` restart-read live test (EOS-01).
  5. **Docs:** the `FORK-DELTA.md` rows and the checklist closure.

  Order is the planner's call, except that the anchors file must change in the same commit that changes an anchor (D-08).
  - No `.planning/` commit is interleaved with in-progress conflict resolution (Sync Mechanics).
- **D-05:** **`VERSION` takes the fork's side (`v1.6.0_gg1`) in the merge and stays there through Phases 12–14.** Phase 15 (PUB-01) sets `v1.7.0_gg1` after a `clean`. That way the JMX `version=` segment and the client name never claim an unpublished release, and the tree never carries upstream's bare `v1.7.0`.

**Merge checklist & invariant script**

- **D-06:** SYNC-08's checklist is a **new sibling file, `.planning/MERGE-CHECKLIST-v1.7.0.md`**, laid out like the v1.6.0 one:
  - Section 1, conflicted paths: path, predicted vs actual conflict class, resolution shape, owning phase, fork behaviour at risk, adjudicating test, status.
  - Section 2: every path upstream changed between `v1.6.0` and `v1.7.0` (22 files), with a **reviewed verdict for each auto-merged hunk inside a fork-touched file**. Named in the roadmap: the `ClickHouseWriter` MAP branch, the `build.gradle.kts` LZ4 rule, the `ClickHouseClientBuilder` setter sites and the test helpers. Also `StructToJsonMap` and `ClickHouseTestHelpers`, which merge clean.
  - `## Recorded Facts`.

  The v1.6.0 `.planning/MERGE-CHECKLIST.md` stays where it is and is not edited, except for BLD-09's "1.10.2" wording fix. `docs/FORK-DELTA.md` cites its Recorded Facts and Section 1 throughout.
  - **New Recorded Facts continue the global numbering at 17**, so a "Recorded Fact N" citation in FORK-DELTA is never ambiguous. Number 17 is the first fact the v1.7.0 checklist records.
- **D-07:** The v1.7.0 checklist is a **living gate with a status column**. Phase 12 writes every row. Rows it cannot close, such as writer hunks that Phase 13 edits again, stay `pending` with their owning phase. **Phase 15 asserts zero `pending` rows before publication**, the same contract as v1.6.0's D-08.
- **D-08:** The anchor survival check reads a **committed `.planning/scripts/fork-anchors.txt`**. Each line holds a path, a fixed-string pattern, an expected count and the FORK-DELTA row it backs. Examples: `shouldSkipTableMetadata()` in `ClickHouseWriter`, `Type.DYNAMIC`, the `connector=` MBean segment, `buildTableFilterClause`, `shouldSkipDescribeRow`, the commented `async_insert`, `CLICKHOUSE_VERSION_LATEST`, and `StandardCharsets.UTF_8` at its counted sites.
  - Markdown is never parsed. It mirrors `conflicted-paths.txt` and is reviewable and diffable.
  - **Any commit that intentionally changes an anchor updates `fork-anchors.txt` in the same commit.** This covers D-17's anchors retiring here and R-01 routing in Phase 13. Anchor drift is therefore always deliberate.
  - Fits naturally here: research Pitfall 4's builder-site invariant. Count `new ClickHouseHelperClient.ClickHouseClientBuilder(` against `.setCompressionMethod(csc.isSetCompressionMethod())` in `src/main`: 3 builder sites today, at `ClickHouseWriter.java:84`, `:1575` and `KeeperStateProvider.java:48`.
- **D-09:** `verify-merge-invariants.sh` is parameterised (`PRE_TAG`, `UPSTREAM_REF`) and **runs at the end of every phase from 12 to 15** over `pre-v1.7.0-sync..HEAD`. It is part of each phase's verification, not only Phases 12 and 15.
  - Its old invariant 1 ("conflicted paths are byte-identical to upstream") is replaced by line provenance plus the anchor grep (SYNC-07).
  - The conflict-marker scan (invariant 2) is kept.

**Compression evidence (CMP-01..03)**

- **D-10:** **`NetworkCompressionPinUnitTest` and `NetworkCompressionPinLiveTest` keep their names and all 9 existing cases, and are extended in place.**
  - The existing cases still hold: the flag defaults to `true`, and upstream's `if (setCompressionMethod) serverSetting(...)` sits before the `jdbcConnectionProperties` loop, so operator-wins ordering is preserved.
  - New unit cases:
    - `compress=false` on the V1 options, the V1 node and the V2 config (`compressServerResponse(false)`).
    - With `setCompressionMethod=false`, the lz4 setting is absent and response compression is still off.
  - Existing tests are never deleted or weakened (AGENTS.md). FORK-DELTA and anchor references keep resolving.
  - The class Javadoc rewrite stays Phase 13's DEBT-05.
- **D-11:** CMP-03's live insert-and-read-back runs through a **full `ClickHouseSinkTask` on client V2** (`start`/`put`, then `assertEquals` against the original records), once with `setCompressionMethod=true` and once with `false`.
  - This exercises the writer's main client and its temporary client, which are the builder sites CMP-01 must thread.
  - The existing helper-level V1 ping and query cases keep covering V1.
- **D-12:** The research experiment, which strips both protections (`compress=true` through `jdbcConnectionProperties` plus `setCompressionMethod=false` against `latest`), is a **one-off probe during the phase, not a committed test**.
  - Record the result as a Recorded Fact in the v1.7.0 checklist: server version, client `0.9.6`, the outcome of each configuration and the exact error text.
  - Also record one `FORK-DELTA.md` sentence naming which control is load-bearing against the ZSTD default (CMP-03).
  - No committed test can go red because `latest` drifts or the client changes.
- **D-13:** **If the `setCompressionMethod=false` live insert-and-read-back fails on `latest`, the executor stops at a blocking checkpoint.** It must not record the failure as a rollout fact and continue. A failure means upstream's `compress=false` does not cover what the locked D-17 decision assumed, and any fork patch to compensate is the user's call.
  - **Reversibility:** one-way — continuing past it would change what the published connector promises operators about `setCompressionMethod=false`.

**EOS-01 restart test**

- **D-14:** EOS-01 is a **live test in its own class, `KeeperStateProvider*LiveTest`, on the shared container**, which already loads `src/test/resources/clickhouse_keeper_map.xml` with an embedded keeper. Cases:
  - Write `BEFORE`, then `AFTER`, with distinct min/max offsets. Build a **new** `KeeperStateProvider` from the same config and assert the exact `(minOffset, maxOffset, state)`.
  - A fresh key reads back `NONE`.
  - Run everything with `setCompressionMethod=true` and with `false`, on a client-V2 config, which is what production uses. The read path is V1 internally either way.
  - Watch `StateRecord`'s constructor argument order. Upstream's `getStateRecord` passes `(topic, partition, maxOffset, minOffset, …)`.
- **D-15:** "Shown to fail against the old summary-count code" is proven by a **temporary working-tree revert, never committed**. Steps:
  1. Restore `response.getSummary().getResultRows()` in `getStateRecord`.
  2. Run the new test and capture the red output: which assertion failed and with which values.
  3. Restore the file, confirm green, and confirm `KeeperStateProvider.java` is byte-identical to `v1.7.0`.

  The evidence goes into the plan SUMMARY and a Recorded Fact. The file keeps zero fork edits; AGENTS.md lists it as high-risk.
- **D-16:** **If the temporary revert does not turn the test red**, the executor does a **time-boxed hunt** for a configuration where the summary count and the streamed count disagree. Candidates:
  - flag `false` vs `true`
  - empty vs one-row result
  - the `send_progress_in_http_headers` and `wait_end_of_query` defaults

  A discriminating condition that turns up is added as a committed test case. If none does, **stop at a blocking checkpoint** with the evidence. EOS-01's "shown to fail" clause would then be unmeetable as worded, and rewording a requirement is the user's decision.

### Claude's Discretion

- How the script implements line provenance: for example, derive the conflicted-path list from `git merge-tree --write-tree --name-only` on the merge's two parents, versus committing a `conflicted-paths-v1.7.0.txt`. Also the exact `fork-anchors.txt` format and the initial anchor set.
- Whether `PRE_TAG`/`UPSTREAM_REF` default to the v1.7.0 values or are required arguments. The script must still be able to re-check the v1.6.0 merge when given the v1.6.0 values.
- Extend `NewUpstreamConfigDefaultsUnitTest` with the 14th upstream option, `setCompressionMethod`: default `true`, canonical key, and the `set_compression_method` alias (research Pitfall 4).
- Wording of each WRT-09 rationale comment. It must sit next to the changed assertion and cite the user-approved tightening of 2026-10-07 (REQUIREMENTS locked decisions table).
- Plan and wave breakdown, and the exact names of new test classes.

### Deferred Ideas (OUT OF SCOPE)

None. The discussion stayed within the phase's scope. The WRT-06/09 re-pin scoping and the line between Phase 12's MAP guard and Phase 13's WR-01/WR-02 were offered as extra gray areas and not taken up. The planner follows the roadmap split: Phase 12 scopes the top-level MAP guard and re-pins CR-02. Nested-slot nulls belong to Phase 13.

**Not this phase** (CONTEXT `<domain>`): R-01 routing, WR-01/WR-02 nested-null handling, P9-IN-01, the metadata/config fixes and the test debt (including DEBT-05's Javadoc rewrite of `NetworkCompressionPin*`) are Phase 13. Dependency bumps beyond upstream's own `lz4-java 1.10.4`, and all CI workflow changes, are Phase 14. The `VERSION` bump, CHANGELOG, publication and rollout checklist are Phase 15.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SYNC-07 | Scratch-branch merge, `pre-v1.7.0-sync` tag, parameterised script with line provenance + anchor grep | "Merge mechanics", "Invariant script design" (prototype verified against the v1.6.0 merge: `paths=40 viol=0`), initial anchor table |
| SYNC-08 | v1.7.0 checklist with predicted/actual class and reviewed verdicts for auto-merged hunks | "Merge prediction" (13 hunks in 5 files, all 22 upstream paths classified) and the auto-merged-hunk verdict table |
| SYNC-09 | Every FORK-DELTA feature survives, each proven by its existing pin test | "SYNC-09 pin-test matrix": which classes are Docker-free (`testUnit`) vs live, derived from the class declarations |
| SYNC-10 | Upstream's new/changed tests accepted unmodified and pass, or skip for a named reason | "SYNC-10 inventory": 9 test-tree paths to byte-compare against `v1.7.0`; #800 and cluster `buildFromClause` live only in the `integrationTest` source set (compile-only) |
| CMP-01 | D-17 retired; every client built receives the flag | Exact D-17 sites, 3 `src/main` builder sites + 1 `testFixtures` site, and the finding that the writer's temporary client is only built on V1 insert paths |
| CMP-02 | Docker-free pins for `compress=false` / `compressServerResponse(false)` / lz4 absent when flag is off | Verified option keys and defaults; exact assertion targets for V1 options, V1 node and V2 config |
| CMP-03 | Live insert+read-back with flag true/false; record the load-bearing control | Live experiment protocol (4 configurations, assert the control took effect), harness template (`AsyncInsertLiveTest`), local `latest` is 26.9.12.8 |
| WRT-06 | Null optional Map into non-nullable Map gives #820 bytes; guard scoped to nullable | Merged `case MAP:` text, scoped-guard recipe, expected bytes, RED-first protocol |
| WRT-09 | CR-02 pins re-pinned with rationale | Finding: the only MAP CR-02 pin already uses `isNullable(true)`, so no existing assertion changes; the work is new non-nullable pins plus a recorded "zero assertions changed" fact |
| BLD-09 | One LZ4 rule; one `at.yawk.lz4:lz4-java` on `runtimeClasspath`; stale "1.10.2" wording gone | Baseline `dependencyInsight` output captured (1.10.2 today), how to count selected components, full list of files carrying the "1.10.2" wording |
| BLD-13 | Catalog drops `clickhouse-jdbc` and the FORK OVERRIDE comment, keeps `kafka = "4.1.1"` | Finding: the merge itself already removes `clickhouse-jdbc` from both the catalog and `integrationTest`, so BLD-13 is satisfied mechanically and only needs proof |
| EOS-01 | `KeeperStateProvider` restart-read test seen to fail against old summary-count code | Provider constructors, KeeperMap config, unique zkPath/zkDatabase per test, mutation-check protocol, hypothesis for why it goes red |
</phase_requirements>

## Project Constraints (from CLAUDE.md / AGENTS.md)

- **Never delete or weaken existing tests; only add.** Re-pins of CR-02 are permitted only as the user-approved tightening of 2026-10-07 and only with rationale next to the assertion (CONTEXT Discretion, REQUIREMENTS locked decisions).
- **Read back and `assertEquals` against the original records** for every live write test (AGENTS.md Writing Tests).
- **Key Files are high-risk:** `ClickHouseWriter.java`, `KeeperStateProvider.java`, `ClickHouseSinkConfig.java`. `KeeperStateProvider.java` must stay byte-identical to `v1.7.0` (D-15).
- **Feature tests for new functionality; Docker-free tests must not extend `ClickHouseBase`** (it carries `@Tag("clickhouse")` and would drop the class out of `testUnit`).
- **Test target is ClickHouse `latest` + client V2 only** (`.claude/CLAUDE.md` Constraints); additive changes only for rebasability.
- **Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`** (this shell sets the variable to the literal string `null`).
- **Do not log secrets or record data.** Probe output recorded in Recorded Facts must be scrubbed of passwords.
- **AGENTS.md says "run `spotlessApply`" and "do NOT commit without asking".** Both are overridden in practice here: `spotlessCheck` is red at baseline on 44 files (measured this session), so a repo-wide `spotlessApply` would rewrite unrelated fork files and break rebasability; and GSD plan execution commits per task by established workflow (Phase 6-11). Treat commits as authorised by the GSD workflow, never push (CONTEXT D-01, Sync Mechanics).
- **GSD Workflow Enforcement:** repo edits go through a GSD command (`/gsd-execute-phase`).
- **Dependency upgrades need user approval** (AGENTS.md Security). Phase 12 takes only upstream's own `lz4-java 1.10.4`; further bumps are Phase 14.

## Summary

The merge is small and fully predictable: a dry run of `git merge-tree --write-tree --name-only HEAD v1.7.0` reports exactly **5 conflicted files and 13 conflict hunks** (`VERSION` 1, `gradle/libs.versions.toml` 1, `ClickHouseSinkConfig` 4, `ClickHouseWriter` 1, `ClickHouseHelperClient` 6), matching the milestone research except that the helper has 6 hunks, not 7. Three more fork-touched files auto-merge (`build.gradle.kts`, `StructToJsonMap.java`, `ClickHouseTestHelpers.java`), and the remaining 14 of upstream's 22 changed paths are upstream-only. The D-02 resolution recipe is fully determined by the hunks, written out below with the shared-trailer trap handled.

Four things in CONTEXT.md or the milestone research do not survive contact with the code and the planner should plan around them: (1) the writer's "temporary client" (`ClickHouseWriter.java:1575`) is only built inside `getMutationRequest`, which is reached only from the **V1** insert paths, so D-11's V2 sink-task test does **not** exercise it; (2) the only MAP CR-02 pin already builds its column with `isNullable(true)`, so the scoped guard leaves all three CR-02 pins green and **no existing assertion needs to change** (WRT-09's real work is new non-nullable byte pins plus a recorded "zero assertions changed" fact); (3) BLD-13 is satisfied by the mechanical merge itself (the catalog entry, the `integrationTest` dependency and the FORK OVERRIDE comment all disappear), so the BLD commit shrinks to deleting the fork's LZ4 block and fixing wording; (4) `spotlessCheck` is red at baseline, so no gate may rely on it.

Environment is ready: Docker 27.4.0 answers on the colima socket, the local `clickhouse/clickhouse-server:latest` image is ClickHouse **26.9.12.8** (ZSTD default, so the compression experiment is meaningful), Gradle works offline, and both `lz4-java 1.10.2` and `1.10.4` jars are in the cache. `colima status` reports "not running" even though the socket pings `OK`; trust the socket.

**Primary recommendation:** Execute as: pre-merge script parameterisation, then one pure merge commit gated by D-03, then four file-disjoint-in-code reconcile commits (WRT, CMP, BLD, EOS) run sequentially because each edits `fork-anchors.txt`, then one docs/checklist commit; make the D-13 and D-16 stops explicit `checkpoint` tasks, and make every experiment assert that the control it varies actually took effect before reading its outcome.

## Architectural Responsibility Map

This is a build/merge phase for a connector with no browser or web tier; the relevant tiers are the connector's own layers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Compression flag threading (`setCompressionMethod`) | Config (`ClickHouseSinkConfig`) | Client factory (`ClickHouseHelperClient` builder), 3 `src/main` call sites | Config owns the value; every builder site must pass it, otherwise the default `true` silently overrides an operator's `false` |
| Response-compression off (`compress=false`) | Client factory (`getDefaultClientOptions`, V1 node options, V2 `Client.Builder`) | — | Applied unconditionally in `ClickHouseHelperClient`; independent of the flag |
| Null optional Map bytes (#820) | Writer (`ClickHouseWriter.doWriteCol` / `doWriteColValue`) | `StructToJsonMap` (JSON route), `cleanupExtraFields` | The byte stream is produced only in the writer; the preamble in `doWriteCol` and the `case MAP:` branch together produce `00` / `00 00` |
| Exactly-once state read | State store (`KeeperStateProvider`) over KeeperMap | `ClickHouseHelperClient` (V1 options/node) | The row count that decides `NONE` vs real state is read in the provider; nothing above it re-checks |
| LZ4 capability resolution | Build (`build.gradle.kts`, version catalog) | Confluent archive (`clickhouseDependencies`) | Gradle resolution decides which jar ships |
| Merge provenance and anchors | Planning scripts (`.planning/scripts/`) | Checklist + `docs/FORK-DELTA.md` | Script is the machine check; markdown is never parsed (D-08) |

## Standard Stack

No new libraries are installed in this phase. The relevant pinned artifacts, all already resolvable offline:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.clickhouse:client-v2` / `clickhouse-client` / `clickhouse-data` | `0.9.6` | Client under test; its `compress` default is `true` | Upstream's own value at v1.7.0; fork override comment drops |
| `at.yawk.lz4:lz4-java` | `1.10.4` (upstream catalog `lz4Java`); `1.10.2` today | LZ4 for native compression | Arrives with the merge; both jars present in `~/.gradle/caches` |
| JUnit Jupiter | `5.14.4` (catalog BOM) | Test framework | Existing |
| Testcontainers + `clickhouse/clickhouse-server:latest` | image `26.9.12.8` locally (label `com.clickhouse.build.version`) | Live gate | `CLICKHOUSE_VERSION_DEFAULT = latest`; pull policy is always-pull for floating tags |
| Gradle wrapper | `9.6.1`, JDK 17 toolchain cached | Build | Existing |

[VERIFIED: ~/.gradle/jdks listing `adoptium-17-aarch64-hotspot-mac`; `at.yawk.lz4/lz4-java/1.10.2` and `/1.10.4` jar paths found by `find`; Docker image label read via the Docker socket API: `com.clickhouse.build.version=26.9.12.8`, `clickhouse/clickhouse-server:latest created=2026-10-06T18:12:57Z`]

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Set-membership line provenance (`grep -vxFf`) | Hunk-by-hunk diff allow-list per path (ARCHITECTURE.md Invariant 1b) | The allow-list is brittle and needs per-path edits; set membership proves "no authored lines" in ~10 lines of bash and already passes the v1.6.0 merge |
| Derive conflicted paths via `git merge-tree` at check time | Hand-transcribed list | Hand transcription is exactly what v1.6.0's Recorded Fact 5 had to correct (39 vs 40) |

**Installation:** none. **Package Legitimacy Audit:** not applicable. No package is added to any manifest; `lz4-java 1.10.4` is upstream's own catalog entry at the pinned `v1.7.0` tag, and the Package Legitimacy seam covers npm/PyPI/crates, not Maven. [ASSUMED: seam ecosystems per the agent's own list]

**Version verification:** `dependencyInsight` baseline run this session on the unmerged tree returned `at.yawk.lz4:lz4-java:1.10.2` (client-v2 edge) and `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.10.2` (kafka-clients edge). Re-run after the merge and after the BLD commit; expect `1.10.4`.

## Architecture Patterns

### Merge flow and commit sequence

```
c61d90ce (custom_build == origin/custom_build)        v1.7.0 (379b8d01, lightweight tag)
   |                                                      |
   +-- 20 planning-only commits --> sync/v1.6.0 HEAD      |
   |   (f4fd6fcb today; code == c61d90ce)                 |
   |                                                      |
 tag pre-v1.7.0-sync (-> c61d90ce)                        |
                       |                                  |
   git switch -c sync/v1.7.0   (from sync/v1.6.0 HEAD)    |
          |                                               |
   [P0] script parameterisation commit (planning-only) <--+-- before the merge, never during it
          |
   git merge --no-ff --no-commit refs/tags/v1.7.0   -> 5 files conflict, resolve per recipe below
          |
   [M]  one merge commit  ---- gate D-03: 4 compile tasks + testUnit (Docker-free)
          |
   [A]  fork-anchors.txt + conflicted-paths-v1.7.0.txt + checklist Section 1/2 stub (planning-only)
          |
   [WRT] scoped MAP guard + #820 byte pins     (ClickHouseWriter.java, NestedNullable..., anchors)
   [CMP] D-17 leftovers + CMP-02 pins + CMP-03 live + D-12 probe (HelperClient, pin tests, anchors)
   [BLD] delete fork LZ4 block + wording       (build.gradle.kts, 4 doc files, anchors)
   [EOS] KeeperStateProvider restart test + mutation evidence   (new test file only)
          |
   [DOC] FORK-DELTA rows + checklist closure + Recorded Facts 17+
          |
   verify-merge-invariants.sh  (end-of-phase gate, D-09)
```

WRT, CMP and BLD all edit `.planning/scripts/fork-anchors.txt` (D-08), so run them sequentially even though their source files are disjoint; parallel worktrees would conflict on that one file. EOS touches only a new test file and can run in parallel with any of them. Use the full ref `refs/tags/v1.7.0` rather than `v1.7.0` in the merge command: `refs/upstream/main` and `refs/upstream/v1.6.0` exist and are stale (`789a57c2`), while `refs/remotes/upstream/main` is `379b8d01`. [VERIFIED: `git for-each-ref refs/upstream refs/remotes/upstream/main` output this session; `git cat-file -t v1.7.0` = `commit`]

### Merge prediction (dry run, read-only)

Command run: `git merge-tree --write-tree --name-only HEAD v1.7.0` (no refs, index or working tree touched; it did write dangling blob/tree objects, which `git gc` prunes). `git merge-tree c61d90ce v1.7.0` and `HEAD v1.7.0` give the same 5 paths. Merge base is `789a57c2` = `v1.6.0`.

| Path | Predicted class | Hunks | Resolution (D-02) |
|------|-----------------|-------|-------------------|
| `VERSION` | CONFLICT content | 1 | Fork side: `v1.6.0_gg1` (D-05) |
| `gradle/libs.versions.toml` | CONFLICT content | 1 | Upstream's `clickHouseDriver = "0.9.6"` + fork's `kafka = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)`; drop fork's `clickHouseDriver ... # FORK OVERRIDE (upstream 0.9.5)` line and upstream's `kafka = "2.7.0"` |
| `ClickHouseSinkConfig.java` | CONFLICT content | 4 (marker-inclusive lines 72-78, 144-150, 350-354, 835-856 of the merge-tree output) | Pure union of both sides, in all four hunks |
| `ClickHouseWriter.java` | CONFLICT content | 1 (`start()` builder chain, marker-inclusive lines 96-100 of the merge-tree output) | Union: `.setTargetTableFilter(...)` then `.setCompressionMethod(...)` |
| `ClickHouseHelperClient.java` | CONFLICT content | 6 (fields, ctor assignment, `getDefaultClientOptions`, `createClientV2` chain, builder field, builder setters) | 4 union hunks (fields, ctor, builder field, builder setters); 2 resolve to upstream (V1 options, V2 chain) |
| `build.gradle.kts`, `StructToJsonMap.java`, `ClickHouseTestHelpers.java` | auto-merged both sides | — | Read anyway; verdicts below |
| `CHANGELOG.md`, `AtLeastOnceBufferStrategy.java`, `KeeperStateProvider.java`, `ClickHouseSinkTaskBufferTest`, `ClickHouseSinkTaskSchemalessTest`, `ClickHouseSinkTaskWithSchemaTest`, `StructToJsonMapTest` (new), `ClickHouseHelperClientTest`, `SchemaTestData`, `SchemalessTestData`, `union_two_records.json` (A and D), `ClickHouseSinkConnectorIntegrationTest`, `clickhouse_sink_avro_json.json` (new) | upstream-only, clean | — | Arrive byte-identical to `v1.7.0` |

That is 22 upstream paths: 5 conflicted + 3 auto-merged-both-sides + 14 upstream-only. The fork-touched-and-upstream-touched set (`comm -12` of the two name-only diffs) is exactly the 8 files in the first two rows. [VERIFIED: `git diff --name-only v1.6.0 v1.7.0` vs `git diff --name-only v1.6.0 c61d90ce` this session; conflict counts from marker scan of the merge-tree result]

**Exact hunk text and resolution (conflict markers seen in the merged tree):**

`ClickHouseHelperClient.java`:

| Hunk | Fork (HEAD) side | Upstream side | Resolve to |
|------|------------------|---------------|-----------|
| fields | `private final String targetTableFilter;` | `@Getter` + `private final boolean setCompressionMethod;` | both, in that order |
| ctor | `this.targetTableFilter = builder.targetTableFilter;` | `this.setCompressionMethod = builder.setCompressionMethod;` | both |
| `getDefaultClientOptions` | `options.put(ClickHouseClientOption.CUSTOM_SETTINGS, NETWORK_COMPRESSION_METHOD_SETTING + "=" + NETWORK_COMPRESSION_METHOD_LZ4);` | `if (setCompressionMethod) { options.put(CUSTOM_SETTINGS, NETWORK_COMPRESSION_METHOD + "=" + LZ4); }` then `options.put(ClickHouseClientOption.COMPRESS, false);` | **upstream** |
| `createClientV2` chain | `.setDefaultDatabase(this.database)` + comment + `.serverSetting(NETWORK_COMPRESSION_METHOD_SETTING, NETWORK_COMPRESSION_METHOD_LZ4);` | `.compressServerResponse(false)` + `.setDefaultDatabase(this.database);` (the `if (setCompressionMethod) clientBuilder.serverSetting(...)` below the hunk auto-merges) | **upstream** |
| builder field | `private String targetTableFilter = "";` | `private boolean setCompressionMethod = ClickHouseSinkConfig.setCompressionMethodDefault;` | both |
| builder setters | `setTargetTableFilter(...) { this.targetTableFilter = ...;` | `setCompressionMethod(...) { this.setCompressionMethod = ...;` — **both share the trailing `return this; }`** | write both methods out in full (see Code Examples) |

`ClickHouseSinkConfig.java`: the four hunks add `TARGET_TABLE_FILTER` vs `SET_COMPRESSION_METHOD`/`NETWORK_COMPRESSION_METHOD`/`LZ4` constants; the fork's `targetTableFilter`/`jsonInsertColumns`/`jsonInsertFormatExplicit` fields vs `setCompressionMethod`; the two constructor assignments; the two `configDef.define` blocks. Keep every line from both sides. `setCompressionMethodDefault = Boolean.TRUE` auto-merges outside any hunk (merged line 100).

**Auto-merged hunks inside fork-touched files: review verdicts (pre-fill for Section 2):**

Line numbers below come from the merge-tree output (conflict markers included); they shift by a few lines in the real merge.

| File | Hunk | Verdict |
|------|------|---------|
| `ClickHouseWriter` | `chcTmp` builder gains `.setCompressionMethod(csc.isSetCompressionMethod())` (merged ~1507-1520) | Correct, but see Finding 1: only reachable on V1 insert paths |
| `ClickHouseWriter` | `case MAP:` upstream `if (mapTmp == null) {...}` lands **below** the fork's `if (value.getObject() == null)` guard | **WRONG by intent: dead code (silent shadow).** Fixed in the WRT commit |
| `ClickHouseWriter` | `doWriteCol` defaults branch `colType == Type.ARRAY \|\| colType == Type.MAP` -> `writeNonNull` (merged 975) | Correct: this is the `00` that doubles as the empty-map size |
| `ClickHouseWriter` | `doWriteCol` non-defaults branch same condition (merged 991) | Correct |
| `ClickHouseWriter` | `cleanupExtraFields` null non-nullable `Type.MAP` -> `Collections.emptyMap()` (merged 1423) | Correct; the fork guard on the line above it (P9-IN-01) is untouched and stays Phase 13 |
| `build.gradle.kts` | upstream `configurations.all { ... withCapability("org.lz4:lz4-java") ... select(fork) }` (merged lines 64-74) + `implementation(libs.lz4.java)` + `clickhouseDependencies(libs.lz4.java)` + integrationTest `clickhouse-jdbc` removed | Correct; **but** the fork's `configurations.configureEach { ... select("at.yawk.lz4:lz4-java:1.10.2") }` block survives (merged lines 153-159) so there are 2 `withCapability("org.lz4:lz4-java")` rules. Delete the fork block in the BLD commit |
| `StructToJsonMap` | null optional Map -> `new Data(field.schema(), null)` | Correct; fork delta in this file is import order only (per v1.6.0 Recorded Facts) |
| `ClickHouseTestHelpers` | `createClient` gains `.setCompressionMethod(csc.isSetCompressionMethod())` (testFixtures:434-444 today) | Correct; this is the 4th builder site, outside `src/main`. The version-default canary `ClickHouseVersionDefaultUnitTest` is Docker-free and must be green at M |

### Recommended repository changes (by commit)

```
.planning/scripts/verify-merge-invariants.sh        # P0: PRE_TAG/UPSTREAM_REF/ANCHORS_FILE env, provenance, anchors
.planning/scripts/conflicted-paths-v1.7.0.txt       # A: generated from merge-tree, 5 lines (do NOT overwrite conflicted-paths.txt)
.planning/scripts/fork-anchors.txt                  # A: authored after M, counts measured at M, updated by every anchor-changing commit
.planning/MERGE-CHECKLIST-v1.7.0.md                 # A/DOC: Sections 1-2, Recorded Facts from 17
src/main/.../ClickHouseWriter.java                  # WRT: 1 condition + comment
src/test/.../db/NestedNullableCompositeWriteTest.java        # WRT: add cases only
src/test/.../db/NullOptionalMapBytesTest.java (new)          # WRT: Docker-free #820 pins (name is discretion)
src/main/.../db/helper/ClickHouseHelperClient.java  # CMP: delete 2 dead constants + stale comment
src/test/.../db/helper/NetworkCompressionPin{Unit,Live}Test.java   # CMP: extend in place
src/test/.../sink/NewUpstreamConfigDefaultsUnitTest.java  # CMP: add a 14th-option method
src/test/.../sink/CompressionFlagLiveTest.java (new)        # CMP-03 (name is discretion)
build.gradle.kts                                    # BLD: delete lines 153-159 of the merged file
docs/FORK-DELTA.md, .claude/CLAUDE.md, .planning/codebase/STACK.md, .planning/MERGE-CHECKLIST.md  # BLD wording
src/test/.../state/provider/KeeperStateProviderRestartLiveTest.java (new)  # EOS
```

### Pattern 1: Merge-commit line provenance (script)

**What:** For every path in `union(conflicted paths from merge-tree of M^1/M^2, git diff --name-only M^1 M)`, every line of `M:path` must appear in `M^1:path` or `M^2:path`. This catches hand-authored lines anywhere in the merge commit (for example someone slipping `&& col.isNullable()` into the merge), not just in conflicted files.

**Why set membership is enough:** D-02 forbids authored lines; ordering and multiplicity are the compiler's problem (D-03 gate), and the union hunks legitimately duplicate shared `return this; }` lines.

**Verified prototype** (throwaway script in `/tmp`, run against the v1.6.0 merge `673d29c3`): `paths=40 viol=0`, with 5 deleted/moved paths reported as "absent in M" and skipped. Its first version reported 30+ false violations. **Cause: `git show`/`git cat-file` inside a `while read` loop consumed the loop's stdin.** Always feed the loop on fd 3 (`while IFS= read -r p <&3; do ... done 3< <(...)`) or read into an array first.

```bash
# Source: prototype validated this session against 673d29c3 (v1.6.0 merge) - bash, not zsh
M="$1"; P1=$(git rev-parse "$M^1"); P2=$(git rev-parse "$M^2")
viol=0
while IFS= read -r p <&3; do
  git cat-file -e "$M:$p" 2>/dev/null || continue            # deleted/moved in M
  { for r in "$P1" "$P2"; do git cat-file -e "$r:$p" 2>/dev/null && { git show "$r:$p"; echo; }; done; } > "$TMP/u"
  git show "$M:$p" > "$TMP/m"
  extra=$(grep -vxFf "$TMP/u" "$TMP/m" | wc -l | tr -d ' ')
  [ "$extra" != 0 ] && { echo "PROVENANCE VIOLATION: $extra authored line(s) in $p" >&2; viol=1; }
done 3< <( { git merge-tree --write-tree --name-only "$P1" "$P2" | awk 'NR==1{next} /^$/{exit} {print}'; \
             git diff --name-only "$P1" "$M"; } | sort -u )
```

Also assert `git rev-parse "$M^2"` equals `git rev-parse "$UPSTREAM_REF^{commit}"` (the merge really took the tag) and `git merge-base --is-ancestor "$PRE_TAG" "$M^1"`. `git merge-tree --write-tree --name-only` prints the tree OID on line 1, then one path per line until a blank line, then informational messages (confirmed on git 2.50.1).

**Interface to keep:** `verify-merge-invariants.sh <merge-sha> [range-end]`, with `PRE_TAG`, `UPSTREAM_REF`, `ANCHORS_FILE` as env overrides. Defaults to the v1.7.0 values; with `PRE_TAG=pre-v1.6.0-sync UPSTREAM_REF=v1.6.0 ANCHORS_FILE=` it must still return 0 against `673d29c3` (the unmodified script returns 0 today). Skip the anchor step when `ANCHORS_FILE` is empty: the v1.6.0 merge is the commit where seven fork behaviours vanished, so anchors cannot hold at that SHA. Keep the runtime-assembled conflict-marker trick and the fail-closed `git grep` handling from commit `4b03e6f3` unchanged.

### Pattern 2: Anchor file

Format: TAB-separated `path<TAB>fixed-string<TAB>expected-line-count<TAB>FORK-DELTA row`; count with `grep -cF -e "$pattern" "$path"` (counts matching **lines**, so say so in the file header). Tab, not `|`, because real patterns contain `|`. Evaluate anchors at `RANGE_END` (HEAD), and add a one-off survival proof in the checklist: every anchor not retired in this phase has the same count at `pre-v1.7.0-sync` and at the merge SHA.

Author the file in commit [A], after M, so the merge commit stays pure and no `.planning` commit interleaves with conflict resolution. Initial set, with counts **measured this session on the unmerged tree** (code identical to `c61d90ce`), and what Phase 12 does to each:

| Path | Pattern | Count now | After Phase 12 | Backs |
|------|---------|-----------|----------------|-------|
| `sink/ClickHouseSinkConfig.java` | `TARGET_TABLE_FILTER` | 3 | 3 (upstream adds no line containing it) | row 29 |
| `sink/ClickHouseSinkConfig.java` | `jsonInsertFormatExplicit` | 2 | 2 | row 29 |
| `sink/ClickHouseSinkConfig.java` | `//this.addClickHouseSetting("async_insert", "0", false);` | 1 | 1 | row 30 |
| `sink/ClickHouseSinkTask.java` | `public void onPartitionsRevoked(Collection<TopicPartition> partitions) {` | 1 | 1 | row 32 |
| `util/jmx/SinkTaskStatistics.java` | `connector=%s` | 1 | 1 | row 31 |
| `sink/db/ClickHouseWriter.java` | `shouldSkipTableMetadata()` | 6 | 6 (Phase 13 R-01 makes 7: update then) | row 33 |
| `sink/db/ClickHouseWriter.java` | `csc.getJsonInsertColumns()` | 1 | 1 | row 34 |
| `sink/db/ClickHouseWriter.java` | `case "DYNAMIC":` | 1 | 1 | row 35 |
| `sink/db/ClickHouseWriter.java` | `buildRowBinaryUnsupportedTypeMessage` | 5 | 5 | row 35 |
| `sink/db/ClickHouseWriter.java` | `.setTargetTableFilter(csc.getTargetTableFilter())` | 1 | 1 | row 37 |
| `sink/db/helper/ClickHouseHelperClient.java` | `buildTableFilterClause` | 4 | 4 | row 37 |
| `sink/db/helper/ClickHouseHelperClient.java` | `shouldSkipDescribeRow` | 3 | 3 | row 38 |
| `sink/db/helper/ClickHouseHelperClient.java` | `StandardCharsets.UTF_8` | 1 | 1 | row 39 |
| `sink/db/helper/ClickHouseHelperClient.java` | `InputStreamReader` | 2 (import + the one use) | 2 | row 39 |
| `sink/db/mapping/Type.java` | `DYNAMIC` | 1 | 1 | row 40 |
| `sink/db/mapping/Column.java` | `Dynamic` | 1 | 1 | row 40 |
| `META-INF/services/...Transformation` | `com.clickhouse.kafka.connect.transforms` | 7 | 7 | row 41 |
| `testFixtures/.../ClickHouseTestHelpers.java` | `CLICKHOUSE_VERSION_LATEST` | 2 | 2 | row 58 |
| `testFixtures/.../ClickHouseTestHelpers.java` | `StandardCharsets.UTF_8` | 3 | 3 | row 59 |
| `gradle/libs.versions.toml` | `kafka = "4.1.1"` | 1 | 1 | row 51 |
| `build.gradle.kts` | `slf4j-reload4j:2.0.17` | 1 | 1 | row 50 |
| `build.gradle.kts` | `windows-1252` | 2 | 2 | row 49 |
| `build.gradle.kts` | `withCapability("org.lz4:lz4-java")` | 1 (fork's) | 2 at M, **1** after BLD | BLD-09 |
| `ClickHouseWriter.java` / `KeeperStateProvider.java` | `new ClickHouseHelperClient.ClickHouseClientBuilder(` | 2 / 1 | 2 / 1 | CMP-01 |
| `ClickHouseWriter.java` / `KeeperStateProvider.java` | `.setCompressionMethod(csc.isSetCompressionMethod())` | 0 / 0 | **2 / 1** at M | CMP-01 |
| `ClickHouseHelperClient.java` | `.compressServerResponse(false)` | 0 | 1 at M | CMP-01/02 |
| `ClickHouseHelperClient.java` | `NETWORK_COMPRESSION_METHOD_SETTING` | 4 | 2 at M (the 2 dead constants remain), **0** after CMP | CMP-01 |
| `docs/FORK-DELTA.md`, `.claude/CLAUDE.md`, `.planning/codebase/STACK.md`, `.planning/MERGE-CHECKLIST.md` | `1.10.2` | 1 each (FORK-DELTA:49, CLAUDE.md:61, STACK.md:76, MERGE-CHECKLIST.md:74) | **0** after BLD | BLD-09 |

[VERIFIED: "Count now" values from `grep -cF` runs this session on the working tree. Every "After" value is derived from the merge hunks above, not measured; recount all of them at M before committing the file]

The builder-site invariant reads as: lines containing `ClickHouseClientBuilder(` across `src/main` (3) equals lines containing `.setCompressionMethod(csc.isSetCompressionMethod())` across `src/main` (3), excluding the helper's own constructor at `ClickHouseHelperClient.java:792`, which is a declaration, not a `new` expression, so the `new ...Builder(` pattern already excludes it.

### Pattern 3: Docker-free pins for CMP-02 (extend in place)

`NetworkCompressionPinUnitTest` builds clients against `localhost:1` and asserts configuration only (4 existing cases); keep its `buildClient(String)` helper and add cases beside it. Assertion targets, all in the same class, none needing Docker:

- V1 options: `chc.getDefaultClientOptions().get(ClickHouseClientOption.COMPRESS)` equals `false` (upstream puts the boolean: `options.put(ClickHouseClientOption.COMPRESS, false);`).
- V1 node: `chc.getServer().getOptions().get("compress")` equals `"false"` (upstream: `options.put(ClickHouseClientOption.COMPRESS.getKey(), "false");`; the V1 key is `compress`, default `true`).
- V2: `chc.getClient().getConfiguration().get("compress")` equals `"false"` (V2 key `compress`, default `true`; `compress` is `COMPRESS_SERVER_RESPONSE`). [ASSUMED: the stored string is exactly `"false"`; the first run will show it]
- Flag off: build with `.setCompressionMethod(false)` and assert (a) V1 options have no `CUSTOM_SETTINGS`, (b) V1 node options have no `network_compression_method`, (c) V2 config has no `clickhouse_setting_network_compression_method` (the class's own `V2_SETTING_KEY`), and (d) all three `compress` assertions above still hold.
- Optional but recommended pin of the rollout caveat: V2 with `?compress=true` makes `getConfiguration().get("compress")` `"true"` (operator value wins because the `jdbcConnectionProperties` loop runs after `compressServerResponse(false)`); this pins what ROLL-01 must tell operators.

Add a separate method to `NewUpstreamConfigDefaultsUnitTest` rather than editing the existing "thirteen" test: default `isSetCompressionMethod()` is `true` and equals `ClickHouseSinkConfig.setCompressionMethodDefault`, canonical key `"setCompressionMethod"`, alias `"set_compression_method"`, and `ConfigDef` default `true`. (Upstream's `testSinkConfig_SetCompressionMethod` already covers key, alias and default, but only in a Docker-tagged class, so this is the `testUnit` copy.)

### Anti-Patterns to Avoid

- **"Keep both" on the builder setters by editing conflict markers mechanically.** The shared `return this; }` trailer yields a setter without a return. Write both methods out.
- **Resolving the V2 chain hunk as a union.** Keeping the fork's `.serverSetting(...)` makes upstream's `if (setCompressionMethod)` below it meaningless and fails upstream's `testSetCompressionMethod_Disabled`.
- **Running repo-wide `spotlessApply`.** Baseline `spotlessCheck` is red on 44 files; applying it rewrites unrelated fork files.
- **Putting the scoped guard, the dead-constant deletion or the LZ4 block deletion into the merge commit.** D-02.
- **Reading a probe outcome without checking the control took effect.** See the experiment protocol.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Conflicted-path list | A hand-typed list | `git merge-tree --write-tree --name-only <M^1> <M^2>` (and commit the generated 5-line output as `conflicted-paths-v1.7.0.txt`) | v1.6.0's list was off by one until measured (Recorded Fact 5) |
| Resolved LZ4 version proof | Reading the catalog | `./gradlew dependencyInsight --dependency lz4-java --configuration runtimeClasspath` (and `clickhouseDependencies`) | The catalog says what was requested, not what resolved; Gradle's capability rule decides |
| Server version for the Recorded Fact | Parsing logs | `chc.version()` (used by `NetworkCompressionPinLiveTest.versionOnBothClients`) | Already on both clients |
| State-read assertions | Mocking the provider | A real `KeeperStateProvider` against the shared container | AGENTS.md: do not mock the thing under test; the whole point is the live read path |
| Per-test isolation of the KeeperMap table | Dropping and recreating shared tables | A unique `zkPath` **and** a unique `zkDatabase` (the table name) per test | KeeperMap tables with the same path share data across databases |

**Key insight:** every guard in this phase has already failed silently once (seven vanished fork behaviours in v1.6.0, the image-default reversion). Prefer mechanical checks (script, grep counts, dependency resolution output) over recorded prose.

## Common Pitfalls

### Pitfall 1: The writer's temporary client is not exercised by a V2 sink-task test
**What goes wrong:** D-11 expects a full `ClickHouseSinkTask` on client V2 to exercise "the writer's main client and its temporary client". It does not. `chcTmp` is built inside `getMutationRequest` (`ClickHouseWriter.java:1573-1588`), which is called only from `doInsertRawBinaryV1` (1228/1230), `doInsertJsonV1` (1333) and `doInsertStringV1` (1481). Under V2, `doInsertRawBinary` dispatches to `doInsertRawBinaryV2` (line 1072-1075), `doInsertJson` to `doInsertJsonV2` (1257-1260) and `doInsertString` to `doInsertStringV2`; none builds `chcTmp`.
**Why it happens:** the temporary client exists to carry a per-record database into a V1 `ClickHouseNode`.
**How to avoid:** (a) keep the V2 sink-task test (production path); (b) add one V1 case (`client_version=V1`, the key is `ClickHouseSinkConnector.CLIENT_VERSION = "client_version"`) so `chcTmp` is built under `setCompressionMethod=false`; (c) rely on the D-08 builder-site invariant (3 sites = 3 `.setCompressionMethod(...)` lines) for the threading proof, since `chcTmp` is a local variable and cannot be inspected from a test. Observe that `chcTmp.getServer()` carries `compress=false` regardless of the flag, so a live V1 insert passing says little about whether the flag was threaded; the count invariant is the real check.
**Warning signs:** `git grep -c "setCompressionMethod(csc" src/main` is not 3.
[VERIFIED: ClickHouseWriter.java:1225-1230, 1332-1333, 1480-1481, 1573-1588 read this session]

### Pitfall 2: The scoped MAP guard is only reachable from a hand-built Column
**What goes wrong:** `Column.java:386` states "ClickHouse forbids Nullable wrapping for Array, Map, and Variant types." If that holds, `Column.isNullable()` is never true for a MAP column built from a server `DESCRIBE`, so after scoping, the fork's `writeNull` branch fires only in unit tests that construct `Column.builder()...isNullable(true)`. Real nested null Maps (`Map(String, Map(...))` with a null inner value) now fall through to upstream's `mapTmp == null` branch: with defaults it writes `varint 0` (an empty map, accidentally right); without defaults it writes nothing (a short stream). That is WR-02's territory (Phase 13) and is no regression, because the pre-merge fork wrote a stray `0x01` there.
**How to avoid:** do not widen the guard to "fix" nested nulls in Phase 12; record the observation as a Recorded Fact so Phase 13's WR-02 design starts from it. Keep the CR-02 MAP pin as it is.
[CITED: src/main/.../db/mapping/Column.java:386 - "ClickHouse forbids Nullable wrapping for Array, Map, and Variant types."; the server rule itself is [ASSUMED]]

### Pitfall 3: WRT-09's "re-pin" set is probably empty
**What goes wrong:** the plan or verifier expects changed assertions. `NestedNullableCompositeWriteTest.nullMapValueWritesNullMarker` builds `Column.builder().name("nested_map").type(Type.MAP).isNullable(true).build()` and asserts `{1}` after `doWriteColValue(column, out, nullData, true)`. With `&& col.isNullable()` this case still takes the fork branch and still writes `01`. The TUPLE and VARIANT pins are untouched by the guard. No other test in `src/test` references `Type.MAP` with null data (`grep -rln "Type.MAP" src/test` lists only this class, `ColumnTest` and `TableTest`).
**How to avoid:** make WRT-09 an evidence task: run `NestedNullableCompositeWriteTest` unmodified against the scoped guard and record "0 existing assertions changed, because the pin uses a nullable column"; add new non-nullable cases next to it, each with a comment citing the 2026-10-07 approval. If the user expected a changed assertion, that is a question for the user, not a reason to edit a passing pin.
**Warning signs:** a diff that modifies `assertArrayEquals(new byte[] {NULL_MARKER}, ...)` in the existing MAP test.
[VERIFIED: NestedNullableCompositeWriteTest.java:32-41 read this session]

### Pitfall 4: `while read` plus `git` inside the loop eats stdin
Covered under Pattern 1. Reproduced this session; the same loop gave 30+ false violations until moved to fd 3. The final script runs under `bash`, not the interactive zsh.

### Pitfall 5: `dependencyInsight` prints two `lz4-java` lines even when resolution is correct
**What goes wrong:** the BLD-09 check "exactly one `at.yawk.lz4:lz4-java`" is easy to mis-grep. Baseline output contains the selected component `at.yawk.lz4:lz4-java:1.10.2` **and** a requested-edge line `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.10.2`.
**How to avoid:** assert (1) `grep -cE '^at\.yawk\.lz4:lz4-java:[0-9.]+$'` over the `-q` output is at least 1 and `sort -u` of those headline lines is exactly 1 distinct version, (2) no line of the form `^org\.lz4:lz4-java:[0-9.]+$` without `->`, (3) `grep -c 'withCapability("org.lz4:lz4-java")' build.gradle.kts` is 1. Run it for `runtimeClasspath` and `clickhouseDependencies` (the Confluent archive input).
[VERIFIED: baseline `dependencyInsight` run this session]

### Pitfall 6: Old-summary read can look green behind the dedup window
`exactlyOnceStateMismatchTest` is skipped standalone (11 parameterised cases, FORK-DELTA named skips), and the `strictChunking*` replay tests in `ClickHouseSinkTaskBufferTest` pass if either the state read works or ClickHouse's dedup token suppresses the duplicate. EOS-01 therefore needs a direct provider test, and the mutation check is the only proof it can fail.

### Pitfall 7: Spurious red full suite from `latest` drift
Floating tag + always-pull means a new `latest` image can change the result between two runs. Record `chc.version()` and the image creation date in each Recorded Fact; the local image today is `26.9.12.8` created `2026-10-06T18:12:57Z`.

### Pitfall 8: Phase 12's D-17 anchors will drift unless the file is authored after M
If `fork-anchors.txt` is committed before the merge with D-17-era counts, the merge legitimately changes them and the "drift is always deliberate" rule makes the script fail at M. Author it in commit [A] after M; update it in the same commit as each of WRT, CMP, BLD.

## Code Examples

### Resolved builder setters (union, both returns present)

```java
// ClickHouseHelperClient.ClickHouseClientBuilder - the two setters each carry their own return
public ClickHouseClientBuilder setTargetTableFilter(String targetTableFilter) {
    this.targetTableFilter = targetTableFilter;
    return this;
}

public ClickHouseClientBuilder setCompressionMethod(boolean setCompressionMethod) {
    this.setCompressionMethod = setCompressionMethod;
    return this;
}
```

### Resolved `ClickHouseWriter.start()` chain

```java
                .enableReplicaPinning(csc.isEnableReplicaPinning())
                .setTargetTableFilter(csc.getTargetTableFilter())
                .setCompressionMethod(csc.isSetCompressionMethod())
                .build();
```

### Scoped MAP guard (WRT commit; new code, never in M)

The fork's text (`ClickHouseWriter.java:612-622` on the current tree) begins `case MAP:` then `if (value.getObject() == null) {`; the merged file has the same text with upstream's `mapTmp == null` block below it. Change only the condition and keep the existing comment, adding the reason the scope exists:

```java
case MAP:
    if (value.getObject() == null && col.isNullable()) {
        // existing CR-02 comment ...
        // Scoped to Nullable columns: for a non-nullable Map the null is upstream #820's
        // empty map, whose size byte doWriteCol already wrote (`00`, or `00 00` with defaults),
        // so a second marker here would desync the RowBinary stream.
        BinaryStreamUtils.writeNull(stream);
        break;
    }
    Map<?, ?> mapTmp = (Map<?, ?>) value.getObject();
    if (mapTmp == null) {            // upstream #820 block, now reachable
        if (defaultsSupport) {
            BinaryStreamUtils.writeVarInt(stream, 0);
        }
        break;
    }
```

### Docker-free #820 byte pins (WRT; same package as `NestedNullableCompositeWriteTest`, extends nothing)

`doWriteCol` is `protected` (merged line 952) and the test lives in package `com.clickhouse.kafka.connect.sink.db`, so it is callable without reflection. Expected bytes are from the milestone byte probe (`ARCHITECTURE.md` headline 2: `00` / `00 00` after the scoped guard, `00 01` before). Run RED first on M (expect `00 01`), then GREEN after the guard.

```java
ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
Column nonNullableMap = Column.builder().name("m").type(Type.MAP).isNullable(false).build();
Data nullMap = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);

ByteArrayOutputStream plain = new ByteArrayOutputStream();
writer.doWriteCol(nullMap, true, nonNullableMap, plain, false);
assertArrayEquals(new byte[] {0x00}, plain.toByteArray());          // #820, RowBinary

ByteArrayOutputStream withDefaults = new ByteArrayOutputStream();
writer.doWriteCol(nullMap, true, nonNullableMap, withDefaults, true);
assertArrayEquals(new byte[] {0x00, 0x00}, withDefaults.toByteArray()); // #820, RowBinaryWithDefaults
```

Keep a third case that builds the column with `.isNullable(true)` and `doWriteColValue(..., true)` expecting `{0x01}` (the existing CR-02 pin already does this; do not duplicate it, reference it).

### EOS-01 restart-read test skeleton (new class; extends `ClickHouseBase`)

```java
// Constructors: KeeperStateProvider(ClickHouseSinkConfig) builds its own helper client and calls init();
// there is no ping failure path that throws in this constructor (it logs and continues).
@ParameterizedTest @ValueSource(booleans = {true, false})
void stateWrittenByOneProviderIsReadByANewOne(boolean setCompressionMethod) {
    String id = "eos01_" + System.nanoTime();
    Map<String, String> props = getBaseProps();                       // client_version V2 unless CLIENT_VERSION=V1
    props.put(ClickHouseSinkConfig.ZK_PATH, "/" + id);                // must start with "/" (ZKPathValidator)
    props.put(ClickHouseSinkConfig.ZK_DATABASE, id);                  // this is the KeeperMap TABLE name
    props.put("setCompressionMethod", String.valueOf(setCompressionMethod));
    ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);

    String topic = "t_" + id;
    KeeperStateProvider writer = new KeeperStateProvider(csc);
    // StateRecord(topic, partition, maxOffset, minOffset, state, originalTopic) - max BEFORE min
    writer.setStateRecord(new StateRecord(topic, 0, 19, 10, State.BEFORE_PROCESSING, topic));
    writer.setStateRecord(new StateRecord(topic, 0, 19, 10, State.AFTER_PROCESSING, topic));

    KeeperStateProvider restarted = new KeeperStateProvider(new ClickHouseSinkConfig(props)); // new instance, empty stateMap
    StateRecord read = restarted.getStateRecord(topic, 0);
    assertEquals(State.AFTER_PROCESSING, read.getState());
    assertEquals(10, read.getMinOffset());
    assertEquals(19, read.getMaxOffset());
    assertEquals(State.NONE, restarted.getStateRecord(topic, 1).getState());  // fresh key
}
```

Facts this relies on: `setStateRecord` writes `INSERT ... VALUES ('<topic>-<partition>', min, max, '<state>')` and `getStateRecord` queries key `"%s-%d"`; `getTopicAndPartitionKey()` is `String.format("%s-%d", topic, partition)`; `State` values are `NONE, BEFORE_PROCESSING, IN_PROCESSING, AFTER_PROCESSING`. The existing state-machine `stateMap` is per instance (empty on a new provider), so the second provider can only get the values from ClickHouse. [VERIFIED: KeeperStateProvider.java:35-70, 95-135, State.java, TopicPartitionContainer.java:21-23, RangeContainer.java ctor `(String topic, int partition, long maxOffset, long minOffset)` all read this session]

### CMP-03 live test template

`AsyncInsertLiveTest` is the in-repo template: `getBaseProps()`, `ClickHouseTestHelpers.createClient(props)`, create a table with `chc.queryV2("CREATE TABLE ...")`, `props.put("name", uniqueConnectorName(testInfo))`, schemaless `Map` records, `start`/`put`/`stop` in `try/finally`, read back with an `ORDER BY` query and `assertEquals` per row. Parameterise over `setCompressionMethod` (true, false), and over `client_version` (`V2` required by D-11; add `V1` per Pitfall 1). Assert row values, not counts.

## SYNC-09 pin-test matrix (which gate runs where)

Classified from the class declarations (`extends ClickHouseBase` or a leading `@Tag("clickhouse")`); the Docker-free set was cross-checked against `build/test-results/testUnit/` (30 classes). Several classes named `*UnitTest` that mention `@Tag("clickhouse")` only in Javadoc are Docker-free; a naive grep misclassifies them.

| FORK-DELTA behaviour | Docker-free (`testUnit`) | Live (`test`, needs Docker) |
|----------------------|--------------------------|-----------------------------|
| `Type.DYNAMIC` fail-fast | `RowBinaryUnsupportedTypeGateTest`, `JsonDynamicTypeMappingTest` | `RowBinaryFailFastLiveTest`, `JsonDynamicRoundTripTest` |
| JMX `connector=<name>` | `MBeanNameUnitTest` | `MBeanRegistrationLiveTest` |
| `targetTableFilter` | `ClickHouseHelperClientForkUnitTest` | `ClickHouseHelperClientForkLiveTest`, `ClickHouseSinkTaskMappingTest` |
| describe sub-column skip | `ClickHouseHelperClientForkUnitTest` | `TableTest` |
| metadata-free JSON path | `MetadataFreeJsonInsertUnitTest`, `JsonInsertColumnsUnitTest` | `ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest`, `ClickHouseSinkTaskJsonInsertFormatTest` |
| no `async_insert` default | `AsyncInsertSettingsUnitTest` | `AsyncInsertLiveTest` |
| `CLICKHOUSE_VERSION_DEFAULT = latest` | `ClickHouseVersionDefaultUnitTest` | `ClickHouseTestHarnessTest` |
| 13 upstream options at defaults | `NewUpstreamConfigDefaultsUnitTest` | — |
| CR-02 composite null pins | `NestedNullableCompositeWriteTest` | — |
| D-17 / compression | `NetworkCompressionPinUnitTest` | `NetworkCompressionPinLiveTest` |
| SMTs | `ConvertToMapOrJsonTest`, `HeaderToValueTest`, `HeadersToValueTest`, `TableOperationFilterTest` | `SmtSinkLandingTest` |
| Connect API `4.1.1` | no test: proof is compile of `onPartitionsRevoked` (no `@Override`) plus `dependencyInsight --dependency connect-api` | — |

[VERIFIED: class declarations read by regex this session; `build/test-results/testUnit/*.xml` listing; FORK-DELTA "Pinned by" column]

## SYNC-10 inventory (upstream tests to accept unmodified)

Byte-compare check: `git diff --quiet v1.7.0 HEAD -- <path>` must pass for each of these 11 paths (none is fork-modified, so each must equal the `v1.7.0` blob at every commit on the branch): `ClickHouseSinkTaskBufferTest`, `ClickHouseSinkTaskSchemalessTest`, `ClickHouseSinkTaskWithSchemaTest`, `StructToJsonMapTest`, `ClickHouseHelperClientTest` (all under `src/test`), `SchemaTestData`, `SchemalessTestData`, the two `union_two_records.json` fixtures (testFixtures), plus `ClickHouseSinkConnectorIntegrationTest.java` and `clickhouse_sink_avro_json.json` (integrationTest). `ClickHouseTestHelpers.java` is excluded because it is fork-modified (it carries the harness contract).

New or changed test methods and where they run:

| Test | Class | Runs in |
|------|-------|---------|
| `nullMapTypeTest`, `nullMapTypeJsonInsertTest` | `ClickHouseSinkTaskWithSchemaTest` | live; the first is what the MAP shadow breaks |
| `nullMapTypesTest` | `ClickHouseSinkTaskSchemalessTest` | live (JSON route) |
| `optionalMapFieldNullDoesNotThrow`, `optionalMapFieldPresent` | `StructToJsonMapTest` | **Docker-free**; testUnit baseline 331 tests in 30 classes, so M should report 333 [VERIFIED baseline: sum of `tests=` in `build/test-results/testUnit/*.xml`; +2 is derived] |
| `testSetCompressionMethod_DefaultIsTrue`, `_Disabled`, `testSinkConfig_SetCompressionMethod` | `ClickHouseHelperClientTest` (extends `ClickHouseBase`) | live (but needs only config) |
| `retriableFlushFailureDoesNotDuplicateBufferedRecords` (#801) | `ClickHouseSinkTaskBufferTest` | live |
| #800 Avro record-union -> JSON and cluster `buildFromClause` | `ClickHouseSinkConnectorIntegrationTest` + `clickhouse_sink_avro_json.json` + testFixtures `union_two_records.json` | **`integrationTest` source set only**: `compileIntegrationTestJava` is the gate (HARN-06); it needs a Confluent platform and is not run by `./gradlew test` |

The last row is a skip with a reason SYNC-10 allows ("skip for a named reason recorded in `FORK-DELTA.md`"). Add it to FORK-DELTA's Named skip inventory as a sentence naming the `integrationTest` source set as compile-only, otherwise SYNC-10 is not closeable as written. No new `@Disabled` or `Assumptions` appear in upstream's diff (grep of the added lines found none), so the existing 18 named skips are unchanged. [VERIFIED: `git diff v1.6.0 v1.7.0 -- src/test src/testFixtures src/integrationTest` filtered for `assume|Assumptions|@Disabled|@Test`]

## D-17 inventory (what CMP-01 retires)

| Site | Today (current tree) | After M | After CMP |
|------|----------------------|---------|-----------|
| `ClickHouseHelperClient.java:107-110` | comment `// ClickHouse 26.9 defaults network_compression_method to ZSTD, which client 0.9.6 cannot decode.` / `// Remove once the pinned client decodes ZSTD responses.` and `static final String NETWORK_COMPRESSION_METHOD_SETTING = "network_compression_method";` / `static final String NETWORK_COMPRESSION_METHOD_LZ4 = "lz4";` | still present, **unused** (outside any conflict hunk) | deleted |
| `:115-116` V1 options | unconditional `CUSTOM_SETTINGS` | upstream's `if (setCompressionMethod)` + `COMPRESS=false` | unchanged |
| `:185-187` V2 chain | `.setDefaultDatabase(...)` + `.serverSetting(NETWORK_COMPRESSION_METHOD_SETTING, NETWORK_COMPRESSION_METHOD_LZ4);` | `.compressServerResponse(false).setDefaultDatabase(this.database);` + auto-merged `if (setCompressionMethod) clientBuilder.serverSetting(...)` | unchanged |
| references to the two constants | none outside this file, tests included (they use string literals) | none | none |
| pin tests | `NetworkCompressionPinUnitTest` (4 cases), `NetworkCompressionPinLiveTest` (5 cases) = the "9 existing cases" | green unchanged | extended in place |

Upstream's public replacements: `ClickHouseHelperClient.NETWORK_COMPRESSION_METHOD = "network_compression_method"` and `LZ4 = "lz4"` (merged lines 77-78), `getDefaultClientOptions()` at upstream `:108-111`, V1 node options `:141-144`, V2 `:184-189`, builder field `:700`, setter `:766`. [VERIFIED: current helper lines 107-116, 185-187 read this session; upstream helper read from `git show v1.7.0:...` with line numbers from `grep -n`]

Client option facts (probed with a throwaway class against the cached 0.9.6 jars): V2 `ClientConfigProperties.COMPRESS_SERVER_RESPONSE` key `compress`, default `true`; `COMPRESS_CLIENT_REQUEST` key `decompress`, default `false`; V1 `ClickHouseClientOption.COMPRESS` key `compress`, default `true`. [VERIFIED: probe output `COMPRESS_SERVER_RESPONSE key=compress default=true`, `COMPRESS_CLIENT_REQUEST key=decompress default=false`, `V1 COMPRESS key=compress default=true`]

## Live compression experiment (D-12) and CMP-03 protocol

The server is ClickHouse `26.9.12.8` locally, so the ZSTD default applies. Run the probe as a throwaway class extending `ClickHouseBase` that is **never committed**, and record results in a Recorded Fact (server version, client `0.9.6`, per-configuration outcome, exact error text with credentials scrubbed).

| Config | `setCompressionMethod` | `jdbcConnectionProperties` | Protection in force | Expected on 26.9+ (if the original failure model is right) |
|--------|-----------------------|---------------------------|---------------------|-------------------------------------------------------------|
| S0 default | true | none | `compress=false` + lz4 | passes |
| S1 CMP-03 case | **false** | none | `compress=false` only | passes (this is the D-13 gate) |
| S2 | true | `?compress=true` | lz4 only | passes if lz4 alone suffices |
| S3 | **false** | `?compress=true` | none | fails: `Invalid LZ4 magic byte: '-112'` surfaced as `Connection to ClickHouse is not active` (FORK-DELTA row 36 text) |

Run each on V2 and on V1 (`getDefaultClientOptions()` + `chc.getServer()` as `NetworkCompressionPinLiveTest` does). **Before reading any outcome, assert the control took effect**: V2 `chc.getClient().getConfiguration().get("compress")`, V1 `chc.getServer().getOptions().get("compress")`, and `getSetting('network_compression_method')` from the typed query. The V1 case is genuinely uncertain: `?compress=true` becomes a node URL parameter while `compress=false` is also put in the node options map, and which wins is not documented here. If the stripped configuration did not actually strip, the result is **no observation**, not "protection unnecessary". A probe that unexpectedly *succeeds* in S3 refutes the premise: record it and escalate, do not tidy it away. Only S1 is a committed assertion; if S1 fails, stop at the D-13 blocking checkpoint.

The CMP-03 `FORK-DELTA.md` sentence is derived only from this observation: S1 passing proves `compress=false` suffices on its own; S2 passing proves lz4 alone suffices when response compression is requested; S3 failing proves at least one is needed. State what was observed, not what the model predicted.

## EOS-01 mutation check (D-15/D-16)

Procedure: after the new test is green, edit `KeeperStateProvider.getStateRecord` in the working tree back to the pre-v1.7.0 shape that the current unmerged tree still has (`long totalResultsFound = response.getSummary().getResultRows();` at line 107 and `ClickHouseRecord r = response.firstRecord();` at line 115, replacing the `response.stream()` collection and `records.get(0)`), run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*KeeperStateProvider*'`, capture the failing assertion and values, restore with `git checkout -- <file>`, confirm green, then prove byte identity with `git diff --quiet v1.7.0 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java`.

Expectation, flagged as a hypothesis not a fact: the summary header `result_rows` is a response-header value, and ClickHouse documents `wait_end_of_query` as delaying header transmission until the query completes, so on a streamed unbuffered response the header can carry `0`. A `0` returns `State.NONE` for a key that does have state, which fails the test at `assertEquals(State.AFTER_PROCESSING, read.getState())` with actual `NONE`. Upstream's commit `a7cc5ec9` pairs the read change with `COMPRESS=false`, which is consistent with that, but the repository evidence does not say which. If the revert stays green, follow D-16: vary the flag, an empty vs one-row result, and `send_progress_in_http_headers`/`wait_end_of_query`, time-boxed, then stop at the blocking checkpoint. [CITED: https://clickhouse.com/docs/interfaces/http - `result_rows` is a field of `X-ClickHouse-Summary`; `wait_end_of_query=1` delays header transmission until completion]; outcome [ASSUMED].

## Runtime State Inventory

Not a rename/refactor phase; omitted. (Git state that the plan must respect: `custom_build` and `origin/custom_build` both at `c61d90ce`; `sync/v1.6.0` at `f4fd6fcb` = `c61d90ce` + **20** planning-only commits, not the 18 CONTEXT counted at discussion time; `.planning/.gsd-ws-arg` is untracked and not ignored. Cut `sync/v1.7.0` from the HEAD at execution time.)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Fork forces `network_compression_method=lz4` unconditionally (D-17) | Upstream `setCompressionMethod` flag (default `true`) plus unconditional `compress=false` (V1 options, V1 node, V2) | upstream `v1.7.0`, issue #841 | Response compression is now off by upstream design; lz4 is defence in depth; `setCompressionMethod=false` removes only the lz4 half |
| Two LZ4 capability rules, fork pinned to `1.10.2` | One upstream rule + catalog `lz4Java = "1.10.4"` | `v1.7.0` | Fork block redundant; delete in BLD |
| State read via `getSummary().getResultRows()` | `response.stream()` row count | `v1.7.0`, commit `a7cc5ec9` | The read no longer depends on a header value |

**Deprecated/outdated:** `# FORK OVERRIDE (upstream 0.9.5)` (client `0.9.6` is now upstream's own value); FORK-DELTA rows 36, 49, 51 and the "For the next syncer" eighth-behaviour paragraph (line 167) describe D-17 as live; "1.10.2" wording in four files.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker daemon (via colima socket) | live gates, CMP-03, EOS-01, probe | yes (socket pings `OK`; `docker` CLI is not on PATH, Testcontainers uses `DOCKER_HOST`) | Docker 27.4.0 linux/arm64 | none needed |
| `clickhouse/clickhouse-server:latest` | all live tests | yes, local image | `26.9.12.8`, created 2026-10-06T18:12:57Z (always-pull will re-pull at test start, needs network) | pinned `CLICKHOUSE_VERSION` is possible but out of scope |
| JDK 17 toolchain | Gradle test JVM | yes (`~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`); shell JDK is 21 | 17 / 21.0.12 | — |
| Gradle wrapper 9.6.1, offline | all gates | yes (`--offline` ran `dependencyInsight`, `testUnit`, `spotlessCheck`) | 9.6.1 | — |
| git | merge, merge-tree | yes | 2.50.1 (`merge-tree --write-tree` needs >= 2.38) | — |
| `lz4-java` 1.10.2 and 1.10.4 jars | BLD proof offline | yes, both in the Gradle cache | 1.10.2, 1.10.4 | — |
| `yq`, `docker` CLI | AGENTS.md matrix lookup only | no | — | not needed |

`colima status` prints "colima is not running" while the socket serves a healthy daemon (a profile-name mismatch); do not "fix" it by restarting colima. Use `curl --unix-socket "$HOME/.colima/vz/docker.sock" http://localhost/_ping` as the pre-flight check. `TESTCONTAINERS_HOST_OVERRIDE` is the literal string `null` in this shell, so every Gradle call needs `env -u`.

**Missing dependencies with no fallback:** none. **Missing with fallback:** none.

## Validation Architecture

`workflow.nyquist_validation` is `false` in `.planning/config.json`, so this section would normally be skipped; it is included compactly because the orchestrator asked for it and the planner needs the gate list.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.14.4 + Mockito; Testcontainers for live classes |
| Config file | `build.gradle.kts` (`tasks.withType<Test>`, `testUnit` task excludes tag `clickhouse`) |
| Quick run command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --offline testUnit` |
| Full suite command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` (add `--rerun` for a second run) |
| Compile gate (D-03) | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SYNC-07 | provenance + anchors + marker scan | script | `bash .planning/scripts/verify-merge-invariants.sh <M> HEAD` (exit 0) | parameterised version Wave 0 |
| SYNC-08 | checklist rows complete, no unreviewed hunk | grep over the checklist | `grep -c '| pending |' .planning/MERGE-CHECKLIST-v1.7.0.md` equals the planned count | Wave 0 |
| SYNC-09 | fork rows survive | unit + live | `./gradlew testUnit` then the live classes in the matrix above | existing |
| SYNC-10 | upstream tests unmodified and green | git + live | `git diff --quiet v1.7.0 HEAD -- <11 paths>`; `./gradlew test --tests '*ClickHouseHelperClientTest' --tests '*ClickHouseSinkTaskWithSchemaTest' ...` | existing |
| CMP-01 | 3 builder sites threaded | script anchors | anchor rows (`new ...Builder(` = `.setCompressionMethod(csc...)` = 3 in `src/main`) | Wave 0 |
| CMP-02 | compress/lz4 pins | unit (Docker-free) | `./gradlew testUnit --tests '*NetworkCompressionPinUnitTest' --tests '*NewUpstreamConfigDefaultsUnitTest'` | extend existing |
| CMP-03 | flag true/false live round trip | live | `./gradlew test --tests '*CompressionFlagLiveTest'` | new, Wave 0 |
| WRT-06 | #820 bytes | unit (Docker-free) + live | `./gradlew testUnit --tests '*NullOptionalMapBytesTest' --tests '*NestedNullableCompositeWriteTest'`; live `*nullMapTypeTest` | new + existing |
| WRT-09 | CR-02 pins | unit | same as above; plus a Recorded Fact that 0 assertions changed | existing |
| BLD-09 | one LZ4 rule/version | build output | `dependencyInsight` checks from Pitfall 5 + `grep -c '1\.10\.2'` = 0 in 4 files | none needed |
| BLD-13 | catalog state | grep | `! grep -q clickhouse-jdbc gradle/libs.versions.toml build.gradle.kts`; `grep -q 'kafka = "4.1.1"' gradle/libs.versions.toml`; `! grep -q 'FORK OVERRIDE (upstream 0.9.5)' gradle/libs.versions.toml` | none needed |
| EOS-01 | restart read | live + mutation | `./gradlew test --tests '*KeeperStateProvider*'`; mutation procedure above | new, Wave 0 |

### Sampling Rate
- **Per task commit:** `testUnit` (about 333 tests, Docker-free) plus the four compile tasks.
- **Per wave merge:** the live classes touched by that wave.
- **Phase gate:** full `test` on `latest` x V2 once, failures 0, skips equal to FORK-DELTA's 18 named skips plus the integrationTest sentence; then the invariants script. The two-run `--rerun` gate is Phase 15's SUITE-04, not this phase's.

### Wave 0 Gaps
- [ ] Parameterised `verify-merge-invariants.sh` with `PRE_TAG`, `UPSTREAM_REF`, `ANCHORS_FILE`, provenance and anchor steps
- [ ] `fork-anchors.txt`, `conflicted-paths-v1.7.0.txt`, `MERGE-CHECKLIST-v1.7.0.md`
- [ ] New test classes: #820 byte pins, CMP-03 live, EOS-01 live

## Security Domain

`security_enforcement` is enabled (absent = enabled; config shows `true`, `security_asvs_level: 1`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (no auth code changed) | — |
| V5 Input Validation | partly | `ZKPathValidator` already rejects non-`/` paths; new tests must use generated, not user, identifiers |
| V6 Cryptography | no | TLS/compression settings only; no hand-rolled crypto |
| V8 Data Protection | yes | Do not write passwords or record payloads into Recorded Facts, test output or commits; `KeeperStateProvider` already masks the password in its log line |
| V14 Configuration / dependencies | yes | Take only upstream's pinned `lz4-java 1.10.4`; no new dependency; further `lz4-java` bump is Phase 14 (milestone locked decision) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Merge silently re-introduces a known-vulnerable transitive (`lz4-java 1.10.2` pinned by the fork rule) | Tampering/Elevation | Delete the fork rule; prove with `dependencyInsight` |
| Probe or Recorded Fact leaks credentials (`test_password`, connection strings) | Information disclosure | Scrub error text before recording; the probe is never committed |
| SQL built by string interpolation in `KeeperStateProvider` (`'%s'` key, table name) | Tampering | Upstream's code, **out of scope** (REQUIREMENTS Out of Scope); new tests use generated identifiers only |
| Operator sets `compress=true` in `jdbcConnectionProperties` | Denial of service (connector breaks on 26.9+) | Pin and document the caveat (Pattern 3, ROLL-01); no code change |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The `X-ClickHouse-Summary` `result_rows` header is why the old summary-count read fails on unbuffered responses; the mutation check therefore goes red | EOS-01 mutation check | D-16's hunt branch is taken instead; at worst a blocking checkpoint |
| A2 | V2 `getConfiguration().get("compress")` returns the string `"false"` after `compressServerResponse(false)` | Pattern 3 | First run of the new unit test fails on the literal; fix the expected value, not the code |
| A3 | For V1, a `?compress=true` URL parameter versus the explicit node-options `compress=false`: precedence unknown | Live experiment | The V1 stripped case may not strip; the protocol makes that "no observation" |
| A4 | ClickHouse forbids `Nullable(Map)`, so `isNullable()` is never true for server-derived MAP columns (the repo's own comment at `Column.java:386` says so) | Pitfall 2 | If wrong, the scoped guard is reachable from real tables and the WRT-09 reasoning gets richer, not wrong |
| A5 | Expected bytes `00` / `00 00` (after) and `00 01` (before) come from the milestone byte probe, not re-run this session (the merged tree does not exist yet) | Code Examples | RED/GREEN run on M and after the guard settles it |
| A6 | `testUnit` reports 333 tests at M (baseline 331 + 2 `StructToJsonMapTest` methods) | SYNC-10 inventory | A different count prompts a look at which class appeared or vanished |
| A7 | `latest` may be re-pulled to a newer build at test time (always-pull policy) | Environment | Record `chc.version()` with every live result |
| A8 | The Package Legitimacy seam covers npm/PyPI/crates only, so Maven coordinates are out of its scope | Standard Stack | None for this phase: no package is added |

## Open Questions

1. **Does a "re-pinned CR-02 assertion" exist at all?**
   - What we know: the one MAP pin uses a nullable column and is unaffected; no other test pins pre-#820 MAP null bytes.
   - What's unclear: whether the user expects visible changes to `NestedNullableCompositeWriteTest`.
   - Recommendation: plan WRT-09 as evidence plus added non-nullable cases; if the first run proves otherwise, adapt.

2. **Where should the V1 `chcTmp` threading be proven behaviourally?**
   - What we know: it cannot be inspected from outside, and `compress=false` is applied on its node regardless of the flag.
   - What's unclear: whether a query-log probe of `network_compression_method` on the INSERT is worth it.
   - Recommendation: rely on the builder-site count invariant and a V1 live case; skip the query-log probe.

3. **Can the `integrationTest` source set's #800 and cluster tests be run for real in Phase 12?**
   - What we know: they need a Confluent platform stack; `compileIntegrationTestJava` is the existing gate (HARN-06).
   - Recommendation: name the skip in FORK-DELTA; do not attempt a Confluent run.

## Sources

### Primary (HIGH confidence)
- Repository, read this session: `ClickHouseHelperClient.java` (current), upstream helper via `git show v1.7.0:`, `ClickHouseWriter.java` (current and merge-tree result), `KeeperStateProvider.java` (current and `v1.7.0`), `ClickHouseSinkConfig.java`, `Column.java`, `StateRecord.java`, `RangeContainer.java`, `State.java`, `ClickHouseBase.java`, `NetworkCompressionPin{Unit,Live}Test.java`, `NestedNullableCompositeWriteTest.java`, `NewUpstreamConfigDefaultsUnitTest.java`, `AsyncInsertLiveTest.java`, `verify-merge-invariants.sh`, `docs/FORK-DELTA.md`, `.planning/MERGE-CHECKLIST.md`, `build.gradle.kts`, `gradle/libs.versions.toml`
- `git merge-tree --write-tree --name-only`, `git diff --name-only`, `git rev-parse`, `git for-each-ref` outputs this session
- Gradle: `dependencyInsight`, `testUnit`, `spotlessCheck` (baseline, `--offline`)
- Cached 0.9.6 jars, probed with throwaway classes in `/tmp/jp` (not in the repo): option keys and defaults
- Docker daemon API over the colima socket: version, image list, image label `com.clickhouse.build.version=26.9.12.8`
- Milestone research reused, not redone: `.planning/research/{SUMMARY,ARCHITECTURE,PITFALLS,STACK,FEATURES}.md`
- Upstream `v1.7.0` `CHANGELOG.md` (via `git show v1.7.0:CHANGELOG.md`)

### Secondary (MEDIUM confidence)
- https://clickhouse.com/docs/interfaces/http (WebFetch): `X-ClickHouse-Summary` fields including `result_rows`; `wait_end_of_query=1` delays header transmission; `send_progress_in_http_headers` can interfere

### Tertiary (LOW confidence)
- None used as a basis for a recommendation; the `network_compression_method` ZSTD default on 26.9 is the fork's own FORK-DELTA row 36 and is re-observed, not assumed, by the S0-S3 probe.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing new is installed and every artifact was found locally
- Architecture / merge mechanics: HIGH, conflict set measured by dry run, hunks read, script prototype passed against a real historical merge
- Pitfalls: HIGH for Findings 1-3 (read in code); MEDIUM for the EOS mutation outcome and the V1 `?compress=true` precedence (cannot be known before running)

**Research date:** 2026-10-07
**Valid until:** until `custom_build` moves or `v1.7.0` is re-pointed (the conflict set is a function of those two refs); the local `latest` image, the one volatile input, can change at any test run.
