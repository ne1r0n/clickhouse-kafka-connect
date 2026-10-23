---
phase: 21-dependabot-integration-ci-hygiene
reviewed: 2026-10-18T00:00:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - gradle/libs.versions.toml
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
findings:
  critical: 0
  warning: 3
  info: 4
  total: 7
status: issues_found
---

# Phase 21: Code Review Report

**Reviewed:** 2026-10-18
**Depth:** standard
**Files Reviewed:** 4
**Status:** issues_found

## Summary

The three Dependabot bumps (lombok 1.18.48, spotless 8.10.4, slf4j-reload4j 2.0.20) are mechanical and consistent. The `build.gradle.kts` literal, the `.planning/scripts/fork-anchors.txt` row and the two FORK-DELTA rows all agree on the new values. `TestsWorkflowPinGuardUnitTest` passes against the real `tests.yaml`, `dependabot.yml` and `build.gradle.kts` (`./gradlew --offline testUnit --tests '*TestsWorkflowPinGuardUnitTest'` exits 0).

No BLOCKER was found. The refactored rules are line-level heuristics with several holes. Some give false failures on legitimate edits, and some can be satisfied by text that is not live configuration. The guard's purpose is to catch regressions, so these weaken it in both directions. The remaining findings are documentation drift and duplication.

## Warnings

### WR-01: runs-on rule rejects a pinned runner whose trailing comment mentions the floating label

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:137-144`
**Issue:** `RUNS_ON` is written to tolerate a trailing comment, and the fixtures `runs-on: ubuntu-24.04 # pinned` are accepted. `nonCommentLines` only drops lines that start with `#`. The second loop in `runsOnProblem` then runs `line.contains("ubuntu-latest")` over every remaining line, including trailing comments. `runs-on: ubuntu-24.04 # was ubuntu-latest` therefore fails with "The floating runner label must not appear outside comments", although the label is in a comment. The message contradicts the behaviour. A harmless explanatory comment next to the pin breaks `testUnit`, which pushes people to delete the comment or weaken the rule.
**Fix:** Strip trailing YAML comments (whitespace followed by `#`) before the scan, or restrict the scan to `runs-on:` lines:
```java
for (String line : lines) {
    String code = line.replaceFirst("\\s#.*$", "");
    if (code.contains(FLOATING_RUNNER)) { ... }
}
```
Add a fixture such as `rejects`/`accepts(rule, "    runs-on: ubuntu-24.04 # was ubuntu-latest")` to pin the chosen behaviour.

### WR-02: `ktsCode` comment stripping is not string- or line-comment-aware

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:305-309`
**Issue:** `ktsCode` removes block comments with `(?s)/\*.*?\*/` over the whole text before it filters `//` lines. Two consequences:
1. A `//` line comment, or a string literal, that contains `/*` (for example a glob such as `"**/*Test.class"`, or `// excludes /* ...`) opens a phantom block comment. It runs to the next `*/` anywhere in `build.gradle.kts`, and it can swallow the `reportTestForks` registration. The rule then reports "not found outside comments" for code that is live.
2. Only lines that begin with `//` are filtered. Trailing `// ...` text stays in the "code", which feeds WR-03.

The current `build.gradle.kts` has only three balanced real block comments (lines 108/110, 353/355, 476/486), so the rule passes today. It is fragile against ordinary future edits to a 500-line build script.
**Fix:** Tokenise with a small scanner that tracks string literals and strips comments in one pass. Alternatively strip trailing `//` per line first, and only then match block comments at the start of a token. Add fixtures for a `/*` inside a string and inside a `//` comment.

### WR-03: forkTaskProblem and the fork-count step rule accept text that is not live code

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:56-57, 266-269, 231`
**Issue:** The rules exist to stop `reportTestForks` and the workflow step from degrading to a constant or to a shell formula. They are satisfied by substring presence only:
- `forkTaskProblem` sets `readsTestTask`/`readsForks` when any line in the block `contains` `tasks.named<Test>("test")` or `maxParallelForks`. `println(6) // tasks.named<Test>("test").maxParallelForks` passes, as does a `val` that is declared but never printed.
- `GRADLEW_REPORT` excludes only lines that start with `echo`/`printf`. A line such as `x=1 # ./gradlew reportTestForks`, `: ./gradlew reportTestForks`, or `false && ./gradlew reportTestForks` counts as the call. The rule does not check that the output feeds `forks`.

