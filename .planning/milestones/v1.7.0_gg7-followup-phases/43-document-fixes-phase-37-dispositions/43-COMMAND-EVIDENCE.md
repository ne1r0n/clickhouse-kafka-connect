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
| E-03 | DOC-02 | 43-01 | pass |
| E-04 | DOC-02 | 43-01 | pass |
| E-05 | DOC-02 | 43-01 | pass |
| E-06 | DOC-01, DOC-02 | 43-02 | pass |
| E-07 | DOC-01 | 43-02 | pass |
| E-08 | DOC-01 | 43-02 | pass |
| E-09 | DOC-02 | 43-03 | pass |
| E-10 | DOC-01, DOC-02 | 43-03 | pass |

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

### E-03: Row 37-IN-04: the tag gate in CHAIN, tests first, one commit

Backs: DOC-02 (row 37-IN-04, approved by the user: the new fixture test seen red on the unchanged `CHAIN`, then the one added `CHAIN` line, green, one commit).

```sh
git show f8ff8fc3:.planning/phases/43-document-fixes-phase-37-dispositions/43-RESEARCH.md | grep -F 'Approve 37 IN-04'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd43/exec/sc-red-rg.out 2> /tmp/gsd43/exec/sc-red-rg.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*ReleaseWorkflowGuardUnitTest' > /tmp/gsd43/exec/red-chain.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-red-chain
cp src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java /tmp/gsd43/exec/red-ReleaseWorkflowGuardUnitTest.java
shasum -a 256 /tmp/gsd43/exec/red-ReleaseWorkflowGuardUnitTest.java
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py failed /tmp/gsd43/exec/xml-red-chain --expect 'ReleaseWorkflowGuardUnitTest#CIH-04: fixture: the chain rule requires the tag gate after the tests gate'
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-red-chain --class ReleaseWorkflowGuardUnitTest tests=38 failures=1 errors=0
diff -U0 /tmp/gsd43/exec/red-ReleaseWorkflowGuardUnitTest.java src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*ReleaseWorkflowGuardUnitTest' > /tmp/gsd43/exec/green-chain.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-green-chain
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-green-chain --class ReleaseWorkflowGuardUnitTest tests=38 failures=0 errors=0
git add src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
git commit -F /tmp/gsd43/exec/msg-c2.txt
git diff --numstat 33d079a986b04ade5c878caa0d3aa392915c38a8^ 33d079a986b04ade5c878caa0d3aa392915c38a8 -- src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
```

```text
approval_quote: Approve 37 IN-04, defer 28 IN-05(b) (Recommended)
approval_source: 43-RESEARCH.md at f8ff8fc3, User Decisions Recorded
spotless_red_file: IS CLEAN
red_run_exit: 1
red_class_tests: 38
red_class_failures: 1
red_failed_set: ReleaseWorkflowGuardUnitTest#CIH-04: fixture: the chain rule requires the tag gate after the tests gate
red_failure_type: org.opentest4j.MultipleFailuresError
red_reason_text: yes
red_file_sha256: 5c8244cfad77526db6519b1816c83d3373edda894f0d2e31ea44901f494b7b88
chain_gate_before: fail
red_to_commit_added_lines: 1
red_to_commit_removed_lines: 0
green_run_exit: 0
green_class_tests: 38
green_class_failures: 0
chain_gate_after: pass
spotless_commit_file: IS CLEAN
code_commit: 33d079a986b04ade5c878caa0d3aa392915c38a8
code_commit_files: src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
code_commit_deleted_lines: 0
jvm_crash: none
```

