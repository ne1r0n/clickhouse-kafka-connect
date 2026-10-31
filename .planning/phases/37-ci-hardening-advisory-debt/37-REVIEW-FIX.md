---
phase: 37-ci-hardening-advisory-debt
fixed_at: 2026-10-31T12:30:00Z
review_path: .planning/phases/37-ci-hardening-advisory-debt/37-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
out_of_scope: 6
status: all_fixed
---

# Phase 37: Code Review Fix Report

**Fixed at:** 2026-10-31T12:30:00Z
**Source review:** .planning/phases/37-ci-hardening-advisory-debt/37-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (WR-01 to WR-04; the Info findings IN-01 to IN-06 were excluded by the caller)
- Fixed: 4 (WR-02 and WR-04 are logic changes and are flagged for human verification)
- Skipped: 0 in scope; 6 Info findings are listed below as skipped, out of scope

## Fixed Issues

### WR-01: The baseline build's exit status is masked by `| tee` because the job shell has no `pipefail`

**Files modified:** `.github/workflows/release.yaml`, `.github/workflows/pre-release.yaml`, `src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java`
**Commit:** b6fd9a0a
**Status:** fixed
**Applied fix:** the step `Build the baseline archive from the previous tag` now begins with `set -o pipefail`, identically in both twins (the twin-equality rule still passes). The review's preferred variant, `shell: bash -eo pipefail {0}` under `defaults.run`, was not taken: it would change every pipe of the job, including the main build step whose masked status is deliberately covered by the exact `> Task :verifyConfluentArchive` log-line check, and it would alter lines that `fork-anchors.txt` and the pin guards count. No anchored literal changed (`rows=172 bad=0`; the new comment on the line contains none). `ReleaseWorkflowGuardUnitTest` gained `baselinePipefailProblem` (the option must be its own non-comment line, before the first call of the baseline wrapper), a test over both real files and a fixture that accepts the valid step and rejects a missing option, one set after the call, one only in a comment and `set +o pipefail`.
**Verification:** the run block of the baseline step was extracted from the committed `release.yaml` and replayed under `bash --noprofile --norc -e` (the runner's `bash -e {0}`) against a throwaway repository whose baseline `gradlew` writes the zip and then exits 1 (the finalizer-failure case). Without the line the step exited 0 and wrote `BASELINE_ARCHIVE` to `GITHUB_ENV`; with it the step exited 1 and wrote nothing. Note for the record: the E-11 replay of plan 37-03 ran the blocks under `bash -eo pipefail`, which is not the runner's shell for an explicit `shell: bash`, so it could not have shown this defect.

### WR-02: The "catch-all glob" refusal in `load_allowlist` accepts every near-catch-all

**Files modified:** `.github/scripts/compare_confluent_archives.py`, `src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java`
**Commit:** 2bec20d8
**Status:** fixed: requires human verification (a rule design)
**Applied fix:** `load_allowlist` now also refuses, with exit 2, any glob whose first character is `*`, `?` or `[`. The rule is sound for the stated purpose: a glob that starts with the literal character c cannot match a path that does not start with c, so no row can match every path, and `*?`, `**/*`, `*/*`, `[!x]*` are all refused. It is deliberately stricter than "no literal at all": `*.class`, `*.*` and `*/A.class` (the review's other examples) match a whole file type in every directory, because a star crosses slashes, so they are refused too, and `[x]*` is refused conservatively. The rule and its reason are in the module docstring; the star-only message that the existing tests assert is unchanged. The review's suggested fix (remove `*?[]!` and require a remainder) was not used: it is unsound for character classes, because `[!x]*` leaves the literal-looking `x` behind.
**Tests added:** nine refused globs (`*?`, `**/*`, `*/*`, `[!x]*`, `?*`, `[a-z]/*`, `*.class`, `*.*`, `*/A.class`), seven accepted rows that still excuse a changed class (`a/A.class`, `a/*`, `a/*.class`, `a/?.class`, `a/[A].class`, `a/[!x].class`, `a*`), and a test that every allow-list under `.planning/scripts` loads (exit 1 on stale rows, never exit 2). All nine refusals fail against the pre-fix script (mutation run with `--rerun`).
**Verification:** the real compare of the phase's archives `v1.7.0_gg4` against `v1.7.0_gg5` under `confluent-archive-allowlist-v1.7.0_gg5.txt` still ends `RESULT: PASS` (exit 0, changed 4, same 7395); the `gg4`, `gg5` and `25` allow-lists load with 50, 4 and 28 rows.

### WR-03: `IncrementVersionWorkflowGuardUnitTest` pins the checkout SHA and version comment of the real file, so a Dependabot bump turns `testUnit` red

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java`
**Commit:** 058eca48
**Status:** fixed
**Applied fix:** `checkoutProblem` now requires, inside the step `Check out code`, exactly one `uses:` line, and that line must match `^uses: actions/checkout@[0-9a-f]{40} # v\d+\.\d+\.\d+$`; the `persist-credentials` absence rule is unchanged. Compared with the old rule it is stricter on placement (the pin must sit in the checkout step, not anywhere in the file) and on a second `uses:` line, and equal or looser only in the SHA and version values. The reviewer's regex was used as is.
**Tests added:** a fixture that accepts a bumped SHA with `# v7.1.0` and `# v10.12.3` and rejects a tag pin, a branch, a 39-hex and a 41-hex SHA, an upper-case SHA, a SHA without a comment, a `# main` comment, a `# v7` comment, `actions/checkout-fork@`, `other/checkout@`, a second `uses:` line and a removed pin. The pre-existing fixture and the real-file test pass unchanged.

### WR-04: `bump_version.py` is not atomic across the two files, and the docstring says it is

**Files modified:** `.github/scripts/bump_version.py`, `src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java`
**Commit:** 27f9ff89
**Status:** fixed: requires human verification (error-path logic)
**Applied fix:** `write_both` stages both new contents and a backup of the first file's current bytes next to their targets before replacing anything. If the second `os.replace` fails, the first file is put back from the backup; if even that fails, the backup is kept and the `BumpError` names it. Every exception (not only `OSError`) becomes a `BumpError`, so the script keeps its exit-1 `ERROR:` contract, and every staged temp file is removed in a `finally`. Bytes are written in binary, so CRLF anchors files are preserved (the existing CRLF test passes). The docstring now states the real guarantee: a failure while staging or replacing leaves both files as they were, and only a process kill between the two renames can leave the anchors row ahead of `VERSION`, after which the next run refuses and says to fix the row first. Full atomicity across two files is not possible with two renames, so the review's second option (restore from a backup) was implemented and the claim narrowed to match.
**Tests added:** four, driving the script's own functions through `python3 -I -B -c` with `os.replace` made to fail (a failing second rename cannot be produced from the command line, because staging and replacing need the same directory permission): a failing second replace leaves both files unchanged and no `.bump-*` file, and the driver output proves the first rename really happened; the same through `main` gives exit 1 with an `ERROR:` line; a failing restore keeps one backup that holds the original and names it; a non-`OSError` while staging (a lone surrogate) leaves both files unchanged, no stray file and a `BumpError`. All four fail against the pre-fix script (mutation run with `--rerun`).

## Skipped Issues

All six Info findings were excluded from this run by the caller (fix scope `critical_warning`). None was touched.

### IN-01: The workflows accept a looser version grammar than the scripts, and the failure message for the mismatch is wrong

**File:** `.github/workflows/release.yaml:65` (twin `pre-release.yaml:65`); `.github/workflows/release.yaml:93-96`
**Reason:** skipped: out of scope (Info finding)
**Original issue:** "Read version" accepts `(_gg[0-9]+)?` while `baseline_tag.py` and `bump_version.py` require `_gg[1-9][0-9]*`, and the `||` branch reports the wrong cause for the grammar exit 2.

### IN-02: An allow-list row cannot say whether it allows a changed, an added or a removed entry

**File:** `.github/scripts/compare_confluent_archives.py:159-163`
**Reason:** skipped: out of scope (Info finding)
**Original issue:** a row matches on the path alone, so a row that excuses a `CHANGED` entry also excuses its removal or an unrelated addition at the same path.

### IN-03: Corrupt archives can escape as a traceback with exit 1, which the script's contract reserves for FAIL

**File:** `.github/scripts/compare_confluent_archives.py:74-85` and `:90-93`
**Reason:** skipped: out of scope (Info finding)
**Original issue:** `outer.read` and the `inner.read` calls sit outside the `BadZipFile` mapping, so a bad CRC or an unsupported method exits 1 with a traceback instead of 2.

### IN-04: The release-workflow guard documents "compare before the tag gate" but its `CHAIN` omits the tag gate step

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java:106-115` (`CHAIN`) and the class Javadoc
**Reason:** skipped: out of scope (Info finding)
**Original issue:** `TAG_GATE_STEP` is not in `CHAIN`, so the pinned claim is wider than the pinned fact.

### IN-05: The two allow-list headers still name the removed script path

**File:** `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt:1`, `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt:1`
**Reason:** skipped: out of scope (Info finding)
**Original issue:** both headers say "compare-confluent-archives.py", the script is now `.github/scripts/compare_confluent_archives.py`.

### IN-06: The "stale directory" hint in `ConfluentArchiveCheck` is printed for an archive with zero lib jars as well

**File:** `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java:122-127`
**Reason:** skipped: out of scope (Info finding)
**Original issue:** the hint is printed when `libJarNames().size() != 1`, which includes zero jars.

## Verification

**Where it ran:** an isolated git worktree (`.claude/worktrees/rf-37-73083-1793440879`, temp branch `gsd-reviewfix/37-73083`, `workflow.use_worktrees` unset so the default applies), not the main checkout. The numbers are reproducible from the main checkout after the fast-forward, except the Gradle caches. The main checkout's `.planning/config.json` and `.planning/state.json` modifications were never staged.

**E-20 is not recorded in `37-COMMAND-EVIDENCE.md`.** `37-evidence-check.py` (never edited) requires the index rows to be exactly E-01..E-N in order, so an E-20 without E-19 would break `index` for both 18 and 20 rows, and E-19 is reserved for the user's dry-run report. The evidence of this pass is therefore recorded here only.

- Commits (plain git, `fix(37): ...` with the trailer): WR-01 `b6fd9a0a`, WR-02 `2bec20d8`, WR-03 `058eca48`, WR-04 `27f9ff89`; docs `41369395` (dry-run handoff and 37-05 SUMMARY pointer).
- Cache-busted `testUnit` (`DOCKER_HOST=unix:///nonexistent/docker.sock`, `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun`): exit 0, 1658 tests, 0 failures, 0 errors, 2 skipped, 80 files; 0 failing testcases. The gate of plan 37-05 read 1633, plus 25 additions: `ReleaseWorkflowGuardUnitTest` 37 (+3), `CompareConfluentArchivesScriptUnitTest` 44 (+17), `IncrementVersionWorkflowGuardUnitTest` 17 (+1), `BumpVersionScriptUnitTest` 52 (+4). The full suite on ClickHouse `latest` was not re-run: no `src/main`, build or Docker-backed file changed.
- Anchors mirror (`37-evidence-check.py anchors .planning/scripts/fork-anchors.txt`): `rows=172 bad=0`.
- `git diff --quiet b625967b -- src/main`: unchanged.
- Hygiene: `git grep -I` for the three forbidden names and the `git diff --cached` scan printed nothing before every commit; `37-hygiene-check.py` over the 10 added or modified files: 0 hits; `37-ledger-check.py check` on the ledger: 36 rows, 0 problems.
- Cumulative assertion diff (`git diff -w -U0 b625967b -- src/test`, removed lines): no removed line matches `assert|fail(|expect`. Four lines were removed in total: three in `IncrementVersionWorkflowGuardUnitTest.checkoutProblem` (the exact-line `CHECKOUT_USES` rule, the intended WR-03 relaxation, replaced by a rule that is stricter on placement and equal on shape) and one in `ReleaseWorkflowGuardUnitTest` (the baseline-call fixture line, moved to the constant `BASELINE_CALL_LINE` with the same text; a pure refactor).
- Mutation checks: the pre-fix `compare_confluent_archives.py` fails the nine new refusal tests, the pre-fix `bump_version.py` fails the four new WR-04 tests; both were restored from the fix commits. Gradle does not track the script files as inputs, so those runs used `--rerun`; a plain `test` run after editing a script alone is UP-TO-DATE.
- Stray files: an import of the compare script for a local check created `.github/scripts/__pycache__/`; it was untracked, never staged, and removed before any commit. `git status` is clean apart from this report. Nothing pushed, no mutating `gh` call.
- Handoff: `37-USER-DISPATCH-DRYRUN.md` now names the gated sha `27f9ff892eb6d4208a4f4e90ae204d6996fa72de` (the last commit outside `.planning`; `git diff --name-only 1c583b46 27f9ff89 -- . ':(exclude).planning'` lists exactly the eight files of the four fixes), 221 commits up to it (`git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463..27f9ff89`) and 85 files, 7880 insertions, 607 deletions against the published tip. The ledger and the release record name neither the gated sha nor these counts, so they were not edited; the E-18 entry is evidence and is not edited.

---

_Fixed: 2026-10-31T12:30:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_

## Erratum (added after the security audit, 2026-10-31)

The text above is left as written. Two statements in it are wrong or superseded, and one follow-up fix was made after the audit.

**1. The premise of WR-01 is false, and so is the E-11 note.** WR-01 and its `Applied fix` and `Verification` text say that the job shell for an explicit `shell: bash` is `bash -e {0}` without `pipefail`, and the `Verification` paragraph adds that the E-11 replay of plan 37-03 ran under `bash -eo pipefail`, "which is not the runner's shell", so it "could not have shown this defect". The security audit (`37-SECURITY.md` A-04, UF-37-2) read the runner log of run `40693904004` (a past run whose `release.yaml` already had `defaults.run.shell: bash`; read-only `GET`) and found nine step headers that read `/usr/bin/bash --noprofile --norc -e -o pipefail {0}`. So `-eo pipefail` is the runner's real shell, the E-11 replay used the right options, the baseline step was never fail-open, and the `set -o pipefail` line added in `b6fd9a0a` is redundant defence in depth. It stays (it is explicit, harmless and pinned by `baselinePipefailProblem`). The comment on the line in `release.yaml` and `pre-release.yaml`, the Javadoc of `baselinePipefailProblem` and the text of one guard fixture line were corrected in `e1e05604`; no guard logic or assertion changed.

**2. The handoff's gated sha moved again.** The `Handoff` bullet of the `Verification` section names `27f9ff89` as the last commit outside `.planning`. Two commits since then touch files outside `.planning` (below), so `37-USER-DISPATCH-DRYRUN.md` now names `e1e05604fba53aaa2aa1b33dc66ec33624343dbe`, with the measured figures 227 commits up to it and 85 files, 8036 insertions, 607 deletions against the published tip. The expected RED outcome of the dry run is unchanged.

**3. UF-37-1 (log-command forging through archive entry names) is fixed.** Approved by the user after the audit and the verification report. Commits (plain git, `fix(37): ...` with the trailer): `f523cf3c` (the report escaping and 11 added test cases) and `e1e05604` (the pipefail comment).

- Design: the fix is in the source of the text, `.github/scripts/compare_confluent_archives.py`, not in the workflows, so the artifact and the job log are both safe and the twin blocks of `release.yaml` and `pre-release.yaml` did not change (the workflow diff is the one comment line in each twin; the twins still differ in exactly the 8 line pairs). Every report line and the `ERROR:` text go through `safe_line()`: control characters (C0, DEL, C1, U+2028, U+2029) and the backslash become `\n`, `\r`, `\t`, `\xNN`, `\uNNNN` and `\\`, and a line that would still start (after leading whitespace) with `::` or `##[` gets a leading backslash. Classification, the `UNALLOWED` and `STALE-ROW` counts and the exit codes use the raw names; only printed text is escaped.
- Tests, additions only, in `CompareConfluentArchivesScriptUnitTest` (44 to 55): an added entry named by a newline followed by `::error title=forged::boom`, `::add-mask::`, `::stop-commands::`, `##[error]` and a CRLF variant, and a name that starts with `::`; the forged text still visible as escaped text on the entry's own `ONLY-B` and `UNALLOWED` lines; a name with ESC, CR, BEL, DEL, NEL, U+2028, U+2029, tab and a backslash; a hostile name covered by an allow-list row (exit 0, `CHANGED` reported escaped); a stale-row glob with a control character; `safe_line` itself for the leading-command guard and the doubled backslash. Each asserts no report line starts with `::` or `##[` and the report holds no raw control character, with the exit code and the line counts unchanged.
- Mutation: with the previous script (`git checkout -- ` of the file, then restored from a copy) the class reads 55 tests, 10 failed; the eleventh case (a name that only starts with `::`) is harmless on the old script and stays as a regression guard. With the fix: 55 tests, 0 failures. Gradle does not track the script as an input, so both runs used `--rerun`.
- Re-check of the audit's A-06 shape: a synthetic archive with entries named `a/Evil.class` + newline + `::error title=forged::boom`, `a/M.class` + newline + `::add-mask::secret`, `a/S.class` + newline + `::stop-commands::tok` and `a/C` + ESC `[31m` + CR + `.class`, compared by `python3 -I` from an empty directory with a comment-only allow-list. Previous script: exit 1, 6 report lines starting with `::`, 2 lines with a raw control character. Fixed script: exit 1, 0 lines starting with `::` or `##[`, 0 raw control characters, 4 `UNALLOWED` lines (the same 4), the escaped names on their own lines.
- Real archives (`/tmp/gsd37/w/archives`, the E-07 recipe output, scratch only): `base_gg4` against `cand_gg5` under the gg5 allow-list: exit 0, `RESULT: PASS`, `changed 4 same 7395`, 0 unallowed, 0 stale; `base_gg4` against `cand_tip`: exit 1, `RESULT: FAIL`, 238 unallowed, 0 stale, `files-b 7513 only-b 118 changed 120`. The `cand_tip` report is byte-identical (`cmp`) between the previous and the fixed script, so no real report line changes.
- Gates at `e1e05604`: cache-busted Docker-free `testUnit --rerun` with `DOCKER_HOST=unix:///nonexistent/docker.sock`: exit 0, 1669 tests (1658 + 11), 0 failures, 0 errors, 2 skipped, 80 files; `CompareConfluentArchivesScriptUnitTest` 55, `ReleaseWorkflowGuardUnitTest` 37 (unchanged); anchors mirror `rows=172 bad=0`; `git diff --quiet ff873086 HEAD -- src/main`: unchanged; the removed test lines since `ccdc72c6` (whitespace-insensitive) are 4, none an assertion, `fail(` or `expect`: three Javadoc lines and one fixture comment of the pipefail correction; hygiene greps empty before each commit. The full Docker-backed suite was not re-run: only a script, two workflow comment lines and Docker-free test classes changed.
- Evidence entries: none added. `37-evidence-check.py` requires E-01..E-N in order and E-19 is reserved for the user's dry-run report, so the evidence of this fix is recorded here and in `37-SECURITY.md` A-16.
