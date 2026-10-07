---
phase: 06-merge-groundwork-build-migration
reviewed: 2026-09-18T00:00:00Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - .planning/scripts/verify-merge-invariants.sh
  - .planning/scripts/conflicted-paths.txt
  - .gitignore
  - gradle/libs.versions.toml
  - .github/workflows/tests.yaml
  - build.gradle.kts
  - VERSION
  - .planning/MERGE-CHECKLIST.md
findings:
  critical: 1
  warning: 4
  info: 2
  total: 7
status: issues_found
---

# Phase 6: Code Review Report

**Reviewed:** 2026-09-18T00:00:00Z
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Phase 6 re-applies the fork's six build additions onto upstream `v1.6.0`'s `build.gradle.kts`, unions the `.gitignore`, keeps the fork's two version-catalog overrides, rewrites the CI workflow to the single-job `latest`/V2 form, and introduces a merge-safety-net script plus its supporting checklist/manifest files. I diffed the current tree against `pre-v1.6.0-sync` and `v1.6.0` directly rather than trusting the narrative: the build-file re-application is byte-accurate against the claimed "six additions" (JaCoCo report-only wiring, `testUnit` + exact-name `check` exclusion, JDK-17 test launcher + `windows-1252`, LZ4 capability resolution, ben-manes `dependencyUpdates`, Spotless config), the `.gitignore` is a genuine union with no dropped entries, and `gradle/libs.versions.toml` differs from upstream on exactly the two claimed keys (`clickHouseDriver`, `kafka`). The JaCoCo XML output paths also line up with what `.github/scripts/jacoco_summary.py` hardcodes.

The one real defect is in the safety-net script itself: `verify-merge-invariants.sh`'s Invariant 2 loop discards `git grep`'s stderr and only branches on exit code `0`, so a genuine `git` failure (confirmed reproducible — a bad commit-ish returns exit `128`) is indistinguishable from "no conflict marker found" and is silently swallowed rather than surfaced as a script/environment failure. This undermines the one property this script exists to guarantee. The rest of the findings are CI-hardening and documentation-completeness gaps rather than functional defects.

## Critical Issues

### CR-01: verify-merge-invariants.sh Invariant 2 fails open on git errors

**File:** `.planning/scripts/verify-merge-invariants.sh:43-48`
**Issue:** The per-commit marker scan is:
```bash
for c in ${RANGE_COMMITS}; do
  if git grep -q -- "^${MARKER}" "${c}" -- "${SOURCE_PATHSPEC[@]}" 2>/dev/null; then
    echo "INVARIANT 2 VIOLATION: ${MARKER} marker found in commit ${c}" >&2
    fail=1
  fi
done
```
`git grep -q` returns `0` when a match is found, `1` when there is no match, and `≥2` on a real error (bad tree-ish, corrupted object, pathspec/repo problem, etc.). The `if` only treats exit `0` as "found"; every non-zero exit — including a genuine failure — falls through as if the commit were clean, and stderr is discarded via `2>/dev/null` so there is no trace of the failure either. I reproduced this directly: `git grep -q -- "^<<<<<<<" <bad-sha> -- src build.gradle.kts` exits `128`, and the loop's `if` treats that identically to "no marker present." Because this script is the milestone's sole safety net for "no conflict marker survived into tracked source" (SYNC-06), a script-level failure during the scan currently produces a silent pass instead of a loud failure — the opposite of the fail-closed behavior the invariant is supposed to guarantee. (Invariant 1's `git diff --quiet` call has the same non-zero-exit ambiguity but happens to fail closed today because `!`-negation routes any non-zero exit into the violation branch — see WR-01 for why that's still worth tightening.)
**Fix:** Capture and branch on the actual exit code instead of relying on the truthiness of the `if`:
```bash
for c in ${RANGE_COMMITS}; do
  git grep -q -- "^${MARKER}" "${c}" -- "${SOURCE_PATHSPEC[@]}"
  rc=$?
  if [ "${rc}" -eq 0 ]; then
    echo "INVARIANT 2 VIOLATION: ${MARKER} marker found in commit ${c}" >&2
    fail=1
  elif [ "${rc}" -gt 1 ]; then
    echo "INVARIANT 2 ERROR: git grep failed (exit ${rc}) scanning commit ${c}" >&2
    fail=1
  fi
done
```

## Warnings

### WR-01: verify-merge-invariants.sh git-command failures produce misleading violation messages

**File:** `.planning/scripts/verify-merge-invariants.sh:22-29, 38-41`
**Issue:** Invariant 1's `if ! git diff --quiet "${UPSTREAM_REF}" "${MERGE_SHA}" -- "${path}"` treats any non-zero exit (a real "differs" as well as a `git` error, e.g. an unresolvable ref or bad `MERGE_SHA`) as "INVARIANT 1 VIOLATION: ${path} differs from ${UPSTREAM_REF}" — which is factually wrong when the underlying cause is a broken ref rather than a content difference. Similarly, `RANGE_COMMITS="$(git rev-list "${PRE_TAG}..${RANGE_END}" 2>/dev/null || true)"` swallows `git rev-list` errors (e.g. a missing `pre-v1.6.0-sync` tag) and reports the identical "range contains zero commits" message that a legitimately empty range would produce. Both paths happen to fail closed (the script still exits non-zero), but the printed diagnostics actively mislead whoever is debugging a CI failure into looking at the wrong thing (content drift) instead of the real one (missing ref / bad argument).
**Fix:** Check `git`'s own exit code before interpreting it, e.g. capture stderr into a variable and print it verbatim when the underlying command errors, distinct from the "content differs" / "zero commits" messages.