The user's approval is read from the committed research file at `f8ff8fc3` (section "User Decisions Recorded"), not from a relayed message. The red run took 0.005 s for the new testcase, whose single `failure` element has type `org.opentest4j.MultipleFailuresError` and holds `expected the fixture to be rejected by compare chain order` (5 occurrences: both `assertAll` executables fail on the unchanged `CHAIN`, because with no `TAG_GATE_STEP` in it neither a swapped nor a missing tag-gate step is a violation). The 37 pre-existing testcases of the class passed in the red run. The committed file differs from the saved red copy by exactly one added line, `TAG_GATE_STEP,` at line 115 of the `CHAIN` list between `TESTS_GATE_STEP,` and `PUBLISH_STEP,`; the commit adds 15 lines and deletes none. Both real workflows pass `workflowRunsTheCompareChainInOrder` in the green run (class total 38, 0 failures).

2026-11-02T19:29:05Z

Verdict: pass

### E-04: Row 37-IN-06: the zero-jar hint, tests first, one commit

Backs: DOC-02 (row 37-IN-06: the new zero-jar stderr test seen red on the unchanged helper, then the one size condition of `ConfluentArchiveCheck.run` changed from "not one" to "more than one", green, one commit).

```sh
grep -c 'result.libJarNames().size() != 1' src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd43/exec/sc-red-act.out 2> /tmp/gsd43/exec/sc-red-act.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*ConfluentArchiveCheckUnitTest' > /tmp/gsd43/exec/red-archive.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-red-archive
cp src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java /tmp/gsd43/exec/red-ConfluentArchiveCheckUnitTest.java
shasum -a 256 /tmp/gsd43/exec/red-ConfluentArchiveCheckUnitTest.java
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py failed /tmp/gsd43/exec/xml-red-archive --expect 'ConfluentArchiveCheckUnitTest#CI-20: an archive with no lib jar gets no stale directory hint on stderr'
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-red-archive --class ConfluentArchiveCheckUnitTest tests=8 failures=1 errors=0
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd43/exec/sc-commit-<name>.out 2> /tmp/gsd43/exec/sc-commit-<name>.err
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*ConfluentArchiveCheckUnitTest' > /tmp/gsd43/exec/green-archive.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-green-archive
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-green-archive --class ConfluentArchiveCheckUnitTest tests=8 failures=0 errors=0
git add src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
git commit -F /tmp/gsd43/exec/msg-c3.txt
git diff --numstat 7796ad0ab338ca623f41901dcb36935c49226a40^ 7796ad0ab338ca623f41901dcb36935c49226a40
```

```text
spotless_test_file_red: IS CLEAN
red_run_exit: 1
red_class_tests: 8
red_class_failures: 1
red_failed_set: ConfluentArchiveCheckUnitTest#CI-20: an archive with no lib jar gets no stale directory hint on stderr
red_failure_type: org.opentest4j.MultipleFailuresError
red_reason_text: yes
red_test_sha256: 8fad0b14340b7c48d455ed0ab4d74ec270d044b5254c56acba96d2bacf1fdec5
helper_condition_before: 1
green_run_exit: 0
green_class_tests: 8
green_class_failures: 0
committed_test_sha256_equals_red: yes
helper_numstat: 1 1
test_numstat_deleted: 0
spotless_helper: IS CLEAN
spotless_test_file: IS CLEAN
code_commit: 7796ad0ab338ca623f41901dcb36935c49226a40
code_commit_files: src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
jvm_crash: none
```

On the unchanged helper the new testcase took 0.006 s and failed with one `failure` element of type `org.opentest4j.MultipleFailuresError` that holds `lib jars found: ` (the helper printed the stale-directory hint line for an archive with zero lib jars, so the two `noneMatch` assertions fail while the exit-1 and shape-line assertions pass). The 7 pre-existing testcases of the class passed in the red run. The committed test file has the red copy's SHA-256 and the commit changes one line of `ConfluentArchiveCheck.java` (`result.libJarNames().size() != 1` becomes `result.libJarNames().size() > 1`; the `check` method's separate `libJarNames.size() != 1` shape violation is untouched) and adds 35 lines to the test file with none deleted. The existing `twoLibJarsAreNamedOnStderr` case still passes in the green run (class total 8, 0 failures).

2026-11-02T19:31:40Z

Verdict: pass

### E-05: Row 37-IN-05, the expected-added rows and the full testUnit delta

