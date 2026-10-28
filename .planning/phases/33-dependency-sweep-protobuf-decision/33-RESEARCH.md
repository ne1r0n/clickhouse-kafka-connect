# Phase 33: Dependency Sweep & Protobuf Decision - Research

**Researched:** 2026-10-28
**Domain:** Git/Gradle/GitHub-Actions dependency hygiene on a fork (Dependabot PR absorption, pin guards, anchors invariant). No connector code changes.
**Confidence:** HIGH (every command below was run read-only this session; the three unconfirmed items are in the Assumptions Log)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md has no `## Decisions` content; discuss was skipped (`workflow.skip_discuss`). The phase boundary below is copied verbatim from CONTEXT.md `<domain>` and binds the plan like a locked decision:

> Every later phase builds on a tree that already holds the four safe Dependabot bumps (PRs #19 lz4-java, #18 okhttp, #20 Gradle wrapper, #16 setup-java), each in its own commit, lz4-java first. PR #10 (protobuf-java 4.x) is settled by a decision rather than a merge: protobuf-java stays on 3.x, `dependabot.yml` ignores protobuf-java major 4+ in the `gradle` block, and `docs/FORK-DELTA.md` records the cause and re-open trigger. Nothing is merged on `origin`; a local `pre-v1.7.0_gg6` rollback tag marks the start. No connector behaviour changes. Requirements: SWP-01, SWP-02, SWP-03, SWP-04.

### Claude's Discretion
> All implementation choices are at Claude's discretion - discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Deferred Ideas (OUT OF SCOPE)
> None - discuss phase was skipped.

Out of scope by ROADMAP note: PR #17 (client 0.11.0) lands in Phase 36, not here. This phase pushes nothing.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SWP-01 | lz4-java 1.12.1 (PR #19) absorbed as its own commit, suite green | PR #19 head `2a806038...` is a one-line catalog change; `dependencyInsight` and archive `pom.properties` checks measured at baseline (1.12.0) so the post-bump read is a known-format diff |
| SWP-02 | okhttp 5.5.1 (#18), wrapper 9.8.2 (#20), setup-java 6.1.0 (#16), each its own commit | Heads, touched files, per-bump gate commands, CRLF facts, pin-guard behaviour all measured below |
| SWP-03 | PR #10 declined; Dependabot `ignore` major >= 4 in `gradle` block only; FORK-DELTA records cause and trigger | Exact `ignore` YAML, guard-test interaction verified by reading the guard, FORK-DELTA rows to edit identified, PR-closing mechanism cited from dependabot-core |
| SWP-04 | Heads merged on the scratch branch not `origin`; `pre-v1.7.0_gg6` tag before first change | Tag target, fetch-by-ref recipe, `merge-tree` dry-run (all four clean), origin re-read recipe |
</phase_requirements>

## Summary

All six open Dependabot PRs on `ne1r0n/clickhouse-kafka-connect` are children of the published `origin/custom_build` tip `3a3a15a9` (the `v1.7.0_gg5` release merge), except PR #10, which sits on an old base (`71ac9660`) and is stale. The four sweep PRs each change exactly one file set (`gradle/libs.versions.toml` once for #19, once for #18; `gradle/wrapper/gradle-wrapper.properties` for #20; four `.github/workflows/*.yaml` files for #16), had a green `ClickHouse latest Client version V2 tests` check on 2026-10-26, and `git merge-tree --write-tree HEAD <head>` against the current scratch tip reports no conflicts for each. No `fork-anchors.txt` row targets any line they change, so no anchor row moves and no exemption row is needed.

Two facts shape the plan more than the bumps themselves. First, the scratch branch `sync/v1.7.0` does not contain `3a3a15a9` (left/right count `2 27`: origin has the gg4 and gg5 release merges that local lacks), so the first PR-head merge (#19) will also pull in those two release-merge commits as history. Their trees equal the sync tree (`abb6c93e...` for both `3a3a15a9^{tree}` and `41e7d546^{tree}`), so the merge's first-parent diff is still exactly the one-line lz4 change. Second, the sweep makes `okhttp 5.5.1` and `lz4Java 1.12.1` bumps past upstream again (upstream `main` `9bed6f79` still has `lz4Java = "1.11.4"` and `okhttp = "5.5.0"`), so FORK-DELTA rows 70 and 117 (lz4/okhttp) and row 76 (setup-java v6.0.1) and row 83 (Dependabot) go stale and must be edited.

The protobuf decision is a config-plus-docs change: one `ignore:` block in the `gradle` entry of `.github/dependabot.yml` using `update-types: ["version-update:semver-major"]` (3.25.9 is the last 3.x on Maven Central, so every 4.x is a major), one FORK-DELTA row plus an open-items entry with the `VerifyError` cause (run 39501799049) and the FUT-03 re-open trigger, and, as a recommended discretionary addition, a tiny Docker-free guard test so the ignore cannot be silently dropped or widened into the `github-actions` block. Dependabot's own refresh logic closes a PR as `up_to_date` when every candidate version is ignored, but that only runs after the ignore is on the default branch (`custom_build`), i.e. at Phase 38 publication, so PR #10 stays open through this phase unless the user closes it.

**Primary recommendation:** Tag `pre-v1.7.0_gg6` on re-read `origin/custom_build` (`3a3a15a9`), then `git fetch origin refs/pull/N/head` + `git merge --no-ff` each of #19, #18, #20, #16 in that order with a per-bump gate and recorded head SHA; make all FORK-DELTA/dependabot edits in separate commits after the four pure merges.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version pins (lz4, okhttp, libprotobuf) | Build config (`gradle/libs.versions.toml`) | CI (PR checks) | Single catalog owns versions; archive/classpath derive from it |
| Gradle wrapper version | Build config (`gradle-wrapper.properties`) | CI (`setup-gradle` reads wrapper) | Only the properties file selects the distribution |
| setup-java action pin | CI workflows (4 fork-owned files) | Unit test guards (`WorkflowActionPinGuardUnitTest`) | SHA pin + `# vX.Y.Z` comment is the guarded contract |
| Which bumps Dependabot proposes | Repo meta (`.github/dependabot.yml`, default branch) | Pin guard tests | Config is read from the default branch `custom_build`, not the scratch branch |
| Decision record | Docs (`docs/FORK-DELTA.md`) | Planning artifacts | FORK-DELTA is the fork's re-litigation guard for the next syncer |
| Rollback marker, "nothing on origin" | Git state (local tag, `ls-remote` reads) | - | Pure git; nothing is pushed in this phase |

## Standard Stack

This phase adds no libraries. It changes versions of four existing coordinates.

### Core (versions after the sweep)
| Item | Before | After | Source of the target version |
|------|--------|-------|------------------------------|
| `at.yawk.lz4:lz4-java` (`lz4Java`) | 1.12.0 | 1.12.1 | PR #19; Maven Central `maven-metadata.xml` `<release>1.12.1</release>`, lastUpdated 2026-10-22 [VERIFIED: repo1.maven.org] |
| `com.squareup.okhttp3:okhttp` (`okhttp`) | 5.5.0 | 5.5.1 | PR #18; `okhttp-5.5.1.pom` returns HTTP 200 on Maven Central [VERIFIED: repo1.maven.org] |
| Gradle wrapper | 9.8.1 | 9.8.2 | PR #20; `services.gradle.org/versions/current` reports `"version" : "9.8.2"`, `"current" : true`, `"broken" : false` [VERIFIED: services.gradle.org] |
| `actions/setup-java` | v6.0.1 (`de7274f0...`) | v6.1.0 (`b843ebf9b913146b38da2eaa7aeddf76074ae1ad`) | PR #16; `git ls-remote https://github.com/actions/setup-java refs/tags/v6.1.0` returns `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` and no `^{}` line, i.e. a lightweight tag pointing straight at that commit, so the PR's SHA is the tag's commit [VERIFIED: git ls-remote] |
| `com.google.protobuf:protobuf-java` (`libprotobuf`) | 3.25.9 | 3.25.9 (unchanged) | Last 3.x on Maven Central is `3.25.9`; next release is `4.0.0-rc-1` [VERIFIED: repo1.maven.org maven-metadata.xml] |

Verbatim current values [VERIFIED: gradle/libs.versions.toml:9, :19, :26]:
```
lz4Java = "1.12.0"
okhttp = "5.5.0"
libprotobuf = "3.25.9"
```
and [VERIFIED: gradle/wrapper/gradle-wrapper.properties:3]:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.1-bin.zip
```

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `git merge --no-ff <PR head>` | `git cherry-pick` of the PR commit | ROADMAP criterion 1 says "merged on the scratch branch"; a merge also records the PR head SHA as a parent, which is the audit trail SWP-04 wants. Cherry-pick loses it. |
| `update-types: semver-major` ignore | `versions: [">=4"]` | The `versions` range syntax for Gradle is not documented in what was fetched (examples given are npm/Bundler/NuGet/Maven `[1.4,)`), so it is unverified; `update-types` is ecosystem-independent. |

**Installation:** none. **Registry check:** these are existing coordinates, not new packages; `gsd-tools package-legitimacy` supports only npm/pypi/crates, so it does not apply to Maven coordinates.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| at.yawk.lz4:lz4-java 1.12.1 | Maven Central | existing dependency since earlier gg releases | n/a | github.com/yawkat/lz4-java (advisory link in PR #19 body) | n/a (version bump of an existing, already-shipped coordinate; seam does not cover Maven) | Approved; PR #19 body: "Security release for GHSA-48mj-8f7c-7qfm" [CITED: PR #19 body via `gh pr view 19`] |
| com.squareup.okhttp3:okhttp 5.5.1 | Maven Central | existing (integration-test source set only) | n/a | github.com/square/okhttp | n/a (same) | Approved |

**Packages removed due to [SLOP] verdict:** none. **Flagged [SUS]:** none. No `checkpoint:human-verify` install gate is needed because nothing new is installed.

## Architecture Patterns

### System Architecture Diagram

```
origin (GitHub, ne1r0n)                    local scratch branch sync/v1.7.0
 custom_build tip 3a3a15a9  --ls-remote-->  (1) re-read, record SHA
 refs/pull/{19,18,20,16}/head                (2) git tag pre-v1.7.0_gg6 3a3a15a9   [local only]
        |                                     |
        | git fetch origin refs/pull/N/head   v
        +----------> FETCH_HEAD -> SHA recorded -> git merge --no-ff -m ... SHA
                                              |
        per bump, in order #19 -> #18 -> #20 -> #16:
          gate  -> commit stays only if its own gate is green
                                              |
                                              v
   sweep tip --> docs/FORK-DELTA.md + .github/dependabot.yml (+ optional guard test)  [separate commits]
                                              |
   phase gate: cache-busted testUnit; full suite latest x V2 + SELECT version(); anchors invariant;
               git ls-remote origin custom_build re-read (SWP-04 close-out)
                                              |
   NOTHING PUSHED. PR #10 stays open; ignore reaches custom_build only at Phase 38 publication
```

### Recommended commit shape on the scratch branch

1. (no commit) re-read `git ls-remote origin custom_build`; `git tag pre-v1.7.0_gg6 <that SHA>`.
2. optional but recommended: a baseline full suite on the untouched tip (see Pitfall 1).
3. `Merge ... PR #19 (<head sha>): lz4-java 1.12.1` - gate: `dependencyInsight`, archive check, full suite.
4. `Merge ... PR #18 (<sha>)` - gate: `compileIntegrationTestJava`.
5. `Merge ... PR #20 (<sha>)` - gate: changed-files check, `./gradlew --version`, CRLF check.
6. `Merge ... PR #16 (<sha>)` - gate: pin-guard tests under `testUnit`.
7. docs/config commits: FORK-DELTA staleness fixes for 3-6; dependabot `ignore` + FORK-DELTA protobuf rows (+ optional guard test).
8. phase gate.

Keep steps 3-6 pure (the PR head only). Putting doc edits inside those merge commits would break criterion 3's "the commit changes only `gradle/wrapper/gradle-wrapper.properties`".

### Pattern 1: fetch a Dependabot head by ref and record it

```bash
# Source: measured this session; `git ls-remote origin refs/pull/N/head` returned all six heads
N=19
git fetch origin "refs/pull/${N}/head"        # writes FETCH_HEAD only, no local ref
SHA=$(git rev-parse FETCH_HEAD)
git log -1 --format='%H %an %s' "$SHA"        # expect author dependabot[bot]
git merge-tree --write-tree --name-only HEAD "$SHA"   # prints a tree id and nothing else when clean
git merge --no-ff -m "Merge Dependabot PR #${N} (${SHA}): <subject>" "$SHA"
git diff --stat HEAD^1 HEAD                    # must list only the expected file(s)
```
Dependabot rebases heads when `custom_build` moves, so fetch right before each merge and write the SHA into the commit message and the evidence file. The heads read at research time (all `custom_build` children of `3a3a15a9`):

| PR | Head SHA at research time | Files | PR check on 2026-10-26 |
|----|---------------------------|-------|------------------------|
| #19 lz4-java 1.12.0 -> 1.12.1 | `2a8060389924edcabcac26c3c22e397f7939c098` | `gradle/libs.versions.toml` (`lz4Java`) | SUCCESS |
| #18 okhttp 5.5.0 -> 5.5.1 | `cb898dc2a9f52ace13c8f9ff8602a37fcc4fb6cd` | `gradle/libs.versions.toml` (`okhttp`) | SUCCESS |
| #20 wrapper 9.8.1 -> 9.8.2 | `8786723bca5a0a2d0a014d15c15981386da8345d` | `gradle/wrapper/gradle-wrapper.properties` | SUCCESS |
| #16 setup-java 6.0.1 -> 6.1.0 | `cb3c77187eb5c036dc3d6d155aca4e2cd85dd76f` | `nightly-integration-tests.yaml`, `pre-release.yaml`, `release.yaml`, `tests.yaml` (one line each) | SUCCESS |
| #17 client 0.9.6 -> 0.11.0 | `6a53525b738d90ee4c98a3761ebebe11421cf1f0` | `gradle/libs.versions.toml` | FAILURE (compileTestJava) - Phase 36, not here |
| #10 protobuf-java 3.25.9 -> 4.36.2 | `011bc42bfe8d8230c430ec8001d481885daec5fa` (parent `71ac9660`, stale base) | `gradle/libs.versions.toml` | FAILURE (run 39501799049) - declined |

[VERIFIED: `gh pr list/diff/view -R ne1r0n/clickhouse-kafka-connect`, `git ls-remote origin refs/pull/*/head`]. Note `gh` without `-R` resolves to the upstream repo (`ClickHouse/clickhouse-kafka-connect`) in this checkout and lists unrelated PRs; always pass `-R ne1r0n/clickhouse-kafka-connect`.

### Pattern 2: the pre-sweep rollback tag

`pre-v1.7.0_gg5` is `21c51598` (the gg4 release merge = the `origin/custom_build` tip before gg5 published) and `v1.7.0_gg5` is `3a3a15a9`; both are lightweight tags (`git cat-file -t` prints `commit`) [VERIFIED: git]. Following that pattern, `pre-v1.7.0_gg6` marks `3a3a15a9ae064ffe09c696b8bb87bce23419d463`, the `origin/custom_build` tip that publication rolls back to. `git ls-remote origin custom_build` read exactly that SHA at research time. It is the same commit as `v1.7.0_gg5`; the two tags coexist. Create it local-only (`git tag pre-v1.7.0_gg6 3a3a15a9...`), never `git push` it (Phase 38 pushes it after the published SHA's run is green).

### Anti-Patterns to Avoid
- **Editing FORK-DELTA/anchor/test files inside a bump merge commit.** Breaks "each in its own commit" and the wrapper commit's "changes only the properties file" criterion.
- **Running `./gradlew wrapper ...` or `spotlessApply` in the main tree.** The first regenerates `gradlew`/`gradlew.bat`; prior evidence shows `spotlessApply` rewrote 48 tracked files including `gradlew.bat` in a clean worktree (31-COMMAND-EVIDENCE.md line 618). If a new Java test class is added, check formatting in a detached worktree and `cmp` the file, as Phase 31 did.
- **Reading "PR CI green" as the sweep tip being green.** The PR runs were on `3a3a15a9` + one bump each. The combined tip, the local `latest` image (now different), and the cache must be re-measured.
- **Using `git clean`** to remove build leftovers (project memory: delete files one by one).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Applying each Dependabot bump | A hand edit of the toml/properties/workflow lines | `git merge --no-ff` of the fetched PR head | Preserves the PR head SHA as a parent; the SHA pins and `# v6.1.0` comments in #16 are already correct for all four files |
| Checking that a SHA pin matches the tag | A script | `git ls-remote https://github.com/actions/setup-java refs/tags/v6.1.0 refs/tags/v6.1.0^{}` | Already measured: `b843ebf9...` matches the PR's SHA |
| Anchor invariant | A new checker | `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (+ `verify-merge-invariants-selftest.sh`) | The established gate; baseline measured PASS this session |
| "Did a bump move an anchor row" | Reading the diff | The anchors-mirror loop below | Same method as Phase 31 evidence |
| Closing PR #10 | A scripted `gh pr close` | The user, or Dependabot at publication | The phase pushes nothing and must not act on `origin` without the user |

**Key insight:** every artifact this phase needs already exists in the repo; the work is ordering, evidence, and not disturbing three things: `gradlew.bat`, upstream-byte-identical `integrationTests.yaml`, and `origin`.

## Common Pitfalls

### Pitfall 1: a red that is the `latest` image, not the bump
**What goes wrong:** The local `clickhouse/clickhouse-server:latest` is now `26.10.3.7` (measured: `SELECT version()` -> `26.10.3.7`; image created 2 days ago). Earlier project notes record ClickHouse 26.9.19.9 and a 26.9 default change that broke client 0.9.6. A red full suite right after the lz4 merge is ambiguous without a baseline.
**How to avoid:** Run one full suite on the untouched tip (before the lz4 merge) on the same image, record `SELECT version()` and the counts, then run the lz4 gate. The ROADMAP's "a later red is attributable to one change" goal needs this. ([ASSUMED] ~3 min per full suite from project memory notes; the milestone research baseline is 1,961 tests, 1,943 passed, 18 skipped at `v1.7.0_gg5`.)
**Warning signs:** Failures in tests unrelated to lz4 in the first run after the sweep begins; compression "magic byte" errors.

### Pitfall 2: the first merge drags in two release-merge commits
**What goes wrong:** `git rev-list --left-right --count origin/custom_build...HEAD` prints `2 27`. Merging PR #19's head (parent `3a3a15a9`) adds `21c51598` and `3a3a15a9` as ancestors. Someone reading `git log` or the invariants gate may think the lz4 commit "also changed other things".
**Why it is safe:** `git rev-parse 3a3a15a9^{tree} 41e7d546^{tree}` print the same tree `abb6c93eed3a82638ac8252b21ee9f228cb19c05`, and `git merge-tree --write-tree HEAD 2a806038...` is conflict-free. The first-parent diff of the merge is only the `lz4Java` line.
**How to avoid:** Assert `git diff --stat HEAD^1 HEAD` shows exactly the expected file after each merge, and state in the evidence file that the first merge also absorbs the two release merges (SWP-04 close-out, since this makes the scratch branch contain the published tip). Do not add a separate "merge origin" commit unless the plan wants the extra record; it is optional and tree-neutral.

### Pitfall 3: `gradlew.bat` line endings
**What goes wrong:** `.gitattributes` has `*.bat           text eol=crlf` [VERIFIED: .gitattributes:5]. Local commit `f71468bc` ("chore: renormalize gradlew.bat line endings to match .gitattributes", 2026-10-28) is on `sync/v1.7.0`: the committed blob at `3a3a15a9` starts `@rem\r\n` (CRLF) while the blob at HEAD starts `@rem\n` (LF), and `git ls-files --eol gradlew.bat` reads `i/lf w/crlf attr/text eol=crlf` (index LF, work tree CRLF through the attribute). So the working-tree file is CRLF and `file gradlew.bat` prints "with CRLF line terminators". Prior evidence: a fresh checkout lists `gradlew.bat` as modified, and `./gradlew wrapper`-style regeneration can bring LF back.
**How to avoid for the wrapper commit (criterion 3):**
- `git diff --name-only HEAD^1 HEAD` must print only `gradle/wrapper/gradle-wrapper.properties`.
- `git ls-files --eol gradlew.bat` unchanged (`w/crlf attr/text eol=crlf`) and `file gradlew.bat` still reports CRLF.
- `git status --short` shows nothing for `gradlew.bat` after `./gradlew --version` (verified clean after running Gradle 9.8.1 this session).
- Do not run the `wrapper` task. PR #20 changes only the properties file [VERIFIED: `gh pr diff 20`, `changedFiles: 1`].
**Warning signs:** `gradlew.bat` in `git status` or in the diff list.

### Pitfall 4: dirty `.planning/config.json` during merges
`git status --short` shows ` M .planning/config.json` (pre-existing, unrelated). The four merges touch no `.planning` file, so `git merge` is not blocked, but a plan task that commits with `git commit -a` would sweep it into a bump commit. Use path-specific adds and plain `git merge`. `git merge` creates the commit itself; the GSD `commit` verb adds no trailer (project memory), so end messages with the attribution line from the session reminder via plain git.

### Pitfall 5: setup-java guard fixture is not a live pin
`TestsWorkflowPinGuardUnitTest.java:642` holds `pinnedUses("actions/setup-java", "v6.0.1")` over a fake SHA (`static final String SHA = "3d3c42e5aac5ba805825da76410c181273ba90b1"` at :627) inside `FIVE_PINNED` fixtures [VERIFIED: file read]. It is test data for the rule, not a check of the real workflows, so it does NOT need to move with PR #16. Do not "fix" it. The live checks are the tests that read `.github/workflows/*` (`WorkflowActionPinGuardUnitTest`, `TestsWorkflowPinGuardUnitTest`, `NightlyWorkflowGuardUnitTest`, `UpstreamIntegrationWorkflowBytesGuardUnitTest`). `integrationTests.yaml` (upstream-identical, `actions/setup-java@v3`) is excluded from Dependabot and not touched by #16 (`changedFiles: 4`, none of them it).

### Pitfall 6: `dependabot.yml` edit trips the wrong guard
`WorkflowActionPinGuardUnitTest.dependabotExcludesOnlyTheUpstreamIdenticalWorkflow` asserts `excludePaths(...)` equals only `integrationTests.yaml`. `excludePaths` starts scanning at the `github-actions` entry (`TestsWorkflowPinGuardUnitTest.java:542-575`) and `dependabotEcosystemProblem` only needs a `- package-ecosystem: "gradle"` line and a `github-actions` entry (`:228-236`). An `ignore:` list inside the `gradle` entry (which comes first in the file) is invisible to both. Putting `ignore:` after the `github-actions` entry's `exclude-paths` would be read by `excludePaths` as the end of that list but would also be the wrong block; keep it in the `gradle` block.

### Pitfall 7: full-suite caching
Gradle replays a cached `test` result. Use `--rerun` and clear the results first (the pattern used in Phase 26/31 evidence): `rm -rf build/test-results/test` then `./gradlew ... test --rerun`. `build.gradle.kts` is an input of every Test task (`inputs.file(layout.projectDirectory.file("build.gradle.kts"))`), but a catalog-only change is picked up through the classpath; the lz4/okhttp bumps change classpath jars so `test` is not UP-TO-DATE, but still use `--rerun` for the evidence run.

## Code Examples

### dependabot.yml: the ignore, in the gradle block only
```yaml
# Source: GitHub docs `ignore` option (update-types values) [CITED: docs.github.com dependabot-options-reference]
# Gradle dependency-name format is groupId:artifactId [CITED: same page]
  - package-ecosystem: "gradle"
    directory: "/" 
    schedule:
      interval: "monthly"
    labels:
      - "dependencies"
    open-pull-requests-limit: 5
    # FORK: protobuf-java stays on 3.x until the Confluent test stack moves to 8.x (FUT-03, docs/FORK-DELTA.md).
    ignore:
      - dependency-name: "com.google.protobuf:protobuf-java"
        update-types: ["version-update:semver-major"]
```
Current `gradle` entry for context [VERIFIED: .github/dependabot.yml:8-14]:
```
  - package-ecosystem: "gradle"
    directory: "/" 
    schedule:
      interval: "monthly"
    labels:
      - "dependencies"
    open-pull-requests-limit: 5
```
Because `libprotobuf` is the single version ref feeding both the runtime jar and `protoc` (`build.gradle.kts:541` `artifact = "com.google.protobuf:protoc:${libs.versions.libprotobuf.get()}"`), ignoring `protobuf-java` also stops Dependabot from moving the code generator. The protobuf Gradle plugin (`protobufPlugin = "0.10.0"`) is a different coordinate and stays updatable. A `#` comment line is fine for the guards, which drop lines whose stripped text starts with `#` (`nonCommentLines`).

### Anchors mirror (run on each merge tip; measured: rows=131 bad=0 at HEAD `28d1235d`)
```bash
bad=0; rows=0
while IFS=$'\t' read -r path pat cnt id; do
  case "$path" in ''|'#'*) continue;; esac
  rows=$((rows+1)); got=$(grep -cF -e "$pat" "$path" 2>/dev/null)
  [ "${got:-0}" = "$cnt" ] || { echo "ANCHOR MOVED $path [$pat] expected $cnt got ${got:-0}"; bad=$((bad+1)); }
done < .planning/scripts/fork-anchors.txt
echo "rows=$rows bad=$bad"
```
No row in `fork-anchors.txt` mentions `lz4Java`, `okhttp`, `setup-java`, `gradle-9.8` or `libprotobuf` (read the whole file). The only lz4 row is `build.gradle.kts	withCapability("org.lz4:lz4-java")	1	BLD-09`, which the bump does not touch. So expect zero anchor moves; if the mirror reports one, it is a real finding.

### dependencyInsight and archive checks (baseline measured at 1.12.0; expect 1.12.1 after #19)
```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q dependencyInsight \
  --dependency at.yawk.lz4:lz4-java --configuration runtimeClasspath
# baseline headline: "at.yawk.lz4:lz4-java:1.12.0"; selection reason "By conflict resolution: between versions 1.12.0, 1.10.2 and 1.11.2"
# Use the full coordinate: `--dependency lz4-java` would also match the original org.lz4 module.
# Repeat with --configuration clickhouseDependencies (the set the archive bundles).

env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q createConfluentArchive
# baseline last line: "verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class"
# archive: build/confluent/clickhouse-kafka-connect-v1.7.0_gg5.zip (VERSION is untouched in this phase)
rm -rf /tmp/p33-arch && mkdir -p /tmp/p33-arch && unzip -q -o build/confluent/clickhouse-kafka-connect-*.zip -d /tmp/p33-arch
unzip -p /tmp/p33-arch/*/lib/*-confluent.jar META-INF/maven/at.yawk.lz4/lz4-java/pom.properties
# baseline output: artifactId=lz4-java / groupId=at.yawk.lz4 / version=1.12.0  -> expect version=1.12.1
```
`createConfluentArchive` is `finalizedBy("verifyConfluentArchive")` [VERIFIED: build.gradle.kts:452]; the archive holds exactly one `lib/*-confluent.jar` that unpacks the dependencies, so the version is read from the inner jar's `pom.properties`, not from a nested jar name. Extract to a fresh empty directory (the archive is a build output, treat as untrusted data) and keep scripts elsewhere.

### Per-bump gates
```bash
# #18 okhttp (Docker-free)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileIntegrationTestJava
# #20 wrapper (Docker-free; first run downloads the 9.8.2 distribution, which is not in ~/.gradle/wrapper/dists)
git diff --name-only HEAD^1 HEAD            # only gradle/wrapper/gradle-wrapper.properties
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain --version   # "Gradle 9.8.2"
git ls-files --eol gradlew.bat; file gradlew.bat; git status --short
# #16 setup-java (Docker-free)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun \
  --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' \
  --tests '*NightlyWorkflowGuardUnitTest' --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest'
grep -n 'actions/setup-java@' .github/workflows/*.y*ml   # all fork-owned: 40-hex SHA + "# v6.1.0"; only integrationTests.yaml stays @v3
```
Also run `createConfluentArchive` once after the wrapper bump (PITFALLS: "run, not just test"); PR #20's own CI ran `Build and verify Confluent archive` green.

### Phase gate
```bash
# 1. cache-busted testUnit (no Docker)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
# 2. full suite on latest x V2 (colima profile vz must be Running: `colima list`)
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse \
  clickhouse/clickhouse-server:latest local --query 'SELECT version()'     # measured: 26.10.3.7
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun
# 3. anchors invariant (baseline measured this session: ~19.5 min wall)
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD            # run in background
bash .planning/scripts/verify-merge-invariants-selftest.sh
# 4. nothing on origin
git ls-remote origin custom_build   # expect 3a3a15a9ae064ffe09c696b8bb87bce23419d463 (or absorb any new tip first)
git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main; echo $?   # expect 0 (no connector change)
```
`TESTCONTAINERS_HOST_OVERRIDE=192.168.64.4` is exported in this shell, hence `env -u`. The docker CLI is not on PATH (use the Cellar path). The tests run on the Gradle-provisioned Adoptium 17 HotSpot (`~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac`, toolchain `defaultJdkVersion = 17`), while the default `java` is now Homebrew HotSpot 21, so the older OpenJ9/Mockito crash note (exit 255) is unlikely; if it appears, read `javacore.*`, delete the dump files one by one, re-run once and record both runs.
The suite's 18 named skips are 7 `@Disabled` methods plus the 11 parameterized `exactlyOnceStateMismatchTest` cases skipped by `Assumptions.assumeTrue(isCluster || isCloud)` [CITED: docs/FORK-DELTA.md "Named skip inventory"]; confirm `skipped=18` from the JUnit XML, not only the console summary.

## FORK-DELTA / documentation edits (separate commits, after the four merges)

[VERIFIED: docs/FORK-DELTA.md read this session; line numbers as of HEAD `28d1235d`]

| Line | Section | Stale or new content | Edit |
|------|---------|----------------------|------|
| 70 | Build and dependencies, "Fork dependency pins in the version catalog" | `lz4Java = "1.12.0"` (upstream `1.11.4`); "okhttp `5.5.0`, org.json `20260814` and mockito `5.24.0` are now upstream's own values and no longer bumps past upstream"; "Each bump is its own one-line catalog commit" | lz4Java becomes `1.12.1`; okhttp `5.5.1` moves back into the "past upstream" list (upstream `main` `9bed6f79` still has `okhttp = "5.5.0"`, `lz4Java = "1.11.4"`); keep org.json/mockito as upstream's own |
| 117 | Retired in favour of upstream, the lz4 capability-rule row | "the catalog `lz4Java` entry, which the fork now pins at `lz4Java = "1.12.0"`" | `1.12.1` |
| 76 | CI workflows, the single-job workflow row | "(checkout v7.0.1, setup-java v6.0.1, setup-gradle v6.4.0 ..., upload-artifact v7.0.2)" | setup-java v6.1.0 |
| 83 | CI workflows, the Dependabot row | "open pull requests `#4` and `#10`"; "no `github-actions` pull request has been observed yet, so the `github-actions` entry and its `exclude-paths` are untested in a run" | PR #16 is the first observed `github-actions` PR (`dependabot/github_actions/github-actions-4430c5f07e`): it touched the four fork-owned workflows and not `integrationTests.yaml`, which is the first observed evidence that `exclude-paths` works; update the open-PR list (now #10, #16, #17, #18, #19, #20 before the sweep) |
| new row, Build and dependencies | protobuf decision | none yet | New row: `libprotobuf` stays `3.25.9`; `dependabot.yml` `gradle` entry ignores `com.google.protobuf:protobuf-java` semver-major; cause: `ProtobufIngestTest` fails with `java.lang.VerifyError` against the Confluent 7.9.1 test stack (PR #10 run 39501799049, 2026-10-20: 27 cases, `DescriptorProtos$EnumValueOptions is not assignable to GeneratedMessageV3$ExtendableMessage`); one version ref feeds both `protobuf-java` and `protoc` (`build.gradle.kts:541`); re-open trigger FUT-03 (Confluent 8.x test stack); Pinned by SWP-03 (+ the guard test if added). Upstream is also on 3.25.9 with no ignore, so the "why upstream does not provide it" cell says the ignore is a fork-owned config |
| Known costs and open items | numbered list ends at item 11+ (read to confirm the next number) | none | Add one open item pointing at the same decision and FUT-03 |

The `docs/FORK-DELTA.md` is not an input of any guard test (`grep` for `FORK-DELTA` in `src`, `build.gradle.kts` and `.planning/scripts` finds only a mention inside `fork-anchors.txt` comments), so wording edits cannot turn a test red. `SourcePlanningIdGuardUnitTest` scans only `src/main/java` (`SOURCE_ROOT = Path.of("src", "main", "java")`), so planning IDs such as SWP-03 in `docs/` and in a test class are allowed; avoid them in any `src/main` file (there should be none). Do NOT edit `CHANGELOG.md` or `VERSION` here: `VERSION=v1.7.0_gg6` + CHANGELOG are RLS-01 (Phase 38), and `fork-anchors.txt` row `VERSION	v1.7.0_gg5	1	BLD-07` must stay as is.

### Optional guard (recommended, Claude's discretion)
A small Docker-free class in `src/test/java/com/clickhouse/kafka/connect/ci/` (extends nothing, so it stays in `testUnit`; reuses package-private `TestsWorkflowPinGuardUnitTest.nonCommentLines` and `DEPENDABOT`) that asserts: the `gradle` entry has an `ignore` item for `com.google.protobuf:protobuf-java` with `version-update:semver-major`, and the `github-actions` entry has no `ignore`. This pins SWP-03's "gradle block only" and survives future edits to the file. AGENTS.md requires test coverage for new functionality; config is borderline, so treat as recommended, not required. Format check via a detached worktree, never `spotlessApply` in the main tree.

## PR #10 closure

- PR #10 is open (`state: OPEN`), `mergeable: MERGEABLE`, check `ClickHouse latest Client version V2 tests` = FAILURE, one comment (a Dependabot label notice: the `dependencies` label "could not be found"). Its base is the old `71ac9660`, so it would also need a rebase before any merge.
- Dependabot closes a version-update PR when all candidate versions are ignored: in `refresh_version_update_pull_request.rb`, `return close_pull_request(reason: :up_to_date) if all_versions_ignored?(lead_dependency, checker)` [CITED: github.com/dependabot/dependabot-core updater/lib/dependabot/updater/operations/refresh_version_update_pull_request.rb]. 3.25.9 is the last 3.x on Maven Central, so every newer version is a major bump and is ignored. This runs only from the default branch's config, so it can fire only after the ignore is on `custom_build` (Phase 38). When exactly the refresh runs is not documented in what was fetched [ASSUMED: it happens on the next scheduled run or a config-change-triggered run].
- GitHub's docs do not say what happens to open PRs when an `ignore` is added to the YAML (both fetched pages are silent) [CITED: docs.github.com controlling-dependencies-updated, managing-pull-requests-for-dependency-updates].
- Therefore the phase cannot confirm self-closing; ROADMAP criterion 4 allows "closed by the user, confirmed in their own words, or recorded as closing once the `ignore` reaches `custom_build` at publication". Plan for the second option as default and ask the user whether they want to close #10 now. An agent must not close it without the user's own message. A `@dependabot ignore this major version` comment would store the ignore centrally instead of in the YAML, which is not what SWP-03 asks for.

## PR #17 identity (confirmed out of scope)

PR #17 is `chore(deps): bump clickHouseDriver from 0.9.6 to 0.11.0` (head `6a53525b...`, one-line `gradle/libs.versions.toml`, parent `3a3a15a9`). Its run failed at `:compileTestJava` (`UtilsTest` `ServerException` constructor), per the milestone research. It is Phase 36 (CLT-*), after its pins. Phase 33 must leave it alone; it will not conflict with the sweep (it edits line 2 of the catalog, #19 line 9, #18 line 19).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| git, `origin` and `upstream` remotes | all | yes | origin `git@github.com:ne1r0n/clickhouse-kafka-connect.git` | - |
| gh CLI (authenticated) | PR identity checks | yes | 2.102.0 | `git ls-remote` for heads |
| Gradle wrapper 9.8.1 / JDK | all builds | yes | wrapper dist 9.8.1 cached; default java Homebrew HotSpot 21.0.12.1; Adoptium 17 HotSpot toolchain in `~/.gradle/jdks` | - |
| Gradle 9.8.2 distribution | PR #20 gate | not cached; downloadable | `services.gradle.org` reports 9.8.2 current; `HTTP/1.1 307` redirect on the zip | - (network required) |
| Docker (colima profile `vz`) | full suite, `SELECT version()` | yes, Running (8 CPU / 16 GiB) | docker CLI 29.8.2 at `/opt/homebrew/Cellar/docker/29.8.2/bin/docker` (not on PATH) | - |
| ClickHouse `latest` image | full suite | yes (pulled 2 days ago) | `SELECT version()` -> 26.10.3.7 | - |
| `yq` | optional | no | - | not needed in this phase |
| Maven Central | version checks | yes | - | - |

**Missing dependencies with no fallback:** none. Gradle 9.8.2 needs network on first use.

## Security Domain

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1/V14 Config and supply chain | yes | Actions pinned to full 40-hex SHAs with `# vX.Y.Z` (guarded); catalog version bumps from vetted PR heads; no new packages |
| V2/V3/V4 Authn/session/access | no | no code paths touched |
| V5 Input validation | no | no connector code changes (`git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main` expected 0) |
| V6 Cryptography | no | none hand-rolled |

| Threat pattern | STRIDE | Mitigation |
|----------------|--------|------------|
| Dependabot PR head rewritten between review and merge | Tampering | Fetch by `refs/pull/N/head` immediately before merge, record SHA, check author `dependabot[bot]` and `git diff --stat` of the merge matches the PR's file list |
| Mutable action tag swapped | Tampering | #16 uses the full commit SHA; `v6.1.0` tag verified to point at `b843ebf9...` |
| Vulnerable lz4 in the shipped archive | Information disclosure / DoS | #19 (GHSA-48mj-8f7c-7qfm per PR body) lands first and is verified in the archive's `pom.properties` |
| Accidental write to `origin` | Tampering | Phase pushes nothing; `ls-remote` re-read at start and gate; ask the user not to merge Dependabot PRs on `origin` until publication |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A full suite takes about 3 minutes locally (from project memory, not re-measured) | Pitfall 1 | Plan timeouts too short; `test_gate_timeout` is 2400 s in config so low risk |
| A2 | Dependabot's refresh that closes PR #10 as `up_to_date` runs on the next scheduled run or after a config change on `custom_build` | PR #10 closure | PR #10 might linger after publication; user can then close it |
| A3 | `versions: [">=4"]` may or may not be accepted for the Gradle ecosystem; not used, `update-types` is used instead | Alternatives | None if `update-types` is used as recommended |
| A4 | The optional guard test is wanted by the user | FORK-DELTA / Optional guard | Extra test class the user did not want; low cost, discretionary |
| A5 | "Dependabot reads this file from the default branch" is taken from FORK-DELTA row 83, not from a GitHub doc page fetched this session | Architectural map | If wrong, the ignore could take effect earlier or differently; the in-repo claim has been the project's working assumption |

## Open Questions

1. **Who closes PR #10, and when?**
   - Known: it cannot self-close before the ignore is on `custom_build`; criterion 4 accepts "recorded as closing at publication".
   - Unclear: whether the user prefers to close it now.
   - Recommendation: ask once at the end of the phase; default to recording it as closing at Phase 38 publication.
2. **Ask the user to hold Dependabot merges on `origin`** (ROADMAP note): do this at the start. Project memory records that the user merges Dependabot PRs directly on `origin/custom_build` and that this diverges the scratch branch (and can break the anchors invariant for an anchored line). Re-read `git ls-remote origin custom_build` at start and at the gate; if it moved, merge `origin/custom_build` in (never rebase), check anchors, and extend `fork-anchor-exemptions.txt` only per its rules.
3. **Baseline full suite before the first merge?** Recommended (Pitfall 1); costs one run.
4. **Record the first merge's extra ancestors** (the gg4 and gg5 release merges) explicitly in the evidence file.

## Sources

### Primary (HIGH confidence, measured this session)
- `gh pr list/diff/view -R ne1r0n/clickhouse-kafka-connect`, `git ls-remote origin` (heads, base, checks, file lists)
- `git merge-tree --write-tree HEAD <head>` for the four heads (clean), `git ls-files --eol`, `git cat-file`
- Local Gradle runs: `dependencyInsight`, `createConfluentArchive` (baseline lz4 1.12.0); `verify-merge-invariants.sh 54e63648 HEAD` -> `invariants: provenance_paths=21 anchor_commits=974 anchor_rows=131 marker_commits=1033 anchor_exempt=19 result=PASS` (19 min 26 s); anchors mirror rows=131 bad=0
- Files read: `gradle/libs.versions.toml`, `.github/dependabot.yml`, `gradle/wrapper/gradle-wrapper.properties`, `.gitattributes`, `.planning/scripts/fork-anchors.txt`, `fork-anchor-exemptions.txt`, `build.gradle.kts` (relevant ranges), the CI guard tests, `docs/FORK-DELTA.md`
- repo1.maven.org `maven-metadata.xml` and poms; `services.gradle.org/versions/current`; `git ls-remote https://github.com/actions/setup-java`

### Secondary (MEDIUM confidence)
- https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference (`ignore`: `dependency-name`, `versions`, `update-types`; Gradle name format)
- https://raw.githubusercontent.com/dependabot/dependabot-core/main/updater/lib/dependabot/updater/operations/refresh_version_update_pull_request.rb (closing on all-versions-ignored)
- https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/manage-your-dependency-security/controlling-dependencies-updated and .../managing-pull-requests-for-dependency-updates (silent on open-PR effect)

### Tertiary (LOW confidence)
- Project memory notes (full-suite duration, Dependabot-merge divergence, colima profile)

## Metadata

**Confidence breakdown:**
- Standard stack / targets: HIGH - versions confirmed against the registries and PR diffs
- Architecture / commit shape: HIGH - dry-run merges clean, anchors measured
- Pitfalls: HIGH - each reproduced or read from source; Dependabot closing timing MEDIUM

**Research date:** 2026-10-28
**Valid until:** 2026-11-04 (PR heads, `latest` image and `origin/custom_build` can all move; re-read heads right before each merge)
