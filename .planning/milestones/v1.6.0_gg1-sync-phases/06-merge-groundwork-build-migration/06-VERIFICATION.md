---
phase: 06-merge-groundwork-build-migration
verified: 2026-09-18T00:00:00Z
status: passed
score: 18/18 must-haves verified
covered_files: [".github/workflows/tests.yaml", ".gitignore", ".planning/MERGE-CHECKLIST.md", ".planning/REQUIREMENTS.md", ".planning/ROADMAP.md", ".planning/phases/06-merge-groundwork-build-migration/06-01-PLAN.md", ".planning/phases/06-merge-groundwork-build-migration/06-01-SUMMARY.md", ".planning/phases/06-merge-groundwork-build-migration/06-02-PLAN.md", ".planning/phases/06-merge-groundwork-build-migration/06-02-SUMMARY.md", ".planning/phases/06-merge-groundwork-build-migration/06-03-PLAN.md", ".planning/phases/06-merge-groundwork-build-migration/06-03-SUMMARY.md", ".planning/phases/06-merge-groundwork-build-migration/06-REVIEW.md", ".planning/scripts/conflicted-paths.txt", ".planning/scripts/verify-merge-invariants.sh", "VERSION", "build.gradle.kts", "gradle/libs.versions.toml", "gradle/wrapper/gradle-wrapper.properties"]
covered_digest: "v1:sha256:4f1386eeba078c127503063fc095114316ff38d42ba2b5a51a164e7e1ea5d289"
behavior_unverified: 0
overrides_applied: 0
human_verification:

  - test: "Read `.planning/MERGE-CHECKLIST.md` Section 1's `owning phase`, `fork behaviour at risk`, and `adjudicating test` columns end-to-end for all 35 still-`pending` rows (plus Section 2's 8 rows) and confirm each is assigned to the phase that will genuinely resolve it, and that each of the five silently-vanishing fork behaviours (JMX connector-name segment, un-forcing of `async_insert`, UTF-8 describe-table readers, target-table filter + describe sub-column skip, and the test base class's tag/shared-container/MBean-sweep contract) appears against at least one row with a named adjudicating test."
    expected: "No row is mis-assigned to a phase that will not actually resolve it; no fork behaviour is orphaned between two phases; every named fork behaviour has a real adjudicating test/requirement ID attached."
    why_human: "This is a judgment call about future work distribution and behavioural completeness that requires domain knowledge of what each of Phases 7-10 will actually touch — no automated check can verify that an owning-phase assignment is *correct* (only that the column is *non-empty*), and 06-03-PLAN.md's own task 3 verify block flags this exact check as `<human-check>`, deferred to end-of-phase per the project's human-verify-mode convention."
---

# Phase 6: Merge Groundwork & Build Migration Verification Report

**Phase Goal:** The merge is underway on a disposable branch with a parseable, resolvable build — upstream is a named remote, an unambiguous rollback point exists, every open version decision is on the record, and upstream's version-catalog `build.gradle.kts` carries all five fork additions.
**Verified:** 2026-09-18
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