Backs: DOC-02 (row 37-IN-05(a): line 1 of the two allow-lists the review names; the expected-added rows of the phase; the full cache-busted `testUnit` whose per-testcase status delta against the E-01 baseline is exactly the two new testcases; the phase facts so far: no existing assertion removed, `src/main` unchanged).

```sh
grep -c 'compare-confluent-archives' .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
git diff -U0 -- .planning/scripts
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*CompareConfluentArchivesScriptUnitTest' > /tmp/gsd43/exec/allowlist.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-allowlist
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-allowlist --class CompareConfluentArchivesScriptUnitTest tests=58 failures=0 errors=0
git add .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
git commit -F /tmp/gsd43/exec/msg-c4.txt
git diff --quiet cb1ccb669be916b6da439d3f936c5cfbade42bd2 HEAD -- .planning/scripts/confluent-archive-allowlist-25.txt .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt
git diff --quiet v1.7.0_gg6 HEAD -- .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt
shasum -a 256 .planning/phases/43-document-fixes-phase-37-dispositions/43-expected-added.tsv
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd43/exec/p01-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-p01-testunit
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py dump /tmp/gsd43/exec/xml-p01-testunit /tmp/gsd43/exec/status-p01-testunit.tsv
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py delta /tmp/gsd43/exec/status-base-testunit.tsv /tmp/gsd43/exec/status-p01-testunit.tsv .planning/phases/43-document-fixes-phase-37-dispositions/43-expected-added.tsv
git diff --quiet cb1ccb669be916b6da439d3f936c5cfbade42bd2 HEAD -- src/main
git diff -U0 cb1ccb669be916b6da439d3f936c5cfbade42bd2 HEAD -- src/test src/testFixtures src/integrationTest
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git ls-remote origin refs/heads/custom_build
```

```text
allowlist_old_name_count_before: 2
allowlist_old_name_count_after: 0
allowlist_new_name_line1: 2
allowlist_diff: 2 files, line 1 only
allowlists_untouched: yes
allowlist_run_exit: 0
allowlist_run_tests: 58
allowlist_run_failures: 0
code_commit: 103221a459dcd742a7404d3ea8eceefa17493a29
code_commit_files: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
expected_added_rows: 2
expected_added_sha256: 25f7aebc771c23c5199b8dbd30f7c3e6f16edbaaefafa484c38b17d091ebb246
testunit_exit: 0
testunit_tests: 1826
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 2
status_delta: removed=0 added=2 equals_expected=yes
removed_test_lines: 1
src_main_unchanged: yes
anchors_mirror: rows=190 bad=0
origin_reread: unchanged
jvm_crash: none
```

