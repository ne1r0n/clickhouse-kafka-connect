---
phase: 33-dependency-sweep-protobuf-decision
verified: 2026-10-28T20:40:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".github/dependabot.yml"
  - ".github/workflows/nightly-integration-tests.yaml"
  - ".github/workflows/pre-release.yaml"
  - ".github/workflows/release.yaml"
  - ".github/workflows/tests.yaml"
  - ".planning/phases/33-dependency-sweep-protobuf-decision/33-01-PLAN.md"
  - ".planning/phases/33-dependency-sweep-protobuf-decision/33-01-SUMMARY.md"
  - ".planning/phases/33-dependency-sweep-protobuf-decision/33-02-PLAN.md"
  - ".planning/phases/33-dependency-sweep-protobuf-decision/33-02-SUMMARY.md"
  - ".planning/phases/33-dependency-sweep-protobuf-decision/33-03-PLAN.md"
  - ".planning/phases/33-dependency-sweep-protobuf-decision/33-03-SUMMARY.md"
  - "docs/FORK-DELTA.md"
  - "gradle/libs.versions.toml"
  - "gradle/wrapper/gradle-wrapper.properties"
  - "src/test/java/com/clickhouse/kafka/connect/ci/DependabotProtobufIgnoreGuardUnitTest.java"
covered_digest: "v4:sha256:9d029f2452731841f09b18da1111c4811ab0cc7372ab52902007769c3b514345"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 33: Dependency Sweep & Protobuf Decision Verification Report

