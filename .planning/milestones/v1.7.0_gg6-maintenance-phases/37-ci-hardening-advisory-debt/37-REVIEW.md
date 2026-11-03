---
phase: 37-ci-hardening-advisory-debt
reviewed: 2026-10-31T09:59:45Z
depth: standard
files_reviewed: 28
files_reviewed_list:
  - .gitattributes
  - .gitignore
  - .github/dependabot.yml
  - .github/scripts/baseline_tag.py
  - .github/scripts/bump_version.py
  - .github/scripts/compare_confluent_archives.py
  - .github/scripts/failed_tests_summary.py
  - .github/workflows/increment-version.yml
  - .github/workflows/pre-release.yaml
  - .github/workflows/release.yaml
  - docs/FORK-DELTA.md
  - src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
  - .planning/scripts/fork-anchors.txt
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt
  - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
  - .planning/phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md
findings:
  critical: 0
  warning: 4
  info: 6
  total: 10
status: issues_found
---

# Phase 37: Code Review Report

**Reviewed:** 2026-10-31T09:59:45Z
**Depth:** standard (deep on the CI scripts and workflows)
**Files Reviewed:** 28
**Status:** issues_found

## Summary

Scope was the diff `c9d426ae..2b44ba04` outside `.planning`, plus the anchors file and the renamed allow-lists. The scripts were exercised directly (version grammar, glob semantics) and the workflow shell was read against the runner's shell semantics. The ledger dispositions in `37-ADVISORY-LEDGER.md` and the recorded header-comment item of `increment-version.yml` were not re-reported.

No Critical findings. The version grammar is consistent between `bump_version.py` and `baseline_tag.py`. Both scripts fail closed and use `fullmatch` on ASCII digits. No workflow expression is interpolated into a `run:` block, and every value written to `GITHUB_ENV` is validated first. The four Warnings are:

- a masked pipeline status in the baseline build;
- a catch-all guard on allow-list globs that is trivially bypassed;
- a guard that pins a checkout SHA literally and will go red on the next Dependabot bump;
- an overclaimed atomicity in `bump_version.py`.

## Warnings

### WR-01: The baseline build's exit status is masked by `| tee` because the job shell has no `pipefail`

**File:** `.github/workflows/release.yaml:115` (identical in `.github/workflows/pre-release.yaml:115`; the shell is set at line 41)
**Issue:** `defaults.run.shell: bash` makes the runner use `bash -e {0}`. Only the unspecified default shell gets `-eo pipefail`. So `"$baseline_dir/gradlew" ... createConfluentArchive | tee ...` returns the exit status of `tee`, and a failing baseline Gradle build does not stop the step. The only remaining guard is `test -s "$baseline_archive"`. If the build fails after the zip exists, the step goes on. For example, the `verifyConfluentArchive` finalizer could fail on the baseline tag. The compare then runs against a baseline archive that never passed its own verification. The main build step at line 76 has the same pipe, but it is covered by the exact `> Task :verifyConfluentArchive` log-line check. The baseline step has no such check. `TestsWorkflowPinGuardUnitTest` already rejects `|| true` on the archive call, yet nothing guards this masked status.
**Fix:** Add `set -o pipefail` as the first line of the baseline step, or drop the pipe and write the log with `> "$RUNNER_TEMP/baseline-build.log" 2>&1; cat "$RUNNER_TEMP/baseline-build.log"`. Apply it to both twin files, since `ReleaseWorkflowGuardUnitTest` pins their equality. Preferably set `shell: bash -eo pipefail {0}` under `defaults.run` so every pipe in the job is covered. `ReleaseWorkflowGuardUnitTest` can pin the new line. Add a fixture that rejects its removal.

### WR-02: The "catch-all glob" refusal in `load_allowlist` accepts every near-catch-all

**File:** `.github/scripts/compare_confluent_archives.py:41` and `:115`
**Issue:** `CATCH_ALL_GLOB = \*+` refuses only globs made of stars. These are all accepted, and each matches every inner entry or nearly all of them, which defeats the "no catch-all" control:

