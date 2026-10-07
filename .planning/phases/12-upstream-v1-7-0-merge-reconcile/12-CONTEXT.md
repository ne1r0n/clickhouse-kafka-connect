# Phase 12: Upstream v1.7.0 Merge & Reconcile - Context

**Gathered:** 2026-10-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Upstream `v1.7.0` (`379b8d01`, addressed by tag, never bare `upstream/main`) is merged onto a local scratch branch as one mechanical merge commit, with every fork feature in `docs/FORK-DELTA.md` proven intact by its existing pin test. Three hazards that raise no conflict marker are resolved by intent:

1. D-17 is retired in favour of upstream's `setCompressionMethod`, which also sets `compress=false` unconditionally.
2. Upstream's #820 null-Map fix is made reachable. The fork's MAP null guard is scoped to nullable columns, and the CR-02 pins are re-pinned to the correct bytes.
3. The build is reduced to one LZ4 rule, and the catalog is cleaned (`clickhouse-jdbc` and the `# FORK OVERRIDE (upstream 0.9.5)` comment are removed; `kafka = "4.1.1"` stays).

Upstream's exactly-once state-read change in `KeeperStateProvider` is proven across a provider restart. `custom_build` and `origin` are not touched. `c61d90ce` stays one tag away (`pre-v1.7.0-sync`).

Requirements: SYNC-07, SYNC-08, SYNC-09, SYNC-10, CMP-01, CMP-02, CMP-03, WRT-06, WRT-09, BLD-09, BLD-13, EOS-01.

**Not this phase:**
- R-01 routing, WR-01/WR-02 nested-null handling, P9-IN-01, the metadata/config fixes and the test debt belong to Phase 13. That includes DEBT-05's Javadoc rewrite of the `NetworkCompressionPin*` rationale.
- Dependency bumps beyond upstream's own `lz4-java 1.10.4`, and all CI workflow changes, belong to Phase 14.
- The `VERSION` bump, the CHANGELOG, publication and the rollout checklist belong to Phase 15.

</domain>

<decisions>
## Implementation Decisions

### Branch & commit shape

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

### Merge checklist & invariant script

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

### Compression evidence (CMP-01..03)

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

### EOS-01 restart test

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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope & locked decisions
- `.planning/ROADMAP.md` § "Phase 12: Upstream v1.7.0 Merge & Reconcile" — goal, 5 success criteria, research flag (EOS-01 mutation check, live compression experiment); § "Sync Mechanics" — scratch-branch, tag-not-`upstream/main`, commit cadence, `env -u TESTCONTAINERS_HOST_OVERRIDE`, gates, push rules, ROADMAP editing rule
- `.planning/REQUIREMENTS.md` — locked-decisions table (top) and the 12 requirement texts for this phase
- `.planning/PROJECT.md` — current state, retired/kept fork features

### Research (merge simulation, hunk tables, pitfalls)
- `.planning/research/SUMMARY.md` — executive summary, three silent hazards
- `.planning/research/ARCHITECTURE.md` — per-file conflict/hunk tables for `ClickHouseHelperClient`, `ClickHouseSinkConfig`, `ClickHouseWriter`; `KeeperStateProvider` row; builder setter textual trap
- `.planning/research/PITFALLS.md` — Pitfall 1 (MAP shadow), 2 (duplicate LZ4 rule), 3 (`compress=false` is load-bearing), 4 (`setCompressionMethod` builder sites), 7 (KeeperStateProvider read), 9 (green build ≠ fork survived)
- `.planning/research/FEATURES.md` — D-17 retirement table, #841 behaviour change
- `.planning/research/STACK.md` — lz4 1.10.4, catalog changes

