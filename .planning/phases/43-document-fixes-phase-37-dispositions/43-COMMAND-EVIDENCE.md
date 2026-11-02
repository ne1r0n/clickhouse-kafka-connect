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
