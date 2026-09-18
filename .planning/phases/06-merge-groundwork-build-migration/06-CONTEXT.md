# Phase 6: Merge Groundwork & Build Migration - Context

**Gathered:** 2026-09-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Get the upstream merge underway on a disposable branch with a build that parses and resolves.

Delivers: upstream wired as a named git remote; a `pre-v1.6.0-sync` tag naming the pre-merge tip of `custom_build`; the scratch branch `sync/v1.6.0`; a checked-in per-file conflict-resolution checklist covering all 40 `git merge-tree` messages across 39 paths; the SYNC-04 client-version fact and the SYNC-05 production-config audit on the record; and upstream's version-catalog `build.gradle.kts` + `gradle/libs.versions.toml` adopted with all five fork build additions re-applied, wrapper at `9.6.1`, `VERSION` at `v1.6.0_gg1`.

Gate: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` and `./gradlew dependencies` both succeed. **No compilation is attempted in this phase** — `src/main` does not compile until Phase 7, by design.

Out of this phase: any semantic re-application of fork behaviour (Phases 7-10), any test execution, and publication (Phase 11).

</domain>

<decisions>
## Implementation Decisions

### Merge Commit Shape

- **D-01:** The merge is landed as **two commits**, not one.
  - **Commit 1 (the merge):** 100% mechanical. Every conflicted path resolves to **upstream `v1.6.0`'s exact file** — stage-3 `--theirs`, *not* `-X theirs` hunk-merging. The 5 `modify/delete` conflicts resolve as **deletions**. Auto-merged paths stay exactly as git produced them (this is how the free `ServerException` fix in `Utils.java` arrives). Fork-only files (the 4 SMTs, the fork's test files) are added untouched. Including `build.gradle.kts`, `VERSION` and the wrapper — commit 1 carries upstream's versions of those too.
  - **Commit 2 (ordinary commit):** re-applies the five fork build additions, sets `VERSION` to `v1.6.0_gg1`, and settles `.gitignore` / `tests.yaml`.
  - The Phase 6 gate (`gradlew help` + `dependencies`) runs on **commit 2**.
  - **Reversibility:** costly — `git revert -m 1` on commit 1 stays viable and is the designed rollback, but once Phases 7-10 commit on top, reverting means re-doing the whole merge. The `pre-v1.6.0-sync` tag plus the scratch branch are what make it cheap.

- **D-02:** Because commit 1 is purely mechanical, it carries a **script-verifiable invariant**: `git diff v1.6.0 <merge-sha> -- <every conflicted path>` is empty. This is the property that makes Phases 7-10 legible — each fork behaviour they re-apply shows up as its own reviewable diff against upstream, which is exactly the record SUITE-03 has to produce in Phase 11.

- **D-03:** `.github/workflows/tests.yaml` resolves to the **fork's 55-line single-job workflow, verbatim**, in commit 2. Upstream's 245-line version (3-D matrix × cloud × cluster jobs) is dropped — it needs credentials and a cluster deployment the fork does not have, and the fork's action pins are already ahead of upstream's in every case. Not touched again until Phase 11 (CI-05 wall-clock against `timeout-minutes: 45`).

- **D-04:** `.gitignore` resolves as a **union of both sides** — the diff is 2+/3-, and there is no reason to lose either the fork's entries or upstream's.

- **D-05:** `sync/v1.6.0` **stays local** through Phases 6-10. It is never pushed to `origin` (which is the public fork). `tests.yaml` is `on: [push]` unqualified, so pushing the scratch branch would fire CI on every push against a tree that cannot compile until Phase 7 — up to 45 minutes of runner time per push, red by design, for five phases. Phase 11 pushes `custom_build` once, which is the only SHA CI-06 needs to verify.
  - **Reversibility:** reversible — the branch can be pushed at any point if off-machine backup becomes a concern.

### Conflict Checklist Artifact

- **D-06:** SYNC-03's checklist lives at **`.planning/MERGE-CHECKLIST.md`** — milestone level, beside `ROADMAP.md` and `REQUIREMENTS.md`, because Phases 7-10 each tick off their own rows. Archived with the milestone at ship time.

- **D-07:** The checklist has **two sections**:
  - **Section 1 — Conflicted (39 paths):** `path | conflict type | resolution shape (survives-as-is / re-express / redundant) | owning phase | fork behaviour at risk | adjudicating test | status`.
  - **Section 2 — Merged clean, READ ANYWAY (~7 paths):** the files that merge with **no conflict marker at all** yet carry fork logic — `Utils.java`, `Processing.java`, `Record.java`, `Data.java`, `StructToJsonMap.java`, the record convertors, `state/provider`. This is research Pitfall 5: a reviewer who reads only the conflict list never opens them, and misses that `UtilsTest`/`ProcessingTest` *do* conflict and need upstream's versions wholesale. ORCH-04 makes this a hard requirement in Phase 8; the checklist is where it is not forgotten.

- **D-08:** The checklist is a **living document with a status column**. Phase 6 writes every row as `pending` with its intended shape and owning phase; each of Phases 7-10 flips its own rows to `done` inside that phase's own commits; **Phase 11 asserts zero `pending` rows before publication**. That turns the checklist into a gate rather than a document, and feeds SUITE-03's written record directly.

- **D-09:** A **committed script** — `.planning/scripts/verify-merge-invariants.sh` — asserts both machine-checkable invariants: (1) every conflicted path is byte-identical to `v1.6.0` at the merge commit, and (2) no revision in `pre-v1.6.0-sync..HEAD` contains a literal `<<<<<<<` in tracked source. Written and run in Phase 6 against one commit; **re-run in Phase 11 across the whole range**, which is where SYNC-06 actually has to hold. The conflicted-path list is committed alongside it rather than re-derived from prose.

### Open Facts (SYNC-04 / SYNC-05)

- **D-10:** **SYNC-05 is closed on both halves, no operator follow-up needed.**
  - `bufferCount > 0` + `exactlyOnce = true`: **structurally unreachable at upgrade.** `bufferCount` does not exist in `v1.2.9_gg4` at all — it is new in `v1.6.0` (`ClickHouseSinkConfig.java:56`, default `0`). No pre-merge connector config can set a key that did not exist, so upstream's new startup rejection cannot fire on an upgraded config. Derived from the repo, not asked.
  - `enableDbTopicSplit = true` + `exactlyOnce = true`: **not applicable.** Operator confirmed on 2026-09-18 that this functionality is not used in production. Both keys exist in the fork today (`ClickHouseSinkConfig.java:47` and `:33`, both defaulting to `false`), but neither is enabled, so upstream's documented one-time dedup gap on upgrade does not apply.
  - Both facts are recorded in `.planning/MERGE-CHECKLIST.md` under a **`## Recorded Facts`** section, with their source (repo-derived vs operator-confirmed) stated.

