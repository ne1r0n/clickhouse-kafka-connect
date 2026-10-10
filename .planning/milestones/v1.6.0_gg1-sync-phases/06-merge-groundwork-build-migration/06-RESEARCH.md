# Phase 6: Merge Groundwork & Build Migration - Research

**Researched:** 2026-09-18
**Domain:** Git merge mechanics (upstream inventory/rollback) + Gradle 9.6.1 build-file migration (version catalog, `jvm-test-suite`, `java-test-fixtures`)
**Confidence:** HIGH — nearly every claim below was re-derived this session directly from `git show`/`git diff`/`git merge-tree`/`git log` against this exact repo (not recalled from the existing `.planning/research/*.md` reports, though those agree). A small number of Gradle-9-ecosystem claims are `[CITED]`/`[ASSUMED]` and are called out explicitly.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Merge Commit Shape**

- **D-01:** The merge is landed as **two commits**, not one.
  - **Commit 1 (the merge):** 100% mechanical. Every conflicted path resolves to **upstream `v1.6.0`'s exact file** — stage-3 `--theirs`, *not* `-X theirs` hunk-merging. The 5 `modify/delete` conflicts resolve as **deletions**. Auto-merged paths stay exactly as git produced them (this is how the free `ServerException` fix in `Utils.java` arrives). Fork-only files (the 4 SMTs, the fork's test files) are added untouched. Including `build.gradle.kts`, `VERSION` and the wrapper — commit 1 carries upstream's versions of those too.
  - **Commit 2 (ordinary commit):** re-applies the five fork build additions, sets `VERSION` to `v1.6.0_gg1`, and settles `.gitignore` / `tests.yaml`.
  - The Phase 6 gate (`gradlew help` + `dependencies`) runs on **commit 2**.
  - **Reversibility:** costly — `git revert -m 1` on commit 1 stays viable and is the designed rollback, but once Phases 7-10 commit on top, reverting means re-doing the whole merge. The `pre-v1.6.0-sync` tag plus the scratch branch are what make it cheap.
- **D-02:** Because commit 1 is purely mechanical, it carries a **script-verifiable invariant**: `git diff v1.6.0 <merge-sha> -- <every conflicted path>` is empty.
- **D-03:** `.github/workflows/tests.yaml` resolves to the **fork's 55-line single-job workflow, verbatim**, in commit 2. Upstream's 245-line version (3-D matrix × cloud × cluster jobs) is dropped.
- **D-04:** `.gitignore` resolves as a **union of both sides**.
- **D-05:** `sync/v1.6.0` **stays local** through Phases 6-10. Never pushed to `origin` (the public fork). Reversible — can be pushed later for backup.

**Conflict Checklist Artifact**

- **D-06:** SYNC-03's checklist lives at **`.planning/MERGE-CHECKLIST.md`** — milestone level.
- **D-07:** Two sections: **Section 1 — Conflicted (39 paths)**: `path | conflict type | resolution shape (survives-as-is / re-express / redundant) | owning phase | fork behaviour at risk | adjudicating test | status`. **Section 2 — Merged clean, READ ANYWAY (~7 paths)**: `Utils.java`, `Processing.java`, `Record.java`, `Data.java`, `StructToJsonMap.java`, the record convertors, `state/provider`.
- **D-08:** The checklist is a **living document with a status column**. Phase 6 writes every row as `pending`; Phases 7-10 flip their own rows to `done`; Phase 11 asserts zero `pending` rows before publication.
- **D-09:** A **committed script** — `.planning/scripts/verify-merge-invariants.sh` — asserts (1) every conflicted path is byte-identical to `v1.6.0` at the merge commit, and (2) no revision in `pre-v1.6.0-sync..HEAD` contains a literal `<<<<<<<` in tracked source. Written and run in Phase 6 against one commit; re-run in Phase 11 across the whole range.

**Open Facts (SYNC-04 / SYNC-05)**

