---
phase: 31-fork-owned-nightly-integration-tests
reviewed: 2026-10-26T00:00:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - .github/scripts/failed_tests_summary.py
  - .github/workflows/nightly-integration-tests.yaml
  - src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
  - src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/NightlyWorkflowGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
findings:
  critical: 0
  warning: 3
  info: 3
  total: 6
status: issues_found
---

# Phase 31: Code Review Report

**Reviewed:** 2026-10-26
**Depth:** standard
**Files Reviewed:** 7
**Status:** issues_found

## Summary

The scope is a fork-owned nightly workflow, its failed-test summary script, a Connect-worker image change in the integration harness, and four guard tests.

**Supply chain and permissions of the workflow: clean.**
- All four actions are pinned to full SHAs. I resolved each against the GitHub API:
  - `actions/checkout` v7.0.1 resolves to the pinned commit.
  - `actions/setup-java` v6.0.1 resolves to the pinned commit.
  - `actions/upload-artifact` v7.0.2 resolves to the pinned commit.
  - `gradle/actions` v6.4.0 is an annotated tag, and its tag object resolves to the pinned commit.
- The token is `contents: read` at the top level only, and checkout sets `persist-credentials: false`.
- There is no `${{ }}` expression, no secret, and no untrusted-input interpolation into `run:`, so no script-injection surface.
- The triggers are only `schedule` and `workflow_dispatch`.
- The only context read is `vars.NIGHTLY_ENABLED`, in the job `if`.
- `python3 -I` is used for the script.

**Byte pin:** `integrationTests.yaml` currently matches the pinned values (4812 bytes, sha256 `07dff3d0...`, blob `3e9b9be0...`).

**Python script:** the escaping logic is correct for workflow commands, and the 300/1000 caps hold. The defects are in what the summary claims on bad input and in Markdown escaping. No Critical findings.

## Warnings

### WR-01: Unreadable-only or partly unreadable reports are summarised as "No failed tests." with a success notice

**File:** `.github/scripts/failed_tests_summary.py:128-161`
**Issue:** A JVM killed by the 75-minute step timeout or by an OOM can leave truncated `TEST-*.xml` files. If every report fails to parse, `collect` returns `totals` all zero, `failed == []` and a non-empty `unreadable`. `render` then prints `tests=0 failures=0 ...`, the line `No failed tests.`, and the annotation `::notice ... tests=0 failures=0 errors=0 skipped=0`. The warning is added, but the summary body and the notice assert a clean result in exactly the situation where the real result is unknown. A reader of the step summary, which is the only artifact readable without admin rights, sees "No failed tests." The same happens when only some files are unreadable. The failed tests in the bad files are silently absent while the heading says none failed. None of the tests cover this case. `malformedReportIsNamedAndTheOthersAreStillRead` only covers one bad file next to a good file that contains a failure.
**Fix:** When `unreadable` is non-empty, do not emit "No failed tests." or the success notice. For example:
```python
if failed:
    ...
elif unreadable:
    markdown.append("Result unknown: report files could not be parsed.")
else:
    markdown.append("No failed tests.")
...
if failed:
    annotations.append(failed_annotation(...))
elif not unreadable:
    annotations.append("::notice ...")
```
Add a test for the all-unreadable directory.

### WR-02: Markdown escaping misses `<`, `&`, `[`, `*` and `\`, so JUnit messages are mangled or the table breaks

**File:** `.github/scripts/failed_tests_summary.py:39-46`
**Issue:** `escape_markdown` handles only `|`, backtick, CR and LF.
- JUnit and AssertJ first-lines are full of `<...>`, for example `expected: <1> but was: <2>`, `java.util.List<String>` and `Expected <Foo> ...`. GitHub's Markdown sanitiser treats `<Foo>` as an HTML tag, drops it, and the failure message loses its content.
- `&` entities (`&lt;`) render as the decoded character.
- A message or test name that already holds `\|` becomes `\\|`. The backslash escapes itself and the pipe now terminates the cell, which corrupts the row.
- Parameterized names (`test[1] a*b`) can trigger link or emphasis syntax.

