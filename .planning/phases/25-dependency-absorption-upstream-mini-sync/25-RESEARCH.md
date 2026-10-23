# Phase 25: Dependency Absorption & Upstream Mini-Sync - Research

**Researched:** 2026-10-21
**Domain:** Gradle/Shadow build repair, git merge mechanics behind an invariant gate, Kafka Connect API compatibility evidence, pin-guard unit test hardening (Java 17, Gradle 9.8.1 wrapper, Shadow 9.6.1)
**Confidence:** HIGH. Every load-bearing fact below was measured in throwaway worktrees in this session (commands recorded). The remaining MEDIUM items are the advisory severity wording (reported by CNAs, NVD analysis pending) and everything that needs the Docker suite, which was not run (A5).

> Method note. All measurements ran in detached throwaway worktrees under `/tmp/p25/` (`w0` = tag `v1.7.0_gg3`, `w1` = dry-run merge 1, `w2` = merge 1 + Shadow repair + dry-run merge 2) plus docker pulls. Nothing was pushed, no ref in the real repository was created or moved, the real working tree is untouched. The `gsd-tools research-plan/research-store` seam was not used: every source was a primary one (OSV API, NVD API, GitHub REST, Gradle/Docker output) fetched directly. Every Gradle call used `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Locked by ROADMAP, REQUIREMENTS, STATE and precedent, not re-opened here:**

*Order and merges*
- The six-step order in the ROADMAP Phase 25 note: DEBT-13 with its own `testUnit` gate; pre-flight (delete `refs/upstream/main` and `refs/upstream/v1.6.0`, `git ls-remote origin custom_build`, record `SELECT version()` of `latest`, tag the pre-merge tip locally); merge 1; Shadow repair + archive compare + connect-api evidence + CI-20 + DEBT-14 + one full suite run; merge 2; DEBT-15 and the two `--rerun` runs.
- Merge 1 is conflict-free (`git merge-tree` confirmed in this discussion: tree `ca99beea`). Merge 2 conflicts in exactly `.gitignore` and `gradle/libs.versions.toml`; shared keys keep the fork's values (`kafka` 4.4.0, `lz4Java` 1.12.0, jackson 2.22.4, gson 2.14.0, lombok 1.18.48), `guava = "33.7.2-jre"` is added, `httpclientTest` is deleted. `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` records the resolution table and the conflicted paths. No dependency bump rides inside merge 2.
- `UPSTREAM_REF` is the full SHA, never `upstream/main`. `PRE_TAG` for merge 2 is a distinct local rollback tag (`pre-v1.7.0-tail`; `pre-v1.7.0_gg4` is reserved for the release).
- `verify-merge-invariants.sh` is never edited (Phase 21 D-02). Anchors and exemptions carry the change; exemption rows are generated from the gate's own stderr and key on the exact row at the exact commit. Anchor rows move in the same commit as the code they pin.
- No `.planning/` commit is interleaved with in-progress conflict resolution.

*Gates and evidence*
- Every Gradle call runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew …`. Docker is colima profile `vz`. A two-run gate uses `--rerun` for the second run. If the suite goes red on unchanged code, check the `latest` image date first.
- Spotless gate is baseline-equal (Phase 21 D-14): no tree-wide reformat; files this phase edits are spotless-clean on their own.
- Pin-guard changes are seen red on committed in-test fixtures before the fix, rules extracted as static methods, existing test names and assertions kept (Phase 21 D-05..D-07).
- A `--warning-mode all` run under Gradle 9.8.1 is recorded with no build-script change. The 18 named skips stay the same.
- Measured, not cited: every operator-facing claim this phase produces (archive delta, bundled set, Connect API references) rests on a transcript taken in this phase.

*Scope exclusions*
- Dependabot PR #4 (client `0.10.0`) and PR #10 (protobuf-java 4.36.2) stay open, unmerged and not ignored.
- OPS-04 is a user action in the GitHub web UI; the answer is given in the user's own words at the start of execution, and a relayed answer is not accepted (Phase 21 D-13). No agent creates the label or edits `dependabot.yml`.
- `integrationTests.yaml` and the other upstream workflows are not edited.
- Production worker: `confluentinc/cp-kafka-connect-base:7.9.7` (Confluent Platform 7.9, Apache Kafka 3.9). The rollout-note content about it (OPS-06) is Phase 29's; this phase produces the evidence.

## Implementation Decisions