- `*?`
- `**/*`
- `*/*`
- `[!x]*`
- `*.class`
- `*.*`

I confirmed `*?`, `**/*`, `*/*` and `[!x]*` load and match `com/a/B.class`. With such a row, any difference is `ALLOWED`, and a single row can never go `STALE-ROW`. A tired maintainer or an automated edit can therefore silence the whole compare. The module docstring promises that a catch-all row makes the list unusable, but the check implements only the narrowest case.
**Fix:** Reject globs that match the wildcard-only family, or require a literal anchor. For example, refuse any glob whose literal characters (after removing `*`, `?`, `[`, `]` and `!`) are shorter than a floor such as 8, or that does not start with a literal path segment. A concrete check is `re.sub(r"[*?\[\]!]", "", glob)` and then requiring a non-trivial remainder. Add rows for `*?`, `**/*` and `*/*` to `CompareConfluentArchivesScriptUnitTest`.

### WR-03: `IncrementVersionWorkflowGuardUnitTest` pins the checkout SHA and version comment of the real file, so a Dependabot bump turns `testUnit` red

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java:62-63` (constant), `:325-328` (rule), `:425-427` (test)
**Issue:** `CHECKOUT_USES` is `uses: actions/checkout@3d3c42e5... # v7.0.1`. `checkoutProblem(nonCommentLines(WORKFLOW))` fails unless a line equals that string exactly. `.github/dependabot.yml` is configured to move these SHA pins monthly in one grouped PR, and `increment-version.yml` is expressly in scope of that entry. The first bump of `actions/checkout` therefore fails this guard on the Dependabot PR and on `custom_build`. The rule's real purpose is only that the checkout keeps its default persisted credentials, because the push step needs them, and that a pin exists. The pin form is already enforced for every workflow by `WorkflowActionPinGuardUnitTest`. `ReleaseWorkflowGuardUnitTest` is not affected in the same way: it keeps the literal SHA only in its fixtures, and the real files go through `checkoutCredentialsProblem`.
**Fix:** Match the step by pattern instead of by the exact SHA. For example, require a line that matches `^uses: actions/checkout@[0-9a-f]{40} # v\d+\.\d+\.\d+$` and keep the `persist-credentials` absence rule. Update the fixture `VALID` and its reject cases accordingly.

### WR-04: `bump_version.py` is not atomic across the two files, and the docstring says it is

**File:** `.github/scripts/bump_version.py:98-117` (`write_both`) and docstring lines 14-16
**Issue:** The docstring promises: "Both new contents are computed and staged before either file is replaced, so any failure leaves VERSION and the anchors file as they were." Staging is atomic, but the two `os.replace` calls are not. If the second `os.replace` raises `OSError` after the first has succeeded, the `except` branch removes the temp files. The first `os.remove(tmp)` fails silently because the file was already moved, and the anchors file is left on the new version while `VERSION` is on the old one. In the next run `rewrite_anchor_row` refuses with the "fix the row first" message, so the failure is loud and not silent, but the stated guarantee is false. Non-`OSError` exceptions during staging, such as a `ValueError` from `write`, bypass the cleanup and leave `.bump-*` files in the working tree and a raw traceback (exit 1 instead of the contracted 1 with an `ERROR:` line). `git add` names the two paths, so the stray files do not reach a commit.
**Fix:** Either narrow the docstring to "staged before either is replaced; a failure while replacing the second file can leave the anchors row ahead of VERSION and the next run refuses", or make the replace sequence recoverable. For example, keep the original text of the first file and restore it with `os.replace` of a pre-staged backup if the second replace fails. Catch `Exception` (not just `OSError`) around staging so the temp files are always removed. Add a test that makes the second target unwritable.

## Info

### IN-01: The workflows accept a looser version grammar than the scripts, and the failure message for the mismatch is wrong

