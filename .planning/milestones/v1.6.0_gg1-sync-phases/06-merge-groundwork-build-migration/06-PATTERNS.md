# Phase 6: Merge Groundwork & Build Migration - Pattern Map

**Mapped:** 2026-09-18
**Files analyzed:** 9 direct-authorship targets (build/config/doc/script) + 39 conflicted paths handled as one mechanical-resolution class + 7 "read anyway" clean-merge paths
**Analogs found:** 9 / 9 direct-authorship targets have a concrete analog (the file's own fork and/or upstream revision, read directly this session); the 39 conflicted paths take a single git-mechanics pattern, not per-file code analogs (this phase does not re-express their contents — see note below)

**Phase-shape note:** This phase has no application feature surface. Every file in scope is a build script, dependency catalog, CI/process config, or a new planning artifact. Consequently "closest analog" for most rows below is **the same file's own content on the fork side (`custom_build`, read live from the working tree) and/or the upstream side (`v1.6.0`, read via `git show`)** — not a different file in the codebase. This is the correct and complete analog for this phase; do not force-fit an unrelated controller/service/component analog onto a Gradle build file. All line numbers below were read directly from the actual file contents this session (not carried over from prose in RESEARCH.md unchecked).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `build.gradle.kts` | config | transform (build-script authoring) | Itself: `custom_build:build.gradle.kts` (386 lines, current) and `v1.6.0:build.gradle.kts` (247 lines, target base) | exact — both revisions read in full this session |
| `gradle/libs.versions.toml` | config | transform | `v1.6.0:gradle/libs.versions.toml` (new file, arrives verbatim at commit 1; fork has no catalog today — its `extra.apply{}` block at `build.gradle.kts:66-81` is the source of the two override values) | exact for the arriving file; role-match for the override source |
| `gradle/wrapper/gradle-wrapper.properties` | config | transform | Itself: fork's current 5-line file vs `v1.6.0`'s 8-line file | exact |
| `VERSION` | config | transform | Itself: fork's `v1.2.9_gg4` vs `v1.6.0`'s `v1.6.0` | exact |
| `.gitignore` | config | transform (union merge, not take-theirs) | Itself: fork's current file vs `v1.6.0`'s file — real diff captured below | exact |
| `.github/workflows/tests.yaml` | config (CI) | event-driven (push-triggered pipeline) | Itself: fork's current 55-line file (survives verbatim, D-03) | exact — no merge needed, just confirm it lands unedited in commit 2 |
| `.github/scripts/jacoco_summary.py` | utility | batch (report post-processing) | Itself — already correct, read in full this session, **zero edits required** | exact (no-op analog) |
| `.planning/MERGE-CHECKLIST.md` | config/doc (process artifact) | CRUD (status column flips `pending`→`done` across Phases 7-11) | No code analog exists. Structural analog only: `.planning/REQUIREMENTS.md` and `.planning/ROADMAP.md`'s existing `| Decision \| Choice \|` / `| Item \| Reason \|` markdown-table conventions | no code analog — structural doc-table analog only |
| `.planning/scripts/verify-merge-invariants.sh` | utility (verification script) | batch | `v1.6.0:scripts/run-ch-version-client-matrix.sh` (bash style: `set -...o pipefail`, `ROOT_DIR="$(cd ... && pwd)"` resolution, heredoc-free) — plus the invariant-shape draft already in `06-RESEARCH.md` § Code Examples | role-match (only bash script in the repo to mirror style from) |
| `.planning/scripts/conflicted-paths.txt` | config (data file consumed by the verify script) | batch | No analog — generated fresh from `git merge-tree --write-tree --name-only -z custom_build v1.6.0` output, one path per line | no analog — generated artifact |
| **The 39 conflicted paths** (9 `src/main`, ~14 `src/test`, 6 `src/integrationTest`, 5 build/infra already covered above, `testFixtures` migrations) | mixed (controller/model/test/migration by original role) | mixed | **Not resolved via a code analog.** Pattern is git mechanics only: `git checkout --theirs -- <path>` (content conflicts) / `git rm -- <path>` (modify/delete conflicts) — see Shared Pattern "Mechanical Conflict Resolution" below | n/a — mechanical, not a copy-a-pattern operation |
| 7 "merge clean, read anyway" paths (`Utils.java`, `Processing.java`, `Record.java`, `Data.java`, `StructToJsonMap.java`, 5 record convertors, 2 state providers) | source (unchanged in this phase) | n/a | No action in Phase 6 — read-only awareness item for the checklist's Section 2, actioned in Phase 8 (ORCH-04) | n/a — informational only |

## Pattern Assignments

### `build.gradle.kts` (config, transform)

**Analog:** the file's own two revisions — `custom_build:build.gradle.kts` (current, 386 lines) and `v1.6.0:build.gradle.kts` (target base, 247 lines) — both read in full this session.

This is a **transplant**, not a merge-conflict resolution: commit 1 takes `v1.6.0`'s file verbatim (it is one of the 39 conflicted paths); commit 2 hand-applies the five numbered fork additions below onto that verbatim base. Use the fork's *current* line numbers only to locate source material — they do not carry over to the post-commit-1 file, whose line numbers shift because of the version-catalog + `jvm-test-suite` rewrite.

**Addition 1 — JaCoCo (BLD-03).** Source: `custom_build:build.gradle.kts:37` (`jacoco` in `plugins{}`), `:237-239` (`jacoco { toolVersion = "0.8.15" }`), `:241-276` (`forkChangedClasses` list, 15 entries + wildcards), `:278-300` (`jacocoTestReport` config + `jacocoForkReport` registration), `:302-310` (`finalizedBy` wiring on `test`, JaCoCo disabled on `integrationTest`). Upstream has **no** `jacoco` plugin or block anywhere in `v1.6.0:build.gradle.kts` — nothing to merge against, transplant verbatim including the imports `org.gradle.testing.jacoco.plugins.JacocoTaskExtension` / `org.gradle.testing.jacoco.tasks.JacocoReport` (fork's lines 11-12). Landing zone: `tasks.named<Test>("test")` and `tasks.named<Test>("integrationTest")` still exist as named tasks under upstream's `testing { suites { val test by getting(...); val integrationTest by registering(...) } }` DSL (`v1.6.0:build.gradle.kts` testing block, confirmed this session), so `tasks.named<JacocoReport>("jacocoTestReport")`, the `jacocoForkReport` registration, and the two `tasks.named<Test>(...)` post-wiring blocks all resolve unchanged against the new file.

**Addition 2 — `testUnit` + `check` exclusion guard (BLD-04).** Source: `custom_build:build.gradle.kts:176-178` (the `if (name != "testUnit") { tasks.getByName("check").dependsOn(this) }` guard, inside the shared `tasks.withType<Test> { ... }` block) and `:226-235` (the `tasks.register<Test>("testUnit") { ... excludeTags("clickhouse") ... }` block). Upstream's `tasks.withType<Test>` (`v1.6.0:build.gradle.kts`, the block starting `tasks.withType<Test> { if (System.getenv("CLICKHOUSE_CLUSTER_NAME") ...`) has **no** `check.dependsOn` line at all — it relies on `jvm-test-suite`'s implicit main-suite-to-`check` wiring. Re-insert the guard verbatim inside upstream's `tasks.withType<Test>` block (not as a separate block) so `check` still picks up `test` implicitly and `testUnit` still stays excluded explicitly.

**Addition 3 — Test-JVM JDK-17 pin + `windows-1252` encoding (BLD-05).** Source: `custom_build:build.gradle.kts:185-189` (`javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(javaVersion)) })`, reading `javaVersion` via `project.findProperty("javaVersion")` defaulting to `defaultJdkVersion`) and `:179` (`systemProperty("file.encoding", "windows-1252")`). Upstream's `tasks.withType<Test>` block keeps `maxParallelForks` (with its own new cluster-aware branch — see Addition 5 below), `testLogging`, the `javaVersion` property read, and `com.clickhouse.test.uri`, but **deletes** `useJUnitPlatform()` (moved into the suite DSL as `useJUnitJupiter()`), `file.encoding`, and `javaLauncher` entirely — confirmed by direct read this session, not merely by prose in RESEARCH.md. This is the requirement flagged as **non-negotiable**: without re-inserting `javaLauncher`, the test JVM silently runs on whatever JDK invoked Gradle (this machine has JDK 21 on `PATH` per `java -version`), not JDK 17.

**Addition 4 — LZ4 capability resolution (BLD-06, first half).** Source: `custom_build:build.gradle.kts:145-151` — verbatim:
```kotlin
configurations.configureEach {
    resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") {
        select("at.yawk.lz4:lz4-java:1.10.2")
        because("Resolve capability conflict between org.lz4 and at.yawk.lz4 providers.")
    }
}
```
Absent from both `main` and `v1.6.0`. Keep verbatim per D-16 — this is coupled to the Kafka `4.1.1` override, so it must land in the same commit as the `libs.versions.toml` override lines.

**Addition 5 — ben-manes `dependencyUpdates` (BLD-06, second half).** Source: `custom_build:build.gradle.kts:41` (`id("com.github.ben-manes.versions") version "0.53.0"` in `plugins{}`) and `:45-54` (the `tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure { rejectVersionIf { ... } }` block). **Pitfall (verified real, not hypothetical):** the config block references a plugin-supplied Kotlin type; dropping the plugin ID while keeping the block fails the *build-script Kotlin compile*, not a test. Treat plugin ID + config block as one atomic unit.

**Discretionary items also landing in this file, not among the "five" but locked by CONTEXT.md:**
- `maxParallelForks`: **take upstream's** cluster-aware version verbatim (upstream's `tasks.withType<Test>` opening lines: `if (System.getenv("CLICKHOUSE_CLUSTER_NAME") != null) { maxParallelForks = 1 } else { maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1 }`) — this *replaces* the fork's simple one-line version at `custom_build:build.gradle.kts:175`, it does not sit alongside it.
- JDK toolchain (D-14): **replace** upstream's `val defaultJdkVersion = 11` + `java { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }` (`v1.6.0:build.gradle.kts:6-9`) with the fork's `val defaultJdkVersion = 17` + `java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }` (`custom_build:build.gradle.kts:16-21`), and drop upstream's `options.release.set(11)` from `tasks.withType<JavaCompile>` (`v1.6.0:build.gradle.kts`, the `-Xlint` block) without porting any `sourceCompatibility`/`targetCompatibility` alongside it.
- `jdkHome` (fork's `custom_build:build.gradle.kts:193-199`): **drop**, per D-13. Do not port.
- `addTestListener` (fork's `custom_build:build.gradle.kts:201-220`): **keep**, verbatim, per D-13.

**Observation beyond the five named additions (flag for the planner, not a locked decision):** the fork also carries a `spotless { java { importOrder(); removeUnusedImports() } }` block plus a `tasks.register("removeUnusedImports") { dependsOn("spotlessApply") }` task at `custom_build:build.gradle.kts:374-386`. `v1.6.0:build.gradle.kts` applies `alias(libs.plugins.spotless)` in `plugins{}` but has **no** `spotless { java { ... } }` configuration block at all (confirmed by grep this session — the only `spotless` hit is the plugin alias line). CONTEXT.md/RESEARCH.md's "five fork build additions" list does not mention this block, and D-08's Spotless decision only addresses the plugin *version* (take upstream's `8.10.1`), not this formatting-config block's survival. If left unaddressed, this block silently disappears with no conflict marker and no compile error (Spotless would just run with default settings post-merge) — worth a planner decision, not assumed here.

---

### `gradle/libs.versions.toml` (config, transform — new file)

**Analog:** `v1.6.0:gradle/libs.versions.toml` (68 lines, arrives verbatim at commit 1 — the fork has no catalog today). Full content read this session; confirmed values:
- `gradle/libs.versions.toml:2`: `clickHouseDriver = "0.9.5"`
- `gradle/libs.versions.toml:3`: `kafka = "2.7.0"`
- No `slf4j-reload4j` entry anywhere in the catalog (grepped — zero matches).

**Override source (D-15):** the fork's `extra.apply { set("clickHouseDriverVersion", "0.9.6"); set("kafkaVersion", "4.1.1") ... }` block at `custom_build:build.gradle.kts:66-81` is where the two override values (`0.9.6`, `4.1.1`) come from — not a separate file. In commit 2, edit exactly two lines of the arrived catalog:
```toml
clickHouseDriver = "0.9.6"  # FORK OVERRIDE (upstream 0.9.5)
kafka            = "4.1.1"  # FORK OVERRIDE (upstream 2.7.0)
```
Every other catalog key stays byte-identical to upstream's arrived file — do not hand-port any other fork version (Spotless, Testcontainers, JUnit Jupiter, Jackson all take upstream's catalog values per the locked table in `06-CONTEXT.md`/`06-RESEARCH.md`).

**Fork-only coordinates that must NOT enter this file (D-16):** `org.slf4j:slf4j-reload4j:2.0.17` (fork's inline literal at `custom_build:build.gradle.kts:106`), the ben-manes plugin coordinate, the core `jacoco` plugin, the LZ4 `resolutionStrategy` capability block — all stay inline in `build.gradle.kts`.

---

### `gradle/wrapper/gradle-wrapper.properties` (config, transform)

**Analog:** the file's own two revisions, both read this session.

Fork (current, 5 lines):
```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```
Upstream `v1.6.0` (target, 8 lines):
```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
networkTimeout=10000
retries=1
retryBackOffMs=500
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```
Resolution: this is one of the 39 conflicted paths — takes upstream's file wholesale via stage-3 `--theirs` in commit 1, no hand-editing in commit 2. **Verification gap flagged by RESEARCH.md Open Question 1, not yet answered:** confirm whether `gradle/wrapper/gradle-wrapper.jar` is itself a tracked file needing separate handling — it did not appear as a conflicting path in this session's `git merge-tree` run, but a plan step should verify this directly (`git log --follow -- gradle/wrapper/gradle-wrapper.jar` / check if it's binary-identical or auto-regenerates) rather than assume.

---

### `VERSION` (config, transform)

**Analog:** the file's own two revisions. Fork: `v1.2.9_gg4`. Upstream `v1.6.0`: `v1.6.0` (bare, single line, confirmed via `git show v1.6.0:VERSION` this session). Resolution: commit 1 takes upstream's `v1.6.0` value (it's a conflicted path). Commit 2 is a one-line hand-edit to `v1.6.0_gg1`. No other pattern needed — `version = file("VERSION").readText().trim()` at `build.gradle.kts:57` is the only consumer, unchanged by the merge.

---

### `.gitignore` (config, transform — union merge, D-04)

**Analog:** the file's own two revisions, real diff captured this session (`diff <(cat .gitignore) <(git show v1.6.0:.gitignore)`):
```diff
37,41c37,38
< /bin/
< 
< .vscode
< 
< .gsd
---
> # AI
> memory
```
Both sides are otherwise identical (the standard Java/Gradle/IntelliJ/macOS boilerplate, lines 1-36 on both). Resolution: this is a genuine **union**, not a take-theirs — final file keeps everything through line 36 unchanged, then appends both tails: `/bin/`, `.vscode`, `.gsd` (fork) and `# AI` / `memory` (upstream). Note per CONTEXT.md's discretionary item: the working `.gsd` line must be committed onto `custom_build` *before* tagging `pre-v1.6.0-sync`, so this union resolution happens against a tree that already has `.gsd` tracked in the pre-merge tip.

---

### `.github/workflows/tests.yaml` (config/CI, event-driven)

**Analog:** itself — fork's current 55-line file (confirmed via `wc -l` this session) vs upstream's 245-line file (also confirmed via `wc -l`). No hand-merge pattern applies here: D-03 takes the fork's file **verbatim** in commit 2, dropping upstream's 245-line 3-D matrix entirely. The only "pattern" is confirming zero coupling to `build.gradle.kts` internals — the fork's workflow only shells out to `./gradlew test`, so it needs no edits regardless of which build-file shape lands. `on: [push]` is unqualified at line 8 — this is why D-05 keeps `sync/v1.6.0` local (pushing it would fire this workflow against a tree that can't compile until Phase 7).

---

### `.github/scripts/jacoco_summary.py` (utility, batch)

**Analog:** itself, read in full this session — **no edits required, this is a verification-only inclusion in the pattern map.** Its two hardcoded input paths (`TEST_REPORT_PATH = "build/reports/jacoco/test/jacocoTestReport.xml"`, line 19; `FORK_REPORT_PATH = "build/reports/jacoco/fork/jacocoForkReport.xml"`, line 20) match exactly the `xml.outputLocation` values the re-applied `jacocoTestReport`/`jacocoForkReport` tasks in `build.gradle.kts` produce (Addition 1 above — `jacocoTestReport`'s XML path is Gradle's JaCoCo-plugin default, `jacocoForkReport`'s is explicit at `build.gradle.kts:296`). Its `FORK_CLASSES_IN_ORDER` list (lines 26-42, 15 entries) is a base-class-name mirror of `forkChangedClasses` in `build.gradle.kts:245-276`, confirmed identical up to the `.class`/`$*.class` suffix stripping this script does deliberately (comment at lines 22-25 explains the wildcard-glob exclusion). **Nothing to plan here beyond "confirm zero drift" once Addition 1 lands** — this is the concrete evidence backing RESEARCH.md § Sources' claim that BLD-03's transplant needs no companion script edit.

---

### `.planning/MERGE-CHECKLIST.md` (config/doc, new file — SYNC-03/D-06/D-07/D-08)

**Analog:** no code analog — this is a new process document, not a re-expression of existing logic. Closest **structural** analog: `.planning/REQUIREMENTS.md`'s and `.planning/ROADMAP.md`'s existing markdown-table conventions (`| Decision | Choice |`, `| Item | Reason |` — two-column decision tables already used at the milestone level). Extend that convention to the 7-column table D-07 specifies: `path | conflict type | resolution shape (survives-as-is / re-express / redundant) | owning phase | fork behaviour at risk | adjudicating test | status`.

**Content sources, not analogs — pull directly, do not hand-transcribe from RESEARCH.md prose (RESEARCH.md's own instruction):**
- Section 1 (39 rows): regenerate fresh via `git merge-tree --write-tree --name-only -z custom_build v1.6.0`, capture to `.planning/scripts/merge-tree-raw.txt` first, derive rows from that file. RESEARCH.md's "Verified Conflict Inventory" table (39 numbered rows + 1 sibling) is a corroborating cross-check, not the source of truth.
- Section 2 (~7 rows): the "merge clean, READ ANYWAY" list is already fully enumerated in RESEARCH.md § Verified Conflict Inventory's second table and in `06-CONTEXT.md` D-07 — `Utils.java`, `Processing.java`, `Record.java`, `Data.java`, `StructToJsonMap.java`, the 5 record convertors (as one row or five — planner's call), `state/provider`'s 2 files.
- `## Recorded Facts` section (D-10): the two SYNC-05 findings, each labelled with its source type (`repo-derived` for `bufferCount`, `operator-confirmed` for `enableDbTopicSplit`) — exact wording available in `06-CONTEXT.md` D-10 and `06-RESEARCH.md` § Verified In-Repo Facts, both already line-cited against `ClickHouseSinkConfig.java`.
- Status column: every row starts `pending` (D-08) — this is the field Phase 11 greps for zero-`pending` before publication.

---

### `.planning/scripts/verify-merge-invariants.sh` (utility, batch — new file, D-09)

**Analog:** `v1.6.0:scripts/run-ch-version-client-matrix.sh` (read this session) for **bash style conventions only** — this is the only shell script anywhere in this repository to mirror idiom from:
```bash
#!/usr/bin/env bash

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
```
Adopt: `#!/usr/bin/env bash` shebang, an explicit `set -...o pipefail` line, and root-relative path resolution via `BASH_SOURCE[0]` rather than assuming CWD — the fork's one existing script does not assume it's invoked from the repo root either.

**Functional content — draft already exists in `06-RESEARCH.md` § Code Examples "verify-merge-invariants.sh skeleton"** (explicitly flagged there as `[ASSUMED: script shape]` — the two invariants themselves are D-09's locked specification, only the bash syntax is a first draft for the planner to refine):
```bash
#!/usr/bin/env bash
set -euo pipefail

PRE_TAG="pre-v1.6.0-sync"
UPSTREAM_REF="v1.6.0"
MERGE_SHA="${1:?usage: verify-merge-invariants.sh <merge-commit-sha> [range-end]}"
RANGE_END="${2:-HEAD}"

# Invariant 1: every conflicted path byte-identical to upstream at the merge commit.
CONFLICTED_PATHS_FILE=".planning/scripts/conflicted-paths.txt"
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
**Note on Pitfall 5 (already validated against this repo's own semantics):** invariant 2 must walk `git rev-list "${PRE_TAG}..${RANGE_END}"` (every commit in the range), not just `grep` the working tree/HEAD — a marker committed then removed in a later commit would pass a HEAD-only check but must fail this one.

**Companion data file `.planning/scripts/conflicted-paths.txt`:** one path per line, the 39 unique paths from Section 1 of `MERGE-CHECKLIST.md`, committed alongside the script per D-09's explicit instruction ("the conflicted-path list is committed alongside it rather than re-derived from prose").

---

## Shared Patterns

### Mechanical Conflict Resolution (governs all 39 conflicted paths + `.gitignore`'s union exception)

**Source:** D-01, D-04, verified via `git merge-tree --write-tree --name-only -z custom_build v1.6.0` this session (40 messages / 39 paths, matching RESEARCH.md's independently re-run figure exactly).

**Apply to:** every path in the 39-path conflict list — the 9 `src/main`, ~14 `src/test`, 6 `src/integrationTest`, `testFixtures` migrations, plus 5 build/infra paths (all covered individually above).

```bash
# Produce conflicts without auto-committing:
git merge --no-commit --no-ff v1.6.0

# Per content-conflict path (35 of the 39):
git checkout --theirs -- <path>
git add -- <path>

# Per modify/delete conflict (5 of the 39 — resolves as deletion, not a keep):
git rm -- <path>

# EXCEPTION: .gitignore is NOT resolved this way — it's a hand-authored union (see its own section above).

# Commit 1 — 100% mechanical, no hunk-merging:
git commit -m "merge: upstream v1.6.0 into custom_build (mechanical, all conflicts -> upstream)"
```

**Anti-pattern, explicitly rejected by D-01:** `git merge -X theirs upstream/v1.6.0` — this is a merge-strategy option that hunk-merges non-conflicting fork lines into an otherwise-upstream file, silently defeating the D-02 invariant (`git diff v1.6.0 <merge-sha> -- <path>` must be empty for every conflicted path). Use per-path `git checkout --theirs` after an unmerged `--no-commit` merge, never the `-X` flag.

### Invariant Verification (D-02, D-09, governs commit 1's acceptance)

**Source:** `.planning/scripts/verify-merge-invariants.sh` (see its own section above).
**Apply to:** commit 1 immediately after it lands (single-commit check in Phase 6); re-run in Phase 11 across the full `pre-v1.6.0-sync..HEAD` range.

### Gate Command (governs commit 2's acceptance — the only pass/fail signal for this entire phase)

**Source:** `06-CONTEXT.md` § Phase Boundary, `.planning/ROADMAP.md`, `.planning/config.json`'s `build_command`.
```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew dependencies
```
**Apply to:** commit 2 only — never commit 1 (which is not expected to have a working build file until the five additions land). No `compileJava`, no test execution — `src/main` does not compile until Phase 7 by explicit design.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.planning/MERGE-CHECKLIST.md` | config/doc | CRUD | Genuinely new process artifact — no prior file in this repo tracks per-conflict resolution status across phases. Use `.planning/REQUIREMENTS.md`/`ROADMAP.md`'s table conventions structurally (cited above), but there is no content to copy — the 39+7 rows must be generated fresh from `git merge-tree` output and the SYNC-05 facts, not adapted from an existing document. |
| `.planning/scripts/conflicted-paths.txt` | config (data) | batch | Generated artifact, not authored — one path per line, produced by piping `git merge-tree`'s output, no prior file of this shape exists in the repo. |

## Metadata

**Analog search scope:** repository root (`build.gradle.kts`, `gradle/`, `VERSION`, `.gitignore`), `.github/workflows/`, `.github/scripts/`, `.planning/` (existing docs for structural convention only), `git show v1.6.0:*` for every upstream counterpart, `git merge-tree --write-tree --name-only -z custom_build v1.6.0` for the conflict inventory.
**Files scanned:** `build.gradle.kts` (both revisions, full read), `gradle/libs.versions.toml` (upstream, full read), `gradle/wrapper/gradle-wrapper.properties` (both revisions), `VERSION` (both revisions), `.gitignore` (both revisions, diffed), `.github/workflows/tests.yaml` (line counts both sides), `.github/scripts/jacoco_summary.py` (full read), `v1.6.0:scripts/run-ch-version-client-matrix.sh` (style reference), `.planning/REQUIREMENTS.md` (table-convention reference).
**Tracked-source gate:** every path cited above was verified with `git ls-files -- <path>` this session (all print non-empty except `gradle/libs.versions.toml`, which is intentionally new — it does not exist on `custom_build` yet and arrives from upstream at commit 1). No gitignored mirror paths were used as an analog anywhere in this document.
**Pattern extraction date:** 2026-09-18