The rejection fixtures cover only the obvious shapes, so the guard can be defeated by an innocuous-looking edit. That is the opposite of its CI-18 purpose.
**Fix:** In `forkTaskProblem`, strip trailing `//` comments (see WR-02) and require that the `doLast` body prints the value derived from the `forks` provider. For the workflow, anchor the call to the assignment, for example `^\s*forks="?\$\(\./gradlew\s[^)]*\breportTestForks\b`. Add fixtures for the comment-only and `&&`-guarded variants.

## Info

### IN-01: dependabot parsing is stricter than YAML and silently depends on one indent style

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:206-217, 324-345`
**Issue:** `excludePaths` breaks as soon as `indent(line) <= keyIndent`. A valid YAML sequence indented at the same column as its key,
```yaml
    exclude-paths:
    - ".github/workflows/release.yaml"
```
makes the list read as empty, so the rule reports "no exclude-paths list" for a correct file. `dependabotEcosystemProblem` and `dependabotExcludeProblem` also require the exact text `- package-ecosystem: "github-actions"` (double quotes, no trailing comment), so a single-quoted or commented entry fails as "missing". These are false failures, not missed regressions, and the real file uses the matching style.
**Fix:** Either document the supported layout in the Javadoc and in the failure message, or accept a dash at the key's indent and either quote style.

### IN-02: Fixture list duplicates the production constant

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:74-79, 460-465`
**Issue:** `UPSTREAM_WORKFLOWS` (the rule's expected list) and the fixture `UPSTREAM` hold the same four paths, built differently. If the fork starts excluding a fifth workflow, the rule changes and the "accepts" fixtures still pass only if someone also edits the fixture. The class Javadoc also still describes "T-14-16 and CI-11 to CI-13, plus the Dependabot entry of CI-15", but the class now also pins CI-18 and CI-19, the fork-count step, the `reportTestForks` task and the `exclude-paths` list.
**Fix:** Build `UPSTREAM` from `UPSTREAM_WORKFLOWS`, and refresh the Javadoc scope.

### IN-03: FORK-DELTA BLD-14 evidence not re-established for Lombok 1.18.48

**File:** `docs/FORK-DELTA.md:65`
**Issue:** The row now states that "the catalog's Lombok 1.18.48 warns at compile time when it cannot tell which Jackson major `@Jacksonized` targets" and that "a `javap` diff shows `ClickHouseFieldDescriptor`'s generated code unchanged". Both claims cite Phase 17 evidence E-07, which was gathered on 1.18.46. Only the version number was edited, so the doc asserts a verified fact for a version it was not verified on. `.claude/CLAUDE.md:57` also still lists "SLF4J with Reload4j 2.0.17", which now disagrees with `build.gradle.kts:106`.
**Fix:** Either re-run the warning and `javap` check on 1.18.48 and cite it, or word the row as "verified on 1.18.46; bumped by Dependabot to 1.18.48". Update the stale `.claude/CLAUDE.md` entry.

### IN-04: slf4j-reload4j is versioned independently of the slf4j catalog entry

**File:** `build.gradle.kts:106`, `gradle/libs.versions.toml:28`
**Issue:** `slf4j-reload4j` is a literal `2.0.20`, while the catalog `slf4j = "2.0.18"` drives `slf4j-simple` and the other slf4j artifacts. Binding and API versions now drift two patch releases apart. This is harmless in practice because Gradle resolves `slf4j-api` to the highest version. It is the third time a Dependabot bump of the literal has needed a manual `fork-anchors.txt` and FORK-DELTA move (see `.planning/scripts/fork-anchor-exemptions.txt`).
**Fix:** Optional. Either add a catalog entry for `slf4j-reload4j` (and give up the "no upstream catalog entry" rationale in BLD-08), or leave it and keep relying on the anchor exemption. The version skew is only worth noting if a future slf4j-api and binding major diverge.

---

_Reviewed: 2026-10-18_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
