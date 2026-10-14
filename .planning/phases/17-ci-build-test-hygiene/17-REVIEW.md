---
phase: 17-ci-build-test-hygiene
reviewed: 2026-10-12T00:00:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - .github/dependabot.yml
  - .github/workflows/tests.yaml
  - build.gradle.kts
  - docs/FORK-DELTA.md
  - lombok.config
  - src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
status: issues_found
---

# Phase 17: Code Review Report

**Reviewed:** 2026-10-12
**Depth:** standard
**Files Reviewed:** 9
**Status:** issues_found

## Summary

Reviewed the Phase 17 diff (`e0017d69..HEAD`) for the nine listed files. I found no correctness or security defects that block shipping.

Checked and found correct:

- **Action pins.** All four SHAs in `tests.yaml` match the upstream tags. I confirmed this with `git ls-remote`.
  - `actions/checkout` v7.0.1 is `3d3c42e5…`.
  - `actions/setup-java` v6.0.1 is `de7274f0…`.
  - `gradle/actions` v6.4.0 is `3f5f9ada…`, the peeled commit of an annotated tag. The tag-object SHA is `b9bee63e…`, so FORK-DELTA's "peeled commit" wording is right.
  - `actions/upload-artifact` v7.0.2 is `cf430e03…`.
- **`reportTestForks`.** It prints `6` locally and `1` when `CLICKHOUSE_CLUSTER_NAME` is set. That matches the `maxParallelForks` formula in `tasks.withType<Test>`. The workflow step rejects empty and non-numeric output, so the missing `pipefail` is covered.
- **LZ4 classpath.**
  - `testRuntimeClasspath` resolves only `at.yawk.lz4:lz4-java:1.12.0`.
  - `integrationTestRuntimeClasspath` substitutes `org.lz4` through the capability rule, so the test-only exclude is correctly scoped.
  - Both new `*UnitTest` classes pass under `testUnit`.
- **NanoTimestamp rows.** I derived the `BigDecimal.toString` forms by hand:
  - `123` at scale 9 gives `1.23E-7`.
  - `-123` gives `-1.23E-7`.
  - `999` gives `9.99E-7`.
  - `1000` gives `0.000001000`.
  - These are the correct scientific/plain boundaries.
- **`lombok.config`.** `lombok.jacksonized.jacksonVersion += 2` is valid for Lombok 1.18.46, and `lombok.config` is not present in upstream.
- **`test-head.yaml` deletion.** A repo-wide grep (excluding `.git` and `.planning`) finds no reference outside the one `docs/FORK-DELTA.md` row that documents the deletion. No remaining workflow calls `tests.yaml` as a reusable workflow. The file does exist in `upstream/main`, so the "keep it deleted on modify/delete conflict" instruction is accurate.

The findings below are robustness gaps in the new guard and in an unverified Dependabot key.

## Warnings

### WR-01: Unobserved `exclude-paths` key could invalidate the whole Dependabot config, and the guard does not pin it

**File:** `.github/dependabot.yml:18-24`
**Issue:** Support for `exclude-paths` on the `github-actions` ecosystem was confirmed only by reading dependabot-core source (17-RESEARCH A3, FORK-DELTA CI-15 row). It has never been seen working in a real run.

The failure mode is worse than a stray PR. If the key is rejected or ignored, Dependabot may flag the whole `dependabot.yml` as invalid. That would also disable the pre-existing `gradle` updates in the same file. The "close stray PRs" fallback covers only the ignored-key case, not the rejected-key case.

`TestsWorkflowPinGuardUnitTest.dependabotUpdatesTheGithubActionsEcosystem` asserts only that the two `package-ecosystem` lines exist. It does not assert the four `exclude-paths` entries, so dropping them or adding a typo would pass the guard.

**Fix:**
- Add the check to the existing manual list for the next release: after `dependabot.yml` reaches the default branch, open Insights > Dependency graph > Dependabot and confirm there is no config error and that the gradle entry still runs.
- Extend the guard to assert that each of the four upstream workflow paths appears under `exclude-paths`, and that every entry listed there exists in `.github/workflows/`. For example:

```java
for (String wf : List.of("increment-version.yml", "integrationTests.yaml", "pre-release.yaml", "release.yaml")) {
    assertTrue(stripped.contains("- \".github/workflows/" + wf + "\""), "exclude-paths must list " + wf);
}
```

## Info

### IN-01: The guard does not pin the workflow's use of `reportTestForks`

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:48-176`
**Issue:** CI-14 (the fork-count step reads `reportTestForks` instead of a shell copy of the formula) is the other half of this tracer, and no Docker-free test covers it.
- Reverting `tests.yaml` to the old `nproc / 2` shell formula would pass every guard.
- Removing or renaming the `reportTestForks` task in `build.gradle.kts` would also pass until a push hits CI.

**Fix:** Add one line-level assertion that `tests.yaml` contains `reportTestForks` and does not contain `nproc` outside the message-formatting line. Optionally add a build-script test that the task name is registered.

### IN-02: Pin guard regexes are brittle in two small ways

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:34-37`
**Issue:**
- `RUNS_ON` captures a trailing inline comment, so `runs-on: ubuntu-24.04 # pinned` fails the equality check even though the workflow is correct. This is a false failure. It fails closed, but it is confusing.
- `WRITE_SCOPE` (`.*:\s*write\b.*`) matches any non-comment line containing `: write` followed by a word boundary. A step `name: write summary`, or a `run:` echo, would be reported as granting a write scope.

**Fix:** Strip trailing ` #...` before matching `runs-on`. Restrict `WRITE_SCOPE` to lines at 2-space indent, or to lines under the `permissions:` block, for example `^\s{2,6}[\w-]+:\s*write\s*$`.

### IN-03: The jar-origin check in `Lz4RoundTripLiveTest` now duplicates the Docker-free unit test

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java:100-110`
**Issue:** `lz4FactoryLoadsFromTheMaintainedFork` checks the first jar's code source for the `at.yawk.lz4` marker. `TestRuntimeClasspathLz4UnitTest` now checks the same marker and also counts every copy. The two share a constant and a rule, which violates the project's DRY convention. The live copy is weaker and needs Docker. AGENTS.md says to never delete or weaken existing tests, so this is not a request to remove it, only to avoid growing it further.

**Fix:** Leave the live test as is. Cross-reference the unit test (the Javadoc already does). Do not add more origin assertions to the live class.

---

_Reviewed: 2026-10-12_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