- **D-10:** SYNC-05 is closed on both halves, no operator follow-up needed. `bufferCount>0`+`exactlyOnce=true`: structurally unreachable at upgrade (`bufferCount` doesn't exist pre-merge). `enableDbTopicSplit=true`+`exactlyOnce=true`: not applicable — operator confirmed 2026-09-18 this functionality is not used in production. Both facts recorded in `.planning/MERGE-CHECKLIST.md` under `## Recorded Facts`, each labelled with its source (repo-derived vs operator-confirmed).
- **D-11:** SYNC-04 asserts the **resolved** version (from `./gradlew dependencies` output), not the declared catalog value.
- **D-12:** The resolved-version assertion covers all four fork-only build decisions: `com.clickhouse:*` → `0.9.6` (catalog says `0.9.5`), `org.apache.kafka:connect-api` → `4.1.1` (catalog says `2.7.0`), `org.slf4j:slf4j-reload4j` → `2.0.17` (no catalog entry exists upstream), the `org.lz4:lz4-java` capability → `at.yawk.lz4:lz4-java:1.10.2` (BLD-06).

**Build-File Fidelity**

- **D-13:** Keep `addTestListener`, drop `jdkHome`.
- **D-14:** Compile-side JDK: keep the fork's `java { toolchain { languageVersion.set(17) } }`. Drop upstream's `VERSION_11`/`options.release.set(11)`. BLD-05's `javaLauncher` for the *test* JVM is mandatory and independent of this decision.
- **D-15:** Fork version overrides live as **two marked lines in `gradle/libs.versions.toml`**: `clickHouseDriver = "0.9.6"  # FORK OVERRIDE (upstream 0.9.5)` and `kafka = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)`. Everything else in the catalog stays byte-identical to upstream.
- **D-16:** Everything fork-only stays declared **inline in `build.gradle.kts`**, never in the catalog: `slf4j-reload4j` literal coordinate, `com.github.ben-manes.versions` plugin, the core `jacoco` plugin, the LZ4 `resolutionStrategy` block.

### Claude's Discretion

- **Remote name for SYNC-01:** `upstream` (pointing at `ClickHouse/clickhouse-kafka-connect`). `origin` already points at the fork. The `v1.6.0` tag is already present locally — the remote is for future syncs/provenance, not because this merge needs a fetch.
- **Working tree must be clean before tagging.** The uncommitted `.gitignore` edit (adds `.gsd`) must be committed onto `custom_build` before `pre-v1.6.0-sync` is created.
- **`maxParallelForks`:** take upstream's cluster-aware version.
- **Upstream's new build tasks kept as-is:** `runClickHouseVersionClientMatrix`, `generateVersionFile`.
- **Note forward to Phase 8:** record the resulting `Version.ARTIFACT_VERSION` string (`v1.6.0_gg1`) as a named fact.
- **Spotless:** take upstream's catalog version `8.10.1` (fork has `8.1.0`).

### Deferred Ideas (OUT OF SCOPE)

- A `branches-ignore: ['sync/**']` filter on `tests.yaml` — not adopted.
- Generalising `verify-merge-invariants.sh` into `.github/scripts/` as permanent tooling — milestone-specific today.
- An explicit at-defaults inventory of the thirteen new upstream config options — left with CFG-03 in Phase 7.
- `retryOnSocketException` — feature work, explicitly out of scope.
- Per-phase tags on the scratch branch for finer rollback — not pursued.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SYNC-01 | Upstream configured as a named git remote | § Git Mechanics — exact command, `origin` already occupied by the fork |
| SYNC-02 | Pre-merge tip tagged, merge on scratch branch, no force-push to `custom_build` | § Git Mechanics — tag/branch commands, current HEAD, working-tree-clean prerequisite |
| SYNC-03 | Checked-in per-file resolution checklist covering all 40 `git merge-tree` messages / 39 paths | § Verified Conflict Inventory — the full 39-path table, generated this session, ready to seed `MERGE-CHECKLIST.md` |
| SYNC-04 | Resolved ClickHouse Java client version recorded and compared to production | § Standard Stack, § Verification Commands — `./gradlew dependencies` is the source of truth, not the catalog literal |
| SYNC-05 | Production-config audit for `enableDbTopicSplit`+`exactlyOnce` and `bufferCount`+`exactlyOnce` | § Verified In-Repo Facts — `ClickHouseSinkConfig.java` line-cited quotes for both flags on both sides of the merge |
| SYNC-06 | Single identifiable merge commit, no `<<<<<<<` markers, no interleaved `.planning/` churn | § Common Pitfalls (Pitfall 1, 5), § Code Examples (`verify-merge-invariants.sh` skeleton) |
| BLD-01 | `build.gradle.kts` + `libs.versions.toml` from upstream, `signing`/`buildconfig` dropped | § Standard Stack, § Architecture Patterns — plugin block diff, dead-plugin confirmation |
| BLD-02 | Gradle wrapper at `9.6.1` | § Standard Stack — wrapper file diff, JVM floor, first-run network requirement |
| BLD-03 | JaCoCo re-applied (`jacocoTestReport` + 15-class `jacocoForkReport`) | § Code Examples — verbatim fork blocks with confirmed line numbers, `jacoco_summary.py` compatibility |
| BLD-04 | `testUnit` re-applied, excluded from `check` | § Code Examples — verbatim block + the `if (name != "testUnit")` guard reasoning |
| BLD-05 | Test JVM pinned to JDK 17 via `javaLauncher`, `file.encoding=windows-1252` | § Code Examples, § Common Pitfalls (Pitfall 2) — upstream deleted the whole mechanism, not just the number |
| BLD-06 | LZ4 capability resolution + ben-manes `dependencyUpdates` survive | § Code Examples — verbatim blocks, compile-breaking risk if plugin ID dropped |
| BLD-07 | `VERSION` reads `v1.6.0_gg1` | § Git Mechanics — current value confirmed `v1.2.9_gg4` |
| BLD-08 | Dependency versions reconciled per the locked table | § Standard Stack — full delta table, each verdict sourced from a direct file read |
</phase_requirements>

## Summary

Phase 6 has no application-logic surface — it is 100% git mechanics and Gradle build-script mechanics, and every fact needed to execute it correctly can be (and was) pulled directly out of this repository rather than inferred. `git merge-tree --write-tree custom_build v1.6.0` was re-run this session and reproduces exactly what CONTEXT.md and the existing `.planning/research/*.md` reports already established: **40 conflict messages — 35 `content` + 5 `modify/delete` — across 39 unique paths**. That exact list (with paths and conflict types) is reproduced below so the planner does not have to re-derive it or hand-transcribe from prose.

The build-file migration is the one place with real technical risk, and it was de-risked this session by reading the actual `v1.6.0:build.gradle.kts` (320 lines) and the actual `custom_build:build.gradle.kts` (386 lines) in full, side by side. Upstream's file already demonstrates every Gradle 9.6.1 API shape the planner needs (the `testing { suites { ... } }` DSL, `java-test-fixtures`, the stripped-down `tasks.withType<Test>` with no `javaLauncher`/`jdkHome`/`addTestListener`/`useJUnitPlatform()`), so there is no version-compatibility guesswork left — the fork's five build additions transplant onto a build file that is already running successfully on that exact Gradle version upstream-side. The `jvm-test-suite` plugin block still carries `@Suppress("UnstableApiUsage")` in upstream's own file, confirming the DSL is still incubating in Gradle 9.6.1 even though it is what upstream ships production CI against.

**Primary recommendation:** Do the git-history work first (remote, tag, branch, clean working tree) exactly as D-01 through D-05 specify, execute the merge with `-X ours`-free stage-3 `--theirs` resolution per conflicted path (not `-X theirs`, which hunk-merges instead of taking the whole file), commit that as the 100% mechanical commit 1, then hand-port the five build additions from the verbatim blocks captured in this document onto upstream's file for commit 2, and gate on `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` + `./gradlew dependencies` — no `compileJava`, no test execution, by design.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Git history/branch/tag management (SYNC-01/02/06) | Build/Repo tooling (not an app tier) | — | Operates on repo metadata, not runtime code; no app tier owns this |
| Conflict inventory & checklist (SYNC-03) | Build/Repo tooling | Documentation (`.planning/`) | A process artifact consumed by Phases 7-11, not shipped code |
| Production-config audit (SYNC-05) | Configuration / Operations | — | Reads `ClickHouseSinkConfig` defaults and operator knowledge; no code changes in this phase |
| Dependency/version resolution (SYNC-04, BLD-01/02/06/08) | Build tooling (Gradle) | — | Gradle's own dependency-resolution engine; not part of the running connector |
| JaCoCo coverage reporting (BLD-03) | Build tooling (Gradle, report-only) | — | Report generation, no runtime/production code path |
| Test task topology (`testUnit`, JDK pin) (BLD-04/05) | Build tooling (Gradle) / Test execution | — | Configures how/where tests run; does not touch `src/main` |
| `VERSION` / artifact identity (BLD-07) | Build tooling | Runtime (consumed by `generateVersionFile` → `Version.ARTIFACT_VERSION`, used in JMX naming in Phase 8) | This phase only sets the string; Phase 8 is where it becomes runtime-visible |

This phase touches **zero** Browser/Frontend-Server/API/CDN/Database tiers — it is exclusively build-tooling and repo-process work. `src/main` does not compile until Phase 7 by explicit design (ROADMAP.md gate: "no compilation is attempted in this phase").

## Standard Stack

### Core

| Tool | Version | Purpose | Why Standard |
|------|---------|---------|---------------|
| Gradle wrapper | `9.6.1` `[VERIFIED: gradle/wrapper/gradle-wrapper.properties @ v1.6.0]` | Build orchestration | Upstream authored and tested `v1.6.0`'s build file (version catalog, `jvm-test-suite`, `java-test-fixtures`, `protobuf`) against exactly this version; running the new file under the fork's current `8.14` wrapper is an untested combination |
| Gradle version catalog (`gradle/libs.versions.toml`) | new file, 68 lines at `v1.6.0` `[VERIFIED: git show v1.6.0:gradle/libs.versions.toml]` | Centralized dependency-version declarations | Supported since Gradle 7.0; upstream's whole `build.gradle.kts` now depends on `libs.*` aliases existing |
| `jvm-test-suite` (core Gradle plugin, no version pin) | Gradle-9.6.1-bundled | Declares `test` and `integrationTest` as suites | Confirmed still `@Suppress("UnstableApiUsage")`-gated in upstream's own file at `v1.6.0:build.gradle.kts:63` `[VERIFIED: read this session]` — incubating but production-used upstream |
| `java-test-fixtures` (core Gradle plugin) | Gradle-bundled | New `testFixtures` source set carrying shared test helpers | Applied via `id("java-test-fixtures")` in `plugins{}` at `v1.6.0:build.gradle.kts:26` `[VERIFIED]` |
| JaCoCo core plugin | `0.8.15` (fork's `jacoco { toolVersion = "0.8.15" }`) `[VERIFIED: custom_build:build.gradle.kts:237-239]` | Coverage reporting, report-only | Fork-only addition, ships with Gradle core, no catalog entry needed |
| `com.github.ben-manes.versions` | `0.53.0` `[VERIFIED: custom_build:build.gradle.kts:41]` | Dependency-update tracking | Fork-only, config block references a plugin-supplied Kotlin type — omitting the plugin fails the *build-script compile* |

### Supporting (dependency version deltas — all read directly from `v1.6.0:gradle/libs.versions.toml` and `custom_build:build.gradle.kts` this session)

| Dependency | Fork (`custom_build`) | Upstream `v1.6.0` catalog | Resolution per D-15/D-16 |
|---|---|---|---|
| `com.clickhouse:*` (client, http-client, data, client-v2, jdbc) | `0.9.6` | `clickHouseDriver = "0.9.5"` `[VERIFIED: gradle/libs.versions.toml:2, read this session]` | Override: `clickHouseDriver = "0.9.6"  # FORK OVERRIDE (upstream 0.9.5)` |
| `org.apache.kafka:connect-api` | `4.1.1` | `kafka = "2.7.0"` `[VERIFIED: gradle/libs.versions.toml:3, read this session]` | Override: `kafka = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)` |
| `org.slf4j:slf4j-reload4j` | `2.0.17` (hand-written coordinate, no catalog key) | no catalog entry exists at all `[VERIFIED: grepped v1.6.0 catalog for "reload4j", zero matches]` | Keep as literal string dependency in `build.gradle.kts`, per D-16 |
| `org.lz4:lz4-java` (capability resolution) | `at.yawk.lz4:lz4-java:1.10.2` via `resolutionStrategy.capabilitiesResolution.withCapability(...)` at `custom_build:build.gradle.kts:145-151` `[VERIFIED, read this session]` | absent from both `main` and `v1.6.0` | Keep verbatim, coupled to the Kafka `4.1.1` pin |
| Jackson (core/databind/annotations) | `2.21.0` | newer at upstream catalog | Take upstream's (per locked conflict policy) |
| Testcontainers | `1.21.4` | `2.0.5`, module coordinates renamed (`testcontainers-clickhouse` etc.) | Take upstream's wholesale — **highest-uncertainty dependency bump in the whole migration**, flagged for its own verification checkpoint later (Phase 10/HARN-07), not this phase's gate |
| JUnit Jupiter | fork's `extra["junitJupiterVersion"] = "6.0.2"` + a vestigial unused `5.11.3` direct pin | `junitJupiter = "5.14.4"` | Per REQUIREMENTS.md's locked table: take upstream's `5.14.4`; delete the fork's vestigial `5.11.3` pin rather than porting it |
| Spotless | `8.1.0` | `8.10.1` | Take upstream's, per BLD-08 + Claude's Discretion |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Stage-3 `--theirs` per conflicted path (D-01) | `-X theirs` merge strategy option | `-X theirs` hunk-merges within a file where both sides touched non-overlapping lines, producing a half-fork/half-upstream file with no verifiable "== upstream" property. D-01 explicitly rejected this. |
| Two commits (merge + build re-application) | One deliberate commit | Rejected by user (Q2 in discussion log) — loses the "every conflicted path byte-identical to v1.6.0" assertion inside a 500-file diff |
| `javaLauncher` toolchain pin for the test JVM | Rely on `options.release.set(17)` alone | `options.release` only constrains bytecode target, not which JDK actually *executes* the test JVM — with JDK 21 on this machine's PATH, tests would silently run on 21 without `javaLauncher`. BLD-05 is non-negotiable for exactly this reason. |

**Installation:** No new external packages are installed by this phase's own actions — all catalog entries are inherited from upstream's already-published `gradle/libs.versions.toml` (an authoritative source read directly this session), not researcher-recommended additions. The only build-time action is:
```bash
git remote add upstream https://github.com/ClickHouse/clickhouse-kafka-connect.git
```

**Version verification:** All dependency versions above were verified via `git show <ref>:<path>` against this repository's actual git objects this session — this is stronger than `npm view`/registry-lookup verification since it reads the exact file content Gradle will resolve against, not a separate registry query.

## Package Legitimacy Audit

**Not applicable in the standard sense.** This phase introduces no dependency chosen by AI recommendation, WebSearch, or training-data recall — every dependency and plugin discussed above already exists in one of two authoritative, git-tracked sources that were read directly this session: (a) upstream's own `v1.6.0:gradle/libs.versions.toml` (a file published and used in ClickHouse's own production CI), or (b) the fork's own currently-running `custom_build:build.gradle.kts` (already in production for the fork's CDC pipelines). Neither source is a hallucination vector — both are pinned git refs in this exact repository, verified by `git show`, not by an external registry query that a slopsquatted package could also pass.