Before the edit, line 1 of each of the gg4 and gg5 lists named the hyphenated `compare-confluent-archives.py` once (count 2 over the two files, 0 after); after it, both lines start `# Allow-list for .github/scripts/compare_confluent_archives.py (`, and each C4 hunk is `@@ -1 +1 @@`. `confluent-archive-allowlist-25.txt` and `-v1.7.0_gg6.txt` are unchanged since the phase start, and the gg6 list is identical to its copy in tag `v1.7.0_gg6`; both stay with row 37-IN-05(b), deferred by plan 43-03. `CompareConfluentArchivesScriptUnitTest` ran 58 testcases (its E-01 count) with 0 failures, including `WR-02: every allow-list shipped under .planning/scripts loads` (`everyShippedAllowListStillLoads`). The full cache-busted `testUnit` ran 1826 tests (the E-01 total 1824 plus 2) with 0 failures, 0 errors and 2 skipped; its dump holds 1826 lines and `43-testcase-status.py delta` against `43-expected-added.tsv` reports removed=0 added=2 with the added set equal to the expected file. The only line removed under `src/test`, `src/testFixtures` and `src/integrationTest` since `cb1ccb66` is the old size condition `if (result.libJarNames().size() != 1) {` of `ConfluentArchiveCheck.run`; `src/main` is byte-identical to the phase start. The full ClickHouse-backed suite is not run in this phase (decision and reason recorded in the plan's Execution environment and re-stated in E-10).

2026-11-02T19:32:31Z

Verdict: pass

### E-06: The gg6 publication facts, re-read from the GitHub REST API

Backs: DOC-01 (the facts the corrections state: the publish run, the release, the closed Dependabot pull requests, the one tests run on the merge, the two tags and the equal trees of the merge and its second parent; read live before any text is written).

```sh
R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect
curl -fsS -H 'Accept: application/vnd.github+json' "$R/actions/runs/41298074815" -o /tmp/gsd43/exec/gh-run.json
curl -fsS -H 'Accept: application/vnd.github+json' "$R/releases/tags/v1.7.0_gg6" -o /tmp/gsd43/exec/gh-release.json
curl -fsS -H 'Accept: application/vnd.github+json' "$R/pulls/10" -o /tmp/gsd43/exec/gh-pr10.json
curl -fsS -H 'Accept: application/vnd.github+json' "$R/issues/10" -o /tmp/gsd43/exec/gh-issue10.json
curl -fsS -H 'Accept: application/vnd.github+json' "$R/pulls/17" -o /tmp/gsd43/exec/gh-pr17.json
curl -fsS -H 'Accept: application/vnd.github+json' "$R/issues/17" -o /tmp/gsd43/exec/gh-issue17.json
curl -fsS -H 'Accept: application/vnd.github+json' "$R/actions/workflows/tests.yaml/runs?head_sha=72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236" -o /tmp/gsd43/exec/gh-tests-runs.json
python3 -I <parse the seven saved responses and print the keys below>
git ls-remote origin refs/tags/v1.7.0_gg6 refs/tags/pre-v1.7.0_gg6 refs/heads/custom_build
git diff --quiet 72e8ccff a46be37f -- src build.gradle.kts gradle .github
```

```text
run_name: Release custom_build (publish)
run_event: workflow_dispatch
run_head_sha: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
run_conclusion: success
release_id: 430511927
release_author: github-actions[bot]
release_draft: false
release_prerelease: false
release_asset_name: clickhouse-kafka-connect-v1.7.0_gg6.zip
release_asset_size: 13558692
pr10_state: closed
pr10_merged: false
pr10_closed_at: 2026-11-01T10:26:38Z
pr10_closed_by: dependabot[bot]
pr17_state: closed
pr17_merged: false
pr17_closed_at: 2026-11-01T10:28:35Z
pr17_closed_by: dependabot[bot]
tests_runs_on_merge_sha: 1
tests_run_id: 41297502026
tests_run_event: push
tests_run_branch: custom_build
tests_run_conclusion: success
tag_v1_7_0_gg6: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
tag_pre_v1_7_0_gg6: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
merge_tip_trees_equal: yes
facts_match_release_record: yes
api_auth: none
requests: 7
```

Every value equals the expectation taken from `38-RELEASE-RECORD.md`, `MILESTONES.md` and the research (read from the GitHub REST API; the two tag ids from `git ls-remote origin`; the tree equality from `git diff --quiet`). The release holds exactly one asset. The tests-run list for the merge SHA holds one run, the push run on `custom_build`. `origin/custom_build` still equals `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`. The seven requests carried no token and no `Authorization` header; the responses are saved under the scratch directory only.

2026-11-02T19:36:25Z

Verdict: pass

### E-07: The gg1 half of WR-08 and FUT-05 in gg6 row C2

Backs: DOC-01 (ROADMAP Phase 43 success criterion 1: item 6 of the gg1 note carries the gg6 evidence labels and both client lines; FUT-05 is named in row C2 of the gg6 note with a correction notice; the C2 basis read from the code).

```sh
grep -rn 'getRetry()' src/main/java
grep -rln 'CONNECTOR_RETRY_TIMEOUT' src/main/java
python3 -I <list the methods of ClickHouseHelperClient.java that read the retry field, outside its field, constructor and builder>
grep -oF 'of any line' docs/ROLLOUT-v1.7.0_gg1.md | wc -l
grep -oF 'FUT-05' docs/ROLLOUT-v1.7.0_gg6.md | wc -l
git add docs/ROLLOUT-v1.7.0_gg1.md docs/ROLLOUT-v1.7.0_gg6.md
git commit -F /tmp/gsd43/exec/msg-d1.txt
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py links docs/ROLLOUT-v1.7.0_gg1.md docs/ROLLOUT-v1.7.0_gg6.md
git diff -U0 965e646f934cded3a1d1a855edc19c8a0cdfe145^ 965e646f934cded3a1d1a855edc19c8a0cdfe145 -- <each note>
```

```text
gg1_any_line_before: 1
gg1_any_line_after: 0
gg1_measured_public_after: 0
gg1_item6_labels: yes
gg6_fut05_before: 0
gg6_fut05_count: 2
gg6_c2_label: read from the code
gg6_c2_scoped: yes
retry_keys_set_retry_sites: 3
retry_keys_helper_loops: pingV1 pingV2 queryV1 queryV2
retry_keys_retry_timeout_reads: ClickHouseSinkConfig.java only
gg6_notice_before_summary: yes
links: OK
gg1_scope: removed=2 added=2
gg6_scope: removed=1 added=3
notes_commit: 965e646f934cded3a1d1a855edc19c8a0cdfe145
notes_commit_files: docs/ROLLOUT-v1.7.0_gg1.md docs/ROLLOUT-v1.7.0_gg6.md
```

The C2 sentence rests on three greps of the code at this tree (evidence kind: read from the code). `getRetry()` is called at three places, each `.setRetry(csc.getRetry())` (`ClickHouseWriter` lines 109 and 1840, `KeeperStateProvider` line 64), so `retryCount` reaches only the helper client builder; the helper's `retry` field is read only inside `pingV1`, `pingV2`, `queryV1` and `queryV2`; `CONNECTOR_RETRY_TIMEOUT` occurs only in `ClickHouseSinkConfig.java`. In the gg1 note, "of any line" occurred once before and 0 times after; item 6 holds `decodes ZSTD (measured)`, `measured by the maintainers in research`, `not re-run on the release tip`, the link to rows D9 and D13 and both client lines. In the gg6 note the notice paragraph sits at line 3, above the summary paragraph at line 5; FUT-05 occurs twice (the notice and row C2); only row C2 changed (one removed, one added line) plus the notice and its blank line. The 9 relative targets of the two notes resolve. The correction notice's claim about row D7 rests on `git diff v1.7.0_gg6 HEAD -- docs/ROLLOUT-v1.7.0_gg6.md`, which shows that row D7 was the one row changed since the tag (read from git).

2026-11-02T19:36:25Z

Verdict: pass

### E-08: FORK-DELTA: the gg6 publication in the past tense, tag-based commands, items 12, 22 and 23

Backs: DOC-01 (ROADMAP Phase 43 success criterion 1: the FORK-DELTA paragraphs the gg6 close numbered 6, 139 and 286 describe the publication in the past tense; the gg6 reproduction commands name the tags and reproduce the published rows; items 12, 22 and 23 and the protobuf row state the closed facts).

```sh
git rev-parse --verify refs/tags/v1.7.0 refs/tags/v1.7.0_gg5 refs/tags/v1.7.0_gg6
grep -oF <each of the fourteen old phrases> docs/FORK-DELTA.md | wc -l
grep -oF <each new phrase> docs/FORK-DELTA.md | wc -l
git diff --shortstat v1.7.0 v1.7.0_gg6 -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg6 -- src/main
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py numbers docs/FORK-DELTA.md 'after gg6: `v1.7.0` to `v1.7.0_gg6`' --wider <wider> --narrow <narrow>
git diff --shortstat v1.7.0_gg5 v1.7.0_gg6 -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0_gg5 v1.7.0_gg6 -- src/main
git diff -U0 -- docs/FORK-DELTA.md
git add docs/FORK-DELTA.md
git commit -F /tmp/gsd43/exec/msg-d2.txt
git show 870fe9d8f918d67516666e8272960ab1df628621^:docs/FORK-DELTA.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py links docs/FORK-DELTA.md
```

```text
old_phrases_absent: 14
new_phrases_present: yes
run_id_count: 3
pr10_closed_count: 2
tag_commands: 2
table_matches_tag: yes
gg5_gg6_paragraph_reproduces: yes
raw_table_rows_unchanged: yes
scope: removed=10 added=10
links: OK
forkdelta_commit: 870fe9d8f918d67516666e8272960ab1df628621
forkdelta_commit_files: docs/FORK-DELTA.md
```

Each of the fourteen old phrases of the task occurred at least once before the edit (the eleven old strings of the text specification each exactly once) and occurs 0 times now. The new text is present: the run id `41298074815` occurs 3 times (the intro paragraph, item 22 and the syncer paragraph), the closure of PR #10 occurs twice (the protobuf row and item 12), and each of the two tag-based gg6 commands occurs once. The tag-to-tag shortstats print 331 files, 39665 insertions and 1161 deletions at the wider scope and 40, 2989 and 272 for `src/main`, which equal the after-gg6 rows (the rows themselves are byte-identical before and after: all 14 rows of the two scopes unchanged); the gg5-to-gg6 paragraph states 79 files, 7939 insertions and 478 deletions (wider) and 7, 112 and 31 (`src/main`), which the tag-to-tag command prints. The compare result in the syncer paragraph (0 unallowed, 0 stale rows, baseline `v1.7.0_gg5`) and the rollback tag pushed by hand after the publish run are read from `38-RELEASE-RECORD.md`; the run, release and PR facts are those of E-06 (read from the GitHub REST API). The commit changes exactly ten physical lines (ten removed, ten added; adjacent lines share hunks). Item 22 names the two moved release-record paths under `.planning/milestones/v1.7.0_gg6-maintenance-phases/`, and the 11 relative targets of the file resolve.

2026-11-02T19:38:30Z

Verdict: pass

### E-09: The disposition ledger, its checker and FORK-DELTA items 19 to 21

Backs: DOC-02 (ROADMAP Phase 43 success criteria 2 and 3: one ledger row for each Phase 37 item with 37-IN-05 split into a fixed and a deferred part, each fixed row pointing at its Phase 43 code commit, the four `src/main` rows deferred in the user's own words, the twelve other dispositioned rows carried, the Phase 44 handoff; FORK-DELTA items 19, 20 and 21 agree with the ledger).

```sh
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py check .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py counts .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py pointer .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md <37-HDR|37-IN-04|37-IN-06|37-IN-05(a)>
git diff-tree --no-commit-id --name-only -r <C1|C2|C3|C4>
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py check .planning/milestones/v1.7.0_gg6-maintenance-phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py check /tmp/gsd43/exec/v-ledger-bad-owner.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py check /tmp/gsd43/exec/v-ledger-short-pointer.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py status .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md 37-IN-05
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py carried .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md .planning/milestones/v1.7.0_gg6-maintenance-phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py carried /tmp/gsd43/exec/v-ledger-bad-carried.md .planning/milestones/v1.7.0_gg6-maintenance-phases/37-ci-hardening-advisory-debt/37-ADVISORY-LEDGER.md
git diff -U0 HEAD^ HEAD -- docs/FORK-DELTA.md
git add docs/FORK-DELTA.md .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py
git commit -F /tmp/gsd43/exec/msg-d3.txt
```

```text
ledger_head: 5f32e312000dbaa058bc8331c46694c545cd7686
ledger_check: OK ledger: 18 rows, 0 problems
ledger_counts: rows=18 bases=17 fixed=4 dispositioned=14
pointers_match_evidence: yes
pointer_files_ok: yes
checker_rejects_37_ledger: yes
checker_rejects_blank_owner: yes
checker_rejects_short_pointer: yes
status_37_in_05: 37-IN-05=fixed,dispositioned
carried_ids: 12
carried_check: OK carried: 12 rows, 0 problems
checker_rejects_bad_carried: yes
user_quotes_rows: yes
handoff_fut05: yes
forkdelta_items_scope: removed=3 added=3
item19_cites_c1: yes
old_ledger_path_absent: yes
ledger_commit: e72447b574ee822c5a8404ea28d3d22e2642f8d0
ledger_commit_files: .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py docs/FORK-DELTA.md
```

The ledger holds 18 rows for 17 findings: 4 `fixed` (37-HDR at E-02, 37-IN-04 at E-03, 37-IN-05(a) at E-05, 37-IN-06 at E-04) and 14 `dispositioned`, each with the owner `fork maintainer (the user)` and a re-open trigger. Every fixed row's 40-hex pointer equals its code commit in E-02..E-05, and each of those commits touches exactly the files of its row (C4: the gg4 and gg5 allow-lists only). The four `src/main` rows (26-IN-02, 27-WR-01, 27-IN-01, 28-IN-01) each quote "Defer all four (Recommended)", and rows 37-IN-04 and 28-IN-05(b) each quote "Approve 37 IN-04, defer 28 IN-05(b) (Recommended)"; the three answers are read from `43-RESEARCH.md` at `f8ff8fc3`. The checker discriminates: it rejects the Phase 37 ledger (old statuses, IDs and owners), a copy of the ledger with a blank owner on row 26-IN-02, a copy with the 37-HDR pointer cut to 7 hex digits, and (for `carried`) a copy with the trigger of the 25-WR-03 line altered. The twelve carried lines equal the Phase 37 rows in owner and trigger. The handoff section holds "FUT-05 stays open" and RSK-03. D3 changed exactly three physical lines of `docs/FORK-DELTA.md` (items 19, 20 and 21): item 19 is closed and cites `566f92e4`, item 20 names `43-DISPOSITION-LEDGER.md`, item 21 names run `41297502026`; the old item-19 wording and the moved Phase 37 phase path occur 0 times. No file under `src/` changed and nothing was pushed.

2026-11-02T19:42:05Z

Verdict: pass

### E-10: Phase gate: testUnit delta, anchors, scope, decisions, origin

Backs: DOC-01, DOC-02 (ROADMAP Phase 43 success criterion 4: cache-busted `testUnit` whose status delta against the E-01 baseline is exactly the two new testcases, the anchors invariant with the anchored-line proof, the scope facts against the phase start, the recorded decisions not to run the full suite or the cumulative invariant script, the DOC-01 re-checks, the ledger re-check and origin).

```sh
git rev-parse HEAD
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd43/exec/gate-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd43/exec/xml-gate-testunit
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py dump /tmp/gsd43/exec/xml-gate-testunit /tmp/gsd43/exec/status-gate-testunit.tsv
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py delta /tmp/gsd43/exec/status-base-testunit.tsv /tmp/gsd43/exec/status-gate-testunit.tsv .planning/phases/43-document-fixes-phase-37-dispositions/43-expected-added.tsv
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py junit /tmp/gsd43/exec/xml-gate-testunit tests=1826 failures=0 errors=0 skipped=2
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git diff --name-only <phase_start_sha> HEAD
git diff -U0 <phase_start_sha> HEAD -- .github/workflows/increment-version.yml
git diff --quiet <phase_start_sha> HEAD -- src/main
git diff --quiet <phase_start_sha> HEAD -- src/testFixtures src/integrationTest build.gradle.kts gradle VERSION CHANGELOG.md .planning/scripts/fork-anchors.txt
git diff --name-only --diff-filter=M <phase_start_sha> HEAD -- src/test
git diff --name-only --diff-filter=ADR <phase_start_sha> HEAD -- src/test src/testFixtures src/integrationTest
git diff -U0 <phase_start_sha> HEAD -- src/test src/testFixtures src/integrationTest
git log --format=%H <phase_start_sha>..HEAD -- . ':!.planning'
git diff --quiet v1.7.0_gg6 HEAD -- .planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd43/exec/sc-<name>.out 2> /tmp/gsd43/exec/sc-<name>.err
python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-ledger-check.py check .planning/phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md
git ls-remote origin refs/heads/custom_build
```

```text
gate_head: c55ff51da96ad49a4047f916e0ef71318dc9b3a6
testunit_exit: 0
testunit_tests: 1826
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 2
status_delta: removed=0 added=2 equals_expected=yes
anchors_mirror: rows=190 bad=0
anchored_paths_changed: .github/workflows/increment-version.yml
anchored_strings_in_diff: 0
invariants_script: not run
invariants_reason: no anchored line and no fork-anchors.txt row changed in this phase (the only anchored path changed is increment-version.yml and no changed line of it holds an anchored string), the mirror reads rows=190 bad=0, and the cumulative script needs a re-run only when a commit changes an anchored line or fork-anchors.txt (42-04-SUMMARY); Phase 44's release gate runs the anchors gate again
full_suite: not run
full_suite_reason: src/main did not change (no src/main row was approved), and the Java changes are Docker-free classes of package com.clickhouse.kafka.connect.ci that testUnit runs; Phase 44 runs two full suites on the release tip
src_main_unchanged: yes
unchanged_paths: yes
test_files_modified: src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
test_files_added_deleted_renamed: none
removed_test_lines: 1
non_planning_commits: e72447b574ee822c5a8404ea28d3d22e2642f8d0 870fe9d8f918d67516666e8272960ab1df628621 965e646f934cded3a1d1a855edc19c8a0cdfe145 7796ad0ab338ca623f41901dcb36935c49226a40 33d079a986b04ade5c878caa0d3aa392915c38a8 566f92e4f612dc4a33eebd3683ea04643e15daa9
allowlist_commit: yes
allowlists_25_gg6_untouched: yes
docs_changed: docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg1.md docs/ROLLOUT-v1.7.0_gg6.md
github_changed: .github/workflows/increment-version.yml
spotless_release_guard: IS CLEAN
spotless_archive_check: IS CLEAN
spotless_archive_check_test: IS CLEAN
doc01_rechecks: pass
ledger_recheck: OK ledger: 18 rows, 0 problems
origin_custom_build_at_gate: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
pushes: none
post_review_recheck: pending the orchestrator's code review: after it and any review fix, re-run the scoped Spotless hook and the filtered testUnit of each class a fix touches, the anchors mirror at rows=190 and the ledger check; a review fix is its own commit and is recorded in the review disposition, not in the ledger
jvm_crash: none
```

The gate `testUnit` ran cache-busted (`--rerun`) with Docker unreachable on HotSpot 21: 1826 testcases (the E-01 baseline 1824 plus 2), 0 failures, 0 errors, 2 skipped, exit 0; its dump holds 1826 lines and the status delta against the baseline reads removed=0 added=2 with the added set equal to `43-expected-added.tsv`. The removed-test-lines fact is the old size condition `if (result.libJarNames().size() != 1) {` of `ConfluentArchiveCheck.run` and nothing else. `src/main`, `src/testFixtures`, `src/integrationTest`, `build.gradle.kts`, `gradle/`, `VERSION`, `CHANGELOG.md` and `fork-anchors.txt` are byte-identical to the phase start; the 25 and gg6 allow-lists are unchanged since the phase start and the gg6 list equals its copy in tag `v1.7.0_gg6`. The non-planning commits since the phase start are exactly D3, D2, D1, C3, C2 and C1. The DOC-01 re-checks hold: the gg1 note holds "of any line" 0 times, row C2 of the gg6 note names FUT-05, and FORK-DELTA holds the publish run id `41298074815` 3 times. Both decisions not to run the cumulative invariant script and the full suite carry their reasons above. `origin/custom_build` still equals the E-01 value and nothing was pushed.

2026-11-02T19:43:43Z

Verdict: pass
