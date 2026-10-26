# Phase 25: Dependency Absorption & Upstream Mini-Sync - Context

**Gathered:** 2026-10-21
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase produces the post-absorption baseline tree that Phases 26–29 build, test and gate on. In the roadmap's fixed order it:

- fixes the pin-guard reader (`TestsWorkflowPinGuardUnitTest.ktsCode`) so it survives a `/*` inside a string or a `//` comment, before any authored `build.gradle.kts` edit (DEBT-13);
- merges `origin/custom_build`'s nine Dependabot commits (tip `69cd680b`, re-read with `git ls-remote` immediately before) as one merge commit that itself carries the `fork-anchors.txt` edits and the SHA-pinned exemption rows (DEPS-02, DEPS-03);
- repairs `confluentJar` under Shadow 9.6.1 and proves the archive intact against `v1.7.0_gg3` (BLD-15), proves `connect-api` 4.4.0 compile-time only against the production worker `cp-kafka-connect-base:7.9.7` (Kafka 3.9) (BLD-16), and adds a CI guard so an archive of nested jars can never be published unnoticed (CI-20);
- closes the remaining pin-guard advisories in the same test file (DEBT-14);
- merges upstream's untagged tail `a6f1df839c079180bc7c343b5823f205e204dafc` as a tagless mini-sync behind the unedited `verify-merge-invariants.sh` (SYNC-11);
- moves the `slf4j-reload4j` literal into the version catalog (DEBT-15);
- records two green `--rerun` suite runs on `latest` × V2 as the baseline, with guava on the runtime classpath and FORK-DELTA rows for guava and httpclient5 (SYNC-12);
- presents OPS-04 (the user creates the `dependencies` label in the GitHub UI) at the start of execution.

No connector behaviour changes. Nothing is pushed. `origin/custom_build` is frozen from merge 1 until publication.

Requirements: DEBT-13, DEPS-02, DEPS-03, BLD-15, BLD-16, CI-20, DEBT-14, SYNC-11, DEBT-15, SYNC-12, OPS-04.

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

</domain>