### Fork delta & prior merge record
- `docs/FORK-DELTA.md` — every fork feature + its pin test (SYNC-09's list); D-17 row (line 36), LZ4 build row (line 49), dependency-pin row (line 51), named skip inventory (≈line 146–160)
- `.planning/MERGE-CHECKLIST.md` — v1.6.0 checklist; Recorded Facts 1–16 cited by FORK-DELTA; layout template for the v1.7.0 sibling
- `.planning/scripts/verify-merge-invariants.sh`, `.planning/scripts/conflicted-paths.txt` — the script to parameterise
- `.planning/milestones/v1.6.0_gg1-sync-phases/06-merge-groundwork-build-migration/06-CONTEXT.md` — v1.6.0 merge decisions (D-01..D-16) this phase mirrors

### Project rules
- `AGENTS.md` — Key Files (high risk: `KeeperStateProvider`, `ClickHouseWriter`, `ClickHouseSinkConfig`), never delete/weaken tests, read-back `assertEquals`
- `.claude/CLAUDE.md` — "1.10.2" wording to fix (BLD-09)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ClickHouseBase` (`src/test/.../sink/ClickHouseBase.java`): shared container per JVM fork; it already wires `clickhouse_keeper_map.xml`, so a standalone `KeeperStateProvider` live test needs no cluster.
- `NetworkCompressionPinUnitTest` / `NetworkCompressionPinLiveTest` (`src/test/.../sink/db/helper/`): they assert through `getClient().getConfiguration()` and `getDefaultClientOptions()`, not constant names, so they survive D-17's retirement and host CMP-02/03.
- `NestedNullableCompositeWriteTest` (`src/test/.../sink/db/`): the three CR-02 Docker-free byte pins (MAP/TUPLE/VARIANT). The MAP case is the WRT-09 re-pin target.
- `NewUpstreamConfigDefaultsUnitTest`, `ClickHouseVersionDefaultUnitTest`: canaries to run at the merge SHA.
- `ClickHouseTestHelpers.createClient` (`src/testFixtures/...:434`): a 4th builder site, outside `src/main`. Upstream's auto-merge adds `.setCompressionMethod(...)` to it.

### Established Patterns
- Merge-then-reconcile with a living checklist and a committed invariant script (v1.6.0 Phase 6 D-01..D-09).
- Docker-free pins go in `testUnit` (tests without `@Tag("clickhouse")`). Live tests extend `ClickHouseBase`.
- Recorded Facts state their source (repo-derived vs operator-confirmed vs observed run).

### Integration Points
- Dry `merge-tree custom_build v1.7.0` conflicts in exactly 5 files: `VERSION`, `gradle/libs.versions.toml`, `ClickHouseSinkConfig.java`, `ClickHouseWriter.java` and `ClickHouseHelperClient.java`.
- Auto-merged but fork-touched: `build.gradle.kts` (two LZ4 rules side by side: the fork's at `:134-138` and upstream's group-based one), `StructToJsonMap.java` and `ClickHouseTestHelpers.java`.
- `KeeperStateProvider.java` has no fork edits and arrives byte-identical to `v1.7.0`. Its builder gains `.setCompressionMethod(...)`, and `getStateRecord` counts `response.stream()` rows.

</code_context>

<specifics>
## Specific Ideas

- The merge commit keeps the line-provenance property so that every fork re-application is its own reviewable diff on top. This is the same reason as v1.6.0's D-02, with a weaker invariant because conflicts are now resolved hunk by hunk rather than wholesale `--theirs`.
- Two blocking-checkpoint triggers are pre-agreed (D-13, D-16). Both exist because a silent workaround would change what a locked requirement means.

</specifics>

<deferred>
## Deferred Ideas

None. The discussion stayed within the phase's scope. The WRT-06/09 re-pin scoping and the line between Phase 12's MAP guard and Phase 13's WR-01/WR-02 were offered as extra gray areas and not taken up. The planner follows the roadmap split: Phase 12 scopes the top-level MAP guard and re-pins CR-02. Nested-slot nulls belong to Phase 13.

</deferred>

---

*Phase: 12-upstream-v1-7-0-merge-reconcile*
*Context gathered: 2026-10-07*