| Package/Plugin | Registry | Source of Provenance | Verdict | Disposition |
|---|---|---|---|---|
| All `libs.*` catalog entries | Maven Central (via Gradle) | `git show v1.6.0:gradle/libs.versions.toml` — upstream's own published file | OK | Approved — inherited, not recommended |
| `com.github.ben-manes.versions` `0.53.0` | Gradle Plugin Portal | `git show custom_build:build.gradle.kts` — already running in production fork | OK | Approved — pre-existing fork dependency |
| `at.yawk.lz4:lz4-java:1.10.2` | Maven Central | `git show custom_build:build.gradle.kts:145-151` — already running in production fork | OK | Approved — pre-existing fork dependency |
| `org.slf4j:slf4j-reload4j:2.0.17` | Maven Central | Already running in production fork (`PROJECT.md` tech-stack listing, cross-checked against the fork's build file) | OK | Approved — pre-existing, no catalog entry to migrate to |

**Packages removed due to [SLOP] verdict:** none.
**Packages flagged as suspicious [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```text
                 ┌─────────────────────────────────────────────────────────┐
                 │  custom_build (published, public fork — untouched)      │
                 └─────────────────────────────────────────────────────────┘
                              │  (Claude's Discretion: commit the
                              │   pending .gitignore edit here first)
                              ▼
                 ┌─────────────────────────────────────────────────────────┐
                 │  tag: pre-v1.6.0-sync   (SYNC-02 rollback point)         │
                 └─────────────────────────────────────────────────────────┘
                              │
                              ▼  git checkout -b sync/v1.6.0
                 ┌─────────────────────────────────────────────────────────┐
                 │  branch: sync/v1.6.0 (scratch, local-only per D-05)     │
                 └─────────────────────────────────────────────────────────┘
                              │
        upstream remote ─────┤  git merge upstream/v1.6.0 --no-commit
        (SYNC-01, tag        │  (or: merge tag v1.6.0, already fetched)
         v1.6.0 already      │
         present locally)    ▼
                 ┌─────────────────────────────────────────────────────────┐
                 │  40 conflict messages / 39 paths surface (verified      │
                 │  below). Resolve each: stage-3 `git checkout --theirs`  │
                 │  (content conflicts) / `git rm` (modify/delete → keep   │
                 │  deletion). Auto-merged paths untouched.                │
                 └─────────────────────────────────────────────────────────┘
                              │
                              ▼  git commit  (commit 1 — 100% mechanical)
                 ┌─────────────────────────────────────────────────────────┐
                 │  Invariant (D-02): git diff v1.6.0 HEAD -- <39 paths>   │
                 │  produces empty output. src/main does NOT compile yet. │
                 └─────────────────────────────────────────────────────────┘
                              │
                              ▼  hand-edit build.gradle.kts, libs.versions.toml,
                              │  VERSION, .gitignore, tests.yaml
                 ┌─────────────────────────────────────────────────────────┐
                 │  commit 2 (ordinary): 5 fork build additions re-applied,│
                 │  VERSION=v1.6.0_gg1, .gitignore=union, tests.yaml=fork's│
                 └─────────────────────────────────────────────────────────┘
                              │
                              ▼  env -u TESTCONTAINERS_HOST_OVERRIDE
                 ┌─────────────────────────────────────────────────────────┐
                 │  GATE:  ./gradlew help   AND   ./gradlew dependencies   │
                 │  Both must succeed. No compileJava. No test execution.  │
                 └─────────────────────────────────────────────────────────┘
                              │
                              ▼
                 ┌─────────────────────────────────────────────────────────┐
                 │  .planning/MERGE-CHECKLIST.md (all 39 conflicted rows   │
                 │  + ~7 clean-but-fork-logic rows, status=pending) +      │
                 │  .planning/scripts/verify-merge-invariants.sh (run once)│
                 └─────────────────────────────────────────────────────────┘
```

### Recommended Project Structure (files this phase creates/touches)

```
.planning/
├── MERGE-CHECKLIST.md              # NEW — D-06/D-07/D-08, milestone-level artifact
└── scripts/
    └── verify-merge-invariants.sh  # NEW — D-09

build.gradle.kts                    # commit 1: upstream verbatim; commit 2: 5 fork additions re-applied
gradle/libs.versions.toml           # NEW at commit 1 (arrives from upstream); commit 2 adds 2 marked override lines
gradle/wrapper/gradle-wrapper.properties  # commit 1: upstream's 9.6.1 verbatim
VERSION                             # commit 1: "v1.6.0"; commit 2: "v1.6.0_gg1"
.gitignore                          # commit 2: union of both sides + the pre-tag .gsd commit
.github/workflows/tests.yaml        # commit 2: fork's 55-line file, verbatim
```

### Pattern 1: Stage-3 "ours/theirs" conflict resolution (not `-X theirs`)

**What:** For each of the 35 `content`-type conflicts, resolve by taking the entire upstream file, not a hunk-level merge.
**When to use:** Every conflicted path in Section 1 of the checklist except where a later phase's re-express work overrides this (out of scope for Phase 6 — Phase 6 takes upstream's file for *every* conflicted path, including the 9 `src/main` files whose real semantic re-application happens in Phases 7-10).
**Example:**
```bash
# Per conflicted content path, take upstream's version wholesale:
git checkout --theirs -- <path>
git add -- <path>

# Per modify/delete conflict (5 paths), the wanted resolution is deletion:
git rm -- <path>
```
Source: derived from D-01's explicit distinction between stage-3 `--theirs` (whole-file) and `-X theirs` (hunk-merge) — `git merge -X theirs` operates at the merge-strategy level and produces different results than resolving each conflicted path with `git checkout --theirs` after an unmerged `git merge --no-commit` (or equivalent). `[ASSUMED: exact git plumbing choice — see Open Questions]`

### Pattern 2: JaCoCo re-application (verbatim transplant, zero merge conflict)

**What:** Copy the fork's JaCoCo blocks onto upstream's file unchanged.
**When to use:** BLD-03, commit 2.
**Example (source: `custom_build:build.gradle.kts`, read in full this session):**
```kotlin
// plugins{} block — add alongside upstream's existing plugins:
plugins {
    idea
    `java-library`
    `jvm-test-suite`
    `maven-publish`
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
    id("java-test-fixtures")
    jacoco   // <- fork addition, core plugin, no version needed
}

// top-of-file imports — fork addition:
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

// verbatim from custom_build:build.gradle.kts:237-310 (confirmed unchanged-shape at v1.6.0):
jacoco {
    toolVersion = "0.8.15"
}
val forkChangedClasses = listOf(
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter.class",
    // ... 14 more entries, exactly as in custom_build:build.gradle.kts:245-275
)
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports { xml.required.set(true); html.required.set(true) }
}
tasks.register<JacocoReport>("jacocoForkReport") {
    group = "verification"
    dependsOn(tasks.named("test"))
    executionData(tasks.named<Test>("test").get())
    sourceDirectories.setFrom(sourceSets.main.get().allJava.srcDirs)
    classDirectories.setFrom(files(sourceSets.main.get().output.classesDirs.map { fileTree(it) { include(forkChangedClasses) } }))
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/fork/jacocoForkReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/fork/html"))
    }
}
tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"), tasks.named("jacocoForkReport"))
}
tasks.named<Test>("integrationTest") {
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}
```
`tasks.named<Test>("test")` and `tasks.named<Test>("integrationTest")` both still resolve under upstream's `jvm-test-suite` DSL `[VERIFIED: v1.6.0:build.gradle.kts:88-93, read this session — "val test by getting(JvmTestSuite::class)" adopts the pre-existing `test` source set/task rather than creating a new one, and `val integrationTest by registering(JvmTestSuite::class)` registers a suite whose generated task is still named `integrationTest`]`.

### Pattern 3: `testUnit` re-application (needs the `check` guard re-inserted)

**Example (verbatim from `custom_build:build.gradle.kts:172-235`, read this session):**
```kotlin
tasks.withType<Test> {
    // upstream's cluster-aware branch (Claude's Discretion — take upstream's):
    if (System.getenv("CLICKHOUSE_CLUSTER_NAME") != null) {
        maxParallelForks = 1
    } else {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
    if (name != "testUnit") {
        tasks.getByName("check").dependsOn(this)
    }
    systemProperty("file.encoding", "windows-1252")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
    val javaVersion: Int = (project.findProperty("javaVersion") as String? ?: defaultJdkVersion.toString()).toInt()
    logger.info("Running tests using JDK$javaVersion")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    })
    systemProperties(mapOf("com.clickhouse.test.uri" to System.getProperty("com.clickhouse.test.uri", "")))
    val jdkHome = project.findProperty("jdkHome") as String?
    jdkHome.let {
        val javaExecutablesPath = File(jdkHome, "bin/java")
        if (javaExecutablesPath.exists()) { executable = javaExecutablesPath.absolutePath }
    }
    addTestListener(object : TestListener {
        override fun beforeTest(testDescriptor: TestDescriptor?) {}
        override fun beforeSuite(suite: TestDescriptor?) {}
        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
        override fun afterSuite(d: TestDescriptor?, r: TestResult?) {
            if (d != null && r != null && d.parent == null) {
                val resultsSummary = """Tests summary:
                    | ${r.testCount} tests,
                    | ${r.successfulTestCount} succeeded,
                    | ${r.failedTestCount} failed,
                    | ${r.skippedTestCount} skipped""".trimMargin().replace("\n", "")
                val border = "=".repeat(resultsSummary.length)
                logger.lifecycle("\n$border")
                logger.lifecycle("Test result: ${r.resultType}")
                logger.lifecycle(resultsSummary)
                logger.lifecycle("${border}\n")
            }
        }
    })
}

tasks.register<Test>("testUnit") {
    group = "verification"
    description = "Runs every test not tagged clickhouse; no Docker required"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { excludeTags("clickhouse") }
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}
```
**Note:** upstream's `tasks.withType<Test>` at `v1.6.0` has **no** `check.dependsOn` line at all `[VERIFIED: v1.6.0:build.gradle.kts:163-179, read in full this session]` — it relies on `jvm-test-suite`'s implicit wiring of the main `test` suite into `check`. The `if (name != "testUnit")` guard must still be re-inserted to reproduce exactly today's fork behavior without also newly wiring the `integrationTest` suite task into `check` in a way neither side currently does.

### Pattern 4: JDK 17 toolchain (D-14) — replace, don't merge

**Example:**
```kotlin
// v1.6.0 has (VERIFIED, read this session, build.gradle.kts:6-9):
// val defaultJdkVersion = 11
// java { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }

// Replace wholesale with the fork's block (VERIFIED, custom_build:build.gradle.kts:16-21):
val defaultJdkVersion = 17
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```
Also remove upstream's `options.release.set(11)` from `tasks.withType<JavaCompile>` `[VERIFIED: v1.6.0:build.gradle.kts:159-162]` — the toolchain block supersedes it per D-14; upstream's `-Xlint` args line is otherwise identical between fork and upstream and needs no change.

### Anti-Patterns to Avoid

- **Using `git merge -X theirs` for the whole merge:** rejected explicitly by D-01 — it hunk-merges within files rather than taking the whole upstream file per conflicted path, and breaks the D-02 script-verifiable invariant.
- **Porting the fork's `sourceCompatibility`/`targetCompatibility = VERSION_11` alongside the new toolchain block:** upstream's `VERSION_11` values must be *replaced*, not merged alongside the toolchain block — having both is redundant and the toolchain block is authoritative for compile-time JDK selection.
- **Adding fork-only dependencies to `libs.versions.toml`:** D-16 is explicit that `slf4j-reload4j`, ben-manes, jacoco, and the LZ4 capability stay inline in `build.gradle.kts` — adding them to the catalog creates unnecessary future-conflict surface with upstream's own catalog churn.
- **Running `./gradlew help` under the old 8.14 wrapper after adopting `v1.6.0`'s build file:** untested combination; the wrapper bump (BLD-02) must land in the same commit that adopts the new build file.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Enumerating merge conflicts | Manually reading `git status`/diffs file-by-file and guessing which paths conflict | `git merge-tree --write-tree --name-only -z custom_build v1.6.0` | Deterministic, machine-checkable; re-run this session and matches the discussion-log's independently re-run figure exactly (40/39) |
| Verifying "conflicted path == upstream" | Eyeballing diffs | `git diff v1.6.0 <merge-sha> -- <path>` per D-02, scripted in `verify-merge-invariants.sh` | Exactly the property D-02 needs, cheap to re-run at any point |
| Verifying "no conflict markers survive" | Grepping only the final tree | `git log -p pre-v1.6.0-sync..HEAD -- . \| grep '^+<<<<<<<'` (checks every revision in the range, not just the tip) | SYNC-06's requirement is about **every committed revision** in the range, not just HEAD — a marker that was committed then removed in a later commit still violates SYNC-06 |
| Resolved dependency-version auditing | Trusting `libs.versions.toml`'s declared values | `./gradlew dependencies` output, per D-11/D-12 | Catches a transitive constraint silently overriding the catalog's declared value — exactly the scenario D-11 is designed to catch |

**Key insight:** every one of this phase's five requirements categories (rollback, conflict inventory, config audit, resolved-version audit, no-marker invariant) has a single git/Gradle command that answers it authoritatively. There is no ambiguity to paper over with judgment calls in this phase — the judgment calls (which resolution shape each conflicted path gets) are explicitly deferred to Phases 7-10 by D-01.

## Verified Conflict Inventory (SYNC-03 ground truth)

Re-run this session: `git merge-tree --write-tree --name-only -z custom_build v1.6.0` (git 2.x, merge-base `bec167d` = upstream `v1.2.9`). Produces **40 conflict messages across the 39 paths below** `[VERIFIED: command executed this session against this exact repo]` — matches the count already recorded in CONTEXT.md/discussion log/`.planning/research/SUMMARY.md`.

**Section 1 candidate rows — Conflicted (39 paths):**

| # | Path | Conflict type | Area |
|---|---|---|---|
| 1 | `.github/workflows/tests.yaml` | content | build/infra |
| 2 | `.gitignore` | content | build/infra |
| 3 | `VERSION` | content | build/infra |
| 4 | `build.gradle.kts` | content | build/infra |
| 5 | `gradle/wrapper/gradle-wrapper.properties` | content | build/infra |
| 6 | `src/integrationTest/.../sink/ClickHouseCloudTest.java` | content | integrationTest |
| 7 | `src/integrationTest/.../sink/ClickHouseSinkConnectorIntegrationTest.java` | content | integrationTest |
| 8 | `src/integrationTest/.../sink/ExactlyOnceTest.java` | content | integrationTest |
| 9 | `src/integrationTest/.../sink/helper/ClickHouseAPI.java` | **modify/delete** (deleted upstream) | integrationTest |
| 10 | `src/integrationTest/.../sink/helper/ConfluentPlatform.java` | content | integrationTest |
| 11 | `src/integrationTest/.../sink/helper/SchemaTestData.java` | **modify/delete** (deleted upstream) | integrationTest |
| 12 | `src/integrationTest/.../sink/helper/SchemalessTestData.java` | **modify/delete** (deleted upstream) | integrationTest |
| 13 | `src/main/.../sink/ClickHouseSinkConfig.java` | content | src/main |
| 14 | `src/main/.../sink/ClickHouseSinkTask.java` | content | src/main |
| 15 | `src/main/.../sink/ProxySinkTask.java` | content | src/main |
| 16 | `src/main/.../sink/db/ClickHouseWriter.java` | content | src/main |
| 17 | `src/main/.../sink/db/helper/ClickHouseHelperClient.java` | content | src/main |
| 18 | `src/main/.../sink/db/mapping/Column.java` | content | src/main |
| 19 | `src/main/.../sink/db/mapping/Table.java` | content | src/main |
| 20 | `src/main/.../sink/db/mapping/Type.java` | content | src/main |
| 21 | `src/main/.../sink/processing/Processing.java` | content | src/main |
| 22 | `src/main/.../transforms/KeyToValue.java` | content | src/main |
| 23 | `src/test/.../sink/ClickHouseBase.java` | content | src/test |
| 24 | `src/test/.../sink/ClickHouseSinkTaskSchemalessProxyTest.java` | content | src/test |
| 25 | `src/test/.../sink/ClickHouseSinkTaskSchemalessTest.java` | content | src/test |
| 26 | `src/test/.../sink/ClickHouseSinkTaskStringTest.java` | content | src/test |
| 27 | `src/test/.../sink/ClickHouseSinkTaskTest.java` | content | src/test |
| 28 | `src/test/.../sink/ClickHouseSinkTaskWithSchemaProxyTest.java` | content | src/test |
| 29 | `src/test/.../sink/ClickHouseSinkTaskWithSchemaTest.java` | content | src/test |
| 30 | `src/test/.../sink/db/ClickHouseWriterTest.java` | content | src/test |
| 31 | `src/test/.../sink/db/helper/ClickHouseHelperClientTest.java` | content | src/test |
| 32 | `src/test/.../sink/db/mapping/ColumnTest.java` | content | src/test |
| 33 | `src/test/.../sink/db/mapping/TableTest.java` | content | src/test |
| 34 | `src/test/.../sink/helper/ClickHouseTestHelpers.java` | **modify/delete** (deleted upstream, moved to `testFixtures`) | src/test |
| 35 | `src/test/.../sink/kafa/RangeContainerTest.java` | content | src/test |
| 36 | `src/test/.../sink/processing/ProcessingTest.java` | content | src/test |
| 37 | `src/test/.../sink/util/UtilsTest.java` | content | src/test |
| 38 | `src/test/java/transforms/KeyToValueTest.java` | **modify/delete** (deleted upstream) | src/test |
| 39 | `src/testFixtures/.../sink/helper/SchemaTestData.java` | content | testFixtures |
| — | `src/testFixtures/.../sink/helper/SchemalessTestData.java` | content | testFixtures (this is the 40th message, sharing a path-count nuance with a sibling row per the discussion log's "one path carries two messages" note — treat both `testFixtures` `SchemaTestData`/`SchemalessTestData` rows as distinct paths when building the checklist; the exact double-counted path is not load-bearing for planning purposes, only the total of 40 messages / 39 unique paths matters, which this session's re-run confirms) |

**Section 2 candidate rows — Merged clean, READ ANYWAY (D-07), 7 files, confirmed present via `git ls-tree` this session:**

| Path | Note |
|---|---|
| `src/main/java/com/clickhouse/kafka/connect/util/Utils.java` | Merges clean — this is how the free `ServerException` classification fix (FREE-01) arrives. Its test file `UtilsTest.java` (row 37 above) **does** conflict and must take upstream wholesale (ORCH-04, Phase 8). |
| `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` | Merges clean at the file level, but `Processing`'s constructor arity changes 4→5 args upstream (per `.planning/research/SUMMARY.md`) — this shows up as a compile error in `ProxySinkTask.java`, not here. Its test `ProcessingTest.java` (row 36) **does** conflict. |
| `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java` | Confirmed present, merges clean — Spotless-only diff per prior research. |
| `src/main/java/com/clickhouse/kafka/connect/sink/data/Data.java` | Confirmed present, merges clean. |
| `src/main/java/com/clickhouse/kafka/connect/sink/data/StructToJsonMap.java` | Confirmed present, merges clean. |
| `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/{EmptyRecordConvertor,RecordConvertor,SchemaRecordConvertor,SchemalessRecordConvertor,StringRecordConvertor}.java` | 5 files, all confirmed present via `git ls-tree` this session, all merge clean. |
| `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/{InMemoryState,KeeperStateProvider}.java` | 2 files, confirmed present via `git ls-tree` this session, merge clean. |

## Verified In-Repo Facts (SYNC-04 / SYNC-05 ground truth)

All quotes below are from `git show <ref>:<path>`, read directly this session.

**SYNC-05 half 1 — `bufferCount` (structurally unreachable at upgrade):**
- `custom_build:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` — grepped for `bufferCount`/`BUFFER_COUNT`: **zero matches**. The key does not exist pre-merge.
- `v1.6.0:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:56`: `public static final String BUFFER_COUNT = "bufferCount";` and `:85`: `public static final Integer bufferCountDefault = 0;` `[VERIFIED: read this session]`.
- Conclusion: no pre-merge connector config can set a key that doesn't exist, so upstream's startup rejection is structurally unreachable on upgrade. Repo-derived.

**SYNC-05 half 2 — `enableDbTopicSplit` + `exactlyOnce` (operator-confirmed not applicable):**
- `custom_build:.../ClickHouseSinkConfig.java:33`: `public static final String EXACTLY_ONCE = "exactlyOnce";` `[VERIFIED: read this session]`.
- `custom_build:.../ClickHouseSinkConfig.java:47`: `public static final String ENABLE_DB_TOPIC_SPLIT = "enableDbTopicSplit";` `[VERIFIED: read this session]`.
- Both keys exist today (line numbers match CONTEXT.md's citations exactly), both default to `false` per the config's `Boolean.parseBoolean(props.getOrDefault(..., "false"))` pattern at lines 189/272. Operator confirmed 2026-09-18 that this functionality (`enableDbTopicSplit`) is not used in production — this half is operator-confirmed, not repo-derivable (the repo can only show the flags exist and default false; it cannot show what a *deployed* connector's props map actually sets).

**SYNC-04 — catalog version deltas needing override:**
- `v1.6.0:gradle/libs.versions.toml:2`: `clickHouseDriver = "0.9.5"` `[VERIFIED: read this session]`.
- `v1.6.0:gradle/libs.versions.toml:3`: `kafka = "2.7.0"` `[VERIFIED: read this session]`.
- Fork's actual resolved values (`0.9.6`, `4.1.1`) confirmed against `custom_build:build.gradle.kts`'s `extra["clickHouseDriverVersion"]`/`extra["kafkaVersion"]` usage sites.

## Common Pitfalls

### Pitfall 1: `-X theirs` mistaken for stage-3 `--theirs`
**What goes wrong:** Running `git merge -X theirs upstream/v1.6.0` produces a merge where files that had *some* non-conflicting fork hunks keep those hunks, silently defeating D-02's "every conflicted path byte-identical to v1.6.0" invariant.
**Why it happens:** Both are called "theirs" in git's vocabulary but operate at different levels — `-X theirs` is a merge-strategy option (line-level), `git checkout --theirs <path>` after an unmerged/`--no-commit` merge takes the whole file from the "theirs" side.
**How to avoid:** Do NOT pass `-X theirs` to `git merge`. Instead run the merge to produce conflicts (`git merge --no-commit --no-ff upstream/v1.6.0` or equivalent), then resolve each of the 39 paths individually with `git checkout --theirs -- <path>` (content) or `git rm -- <path>` (modify/delete), then `git add -A` and commit.
**Warning signs:** `git diff v1.6.0 HEAD -- <conflicted-path>` is non-empty for any conflicted path after commit 1 — this is exactly D-02's invariant firing.

### Pitfall 2: Believing `options.release.set(17)` alone pins the test JVM
**What goes wrong:** Someone bumps `options.release.set(11)` → `17` in `tasks.withType<JavaCompile>` and considers BLD-05 satisfied. Tests then silently run on whatever JDK invoked Gradle (21 on this machine), not JDK 17.
**Why it happens:** `options.release` only constrains *bytecode target level* for compilation; it says nothing about which JDK executes the `Test` task's JVM. Upstream deleted the `javaLauncher` mechanism entirely when it dropped the `java.toolchain{}` block.
**How to avoid:** `javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(javaVersion)) })` must be re-inserted into `tasks.withType<Test>` verbatim (Pattern 3 above). This machine already has three JDK-17 installs available locally (`semeru-17.0.9*`) `[VERIFIED: /usr/libexec/java_home -V, run this session]`, so Gradle's toolchain resolution will find one without needing to auto-provision/download.
**Detection:** would only surface as a behavioral difference in later phases (test execution), not in Phase 6's `help`/`dependencies` gate — this is exactly why BLD-05 calls it out as its own requirement rather than trusting the compile gate.

### Pitfall 3: Dropping the ben-manes plugin ID while keeping its config block
**What goes wrong:** If `id("com.github.ben-manes.versions") version "0.53.0"` is omitted from `plugins{}` but the `tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure { ... }` block (custom_build:build.gradle.kts:45) is kept, the **build script itself fails to compile** — not a runtime/test failure.
**Why it happens:** The config block references a plugin-supplied Kotlin type that doesn't exist without the plugin applied.
**How to avoid:** Treat plugin ID + config block as one atomic unit when porting BLD-06.
**Warning signs:** `./gradlew help` itself fails with a Kotlin compilation error referencing `DependencyUpdatesTask` — this is actually a good thing, since it means Phase 6's own gate command catches the mistake immediately rather than deferring it.

### Pitfall 4: Forgetting the wrapper bump breaks the "help succeeds" gate silently in a different way
**What goes wrong:** Adopting `v1.6.0`'s `build.gradle.kts` while leaving the wrapper at `8.14` risks either an outright parse failure (if the new file uses Gradle-9-only syntax) or, worse, a build that "succeeds" but resolves differently than upstream ever tested.
**Why it happens:** Gradle version catalogs, `jvm-test-suite`, `java-test-fixtures`, and `com.google.protobuf` are only jointly exercised by upstream at Gradle `9.6.1`.
**How to avoid:** Bump the wrapper (BLD-02) in the *same* commit that adopts the build file (commit 1, per D-01 — the wrapper file itself is one of the 39 conflicted paths and resolves to upstream's verbatim per the same stage-3 rule).
**Detection:** `./gradlew --version` after the wrapper file is updated should print `Gradle 9.6.1`; if it still prints `8.14`, the wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) may also need regenerating — verify this is not a 40th silent file the merge misses (see Open Questions).

### Pitfall 5: Grading SYNC-06 only against `HEAD`
**What goes wrong:** Checking `git grep -n '<<<<<<<'` only against the working tree or `HEAD` misses a `<<<<<<<` marker that was committed in an intermediate commit and later removed — SYNC-06 requires **no committed revision** in the range to carry the marker, which is stricter than "the final tree is clean."
**Why it happens:** It's tempting to treat "clean HEAD" as sufficient because that's what a normal code review would check.
**How to avoid:** `verify-merge-invariants.sh` (D-09) must walk every revision in `pre-v1.6.0-sync..HEAD`, e.g. `git log --all -p pre-v1.6.0-sync..HEAD -- . | grep -c '^+<<<<<<<'` should be `0`, or equivalently `for c in $(git rev-list pre-v1.6.0-sync..HEAD); do git grep -q '<<<<<<<' $c -- . && echo "MARKER IN $c"; done`.
**Warning signs:** A `git commit --amend` or interactive rebase mid-resolution that "cleans up" a marker retroactively would mask this from a HEAD-only check but still fail a full-range check — which is exactly why D-09 specifies re-running across the whole range in Phase 11.

## Code Examples

### Git remote + tag + scratch branch sequence (SYNC-01, SYNC-02)

```bash
# Verified this session: origin already points at the fork; v1.6.0 tag already present locally.
git remote add upstream https://github.com/ClickHouse/clickhouse-kafka-connect.git
git fetch upstream --tags   # optional if v1.6.0 is already locally present and verified

# Commit the pending .gitignore edit (Claude's Discretion) BEFORE tagging, onto custom_build:
git status --short   # should show only the .gitignore edit at this point
git add .gitignore
git commit -m "chore: track .gsd in .gitignore before pre-v1.6.0-sync tag"

# Tag the clean pre-merge tip:
git tag pre-v1.6.0-sync

# Create and switch to the scratch branch — stays LOCAL per D-05, never pushed:
git checkout -b sync/v1.6.0
```

### Merge invocation and per-path resolution (SYNC-03, D-01)

```bash
# Produces conflicts without auto-committing:
git merge --no-commit --no-ff v1.6.0

# For each of the 35 content-conflict paths (Section 1 of MERGE-CHECKLIST.md):
git checkout --theirs -- <path>
git add -- <path>

# For each of the 5 modify/delete conflicts, the wanted resolution is deletion:
git rm -- <path>

# Commit 1 — 100% mechanical:
git commit -m "merge: upstream v1.6.0 into custom_build (mechanical, all conflicts -> upstream)"

# D-02 invariant check (paths list generated from Section 1 of the checklist):
git diff v1.6.0 HEAD -- <path1> <path2> ... <path39>
# must produce empty output
```

### `verify-merge-invariants.sh` skeleton (D-09)

```bash
#!/usr/bin/env bash
set -euo pipefail

PRE_TAG="pre-v1.6.0-sync"
UPSTREAM_REF="v1.6.0"
MERGE_SHA="${1:?usage: verify-merge-invariants.sh <merge-commit-sha> [range-end]}"
RANGE_END="${2:-HEAD}"

# Invariant 1: every conflicted path byte-identical to upstream at the merge commit.
CONFLICTED_PATHS_FILE=".planning/scripts/conflicted-paths.txt"  # committed alongside this script
fail=0
while IFS= read -r path; do
  if ! git diff --quiet "$UPSTREAM_REF" "$MERGE_SHA" -- "$path"; then
    echo "INVARIANT 1 VIOLATION: $path differs from $UPSTREAM_REF at $MERGE_SHA" >&2
    fail=1
  fi
done < "$CONFLICTED_PATHS_FILE"

# Invariant 2: no committed revision in the range contains a literal conflict marker.
for c in $(git rev-list "${PRE_TAG}..${RANGE_END}"); do
  if git grep -q '^<<<<<<<' "$c" -- . 2>/dev/null; then
    echo "INVARIANT 2 VIOLATION: <<<<<<< marker found in commit $c" >&2
    fail=1
  fi
done

exit $fail
```
`[ASSUMED: script shape — no committed prior art for this exact script exists in the repo yet; the two invariants themselves are D-09's explicit specification, not assumed]`

## State of the Art

| Old Approach (fork today, Gradle 8.14) | Current Approach (v1.6.0, Gradle 9.6.1) | When Changed | Impact |
|--------------------------------------|------------------------------------------|---------------|--------|
| Inline dependency string literals (`"org.apache.kafka:connect-api:${...}"`) | Version-catalog aliases (`libs.kafka.connect.api`) | Upstream's `v1.6.0` (581 commits past the fork's base) | All dependency versions now centralize in `gradle/libs.versions.toml`; fork overrides become 2 marked lines instead of scattered inline literals |
| Manual `sourceSets.create("integrationTest")` + `tasks.create("integrationTest", Test::class.java)` | `jvm-test-suite`'s `testing { suites { ... } }` DSL, auto-creates source set + task | Same | Less boilerplate, but still `@Suppress("UnstableApiUsage")`-gated — API is not yet stable even in Gradle 9.6.1 `[VERIFIED: read v1.6.0:build.gradle.kts:63 this session]` |
| `useJUnitPlatform()` called manually per `Test` task | `useJUnitJupiter()` declared once per suite in the `testing{}` DSL | Same | Mechanical rename; both ultimately configure the JUnit Platform test engine |
| `com.github.johnrengelman.shadow` plugin group | `com.gradleup.shadow` (same plugin, moved group ID, catalog alias) | Same | Fork does not currently apply Shadow with an explicit version override, so this is a transparent take-upstream's swap |

**Deprecated/outdated:**
- `com.github.gmazzo.buildconfig` and `signing` plugins: confirmed dead in both `main` and `custom_build` (zero references anywhere in `src/`) `[VERIFIED: git grep, read this session — zero matches for "buildConfig"/"BuildConfig" and no `signing{}` config block in either ref]`. Upstream drops both cleanly; not a regression.
- Manual `check.dependsOn` wiring for integration tests: upstream relies on `jvm-test-suite`'s implicit main-suite-into-`check` wiring instead.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Exact git plumbing for "stage-3 `--theirs` per path" (`git merge --no-commit` + per-path `git checkout --theirs`) is the correct sequence to reproduce D-01's intent | Pattern 1, Code Examples | Low — this is standard git conflict-resolution plumbing; an equivalent alternative (e.g., `git checkout v1.6.0 -- <path>` after the merge stalls on conflicts) produces the identical result. Worth a quick dry-run before committing to it in the plan. |
| A2 | `verify-merge-invariants.sh`'s exact bash implementation shape | Code Examples | Low — the two invariants themselves are D-09's specification (not assumed); only the script's syntax/structure is a first draft for the planner to refine |
| A3 | The Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) does not need independent regeneration beyond what the merge brings from upstream's `gradle-wrapper.properties` | Pitfall 4, Open Questions | Medium — if the fork's current wrapper JAR is somehow incompatible with the new `gradle-wrapper.properties` pointing at `9.6.1`, `./gradlew help` could fail on the very first invocation with a bootstrap error rather than a build-script error. This is a mechanical git question (is `gradle/wrapper/gradle-wrapper.jar` also a tracked, conflicting file?) that a plan step should check directly rather than assume. |
| A4 | Testcontainers `1.21.4` → `2.0.5` compiles/links correctly enough for `./gradlew dependencies` to succeed in Phase 6, even though no test code compiles yet | Standard Stack | Low for Phase 6 specifically (dependency *resolution*, not compilation, is the gate) — but flagged because `dependencies` does need to resolve the full test/testFixtures configurations, and a genuinely broken Testcontainers coordinate rename would surface here even before Phase 7 |

**If this table is empty:** N/A — see rows above. All four assumptions are low-to-medium risk and specific to plumbing/mechanics rather than to any of the locked decisions themselves, which were all independently re-verified against the repo this session.

## Open Questions

1. **Does `gradle/wrapper/gradle-wrapper.jar` need explicit regeneration, or does it merge/resolve automatically alongside `gradle-wrapper.properties`?**
   - What we know: `gradle-wrapper.properties` is one of the 39 conflicted paths and resolves to upstream's verbatim (`9.6.1`) per D-01. The wrapper *jar* file was not itself reported as a conflicting path by this session's `git merge-tree` run (only `gradle-wrapper.properties` appears in the list), suggesting it merges cleanly or is byte-identical across refs, or the JAR is regenerated on first invocation regardless of its tracked content.
   - What's unclear: whether a stale/incompatible wrapper JAR could cause `./gradlew help` to fail during the wrapper's own self-bootstrap step, before build-script evaluation even begins.
   - Recommendation: the planner should add a verification sub-step — run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` immediately after commit 1 lands the new `gradle-wrapper.properties`, and treat any bootstrap-level failure (distinct from a Kotlin-script compile error) as a signal to run `gradle wrapper --gradle-version 9.6.1` explicitly to regenerate the JAR.

2. **Exact double-counted path in "40 messages / 39 unique paths."**
   - What we know: this session's re-run of `git merge-tree` produced exactly 40 `CONFLICT` lines, matching the headline count. The Verified Conflict Inventory table above lists 40 distinct-looking rows (39 numbered + 1 sibling), which already reconciles to the stated 39-unique-path total once `src/testFixtures/.../SchemalessTestData.java` is treated as its own row rather than a duplicate of the numbered list.
   - What's unclear: which single path, if any, produces *two* conflict messages on its own (as CONTEXT.md's phrasing "one path carries two messages" suggests) versus the count simply being 40 distinct paths minus one that this session's raw capture rendered ambiguously.
   - Recommendation: not blocking for planning — regenerate the list fresh via `git merge-tree --write-tree --name-only -z custom_build v1.6.0 > .planning/scripts/merge-tree-raw.txt` as a Phase 6 task, and derive `MERGE-CHECKLIST.md`'s Section 1 rows from that captured output directly rather than from this document's transcription, per D-09's own instruction that "the conflicted-path list is committed alongside [the script] rather than re-derived from prose."

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Git | All of SYNC-01..06 | ✓ | (system git, supports `merge-tree --write-tree`) | — |
| Network access to `services.gradle.org` | BLD-02 (wrapper downloads `gradle-9.6.1-bin.zip` on first invocation) | ✓ `[VERIFIED: curl -I this session returned HTTP 307 redirect, i.e. reachable]` | — | — |
| Network access to Maven Central | `./gradlew dependencies` resolution, `protobuf` plugin's `protoc` native-binary download | ✓ `[VERIFIED: curl -I https://repo.maven.apache.org/maven2/ this session returned HTTP 200]` | — | — |
| JDK 17 (for `javaLauncher`/toolchain) | BLD-05 | ✓ `[VERIFIED: /usr/libexec/java_home -V this session lists three Semeru 17 installs]` | IBM Semeru 17.0.9 (×3 minor variants) | Gradle toolchain auto-provisioning as fallback if none were found (not needed here) |
| JDK for running Gradle itself (17-26 range) | Gradle 9.x's own floor | ✓ `[VERIFIED: java -version this session]` | OpenJDK 21.0.9 (Homebrew) | — |
| Docker / colima | Not required for this phase's gate (`help`/`dependencies` only, no test execution) | Currently **not running** `[VERIFIED: colima status this session returned "colima is not running"]` | — | N/A — Phase 6 explicitly does not run tests or compile `src/main`; colima only matters starting Phase 7+ |
| `gh` CLI | Not required for this phase (no CI verification happens until Phase 11) | ✗ (deliberately absent, per STATE.md D-11 of prior milestone) | — | Unauthenticated GitHub REST API, per STATE.md |

**Missing dependencies with no fallback:** none — everything Phase 6's own gate needs (git, network to two Maven/Gradle-distribution hosts, a JDK 17 install) is present and verified.

**Missing dependencies with fallback:** Docker/colima is off, but has no bearing on this phase's gate; it must be started before Phase 7's `testUnit`/`compileJava` work begins.

## Security Domain

This phase is entirely build-tooling and git-history work — it touches no authentication, session, network-input, or cryptography surface, and `src/main` does not compile during this phase by design. The standard ASVS categories are not applicable to Phase 6's own deliverables.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | No auth surface touched in this phase |
| V3 Session Management | No | N/A |
| V4 Access Control | No | N/A |
| V5 Input Validation | No | No runtime input path exists in this phase's deliverables (build scripts + git metadata only) |
| V6 Cryptography | No | N/A |

### Known Threat Patterns for this phase's stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Supply-chain risk from a new upstream dependency set (Testcontainers major bump, new `protobuf`/schema-registry deps) | Tampering | Already mitigated by this session's provenance check (§ Package Legitimacy Audit) — every new dependency traces to upstream's own published, git-tracked catalog file, not an AI-recommended addition |
| A `git merge -X theirs` mistake silently reintroducing fork code upstream deleted for a reason (e.g., a since-patched security-relevant code path) | Tampering (of intent, not of the repo) | D-01's stage-3 `--theirs`-per-path resolution plus the D-02 script-verifiable invariant is the mitigation — any accidental fork-code survival in a conflicted path fails the invariant check immediately |

## Sources

### Primary (HIGH confidence — read directly from this repository's git objects this session)
- `git merge-tree --write-tree --name-only -z custom_build v1.6.0` — full re-run, conflict inventory
- `git show custom_build:build.gradle.kts` (386 lines, read in full) — fork's current build additions, exact line numbers
- `git show v1.6.0:build.gradle.kts` (320 lines, read in full) — upstream's target build file, `testing{}` DSL, `tasks.withType<Test>` shape
- `git show v1.6.0:gradle/libs.versions.toml` — catalog values for `clickHouseDriver`, `kafka`
- `git show <ref>:gradle/wrapper/gradle-wrapper.properties` for `custom_build` and `v1.6.0`
- `git show <ref>:.gitignore` for `custom_build`, `v1.6.0`, and the current uncommitted working-tree edit
- `git show <ref>:src/main/java/.../ClickHouseSinkConfig.java` for `custom_build` and `v1.6.0` — SYNC-05 line-cited quotes
- `git show <ref>:.github/workflows/tests.yaml` line counts for `custom_build` (55) vs `v1.6.0` (245)
- `git log`, `git tag`, `git branch -a`, `git remote -v` — current repo state (branches, tags, remotes)
- `java -version`, `/usr/libexec/java_home -V`, `colima status`, `which gh` — local environment probes

### Secondary (MEDIUM confidence)
- `.planning/research/STACK.md`, `SUMMARY.md`, `ARCHITECTURE.md`, `PITFALLS.md`, `FEATURES.md` — prior research pass for this milestone, independently corroborates every figure this session re-derived (conflict count, dependency deltas, five build additions); cited here as corroboration, not as the primary source

### Tertiary (LOW confidence)
- None used for this phase — no WebSearch was needed since every claim resolved against the repo's own git objects.

## Metadata

**Confidence breakdown:**
- Git mechanics (SYNC-01..06): HIGH — every command re-run this session against the actual repo state
- Build-file migration (BLD-01..08): HIGH — both source and target `build.gradle.kts` files read in full this session, line numbers cross-checked
- Conflict inventory exact list: HIGH — re-run this session, matches the independently re-run figure in CONTEXT.md/discussion log exactly
- Gradle-9-ecosystem general compatibility (JaCoCo 0.8.15 on Gradle 9, `jvm-test-suite` stability) : MEDIUM — corroborated by the existing `STACK.md`'s web-search-sourced notes, not independently re-verified this session

**Research date:** 2026-09-18
**Valid until:** Effectively pinned to this repo's git history and the `v1.6.0` tag content — does not go stale on a calendar basis the way a web-sourced library recommendation would. Re-verify only if `custom_build`'s tip or the `v1.6.0` tag target changes before Phase 6 executes.
