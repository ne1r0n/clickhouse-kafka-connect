# Phase 45: Command evidence

- **Base commit:** `local_head` = `2c19e93fca9e155f08ddd646eb06f8e990d94ecf` (HEAD when plan 45-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-04.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `45-evidence-check.py` is the read-only gate helper written at plan time (byte-identical to the Phase 34-44 copies); it is never edited.
- **No push happens in Phase 45.** Reads use `git ls-remote` and `gh api` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | UPS-01 | 45-01 | pass |
| E-02 | UPS-01 | 45-01 | pass |
| E-03 | UPS-01 | 45-01 | pass |
| E-04 | UPS-01, UPS-02, UPS-04 | 45-01 | pass |
| E-05 | UPS-02, UPS-04 | 45-02 | pass |
| E-06 | UPS-02 | 45-02 | pass |
| E-07 | UPS-02 | 45-03 | pass |
| E-08 | UPS-02 | 45-03 | pass |
| E-09 | UPS-02 | 45-03 | pass |
| E-10 | UPS-02 | 45-04 | pass |
| E-11 | UPS-02 | 45-04 | pass |
| E-12 | UPS-02 | 45-04 | pass |
| E-13 | UPS-03, UPS-04 | 45-05 | pass |
| E-14 | UPS-05 | 45-05 | pass |
| E-15 | UPS-02, UPS-04 | 45-05 | pass |
| E-16 | DEP-01 | 45-06 | pass |
| E-17 | DEP-01, DEP-02 | 45-06 | pass |
| E-18 | UPS-01, UPS-02, UPS-03, UPS-04, UPS-05, DEP-02 | 45-06 | pending (plan 45-06) |

### E-01: Pre-flight: upstream and origin re-read, conflict set, baseline suites on the untouched tip

Backs: UPS-01, ROADMAP Phase 45 criterion 1 (pre-flight reads before the merge; baseline suites on the untouched tip so every later red is attributable to the tail).

```sh
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
colima list
git remote get-url upstream
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*' | sed 's#.*refs/tags/##' | sort -V | tail -3
git ls-remote --tags upstream 'v1.7.1*'
git fetch upstream --tags
git rev-parse upstream/main
git merge-base HEAD 83808df9a0993cf49b2ceaa4633c782e550c0180
git diff --name-only 9bed6f7970670a55c66be8efda246889e1f95bd0 83808df9a0993cf49b2ceaa4633c782e550c0180 | wc -l
git merge-tree --write-tree --name-only HEAD 83808df9a0993cf49b2ceaa4633c782e550c0180 > /tmp/gsd45/exec/mt-preflight.txt 2>&1
gh api repos/ne1r0n/clickhouse-kafka-connect/branches/custom_build --jq .commit.sha
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/tags/pre-v1.7.0_gg8 --jq length
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/heads/sync/v1.7.0 --jq '.[].object.sha'
git log -1 --format=%H -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
bash .planning/scripts/verify-merge-invariants-selftest.sh > /tmp/gsd45/exec/selftest.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
md5 -q .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-baseline.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd45/exec/suite-baseline.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/test
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
local_head: 2c19e93fca9e155f08ddd646eb06f8e990d94ecf
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
upstream_url: https://github.com/ClickHouse/clickhouse-kafka-connect.git
upstream_main: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_moved: no
upstream_newest_release_tag: v1.7.0
upstream_release_tag_past_v170: no
upstream_v171_tag: absent
merge_base: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_changed_paths: 11
merge_tree_exit: 1
merge_tree_conflicts: .github/workflows/tests.yaml; CHANGELOG.md; VERSION; gradle/libs.versions.toml; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
merge_tree_conflicts_match_roadmap: yes
origin_custom_build: 9483476322e0d331e81c5b92ac266a18f0f01450
origin_custom_build_moved: no
origin_pre_tag: absent
origin_sync_branch: absent
gate_scripts_last_commit: 046fe59defa5ed7cab9dc8cb29c25b4e36fd9d28
selftest_last_line: selftest: no failures
anchors_mirror: rows=190 bad=0
helper_md5: 7123d354f51d45e019837f43b3a30fbc
baseline_testunit_exit: 0
baseline_testunit_tests: 1826
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_suite_exit: 0
baseline_suite_tests: 2425
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_suite_files: 138
baseline_suite_duration: 176s
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
baseline_docker_only_tests: 599
```

The gate self-test ran 16 cases (ST1-ST9 variants), all PASS, in about 55 minutes (ST4 runs the cumulative gate). `baseline_docker_only_tests` is 2425 suite tests minus 1826 testUnit tests. The research rehearsal's full-suite totals (2245 tests, partial, from a crashed run) were not used; these numbers are the measured expectation for the later plans. No `core.*`, `javacore.*`, `Snap.*` or `jitdump.*` file was left in the root. `origin_custom_build` has the `94834763` prefix, so origin did not move since planning.

2026-11-04T20:06:30Z
Verdict: pass

### E-02: Rollback tag, upstream re-read immediately before the merge, and the mechanical merge M

Backs: UPS-01, ROADMAP Phase 45 criterion 1 (the tag on the pre-merge tip; `git ls-remote upstream main` re-read immediately before `git merge`; one mechanical two-parent merge commit whose conflicted hunks and the one auto-merged dead-resource hunk all take the fork side).

```sh
git tag pre-v1.7.0_gg8 HEAD
git rev-parse refs/tags/pre-v1.7.0_gg8
git cat-file -t refs/tags/pre-v1.7.0_gg8
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*' | sed 's#.*refs/tags/##' | sort -V | tail -1
git merge-tree --write-tree --name-only pre-v1.7.0_gg8 83808df9a0993cf49b2ceaa4633c782e550c0180 > /tmp/gsd45/exec/mt-atmerge.txt 2>&1
git merge --no-ff --no-commit 83808df9a0993cf49b2ceaa4633c782e550c0180
git diff --name-only --diff-filter=U | LC_ALL=C sort
python3 -I /tmp/gsd45/exec/keep_head.py <the six conflicted files>
git add <the six conflicted files>
git grep -n --cached -E '^(<{7}|>{7})( |$)' -- src CHANGELOG.md VERSION gradle .github
git diff --cached --quiet pre-v1.7.0_gg8 -- .github/workflows/tests.yaml CHANGELOG.md VERSION <FD> <HC>
git diff --cached --numstat pre-v1.7.0_gg8 -- gradle/libs.versions.toml
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava > /tmp/gsd45/exec/compilejava-premerge.log 2>&1
git commit -F /tmp/gsd45/exec/msg-m.txt
git rev-list --parents -n 1 HEAD
git diff --name-only HEAD^1 HEAD
git diff --numstat HEAD^1 HEAD
```

```text
pre_tag_target: baa404cd5ed95c556db0acff84def1024c3bafe3
pre_tag_object_type: commit
upstream_main_at_merge: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_newest_release_tag_at_merge: v1.7.0
merge_tree_conflicts_at_merge: .github/workflows/tests.yaml; CHANGELOG.md; VERSION; gradle/libs.versions.toml; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
expected_provenance_paths: 11
merge_conflicted_paths: .github/workflows/tests.yaml; CHANGELOG.md; VERSION; gradle/libs.versions.toml; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
dead_resource_hunk_dropped: yes
marker_scan_staged: clean
fork_side_paths_identical_to_parent1: yes
libs_versions_numstat: 1 0
anchors_mirror_staged: rows=190 bad=0
resolution_tests_yaml: fork side, 1 hunk (no cloud job in the fork)
resolution_changelog: fork side in both hunks (2 hunks, plan expected 1)
resolution_version: fork side, v1.7.0_gg7
resolution_catalog: fork side (shadow 9.6.1); bump lines no-op, connect-json line kept
resolution_field_descriptor: fork side, 1 hunk
resolution_helper_client: fork side in all three hunks plus the dead resource restored
compile_java_before_commit: 0
merge_commit: 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10
merge_parent1: baa404cd5ed95c556db0acff84def1024c3bafe3
merge_parent2: 83808df9a0993cf49b2ceaa4633c782e550c0180
merge_changed_paths: 6
merge_changed_path_list: build.gradle.kts; gradle/libs.versions.toml; src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/SlashColumnRoundTripTest.java
merge_numstat_total: +298/-5
merge_planning_paths: 0
```

Every hunk was resolved by the one script `keep_head.py` (scratch, not committed), which drops the three marker lines and the upstream side of each marker hunk and touches nothing else, so the auto-merged lines (the catalog's connect-json line, the three no-op bumps) survive; no whole-file `--ours` or `-X ours` was used. The auto-merged dead resource in `describeTableV1` was dropped by a separate explicit Edit that restored the fork's `.executeAndWait()) {` try header. `CHANGELOG.md` carried two conflict hunks (the top of the file and the `Known Issues` block), where the plan predicted one; both take the fork side, which matches the plan's whole-file intent and leaves the file byte-identical to M's first parent. The pre-merge `compileJava` passed. The merge numstat equals the research rehearsal (+298/-5).

2026-11-04T20:08:20Z
Verdict: pass

### E-03: Record commit and the unedited tail gate over M

Backs: UPS-01, ROADMAP Phase 45 criterion 1 (the conflicted-paths record and the per-path checklist land in the commit after M; the unedited tail gate over M..record commit ends `result=PASS`).

```sh
git merge-tree --write-tree --name-only 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10^1 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10^2 > /tmp/gsd45/exec/mt-m.txt 2>&1
git add .planning/scripts/conflicted-paths-v1.7.0-tail-3.txt .planning/MERGE-CHECKLIST-v1.7.0-tail-3.md
git commit -F /tmp/gsd45/exec/msg-rec.txt
git diff --name-only HEAD^ HEAD | LC_ALL=C sort
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 43676e9aaff31961c062217bede6116d1403ef84 > /tmp/gsd45/exec/tail-gate-m.log 2>&1
```

```text
record_commit: 43676e9aaff31961c062217bede6116d1403ef84
record_commit_files: .planning/MERGE-CHECKLIST-v1.7.0-tail-3.md .planning/scripts/conflicted-paths-v1.7.0-tail-3.txt
conflicted_paths_lines: 6
tail_gate_line: invariants: provenance_paths=11 anchor_commits=2 anchor_rows=190 marker_commits=20 anchor_exempt=0 result=PASS
tail_gate_exit: 0
tail_gate_duration: 10s
```

The gate ran unedited with the full SHAs, `PRE_TAG=pre-v1.7.0_gg8` and the new conflicted-paths file; the scripts have not changed since `046fe59d`. `anchor_commits=2` (M and the record commit) because the range is `pre-v1.7.0_gg8..record commit` over a short history. The checklist's Sections 1, 2, 2a and 3, the gate commands and Recorded Facts 46-47 are in the record commit.

2026-11-04T20:09:05Z
Verdict: pass

### E-04: Break inventory on the bare merge (before any repair)

Backs: UPS-01, UPS-02, UPS-04 (every break plans 45-02 and 45-03 repair is measured on the bare merge first; the one new Maven coordinate is checked against Maven Central; the static facts later plans rely on).

```sh
git diff --quiet 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- . ':(exclude).planning'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileTestFixturesJava compileIntegrationTestJava > /tmp/gsd45/exec/compile-main-m.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava > /tmp/gsd45/exec/compile-test-m.log 2>&1
find ~/.gradle/caches/modules-2/files-2.1/org.apache.kafka/connect-json/4.4.0 -name 'connect-json-4.4.0.jar'
shasum -a 1 <connect-json-4.4.0.jar>
curl -fsS https://repo1.maven.org/maven2/org/apache/kafka/connect-json/4.4.0/connect-json-4.4.0.jar.sha1
grep -F 'kafka-connect-json' gradle/libs.versions.toml
grep -cF 'writeColumnWithRecordContext(' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
grep -cF '    long writeRecordsRowBinary(' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
grep -cF 'new SinkTaskStatistics(0)' src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
git diff --quiet pre-v1.7.0_gg8 HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/ScriptedWriteFailureWriter.java
git diff --quiet 83808df9a0993cf49b2ceaa4633c782e550c0180 HEAD -- <the three new upstream test paths>
```

```text
measured_head: 43676e9aaff31961c062217bede6116d1403ef84
tree_vs_m_outside_planning: identical
compile_java_exit: 0
compile_testfixtures_java_exit: 0
compile_integration_test_java_exit: 0
connect_json_resolved: yes
compile_test_java_exit: 1
compile_test_java_errors: 2
compile_test_java_error_files: ClickHouseWriterRecordContextTest.java x2
error_excerpt: ClickHouseWriterRecordContextTest.java:41: error: constructor SinkTaskStatistics in class SinkTaskStatistics cannot be applied to given types; (required: String,int; found: int)
break_prediction_met: yes
connect_json_sha1_local: d9a119423f72cdf12c32dd4aac958ec7dc35368f
connect_json_sha1_central: d9a119423f72cdf12c32dd4aac958ec7dc35368f
connect_json_sha1_matches_central: yes
connect_json_catalog_line: kafka-connect-json = { module = "org.apache.kafka:connect-json", version.ref = "kafka" }
writer_seam_count: 2
writer_rowbinary_loop_visibility: package-private
record_context_test_old_ctor_sites: 2
matrix_file_unchanged_since_tag: yes
upstream_tests_byte_identical_to_upstream: yes
```

`measured_head` is the record commit; the evidence commit after it touches only `.planning`, so the tree outside `.planning` is the tree of M. The three compile tasks of the combined run all pass; `compileTestJava` fails with exactly the predicted two javac errors, both `new SinkTaskStatistics(0)` against the fork's `(String connectorName, int taskId)` constructor (lines 41 and 63). `connect-json` 4.4.0 resolved from the network on the first build after M (no `Could not resolve` or `No cached version` line) and its SHA-1 equals Maven Central's. Nothing else is red on the compile side, so plan 45-02's repairs are scoped to the predicted set. Nothing in `src` changed in this task.

2026-11-04T20:10:50Z
Verdict: pass

### E-05: U-2 upstream-test edits, testUnit before and after, and the attributable full-suite red set

Backs: UPS-02, UPS-04, ROADMAP Phase 45 criterion 3 (U-2: upstream's two edited tests are kept green by named, minimal, test-only edits, each in its own commit, and the only red left on the merged tree is the one #857 causes).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava > /tmp/gsd45/exec/compile-fix.log 2>&1
git worktree add --detach /tmp/gsd45/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/fmt --console=plain spotlessApply > /tmp/gsd45/exec/fmt-1.log 2>&1
git commit -F /tmp/gsd45/exec/msg-a.txt
git diff --numstat 83808df9a0993cf49b2ceaa4633c782e550c0180 HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-after-compile-fix.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*FieldDescriptorEscapeTest*' > /tmp/gsd45/exec/fdt.log 2>&1
git worktree add --detach /tmp/gsd45/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/fmt --console=plain spotlessApply > /tmp/gsd45/exec/fmt-2.log 2>&1
git commit -F /tmp/gsd45/exec/msg-b.txt
git diff -U0 83808df9a0993cf49b2ceaa4633c782e550c0180 HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java | grep -cE '^-[^-]'
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-after-u2.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd45/exec/suite-after-u2.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/test --expect 'WriterErrorPathLiveTest#WRT-10: a record whose Array'
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
compile_four_tasks_exit: 0
compile_fix_commit: c3946803517bbeb4e807569cac797170ba3128d2
compile_fix_numstat: 2 2
fmt_record_context_test: preexisting
testunit_after_fix_exit: 1
testunit_after_fix_tests: 1837
testunit_after_fix_failed: FieldDescriptorEscapeTest#preservesEscapedCommentsAndDefaultExpressions
escape_edit_commit: 0f4e45bd698e59acb977daa0c3878f2afeab2800
escape_edit_removed_lines: 2
fmt_field_descriptor_escape_test: identical
testunit_exit: 0
testunit_tests: 1837
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
suite_exit: 1
suite_tests: 2440
suite_failures: 1
suite_errors: 0
suite_skipped: 18
suite_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite_failed: WriterErrorPathLiveTest#WRT-10: a record whose Array
suite_files: 141
suite_duration: 164s
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: first suite run crashed (OpenJ9 exit 255, partial XML: 2356 tests, 133 files); core dump and sibling files deleted; the single re-run recorded here is clean
```

Compile fix: the four compile tasks pass after the two-line edit; the numstat against upstream's blob is `2 2`, so the file differs from upstream only by its two constructor lines. `fmt_record_context_test` is `preexisting`: Spotless' `removeUnusedImports` would drop one line, the unused `java.io.IOException` import that upstream's own blob carries (the file at HEAD is byte-identical to upstream's blob apart from the two constructor lines), so it was left as merged to keep the diff to the two named lines. Escape edit: testUnit was red on exactly `FieldDescriptorEscapeTest#preservesEscapedCommentsAndDefaultExpressions` after the compile fix (1837 = 1826 baseline + 11 upstream tests), with the failure `expected: <'a\\b'> but was: <'a\\\\b'>` as predicted; the edit changed only the two expected values of `getDefaultExpression()` and `getComment()` to the literal JSON text (default expression with four backslashes; comment with `\\`, `\"`, `\n`, `\t` kept as backslash sequences) plus one comment line, the file's Spotless run is `identical`, and the class passed on the first run after the edit. The full suite on the repaired merge is baseline 2425 + 15 = 2440 tests with the 18 named skips and exactly one red testcase, WRT-10, so plan 45-03's repair is attributable. The first full-suite attempt hit the known intermittent OpenJ9 crash (exit 255, `Gradle Test Executor 33`, 8 GB core dump) and was discarded; its partial XML was not used.

2026-11-04T20:21:30Z
Verdict: pass

### E-06: #857 measured before re-expression (15 shapes, worktree probe)

Backs: UPS-02, ROADMAP Phase 45 criterion 2, first clause (what #857's record context and message carry is measured per writer failure shape, before the code is re-expressed; the probe exists only in a detached worktree that is removed).

```sh
git worktree add --detach /tmp/gsd45/probe-wt HEAD
git -C /tmp/gsd45/probe-wt rev-parse HEAD
cp /tmp/gsd45/probe-src/Probe857MeasurementTest.java /tmp/gsd45/probe-wt/src/test/java/com/clickhouse/kafka/connect/sink/db/Probe857MeasurementTest.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/probe-wt --console=plain testUnit --rerun --tests '*Probe857MeasurementTest*' > /tmp/gsd45/exec/probe857.log 2>&1
python3 -I <extract the PROBE857 lines from TEST-...Probe857MeasurementTest.xml into /tmp/gsd45/exec/probe857-lines.txt>
grep -c '<the probe sentinel text>' /tmp/gsd45/exec/probe857-lines.txt
git worktree remove --force /tmp/gsd45/probe-wt
git worktree prune
git worktree list
git log --all --format=%H -- '*Probe857MeasurementTest.java'
```

```text
probe_tree: 05183afc0ad639f6004e85fd5320c3142875c8e2
probe_shapes: 15
probe_run: 1 test, 0 failures, BUILD SUCCESSFUL
probe_lines_with_sentinel: 0
worktree_removed: yes
probe_class_committed: no
m857_new_layer_shapes: 14
m857_retriable_rethrown_same_instance: yes
m857_column_named_twice_shapes: 8
m857_sentinel_in_message_shapes: JACKSON_OVER_SOCKET_TIMEOUT
m857_sentinel_in_error_log_shapes: JACKSON_OVER_SOCKET_TIMEOUT
m857_sentinel_in_dlq_headers_shapes: JACKSON_OVER_SOCKET_TIMEOUT
baseline_sentinel_in_message_shapes: JACKSON_OVER_SOCKET_TIMEOUT
baseline_sentinel_in_dlq_headers_shapes: JACKSON_OVER_SOCKET_TIMEOUT
baseline_sentinel_in_error_log_shapes: none
m857_error_log_has_throwable_shapes: 14
m857_outcome_cells: 56
m857_outcome_changes_through_wrap: 0
```

The 15 probe lines, one per shape, as the probe printed them (the record-value stand-in is masked as `<SENTINEL>` and never printed; `after_message_head` is AFTER's message cut to 120 characters; `outcome_*` lists the four settings in the order errors.tolerance none/socket off, none/socket on, all/socket off, all/socket on; `after_is_new_layer` is yes when AFTER's cause has BEFORE's class and AFTER is not a RetriableException; BEFORE is a direct `doWriteCol` call, AFTER is `writeRecordsRowBinary`):

```probe
PROBE857 shape=INVALID_UUID before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: Failed to write column 
PROBE857 shape=UNPARSEABLE_INT128 before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_int128`: Failed to write colum
PROBE857 shape=TYPE_MISMATCH before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_u64`: Failed to write column `
PROBE857 shape=UNKNOWN_ENUM_LABEL before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_enum`: Failed to write column 
PROBE857 shape=NULL_IN_NON_NULLABLE before_class=java.lang.RuntimeException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_i32`: An attempt to write null
PROBE857 shape=NESTED_NULL_IN_TUPLE before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `t`: column 't' (Tuple(...)) tupl
PROBE857 shape=SCRIPTED_ARITHMETIC before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_int`: Failed to write column `
PROBE857 shape=IO_PLAIN before_class=java.io.IOException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=no sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: stream closed by test
PROBE857 shape=IO_READ_TIMEOUT_PREFIX before_class=java.io.IOException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=no sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE outcome_after=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: Read timed out after 30
PROBE857 shape=SOCKET_TIMEOUT before_class=java.net.SocketTimeoutException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=no sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE outcome_after=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: timed out
PROBE857 shape=SOCKET_EXCEPTION before_class=java.net.SocketException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=no sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RETRIABLE,RETURNS,RETRIABLE outcome_after=RUNTIME_WRAP,RETRIABLE,RETURNS,RETRIABLE same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: Connection reset
PROBE857 shape=UNKNOWN_HOST before_class=java.net.UnknownHostException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=no sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE outcome_after=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: clickhouse.invalid
PROBE857 shape=JACKSON_ALONE before_class=org.apache.kafka.connect.errors.DataException after_class=org.apache.kafka.connect.errors.DataException same_class=yes after_is_new_layer=yes column_named_twice=yes sentinel_before_message=no sentinel_after_message=no sentinel_in_error_log=no error_log_has_throwable=yes sentinel_in_dlq_headers_before=no sentinel_in_dlq_headers_after=no outcome_before=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_after=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: Failed to write column 
PROBE857 shape=JACKSON_OVER_SOCKET_TIMEOUT before_class=com.fasterxml.jackson.databind.JsonMappingException after_class=org.apache.kafka.connect.errors.DataException same_class=no after_is_new_layer=yes column_named_twice=no sentinel_before_message=yes sentinel_after_message=yes sentinel_in_error_log=yes error_log_has_throwable=yes sentinel_in_dlq_headers_before=yes sentinel_in_dlq_headers_after=yes outcome_before=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE outcome_after=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE same_instance=n/a sentinel_in_log_before=no after_message_head=Failed to serialize record for topic `probe-topic`, partition `3`, offset `42`, column `c_uuid`: reference chain: Map["<
PROBE857 shape=RETRIABLE_FROM_COLUMN_WRITER before_class=org.apache.kafka.connect.errors.RetriableException after_class=org.apache.kafka.connect.errors.RetriableException same_class=yes after_is_new_layer=no column_named_twice=n/a sentinel_before_message=n/a sentinel_after_message=n/a sentinel_in_error_log=n/a error_log_has_throwable=no sentinel_in_dlq_headers_before=n/a sentinel_in_dlq_headers_after=n/a outcome_before=n/a outcome_after=n/a same_instance=yes sentinel_in_log_before=n/a after_message_head=server is restarting
```

What #857 adds relative to the direct path is one `DataException` layer around 14 of the 15 shapes (its message is `Failed to serialize record for topic, partition, offset, column` followed by the cause's own message, which repeats the column name in 8 shapes), one ERROR log line per failure that carries the message and the full cause chain, and an unchanged pass-through for the retriable shape; none of the 56 outcome cells (14 shapes by 4 tolerance and socket settings, through the `RuntimeException(queryId)` wrap that Processing applies) changes, and the record-value stand-in reaches the message, the printed trace and the DLQ headers only for JACKSON_OVER_SOCKET_TIMEOUT, where it is already present before #857 in the pass-through Jackson exception (the documented residual of FORK-DELTA Known costs item 6, which plan 45-03 Task 1 closes per plan-check answer 1), while the one surface #857 adds for that shape is the ERROR log line (`sentinel_in_log_before=no`, `sentinel_in_error_log=yes`). Every research prediction held (14 new-layer shapes, the retriable one rethrown as the same instance, zero outcome changes); the thirteen other shapes carry no value on any surface because the fork's own `doWriteColValue` messages are value-free, so what #857 adds for them is the extra coordinates and the repeated column, not a value.

2026-11-04T20:22:00Z
Verdict: pass

### E-07: Value-free rebuild of every Jackson chain: red on the unchanged writer, green in one commit, matrix unchanged

Backs: UPS-02, ROADMAP Phase 45 criterion 2 (plan-check answer 1 and the iteration-2 answer: every value-carrying Jackson chain `doWriteColValue` would pass through is rebuilt value-free while the links and root facts retry classification reads stay).

```sh
git diff --quiet 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- src/main
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*JacksonOverIoValueFreeUnitTest*' --tests '*WriterValueFreeFailureUnitTest*' --tests '*WriterFailureClassificationUnitTest*' > /tmp/gsd45/exec/jx-redfirst.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*JacksonOverIoValueFreeUnitTest*' --tests '*WriterValueFreeFailureUnitTest*' --tests '*WriterFailureClassificationUnitTest*' > /tmp/gsd45/exec/jx-green.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class JacksonOverIoValueFreeUnitTest
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class WriterValueFreeFailureUnitTest
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class WriterFailureClassificationUnitTest
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*WithheldMessageExceptionUnitTest*' --tests '*WriterLogMaskingUnitTest*' --tests '*ClickHouseWriterColumnContextTest*' --tests '*SourcePlanningIdGuardUnitTest*' --tests '*UtilsTest*' --tests '*UtilsNullMessageClassificationUnitTest*' --tests '*AdditionalRetriableErrorCodesTest*' > /tmp/gsd45/exec/jx-gg3-unit.log 2>&1
grep -cF '!(t instanceof JacksonException)' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
grep -cF 'withheldJacksonChain(' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git worktree add --detach /tmp/gsd45/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/fmt --console=plain spotlessApply > /tmp/gsd45/exec/fmt-jx.log 2>&1
git commit -F /tmp/gsd45/exec/msg-jx.txt
```

```text
jx_redfirst_head: clean
jx_redfirst_failed_count: 41
jx_redfirst_red_checks: carriesNoRecordValue; rebuildIsValueFreeAndKeepsTheSuffix
jx_redfirst_red_shapes: 19
jx_redfirst_wvf_red: WriterValueFreeFailureUnitTest#WRT-17 value-free: a Jackson exception over a SocketTimeoutException becomes a value-free IOException over the same SocketTimeoutException; WriterValueFreeFailureUnitTest#WRT-17 value-free: a Jackson exception over a plain IOException becomes a value-free IOException over the same IOException; WriterValueFreeFailureUnitTest#WRT-17 value-free: a plain IOException over a Jackson exception becomes a value-free IOException over a withheld IOException root
jx_redfirst_nonchange_green: yes
jx_redfirst_matrix_failed: 0
jx_green_failed_count: 0
jx_tests: 95
wvf_tests: 29
matrix_tests: 216
jx_gg3_unit_failed_count: 0
wrt17_anchor_count: 1
anchors_mirror: rows=191 bad=0
fmt_writer_jx: preexisting
fmt_utils: identical
fmt_jx: identical
fmt_wvf: identical
v1_constructor_null_node: accepted
jx_commit: f13d83781efd97589d8aec292c39faca4face820
```

Red-first on the unchanged writer (HEAD equals M outside `.planning`): 340 tests, 41 failures. In `JacksonOverIoValueFreeUnitTest` (95 cases) exactly the two value checks of every one of the 19 chains were red (`rebuildIsValueFreeAndKeepsTheSuffix` and `carriesNoRecordValue`, 38 cases); `rawChainCarriesTheValue`, `keepsTheClassifiedLinks` and `keepsTheRetryOutcome` held on all 19 chains because the unchanged writer throws the scripted chain itself (their bites are mutations M4-M6 in E-09). The three edited `WriterValueFreeFailureUnitTest` methods were the only red ones of that class, and the 216-case matrix was green. After the change every one of the 340 tests passes, the other gg3 pins (91 tests over 7 classes) pass, and the 19 shapes include the iteration-2 shapes (a) OVER_RETRIABLE_OVER_PLAIN_IO, (b) PLAIN_IO_OVER_JACKSON_ROOT (also timeout-prefixed and the exact RuntimeException), (c) OVER_CONNECT_OVER_SOCKET_TIMEOUT, the four between-link chains and a value-carrying non-IO link below a Jackson link. The client V1 `ClickHouseException(int, Throwable, ClickHouseNode)` constructor accepts a `null` node, so no node was needed for the shape or the stand-in. The stand-in text for a link is its class name plus the withheld note held as one constant in the writer (a second `WithheldMessageException.of(` call would have moved the existing WRT-13 anchor row, whose count is 1). `fmt_writer_jx` is `preexisting`: Spotless would only move the static import and the `org.apache.kafka.connect` import block of `ClickHouseWriter.java`, which this commit does not touch. Nothing was pushed.

2026-11-04T20:31:30Z
Verdict: pass

### E-08: Option A re-expression: pins red on the #857 tree, then green in one commit with four anchor rows

Backs: UPS-02, ROADMAP Phase 45 criterion 2 (U-1 Option A with the log line of plan-check answer 3: the record loop rethrows the fork's own exception and logs the record's coordinates, the column and the exception class, never a message or a value; upstream's three coordinate assertions move to the log line, U-2 extended).

```sh
git diff --name-only 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- src/main
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*WriterRecordContextUnitTest*' --tests '*WriterRecordContextDlqUnitTest*' --tests '*ClickHouseWriterRecordContextTest*' > /tmp/gsd45/exec/redfirst.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*WriterRecordContextUnitTest*' --tests '*WriterRecordContextDlqUnitTest*' --tests '*ClickHouseWriterRecordContextTest*' > /tmp/gsd45/exec/green.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class WriterRecordContextUnitTest
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class WriterRecordContextDlqUnitTest
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class ClickHouseWriterRecordContextTest
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*WriterFailureClassificationUnitTest*' --tests '*WriterValueFreeFailureUnitTest*' --tests '*JacksonOverIoValueFreeUnitTest*' --tests '*WithheldMessageExceptionUnitTest*' --tests '*WriterLogMaskingUnitTest*' --tests '*ClickHouseWriterColumnContextTest*' --tests '*SourcePlanningIdGuardUnitTest*' --tests '*ConnectApiReferencesUnitTest*' > /tmp/gsd45/exec/gg3-unit.log 2>&1
grep -cF -e 'writeColumnWithRecordContext(' -e 'RECORD_CONTEXT_FAILURE_LOG_FORMAT' -e 'serializationFailure(' -e 'Failed to serialize record for' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git diff -U0 83808df9a0993cf49b2ceaa4633c782e550c0180 HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
git worktree add --detach /tmp/gsd45/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/fmt --console=plain spotlessApply > /tmp/gsd45/exec/fmt-fix.log 2>&1
git commit -F /tmp/gsd45/exec/msg-fix.txt
```

```text
redfirst_head: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java
redfirst_failed_count: 32
redfirst_failed_checks: logsOneValueFreeCoordinateLine; rethrowsWhatDoWriteColThrew; dlqHeadersAndLogsCarryNoRecordValue; thrownChainAndLogsCarryNoRecordValue
redfirst_rethrow_red_shapes: 14
redfirst_coordinate_red_shapes: 14
redfirst_dlq_pin_red_cases: 4
redfirst_value_free_red_shapes: none
redfirst_retriable_shape_green: yes
redfirst_record_context_test_green: yes
dlq_pin_shapes: INVALID_UUID; UNPARSEABLE_INT128
dlq_pin_shape_replaced: no
green_failed_count: 0
p1_tests: 59
p2_tests: 4
rct_tests: 2
rct_bound: bounded
rct_approved_removed_lines: 5
gg3_unit_tests: 391
gg3_unit_failed_count: 0
matrix_tests: 216
anchor_counts: 2; 2; 0; 0
anchors_mirror: rows=195 bad=0
fmt_writer: preexisting
fmt_p1: identical
fmt_p2: identical
fmt_rct: preexisting
fix_commit: f4b85584a112062d8872aa31e6806cae6b6f55d0
```

Red-first on the #857 tree with Task 1's rebuild (`src/main` differs from M only by the writer and Utils): 65 tests, 32 red. `rethrowsWhatDoWriteColThrew` and `logsOneValueFreeCoordinateLine` were red for exactly the 14 shapes that E-06 `m857_new_layer_shapes` counts, and green for RETRIABLE_FROM_COLUMN_WRITER; the four production-path cases of `WriterRecordContextDlqUnitTest` were red on `exactly one coordinate line ... expected 1 but was 0` (#857's line does not start with the coordinate prefix), after their DLQ-header, thrown-chain and exception-text checks had already passed. `carriesNoRecordValue` and `keepsTheRetryOutcome` (non-change checks, bitten by M1 and M2 in E-09) were green for all 15 and 14 shapes, so no value-free check was red there, and upstream's `ClickHouseWriterRecordContextTest` (both methods) was green on the #857 tree. `WriterRecordContextDlqUnitTest` runs INVALID_UUID and UNPARSEABLE_INT128 (schema validation sits behind `doInsertRawBinary`, which `CapturingClickHouseWriter` replaces, so no shape needed substituting). After the change all 65 tests and the 391 gg3/matrix tests are green; the matrix file is unchanged since the tag. The diff of upstream's test against `83808df9` removes 13 lines, 5 approved (the two constructor lines and the three coordinate assertions) and 8 that reappear re-indented. `fmt_writer` and `fmt_rct` are `preexisting`: Spotless would only move the writer's static import and the Connect import block, and drop upstream's unused `java.io.IOException` import, none of which this commit touches. Nothing was pushed.

2026-11-04T20:36:00Z
Verdict: pass

### E-09: Plan 45-03 bite and gate: six mutations, testUnit, live WRT-10, unchanged pins, tail gate

Backs: UPS-02, ROADMAP Phase 45 criterion 2 (the non-change checks bite under six mutations applied only in a removed worktree; the cache-busted testUnit and the live gg3 pins pass; the gg3 pin files are unchanged except the three retired methods; the unedited tail gate passes with `anchor_rows=195`).

```sh
git worktree add --detach /tmp/gsd45/mut HEAD
python3 -I /tmp/gsd45/mutate.py /tmp/gsd45/mut/src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java <old> <new>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/mut --console=plain testUnit --rerun --tests '<class filter>' > /tmp/gsd45/exec/mut-<n>.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed /tmp/gsd45/mut/build/test-results/testUnit
git worktree remove --force /tmp/gsd45/mut
git worktree prune
git worktree list
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-ups02.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*WriterErrorPathLiveTest*' --tests '*JsonAsStringKeyResidualLiveTest*' --tests '*WriterFailureClassificationUnitTest*' > /tmp/gsd45/exec/live-gg3.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/test
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
git diff --quiet pre-v1.7.0_gg8 HEAD -- <the seven gg3 pin files>
git diff --name-only 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- src/main
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 d052b6b8a3f51a0e9518a13e6c6aa2f267be6b52 > /tmp/gsd45/exec/tail-gate-03.log 2>&1
```

```text
m1_target: in writeColumnWithRecordContext, the log call's `name,` argument followed by `e.getClass().getName());` (the column argument)
m1_failed: 18
m1_expected_met: yes
m2_target: in writeColumnWithRecordContext, the `throw e;` after the LOGGER.error(RECORD_CONTEXT_FAILURE_LOG_FORMAT, ...) call, replaced by `throw new RetriableException(e);`
m2_failed_count: 38
m2_expected_met: yes
m3_target: withheldJacksonChain(e, buildValueConversionFailureMessage(col, columnType, value, e)) replaced by null
m3_failed_count: 42
m3_expected_met: yes
m4_target: below = chain.get(suffixStart); replaced by below = WithheldMessageException.of(chain.get(suffixStart));
m4_failed_count: 40
m4_expected_met: yes
m5_target: standIn = new RetriableException(text, cause); replaced by standIn = new RuntimeException(text, cause); (the one RetriableException stand-in, in sameKindStandIn)
m5_failed: 4
m5_expected_met: yes
m6_target: IOException standIn = new IOException(prefix + withheldText(root)); replaced by IOException standIn = new IOException(withheldText(root));
m6_failed: 3
m6_expected_met: yes
worktrees_removed: yes
testunit_exit: 0
testunit_tests: 1995
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
live_exit: 0
live_failed_count: 0
wrt10_passed: yes
live_matrix_tests: 216
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
gg3_pins_unchanged_since_tag: yes
wvf_edit_scope: three retired methods
utils_edit_scope: visibility only
main_diff_since_merge: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java
anchors_mirror: rows=195 bad=0
tail_gate_head: d052b6b8a3f51a0e9518a13e6c6aa2f267be6b52
tail_gate_line: invariants: provenance_paths=11 anchor_commits=15 anchor_rows=195 marker_commits=33 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

Mutation red sets (each in its own detached worktree, removed afterwards; the main tree was never touched). M1 (a value in the coordinate line): 18 red in `WriterRecordContextUnitTest`, all `logsOneValueFreeCoordinateLine` (14) or `carriesNoRecordValue` (INVALID_UUID, UNPARSEABLE_INT128, TYPE_MISMATCH, UNKNOWN_ENUM_LABEL), so the value check bites. M2 (rethrow as RetriableException): `keepsTheRetryOutcome` red for the nine NON_RETRIABLE shapes and SOCKET_EXCEPTION, `rethrowsWhatDoWriteColThrew` red for 14. M3 (rebuild call replaced by null): `carriesNoRecordValue` and `rebuildIsValueFreeAndKeepsTheSuffix` red for all 19 chains, the three renamed `WriterValueFreeFailureUnitTest` methods red, and `WriterRecordContextUnitTest#carriesNoRecordValue JACKSON_OVER_SOCKET_TIMEOUT` red. M4 (kept suffix replaced by a withheld copy): 9 `keepsTheClassifiedLinks`, 9 `rebuildIsValueFreeAndKeepsTheSuffix` and 6 `keepsTheRetryOutcome` cases red including OVER_SOCKET_TIMEOUT in the last two, and 16 matrix cases of `WriterFailureClassificationUnitTest` red, 8 of them named JACKSON_OVER_SOCKET_TIMEOUT, so the 216-case matrix watches the rebuild. M5 (RetriableException stand-in made a RuntimeException): `keepsTheClassifiedLinks` and `keepsTheRetryOutcome` red for RETRIABLE_OVER_JACKSON_ROOT and OVER_RETRIABLE_OVER_PLAIN_IO. M6 (root stand-in without the timeout prefix): `keepsTheRetryOutcome`, `keepsTheClassifiedLinks` and `rebuildIsValueFreeAndKeepsTheSuffix` red for PLAIN_IO_OVER_TIMEOUT_PREFIXED_JACKSON_ROOT only. The cache-busted testUnit is 1837 (E-05) + 95 + 59 + 4 = 1995 tests with the 2 skips. The live run on ClickHouse `26.10.3.7` x client V2 passes `WriterErrorPathLiveTest` (10 tests, WRT-10 included), `JsonAsStringKeyResidualLiveTest` (4) and the 216-case matrix, and no crash dump was left in the root. The m1 and m2 targets differ from the plan's one-line text only in the line breaks of the formatted call; each was replaced exactly once. Nothing was pushed.

2026-11-04T20:41:00Z
Verdict: pass

### E-10: JSON insert route measured before the guard (worktree probe)

Backs: UPS-02, ROADMAP Phase 45 criterion 2 (the JSON insert route's failure shapes are measured on the real tree before the guard: which are reachable, which surfaces a record value reaches, and the retry outcome as thrown and as client V2 wraps it; the probe exists only in a detached worktree that is removed).

```sh
git worktree add --detach /tmp/gsd45/probe-json HEAD
git -C /tmp/gsd45/probe-json rev-parse HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/probe-json --console=plain testUnit --rerun --tests '*ProbeJsonRouteMeasurementTest*' > /tmp/gsd45/exec/probe-json.log 2>&1
python3 -I <extract the PROBEJSON lines from TEST-...ProbeJsonRouteMeasurementTest.xml into /tmp/gsd45/exec/probe-json-lines.txt>
grep -c '<the probe sentinel text>' /tmp/gsd45/exec/probe-json-lines.txt
git worktree remove --force /tmp/gsd45/probe-json
git worktree prune
git worktree list
git log --all --format=%H -- '*ProbeJsonRouteMeasurementTest.java'
```

```text
json_probe_tree: fb5fa4cbd758b32f5e14cdc4c3bb8cb81d379325
json_probe_shapes: 8
json_probe_lines: 9
json_probe_run: 1 test, 0 failures, BUILD SUCCESSFUL
json_probe_lines_with_sentinel: 0
json_reachable_shapes: 8
json_unreachable: none
json_pinned_shapes: JSON_SCRIPTED_RUNTIME; JSON_KEY_IN_REFERENCE_CHAIN; JSON_SCRIPTED_JACKSON_ALONE; JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT; JSON_NO_JACKSON_NULL_DATA_CONTROL; JSON_SCHEMA_BYPASS_KEY; JSON_METADATA_FREE_STRUCT_VALUE; JSON_METADATA_FREE_CDC_VALUE
json_value_in_message_shapes: JSON_SCRIPTED_RUNTIME; JSON_KEY_IN_REFERENCE_CHAIN; JSON_SCRIPTED_JACKSON_ALONE; JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT; JSON_SCHEMA_BYPASS_KEY; JSON_METADATA_FREE_STRUCT_VALUE; JSON_METADATA_FREE_CDC_VALUE
json_value_in_dlq_headers_shapes: JSON_SCRIPTED_RUNTIME; JSON_KEY_IN_REFERENCE_CHAIN; JSON_SCRIPTED_JACKSON_ALONE; JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT; JSON_SCHEMA_BYPASS_KEY; JSON_METADATA_FREE_STRUCT_VALUE; JSON_METADATA_FREE_CDC_VALUE
json_value_in_logs_shapes: none
json_jackson_shapes: JSON_SCRIPTED_RUNTIME; JSON_KEY_IN_REFERENCE_CHAIN; JSON_SCRIPTED_JACKSON_ALONE; JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT; JSON_SCHEMA_BYPASS_KEY; JSON_METADATA_FREE_STRUCT_VALUE; JSON_METADATA_FREE_CDC_VALUE
json_control_unchanged: yes
json_io_timeout_wrapped: yes
json_retriable_shapes: JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT
json_outcome_v1_equals_v2: yes
worktree_removed: yes
json_probe_class_committed: no
```

The first run of this probe, on the plan's original shape table, found two plan assumptions false, and the table was amended in its own commit before this entry (`fb5fa4cb`). First, `ObjectMapper.writeValueAsBytes` turns every non-Jackson IOException into a `JsonMappingException` with no cause (message `Unexpected IOException (of type <class>): <text>`) on all four routes, so a plain IOException cannot leave the serialization statements and an unchanged retriable IOException control cannot exist; the control is a Jackson-free `NullPointerException` (a null `Data` in a DEBEZIUM_CDC `bypassRowBinary` record, thrown by the writer's own `v.getObject()`), and the scripted IO timeout is the ninth line, kept as a measured fact (`json_io_timeout_wrapped`): Jackson wraps it, its chain holds no IOException, its outcome is not retriable, and the guard does not try to change that. Second, the metadata-free serializers write map keys themselves, so a key never reached a reference chain there (message `boom`, no value); their shapes now put the value in the scripted exception's message. Every shape drives `CapturingClickHouseWriter.doInsert(records, queryId, null)` (the real `writeRecordsJson`), the sentinel is masked as `<SENTINEL>` and never printed, and `outcome_*` lists the four settings in the order errors.tolerance none/socket off, none/socket on, all/socket off, all/socket on through the `RuntimeException(queryId)` wrap Processing applies (the V2 column after the client-V2 wrap of a RuntimeException).

```probejson
PROBEJSON shape=JSON_SCRIPTED_RUNTIME reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=<SENTINEL> (through reference chain: java.util.HashMap["payload"]->java.util.HashMap["k"])
PROBEJSON shape=JSON_KEY_IN_REFERENCE_CHAIN reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=boom (through reference chain: java.util.HashMap["payload"]->java.util.HashMap["<SENTINEL>"])
PROBEJSON shape=JSON_SCRIPTED_JACKSON_ALONE reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=<SENTINEL> (through reference chain: java.util.HashMap["payload"]->java.util.HashMap["k"])
PROBEJSON shape=JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE outcome_v2=RETRIABLE,RETRIABLE,RETRIABLE,RETRIABLE message_head=<SENTINEL> (through reference chain: java.util.HashMap["payload"]->java.util.HashMap["k"])
PROBEJSON shape=JSON_NO_JACKSON_NULL_DATA_CONTROL reachable=yes thrown_class=java.lang.NullPointerException jackson_link=no sentinel_in_message=no sentinel_in_stack=no sentinel_in_dlq_headers=no sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=Cannot invoke "com.clickhouse.kafka.connect.sink.data.Data.getObject()" because "v" is null
PROBEJSON shape=JSON_SCHEMA_BYPASS_KEY reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=boom (through reference chain: java.util.HashMap["payload"]->java.util.HashMap["<SENTINEL>"])
PROBEJSON shape=JSON_METADATA_FREE_STRUCT_VALUE reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=<SENTINEL>
PROBEJSON shape=JSON_METADATA_FREE_CDC_VALUE reachable=yes thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=yes sentinel_in_stack=yes sentinel_in_dlq_headers=yes sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=<SENTINEL>
PROBEJSON shape=JSON_SCRIPTED_IO_TIMEOUT reachable=no thrown_class=com.fasterxml.jackson.databind.JsonMappingException jackson_link=yes sentinel_in_message=no sentinel_in_stack=no sentinel_in_dlq_headers=no sentinel_in_logs=no outcome_v1=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS outcome_v2=RUNTIME_WRAP,RUNTIME_WRAP,RETURNS,RETURNS message_head=Unexpected IOException (of type java.io.IOException): Read timed out after 30000 ms
```

Today the record value reaches the thrown message, the rendered stack trace and the Connect DLQ headers for seven of the eight shapes (every Jackson-wrapped value-carrying failure on the schemaless, bypass and both metadata-free routes) and no log line, while the null-Data control and the IO-timeout fact carry none; the only retriable shape is the Jackson-over-socket-timeout one, whose outcome the guard must keep.

2026-11-04T20:50:10Z
Verdict: pass

### E-11: JSON-route guard: pins red on the unguarded tree, green in one commit, success-path bytes unchanged

Backs: UPS-02, ROADMAP Phase 45 criterion 2 (the JSON insert route: a serialization failure leaves `writeRecordsJson` value-free through the shared rebuild, on the failure path only; the retry outcome is the serializer's own; a successful batch sends the same bytes).

```sh
git diff --name-only 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- src/main
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*JsonRouteValueFreeUnitTest*' --tests '*JsonRouteValueFreeDlqUnitTest*' > /tmp/gsd45/exec/json-redfirst.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*JsonRouteValueFreeUnitTest*' --tests '*JsonRouteValueFreeDlqUnitTest*' > /tmp/gsd45/exec/json-green.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class JsonRouteValueFreeUnitTest
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class JsonRouteValueFreeDlqUnitTest
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*JacksonOverIoValueFreeUnitTest*' --tests '*WriterValueFreeFailureUnitTest*' --tests '*WriterRecordContextUnitTest*' --tests '*WriterFailureClassificationUnitTest*' --tests '*MetadataFreeJsonInsertUnitTest*' --tests '*JsonSerializationEquivalenceTest*' --tests '*MetadataFreeJsonWriterTest*' --tests '*EmptyCdcSkipUnitTest*' --tests '*JsonInsertColumnsUnitTest*' --tests '*SourcePlanningIdGuardUnitTest*' > /tmp/gsd45/exec/json-neighbours.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit --class WriterFailureClassificationUnitTest
grep -cF 'withheldJacksonChain(' src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 197
bash /tmp/gsd45/fmtcheck.sh fmt-json.log <the writer and the two pin files>
git commit -F /tmp/gsd45/exec/msg-json.txt
```

```text
json_redfirst_head: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java
json_redfirst_tests: 46
json_redfirst_failed_count: 18
json_redfirst_red_checks: carriesNoRecordValue; isValueFreeAndKeepsTheTrustedSuffix; dlqHeadersAndLogsCarryNoRecordValue; thrownChainAndLogsCarryNoRecordValue
json_redfirst_value_red_shapes: 7
json_redfirst_dlq_red: yes
json_redfirst_control_green: yes
json_redfirst_bytes_green: yes
json_redfirst_nonchange_green: yes
json_green_failed_count: 0
json_unit_tests: 42
json_dlq_tests: 4
json_pin_tests: 46
json_neighbours_failed_count: 0
json_neighbours_tests: 529
matrix_tests: 216
rebuild_anchor_count: 2
anchors_mirror: rows=197 bad=0
fmt_writer_json: preexisting
fmt_jr: identical
fmt_jd: identical
json_commit: 0c8be1daedb04a77240191cbf76a857fa15a4a82
```

Red-first on the unguarded tree (`src/main` differs from the merge commit only by plan 45-03's two files): 46 tests, 18 red. `carriesNoRecordValue` and `isValueFreeAndKeepsTheTrustedSuffix` are red for exactly the seven value-carrying shapes (every shape except the Jackson-free null-Data control), with `no Jackson link may be left in the thrown chain` and `no message of the thrown chain may carry the value` as the failures; the four production-path cases of `JsonRouteValueFreeDlqUnitTest` (two shapes by errors.tolerance all and none) are red on the sentinel in the DLQ stack trace and in the thrown chain. The control's five checks, `rawFailureCarriesTheValue` on every shape, both success-path byte tests and the non-change checks (`keepsTheClassifiedLinks`, `keepsTheRetryOutcome`) are green there: they hold because THROWN is the serializer's own exception, so their bite is mutation J3. After the change all 46 pass (42 + 4 = 5 x 8 shapes + 2 + 4), the 529 neighbouring tests (the other Jackson, writer and JSON-route pins, with the 216-case matrix) pass, `withheldJacksonChain(` occurs twice (declaration and the shared entry point) and the mirror reads `rows=197 bad=0`. Two details of the final pins differ from the first draft that was run red: a lone Jackson exception rebuilds to the value-free top alone (the test now asserts that single link, as the rebuild produces no separate root), and the message names the table with `getCleanName()` because the format already wraps it in backticks (`getName()` would print them twice); both only affect assertions that were already failing for the unguarded tree. `fmt_writer_json` is `preexisting`: Spotless would only move the static import and the Connect import block of `ClickHouseWriter.java`, which this commit does not touch. Nothing was pushed.

2026-11-04T20:54:19Z
Verdict: pass

### E-12: JSON-route bite and gate: three mutations, testUnit, live JSON run, anchors, tail gate

Backs: UPS-02, ROADMAP Phase 45 criterion 2 (the JSON-route checks bite under three mutations applied only in a removed worktree; the cache-busted testUnit and a targeted live JSON run on latest x V2 pass; the anchors stay at 197 rows; the unedited tail gate passes with `anchor_rows=197`).

```sh
git worktree add --detach /tmp/gsd45/mut HEAD
python3 -I /tmp/gsd45/mutate.py /tmp/gsd45/mut/src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java <old> <new>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/mut --console=plain testUnit --rerun --tests '<class filter>' > /tmp/gsd45/exec/mut-<n>.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed /tmp/gsd45/mut/build/test-results/testUnit
git worktree remove --force /tmp/gsd45/mut
git worktree prune
git worktree list
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-json.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*ClickHouseSinkTaskSchemalessTest*' --tests '*JsonInsertColumnsLiveTest*' --tests '*ExplicitJsonInsertPathLiveTest*' --tests '*ClickHouseSinkTaskJsonInsertFormatTest*' --tests '*JsonAsStringKeyResidualLiveTest*' > /tmp/gsd45/exec/live-json.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/test
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git diff --name-only 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- src/main
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 79eb0b95449ef188b9f0957313add68348787553 > /tmp/gsd45/exec/tail-gate-04.log 2>&1
```

```text
j1_target: in writeRecordsJson's catch, the throwRebuiltJacksonChain(e, String.format(JSON_ROW_FAILURE_FORMAT, table.getCleanName(), e.getClass().getName())) statement removed, so the catch only rethrows e
j1_failed_count: 18
j1_expected_met: yes
j2_target: in the same catch, `table.getCleanName(), e.getClass().getName()` replaced by `table.getCleanName(), e.getMessage()`
j2_failed: 14
j2_expected_met: yes
j3_target: in withheldJacksonChain, below = chain.get(suffixStart); replaced by below = WithheldMessageException.of(chain.get(suffixStart));
j3_failed: 3
j3_expected_met: yes
worktrees_removed: yes
testunit_exit: 0
testunit_tests: 2041
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
live_json_exit: 0
live_json_failed_count: 0
live_json_tests: 31
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
anchors_mirror: rows=197 bad=0
main_diff_since_merge: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java
tail_gate_head: 79eb0b95449ef188b9f0957313add68348787553
tail_gate_line: invariants: provenance_paths=11 anchor_commits=21 anchor_rows=197 marker_commits=39 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

Mutation red sets (each in its own detached worktree, removed afterwards; the main tree was never touched). J1 (guard off): 18 of the 46 pins red, `carriesNoRecordValue` and `isValueFreeAndKeepsTheTrustedSuffix` for all seven value-carrying shapes and all four production-path cases of `JsonRouteValueFreeDlqUnitTest`. J2 (the top text carries the exception message): 14 red, the same two checks for the seven value-carrying shapes including JSON_SCRIPTED_JACKSON_ALONE, so the value-free top text is watched. J3 (kept trusted suffix replaced by a withheld copy): 3 red, all for JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT: `isValueFreeAndKeepsTheTrustedSuffix`, `keepsTheClassifiedLinks` and `keepsTheRetryOutcome`, so the retriable classification on the JSON route is pinned. The cache-busted testUnit is 1995 (E-09) + 46 = 2041 tests with the 2 skips. The targeted live run on ClickHouse `26.10.3.7` x client V2 passes the five JSON-route classes (31 tests), which read back what they insert, so the bytes sent are unchanged on a real server. The plan's J1 and J2 targets name `table.getName()`; the code uses `table.getCleanName()` (see E-11), and the recorded targets are the exact replaced text. Nothing was pushed.

2026-11-04T20:57:37Z
Verdict: pass

### E-13: fromJsonRow difference pinned and biting; slash column and NAME pins live on latest x V2; NAME pins unchanged

Backs: UPS-03, UPS-04, ROADMAP Phase 45 criterion 3 (the one behaviour where the fork's `fromJsonRow` differs from upstream #839 is pinned Docker-free and bites against upstream's `readValue`; upstream's `SlashColumnRoundTripTest` and the live NAME pins pass on ClickHouse latest x client V2; the NAME pins are byte-unchanged since the tag).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*FieldDescriptorLiteralFieldsUnitTest*' > /tmp/gsd45/exec/lf.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
git worktree add --detach /tmp/gsd45/mut HEAD
cp <the new test> /tmp/gsd45/mut/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/
python3 -I /tmp/gsd45/mutate.py /tmp/gsd45/mut/<FD> 'try (JsonParser parser = OBJECT_MAPPER.createParser(json)) {' 'if (true) { return OBJECT_MAPPER.readValue(json, ClickHouseFieldDescriptor.class); } try (...'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd45/mut --console=plain testUnit --rerun --tests '*FieldDescriptorLiteralFieldsUnitTest*' > /tmp/gsd45/exec/lf-mut.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit /tmp/gsd45/mut/build/test-results/testUnit
git worktree remove --force /tmp/gsd45/mut
git worktree prune
git worktree list
bash /tmp/gsd45/fmtcheck.sh fmt-lf.log <the new test>
git commit -F /tmp/gsd45/exec/msg-lf.txt
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*SlashColumnRoundTripTest*' --tests '*RawIdentifierNameLiveTest*' --tests '*RawNameMatrixLiveTest*' --tests '*NullableTupleMatrixLiveTest*' > /tmp/gsd45/exec/live-names.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/test --class <each of the four>
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
grep -o 'NAME-0[0-9]' docs/FORK-DELTA.md | sort -u
git ls-files '*/<Class>.java'
git diff --quiet pre-v1.7.0_gg8 HEAD -- <the tracked files of the NAME pin classes>
git diff --quiet 83808df9a0993cf49b2ceaa4633c782e550c0180 HEAD -- src/test/java/com/clickhouse/kafka/connect/sink/db/SlashColumnRoundTripTest.java
```

```text
lf_tests: 31
lf_failures: 0
mutation_target: first statement of fromJsonRow in the worktree copy of ClickHouseFieldDescriptor.java: if (true) { return OBJECT_MAPPER.readValue(json, ClickHouseFieldDescriptor.class); } (upstream's body; the anchor text occurred once)
mutation_failures: 30
mutation_null_case_green: yes
worktree_removed: yes
fmt_lf: identical
lf_commit: b41f913c893c459fa200c8a7221b7d21ca20501b
live_exit: 0
live_failed_count: 0
live_tests: 135
slash_tests: 4
slash_failures: 0
raw_identifier_live_tests: 73
raw_name_matrix_live_tests: 25
nullable_tuple_matrix_live_tests: 33
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
name_ids_found: NAME-01; NAME-02; NAME-03; NAME-04; NAME-05; NAME-06
name_pin_classes: ClickHouseHelperClientForkLiveTest; ClickHouseHelperClientForkUnitTest; ColumnTest; DeclaredNullabilityTest; JsonDynamicRoundTripTest; NestedNullRowBinaryLiveTest; NestedNullableCompositeWriteTest; NullableTupleMatrixLiveTest; RawIdentifierNameLiveTest; RawNameMatrixDriftLiveTest; RawNameMatrixFixturesUnitTest; RawNameMatrixLiveTest; SimpleAggregateTupleRoundTripLiveTest; TableTest
name_pins_unchanged_since_tag: yes
slash_test_identical_to_upstream: yes
jvm_crash: none
```

The new class holds 31 cases: five fields (`type`, `comment`, `default_expression`, `codec_expression`, `ttl_expression`) times six escape samples (escaped slash, backslash, quote, newline, tab, unicode with slash), plus one JSON-null case. Against upstream's decode-all `readValue` in a detached worktree exactly the 30 field cases go red (every sample holds an escape, so every field is decoded away from its literal text) and the null case stays green, so the pin watches the difference and not the shared behaviour. `SlashColumnRoundTripTest` (4 cases: V1 and V2 for each of its two methods), byte-identical to upstream's blob, passes through DESCRIBE, insert and typed read-back, with the three live NAME pin classes in the same run (135 tests, no failure). The NAME pin classes were read from the "Pinned by" cells of the four FORK-DELTA rows that cite a NAME-0N ID (`RawNameMatrix` is a helper, not a test class, and is not in the list); none of their 14 files differs from `pre-v1.7.0_gg8`. `FieldDescriptorEscapeTest` is not in this list: it is an upstream test edited in plan 45-02 and is named in the Test-harness row written by Task 3. Nothing was pushed; the worktree was removed.

2026-11-04T21:03:00Z
Verdict: pass

### E-14: UPS-05 resolution facts, three anchor rows and the tail gate

Backs: UPS-05, ROADMAP Phase 45 criterion 4 (resolution and anchor clauses: the upstream coordinates resolve where they should, connect-json stays out of the shipped set, the anchor count changes in the commit that adds its rows, the tail gate passes).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration integrationTestRuntimeClasspath --dependency slf4j-simple
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration testRuntimeClasspath --dependency commons-lang3
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration testFixturesRuntimeClasspath --dependency commons-lang3
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration runtimeClasspath --dependency commons-lang3
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain buildEnvironment | grep -i spotless
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration testRuntimeClasspath --dependency connect-json
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration runtimeClasspath --dependency connect-json
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration clickhouseDependencies --dependency connect-json
git show pre-v1.7.0_gg8:gradle/libs.versions.toml | grep -cxF -e '<each of the three lines>'
git diff 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10^1 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 -- gradle/libs.versions.toml
grep -cF -e '<each of the three patterns>' <its file>
git show 83808df9a0993cf49b2ceaa4633c782e550c0180:src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java | grep -cF -e 'BufferedReader reader = new BufferedReader(new InputStreamReader(response.getInputStream()'
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd45/exec/msg-anch.txt
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 9c7a31a14de4638aa38a9d050fae88bf8340ff12 > /tmp/gsd45/exec/tail-gate-04b.log 2>&1
```

```text
slf4j_simple_integration: 2.0.20
commons_lang3_test: 3.21.0
commons_lang3_test_fixtures: 3.21.0
commons_lang3_runtime: 3.20.0
spotless_plugin: 8.10.4
connect_json_test: 4.4.0
connect_json_test_requesters: root project
connect_json_runtime: absent
connect_json_shipped: absent
bumps_already_at_tag: yes
catalog_change_in_m: +kafka-connect-json = { module = "org.apache.kafka:connect-json", version.ref = "kafka" }
catalog_commons_lang3: commonsLang3 = "3.21.0"
catalog_slf4j: slf4j = "2.0.20"
catalog_spotless: spotless = "8.10.4"
anchor_counts: 1; 1; 0
dead_reader_count_upstream: 1
anchors_mirror: rows=200 bad=0
anchors_commit: 9c7a31a14de4638aa38a9d050fae88bf8340ff12
tail_gate_line: invariants: provenance_paths=11 anchor_commits=26 anchor_rows=200 marker_commits=44 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

Configurations: `slf4j_simple_integration` is `integrationTestRuntimeClasspath`; `commons_lang3_test` and `commons_lang3_test_fixtures` are `testRuntimeClasspath` and `testFixturesRuntimeClasspath`, where conflict resolution picks the catalog's 3.21.0 over 3.20.0, 3.18.0, 3.17.0, 3.13.0 and 3.7; `commons_lang3_runtime` is `runtimeClasspath`, where the ClickHouse client 0.11.0's own 3.20.0 wins over 3.18.0; `spotless_plugin` is the `com.diffplug.spotless.gradle.plugin` line of `buildEnvironment`; `connect_json_test` is `testRuntimeClasspath`.

Of the four coordinates the user approved to land in M (plan-check answer 4), three were already at the pre-merge tip (`pre-v1.7.0_gg8` holds `commonsLang3 = "3.21.0"`, `slf4j = "2.0.20"` and `spotless = "8.10.4"`, each exactly once) and only connect-json changes: M's diff of `gradle/libs.versions.toml` against its first parent is the one added `kafka-connect-json` library line. The shipped `runtimeClasspath` carries commons-lang3 3.20.0, the ClickHouse client's own pin; the catalog's 3.21.0 reaches only `testRuntimeClasspath` and `testFixturesRuntimeClasspath`. connect-json 4.4.0 is requested only by the root project on `testRuntimeClasspath` and no dependency of that name resolves on `runtimeClasspath` or `clickhouseDependencies`, so it cannot reach the Confluent archive. The catalog has a second line starting `spotless =` (the plugin alias, `{ id = "com.diffplug.spotless", version.ref = "spotless" }`); `catalog_spotless` is the version line. The three anchor rows measured 1, 1 and 0 on the tree before the commit; the 0 row's pattern occurs once in upstream's blob of `ClickHouseHelperClient.java`, so it would catch #839's dead reader if it came back. The gate ran unedited over `M` to the anchors commit with the full SHAs and the tail-3 conflicted-paths file (about 90 s).

2026-11-04T21:05:30Z
Verdict: pass

### E-15: FORK-DELTA names the tail-3 differences

Backs: UPS-02, UPS-04, ROADMAP Phase 45 criteria 2 and 3 (every difference from upstream's #857 text and from #839's `fromJsonRow` that this tail introduced is named in `docs/FORK-DELTA.md` with its pin; the documentation commit leaves `testUnit` green and `CHANGELOG.md` and `VERSION` untouched).

```sh
git show pre-v1.7.0_gg8:docs/FORK-DELTA.md
python3 -I <row counts of the Connector source and Test harness tables, HEAD minus the tag>
grep -cF -e '<each new pin name>' docs/FORK-DELTA.md
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-docs.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
git commit -F /tmp/gsd45/exec/msg-fd.txt
git diff --name-only 722d713fcfd1c44b542b1bd3914a977f6525ab7a^ 722d713fcfd1c44b542b1bd3914a977f6525ab7a
git diff --quiet refs/tags/pre-v1.7.0_gg8 HEAD -- CHANGELOG.md VERSION
```

```text
connector_rows_delta: 2
harness_rows_delta: 1
mentions_new_pins: yes
cites_e06: yes
cites_e10: yes
testunit_exit: 0
testunit_tests: 2072
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
fork_delta_commit: 722d713fcfd1c44b542b1bd3914a977f6525ab7a
fork_delta_commit_files: docs/FORK-DELTA.md
changelog_version_unchanged_since_tag: yes
```

The Connector-source table gains two rows (the Option A record-context row citing E-06, and the JSON-route row citing E-10) and the Test-harness table one row, which names every test edited by a user decision: upstream's `ClickHouseWriterRecordContextTest` (constructor, and the three coordinate assertions moved to the log line), upstream's `FieldDescriptorEscapeTest#preservesEscapedCommentsAndDefaultExpressions` (literal JSON text), and the three renamed WRT-17 assertions of `WriterValueFreeFailureUnitTest`. The WRT-13/WRT-17 row now describes the value-free rebuild of every Jackson chain, with `Utils` named for its two public constants, and Known costs item 6 reads the Jackson residual as closed on 2026-11-04 with E-07 and E-10 to E-12. The raw-identifier row names #839's decode-all `readValue`, the five literal fields (`type`, `comment`, `default_expression`, `codec_expression`, `ttl_expression`), the slash round trip and the malformed-row contract, with the new pins. "For the next syncer" leads with `.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md` and reads Recorded Facts 42-43 in the first tail checklist, 44-45 in the second and 46 onward in the third. The testUnit total is E-12's 2041 plus E-13's 31 `FieldDescriptorLiteralFieldsUnitTest` cases. Two wording reconciliations against the plan text: the JSON-route row says the value reaches the thrown message, the rendered trace and the DLQ headers and that no measured logger line carried it (E-10 `json_value_in_logs_shapes: none`; the plan's wording named the logs), and the raw-identifier row says the comment and the three expression fields are read nowhere in `src/main` (the plan's "the four expression fields" counted `comment` as one). FORK-DELTA is an operator document: the orchestrator spawns the independent `gsd-code-reviewer` pass over it before Phase 49's publication gate. Nothing was pushed.

2026-11-04T21:10:30Z
Verdict: pass

### E-16: DEP-01 shipped set: resolution, archive cross-check, latest versions, advisories

Backs: DEP-01, ROADMAP Phase 45 criterion 5 (the dependencies that ship are checked for open advisories on the merged catalog: every coordinate `clickhouseDependencies` resolves, cross-checked against the Confluent archive, with its latest stable version and the advisories two live sources report on the exact version).

```sh
gh auth status
curl -sS -o /dev/null -w '%{http_code}' https://api.osv.dev/v1/query -X POST -d '{}'
gh api repos/ne1r0n/clickhouse-kafka-connect/dependabot/alerts
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencies --configuration clickhouseDependencies > /tmp/gsd45/exec/deps-shipped.txt
python3 -I /tmp/gsd45/scripts/parse_deps.py /tmp/gsd45/exec/deps-shipped.txt > /tmp/gsd45/exec/shipped-coords.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive > /tmp/gsd45/exec/archive.log 2>&1
unzip -Z1 build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip
python3 -I <read META-INF/maven/*/*/pom.properties and the top-level paths of the shadow jar, compare with shipped-coords.txt>
python3 -I /tmp/gsd45/scripts/advq.py control   # OSV and GitHub advisories for jackson-databind@2.9.0
python3 -I /tmp/gsd45/scripts/runq.py shipped-coords.txt shipped-adv.json   # maven-metadata.xml, OSV.dev, GitHub per coordinate
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py secrets .planning/phases/45-upstream-tail-merge-dependency-check/45-DEPENDENCY-CHECK.md
git commit -F /tmp/gsd45/exec/msg-dc1.txt
```

```text
query_date: 2026-11-04
dependabot_alerts_status: 403 Dependabot alerts are disabled for this repository.
osv_control_count: 74
ghsa_control_count: 74
shipped_coordinates: 27
archive_task_exit: 0
archive_path: build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip
archive_jars: 1
archive_jars_not_in_resolution: none
archive_pom_properties: 18
archive_pom_properties_version_mismatches: 0
archive_coordinates_matched_by_package_root: 8
archive_pom_only_coordinates: 1
archive_unmapped_top_level_paths: none
shipped_open_advisories: 0
shipped_advisory_ids: none
shipped_stale: 10
shipped_stale_list: lz4-java 1.12.1 -> 1.13.0; zstd-jni 1.5.7-23 -> 1.5.7-25; error_prone_annotations 2.50.0 -> 2.52.0; guava 33.7.2-jre -> 33.8.0-jre; commons-codec 1.19.0 -> 1.23.0; commons-io 2.20.0 -> 2.22.0; commons-lang3 3.20.0 -> 3.21.0; asm 9.7 -> 9.11.1; RoaringBitmap 1.0.6 -> 1.6.29; slf4j-api 2.0.7 -> 2.0.20
dc_commit_1: 2f126eac43c8d054450c6d9ae1403c33fd15dce8
```

The two advisory sources were proven live first: the `jackson-databind@2.9.0` control returned 74 advisories from OSV.dev and 74 from the GitHub advisory database. Dependabot alerts are disabled for the repository (the read answers HTTP 403), so the two databases are the only sources. Plan assumption A2 ("list the archive's `.jar` entries and map each file name to a coordinate") did not hold as written: `createConfluentArchive` packs one shadow jar with every dependency unpacked inside it (`verifyConfluentArchive` itself asserts "no nested jar"), so `archive_jars` is 1 and the cross-check reads inside that jar. 18 `META-INF/maven/.../pom.properties` entries each carry the same version as the resolved coordinate; eight more coordinates are matched by package root or native file name (the four ClickHouse client artifacts, zstd-jni, jspecify, asm, RoaringBitmap); `jackson-bom` is a POM and puts no jar in the archive. No jar, pom.properties or package root exists that the resolution does not explain. None of the 27 coordinates has an advisory in either source; ten are behind the newest stable version (recorded as not proposed under U-4). The note on the commons-lang3 row matches E-14: the shipped classpath carries the client's 3.20.0, not the catalog's 3.21.0.

2026-11-04T21:15:27Z
Verdict: pass

### E-17: DEP-01 remaining scopes and the DEP-02 disposition

Backs: DEP-01, DEP-02, ROADMAP Phase 45 criterion 5 (every runtime and test dependency, the plugins, the wrapper, the JDK, the workflow actions and the test images recorded with latest versions and the advisories two live sources report; under U-4 an open advisory stops the plan at a decision checkpoint and nothing moves before the user answers).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencies --configuration testRuntimeClasspath > /tmp/gsd45/exec/deps-testRuntimeClasspath.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencies --configuration testFixturesRuntimeClasspath > /tmp/gsd45/exec/deps-testFixturesRuntimeClasspath.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencies --configuration integrationTestRuntimeClasspath > /tmp/gsd45/exec/deps-integrationTestRuntimeClasspath.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain buildEnvironment > /tmp/gsd45/exec/buildenv2.txt
python3 -I /tmp/gsd45/scripts/runq.py coords-s2.txt s2-adv.json   # also coords-s3.txt, coords-s4.txt
python3 -I /tmp/gsd45/scripts/advdetail.py   # gh api advisories/<GHSA id> for severity and fixed versions
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --configuration <each test classpath> --dependency <each affected artifact>
gh api repos/ne1r0n/clickhouse-kafka-connect/actions/workflows --jq '.workflows[] | [.path, .state] | @tsv'
gh api repos/<owner>/<repo>/git/ref/tags/<tag>   # and git/tags/<sha> for annotated tags, releases/latest
gh api "advisories?ecosystem=actions&affects=<owner>/<repo>@<version>&per_page=100"
curl -s https://services.gradle.org/versions/current
git diff --quiet 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- build.gradle.kts gradle .github
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py secrets .planning/phases/45-upstream-tail-merge-dependency-check/45-DEPENDENCY-CHECK.md
git commit -F /tmp/gsd45/exec/msg-dc2.txt
```

```text
section_counts: section_2=228; section_3=7; section_4=50 (47 artifact rows, the wrapper, two JDK rows); section_5=7; section_6=5 rows (11 images)
total_open_advisories: 16
open_advisory_rows: ch.qos.logback:logback-core@1.2.13: GHSA-25qh-j22f-pwp8, GHSA-6v67-2wr5-gvf4, GHSA-jhq6-gfmj-v8fx, GHSA-p47f-322f-whfh, GHSA-pr98-23f8-jwxv, GHSA-qqpg-mvqg-649v; com.squareup.wire:wire-runtime-jvm@5.1.0: GHSA-7xpr-hc2w-34m9; com.squareup.wire:wire-runtime@5.1.0: GHSA-7xpr-hc2w-34m9, GHSA-9rm7-3qhh-h2mc, GHSA-pwf9-q62p-v7wc; commons-beanutils:commons-beanutils@1.9.4: GHSA-wxr5-93ph-8wr9; io.netty:netty-codec@4.1.115.Final: GHSA-3p8m-j85q-pgmj, GHSA-558v-64gr-wgg4, GHSA-mj4r-2hfc-f8p6; io.netty:netty-common@4.1.115.Final: GHSA-389x-839f-4rhx; io.netty:netty-handler@4.1.105.Final: GHSA-3qp7-7mw8-wx86, GHSA-4g8c-wm8x-jfhw, GHSA-c4c3-7fpv-j4q5, GHSA-c653-97m9-rcg9, GHSA-fccg-mwvh-qqg4, GHSA-x4gw-5cx5-pgmh; io.netty:netty-transport-native-epoll@4.1.105.Final: GHSA-w573-9ffj-6ff9; org.apache.mina:mina-core@2.2.3: GHSA-76h9-2vwh-w278, GHSA-8297-v2rf-2p32, GHSA-995c-6rp3-4m4x, GHSA-f2wh-grmh-r6jm, GHSA-v3pr-hxpr-mfm8, GHSA-vf5j-865m-mq7c; org.apache.zookeeper:zookeeper@3.8.4: GHSA-7xrh-hqfc-g7qr, GHSA-crhr-qqj8-rpxc; org.bitbucket.b_c:jose4j@0.9.4: GHSA-3677-xxcr-wjqv; org.eclipse.jetty.http2:http2-common@9.4.57.v20241219: GHSA-mmxm-8w33-wc4h; org.eclipse.jetty:jetty-http@9.4.57.v20241219: GHSA-355h-qmc2-wpwf, GHSA-qh8g-58pp-2wxh, GHSA-wjpw-4j6x-6rwh; org.eclipse.jetty:jetty-security@9.4.57.v20241219: GHSA-2fvj-hgj9-j2gr; org.eclipse.jetty:jetty-server@9.4.57.v20241219: GHSA-7p3p-8qv8-m2vh; org.xerial.snappy:snappy-java@1.1.10.5: GHSA-gwq9-9ffj-mvrv, GHSA-q233-g92q-qxp6, GHSA-xqrx-qh46-34m7
open_advisory_rows_shipped: 0
open_advisory_rows_by_section: section_1=0; section_2=16; section_3=0; section_4=0; section_5=0
advisory_sources_agree: yes
ccs_rows_inconclusive: 12
stale_rows: 223
tag_pinned_actions: actions/checkout@v3 resolves to v3.7.0; actions/setup-java@v3 resolves to v3.14.2; gradle/gradle-build-action@v2 resolves to v2.12.0
active_workflows_with_tag_pinned_actions: none
dep02_proposals: 16
dep02_bump_commits: 0
build_files_unchanged_since_merge: yes
dc_commit_2: ef369177a10913715bea679bf30c7353201787eb
```

Scopes: section 2 has 228 rows (the union of `testRuntimeClasspath` and `testFixturesRuntimeClasspath` minus the shipped set), section 3 has 7 (the integration-test classpath minus sections 1 and 2), section 4 has the 43 buildscript artifacts, the four JaCoCo 0.8.15 tool artifacts, the Gradle 9.8.2 wrapper (current) and the two JDK rows, section 5 has the 7 distinct `uses:` values (four SHA-pinned and current, three tag-pinned in the disabled upstream `integrationTests.yaml`), section 6 has the container images. The shipped set, the plugins, the JaCoCo tools and the seven actions have no open advisory in either source; the GitHub actions ecosystem was proven live by a control (`tj-actions/changed-files@45.0.7`). Sixteen coordinates on the test and test-fixture classpaths have open advisories (OSV.dev and the GitHub advisory database agree on every row); all of them trace to `io.confluent:kafka-schema-registry` 7.9.1, `rest-utils` 7.9.1, `kafka-protobuf-*` 7.9.1 or Kafka's `kafka-clients`, and none is one of the 27 shipped coordinates, so the Confluent archive and the allow-list are unaffected. They are written up as proposals P1 to P16 in section 7. Under U-4 nothing was bumped: `build.gradle.kts`, `gradle/` and `.github/` are byte-unchanged since M, and this entry stays pending until the user records a decision per proposal in their own words. A second finding, outside the exact-version rule: the 12 Confluent `7.9.1-ccs` Kafka artifacts on the test classpath return 0 only because the databases index Apache version numbers; Apache's 3.9.0 and 3.9.1 lines of `kafka-clients` and `kafka_2.13` do carry advisories, and what `7.9.1-ccs` contains cannot be read from either source (named for the user in section 7). Plan text reconciled against the tree: the plan expected zero open advisories (research's 2026-11-04 pre-measurement covered only the shipped set).

2026-11-04T21:25:24Z
Verdict: pending (awaiting the user's decision per proposal, U-4)

Re-measured after the user's decisions (relayed by the coordinator as the user's AskUserQuestion answers on 2026-11-04, recorded in spirit, not verbatim; no agent approved anything):

```sh
git diff --quiet 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 HEAD -- build.gradle.kts gradle .github
git commit -F /tmp/gsd45/exec/msg-dc3.txt
```

```text
dep02_decision_1: record only: P1 to P16 all declined, nothing bumped, kept as backlog in 45-DEPENDENCY-CHECK.md, none ships
dep02_decision_2: the 12 Confluent 7.9.1-ccs Kafka artifacts: the unmapped Apache version is noted as an unknown, not chased
dep02_bumps_approved: 0
dep02_disposition: none approved, nothing moved
build_files_unchanged_at_decision: yes
dc_commit_3: bd34d71bdd76b16a49f44532381378a64b89e32d
```

Every proposal is declined, so no coordinate, plugin, wrapper or action version moved and no bump gate applies; the record and 45-CONTEXT.md carry the decisions.

2026-11-04T21:27:12Z
Verdict: pass
