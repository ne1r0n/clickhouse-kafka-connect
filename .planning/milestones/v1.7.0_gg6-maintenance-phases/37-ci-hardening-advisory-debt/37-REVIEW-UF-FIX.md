---
phase: 37-ci-hardening-advisory-debt
fixed_at: 2026-10-31T00:00:00Z
review_path: .planning/phases/37-ci-hardening-advisory-debt/37-REVIEW-UF.md
iteration: 1
findings_in_scope: 1
fixed: 1
skipped: 0
status: all_fixed
---

# Phase 37: Code Review Fix Report (narrow re-review of the UF-37-1 and UF-37-2 fixes)

**Fixed at:** 2026-10-31
**Source review:** `.planning/phases/37-ci-hardening-advisory-debt/37-REVIEW-UF.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 1 (WR-01; the three Info findings were declared out of scope by the caller)
- Fixed: 1
- Skipped: 0 in scope (IN-01..IN-03 are listed below as out of scope, not attempted)

## Fixed Issues

### WR-01: The ERROR path and the manifest-diff path of the escaping have no test, so a narrowing of the fix passes the suite

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java`
**Commit:** `5ba9b84f8ebace8c748a3669dffe19d18a241b07`
**Applied fix:** two cases and one helper were added, additions only (53 insertions, no removed line, no script change; `git diff --quiet d0293d1c HEAD -- .github` holds):

1. `aManifestDiffLineCannotStartAWorkflowCommand` runs two archives whose `MANIFEST.MF` differ in `Manifest-Version`, with the line `::error::ctx` unchanged in both (so it is a context line of the unified diff), and `::add-mask::added` plus an ESC line added in the candidate. It asserts exit 1 and `MANIFEST.MF: differs`, that no stdout line starts a command after its leading whitespace and none holds a raw control character, that the context line is printed as `\ ::error::ctx`, that the `+::add-mask::added` line is left as it is, and that the ESC line is printed as `+\x1b[31m red`.
2. `aHostilePathCannotForgeAWorkflowCommandOnTheErrorLine` (parameterised over `archive` and `allow-list`, 2 executions) passes a path that does not exist and whose name holds a newline, `::error::` and ESC, in the archive position and in the allow-list position. It asserts exit 2, empty stdout, the `ERROR:` prefix, the escaped name, exactly one stderr line, and the same two stream checks.
3. The helper `assertStreamLogSafe(Run, String)` is new. The existing `assertLogSafe` was left as it is (the task said additions only), so the two helpers duplicate a small loop; merging them is a later cleanup.

**The suggested test was read critically and changed in one point.** The review's manifest case feeds `+::error::mf` and checks lines with `startsWith("::")` (the existing `assertLogSafe`). Every `difflib.unified_diff` line carries a `+`, `-` or space prefix, so a changed manifest line never starts with `::`, and the old check cannot see a raw context line either, because its prefix is a space. The runner trims leading whitespace before it looks for a command, so the context line ` ::error::ctx` is the one that would act as a command. The new helper therefore tests the line after `stripLeading()`, and the fixture holds a context line. Without that change the manifest test would have passed on a script that leaves the diff raw (only the ESC line would have been caught). The `contains` check of the error case is also weak (the exception text holds the path a second time, in `repr` form, which matches either way); the single-line and line-start checks are the ones that kill the mutant.

**Mutation proof** (scratch copy of the repository under `/tmp/gsd37`, mutated script copied in, `testUnit --tests '*CompareConfluentArchivesScriptUnitTest' --rerun`; the scratch tree was deleted afterwards, the mutated scripts are kept as `/tmp/gsd37/mut1-stderr.py` and `/tmp/gsd37/mut2-entries-only.py`, logs `/tmp/gsd37/mut1-stderr.log` and `/tmp/gsd37/mut2-entries-only.log`):