### Shadow 9 repair (BLD-15)
- **D-01:** `confluentJar` is repaired with `configurations.add(clickhouseDependencies)` plus `from(sourceSets.main.get().output)`, as the research recommends. The `zipTree` provider form is the fallback only if the entry-by-entry compare shows a difference outside the allow-list. The choice is made by the measured diff, not in advance.
- **D-02:** `addMultiReleaseAttribute.set(false)`. `MANIFEST.MF` stays equal to the `v1.7.0_gg3` manifest apart from version strings. Turning the attribute on would activate `META-INF/versions/**` classes of jackson, gson and the ClickHouse client jars on the worker's JVM, which is a behaviour change this phase promises not to make. The decision and its reason are recorded in FORK-DELTA.
- **D-03:** `shadow { addShadowJarToAssembleLifecycle.set(false) }` (option since Shadow 9.4.0). No stray `*-all.jar` of the runtime classpath (which would contain connect-api) is built on `assemble`; the GSD build gate runs `./gradlew assemble`. One fork-only line, recorded in FORK-DELTA.
- **D-04:** The repair is pinned by **two anchor rows** in `.planning/scripts/fork-anchors.txt` in the same commit: `build.gradle.kts` `configurations.add(clickhouseDependencies)` count 1, and `build.gradle.kts` `from(clickhouseDependencies, ` count 0. Upstream at `a6f1df83` still has the broken `from(clickhouseDependencies, sourceSets.main.get().output)` line on Shadow 8.3.5, so the next upstream sync will try to restore it; the negative anchor fails the merge gate before CI-20 ever runs. Plus the FORK-DELTA row (BLD-15).
- **D-05:** The archive compare is a **committed `python3 -I` script under `.planning/scripts/`** (beside `verify-merge-invariants.sh`). Inputs: two archive zips and an allow-list file. Output: only-in-a, only-in-b, changed, and the nested-`*.jar` count, with per-entry SHA-256. Its output is the BLD-15 evidence transcript, and Phase 29 reuses it for the release evidence. The gg3 baseline is built from tag `v1.7.0_gg3` in a throwaway worktree (no GitHub release asset exists, per Phase 24's D-07 correction). Expected allow-list: version bumps, guava 33.7.2 and its five small dependencies (`failureaccess`, `listenablefuture`, `jspecify`, `error_prone_annotations`, `j2objc-annotations`).

### CI-20 nested-jar guard
- **D-06:** The check is a **Gradle verification task `verifyConfluentArchive`** that opens the built archive's `lib/*.jar` and fails on any violation; `createConfluentArchive` is `finalizedBy` it, so the guard runs wherever the archive is built (CI, local, GSD gates). `tests.yaml` gets a named step that runs `./gradlew createConfluentArchive`.
- **D-07:** The task rejects **two** things: any nested `*.jar` entry, and any `org/apache/kafka/connect/**` class inside the inner jar. The second rule turns the BLD-16 "not bundled" proof into a standing guard, so a future edit to `clickhouseDependencies` cannot start shipping the Connect API.
- **D-08:** The step sits **before `Run tests`**, after `Compile integration-test sources`. A broken archive fails the job in about a minute rather than after the suite, and the archive build warms the same compile outputs the suite uses. The job stays one job; no second runner, no change to the one-run-per-SHA shape.
- **D-09:** The pin guard pins **step and task text, red-first**, in the `reportTestForks` shape (Phase 21 D-08): find the step by name and require a `./gradlew … createConfluentArchive` call in its `run:` block; require `verifyConfluentArchive` registered in `build.gradle.kts` and `createConfluentArchive` finalized by it. Fixtures: missing step, renamed task, dropped `finalizedBy`, each seen red before the rule goes green. This edit and DEBT-13/DEBT-14 are the only edits of `TestsWorkflowPinGuardUnitTest` in this milestone.

### connect-api 4.4.0 compile-time proof (BLD-16)
- **D-10:** The reference for "the production worker's API" is the **jars copied out of the `confluentinc/cp-kafka-connect-base:7.9.7` image** (`docker create` + `docker cp`), not a Maven Central artifact. That is the API the connector loads against, including any Confluent `-ccs` difference. The image digest and the jar names and versions found are recorded in the evidence.
- **D-11:** The member-reference check is **`javap -v` constant-pool extraction**: over every compiled connector class, collect the `Methodref`, `InterfaceMethodref`, `Fieldref` and `Class` entries under `org.apache.kafka.**`, then prove each exists in the image jars and in `connect-api`/`kafka-clients` 4.1.1 (`javap -p` on those jars). JDK-only, no new dependency. The script is committed under `.planning/scripts/` beside the archive compare.
- **D-12:** The reference scope covers **both `connect-api` and `kafka-clients`**: the connector compiles against `ConfigDef`, `TopicPartition`, `Headers` and friends from kafka-clients 4.4.0, which the worker also supplies, so a member added after 3.9 there would break at load just like a Connect one.
- **D-13:** Durability: the script writes the **set of referenced `org.apache.kafka.**` members to a committed reference list** (under `.planning/scripts/` or `src/test/resources`, planner's call), blessed once against the image jars. A **Docker-free `testUnit` case** re-derives the set from the compiled main classes and fails if a member appears that is not in the list. A future code change that reaches for a newer API shows up in `testUnit`; re-blessing the list requires re-running the script against the worker jars and recording it. — **Reversibility:** reversible — deleting the test and the list restores the transcript-only state.
- **D-14:** The BLD-16 wording edits (FORK-DELTA's "production Connect API version" sentence, the `ClickHouseSinkTask` comment, the three DLQ-emulation Javadocs) land in a **separate commit after the bytecode evidence exists**, so each sentence cites measured facts (which image, which jars, how many members). Merge 1 carries only the anchor and exemption bookkeeping, as Phase 21 D-03 did.

### Anchors gate range and bookkeeping (DEPS-03, SYNC-11, SYNC-12, DEBT-15)
- **D-15:** The **standing gate command Phases 26–29 inherit is the tail gate**: `verify-merge-invariants.sh <merge-2 SHA> HEAD` with `PRE_TAG=pre-v1.7.0-tail`, `UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc` and `CONFLICTED_PATHS_FILE` pointing at the tail's conflicted-paths record. It checks merge 2's provenance, which the gg3 command never does, and walks a short range. The **cumulative gate** `verify-merge-invariants.sh 54e63648 HEAD` (default `PRE_TAG=pre-v1.7.0-sync`, needs the merge-1 exemption rows) runs **twice in the milestone**: once in this phase for DEPS-03, and once more at the Phase 29 release gate. Both commands, with their env overrides, are written into the merge checklist and STATE so later phases copy rather than re-derive them. — **Reversibility:** reversible — the cumulative command keeps working at any time because the exemption rows stay in the tree.
- **D-16:** DEBT-15 **shares the existing `slf4j` version key**: add only a library row `slf4j-reload4j = { module = "org.slf4j:slf4j-reload4j", version.ref = "slf4j" }` and use `implementation(libs.slf4j.reload4j)`. After merge 1 the key is already `2.0.20`, the same as the literal. One Dependabot PR then moves `slf4j-api`, `slf4j-simple` and `slf4j-reload4j` together, the fork adds no new `[versions]` key to the conflict-prone block, and the anchor row moves from the `build.gradle.kts` literal to the catalog library line in the same commit. DEBT-15 lands after merge 2 so it cannot widen the catalog conflict.
- **D-17:** The SYNC-12 baseline is marked by a **local, never-pushed tag** (for example `baseline-v1.7.0_gg4`) at the tip of the two green `--rerun` runs, **plus the SHA in STATE.md**. Later phases can `git diff baseline-v1.7.0_gg4` to show what they changed, and Phase 29 names the baseline rather than copying a 40-hex string.

### Claude's Discretion

- The DEBT-13 reader design: a small character scanner over `"…"`, `"""…"""`, `'…'`, `//` and `/* */` is the obvious shape; keep it line-level with no Kotlin parser, per Phase 17 D-06 and Phase 21 D-06. Fixtures are inline Java text blocks.
- The DEBT-14 regex and layout details (trailing-comment `runs-on`, commented-out or guarded fork-count text, alternative `dependabot.yml` layouts, fixture list derived from the production constant), within the red-first rule.
- The exemption-row reasons (name the Dependabot PR number per row), the pre-merge-1 local tag name, and the conflicted-paths file name for the tail (`conflicted-paths-v1.7.0-tail.txt` by analogy).
- How the `verifyConfluentArchive` task reports what it found (list the offending entries; print the entry count as context).
- Where the Connect-member reference list lives and the exact name of the unit test; whether the test reads `build/classes/java/main` or its own classpath.
- The plan split and commit order within the fixed six-step order; the evidence file layout (Phase 17/20/21 precedent: one phase evidence file with the merge dry-runs, gate and self-test output, red fixture runs, `dependencyInsight`, archive compare, javap compare, the `--warning-mode all` run and the OPS-04 record).
- Whether to rehearse each merge and gate in a throwaway worktree first (recommended: yes, as in Phase 21).

### Deferred Ideas (OUT OF SCOPE)

- Uploading the built Confluent archive as a CI artifact. Mentioned as a possible follow-up to the CI-20 step; it is a new capability, not needed for the guard, and would add an upload step and a pin.
- Kotlin DSL deprecations surfaced by Gradle 9.8.1 `--warning-mode all` (DEPS-F3): recorded this phase, cleaned up in a later milestone.
- Dependabot PR #10 (protobuf-java 4.x, DEPS-F2) and PR #4 (client `0.10.0`, DEPS-F1): stay open, not discussed further.
- A cross-check of the image's `connect-api` jar against the Apache `3.9.x` Central artifact: offered as a stronger-evidence option and not chosen; the image jar alone is the reference.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DEBT-13 | `ktsCode` strips comments string- and line-comment-aware; first `build.gradle.kts` edit of the milestone comes after it | Pattern 6 scanner prototype (passes on the real 546-line script and 7 fixtures); Pitfall 1 (`--rerun`) and Pitfall 2 (measured red); red harness table |
| DEPS-02 | One merge commit absorbing `origin/custom_build` (nine Dependabot merges), suite green with the same 18 skips, spotless baseline-equal, `--warning-mode all` recorded | Pattern 1 (clean merge, `gradlew.bat` trap); Pitfalls 10, 11; suite itself not run here (A5) |
| DEPS-03 | Anchors invariant on the merged tree without editing the script; SHA-pinned exemption rows from the gate's own output | X-2, X-4; Pattern 2 (18 rows / 17 SHAs; `anchor_exempt=19 result=PASS`) |
| BLD-15 | Archive intact under Shadow 9.6.1; compare against `v1.7.0_gg3`; `MANIFEST.MF` unchanged apart from versions; multi-release decision recorded | Pattern 3, X-1, compare table, Pitfalls 3, 6, 7, 9 |
| BLD-16 | connect-api 4.4.0 compile-time only; no Connect API classes in the archive; no member absent from the worker's API or added after 4.1.1 | Bytecode reference set (176 entries, 0 missing against image/4.1.1/4.4.0), Pitfall 8, image facts |
| CI-20 | `tests.yaml` builds `createConfluentArchive` and fails on nested jars; pin guard pins the step | Patterns 4 and 5, verification table |
| DEBT-14 | Pin guard: trailing-comment `runs-on`, commented/guarded fork-count text, valid `dependabot.yml` layouts, fixture list from the production constant | Red harness table (9 measured-wrong behaviours with current code) |
| SYNC-11 | Upstream tail as one mechanical merge behind the unedited gate; conflicts exactly `.gitignore` + catalog; resolution table and conflicted paths recorded | Pitfall 5, X-3, tail gate transcript, Pitfall 12 |
| DEBT-15 | `slf4j-reload4j` literal into the catalog with its anchor row moved | Measured: library row sharing `version.ref = "slf4j"` resolves `2.0.20`; order after merge 2 |
| SYNC-12 | Baseline tree: two green `--rerun` runs, guava 33.7.2 on the runtime classpath, FORK-DELTA rows with advisories labelled by source | `dependencyInsight` transcript, Advisories table, X-1 |
| OPS-04 | User creates the `dependencies` label; record the outcome | Baseline measured 2026-10-21: label absent (9 default labels), PRs #4 and #10 both carry the "labels could not be found" comment |
</phase_requirements>

## Summary

Every decision D-01..D-17 in CONTEXT.md is implementable as written, with seven measured corrections (X-1..X-7, below) that change what the plans must say. The Shadow repair (`configurations.add(clickhouseDependencies)` + `from(sourceSets.main.get().output)` + `addMultiReleaseAttribute.set(false)`) produced a flat 7,395-file jar with zero nested `*.jar` entries and a `MANIFEST.MF` byte-identical to the `v1.7.0_gg3` one; the unrepaired Shadow 9.6.1 build on the same tree produced a 158-entry jar holding 25 nested jars (defect reproduced). The entry-by-entry compare against a gg3 archive rebuilt from the tag (7,376 files, inner-jar SHA-256 `c2514401...` identical to `24-COMMAND-EVIDENCE.md` E-04's tip build, so the baseline is reproducible) shows differences only inside artifacts whose resolved version changed. The primary form of the repair is sufficient, so the `zipTree` fallback (D-01) is not needed.

The merge mechanics were rehearsed end to end. Merge 1 (`origin/custom_build` `69cd680b`) is conflict-free, needs exactly one anchor edit (`kafka = "4.4.0"`), and the unedited cumulative gate then reports 18 violations over 17 SHAs, which become 18 exemption rows; with those rows the gate ends `anchor_exempt=19 result=PASS` (10 min wall). Merge 2 (`a6f1df83`) conflicts in exactly `.gitignore` and `gradle/libs.versions.toml` and its tail gate passes in 5 s, but only if the conflicted-paths record and the merge checklist are committed AFTER the merge commit, not inside it (line provenance rejects new files in the merge commit).

The connect-api proof is feasible JDK-only: the compiled connector references 137 distinct `org/apache/kafka` members and 39 `org/apache/kafka` classes (176 entries, including 5 members inherited through connector classes), and every one resolves in the jars copied out of `confluentinc/cp-kafka-connect-base:7.9.7`, in `connect-api`/`kafka-clients` 4.1.1 and in 4.4.0. The same set is derived identically by a pure-Java class-file reader (the shape D-13's `testUnit` needs) and by the `javap -v` + `javap -p -s` script (the shape D-11 needs).

**Primary recommendation:** Execute the CONTEXT six-step order unchanged, and write the plans around these corrections: (1) guava is already shipped, so BLD-15's allow-list is version bumps only; (2) merge commits carry only anchor/exemption bookkeeping, never new record files; (3) the DEPS-03 gate is the default-env cumulative command with MERGE_SHA `54e63648`; (4) every pin-guard/`testUnit` gate that follows an edit to a file the test only reads at runtime needs `--rerun`; (5) `build.gradle.kts` code needs `import java.util.zip.*` (the `java` extension shadows the package); (6) `gradlew.bat` stays "modified" after merge 1 and must never be staged.

## Corrections to premises in CONTEXT.md / ROADMAP.md / research (found by measurement)

| # | Premise in the planning documents | Measured fact | Consequence for the plans |
|---|-----------------------------------|---------------|---------------------------|
| X-1 | "guava and its five small dependencies" are ADDED to the archive (REQUIREMENTS BLD-15, SYNC-11/12, STACK.md section 5, D-05) | The gg3 archive already contains `guava 33.4.6-jre`, `failureaccess 1.0.3`, `listenablefuture 9999.0-empty-to-avoid-conflict-with-guava`, `error_prone_annotations 2.38.0`, `j2objc-annotations 3.0.0` and `org/jspecify/annotations` (all 7,376 files read from `META-INF/maven/**/pom.properties` and class listings). `client-v2` 0.9.6 itself lists all six; `dependencyInsight` on the tail tree shows `com.google.guava:guava:33.4.6-jre -> 33.7.2-jre` with selection reason "By conflict resolution" | The tail OVERRIDES a transitive version, it does not add a library. BLD-15's allow-list is "version bumps" (guava 33.4.6-jre -> 33.7.2-jre, error_prone 2.38.0 -> 2.50.0, j2objc 3.0.0 -> 3.1, jspecify 1.0.0 -> 1.0.1 bytes-identical) with no new paths for the five small deps. FORK-DELTA/rollout wording must say "overrides client-v2's transitive guava", and gg3 shipped a guava inside the advisory's affected range |
| X-2 | Merge 1 anchor edits are "`kafka = \"4.4.0\"` and the slf4j literal" (DEPS-03, success criterion 2) | After merging `69cd680b` the literal is `slf4j-reload4j:2.0.20` and the HEAD anchor row already says `2.0.20` (count 1). Only the `kafka` row fails at the merge commit | Merge 1 edits ONE anchor row. Do not invent a slf4j edit. If `git ls-remote` shows a newer tip, re-measure |
| X-3 | "The merge carries `CONFLICTED_PATHS_FILE` and the merge checklist" (ROADMAP note step 5, D-15) | A new file inside the merge commit fails invariant 1: `PROVENANCE VIOLATION: 2 authored line(s) in .planning/scripts/conflicted-paths-v1.7.0-tail.txt`, `result=FAIL`. Committed in the commit right after the merge, the same gate prints `provenance_paths=3 anchor_commits=2 anchor_rows=69 marker_commits=14 anchor_exempt=0 result=PASS` | Merge 2 commit = resolved `.gitignore` + catalog only. A second commit adds `conflicted-paths-v1.7.0-tail.txt` and `MERGE-CHECKLIST-v1.7.0-tail.md`. Both exist before the gate runs (it reads them from RANGE_END) |
| X-4 | "`PRE_TAG` at the pre-merge rollback tag" for the merge-1 gate (success criterion 2) | Running the gate with MERGE_SHA = merge 1 fails invariant 1 (`18 authored line(s)` in the exemptions file, `1` in `fork-anchors.txt`) because merge 1 legitimately carries bookkeeping. The cumulative command with MERGE_SHA `54e63648` and default env (`PRE_TAG=pre-v1.7.0-sync`) is the one that passes | DEPS-03's verify command is `.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` with no env overrides, expected last line `invariants: provenance_paths=21 anchor_commits=580 anchor_rows=69 marker_commits=627 anchor_exempt=19 result=PASS` on the merge-1 tree (counts grow with later commits; `anchor_exempt=19` and `result=PASS` are the stable signals). The pre-merge tag created in pre-flight is a rollback tag only |
| X-5 | STACK.md: the self-test needs its `4.1.1` needles edited (lines 138, 156, 167) | The needles run against the Dependabot commit's own tree (`6473e24f`), not HEAD. Unedited self-test on the merged tree: 16 of 16 cases PASS, `selftest: no failures` | No self-test edit. Run it in the background with each gate (12 min) |
| X-6 | `build.gradle.kts` gets a `java.util.zip` based task | `java.util.zip.ZipFile(...)` in the script fails with `Unresolved reference 'util'` / `'ZipFile'` because the `java` project extension shadows the package | Add `import java.util.zip.ZipFile` and `import java.util.zip.ZipInputStream` at the top of the script |
| X-7 | Guava advisory "published 2026-10-16", identity from secondary sources only (STACK.md, Pitfall 4) | NVD and OSV both carry `CVE-2026-102554` / `GHSA-xxph-c9ww-hj94`, published 2026-10-09; the Guava 33.7.2 release notes (2026-09-29) link both ids. NVD (CNA `cve-coordination@google.com`) says "versions 4.0 through 33.7.1". OSV's Maven index returns NO vulnerability for `guava 33.4.6-jre` (the OSV record has only a GIT range) | Cite the id now, labelled by source. Do not claim "OSV flags 33.4.6": it does not; NVD-CPE based scanners would |

## Architectural Responsibility Map

This phase has no runtime tiers; the "tiers" are the places a guarantee can live. The map says which one owns each guarantee so the plans do not put a check where it cannot fire.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Archive is flat (no nested jar, no Connect API class) | Gradle build (`verifyConfluentArchive` finalizer of `createConfluentArchive`, D-06/D-07) | CI (`tests.yaml` step builds the archive, D-08); pin guard (`testUnit`) pins step and task text | Fires wherever the archive is built (CI, local, GSD gate). A CI-only shell check would not fire locally |
| Repair cannot silently regress on the next upstream sync | Anchor gate (`fork-anchors.txt` positive + negative row, D-04) | FORK-DELTA row | Upstream `a6f1df83` still ships `from(clickhouseDependencies, sourceSets.main.get().output)`; the 0-count row fails the merge gate before CI |
| Connector references only worker-available Kafka API | `testUnit` Docker-free test over compiled classes (D-13) | Committed bless script run against the worker jars (D-11) | The test is the standing guard; the script is the one-time proof against the real image jars |
| Merge provenance, anchors, conflict record | `verify-merge-invariants.sh` (never edited) | Exemption rows + `conflicted-paths-*.txt` | Gate reads RANGE_END's tree for the record files, each commit's own tree for anchors |
| Pin-guard reader correctness (comments, strings, YAML layouts) | `TestsWorkflowPinGuardUnitTest` static rule methods | Inline Java text-block fixtures | Line-level scanners, no Kotlin/YAML parser (Phase 17 D-06, Phase 21 D-06) |
| Production Connect API at runtime | Kafka Connect worker (`cp-kafka-connect-base:7.9.7`, Java 17.0.19) | - | The archive must not ship it; the worker's parent classloader supplies it |

## Standard Stack

No new library is introduced. The phase moves existing pins and adds JDK-only tooling.

### Core
| Library / tool | Version | Purpose | Why standard here |
|----------------|---------|---------|-------------------|
| Gradle wrapper | 9.8.1 (from merge 1) | Build | Wrapper jar SHA-256 `3b8a2577...e98` equals Gradle's published `gradle-9.8.1-wrapper.jar.sha256` [VERIFIED: services.gradle.org, curl 2026-10-21]; `distributionSha256Sum` is absent (optional hardening, not this phase) |
| Shadow `com.gradleup.shadow` | 9.6.1 (catalog `shadow`) | `confluentJar` | Requires the `configurations.add(...)` repair; `shadow { addShadowJarToAssembleLifecycle.set(false) }` compiles and works under Gradle 9.8.1 [VERIFIED: build in `/tmp/p25/w1`, `assemble --dry-run` no longer lists `:shadowJar`] |
| Python 3.14 `python3 -I` | system | Archive compare and javap-based bless script (D-05, D-11) | Already the house tool (E-04 `cmp.py`); no new dependency |
| JDK `javap` (JDK 21 here, Temurin 17 in CI) | - | Constant-pool extraction (D-11) | Reads class major 61 |
| JUnit 5.14.4 (catalog `junitJupiter`) + a pure-Java class-file reader | - | D-13 `testUnit` | No ASM, no new test dependency |

### Supporting (versions after absorption, resolved on `runtimeClasspath` / `clickhouseDependencies`)
| Library | Version in gg3 archive | Version after merges | Note |
|---------|-----------------------|----------------------|------|
| guava | 33.4.6-jre | 33.7.2-jre | Override of client-v2's transitive version (X-1) |
| error_prone_annotations | 2.38.0 | 2.50.0 | via guava 33.7.2 |
| j2objc-annotations | 3.0.0 | 3.1 | via guava 33.7.2 |
| jspecify | 1.0.0 | 1.0.1 | classes byte-identical |
| failureaccess / listenablefuture | 1.0.3 / 9999.0-empty-... | unchanged | |
| jackson-core / -databind | 2.22.3 | 2.22.4 | merge 1 (#14) |
| gson | 2.13.1 | 2.14.0 | merge 1 (#13) |
| httpclient5 | 5.6.2 | 5.6.5 | merge 1 (#12) and tail |
| httpcore5 / httpcore5-h2 | 5.4.3 | 5.4.4 | |
| lz4-java (`at.yawk.lz4`) | 1.12.0 | 1.12.0 | fork value kept over upstream 1.11.4 |
| commons-lang3 | 3.20.0 | 3.20.0 | shipped value is client-v2's transitive; catalog 3.21.0 is test-fixtures only |

### Alternatives Considered
| Instead of | Could use | Tradeoff |
|------------|-----------|----------|
| `configurations.add(clickhouseDependencies)` | `from(provider { clickhouseDependencies.files.map { zipTree(it) } })` | Not needed: the primary form produced zero entries outside the version-bump set. Keep as documented fallback only |
| Pure-Java reader in the D-13 test | `javap` subprocess from the test | Pure Java avoids a tool dependency on the test JVM and produced the identical 176-entry set |
| `verifyConfluentArchive` as a Gradle task | A CI-only shell step | Locked by D-06 |

**Installation:** none. All edits are to existing files.

**Version verification:** versions above were read from `META-INF/maven/**/pom.properties` inside the built archives and from `dependencyInsight`/`dependencies --configuration clickhouseDependencies` on the merged tree, not from training data.

## Package Legitimacy Audit

The phase installs no new external package. The `gsd-tools package-legitimacy check` seam supports only npm, pypi and crates (`Usage: ... --ecosystem <npm|pypi|crates>`), so it cannot audit Maven coordinates. The audit below is therefore a manual one and every row was resolved by Gradle from Maven Central / the Gradle plugin portal during the measurement builds.

| Package | Registry | Age / provenance | Source repo | Verdict | Disposition |
|---------|----------|------------------|-------------|---------|-------------|
| `com.google.guava:guava:33.7.2-jre` | Maven Central | library; release tag v33.7.2 published 2026-09-29 (GitHub API) | github.com/google/guava | OK (manual) | Approved; already a transitive of client-v2 |
| `com.gradleup.shadow:9.6.1` | Gradle plugin portal | merged as Dependabot #11 by the user | github.com/GradleUp/shadow | OK (manual) | Approved (already merged on origin) |
| httpclient5 5.6.5, jackson 2.22.4, gson 2.14.0, connect-api 4.4.0, slf4j 2.0.20, commons-lang3 3.21.0 | Maven Central | Dependabot PRs #6-#15 merged by the user | upstream projects | OK (manual) | Approved |

**Packages removed due to SLOP verdict:** none. **Packages flagged SUS:** none. No `[ASSUMED]` package names.

## Architecture Patterns

### Data flow of the phase (what feeds what)

```
                 git ls-remote origin custom_build (69cd680b, re-read before merge 1)
                                  |
 [1] DEBT-13 reader fix + testUnit gate (--rerun)  --> first commit on sync/v1.7.0
                                  |
 [2] pre-flight: delete refs/upstream/{main,v1.6.0}; SELECT version() of latest; local rollback tag
                                  |
 [3] REHEARSE merge 1 in a throwaway worktree: merge, ONE anchor edit, commit, run the unedited cumulative gate
        --> stderr --> 18 exemption rows (they name only origin commits, so they are identical in the real merge)
     MERGE 1 for real: git merge --no-commit --no-ff origin/custom_build
        + ONE anchor edit (kafka = "4.4.0") + the 18 rows --commit--> M1   (no .planning commit before it)
        + real cumulative gate must print anchor_exempt=19 result=PASS
                                  |
 [4] Shadow repair (confluentJar, addMultiReleaseAttribute, shadow{...}) + 2 anchor rows + FORK-DELTA rows
        |-- build gg3 baseline from tag in w0 --+
        |-- build tip archive ------------------+--> compare script + allow-list --> BLD-15 evidence
        |-- docker create/cp image jars --> javap script --> reference list --> testUnit Connect-member test (BLD-16)
        |-- verifyConfluentArchive task + tests.yaml step --> pin-guard rules (red first) (CI-20)
        |-- DEBT-14 edits in the same pin-guard file
        '-- one full suite run (latest x V2, Docker)
                                  |
 [5] MERGE 2  git merge --no-commit a6f1df83...  resolve .gitignore (union) + catalog (fork values + guava, minus httpclientTest)
        --commit--> M2 (nothing else in it);  next commit R2 adds conflicted-paths-v1.7.0-tail.txt + MERGE-CHECKLIST-v1.7.0-tail.md
        tail gate (PRE_TAG=pre-v1.7.0-tail ...) --> PASS
                                  |
 [6] DEBT-15 (catalog library row + implementation(libs.slf4j.reload4j) + anchor move, one commit)
        dependencyInsight (guava 33.7.2-jre) ; two full --rerun suite runs ; local tag baseline-v1.7.0_gg4 + SHA in STATE.md
```

### Recommended file layout for new artifacts

```
.planning/scripts/
  compare-confluent-archives.py      # D-05: python3 -I; args: archive-a.zip archive-b.zip allowlist.txt
  confluent-archive-allowlist-25.txt # glob<TAB>reason rows; every row must be used
  connect-api-refs.py                # D-11: javap -v extraction + javap -p -s resolution against jars
  connect-api-refs.txt               # D-13: blessed reference list (M owner.name:desc / C owner), 176 lines today
  conflicted-paths-v1.7.0-tail.txt   # 2 lines: .gitignore, gradle/libs.versions.toml
src/test/java/com/clickhouse/kafka/connect/ci/
  TestsWorkflowPinGuardUnitTest.java # DEBT-13, DEBT-14, CI-20 rules (only edits of this file in the milestone)
  ConnectApiReferencesUnitTest.java  # D-13 (name is planner's call); reads the list via src/test/resources or the scripts path
.planning/phases/25-.../25-COMMAND-EVIDENCE.md
```

### Pattern 1: Merge 1 (measured recipe)

```bash
git ls-remote origin custom_build                    # must print 69cd680bf548bfb0329142752d37f8a6c3190c26 again; re-absorb if not
git merge --no-commit --no-ff origin/custom_build    # "Automatic merge went well; stopped before committing"
# edit the ONE anchor row, then stage ONLY the intended paths (never `git add -A`, never `commit -a`):
sed -i '' 's|^\(gradle/libs.versions.toml\tkafka = \)"4.1.1"|\1"4.4.0"|' .planning/scripts/fork-anchors.txt   # macOS sed
git add .planning/scripts/fork-anchors.txt && git commit        # merge commit M1
```
[VERIFIED: dry run in `/tmp/p25/w1`] After the merge `git status` shows `MM gradlew.bat`: the Dependabot commits stored `gradlew.bat` with CRLF in the index while `.gitattributes` says `*.bat text eol=crlf`, so the working file is "modified" forever and `git add`/`commit -a` would renormalise the blob and create an authored-line violation. Leave it alone; `git stash` and `git checkout -- gradlew.bat` do not clear it either.

### Pattern 2: exemption rows generated from the gate's own stderr

Run the unedited cumulative gate on the rehearsal merge (anchor edit only, no rows yet), then turn every stderr line `ANCHOR VIOLATION: <short> <path> '<pattern>' expected N got M (<id>)` into one TSV row `<full 40-hex SHA>\t<path>\t<pattern>\t<reason>` (`git rev-parse <short>` expands the 8-hex prefix). Measured on M1: 18 lines, 17 distinct SHAs:
- 13 SHAs fail only `gradle/libs.versions.toml 'kafka = "4.1.1"'`: `69cd680b` (merge #14), `af762b1b` (merge #15), `2555fd2b` (bump slf4j-simple), `de27d2e9` (bump jackson), `d20bae63` (merge #11), `4b9ddf96` (merge #12), `55845c3a` (merge #13), `83de3b78` (bump gson), `dba83534` (bump httpclient5), `5315776f` (bump shadow), `71ac9660` (merge #7), `6e7fbbc7` (merge #6), `75ee14b0` (merge #9);
- `6ad5b80c` (bump connect-api, #9) fails both `build.gradle.kts 'slf4j-reload4j:2.0.17'` and the kafka row;
- `d84272d7` (#8 wrapper), `6d3827a2` (#7 ben-manes), `2ccd7486` (#6 commons-lang3) fail only the slf4j-reload4j row.

The pattern text is copied from the gate's stderr (it is the commit's own copy of the anchor row, which says `2.0.17` for the four origin-lineage commits even though HEAD says `2.0.20`). Reason column: name the Dependabot PR number per row. The rows live in `.planning/scripts/fork-anchor-exemptions.txt` and are read from RANGE_END's tree. Result with the 18 rows appended: `invariants: provenance_paths=21 anchor_commits=580 anchor_rows=69 marker_commits=627 anchor_exempt=19 result=PASS` (19 = 18 new + the existing `6473e24f` row), wall time 10 min 1 s. Run it in the background; the Bash tool's 120 s default will otherwise detach it.

CONTEXT and ROADMAP put the exemption rows in the merge commit itself. That is workable: the rows name only origin commits (their SHAs are fixed by the `git ls-remote` check), so they can be generated in a throwaway rehearsal worktree and written into the real merge before it is committed; the gate does not apply line provenance to M1 (it is not the gate's MERGE_SHA, see X-4), so bookkeeping lines in M1 are fine. The anchor edit is the part that MUST be inside M1 (the merge commit is itself evaluated against its own anchors copy and cannot be exempted). I verified the rows are a pure function of the origin commits: the same 18 lines came out of the gate on the pre-exemption merge (`gate1.err`) and the generator output was used unchanged on the amended merge (`anchor_exempt=19 result=PASS`).

### Pattern 3: the Shadow repair (measured)

```kotlin
tasks.register<ShadowJar>("confluentJar") {
    archiveClassifier.set("confluent")
    // Shadow 9: from(<jars>) copies jars as-is; only `configurations` are unzipped.
    configurations.add(clickhouseDependencies)
    from(sourceSets.main.get().output)
    addMultiReleaseAttribute.set(false)
}
// ... at top level, near the confluentJar block:
shadow {
    addShadowJarToAssembleLifecycle.set(false)
}
```
- Inside the lambda `configurations` is `ShadowJar.configurations` (the build succeeded and produced the flat jar).
- Do NOT repeat the anchor text `configurations.add(clickhouseDependencies)` in a comment: the anchor counts matching LINES and must stay 1.
- Anchor rows (D-04), both verified against the repaired file: `build.gradle.kts<TAB>configurations.add(clickhouseDependencies)<TAB>1<TAB>BLD-15` (count 1) and `build.gradle.kts<TAB>from(clickhouseDependencies, <TAB>0<TAB>BLD-15` (note the trailing space; count is 1 in `v1.7.0_gg3` and in `a6f1df83`, 0 in the repaired file).
- `assemble --dry-run` before the `shadow {}` block lists `:shadowJar` (the `*-all.jar` of `runtimeClasspath`, which would contain connect-api); after it, `:jar` then `:assemble` only [VERIFIED].
- Without `addMultiReleaseAttribute.set(false)` the manifest gains `Multi-Release: true` [VERIFIED: rebuilt with the line removed]. With it, the manifest is the 3-line gg3 manifest. The `META-INF/versions/{9,11,17,21}/**` classes are in the archive either way (gg3 had them); the attribute only controls activation.

### Pattern 4: `verifyConfluentArchive` (measured prototype, positive and negative control)

```kotlin
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
// ...
tasks.register<Zip>("createConfluentArchive") {
    // existing body ...
    finalizedBy("verifyConfluentArchive")
}
tasks.register("verifyConfluentArchive") {
    group = "verification"
    description = "Fails if the Confluent archive nests jars or bundles the Kafka Connect API"
    val archive = tasks.named<Zip>("createConfluentArchive").flatMap { it.archiveFile }
    inputs.file(archive)
    doLast {
        val violations = mutableListOf<String>()
        var innerEntries = 0
        ZipFile(archive.get().asFile).use { outer ->
            outer.stream().filter { it.name.contains("/lib/") && it.name.endsWith(".jar") }.forEach { jar ->
                ZipInputStream(outer.getInputStream(jar)).use { zin ->
                    generateSequence { zin.nextEntry }.forEach { e ->
                        innerEntries++
                        if (e.name.endsWith(".jar")) violations += "nested jar: ${e.name}"
                        if (e.name.startsWith("org/apache/kafka/connect/")) violations += "Connect API class: ${e.name}"
                    }
                }
            }
        }
        if (innerEntries == 0) violations += "no inner jar entries read from ${archive.get().asFile}"
        if (violations.isNotEmpty()) {
            throw GradleException("Confluent archive check failed (${violations.size} violations of $innerEntries entries):\n" + violations.take(20).joinToString("\n"))
        }
        println("verifyConfluentArchive: $innerEntries inner entries, no nested jar, no Connect API class")
    }
}
```
- Positive: `verifyConfluentArchive: 7798 inner entries, no nested jar, no Connect API class` (7,395 files + 403 directory entries). Negative control (repair reverted to `from(clickhouseDependencies, ...)`): `Execution failed for task ':verifyConfluentArchive'. > Confluent archive check failed (25 violations of 158 entries): nested jar: client-v2-0.9.6.jar ...`. Wall time of `createConfluentArchive` incl. verification: 1 s warm, 32 s cold compile.
- The rule strings contain no `/*` sequence on purpose; the broader prefix `org/apache/kafka/` (kafka-clients too, D-12) has 0 entries in the repaired archive and is a stricter alternative to D-07's `org/apache/kafka/connect/` (planner's call; zero cost today).
- Add a positive floor so an EMPTY inner jar also fails (the `innerEntries == 0` line above does that; a check that `com/clickhouse/kafka/connect/ClickHouseSinkConnector.class` and `com/clickhouse/client/api/Client.class` exist is the stronger form).
- `tasks.named<Zip>(...)` at configuration time and `archive.get()` only inside `doLast` is the Gradle 9.x-safe shape (no `project` access at execution time, which Gradle 9.6 deprecates).

### Pattern 5: the CI-20 step and its pin

`tests.yaml` job `build` has steps checkout, JDK, Gradle, `Report test fork count` (line 51), `Compile integration-test sources` (line 66), `Run tests` (line 69). D-08 inserts a named step between 66 and 69, e.g. `- name: Build and verify Confluent archive` with `run: ./gradlew createConfluentArchive`. The pin rule reuses the `forkCountStepProblem` shape: find the step by name (`FORK_STEP`-style pattern), walk lines while indentation exceeds the step's, require a line matching `^(?!\s*(?:echo|printf)\b).*\./gradlew\s.*\bcreateConfluentArchive\b.*$`. Generalise the existing `GRADLEW_REPORT`/`FORK_STEP` patterns into a helper taking (step name, task name) instead of duplicating (DRY, D-09). The kts-side rules: `verifyConfluentArchive` registered (`REGISTER`-style `^tasks\.register\("verifyConfluentArchive"\)`) and `createConfluentArchive`'s block contains `finalizedBy("verifyConfluentArchive")`.

### Pattern 6: DEBT-13 reader (minimal line-level scanner, prototype passes)

One pass over the whole text with these states: raw string `"""..."""` (no escapes, ends at the next `"""`, absorbing a trailing run of quotes), string/char literal with `\` escapes and no span across a newline, `//` to end of line (dropped), `/* */` with nesting depth (Kotlin block comments nest; newlines inside are kept so line numbers survive), everything else copied. Output `lines()`. This replaces `text.replaceAll("(?s)/\\*.*?\\*/", "")` plus the "line starts with `//`" filter in `ktsCode` (`TestsWorkflowPinGuardUnitTest.java:305-309`); it also strips TRAILING `//` comments, which WR-03 needs. About 35 lines, no parser. Measured on the real `build.gradle.kts` (546 lines): 546 lines out, `tasks.register("reportTestForks")` kept, the `// Prevent instantiation` inside the `generateVersionFile` raw string kept, `maven("https://packages.confluent.io/maven/")` kept, the `ShadowJar` block comment gone. Known unsupported case (document it in the Javadoc): quotes nested inside `${...}` templates; the current script has none (`grep -nE '\$\{[^}]*"' build.gradle.kts` is empty).

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---------|-------------|-------------|-----|
| Unpacking dependency jars into the Confluent jar | `zipTree` provider, custom `Copy` | Shadow's `configurations.add(...)` | Measured: zero entries outside the version-bump set; keeps Shadow's duplicate/merge handling |
| Per-entry archive comparison logic in prose | ad-hoc `diff`/`unzip -l` pipelines | One committed `python3 -I` compare script (D-05) with per-entry SHA-256, an allow-list file in which every row must be used, and the nested-jar count | E-04 already needed a throwaway `cmp.py`; the script is reused by Phase 29 |
| Member-reference extraction | a Kotlin/ASM bytecode walker | `javap -v` constant pool (script) and a 60-line `DataInputStream` constant-pool reader (test) | Both produced the identical 176-entry set; no dependency |
| Resolving a member through inheritance | string matching on owners | Walk superclass + interfaces (`javap -p -s` per class in the script; `Class.getDeclared*` in the test) | 5 of the 176 entries are inherited through connector classes (`AbstractConfig.getBoolean/getInt/getList/getString` via `ClickHouseSinkConfig`, field `SinkTask.context`) and are invisible to an owner-prefix filter |
| Conflicted-path record | hand-typed list | Output of `git merge-tree --write-tree --name-only <P1> <P2>` (the gate derives the same list and diffs it) | The gate fails on any mismatch |
| Exemption rows | hand-typed SHAs | Generated from the gate's stderr | Short SHA in stderr, full 40-hex in the file; wrong pattern text silently never matches |
| Kotlin comment/string handling | a regex | The one-pass scanner (Pattern 6) | Regexes cannot know whether `/*` is inside a string; this is the DEBT-13 root cause |

**Key insight:** every "guard" in this phase has a cheaper wrong form that passes today and fails silently later (a count-only anchor, a regex reader, a CI shell step). The measured negative controls (25 nested jars, a red pin guard on a harmless `// lib/*` comment) are what make each guard trustworthy.

## Runtime State Inventory

Omitted: this is not a rename/refactor/migration phase (no identifier is renamed). The only persistent local state touched is git refs and tags, listed under Environment Availability and Pitfall 12.

## Common Pitfalls

### Pitfall 1: `testUnit` is UP-TO-DATE after you edit a file the test only reads at runtime
**What goes wrong:** the pin guard reads `build.gradle.kts`, `tests.yaml` and `dependabot.yml` by path at runtime; none is a declared task input. After editing only `build.gradle.kts`, `./gradlew testUnit --tests '*TestsWorkflowPinGuardUnitTest'` printed `> Task :testUnit UP-TO-DATE` and BUILD SUCCESSFUL even though the same edit makes the test fail.
**How to avoid:** every red-first run and every pin-guard gate uses `--rerun` (`./gradlew testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest'`). Failure signal that proves the run was real: `Tests summary: 26 tests, ...` in the output.
**Warning signs:** a "green" gate with `testUnit UP-TO-DATE`.

### Pitfall 2: the old `ktsCode` mis-parses (DEBT-13) and the Shadow/verify code adds the triggers
**What goes wrong:** measured, a one-line `// the archive holds lib/* entries only` comment placed before `tasks.register("reportTestForks")` makes `reportTestForksReadsMaxParallelForksFromTheTestTask` fail with `tasks.register("reportTestForks") not found in build.gradle.kts outside comments` (26 tests, 25 passed, 1 failed). Same for a string containing `/*` (for example a `"org/apache/kafka/connect/**"` glob), and for a trailing `// see /* here`. A string with only `**/x` is harmless.
**How to avoid:** DEBT-13 is the first commit, with its own `--rerun` gate; the fixtures are committed in the test (Java text blocks) and seen red on the old `ktsCode` first.

### Pitfall 3: the unrepaired Shadow 9.6.1 build is green and wrong
**What goes wrong:** measured on the merged tree: `createConfluentArchive` BUILD SUCCESSFUL, 158 inner entries, 25 nested `*.jar` (client-v2-0.9.6.jar, clickhouse-*, httpclient5-5.6.5.jar, lz4-java-1.12.0.jar, gson, jackson, httpcore5, slf4j-api-2.0.7.jar, commons-*, RoaringBitmap-1.0.6.jar, asm-9.7.jar, guava-33.4.6-jre.jar, failureaccess, listenablefuture, jspecify-1.0.0.jar, error_prone_annotations-2.48.0.jar, j2objc-annotations-3.0.0.jar). A suite run on this tree is green and ships nothing usable. Keep this transcript as the BLD-15 "before".

### Pitfall 4: the merge commit cannot carry record files (X-3) and cannot be the gate's MERGE_SHA when it carries bookkeeping (X-4)
**Failure signals:** `PROVENANCE VIOLATION: N authored line(s) in <path>`. Fix by moving the file to the next commit, not by editing the script.

### Pitfall 5: wrong-side catalog resolution in merge 2
The conflict block covers the whole top of `[versions]`. Resolution table (all verified in the dry run, catalog diff shown in the evidence): `kafka = "4.4.0"  # FORK OVERRIDE (upstream 2.7.0)` keep; `gson = "2.14.0"`, `jackson = "2.22.4"`, `lombok = "1.18.48"`, `lz4Java = "1.12.0"` keep fork; `httpclient = "5.6.5"` identical; DELETE `httpclientTest = "5.6.5"`; ADD `guava = "33.7.2-jre"`. If the resolver keeps "ours" wholesale the `guava` key is lost and `libs.guava` is unknown. The `[libraries]` hunk and `build.gradle.kts` (`implementation(libs.guava)`, `clickhouseDependencies(libs.guava) // temp to override client's with CVE`, `libs.httpclient5.test` -> `libs.httpclient5`) merge without conflict. `.gitignore`: union of both sides (`/bin/ .vscode .gsd` from the fork, `# ecliose .classpath .project .settings/` from upstream, typo included, because provenance needs every line to exist in a parent).

### Pitfall 6: `release_date` makes the outer zip differ
`config/archive/manifest.json` carries `"release_date": "${project.releaseDate}"` (build day) and `"version"`. Rebuilt the same day, `manifest.json` is byte-identical to gg3's; E-04's outer zip SHA (`fc4a0511...`) differs from today's gg3 rebuild (`60963a7f...`) while the inner jar SHA is identical. The compare script must treat `manifest.json` line-wise (ignore `release_date` and `version`) or compare inner jars only for the byte-identity claim.

### Pitfall 7: the baseline must be rebuilt from the tag, same JDK-independent bytes
The gg3 archive rebuilt in `w0` from tag `v1.7.0_gg3` (3376a18f) reproduces E-04's inner-jar SHA-256 `c2514401f76729d17f61bab3d405288801c2b162dba10a58eead3d20e264b36e` and 7,376 files. `6a2a87a5` (the corrected-note republish) differs from `3376a18f` only in docs/`.planning` per the 17-file stat, so the tag is the right baseline.

### Pitfall 8: Docker image arch and digest
Pulled on this arm64 host: `confluentinc/cp-kafka-connect-base:7.9.7`, digest `sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055` (the pull's index digest; the local image is `arm64`). The jars are architecture-independent. Record the digest the pull prints, not a tag. Only `/usr/share/java/kafka/connect-api-7.9.7-ccs.jar` and `.../kafka-clients-7.9.7-ccs.jar` are on the Connect classpath; `confluent-control-center/` holds `-ce` copies that are not.

### Pitfall 9: `Multi-Release: true` appears silently (D-02)
Measured: removing `addMultiReleaseAttribute.set(false)` adds `Multi-Release: true` to the manifest and nothing else. A manifest compare is the only thing that would notice.

### Pitfall 10: spotless is red on baseline
`./gradlew spotlessCheck` fails on both the pre-merge tip and the merged tree with exactly the same 2 files: `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseCloudTest.java` and `.../ClickHouseSinkConnectorIntegrationTest.java` (upstream-owned). "Baseline-equal" (Phase 21 D-14) means this file set must not change; record it with `grep -E '^      src/'` over the failure text.

### Pitfall 11: `--warning-mode all` list (record, do not fix)
On the merged tree under Gradle 9.8.1: `Configuration.setVisible(boolean)` deprecated (removal in Gradle 11); the `com.github.ben-manes.versions` plugin id deprecated (apply `io.github.ben-manes.versions`, new with the 0.66.0 bump); and four Kotlin DSL property-delegate deprecations (`by creating`, `by getting`, `by registering`, `by extra`; removal in Gradle 10). Six distinct warning kinds, no build-script change (DEPS-F3).

### Pitfall 12: ref and tag hygiene
`git for-each-ref refs/upstream` lists `refs/upstream/main` and `refs/upstream/v1.6.0`, both `789a57c2`; `git rev-parse upstream/main` warns `refname 'upstream/main' is ambiguous`. Delete with `git update-ref -d refs/upstream/main` and `git update-ref -d refs/upstream/v1.6.0`. `refs/remotes/upstream/main` is already `a6f1df83...`; `git ls-remote upstream main` agrees and there is no tag past `v1.7.0` upstream. Local tags to create: a merge-1 rollback tag and `pre-v1.7.0-tail` at the pre-merge-2 tip (distinct names; `pre-v1.7.0_gg4` is reserved), and `baseline-v1.7.0_gg4` at the end. None is pushed.

### Pitfall 13: `dependabot` PR #4 shows an owner `@dependabot close` comment
The owner commented `@dependabot close` on PR #4 on 2026-10-16, yet the REST API (`/pulls?state=open`) still lists #4 open on 2026-10-21. Decision stays "open, not ignored" (CONTEXT); do nothing, but do not be surprised if it closes.

## Code Examples

### Archive compare (D-05) - behaviour the script must have (prototype `cmp.py` measured the numbers below)

- Inputs: `a.zip b.zip allowlist.txt`. For each zip open `lib/*-confluent.jar` in memory, map `entry name -> sha256` for FILE entries only (directories excluded; gg3 = 7,376 files, 7,782 entries counting 406 directories).
- Output lines `ONLY-A`, `ONLY-B`, `CHANGED` with the hash pair, then totals and `nested jars in b: N`.
- Exit non-zero if N > 0, or any difference matches no allow-list glob, or an allow-list row matches nothing (stale row).
- Also compare outer entries (`manifest.json` ignoring `release_date`/`version`, `doc/*`, `assets/*`) and print the `META-INF/MANIFEST.MF` diff.
- Print the `META-INF/maven/**/pom.properties` version table of both archives (this is the human-readable "what was bumped" evidence).

Measured result, gg3 (7,376 files) versus merge 1 + repair (7,395 files): `only-a 51 only-b 70 changed 320 same 7005`, and every difference sits in jackson, gson, `org/apache/hc/**`, `org/publicsuffix/**`, `META-INF/maven/**`, `META-INF/versions/**`, `com/google/errorprone/**` or `META-INF/DEPENDENCIES`; zero differences in `com/clickhouse/**`. Versus merge 1 + repair + merge 2 (7,396 files): `only-a 91 only-b 111 changed 1820 same 5465`, nested jars 0, with the groups

| Allow-list glob (reason) | Entries (only-a / only-b / changed) |
|--------------------------|-------------------------------------|
| `com/google/common/**`, `com/google/thirdparty/**` (guava 33.4.6-jre -> 33.7.2-jre) | 1,547 in total across the three kinds |
| `com/google/gson/**` (2.13.1 -> 2.14.0) | 228 |
| `com/fasterxml/jackson/**` (2.22.3 -> 2.22.4) | 147 |
| `org/apache/hc/**`, `org/publicsuffix/**` (httpclient5 5.6.2 -> 5.6.5, httpcore5 5.4.3 -> 5.4.4) | 34 |
| `com/google/j2objc/**` (3.0.0 -> 3.1) | 24 |
| `com/google/errorprone/**` (2.38.0 -> 2.50.0) | 2 |
| `META-INF/maven/<group>/<artifact>/**` for each bumped artifact (pom.properties/pom.xml) | 18 |
| `META-INF/versions/{9,11,17,21}/**` (jackson `fdp/v2_22_3` -> `v2_22_4`, module-info, OSGi) | 16 |
| `META-INF/proguard/*.pro` (guava), `META-INF/DEPENDENCIES`, root `module-info.class` | 6 |

`jspecify` 1.0.0 -> 1.0.1 changes no entry. The version table the script prints (from `pom.properties`): jackson-core/-databind 2.22.3 -> 2.22.4, gson 2.13.1 -> 2.14.0, error_prone_annotations 2.38.0 -> 2.50.0, guava 33.4.6-jre -> 33.7.2-jre, j2objc-annotations 3.0.0 -> 3.1, httpclient5 5.6.2 -> 5.6.5, httpcore5 and httpcore5-h2 5.4.3 -> 5.4.4; unchanged: lz4-java 1.12.0, jackson-annotations 2.22, failureaccess 1.0.3, listenablefuture 9999.0, commons-codec 1.19.0, commons-io 2.20.0, commons-compress 1.28.0, commons-lang3 3.20.0, slf4j-api 2.0.7. The `MANIFEST.MF` of the final archive is identical to gg3's (VERSION is still `v1.7.0_gg3`; at the Phase 29 version bump `Implementation-Version`, `Version.class` and the two classes that inline `Version.ARTIFACT_VERSION` will differ, per E-04).

### Bytecode reference set (D-11/D-13) - pure-Java derivation that reproduced the javap set exactly

```java
// Constant-pool reader: no dependency, JDK 17 source level.
static void parse(byte[] b, Set<String> out, Set<Ref> own) throws IOException {
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
    in.readInt(); in.readUnsignedShort(); in.readUnsignedShort();
    int n = in.readUnsignedShort();
    String[] utf = new String[n]; int[] tag = new int[n], i1 = new int[n], i2 = new int[n];
    for (int i = 1; i < n; i++) {
        int t = in.readUnsignedByte(); tag[i] = t;
        switch (t) {
            case 1 -> utf[i] = in.readUTF();
            case 3, 4 -> in.readInt();
            case 5, 6 -> { in.readLong(); i++; }                       // long/double take two slots
            case 7, 8, 16, 19, 20 -> i1[i] = in.readUnsignedShort();
            case 9, 10, 11, 12, 17, 18 -> { i1[i] = in.readUnsignedShort(); i2[i] = in.readUnsignedShort(); }
            case 15 -> { in.readUnsignedByte(); i1[i] = in.readUnsignedShort(); }
            default -> throw new IOException("tag " + t);
        }
    }
    for (int i = 1; i < n; i++) {
        if (tag[i] == 7) { /* Class: strip leading [ and L...; ; keep names starting org/apache/kafka/ as "C name" */ }
        else if (tag[i] == 9 || tag[i] == 10 || tag[i] == 11) {        // Fieldref, Methodref, InterfaceMethodref
            String owner = utf[i1[i1[i]]], name = utf[i1[i2[i]]], desc = utf[i2[i2[i]]];
            // owner org/apache/kafka/ -> "M owner.name:desc"; owner com/clickhouse/kafka/connect/ -> resolve by reflection:
            // walk superclass + interfaces, match getDeclaredMethods/Constructors/Fields by name + descriptor,
            // and add "M <declaring class>.name:desc" when the declaring class is org.apache.kafka.*
        }
    }
}
```
Output format (sorted): `C org/apache/kafka/connect/sink/SinkRecord` and `M org/apache/kafka/connect/sink/SinkConnector.validate:(Ljava/util/Map;)Lorg/apache/kafka/common/config/Config;`. Today: 137 `M` and 39 `C` = 176 lines. The Python path (`javap -v -p -cp build/classes/java/main <107 classes>`, regex on `#n = (Methodref|InterfaceMethodref|Fieldref) #a.#b // owner.name:desc` and `#n = Class #m // name`, then `javap -p -s -cp <jars> <class>` per owner with a superclass/interface walk) yields the same 176 lines; the comparison printed `python 176 java 176, only python: [], only java: []`.

Result of the bless run (all three reference sets, 0 missing classes, 0 missing members for each):

| Reference | Jars | Missing classes | Missing members |
|-----------|------|-----------------|-----------------|
| Worker | `connect-api-7.9.7-ccs.jar` + `kafka-clients-7.9.7-ccs.jar` copied from the image | 0 | 0 |
| 4.1.1 | `connect-api-4.1.1.jar` + `kafka-clients-4.1.1.jar` | 0 | 0 |
| 4.4.0 | `connect-api-4.4.0.jar` + `kafka-clients-4.4.0.jar` | 0 | 0 |

Negative controls that prove the checker can fail: `connect-api-7.9.7-ccs.jar` alone (no kafka-clients) reports 17 missing classes and 36 missing members; `org/apache/kafka/connect/components/ConnectPlugin` (new in 4.3) is absent from the image jars and present in 4.4.0. Class files are major version 61 (107 of 107) and the worker JVM is Temurin 17.0.19, so Java level is not a skew. Gotcha for the script: filter own-owner refs with `com/clickhouse/kafka/connect/`, NOT `com/clickhouse/` (the latter also matches the ClickHouse client library and yields 140 false "unresolved").

### Pin-guard red harness (measured against the committed test class)

Compile the committed `TestsWorkflowPinGuardUnitTest` with `javac -cp <junit-jupiter-api, opentest4j, apiguardian, junit-platform-commons jars>` and call its package-private static rules from a `Harness` in the same package. Results on the CURRENT code (every `WRONG` line is a fixture that must be added red-first):

| Fixture | Wanted | Current rule |
|---------|--------|--------------|
| `runs-on: ubuntu-24.04 # was ubuntu-latest` | accept (WR-01) | rejects: "The floating runner label must not appear outside comments" |
| fork step body `: ./gradlew reportTestForks` | reject (WR-03) | accepts |
| fork step body `false && ./gradlew reportTestForks` | reject | accepts |
| fork step body `x=1 # ./gradlew reportTestForks` | reject | accepts |
| fork step body `echo hi # forks=$(./gradlew -q reportTestForks)` | reject | already rejects (keep as a green control) |
| kts `println(6) // tasks.named<Test>("test").maxParallelForks` inside the task | reject (WR-03) | accepts |
| `exclude-paths:` list whose `-` items sit at the key's indent | accept (IN-01) | rejects: "has no exclude-paths list" |
| `- package-ecosystem: 'github-actions'` (single quotes) | accept (IN-01) | rejects: "Missing '- package-ecosystem: \"github-actions\"'" |
| `- package-ecosystem: "github-actions" # keeps pins current` | accept (IN-01) | rejects (same message) |
| kts `// copies lib/* here` before the task | accept (WR-02/DEBT-13) | rejects: "not found ... outside comments" |

IN-02: `UPSTREAM` (fixtures, test lines ~460-465) duplicates `UPSTREAM_WORKFLOWS` (rule, lines 74-79); build the fixture from the constant (`UPSTREAM = UPSTREAM_WORKFLOWS`, `EXISTING_WORKFLOWS = plus(UPSTREAM, ".github/workflows/tests.yaml")`). The class Javadoc still says "T-14-16 and CI-11 to CI-13 plus CI-15" and should name CI-18..CI-20 too. The existing 26 tests and their `@DisplayName`s stay (Phase 21 D-07).

## Verification Commands for the Planner (turn into `<verify>` with these failure signals)

`workflow.nyquist_validation` is `false` in `.planning/config.json`, so no Nyquist Validation Architecture section is emitted; this table is the equivalent for planning.

| Step / requirement | Command (all Gradle calls prefixed `env -u TESTCONTAINERS_HOST_OVERRIDE`) | Pass signal | Failure signal |
|--------------------|-----------------------------------------------------------------------------|-------------|----------------|
| DEBT-13 red | add fixtures, then `./gradlew testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest'` on the OLD `ktsCode` | `FAILED` for the new fixtures | `UP-TO-DATE` (no `--rerun`) or all green (fixture is wrong) |
| DEBT-13 green | same command after the scanner | `Tests summary: N tests, N succeeded, 0 failed, 0 skipped` | any `FAILED` |
| Pre-flight | `git for-each-ref refs/upstream` | empty output | any line |
| Pre-flight | `git ls-remote origin custom_build` | `69cd680bf548bfb0329142752d37f8a6c3190c26` | different SHA: re-absorb with a fresh exemption set |
| Pre-flight | `docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local -q "select version()"` | recorded (here `26.9.19.9`, image `bdb5c911a23d`, created 2026-10-14) | - |
| Merge 1 shape | `git merge-tree --write-tree HEAD origin/custom_build; echo $?` before merging | `0` | `1` / conflict names |
| Merge 1 | `git rev-list --parents -n1 HEAD` | two parents, second `69cd680b...` | one parent |
| DEPS-03 | `.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (background, ~10 min) | last line `... anchor_exempt=19 result=PASS` | any `ANCHOR VIOLATION` / `PROVENANCE VIOLATION` |
| Self-test | `.planning/scripts/verify-merge-invariants-selftest.sh` (background) | every case `PASS` (see Self-test note) | any `FAIL` |
| BLD-15 | `./gradlew clean createConfluentArchive`; compare script vs a gg3 archive built in a `v1.7.0_gg3` worktree | `nested jars in b: 0`, every difference allow-listed, `MANIFEST.MF` diff empty | `nested jars in b: >0` or an un-allow-listed path |
| CI-20 task | `./gradlew createConfluentArchive` | `verifyConfluentArchive: 7798 inner entries, no nested jar, no Connect API class` (count moves with versions) | `Confluent archive check failed (N violations of M entries)` |
| CI-20 pin | `./gradlew testUnit --rerun --tests '*TestsWorkflowPinGuardUnitTest'` with fixtures: missing step, renamed task, dropped `finalizedBy` | red on each fixture first, then green | green on a fixture that must reject |
| BLD-16 | bless script against image jars, 4.1.1 jars, 4.4.0 jars | `missing classes 0`, `missing members 0` for all three | any non-zero |
| BLD-16 standing test | `./gradlew testUnit --rerun --tests '*<ConnectApiReferences test>'` | green; red when a new `org.apache.kafka` member is referenced | - |
| Anchors | `grep -cF -e 'configurations.add(clickhouseDependencies)' build.gradle.kts` / `grep -cF -e 'from(clickhouseDependencies, ' build.gradle.kts` | `1` / `0` | other counts |
| Merge 2 shape | `git merge-tree --write-tree --name-only HEAD refs/remotes/upstream/main` | second line block lists exactly `.gitignore` and `gradle/libs.versions.toml` | any other path |
| SYNC-11 tail gate | `PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh <M2> HEAD` | `result=PASS` (measured: 5 s, `provenance_paths=3 anchor_commits=2 anchor_rows=69 marker_commits=14 anchor_exempt=0`) | `PROVENANCE VIOLATION`, `CONFLICTED-PATHS MISMATCH` |
| SYNC-11 cumulative gate again (Phase 29 repeats it) | `.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` on the merge-2 tree (measured: merge 1 + 18 rows + Shadow repair + merge 2) | `invariants: provenance_paths=21 anchor_commits=582 anchor_rows=69 marker_commits=641 anchor_exempt=19 result=PASS` (the 12 upstream tail commits carry no anchors file and add 0 to `anchor_commits`) | any violation |
| SYNC-12 guava | `./gradlew dependencyInsight --configuration runtimeClasspath --dependency guava` and the same with `--configuration clickhouseDependencies --dependency com.google.guava:guava` | `com.google.guava:guava:33.7.2-jre` with `By conflict resolution: between versions 33.7.2-jre and 33.4.6-jre`; `33.4.6-jre -> 33.7.2-jre` under client-v2 | 33.4.6-jre selected |
| DEBT-15 | `./gradlew dependencyInsight --configuration runtimeClasspath --dependency slf4j-reload4j` | `org.slf4j:slf4j-reload4j:2.0.20` | version drift or unresolved `libs.slf4j.reload4j` |
| Spotless | `./gradlew spotlessCheck 2>&1 \| grep -E '^      src/'` | exactly the 2 baseline files | a third file |
| Warnings | `./gradlew assemble --warning-mode all` | 6 warning kinds recorded (Pitfall 11) | build-script change required |
| Suite | `./gradlew test --rerun` twice (Docker, `latest` x V2) | green, 18 named skips | red: check the `latest` image date first (STATE carried rule) |

### Self-test (verify-merge-invariants-selftest.sh)
Measured on the merge-2 rehearsal tree (`w2`: merge 1 + 18 exemption rows + Shadow repair + merge 2, unedited self-test): all 16 cases `PASS` (`ST1`, `ST2`, `ST3`, `ST4`, `ST5-empty-file`, `ST5-disabled`, `ST6-other-row`, `ST6-other-commit`, `ST7-other-bld08-row`, `ST7-exempted-row-silent`, `ST8-short-sha`, `ST8-uppercase-sha`, `ST8-missing-reason`, `ST8-empty-reason`, `ST8-extra-field`, `ST9-exact-row`), final line `selftest: no failures`, exit 0, 12 min 12 s wall (run in the background; it was sharing the CPU with the cumulative gate). The `4.1.1` needles are evaluated against the Dependabot commit `6473e24f`'s own tree, not against HEAD, so the bump to `kafka = "4.4.0"` does not break them. No self-test edit is needed; STACK.md's prediction (X-5) was wrong.

## State of the Art

| Old approach | Current approach | When changed | Impact |
|--------------|------------------|--------------|--------|
| Shadow `from(<jars>)` unzips jars (8.x) | `configurations.add(...)` unzips; `from` copies as-is | Shadow 9.0.0 (changelog, PR #1233, per STACK.md) | Silent nested-jar archive; repaired here |
| `assemble` did not build a fat jar | `assemble` depends on `shadowJar` | Shadow 9.0.0; opt-out `addShadowJarToAssembleLifecycle` since 9.4.0 | Stray `*-all.jar` containing connect-api; opt out (D-03) |
| Shadow adds no `Multi-Release` attribute | adds `Multi-Release: true` when an included manifest has it | Shadow 9.0.0 | Opt out (D-02) |
| Guava via client-v2 at 33.4.6-jre | direct `implementation(libs.guava)` + `clickhouseDependencies(libs.guava)` at 33.7.2-jre | upstream `30521f74`, PR #859 | Version override; no new library |
| `httpclientTest` catalog key | removed; `integrationTest` uses `libs.httpclient5` | upstream `bc896506` | Delete the orphan key in merge 2 |

**Deprecated/outdated:** `by creating`, `by getting`, `by registering`, `by extra` delegates (Gradle 10), `Configuration.setVisible` (Gradle 11), ben-manes plugin id `com.github.ben-manes.versions` (apply `io.github.ben-manes.versions`). Recorded, not fixed (DEPS-F3).

## Advisories, labelled by source (re-queried 2026-10-21; use these labels in FORK-DELTA)

| Item | Identity | Source label | Affected / fixed | Severity as reported |
|------|----------|--------------|------------------|----------------------|
| Guava deserialization DoS (`CompactHashMap`, `CompactHashSet`, `MapMakerInternalMap`) | `CVE-2026-102554` = `GHSA-xxph-c9ww-hj94` | NVD record `CVE-2026-102554` (CNA `cve-coordination@google.com`, status "Awaiting Analysis", published 2026-10-09); OSV `CVE-2026-102554` (aliases the GHSA); Guava GitHub release `v33.7.2` notes (2026-09-29) link both ids | NVD text: "Google Guava versions 4.0 through 33.7.1", fixed in 33.7.2; CWE-502, CWE-770 | CVSS 4.0 8.2 HIGH from the CNA (`AV:N/AC:L/AT:P/PR:N/UI:N/VA:H`, a secondary NVD metric). OSV's Maven-ecosystem query for `guava 33.4.6-jre` AND `33.7.2-jre` returns nothing (GIT-range record only) |
| httpclient5 connection leak on a bad `Content-Encoding` (classic client only) | `CVE-2026-64607` = `GHSA-hjcp-jmpx-g3qm` | NVD (CNA `security@apache.org`, "Modified"); OSV (GitHub-reviewed, MODERATE) | "from 5.0-alpha1 through 5.6.2"; OSV fixed `5.6.3`; gg3 shipped `5.6.2` (read from `pom.properties`) | CVSS 3.1 5.3 (`AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L`) |
| httpcore5-h2 (client-v2's declared `5.3.4`) | `CVE-2026-54428` = `GHSA-v3jc-474w-2wm6` | OSV query on `httpcore5-h2 5.3.4` | affected; the build resolves `5.4.4`, which OSV reports clean | - |

Do not write "closes the guava finding on every scanner": OSV/Maven-ecosystem scanners never flagged 33.4.6-jre; NVD/CPE-based ones did. Exposure in this connector is theoretical (no Java deserialization of record data) [ASSUMED: from STACK.md reasoning, not re-verified in code this session]. Keep the explicit `implementation(libs.httpclient5)` pin: without it `client-v2` 0.9.6 requests `httpclient5` `5.4.4`, which is inside the affected range (`dependencyInsight`: `httpclient5:5.4.4 -> 5.6.5`).

## Assumptions Log

| # | Claim | Section | Risk if wrong |
|---|-------|---------|---------------|
| A1 | Gradle runs a `finalizedBy` task even when the finalized task fails (so `verifyConfluentArchive` still reports on a half-built archive) | Pattern 4 | Only the error message quality changes; the guard still fires on any built archive |
| A2 | The D-13 `testUnit` test can locate the compiled main classes through `Path.of("build","classes","java","main")` (project dir as working directory) or through a main class's code source, because `testUnit` uses `sourceSets["test"].runtimeClasspath` which includes the main class directory | Architecture | Test cannot find classes; fall back to the code-source URL. Not executed in this session (no Java test was added to the repo) |
| A3 | The amd64 image of `cp-kafka-connect-base:7.9.7` carries the same `-ccs` jars (only the arm64 variant was pulled; the digest recorded is the index digest the pull printed) | Pitfall 8 | Evidence names an arm64 pull; jars are architecture-independent, but a reviewer may ask for the amd64 jar SHA-256 (`docker pull --platform linux/amd64`) |
| A4 | Exposure to the guava deserialization CVE is theoretical because the connector does no Java deserialization of record data (STACK.md reasoning, not re-checked in code) | Advisories | Rollout-note wording only |
| A5 | The two full `--rerun` suite runs and the "same 18 named skips" were NOT executed here (Docker suite is out of scope for research); the suite state on the merged tree is unmeasured | Verification | Red suite on unchanged code: check the `latest` image date (26.9.19.9, created 2026-10-14) first |
| A6 | Shadow "9.0.0 breaking change / PR #1233" and "`addShadowJarToAssembleLifecycle` since 9.4.0" come from milestone STACK.md; the behaviour was measured here, the version provenance was not re-fetched | State of the Art | None for execution; cite as STACK.md |

## Open Questions

1. **(Resolved) Does the unedited self-test pass on the merged tree?** Yes, 16 of 16 (see "Self-test"); no edit is planned.
2. **Where does the D-13 reference list live?** (`.planning/scripts/connect-api-refs.txt` versus `src/test/resources`)
   - What we know: the test must read it from the working directory; `.planning/` paths are unavailable if the project is ever built from an sdist-like export, `src/test/resources` is on the classpath.
   - Recommendation: `src/test/resources/connect-api-references.txt` (classpath, no path assumptions), the bless script writes to it, CONTEXT leaves the choice to the planner.
3. **Exemption rows: merge commit or follow-up commit?**
   - What we know: the gate needs the rows only at RANGE_END; the anchor edit must be in M1; the rows depend only on the origin commits.
   - Recommendation: follow CONTEXT (rows in M1), generated from a rehearsal gate run, confirmed by a real run showing `anchor_exempt=19 result=PASS`. A follow-up commit is an acceptable fallback and changes nothing for the gate.
4. **Stricter D-07 prefix?** `org/apache/kafka/` (covers kafka-clients, D-12) versus `org/apache/kafka/connect/`; both give 0 matches today.
5. **PR #4 `@dependabot close` comment** (Pitfall 13): no action; recorded only.

## Environment Availability

| Dependency | Required by | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Docker (colima profile `vz`) | image jar copy, `latest` suite | yes | Docker Engine 29.8.2, `colima [profile=vz] is running` | `colima start -p vz` (bare `colima status/start` hits the default profile) |
| `confluentinc/cp-kafka-connect-base:7.9.7` | BLD-16 | pulled (42 s) | digest `sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055`, Temurin 17.0.19, RHEL 8.10 | none needed |
| `clickhouse/clickhouse-server:latest` | suite, `SELECT version()` | local | `26.9.19.9`, id `bdb5c911a23d`, created 2026-10-14 | - |
| JDK with `javap` | D-11 script | yes | OpenJDK 21.0.12.1 (system); Gradle toolchain 17 for builds | Temurin 17 in CI has `javap` |
| Python | compare and bless scripts | yes | 3.14.8 (`python3 -I`) | - |
| `git` merge-tree `--write-tree` | gate | yes | system git | - |
| Network (Maven Central, plugin portal, api.osv.dev, NVD, GitHub REST) | builds, advisories | yes (unauthenticated) | - | NVD/OSV are rate limited; cache the JSON in the evidence |
| `gh` CLI | - | not used (carried rule: no `gh`) | - | GitHub REST via curl for read-only facts |

**Missing dependencies:** none. Background-run any command that can exceed 120 s (cumulative gate 10 min, self-test, suite).

## Security Domain

`security_enforcement` is enabled (absent = enabled). The phase changes build/CI/supply-chain only; no request handling, auth or crypto code.

### Applicable ASVS categories (v4, level 1)
| Category | Applies | Control |
|----------|---------|---------|
| V1 Architecture | yes | Archive guard + anchors keep the shipped artifact shape |
| V5 Input Validation | partial | Compare/bless scripts read untrusted jars/zips: extract into new empty directories, keep scripts elsewhere, `python3 -I`, pass paths as arguments (task rule). The Gradle task reads entries in memory, never extracts to disk |
| V10 Malicious Code / V14 Configuration (dependencies) | yes | SHA-pinned actions already enforced by the pin guard; wrapper jar checksum verified against Gradle's published value; exemptions are SHA-pinned, never wildcards |
| V2, V3, V4, V6, V7-V9 | no | No auth, session, access control, crypto or data-protection code is touched |

### Known threat patterns
| Pattern | STRIDE | Mitigation |
|---------|--------|------------|
| A dependency bump silently changes the shipped artifact (nested jars) | Tampering | `verifyConfluentArchive` + CI step + pin guard (CI-20) |
| Vulnerable transitive pinned in a vendor pom (guava 33.4.6-jre, httpclient5 5.4.4 via client-v2) | Elevation/DoS | Explicit direct pins (`libs.guava`, `libs.httpclient5`), `dependencyInsight` evidence |
| Slopsquatted / unexpected package | Spoofing | No new package; every coordinate already resolved by the user-merged Dependabot PRs |
| Tampered Gradle wrapper jar | Tampering | SHA-256 equals services.gradle.org value (X-checked); optional `distributionSha256Sum` is a user decision |
| Secrets in logs | Info disclosure | Evidence transcripts contain no credentials; the unauthenticated GitHub/NVD/OSV calls carry none |
| Untrusted archive extraction | Tampering | Fresh empty directories, `-I`, no script run from inside an extracted directory |

## Project Constraints (from CLAUDE.md)

- Tests: JUnit 5 + Mockito; feature tests under `src/test/java`; negative tests required; "Existing tests: only add new tests, never delete/weaken existing ones" - the pin-guard edits keep all 26 test names and assertions.
- Spotless/Google Java Style on files this phase edits; `./gradlew spotlessApply` on edited files only (baseline has 2 pre-existing violations, no tree-wide reformat, Phase 21 D-14).
- Do NOT make commits without being asked / do not change PR text; GSD workflow enforcement: start file changes through a GSD command (this research writes only its own RESEARCH file).
- `docs/FORK-DELTA.md` is the fork-delta record; `VERSION` and `CHANGELOG.md` bumps belong to Phase 29, not here.
- Error messages must be actionable (the Gradle task lists offending entries and counts); no secrets in logs; try-with-resources for `AutoCloseable` (`ZipFile`, `ZipInputStream` use `.use {}`).
- Dependency policy: "DO NOT try to upgrade dependencies without asking the user first" - this phase moves no version beyond what the user already merged and what upstream's tail dictates; PR #4 and #10 stay untouched.
- Memory/carried rules: Docker is colima profile `vz`; `env -u TESTCONTAINERS_HOST_OVERRIDE` on every Gradle call; `./gradlew test` exit 255 may be the intermittent OpenJ9 Mockito GPF (delete the core dump, re-run once); read-only user-made token for CI logs; `dispatch-isolation --json` after `--force-isolation none` breaks the next dispatch.

## Sources

### Primary (HIGH)
- Measurements in this session (commands in the sections above): git worktrees `/tmp/p25/{w0,w1,w2,w3}`, `verify-merge-invariants.sh` runs, Gradle builds under wrapper 9.8.1 / Shadow 9.6.1, `javap`, docker pull/create/cp of `confluentinc/cp-kafka-connect-base:7.9.7`, `unzip`, `python3 -I` compare.
- NVD API `CVE-2026-102554` and `CVE-2026-64607` (services.nvd.nist.gov, fetched 2026-10-21); OSV API (api.osv.dev) `CVE-2026-102554`, `GHSA-hjcp-jmpx-g3qm`, `/v1/query` for the six exact Maven versions.
- GitHub REST: `google/guava` release `v33.7.2`; `ne1r0n/clickhouse-kafka-connect` labels, open pulls and issue comments.
- Gradle: `services.gradle.org/distributions/gradle-9.8.1-wrapper.jar.sha256`.
- Repository files: `build.gradle.kts`, `TestsWorkflowPinGuardUnitTest.java`, `verify-merge-invariants.sh`, `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `.github/workflows/tests.yaml`, `.github/dependabot.yml`, `docs/FORK-DELTA.md`.

### Secondary (MEDIUM)
- [CITED: docs.confluent.io/platform/current/installation/versions-interoperability.html] "Confluent Platform 7.9.x corresponds to Apache Kafka 3.9.x" (WebFetch summary of the page; the jars carry `7.9.7-ccs`, no Apache version string).
- Milestone research under `.planning/research/` (STACK.md for Shadow 9.0.0/9.4.0 provenance, SUMMARY.md, PITFALLS.md).

### Tertiary (LOW)
- None used for a recommendation.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - read from built archives and `dependencyInsight`.
- Architecture (merge mechanics, gate behaviour): HIGH - the gate was run unedited on rehearsal merges; self-test result recorded above.
- Pitfalls: HIGH - each one reproduced with a transcript, except A1-A6 in the Assumptions Log.

**Research date:** 2026-10-21
**Valid until:** 2026-10-28 for the origin tip and upstream tip (re-read `git ls-remote` immediately before each merge); 30 days for the build-mechanics findings.