**File:** `.github/workflows/release.yaml:65` (twin `pre-release.yaml:65`); `.github/workflows/release.yaml:93-96`
**Issue:** "Read version" accepts `^v[0-9]+\.[0-9]+\.[0-9]+(_gg[0-9]+)?$`. `baseline_tag.py` and `bump_version.py` require `_gg[1-9][0-9]*`. A `VERSION` of `v1.7.0_gg0` or `v1.7.0_gg05` passes the first gate and then makes `baseline_tag.py` exit 2. The step's `|| { ... }` branch reports "no release tag below ... is reachable from this commit" for every non-zero status, including the usage and grammar exit 2. The script's own stderr line does appear in the log, so the run still fails closed, but the annotation points at the wrong cause.
**Fix:** Tighten the workflow regex to `(_gg[1-9][0-9]*)?` so one grammar is used everywhere. Separately, distinguish exit 1 from exit 2 in the `||` branch.

### IN-02: An allow-list row cannot say whether it allows a changed, an added or a removed entry

**File:** `.github/scripts/compare_confluent_archives.py:159-163`
**Issue:** `matched` is computed from the path alone, so the exact row that exists to excuse a `CHANGED Version.class` also silently excuses that class being deleted (`ONLY-A`) or an unrelated new class at the same path. For the release-identity rows this is acceptable, but the `com/fasterxml/jackson/core/*` style rows of the `gg4` list excuse additions and removals of anything below them.
**Fix:** Optionally accept a third column `CHANGED|ONLY-A|ONLY-B|*` (default `CHANGED`) and match on the kind as well. Leave existing rows on `*` to avoid churn.

### IN-03: Corrupt archives can escape as a traceback with exit 1, which the script's contract reserves for FAIL

**File:** `.github/scripts/compare_confluent_archives.py:74-85` and `:90-93`
**Issue:** `outer.read(n)` (line 78) sits outside the `try` that maps `BadZipFile` to `InputError`, and the `inner.read(...)` calls are not wrapped at all. A bad CRC, an unsupported compression method or an encrypted entry raises `BadZipFile`, `NotImplementedError` or `RuntimeError` out of `load_inner`, and the script exits 1 with a traceback. The docstring says exit 2 means "unusable input (... unreadable archive ...)". The workflow treats every non-zero status as failure, so nothing fails open, but the exit-code contract that `CompareConfluentArchivesScriptUnitTest` pins is not honoured on this path.
**Fix:** Wrap the whole body of `load_inner` after the `ZipFile` open in `except (zipfile.BadZipFile, RuntimeError, NotImplementedError, OSError) as e: raise InputError(...)`.

### IN-04: The release-workflow guard documents "compare before the tag gate" but its `CHAIN` omits the tag gate step

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java:106-115` (`CHAIN`) and the class Javadoc
**Issue:** `TAG_GATE_STEP` ("Check the release tag is absent") is defined and present in the fixtures but is not part of `CHAIN`. The Javadoc, the workflow header comment and `FORK-DELTA.md` all say the compare runs "before the tag gate". Moving the tag-gate step above the compare chain passes the guard. This does no direct harm, because the compare is still ordered before the tests gate and the publish step, but the pinned claim is wider than the pinned fact.
**Fix:** Add `TAG_GATE_STEP` to `CHAIN` between `TESTS_GATE_STEP` and `PUBLISH_STEP`, or narrow the three descriptions to "before the tests gate and the publish step".

### IN-05: The two allow-list headers still name the removed script path

**File:** `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt:1`, `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt:1`
**Issue:** Both headers say "Allow-list for compare-confluent-archives.py". The script is now `.github/scripts/compare_confluent_archives.py` (hyphens replaced by underscores, directory moved). Comments only, but they are the only in-file pointer for a maintainer writing the next list.
**Fix:** Update the first header line of both files to name `.github/scripts/compare_confluent_archives.py`.

### IN-06: The "stale directory" hint in `ConfluentArchiveCheck` is printed for an archive with zero lib jars as well

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java:122-127`
**Issue:** The condition is `libJarNames().size() != 1`. With zero jars, stderr reads "lib jars found: ; a version directory left under build/confluentArchive by an earlier VERSION adds a jar, run clean first". The empty list and a hint about an added jar are misleading for a missing jar.
**Fix:** Print the line only when `size() > 1`, or print "lib jars found: none" with a different hint for the zero case.

---

_Reviewed: 2026-10-31T09:59:45Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