| Mutation | Result |
|----------|--------|
| M1: `safe_line(str(e))` replaced by `str(e)` on the `ERROR:` line | 58 tests, 2 failed: both executions of the error case (the archive path and the allow-list path), on the line count (2 instead of 1) |
| M2: the final escaping applied only to lines that start with `ONLY-A `, `ONLY-B `, `CHANGED `, `UNALLOWED ` or `STALE-ROW ` | 58 tests, 1 failed: the manifest case ("a line starts a workflow command after its leading whitespace"). The 55 earlier tests all passed, which is the gap WR-01 named |
| Unmutated script | 58 tests, 58 passed |

**Verification** (executed in the main checkout, where the committed tree is):
- Unmutated class run: 58 tests (55 + 3), 0 failures.
- Cache-busted Docker-free suite, `env -u TESTCONTAINERS_HOST_OVERRIDE DOCKER_HOST=unix:///nonexistent/docker.sock ./gradlew testUnit --rerun`: exit 0, 1672 tests (1669 + 3), 0 failures, 0 errors, 2 skipped, 80 files; 0 failing testcases (`37-evidence-check.py junit` and `failed`). No OpenJ9 crash file appeared.
- Anchors mirror (`37-evidence-check.py anchors .planning/scripts/fork-anchors.txt`): `rows=172 bad=0`.
- `git diff --quiet ff873086 HEAD -- src/main`: unchanged (empty diff).
- `git diff -U0 -- src/test` before the commit: no removed line, so no assertion was removed.
- `37-ledger-check.py check 37-ADVISORY-LEDGER.md`: `OK ledger: 36 rows, 0 problems`.
- Spotless (`spotlessJavaCheck`) names no violation in the edited file.
- Hygiene greps empty before each commit; `git status` shows only the two orchestrator-owned state files besides the staged work, and no stray file.

**Gated sha moved.** The commit touches a file outside `.planning`, so the last commit outside `.planning` is now `5ba9b84f8ebace8c748a3669dffe19d18a241b07`. Measured against the published tip `3a3a15a9`: 230 commits up to it, 85 files outside `.planning`, 8089 insertions, 607 deletions (before: 227 commits, 8036 insertions). `37-USER-DISPATCH-DRYRUN.md`, the forward pointer in `37-05-SUMMARY.md` and `37-SECURITY.md` (A-16, UF-37-6, the audit trail; status stays `secured`, 20 threats, 0 open) were updated in the docs commit that carries this report.

## Skipped Issues

These three Info findings are out of scope for this pass by the caller's decision. They were not attempted and the code is unchanged.

### IN-01: Invisible and bidirectional-format characters are not escaped, so a name can still mislead a reader of the log

**File:** `.github/scripts/compare_confluent_archives.py:57`
**Reason:** out of scope (Info, fix needs a script change; the task is additions to the test file only)
**Original issue:** `UNSAFE_CHARS` covers C0, DEL, C1, U+2028, U+2029 and the backslash. NBSP, zero-width and bidi-format characters pass through. None starts a workflow command; the remaining risk is cosmetic.

### IN-02: Safety of an uncaught traceback rests on the zip library quoting names with `%r`

**File:** `.github/scripts/compare_confluent_archives.py:112`, `:127`
**Reason:** out of scope (Info, fix needs a script change)
**Original issue:** only `InputError` goes through `safe_line`; any other exception prints a raw traceback and exits 1 instead of 2. No forgery today because `zipfile` formats names with `%r`.

### IN-03: The workflow comment cites "a runner log" without a pointer, while the Javadoc cites the audit record

**File:** `.github/workflows/release.yaml:108`, `.github/workflows/pre-release.yaml:108`
**Reason:** out of scope (Info, fix edits both release workflows and a test fixture; the task keeps `.github` byte-identical)
**Original issue:** the `set -o pipefail` comment says "(measured in a runner log)" with no pointer to the record and is 173 columns long.

---

_Fixed: 2026-10-31_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