- **D-11:** SYNC-04 asserts the **resolved** version, not the declared one. The Phase 6 gate already runs `./gradlew dependencies`; the assertion reads that output. This catches the real failure mode — a transitive constraint from upstream's new protobuf / schema-registry dependencies quietly resolving a different version than the catalog declares.

- **D-12:** The resolved-version assertion covers **all four fork-only build decisions** a wholesale catalog adoption would silently erase:
  - `com.clickhouse:*` (all five modules) → `0.9.6` (upstream catalog says `0.9.5`)
  - `org.apache.kafka:connect-api` → `4.1.1` (upstream catalog says `2.7.0`)
  - `org.slf4j:slf4j-reload4j` → `2.0.17` (**no catalog entry exists upstream** — the single easiest thing to lose)
  - the `org.lz4:lz4-java` capability → `at.yawk.lz4:lz4-java:1.10.2` (BLD-06)

  Also recorded as a named fact: production runs the shaded jar built from this repo, therefore `0.9.6` — no separate runtime verification needed.

### Build-File Fidelity

- **D-13:** Of the fork's two optional carry-overs: **keep `addTestListener`** (`build.gradle.kts:201-220`), **drop `jdkHome`** (`:193-199`). The test-summary block earns its keep precisely during this milestone — Phases 7-11 are one long sequence of `testUnit` / `test` runs and a pass/fail/skip line per run is the signal needed. `jdkHome` is superseded by `javaLauncher` + the toolchain and has zero references in the fork's docs, CI or requirements.

