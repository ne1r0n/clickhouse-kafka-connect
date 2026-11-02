# Phase 43: Command evidence

- **Base commit:** `phase_start_sha` = `cb1ccb669be916b6da439d3f936c5cfbade42bd2` (HEAD when plan 43-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ... -PjavaVersion=21`, so every run shares one HotSpot 21 JVM line and the OpenJ9 crash is avoided.
- **Date (UTC):** 2026-11-02.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** `43-evidence-check.py` is a byte copy of Phase 42's `42-evidence-check.py`, `43-testcase-status.py` a byte copy of `42-testcase-status.py`, and `43-doc-check.py` a byte copy of Phase 38's `38-doc-check.py`; none is ever edited.
- **Red-reason rule:** a red counts only when the class XML exists, exactly the new testcase fails (named by its display name), it carries a `failure` element (no `error`) of type `org.opentest4j.MultipleFailuresError` whose text holds the reason string the task names, it took less than 30 s, and every other case of the class passes.
- **Red freeze rule:** the SHA-256 of the new test file is recorded at its red run. For row 37-IN-06 the committed test file has the same SHA-256; for row 37-IN-04 the committed file differs from the red copy by exactly the one planned added line.
- **Status-delta rule:** a gate compares the per-testcase status dump of the baseline with the new one against the committed expected-added file; no removed line and no unexpected added line is allowed (the existing-test stop rule).
- **No push happens in Phase 43.** Reads use `git ls-remote` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | DOC-01, DOC-02 | 43-01 | pass |
| E-02 | DOC-02 | 43-01 | pass |
| E-03 | DOC-02 | 43-01 | pending |
| E-04 | DOC-02 | 43-01 | pending |
| E-05 | DOC-02 | 43-01 | pending |
| E-06 | DOC-01, DOC-02 | 43-02 | pending |
| E-07 | DOC-01 | 43-02 | pending |
| E-08 | DOC-01 | 43-02 | pending |
| E-09 | DOC-02 | 43-03 | pending |
| E-10 | DOC-01, DOC-02 | 43-03 | pending |

### E-01: Phase start facts and the cache-busted testUnit baseline

Backs: DOC-01, DOC-02 (execution-start facts: base commit, origin re-read, the helper copies, the anchors mirror, the environment, the Spotless baselines of the three Java files the plan touches, the user's three answers read from the committed research file, and the start baseline measured on the unchanged tree instead of taken from the recorded 1824).

```sh
mkdir -p /tmp/gsd43/exec
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...HEAD
cp .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py
cp .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py
cp .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-doc-check.py .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py
cmp <source> <copy>
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
/usr/libexec/java_home -v 21
git show f8ff8fc3:.planning/phases/43-document-fixes-phase-37-dispositions/43-RESEARCH.md | grep -F -e 'Defer all four (Recommended)' -e 'Approve 37 IN-04, defer 28 IN-05(b) (Recommended)' -e 'gg6 note C2 + gg7 handoff (Recommended)' | wc -l
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd43/exec/sc-base-<name>.out 2> /tmp/gsd43/exec/sc-base-<name>.err
git rev-parse HEAD:src/main
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd43/exec/base-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-base-testunit
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py dump /tmp/gsd43/exec/xml-base-testunit /tmp/gsd43/exec/status-base-testunit.tsv
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-base-testunit --class <Class> files=1
```

```text
phase_start_sha: cb1ccb669be916b6da439d3f936c5cfbade42bd2
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
origin_custom_build_expected: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
divergence_left: 0
helper_copy_evidence: cmp-equal
helper_copy_status: cmp-equal
helper_copy_doc: cmp-equal
anchors_mirror: rows=190 bad=0
jdk21_home: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
testcontainers_host_override: set
approval_quotes_in_research: 3
spotless_baseline_release_guard: IS CLEAN
spotless_baseline_archive_check: IS CLEAN
spotless_baseline_archive_check_test: IS CLEAN
baseline_testunit_exit: 0
baseline_testunit_tests: 1824
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_recorded_testunit: 1824
baseline_matches_record: yes
base_dump_testunit: /tmp/gsd43/exec/status-base-testunit.tsv
release_guard_tests_base: 37
archive_check_tests_base: 7
compare_script_tests_base: 58
six_guard_tests_base: 188
src_main_tree: 3ec868c5bf1e32cdcd686cc7539925dd695b0a1f
jvm_crash: none
```

The only dirty paths are the two orchestrator-managed `.planning/` files, so the dirty-path rule holds. `origin/custom_build` equals the expected tip and the left count of `origin/custom_build...HEAD` is 0. The three Spotless verdicts equal the planning measurement. The three user answers (`Defer all four (Recommended)`, `Approve 37 IN-04, defer 28 IN-05(b) (Recommended)`, `gg6 note C2 + gg7 handoff (Recommended)`) are each present in `43-RESEARCH.md` as committed at `f8ff8fc3`. The measured `testUnit` total is 1824, equal to the recorded total, 0 failures, 0 errors, 2 skipped, exit 0; the dump holds 1824 lines. The per-class counts read from the dump and from the XML copy are 37 (`ReleaseWorkflowGuardUnitTest`), 7 (`ConfluentArchiveCheckUnitTest`), 58 (`CompareConfluentArchivesScriptUnitTest`) and 17 + 21 + 44 + 37 + 58 + 11 = 188 for the six workflow guard classes. No crash file (`hs_err_pid*`, `javacore.*`, `core*`, `Snap.*`) appeared in the repository root. `testcontainers_host_override` is recorded as set only: this shell sets it to a multi-line literal that begins with `null`, which breaks Ryuk, which is why every Gradle run unsets it.

2026-11-02T19:27:08Z

Verdict: pass

### E-02: Row 37-HDR: the header comment commit and the six workflow guard classes

Backs: DOC-02 (row 37-HDR, ROADMAP success criterion 4: the header of `increment-version.yml` names `IncrementVersionWorkflowGuardUnitTest`, the class that reads the file, and the six workflow guard classes pass cache-busted).

```sh
grep -c ReleaseWorkflowGuardUnitTest .github/workflows/increment-version.yml
git diff -U0 -- .github/workflows/increment-version.yml
grep -c ReleaseWorkflowGuardUnitTest .github/workflows/increment-version.yml
grep -c IncrementVersionWorkflowGuardUnitTest .github/workflows/increment-version.yml
head -1 .github/workflows/increment-version.yml
grep -E 'left unfixed|vnull' .github/workflows/increment-version.yml
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*IncrementVersionWorkflowGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*ReleaseWorkflowGuardUnitTest' --tests '*NightlyWorkflowGuardUnitTest' --tests '*UpstreamIntegrationWorkflowBytesGuardUnitTest' > /tmp/gsd43/exec/hdr-guards.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-hdr
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-hdr tests=188 failures=0 errors=0
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py failed /tmp/gsd43/exec/xml-hdr
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git ls-remote origin refs/heads/custom_build
git add .github/workflows/increment-version.yml
git commit -F /tmp/gsd43/exec/msg-c1.txt
git diff-tree --no-commit-id --name-only -r 566f92e4f612dc4a33eebd3683ea04643e15daa9
```

```text
hdr_old_count_before: 1
hdr_old_count_after: 0
hdr_new_count_after: 1
header_first_line_ok: yes
header_forbidden_text: none
hdr_diff: removed=2 added=2 comment_only=yes
guard_run_exit: 0
guard_run_tests: 188
guard_run_failures: 0
guard_run_errors: 0
anchors_mirror: rows=190 bad=0
code_commit: 566f92e4f612dc4a33eebd3683ea04643e15daa9
code_commit_files: .github/workflows/increment-version.yml
origin_reread: unchanged
jvm_crash: none
```

The two replaced lines are header lines 5 and 6 (110 and 94 columns after the edit); line 1 still starts `# FORK MODIFICATION` and no header line holds `left unfixed` or `vnull`. The six workflow guard classes ran 188 testcases (equal to `six_guard_tests_base`) with 0 failures and 0 errors before the commit; the anchored strings of the file are not on the edited lines, so the anchors mirror stays at `rows=190 bad=0`. `origin/custom_build` was re-read before the commit and is unchanged. The commit pointer `code_commit` is the one the disposition ledger of plan 43-03 consumes for row 37-HDR.

2026-11-02T19:28:30Z

Verdict: pass
