---
phase: 37-ci-hardening-advisory-debt
reviewed: 2026-10-31T12:00:00Z
depth: deep
files_reviewed: 5
files_reviewed_list:
  - .github/scripts/compare_confluent_archives.py
  - src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java
  - .github/workflows/release.yaml
  - .github/workflows/pre-release.yaml
  - src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
status: issues_found
---

# Phase 37: Narrow Re-review of the UF-37 Fixes

**Reviewed:** 2026-10-31
**Depth:** deep
**Scope:** `f523cf3c` (UF-37-1, `safe_line()` in the compare script and 11 added test cases) and `e1e05604` (UF-37-2, comment and Javadoc wording). HEAD `b428a2e4`.
**Status:** issues_found (0 critical, 1 warning, 3 info)

## Summary

The fix is sound. No path was found by which an archive entry name, an allow-list glob or a MANIFEST.MF line can start a workflow command in the job log or the uploaded artifact, and the comment edits contain no logic change. The one real gap is that two of the escaped output paths are not pinned by any test. The other items are Info.

What was checked, and the result:

- **Command starts.** The runner reads a command only at the start of a line, after leading whitespace is trimmed. Raw `\n`, `\r`, `\x0b`, `\x0c`, `\x1c`-`\x1e`, U+0085, U+2028 and U+2029 are all escaped before output, so a name cannot begin a new log line. `::` and `##[` are the only command prefixes, and `safe_line` neutralises both after `lstrip()`. I confirmed that `str.lstrip()` removes every character .NET's `char.IsWhiteSpace` does (U+0009-000D, 0020, 0085, 00A0, 1680, 2000-200A, 2028, 2029, 202F, 205F, 3000), by running `safe_line(chr(c) + "::x")` for each one. The Python whitespace set is a superset of the runner's, so no gap remains.
- **Encoded forms.** `%0A`, `%0D` and `%3A` are decoded by the runner only inside the data or properties of a command it has already recognised. A line that is not a command leaves them inert, and the report never decodes them. `::stop-commands::token` is a `::` start and is covered. A mid-line `::` is not a command start and is correctly left alone.
- **ANSI and CSI.** ESC (0x1b) and the C1 range (including U+009B CSI) are escaped, so colour or cursor sequences cannot be injected into the log.
- **Raw names for classification.** In `compare()` the `diffs`, `fnmatch.fnmatchcase`, `used`, `unallowed`, `stale`, `passed` and the exit code are all computed from raw strings. `safe_line` is applied once, in the final `return`. A scratch run (archives built in `/tmp/gsd37/work`, script run by `python3 -I`) gave exit 1 and the same four `UNALLOWED` lines before and after the fix, and a hostile name covered by an allow-list row still passes (test `aHostileNameCoveredByARowStillPasses`).
- **Collisions.** The mapping is injective. The backslash is always doubled, so every backslash in the output belongs to exactly one escape sequence, and the decode is unique. I enumerated all strings of length 1-4 over `{\, n, LF, x, 1, b, ESC, U+2028, u, 2}` (11110 outputs) with no collision. A name holding the two characters `\n` prints as `\\n`, and a name holding a newline prints as `\n`, as the scratch run shows (`ONLY-B p\n/q` against `ONLY-B p\\n/q`). The command-guard backslash can be confused with an escaped backslash only on a line that starts with a backslash, and no report line starts with one (every line begins with a keyword or a diff marker).
- **Report usefulness.** The artifact keeps one line per entry, with the escaped name inline and the hash intact. The report of the real archives is unchanged, per the commit message and the 37-SECURITY A-16 record. The report stays greppable: `grep -c '^UNALLOWED '` and `'^STALE-ROW '` in the workflow now count real lines, and a name can no longer forge one.
- **Mutation.** I replayed the previous script and the fixed one over a hostile archive (newline then `::error`, CRLF then `##[error]`, a manifest line `::error::mf` and an ESC line). The previous script printed lines starting with `::` and `##[`; the fixed one printed none and no raw control character. Test by test:
  - Dropping the C0 or C1 range, U+2028 or U+2029, or the backslash doubling fails `controlCharactersInAnEntryNameAreEscaped`.
  - Dropping the command-start guard or its `lstrip` fails `safeLineNeutralisesACommandStart`.
  - Dropping the final `safe_line` over the entry lines fails the parameterised tests.
  - `"::error::leading"` is green on the old script too (the line starts with `ONLY-B `); it is a regression pin, as the commit message says.
  - See WR-01 for the paths that survive mutation.
- **Existing assertions.** The diff to `CompareConfluentArchivesScriptUnitTest` is additions only (two imports, constants, 11 cases); no existing assertion changed. The imports `Arrays` and `Pattern` are used and correctly ordered. Spotless in this repo checks import order and unused imports only (`googleJavaFormat()` is commented out in `build.gradle.kts`), so the long lines do not matter.
- **e1e05604.** `git diff -w` over the three files shows only a shell comment on `set -o pipefail` (twice, identical in both workflows), one fixture string and one Javadoc block. `PIPEFAIL_LINE` is run through `TRAILING_COMMENT` before comparison, and `baselinePipefailProblem` matches on the comment-stripped line, so the new comment text (it holds an apostrophe, a semicolon and the word pipefail) cannot change a verdict. The twins still differ only in the lines the pre-release file is supposed to differ in. The new claim is accurate: an explicit `shell: bash` is run as `bash --noprofile --norc -eo pipefail {0}`, and 37-SECURITY A-04 records this from a runner log. No other file in the tree repeats the old wrong claim; two historical planning files state it about the default shell, where it is true.