- **D-14:** Compile-side JDK: **keep the fork's `java { toolchain { languageVersion.set(17) } }`**. Upstream's `sourceCompatibility`/`targetCompatibility = VERSION_11` and `options.release.set(11)` are both dropped. Shipped bytecode stays at 17, unchanged from today — a sync milestone that is already architecturally discontinuous should not also move the artifact's runtime floor. This also pins the *compiler* to JDK 17, not merely the output level, which matters with JDK 21 on the local PATH.
  - Independent of this, BLD-05's `javaLauncher.set(javaToolchains.launcherFor { 17 })` in `tasks.withType<Test>` is **mandatory and non-negotiable** — it is the only mechanism pinning the test JVM to 17 regardless of the invoking JDK, and upstream deleted it.
  - **Reversibility:** costly — changing the bytecode target after publication changes the connector's minimum runtime JVM for every deployment.

- **D-15:** Fork version overrides live as **two marked lines in `gradle/libs.versions.toml`**:
  ```toml
  clickHouseDriver = "0.9.6"  # FORK OVERRIDE (upstream 0.9.5)
  kafka            = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)
  ```
  Every other catalog key stays byte-identical to upstream. `kafka` is referenced by exactly one library entry and `clickHouseDriver` by five, so this is a genuine 2-line change. The next upstream sync will conflict on exactly those two lines — and that conflict is the wanted signal that upstream finally moved.

- **D-16:** Everything **fork-only** stays declared **inline in `build.gradle.kts`** and never enters the catalog: `slf4j-reload4j` as a literal coordinate (it has no upstream catalog entry), the `com.github.ben-manes.versions` plugin with its inline version, the core `jacoco` plugin (ships with Gradle, no version needed), and the LZ4 `resolutionStrategy` capability block. Rationale: `build.gradle.kts` is already a permanently fork-modified file, so these cost no new conflict surface — whereas a fork-only coordinate added to the catalog would conflict with upstream's constant catalog churn for no benefit.

### Claude's Discretion

The user delegated the SYNC-05 disposition ("decide yourself what's right") and did not constrain the following; these are the calls made, all reversible and all open to a planner overriding them with a reason:

- **Remote name for SYNC-01:** `upstream` (pointing at `ClickHouse/clickhouse-kafka-connect`). `origin` already points at the fork `ne1r0n/clickhouse-kafka-connect`. Note the `v1.6.0` tag is **already present locally**, so the remote is being added for future syncs and provenance, not because the merge needs a fetch.
- **Working tree must be clean before tagging.** There is currently an uncommitted `.gitignore` edit (adds `.gsd`). It must be committed onto `custom_build` *before* `pre-v1.6.0-sync` is created, so the tag names a clean, reproducible tree. Its content then feeds D-04's union resolution.
- **`maxParallelForks`:** take upstream's cluster-aware version (`CLICKHOUSE_CLUSTER_NAME != null → 1`, else `processors/2`). Strictly additive over the fork's simple version and harmless when the env var is unset.
- **Upstream's new build tasks are kept as-is:** `runClickHouseVersionClientMatrix` (irrelevant to the fork's `latest` × V2 constraint but must compile, since it lives in the same file) and `generateVersionFile`. Nothing scoped in this milestone says to remove them, and dropping them would enlarge the fork delta the milestone is trying to shrink.
- **Note forward to Phase 8:** `generateVersionFile` writes `Version.ARTIFACT_VERSION` from `project.version`, which D-01/commit 2 sets to `v1.6.0_gg1`. Upstream's `SinkTaskStatistics` MBean name sources its `version=` segment from that constant rather than from the client jar's manifest. Phase 6 should **record the resulting string as a named fact** so Phase 8's JMX re-plumb inherits it rather than rediscovering it — this is an open gap flagged in `research/SUMMARY.md` § Gaps to address.
- **Spotless:** take upstream's catalog version `8.10.1` (fork has `8.1.0`) per BLD-08's "upstream's for everything not fork-pinned".

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone contract
- `.planning/ROADMAP.md` § "Phase 6: Merge Groundwork & Build Migration" — the five success criteria this phase is judged against
- `.planning/ROADMAP.md` § "Merge Mechanics (applies to every phase)" — scratch-branch rule, commit cadence, the `env -u TESTCONTAINERS_HOST_OVERRIDE` Gradle invocation, `testUnit` as the cheap gate
- `.planning/REQUIREMENTS.md` § "Locked decisions this milestone is built on" — the 14 decisions plans must not re-open
- `.planning/REQUIREMENTS.md` § "Inventory & Rollback (SYNC)" and § "Build Migration (BLD)" — SYNC-01..06 and BLD-01..08, this phase's 14 requirements
- `.planning/REQUIREMENTS.md` § "Out of Scope" — what must not be attempted