<decisions>
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and acceptance
- `.planning/ROADMAP.md` § "Phase 25: Dependency Absorption & Upstream Mini-Sync" — goal, success criteria 1–5, the research flag and the six-step order note; § "Structure Notes" (files shared across phases: `build.gradle.kts`, `gradle/libs.versions.toml`, `tests.yaml`, the pin guard, FORK-DELTA rows, anchors, exemptions); § "Sync Mechanics".
- `.planning/REQUIREMENTS.md` — DEPS-02, DEPS-03, BLD-15, BLD-16, CI-20, SYNC-11, SYNC-12, DEBT-13, DEBT-14, DEBT-15, OPS-04; the scope decisions of 2026-10-21; Out of Scope (PR #4, PR #10, no script edit, no nightly edit).
- `.planning/STATE.md` § Accumulated Context → Decisions and § Blockers/Concerns — the carried rules (`env -u TESTCONTAINERS_HOST_OVERRIDE`, `--rerun`, one branch per SHA, no `gh`, the dispatch-isolation quirk, the Dependabot facts).
- `.planning/PROJECT.md` — milestone framing and Key Decisions.

### Research for this phase
- `.planning/research/SUMMARY.md` — § Baseline and merge mechanics, § Dependencies (Shadow 9 defect, pin-guard order, connect-api, guava and httpclient5 advisories, Gradle 9.8.1, `-all.jar`), § Phase 25, § Gaps.
- `.planning/research/STACK.md` — § 2 (Shadow 9 repair shape, `addMultiReleaseAttribute`, the archive compare recipe), § 3 (connect-api 4.4.0 and the javap approach), § 5 (upstream tail: guava CVE identity, the catalog conflict table, lz4 and httpclient5 advisories), § 6 (OSV state of the shipped set).
- `.planning/research/PITFALLS.md` — pitfalls 1 (nested jar), 2 (anchor violations), 3 (wrong-side catalog resolution), 4 (unsupported CVE claim), 9 (connect-api skew), 11 (pin-guard misread), 16 (`-all.jar`), 17 (wrapper checksum).
- `.planning/research/ARCHITECTURE.md` — merge order and conflict surface.

### Prior decisions carried
- `.planning/milestones/v1.7.0_gg3-hardening-phases/21-dependabot-integration-ci-hygiene/21-CONTEXT.md` — D-01..D-04 (exemption list: exact row at exact SHA, no script edit, bookkeeping edits inside the merge commit), D-05..D-10 (pin guard: fixtures, extraction, red-first, step-by-name rules), D-13 (OPS answers in the user's own words), D-14 (baseline-equal spotless).
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-CONTEXT.md` — D-07 correction (no release asset; the gg3 archive is built from the tag), D-08 (artifact diff in throwaway worktrees), D-10 (freeze is checked, not trusted), D-11 (the `dependencies` label reminder).
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-COMMAND-EVIDENCE.md` E-04 — the 7,376-entry gg3 archive baseline and the compare procedure that BLD-15 extends.
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md` — the carried 21-REVIEW advisories WR-01, WR-02, WR-03, IN-01, IN-02, IN-04 with their re-open triggers (DEBT-13, DEBT-14, DEBT-15).
- `.planning/milestones/v1.7.0_gg3-hardening-phases/21-dependabot-integration-ci-hygiene/21-REVIEW.md` — the finding texts for WR-01..WR-03, IN-01, IN-02, IN-04.

### Merge mechanics and gate files
- `.planning/scripts/verify-merge-invariants.sh` — invariant 0 (shape, `PRE_TAG`, `UPSTREAM_REF`), invariants 1–2 (provenance, `CONFLICTED_PATHS_FILE`), invariant 3 (anchors, exemptions). Read, never edited.
- `.planning/scripts/verify-merge-invariants-selftest.sh` — ST cases; run with each gate.
- `.planning/scripts/fork-anchors.txt` — rows that move in this phase: `gradle/libs.versions.toml kafka = "4.1.1"` (BLD-08), `build.gradle.kts slf4j-reload4j:2.0.20` (BLD-08, moved again by D-16); rows added: D-04's two Shadow rows, D-16's catalog row.
- `.planning/scripts/fork-anchor-exemptions.txt` — format and the one gg3 row; merge 1 adds the SHA-pinned rows generated from the cumulative gate's stderr (research measured 18 rows over 17 SHAs; re-measure).
- `.planning/MERGE-CHECKLIST-v1.7.0.md` — the shape `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` follows.
- `docs/FORK-DELTA.md` — rows this phase edits: the Connect API sentence (D-14), the `kafka` and `slf4j-reload4j` rows, new rows for the Shadow repair (D-02..D-04), guava and httpclient5 with advisories labelled by source. Raw numbers wait for Phase 29.

### Build, CI and test files touched
- `build.gradle.kts` — `confluentJar` (`:362-365`), `prepareConfluentArchive`/`createConfluentArchive` (`:370-407`), the `slf4j-reload4j` literal (`:106`), the `clickhouseDependencies` block (`:109-118`), `reportTestForks` (the pin-guard precedent).
- `gradle/libs.versions.toml` — the `[versions]` block that conflicts in merge 2; `slf4j`, `shadow`, `kafka`, `httpclientTest`.
- `.github/workflows/tests.yaml` — the single job; the `Report test fork count` step (`:51-64`) is the shape for the CI-20 step; `Compile integration-test sources` (`:66-67`) precedes it; `Run tests` (`:69-70`) follows.
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` — `ktsCode` (`:305`), `forkCountStepProblem` (`:219`), `dependabotEcosystemProblem` (`:206`), `RUNS_ON` (`:45-46`), the fixture constants (`:394-503`).
- `.github/dependabot.yml` — read for DEBT-14's layout tolerance and the `dependencies` label request; never edited.
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` — the Connect-version comment D-14 rewrites; the three DLQ-emulation Javadocs are in the test tree (`ValueFreeDlqHeadersUnitTest` and neighbours; locate by grepping `4.1.1`).
- `AGENTS.md` — pre-PR obligations and test rules.

</canonical_refs>

<code_context>
## Existing Code Insights

### Current state (scouted 2026-10-21 in this discussion)
- Local branch `sync/v1.7.0` is at `443c8abc` (roadmap commit). `git merge-base HEAD origin/custom_build` = `a403cc1d`; `git rev-list --left-right --count HEAD...origin/custom_build` = `9 20` (9 local `.planning/` commits, 20 origin commits = nine Dependabot PR merges plus their bump commits). `git merge-tree --write-tree HEAD origin/custom_build` succeeds with no conflicts (tree `ca99beea`).
- `origin/custom_build` tip `69cd680b` catalog: `kafka = "4.4.0"`, `gson = "2.14.0"`, `jackson = "2.22.4"`, `httpclient = "5.6.5"`, `httpclientTest = "5.6.5"`, `commonsLang3 = "3.21.0"`, `slf4j = "2.0.20"`, `shadow = "9.6.1"`; `build.gradle.kts` has ben-manes `0.66.0` and the literal `slf4j-reload4j:2.0.20`; the wrapper is 9.8.1 (local tree is 9.6.1).
- Upstream tail `a6f1df83` is 12 commits past `v1.7.0` and reachable locally. Its diff against `v1.7.0` touches only `.gitignore` (+5 Eclipse lines), `build.gradle.kts` (+`implementation(libs.guava)`, +`clickhouseDependencies(libs.guava)`, `libs.httpclient5.test` → `libs.httpclient5`) and the catalog (`httpclient 5.6.5`, `lz4Java 1.11.4`, `guava 33.7.2-jre`, `kafka 2.7.0`, `shadow 8.3.5`). Upstream's `confluentJar` is still `from(clickhouseDependencies, sourceSets.main.get().output)`.
- Stale refs `refs/upstream/main` and `refs/upstream/v1.6.0` both point at `789a57c2` and must be deleted in pre-flight.
- Local tags present: `pre-v1.7.0-sync`, `pre-v1.7.0_gg2`, `pre-v1.7.0_gg3`, `v1.7.0`, `v1.7.0_gg3`. No local archive zip exists; the gg3 baseline is built from the tag.
- `.github/scripts/` holds only `jacoco_summary.py`; `scripts/` holds `run-ch-version-client-matrix.sh`.

### Reusable Assets
- `TestsWorkflowPinGuardUnitTest`: `nonCommentLines(Path)` (UTF-8 explicit), the step-by-name pattern of `forkCountStepProblem`, the `accepts`/`rejects` fixture helpers, and the extracted static rule methods from Phase 21 D-06. D-09 and DEBT-13/14 extend these; no regex is duplicated.
- `verify-merge-invariants-selftest.sh`: `synth_tree`, `expect_failure`, `report` helpers for any new self-test case.
- `24-COMMAND-EVIDENCE.md` E-04: the two-worktree build-and-compare procedure that D-05's script formalises.
- The Phase 21 DEPS-01 recipe (`git merge --no-commit`, edit anchors and FORK-DELTA rows, commit the merge) is the shape for merge 1.
- Existing `clickhouseDependencies` configuration is what `configurations.add(...)` takes; `prepareConfluentArchive` already knows the inner jar path (`lib/${project.name}-${project.version}-confluent.jar`) that `verifyConfluentArchive` opens.

### Established Patterns
- Anchor rows: `path TAB fixed-string TAB count TAB backs-id`; a 0-count row pins that an upstream text stays gone (Phase 18 precedent, used by D-04).
- Docker-free tests are untagged, do not extend `ClickHouseBase`, and run in `testUnit` (D-13's unit test follows this).
- Evidence transcripts name the server version and the image digest; every gate bundle is measured in the phase.
- Line-level workflow and build-script checks with no YAML or Kotlin parser (Phase 17 D-06).

### Integration Points
- The exemption rows and the tail-gate command feed every later phase's gate and Phase 29's pre-gate bundle (D-15).
- `verifyConfluentArchive` and the CI-20 step are pinned by the pin guard; the Connect-member unit test rides `testUnit` (D-07, D-13).
- The archive compare script and the javap script are reused by Phase 29's release evidence (D-05, D-11).
- FORK-DELTA rows for guava, httpclient5, the Shadow repair and the Connect API sentence feed Phase 29's rollout note (ROLL-04 items on connect-api, Shadow/Gradle, and the advisories labelled by source).
- The baseline tag (D-17) is the starting point for Phases 26–28 and the "rehearsed tree equals published tree" check in Phase 29.

</code_context>

<specifics>
## Specific Ideas

- The user again chose the narrowest durable mechanism with a standing guard over one-off evidence, every time it was offered: negative anchors for the Shadow line, a Gradle task rather than a CI-only shell step, a committed reference list with a unit test rather than a transcript, committed compare and javap scripts.
- The guard should catch the thing this phase exists to fix at the earliest point: the negative anchor at the merge gate, the archive step before the suite runs, the Connect-member test in `testUnit`.
- "The production worker's API" means the jars inside `cp-kafka-connect-base:7.9.7`, not a Maven coordinate chosen to resemble it.
- Wording that cites a measurement is written after the measurement (D-14), continuing the Phase 24 "measured, not cited" discipline.

</specifics>

<deferred>
## Deferred Ideas

- Uploading the built Confluent archive as a CI artifact. Mentioned as a possible follow-up to the CI-20 step; it is a new capability, not needed for the guard, and would add an upload step and a pin.
- Kotlin DSL deprecations surfaced by Gradle 9.8.1 `--warning-mode all` (DEPS-F3): recorded this phase, cleaned up in a later milestone.
- Dependabot PR #10 (protobuf-java 4.x, DEPS-F2) and PR #4 (client `0.10.0`, DEPS-F1): stay open, not discussed further.
- A cross-check of the image's `connect-api` jar against the Apache `3.9.x` Central artifact: offered as a stronger-evidence option and not chosen; the image jar alone is the reference.

</deferred>

---

*Phase: 25-dependency-absorption-upstream-mini-sync*
*Context gathered: 2026-10-21*