## Warnings

### WR-01: The ERROR path and the manifest-diff path of the escaping have no test, so a narrowing of the fix passes the suite

**File:** `.github/scripts/compare_confluent_archives.py:238` and `:251`; `src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java:529-638`
**Issue:** The 11 new cases feed hostile text only through entry names (`ONLY-B`, `UNALLOWED`) and one allow-list glob (`STALE-ROW`). Two other channels carry untrusted or semi-trusted text and are not exercised:
1. The `MANIFEST.MF` diff. `difflib.unified_diff` lines (`+::error::mf`, ` ::add-mask::x`) carry content of the built jar and reach the log through the same final `[safe_line(line) for line in lines]`. Today that is correct (I confirmed `+\x1b[31m red` and `+::error::mf` come out inert). But a later edit that applies `safe_line` only to the `ONLY-*`, `CHANGED`, `UNALLOWED` and `STALE-ROW` lines, for instance inside the `diffs` loop, would leave the manifest lines raw and still pass all 55 tests.
2. The `ERROR:` line on stderr (`safe_line(str(e))`, line 251). Removing that call also leaves every test green, because `assertUnusableInput` only checks `startsWith("ERROR:")` or the usage text. Its reachable input today is only the argv paths and zipfile messages, so this is defence in depth; but it is the one line the fix touched that nothing pins.

For a security fix, a path that can be reverted without a red test is a gap in the guard the fix was meant to add.
**Fix:** Add two cases to `CompareConfluentArchivesScriptUnitTest`:
```java
// manifest: a MANIFEST.MF line that is a command, in a differing jar
Run run = compare(baseInner(),
        baseWith(MANIFEST_MF, "Manifest-Version: 2.0\n::error::mf\n\u001b[31m red\n"), COMMENT_ONLY);
assertFail(run);
assertLogSafe(run);
assertTrue(run.stdout().contains("+\\x1b[31m red"), run::describe);

// ERROR: pass an allow-list path (or a zip path) holding a newline and ::error, and assert
// that stderr has no line starting with "::" and no raw control character.
```
For the second, pass a path argument such as `temp.resolve("a\n::error::x.zip")` (not created) and assert on `run.stderr()` with the same two checks as `assertLogSafe`.

## Info

### IN-01: Invisible and bidirectional-format characters are not escaped, so a name can still mislead a reader of the log

**File:** `.github/scripts/compare_confluent_archives.py:57`
**Issue:** `UNSAFE_CHARS` covers C0, DEL, C1, U+2028, U+2029 and the backslash. Other zero-width and format characters pass through: NBSP (U+00A0, shown as `u ::x` in my scratch run, indistinguishable from a space), U+200B, U+FEFF, and the bidi overrides U+202A-U+202E and U+2066-U+2069. None starts a workflow command: the runner's whitespace set contains NBSP but not the zero-width or bidi characters, so `NBSP + "::"` at a line start would be neutralised by `lstrip()` while a ZWSP or BOM in front would simply make the line not a command. The remaining risk is cosmetic, namely a log line that reads differently from the entry it describes (for example a name that reorders its own displayed hash). This is not command forgery and is a weaker class than UF-37-1.
**Fix:** Optional. Extend the escape class to the Unicode `Cf` and `Zs` categories other than the ASCII space, for example by testing `unicodedata.category(ch)` in `_escape_char` and escaping to `\uNNNN` (keep the existing named escapes). If it is left as is, say so in the module docstring so the "log-safe" claim is read as "cannot start a command".

### IN-02: Safety of an uncaught traceback rests on the zip library quoting names with `%r`

**File:** `.github/scripts/compare_confluent_archives.py:112`, `:127`
**Issue:** Only `InputError` goes through `safe_line`. Any other exception (for example a CRC or decompression failure on `outer.read(n)` / `inner.read(...)`, which sit outside the `try`) prints a raw traceback to the same stream and exits 1, not the documented 2. I checked a corrupted inner entry with a hostile name: the traceback held no part of the name, because `zipfile` formats names with `%r`. So there is no forgery today, but the guarantee is implicit in a standard-library detail and is not stated or tested. The wrong exit code (1, the FAIL code, for unreadable input) predates both commits.
**Fix:** In `load_inner`, catch `(zipfile.BadZipFile, ValueError, NotImplementedError, RuntimeError, zlib.error)` around the reads and raise `InputError` with a name-free message, or wrap `main` in a catch-all that prints `safe_line(repr(e))` and returns 2.

### IN-03: The workflow comment cites "a runner log" without a pointer, while the Javadoc cites the audit record

**File:** `.github/workflows/release.yaml:108`, `.github/workflows/pre-release.yaml:108`
**Issue:** The comment says "(measured in a runner log)". The Javadoc in `ReleaseWorkflowGuardUnitTest` names `37-SECURITY UF-37-2`. A maintainer reading only the workflow cannot find where the measurement is recorded, and the claim looks like the kind of assertion that was wrong before. The comment line is also 173 columns, longer than its neighbours.
**Fix:** Shorten and point at the record, in both twins and in the fixture string:
`set -o pipefail # redundant: shell: bash already runs -eo pipefail (37-SECURITY A-04); kept so the tee cannot hide a failed build`.

---

_Reviewed: 2026-10-31_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