### Build migration (the primary source for this phase)
- `.planning/research/STACK.md` § 1 — structural diff of `build.gradle.kts`, `main` → `v1.6.0`, area by area
- `.planning/research/STACK.md` § 2 — dependency version deltas, catalog vs fork, with the verdict per dependency
- `.planning/research/STACK.md` § 3 — **the re-application recipe for all five fork build additions**, including the exact line numbers in the fork's current file and the fact that upstream deleted the *mechanism* the JDK-17 pin depends on, not just the number
- `.planning/research/STACK.md` § 4 — Gradle wrapper `8.14` → `9.6.1`, and why the new build file should not be run under the old wrapper
- `.planning/research/STACK.md` § 5 — CI survival analysis; why the fork's `tests.yaml` needs no rework
- `.planning/research/STACK.md` § 6 — the 15-class `jacocoForkReport` list audited against `v1.6.0`: **zero edits needed**, and `.github/scripts/jacoco_summary.py` needs none either
- `.planning/research/STACK.md` § 7 — the four stack-level open decisions, all now settled in `REQUIREMENTS.md`'s locked table

### Merge mechanics and risk
- `.planning/research/SUMMARY.md` § Executive Summary — why this is architecturally discontinuous, not merely large
- `.planning/research/SUMMARY.md` § "Must-not-silently-lose list" — the five fork behaviours that vanish with no conflict marker and no compile error, each with its canary test. Section 1 of the checklist must reflect this table
- `.planning/research/SUMMARY.md` § "Hard numbers for sizing" — 581 commits, 40 messages / 39 paths, 9 of 12 named collision files are phantom (Spotless-only), work concentrates in 5 files
- `.planning/research/SUMMARY.md` § "Critical Pitfalls" — Pitfall 5 is the direct source of the checklist's Section 2
- `.planning/research/PITFALLS.md` — 17 pitfalls with symptom → cause → prevention → detection → owning phase
- `.planning/research/ARCHITECTURE.md` — per-collision-file resolution shapes; the source for Section 1's "shape" column
- `.planning/research/FEATURES.md` — per-feature disposition for all eight fork features

### Environment and process
- `.planning/STATE.md` § "Blockers/Concerns" — the `TESTCONTAINERS_HOST_OVERRIDE="null"` hazard, colima profile `vz`, and `gh` CLI deliberately absent (D-11 of the prior milestone) so CI is checked through the unauthenticated GitHub REST API
- `.planning/RETROSPECTIVE.md` — carried-forward process hazards
- `.planning/config.json` — `build_command` / `test_command` already carry the `env -u` prefix; `granularity: coarse`, `mode: yolo`

### Files this phase touches directly
- `build.gradle.kts` — the fork's current file; §3 of STACK.md cites its exact line numbers
- `gradle/libs.versions.toml` — does not exist in the fork yet; arrives from upstream in commit 1
- `gradle/wrapper/gradle-wrapper.properties` — `8.14` → `9.6.1`
- `VERSION` — `v1.2.9_gg4` → `v1.6.0_gg1`
- `.github/workflows/tests.yaml` — fork's version survives; `on: [push]` is unqualified (line 8)
- `.github/scripts/jacoco_summary.py` — needs no edits; its two hardcoded XML paths match the re-applied tasks

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **The fork's `build.gradle.kts` blocks are copy-forward, not rewrite.** JaCoCo (`:37`, `:237-239`, `:241-276`, `:278-300`, `:302-310`) transplants verbatim — upstream has no `jacoco` plugin, so there is nothing to merge against. `testUnit` (`:226-235`) plus the `if (name != "testUnit")` check guard (`:176-178`) likewise.
- **`.github/scripts/jacoco_summary.py` is untouched.** Its `FORK_CLASSES_IN_ORDER` list mirrors `forkChangedClasses`, and all 15 paths resolve post-merge (5 are fork-only transform classes that return with the merge itself; `MBeanServerUtils.java` is byte-identical to `main` at `v1.6.0`).
- **The fork's `tests.yaml` has zero coupling to the build file's internals** — it only shells out to `./gradlew test`, so it survives the migration untouched.

### Established Patterns