The first message line is the main diagnostic, since logs are unreadable, so losing its content defeats the D-10 purpose.
**Fix:** Escape in this order, backslash first:
```python
def escape_markdown(text):
    text = text.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return (text.replace("|", "\\|").replace("`", "'").replace("*", "\\*")
                .replace("[", "\\[").replace("\r", " ").replace("\n", " "))
```
Add a fixture test with `<..>` and `\|` in the message.

### WR-03: The byte-identity guard is checkout-sensitive and can fail on Windows or autocrlf clones

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java:34-37, 105-108`
**Issue:** The guard reads raw bytes and fails on any CRLF. `.gitattributes` only forces `eol=crlf` for `*.bat` and has no `* text=auto` or `-text` rule for `integrationTests.yaml`. On a Windows clone with `core.autocrlf=true` the checked-out file is CRLF, so `integrationTestsWorkflowIsByteIdenticalToUpstream` fails although git history is byte-identical. The sibling tests' comments say the test JVM runs with `file.encoding=windows-1252`, which suggests Windows developers are in play. The failure message tells the developer to replace the SHA with the CRLF digest, which would then break Linux CI.
**Fix:** Pin the checkout bytes in `.gitattributes` so the byte guard is stable on every platform, for example:
```
.github/workflows/integrationTests.yaml -text
```
Alternatively, hash `git show HEAD:<path>` instead of the working-tree file.

## Info

### IN-01: A run where every report parses but zero testcases exist reports a green notice

**File:** `.github/scripts/failed_tests_summary.py:154-161`
**Issue:** If the reports exist but hold zero `<testcase>` elements, for example when Gradle fails before any test runs and writes empty suite XML, the notice reads `tests=0 failures=0`. This is the same false-reassurance class as WR-01. In addition, a testcase that has both `<failure>` and `<error>` is counted in both `failures` and `errors`, so `tests` is not the sum of the outcomes. That is only cosmetic.
**Fix:** Emit `::warning` instead of `::notice` when `totals["tests"] == 0`.

### IN-02: Container images in the harness are tag-pinned, not digest-pinned, and nothing guards against Docker Hub throttling

**File:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java:36-62, 101-112`
**Issue:** The worker image is now built per `ConfluentPlatform` instance from two Docker Hub images (`cnfldemos/cp-server-connect-datagen`, `confluentinc/cp-server-connect`) referenced by mutable tag only. The workflow rules apply a strict full-SHA policy to actions, but the nightly's test containers do not get the same treatment. Unauthenticated pulls from a shared GitHub runner IP are rate limited, and the image build adds a second pull. This is a nightly flakiness and supply-chain observation, not a correctness bug. The two-stage Dockerfile itself (`FROM ... AS datagen`, `FROM cp-server-connect`, `COPY --from=datagen`) is well formed: root-owned files are readable by the Connect user, and the later `withCopyToContainer` mount path is unchanged.
**Fix:** Optional. Pin to image digests, or document that Docker Hub throttling is an accepted source of nightly reds.

### IN-03: Duplicate globbing and a Windows-fragile `python3` requirement in the script test

**File:** `.github/scripts/failed_tests_summary.py:68, 114`; `src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java:33, 64-66`
**Issue:**
- `render` globs the directory and `collect` globs it again, so a file appearing between the two calls yields inconsistent state. `glob.glob` also treats `[`, `]` and `*` in the directory argument as patterns.
- The test hard-fails when `python3` cannot start. On Windows `python3` is often the Microsoft Store stub, so `testUnit` fails on a machine that does have `python`. The class comment declares this intentional, so this is only a portability note.

**Fix:** Pass the file list from `render` into `collect`, and use `glob.escape(directory)`. Optionally try `python3`, then `python`, then `py -3`, in the test.

---

_Reviewed: 2026-10-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