All checks below were run directly against the repository (git, gradle, grep) rather than taken from SUMMARY.md claims. `env -u TESTCONTAINERS_HOST_OVERRIDE` was used for every Gradle invocation, and only `--version`, `help`, `dependencies`, and `tasks --all` were run — no `compileJava`/`assemble`/`build`/`test`, per the explicit Phase 6 scope limit.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `upstream` remote resolves to the ClickHouse upstream URL exactly once (SYNC-01) | ✓ VERIFIED | `git remote get-url upstream` → `https://github.com/ClickHouse/clickhouse-kafka-connect.git`; `git remote -v \| grep upstream` shows exactly 2 lines (fetch+push) |
| 2 | `pre-v1.6.0-sync` names the clean pre-merge `custom_build` tip; merge lives on `sync/v1.6.0`; `custom_build` unmoved/unpushed (SYNC-02) | ✓ VERIFIED | `git rev-parse pre-v1.6.0-sync` == `git rev-parse custom_build` == `6ac2d20c...`; `git rev-list --count pre-v1.6.0-sync..custom_build` = 0; `git config --get branch.sync/v1.6.0.remote` prints nothing |
| 3 | The merge lands as one identifiable, 100%-mechanical merge commit; first/second parents are the tagged tip and `v1.6.0`; no `.planning/` churn inside it; no unmerged paths | ✓ VERIFIED | `673d29c3`'s parents are `6ac2d20` (== `pre-v1.6.0-sync`) and `789a57c2` (== `v1.6.0^{commit}`); `git diff --name-only --diff-filter=U` empty; `git show --stat 673d29c3 -- .planning/` empty |
| 4 | Every one of the 40 conflicted paths is byte-identical to upstream `v1.6.0` at the merge commit (D-02 invariant 1) | ✓ VERIFIED | Independently re-ran `git diff --quiet v1.6.0 673d29c3 -- <path>` over all 40 lines of `conflicted-paths.txt` — zero differences |
| 5 | `conflicted-paths.txt` is a genuine regeneration from `git merge-tree`, not hand-transcribed (D-09); sorted, no duplicates, 40 lines | ✓ VERIFIED | Regenerated independently from `git merge-tree --write-tree custom_build v1.6.0`; `diff` against the committed file is empty; `sort -u` is byte-identical to the file |
| 6 | `signing`/`buildconfig` plugins are gone; `build.gradle.kts`/`gradle/libs.versions.toml` came from upstream `v1.6.0` at commit 1 (BLD-01) | ✓ VERIFIED | `grep -iF 'signing\|buildconfig' build.gradle.kts` — no hits; catalog diffs against `v1.6.0` on exactly 2 marked lines (see #10) |
| 7 | Gradle wrapper bootstraps at 9.6.1 (BLD-02) | ✓ VERIFIED | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --version` → `Gradle 9.6.1` |
| 8 | The build parses and resolves — `help` and `dependencies` both succeed on the final commit | ✓ VERIFIED | Both `./gradlew help` and `./gradlew dependencies` printed `BUILD SUCCESSFUL` when run directly by this verifier |
| 9 | All five (six, including PD-01's Spotless block) fork build additions are provably present and correctly shaped: JaCoCo report-only (30/15-entry class list, no `violationRules`/`jacocoTestCoverageVerification`), `testUnit` + exact-name `check` exclusion, JDK-17 `javaLauncher` + `windows-1252` inside `tasks.withType<Test>`, LZ4 capability resolution, ben-manes `dependencyUpdates` (BLD-03..06) | ✓ VERIFIED | All 20 required string patterns present in `build.gradle.kts`; all 7 forbidden patterns (`jdkHome`, `options.release`, `JavaVersion.VERSION_11`, `jacocoTestCoverageVerification`, `violationRules`, `signing`, `buildconfig`) absent; `.class"` count = 30, `$*.class"` count = 15, `maxParallelForks` count = 2; `./gradlew tasks --all` lists `testUnit`, `jacocoForkReport`, `removeUnusedImports` |
| 10 | Four D-12 resolved-version facts hold in the actual dependency graph, not just the catalog literal (BLD-08) | ✓ VERIFIED | `./gradlew dependencies` output: `com.clickhouse` → 152 hits at `0.9.6`, 0 at `0.9.5`; `connect-api` → 15 hits at `4.1.1`, 0 at `2.7.0`; `slf4j-reload4j` → 14 hits at `2.0.17`; `at.yawk.lz4` → 27 hits |
| 11 | `VERSION` reads `v1.6.0_gg1` (BLD-07) | ✓ VERIFIED | `cat VERSION` → `v1.6.0_gg1` |
| 12 | `.gitignore` is a genuine union (D-04); `tests.yaml` is the fork's 55-line workflow byte-identical to pre-merge (D-03) | ✓ VERIFIED | All 5 tokens (`/bin/`, `.vscode`, `.gsd`, `# AI`, `memory`) present; `diff` against `git show pre-v1.6.0-sync:.github/workflows/tests.yaml` empty, 55 lines |
| 13 | `.planning/MERGE-CHECKLIST.md` Section 1 has one row per conflicted path, same sorted order, all 7 columns non-empty, `status=pending` except Phase 6's own 5 rows (SYNC-03) | ✓ VERIFIED | 40/40 paths each match exactly one Section-1 row; `\| pending \|` count 43 (35 Section-1 + 8 Section-2), `\| done \|` count 5, `\| modify/delete \|` count 5 |
| 14 | `## Recorded Facts` closes SYNC-04 (resolved client version, not catalog literal) and SYNC-05 (both production-config halves, source-labelled, no operator follow-up outstanding) | ✓ VERIFIED | `0.9.6`, `4.1.1`, `2.0.17`, `at.yawk.lz4`, `repo-derived`, `operator-confirmed`, `v1.6.0_gg1`, `PD-01` all present; cross-checked `bufferCount`/`enableDbTopicSplit` defaults directly against `ClickHouseSinkConfig.java` — matches the recorded facts |
| 15 | No revision in `pre-v1.6.0-sync..HEAD` carries a conflict marker in tracked source (SYNC-06) | ✓ VERIFIED (see note below) | Independently re-scanned all commits in the range with corrected exit-code handling (distinguishing `git grep` "no match" from a real error) — zero markers, zero git errors |
| 16 | No `.planning/` commit is interleaved with in-progress conflict resolution; all post-merge commits touch only `.planning/` or the plan-scoped build files (SYNC-06) | ✓ VERIFIED | Every commit past `673d29c3` (`4f3ff2a9`, `eaf7c280`, `b582a44b`, `a87a12eb`, `1afa4479`, `ae86b603`, `09932b8f`, `b8a0c29d`) inspected via `git show --stat`; none touches an unmerged path or mixes source edits with in-flight conflict state |
| 17 | Requirement coverage: all 14 declared requirement IDs (SYNC-01..06, BLD-01..08) map 1:1 to REQUIREMENTS.md's Phase-6 rows, no orphans | ✓ VERIFIED | `grep "Phase 6" REQUIREMENTS.md` lists exactly the same 14 IDs declared across the three plans' frontmatter |
| 18 | No debt markers (TBD/FIXME/XXX) in any phase-modified file | ✓ VERIFIED | `grep -nE "TBD\|FIXME\|XXX"` over all 8 phase-modified files — zero hits |

**Score:** 18/18 truths verified (0 present-but-behavior-unverified)

### Critical Finding Weighed: CR-01 (verify-merge-invariants.sh fails open on git errors)

The phase's own code review (`06-REVIEW.md`, committed as part of this phase) flags a **Critical, unresolved** defect in `.planning/scripts/verify-merge-invariants.sh`: Invariant 2's per-commit scan is

```bash
if git grep -q -- "^${MARKER}" "${c}" -- "${SOURCE_PATHSPEC[@]}" 2>/dev/null; then
```

`git grep -q` exits `0` (match), `1` (no match), or `≥2` (real error — bad ref, corrupted object, etc.). The `if` only branches on exit `0`; any other exit code — including a genuine `git` failure — is treated identically to "no marker found," and `2>/dev/null` discards the evidence. I reproduced this directly: `git grep -q ... <bad-sha> ...` exits `128`, and the script's loop would silently continue with `fail` unchanged. This is a real, live defect in the phase's stated "safety net" artifact, not a documentation nit.

**Judgment:** I independently re-ran the full-range scan (`pre-v1.6.0-sync..HEAD`, 588 commits) using corrected logic that explicitly distinguishes exit `0` / `1` / `≥2`, and confirmed: zero conflict markers present, and zero `git` errors occurred on any commit in the actual range. **SYNC-06 and Success Criterion 5 are therefore genuinely TRUE for the repository as it exists today** — the defect did not (and could not have) produced a false pass here, because no error condition was ever triggered during this run.

However, the defect itself is real and un-fixed, and this exact script is re-invoked in Phase 11 over a still-larger range in what may be a different (e.g., CI) environment where a `git` error is more plausible. I am treating this as a **WARNING**, not a phase-6 BLOCKER: none of Plan 06-01's must-have truths asserted git-error robustness (they asserted vacuous-empty-range and vacuous-empty-worklist handling, both of which are correctly implemented and independently verified), and the goal-relevant fact (no marker in tracked source) holds by direct, corrected re-derivation. This should be fixed before Phase 11 relies on the script unattended — see Anti-Patterns Found.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.planning/scripts/verify-merge-invariants.sh` | D-02/SYNC-06 invariant script, executable, self-testing | ✓ VERIFIED (wired, functionally correct for the observed range; CR-01 noted above) | Executable, exits 0 against `673d29c3 HEAD`; correctly fails on empty-worklist and zero-commit-range degenerate inputs (re-tested) |
| `.planning/scripts/conflicted-paths.txt` | Generated 40-line worklist | ✓ VERIFIED | Byte-identical to fresh `git merge-tree` regeneration; sorted; no duplicates |
| `gradle/libs.versions.toml` | Upstream catalog + 2 marked fork overrides | ✓ VERIFIED | `clickHouseDriver=0.9.6`, `kafka=4.1.1`, both `# FORK OVERRIDE`; no other key touched; zero `reload4j` entries |
| `build.gradle.kts` | Upstream version-catalog file + 6 fork additions | ✓ VERIFIED | All required patterns present, all forbidden patterns absent (see Truth #9) |
| `VERSION` | `v1.6.0_gg1` | ✓ VERIFIED | Exact match |
| `.gitignore` | Hand-authored union | ✓ VERIFIED | All 5 tokens present |
| `.github/workflows/tests.yaml` | Fork's 55-line workflow, byte-identical | ✓ VERIFIED | `diff` empty, 55 lines |
| `.planning/MERGE-CHECKLIST.md` | 3-section checklist (Section 1, Section 2, Recorded Facts) | ✓ VERIFIED | All sections present with required content; 40/8 rows; facts confirmed against source |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `gradle/wrapper/gradle-wrapper.properties` | `gradle/wrapper/gradle-wrapper.jar` | Both arrive from upstream in commit 1 | ✓ WIRED | `git rev-parse HEAD:gradle/wrapper/gradle-wrapper.jar` == `v1.6.0`'s blob; properties file names `gradle-9.6.1-bin.zip`; `./gradlew --version` confirms bootstrap succeeded |
| `.planning/scripts/conflicted-paths.txt` | `.planning/MERGE-CHECKLIST.md` Section 1 | Generating source → checklist rows | ✓ WIRED | Every one of the 40 lines maps 1:1 to exactly one checklist row, same sorted order |
| ben-manes plugin id | `dependencyUpdates` configuration block | Atomic unit — Kotlin type reference | ✓ WIRED | Both present; `./gradlew help` (build-script compile gate) succeeds, proving they aren't split |
| `build.gradle.kts` LZ4 capability block | `gradle/libs.versions.toml` `kafka=4.1.1` override | Coupled — capability conflict only exists because of the pin | ✓ WIRED | Both landed in the same commit (`b582a44b`); resolved graph confirms `at.yawk.lz4:lz4-java:1.10.2` satisfies the capability with Kafka at `4.1.1` |
| `.github/scripts/jacoco_summary.py` hardcoded paths | `build.gradle.kts` JaCoCo report output paths | Must stay byte-matched | ✓ WIRED | `reports/jacoco/fork/jacocoForkReport.xml` present in `build.gradle.kts`; `git diff --stat -- .github/scripts/jacoco_summary.py` empty (untouched) |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Wrapper bootstraps at pinned version | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --version` | `Gradle 9.6.1` | ✓ PASS |
| Build-script compiles/configures | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` | `BUILD SUCCESSFUL` | ✓ PASS |
| Dependency graph resolves with all 4 D-12 facts | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew dependencies` | `BUILD SUCCESSFUL`; 0.9.6/4.1.1/2.0.17/at.yawk.lz4 all present, 0.9.5/2.7.0 absent | ✓ PASS |
| Fork-added tasks registered in Gradle's own task graph | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew tasks --all` | `testUnit`, `jacocoForkReport`, `removeUnusedImports` all listed | ✓ PASS |
| Invariant script self-test on full range | `bash .planning/scripts/verify-merge-invariants.sh 673d29c3... HEAD` | exit 0 | ✓ PASS |
| Invariant script re-derived with corrected error handling (addresses CR-01) | manual loop, `git grep` per commit with explicit `$rc` branching | 0 markers, 0 git errors across 588 commits | ✓ PASS |

Per the explicit scope limit for this phase, `compileJava`/`assemble`/`build`/`test`/`testUnit` were deliberately **not** run — `src/main` is not re-expressed against upstream's rewritten APIs until Phase 7, so their failure would be expected noise, not signal.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| SYNC-01 | 06-01 | Upstream named remote | ✓ SATISFIED | Truth #1 |
| SYNC-02 | 06-01 | Rollback tag + scratch branch | ✓ SATISFIED | Truth #2 |
| SYNC-03 | 06-03 | Per-path resolution checklist | ✓ SATISFIED | Truth #13 |
| SYNC-04 | 06-03 | Resolved client version recorded | ✓ SATISFIED | Truth #14 |
| SYNC-05 | 06-03 | Production-config audit recorded | ✓ SATISFIED | Truth #14 |
| SYNC-06 | 06-01, 06-03 | Single mechanical merge, no conflict marker, no interleaved `.planning/` commits | ✓ SATISFIED (CR-01 weighed, judged non-blocking — see above) | Truths #3, #15, #16 |
| BLD-01 | 06-01, 06-02 | Upstream build file + catalog, dead plugins dropped | ✓ SATISFIED | Truth #6 |
| BLD-02 | 06-01, 06-02 | Gradle wrapper 9.6.1 | ✓ SATISFIED | Truth #7 |
| BLD-03 | 06-02 | JaCoCo report-only | ✓ SATISFIED | Truth #9 |
| BLD-04 | 06-02 | `testUnit` + check exclusion | ✓ SATISFIED | Truth #9 |
| BLD-05 | 06-02 | JDK-17 test-JVM pin + encoding | ✓ SATISFIED | Truth #9 |
| BLD-06 | 06-02 | LZ4 capability + ben-manes plugin | ✓ SATISFIED | Truth #9 |
| BLD-07 | 06-02 | `VERSION=v1.6.0_gg1` | ✓ SATISFIED | Truth #11 |
| BLD-08 | 06-02 | Resolved dependency versions reconciled | ✓ SATISFIED | Truth #10 |

No orphaned requirements: `REQUIREMENTS.md`'s Phase-6 rows list exactly these 14 IDs, matching the union of all three plans' `requirements:` frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `.planning/scripts/verify-merge-invariants.sh` | 43-48 | `git grep -q ... 2>/dev/null` branches only on exit 0; a real `git` error (exit ≥2) is silently treated as "no marker found," with stderr discarded | 🛑 Critical (code-review CR-01), judged **non-blocking for Phase 6** — see explicit judgment above | Latent fail-open risk in the sole safety net reused by Phase 11; recommend fixing before Phase 11 depends on this script unattended |
| `.planning/scripts/verify-merge-invariants.sh` | 22-29, 38-41 | Invariant 1 and the empty-range guard also don't distinguish a "differs" from a `git` command error; diagnostics can mislead a debugger toward content drift when the real cause is a bad ref | ⚠️ Warning (code-review WR-01) | Both paths happen to fail closed today (confirmed non-blocking), but printed messages could send a future debugger down the wrong path |
| `build.gradle.kts` | 6-13 vs merged file | A seventh, unlisted `build.gradle.kts` divergence from upstream (project-wide JDK-17 toolchain replacing upstream's JDK-11 `sourceCompatibility`/`targetCompatibility`/`options.release`) is a real, intentional, pre-existing fork decision but is not named in `MERGE-CHECKLIST.md`'s enumeration of build-file differences (code-review WR-02) | ⚠️ Warning | Documentation-completeness gap for future rebases, not a functional defect; recommend adding as a Recorded Fact |
| `.github/workflows/tests.yaml` | 1-56 | No `permissions:` block (WR-03); actions pinned to mutable tags not SHAs (WR-04) | ⚠️ Warning | Standard CI supply-chain hardening gaps, out of Phase 6's stated scope (workflow content itself is correctly restored per D-03) |

No `TBD`/`FIXME`/`XXX` debt markers found in any phase-modified file.

### Human Verification Required

### 1. Section 1 owning-phase / adjudicating-test assignment review

**Test:** Read `.planning/MERGE-CHECKLIST.md` Section 1's `owning phase`, `fork behaviour at risk`, and `adjudicating test` columns end-to-end for the 35 still-`pending` Section 1 rows and 8 Section 2 rows, and confirm each is assigned to the phase that will genuinely resolve it, and that all five silently-vanishing fork behaviours (JMX connector-name segment, un-forcing of `async_insert`, UTF-8 describe-table readers, target-table filter + describe sub-column skip, test base class's tag/shared-container/MBean-sweep contract) each appear against at least one row with a named adjudicating test.
**Expected:** No row mis-assigned to a phase that will not actually resolve it; no fork behaviour orphaned between two phases.
**Why human:** This is a domain-knowledge judgment call about future-phase scope, not a presence/absence fact a grep can verify. 06-03-PLAN.md's own task 3 explicitly marks this a `<human-check>` — I performed a spot check of the first ~30 rows and found the assignments internally consistent with the ROADMAP's phase descriptions (mapping/writer/helper/config → Phase 7, orchestration/JMX → Phase 8, harness/fixtures/integrationTest → Phase 10, zero rows on Phase 9 as expected since no path is JSON-path-specific), but a full end-to-end read against Phases 7-10's actual future scope is the kind of judgment this project's own plan explicitly defers to a human.

### Gaps Summary

No gaps. All 18 observable truths derived from the ROADMAP's 5 Success Criteria and the three plans' `must_haves` are independently verified true against the actual repository state (git refs, commit shapes, file contents, and a live `./gradlew help`/`dependencies`/`tasks --all`/`--version` run). Requirement coverage is complete with no orphans. The one Critical code-review finding (CR-01, safety-net script fails open on `git` errors) was investigated directly, reproduced, and judged non-blocking for Phase 6's own goal because the actual invariant it's supposed to check (no conflict marker across the full commit range) was independently re-derived as true using corrected exit-code handling — but it remains an open, unfixed defect that should be addressed before Phase 11 relies on this exact script unattended. Status is `human_needed` solely because of the one deferred human-check item (Section 1 owning-phase/adjudicating-test correctness), which the plan itself explicitly defers to human judgment.

---

_Verified: 2026-09-18_
_Verifier: Claude (gsd-verifier)_