**Phase Goal:** Every later phase builds on a tree that already holds the four safe Dependabot bumps (#19 lz4-java first, #18 okhttp, #20 Gradle wrapper, #16 setup-java), each in its own commit; PR #10 is settled by decision (protobuf-java stays 3.x, Dependabot ignores 4.x in the `gradle` block, FORK-DELTA records why); nothing is merged on `origin`; a rollback tag `pre-v1.7.0_gg6` marks the start; no connector behaviour changes.
**Verified:** 2026-10-28
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Origin re-read before the first change; local `pre-v1.7.0_gg6` tag exists; each bump is a fetched PR head merged on the scratch branch with SHA recorded; origin untouched at gate (SWP-04) | VERIFIED | Live: `git rev-parse pre-v1.7.0_gg6` = `3a3a15a9`, equal to `origin/custom_build` per live `git ls-remote origin` (`custom_build` 3a3a15a9, `main` bec167d8 unchanged, no `sync/v1.7.0` or tag on origin). The four PR heads (#19 2a806038, #18 cb898dc2, #20 8786723b, #16 cb3c7718) are each second parent of a merge commit on `sync/v1.7.0` and match the live `refs/pull/N/head` values on origin. E-01, E-03, E-07 to E-09, E-14 record the re-reads. |
| 2 | lz4-java 1.12.1 (PR #19) lands first in its own commit; `dependencyInsight` and the archive show 1.12.1; full suite green with 18 skips (SWP-01) | VERIFIED | `6bf1c43b` is the first merge after the tag, touches only `gradle/libs.versions.toml` (`lz4Java 1.12.0 -> 1.12.1`; live diff confirmed). Recorded E-04: `dependencyInsight` 1.12.1 on `runtimeClasspath`, archive `pom.properties` version 1.12.1, `verifyConfluentArchive` passed. E-05: 1961 tests, 0 fail, 0 error, 18 skipped on `latest` x V2. |
| 3 | okhttp 5.5.1, Gradle 9.8.2, setup-java 6.1.0 each in own commit with own gate; wrapper commit changes only `gradle-wrapper.properties`, `gradlew.bat` CRLF intact; setup-java full-SHA pin with `# v6.1.0`; pin-guards pass; full suite green on sweep tip (SWP-02) | VERIFIED | Merges in order `e075b2cc` (okhttp, libs.versions.toml only), `e4f49dca` (wrapper, `gradle-wrapper.properties` only), `79f56a5e` (4 workflow files, one line each). Live: `./gradlew --version` reads Gradle 9.8.2; `gradlew.bat` is CRLF (`file`); all four fork workflows pin `actions/setup-java@b843ebf9b913146b38da2eaa7aeddf76074ae1ad # v6.1.0`. Live `testUnit` of `WorkflowActionPinGuardUnitTest` (21), `TestsWorkflowPinGuardUnitTest` (39), `DependabotProtobufIgnoreGuardUnitTest` (13): 0 failures. E-07 `compileIntegrationTestJava` exit 0; E-10 full suite on sweep tip 1961/0/0/18; anchors mirror `rows=131 bad=0` after every bump. |
| 4 | PR #10 not merged; `dependabot.yml` ignores protobuf-java majors in the `gradle` block only; pin guard passes; FORK-DELTA records cause (`VerifyError`, run 39501799049) and FUT-03 trigger; PR #10 closed or recorded as closing at publication (SWP-03) | VERIFIED | Live: PR #10 head `011bc42b` is not an ancestor of `HEAD`; origin still lists it as an open PR ref. `.github/dependabot.yml` `gradle` entry carries an `ignore:` for `com.google.protobuf:protobuf-java` and `com.google.protobuf:protoc` with `version-update:semver-major`; the `github-actions` entry has none (diff vs tag: +7 lines, insertions only). `libprotobuf = "3.25.9"` still in the catalog. FORK-DELTA has the new protobuf row and open item 12 with the `VerifyError` cause, run id and FUT-03 trigger; PR #10 recorded as "closing once the ignore reaches `custom_build` at the `v1.7.0_gg6` publication". Guard was seen red then green (E-12), then hardened by WR-01/WR-02 fixes (13 tests, green live). |

**Score:** 4/4 truths verified (0 behavior-unverified)

Additional goal clauses:

| Clause | Status | Evidence |
|--------|--------|----------|
| No connector behaviour changes | VERIFIED | Live: `git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main` exits 0 (empty). The only `src/test` change is the new guard class. |
| Merge order #19, #18, #20, #16 | VERIFIED | Live `git log --first-parent`: `6bf1c43b` (#19), `e075b2cc` (#18), `e4f49dca` (#20), `79f56a5e` (#16). |
| Nothing merged on `origin` | VERIFIED | Live `git ls-remote origin`: `custom_build` equals the tag; all Dependabot branches and `refs/pull/*/head` SHAs match those recorded in E-01. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/libs.versions.toml` | lz4Java 1.12.1, okhttp 5.5.1, libprotobuf 3.25.9 | VERIFIED | Diff vs tag shows exactly the two bumps; protobuf untouched |
| `gradle/wrapper/gradle-wrapper.properties` | 9.8.2 distribution | VERIFIED | One-line change; live `--version` 9.8.2 |
| `.github/workflows/{tests,release,pre-release,nightly-integration-tests}.yaml` | setup-java v6.1.0 full SHA pin | VERIFIED | 1 line each |
| `.github/dependabot.yml` | gradle-block protobuf ignore | VERIFIED | Substantive and guard-pinned |
| `DependabotProtobufIgnoreGuardUnitTest.java` | Guard test | VERIFIED | 13 tests, 0 failures live |
| `docs/FORK-DELTA.md` | Decision, cause, trigger, refreshed rows | VERIFIED | Diff read |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| Merge commits | PR heads on origin | second parent equals `refs/pull/N/head` | WIRED |
| `dependabot.yml` ignore | guard test | reads real file in `testUnit` | WIRED |
| FORK-DELTA protobuf row | `dependabot.yml` / FUT-03 | text references, Pinned-by cell | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Wrapper version | `./gradlew --version` | Gradle 9.8.2 | PASS |
| Pin and ignore guards | `testUnit --rerun` on the three guard classes | 21/39/13 tests, 0 failures | PASS |
| `src/main` unchanged | `git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main` | exit 0 | PASS |

The full suite (E-10, E-14: 1972 tests, 0 failures, 18 skipped, ClickHouse 26.10.3.7) and the ~20 min cumulative invariants gate (E-14: `result=PASS`, self-test no failures) were not re-run, per instruction; recorded evidence relied on. The post-gate review fixes (WR-01, WR-02) touched only the guard test class, which was re-run live.

### Probe Execution

SKIPPED. No probe scripts are declared by this phase's plans.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SWP-01 | 33-01, 33-02, 33-03 | lz4-java 1.12.1 own commit, suite green | SATISFIED | Truth 2 |
| SWP-02 | 33-02, 33-03 | okhttp, wrapper, setup-java each own commit | SATISFIED | Truth 3 |
| SWP-03 | 33-03 | PR #10 declined by ignore + FORK-DELTA record | SATISFIED (see info item) | Truth 4 |
| SWP-04 | 33-01, 33-02, 33-03 | Merges on scratch branch, rollback tag first | SATISFIED | Truth 1 |

All four IDs in PLAN frontmatter appear in REQUIREMENTS.md and map to Phase 33; no orphaned requirements.

### Anti-Patterns Found

No TBD/FIXME/XXX debt markers were introduced in the phase's implementation files. No stubs. No blockers.

### Info Items (not gaps)

1. **SWP-03 wording gap.** REQUIREMENTS.md says PR #10 "is closed"; ROADMAP criterion 4 (governing) accepts "recorded as closing once the `ignore` reaches `custom_build` at publication". PR #10 is open on origin and recorded as closing at publication, which meets the ROADMAP criterion. The user may want to align REQUIREMENTS.md text. The executor flagged this in 33-03-SUMMARY.md and did not edit it.
2. **`gradlew.bat` diff vs tag** is a line-ending renormalisation from commit `f71468bc`, which predates the phase; no sweep commit touches the file, and it is CRLF.
3. **Open review findings** IN-01 (protoc detection by Dependabot unverified; guard pins file text only), IN-02 (wrapper distribution checksum is not validated in the properties; E-08 did compare the SHA-256 manually), IN-03 (FORK-DELTA text references planning evidence). All are info-level and dispositioned as open.
4. **Pending publication work:** PR #10 and PRs #16 to #20 remain open on origin until Phase 38 publishes; this is expected and must not be merged on origin before then.

### Human Verification Required

None.

### Gaps Summary

No gaps. All four ROADMAP success criteria and the extra goal clauses are supported by live git/file/test evidence and by recorded gate evidence (E-01 to E-14).

---

_Verified: 2026-10-28_
_Verifier: Claude (gsd-verifier)_