- **`tasks.named<Test>("test")` still resolves under `jvm-test-suite`.** Upstream's `testing { suites { val test by getting(...) } }` adopts the pre-existing `test` source set rather than creating one, so `sourceSets["test"]` and the conventionally-named `test` / `integrationTest` tasks the JaCoCo blocks reference are unchanged.
- **Upstream's `tasks.withType<Test>` keeps** `maxParallelForks` (with a new cluster-aware branch), `testLogging`, the `javaVersion` property read, and `com.clickhouse.test.uri`. It **drops** `useJUnitPlatform()` (moved into the suite DSL as `useJUnitJupiter()`), `file.encoding`, `javaLauncher`, `jdkHome`, `addTestListener`, and any `check.dependsOn`. Only the first is replaced by an upstream equivalent; the rest are fork additions to re-insert.
- **`signing` and `buildconfig` are provably dead** in fork and upstream both — zero references anywhere in `src/`. Dropping them is a clean loss, not a regression.
- **The `dependencyUpdates` config block references a plugin-supplied Kotlin type** (`com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask`), so omitting the ben-manes plugin ID fails the **build-script compile**, not just behaviour. It is self-detecting.

### Integration Points

- **`git merge-tree --write-tree custom_build v1.6.0`** is the source of truth for Section 1 of the checklist. Re-verified at discussion time: **40 messages — 35 `content` + 5 `modify/delete` — across 39 unique paths**, matching the research figure exactly. Do not hand-transcribe; generate the skeleton from this command.
- **The conflict surface spans four areas:** 6 build/infra paths (`build.gradle.kts`, `gradle-wrapper.properties`, `VERSION`, `.gitignore`, `tests.yaml`, plus `libs.versions.toml` arriving new), 9 `src/main` paths, ~14 `src/test` paths, 6 `src/integrationTest` paths.
- **The 5 `modify/delete` conflicts all resolve as deletions under D-01**, and that is the *wanted* outcome: `ClickHouseTestHelpers.java`'s orphaned fork copy, and the `src/integrationTest` duplicates of `SchemaTestData` / `SchemalessTestData` / `ClickHouseAPI` — exactly what HARN-04 and HARN-05 require in Phase 10.
- **The `protobuf` plugin downloads a native `protoc` binary at configure time.** The first `./gradlew` invocation after commit 1 needs Maven Central reachable for a non-jar artifact. Worth knowing if `gradlew help` behaves oddly on the first run.

</code_context>

<specifics>
## Specific Ideas

- **"Every conflicted path is byte-identical to `v1.6.0`" is the organizing idea of this phase.** It was chosen over more surgical alternatives specifically because it is a one-line assertion anyone can re-run, and because it makes every subsequent phase's work a clean, reviewable diff against upstream rather than a judgement call buried in a 500-file merge.
- **Conflicts on the two catalog override lines at the next sync are wanted, not avoided.** The explicit reasoning: a silent `resolutionStrategy.force` would keep the fork's version forever without ever telling anyone upstream had moved. A two-line conflict is the notification mechanism.
- **The `addTestListener` block was kept for a milestone-specific reason**, not for general parity: Phases 7-11 are a long run of `testUnit` / `test` invocations and its summary line is the fastest read of each.
- The `v1.6.0` tag and a local `main` branch are **already present** in this repo; `origin` is the fork, not upstream.

</specifics>

<deferred>
## Deferred Ideas

- **A `branches-ignore: ['sync/**']` filter on `tests.yaml`** — considered as a way to push the scratch branch for backup without firing red CI. Not adopted (D-05 keeps the branch local instead). If off-machine backup ever becomes a concern mid-milestone, this is the cheap enabler.
- **Generalising `verify-merge-invariants.sh` into `.github/scripts/`** as permanent tooling for future upstream syncs. It is milestone-specific today (hardcoded `pre-v1.6.0-sync` / `v1.6.0` refs); promoting it is real work this milestone did not scope.
- **An explicit at-defaults inventory of the thirteen new upstream config options** recorded in Phase 6 rather than Phase 7. Left with CFG-03 in Phase 7 where it belongs.
- **`retryOnSocketException`** — `REQUIREMENTS.md` § Future Requirements calls it "worth deliberately enabling", since it targets a client-V2 persistent-connection failure mode the fork is exposed to today with no mitigation. Adopting it is feature work, explicitly out of scope; CFG-03 keeps it at its upstream default.
- **Per-phase tags on the scratch branch** for finer rollback than `pre-v1.6.0-sync`. Not pursued — `git revert -m 1` plus the single tag was judged sufficient.

</deferred>

---

*Phase: 6-Merge Groundwork & Build Migration*
*Context gathered: 2026-09-18*
