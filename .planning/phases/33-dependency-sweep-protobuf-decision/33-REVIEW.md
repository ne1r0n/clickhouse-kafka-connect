---
phase: 33-dependency-sweep-protobuf-decision
reviewed: 2026-10-28T00:00:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - .github/dependabot.yml
  - src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java
  - docs/FORK-DELTA.md
  - gradle/libs.versions.toml
  - gradle/wrapper/gradle-wrapper.properties
  - .github/workflows/tests.yaml
  - .github/workflows/release.yaml
  - .github/workflows/pre-release.yaml
  - .github/workflows/nightly-integration-tests.yaml
findings:
  critical: 0
  warning: 2
  info: 3
  total: 5
status: issues_found
---

# Phase 33: Code Review Report

**Reviewed:** 2026-10-28
**Depth:** standard
**Files Reviewed:** 9
**Status:** issues_found

## Summary

Nothing here is a blocker. The four dependency bumps are clean one-line changes.
- `lz4Java` 1.12.1 and okhttp 5.5.1 are catalog-only edits.
- The setup-java pin `b843ebf9...` resolves upstream to tag `v6.1.0`, and it is the same on all four fork-owned workflows (tests, release, pre-release, nightly). `integrationTests.yaml` is untouched.
- The wrapper properties change only the distribution URL.
- No stale `1.12.0`, `5.5.0` or `v6.0.1` references remain in FORK-DELTA. The `v6.0.1` in `TestsWorkflowPinGuardUnitTest` is a fixture constant.

The new `ignore` block in `dependabot.yml` is valid Dependabot syntax and matches the coordinates in `libs.versions.toml` and `build.gradle.kts` (`protoc` is declared at line 541 on the same `libprotobuf` ref).

I ran `DependabotProtobufIgnoreGuardUnitTest` with `./gradlew test --tests '*DependabotProtobufIgnoreGuardUnitTest' --offline`. It passes. Spotless (`importOrder` and `removeUnusedImports` only) reports nothing for the new file.

The findings are robustness gaps in the guard test and in the documented decision. All of them could let the guard report the wrong thing, or fail to report, after a later edit of `dependabot.yml`.

## Warnings

### WR-01: Guard reports "no `ignore:` list" for a valid `ignore:` key that carries a trailing comment

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java:141` (also `:245`)
**Issue:** `ignoreItems` and `githubActionsIgnoreProblem` find the key with `line.strip().equals("ignore:")`. The sibling `ecosystemEntry` regex deliberately tolerates a trailing ` # comment`, and `nonCommentLines` only drops whole-line comments. A maintainer who writes `ignore:   # FORK: see FORK-DELTA` (inline instead of the two-line comment now at `dependabot.yml:15-16`) gets a valid Dependabot config.
- For the gradle entry, the guard fails with the misleading "has no `ignore:` list".
- For the github-actions entry, `githubActionsIgnoreProblem` silently passes while an `ignore:` is present. This is a false pass, and the one place the guard can miss a real violation.

The two entry lookups are inconsistent with each other, and the test never exercises the trailing-comment form.
**Fix:** Match the key with a regex that tolerates whitespace and a trailing comment, and use it in both places:
```java
private static final Pattern IGNORE_KEY_LINE = Pattern.compile("^\\s*ignore:\\s*(?:#.*)?$");
// ignoreItems:            IGNORE_KEY_LINE.matcher(block.get(i)).matches()
// githubActionsIgnoreProblem: block.get().stream().anyMatch(l -> IGNORE_KEY_LINE.matcher(l).matches())
```
Add fixtures for `ignore: # note` in both entries (accepted for gradle, rejected for github-actions).

### WR-02: Guard checks only the first `gradle` entry and the first item per name, so a later duplicate can weaken or bypass the ignore unseen

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java:93-107, 213`
**Issue:** `entryBlock` returns after the first matching `package-ecosystem: "gradle"` entry. If a second gradle entry is added (for example another `directory`, which Dependabot allows), the guard never reads it. It also never checks that the protobuf ignore is on the entry that actually covers `/` and `gradle/libs.versions.toml`. Within an ignore list, `findFirst()` validates only the first item for each name.
- A later `- dependency-name: "com.google.protobuf:protobuf-java"` item is ignored by the guard. Extra items only add ignores, so this one is harmless.
- A second gradle entry that covers `gradle/libs.versions.toml` without the ignore would let PR #10 come back.

The javadoc and the FORK-DELTA row say the guard pins "the `gradle` entry", singular.
**Fix:** Make the lookup strict, so a layout change fails loudly instead of passing unseen. Either fail when more than one gradle entry exists, or require the ignore in every gradle entry whose `directory` is `/`:
```java
long gradleEntries = lines.stream().filter(l -> GRADLE_ENTRY.matcher(l).matches()).count();
if (gradleEntries != 1) {
    return Optional.of(FILE + " must have exactly one gradle package-ecosystem entry, found " + gradleEntries);
}
```

## Info

### IN-01: Documented protoc coverage is unverified, and the guard only pins file text

**File:** `docs/FORK-DELTA.md:71`, `.github/dependabot.yml:20-21`
**Issue:** FORK-DELTA states that "whether Dependabot detects `protoc` through that interpolated coordinate is not verified". The test pins that the `protoc` item exists, not that it has any effect. The real protection is the `protobuf-java` item, because both coordinates read the one `libprotobuf` ref. That is a sound decision, but the new test's name and javadoc present `protoc` as a guarded requirement. The test's `shared` message (lines 215-220) already argues the dependency correctly. It would be more honest if the javadoc and `REQUIRED_NAMES` comment said the `protoc` item is defensive and unverified.
**Fix:** Add one sentence to the javadoc: "The `protoc` item is defensive; Dependabot may not parse the interpolated `build.gradle.kts` coordinate". After the next Dependabot run, record whether the `libprotobuf` ref still gets a protobuf PR. FORK-DELTA already schedules that re-read.

### IN-02: Wrapper distribution is downloaded without a checksum

**File:** `gradle/wrapper/gradle-wrapper.properties:3`
**Issue:** There is no `distributionSha256Sum`. The 9.8.2 bump downloads the zip over HTTPS with only `validateDistributionUrl=true`, which checks the URL, not the content. A bumped wrapper would normally be re-pinned. This is pre-existing (the other wrapper properties lines are unchanged), but a wrapper bump is the natural moment to add the checksum.
**Fix:** Add `distributionSha256Sum=<sha256 of gradle-9.8.2-bin.zip from services.gradle.org>`. Verify it against the published `.sha256` file before committing. Note that Dependabot's wrapper updates keep a checksum current once one is present.

### IN-03: Long-lived FORK-DELTA text depends on planning-phase references

**File:** `docs/FORK-DELTA.md:84, 209`
**Issue:** The text says "Phase 38 re-reads it and the user closes it" and "recorded as closing once the `ignore` reaches `custom_build` at the `v1.7.0_gg6` publication". It also cites `33-COMMAND-EVIDENCE.md` E-xx throughout. These point at `.planning/` artifacts and future phase numbers. They go stale when phases are renumbered or archived, and the doc is meant to survive upstream rebases. This is a documentation maintainability concern only.
**Fix:** When Phase 38 runs, replace the forward references with the outcome ("PR #10 closed on <date>"). Optionally state the open-PR follow-up as a condition rather than a phase number.