### WR-02: A material build.gradle.kts divergence from upstream is untracked by the checklist

**File:** `.planning/MERGE-CHECKLIST.md:14`, `build.gradle.kts:6-13`
**Issue:** The checklist's `build.gradle.kts` row lists exactly six named fork build additions ("JaCoCo report-only, `testUnit` + check-exclusion guard, JDK-17 test-JVM launcher + `windows-1252`, LZ4 capability resolution, ben-manes `dependencyUpdates`, Spotless config/`removeUnusedImports`"), and Recorded Fact 7 explicitly calls out Spotless as a "sixth build addition beyond the five the milestone originally named" so future readers don't mistake it for an unexplained fork addition. Diffing the merged file against upstream's `v1.6.0` copy shows a seventh, unlisted divergence: upstream compiles the whole project at JDK 11 (`java { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }`, `defaultJdkVersion = 11`, `options.release.set(11)`), while the merged file keeps the fork's project-wide `java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }` and `defaultJdkVersion = 17`, dropping upstream's `options.release.set(11)` entirely. This is a real, intentional fork decision (confirmed pre-existing on `pre-v1.6.0-sync`, and consistent with the "JDK-17 test-JVM launcher" bullet's sibling requirement in `tasks.withType<Test>`) but it is a *whole-project compile-target* change, not merely a test-JVM pin, and it changes the bytecode compatibility of the shipped library relative to upstream. It is not named anywhere in the checklist's enumeration of build.gradle.kts differences, so a future rebase reviewer relying on this document as the authoritative list of "intentional fork deltas from upstream" would not know to re-check it.
**Fix:** Add this as a documented, named divergence (e.g. a Recorded Fact similar to #7) — "the fork compiles the whole project at JDK 17 via a project-wide toolchain, replacing upstream's JDK 11 `sourceCompatibility`/`targetCompatibility`/`options.release`" — so it survives future rebases as an intentional decision rather than as an unlisted diff.

### WR-03: CI workflow has no explicit least-privilege `permissions:` block

**File:** `.github/workflows/tests.yaml:1-56`
**Issue:** The workflow declares no `permissions:` key at either the workflow or job level, so the job's `GITHUB_TOKEN` inherits whatever the repository/organization default is (which can be broader than `read`, depending on settings). The job only checks out code, runs Gradle, and uploads build artifacts — none of which require write access to repository contents or other scopes.
**Fix:** Add an explicit least-privilege block, e.g.:
```yaml
permissions:
  contents: read
```

### WR-04: Third-party GitHub Actions are pinned to mutable tags, not commit SHAs

**File:** `.github/workflows/tests.yaml:20,23,29,43,51`
**Issue:** `actions/checkout@v4`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6`, and `actions/upload-artifact@v4` (used twice) are referenced by mutable major-version tags. A tag can be repointed (by the publisher, or via a compromised publisher account) to a different commit without the workflow file changing, giving that action's new code full ambient access to the run (including `GITHUB_TOKEN` and any repo secrets in scope). This is the standard "unpinned action" supply-chain hardening gap flagged by tools like OpenSSF Scorecard.
**Fix:** Pin each action to a full commit SHA with the version as a trailing comment, e.g. `uses: actions/checkout@<sha> # v4.x.x`, and update via Dependabot/Renovate.

## Info

### IN-01: Dead branch in the concurrency-group expression

**File:** `.github/workflows/tests.yaml:8-12`
**Issue:** The workflow trigger is `on: [push]` only, yet the concurrency group is `${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}`. Since `github.event_name` can never be `'pull_request'` on this trigger, the ternary always resolves to `github.sha` — the `pull_request`/`github.event.number` branch is unreachable and reads as if `pull_request` were a supported trigger.
**Fix:** Simplify to `${{ github.workflow }}-${{ github.sha }}` (or add the `pull_request` trigger if PR-scoped concurrency is actually wanted).

### IN-02: ben-manes plugin version hardcoded inline rather than via the version catalog

**File:** `build.gradle.kts:30`
**Issue:** `id("com.github.ben-manes.versions") version "0.53.0"` hardcodes its version directly in the plugins block, while the other plugins in the same block (`spotless`, `shadow`, `protobuf`) resolve their versions through `alias(libs.plugins.*)` against `gradle/libs.versions.toml`. This is carried over unchanged from the pre-merge fork (not a regression introduced by this phase) but is an inconsistent pattern within the same file that makes future version bumps easy to miss in the catalog.
**Fix:** Add a `[versions]`/`[plugins]` entry for `ben-manes` to `gradle/libs.versions.toml` and reference it via `alias(libs.plugins.benManesVersions)` for consistency with the rest of the plugin declarations.

---

_Reviewed: 2026-09-18T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
